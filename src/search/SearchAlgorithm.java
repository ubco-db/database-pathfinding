package search;

import java.util.ArrayList;

public interface SearchAlgorithm {
    ArrayList<SearchState> computePath(SearchState start, SearchState goal, StatsRecord stats);
}
