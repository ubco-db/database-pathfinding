import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.AStar;
import search.DBAStar;
import search.SearchState;
import search.StatsRecord;
import util.DBAStarUtil;

import java.util.ArrayList;

public class ComparePathsDBAStarVsAStar {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "compare/";
    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;
    final static int GRID_SIZE = 16;

    private static final Logger logger = LogManager.getLogger(ComparePathsDBAStarVsAStar.class);

    public static void main(String[] args) throws Exception {
        GameMap startingMap = new GameMap(PATH_TO_MAP, GRID_SIZE);

        ArrayList<Integer> goalIds = new ArrayList<>();
        for (int i = 16; i < startingMap.rows; i++) {
            for (int j = 0; j < startingMap.cols; j++) {
                if (!startingMap.isWall(i, j)) {
                    goalIds.add(startingMap.getId(i, j));
                }
            }
        }

        // Print number of goals
        logger.info("Number of goals: " + goalIds.size());

        DBAStarUtil dbaStarUtil = new DBAStarUtil(1, MAP_FILE_NAME, DBA_STAR_DB_PATH);
        DBAStar dbaStar = dbaStarUtil.computeDBAStarDatabase(startingMap, "");
        AStar aStar = new AStar(dbaStar.getProblem());

        // Map is used to generate images
        GameMap map = dbaStar.getMap();

        double averageSuboptimalityGlobal = 0;
        double maxSuboptimalityGlobal = -1;
        double goalIdWithMaxSuboptimalityGlobal = -1;
        double startIdWithMaxSuboptimalityGlobal = -1;

        for (int goalId : goalIds) {
            double totalSuboptimalityLocal = 0;
            double maxSuboptimalityLocal = -1;
            int goalIdWithMaxSuboptimalityLocal = -1;
            int startIdWithMaxSuboptimalityLocal = -1;

            for (int startId : goalIds) {
                if (startId != goalId) {
                    StatsRecord dbaStats = new StatsRecord();
                    dbaStarUtil.recomputeWallAdditionNoLogging(goalId, dbaStar);
                    dbaStarUtil.recomputeWallRemovalNoLogging(goalId, dbaStar);
                    ArrayList<SearchState> path = dbaStar.computePath(new SearchState(startId), new SearchState(goalId), dbaStats);

                    StatsRecord aStarStats = new StatsRecord();
                    ArrayList<SearchState> optimalPath = aStar.computePath(new SearchState(startId), new SearchState(goalId), aStarStats);

                    double subOptimality = ((((double) dbaStats.getPathCost()) / aStarStats.getPathCost()) - 1) * 100.0;
                    totalSuboptimalityLocal += subOptimality;

                    if (maxSuboptimalityLocal < subOptimality) {
                        maxSuboptimalityLocal = subOptimality;
                        goalIdWithMaxSuboptimalityLocal = goalId;
                        startIdWithMaxSuboptimalityLocal = startId;
                    }

                    if (path == null || path.isEmpty()) {
                        logger.warn(String.format("No path was found between %d and %d!", startId, goalId));
                        // map.computeCentroidMap().outputImage(DBA_STAR_DB_PATH  + "path_" + startId + "_" + goalId + ".png", path, dbaStar.getSubgoals());
                        map.computeCentroidMap().outputImage(DBA_STAR_DB_PATH + "optimal_path_" + startId + "_" + goalId + ".png", optimalPath, dbaStar.getSubgoals());
                    }
                }
            }

            double averageSuboptimalityLocal = totalSuboptimalityLocal / (goalIds.size());
            logger.info("Average suboptimality: " + averageSuboptimalityLocal);
            logger.info("Max. suboptimality: " + maxSuboptimalityLocal + " for path from " + startIdWithMaxSuboptimalityLocal + " to " + goalIdWithMaxSuboptimalityLocal);

            if (maxSuboptimalityGlobal < maxSuboptimalityLocal) {
                maxSuboptimalityGlobal = maxSuboptimalityLocal;
                goalIdWithMaxSuboptimalityGlobal = goalIdWithMaxSuboptimalityLocal;
                startIdWithMaxSuboptimalityGlobal = startIdWithMaxSuboptimalityLocal;
            }

            averageSuboptimalityGlobal += averageSuboptimalityLocal;
        }

        logger.info("");
        logger.info("Average suboptimality whole map: " + averageSuboptimalityGlobal / (goalIds.size()));
        logger.info("Max. suboptimality whole map: " + maxSuboptimalityGlobal + " for path from " + startIdWithMaxSuboptimalityGlobal + " to " + goalIdWithMaxSuboptimalityGlobal);
    }
}