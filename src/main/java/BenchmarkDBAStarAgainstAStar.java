import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.AStar;
import search.DBAStar;
import search.SearchState;
import search.StatsRecord;
import util.DBAStarUtil;

import java.util.ArrayList;

/**
 * Basic benchmarking class, runs DBAStar pathfinding and AStar pathfinding for all goals with fixed start
 */
public class BenchmarkDBAStarAgainstAStar {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "benchmark/";
    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;
    final static int GRID_SIZE = 16;

    private static final Logger logger = LogManager.getLogger(BenchmarkDBAStarAgainstAStar.class);
    public static void main(String[] args) throws Exception {
        // Fix start
        int startId = 13411;

        GameMap startingMap = new GameMap(PATH_TO_MAP, GRID_SIZE);

        ArrayList<Integer> goalIds = new ArrayList<>();
        for (int i = 16; i < startingMap.rows; i++) {
            for (int j = 0; j < startingMap.cols; j++) {
                if (!startingMap.isWall(i, j)) {
                    goalIds.add(startingMap.getId(i, j));
                }
            }
        }

        // Remove startStateId from list of goals
        goalIds.remove((Integer) startId);

        // Print number of goals
        logger.info("Number of goals: " + goalIds.size());

        DBAStarUtil dbaStarUtil = new DBAStarUtil(1, MAP_FILE_NAME, DBA_STAR_DB_PATH);
        DBAStar dbaStar = dbaStarUtil.computeDBAStarDatabase(startingMap, "");

        long startTimeDBAStar = System.currentTimeMillis();
        for (int goalId: goalIds) {
            StatsRecord dbaStats = new StatsRecord();
            dbaStar.computePath(new SearchState(startId), new SearchState(goalId), dbaStats);
        }
        logger.info("Time taken for DBAStar pathfinding: " + (System.currentTimeMillis() - startTimeDBAStar));

        AStar aStar = new AStar(dbaStar.getProblem());

        long startTimeAStar = System.currentTimeMillis();
        for (int goalId: goalIds) {
            StatsRecord aStarStats = new StatsRecord();
            aStar.computePath(new SearchState(startId), new SearchState(goalId), aStarStats);
        }
        logger.info("Time taken for AStar pathfinding: " + (System.currentTimeMillis() - startTimeAStar));
    }
}
