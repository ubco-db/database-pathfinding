package database;

import search.SearchAlgorithm;
import search.SearchProblem;
import search.SearchState;
import search.StatsRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * A generic database class used to store pre-computed path or subgoal information for speeding up real-time searches.
 * An algorithm may use this base class or extend the class to contain additional information required.
 *
 * @author rlawrenc
 */
public class SubgoalDB implements Cloneable {
    protected ArrayList<SubgoalDBRecord> records;
    protected SearchProblem problem;

    public SubgoalDB() {
        records = new ArrayList<SubgoalDBRecord>();
    }

    public int getSize() {
        return records.size();
    }

    /**
     * Verifies all subgoals in the record can be reached via hill-climbing from the previous one.
     * Verification is performed for all records.
     *
     * @param searchAlg
     */
    public void verify(SearchAlgorithm searchAlg) {
        StatsRecord stats = new StatsRecord();
        int countFailed = 0;
        for (SubgoalDBRecord rec : records) {
            // Try to Hill climb from start to each subgoal to the end
            SearchState currentStart = new SearchState(rec.getStartId());
            SearchState currentGoal;
            int[] subgoals = rec.getSubgoalList();
            if (subgoals != null) {
                for (int subgoal : subgoals) {
                    currentGoal = new SearchState(subgoal);

                    if (!searchAlg.isPath(currentStart, currentGoal, stats)) countFailed++;
                    currentStart = currentGoal;
                }
            }
            currentGoal = new SearchState(rec.getGoalId());
            if (!searchAlg.isPath(currentStart, currentGoal, stats)) countFailed++;
        }

        System.out.println("# of records that failed: " + countFailed);
    }

    public SearchProblem getProblem() {
        return problem;
    }

    public void setProblem(SearchProblem problem) {
        this.problem = problem;
    }

    /**
     * Adds record to database.
     *
     * @param rec
     * @return boolean
     */
    public boolean addRecord(SubgoalDBRecord rec) {
        records.add(rec);
        return true;
    }

    /**
     * Computes the distance between a record start and end the problem goal and end.
     * Computation takes the maximum distance of record start to problem start and record end to problem end.
     *
     * @param problem
     * @param rec
     * @param startId
     * @param goalId
     * @return int
     */
    public int computeDistance(SearchProblem problem, SubgoalDBRecord rec, int startId, int goalId) {
        int dist1 = problem.computeDistance(rec.getStartId(), startId);
        int dist2 = problem.computeDistance(rec.getGoalId(), goalId);

        return Math.max(dist1, dist2);
    }

    /**
     * Finds the top 10 closest records in the database for a given start and goal (using heuristic distances).
     * Search is done linearly through all records.
     *
     * @param problem
     * @param start
     * @param goal
     * @return ArrayList<SubgoalDBRecord>
     */
    public ArrayList<SubgoalDBRecord> findNearest(SearchProblem problem, SearchState start, SearchState goal, int max) {
        int j;
        ArrayList<SubgoalDBRecord> best = new ArrayList<>();
        for (SubgoalDBRecord rec : records) {
            int dist = computeDistance(problem, rec, start.id, goal.id);

            // Insert record into sorted order
            boolean doInsert = false;
            for (j = 0; j < best.size() && j < max; j++) {
                if (computeDistance(problem, best.get(j), start.id, goal.id) > dist) {
                    doInsert = true;
                    break;
                }
            }
            if (j < max || doInsert) {
                best.add(j, rec);
            }
        }
        return best;
    }

    /**
     * Finds up top 10 closest records in the database for a given start and goal (using heuristic distances) where start and goal are HC-reachable.
     *
     * @param problem
     * @param start
     * @param goal
     * @param searchAlg
     * @param max      - number of results to return
     * @param stats
     * @param used
     * @return ArrayList<SubgoalDBRecord>
     */
    public ArrayList<SubgoalDBRecord> findNearest(SearchProblem problem, SearchState start, SearchState goal, SearchAlgorithm searchAlg, int max, StatsRecord stats, ArrayList<SubgoalDBRecord> used) {
        ArrayList<SubgoalDBRecord> results = findNearest(problem, start, goal, max);
        ArrayList<SubgoalDBRecord> resultsHC = new ArrayList<>(1);

        // Filter out those that are not HC-reachable
        for (int i = 0; i < results.size() && i < max; i++) {
            SubgoalDBRecord rec = results.get(i);
            if (used != null && used.contains(rec)) continue;
            if (searchAlg.isPath(start, new SearchState(rec.getStartId()), stats) && searchAlg.isPath(new SearchState(rec.getGoalId()), goal, stats)) {
                resultsHC.add(rec);
                return resultsHC;        // Only returning one	- instead of a number up to 10
                // if (resultsHC.size() >= max)
//					break;			
            }
        }
        return resultsHC;
    }


    /**
     * Converts a path of states into a fixed array of state ids.
     *
     * @param path
     * @return int[]
     */
    public static int[] convertPathToIds(ArrayList<SearchState> path) {
        int[] idPath = new int[path.size()];
        for (int i = 0; i < path.size(); i++)
            idPath[i] = path.get(i).id;
        return idPath;
    }

    /**
     * Saves the subgoal database (records only).
     *
     * @param fileName
     */
    public void exportDB(String fileName) {
        try (PrintWriter out = new PrintWriter(fileName)) {
            out.println(records.size());
            for (SubgoalDBRecord record : records) record.save(out);
        } catch (FileNotFoundException e) {
            System.out.println("Error with output file: " + e);
        }
    }

    /**
     * Returns true if file exists (more general than just if subgoal database exists).
     *
     * @param fileName
     * @return boolean
     */
    public boolean exists(String fileName) {
        File f = new File(fileName);
        return f.exists();
    }

    /**
     * Loads a database of records.
     *
     * @param fileName
     * @return boolean
     */
    public boolean load(String fileName) {
        Scanner sc = null;
        boolean success = true;

        records.clear();
        try {
            sc = new Scanner(new File(fileName));
            long currentTime = System.currentTimeMillis();
            int num = Integer.parseInt(sc.nextLine());
            records.ensureCapacity(num);
            for (int i = 0; i < num; i++) {
                SubgoalDBRecord rec = new SubgoalDBRecord(i, sc.nextLine());
                this.addRecord(rec);
            }
            System.out.println("Loaded " + num + " records in " + (System.currentTimeMillis() - currentTime));
        } catch (FileNotFoundException e) {
            System.out.println("Did not find input file: " + e);
            success = false;
        } finally {
            if (sc != null) sc.close();
        }
        return success;
    }

    @Override
    public SubgoalDB clone() {
        try {
            SubgoalDB clone = (SubgoalDB) super.clone();

            clone.records = new ArrayList<>();
            for (SubgoalDBRecord record : this.records) {
                clone.records.add(record.clone());
            }

            clone.problem = problem.clone();

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
