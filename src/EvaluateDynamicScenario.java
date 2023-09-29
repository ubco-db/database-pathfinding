import database.DBStats;
import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDynamicDB2;
import dynamic.Walls;
import map.GameMap;
import search.GenHillClimbing;
import search.MapSearchProblem;
import search.SearchAbstractAlgorithm;
import search.SearchProblem;
import search.SearchState;
import search.StatsRecord;

import java.util.ArrayList;

public class EvaluateDynamicScenario {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "DBA/";

    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";

    final static int CUTOFF = 250; // The maximum # of moves for hill-climbing checks.
    final static int GRID_SIZE = 16;
    final static int NUM_NEIGHBOUR_LEVELS = 1; // # of neighbor levels for HCDPS


    public static void main(String[] args) {
        // build DBAStar Database

        GameMap map = new GameMap(MAP_FILE_PATH + MAP_FILE_NAME);
        computeDBAStarDatabase(map);

        // add wall
        ArrayList<SearchState> a = new ArrayList<>();
        a.add(new SearchState(7003));
        a.add(new SearchState(7151));
        a.add(new SearchState(7299));
        a.add(new SearchState(7447));
        a.add(new SearchState(7595));
        a.add(new SearchState(7743));
        Walls.addWall("maps/dMap/012.map", a, map);

        // recompute database
        // compare databases
    }

    private static void computeDBAStarDatabase(GameMap map) {
        SearchProblem problem = null;

        StatsRecord stats = new StatsRecord();
        DBStatsRecord rec;
        int dbaStarRecords = 0;

        SubgoalDynamicDB2 database = new SubgoalDynamicDB2();
        DBStats[] dbStats = new DBStats[1];
        GameMap[] maps = new GameMap[1];

        long currentTime = System.currentTimeMillis();

        problem = new MapSearchProblem(map);

        SearchAbstractAlgorithm alg = new GenHillClimbing(problem, CUTOFF);
        GenHillClimbing pathCompressAlgDba = new GenHillClimbing(problem, 10000);

        // Load abstract map and database
        System.out.println("Loading database.");
        String fileName;

        database = new SubgoalDynamicDB2();   // DP matrix in adjacency list representation (computed at run-time)

        fileName = DBA_STAR_DB_PATH + MAP_FILE_NAME + "_DBA-STAR_G" + GRID_SIZE + "_N" + NUM_NEIGHBOUR_LEVELS + "_C" + CUTOFF + ".dat";

        System.out.println("Loading map and performing abstraction...");

        // GreedyHC map abstraction
        if (dbStats[0] == null) {
            dbStats[0] = new DBStats();
            DBStats.init(dbStats[0]);
        }
        rec = new DBStatsRecord(dbStats[0].getSize());
        rec.addStat(0, "dbaStar (" + NUM_NEIGHBOUR_LEVELS + ")");
        rec.addStat(1, GRID_SIZE);
        rec.addStat(3, CUTOFF);
        rec.addStat(4, MAP_FILE_NAME);
        rec.addStat(5, map.rows);
        rec.addStat(6, map.cols);

        currentTime = System.currentTimeMillis();
        maps[0] = map.sectorAbstract2(GRID_SIZE);
        long resultTime = System.currentTimeMillis() - currentTime;
        rec.addStat(12, resultTime);
        rec.addStat(10, resultTime);
        rec.addStat(11, maps[0].states);
        rec.addStat(7, maps[0].states);
        dbStats[0].addRecord(rec);

        System.out.println("Exporting map with areas.");
        maps[0].outputImage(DBA_STAR_DB_PATH + MAP_FILE_NAME + "_DBA.png", null, null);

        System.out.println("Exporting map with areas and centroids.");
        maps[0].computeCentroidMap().outputImage(DBA_STAR_DB_PATH + MAP_FILE_NAME + "_DBA_Centroid.png", null, null);

        SearchProblem tmpProb = new MapSearchProblem(maps[0]);
        GameDB gameDB = new GameDB(tmpProb);

        currentTime = System.currentTimeMillis();
        // ((SubgoalDBExact) database).computeIndex(tmpProb, rec);
        database.computeIndex(tmpProb, rec);
        rec.addStat(23, System.currentTimeMillis() - currentTime);

        System.out.println("Generating gameDB.");
        currentTime = System.currentTimeMillis();

        database = gameDB.computeDynamicDB(database, pathCompressAlgDba, rec, NUM_NEIGHBOUR_LEVELS);
        System.out.println("Time to compute DBAStar gameDB: " + (System.currentTimeMillis() - currentTime));

        database.init();

        database.exportDB(fileName);
        maps[0].computeComplexity(rec);
        dbStats[0].addRecord(rec);
        database.setProblem(problem);
        System.out.println("Verifying database.");
        database.verify(pathCompressAlgDba);
        System.out.println("Database verification complete.");
        System.out.println("Databases loaded.");
        dbaStarRecords = database.getSize();
    }
}
