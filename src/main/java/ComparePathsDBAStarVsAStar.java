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
        AStar aStar = new AStar(dbaStar.getProblem());

        GameMap map = dbaStar.getMap();

        double maxSuboptimality = -1;
        double averageSuboptimality = 0;

        int goalIdWithMaxSuboptimality = -1;

        for (int goalId : goalIds) {
            StatsRecord dbaStats = new StatsRecord();
//            dbaStarUtil.recomputeWallAdditionNoLogging(goalId, dbaStar);
//            dbaStarUtil.recomputeWallRemovalNoLogging(goalId, dbaStar);
            dbaStarUtil.recomputeWallAdditionNoChecks(goalId, dbaStar);
            dbaStarUtil.recomputeWallRemovalNoChecks(goalId, dbaStar);
            ArrayList<SearchState> path = dbaStar.computePath(new SearchState(startId), new SearchState(goalId), dbaStats);
            StatsRecord aStarStats = new StatsRecord();
            ArrayList<SearchState> optimalPath = aStar.computePath(new SearchState(startId), new SearchState(goalId), aStarStats);
            logger.info("AStar path cost: " + aStarStats.getPathCost() + " DBAStar path cost: " + dbaStats.getPathCost());
            double subOptimality = ((((double) dbaStats.getPathCost()) / aStarStats.getPathCost()) - 1) * 100.0;
            logger.info("Suboptimality: " + subOptimality);
            averageSuboptimality += subOptimality;

            if (maxSuboptimality < subOptimality) {
                maxSuboptimality = subOptimality;
                goalIdWithMaxSuboptimality = goalId;
            }

            if (path == null || path.isEmpty()) {
                logger.warn(String.format("No path was found between %d and %d!", startId, goalId));
                // map.computeCentroidMap().outputImage(DBA_STAR_DB_PATH  + "path_" + startId + "_" + goalId + ".png", path, dbaStar.getSubgoals());
                map.computeCentroidMap().outputImage(DBA_STAR_DB_PATH + "optimal_path_" + startId + "_" + goalId + ".png", optimalPath, dbaStar.getSubgoals());
            }
        }

        logger.info("Average suboptimality: " + averageSuboptimality / goalIds.size());
        logger.info("Max. suboptimality: " + maxSuboptimality + " for path from " + startId + " to " + goalIdWithMaxSuboptimality);
    }
}