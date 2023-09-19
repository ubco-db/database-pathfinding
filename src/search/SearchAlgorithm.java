package search;

import java.util.ArrayList;

public interface SearchAlgorithm {
    public ArrayList<SearchState> computePath(SearchState start, SearchState goal, StatsRecord stats);

    public boolean isPath(SearchState start, SearchState goal, StatsRecord stats);

    public boolean isPath(int startId, int goalId, StatsRecord stats);
}
