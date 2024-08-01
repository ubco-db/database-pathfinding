package search.algorithms;

import map.GameMap;
import org.junit.jupiter.api.Test;
import search.MapSearchProblem;
import search.SearchState;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HillClimbingWithClosedSetTest {
    @Test
    void neighboursAreAllInClosedSet() {
        MapSearchProblem mapSearchProblem = new MapSearchProblem(new GameMap(new int[][]{{32,32,32,42,32},{32,32,32,42,32},{32,32,32,42,32}}));
        HillClimbingWithClosedSet hillClimbingWithClosedSet = new HillClimbingWithClosedSet(mapSearchProblem);

        int startId = 5;
        int goalId = 9;
        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);

        List<SearchState> path = hillClimbingWithClosedSet.findPath(start, goal);

        assertNull(path);
    }
}