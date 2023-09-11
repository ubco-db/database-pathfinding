package search;

import java.util.Arrays;

/**
 * Stores statistics on search algorithm.
 *
 * @author rlawrenc
 */
public class StatsRecord {
    private int statesExpandedHC;            // States expanded during hill climbing
    private long statesExpanded;                // States expanded during LRTA*/A* or other search algorithm (does not include HC states if applicable)
    private long statesUpdated;                // States updated during LRTA*/A* or other search algorithm

    private int pathLength;                    // Path length as found by algorithm
    private int pathCost;                    // Cost of path found
    private long time;                        // Time in milliseconds
    // private long maxTime;					// Maximum move time in nanoseconds

    private long revisits;                    // Number of state revisits
    private final long[] moveTimes = new long[100];
    private int subgoals;

    public void setRevisits(long revisits) {
        this.revisits = revisits;
    }

    private int closedListSize;                // Maximum size of closed list
    private int openListSize;                // Maximum size of open list
    private int maxMemSize;                    // Maximum size of closed/open list together

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public StatsRecord() {
        clear();
    }

    public int getStatesExpandedHC() {
        return statesExpandedHC;
    }

    public long getStatesExpanded() {
        return statesExpanded;
    }

    public int getPathLength() {
        return pathLength;
    }

    public void setPathLength(int pathLength) {
        this.pathLength = pathLength;
    }

    public void incrementStatesExpandedHC(int num) {
        statesExpandedHC += num;
    }

    public void incrementStatesExpanded(long num) {
        statesExpanded += num;
    }

    public void incrementStatesUpdated(long num) {
        statesUpdated += num;
    }

    public long getStatesUpdated() {
        return statesUpdated;
    }

    public String toString() {
        return "Path cost: " +
                pathCost +
                "\tPath length: " +
                pathLength +
                "\nStates HC: " +
                statesExpandedHC +
                "\t States Expanded: " +
                statesExpanded +
                "\tStates updated: " +
                statesUpdated +
                "\tTime: " +
                time +
                "\tMaxTime: " +
                moveTimes[0] +
                //	buf.append(maxTime);
                "\tRevisits: " +
                revisits;
    }

    public void clear() {
        statesExpandedHC = 0;
        statesExpanded = 0;
        pathLength = 0;
        pathCost = 0;
        statesUpdated = 0;
        closedListSize = 0;
        openListSize = 0;
        //	maxTime = 0;
        revisits = 0;
        Arrays.fill(moveTimes, 0);
    }


    public int getPathCost() {
        return pathCost;
    }

    public void setPathCost(int pathCost) {
        this.pathCost = pathCost;
    }

    public void setClosedListSize(int closedListSize) {
        this.closedListSize = closedListSize;
    }

    public int getClosedListSize() {
        return closedListSize;
    }

    public void setOpenListSize(int openListSize) {
        this.openListSize = openListSize;
    }

    public int getOpenListSize() {
        return openListSize;
    }

    public void setMaxMemSize(int maxMemSize) {
        this.maxMemSize = maxMemSize;
    }

    public int getMaxMemSize() {
        return maxMemSize;
    }

    public int getSubgoals() {
        return subgoals;
    }

    public void setSubgoals(int subgoals) {
        this.subgoals = subgoals;
    }
}
