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
        // add wall(s)
        ArrayList<SearchState> wallLocation = new ArrayList<>();
        // 8942
        int wallLoc = 12969; // adding this wall changes the shortest path between 12963 and 12978
        SearchState wall = new SearchState(wallLoc);
        wallLocation.add(wall);

        // set start and goal
        int startId = 13411;
        int goalId = 13901;

        // build DBAStar Database
        GameMap startingMap = new GameMap(PATH_TO_MAP);
        DBAStar dbaStarBW = computeDBAStarDatabase(startingMap, "BW"); // BW = before wall
        getDBAStarPath(startId, goalId, "BW", dbaStarBW);

        System.out.println();
        System.out.println();
        System.out.println();

        // Use the map returned after the database is fully computed
        GameMap map = dbaStarBW.getMap();

        // Get the id of the region rep of the region the wall was added in
        int regionRepId = dbaStarBW.getAbstractProblem().findRegionRep(wall).getId();
        System.out.println("regionRepId: " + regionRepId);

        // Get the id of the region the wall was added in using its regionRepId
        HashMap<Integer, GroupRecord> groups = new MapSearchProblem(map).getGroups(); // stores number of states per region as well, may be useful later
        Iterator<Map.Entry<Integer, GroupRecord>> it = groups.entrySet().iterator();
        Map.Entry<Integer, GroupRecord> elem;
        HashMap<Integer, Integer> regionRepIdToRegionId = new HashMap<>();
        while (it.hasNext()) {
            elem = it.next();
            regionRepIdToRegionId.put(elem.getValue().groupRepId, elem.getKey());
        }
        int regionId = regionRepIdToRegionId.get(regionRepId);
        System.out.println("regionId: " + regionId);

        // Get the neighbour ids regions using the region id
        GroupRecord groupRecord = map.getGroups().get(regionId);
        ArrayList<Integer> neighborIds = new ArrayList<>(groupRecord.getNeighborIds());

        // Get region reps of neighbor regions
        HashSet<Integer> neighborRegionRepIds = new HashSet<>();
        int neighborRegionRegionRep;
        System.out.println("neighborRegionRepIds:");
        for (int neighborId : neighborIds) {
            neighborRegionRegionRep = groups.get(neighborId).getGroupRepId();
            neighborRegionRepIds.add(neighborRegionRegionRep);
            System.out.println(neighborRegionRegionRep);
        }

        // TODO: Update regions for neighborIds in the database
        SubgoalDynamicDB2 dbBW = (SubgoalDynamicDB2) dbaStarBW.getDatabase();

        // 55 and 56 need to be updated (how does the indexing in this array work?)
        int[][] lowestCostBW = dbBW.getLowestCost();

        // need to look at lowest costs for neighbours and recompute those (how to recompute?)


        System.out.println();

        Walls.addWall(PATH_TO_MAP, wallLocation, startingMap);
        startingMap = new GameMap(PATH_TO_MAP);

        // recompute database TODO: don't fully recompute
        // try to only recompute immediate changes, then recompute entire database to see if I matched it

        DBAStar dbaStarAW = computeDBAStarDatabase(startingMap, "AW"); // AW = after wall
        getDBAStarPath(startId, goalId, "AW", dbaStarAW);

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
