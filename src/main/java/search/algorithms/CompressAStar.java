package search.algorithms;

import search.MapSearchProblem;
import search.SearchProblem;
import search.SearchState;
import stats.SearchStats;

import java.util.ArrayList;
import java.util.List;

public class CompressAStar extends AStar {
    private int checkTriggeredCount;
    private boolean checkTriggered;

    public CompressAStar(SearchProblem searchProblem) {
        super(searchProblem);

        this.checkTriggeredCount = 0;
        this.checkTriggered = false;
    }

    public CompressAStar(AStar aStar) {
        super(aStar.getSearchProblem());

        this.checkTriggeredCount = 0;
        this.checkTriggered = false;
    }

    /* PERFECT COMPRESSION */

    /**
     * This method perfectly compresses an A* path into a series of hill-climbable subgoals.
     * Perfectly, because hill-climbing between the subgoals returns the original, optimal path again.
     * The compressed path does not include start and goal of the optimal path.
     *
     * @return a series of hill-climbable subgoals such that hill-climbing between them will return the optimal path
     */
    public List<SearchState> findCompressedPath(List<SearchState> optimalPath, HillClimbingWithClosedSet hc, SearchStats searchStats, boolean runCheck) {
        ArrayList<SearchState> subgoals = new ArrayList<>();

        // Compress path into a series of hill-climbing reachable subgoals
        int startIdx = 0, endIdx = optimalPath.size() - 1;
        int currentIdx, bestIdx = startIdx + 1;
        SearchState currentStart = optimalPath.get(startIdx), currentGoal;

        int currentStartIdx = startIdx;

        // Path should be hill-climbable if the size of the optimal path equals that of the closed set
        if (runCheck && pathSizeEqualsClosedSetSize(optimalPath.size())) {
            checkTriggeredCount++;
            checkTriggered = true;
            return subgoals;
        }

        checkTriggered = false;

        // Add start of path to list of subgoals
//        subgoals.add(currentStart);

        while (startIdx < optimalPath.size() - 1) {
            currentGoal = optimalPath.get(endIdx);

            // If we can hill-climb from start to goal while staying on the path
            // TODO: Can we use some information we gain here below?
            if (hc.pathExistsAndFollowsOptimal(currentStart.getStateId(), currentGoal.getStateId(), currentStartIdx, optimalPath, searchStats)) {
                break;
            }

            while (startIdx < endIdx) {
                currentIdx = (startIdx + endIdx) / 2; // Find midpoint
                currentGoal = optimalPath.get(currentIdx);

                // If we can't hill-climb from current start to current goal while staying on the path
                if (!hc.pathExistsAndFollowsOptimal(currentStart.getStateId(), currentGoal.getStateId(), currentStartIdx, optimalPath, searchStats)) {
                    endIdx = currentIdx - 1;
                } else {
                    bestIdx = currentIdx;
                    startIdx = currentIdx + 1;
                }
            }

            // Add furthest hc-reachable state (best) to list of subgoals
            SearchState bestState = optimalPath.get(bestIdx);
            subgoals.add(bestState);

            // Setup for next iteration, now finding path from subgoal to goal
            currentStart = bestState;
            startIdx = bestIdx + 1;
            currentStartIdx = bestIdx;
            // Increment best index (otherwise if can't hill-climb between neighbours same state will keep being added as subgoal)
            bestIdx++;
            endIdx = optimalPath.size() - 1;
        }

        // Add last state (goal) of path to list of subgoals
//        subgoals.add(path.get(endIdx));

        return subgoals;
    }

    public boolean pathSizeEqualsClosedSetSize(int pathSize) {
        return pathSize == getClosedHashSet().size();
    }

    public int getCheckTriggeredCount() {
        return checkTriggeredCount;
    }

    public boolean isCheckTriggered() {
        return checkTriggered;
    }

    /* WALL COMPRESSION */

    public List<SearchState> findWallCompressedPath(SearchState start, SearchState goal, List<SearchState> optimalPath) {
        List<SearchState> subgoals = new ArrayList<>();

        for (SearchState state : optimalPath) {
            // Don't add start and goal as subgoals
            if (state.equals(start) || state.equals(goal)) {
                continue;
            }
            // Make state a subgoal if it touches a wall
            if (isTouchingWall(state)) {
                subgoals.add(state);
            }
        }

        return subgoals;
    }

    private boolean isTouchingWall(SearchState state) {
        return ((MapSearchProblem) getSearchProblem()).isTouchingWall(state);
    }
}
