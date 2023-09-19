package search;

public interface SearchAbstractAlgorithm extends SearchAlgorithm {
	/*
	 * Returns -1 if no path found otherwise cost (or # of states expanded/cutoff) of path from start to goal as appropriate to algorithm.
	 */
	public int isPath(int startId, int goalId, StatsRecord stats, SavedSearch database);
}
