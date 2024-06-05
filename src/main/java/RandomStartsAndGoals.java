import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    final static String MAP_FILE_NAME = "012.map";
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

        final int NUM_PATHS = 1_000_000;

        logger.info("Map: " + MAP_FILE_NAME);
        logger.info("Grid size: " + GRID_SIZE);
        logger.info("Number of goals: " + NUM_PATHS);

        for (int s = 1; s < 10; s++) {
            logger.info("Searches per wall change: {}", s);

            for (int i = 0; i < 10; i++) {
                // Find 100_000 random start and 100_000 random goals
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

                    for (int k = 0; k < s; k++) {
                        dbaStar.computePath(randomStarts[j], randomGoals[j], new StatsRecord());
                    }
                }

                logger.info("Time taken for DBA* pathfinding (partial recomputation) with {} searches: {}", s, System.currentTimeMillis() - startTimeDBAStar);
            }
        }
    }
}
