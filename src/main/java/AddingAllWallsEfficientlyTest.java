import dynamic.Walls;
import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.DBAStar;
import search.SearchState;
import util.DBAStarUtil;

import java.util.ArrayList;

import static util.FileWritingUtil.writeResultToFile;

public class AddingAllWallsEfficientlyTest {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "test/";
    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;

    private static final Logger logger = LogManager.getLogger(AddingAllWallsEfficientlyTest.class);

    public static void main(String[] args) throws Exception {
        DBAStar dbaStar;
        GameMap startingMap = new GameMap(PATH_TO_MAP);

        // Initialize DBAStarUtil with settings for DBAStar run
        DBAStarUtil dbaStarUtil = new DBAStarUtil(16, 1, 250, MAP_FILE_NAME, DBA_STAR_DB_PATH);

        // Fix start
        int startId = 13411;

        ArrayList<Integer> goalIds = new ArrayList<>();
        // iterate over all goals (open spots no wall)
        for (int i = 16; i < startingMap.rows; i++) {
            for (int j = 0; j < startingMap.cols; j++) {
                if (!startingMap.isWall(i, j)) {
                    goalIds.add(startingMap.getId(i, j));
                }
            }
        }

        // Remove startId from list of goals
        goalIds.remove((Integer) startId);

        // Print number of goals (6175 on 012.map)
        logger.info("Number of goals: " + goalIds.size());

        // compute DBAStar database before adding wall
        dbaStar = dbaStarUtil.computeDBAStarDatabaseUsingSubgoalDynamicDB3(startingMap, "BW");

        /* partial recomputation */

        long startTimePartial = System.currentTimeMillis();

        for (int wallId : goalIds) {
            // Add wall & recompute database
            logger.debug("\nRecompute wall addition: ");
            dbaStarUtil.recomputeWallAdditionUsingSubgoalDynamicDB3(wallId, dbaStar);

            // Remove wall
            logger.debug("\nRecompute wall removal: ");
            dbaStarUtil.recomputeWallRemovalUsingSubgoalDynamicDB3(wallId, dbaStar);
        }

        writeResultToFile(DBA_STAR_DB_PATH + "figureOutWhereTimeGoes.txt", "Total time partial recomputation: " + (System.currentTimeMillis() - startTimePartial) + "ms\n");

        /* complete recomputation */

        long startTimeFull = System.currentTimeMillis();

        for (int wallId : goalIds) {
            // Setting up walls
            ArrayList<SearchState> wallLocation = new ArrayList<>();
            wallLocation.add(new SearchState(wallId));

            // Adding wall and computing database
            Walls.addWall(PATH_TO_MAP, wallLocation, startingMap);
            startingMap = new GameMap(PATH_TO_MAP); // resetting map
            dbaStarUtil.computeDBAStarDatabaseUsingSubgoalDynamicDB3(startingMap, "AW");

            Walls.removeWall(PATH_TO_MAP, wallLocation, startingMap);
        }

        writeResultToFile(DBA_STAR_DB_PATH + "results.txt", "Total time complete recomputation: " + (System.currentTimeMillis() - startTimeFull) + "ms\n");
    }
}
