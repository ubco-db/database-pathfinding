package search;

import map.GameMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class MapSearchProblemTest {

    @Test
    void cardinalCostShouldBeCorrect() {
        int moveCost;
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);

        int startId = 14002;

        // north
        moveCost = mapSearchProblem.getMoveCost(startId, 13854);
        assertEquals(MapSearchProblem.EDGE_COST_CARDINAL, moveCost);
        // east
        moveCost = mapSearchProblem.getMoveCost(startId, 14003);
        assertEquals(MapSearchProblem.EDGE_COST_CARDINAL, moveCost);
        // south
        moveCost = mapSearchProblem.getMoveCost(startId, 14150);
        assertEquals(MapSearchProblem.EDGE_COST_CARDINAL, moveCost);
        // west
        moveCost = mapSearchProblem.getMoveCost(startId, 14001);
        assertEquals(MapSearchProblem.EDGE_COST_CARDINAL, moveCost);
    }

    @Test
    void diagonalCostShouldBeCorrect() {
        int moveCost;
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);

        int startId = 14002;

        // north-east
        moveCost = mapSearchProblem.getMoveCost(startId, 13855);
        assertEquals(MapSearchProblem.EDGE_COST_DIAGONAL, moveCost);
        // south-east
        moveCost = mapSearchProblem.getMoveCost(startId, 14151);
        assertEquals(MapSearchProblem.EDGE_COST_DIAGONAL, moveCost);
        // south-west
        moveCost = mapSearchProblem.getMoveCost(startId, 14149);
        assertEquals(MapSearchProblem.EDGE_COST_DIAGONAL, moveCost);
        // north-west
        moveCost = mapSearchProblem.getMoveCost(startId, 13853);
        assertEquals(MapSearchProblem.EDGE_COST_DIAGONAL, moveCost);
    }

    @Test
    void moveCostShouldBeZeroIfSameStartAndGoal() {
        int moveCost;
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);

        int startId = 14002;

        moveCost = mapSearchProblem.getMoveCost(startId, startId);

        assertEquals(0, moveCost);
    }

    @Test
    void shouldThrowExceptionForNonAdjacentMoves() {
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);

        int startId = 14002;

        assertThrowsExactly(RuntimeException.class, () -> mapSearchProblem.getMoveCost(startId, 14004));
        assertThrowsExactly(RuntimeException.class, () -> mapSearchProblem.getMoveCost(startId, 14000));

        // TODO: Add checks for diagonals and verticals
    }
}
