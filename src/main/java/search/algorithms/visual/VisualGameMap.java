package search.algorithms.visual;

import map.GameMap;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * GameMap used in HCVsAStarVisualization, uses 2D array of AtomicInteger to store map states
 */
public class VisualGameMap extends GameMap {
    final AtomicInteger[][] states;

    public VisualGameMap(AtomicInteger[][] states) {
        super(states.length, states[0].length);
        this.states = states;
    }

    public void setStateValue(int row, int col, int value) {
        states[row][col].set(value);
    }

    public void placeWallAt(int sid) {
        int row = getRowFromStateId(sid);
        int col = getColFromStateId(sid);

        if (states[row][col].get() == WALL_CHAR) {
            throw new RuntimeException("There is a wall at " + sid + " (" + row + ", " + col + ") already!");
        } else {
            setStateValue(row, col, WALL_CHAR);
        }
    }

    public void placeOpenStateAt(int sid) {
        int row = getRowFromStateId(sid);
        int col = getColFromStateId(sid);

        if (states[row][col].get() == WALL_CHAR) {
            setStateValue(row, col, EMPTY_CHAR);
        } else {
            throw new RuntimeException("There is an open state at " + sid + " (" + row + ", " + col + ") already!");
        }
    }

    public boolean isWall(int row, int col) {
        return states[row][col].get() == WALL_CHAR;
    }
}
