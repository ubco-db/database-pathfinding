import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.AStar;
import search.DBAStar;
import search.SearchState;
import search.StatsRecord;
import util.DBAStarUtil;

import java.util.ArrayList;
import java.util.Random;

public class RandomStartsAndGoals {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "checking_unequal_paths/";
    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "701.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;
    final static int GRID_SIZE = 16;

    private static final Logger logger = LogManager.getLogger(RandomStartsAndGoals.class);

    public static void main(String[] args) throws Exception {
        GameMap startingMap = new GameMap(PATH_TO_MAP, GRID_SIZE);

        // Get all open states on the map
        ArrayList<Integer> openStates = new ArrayList<>();
        for (int i = 0; i < startingMap.rows; i++) {
            for (int j = 0; j < startingMap.cols; j++) {
                if (!startingMap.isWall(i, j)) {
                    openStates.add(startingMap.getId(i, j));
                }
            }
        }

        logger.info("Number of open states: " + openStates.size());

        int NUM_PATHS = 38_142_976;

        for (int i = 0; i < 10; i++) {
            // Find 100_000 random start and 1_000_000 random goals
            SearchState[] randomStarts = new SearchState[NUM_PATHS];
            SearchState[] randomGoals = new SearchState[NUM_PATHS];
            Random random = new Random();
            for (int j = 0; j < NUM_PATHS; j++) {
                randomStarts[j] = new SearchState(openStates.get(random.nextInt(openStates.size())));
                randomGoals[j] = new SearchState(openStates.get(random.nextInt(openStates.size())));
            }

            DBAStarUtil dbaStarUtil = new DBAStarUtil(1, MAP_FILE_NAME, DBA_STAR_DB_PATH);
            DBAStar dbaStar = dbaStarUtil.computeDBAStarDatabase(startingMap, "");
            long startTimeDBAStar = System.currentTimeMillis();
            for (int j = 0; j < NUM_PATHS; j++) {
                dbaStarUtil.recomputeWallAdditionNoLogging(randomGoals[j].id, dbaStar);
                dbaStarUtil.recomputeWallRemovalNoLogging(randomGoals[j].id, dbaStar);
                ArrayList<SearchState> path = dbaStar.computePath(randomStarts[j], randomGoals[j], new StatsRecord());
                if (path == null) {
                    System.out.println(randomStarts[j].id);
                    System.out.println(randomGoals[j].id);
                    System.out.println("ERROR");
                }
            }
            logger.info("Time taken for DBAStar pathfinding: " + (System.currentTimeMillis() - startTimeDBAStar));

            AStar aStar = new AStar(dbaStar.getProblem());
            long startTimeAStar = System.currentTimeMillis();
            for (int j = 0; j < NUM_PATHS; j++) {
                aStar.computePath(randomStarts[j], randomGoals[j], new StatsRecord());
            }
            logger.info("Time taken for AStar pathfinding: " + (System.currentTimeMillis() - startTimeAStar));
        }
    }
}
