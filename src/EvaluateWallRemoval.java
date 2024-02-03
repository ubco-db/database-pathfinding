import database.SubgoalDynamicDB2;
import dynamic.Walls;
import map.GameMap;
import search.DBAStar;
import search.MapSearchProblem;
import search.SearchState;
import util.DBAStarUtil;

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
        DBAStarUtil dbaStarUtil = new DBAStarUtil(16, 1, 250, MAP_FILE_NAME, DBA_STAR_DB_PATH);

        // Load map
        GameMap startingMap = new GameMap(PATH_TO_MAP);

        // Build DBAStar Database with starting map and compute path on starting map
        DBAStar dbaStarBW = dbaStarUtil.computeDBAStar(startingMap, "BW"); // BW = before wall
        dbaStarUtil.getDBAStarPath(startId, goalId, "BW", dbaStarBW);

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
        dbaStarUtil.recomputeWallRemoval(wallLoc, dbaStarBW.getMap(), (MapSearchProblem) dbaStarBW.getProblem(), (SubgoalDynamicDB2) dbaStarBW.getDatabase());
        dbaStarUtil.getDBAStarPath(startId, goalId, "BW_Recomp", dbaStarBW);

        endTime = System.currentTimeMillis();

        elapsedTime = endTime - startTime;
        System.out.println("Elapsed Time in milliseconds for partial recomputation: " + elapsedTime);


        /* FULL RECOMPUTATION */
        System.out.println();

        startTime = System.currentTimeMillis();

        // Recompute entire database to see if I matched it and compute path after full recomputation
        Walls.removeWall(PATH_TO_MAP, wallLocation, startingMap);
        startingMap = new GameMap(PATH_TO_MAP);
        DBAStar dbaStarRW = dbaStarUtil.computeDBAStar(startingMap, "RW"); // RW = removed wall
        dbaStarUtil.getDBAStarPath(startId, goalId, "RW", dbaStarRW);

        endTime = System.currentTimeMillis();

        elapsedTime = endTime - startTime;
        System.out.println("Elapsed Time in milliseconds for full recomputation: " + elapsedTime);

        // Add wall back in
        Walls.addWall(PATH_TO_MAP, wallLocation, startingMap);
    }
}
