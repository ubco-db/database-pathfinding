import comparison.DBDiff;
import database.DBStats;
import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDynamicDB2;
import dynamic.Walls;
import map.GameMap;
import map.GroupRecord;
import search.*;
import util.ExpandArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

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
        int wallLoc = 14299;
        SearchState wall = new SearchState(wallLoc);
        wallLocation.add(wall);

        // set start and goal
        int startId = 13411;
        int goalId = 13901;

        // build DBAStar Database
        GameMap startingMap = new GameMap(PATH_TO_MAP);
        DBAStar dbaStarBW = computeDBAStarDatabase(startingMap, "BW"); // BW = before wall
        getDBAStarPath(startId, goalId, "BW", dbaStarBW);

        // add wall
        Walls.addWall(PATH_TO_MAP, wallLocation, startingMap);
        startingMap = new GameMap(PATH_TO_MAP);

        System.out.println("HERE");

        // Use the map returned after the database is fully computed
        GameMap map = dbaStarBW.getMap();

        // Get the region rep of the region the wall was added in
        int regionRepId = dbaStarBW.getAbstractProblem().findRegionRep(wall).getId();

        // region rep id for 14299 should be 15821, region id should be 116
        System.out.println(regionRepId);

        int regionId = 116;

        // Get the neighbour regions by the region id
        GroupRecord groupRecord = map.getGroups().get(regionId);

        groupRecord.getNeighborIds().forEach(System.out::println);
        System.out.println(groupRecord.getGroupRepId());

//        System.out.println(dbaStarBW.getMap().getAbstractProblem().getNeighbors(wall));
//        dbaStarBW.getAbstractProblem().getNeighbors(wall);
//        for (SearchState neighbor : dbaStarBW.getAbstractProblem().getNeighbors(wall)) {
//            System.out.println(neighbor.getId());
//        }
//        dbaStarBW.getProblem().getNeighbors(wall).forEach(neighbor -> System.out.println(neighbor.getId()));

        // recompute database
        // TODO: don't fully recompute
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
