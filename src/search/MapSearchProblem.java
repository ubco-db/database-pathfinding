package search;

import map.GameMap;
import util.HeuristicFunction;

import java.util.ArrayList;


/**
 * Supports grid-based search problems (for game maps).
 *
 * @author rlawrenc
 */
public class MapSearchProblem extends SearchProblem {
    private final GameMap map;

    public MapSearchProblem(GameMap map) {
        this.map = map;
    }

    public int computeDistance(SearchState start, SearchState goal, HeuristicFunction heuristic) {
        return GameMap.computeDistance(start.id, goal.id, map.cols, heuristic);
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

    public String idToString(int id) {
        return "(" + map.getRow(id) + ", " + map.getCol(id) + ")";
    }

    // QUESTION: Why only 4 directional movement? Why is the move cost 1 and not 10?
    public int getMoveCost(int startId, int goalId) {    // Assumes they are not the same state (as move cost would be zero then)
        return 1; // Update: Only allowing 4 directional movement
    }

    public int getMoveCost(SearchState start, SearchState goal) {
        return getMoveCost(start.id, goal.id);
    }

    public int computeDistance(SearchState start, SearchState goal) {
        return GameMap.computeDistance(start.id, goal.id, map.cols);
    }
}
