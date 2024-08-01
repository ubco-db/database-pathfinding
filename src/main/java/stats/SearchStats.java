package stats;

public class SearchStats {
    private long numPaths;

    private long numStatesExpandedHC;               // States expanded during hill climbing
    private long numStatesExpandedHCCompression;

    private long numStatesExpandedBFS;              // States expanded during BFS (for region abstraction)

    private long numStatesExpanded;                 // States expanded during A* search
    private long numStatesUpdated;                  // States updated during A* search
    private long numAbstractStatesExpanded;         // States expanded during A* search in abstract space
    private long numAbstractStatesUpdated;

    private long pathLength;                         // Path length found by the algorithm
    private long pathCost;                           // Cost of path found

    private long numberOfSubgoals;                   // Number of subgoals in database
    private long numberOfPathsThatHaveSubgoals;      // Number of paths in database that have subgoals

    private long timeToFindPathOnline;
    private long timeToFindAStarPathsOffline;
    private long timeToPerformHCCompression;
    private long timeToGenerateDatabase;
    private long timeToAbstractRegions;
    private long timeToDetermineNeighbourhoods;
    private long totalAbstractionTime;
    private long totalTime;

    private long timeToFindPathsUsingHC;
    private long timeToFindPathsUsingAStar;

    public SearchStats() {
    }

    public SearchStats(long numStatesExpanded, long numStatesUpdated, int pathLength, int pathCost) {
        this.numStatesExpanded = numStatesExpanded;
        this.numStatesUpdated = numStatesUpdated;
        this.pathLength = pathLength;
        this.pathCost = pathCost;
    }

    public void incrementNumStatesExpandedHC(int increment) {
        numStatesExpandedHC += increment;
    }

    public void incrementNumStatesExpandedHCCompression(int increment) {
        numStatesExpandedHCCompression += increment;
    }

    public void incrementNumStatesExpanded(int increment) {
        numStatesExpanded += increment;
    }

    public void incrementNumAbstractStatesExpanded(int increment) {
        numAbstractStatesExpanded += increment;
    }

    public void incrementNumStatesUpdated(int increment) {
        numStatesUpdated += increment;
    }

    public void incrementNumAbstractStatesUpdated(int increment) {
        numAbstractStatesUpdated += increment;
    }

    public void incrementNumPaths(int increment) {
        numPaths += increment;
    }

    public void incrementNumberOfSubgoals(int increment) {
        numberOfSubgoals += increment;
    }

    public void incrementNumberOfPathsThatHaveSubgoals(int increment) {
        numberOfPathsThatHaveSubgoals += increment;
    }

    public void incrementTimeToFindPathsUsingHC(long increment) {
        timeToFindPathsUsingHC += increment;
    }

    public void incrementTimeToFindPathsUsingAStar(long increment) {
        timeToFindPathsUsingAStar += increment;
    }

    // GETTERS AND SETTERS

    public long getNumPaths() {
        return numPaths;
    }

    public void setNumPaths(long numPaths) {
        this.numPaths = numPaths;
    }

    public long getNumStatesExpandedHC() {
        return numStatesExpandedHC;
    }

    public void setNumStatesExpandedHC(long numStatesExpandedHC) {
        this.numStatesExpandedHC = numStatesExpandedHC;
    }

    public long getNumStatesExpandedHCCompression() {
        return numStatesExpandedHCCompression;
    }

    public void setNumStatesExpandedHCCompression(long numStatesExpandedHCCompression) {
        this.numStatesExpandedHCCompression = numStatesExpandedHCCompression;
    }

    public long getNumStatesExpandedBFS() {
        return numStatesExpandedBFS;
    }

    public void setNumStatesExpandedBFS(long numStatesExpandedBFS) {
        this.numStatesExpandedBFS = numStatesExpandedBFS;
    }

    public long getNumStatesExpanded() {
        return numStatesExpanded;
    }

    public void setNumStatesExpanded(long numStatesExpanded) {
        this.numStatesExpanded = numStatesExpanded;
    }

    public long getNumStatesUpdated() {
        return numStatesUpdated;
    }

    public void setNumStatesUpdated(long numStatesUpdated) {
        this.numStatesUpdated = numStatesUpdated;
    }

    public long getNumAbstractStatesExpanded() {
        return numAbstractStatesExpanded;
    }

    public void setNumAbstractStatesExpanded(long numAbstractStatesExpanded) {
        this.numAbstractStatesExpanded = numAbstractStatesExpanded;
    }

    public long getNumAbstractStatesUpdated() {
        return numAbstractStatesUpdated;
    }

    public void setNumAbstractStatesUpdated(long numAbstractStatesUpdated) {
        this.numAbstractStatesUpdated = numAbstractStatesUpdated;
    }

    public long getPathLength() {
        return pathLength;
    }

    public void setPathLength(long pathLength) {
        this.pathLength = pathLength;
    }

    public long getPathCost() {
        return pathCost;
    }

    public void setPathCost(long pathCost) {
        this.pathCost = pathCost;
    }

    public long getNumberOfSubgoals() {
        return numberOfSubgoals;
    }

    public void setNumberOfSubgoals(long numberOfSubgoals) {
        this.numberOfSubgoals = numberOfSubgoals;
    }

    public long getNumberOfPathsThatHaveSubgoals() {
        return numberOfPathsThatHaveSubgoals;
    }

    public void setNumberOfPathsThatHaveSubgoals(long numberOfPathsThatHaveSubgoals) {
        this.numberOfPathsThatHaveSubgoals = numberOfPathsThatHaveSubgoals;
    }

    public long getTimeToFindPathOnline() {
        return timeToFindPathOnline;
    }

    public void setTimeToFindPathOnline(long timeToFindPathOnline) {
        this.timeToFindPathOnline = timeToFindPathOnline;
    }

    public long getTimeToFindAStarPathsOffline() {
        return timeToFindAStarPathsOffline;
    }

    public void setTimeToFindAStarPathsOffline(long timeToFindAStarPathsOffline) {
        this.timeToFindAStarPathsOffline = timeToFindAStarPathsOffline;
    }

    public long getTimeToPerformHCCompression() {
        return timeToPerformHCCompression;
    }

    public void setTimeToPerformHCCompression(long timeToPerformHCCompression) {
        this.timeToPerformHCCompression = timeToPerformHCCompression;
    }

    public long getTimeToGenerateDatabase() {
        return timeToGenerateDatabase;
    }

    public void setTimeToGenerateDatabase(long timeToGenerateDatabase) {
        this.timeToGenerateDatabase = timeToGenerateDatabase;
    }

    public long getTimeToAbstractRegions() {
        return timeToAbstractRegions;
    }

    public void setTimeToAbstractRegions(long timeToAbstractRegions) {
        this.timeToAbstractRegions = timeToAbstractRegions;
    }

    public long getTimeToDetermineNeighbourhoods() {
        return timeToDetermineNeighbourhoods;
    }

    public void setTimeToDetermineNeighbourhoods(long timeToDetermineNeighbourhoods) {
        this.timeToDetermineNeighbourhoods = timeToDetermineNeighbourhoods;
    }

    public long getTotalAbstractionTime() {
        return totalAbstractionTime;
    }

    public void setTotalAbstractionTime(long totalAbstractionTime) {
        this.totalAbstractionTime = totalAbstractionTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    @Override
    public String toString() {
        return "SearchStats{" +
                "numPaths=" + numPaths +
                ", numStatesExpandedHC=" + numStatesExpandedHC +
                ", numStatesExpandedHCCompression=" + numStatesExpandedHCCompression +
                ", numStatesExpandedBFS=" + numStatesExpandedBFS +
                ", numStatesExpanded=" + numStatesExpanded +
                ", numStatesUpdated=" + numStatesUpdated +
                ", numAbstractStatesExpanded=" + numAbstractStatesExpanded +
                ", numAbstractStatesUpdated=" + numAbstractStatesUpdated +
                ", pathLength=" + pathLength +
                ", pathCost=" + pathCost +
                ", numberOfSubgoals=" + numberOfSubgoals +
                ", numberOfPathsThatHaveSubgoals=" + numberOfPathsThatHaveSubgoals +
                ", timeToFindPathOnline=" + timeToFindPathOnline +
                ", timeToFindAStarPathsOffline=" + timeToFindAStarPathsOffline +
                ", timeToPerformHCCompression=" + timeToPerformHCCompression +
                ", timeToGenerateDatabase=" + timeToGenerateDatabase +
                ", timeToAbstractRegions=" + timeToAbstractRegions +
                ", timeToDetermineNeighbourhoods=" + timeToDetermineNeighbourhoods +
                ", totalAbstractionTime=" + totalAbstractionTime +
                ", totalTime=" + totalTime +
                '}';
    }

    public String getData() {
        return numPaths +
                "," + pathLength +
                "," + pathCost +
                "," + numberOfSubgoals +
                "," + numberOfPathsThatHaveSubgoals +
                "," + numStatesExpandedBFS +
                "," + numStatesExpanded +
                "," + numStatesExpandedHCCompression +
                "," + timeToAbstractRegions +
                "," + timeToDetermineNeighbourhoods +
                "," + totalAbstractionTime +
                "," + timeToFindAStarPathsOffline +
                "," + timeToPerformHCCompression +
                "," + timeToGenerateDatabase +
                "," + totalTime;
    }

    public String getTimesOnly() {
        return (timeToAbstractRegions / 1_000_000.0) +
                "," + (timeToDetermineNeighbourhoods / 1_000_000.0) +
                "," + (totalAbstractionTime / 1_000_000.0) +
                "," + (timeToFindAStarPathsOffline / 1_000_000.0) +
                "," + (timeToPerformHCCompression / 1_000_000.0) +
                "," + (timeToGenerateDatabase / 1_000_000.0) +
                "," + (totalTime / 1_000_000.0);
    }

    public String getStatesExpandedOnly() {
        return numStatesExpandedBFS +
                "," + numStatesExpanded +
                "," + numStatesExpandedHCCompression;
    }

    public String getTotalTimeOnly() {
        return " " + (totalTime / 1_000_000.0);
    }

    public String getTotalAbstractionTimeOnly() {
        return " " + (totalAbstractionTime / 1_000_000.0);
    }

    public void addAll(SearchStats searchStats) {
        this.numPaths += searchStats.numPaths;
        this.numStatesExpandedHC += searchStats.numStatesExpandedHC;
        this.numStatesExpandedHCCompression += searchStats.numStatesExpandedHCCompression;
        this.numStatesExpandedBFS += searchStats.numStatesExpandedBFS;
        this.numStatesExpanded += searchStats.numStatesExpanded;
        this.numStatesUpdated += searchStats.numStatesUpdated;
        this.numAbstractStatesExpanded += searchStats.numAbstractStatesExpanded;
        this.numAbstractStatesUpdated += searchStats.numAbstractStatesUpdated;
        this.pathLength += searchStats.pathLength;
        this.pathCost += searchStats.pathCost;
        this.numberOfSubgoals += searchStats.numberOfSubgoals;
        this.numberOfPathsThatHaveSubgoals += searchStats.numberOfPathsThatHaveSubgoals;
        this.timeToFindPathOnline += searchStats.timeToFindPathOnline;
        this.timeToFindAStarPathsOffline += searchStats.timeToFindAStarPathsOffline;
        this.timeToPerformHCCompression += searchStats.timeToPerformHCCompression;
        this.timeToGenerateDatabase += searchStats.timeToGenerateDatabase;
        this.timeToAbstractRegions += searchStats.timeToAbstractRegions;
        this.timeToDetermineNeighbourhoods += searchStats.timeToDetermineNeighbourhoods;
        this.totalAbstractionTime += searchStats.totalAbstractionTime;
        this.totalTime += searchStats.totalTime;
        this.timeToFindPathsUsingAStar += searchStats.timeToFindPathsUsingAStar;
        this.timeToFindPathsUsingHC += searchStats.timeToFindPathsUsingHC;
    }

    public void divideBy(int i) {
        this.numPaths /= i;
        this.numStatesExpandedHC /= i;
        this.numStatesExpandedHCCompression /= i;
        this.numStatesExpandedBFS /= i;
        this.numStatesExpanded /= i;
        this.numStatesUpdated /= i;
        this.numAbstractStatesExpanded /= i;
        this.numAbstractStatesUpdated /= i;
        this.pathLength /= i;
        this.pathCost /= i;
        this.numberOfSubgoals /= i;
        this.numberOfPathsThatHaveSubgoals /= i;
        this.timeToFindPathOnline /= i;
        this.timeToFindAStarPathsOffline /= i;
        this.timeToPerformHCCompression /= i;
        this.timeToGenerateDatabase /= i;
        this.timeToAbstractRegions /= i;
        this.timeToDetermineNeighbourhoods /= i;
        this.totalAbstractionTime /= i;
        this.totalTime /= i;
    }

    public String getHCStats() {
        return numStatesExpandedHC + ", " + (timeToFindPathsUsingHC / 1_000_000.0);
    }

    public String getAStarStats() {
        return numStatesExpanded + ", " + (timeToFindPathsUsingAStar / 1_000_000.0);
    }
}
