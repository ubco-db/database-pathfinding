package search;

import database.SubgoalDB;
import database.SubgoalDynamicDB3;
import map.GameMap;
import org.junit.Test;
import util.DBAStarUtil;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class SearchUtilTest {
    @Test
    public void hillClimbingPathCompressionEqualAStarPath() {
        GameMap map = new GameMap("maps/dMap/012.map", 16);
        MapSearchProblem mapSearchProblem = new MapSearchProblem(map);

        AStar aStar = new AStar(mapSearchProblem);
        HillClimbing hc = new HillClimbing(mapSearchProblem, 10000);

        int startId = 3460;
        int goalId = 5826;

        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);

        StatsRecord stats = new StatsRecord();

        ArrayList<SearchState> aStarPath = aStar.computePath(start, goal, stats);

        int[] path = SubgoalDB.convertPathToIds(aStarPath);
        int[] compressedPath = new int[2000];
        SearchUtil.compressPath(path, hc, compressedPath, aStarPath.size());

        System.out.println("Path");
        System.out.println(Arrays.toString(path));
        System.out.println("Compressed Path");
        System.out.println(Arrays.toString(compressedPath));

//        System.out.println(Arrays.toString(path));
//        System.out.println(Arrays.toString(compressedPath));

        int[] tmp = new int[2000];
        int[] subgoals = SearchUtil.computeSubgoalsBinaryByIds(compressedPath, hc, tmp, aStarPath.size());
        System.out.println("Subgoals");
        System.out.println(Arrays.toString(subgoals));

        ArrayList<SearchState> fullPath = new ArrayList<>();
        ArrayList<SearchState> pathFragment;
        for (int j = 0; j < compressedPath.length - 1; j++) {
            int subgoal1 = compressedPath[j];
            int subgoal2 = compressedPath[j + 1];

            pathFragment = hc.computePath(new SearchState(subgoal1), new SearchState(subgoal2), stats);

            SearchUtil.mergePaths(fullPath, pathFragment);
        }

        System.out.println(aStarPath);
        System.out.println(fullPath);
    }

    @Test
    public void DBAPathBetween3460And5826EqualsAPath() throws Exception {
        final String DB_PATH = "dynamic/databases/";
        final String DBA_STAR_DB_PATH = DB_PATH + "checking_unequal_paths/";
        final String MAP_FILE_PATH = "maps/dMap/";
        final String MAP_FILE_NAME = "012.map";
        final String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;
        final int GRID_SIZE = 16;

        // Configure settings for the run
        DBAStarUtil dbaStarUtil = new DBAStarUtil( 1,  MAP_FILE_NAME, DBA_STAR_DB_PATH);

        // Load map
        GameMap startingMap = new GameMap(PATH_TO_MAP, GRID_SIZE);
        DBAStar dbaStarBW = dbaStarUtil.computeDBAStarDatabase(startingMap, "BW");

        int startId = 3460;
        int goalId = 5826;

        ArrayList<SearchState> dbaStarPath = dbaStarUtil.getDBAStarPath(startId, goalId, dbaStarBW);
        System.out.println(dbaStarPath);

        AStar aStar = new AStar(new MapSearchProblem(dbaStarBW.getMap()));

        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);
        ArrayList<SearchState> aStarPath = aStar.computePath(start, goal, new StatsRecord());

        System.out.println(aStarPath);

        System.out.println(aStarPath.equals(dbaStarPath));
        System.out.println(dbaStarPath.equals(aStarPath));
    }

    @Test
    public void allDBAPathsBetweenRepsEqualAPaths() throws Exception {
        final String DB_PATH = "dynamic/databases/";
        final String DBA_STAR_DB_PATH = DB_PATH + "checking_unequal_paths/";
        final String MAP_FILE_PATH = "maps/dMap/";
        final String MAP_FILE_NAME = "012.map";
        final String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;
        final int GRID_SIZE = 16;

        // Configure settings for the run
        DBAStarUtil dbaStarUtil = new DBAStarUtil( 1,  MAP_FILE_NAME, DBA_STAR_DB_PATH);

        // Load map
        GameMap startingMap = new GameMap(PATH_TO_MAP, GRID_SIZE);
        DBAStar dbaStarBW = dbaStarUtil.computeDBAStarDatabase(startingMap, "BW");

        AStar aStar = new AStar(new MapSearchProblem(dbaStarBW.getMap()));

        int[][][] paths = ((SubgoalDynamicDB3) dbaStarBW.getDatabase()).getPaths();
        int numNeighbours = ((SubgoalDynamicDB3) dbaStarBW.getDatabase()).getNumGroups();

        SearchState start, goal;
        int startId, goalId;
        for (int i = 0; i < numNeighbours; i++) {
            for (int[] path: paths[i]) {
                startId = path[0];
                goalId = path[path.length - 1];

                start = new SearchState(startId);
                goal = new SearchState(goalId);

//                System.out.println(start + " " + goal);

                ArrayList<SearchState> dbaStarPath = dbaStarUtil.getDBAStarPath(startId, goalId, dbaStarBW);
                ArrayList<SearchState> aStarPath = aStar.computePath(start, goal, new StatsRecord());

                System.out.println(dbaStarPath);
                System.out.println(aStarPath);

                assertEquals(dbaStarPath, aStarPath);
            }
        }
    }
}