import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.AStar;
import search.DBAStar;
import search.SearchState;
import search.StatsRecord;
import util.DBAStarUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        double maxSuboptimality = -1;
        BigDecimal averageSuboptimality = new BigDecimal(0);

        int goalIdWithMaxSuboptimality = -1;
        int startIdWithMaxSuboptimality = -1;

        for (int goalId : goalIds) {
            for (int startId: goalIds) {
//            dbaStarUtil.recomputeWallAdditionNoLogging(goalId, dbaStar);
//            dbaStarUtil.recomputeWallRemovalNoLogging(goalId, dbaStar);

                StatsRecord dbaStats = new StatsRecord();
                dbaStarUtil.recomputeWallAdditionNoChecks(goalId, dbaStar);
                dbaStarUtil.recomputeWallRemovalNoChecks(goalId, dbaStar);
                ArrayList<SearchState> path = dbaStar.computePath(new SearchState(startId), new SearchState(goalId), dbaStats);

                StatsRecord aStarStats = new StatsRecord();
                ArrayList<SearchState> optimalPath = aStar.computePath(new SearchState(startId), new SearchState(goalId), aStarStats);
                logger.info("AStar path cost: " + aStarStats.getPathCost() + " DBAStar path cost: " + dbaStats.getPathCost());

                double subOptimality = ((((double) dbaStats.getPathCost()) / aStarStats.getPathCost()) - 1) * 100.0;
                logger.info("Suboptimality: " + subOptimality);
                averageSuboptimality = averageSuboptimality.add(BigDecimal.valueOf(subOptimality));

                if (maxSuboptimality < subOptimality) {
                    maxSuboptimality = subOptimality;
                    goalIdWithMaxSuboptimality = goalId;
                    startIdWithMaxSuboptimality = startId;
                }

                if (path == null || path.isEmpty()) {
                    logger.warn(String.format("No path was found between %d and %d!", startId, goalId));
                    // map.computeCentroidMap().outputImage(DBA_STAR_DB_PATH  + "path_" + startId + "_" + goalId + ".png", path, dbaStar.getSubgoals());
                    map.computeCentroidMap().outputImage(DBA_STAR_DB_PATH + "optimal_path_" + startId + "_" + goalId + ".png", optimalPath, dbaStar.getSubgoals());
                }
            }
        }

        logger.info("Average suboptimality: " + averageSuboptimality.divide(BigDecimal.valueOf(goalIds.size()),5, RoundingMode.HALF_UP));
        logger.info("Max. suboptimality: " + maxSuboptimality + " for path from " + startIdWithMaxSuboptimality + " to " + goalIdWithMaxSuboptimality);
    }
}