import database.DBStats;
import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDynamicDB2;
import dynamic.Walls;
import map.GameMap;
import map.GroupRecord;
import search.*;
import util.ExpandArray;

import java.util.*;

import static map.GameMap.START_NUM;

public class EvaluateWallRemoval {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "removal/";

    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;

    final static int CUTOFF = 250; // The maximum # of moves for hill-climbing checks.
    final static int GRID_SIZE = 16;
    final static int NUM_NEIGHBOUR_LEVELS = 1; // # of neighbor levels for HCDPS

    public static void main(String[] args) {
        // set start and goal
        int startId = 10219;
        int goalId = 4632;

        // build DBAStar Database
        GameMap startingMap = new GameMap(PATH_TO_MAP);
        DBAStar dbaStarBW = computeDBAStarDatabase(startingMap, "BW"); // BW = before wall
        getDBAStarPath(startId, goalId, "BW", dbaStarBW);

        // set wall(s)
        ArrayList<SearchState> wallLocation = new ArrayList<>();
        int wallLoc = 4632;
        SearchState wall = new SearchState(wallLoc);
        wallLocation.add(wall);

        // Use the map returned after the database is fully computed
        System.out.println();
        System.out.println();
        System.out.println();

        long startTimeRecomp = System.currentTimeMillis();

        GameMap map = dbaStarBW.getMap();

        boolean priorWall = map.isWall(wallLoc);

        int wallRow = map.getRow(wallLoc);
        int wallCol = map.getCol(wallLoc);

        // Remove wall from existing map and map inside problem
        map.squares[wallRow][wallCol] = ' '; // TODO: add correct region id here later
        MapSearchProblem problem = (MapSearchProblem) dbaStarBW.getProblem();
        priorWall = priorWall && problem.getMap().isWall(wallLoc);
        problem.getMap().squares[wallRow][wallCol] = ' '; // TODO: add correct region id here later

        if (priorWall && !map.isWall(wallLoc) && !problem.getMap().isWall(wallLoc)) {
            System.out.println("Wall at " + wallLoc + " removed successfully!");
        } else {
            System.out.printf("No wall found at (%d, %d)%n", wallRow, wallCol);
        }

        TreeMap<Integer, GroupRecord> groups = new MapSearchProblem(map).getGroups();

        // Grab neighbouring states
        ArrayList<SearchState> neighbours = map.getNeighbors(wallRow, wallCol);
        Map<Integer, Integer> openStatesToSectors = new HashMap<>();

        if (isSurroundedByWalls(map, neighbours, openStatesToSectors)) {
            // Case 1: If a wall is encased by walls, we necessarily have a new, isolated region

            // Assign new region id to the location on the map
            int groupId = groups.size() + START_NUM;

            map.squares[wallRow][wallCol] = groupId;
            problem.getMap().squares[wallRow][wallCol] = groupId;

            // There should not be a group record with the new region id
            GroupRecord rec = groups.get(groupId);
            if (rec != null) System.out.println("Error! Record already exists!");

            // Create a new group record for the new region
            GroupRecord newRec = new GroupRecord();
            newRec.setNumStates(1);
            newRec.groupId = groupId;
            // Group rep id does not need to be computed using compute centroids logic since it must be where the wall was removed
            newRec.groupRepId = map.getId(wallRow, wallCol);
            newRec.states = new ExpandArray(1);
            newRec.states.add(newRec.groupRepId);
            map.addGroup(groupId, newRec);
            groups.put(groupId, newRec);

            // Get database and initialize pathCompressAlgDba
            SubgoalDynamicDB2 dbBW = (SubgoalDynamicDB2) dbaStarBW.getDatabase();
            HillClimbing pathCompressAlgDba = new HillClimbing(problem, 10000);

            // Update regions for neighborIds in the database (only region requiring updates is new region, since it has no neighbours)
            ArrayList<Integer> neighborIds = new ArrayList<>();
            neighborIds.add(newRec.groupId);

            // Value of isPartition actually makes no difference here since that logic is skipped, set to true for consistency with code below
            dbBW.recomputeBasePathsAfterWallChange(problem, groups, neighborIds, pathCompressAlgDba, dbBW.getLowestCost(),
                    dbBW.getPaths(), dbBW.getNeighbor(), neighborIds.size(), NUM_NEIGHBOUR_LEVELS, false, true);

            // Re-generate index database (TODO: optimize)
            // groupId and regionRepId are identical in this case, isPartition because groupsMapping needs to be resized
            dbBW.regenerateIndexDB(true, false, groupId, groupId, groups.size(), map, new GroupRecord[]{newRec});

            dbBW.exportDB(DBA_STAR_DB_PATH + "BW_Recomp_" + MAP_FILE_NAME + "_DBA-STAR_G" + GRID_SIZE + "_N" + NUM_NEIGHBOUR_LEVELS + "_C" + CUTOFF + ".dat");
        } else {
            // Case 2: If a wall is not encased by walls, we need to check its sector membership, and the sector membership of the adjacent open spaces

            // Check sector membership of space where wall was
            int numSectorsPerRow = (int) Math.ceil(map.cols * 1.0 / GRID_SIZE);
            int sectorId = wallRow / GRID_SIZE * numSectorsPerRow + wallCol / GRID_SIZE;
            System.out.println("Wall was removed in sector: " + sectorId);

            // Check if it matches sector membership of surrounding open spaces
            if (openStatesToSectors.containsValue(sectorId)) {
                System.out.println("Removed wall in existing sector!");
                // TODO: Wall touches region that is in same sector as wall -> add wall to region and recompute neighbourhood (may have formed path)

                // can I just run findRegionRep to assign the state to a region?
                int regionRepId = map.getAbstractProblem().findRegionRep(wall, map).getId();
                System.out.println("Existing region, region rep id: " + regionRepId);

                // what if the wall is in the same sector but a new region? would findRegionRep return null?

                // should probably recompute the entire sector, since it may lead to two regions being merged
                // though that could only happen if the wall touches two different regions

                // TODO: reverse partition case, later: optimize by looking at openStatesToSectors, if same sector, different regions are touching wall

                // will need to recompute centroids
            } else {
                System.out.println("Removed wall in new sector!");
                /*
                Case 3: Basically like case 1, but need to recompute paths to neighbours
                TODO: Wall touches region but it is not in same sector as wall -> new, connected, region (recompute neighbourhood)
                 */
                // Assign new region id to the location on the map

                int groupId = groups.size() + START_NUM;

                map.squares[wallRow][wallCol] = groupId;
                problem.getMap().squares[wallRow][wallCol] = groupId;

                // There should not be a group record with the new region id
                GroupRecord rec = groups.get(groupId);
                if (rec != null) System.out.println("Error! Record already exists!");

                // Create a new group record for the new region
                GroupRecord newRec = new GroupRecord();
                newRec.setNumStates(1);
                newRec.groupId = groupId;
                // Group rep id does not need to be computed using compute centroids logic since it must be where the wall was removed
                newRec.groupRepId = map.getId(wallRow, wallCol);
                newRec.states = new ExpandArray(1);
                newRec.states.add(newRec.groupRepId);
                map.addGroup(groupId, newRec);
                groups.put(groupId, newRec);

                // Get database and initialize pathCompressAlgDba
                SubgoalDynamicDB2 dbBW = (SubgoalDynamicDB2) dbaStarBW.getDatabase();
                HillClimbing pathCompressAlgDba = new HillClimbing(problem, 10000);

                // Grab neighbour states from openStatesToSectors, check which regions they belong to, get the reps for those regions, use set to ensure uniqueness
                Set<Integer> neighbouringRegions = new HashSet<>();

                for (Integer stateId : openStatesToSectors.keySet()) {
                    // state id - region id
                    System.out.println(stateId + " - " + map.squares[map.getRow(stateId)][map.getCol(stateId)]);
                    // Add region id for region
                    neighbouringRegions.add(map.squares[map.getRow(stateId)][map.getCol(stateId)]);
                }

                // Update regions for neighborIds in the database
                ArrayList<Integer> neighborIds = new ArrayList<>(neighbouringRegions);
                neighborIds.add(newRec.groupId);

                // TODO: Do I need to rebuild abstract problem and recompute neighbours?

                // Compute start and end of new sector
                int startRow = (sectorId % numSectorsPerRow) * GRID_SIZE;
                int startCol = (sectorId / numSectorsPerRow) * GRID_SIZE;
                int endRow = startRow + GRID_SIZE;
                int endCol = startCol + GRID_SIZE;

                // Rebuild abstract problem
                map.rebuildAbstractProblem(GRID_SIZE, startRow, startCol, groups);

                // Set neighbours
                map.recomputeNeighbors(GRID_SIZE, startRow, startCol, endRow, endCol, neighborIds);

                // Value of isPartition actually makes no difference here since that logic is skipped, set to true for consistency with code below
                dbBW.recomputeBasePathsAfterWallChange(problem, groups, neighborIds, pathCompressAlgDba, dbBW.getLowestCost(), dbBW.getPaths(),
                        dbBW.getNeighbor(), neighborIds.size(), NUM_NEIGHBOUR_LEVELS, false, true);

                // Re-generate index database (TODO: optimize)
                // groupId and regionRepId are identical in this case, isPartition because groupsMapping needs to be resized
                dbBW.regenerateIndexDB(true, false, groupId, groupId, groups.size(), map, new GroupRecord[]{newRec});

                // For checking recomputed database against AW database
                dbBW.exportDB(DBA_STAR_DB_PATH + "BW_Recomp_" + MAP_FILE_NAME + "_DBA-STAR_G" + GRID_SIZE + "_N" + NUM_NEIGHBOUR_LEVELS + "_C" + CUTOFF + ".dat");

                // TODO: Figure out why I need to do this and where subgoals come from
                dbaStarBW.getSubgoals().clear();
            }
        }

        getDBAStarPath(startId, goalId, "BW_Recomp", dbaStarBW);

        System.out.println("Exporting map with areas and centroids.");
        map.computeCentroidMap().outputImage(getImageName("BW_Recomp", true), null, null);

        System.out.println();
        System.out.println();
        System.out.println();

        // remove wall from starting map
        Walls.removeWall(PATH_TO_MAP, wallLocation, startingMap);
        startingMap = new GameMap(PATH_TO_MAP);

        DBAStar dbaStarAW = computeDBAStarDatabase(startingMap, "RW"); // RW = removed wall
        getDBAStarPath(startId, goalId, "RW", dbaStarAW);

        // add wall back in
        Walls.addWall(PATH_TO_MAP, wallLocation, startingMap);
    }

    private static DBAStar computeDBAStarDatabase(GameMap map, String wallStatus) {
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

        // This is where region reps and groups on the map are computed
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

        // QUESTION: Why are we passing this tmpProb?
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

        // return database here to access and modify
        return new DBAStar(problem, map, database);
    }

    private static void getDBAStarPath(int startId, int goalId, String wallStatus, DBAStar dbaStar) {
        GameMap map = dbaStar.getMap();

        AStar aStar = new AStar(dbaStar.getProblem());

        StatsRecord dbaStats = new StatsRecord();
        ArrayList<SearchState> path = dbaStar.computePath(new SearchState(startId), new SearchState(goalId), dbaStats);

        StatsRecord aStarStats = new StatsRecord();
        ArrayList<SearchState> optimalPath = aStar.computePath(new SearchState(startId), new SearchState(goalId), aStarStats);

        System.out.println("AStar path cost: " + aStarStats.getPathCost() + " DBAStar path cost: " + dbaStats.getPathCost());
        System.out.println("Suboptimality: " + ((((double) dbaStats.getPathCost()) / aStarStats.getPathCost()) - 1) * 100.0);

        if (path == null || path.isEmpty()) {
            System.out.printf("No path was found between %d and %d!%n", startId, goalId);
        }
        map.computeCentroidMap().outputImage(DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME + "_path.png", path, null);
        map.computeCentroidMap().outputImage(DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME + "_optimal_path.png", optimalPath, null);
    }

    /* Helper methods */

    private static String getDBName(String wallStatus) {
        return DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME + "_DBA-STAR_G" + GRID_SIZE + "_N" + NUM_NEIGHBOUR_LEVELS + "_C" + CUTOFF + ".dat";
    }

    private static String getImageName(String wallStatus, boolean hasCentroids) {
        String lastToken = hasCentroids ? "_DBA_Centroid.png" : "_DBA.png";
        return DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME + lastToken;
    }

    private static boolean isSurroundedByWalls(GameMap map, ArrayList<SearchState> neighbours, Map<Integer, Integer> openStatesToSectors) {
        // Return true if all 8 neighbours of the cell are walls, else return false

        for (SearchState neighbour : neighbours) {
            // Need to use !isWall instead of isOpenCell, because the cells are not empty, they have their regions written into them
            if (!map.isWall(neighbour.id)) {
                // Fill HashMap with state id to sector id mapping
                openStatesToSectors.put(neighbour.id, getSectorId(map, neighbour.id));
            }
        }

        return openStatesToSectors.isEmpty();
    }

    private static int getSectorId(GameMap map, int row, int col) {
        int numSectorsPerRow = (int) Math.ceil(map.cols * 1.0 / GRID_SIZE);
        return row / GRID_SIZE * numSectorsPerRow + col / GRID_SIZE;
    }

    private static int getSectorId(GameMap map, int sid) {
        int row = map.getRow(sid);
        int col = map.getCol(sid);
        return getSectorId(map, row, col);
    }
}
