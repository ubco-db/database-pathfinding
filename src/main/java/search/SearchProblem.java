package search;

import map.GroupRecord;
import util.ExpandArray;
import util.HeuristicFunction;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

/**
 * Generic class for search problems.
 *
 * @author rlawrenc
 */
public abstract class SearchProblem {

    public abstract ArrayList<SearchState> getNeighbors(SearchState state);

    public abstract void getNeighbors(int stateId, ExpandArray neighbors);

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

    public abstract TreeMap<Integer, GroupRecord> getGroups();

    public abstract void computeNeighbors();

    public abstract String idToString(int id);
}
