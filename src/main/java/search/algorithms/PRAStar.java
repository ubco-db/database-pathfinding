package search.algorithms;

import map.AbstractedMap;
import map.GameMap;
import search.MapSearchProblem;
import search.RegionSearchProblem;
import search.SearchState;
import search.SearchUtil;
import stats.SearchStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements Dragon Age version of PRA*.
 *
 * @author rlawrenc
 */
public class PRAStar implements DynamicSearchAlgorithm {
    private final GameMap gameMap;
    private final AbstractedMap abstractedMap;
    private final RegionSearchProblem regionSearchProblem;
    private final ListAStar aStar;
    private final AStar aStarRefinement;

    private final SearchStats searchStats;

    List<SearchState> subgoals;

    public PRAStar(GameMap gameMap, int gridSize) {
        this.searchStats = new SearchStats();

        this.gameMap = gameMap;
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        this.abstractedMap = new AbstractedMap(gameMap, gridSize, searchStats);
        this.regionSearchProblem = new RegionSearchProblem(abstractedMap);

        this.aStarRefinement = new AStar(mapSearchProblem);
        this.aStar = new ListAStar(regionSearchProblem);
        this.subgoals = new ArrayList<>();
    }

    public List<SearchState> findAbstractPath(SearchState start, SearchState goal, SearchStats searchStats) {
        // Find start and goal region representatives
        SearchState startRegionRep = regionSearchProblem.findRegionRepresentativeFromMap(start);
        SearchState goalRegionRep = regionSearchProblem.findRegionRepresentativeFromMap(goal);

        /*
         "The next step of any pathfinding process is to use A* (Hart, Nilsson, & Raphael 1968) to find a path through
         abstract space. We use the octile-distance between region centers both as our heuristic value, and as the
         g-cost of abstract edges."
         */

        // Compute abstract path between two region centers
        return aStar.findPath(startRegionRep, goalRegionRep, searchStats);
    }

    /**
     * This version does the basic R(1,-,-,-) as described in the paper. Just merge paths by going to the next abstract state.
     */
    @Override
    public List<SearchState> findPath(SearchState start, SearchState goal, SearchStats searchStats) {
        long startTime = System.nanoTime();

        // Get start and goal region ids
        int startRegion = abstractedMap.getRegionIdFromMap(start.getStateId());
        int goalRegion = abstractedMap.getRegionIdFromMap(goal.getStateId());

        // If start and goal are the same region, use A* search to find the path rather than PRA*
        if (startRegion == goalRegion) {
            return aStarRefinement.findPath(start, goal, searchStats);
        }

        // Otherwise, perform PRA*, starting with finding the abstract path
        List<SearchState> abstractPath = findAbstractPath(start, goal, searchStats);

        // No path found in the abstract space
        if (abstractPath == null) {
            return null;
        }

//        if (abstractPath.size() == 2) {
//            List<SearchState> path = aStarRefinement.findPath(start, goal, searchStats);
//            long endTime = System.nanoTime();
//            searchStats.setTimeToFindPathOnline(endTime - startTime);
//            searchStats.setPathLength(path.size());
//            return path;
//        }

        // Refine abstract path
        subgoals.clear();
        subgoals.add(abstractPath.getFirst());

        // Find path from given start to first subgoal
        List<SearchState> path = aStarRefinement.findPath(start, subgoals.getFirst(), searchStats);

        List<SearchState> pathFragment;
        for (int i = 1; i < abstractPath.size(); i++) {
            // Find path from current subgoal to next subgoal
            pathFragment = aStarRefinement.findPath(subgoals.get(i - 1), abstractPath.get(i), searchStats);
            SearchUtil.mergePaths(path, pathFragment);
            subgoals.add(abstractPath.get(i));
        }

        // Find path from final subgoal to given goal
        pathFragment = aStarRefinement.findPath(abstractPath.getLast(), goal, searchStats);
        SearchUtil.mergePaths(path, pathFragment);

        long endTime = System.nanoTime();
        searchStats.setTimeToFindPathOnline(endTime - startTime);
        searchStats.setPathLength(path.size());

        return path;
    }

    /**
     * This version does the basic R(1,10%,-,-) as described in the paper.  Build path by going to next center then removing 10% of the path then planning to the next center.
     */
    public List<SearchState> findPathWithOptimization(SearchState start, SearchState goal, double percentage, SearchStats searchStats) {
        if (percentage < 0 || percentage > 1) {
            throw new IllegalArgumentException("Percentage must be between 0 and 1");
        }

        // Get start and goal region ids
        int startRegion = abstractedMap.getRegionIdFromMap(start.getStateId());
        int goalRegion = abstractedMap.getRegionIdFromMap(goal.getStateId());

        // If start and goal are the same region, use A* search to find the path rather than PRA*
        if (startRegion == goalRegion) {
            return aStarRefinement.findPath(start, goal, searchStats);
        }

        // Otherwise, perform PRA*, starting with finding the abstract path
        List<SearchState> abstractPath = findAbstractPath(start, goal, searchStats);

        // No path found in the abstract space
        if (abstractPath == null) {
            return null;
        }

        int statesToRemove;

        // Refine abstract path
        subgoals = new ArrayList<>();
        SearchState current = abstractPath.getFirst();
        subgoals.add(current);
        // Find path from given start to first subgoal
        List<SearchState> path = aStarRefinement.findPath(start, current, searchStats);

        statesToRemove = (int) (path.size() * percentage);
        SearchUtil.removeLastN(path, statesToRemove);

        List<SearchState> pathFragment = null;
        for (int i = 1; i < abstractPath.size(); i++) {
            // Find path from current subgoal to next subgoal
            pathFragment = aStarRefinement.findPath(current, abstractPath.get(i), searchStats);

            // Remove last 10%
            statesToRemove = (int) (pathFragment.size() * percentage);
            SearchUtil.removeLastN(pathFragment, statesToRemove);

            SearchUtil.mergePaths(path, pathFragment);
            current = pathFragment.getLast();
            subgoals.add(abstractPath.get(i));
        }

        if (pathFragment == null) {
            throw new RuntimeException("Path fragment null");
        }

        // Find path from final subgoal to given goal
        pathFragment = aStarRefinement.findPath(pathFragment.getLast(), goal, searchStats);
        SearchUtil.mergePaths(path, pathFragment);

        return path;
    }

    public List<SearchState> getSubgoals() {
        return subgoals;
    }

    public void addWall(int wallId) {
        recomputeSector(wallId, true, null);
    }

    public void removeWall(int wallId) {
        recomputeSector(wallId, false, null);
    }

    protected void recomputeSector(int wallId, boolean addition, Map<String, ?> cache) {
        // Get sector id from wall id
        int sectorId = abstractedMap.getSectorId(wallId);

        // Get sector bounds from sector id
        int northRow = abstractedMap.getStartRowOfSector(sectorId);
        int southRow = abstractedMap.getEndRowOfSector(sectorId);
        int westCol = abstractedMap.getStartColOfSector(sectorId);
        int eastCol = abstractedMap.getEndColOfSector(sectorId);

        // Wipe sector on the abstract map
        abstractedMap.wipeSector(northRow, southRow, westCol, eastCol, cache);

        if (addition) {
            SearchUtil.placeWall(wallId, gameMap, abstractedMap);
        } else {
            SearchUtil.placeOpenState(wallId, gameMap, abstractedMap);
        }

        // Re-abstract sector on the abstract map
        abstractedMap.abstractStatesToGenerateRegions(sectorId, northRow, southRow, westCol, eastCol);

        // Re-compute neighbourhood
        abstractedMap.computeRegionNeighbourhoodAndStoreRegionReps(northRow, southRow, westCol, eastCol);
    }

    public AbstractedMap getAbstractedMap() {
        return abstractedMap;
    }

    public AStar getAStarRefinement() {
        return aStarRefinement;
    }

    public void printSubgoals() {
        System.out.println("Subgoals: \n");
        for (SearchState s : subgoals) {
            System.out.println(s.getStateId());
        }
    }

    @Override
    public GameMap getGameMap() {
        return gameMap;
    }

    public SearchStats getSearchStats() {
        return searchStats;
    }
}
