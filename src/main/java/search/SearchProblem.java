package search;

import java.util.List;

public abstract class SearchProblem {
    public static final char EDGE_COST_CARDINAL = 10;
    public static final char EDGE_COST_DIAGONAL = 14;

    public abstract void getNeighbours(SearchState currentState, List<SearchState> neighbours);

    public abstract int getNeighbours(SearchState currentState, SearchState[] neighbours);

    public abstract void getNeighbourIds(int currentId, List<Integer> neighbourIds);

    public abstract int getNeighbourIds(int currentId, int[] neighbourIds);

    public abstract int getMoveCost(SearchState start, SearchState goal);

    public abstract int getMoveCost(int startStateId, int goalStateId);

    public abstract int getOctileDistance(int startStateId, int goalStateId);

    public abstract int getMaxSize();
}
