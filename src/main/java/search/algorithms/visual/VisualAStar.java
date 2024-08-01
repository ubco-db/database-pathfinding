package search.algorithms.visual;

import search.SearchProblem;
import search.SearchState;
import search.algorithms.AStar;
import search.algorithms.SearchAlgorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * AStar implementation that has lists to keep track of expanded states for visualization purposes.
 */
public class VisualAStar extends AStar implements SearchAlgorithm {

    private final List<SearchState> statesExpanded;
    private final List<List<SearchState>> statesExpandedPerMove;
    private final List<SearchState> currentStates;

    public VisualAStar(SearchProblem searchProblem) {
        super(searchProblem);

        this.statesExpanded = new ArrayList<>();
        this.currentStates = new ArrayList<>();
        this.statesExpandedPerMove = new ArrayList<>();
    }

    @Override
    public List<SearchState> findPath(SearchState start, SearchState goal) {
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

        statesExpanded.clear();

        // While there are nodes in the list
        while (!openList.isEmpty()) {
            statesExpandedPerMove.add(new ArrayList<>(openList));

            // Remove the first node from the queue (node with lowest cost, in case of ties highest g cost/lowest h cost first)
            SearchState current = openList.remove();
            // System.out.println(current);
            currentStates.add(current);

            int currentId = current.getStateId();
            openListLookup.remove(currentId);

            // If the node has already been expanded
            if (closedHashSet.contains(currentId)) {
                continue;
            }

            statesExpanded.add(current);

            // Set the node as already expanded
            closedHashSet.add(currentId);

            // If we have found the goal
            if (currentId == goalId) {
                // Reconstruct the path from the goal to the start (each node points to its parent)
                return reconstructPath(current);
            }

            // Get the neighbours of the current state (will be a maximum of 8)
            Arrays.fill(neighbours, null);
            int numNeighbours = searchProblem.getNeighbours(current, neighbours);

            for (int i = 0; i < numNeighbours; i++) {
                neighbourId = neighbours[i].getStateId();
                // If the neighbour has already been expanded
                if (closedHashSet.contains(neighbourId)) {
                    continue;
                }

                // Compute cost to start
                int newG = current.getG() + searchProblem.getMoveCost(currentId, neighbourId);
                // Estimate cost to goal
                int h = searchProblem.getOctileDistance(neighbourId, goalId);

                // Attempt to get state from open list
                SearchState state = openListLookup.get(neighbourId);

                if (state == null || state.getG() > newG) {
                    // If the state is not in the open list yet, or it is, but we have found a cheaper way of getting to it
                    neighbours[i].updateCost(newG, h);
                    neighbours[i].setParent(current);
                    openList.add(neighbours[i]);
                    openListLookup.put(neighbourId, neighbours[i]);
                }
            }
        }

        // If there are no nodes left to expand, a path must not exist
        return null;
    }

    public List<SearchState> reconstructPath(SearchState goal) {
        List<SearchState> path = new ArrayList<>();

        SearchState current = goal;

        while (current != null) {
            path.add(current);
            current = current.getParent();
        }

        Collections.reverse(path);

        return path;
    }

    public List<SearchState> getCurrentStates() {
        return currentStates;
    }

    public List<List<SearchState>> getStatesExpandedPerMove() {
        return statesExpandedPerMove;
    }

    public List<SearchState> getStatesExpanded() {
        return statesExpanded;
    }
}
