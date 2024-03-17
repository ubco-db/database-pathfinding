import dynamic.Walls;
import map.GameMap;
import search.DBAStar3;
import search.SearchState;
import util.DBAStarUtil;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EvaluateWallAddition {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "checking_unequal_paths/";
    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;
    final static int GRID_SIZE = 16;

    private static final Logger logger = LogManager.getLogger(EvaluateWallAddition.class);

    public static void main(String[] args) throws Exception {
        // Set start and goal
        int startId = 13411;
        int goalId = 4339;

        long startTime, endTime, elapsedTime;

        // Configure settings for the run
        DBAStarUtil dbaStarUtil = new DBAStarUtil( 1,  MAP_FILE_NAME, DBA_STAR_DB_PATH);

        // Load map
        GameMap startingMap = new GameMap(PATH_TO_MAP, GRID_SIZE);

        // Build DBAStar Database with starting map and compute path on starting map
        logger.info("INITIAL COMPUTATION");
        startTime = System.currentTimeMillis();

        // Compute full database
        DBAStar3 dbaStarBW = dbaStarUtil.computeDBAStarDatabase(startingMap, "BW"); // BW = before wall
        dbaStarUtil.getDBAStarPath(startId, goalId, "BW", dbaStarBW);

        endTime = System.currentTimeMillis();

        elapsedTime = endTime - startTime;
        logger.info("Elapsed Time in milliseconds for full computation: " + elapsedTime + "\n");

        // Set wall
        ArrayList<SearchState> wallLocation = new ArrayList<>();
        int wallLoc = 4635;
        SearchState wall = new SearchState(wallLoc);
        wallLocation.add(wall);


        /* PARTIAL RECOMPUTATION */
        logger.info("PARTIAL RECOMPUTATION");
        startTime = System.currentTimeMillis();

        // Recompute database partially and compute path after partial recomputation
        dbaStarUtil.recomputeWallAddition(wallLoc, dbaStarBW);
        dbaStarUtil.getDBAStarPath(startId, goalId, "BW_Recomp", dbaStarBW);

        endTime = System.currentTimeMillis();

        elapsedTime = endTime - startTime;
        logger.info("Elapsed Time in milliseconds for partial recomputation: " + elapsedTime + "\n");


        /* FULL RECOMPUTATION */
        logger.info("FULL RECOMPUTATION");
        startTime = System.currentTimeMillis();

        // Recompute entire database to see if I matched it and compute path after full recomputation
        Walls.addWall(PATH_TO_MAP, wallLocation, startingMap);
        startingMap = new GameMap(PATH_TO_MAP, GRID_SIZE);
        DBAStar3 dbaStarAW = dbaStarUtil.computeDBAStarDatabase(startingMap, "AW"); // AW = after wall
        dbaStarUtil.getDBAStarPath(startId, goalId, "AW", dbaStarAW);

        endTime = System.currentTimeMillis();

        elapsedTime = endTime - startTime;
        logger.info("Elapsed Time in milliseconds for full recomputation: " + elapsedTime + "\n");

        // Remove wall
        Walls.removeWall(PATH_TO_MAP, wallLocation, startingMap);

        // Compare databases
//        try {
//            String f1Name = "BW012.map_DBA-STAR_G16_N1_C250.";
//            String f2Name = "AW012.map_DBA-STAR_G16_N1_C250.";
//            String ext = "dati2";
//            DBDiff.getDBDiff(DBA_STAR_DB_PATH, wallLoc, f1Name, f2Name, ext);
//            f1Name = "BW012.map_DBA-STAR_G16_N1_C250.";
//            f2Name = "AW012.map_DBA-STAR_G16_N1_C250.";
//            ext = "dat";
//            DBDiff.getDBDiff(DBA_STAR_DB_PATH, wallLoc, f1Name, f2Name, ext);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }
}
    
