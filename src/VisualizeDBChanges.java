import database.DBStats;
import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDynamicDB2;
import dynamic.Walls;
import map.GameMap;
import search.*;
import util.Entry;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class VisualizeDBChanges {
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
        dbaStar = computeDBAStar(map, "BW");

        // compute paths to all goals, store in HashMap of arrays (goal state as key)
        HashMap<Integer, ArrayList<SearchState>> paths = new HashMap<>();
        for (int goalId : goalIds) {
            // System.out.println("Goal Id:" + goalId); // 11922 throws an index out of bounds, 11922 is at index 3076
            // it's because currentId == startGroupId for some reason
            paths.put(goalId, getDBAStarPath(startId, goalId, dbaStar));
        }

        /* start loop */

        ArrayList<SearchState> weirdGoals = getWeirdGoals(); // currently fixed to start

        ArrayList<Entry> entries = new ArrayList<>();

        for (int wallId : goalIds) {
            // setting up walls
            ArrayList<SearchState> wallLocation = new ArrayList<>();
            SearchState wall = new SearchState(wallId);
            wallLocation.add(wall); // adding wall for each open state
            Walls.addWall(PATH_TO_MAP, wallLocation, map);

            map = new GameMap(PATH_TO_MAP); // recomputing map
            System.out.println();
            dbaStar = computeDBAStar(map, "AW");

            Walls.removeWall(PATH_TO_MAP, wallLocation, map);

            // iterate over all goals (open spots no wall, ignore the spot where a wall was added)
            // compare each path with stored path to same location, if identical, do nothing, if not, mark it
            ArrayList<SearchState> newPath;
            ArrayList<SearchState> oldPath;
            // should I add the wall segments to this list? They are changed bc unreachable now
            ArrayList<SearchState> goalsWithChangedPath = new ArrayList<>();
            for (int goalId : goalIds) {
                if (goalId != wallId) {
                    newPath = getDBAStarPath(startId, goalId, dbaStar);
                    oldPath = paths.get(goalId);
                    // compare each path with stored path to the same location
                    if (!isPathEqual(newPath, oldPath)) goalsWithChangedPath.add(new SearchState(goalId));
                }
            }

            // output result as image: colour start green, colour every goal with a changed path red, rest of map white
            map.showChanges(IMAGE_FOLDER_PATH + wallId + "_AW012.map_DBA_ChangedGoals.png", goalsWithChangedPath, new SearchState(startId), weirdGoals);

            // compute percentage changed as: (# goals changed by addition of specific wall) / (total # of open spaces on the wall)
            double percentageChanged = (((double) goalsWithChangedPath.size()) / goalIds.size()) * 100;
            entries.add(new Entry(percentageChanged, wallId));
        }

        // sort entries by percentageChanged in descending order
        Collections.sort(entries);

        int numEntries = 0;
        double percentSum = 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DBA_STAR_DB_PATH + "percentageChangedByWall.txt"))) {
            for (Entry entry: entries) {
                writer.write(entry.getOutput());
                percentSum += entry.getPercentageChanged();
                numEntries++;
            }
            double averagePercentageChanged = Math.round(percentSum / numEntries * 100.0) / 100.0;
            writer.write(String.format("Average percentage of goals changed by adding wall: " + averagePercentageChanged));
            System.out.println("Values written to the file 'percentageChangedByWall.txt' in order of keys.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* end loop */

        long timeTaken = System.currentTimeMillis() - startTime;

        long totalSeconds = timeTaken / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        System.out.println();
        System.out.println("This run took: " + minutes + " minutes, " + seconds + " seconds");
    }

    private static DBAStar computeDBAStar(GameMap map, String wallStatus) {
        long currentTime;

        SearchProblem problem = new MapSearchProblem(map);
        GenHillClimbing pathCompressAlgDba = new GenHillClimbing(problem, 10000);

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

        System.out.println("Exporting map with areas.");
        map.outputImage(getImageName(wallStatus, false), null, null);

        System.out.println("Exporting map with areas and centroids.");
        map.computeCentroidMap().outputImage(getImageName(wallStatus, true), null, null);

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

    private static boolean isPathEqual(ArrayList<SearchState> newPath, ArrayList<SearchState> oldPath) {
        // if path length differs, they are not equal
        if (newPath == null) return false; // QUESTION: can oldPath ever be null?
        if (newPath.size() != oldPath.size()) return false;

        for (int i = 0; i < newPath.size(); i++) {
            // comparing SearchStates (have an equals-method)
            if (!newPath.get(i).equals(oldPath.get(i))) return false;
        }
        return true;
    }

    private static String getImageName(String wallStatus, boolean hasCentroids) {
        String lastToken = hasCentroids ? "_DBA_Centroid.png" : "_DBA.png";
        return DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME + lastToken;
    }

    private static String getDBName(String wallStatus) {
        return DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME + "_DBA-STAR_G" + GRID_SIZE + "_N" + NUM_NEIGHBOUR_LEVELS + "_C" + CUTOFF + ".dat";
    }

    // TODO: find fix for these goals
    private static ArrayList<SearchState> getWeirdGoals() {
        // goals where startGroupId == goalGroupId in findNearest() in SubgoalDynamicDB2
        ArrayList<Integer> weirdGoalIds = new ArrayList<>(Arrays.asList(11922, 11923, 11924, 11925, 11926, 11927, 11928, 11929, 11930, 11931, 11932, 11933, 11934, 11935, 12071, 12072, 12073, 12074, 12075, 12078, 12079, 12080, 12081, 12082, 12083, 12219, 12220, 12221, 12222, 12223, 12226, 12227, 12228, 12229, 12230, 12231, 12364, 12365, 12368, 12369, 12370, 12371, 12372, 12373, 12374, 12375, 12376, 12377, 12378, 12379, 12512, 12513, 12516, 12517, 12518, 12519, 12520, 12521, 12522, 12523, 12524, 12525, 12526, 12527, 12660, 12661, 12662, 12664, 12665, 12666, 12667, 12668, 12669, 12670, 12671, 12672, 12673, 12674, 12675, 12808, 12809, 12810, 12811, 12812, 12813, 12814, 12815, 12816, 12817, 12818, 12819, 12820, 12821, 12822, 12823, 12956, 12957, 12958, 12959, 12960, 12961, 12962, 12963, 12964, 12965, 12966, 12967, 12968, 12969, 12970, 12971, 13104, 13105, 13106, 13107, 13108, 13109, 13110, 13111, 13112, 13113, 13114, 13115, 13116, 13117, 13118, 13119, 13252, 13253, 13254, 13255, 13256, 13257, 13258, 13259, 13260, 13261, 13262, 13263, 13264, 13265, 13266, 13267, 13400, 13401, 13402, 13403, 13404, 13405, 13406, 13407, 13408, 13409, 13410, 13412, 13413, 13414, 13415, 13548, 13549, 13550, 13551, 13552, 13553, 13554, 13555, 13556, 13557, 13558, 13559, 13560, 13561, 13562, 13563, 13696, 13697, 13698, 13699, 13700, 13701, 13702, 13703, 13704, 13705, 13706, 13707, 13708, 13709, 13710, 13711, 13844, 13845, 13846, 13847, 13848, 13849, 13850, 13851, 13852, 13853, 13854, 13855, 13856, 13857, 13858, 13859, 13992, 13993, 13994, 13995, 13996, 13997, 13998, 13999, 14000, 14001, 14002, 14003, 14004, 14005, 14006, 14007, 14140, 14141, 14142, 14143, 14144, 14145, 14146, 14147, 14148, 14149, 14150, 14151, 14152, 14153, 14154, 14155));

        ArrayList<SearchState> weirdGoals = new ArrayList<>();
        for (Integer weirdGoalId : weirdGoalIds) {
            weirdGoals.add(new SearchState(weirdGoalId));
        }

        return weirdGoals;
    }
}
