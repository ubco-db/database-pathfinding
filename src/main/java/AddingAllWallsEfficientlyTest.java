import map.GameMap;
import search.DBAStar3;
import util.DBAStarUtil2;

import java.util.ArrayList;

import static util.FileWritingUtil.writeResultToFile;

public class AddingAllWallsEfficientlyTest {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "test/";
    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;

    // private static final Logger logger = LogManager.getLogger(AddingAllWallsEfficientlyTest.class);

    public static void main(String[] args) throws Exception {
        DBAStar3 dbaStar3;
        GameMap startingMap = new GameMap(PATH_TO_MAP);

        // Initialize DBAStarUtil2 with settings for DBAStar run
        DBAStarUtil2 dbaStarUtil2 = new DBAStarUtil2(16, 1, MAP_FILE_NAME, DBA_STAR_DB_PATH);

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
        // logger.info("Number of goals: " + goalIds.size());

        /* partial recomputation */

        long startTimePartial = System.currentTimeMillis();

        // compute DBAStar database before adding wall
        dbaStar3 = dbaStarUtil2.computeDBAStarDatabase(startingMap, "BW"); // 70ms

        for (int wallId : goalIds) {
            // Add wall & recompute database
            // logger.info("\n\nRecompute wall addition for wall at: " + wallId);
            dbaStarUtil2.recomputeWallAdditionUsingSubgoalDynamicDB3NoLogging(wallId, dbaStar3); // 790ms

            // Remove wall
            // logger.info("\n\nRecompute wall removal for wall at: " + wallId);
            dbaStarUtil2.recomputeWallRemovalUsingSubgoalDynamicDB3NoLogging(wallId, dbaStar3); // 630ms
        }

        writeResultToFile(DBA_STAR_DB_PATH + "results.txt", "Total time partial recomputation: " + (System.currentTimeMillis() - startTimePartial) + "ms\n");

        /* complete recomputation */

        startingMap = new GameMap(PATH_TO_MAP); // resetting map

        long startTimeFull = System.currentTimeMillis();

        for (int wallId : goalIds) {
            int wallRow = startingMap.getRow(wallId);
            int wallCol = startingMap.getCol(wallId);

            // Add wall
            startingMap.squares[wallRow][wallCol] = GameMap.WALL_CHAR;

            // Compute database
            dbaStarUtil2.computeDBAStarDatabase(startingMap, "AW");

            // Remove wall
            startingMap.squares[wallRow][wallCol] = GameMap.EMPTY_CHAR;
        }

        writeResultToFile(DBA_STAR_DB_PATH + "results.txt", "Total time complete recomputation: " + (System.currentTimeMillis() - startTimeFull) + "ms\n");
    }
}
