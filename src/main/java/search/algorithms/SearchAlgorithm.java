package search.algorithms;

import search.SearchState;

import java.util.List;

public interface SearchAlgorithm {
    List<SearchState> findPath(SearchState start, SearchState goal);
}
