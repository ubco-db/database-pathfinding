package database;

import map.GameMap;
import map.GroupRecord;
import search.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;


/**
 * Database where dynamic programming table is not computed offline only the base paths between adjacency neighbors are (and their associated costs).
 * Online, a record is produced by searching the partial complete DP table for the lowest cost path between regions i and j.
 * This path consists of a series of hops between neighbors and each hop's path is combined into a path to solve the entire problem.
 * In effect, this is performing another search on the abstract region space. The algorithm is currently using Dijkstra's but A* may be possible as well.
 * This search is no longer real-time (as number of regions cannot be bounded a priori), so any search using this database cannot also be considered real-time.
 * The savings are that no DP computation needs to be performed which speeds up things when there are a large number of regions and potentially can be useful when
 * the state space is changing.
 * DP table only stores direct neighbors (not entire matrix).
 *
 * @author rlawrenc
 */
public class SubgoalDynamicDB2 extends SubgoalDBExact {
    private int numGroups;              // Number of abstract regions
    private int[][] neighborId;         // neighborId[i] stores list of neighbors for i. neighborId[i][j] is state id of jth neighbor of i.
    private int[][] lowestCost;         // Lowest cost for DP table. lowestCost[i][j] is the cost of the lowest path from region i to region neighborId[i][j]
    private int[][][] paths;            // paths[i][j] is array representing a compressed path of state ids from region i to region neighborId[i][j] of lowest cost path
    private int[][] neighbor;           // neighbor[i][j] is region id of next region to visit on lowest cost path from region i to region neighborId[i][j] (the next hop)


    /**
     * Returns record for start and goal for search problem between two regions.
     * Record produced dynamically from data in DP table by combining base paths between regions (non-real-time).
     */
    public ArrayList<SubgoalDBRecord> findNearest(SearchProblem problem, SearchState start, SearchState goal, SearchAlgorithm searchAlg, int max, StatsRecord stats, ArrayList<SubgoalDBRecord> used) {
        int startGroupId = db.findHT(start.id);
        int goalGroupId = db.findHT(goal.id);

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
            ArrayList<SearchState> startRegionPath = aStar.computePath(start, goal, stats);
            pathSize = startRegionPath.size();
            for (int i = 0; i < pathSize; i++) {
                path[i] = startRegionPath.get(i).getId();
            }
        } else {
            // This code builds only the path required on demand (may incur more time as have to continually merge paths but may save time by avoiding storing/copying lists to do construction)
            pathSize = GameDB.mergePaths4(startGroupId, goalGroupId, paths, neighbor, lowestCost, neighborId, path);
        }

        // System.out.println(Arrays.toString(path));

        if (pathSize == 0) return null;            // No path between two states
        int startId = path[0];
        int goalId = path[pathSize - 1];

        // Does not include start and goal
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
     * Loads DP table and mapping index from file to memory.
     */
    @Override
    public boolean load(String fileName) {
        // Load index first
        db = new IndexDB();
        boolean success = db.load(fileName + "i2");
        if (!success) return false;
        db.buildHT();

        if (!loadDB(fileName)) return false;

        init();
        return true;
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
            neighbor = new int[numGroups][];
            neighborId = new int[numGroups][];
            for (int i = 0; i < numGroups; i++) {    // Read each group which has # neighbors as N, neighborId[N], lowest cost[N], neighbor[] and paths on each line
                int numNeighbors = sc.nextInt();
                lowestCost[i] = new int[numNeighbors];
                neighborId[i] = new int[numNeighbors];
                neighbor[i] = new int[numNeighbors];
                paths[i] = new int[numNeighbors][];

                for (int j = 0; j < numNeighbors; j++)
                    neighborId[i][j] = sc.nextInt();
                for (int j = 0; j < numNeighbors; j++)
                    lowestCost[i][j] = sc.nextInt();
                for (int j = 0; j < numNeighbors; j++)
                    neighbor[i][j] = sc.nextInt();
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

    /**
     * Writes out the abstraction mapping and the DP table and base paths.
     * Cannot use exportDB() in SubgoalDBExact which writes out already computed records.
     */
    @Override
    public void exportDB(String fileName) {
        // Export the index mapping part of the database
        db.export(fileName + "i2");    // A bad file name - I know

        // Export the base "database" of record fragments and dynamic programming table
        saveDB(fileName);
    }

    /**
     * Saves the DP table as several adjacency lists (uses less space as DP table gets large).
     *
     * @param fileName
     */
    private void saveDB(String fileName) {    // Save dynamic programming table and records (.dat file)
        // Format: numGroups
        //		lowestCost matrix (numGroups x numGroups)
        //		neighbor matrix (numGroups x numGroups)
        // 		paths matrix (with paths). Each path on a line.  A path is a list of subgoals.  Just have 0 if no states.
        try (PrintWriter out = new PrintWriter(fileName)) {
            // out.println("numGroups: ");
            out.println(numGroups);
            for (int i = 0; i < numGroups; i++) {    // Read each group which has # neighbors as N, neighborId[N], lowest cost[N], neighbor[] and paths on each line
                if (neighbor[i] != null) {
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
                    // out.println("neighbours: ");
                    for (int j = 0; j < numNeighbors; j++) {
                        out.print(neighbor[i][j] + "\t");
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

    public void compute(SearchProblem problem, TreeMap<Integer, GroupRecord> groups, SearchAlgorithm searchAlg, DBStatsRecord dbStats, int numLevels) {
        numGroups = groups.size();
        lowestCost = new int[numGroups][];
        paths = new int[numGroups][][];
        neighbor = new int[numGroups][];
        neighborId = new int[numGroups][];
        long startTime = System.currentTimeMillis();

        long baseTime = computeBasePaths2(problem, groups, searchAlg, lowestCost, paths, neighbor, numGroups, numLevels, true, dbStats);

        long endTime = System.currentTimeMillis();

        dbStats.addStat(16, baseTime);
        long overallTime = endTime - startTime;
        System.out.println("Total DB compute time: " + overallTime);
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
    public long computeBasePaths2(SearchProblem problem, TreeMap<Integer, GroupRecord> groups, SearchAlgorithm searchAlg, int[][] lowestCost, int[][][] paths, int[][] neighbor, int numGroups, int numLevels, boolean asSubgoals, DBStatsRecord dbStats) {
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

            neighbors = GameDB.getNeighbors(groups, startGroup, numLevels, false);
            int numNeighbors = neighbors.size();
            lowestCost[startGroupLoc] = new int[numNeighbors];
            neighbor[startGroupLoc] = new int[numNeighbors];
            neighborId[startGroupLoc] = new int[numNeighbors];
            paths[startGroupLoc] = new int[numNeighbors][];

            Iterator<Integer> it = neighbors.iterator();
            // Generate for each neighbor group
            int count = 0;
            while (it.hasNext()) {
                // Compute the shortest path between center representative of both groups
                int goalGroupId = it.next();
                goalGroup = groups.get(goalGroupId);

                path = astar.computePath(new SearchState(startGroup.groupRepId), new SearchState(goalGroup.groupRepId), stats);
                numBase++;

                goalGroupLoc = goalGroupId - GameMap.START_NUM;

                // Save information
                SearchUtil.computePathCost(path, stats, problem);
                int pathCost = stats.getPathCost();
                neighborId[startGroupLoc][count] = goalGroupLoc;
                lowestCost[startGroupLoc][count] = pathCost;
                neighbor[startGroupLoc][count] = goalGroupLoc;
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
        dbStats.addStat(9, numStates);        // Set number of subgoals.  Will be changed by a version that pre-computes all paths but will not be changed for the dynamic version.
        dbStats.addStat(8, numBase);          // # of records (only corresponds to base paths)
        return baseTime;
    }

    public int[][] getLowestCost() {
        return lowestCost;
    }

    public int[][][] getPaths() {
        return paths;
    }

    public int[][] getNeighbor() {
        return neighbor;
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
                                                  int[][] lowestCost, int[][][] paths, int[][] neighbor,
                                                  int numGroups, int numLevels,
                                                  boolean isElimination, boolean isPartition) {
        /*
        Currently, I am deleting all regions within a sector and recomputing the whole sector. In the case where the
        regions do not merge, and the number of regions therefore stays the same, this leads to an off-by-one error when
        re-assigning region. This is the case because after removing the regions, they are assigned new indices at the
        end of the groups TreeMap. The indices in this map are in order, but its length plus the offset is not the same
        as the index of its last element anymore.

        Therefore, I need to use groups.lastKey() + 1 rather than groups.size() for this check or one of the removal
        cases won't work.
         */
        if (lowestCost.length < groups.lastKey() + 1) {
            int[][] resizedLowestCost = new int[groups.lastKey() + 1][];
            System.arraycopy(lowestCost, 0, resizedLowestCost, 0, lowestCost.length);
            this.lowestCost = resizedLowestCost;
        }
        if (paths.length < groups.lastKey() + 1) {
            int[][][] resizedPath = new int[groups.lastKey() + 1][][];
            System.arraycopy(paths, 0, resizedPath, 0, paths.length);
            this.paths = resizedPath;
        }
        if (neighbor.length < groups.lastKey() + 1) {
            int[][] resizedNeighbor = new int[groups.lastKey() + 1][];
            System.arraycopy(neighbor, 0, resizedNeighbor, 0, neighbor.length);
            this.neighbor = resizedNeighbor;
        }
        if (neighborId.length < groups.lastKey() + 1) {
            int[][] resizedNeighborId = new int[groups.lastKey() + 1][];
            System.arraycopy(neighborId, 0, resizedNeighborId, 0, neighborId.length);
            neighborId = resizedNeighborId;
        }

        int goalGroupLoc, startGroupLoc;
        GroupRecord startGroup, goalGroup;
        HashSet<Integer> neighbors;
        AStar astar = new AStar(problem);
        ArrayList<SearchState> path;
        StatsRecord stats = new StatsRecord();
        int numBase = 0;

        System.out.println("Number of groups to recompute: " + numGroups);
        long currentTime = System.currentTimeMillis();

        int[] tmp = new int[5000];
        System.out.println("Creating base paths to neighbors.");
        int numStates = 0;

        for (Integer neighbourIndex : neighbourIndices) {
            startGroup = groups.get(neighbourIndex); // will need to redo
            startGroupLoc = neighbourIndex - GameMap.START_NUM;

            if (startGroup == null || neighbourIndices.size() == 1) {
                // Need to initialize arrays so singleton regions work in wall removal
                this.lowestCost[startGroupLoc] = new int[0];
                this.neighbor[startGroupLoc] = new int[0];
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
            this.neighbor[startGroupLoc] = new int[numNeighbors];
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
                    this.neighbor[startGroupLoc][count] = goalGroupLoc;
                    this.paths[startGroupLoc][count] = SubgoalDB.convertPathToIds(path);
                    this.paths[startGroupLoc][count] = SearchUtil.compressPath(this.paths[startGroupLoc][count], searchAlg, tmp, path.size());
                    numStates += this.paths[startGroupLoc][count].length;
                    count++;
                }
            }
        }

        this.numGroups = groups.size();

        long endTime = System.currentTimeMillis();
        long baseTime = endTime - currentTime;
        System.out.println("Time to re-compute base paths: " + (baseTime));
        System.out.println("Base neighbors generated paths: " + numBase + " Number of states: " + numStates);
//        dbStats.addStat(9, numStates);        // Set number of subgoals.  Will be changed by a version that pre-computes all paths but will not be changed for the dynamic version.
//        dbStats.addStat(8, numBase);          // # of records (only corresponds to base paths)
    }
}
