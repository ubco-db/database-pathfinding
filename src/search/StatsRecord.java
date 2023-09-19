package search;

import java.io.PrintWriter;
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
    private long[] moveTimes = new long[100];
    private int arrayCount = 0;                // Number of moves in the array
    private int moveCount = 0;                // Number of moves tracked
    private int[] maxMoveNum = new int[100];    // Track the top most expensive 100 moves
    private int subgoals;

    public long getRevisits() {
        return revisits;
    }

    public void setRevisits(long revisits) {
        this.revisits = revisits;
    }

    private int closedListSize;                // Maximum size of closed list
    private int openListSize;                // Maximum size of open list
    private int maxMemSize;                    // Maximum size of closed/open list together

    private int count;                        // A generic count that can be used in different ways by algorithms

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public StatsRecord() {
        clear();
    }

    public StatsRecord(StatsRecord st) {
        statesExpandedHC = st.statesExpandedHC;
        statesExpanded = st.statesExpanded;
        statesUpdated = st.statesUpdated;
        pathLength = st.pathLength;
        pathCost = st.pathCost;
        time = st.time;
        closedListSize = st.closedListSize;
        openListSize = st.openListSize;
        //	maxTime = st.maxTime;
        revisits = st.revisits;
        moveTimes = st.moveTimes;
        arrayCount = st.arrayCount;
        maxMoveNum = st.maxMoveNum;
    }

    public StatsRecord computeDiff(StatsRecord st) {
        StatsRecord rec = new StatsRecord();
        rec.statesExpandedHC = this.statesExpandedHC - st.statesExpandedHC;
        rec.statesExpanded = this.statesExpanded - st.statesExpanded;
        rec.statesUpdated = this.statesUpdated - st.statesUpdated;
        rec.pathLength = this.pathLength - st.pathLength;
        rec.pathCost = this.pathCost - st.pathCost;
        rec.time = this.time - st.time;
        rec.closedListSize = this.closedListSize - st.closedListSize;
        rec.openListSize = this.openListSize - st.openListSize;
        //	rec.maxTime = this.maxTime - st.maxTime;
        rec.revisits = this.revisits - st.revisits;
        return rec;
    }


    public void merge(StatsRecord st) {
        this.statesExpandedHC = this.statesExpandedHC + st.statesExpandedHC;
        this.statesExpanded = this.statesExpanded + st.statesExpanded;
        this.statesUpdated = this.statesUpdated + st.statesUpdated;
        this.pathLength = this.pathLength + st.pathLength;
        this.pathCost = this.pathCost + st.pathCost;
        this.time = this.time + st.time;
        this.closedListSize = this.closedListSize + st.closedListSize;
        this.openListSize = this.openListSize + st.openListSize;
        //	if (this.maxTime < st.maxTime)
        //		this.maxTime = st.maxTime;
        this.revisits += st.revisits;
        this.moveTimes = st.moveTimes;
    }

    public int getStatesExpandedHC() {
        return statesExpandedHC;
    }

    public void setStatesExpandedHC(int statesExpandedHC) {
        this.statesExpandedHC = statesExpandedHC;
    }

    public long getStatesExpanded() {
        return statesExpanded;
    }

    public void setStatesExpanded(int statesExpanded) {
        this.statesExpanded = statesExpanded;
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

    public void incrementCost(int cost) {
        pathCost += cost;
    }

    public void incrementLength(int len) {
        pathLength += len;
    }

    public void setStatesUpdated(int statesUpdated) {
        this.statesUpdated = statesUpdated;
    }

    public long getStatesUpdated() {
        return statesUpdated;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(100);
        buf.append("Path cost: ");
        buf.append(pathCost);
        buf.append("\tPath length: ");
        buf.append(pathLength);
        buf.append("\nStates HC: ");
        buf.append(statesExpandedHC);
        buf.append("\t States Expanded: ");
        buf.append(statesExpanded);
        buf.append("\tStates updated: ");
        buf.append(statesUpdated);
        buf.append("\tTime: ");
        buf.append(time);
        buf.append("\tMaxTime: ");
        buf.append(moveTimes[0]);
        //	buf.append(maxTime);
        buf.append("\tRevisits: ");
        buf.append(revisits);
        return buf.toString();
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
        arrayCount = 0;
        moveCount = 0;
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

    public void outputCSV(StatsRecord rec1, PrintWriter out) {
        StatsRecord rec2 = this;
        long val1 = rec1.getPathLength(), val2 = rec2.getPathLength();
        out.print(val2 + "\t");
        double subL = 0;
        if (val1 != 0) {
            subL = 1000 * val2 / val1 / 1000.0;
        }
        out.print(subL + "\t");

        val1 = rec1.getPathCost();
        val2 = rec2.getPathCost();
        out.print(val2 + "\t");
        subL = 0;
        if (val1 != 0) {
            subL = 1000 * val2 / val1 / 1000.0;
        }
        out.print(subL + "\t");

        val1 = rec1.getStatesExpanded();
        val2 = rec2.getStatesExpanded();
        out.print(val2 + "\t");

        val1 = rec1.getStatesUpdated();
        val2 = rec2.getStatesUpdated();
        out.print(val2 + "\t");

        val1 = rec1.getTime();
        val2 = rec2.getTime();
        out.print(val2 + "\t");

        val1 = rec1.getOpenListSize();
        val2 = rec2.getOpenListSize();
        out.print(val2 + "\t");

        val1 = rec1.getClosedListSize();
        val2 = rec2.getClosedListSize();
        out.print(val2 + "\t");

        val1 = rec1.getMaxMemSize();
        val2 = rec2.getMaxMemSize();
        out.print(val2 + "\t");

        val1 = rec1.getMaxTime();
        val2 = rec2.getMaxTime();
        out.print(val2 + "\t");

        val1 = rec1.getRevisits();
        val2 = rec2.getRevisits();
        out.print(val2 + "\t");

        // Write out the move number of the most expensive move
        val2 = rec2.maxMoveNum[0];
        out.print(val2 + "\t");
		
		/*
		// Sort the move array for info.
		long[] tmp = new long[rec2.arrayCount];
		for (int i=0; i < rec2.arrayCount; i++)
			tmp[i] = rec2.moveTimes[i];
		Arrays.sort(tmp);
		*/
        // Now keeping array in sorted order
        // Print the median
        if (rec2.arrayCount > 0) out.print(rec2.moveTimes[rec2.arrayCount / 2] + "\t");
		/*
		// Write the top 5 most expensive moves		
		// Worst 3
		for (int i=rec2.arrayCount-1; i > rec2.arrayCount-4; i--)
		{	if (i >= 0)
				out.print(rec2.moveTimes[i]+"\t");
			else
				out.print("\t");
		}
					
		// Worst 5
		if (rec2.arrayCount-5 >= 0)
			out.print(rec2.moveTimes[rec2.arrayCount-5]+"\t");
		else
			out.print("\t");
		
		// Worst 10
		if (rec2.arrayCount-10 >= 0)
			out.print(rec2.moveTimes[rec2.arrayCount-10]+"\t");
		else
			out.print("\t");
		*/

        // Worst 3
        for (int i = 0; i < 3; i++) {
            if (i >= 0) out.print(rec2.moveTimes[i] + "\t");
            else out.print("\t");
        }

        // Worst 5
        if (rec2.arrayCount >= 4) out.print(rec2.moveTimes[4] + "\t");
        else out.print("\t");

        // Worst 10
        if (rec2.arrayCount >= 10) out.print(rec2.moveTimes[9] + "\t");
        else out.print("\t");

        out.println();
    }


    public long getMaxTime() {
        return moveTimes[0];
    }

    /*
        public void setMaxTime(long maxTime) {
            this.maxTime = maxTime;
        }
        */
    public void updateMaxTime(long maxTime) {
	/*
		if (maxTime > this.maxTime)
		{	maxMoveNum = arrayCount+1;
			this.maxTime = maxTime;
		}
		if (arrayCount < moveTimes.length)
			moveTimes[arrayCount++] = maxTime;
			*/

        // Insert using bubble sort into sorted order descending
        moveCount++;
        int i;
        for (i = arrayCount - 1; i >= 0; i--) {
            if (maxTime > moveTimes[i]) {
                if (i < moveTimes.length - 1) {
                    moveTimes[i + 1] = moveTimes[i];
                    maxMoveNum[i + 1] = maxMoveNum[i];
                }
            } else break;
        }
        if (i + 1 < moveTimes.length) {
            moveTimes[i + 1] = maxTime;
            maxMoveNum[i + 1] = moveCount;
            if (arrayCount < moveTimes.length) arrayCount++;
        }
    }

    public int getSubgoals() {
        return subgoals;
    }

    public void setSubgoals(int subgoals) {
        this.subgoals = subgoals;
    }
}
