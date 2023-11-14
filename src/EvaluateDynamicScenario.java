import comparison.DBDiff;
import database.*;
import dynamic.Walls;
import map.GameMap;
import map.GroupRecord;
import search.*;

import java.io.IOException;
import java.util.*;

public class EvaluateDynamicScenario {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "DBA/";

    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;

    final static int CUTOFF = 250; // The maximum # of moves for hill-climbing checks.
    final static int GRID_SIZE = 16;
    final static int NUM_NEIGHBOUR_LEVELS = 1; // # of neighbor levels for HCDPS


    public static void main(String[] args) {
        // set wall(s)
        ArrayList<SearchState> wallLocation = new ArrayList<>();
        int wallLoc = 4651; // wall eliminates region
        SearchState wall = new SearchState(wallLoc);
        wallLocation.add(wall);

        // set start and goal
        int startId = 13411;
        int goalId = 13901;

        // build DBAStar Database
        GameMap startingMap = new GameMap(PATH_TO_MAP);
        DBAStar dbaStarBW = computeDBAStarDatabase(startingMap, "BW"); // BW = before wall
        // getDBAStarPath(startId, goalId, "BW", dbaStarBW);

        System.out.println();
        System.out.println();
        System.out.println();

        // Use the map returned after the database is fully computed
        GameMap map = dbaStarBW.getMap();

        boolean priorWall = map.isWall(wallLoc);

        // Add wall to existing map
        map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = '*';
        // Make new MapSearchProblem (will use map with added wall)
        MapSearchProblem problem = new MapSearchProblem(map);

        if (!priorWall && map.isWall(wallLoc)) {
            System.out.println("Wall at " + wallLoc + " set successfully!");
        }

        // Get the id of the region rep of the region the wall was added in
        int regionRepId = map.getAbstractProblem().findRegionRep(wall, map).getId();
        System.out.println("regionRepId: " + regionRepId);

        if (regionRepId == wallLoc) {
            System.out.println("Wall on region rep!");
        }

        // Get the id of the region the wall was added in using its regionRepId
        TreeMap<Integer, GroupRecord> groups = new MapSearchProblem(map).getGroups(); // stores number of states per region as well, may be useful later

        Iterator<Map.Entry<Integer, GroupRecord>> it = groups.entrySet().iterator();
        Map.Entry<Integer, GroupRecord> elem;
        HashMap<Integer, Integer> regionRepIdToRegionId = new HashMap<>();
        while (it.hasNext()) {
            elem = it.next();
            regionRepIdToRegionId.put(elem.getValue().groupRepId, elem.getKey());
        }
        int regionId = regionRepIdToRegionId.get(regionRepId);
        System.out.println("regionId: " + regionId);

        // get the neighbour ids regions using the region id
        GroupRecord groupRecord = groups.get(regionId);

        if (groupRecord.getNumStates() == 1) { // scenario when there is only one state in the region
            // need to tombstone region, and make sure it doesn't have neighbours or shortest paths anymore
            groups.remove(regionId);
        } else {
            // scenario when the regioning doesn't change significantly (region id stays the same)
            int newRegionRep = map.recomputeCentroid(groupRecord, wallLoc);
            System.out.println("New rep at: " + newRegionRep);
            // get back new region rep and change the record
            groupRecord.setGroupRepId(newRegionRep);
            groups.replace(regionId, groupRecord);
        }

        // TODO: scenario where map is partitioned by wall addition
        // check that one other region is still reachable from current region
        // if not: region is cut off, may introduce new rep

        ArrayList<Integer> neighborIds = new ArrayList<>(groupRecord.getNeighborIds());
        neighborIds.add(groupRecord.groupId); // need to pass this so updates work both ways

        // Get database and initialize pathCompressAlgDba
        SubgoalDynamicDB2 dbBW = (SubgoalDynamicDB2) dbaStarBW.getDatabase();
        HillClimbing pathCompressAlgDba = new HillClimbing(problem, 10000);

        // Update regions for neighborIds in the database
        dbBW.recomputeBasePaths2(problem, groups, neighborIds, pathCompressAlgDba, dbBW.getLowestCost(), dbBW.getPaths(),
                dbBW.getNeighbor(), neighborIds.size(), NUM_NEIGHBOUR_LEVELS, true);

        // TODO: Update db (need to update numGroups, and potentially the map)

        int[][] groupsArr = dbBW.getDb().getGroups();

        if (groupRecord.getNumStates() == 1) { // tombstone record
            groupsArr[regionId-50] = null;
            dbBW.getDb().setNumRegions(groupsArr.length - 1);
        } else { // update groupsArr
            groupsArr[regionId-50] = new int[]{regionId-50, regionRepId};
        }

        // write groupsArr back to db
        dbBW.getDb().setGroups(groupsArr);

        // For checking recomputed database against AW database
        dbBW.exportDB(DBA_STAR_DB_PATH + "BW_Recomp_" + MAP_FILE_NAME + "_DBA-STAR_G" + GRID_SIZE + "_N" + NUM_NEIGHBOUR_LEVELS + "_C" + CUTOFF + ".dat");

        System.out.println();
        System.out.println();
        System.out.println();

        // recompute database
        // try to only recompute changes, then recompute entire database to see if I matched it

        // add wall to starting map
        Walls.addWall(PATH_TO_MAP, wallLocation, startingMap);
        startingMap = new GameMap(PATH_TO_MAP);

        DBAStar dbaStarAW = computeDBAStarDatabase(startingMap, "AW"); // AW = after wall
        // getDBAStarPath(startId, goalId, "AW", dbaStarAW);

        // remove wall
        Walls.removeWall(PATH_TO_MAP, wallLocation, startingMap);

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
        SearchProblem problem = dbaStar.getProblem();
        GameMap map = dbaStar.getMap();

        AStar aStar = new AStar(problem);

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
}
