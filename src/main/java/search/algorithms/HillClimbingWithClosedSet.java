package search.algorithms;

import search.MapSearchProblem;
import search.SearchState;
import stats.SearchStats;

import java.util.*;

/**
 * This implementation is a mix between classical, greedy hill-climbing and A* search.
 * Like A* search, it keeps track of previously visited states in a closed set.
 * Unlike A* search, it does not have an open set to keep track of promising states, but instead greedily picks the next
 * state to be the one with the lowest cost (in case of ties, lowest h-value/highest g-value)
 */
public class HillClimbingWithClosedSet extends HillClimbing {
    private final HashSet<Integer> closedSet;

    /**
     * Because this implementation uses an array to store neighbours, it may not work for RegionSearchProblems
     * @param searchProblem MapSearchProblem containing the map to search
     */
    public HillClimbingWithClosedSet(MapSearchProblem searchProblem) {
        super(searchProblem);

        this.closedSet = new HashSet<>();
    }

    @Override
    List<Integer> findIdPath(SearchState start, SearchState goal, SearchStats searchStats) {
        List<Integer> idPath = new ArrayList<>();

        closedSet.clear();

        int goalId = goal.getStateId();
        int currentId = start.getStateId();

        while (true) {
            // Add current id to path
            idPath.add(currentId);

            // Update SearchStats
            if (searchStats != null) searchStats.incrementNumStatesExpandedHC(1);

            closedSet.add(currentId);

            // If we have reached the goal, return the path
            if (currentId == goal.getStateId()) {
                return idPath;
            }

            // Grab ids of neighbours of the state
            int numNeighbours = ((MapSearchProblem) searchProblem).getNeighbourIds(currentId, neighbourIds, closedSet);

            // If there are none, no path can be found
            if (numNeighbours == 0) {
                return null;
            }

            // Update nextId and nextH
            exploreNeighbors(neighbourIds, numNeighbours, goalId, currentId);

            // Update currentId, updated currentH so we can detect plateaus
            currentId = nextId;
        }
    }

    /**
     * Verifies that it is possible to hill-climb from currentId to goalId while staying on the optimal path
     *
     * @param currentId   id of current SearchState
     * @param goalId      id of goal SearchState
     * @param currentIdx  current index on optimal path
     * @param optimalPath the A* path leading to goal id (currentId is on the path, at current index)
     * @return true if it is possible to hill-climb from currentId to goalId while staying on the optimalPath
     */
    public boolean pathExistsAndFollowsOptimal(int currentId, int goalId, int currentIdx, List<SearchState> optimalPath, SearchStats searchStats) {
        closedSet.clear();

        while (true) {
            // If we have reached the goal, path has been found
            if (currentId == goalId) {
                return true;
            }

            // TODO: Should this count as an expansion at all?
            if (searchStats != null) searchStats.incrementNumStatesExpandedHCCompression(1);

            // Get next id on optimal path
            int optimalPathId = optimalPath.get(currentIdx).getStateId();
            // If we have strayed from the optimal path
            if (currentId != optimalPathId) {
                return false;
            }

            closedSet.add(currentId);

            // Grab ids of neighbours of the state
            int numNeighbours = ((MapSearchProblem) searchProblem).getNeighbourIds(currentId, neighbourIds, closedSet);

            // If there are none, no path can be found
            if (numNeighbours == 0) return false;

            // Update nextId
            exploreNeighbors(neighbourIds, numNeighbours, goalId, currentId);

            // Setup for next iteration
            currentId = nextId;
            currentIdx++;
        }
    }

    private void exploreNeighbors(int[] neighborIds, int numNeighbours, int goalId, int currentId) {
        // Find next id to go to, initialize it to be the first neighbour
        nextId = neighborIds[0];
        int nextH = searchProblem.getOctileDistance(nextId, goalId);
        int nextMoveCost = searchProblem.getMoveCost(currentId, nextId);
        int bestCost = nextMoveCost + nextH;

        // Iterate over neighbours, if we can find one with lower cost or better moveCost, set next to be that one
        // instead
        for (int j = 1; j < numNeighbours; j++) {
            int neighbourH = searchProblem.getOctileDistance(neighborIds[j], goalId);
            int neighborMoveCost = searchProblem.getMoveCost(currentId, neighborIds[j]);
            int cost = neighborMoveCost + neighbourH;

            if (cost < bestCost || (cost == bestCost && neighborMoveCost > nextMoveCost)) {
                nextId = neighborIds[j];
                nextMoveCost = neighborMoveCost;
                bestCost = cost;
            }
        }
    }
}
