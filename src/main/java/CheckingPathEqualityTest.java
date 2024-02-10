import dynamic.Walls;
import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.DBAStar;
import search.SearchState;
import util.DBAStarUtil;

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

    private static final Logger logger = LogManager.getLogger(CheckingPathEqualityTest.class);

    public static void main(String[] args) throws Exception {
        DBAStar dbaStar;
        GameMap startingMap = new GameMap(PATH_TO_MAP);

        // Initialize DBAStarUtil with settings for DBAStar run
        DBAStarUtil dbaStarUtil = new DBAStarUtil(16, 1, 250, MAP_FILE_NAME, DBA_STAR_DB_PATH);

        // Fix start
        int startId = 13411;

        ArrayList<Integer> goalIds = new ArrayList<>();

        int sectorNum = 12;
        // Start in sector 12 and add all goal ids (open spots, no wall):
        for (int i = 16; i < startingMap.rows; i++) {
            for (int j = 0; j < startingMap.cols; j++) {
                if (!startingMap.isWall(i, j) && (getSectorId(startingMap, startingMap.getId(i, j)) == sectorNum)) {
                    goalIds.add(startingMap.getId(i, j));
                }
            }
        }

        // Remove startId from list of goals
        goalIds.remove((Integer) startId);

        // Print number of goals
        logger.info("Number of goals: " + goalIds.size());
        logger.info("Goals in sector " + sectorNum + ": " + goalIds);

        // Compute DBAStar database before adding wall
        dbaStar = dbaStarUtil.computeDBAStarDatabase(startingMap, "BW");

        // Compute paths to all goals, store in HashMap of arrays (goal state as key)
        HashMap<Integer, ArrayList<SearchState>> paths = new HashMap<>();
        for (int goalId : goalIds) {
            paths.put(goalId, dbaStarUtil.getDBAStarPath(startId, goalId, dbaStar));
        }

        /* partial recomputation */

        long startTimePartial = System.currentTimeMillis();

        @SuppressWarnings("unchecked")
        ArrayList<SearchState>[][] pathsAfterPartialRecomputation = new ArrayList[goalIds.size()][goalIds.size()];
        int wallNumber = 0;
        for (int wallId : goalIds) {
            // Add wall & recompute database
            logger.info("\nRecompute wall addition: ");
            dbaStarUtil.recomputeWallAddition(wallId, dbaStar);

            int goalNum = 0;
            for (int goalId : goalIds) {
                if (goalId != wallId) {
                    System.out.println("Path from " + startId + " to " + goalId + ": ");
                    pathsAfterPartialRecomputation[wallNumber][goalNum] = dbaStarUtil.getDBAStarPath(startId, goalId, dbaStar);
                    System.out.println(pathsAfterPartialRecomputation[wallNumber][goalNum]);
                }
                goalNum++;
            }
            wallNumber++;

            // Remove wall
            logger.debug("\nRecompute wall removal: ");
            dbaStarUtil.recomputeWallRemoval(wallId, dbaStar);
        }

        writeResultToFile(DBA_STAR_DB_PATH + "pathResults.txt", "Total time partial recomputation: " + (System.currentTimeMillis() - startTimePartial) + "ms\n");

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
            Walls.addWall(PATH_TO_MAP, new ArrayList<SearchState>(), startingMap);
            startingMap = new GameMap(PATH_TO_MAP); // Resetting map
            dbaStar = dbaStarUtil.computeDBAStarDatabase(startingMap, "AW");

            int goalNum = 0;
            for (int goalId : goalIds) {
                if (goalId != wallId) {
                    System.out.println("Path from " + startId + " to " + goalId + ": ");
                    pathsAfterFullRecomputation[wallNumber][goalNum] = dbaStarUtil.getDBAStarPath(startId, goalId, dbaStar);
                    System.out.println(pathsAfterFullRecomputation[wallNumber][goalNum]);
                }
                goalNum++;
            }
            wallNumber++;

            // Remove wall
            Walls.removeWall(PATH_TO_MAP, wallLocation, startingMap);
        }

        writeResultToFile(DBA_STAR_DB_PATH + "pathResults.txt", "Total time complete recomputation: " + (System.currentTimeMillis() - startTimeFull) + "ms\n");

        for (int i = 0; i < pathsAfterFullRecomputation.length; i++) {
            for (int j = 0; j < pathsAfterFullRecomputation[i].length; j++) {
                if (i != j) {
                    boolean equal = isPathEqual(pathsAfterFullRecomputation[i][j], pathsAfterPartialRecomputation[i][j]);
                    if (!equal) {
                        // TODO: actually print id of wall here
                        System.out.println("\nERROR! Paths to " + goalIds.get(j) + " for wall at " + goalIds.get(i) + " not equal.\n");
                        System.out.println("Path after full recomp: " + pathsAfterFullRecomputation[i][j]);
                        System.out.println("Path after partial recomp: " + pathsAfterPartialRecomputation[i][j]);
                        System.out.println();
                    }
                }
            }
        }
    }
}
