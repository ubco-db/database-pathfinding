package database;

import map.GameMap;
import org.junit.jupiter.api.Test;
import search.MapSearchProblem;
import search.SearchState;
import search.algorithms.AStar;
import search.algorithms.HillClimbing;
import stats.SearchStats;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static search.SearchUtil.findCompressedPath;

class SubgoalDBTest {
    @Test
    void getsSimpleCompressedPathCorrectly() {
        int startId = 3460;
        int goalId = 4486;
        int[] compressedPath = new int[]{startId, goalId};

        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        HillClimbing hillClimbing = new HillClimbing(mapSearchProblem);
        AStar astar = new AStar(mapSearchProblem);

        List<SearchState> path = astar.findPath(new SearchState(startId), new SearchState(goalId), new SearchStats());

        assertArrayEquals(compressedPath, findCompressedPath(path, hillClimbing, new SearchStats()));
    }

    // TODO: Find difference in A* pathfinding
//    @Test
//    void getsComplexCompressedPathCorrectly() {
//        int startId = 7931;
//        int goalId = 10448;
//        int[] compressedPath = new int[]{startId, 8080, 8081, 8230, 10446, goalId};
//
//        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
//        SearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
//        HillClimbing hillClimbing = new HillClimbing(mapSearchProblem);
//        AStarAndHC aStar = new AStarAndHC(mapSearchProblem);
//
//        List<SearchState> path = aStar.findPath(new SearchState(startId), new SearchState(goalId));
//
//         assertArrayEquals(compressedPath, SubgoalDB.findCompressedPath(path, hillClimbing));
//    }
}
