package database;

import map.GameMap;
import map.GroupRecord;
import search.AStar;
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
 * Database that is dynamically generated from dynamic programming table rather than storing records directly.
 * This version does not store an entire matrix in memory bur rather uses a compressed RLE form (using IndexDB) class to save space.
 * However, the DP table has been fully computed offline so this version is real-time.
 * DP table is stored as adjacency lists rather than full matrix to save space as table is sparse.  This is only difference from SubgoalDynamicDB class.
 *
 * @author rlawrenc
 */
public class SubgoalDynamicDB3 extends SubgoalDBExact {
    private int numGroups;
    private int[][][] paths;            // Used during run-time.  Direct paths between actual neighbors.
    private IndexDB neighbors;            // An RLE compressed mapping of the neighbors matrix.  Used during run-time as are three arrays below.
    private int[] numNeighbors;            // The number of neighbors for each group
    private int[][] neighborId;            // The neighbor id that the path is for.  E.g. paths[i][j] is a list of states ids for a path from i to some state j (not state id j).  neighborId[i][j] indicates what state it is.
    // Note this is necessary for supporting adjacency list of paths.

    /**
     * Produces a record by combining pre-computed base paths between adjacent regions into a complete path between two regions that map to start and goal of search problem.
     */
    public ArrayList<SubgoalDBRecord> findNearest(SearchProblem problem, SearchState start, SearchState goal, SearchAlgorithm searchAlg, int max, StatsRecord stats, ArrayList<SubgoalDBRecord> used) {
        int startGroupId = db.findHT(start.id);
        int goalGroupId = db.findHT(goal.id);

        ArrayList<SubgoalDBRecord> result = new ArrayList<SubgoalDBRecord>(1);

        // Need to calculate record as will not be stored
        int pathSize;
        int[] path = new int[2000], tmp = new int[2000];
        int[] subgoals;

        // This code builds only the path required on demand (may incur more time as have to continually merge paths but may save time by avoiding storing/copying lists to do construction)
        pathSize = GameDB.mergePaths5(startGroupId, goalGroupId, paths, neighbors, path, 0, neighborId, numGroups);

        if (pathSize == 0) return null;            // No path between two states
        int startId = path[0];
        int goalId = path[pathSize - 1];

        // Does not include start and goal
        subgoals = SearchUtil.computeSubgoalsBinaryByIds(path, searchAlg, tmp, pathSize);

        SubgoalDBRecord rec = new SubgoalDBRecord(1, startId, goalId, subgoals, 0);
        // System.out.println("Created record between: "+startId+" and "+goalId+" Record: "+rec.toString(problem));
        result.add(rec);
        return result;
    }

    /**
     * Initializes the dynamic programming table for querying.
     */
    public void init() {
        neighbors.buildHT();
    }

    /**
     * Loads DP table and mapping index from file to memory.
     */
    @Override
    public boolean load(String fileName) {
        // Load index first
        db = new IndexDB();
        boolean success = db.load(fileName + "i3");
        if (!success) return false;
        neighbors = new IndexDB();
        success = neighbors.load(fileName + "idp3");
        if (!success) return false;
        db.buildHT();

        if (!loadDB(fileName)) return false;

        init();
        return true;
    }

    /**
     * Loads DP table from file to memory.  Neighbor matrix stored in compress RLE form rather than as N x N matrix.
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
            paths = new int[numGroups][][];
            neighborId = new int[numGroups][];
            for (int i = 0; i < numGroups; i++) {    // Read each group which has just list of paths to its immediate neighbors
                int numNeighbors = sc.nextInt();
                neighborId[i] = new int[numNeighbors];
                paths[i] = new int[numNeighbors][];

                for (int j = 0; j < numNeighbors; j++)
                    neighborId[i][j] = sc.nextInt();

                for (int j = 0; j < numNeighbors; j++) {
                    int pathSize = sc.nextInt();
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

    public IndexDB getNeighborIndexDB() {
        return this.neighbors;
    }

    /**
     * Writes out the abstraction mapping and the DP table and base paths.
     * Cannot use exportDB() in SubgoalDBExact which writes out already computed records.
     */
    @Override
    public void exportDB(String fileName) {
        // Export the index mapping part of the database
        db.export(fileName + "i3");

        // Export the neighbors mapping table
        neighbors.export(fileName + "idp3");

        // Export the base "database" of record fragments and dynamic programming table
        saveDB(fileName);
    }

    /**
     * Saves the base paths and neighbors in a file.  Rest of data stored in compressed mapping index (abstraction mapping, and neighbors matrix).
     *
     * @param fileName
     */
    private void saveDB(String fileName) {    // Save dynamic programming table and records
        // Format: numGroups
        //		neighbor matrix (numGroups x []) - just stores direct neighbors
        // 		paths matrix (with paths). Each path on a line.  A path is a list of subgoals.  Just have 0 if no states.
        PrintWriter out = null;
        try {
            out = new PrintWriter(fileName);
            out.println(numGroups);
            for (int i = 0; i < numGroups; i++) {    // Read each group
                int numNeighbors = this.numNeighbors[i];
                out.println(numNeighbors);                    // Don't actually save out neighbors array as that is stored else where
                for (int j = 0; j < numNeighbors; j++)        // Save list of neighbor ids mapping to each path in path array
                    out.print(neighborId[i][j] + "\t");
                out.println();
                for (int j = 0; j < numNeighbors; j++)        // Write out neighbor paths
                {
                    out.print(paths[i][j].length + "\t");
                    for (int k = 0; k < paths[i][j].length; k++)
                        out.print("\t" + paths[i][j][k]);
                    out.println();
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error with output file: " + e);
        } finally {
            if (out != null) out.close();
        }
    }

    /**
     * Verifies only the index mapping.
     * Does not currently dynamically compute all records then verifies if they are correct.
     *
     * @param map
     * @param searchAlg
     */
    public void verify(SearchAlgorithm searchAlg) {
        db.verify(problem);
    }

    /**
     * Computes the DP table as full matrices during computation for lowestCost/neighbor arrays and adjacency lists for paths and neighbors.
     * Drops the matrices during run-time and using compressed RLE representation instead.
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
        HashSet<Integer> neighbors;

        numGroups = groups.size();
        int[][] lowestCost = new int[numGroups][numGroups];        // These are used as full matrices by the dynamic programming cannot be adjacency lists.
        int[][] neighbor = new int[numGroups][numGroups];

        paths = new int[numGroups][][];                                // These are used as adjacency lists as they are used at run-time.  Not needed by the DP.
        neighborId = new int[numGroups][];
        numNeighbors = new int[numGroups];

        long startTime = System.currentTimeMillis();

        long baseTime = computeBasePaths2(problem, groups, this, searchAlg, lowestCost, paths, neighbor, numGroups, numLevels, true, dbstats);

        long endTime = System.currentTimeMillis();

        dbstats.addStat(16, baseTime);

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

        // Now compress DP matrix (neighbor) using RLE
        compressDP(neighbor);
        endTime = System.currentTimeMillis();

        long dpTime = endTime - System.currentTimeMillis();
        System.out.println("Time to compute paths via dynamic programming: " + dpTime);
        dbstats.addStat(15, dpTime);
        long overallTime = endTime - startTime;
        System.out.println("Total DB compute time: " + overallTime);
        dbstats.addStat(10, overallTime);
    }

    public void compressDP(int[][] neighbor) {
        neighbors = new IndexDB();
        int lastVal = -1;

        for (int i = 0; i < numGroups; i++) {
            for (int j = 0; j < numGroups; j++) {
                int id = i * numGroups + j;
                // System.out.print(neighbor[i][j]+"\t");
                if (neighbor[i][j] != lastVal) {
                    neighbors.add(id, neighbor[i][j]);
                    lastVal = neighbor[i][j];
                }
            }
            // System.out.println();
        }
    }

    // This version does not assume a full matrix but rather an adjacency list representation
    public long computeBasePaths2(SearchProblem problem, HashMap<Integer, GroupRecord> groups, SubgoalDB db, SearchAlgorithm searchAlg, int[][] lowestCost, int[][][] paths, int[][] neighbor, int numGroups, int numLevels, boolean asSubgoals, DBStatsRecord dbstats) {
        int goalGroupLoc, startGroupLoc;
        GroupRecord startGroup, goalGroup;
        HashSet<Integer> neighbors;
        AStar astar = new AStar(problem);
        ArrayList<SearchState> path;
        StatsRecord stats = new StatsRecord();
        int numBase = 0;

        System.out.println("Number of groups: " + numGroups);
        long currentTime = System.currentTimeMillis();

        int[] tmp = new int[5000];
        System.out.println("Creating base paths to neighbors.");
        int numStates = 0;
        for (int i = 0; i < numGroups; i++) {
            startGroup = groups.get(i + GameMap.START_NUM);
            startGroupLoc = i;

            neighbors = GameDB.getNeighbors(groups, startGroup, numLevels);
            numNeighbors[i] = neighbors.size();
            neighborId[startGroupLoc] = new int[numNeighbors[i]];
            paths[startGroupLoc] = new int[numNeighbors[i]][];

            Iterator<Integer> it = neighbors.iterator();
            // Generate for each neighbor group
            int count = 0;
            while (it.hasNext()) {
                // Compute shortest path between center representative of both groups
                int goalGroupId = (Integer) it.next();
                goalGroup = groups.get(goalGroupId);

                path = astar.computePath(new SearchState(startGroup.groupRepId), new SearchState(goalGroup.groupRepId), stats);
                numBase++;

                goalGroupLoc = goalGroupId - GameMap.START_NUM;

                // Save information
                SearchUtil.computePathCost(path, stats, problem);
                int pathCost = stats.getPathCost();
                neighborId[startGroupLoc][count] = goalGroupLoc;
                lowestCost[startGroupLoc][goalGroupLoc] = pathCost;
                neighbor[startGroupLoc][goalGroupLoc] = goalGroupLoc;
                if (asSubgoals) {
                    paths[startGroupLoc][count] = SubgoalDB.convertPathToIds(path);
                    paths[startGroupLoc][count] = SearchUtil.compressPath(paths[startGroupLoc][count], searchAlg, tmp, path.size());
                    numStates += paths[startGroupLoc][count].length;
                } else {
                    paths[startGroupLoc][count] = SubgoalDB.convertPathToIds(path);
                    numStates += path.size();
                }
                count++;
            }
        }

        long endTime = System.currentTimeMillis();
        long baseTime = endTime - currentTime;
        System.out.println("Time to compute base paths: " + (baseTime));
        System.out.println("Base neighbors generated paths: " + numBase + " Number of states: " + numStates);
        dbstats.addStat(9, numStates);        // Set number of subgoals.  Will be changed by a version that pre-computes all paths but will not be changed for the dynamic version.
        dbstats.addStat(8, numBase);        // # of records (only corresponds to base paths)
        return baseTime;
    }

}
