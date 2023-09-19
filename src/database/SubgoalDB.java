package database;

import map.GameMap;
import map.GroupRecord;
import search.SavedSearch;
import search.SearchAbstractAlgorithm;
import search.SearchAlgorithm;
import search.SearchProblem;
import search.SearchState;
import search.StatsRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Scanner;

/**
 * A generic database class used to store pre-computed path or subgoal information for speeding up real-time searches.
 * An algorithm may use this base class or extend the class to contain additional information required.
 *
 * @author rlawrenc
 */
public class SubgoalDB {
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
     * @param map
     * @param searchAlg
     */
    public void verify(SearchAlgorithm searchAlg) {
        StatsRecord stats = new StatsRecord();
        int countFailed = 0;
        for (int i = 0; i < records.size(); i++) {
            SubgoalDBRecord rec = records.get(i);
            // Try to Hill climb from start to each subgoal to the end
            SearchState currentStart = new SearchState(rec.getStartId());
            SearchState currentGoal;
            int[] subgoals = rec.getSubgoalList();
            if (subgoals != null) {
                for (int j = 0; j < subgoals.length; j++) {
                    currentGoal = new SearchState(subgoals[j]);

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

    public void replaceRecords(ArrayList<SubgoalDBRecord> records) {
        this.records = records;
    }

    /**
     * Adds record to database.
     *
     * @param rec
     * @return
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
     * @return
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
     * @return
     */
    public ArrayList<SubgoalDBRecord> findNearest(SearchProblem problem, SearchState start, SearchState goal, int max) {
        int j;
        ArrayList<SubgoalDBRecord> best = new ArrayList<SubgoalDBRecord>();
        for (int i = 0; i < records.size(); i++) {
            SubgoalDBRecord rec = records.get(i);
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
     * @param map
     * @param startRow
     * @param startCol
     * @param goalRow
     * @param goalCol
     * @param cutoff
     * @param max      - number of results to return
     * @param stats
     * @return
     */
    public ArrayList<SubgoalDBRecord> findNearest(SearchProblem problem, SearchState start, SearchState goal, SearchAlgorithm searchAlg, int max, StatsRecord stats, ArrayList<SubgoalDBRecord> used) {
        ArrayList<SubgoalDBRecord> results = findNearest(problem, start, goal, max);
        ArrayList<SubgoalDBRecord> resultsHC = new ArrayList<SubgoalDBRecord>(1);
        int maxCount = max;

        // Filter out those that are not HC-reachable
        for (int i = 0; i < results.size() && i < maxCount; i++) {
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
     * Same as findNearest but doesn't filter out none HC reachable goals
     *
     * @return
     */
    public ArrayList<SubgoalDBRecord> findNearest2(SearchProblem problem, SearchState start, SearchState goal, SearchAlgorithm searchAlg, int max, StatsRecord stats, ArrayList<SubgoalDBRecord> used) {
        ArrayList<SubgoalDBRecord> results = findNearest(problem, start, goal, max);
        //ArrayList<SubgoalDBRecord> resultsHC = new ArrayList<SubgoalDBRecord>(1);
/*		int maxCount = max;
		
		// Filter out those that are not HC-reachable
		for (int i=0; i < results.size() && i < maxCount; i++)
		{
			SubgoalDBRecord rec = results.get(i);		
			if (used !=null && used.contains(rec))
				continue;
			if (searchAlg.isPath(start, new SearchState(rec.getStartId()), stats) &&
					searchAlg.isPath(new SearchState(rec.getGoalId()), goal, stats))			
			{						
				resultsHC.add(rec);
				return resultsHC;		// Only returning one	- instead of a number up to 10
				// if (resultsHC.size() >= max)
//					break;			
			}
		}*/
        return results;
    }


    /**
     * Converts a path of states into a fixed array of state ids.
     *
     * @param path
     * @return
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
        PrintWriter out = null;
        try {
            out = new PrintWriter(fileName);
            out.println(records.size());

            for (int i = 0; i < records.size(); i++)
                records.get(i).save(out);
        } catch (FileNotFoundException e) {
            System.out.println("Error with output file: " + e);
        } finally {
            if (out != null) out.close();
        }
    }

    /**
     * Returns true if file exists (more general than just if subgoal database exists).
     *
     * @param fileName
     * @return
     */
    public boolean exists(String fileName) {
        File f = new File(fileName);
        return f.exists();
    }

    /**
     * Loads a database of records.
     *
     * @param fileName
     * @return
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

    /**
     * Loads a database of records.
     *
     * @param fileName
     * @return
     */
    public boolean loadMapDB(String fileName, GameMap map) {
        Scanner sc = null;
        boolean success = true;

        records.clear();
        try {
            sc = new Scanner(new File(fileName));
            long currentTime = System.currentTimeMillis();
            int num = Integer.parseInt(sc.nextLine());
            records.ensureCapacity(num);
            for (int i = 0; i < num; i++) {
                SubgoalDBRecord rec = new SubgoalDBRecord(i, sc.nextLine(), map);
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

    /**
     * Verifies all subgoals in the record can be reached via hill-climbing from the previous one.
     * Verification is performed for all records.
     *
     * @param map
     * @param searchAlg
     */
    public void computeCoverage(SearchAbstractAlgorithm searchAlg, GameMap map) {
        StatsRecord stats = new StatsRecord();
        int size = records.size();
        long problemSize = map.size();
        long fullProblemSize = map.rows * map.cols;
        long problems = problemSize * problemSize, total, startSize, goalSize;
        int sr, sc, gr, gc;
        long maxCoverage = 0, minCoverage = 100000000;
        GroupRecord[][] group = new GroupRecord[size][2];
        SavedSearch currentSet = new SavedSearch(65536);
        SavedSearch database = new SavedSearch(65536);
        int[] startCounts = new int[(int) fullProblemSize];
        int[] endCounts = new int[(int) fullProblemSize];

        System.out.println("Search space states: " + problemSize + " # of problems: " + problems);
        for (int i = 0; i < size; i++) {
            SubgoalDBRecord rec = records.get(i);
            sr = map.getRow(rec.getStartId());
            sc = map.getCol(rec.getStartId());
            database.clear();
            group[i][0] = map.expandSpot3(sr, sc, i, map, searchAlg, database, currentSet);
            startSize = group[i][0].getSize();
            gr = map.getRow(rec.getGoalId());
            gc = map.getCol(rec.getGoalId());
            group[i][1] = map.expandSpot3(gr, gc, i + 1, map, searchAlg, database, currentSet);
            goalSize = group[i][1].getSize();
            total = startSize * goalSize;
            if (total > maxCoverage) maxCoverage = total;
            if (total < minCoverage) minCoverage = total;
            System.out.println("Record :" + i + " Start: (" + sr + "," + sc + ") Goal: (" + gr + "," + gc + ") Start size: " + startSize + " Goal size: " + goalSize + " Total: " + total + " Percent: " + (total * 100.0 / problems));

            // Update start counts
            for (int j = 0; j < group[i][0].states.num(); j++)
                startCounts[group[i][0].states.get(j)] += goalSize;

            // Update end counts
            for (int j = 0; j < group[i][1].states.num(); j++)
                endCounts[group[i][1].states.get(j)] += startSize;
			
			/*
			ArrayList<SearchState> a = group[i][0].states.convert();
			a.add(0, new SearchState(rec.getStartId()));
			a.add(new SearchState(rec.getGoalId()));
			map.outputImage("images/coverage_"+i+".png", a, group[i][1].states.convert());
			*/
        }

        System.out.println("# of records: " + records.size());
        System.out.println("Min: " + (minCoverage * 100.0 / problems) + " Max: " + (maxCoverage * 100.0 / problems));

        // Determine min, avg, and max counts for cells
        long minCount = 100000, maxCount = 0, numCounts = 0, totalCounts = 0;

        for (int j = 0; j < fullProblemSize; j++) {
            int r = map.getRow(j);
            int c = map.getCol(j);
            if (!map.isWall(r, c)) {    // Include counts if a wall
                numCounts++;
                totalCounts += startCounts[j];
                if (startCounts[j] < minCount) minCount = startCounts[j];
                if (startCounts[j] > maxCount) maxCount = startCounts[j];
            }
        }
        System.out.println("Total valid states: " + numCounts + " Min: " + minCount + " Max: " + maxCount + " Avg: " + (totalCounts / numCounts));
    }

    /**
     * Verifies all subgoals in the record can be reached via hill-climbing from the previous one.
     * Verification is performed for all records.
     * Stores exact coverage using a BitSet.  Thus, size is limited to small maps.
     *
     * @param map
     * @param searchAlg
     */
    public ArrayList<String> computeCoverageExact(SearchAbstractAlgorithm searchAlg, GameMap map) {
        StatsRecord stats = new StatsRecord();
        int size = records.size();
        long problemSize = map.size();
        long fullProblemSize = map.rows * map.cols;
        long problems = problemSize * problemSize, total, startSize, goalSize;
        int sr, sc, gr, gc;
        long maxCoverage = 0, minCoverage = 100000000;
        GroupRecord[][] group = new GroupRecord[size][2];
        SavedSearch currentSet = new SavedSearch(65536);
        SavedSearch database = new SavedSearch(65536);
        int[] startCounts = new int[(int) fullProblemSize];
        int[] endCounts = new int[(int) fullProblemSize];
        BitSet coveredProblems = new BitSet((int) problems);
        long overlap = 0;

        ArrayList<String> result = new ArrayList<String>();

        System.out.println("Search space states: " + problemSize + " # of problems: " + problems);
        result.add("" + problemSize);
        result.add("" + problems);

        for (int i = 0; i < size; i++) {
            SubgoalDBRecord rec = records.get(i);
            sr = map.getRow(rec.getStartId());
            sc = map.getCol(rec.getStartId());
            database.clear();
            group[i][0] = map.expandSpot3(sr, sc, i, map, searchAlg, database, currentSet);
            startSize = group[i][0].getSize();
            gr = map.getRow(rec.getGoalId());
            gc = map.getCol(rec.getGoalId());
            group[i][1] = map.expandSpot3(gr, gc, i + 1, map, searchAlg, database, currentSet);
            goalSize = group[i][1].getSize();
            total = startSize * goalSize;
            if (total > maxCoverage) maxCoverage = total;
            if (total < minCoverage) minCoverage = total;
			/*
			System.out.println("Record :"+i+" Start: ("+sr+","+sc+") Goal: ("+gr+","+gc+") Start size: "+startSize+" Goal size: "+goalSize+" Total: "+total
					+" Percent: "+(total*100.0/problems));
			*/
            // Update start counts
            for (int j = 0; j < group[i][0].states.num(); j++)
                startCounts[group[i][0].states.get(j)] += goalSize;

            // Update end counts
            for (int j = 0; j < group[i][1].states.num(); j++)
                endCounts[group[i][1].states.get(j)] += startSize;

            // Update BitSet
            int tmp = 0;
            for (int j = 0; j < group[i][0].states.num(); j++) {
                for (int k = 0; k < group[i][1].states.num(); k++) {
                    int startId = group[i][0].states.get(j);
                    int goalId = group[i][1].states.get(k);
                    int idx = startId * (int) fullProblemSize + goalId;

                    tmp++;
                    if (coveredProblems.get(idx))
                        overlap++;                        // Problem already covered by another record
                    else coveredProblems.set(idx);
                }
            }
			/*
			ArrayList<SearchState> a = group[i][0].states.convert();
			a.add(0, new SearchState(rec.getStartId()));
			a.add(new SearchState(rec.getGoalId()));
			map.outputImage("images/coverage_"+i+".png", a, group[i][1].states.convert());
			*/
        }

        System.out.println("# of records: " + records.size());
        result.add("" + records.size());
        System.out.println("Min: " + (minCoverage * 100.0 / problems) + " Max: " + (maxCoverage * 100.0 / problems));

        // Determine min, avg, and max counts for cells
        long minCount = 100000, maxCount = 0, numCounts = 0, totalCounts = 0;

        for (int j = 0; j < fullProblemSize; j++) {
            int r = map.getRow(j);
            int c = map.getCol(j);
            if (!map.isWall(r, c)) {    // Include counts if a wall
                numCounts++;
                totalCounts += startCounts[j];
                if (startCounts[j] < minCount) minCount = startCounts[j];
                if (startCounts[j] > maxCount) maxCount = startCounts[j];
            }
        }
        System.out.println("All states: " + fullProblemSize + " Total valid states: " + numCounts + " Min: " + minCount + " Max: " + maxCount + " Avg: " + (totalCounts / numCounts));

        int cprob = coveredProblems.cardinality();
        result.add("" + cprob);
        System.out.println("Exact coverage results: " + cprob + " Overlap: " + overlap);


        long invalid = 0, cov = 0, uncov = 0;
        for (int i = 0; i < fullProblemSize; i++) {
            for (int j = 0; j < fullProblemSize; j++) {
                int startId = i;
                int goalId = j;
                int idx = startId * (int) fullProblemSize + goalId;

                if (map.isWall(startId) || map.isWall(goalId)) invalid++;
                else {
                    if (coveredProblems.get(idx)) cov++;
                    else uncov++;
                }
            }
        }

        System.out.println("All problems: " + fullProblemSize * fullProblemSize + " Invalid: " + invalid + " Covered: " + cov + " Uncovered: " + uncov + " Sum: " + (invalid + cov + uncov));
        return result;
    }
}
