import map.GameMap;
import org.junit.jupiter.api.Test;
import search.MapSearchProblem;
import search.SearchState;
import search.algorithms.AStar;
import search.algorithms.DBAStar;
import search.algorithms.HillClimbingWithClosedSet;
import stats.SearchStats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Iterating over all DA maps, finding all paths that do not have subgoals, ensuring lengths and costs for A* and HC paths
 * are equal.
 * Also checked whether the number of states expanded by HC vs A* are the same for these paths, they are not for all paths,
 * since A* may expand more from the open list.
 */
class SubgoalsVsNoSubgoalsTest {
    @Test
    void hcPathsShouldEqualAStarPaths() {
        // If we don't have a subgoal, the hc path should equal the A* path and both should expand the same number of states
        String[] mapStringsDA = {"hrt000d", "orz100d", "orz103d", "orz300d", "orz700d", "orz702d", "orz900d", "ost000a",
                "ost000t", "ost100d"};
        final int GRID_SIZE = 32;

        for (String mapString : mapStringsDA) {
            System.out.println("Map: " + mapString);
            GameMap gameMap = new GameMap("src/main/resources/maps/" + mapString + ".map");
            DBAStar dbaStar = new DBAStar(gameMap, GRID_SIZE, false);

            // Get subgoal-compressed paths stored in database
            int[][][] pathSubgoals = dbaStar.getSubgoalDB().getPathSubgoals();
            // Get number of regions (there are null values in the arrays for indices > numRegions)
            int numRegions = dbaStar.getSubgoalDB().getNumGroups();

            // Initialize HC and A*
            HillClimbingWithClosedSet hc = new HillClimbingWithClosedSet(new MapSearchProblem(gameMap));
            AStar aStar = new AStar(new MapSearchProblem(gameMap));

            for (int i = 0; i < numRegions; i++) {
                for (int j = 0; j < pathSubgoals[i].length; j++) {
                    if (pathSubgoals[i][j].length == 2) {
                        SearchState start = new SearchState(pathSubgoals[i][j][0]);
                        SearchState goal = new SearchState(pathSubgoals[i][j][1]);

                        // System.out.println("Start: " + start + " Goal: " + goal);

                        // Find HC path
                        SearchStats hcStats = new SearchStats();
                        hc.findPath(start, goal, hcStats);

                        // Find A* path
                        SearchStats aStarStats = new SearchStats();
                        aStar.findPath(start, goal, aStarStats);

                        // Compare
                        assertEquals(aStarStats.getPathLength(), hcStats.getPathLength());
                        assertEquals(aStarStats.getPathCost(), hcStats.getPathCost());

                        assertTrue(hcStats.getNumStatesExpandedHC() <= aStarStats.getNumStatesExpanded());
//                        if (hcStats.getNumStatesExpandedHC() != aStarStats.getNumStatesExpanded()) {
//                            System.out.println("Start: " + start + " Goal: " + goal);
//                            System.out.println(aStarStats.getNumStatesExpanded() + " " + hcStats.getNumStatesExpandedHC());
//                        }
                        // assertEquals(aStarStats.getNumStatesExpanded(), hcStats.getNumStatesExpandedHC()); // not true
                    }
                }
            }
        }
    }

    // Visualization for a case where A* expands a lot more states than HC
//    @Test
//    void examineBadAStarExpansion() {
//        // Start: 17997 Goal: 16093
//        // 110 21
//        GameMap gameMap = new GameMap("src/main/resources/maps/012.map");
//        VisualAStar visualAStar = new VisualAStar(new MapSearchProblem(gameMap));
//
//        List<SearchState> path = visualAStar.findPath(new SearchState(17997), new SearchState(16093));
//
//        VisualizeMap visualizeMap = new VisualizeMap(gameMap);
//        visualizeMap.outputImage("badExpansion.png", path, visualAStar.getStatesExpanded());
//    }
}