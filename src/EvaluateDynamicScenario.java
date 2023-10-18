import database.DBStats;
import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDynamicDB2;
import dynamic.Walls;
import map.GameMap;
import search.*;
import comparison.DBDiff;

import java.io.IOException;
import java.util.ArrayList;

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
        ArrayList<SearchState> wallLocation = new ArrayList<>();

        // group of walls in where four regions meet
        wallLocation.add(new SearchState(6157));

        // build DBAStar Database
        GameMap map = new GameMap(PATH_TO_MAP);
        computeDBAStarDatabase(map, "BW"); // BW = before wall

        // add wall
        Walls.addWall(PATH_TO_MAP, wallLocation, map);
        map = new GameMap(PATH_TO_MAP);

        // recompute database
        computeDBAStarDatabase(map, "AW"); // AW = after wall

        // remove wall
        Walls.removeWall(PATH_TO_MAP, wallLocation, map);

        // compare databases
        try {
            String f1Name = "BW012.map_DBA-STAR_G16_N1_C250.";
            String f2Name = "AW012.map_DBA-STAR_G16_N1_C250.";
            String ext = "dati2";
            DBDiff.getDBDiff(DBA_STAR_DB_PATH, f1Name, f2Name, ext);
            f1Name = "BW012.map_DBA-STAR_G16_N1_C250.";
            f2Name = "AW012.map_DBA-STAR_G16_N1_C250.";
            ext = "dat";
            DBDiff.getDBDiff(DBA_STAR_DB_PATH, f1Name, f2Name, ext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void computeDBAStarDatabase(GameMap map, String wallStatus) {
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

        DBAStar dbaStar = new DBAStar(problem, map, database);
        ArrayList<SearchState> path = dbaStar.computePath(new SearchState(13411), new SearchState(6009), new StatsRecord());
        map.computeCentroidMap().outputImage(DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME + "_path.png", path, null);
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
