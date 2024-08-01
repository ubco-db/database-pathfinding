package search.algorithms;

import search.SearchState;
import stats.SearchStats;

import java.util.List;

public interface SearchAlgorithmWithStats {
    List<SearchState> findPath(SearchState start, SearchState goal, SearchStats searchStats);
}
