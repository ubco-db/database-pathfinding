import map.AbstractedMap;
import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.MapSearchProblem;
import search.SearchState;
import search.algorithms.CompressAStar;
import search.algorithms.HillClimbingWithClosedSet;

import java.util.List;

/**
 * This class is for comparing the time it takes to run CompressAStar findOptimallyCompressedPath with and without the check enabled
 * TODO: Time compression specifically (also, this is likely skewed by large grid sizes where check will fail almost always)
 */
public class BenchmarkOptimalCompressionPerformance {

    private static final Logger logger = LogManager.getLogger(BenchmarkOptimalCompressionPerformance.class);

    public static void main(String[] args) {
        boolean runCheck = false;

        long start, end, totalTime;
        long globalStart, globalEnd;
        String[] mapNames = {"012", "516", "603", "701", "hrt000d", "orz100d", "orz103d", "orz300d", "orz700d", "orz702d", "orz900d", "ost000a", "ost000t", "ost100d"};

        globalStart = System.currentTimeMillis();
        totalTime = 0;
        int count = 10;
        for (int i = 0; i <= count; i++) {
            for (String mapName : mapNames) {
                GameMap gameMap = new GameMap("src/test/resources/maps/" + mapName + ".map");

//            System.out.println("Map: " + mapName);
                start = System.currentTimeMillis();
                runCompressionForDifferentGridSizesOn(gameMap, runCheck);
                end = System.currentTimeMillis();

                totalTime += end - start;
//            System.out.println();
            }
        }
        globalEnd = System.currentTimeMillis();

        logger.info("Time taken for all maps, grid sizes 8, 16, 32, 64, 128 each, check " + (runCheck ? "enabled" : "disabled") + " averaged over " + count + " runs: " + (totalTime / 10.0));
        logger.info(globalEnd - globalStart);
    }

    private static void runCompressionForDifferentGridSizesOn(GameMap gameMap, boolean runCheck) {
        for (int gridSize = 8; gridSize <= 128; gridSize *= 2) {
//            System.out.println(gridSize);
            expandedPathEqualsOptimalPathBetweenRegionReps(gameMap, gridSize, runCheck);
        }
    }

    private static void expandedPathEqualsOptimalPathBetweenRegionReps(GameMap gameMap, int gridSize, boolean runCheck) {
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        CompressAStar compressAStar = new CompressAStar(mapSearchProblem);
        HillClimbingWithClosedSet hc = new HillClimbingWithClosedSet(mapSearchProblem);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, gridSize);

        int[] regionReps = abstractedMap.getRegionReps();
        int numRegionReps = abstractedMap.getNumRegions();

        long unequalPathsCounter = 0, equalPathsCounter = 0;

        // Number of subgoals we need to store after hc compression
        int numSubgoals = 0;
        // Number of states we need to store without compression
        int numStatesStoredWithoutSubgoals = 0;
        int numPaths = 0;

        List<SearchState> compressedPath, optimalPath;

        SearchState start, goal;
        // Find all paths between neighbouring region reps (as DBA* would for abstraction)
        for (int i = 0; i < numRegionReps; i++) {
            int startRegionRep = regionReps[i];
            start = new SearchState(startRegionRep);

            for (int neighbourRegion : abstractedMap.getRegionById(i + AbstractedMap.START_NUM).getNeighborIds()) {
                int goalRegionRep = regionReps[neighbourRegion - AbstractedMap.START_NUM];
                goal = new SearchState(goalRegionRep);

                // Get optimal path
                optimalPath = compressAStar.findPath(start, goal, null);

                // Find compressed path
                compressAStar.findCompressedPath(optimalPath, hc, null, runCheck);
            }
        }
    }

}
