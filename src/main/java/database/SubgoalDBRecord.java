package database;

/**
 * A record in the subgoal database.
 *
 * @author rlawrenc
 */
public class SubgoalDBRecord {
    // Region to find path from
    private final int startRegionRep;
    // Region to find path to
    private final int goalRegionRep;
    // Subgoals do not include the region representatives of start and goal region
    private final int[] subgoals;

    public SubgoalDBRecord(int startRegionRep, int goalRegionRep, int[] subgoals) {
        this.startRegionRep = startRegionRep;
        this.goalRegionRep = goalRegionRep;
        this.subgoals = subgoals;
    }

    public int getStartRegionRep() {
        return startRegionRep;
    }

    public int getGoalRegionRep() {
        return goalRegionRep;
    }

    public int[] getSubgoals() {
        return subgoals;
    }
}
