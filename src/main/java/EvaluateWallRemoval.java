import dynamic.Walls;
import map.GameMap;
import search.DBAStar3;
import search.SearchState;
import util.DBAStarUtil2;

import java.util.ArrayList;

public class EvaluateWallRemoval {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "removal/";

    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;

    public static void main(String[] args) throws Exception {
        // Set start and goal
        int startId = 15362;
        int goalId = 11671;

        // Configure settings for the run
        DBAStarUtil2 dbaStarUtil2 = new DBAStarUtil2(16, 1,  MAP_FILE_NAME, DBA_STAR_DB_PATH);

        // Load map
        GameMap startingMap = new GameMap(PATH_TO_MAP);

        // Build DBAStar Database with starting map and compute path on starting map
        DBAStar3 dbaStarBW = dbaStarUtil2.computeDBAStarDatabase(startingMap, "BW"); // BW = before wall
        dbaStarUtil2.getDBAStarPath(startId, goalId, "BW", dbaStarBW);

        // Set wall
        ArrayList<SearchState> wallLocation = new ArrayList<>();
        int wallLoc = 2577;
        SearchState wall = new SearchState(wallLoc);
        wallLocation.add(wall);

        long startTime, endTime, elapsedTime;


        /* PARTIAL RECOMPUTATION */
        System.out.println();

        startTime = System.currentTimeMillis();

        // Recompute database partially and compute path after partial recomputation
        dbaStarUtil2.recomputeWallRemoval(wallLoc, dbaStarBW);
        dbaStarUtil2.getDBAStarPath(startId, goalId, "BW_Recomp", dbaStarBW);

        endTime = System.currentTimeMillis();

        elapsedTime = endTime - startTime;
        System.out.println("Elapsed Time in milliseconds for partial recomputation: " + elapsedTime);


        /* FULL RECOMPUTATION */
        System.out.println();

        startTime = System.currentTimeMillis();

        // Recompute entire database to see if I matched it and compute path after full recomputation
        Walls.removeWall(PATH_TO_MAP, wallLocation, startingMap);
        startingMap = new GameMap(PATH_TO_MAP);
        DBAStar3 dbaStarRW = dbaStarUtil2.computeDBAStarDatabase(startingMap, "RW"); // RW = removed wall
        dbaStarUtil2.getDBAStarPath(startId, goalId, "RW", dbaStarRW);

        endTime = System.currentTimeMillis();

        elapsedTime = endTime - startTime;
        System.out.println("Elapsed Time in milliseconds for full recomputation: " + elapsedTime);

        // Add wall back in
        Walls.addWall(PATH_TO_MAP, wallLocation, startingMap);
    }
}
