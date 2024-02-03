import dynamic.Walls;
import map.GameMap;
import search.DBAStar;
import search.SearchState;
import util.DBAStarUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class AddingAllWallsTest {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "test/";
    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;

    // TODO: add path comparison logic to ensure paths are identical
    public static void main(String[] args) throws Exception {
        DBAStar dbaStar;
        GameMap startingMap = new GameMap(PATH_TO_MAP);

        // Initialize DBAStarUtil with settings for DBAStar run
        DBAStarUtil dbaStarUtil = new DBAStarUtil(16, 1, 250, MAP_FILE_NAME, DBA_STAR_DB_PATH);

        // fix start
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

        int sectorNum = 12;
        // start in sector 12 and add all goal ids:
//        for (int i = 16; i < startingMap.rows; i++) {
//            for (int j = 0; j < startingMap.cols; j++) {
//                if (!startingMap.isWall(i, j) && (getSectorId(startingMap, startingMap.getId(i, j)) == sectorNum)) {
//                    goalIds.add(startingMap.getId(i, j));
//                }
//            }
//        }

        // remove startId from list of goals
        goalIds.remove((Integer) startId);

        // print number of goals (6175 on 012.map)
        System.out.println("Number of goals: " + goalIds.size());
        System.out.println("Goals in sector " + sectorNum + ": " + goalIds);

        // compute DBAStar database before adding wall
        System.out.println();
        dbaStar = dbaStarUtil.computeDBAStarDatabase(startingMap, "BW");

        // compute paths to all goals, store in HashMap of arrays (goal state as key)
        HashMap<Integer, ArrayList<SearchState>> paths = new HashMap<>();
        for (int goalId : goalIds) {
            paths.put(goalId, dbaStarUtil.getDBAStarPath(startId, goalId, dbaStar));
        }

        /* partial recomputation */

        long elapsedTimePartialRecomputation = 0;

        long totalTimeStart = System.currentTimeMillis();

        @SuppressWarnings("unchecked")
        ArrayList<SearchState>[][] pathsAfterPartialRecomputation = new ArrayList[goalIds.size()][goalIds.size()];
        int wallNumber = 0;
        for (int wallId : goalIds) {
            // TODO: Remove wall after this to ensure I can reuse map, database, etc.

            long startTimePartialRecomputation = System.currentTimeMillis();
            // add wall & recompute database
            System.out.println("\nRecompute wall addition: ");
            dbaStarUtil.recomputeWallAddition(wallId, dbaStar);
            long endTimePartialRecomputation = System.currentTimeMillis();

            elapsedTimePartialRecomputation += endTimePartialRecomputation - startTimePartialRecomputation;

            // TODO: store paths somewhere for comparison (2D array?)
//            int goalNum = 0;
//            for (int goalId : goalIds) {
//                if (goalId != wallId) {
//                    System.out.println("Path from " + startId + " to " + goalId + ": ");
//                    pathsAfterPartialRecomputation[wallNumber][goalNum] = getDBAStarPath(startId, goalId, dbaStar);
//                    System.out.println(pathsAfterPartialRecomputation[wallNumber][goalNum]);
//                }
//                goalNum++;
//            }

            wallNumber++;

            // remove wall
            System.out.println("\nRecompute wall removal: ");
            dbaStarUtil.recomputeWallRemoval(wallId, dbaStar);
        }

        writeResultToFile(DBA_STAR_DB_PATH + "results.txt", "Total time partial recomputation for sector " + sectorNum + ": " + (System.currentTimeMillis() - totalTimeStart) + "ms\n");

        /* complete recomputation */

        long elapsedTimeCompleteRecomputation = 0;

        @SuppressWarnings("unchecked")
        ArrayList<SearchState>[][] pathsAfterFullRecomputation = new ArrayList[goalIds.size()][goalIds.size()];
        wallNumber = 0;
        for (int wallId : goalIds) {
            long startTimeCompleteRecomputation = System.currentTimeMillis();

            // setting up walls
            ArrayList<SearchState> wallLocation = new ArrayList<>();
            SearchState wall = new SearchState(wallId);
            wallLocation.add(wall); // adding wall for each open state
            Walls.addWall(PATH_TO_MAP, wallLocation, startingMap);

            startingMap = new GameMap(PATH_TO_MAP); // resetting map

            // computing database
            dbaStar = dbaStarUtil.computeDBAStarDatabase(startingMap, "AW");

            long endTimeCompleteRecomputation = System.currentTimeMillis();

            elapsedTimeCompleteRecomputation += endTimeCompleteRecomputation - startTimeCompleteRecomputation;

            Walls.removeWall(PATH_TO_MAP, wallLocation, startingMap);

//            int goalNum = 0;
//            for (int goalId : goalIds) {
//                if (goalId != wallId) {
//                    System.out.println("Path from " + startId + " to " + goalId + ": ");
//                    pathsAfterFullRecomputation[wallNumber][goalNum] = getDBAStarPath(startId, goalId, dbaStar);
//                    System.out.println(pathsAfterFullRecomputation[wallNumber][goalNum]);
//                }
//                goalNum++;
//            }

            wallNumber++;
        }

        writeResultToFile(DBA_STAR_DB_PATH + "results.txt", "Total time  complete recomputation for sector " + sectorNum + ": " + (System.currentTimeMillis() - totalTimeStart) + "ms\n");

//        for (int i = 0; i < pathsAfterFullRecomputation.length; i++) {
//            for (int j = 0; j < pathsAfterFullRecomputation[i].length; j++) {
//                if (i != j) {
//                    boolean equal = isPathEqual(pathsAfterFullRecomputation[i][j], pathsAfterPartialRecomputation[i][j]);
//                    if (!equal) {
//                        // TODO: actually print id of wall here
//                        System.out.println("\nERROR! Paths to " + goalIds.get(j) + " for wall at " + goalIds.get(i) + " not equal.\n");
//                        System.out.println("Path after full recomp: " + pathsAfterFullRecomputation[i][j]);
//                        System.out.println("Path after partial recomp: " + pathsAfterPartialRecomputation[i][j]);
//                        System.out.println();
//                    }
//                }
//            }
//        }
    }

    private static void writeResultToFile(String filePath, String result) {
        try {
            // Create a FileWriter object to write to the file
            FileWriter fileWriter = new FileWriter(filePath, true);

            // Wrap the FileWriter with BufferedWriter for efficient writing
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            // Write the number to the file
            bufferedWriter.write(result);

            // Close the BufferedWriter to flush and close the underlying FileWriter
            bufferedWriter.close();

            System.out.println("Result has been written to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
