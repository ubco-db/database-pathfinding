package search;

import map.GameMap;
import map.GroupRecord;
import util.ExpandArray;
import util.HeuristicFunction;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

/**
 * Supports grid-based search problems (for game maps).
 *
 * @author rlawrenc
 */
public class MapSearchProblem extends SearchProblem {
    private final GameMap map;
    private int row;
    private int col;

    public MapSearchProblem(GameMap map) {
        this.map = map;
    }

    public int computeDistance(SearchState start, SearchState goal) {
        // return GameMap.computeDistance(map.getRow(start.id), map.getCol(start.id), map.getRow(goal.id), map.getCol(goal.id));
        return GameMap.computeDistance(start.id, goal.id, map.cols);

    }

    public int computeDistance(SearchState start, SearchState goal, HeuristicFunction heuristic) {
        // return GameMap.computeDistance(map.getRow(start.id), map.getCol(start.id), map.getRow(goal.id), map.getCol(goal.id));
        return GameMap.computeDistance(start.id, goal.id, map.cols, heuristic);

    }

    public int computeDistance(int startId, int goalId) {
        //return GameMap.computeDistance(map.getRow(startId), map.getCol(startId), map.getRow(goalId), map.getCol(goalId));
        return GameMap.computeDistance(startId, goalId, map.cols);
    }

    public int computeDistance(int startId, int goalId, HeuristicFunction heuristic) {
        //return GameMap.computeDistance(map.getRow(startId), map.getCol(startId), map.getRow(goalId), map.getCol(goalId));
        return GameMap.computeDistance(startId, goalId, map.cols, heuristic);
    }

    public ArrayList<SearchState> getNeighbors(SearchState state) {
        return map.getNeighbors(map.getRow(state.id), map.getCol(state.id));
    }

    public int getMaxSize() {
        return map.rows * map.cols;
    }


    public GameMap getMap() {
        return map;
    }

    public void initIterator() {
        row = 0;
        col = 0;
    }

    public boolean nextState(SearchState state) {
        return getNextState(state);
    }

    private boolean getNextState(SearchState state) {
        for (; row < map.rows; row++) {
            for (; col < map.cols; col++) {
                if (map.squares[row][col] != GameMap.WALL_CHAR) {
                    state.id = map.getId(row, col);
                    state.cost = map.squares[row][col++] - GameMap.START_NUM;
                    return true;
                }
            }
            col = 0;
        }
        return false;
    }

    public SearchState generateRandomState(Random generator) {
        int row, col;
        int maxRow = map.rows;
        int maxCol = map.cols;

        do {
            row = generator.nextInt(maxRow);
            col = generator.nextInt(maxCol);
        } while (map.isWall(row, col));
        return new SearchState(map.getId(row, col));
    }

    public String idToString(int id) {
        return "(" + map.getRow(id) + ", " + map.getCol(id) + ")";
    }

    public GroupRecord[] getGroups() throws Exception {
        return map.getGroups();
    }

    public int getNumGroups() {
        return map.getNumGroups();
    }

    public void computeNeighbors() {
        map.computeNeighbors();
    }

    public int getMoveCost(int startId, int goalId) {    // Assumes they are not the same state (as move cost would be zero then)
        // This was current code for a diagonal movement
        int diff = startId - goalId;
        int bit31 = diff >> 31;
        diff = (diff ^ bit31) - bit31;

        if (diff == 1 || diff == map.cols)
            return 10;
        else
            return 14;
    }

    public int getMoveCost(SearchState start, SearchState goal) {
        return getMoveCost(start.id, goal.id);
    }

    public void getNeighbors(int stateId, ExpandArray neighbors) {
        map.getNeighbors(map.getRow(stateId), map.getCol(stateId), neighbors);
    }

    public boolean isNeighbor(int fromStateId, int toStateId) {
        // Is a neighbor if heuristic distance is <= 14 (one move)
        int dist = computeDistance(fromStateId, toStateId);
        return (dist < 14);
    }
}
