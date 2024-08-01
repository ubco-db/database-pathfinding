package search.algorithms;

import map.AbstractedMap;
import map.GameMap;
import map.VisualizeMap;
import org.junit.jupiter.api.Test;
import search.MapSearchProblem;
import search.SearchState;
import search.SearchUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompressAStarTest {
    String[] mapNames = {"012", "516", "603", "701", "hrt000d", "orz100d", "orz103d", "orz300d", "orz700d", "orz702d", "orz900d", "ost000a", "ost000t", "ost100d"};

    @Test
    void hillClimbingPathAfterCompressionEqualsAStarPathForAllInterRegionRepPaths() {
        for (String mapName : mapNames) {
            GameMap gameMap = new GameMap("src/test/resources/maps/" + mapName + ".map");

            System.out.println("Map: " + mapName);
            runCompressionForDifferentGridSizesOn(gameMap);
            System.out.println();
        }
    }

    private void runCompressionForDifferentGridSizesOn(GameMap gameMap) {
        for (int gridSize = 8; gridSize <= 128; gridSize *= 2) {
            System.out.println(gridSize);
            expandedPathEqualsOptimalPathBetweenRegionReps(gameMap, gridSize);
        }
    }

    @Test
    void hillClimbingPathAfterCompressionEqualsAStarPath72377To72381_hrt000d() {
        /*
        This crashes with an index out of bounds. Something is wrong with the closed list checking for neighbours.
        It was because no check was performed that the next item had not already been visited. The check was previously
        only in the neighbour loop.
         */

        GameMap gameMap = new GameMap("src/test/resources/maps/hrt000d.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        CompressAStar compressAStar = new CompressAStar(mapSearchProblem);

        HillClimbingWithClosedSet hc = new HillClimbingWithClosedSet(mapSearchProblem);

        int startId = 72377;
        int goalId = 72381;

        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);

//        VisualizeMap visualizeMap = new VisualizeMap(gameMap);
//        visualizeMap.outputImage("issue.png", List.of(start, goal), null);

        // Get optimal path
        List<SearchState> optimalPath = compressAStar.findPath(start, goal, null);
        // Find compressed path
        List<SearchState> compressedPath = compressAStar.findCompressedPath(optimalPath, hc, null, false);

        System.out.println("Optimal path: " + optimalPath);
        System.out.println("Compressed path: " + compressedPath);

        assertEquals(optimalPath, getExpandedPath(start, goal, compressedPath, hc));
    }

    @Test
    void hillClimbingPathAfterCompressionEqualsAStarPath46864To45580_orz300d() {
        /*
        This gets stuck in an infinite loop
         */
        GameMap gameMap = new GameMap("src/test/resources/maps/orz300d.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        CompressAStar compressAStar = new CompressAStar(mapSearchProblem);

        HillClimbingWithClosedSet hc = new HillClimbingWithClosedSet(mapSearchProblem);

        int startId = 46864;
        int goalId = 45580;

        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);

        VisualizeMap visualizeMap = new VisualizeMap(gameMap);
        visualizeMap.outputImage("issue.png", List.of(start, goal), null);

        // Get optimal path
        List<SearchState> optimalPath = compressAStar.findPath(start, goal, null);
        // Find compressed path
        List<SearchState> compressedPath = compressAStar.findCompressedPath(optimalPath, hc, null, false);


        System.out.println("Optimal path: " + optimalPath);
        System.out.println("Compressed path: " + compressedPath);

        assertEquals(optimalPath, getExpandedPath(start, goal, compressedPath, hc));
    }

    @Test
    void hillClimbingPathAfterCompressionEqualsAStarPathFrom3460To5826() {
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        CompressAStar compressAStar = new CompressAStar(mapSearchProblem);

        HillClimbingWithClosedSet hc = new HillClimbingWithClosedSet(mapSearchProblem);

        int startId = 3460;
        int goalId = 5826;

        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);

        // Get optimal path
        List<SearchState> optimalPath = compressAStar.findPath(start, goal, null);
        // Find compressed path
        List<SearchState> compressedPath = compressAStar.findCompressedPath(optimalPath, hc, null, false);

//        System.out.println("Optimal path: " + optimalPath);
//        System.out.println("Compressed path: " + compressedPath);

        assertEquals(1, compressedPath.size());

        assertEquals(optimalPath, getExpandedPath(start, goal, compressedPath, hc));
    }

    @Test
    void hillClimbingPathAfterCompressionEqualsAStarPathFrom5826To3460() {
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        CompressAStar compressAStar = new CompressAStar(mapSearchProblem);

        HillClimbingWithClosedSet hc = new HillClimbingWithClosedSet(mapSearchProblem);

        int startId = 5826;
        int goalId = 3460;

        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);

        // Get optimal path
        List<SearchState> optimalPath = compressAStar.findPath(start, goal, null);
        // Find compressed path
        List<SearchState> compressedPath = compressAStar.findCompressedPath(optimalPath, hc, null, false);

//        System.out.println("Optimal path: " + optimalPath);
//        System.out.println("Compressed path: " + compressedPath);

        assertEquals(2, compressedPath.size());

        assertEquals(optimalPath, getExpandedPath(start, goal, compressedPath, hc));
    }

    @Test
    void hillClimbingPathAfterCompressionEqualsAStarPathFrom8195To10861() {
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        CompressAStar compressAStar = new CompressAStar(mapSearchProblem);

        HillClimbingWithClosedSet hc = new HillClimbingWithClosedSet(mapSearchProblem);

        int startId = 8195;
        int goalId = 10861;

        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);

        // Get optimal path
        List<SearchState> optimalPath = compressAStar.findPath(start, goal, null);
        // Find compressed path
        List<SearchState> compressedPath = compressAStar.findCompressedPath(optimalPath, hc, null, false);

//        System.out.println("Optimal path: " + optimalPath);
//        System.out.println("Compressed path: " + compressedPath);

        assertEquals(3, compressedPath.size());

        assertEquals(optimalPath, getExpandedPath(start, goal, compressedPath, hc));
    }

    private List<SearchState> getExpandedPath(SearchState start, SearchState goal, List<SearchState> compressedPath, HillClimbingWithClosedSet hc) {
        List<SearchState> expandedPath = new ArrayList<>();
        List<SearchState> pathFragment;

        SearchState subgoal1 = start, subgoal2;
        for (SearchState state : compressedPath) {
            subgoal2 = state;

            pathFragment = hc.findPath(subgoal1, subgoal2);
            SearchUtil.mergePaths(expandedPath, pathFragment);

            subgoal1 = subgoal2;
        }

        pathFragment = hc.findPath(subgoal1, goal);

        // System.out.println("Path fragment: " + pathFragment);

        SearchUtil.mergePaths(expandedPath, pathFragment);

        // System.out.println("Expanded path: " + expandedPath);

        return expandedPath;
    }

    private void expandedPathEqualsOptimalPathBetweenRegionReps(GameMap gameMap, int gridSize) {
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

        int numPaths = 0, numPathsWithoutSubgoals = 0, numPathsWithSubgoals = 0;

        List<SearchState> compressedPath, optimalPath;

        SearchState start, goal;
        // Find all paths between neighbouring region reps (as DBA* would for abstraction)
        for (int i = 0; i < numRegionReps; i++) {
            int startRegionRep = regionReps[i];
            start = new SearchState(startRegionRep);

            for (int neighbourRegion : abstractedMap.getRegionById(i + AbstractedMap.START_NUM).getNeighborIds()) {
                int goalRegionRep = regionReps[neighbourRegion - AbstractedMap.START_NUM];
                goal = new SearchState(goalRegionRep);

                // System.out.println("Start: " + start + " Goal: " + goal);

                // Get optimal path
                optimalPath = compressAStar.findPath(start, goal, null);

                // Find compressed path
                compressedPath = compressAStar.findCompressedPath(optimalPath, hc, null, true);
                // System.out.println("Compressed path: " + compressedPath);

                numStatesStoredWithoutSubgoals += optimalPath.size() - 2;
                numSubgoals += compressedPath.size();

                if (compressedPath.isEmpty()) numPathsWithoutSubgoals++;
                else numPathsWithSubgoals++;

                if (!optimalPath.equals(getExpandedPath(start, goal, compressedPath, hc))) {
                    unequalPathsCounter++;
                } else {
                    equalPathsCounter++;
                }

                numPaths++;
            }
        }

        System.out.println("Total number of paths: " + numPaths);
        int binarySearchNeededCount = numPaths - compressAStar.getCheckTriggeredCount();
        System.out.printf("Number of paths that needed binary search for compression: %d (%.2f%%)\n", binarySearchNeededCount, getAsPercentageOf(binarySearchNeededCount, numPaths));
        System.out.println("Number of paths that are not equal after compression: " + unequalPathsCounter);
        System.out.println("Number of paths that are equal after compression: " + equalPathsCounter);

        System.out.println("Number of paths that have subgoals: " + numPathsWithSubgoals);
        System.out.println("Number of paths without subgoals: " + numPathsWithoutSubgoals);

        System.out.printf("Number of paths where binary search was run despite there not being subgoals: %d (%.2f%%)\n", (numPathsWithoutSubgoals - compressAStar.getCheckTriggeredCount()), getAsPercentageOf((numPathsWithoutSubgoals - compressAStar.getCheckTriggeredCount()), numPathsWithoutSubgoals));

        System.out.println("Subgoals in database: " + numSubgoals);
        System.out.println("Number of states stored without subgoals: " + numStatesStoredWithoutSubgoals);
        System.out.println("Percentage of states saved: " + ((double) numStatesStoredWithoutSubgoals - numSubgoals) / numStatesStoredWithoutSubgoals);

        assertEquals(0, unequalPathsCounter);
    }

    private double getAsPercentageOf(double d1, double d2) {
        return d1 / d2 * 100;
    }

    @Test
    void hillClimbingPathAfterCompressionEqualsAStarPathFrom159760To152401() {
        /*
        In this case, the A* closed set size equals the optimal path size, but hill-climbing could previously
        not find the path because the h value of the second state is larger than that of the first state.
        Removing the "if (nextH >= currentH)"-check led to the algorithm oscillating between first and second state.
        A closed list was added to fix this issue.
         */
        GameMap gameMap = new GameMap("src/test/resources/maps/hrt000d.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        CompressAStar compressAStar = new CompressAStar(mapSearchProblem);

        HillClimbingWithClosedSet hc = new HillClimbingWithClosedSet(mapSearchProblem);

        int startId = 159760;
        int goalId = 152401;

        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);

        VisualizeMap visualizeMap = new VisualizeMap(gameMap);

        // Get optimal path
        List<SearchState> optimalPath = compressAStar.findPath(start, goal, null);
        // Find compressed path
        List<SearchState> compressedPath = compressAStar.findCompressedPath(optimalPath, hc, null, false);

        // System.out.println("Optimal path: " + optimalPath);
        // System.out.println("Compressed path: " + compressedPath);

        // System.out.println("Closed list: " + compressAStar.getClosedHashSet());

        Integer[] int1 = compressAStar.getClosedHashSet().toArray(new Integer[0]);
        Integer[] int2 = optimalPath.stream().map(SearchState::getStateId).toArray(Integer[]::new);
        Arrays.sort(int1);
        Arrays.sort(int2);

        // visualizeMap.outputImage("map.png", optimalPath, compressedPath);

        assertArrayEquals(int1, int2);

        assertEquals(optimalPath, getExpandedPath(start, goal, compressedPath, hc));
    }

    //    @Test
    void wallBasedCompressionIsOptimal() {
        for (String mapName : mapNames) {
            GameMap gameMap = new GameMap("src/test/resources/maps/" + mapName + ".map");

            System.out.println("Map: " + mapName);
            runWallABasedCompressionForDifferentGridSizesOn(gameMap);
            System.out.println();
        }
    }

    private void runWallABasedCompressionForDifferentGridSizesOn(GameMap gameMap) {
        for (int gridSize = 16; gridSize <= 128; gridSize *= 2) {
            System.out.println(gridSize);
            expandedWallBasedPathHasSameCostAsOptimalPathBetweenRegionReps(gameMap, gridSize);
        }
    }

    /*
    The paths are not always going to be the exact same, but will the costs be?
    TODO: Why are the paths actually not equal?
     */
    private void expandedWallBasedPathHasSameCostAsOptimalPathBetweenRegionReps(GameMap gameMap, int gridSize) {
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        CompressAStar compressAStar = new CompressAStar(mapSearchProblem);
        HillClimbingWithClosedSet hc = new HillClimbingWithClosedSet(mapSearchProblem);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, gridSize);

        int[] regionReps = abstractedMap.getRegionReps();
        int numRegionReps = abstractedMap.getNumRegions();

        long unequalCostPathsCounter = 0, equalPathsCounter = 0, equalCostPathsCounter = 0;

        // Number of subgoals we need to store after hc compression
        int numSubgoals = 0;
        // Number of states we need to store without compression
        int numStatesStoredWithoutSubgoals = 0;

        List<SearchState> compressedPath, expandedPath, optimalPath;

        int expandedCost, optimalCost;

        SearchState start, goal;
        // Find all paths between neighbouring region reps (as DBA* would for abstraction)
        for (int i = 0; i < numRegionReps; i++) {
            int startRegionRep = regionReps[i];
            start = new SearchState(startRegionRep);

            for (int neighbourRegion : abstractedMap.getRegionById(i + AbstractedMap.START_NUM).getNeighborIds()) {
                int goalRegionRep = regionReps[neighbourRegion - AbstractedMap.START_NUM];
                goal = new SearchState(goalRegionRep);

                // System.out.println("Start: " + start + " Goal: " + goal);

                // Get optimal path
                optimalPath = compressAStar.findPath(start, goal, null);
                // Find compressed path
                compressedPath = compressAStar.findWallCompressedPath(start, goal, optimalPath);
                // System.out.println("Compressed path: " + compressedPath);

                numStatesStoredWithoutSubgoals += optimalPath.size() - 2;
                numSubgoals += compressedPath.size();

                expandedPath = getExpandedPath(start, goal, compressedPath, hc);

                if (!optimalPath.equals(expandedPath)) {
                    expandedCost = SearchUtil.findPathCost(expandedPath, mapSearchProblem);
                    optimalCost = SearchUtil.findPathCost(optimalPath, mapSearchProblem);

                    if (expandedCost != optimalCost) {
                        System.out.println("Start " + start + " goal " + goal);
                        System.out.println("Compressed path: " + compressedPath);
                        System.out.println("Optimal path: " + optimalPath);
                        System.out.println("Expanded path: " + expandedPath);

                        unequalCostPathsCounter++;
                    } else {
                        equalCostPathsCounter++;
                    }
                } else {
                    equalPathsCounter++;
                }
            }
        }

        System.out.println("Number of paths that are not equal after compression: " + unequalCostPathsCounter);
        System.out.println("Number of paths that have the same cost after compression: " + equalCostPathsCounter);
        System.out.println("Number of paths that are exactly equal after compression: " + equalPathsCounter);

        System.out.println("Subgoals in database: " + numSubgoals);
        System.out.println("Number of states stored without subgoals: " + numStatesStoredWithoutSubgoals);

        System.out.println("Percentage of states saved: " + ((double) numStatesStoredWithoutSubgoals - numSubgoals) / numStatesStoredWithoutSubgoals);

        assertEquals(0, unequalCostPathsCounter);
    }

    @Test
    void hillClimbingPathAfterWallBasedCompressionEqualsAStarPathFrom3917To3460() {
        /*
        Using the wall-based compression won't lead to equal paths, but it will give optimal paths (that minimize cost)
         */
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        CompressAStar compressAStar = new CompressAStar(mapSearchProblem);

        HillClimbingWithClosedSet hc = new HillClimbingWithClosedSet(mapSearchProblem);

        int startId = 3917;
        int goalId = 3460;

        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);

        VisualizeMap visualizeMap = new VisualizeMap(gameMap);

        // Get optimal path
        List<SearchState> optimalPath = compressAStar.findPath(start, goal, null);
        int optimalCost = SearchUtil.findPathCost(optimalPath, mapSearchProblem);

        // Find compressed path
        List<SearchState> compressedPath = compressAStar.findWallCompressedPath(start, goal, optimalPath);
        System.out.println("Compressed path: " + compressedPath);

        List<SearchState> expandedPath = getExpandedPath(start, goal, compressedPath, hc);
        int expandedCost = SearchUtil.findPathCost(expandedPath, mapSearchProblem);

        List<SearchState> optimallyCompressedPath = compressAStar.findCompressedPath(optimalPath, hc, null, false);
        System.out.println("Optimally compressed path: " + optimallyCompressedPath);

        visualizeMap.outputImage("optimal.png", optimalPath);
        visualizeMap.outputImage("expanded.png", expandedPath);

        assertEquals(expandedCost, optimalCost);
        // assertEquals(optimalPath, expandedPath);
    }

    //    @Test
    void hillClimbingPathAfterWallBasedCompressionHasSameCostAsAStarPathFrom137816To151411() {
        /*
        Cost for expanded path using wall based compression won't always be the same as that of the optimal path.
        TODO: How to remedy?
         */
        GameMap gameMap = new GameMap("src/test/resources/maps/orz100d.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        CompressAStar compressAStar = new CompressAStar(mapSearchProblem);

        HillClimbingWithClosedSet hc = new HillClimbingWithClosedSet(mapSearchProblem);

        int startId = 137816;
        int goalId = 151411;

        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);

        VisualizeMap visualizeMap = new VisualizeMap(gameMap);

        // Get optimal path
        List<SearchState> optimalPath = compressAStar.findPath(start, goal, null);
        int optimalCost = SearchUtil.findPathCost(optimalPath, mapSearchProblem);

        // Find compressed path
        List<SearchState> compressedPath = compressAStar.findWallCompressedPath(start, goal, optimalPath);
        System.out.println("Compressed path: " + compressedPath);

        List<SearchState> expandedPath = getExpandedPath(start, goal, compressedPath, hc);
        int expandedCost = SearchUtil.findPathCost(expandedPath, mapSearchProblem);

        List<SearchState> optimallyCompressedPath = compressAStar.findCompressedPath(optimalPath, hc, null, false);
        System.out.println("Optimally compressed path: " + optimallyCompressedPath);

        visualizeMap.outputImage("optimalPathFrom" + startId + "To" + goalId + ".png", optimalPath);
        visualizeMap.outputImage("expandedPathFrom" + startId + "To" + goalId + ".png", expandedPath);

        assertEquals(optimalCost, expandedCost);
    }

    @Test
    void closedSetCheckCapturesEveryCaseOfNoSubgoals() {
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        CompressAStar compressAStar = new CompressAStar(mapSearchProblem);
        HillClimbingWithClosedSet hc = new HillClimbingWithClosedSet(mapSearchProblem);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 16);

        int[] regionReps = abstractedMap.getRegionReps();
        int numRegionReps = abstractedMap.getNumRegions();

        List<SearchState> compressedPath, expandedPath, optimalPath;

        int numCompressedPathsWithoutSubgoals = 0;
        int timesCheckNotTriggered = 0;
        int numPaths = 0;

        SearchState start, goal;
        // Find all paths between neighbouring region reps (as DBA* would for abstraction)
        for (int i = 0; i < numRegionReps; i++) {
            int startRegionRep = regionReps[i];
            start = new SearchState(startRegionRep);

            for (int neighbourRegion : abstractedMap.getRegionById(i + AbstractedMap.START_NUM).getNeighborIds()) {
                int goalRegionRep = regionReps[neighbourRegion - AbstractedMap.START_NUM];
                goal = new SearchState(goalRegionRep);

                // System.out.println("Start: " + start + " Goal: " + goal);

                // Get optimal path
                optimalPath = compressAStar.findPath(start, goal, null);
                // Find compressed path
                compressedPath = compressAStar.findCompressedPath(optimalPath, hc, null, true);
                // System.out.println("Compressed path: " + compressedPath);

                expandedPath = getExpandedPath(start, goal, compressedPath, hc);
                assertEquals(optimalPath, expandedPath);

                if (compressedPath.isEmpty()) {
                    numCompressedPathsWithoutSubgoals++;
                }

                if (!compressAStar.isCheckTriggered() && compressedPath.isEmpty()) {
//                    System.out.println("Start: " + start + " Goal: " + goal);
                    optimalPath.sort(Comparator.comparingInt(SearchState::getStateId));
//                    System.out.println("Optimal path: " + optimalPath);
//                    System.out.println("Sorted closed set: " + compressAStar.getClosedHashSet().stream().sorted().toList());
                    timesCheckNotTriggered++;
                }

                numPaths++;
            }
        }

        System.out.println("Times check was triggered: " + compressAStar.getCheckTriggeredCount());
        System.out.println("Times check was not triggered: " + timesCheckNotTriggered);
        System.out.println("Number of compressed paths without subgoals: " + numCompressedPathsWithoutSubgoals);
        System.out.println("Total number of paths: " + numPaths);
        System.out.println("Times check was not triggered despite there not being subgoals: " + (numCompressedPathsWithoutSubgoals - compressAStar.getCheckTriggeredCount()));
    }

//    @Test
//    void closedCheckTriggersForPathFrom3460To3917() {
//        /*
//        It doesn't trigger because A* takes from the open list and adds the state to the closed list immediately
//         */
//        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
//        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
//        CompressAStar aStarAndHC = new CompressAStar(mapSearchProblem);
//        HillClimbingWithClosedSet hc = new HillClimbingWithClosedSet(mapSearchProblem);
//
//        int startId = 3460;
//        int goalId = 3917;
//
//        SearchState start = new SearchState(startId);
//        SearchState goal = new SearchState(goalId);
//
//        // Get optimal path
//        List<SearchState> optimalPath = aStarAndHC.findPath(start, goal);
//        // Find compressed path
//        List<SearchState> compressedPath = aStarAndHC.findOptimallyCompressedPath(optimalPath, true);
//        // Find expanded path
//        List<SearchState> expandedPath = getExpandedPath(start, goal, compressedPath, hc);
//
//        optimalPath.sort(Comparator.comparingInt(SearchState::getStateId));
//        System.out.println(optimalPath);
//        System.out.println(aStarAndHC.getClosedHashSet().stream().sorted().toList());
////        System.out.println(compressedPath);
//
//        VisualizeMap visualizeMap = new VisualizeMap(gameMap);
////        visualizeMap.outputImage("closedSetFrom" + startId + "To" + goalId + ".png", null, aStarAndHC.getClosedHashSet().stream().map(SearchState::new).toList());
//        // visualizeMap.outputImage("optimalPathFrom" + startId + "To" + goalId + ".png", optimalPath, null);
//        visualizeMap.outputImage("3312.png", List.of(new SearchState(3312)));
//    }
}
