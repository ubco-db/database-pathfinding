package search;

import map.AbstractedMap;
import map.GameMap;
import search.algorithms.CompressAStar;
import search.algorithms.HillClimbing;
import search.algorithms.HillClimbingWithClosedSet;
import stats.SearchStats;

import java.util.List;

public class SearchUtil {

    private SearchUtil() throws Exception {
        throw new Exception("This is a utility class and should not be instantiated.");
    }

    public static void removeLastN(List<SearchState> path, int statesToRemove) {
        for (int i = 0; i < statesToRemove; i++) {
            path.removeLast();
        }
    }

    public static void mergePaths(List<SearchState> path1, List<SearchState> path2) {
        if (!path1.isEmpty()) {
            path1.removeLast();
        }
        path1.addAll(path2);
    }

    public static int findInArray(int[] arr, int key) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == key) {
                return i;
            }
        }
        return -1;
    }

    public static int findPathCost(List<SearchState> path, SearchProblem problem) {
        int cost = 0;
        SearchState last = path.getLast();
        for (int i = path.size() - 1; i >= 0; i--) {
            SearchState current = path.get(i);
            cost += problem.getMoveCost(last, current);
            last = current;
        }

        return cost;
    }

    public static int[] computeSubgoalsBinaryByIds(int[] path, HillClimbing hillClimbing, int[] tmp, int pathSize, SearchStats searchStats) {
        int current, startIndex = 0, best = startIndex + 1;
        int endIndex = pathSize - 1;
        int currentGoalId, currentStartId = path[startIndex];
        int count = 0;

        while (startIndex < pathSize - 1) {
            currentGoalId = path[endIndex];
            if (hillClimbing.pathExists(currentStartId, currentGoalId, searchStats)) break;

            while (startIndex <= endIndex) {
                current = (startIndex + endIndex) / 2;                  // Get mid-point
                // Determine if you can reach this point from the current start using HC
                currentGoalId = path[current];
                if (!hillClimbing.pathExists(currentStartId, currentGoalId, searchStats))
                    endIndex = current - 1;
                else {    // Can HC reach to here from current start, but can we do better?
                    if (current != startIndex) best = current;
                    startIndex = current + 1;
                }
            }
            // System.out.println(path[best]+" Goal: "+currentGoalId+" Last path entry: "+path[pathSize-1]);
            // Save current best as a subgoal
            tmp[count++] = path[best];
            currentStartId = path[best];
            startIndex = best;
            best = startIndex + 1;
            endIndex = pathSize - 1;
        }

        if (count == 0) return null;
        int[] result = new int[count];
        System.arraycopy(tmp, 0, result, 0, count);
        return result;
    }

    public static int[] findCompressedPath(List<SearchState> path, HillClimbing hillClimbing, SearchStats searchStats) {
        int start = path.getFirst().getStateId();
        int goal = path.getLast().getStateId();

        // If it is possible to hill climb directly from start to goal, simply return {start, goal} as the compressed path
        if (hillClimbing.pathExists(start, goal, searchStats)) {
            return new int[]{start, goal};
        }

        // Else, perform a binary search on the path
        int lowIdx = 0, midIdx, highIdx = path.size() - 1;
        int bestIdx = lowIdx + 1;

        int[] tmp = new int[path.size() + 1];
        int count = 0;
        tmp[count++] = start;

        while (true) {
            goal = path.get(highIdx).getStateId();
            if (hillClimbing.pathExists(start, goal, searchStats)) {
                break;
            }

            while (lowIdx <= highIdx) {
                midIdx = lowIdx + (highIdx - lowIdx) / 2; //
                goal = path.get(midIdx).getStateId();
                if (!hillClimbing.pathExists(start, goal, searchStats)) {
                    highIdx = midIdx - 1;
                } else {
                    if (midIdx != lowIdx) {
                        bestIdx = midIdx;
                    }
                    lowIdx = midIdx + 1;
                }
            }
            tmp[count++] = path.get(bestIdx).getStateId();
            start = path.get(bestIdx).getStateId();
            lowIdx = bestIdx;
            bestIdx = lowIdx + 1;
            highIdx = path.size() - 1;
        }

        // Add end to path
        tmp[count++] = path.getLast().getStateId();

        // Copy tmp into compressed path of length count
        int[] compressedPath = new int[count];
        System.arraycopy(tmp, 0, compressedPath, 0, count);

        return compressedPath;
    }

    public static int[] findOptimallyCompressedPath(List<SearchState> optimalPath, HillClimbingWithClosedSet hc, CompressAStar compressAStar, SearchStats searchStats) {
        int currentStart = optimalPath.getFirst().getStateId();
        int currentGoal = optimalPath.getLast().getStateId();

        // If it is possible to hill climb directly from start to goal, simply return {start, goal} as the compressed path
        if (compressAStar.pathSizeEqualsClosedSetSize(optimalPath.size())) {
            return new int[]{currentStart, currentGoal};
        }

        // Else, perform a binary search on the path
        int startIdx = 0, endIdx = optimalPath.size() - 1;
        int currentIdx, bestIdx = startIdx + 1;

        int[] tmp = new int[optimalPath.size() + 1];
        int count = 0;

        // Add start to path
        tmp[count++] = currentStart;

        int currentStartIdx = startIdx;

        while (true) {
            currentGoal = optimalPath.get(endIdx).getStateId();

            // If we can hill-climb from start to goal while staying on the path
            // TODO: Can we use some information we gain here below?
            if (hc.pathExistsAndFollowsOptimal(currentStart, currentGoal, currentStartIdx, optimalPath, searchStats)) {
                break;
            }

            while (startIdx <= endIdx) {
                currentIdx = startIdx + (endIdx - startIdx) / 2; // Find midpoint
                currentGoal = optimalPath.get(currentIdx).getStateId();

                // If we can't hill-climb from current start to current goal while staying on the path
                if (!hc.pathExistsAndFollowsOptimal(currentStart, currentGoal, currentStartIdx, optimalPath, searchStats)) {
                    endIdx = currentIdx - 1;
                } else {
                    bestIdx = currentIdx;
                    startIdx = currentIdx + 1;
                }
            }

            // Add furthest hc-reachable state (best) to list of subgoals
            tmp[count++] = optimalPath.get(bestIdx).getStateId();

            // Setup for next iteration, now finding path from subgoal to goal
            currentStart = optimalPath.get(bestIdx).getStateId();
            startIdx = bestIdx + 1;
            currentStartIdx = bestIdx;
            // Increment best index (otherwise if can't hill-climb between neighbours same state will keep being added as subgoal)
            bestIdx++;
            endIdx = optimalPath.size() - 1;
        }

        // Add end to path
        tmp[count++] = optimalPath.getLast().getStateId();

        // Copy tmp into compressed path of length count
        int[] compressedPath = new int[count];
        System.arraycopy(tmp, 0, compressedPath, 0, count);

        return compressedPath;
    }

    public static void printPath(List<SearchState> path) {
        for (SearchState state : path) {
            System.out.print(state.getStateId() + ", ");
        }
        System.out.println();
    }

    public static boolean isContinuousPath(List<SearchState> path, MapSearchProblem mapSearchProblem) {
        SearchState prev = path.getFirst();
        for (SearchState state : path) {
            if (state.equals(prev)) {
                continue;
            }
            if (!mapSearchProblem.isAdjacent(prev, state)) {
                return false;
            }
            prev = state;
        }
        return true;
    }

    public static void placeWall(int wallId, GameMap gameMap, AbstractedMap abstractedMap) {
        boolean previousWall = gameMap.isWall(wallId) && abstractedMap.isWall(wallId);

        if (previousWall) {
            throw new RuntimeException("There is a wall at " + wallId + " already!");
        }

        gameMap.placeWallAt(wallId);
        abstractedMap.placeWallAt(wallId);
    }

    public static void placeOpenState(int wallId, GameMap gameMap, AbstractedMap abstractedMap) {
        boolean previousWall = gameMap.isWall(wallId) && abstractedMap.isWall(wallId);

        if (!previousWall) {
            throw new RuntimeException("There isn't a wall at " + wallId + " to remove!");
        }

        gameMap.placeOpenStateAt(wallId);
        abstractedMap.placeOpenStateAt(wallId);
    }
}
