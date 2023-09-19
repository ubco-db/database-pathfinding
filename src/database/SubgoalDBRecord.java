package database;

import map.GameMap;
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
    private int searchDepth;
    // Use a custom array rather than an ArrayList of objects to save memory.
    private int[] stateIds;

    public SubgoalDBRecord(int id, int startId, int goalId, int[] subgoalIds, int depth) {
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

    public SubgoalDBRecord(int id, String st, GameMap map) {
        StringTokenizer tokenizer = new StringTokenizer(st);
        this.id = id;
        int startRow = Integer.parseInt(tokenizer.nextToken());
        int startCol = Integer.parseInt(tokenizer.nextToken());
        startId = map.getId(startRow, startCol);
        int goalRow = Integer.parseInt(tokenizer.nextToken());
        int goalCol = Integer.parseInt(tokenizer.nextToken());
        goalId = map.getId(goalRow, goalCol);
        searchDepth = Integer.parseInt(tokenizer.nextToken());

        int numSubgoals = Integer.parseInt(tokenizer.nextToken());
        stateIds = new int[numSubgoals];
        for (int i = 0; i < numSubgoals; i++) {
            int row = Integer.parseInt(tokenizer.nextToken());
            int col = Integer.parseInt(tokenizer.nextToken());
            stateIds[i] = map.getId(row, col);
        }
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
        StringBuffer buf = new StringBuffer(100);
        buf.append("Id: ");
        buf.append(id);
        buf.append(" Start id: ");
        buf.append(startId);
        buf.append("\t Goal id: (");
        buf.append(goalId);
        buf.append(") Subgoals: ");
        if (stateIds != null) for (int i = 0; i < stateIds.length; i++)
            buf.append(stateIds[i] + " ; ");
        return buf.toString();
    }

    public String toString(SearchProblem problem) {
        StringBuffer buf = new StringBuffer(100);
        buf.append("Id: ");
        buf.append(id);
        buf.append(" Start: ");
        buf.append(problem.idToString(startId));
        buf.append(" Goal: ");
        buf.append(problem.idToString(goalId));
        buf.append(" Subgoals: ");
        if (stateIds != null) {
            for (int i = 0; i < stateIds.length; i++)
                buf.append(problem.idToString(stateIds[i]) + " ; ");
        }
        return buf.toString();
    }

    public void save(PrintWriter out) {
        out.print(startId);
        out.print("\t" + goalId);
        out.print("\t" + searchDepth);
        if (stateIds != null) {
            out.print("\t" + stateIds.length + "\t");
            for (int i = 0; i < stateIds.length; i++)
                out.print(stateIds[i] + "\t");
        } else out.print("\t0");
        out.println();
    }

    public void setSearchDepth(int searchDepth) {
        this.searchDepth = searchDepth;
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

    public SearchState getSubgoal(int idx) {
        return new SearchState(stateIds[idx]);
    }

    /**
     * Computes a path from the compressed record given the search algorithm to expand the record with.
     *
     * @param alg
     * @return
     */
    public ArrayList<SearchState> computePath(SearchAlgorithm alg) {
        ArrayList<SearchState> path = new ArrayList<SearchState>();

        SearchState start = new SearchState(this.startId);
        SearchState goal = new SearchState(this.goalId);
        SearchState currStart = start;

        StatsRecord stats = new StatsRecord();
        if (this.stateIds == null) path = alg.computePath(start, goal, stats);
        else {
            for (int i = 0; i < this.stateIds.length; i++) {
                ArrayList<SearchState> tmp = alg.computePath(currStart, new SearchState(this.stateIds[i]), stats);
                currStart = new SearchState(this.stateIds[i]);
                path = SearchUtil.mergePaths(path, tmp);
            }
            path = SearchUtil.mergePaths(path, alg.computePath(currStart, goal, stats));
        }

        return path;
    }
}
