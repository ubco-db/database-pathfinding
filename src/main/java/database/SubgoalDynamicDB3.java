package database;

import map.GameMap;
import map.GroupRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class SubgoalDynamicDB3 extends SubgoalDB {
    private int numGroups;              // Number of abstract regions
    private int[][] neighborId;         // neighborId[i] stores list of neighbors for i. neighborId[i][j] is state id of jth neighbor of i.
    private int[][] lowestCost;         // Lowest cost for DP table. lowestCost[i][j] is the cost of the lowest path from region i to region neighborId[i][j]
    private int[][][] paths;            // paths[i][j] is array representing a compressed path of state ids from region i to region neighborId[i][j] of lowest cost path
    int[] freeSpace;
    int freeSpaceCount;

    private static final Logger logger = LogManager.getLogger(SubgoalDynamicDB2.class);

    // TODO: How to do this without DP table?

    /**
     * Returns record for start and goal for search problem between two regions.
     * Record produced dynamically from data in DP table by combining base paths between regions (non-real-time).
     */
    public ArrayList<SubgoalDBRecord> findNearest(SearchProblem problem, int startGroupId, int goalGroupId, SearchAlgorithm searchAlg, int max, StatsRecord stats, ArrayList<SubgoalDBRecord> used) {
        // FIXME: This is broken right now, need to make this work without DP table

        ArrayList<SubgoalDBRecord> result = new ArrayList<>(1);

        // Need to calculate record as will not be stored
        int pathSize;
        int[] path = new int[2000], tmp = new int[2000];
        int[] subgoals;

        // Passing startId and goalId that are the same breaks this (happens if start and goal are in same region)
        // If start and goal are in same region, running A* to find path instead of DBA*
        if (startGroupId == goalGroupId) {
            // Tried HC before, but this doesn't work in all cases
            AStar aStar = new AStar(problem);
            ArrayList<SearchState> startRegionPath = aStar.computePath(new SearchState(startGroupId), new SearchState(goalGroupId), stats);
            pathSize = startRegionPath.size();
            for (int i = 0; i < pathSize; i++) {
                path[i] = startRegionPath.get(i).getId();
            }
        } else {
            // This code builds only the path required on demand (may incur more time as have to continually merge paths but may save time by avoiding storing/copying lists to do construction)
            pathSize = GameDB.mergePaths4(startGroupId, goalGroupId, paths, lowestCost, neighborId, path);
        }

        // System.out.println(Arrays.toString(path));

        if (pathSize == 0) return null; // No path between two states
        int startId = path[0];
        int goalId = path[pathSize - 1];

        subgoals = SearchUtil.computeSubgoalsBinaryByIds(path, searchAlg, tmp, pathSize);

        SubgoalDBRecord rec = new SubgoalDBRecord(startId, goalId, subgoals, 0);
        // System.out.println("Created record between: "+startId+" and "+goalId+" Record: "+rec.toString(problem));
        result.add(rec);
        return result;
    }

    /**
     * Initializes the dynamic programming table for querying.
     * Currently just a placeholder.
     */
    public void init() {
    }


    /**
     * Loads DP table from file to memory.  DP table stored in adjacency list form to save space.
     *
     * @param fileName
     * @return boolean
     */
    private boolean loadDB(String fileName) {    // Load dynamic programming table and records
        Scanner sc = null;
        boolean success = true;

        records.clear();
        try {
            sc = new Scanner(new File(fileName));
            long currentTime = System.currentTimeMillis();
            numGroups = Integer.parseInt(sc.nextLine());
            lowestCost = new int[numGroups][];
            paths = new int[numGroups][][];
            neighborId = new int[numGroups][];
            for (int i = 0; i < numGroups; i++) {    // Read each group which has # neighbors as N, neighborId[N], lowest cost[N] and paths on each line
                int numNeighbors = sc.nextInt();
                lowestCost[i] = new int[numNeighbors];
                neighborId[i] = new int[numNeighbors];
                paths[i] = new int[numNeighbors][];

                for (int j = 0; j < numNeighbors; j++)
                    neighborId[i][j] = sc.nextInt();
                for (int j = 0; j < numNeighbors; j++)
                    lowestCost[i][j] = sc.nextInt();
                for (int j = 0; j < numNeighbors; j++) {
                    int pathSize = sc.nextInt();
                    paths[i][j] = new int[pathSize];
                    for (int k = 0; k < paths[i][j].length; k++)
                        paths[i][j][k] = sc.nextInt();
                }
            }
            logger.info("Loaded in " + (System.currentTimeMillis() - currentTime));
        } catch (FileNotFoundException e) {
            logger.error("Did not find input file: " + e);
            success = false;
        } finally {
            if (sc != null) sc.close();
        }
        return success;
    }

    /**
     * Saves the DP table as several adjacency lists (uses less space as DP table gets large).
     *
     * @param fileName
     */
    private void saveDB(String fileName) {    // Save dynamic programming table and records (.dat file)
        // Format: numGroups
        //		neighborId matrix (numGroups x numGroups)
        //		lowestCost matrix (numGroups x numGroups)
        // 		paths matrix (with paths). Each path on a line.  A path is a list of subgoals.  Just have 0 if no states.
        try (PrintWriter out = new PrintWriter(fileName)) {
            // out.println("numGroups: ");
            out.println(numGroups);
            for (int i = 0; i < numGroups; i++) {    // Read each group which has # neighbors as N, neighborId[N], lowest cost[N], neighbor[] and paths on each line
                int numNeighbors = neighborId[i].length;
                // out.println("numNeighbours for " + i + ": ");
                out.println(numNeighbors);
                // out.println("neighbourIds: ");
                for (int j = 0; j < numNeighbors; j++) {
                    out.print(neighborId[i][j] + "\t");
                }
                out.println();
                // out.println("lowestCosts: ");
                for (int j = 0; j < numNeighbors; j++) {
                    out.print(lowestCost[i][j] + "\t");
                }
                out.println();
                // out.println("paths: ");
                for (int j = 0; j < numNeighbors; j++) {
                    out.print(paths[i][j].length + "\t");
                    for (int k = 0; k < paths[i][j].length; k++)
                        out.print("\t" + paths[i][j][k]);
                    out.println();
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("Error with output file: " + e);
        }
    }

    public void compute(SearchProblem problem, TreeMap<Integer, GroupRecord> groups, SearchAlgorithm searchAlg, DBStatsRecord dbStats, int numLevels) {
        numGroups = groups.size();
        // Allocate arrays 10% larger than the current numRegions
        int arraySize = (int) (numGroups * 1.1);
        lowestCost = new int[arraySize][];
        paths = new int[arraySize][][];
        neighborId = new int[arraySize][];

        // How big should I make this? Technically, we could wipe out all regions, in which case freeSpace would be filled up to arraySize
        freeSpace = new int[arraySize];
        freeSpaceCount = arraySize - numGroups;
        // Initialize free space to contain indices of final 10% for arrays of length arraySize
        for (int i = 0; i < freeSpaceCount; i++) {
            freeSpace[i] = numGroups + i;
        }

        logger.debug(Arrays.toString(freeSpace));

        long startTime = System.currentTimeMillis();

        long baseTime = computeBasePaths2(problem, groups, searchAlg, numGroups, numLevels, true, dbStats);

        long endTime = System.currentTimeMillis();

        dbStats.addStat(16, baseTime);
        long overallTime = endTime - startTime;
        logger.info("Total DB compute time: " + overallTime);
        dbStats.addStat(10, overallTime);
    }

    /**
     * Computes the dynamic programming table and base paths.
     * DP table is stored as an adjacency list representation
     *
     * @param problem
     * @param groups
     * @param searchAlg
     * @param dbStats
     * @param numLevels
     */
    public long computeBasePaths2(SearchProblem problem, TreeMap<Integer, GroupRecord> groups, SearchAlgorithm searchAlg, int numGroups, int numLevels, boolean asSubgoals, DBStatsRecord dbStats) {
        int goalGroupLoc, startGroupLoc;
        GroupRecord startGroup, goalGroup;
        HashSet<Integer> neighbors;
        AStar astar = new AStar(problem);
        ArrayList<SearchState> path;
        StatsRecord stats = new StatsRecord();
        int numBase = 0;

        logger.info("Number of groups: " + numGroups);
        long currentTime = System.currentTimeMillis();

        int[] tmp = new int[5000];
        logger.debug("Creating base paths to neighbors.");
        int numStates = 0;

        for (int i = 0; i < numGroups; i++) {
            startGroup = groups.get(i + GameMap.START_NUM);
            startGroupLoc = i;

            neighbors = GameDB.getNeighbors(groups, startGroup, numLevels, false);

            int numNeighbors = neighbors.size();
            lowestCost[startGroupLoc] = new int[numNeighbors];
            neighborId[startGroupLoc] = new int[numNeighbors];
            paths[startGroupLoc] = new int[numNeighbors][];

            Iterator<Integer> it = neighbors.iterator();
            // Generate for each neighbor group
            int count = 0;

            // Iterate over neighbours of current region
            while (it.hasNext()) {
                // Compute the shortest path between center representative of both groups
                int goalGroupId = it.next();
                goalGroup = groups.get(goalGroupId);

                path = astar.computePath(new SearchState(startGroup.groupRepId), new SearchState(goalGroup.groupRepId), stats);
                numBase++; // Is this the number of paths?

                goalGroupLoc = goalGroupId - GameMap.START_NUM;

                // Save information
                SearchUtil.computePathCost(path, stats, problem);
                int pathCost = stats.getPathCost();
                neighborId[startGroupLoc][count] = goalGroupLoc;
                lowestCost[startGroupLoc][count] = pathCost;

                if (asSubgoals) { // This is always true?
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
        logger.info("Time to compute base paths: " + (baseTime));
        logger.info("Base neighbors generated paths: " + numBase + " Number of states: " + numStates);
        dbStats.addStat(9, numStates);        // Set number of subgoals.  Will be changed by a version that pre-computes all paths but will not be changed for the dynamic version.
        dbStats.addStat(8, numBase);          // # of records (only corresponds to base paths)
        return baseTime;
    }

    /**
     * Recomputes the dynamic programming table and base paths.
     * DP table is stored as an adjacency list representation
     *
     * @param problem
     * @param groups
     * @param searchAlg
     * @param numLevels
     */
    public void recomputeBasePathsAfterWallChange(SearchProblem problem, TreeMap<Integer, GroupRecord> groups,
                                                  ArrayList<Integer> neighbourIndices, SearchAlgorithm searchAlg,
                                                  int numGroups, int numLevels,
                                                  boolean isElimination, boolean isPartition) {
        // If we have run out of free space, increase the size of the arrays
        if (freeSpaceCount == 0) {
            // Allocate arrays 10% larger than the current numRegions
            int arraySize = (int) (numGroups * 1.1);

            int[][] resizedLowestCost = new int[arraySize][];
            System.arraycopy(this.lowestCost, 0, resizedLowestCost, 0, this.lowestCost.length);
            this.lowestCost = resizedLowestCost;

            int[][][] resizedPath = new int[arraySize][][];
            System.arraycopy(this.paths, 0, resizedPath, 0, this.paths.length);
            this.paths = resizedPath;

            int[][] resizedNeighborId = new int[arraySize][];
            System.arraycopy(this.neighborId, 0, resizedNeighborId, 0, this.neighborId.length);
            this.neighborId = resizedNeighborId;

            logger.warn("Arrays have been resized since there was no more free space.");

            // TODO: Test whether resizing here works as expected and whether it's necessary
            this.freeSpaceCount = arraySize - numGroups;
            int[] resizedFreeSpace = new int[arraySize];
            System.arraycopy(this.freeSpace, 0, resizedFreeSpace, 0, this.freeSpace.length);
            this.freeSpace = resizedFreeSpace;
        }

        int goalGroupLoc, startGroupLoc;
        GroupRecord startGroup, goalGroup;
        HashSet<Integer> neighbors;
        AStar astar = new AStar(problem);
        ArrayList<SearchState> path;
        StatsRecord stats = new StatsRecord();
        int numBase = 0;

        logger.debug("Number of groups to recompute: " + numGroups);
        long currentTime = System.currentTimeMillis();

        int[] tmp = new int[5000];
        logger.debug("Re-creating base paths to neighbors.");
        int numStates = 0;

        for (Integer neighbourIndex : neighbourIndices) {
            startGroup = groups.get(neighbourIndex);
            startGroupLoc = neighbourIndex - GameMap.START_NUM;

            if (startGroup == null || neighbourIndices.size() == 1) {
                // Need to initialize arrays so singleton regions work in wall removal
                this.lowestCost[startGroupLoc] = new int[0];
                neighborId[startGroupLoc] = new int[0];
                this.paths[startGroupLoc] = new int[0][];
                continue;
            }

            // TODO: could probably simplify this code since we are not taking advantage of numLevels currently anyways
            neighbors = GameDB.getNeighbors(groups, startGroup, numLevels, isPartition);
            int numNeighbors = neighbors.size();

            if (isElimination) {
                numNeighbors -= 1;
            }

            this.lowestCost[startGroupLoc] = new int[numNeighbors];
            neighborId[startGroupLoc] = new int[numNeighbors];
            this.paths[startGroupLoc] = new int[numNeighbors][];

            Iterator<Integer> it = neighbors.iterator();
            // Generate for each neighbor group
            int count = 0;
            while (it.hasNext()) {
                // Compute the shortest path between center representative of both groups
                int goalGroupId = it.next();
                goalGroup = groups.get(goalGroupId);

                if (goalGroup != null) {
                    path = astar.computePath(new SearchState(startGroup.groupRepId), new SearchState(goalGroup.groupRepId), stats);
                    numBase++;
                    goalGroupLoc = goalGroupId - GameMap.START_NUM;

                    // Save information
                    SearchUtil.computePathCost(path, stats, problem);
                    int pathCost = stats.getPathCost();

                    neighborId[startGroupLoc][count] = goalGroupLoc;
                    this.lowestCost[startGroupLoc][count] = pathCost;
                    this.paths[startGroupLoc][count] = SubgoalDB.convertPathToIds(path);
                    this.paths[startGroupLoc][count] = SearchUtil.compressPath(this.paths[startGroupLoc][count], searchAlg, tmp, path.size());
                    numStates += this.paths[startGroupLoc][count].length;
                    count++;
                }
            }
        }

        this.numGroups = groups.size();

//        long endTime = System.currentTimeMillis();
//        long baseTime = endTime - currentTime;
//        logger.info("Time to re-compute base paths: " + (baseTime));
//        logger.info("Base neighbors generated paths: " + numBase + " Number of states: " + numStates);
//        dbStats.addStat(9, numStates);        // Set number of subgoals.  Will be changed by a version that pre-computes all paths but will not be changed for the dynamic version.
//        dbStats.addStat(8, numBase);          // # of records (only corresponds to base paths)
    }
}
