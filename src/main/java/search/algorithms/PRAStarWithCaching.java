package search.algorithms;

import map.GameMap;
import search.SearchState;
import search.SearchUtil;
import stats.SearchStats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PRAStarWithCaching extends PRAStar {
    private final Map<String, List<SearchState>> cache = new HashMap<>();

    public PRAStarWithCaching(GameMap gameMap, int gridSize) {
        super(gameMap, gridSize);
    }

    @Override
    public List<SearchState> findPath(SearchState start, SearchState goal, SearchStats searchStats) {
        long startTime = System.nanoTime();

        // Get start and goal region ids
        int startRegion = super.getAbstractedMap().getRegionIdFromMap(start.getStateId());
        int goalRegion = super.getAbstractedMap().getRegionIdFromMap(goal.getStateId());

        // If start and goal are the same region, use A* search to find the path rather than PRA*
        if (startRegion == goalRegion) {
            return super.getAStarRefinement().findPath(start, goal, searchStats);
        }

        // Otherwise, perform PRA*, starting with finding the abstract path
        List<SearchState> abstractPath = findAbstractPath(start, goal, searchStats);

        // No path found in the abstract space
        if (abstractPath == null) {
            return null;
        }

//        if (abstractPath.size() == 2) {
//            List<SearchState> path = super.getAStarRefinement().findPath(start, goal, searchStats);
//            long endTime = System.nanoTime();
//            searchStats.setTimeToFindPathOnline(endTime - startTime);
//            searchStats.setPathLength(path.size());
//            return path;
//        }

        // Refine abstract path
        subgoals.clear();
        subgoals.add(abstractPath.getFirst());

        // Find path from given start to first subgoal
        List<SearchState> path = super.getAStarRefinement().findPath(start, subgoals.getFirst(), searchStats);

        List<SearchState> pathFragment;
        for (int i = 1; i < abstractPath.size(); i++) {
            // Find path from current subgoal to next subgoal
            if (i != 1 && i != abstractPath.size() - 1) {
                String key = subgoals.get(i - 1).getStateId() + " " + abstractPath.get(i).getStateId();
                pathFragment = cache.get(key);

                if (pathFragment == null) {
                    pathFragment = super.getAStarRefinement().findPath(subgoals.get(i - 1), abstractPath.get(i), searchStats);
                    cache.put(key, pathFragment);
                }
            } else {
                pathFragment = super.getAStarRefinement().findPath(subgoals.get(i - 1), abstractPath.get(i), searchStats);
            }

            SearchUtil.mergePaths(path, pathFragment);
            subgoals.add(abstractPath.get(i));
        }

        // Find path from final subgoal to given goal
        pathFragment = super.getAStarRefinement().findPath(abstractPath.getLast(), goal, searchStats);
        SearchUtil.mergePaths(path, pathFragment);

        long endTime = System.nanoTime();
        searchStats.setTimeToFindPathOnline(endTime - startTime);
        searchStats.setPathLength(path.size());

        return path;
    }

    public Map<String, List<SearchState>> getCache() {
        return cache;
    }

    @Override
    public void addWall(int wallId) {
        recomputeSector(wallId, true, cache);
    }

    @Override
    public void removeWall(int wallId) {
        recomputeSector(wallId, false, cache);
    }
}
