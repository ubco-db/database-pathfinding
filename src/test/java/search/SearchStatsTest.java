package search;

import map.GameMap;
import org.junit.jupiter.api.Test;
import search.algorithms.AStar;
import search.algorithms.HillClimbing;
import stats.SearchStats;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchStatsTest {
    @Test
    void computesStatesExpandedByAStarCorrectly() {
        int[][] states = {{32, 32, 32}, {32, 32, 32}, {32, 32, 32}};
        GameMap gameMap = new GameMap(states);
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        AStar aStar = new AStar(mapSearchProblem);

        SearchState start = new SearchState(3);
        SearchState goal = new SearchState(5);

        SearchStats searchStats = new SearchStats();
        aStar.findPath(start, goal, searchStats);

        assertEquals(0, searchStats.getNumStatesExpandedHC());
        assertEquals(3, searchStats.getNumStatesExpanded());
    }

    @Test
    void computesStatesUpdatedByAStarCorrectly() {
        int[][] states = {{32, 32, 32}, {32, 32, 32}, {32, 32, 32}};
        GameMap gameMap = new GameMap(states);
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        AStar aStar = new AStar(mapSearchProblem);

        SearchState start = new SearchState(3);
        SearchState goal = new SearchState(5);

        SearchStats searchStats = new SearchStats();
        aStar.findPath(start, goal, searchStats);

        assertEquals(12, searchStats.getNumStatesUpdated());
    }

    @Test
    void computesStatesExpandedByHCCorrectly() {
        int[][] states = {{32, 32, 32}, {32, 32, 32}, {32, 32, 32}};
        GameMap gameMap = new GameMap(states);
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        HillClimbing hillClimbing = new HillClimbing(mapSearchProblem);

        SearchState start = new SearchState(3);
        SearchState goal = new SearchState(5);

        SearchStats searchStats = new SearchStats();
        hillClimbing.findPath(start, goal, searchStats);

        assertEquals(0, searchStats.getNumStatesExpanded());
        assertEquals(3, searchStats.getNumStatesExpandedHC());
    }
}