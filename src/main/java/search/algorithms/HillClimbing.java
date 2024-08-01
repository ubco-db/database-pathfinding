package search.algorithms;

import search.MapSearchProblem;
import search.SearchProblem;
import search.SearchState;
import stats.SearchStats;

import java.util.ArrayList;
import java.util.List;

public class HillClimbing implements SearchAlgorithmWithStats, SearchAlgorithm {
    protected final SearchProblem searchProblem;
    protected final int[] neighbourIds;
    protected int nextId;
    protected int nextH;

    /**
     * Because this implementation uses an array to store neighbours, it may not work for RegionSearchProblems
     * @param searchProblem MapSearchProblem containing the map to search
     */
    public HillClimbing(MapSearchProblem searchProblem) {
        this.searchProblem = searchProblem;

        this.neighbourIds = new int[8];
    }

    @Override
    public List<SearchState> findPath(SearchState start, SearchState goal, SearchStats searchStats) {
        // Find path consisting of stateIds
        List<Integer> idPath = findIdPath(start, goal, searchStats);
        // If it's null, no path exists
        if (idPath == null) return null;

        // Turn the id path into a path of SearchStates
//        int pathCost = 0;
        List<SearchState> path = new ArrayList<>();
        for (int i = 0; i < idPath.size(); i++) {
            path.add(new SearchState(idPath.get(i)));
//            if (i < idPath.size() - 1) {
//                pathCost += searchProblem.getMoveCost(idPath.get(i), idPath.get(i + 1));
//            }
        }

        // searchStats.setPathCost(pathCost);
        searchStats.setPathLength(path.size());

        return path;
    }

    @Override
    public List<SearchState> findPath(SearchState start, SearchState goal) {
        // Find path consisting of stateIds
        List<Integer> idPath = findIdPath(start, goal, null);
        // If it's null, no path exists
        if (idPath == null) return null;

        // Turn the id path into a path of SearchStates
        List<SearchState> path = new ArrayList<>();
        for (int id : idPath) {
            path.add(new SearchState(id));
        }
        return path;
    }

    public boolean pathExists(int currentId, int goalId, SearchStats searchStats) {
        int currentH = searchProblem.getOctileDistance(currentId, goalId);

        while (true) {
            // If we have reached the goal, path has been found
            if (currentId == goalId) {
                return true;
            }

            // Update SearchStats
            // TODO: Should this count as an expansion at all?
            searchStats.incrementNumStatesExpandedHCCompression(1);

            // Grab ids of neighbours of the state
            int numNeighbours = searchProblem.getNeighbourIds(currentId, neighbourIds);

            // If there are none, no path can be found
            if (numNeighbours == 0) return false;

            // Update nextId and nextH
            exploreNeighbors(neighbourIds, numNeighbours, goalId, currentId);

            // We must have reached a plateau, no path can be found
            if (nextH >= currentH) {
                return false;
            }

            currentId = nextId;
            currentH = nextH;
        }
    }

    List<Integer> findIdPath(SearchState start, SearchState goal, SearchStats searchStats) {
        List<Integer> idPath = new ArrayList<>();

        int goalId = goal.getStateId();
        int currentId = start.getStateId();
        int currentH = searchProblem.getOctileDistance(currentId, goalId);

        while (true) {
            // Add current id to path
            idPath.add(currentId);

            // Update SearchStats
            if (searchStats != null) searchStats.incrementNumStatesExpandedHC(1);

            // If we have reached the goal, return the path
            if (currentId == goal.getStateId()) {
                return idPath;
            }

            // Grab ids of neighbours of the state
            int numNeighbours = searchProblem.getNeighbourIds(currentId, neighbourIds);

            // If there are none, no path can be found
            if (numNeighbours == 0) return null;

            // Update nextId and nextH
            exploreNeighbors(neighbourIds, numNeighbours, goalId, currentId);

            // We must have reached a plateau, no path can be found
            if (nextH >= currentH) {
                return null;
            }

            // Update currentId, updated currentH so we can detect plateaus
            currentId = nextId;
            currentH = nextH;
        }
    }

    private void exploreNeighbors(int[] neighborIds, int numNeighbours, int goalId, int currentId) {
        // Find next id to go to, initialize it to be the first neighbour
        nextId = neighborIds[0];
        nextH = searchProblem.getOctileDistance(nextId, goalId);
        int nextMoveCost = searchProblem.getMoveCost(currentId, nextId);
        int bestCost = nextMoveCost + nextH;

        // Iterate over neighbours, if we can find one with lower cost or better moveCost, set next to be that one
        // instead
        for (int i = 1; i < numNeighbours; i++) {
            int neighbourH = searchProblem.getOctileDistance(neighborIds[i], goalId);
            int neighborMoveCost = searchProblem.getMoveCost(currentId, neighborIds[i]);
            int cost = neighborMoveCost + neighbourH;

            if (cost < bestCost || (cost == bestCost && neighborMoveCost > nextMoveCost)) {
                nextId = neighborIds[i];
                nextH = neighbourH;
                nextMoveCost = neighborMoveCost;
                bestCost = cost;
            }
        }
    }
}
