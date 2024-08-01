package search.algorithms;

import search.RegionSearchProblem;
import search.SearchProblem;
import search.SearchState;
import stats.SearchStats;

import java.util.ArrayList;
import java.util.List;

/**
 * This implementation uses a List to store neighbours rather than an array. This is necessary for RegionSearchProblems
 * since Regions may have more than eight neighbours (states never will).
 */
public class ListAStar extends AStar {
    private final List<SearchState> neighbours;

    public ListAStar(SearchProblem searchProblem) {
        super(searchProblem);

        this.neighbours = new ArrayList<>(8);
    }

    /**
     * Finds an optimal path from start to goal using A* search
     *
     * @param start       start of path
     * @param goal        goal of path
     * @param searchStats stats object to keep track of states expanded
     * @return optimal path between start and goal found using A* search
     */
    @Override
    public List<SearchState> findPath(SearchState start, SearchState goal, SearchStats searchStats) {
        if (searchStats == null) searchStats = new SearchStats();

        // Clear lists
        openList.clear();
        openListLookup.clear();
        closedHashSet.clear();

        start.setParent(null);
        start.setCost(0);

        int startId = start.getStateId(), goalId = goal.getStateId(), neighbourId;

        // Add the first node to the open list
        openList.add(start);
        openListLookup.put(startId, start);

        boolean isAbstract = searchProblem instanceof RegionSearchProblem;

        // While there are nodes in the list
        while (!openList.isEmpty()) {
            // Remove the first node from the queue (node with lowest cost, in case of ties highest g cost/lowest h cost first)
            SearchState current = openList.remove();
            // System.out.println(current);

            int currentId = current.getStateId();
            openListLookup.remove(currentId);

            // If the node has already been expanded
            if (closedHashSet.contains(currentId)) {
                continue;
            }

            if (isAbstract) {
                searchStats.incrementNumAbstractStatesExpanded(1);
            } else {
                searchStats.incrementNumStatesExpanded(1);
            }

            // Set the node as already expanded
            closedHashSet.add(currentId);

            // If we have found the goal
            if (currentId == goalId) {
                // Reconstruct the path from the goal to the start (each node points to its parent)
                return reconstructPath(current, searchStats);
            }

            // Get the neighbours of the current state (will be a maximum of 8)
            neighbours.clear();
            searchProblem.getNeighbours(current, neighbours);

            for (SearchState neighbour : neighbours) {
                neighbourId = neighbour.getStateId();

                // If the neighbour has already been expanded
                if (closedHashSet.contains(neighbourId)) {
                    continue;
                }

                if (isAbstract) {
                    searchStats.incrementNumAbstractStatesUpdated(1);
                } else {
                    searchStats.incrementNumStatesUpdated(1);
                }

                // Compute cost to start
                int newG = current.getG() + searchProblem.getMoveCost(currentId, neighbourId);
                // Estimate cost to goal
                int h = searchProblem.getOctileDistance(neighbourId, goalId);

                // Attempt to get state from open list
                SearchState state = openListLookup.get(neighbourId);

                if (state == null || state.getG() > newG) {
                    // If the state is not in the open list yet, or it is, but we have found a cheaper way of getting to it
                    neighbour.updateCost(newG, h);
                    neighbour.setParent(current);
                    openList.add(neighbour);
                    openListLookup.put(neighbourId, neighbour);
                }
            }
        }

        // If there are no nodes left to expand, a path must not exist
        return null;
    }
}
