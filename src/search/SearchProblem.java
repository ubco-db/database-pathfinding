package search;

import map.GroupRecord;
import util.ExpandArray;
import util.HeuristicFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Generic class for search problems.
 *
 * @author rlawrenc
 */
public abstract class SearchProblem {
    protected SearchSpace searchSpace;

    public void setSearchSpace(SearchSpace space) {
        this.searchSpace = space;
    }

    public abstract ArrayList<SearchState> getNeighbors(SearchState state);

    public abstract void getNeighbors(int stateId, ExpandArray neighbors);

    public abstract boolean isNeighbor(int fromStateId, int toStateId);

    public abstract int computeDistance(SearchState start, SearchState goal);

    public abstract int computeDistance(SearchState start, SearchState goal, HeuristicFunction heuristic);

    public abstract int computeDistance(int startId, int goalId);

    public abstract int computeDistance(int startId, int goalId, HeuristicFunction heuristic);

    public abstract int getMaxSize();

    public abstract int getMoveCost(SearchState start, SearchState goal);

    public abstract int getMoveCost(int startId, int goalId);

    public abstract void initIterator();

    public abstract boolean nextState(SearchState state);

    public abstract SearchState generateRandomState(Random generator);

    public abstract HashMap<Integer, GroupRecord> getGroups();

    public abstract void computeGroups();

    public abstract void computeNeighbors();

    public abstract String idToString(int id);
}
