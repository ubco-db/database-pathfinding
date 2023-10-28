import comparison.ChangedPath;
import comparison.DBDiff;
import comparison.Entry;
import database.DBStats;
import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDynamicDB2;
import dynamic.Walls;
import map.GameMap;
import search.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


/**
 * In this program, walls are added to each open spot on a map. The impact of those walls is calculated and output to
 * percentageChangedByWall.txt. Additionally, the impact is visualized in impactfulWallsHeatMap.png. In this file, the
 * open spots where adding a wall would make the biggest difference are coloured using a pink gradient (darker pink for
 * higher impact). Impact is calculated as follows: number of open spots to which the path has changed through the
 * addition of the wall divided by the total number of open spots.
 * Furthermore, images of all added walls are stored in the changed_goals folder. The added walls are coloured blue, the
 * (fixed) start of the path is coloured green, and goals to which the path has changed are coloured using a pink
 * gradient (darker pink for higher impact). Impact is calculated using a Jaccard similarity coefficient.
 */
public class VisualizeAddingWallsChanges {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "adding_walls/";
    final static String MAP_FILE_PATH = "maps/dMap/";

    final static String IMAGE_FOLDER_PATH = DBA_STAR_DB_PATH + "changed_goals/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;

    final static int CUTOFF = 250; // The maximum # of moves for hill-climbing checks.
    final static int GRID_SIZE = 16;
    final static int NUM_NEIGHBOUR_LEVELS = 1; // # of neighbor levels for HCDPS

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        DBAStar dbaStar;
        GameMap map = new GameMap(PATH_TO_MAP);

        // fix start
        int startId = 13411;

        ArrayList<Integer> goalIds = new ArrayList<>();
        // iterate over all goals (open spots no wall)
        for (int i = 16; i < map.rows; i++) {
            for (int j = 0; j < map.cols; j++) {
                if (!map.isWall(i, j)) {
                    goalIds.add(map.getId(i, j));
                }
            }
        }

        // remove startId from list of goals
        goalIds.remove((Integer) startId);

        // print number of goals (6175 on 012.map)
        System.out.println("Number of goals: " + goalIds.size());

        // compute DBAStar database before adding wall
        System.out.println();
        dbaStar = computeDBAStar(map, 0, "BW");

        ArrayList<Integer> regionRepsBW = dbaStar.getDBAStarMap().getRegionReps();

        // compute paths to all goals, store in HashMap of arrays (goal state as key)
        HashMap<Integer, ArrayList<SearchState>> paths = new HashMap<>();
        for (int goalId : goalIds) {
            paths.put(goalId, getDBAStarPath(startId, goalId, dbaStar));
        }

        /* start loop */

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> nonExistentPaths = new ArrayList<>();
        ArrayList<SearchState> wallsThatChangeRegioning = new ArrayList<>();

        for (int wallId : goalIds) {
            // setting up walls
            ArrayList<SearchState> wallLocation = new ArrayList<>();
            SearchState wall = new SearchState(wallId);
            wallLocation.add(wall); // adding wall for each open state
            Walls.addWall(PATH_TO_MAP, wallLocation, map);

            map = new GameMap(PATH_TO_MAP); // recomputing map
            System.out.println();
            dbaStar = computeDBAStar(map, wallId, "AW");

            ArrayList<Integer> regionRepsAW = dbaStar.getDBAStarMap().getRegionReps();

            // find differences in region rep lists
            if (!isRegionRepListEqual(regionRepsAW, regionRepsBW)) {
                wallsThatChangeRegioning.add(new SearchState(wallId));
            }

            Walls.removeWall(PATH_TO_MAP, wallLocation, map);

            // iterate over all goals (open spots no wall, ignore the spot where a wall was added)
            // compare each path with stored path to same location, if identical, do nothing, if not, mark it
            ArrayList<SearchState> newPath;
            ArrayList<SearchState> oldPath;

            // should I add the wall segments to this list? They are changed bc unreachable now
            ArrayList<ChangedPath> changedPaths = new ArrayList<>();

            for (int goalId : goalIds) {
                if (goalId != wallId) {
                    newPath = getDBAStarPath(startId, goalId, dbaStar);
                    if (newPath.isEmpty()) {
                        nonExistentPaths.add(String.format("After adding a wall at %d, no path was found between %d and %d!%n", wallId, startId, goalId));
                    }
                    oldPath = paths.get(goalId);
                    // compare each path with stored path to the same location
                    if (!isPathEqual(newPath, oldPath))
                        changedPaths.add(new ChangedPath(new SearchState(goalId), getPathDiff(newPath, oldPath)));
                }
            }

            // output result as image: colour start green, colour added wall blue, colour every goal with a changed path pink, rest of map white
            map.showChanges(IMAGE_FOLDER_PATH + wallId + "_AW012.map_DBA_ChangedGoals.png", changedPaths, new SearchState(startId), new SearchState(wallId));

            // compute percentage changed as: (# goals changed by addition of specific wall) / (total # of open spaces on the wall)
            // TODO: account for amount of change, not just number of changes?
            double percentageChanged = (((double) changedPaths.size()) / goalIds.size()) * 100;
            entries.add(new Entry(percentageChanged, wallId, changedPaths));
        }

        /* end loop */

        // sort entries by percentageChanged in descending order
        Collections.sort(entries);

        int numEntries = 0;
        double percentSum = 0;

        HashMap<SearchState, Double> wallImpactMap = new HashMap<>();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DBA_STAR_DB_PATH + "percentageChangedByWall.txt"))) {
            for (Entry entry : entries) {
                writer.write(entry.getOutput());
                // need to output changedPaths grouped by pathDiff, considering 4 groups
                // 0-25% change, 25-50% change, 50-75% change, 75-100% change
                writer.write(System.lineSeparator());

                wallImpactMap.put(new SearchState(entry.getWallId()), entry.getPercentageChanged());
                percentSum += entry.getPercentageChanged();
                numEntries++;
            }
            double averagePercentageChanged = Math.round(percentSum / numEntries * 100.0) / 100.0;
            writer.write(String.format("Average percentage of goals changed by adding wall: " + averagePercentageChanged));
            System.out.println("Values written to the file 'percentageChangedByWall.txt' in order of keys.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Before walls are added, safe explorability of the wall is given, after, it may not be
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DBA_STAR_DB_PATH + "nonExistentPaths.txt"))) {
            for (String nonExistentPath : nonExistentPaths) {
                writer.write(nonExistentPath);
            }
            System.out.println("Non-existent paths written to 'nonExistentPaths.txt'.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Write out ids of walls that change the regioning (move region reps)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DBA_STAR_DB_PATH + "wallsThatChangeRegioning.txt"))) {
            for (SearchState searchState : wallsThatChangeRegioning) {
                writer.write(searchState.getId() + "\n");
            }
            System.out.println("Walls that change regioning written to 'wallsThatChangeRegioning.txt'.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Draw walls whose addition changes the regioning (move region reps)
        map.showWallsThatChangeRegioning(DBA_STAR_DB_PATH + "wallsThatChangeRegioning.png", wallsThatChangeRegioning, new SearchState(startId));

        // Draw walls, colour them based on how much impact on paths they have
        map.showHeatMap(DBA_STAR_DB_PATH + "impactfulWallsHeatMap.png", wallImpactMap, new SearchState(startId));

        long timeTaken = System.currentTimeMillis() - startTime;

        long totalSeconds = timeTaken / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        System.out.println();
        System.out.println("This run took: " + minutes + " minutes, " + seconds + " seconds");
    }

    private static DBAStar computeDBAStar(GameMap map, int wallLoc, String wallStatus) {
        long currentTime;

        SearchProblem problem = new MapSearchProblem(map);
        HillClimbing pathCompressAlgDba = new HillClimbing(problem, 10000);

        // Load abstract map and database
        System.out.println("Loading database.");

        SubgoalDynamicDB2 database = new SubgoalDynamicDB2(); // DP matrix in adjacency list representation (computed at run-time)

        String fileName = getDBName(wallStatus);

        System.out.println("Loading map and performing abstraction...");

        // GreedyHC map abstraction
        DBStats dbStats = new DBStats();
        DBStats.init(dbStats);

        DBStatsRecord rec = new DBStatsRecord(dbStats.getSize());
        rec.addStat(0, "dbaStar (" + NUM_NEIGHBOUR_LEVELS + ")");
        rec.addStat(1, GRID_SIZE);
        rec.addStat(3, CUTOFF);
        rec.addStat(4, MAP_FILE_NAME);
        rec.addStat(5, map.rows);
        rec.addStat(6, map.cols);

        currentTime = System.currentTimeMillis();
        map = map.sectorAbstract2(GRID_SIZE);

        long resultTime = System.currentTimeMillis() - currentTime;
        rec.addStat(12, resultTime);
        rec.addStat(10, resultTime);
        rec.addStat(11, map.states);
        rec.addStat(7, map.states);
        dbStats.addRecord(rec);

        // System.out.println("Exporting map with areas.");
        // map.outputImage(getImageName(wallStatus, false), null, null);

        // System.out.println("Exporting map with areas and centroids.");
        // map.computeCentroidMap().outputImage(getImageName(wallStatus, true), null, null);

        SearchProblem tmpProb = new MapSearchProblem(map);
        GameDB gameDB = new GameDB(tmpProb);

        currentTime = System.currentTimeMillis();
        database.computeIndex(tmpProb, rec);
        rec.addStat(23, System.currentTimeMillis() - currentTime);

        System.out.println("Generating gameDB.");
        currentTime = System.currentTimeMillis();

        database = gameDB.computeDynamicDB(database, pathCompressAlgDba, rec, NUM_NEIGHBOUR_LEVELS);
        System.out.println("Time to compute DBAStar gameDB: " + (System.currentTimeMillis() - currentTime));

        database.init();

        database.exportDB(fileName);
        map.computeComplexity(rec);
        dbStats.addRecord(rec);
        database.setProblem(problem);
        System.out.println("Verifying database.");
        database.verify(pathCompressAlgDba);
        System.out.println("Database verification complete.");
        System.out.println("Databases loaded.");

        // compare databases
        try {
            String f1Name = "BW012.map_DBA-STAR_G16_N1_C250.";
            String f2Name = "AW012.map_DBA-STAR_G16_N1_C250.";
            String ext = "dati2";
            DBDiff.getDBDiff(DBA_STAR_DB_PATH, wallLoc, f1Name, f2Name, ext);
            f1Name = "BW012.map_DBA-STAR_G16_N1_C250.";
            f2Name = "AW012.map_DBA-STAR_G16_N1_C250.";
            ext = "dat";
            DBDiff.getDBDiff(DBA_STAR_DB_PATH, wallLoc, f1Name, f2Name, ext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new DBAStar(problem, map, database);
    }

    private static ArrayList<SearchState> getDBAStarPath(int startId, int goalId, DBAStar dbaStar) {
        StatsRecord stats = new StatsRecord();
        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);

        // ArrayList<SearchState> subgoals = dbaStar.getSubgoals();
        return dbaStar.computePath(start, goal, stats);
    }

    /* Helper methods */

    // TODO: refactor so that isRegionRepListEqual is same method as isPathEqual
    private static boolean isRegionRepListEqual(ArrayList<Integer> regionRepsAW, ArrayList<Integer> regionRepsBW) {
        if (regionRepsBW.size() != regionRepsAW.size()) return false;

        for (int i = 0; i < regionRepsBW.size(); i++) {
            if (!regionRepsBW.get(i).equals(regionRepsAW.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPathEqual(ArrayList<SearchState> newPath, ArrayList<SearchState> oldPath) {
        // if path length differs, they are not equal
        if (newPath == null)
            return false; // QUESTION: can oldPath ever be null? No, because safe explorability is assumed
        if (newPath.size() != oldPath.size()) return false;

        for (int i = 0; i < newPath.size(); i++) {
            // comparing SearchStates (have an equals-method)
            if (!newPath.get(i).equals(oldPath.get(i))) return false;
        }
        return true;
    }

    private static double getPathDiff(ArrayList<SearchState> newPath, ArrayList<SearchState> oldPath) {
        // Convert ArrayLists to sets
        Set<Integer> set1 = new HashSet<>();
        for (SearchState s : newPath) {
            set1.add(s.getId());
        }

        Set<Integer> set2 = new HashSet<>();
        for (SearchState s : oldPath) {
            set2.add(s.getId());
        }

        // Calculate intersection and union
        Set<Integer> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<Integer> union = new HashSet<>(set1);
        union.addAll(set2);

        // Calculate Jaccard similarity coefficient
        double jaccardSimilarity = (double) intersection.size() / union.size();

        // Return the percentage difference
        return (1 - jaccardSimilarity) * 100;
    }

    private static String getImageName(String wallStatus, boolean hasCentroids) {
        String lastToken = hasCentroids ? "_DBA_Centroid.png" : "_DBA.png";
        return DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME + lastToken;
    }

    private static String getDBName(String wallStatus) {
        return DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME + "_DBA-STAR_G" + GRID_SIZE + "_N" + NUM_NEIGHBOUR_LEVELS + "_C" + CUTOFF + ".dat";
    }
}
