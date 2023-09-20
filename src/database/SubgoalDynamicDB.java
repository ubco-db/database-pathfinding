package database;

import map.GameMap;
import map.GroupRecord;
import search.SearchAlgorithm;
import search.SearchProblem;
import search.SearchState;
import search.SearchUtil;
import search.StatsRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

/**
 * Database that is dynamically generated from dynamic programming table rather than storing results directly.
 * Database consists of pre-computed dynamic programming table represents in a matrix form.
 * Actual database records are computed on demand.
 * This version is real-time as record computation does not depend on problem size (as can piece together a record by combining each neighbor hop).
 *
 * @author rlawrenc
 */
public class SubgoalDynamicDB extends SubgoalDBExact {
    private int numGroups;                        // Number of abstract regions
    private int[][] lowestCost;                    // Lowest cost for DP table.  lowestCost[i][j] is the cost of the lowest path from region i to region j
    private int[][][] paths;                    // paths[i][j] is array representing a compressed path of state ids from region i to region j of lowest cost path
    private int[][] neighbor;                    // neighbor[i][j] is region id of next region to visit on lowest cost path from region i to region j (the next hop)


    /**
     * The method to find the best record changes as now we must piece together a record using the compute DP table rather than return a pre-computed record.
     */
    public ArrayList<SubgoalDBRecord> findNearest(SearchProblem problem, SearchState start, SearchState goal, SearchAlgorithm searchAlg, int max, StatsRecord stats, ArrayList<SubgoalDBRecord> used) {
        int startSeedId = db.findHT(start.id);
        int goalSeedId = db.findHT(goal.id);

        ArrayList<SubgoalDBRecord> result = new ArrayList<SubgoalDBRecord>(1);

        // Need to calculate record as will not be stored
        int pathSize;
        int[] path = new int[2000], tmp = new int[2000];
        int[] subgoals;

        // This code builds only the path required on demand (may incur more time as have to continually merge paths but may save time by avoiding storing/copying lists to do construction)
        pathSize = GameDB.mergePaths3(startSeedId, goalSeedId, paths, neighbor, path, 0);

        if (pathSize == 0) return null;            // No path between two states
        int startId = path[0];
        int goalId = path[pathSize - 1];

        // Does not include start and goal
        subgoals = SearchUtil.computeSubgoalsBinaryByIds(path, searchAlg, tmp, pathSize);

        SubgoalDBRecord rec = new SubgoalDBRecord(startId, goalId, subgoals, (startSeedId) * 10000 + goalSeedId);    // TODO: This will probably need to be changed.
        // System.out.println("Created record between: "+startId+" and "+goalId+" Record: "+rec.toString(problem));
        result.add(rec);
        return result;
    }

    /**
     * Initializes the dynamic programming table for querying.
     * Currently just a place holder.
     */
    public void init() {
    }

    /**
     * Loads DP table and mapping index from file to memory.
     */
    @Override
    public boolean load(String fileName) {
        // Load index first
        db = new IndexDB();
        boolean success = db.load(fileName + "i");
        if (!success) return false;
        db.buildHT();

        if (!loadDB(fileName)) return false;

        init();
        return true;
    }

    /**
     * Loads DP table from file to memory.  Note stored as N x N matrix where N is number of regions.
     * This occupies significant space as N gets large, especially because the matrix is sparse.
     *
     * @param fileName
     * @return
     */
    private boolean loadDB(String fileName) {    // Load dynamic programming table and records
        Scanner sc = null;
        boolean success = true;

        records.clear();
        try {
            sc = new Scanner(new File(fileName));
            long currentTime = System.currentTimeMillis();
            numGroups = Integer.parseInt(sc.nextLine());
            lowestCost = new int[numGroups][numGroups];
            paths = new int[numGroups][numGroups][];
            neighbor = new int[numGroups][numGroups];
            for (int i = 0; i < numGroups; i++)
                for (int j = 0; j < numGroups; j++)
                    lowestCost[i][j] = sc.nextInt();
            for (int i = 0; i < numGroups; i++)
                for (int j = 0; j < numGroups; j++)
                    neighbor[i][j] = sc.nextInt();
            for (int i = 0; i < numGroups; i++)
                for (int j = 0; j < numGroups; j++) {
                    int pathSize = sc.nextInt();
                    if (pathSize == 0) paths[i][j] = null;
                    else {
                        paths[i][j] = new int[pathSize];
                        for (int k = 0; k < paths[i][j].length; k++)
                            paths[i][j][k] = sc.nextInt();
                    }
                }

            System.out.println("Loaded in " + (System.currentTimeMillis() - currentTime));
        } catch (FileNotFoundException e) {
            System.out.println("Did not find input file: " + e);
            success = false;
        } finally {
            if (sc != null) sc.close();
        }
        return success;
    }

    /**
     * Writes out the abstraction mapping and the DP table and base paths.
     * Cannot use exportDB() in SubgoalDBExact which writes out already computed records.
     */
    @Override
    public void exportDB(String fileName) {
        // Export the index mapping part of the database
        db.export(fileName + "i");    // A bad file name - I know

        // Export the base "database" of record fragments and dynamic programming table
        saveDB(fileName);
    }

    /**
     * Saves the DP table as several N x N matrices.  (space costly for large N)
     *
     * @param fileName
     */
    private void saveDB(String fileName) {    // Save dynamic programming table and records
        // Format: numGroups
        //		lowestCost matrix (numGroups x numGroups)
        //		neighbor matrix (numGroups x numGroups)
        // 		paths matrix (with paths). Each path on a line.  A path is a list of subgoals.  Just have 0 if no states.
        try (PrintWriter out = new PrintWriter(fileName)) {
            out.println(numGroups);
            for (int i = 0; i < numGroups; i++) {
                for (int j = 0; j < numGroups; j++)
                    out.print(lowestCost[i][j] + "\t");
                out.println();
            }
            for (int i = 0; i < numGroups; i++) {
                for (int j = 0; j < numGroups; j++)
                    out.print(neighbor[i][j] + "\t");
                out.println();
            }
            for (int i = 0; i < numGroups; i++) {
                for (int j = 0; j < numGroups; j++) {
                    if (paths[i][j] == null) out.println("0");
                    else {
                        out.print(paths[i][j].length);
                        for (int k = 0; k < paths[i][j].length; k++)
                            out.print("\t" + paths[i][j][k]);
                        out.println();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error with output file: " + e);
        }
    }

    /**
     * Verifies only the index mapping.
     * Does not currently dynamically compute all records then verifies if they are correct.
     *
     * @param searchAlg
     */
    public void verify(SearchAlgorithm searchAlg) {
        db.verify(problem);
    }

    /**
     * Computes the dynamic programming table and base paths.
     * DP table is stored as full matrices.
     *
     * @param problem
     * @param groups
     * @param searchAlg
     * @param dbstats
     * @param numLevels
     */
    public void compute(SearchProblem problem, HashMap<Integer, GroupRecord> groups, SearchAlgorithm searchAlg, DBStatsRecord dbstats, int numLevels) {
        GroupRecord startGroup;
        numGroups = groups.size();
        lowestCost = new int[numGroups][numGroups];
        paths = new int[numGroups][numGroups][];
        neighbor = new int[numGroups][numGroups];
        HashSet<Integer> neighbors;
        long startTime = System.currentTimeMillis();

        long baseTime = GameDB.computeBasePaths(problem, groups, searchAlg, lowestCost, paths, neighbor, numGroups, numLevels, true, dbstats);

        long endTime, currentTime = System.currentTimeMillis();

        System.out.println("Performing dynamic programming to generate paths.");
        // Now the dynamic programming portion
        // Idea: Keep updating from neighbors until no further changes are made
        boolean changed = true;
        int numUpdates = 0;
        while (changed) {
            changed = false;

            for (int i = 0; i < numGroups; i++) {
                startGroup = groups.get(i + GameMap.START_NUM);
                // Process all neighbors of this node
                neighbors = GameDB.getNeighbors(groups, startGroup, numLevels);
                Iterator<Integer> it = neighbors.iterator();
                while (it.hasNext()) {
                    int neighborId = (Integer) it.next() - GameMap.START_NUM;
                    // Compute new costs for all locations based on value of neighbor
                    for (int j = 0; j < numGroups; j++) {
                        if (i == j) continue;
                        if (lowestCost[neighborId][j] > 0 && (lowestCost[i][j] == 0 || lowestCost[i][j] > lowestCost[i][neighborId] + lowestCost[neighborId][j])) {
                            changed = true;
                            lowestCost[i][j] = lowestCost[i][neighborId] + lowestCost[neighborId][j];
                            neighbor[i][j] = neighborId;
                            numUpdates++;
                        }
                    }
                }
            }
        }

        System.out.println("Number of cost updates: " + numUpdates);
        endTime = System.currentTimeMillis();
        long dpTime = endTime - currentTime;
        System.out.println("Time to compute paths via dynamic programming: " + dpTime);

        dbstats.addStat(16, baseTime);
        long overallTime = endTime - startTime;
        System.out.println("Total DB compute time: " + overallTime);
        dbstats.addStat(10, overallTime);
        dbstats.addStat(15, dpTime);
    }
}
