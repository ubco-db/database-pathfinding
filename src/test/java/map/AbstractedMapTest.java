package map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractedMapTest {
    @Test
    void getsR1SumCorrectly1() {
        int[][] states = new int[][]{{32, 32, 32}, {32, 42, 32}, {32, 32, 32}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(0, abstractedMap.getR1Sum(directNeighbours));
    }

    @Test
    void getsR1SumCorrectly2() {
        int[][] states = new int[][]{{32, 42, 42}, {32, 42, 32}, {32, 32, 32}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(2, abstractedMap.getR1Sum(directNeighbours));
    }

    @Test
    void getsR1SumCorrectly3() {
        int[][] states = new int[][]{{42, 42, 42}, {32, 42, 32}, {32, 32, 32}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(3, abstractedMap.getR1Sum(directNeighbours));
    }

    @Test
    void getsR2SumCorrectly1() {
        int[][] states = new int[][]{{32, 32, 32}, {32, 42, 32}, {32, 32, 32}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(1, abstractedMap.getR2Sum(directNeighbours));
    }

    @Test
    void getsR2SumCorrectly2() {
        int[][] states = new int[][]{{32, 32, 32}, {42, 42, 32}, {32, 32, 32}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(2, abstractedMap.getR2Sum(directNeighbours));
    }

    @Test
    void getsR2SumCorrectly3() {
        int[][] states = new int[][]{{32, 32, 32}, {42, 42, 42}, {32, 32, 32}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(3, abstractedMap.getR2Sum(directNeighbours));
    }

    @Test
    void getsR3SumCorrectly1() {
        int[][] states = new int[][]{{32, 32, 32}, {32, 42, 32}, {42, 32, 32}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(1, abstractedMap.getR3Sum(directNeighbours));
    }

    @Test
    void getsR3SumCorrectly2() {
        int[][] states = new int[][]{{32, 32, 32}, {32, 42, 32}, {42, 32, 42}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(2, abstractedMap.getR3Sum(directNeighbours));
    }

    @Test
    void getsC1SumCorrectly1() {
        int[][] states = new int[][]{{32, 32, 32}, {32, 42, 32}, {42, 32, 42}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(1, abstractedMap.getC1Sum(directNeighbours));
    }

    @Test
    void getsC1SumCorrectly2() {
        int[][] states = new int[][]{{32, 32, 32}, {42, 42, 32}, {42, 32, 42}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(2, abstractedMap.getC1Sum(directNeighbours));
    }


    @Test
    void getsC1SumCorrectly3() {
        int[][] states = new int[][]{{42, 32, 32}, {42, 42, 32}, {42, 32, 42}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(3, abstractedMap.getC1Sum(directNeighbours));
    }

    @Test
    void getsC2SumCorrectly1() {
        int[][] states = new int[][]{{42, 32, 32}, {42, 42, 32}, {42, 32, 42}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(1, abstractedMap.getC2Sum(directNeighbours));
    }

    @Test
    void getsC2SumCorrectly2() {
        int[][] states = new int[][]{{42, 42, 32}, {42, 42, 32}, {42, 32, 42}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(2, abstractedMap.getC2Sum(directNeighbours));
    }

    @Test
    void getsC2SumCorrectly3() {
        int[][] states = new int[][]{{42, 42, 32}, {42, 42, 32}, {42, 42, 42}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(3, abstractedMap.getC2Sum(directNeighbours));
    }

    @Test
    void getsC3SumCorrectly1() {
        int[][] states = new int[][]{{42, 42, 32}, {32, 42, 32}, {42, 42, 42}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(1, abstractedMap.getC3Sum(directNeighbours));
    }

    @Test
    void getsC3SumCorrectly2() {
        int[][] states = new int[][]{{42, 42, 32}, {32, 42, 42}, {42, 42, 42}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(2, abstractedMap.getC3Sum(directNeighbours));
    }

    @Test
    void getsC3SumCorrectly3() {
        int[][] states = new int[][]{{42, 42, 42}, {32, 42, 42}, {42, 42, 42}};
        GameMap gameMap = new GameMap(states);

        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 5);

        int[] directNeighbours = new int[8];
        abstractedMap.getDirectNeighbourValues(directNeighbours, 1, 1);

        assertEquals(3, abstractedMap.getC3Sum(directNeighbours));
    }
}