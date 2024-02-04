package database;

import search.SearchAlgorithm;
import search.SearchProblem;
import search.SearchState;
import search.SearchUtil;
import search.StatsRecord;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * A record in the subgoal database.
 *
 * @author rlawrenc
 */
public class SubgoalDBRecord {
    private int id;
    private int startId;
    private int goalId;
    private final int searchDepth;
    // Use a custom array rather than an ArrayList of objects to save memory.
    private final int[] stateIds;

    public SubgoalDBRecord(int startId, int goalId, int[] subgoalIds, int depth) {
        super();
        this.startId = startId;
        this.goalId = goalId;
        this.stateIds = subgoalIds;
        this.searchDepth = depth;
    }

    // Extracts a subgoal record from a text line
    public SubgoalDBRecord(int id, String st) {
        StringTokenizer tokenizer = new StringTokenizer(st);
        this.id = id;
        startId = Integer.parseInt(tokenizer.nextToken());
        goalId = Integer.parseInt(tokenizer.nextToken());
        searchDepth = Integer.parseInt(tokenizer.nextToken());

        int numSubgoals = Integer.parseInt(tokenizer.nextToken());
        stateIds = new int[numSubgoals];
        for (int i = 0; i < numSubgoals; i++)
            stateIds[i] = Integer.parseInt(tokenizer.nextToken());
    }

    public int getStartId() {
        return startId;
    }

    public void setStartId(int startId) {
        this.startId = startId;
    }

    public int getGoalId() {
        return goalId;
    }

    public void setGoalId(int goalId) {
        this.goalId = goalId;
    }

    public int[] getSubgoalList() {
        return stateIds;
    }


    public String toString() {
        StringBuilder buf = new StringBuilder(100);
        buf.append("Id: ");
        buf.append(id);
        buf.append(" Start id: ");
        buf.append(startId);
        buf.append("\t Goal id: (");
        buf.append(goalId);
        buf.append(") Subgoals: ");
        if (stateIds != null) for (int stateId : stateIds) buf.append(stateId).append(" ; ");
        return buf.toString();
    }

    public String toString(SearchProblem problem) {
        StringBuilder buf = new StringBuilder(100);
        buf.append("Id: ");
        buf.append(id);
        buf.append(" Start: ");
        buf.append(problem.idToString(startId));
        buf.append(" Goal: ");
        buf.append(problem.idToString(goalId));
        buf.append(" Subgoals: ");
        if (stateIds != null) {
            for (int stateId : stateIds) buf.append(problem.idToString(stateId)).append("; ");
        }
        return buf.toString();
    }

    public void save(PrintWriter out) {
        out.print(startId);
        out.print("\t" + goalId);
        out.print("\t" + searchDepth);
        if (stateIds != null) {
            out.print("\t" + stateIds.length + "\t");
            for (int stateId : stateIds) out.print(stateId + "\t");
        } else out.print("\t0");
        out.println();
    }

    public int getSearchDepth() {
        return searchDepth;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /**
     * Computes a path from the compressed record given the search algorithm to expand the record with.
     *
     * @param alg
     * @return ArrayList<SearchState>
     */
    public ArrayList<SearchState> computePath(SearchAlgorithm alg) {
        ArrayList<SearchState> path = new ArrayList<>();

        SearchState start = new SearchState(this.startId);
        SearchState goal = new SearchState(this.goalId);
        SearchState currStart = start;

        StatsRecord stats = new StatsRecord();
        if (this.stateIds == null) path = alg.computePath(start, goal, stats);
        else {
            for (int stateId : this.stateIds) {
                ArrayList<SearchState> tmp = alg.computePath(currStart, new SearchState(stateId), stats);
                currStart = new SearchState(stateId);
                SearchUtil.mergePaths(path, tmp);
            }
            SearchUtil.mergePaths(path, alg.computePath(currStart, goal, stats));
        }

        return path;
    }
}
