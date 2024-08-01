package map;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameMapTest {
    @Test
    void stateNeighboursShouldBeCorrect() {
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");

        List<Integer> stateNeighbours = gameMap.getStateNeighbourIds(14002);

        List<Integer> expectedNeighbours = Arrays.asList(13854, 14003, 14150, 14001, 13855, 14151, 14149, 13853);

        assertEquals(expectedNeighbours, stateNeighbours);
    }

    @Test
    void mapGetsInitializedProperly() {
        int[][] states = {{32, 42, 32}, {32, 32, 32}, {32, 42, 32}};
        GameMap map = new GameMap(states);

        assertEquals(3, map.getNumCols());
        assertEquals(3, map.getNumRows());
        assertEquals(7, map.getNumOpenStates());
    }

    // TODO: Test when some of the neighbours are walls
    // TODO: Test with diagonal where corresponding cardinals are not open
}
