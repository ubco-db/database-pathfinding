import dynamic.Walls;
import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.DBAStar;
import search.DBAStar3;
import search.SearchState;
import util.DBAStarUtil;
import util.DBAStarUtil2;

import java.util.ArrayList;
import java.util.HashMap;

import static util.FileWritingUtil.writeResultToFile;
import static util.MapHelpers.getSectorId;
import static util.MapHelpers.isPathEqual;

public class CheckingPathEqualityTest {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "test/";
    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;
    final static int GRID_SIZE = 16;

    private static final Logger logger = LogManager.getLogger(CheckingPathEqualityTest.class);

    public static void main(String[] args) throws Exception {
        DBAStar3 dbaStar3;
        GameMap startingMap = new GameMap(PATH_TO_MAP);

        // Initialize DBAStarUtil with settings for DBAStar run
        DBAStarUtil2 dbaStarUtil2 = new DBAStarUtil2(GRID_SIZE, 1, MAP_FILE_NAME, DBA_STAR_DB_PATH);

        // Fix start
        int startStateId = 13411;

        ArrayList<Integer> goalIds = new ArrayList<>();

        int sectorNum = 12;
        // Start in sector 12 and add all goal ids (open spots, no wall):
        for (int i = 16; i < startingMap.rows; i++) {
            for (int j = 0; j < startingMap.cols; j++) {
                if (!startingMap.isWall(i, j) && (getSectorId(startingMap, startingMap.getId(i, j), GRID_SIZE) == sectorNum)) {
                    goalIds.add(startingMap.getId(i, j));
                }
            }
        }

        // Remove startStateId from list of goals
        goalIds.remove((Integer) startStateId);

        // Print number of goals
        logger.info("Number of goals: " + goalIds.size());
        logger.info("Goals in sector " + sectorNum + ": " + goalIds);

        // Compute DBAStar database before adding wall
        dbaStar3 = dbaStarUtil2.computeDBAStarDatabaseUsingSubgoalDynamicDB3(startingMap, "BW");

        // Compute paths to all goals, store in HashMap of arrays (goal state as key)
//        HashMap<Integer, ArrayList<SearchState>> paths = new HashMap<>();
//        for (int goalId : goalIds) {
//            paths.put(goalId, dbaStarUtil2.getDBAStarPath(startStateId, goalId, dbaStar3));
//        }

        /* partial recomputation */

        long startTimePartial = System.currentTimeMillis();

        @SuppressWarnings("unchecked")
        ArrayList<SearchState>[][] pathsAfterPartialRecomputation = new ArrayList[goalIds.size()][goalIds.size()];
        int wallNumber = 0;
        for (int wallId : goalIds) {
            // Add wall & recompute database
            logger.info("\nRecompute wall addition for: " + wallId);
            dbaStarUtil2.recomputeWallAdditionUsingSubgoalDynamicDB3(wallId, dbaStar3);

            int goalNum = 0;
            for (int goalStateId : goalIds) {
                if (goalStateId != wallId) {
                    System.out.println("Path from " + startStateId + " to " + goalStateId + ": ");
                    pathsAfterPartialRecomputation[wallNumber][goalNum] = dbaStarUtil2.getDBAStarPath(startStateId, goalStateId, dbaStar3);
                    dbaStarUtil2.getDBAStarPath(startStateId, goalStateId, "BW_Recomp", dbaStar3);
                    System.out.println(pathsAfterPartialRecomputation[wallNumber][goalNum]);
                }
                goalNum++;
            }
            wallNumber++;

            // Remove wall
            logger.debug("\nRecompute wall removal for: " + wallId);
            dbaStarUtil2.recomputeWallRemovalUsingSubgoalDynamicDB3(wallId, dbaStar3);
        }

        writeResultToFile(DBA_STAR_DB_PATH + "results.txt", "Total time partial recomputation: " + (System.currentTimeMillis() - startTimePartial) + "ms\n");

        /* Full recomputation */

        long startTimeFull = System.currentTimeMillis();

        @SuppressWarnings("unchecked")
        ArrayList<SearchState>[][] pathsAfterFullRecomputation = new ArrayList[goalIds.size()][goalIds.size()];
        wallNumber = 0;
        for (int wallId : goalIds) {
            // Setting up wall
            ArrayList<SearchState> wallLocation = new ArrayList<>();
            wallLocation.add(new SearchState(wallId));

            // Adding wall and computing database
            Walls.addWall(PATH_TO_MAP, wallLocation, startingMap);
            startingMap = new GameMap(PATH_TO_MAP); // Resetting map
            dbaStar3 = dbaStarUtil2.computeDBAStarDatabaseUsingSubgoalDynamicDB3(startingMap, "AW");

            int goalNum = 0;
            for (int goalStateId : goalIds) {
                if (goalStateId != wallId) {
                    System.out.println("Path from " + startStateId + " to " + goalStateId + ": ");
                    pathsAfterFullRecomputation[wallNumber][goalNum] = dbaStarUtil2.getDBAStarPath(startStateId, goalStateId, dbaStar3);
                    dbaStarUtil2.getDBAStarPath(startStateId, goalStateId, "AW", dbaStar3);
                    System.out.println(pathsAfterFullRecomputation[wallNumber][goalNum]);
                }
                goalNum++;
            }
            wallNumber++;

            // Remove wall
            Walls.removeWall(PATH_TO_MAP, wallLocation, startingMap);
        }

        writeResultToFile(DBA_STAR_DB_PATH + "results.txt", "Total time complete recomputation: " + (System.currentTimeMillis() - startTimeFull) + "ms\n");

        for (int i = 0; i < pathsAfterFullRecomputation.length; i++) {
            for (int j = 0; j < pathsAfterFullRecomputation[i].length; j++) {
                if (i != j) {
                    boolean equal = isPathEqual(pathsAfterFullRecomputation[i][j], pathsAfterPartialRecomputation[i][j]);
                    if (!equal) {
                        // TODO: actually print id of wall here
                        logger.error("\nERROR! Paths to " + goalIds.get(j) + " for wall at " + goalIds.get(i) + " are not equal.\n");
                        logger.info("Path after full recomp: " + pathsAfterFullRecomputation[i][j]);
                        logger.info("Path after partial recomp: " + pathsAfterPartialRecomputation[i][j]);
                        logger.info("");
                    } else {
                        logger.info("Paths to " + goalIds.get(j) + " for wall at " + goalIds.get(i) + " are equal.\n");
                    }
                }
            }
        }
    }
}
