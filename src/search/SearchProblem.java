package search;

import util.HeuristicFunction;

import java.util.ArrayList;

/**
 * Generic class for search problems.
 *
 * @author rlawrenc
 */
public abstract class SearchProblem {

    public abstract ArrayList<SearchState> getNeighbors(SearchState state);

    public abstract int computeDistance(SearchState start, SearchState goal);

    public abstract int computeDistance(SearchState start, SearchState goal, HeuristicFunction heuristic);

    public abstract int getMaxSize();

    public abstract int getMoveCost(SearchState start, SearchState goal);

    public abstract int getMoveCost(int startId, int goalId);

    public abstract String idToString(int id);
}
