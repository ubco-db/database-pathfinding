import map.GameMap;
import search.AStar;
import search.DBAStar;
import search.SearchState;
import search.StatsRecord;
import util.DBAStarUtil;

import java.util.ArrayList;
import java.util.Random;

public class RandomStartsAndGoals {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "benchmark/";
    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;
    final static int GRID_SIZE = 16;
    public static void main(String[] args) throws Exception {
        // get all open states on the map
        // pick a random open state as the start, and a second, random open state as the goal
        // run DBA*
        // run A*

        // Get map
        GameMap startingMap = new GameMap(PATH_TO_MAP, GRID_SIZE);

        ArrayList<Integer> openStates = new ArrayList<>();
        for (int i = 0; i < startingMap.rows; i++) {
            for (int j = 0; j < startingMap.cols; j++) {
                if (!startingMap.isWall(i, j)) {
                    openStates.add(startingMap.getId(i, j));
                }
            }
        }

        Random rand = new Random();
        DBAStarUtil dbaStarUtil = new DBAStarUtil(1, MAP_FILE_NAME, DBA_STAR_DB_PATH);

        for (int i = 0; i < 100_000; i++) {
            SearchState start = new SearchState(rand.nextInt(openStates.size()));
            SearchState goal = new SearchState(rand.nextInt(openStates.size()));

            DBAStar dbaStar = dbaStarUtil.computeDBAStarDatabase(startingMap, "");

            dbaStar.computePath(start, goal, new StatsRecord());

            AStar aStar = new AStar(dbaStar.getProblem());

            aStar.computePath(start, goal, new StatsRecord());
        }
    }
}
