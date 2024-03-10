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

    private static final Logger logger = LogManager.getLogger(SubgoalDynamicDB3.class);

    // TODO: How to do this without DP table?

    /**
     * Returns record for start and goal for search problem between two regions.
     * Record produced dynamically from data in DP table by combining base paths between regions (non-real-time).
     */
    public ArrayList<SubgoalDBRecord> findNearest(SearchProblem problem, int startGroupId, int goalGroupId, SearchAlgorithm searchAlg, StatsRecord stats) {
        // TODO: Check whether this actually works

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
            out.println("numGroups: ");
            out.println(numGroups);
            for (int i = 0; i < numGroups; i++) {    // Read each group which has # neighbors as N, neighborId[N], lowest cost[N], neighbor[] and paths on each line
                int numNeighbors = neighborId[i].length;
                out.println("numNeighbours for " + i + ": ");
                out.println(numNeighbors);
                out.println("neighbourIds: ");
                for (int j = 0; j < numNeighbors; j++) {
                    out.print(neighborId[i][j] + "\t");
                }
                out.println();
                out.println("lowestCosts: ");
                for (int j = 0; j < numNeighbors; j++) {
                    out.print(lowestCost[i][j] + "\t");
                }
                out.println();
                out.println("paths: ");
                for (int j = 0; j < numNeighbors; j++) {
                    if (paths[i][j] != null) {
                        out.print(paths[i][j].length + "\t");
                        for (int k = 0; k < paths[i][j].length; k++)
                            out.print("\t" + paths[i][j][k]);
                        out.println();
                    }
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
        // Initialize free space to contain indices of final 10% for arrays of length arraySize (in reverse order)
        for (int i = 0; i < freeSpaceCount; i++) {
            freeSpace[i] = arraySize - (i + 1);
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

    private void resizeFreeSpace() {
        if (freeSpaceCount == 0) {
            // Allocate arrays 10% larger than the current numRegions
            int arraySize = (int) (this.numGroups * 1.1);

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
            this.freeSpaceCount = arraySize - this.numGroups;
            int[] resizedFreeSpace = new int[arraySize];
            System.arraycopy(this.freeSpace, 0, resizedFreeSpace, 0, this.freeSpace.length);
            this.freeSpace = resizedFreeSpace;
        }
    }

    /**
     * @param regionId region id where wall was added
     */
    public void recomputeBasePathsAfterElimination(int regionId) throws Exception {
        // This is the elimination case, where adding a wall leads to the removal of a region

        // If we have run out of free space, increase the size of the arrays
        resizeFreeSpace();

        // Find array location of region
        int groupLoc = regionId - GameMap.START_NUM;

        // Iterate over neighbours of the region to eliminate to scrub references to it
        for (int i = 0; i < this.neighborId[groupLoc].length; i++) {
            // Grab location of neighbour
            int neighbourLoc = this.neighborId[groupLoc][i];
            // Iterate over neighbours of neighbour to find region to eliminate
            int indexOfRegionToEliminate = -1;
            for (int j = 0; j < this.neighborId[neighbourLoc].length; j++) {
                if (this.neighborId[neighbourLoc][j] == groupLoc) {
                    indexOfRegionToEliminate = j;
                    break;
                }
            }
            // If the region to eliminate was not stored as a neighbour of its neighbour
            if (indexOfRegionToEliminate == -1) {
                // If we get here, then the neighbour lists must be messed up, because one of the neighbours
                // of the region were eliminating did not have said region set as a neighbour
                logger.error("There is an issue with the neighbours of region: " + regionId);
                throw new Exception("There is an issue with the neighbours of region: " + regionId);
            }

            // Copying into smaller arrays here
            this.neighborId[neighbourLoc] = copyArrayExceptIndex(this.neighborId[neighbourLoc], indexOfRegionToEliminate);
            this.lowestCost[neighbourLoc] = copyArrayExceptIndex(this.lowestCost[neighbourLoc], indexOfRegionToEliminate);
            // Simply setting path to null (not copying paths array)
            this.paths[neighbourLoc][indexOfRegionToEliminate] = null;
        }
        // Tombstone eliminated region
        this.neighborId[groupLoc] = null;
        this.paths[groupLoc] = null;
        this.lowestCost[groupLoc] = null;
        // Update freeSpace
        pushFreeSpace(regionId);
    }

    /**
     * @param problem     MapSearchProblem used in A*
     * @param groups      groups mapping
     * @param neighborIds ArrayList containing the neighbourhood where the partition happened
     */
    public void recomputeBasePathsAfterPartition(MapSearchProblem problem, TreeMap<Integer, GroupRecord> groups, ArrayList<Integer> neighborIds) throws Exception {
        // This is the partition case, where adding a wall leads to the splitting of a region into two or more smaller regions

        // If we have run out of free space, increase the size of the arrays
        resizeFreeSpace();

        AStar astar = new AStar(problem);
        StatsRecord stats = new StatsRecord();
        ArrayList<SearchState> path;

        // freeSpace has already been updated in DBAStarUtil (needed the information for map updates)

        // regionIds contains the ids of all the regions the original region was split into after the partition
        // Use those to overwrite the neighborId arrays of the regions
        for (Integer id : neighborIds) {
            // Need to update neighborhoods of all the new regions
            int groupLoc = id - GameMap.START_NUM;

            // Get neighbours of the new/surrounding regions (updated in map.recomputeNeighbors)
            HashSet<Integer> neighbours = groups.get(id).getNeighborIds();
            // Create an int array with the same size as the HashSet
            int[] neighbourArray = new int[neighbours.size()];

            // Iterate through the HashSet and copy its elements to the array
            int index = 0;
            for (Integer neighbour : neighbours) {
                neighbourArray[index++] = neighbour - GameMap.START_NUM;
            }

            // Overwrite the neighbourId array of the region
            this.neighborId[groupLoc] = neighbourArray;
            // Create new lowest cost and paths arrays of correct size
            // FIXME: This is throwing away useful data, find a way to not to
            // all but the paths to the new regions should be unaffected, so throwing those away and recomputing them is a waste
            this.lowestCost[groupLoc] = new int[neighbours.size()];
            this.paths[groupLoc] = new int[neighbours.size()][];
        }

        for (Integer id : neighborIds) {
            // Iterate over neighbours of the region
            int groupLoc = id - GameMap.START_NUM;

            for (int i = 0; i < this.neighborId[groupLoc].length; i++) {
                // Grab location of neighbour
                int neighbourLoc = this.neighborId[groupLoc][i];
                int[] tmp = new int[5000];
                // TODO: May want to pass this as parameter
                SearchAlgorithm searchAlg = new HillClimbing(problem, 10000);

                path = astar.computePath(new SearchState(groups.get(id).groupRepId), new SearchState(groups.get(neighbourLoc + GameMap.START_NUM).groupRepId), stats);
                SearchUtil.computePathCost(path, stats, problem);
                int pathCost = stats.getPathCost();

                // Update lowestCost
                this.lowestCost[groupLoc][i] = pathCost;
                int indexToUpdate = -1;
                for (int j = 0; j < this.neighborId[neighbourLoc].length; j++) {
                    if (this.neighborId[neighbourLoc][j] == groupLoc) {
                        indexToUpdate = j;
                        break;
                    }
                }
                // If the region to update was not stored as a neighbour of its neighbour
                if (indexToUpdate == -1) {
                    // If we get here, then the neighbour lists must be messed up, because one of the neighbours
                    // of the region were eliminating did not have said region set as a neighbour
                    throw new Exception("There is an issue with the neighbours of region: " + groups.get(id).groupRepId);
                }
                // Update lowestCost of neighbour
                this.lowestCost[neighbourLoc][indexToUpdate] = pathCost;

                this.paths[groupLoc][i] = SearchUtil.compressPath(SubgoalDB.convertPathToIds(path), searchAlg, tmp, path.size());
            }
        }
    }

    /**
     * @param regionId region id where wall was added
     * @param problem  MapSearchProblem used in A*
     * @param groups   groups mapping
     */
    public void recomputeBasePaths(int regionId, MapSearchProblem problem, TreeMap<Integer, GroupRecord> groups) {
        // This is for all cases where the paths change but the neighbourhood does not:
        // E.g. wall on region rep, wall that moves region rep, wall that changes shortest path

        // If we have run out of free space, increase the size of the arrays
        resizeFreeSpace();

        // Find array location of region
        int groupLoc = regionId - GameMap.START_NUM;

        AStar astar = new AStar(problem);
        StatsRecord stats = new StatsRecord();
        ArrayList<SearchState> path;

        // Update region’s paths to its neighbours (and their costs)
        // Update the region’s neighbours paths to it (and their costs)
        for (int i = 0; i < this.neighborId[groupLoc].length; i++) {
            // Grab location of neighbour
            int neighbourLoc = this.neighborId[groupLoc][i];
            int[] tmp = new int[5000];
            // TODO: May want to pass this as parameter
            SearchAlgorithm searchAlg = new HillClimbing(problem, 10000);

            path = astar.computePath(new SearchState(groups.get(regionId).groupRepId), new SearchState(groups.get(neighbourLoc + GameMap.START_NUM).groupRepId), stats);
            SearchUtil.computePathCost(path, stats, problem);
            int pathCost = stats.getPathCost();

            // Update lowestCost of region
            this.lowestCost[groupLoc][i] = pathCost;
            // Update path to region
            this.paths[groupLoc][i] = SearchUtil.compressPath(SubgoalDB.convertPathToIds(path), searchAlg, tmp, path.size());

            path = astar.computePath(new SearchState(groups.get(neighbourLoc + GameMap.START_NUM).groupRepId), new SearchState(groups.get(regionId).groupRepId), stats);
            SearchUtil.computePathCost(path, stats, problem);
            pathCost = stats.getPathCost();

            // Need to find correct neighborId to update
            for (int j = 0; j < this.neighborId[neighbourLoc].length; j++) {
                if (this.neighborId[neighbourLoc][j] == groupLoc) {
                    // Update lowestCost of neighbour
                    this.lowestCost[neighbourLoc][j] = pathCost;
                    // Update path to neighbour
                    this.paths[neighbourLoc][j] = SearchUtil.compressPath(SubgoalDB.convertPathToIds(new ArrayList<>(path)), searchAlg, tmp, path.size());
                    break;
                }
            }
        }
        saveDB("checkingResultsRegionRep.txt");
    }

    /**
     * @param regionId region id where wall was added
     */
    public void recomputeBasePathsIfSolitary(int regionId) {
        // Case where new region has no neighbours (e.g. is surrounded by walls)

        // If we have run out of free space, increase the size of the arrays
        resizeFreeSpace();

        // freeSpace has already been updated in DBAStarUtil (needed the information for map updates)

        // Find array location of region
        int groupLoc = regionId - GameMap.START_NUM;

        // Create arrays for new group
        this.neighborId[groupLoc] = new int[0];
        this.lowestCost[groupLoc] = new int[0];
        this.paths[groupLoc] = new int[0][];
    }

    /**
     * @param regionId    region id where wall was added
     * @param problem     MapSearchProblem used in A*
     * @param groups      groups mapping
     * @param neighborIds ArrayList containing region ids of neighbour regions
     */
    public void recomputeBasePathsIfConnected(int regionId, MapSearchProblem problem, TreeMap<Integer, GroupRecord> groups, HashSet<Integer> neighborIds) {
        // Case where new region has neighbours (e.g. is in a new sector but connected)

        // If we have run out of free space, increase the size of the arrays
        resizeFreeSpace();

        // freeSpace has already been updated in DBAStarUtil (needed the information for map updates)

        // Find array location of region
        int groupLoc = regionId - GameMap.START_NUM;

        int numNeighbours = neighborIds.size();

        // Create arrays for new group
        this.neighborId[groupLoc] = new int[numNeighbours];
        this.lowestCost[groupLoc] = new int[numNeighbours];
        this.paths[groupLoc] = new int[numNeighbours][];

        AStar astar = new AStar(problem);
        StatsRecord stats = new StatsRecord();
        ArrayList<SearchState> path;

        // Update region’s paths to its neighbours (and their costs)
        // Update the region’s neighbours paths to it (and their costs)
        int i = 0;
        for (int neighbourId : neighborIds) {
            // Grab location of neighbour
            int neighbourLoc = neighbourId - GameMap.START_NUM;

            int[] tmp = new int[5000];
            // TODO: May want to pass this as parameter
            SearchAlgorithm searchAlg = new HillClimbing(problem, 10000);

            path = astar.computePath(new SearchState(groups.get(regionId).groupRepId), new SearchState(groups.get(neighbourLoc + GameMap.START_NUM).groupRepId), stats);
            SearchUtil.computePathCost(path, stats, problem);
            int pathCost = stats.getPathCost();

            this.neighborId[groupLoc][i] = neighbourLoc;
            // Update lowestCost of region
            this.lowestCost[groupLoc][i] = pathCost;
            // Update path to region
            this.paths[groupLoc][i] = SearchUtil.compressPath(SubgoalDB.convertPathToIds(path), searchAlg, tmp, path.size());

            // Need to increase size of arrays of neighbour
            int len = this.neighborId[neighbourLoc].length;

            // Resize arrays
            int[] resizedNeighbourId = new int[len + 1];
            System.arraycopy(this.neighborId[neighbourLoc], 0, resizedNeighbourId, 0, len);
            this.neighborId[neighbourLoc] = resizedNeighbourId;

            int[][] resizedPaths = new int[len + 1][];
            System.arraycopy(this.paths[neighbourLoc], 0, resizedPaths, 0, len);
            this.paths[neighbourLoc] = resizedPaths;

            int[] resizedCosts = new int[len + 1];
            System.arraycopy(this.lowestCost[neighbourLoc], 0, resizedCosts, 0, len);
            this.lowestCost[neighbourLoc] = resizedCosts;

            // Assign neighbourId
            this.neighborId[neighbourLoc][len] = groupLoc;

            path = astar.computePath(new SearchState(groups.get(neighbourLoc + GameMap.START_NUM).groupRepId), new SearchState(groups.get(regionId).groupRepId), stats);
            SearchUtil.computePathCost(path, stats, problem);
            pathCost = stats.getPathCost();

            // Assign lowest cost and new path
            this.lowestCost[neighbourLoc][len] = pathCost;
            this.paths[neighbourLoc][len] = SearchUtil.compressPath(SubgoalDB.convertPathToIds(path), searchAlg, tmp, path.size());

            i++;
        }
    }


    public void recomputeUnblocker(int regionId, int neighbourId, MapSearchProblem problem, TreeMap<Integer, GroupRecord> groups) throws Exception {
        // In the unblocker case, we have two regions that were previously not neighbours but now are

        // If we have run out of free space, increase the size of the arrays
        resizeFreeSpace();

        AStar astar = new AStar(problem);
        StatsRecord stats = new StatsRecord();
        ArrayList<SearchState> path;

        int[] tmp = new int[5000];
        // TODO: May want to pass this as parameter
        SearchAlgorithm searchAlg = new HillClimbing(problem, 10000);

        // Grab location of region and neighbour
        int groupLoc = regionId - GameMap.START_NUM;
        int neighbourLoc = neighbourId - GameMap.START_NUM;

        // Index of last element of array after resizing (= length before resizing)
        int idx = this.neighborId[groupLoc].length;

        // Need to increase size of arrays of region
        increaseArrayLengthBy1AtIndex(this.neighborId, groupLoc, idx);
        increaseArrayLengthBy1AtIndex(this.paths, groupLoc, idx);
        increaseArrayLengthBy1AtIndex(this.lowestCost, groupLoc, idx);

        // Need to compute new paths between regions
        path = astar.computePath(new SearchState(groups.get(regionId).groupRepId), new SearchState(groups.get(neighbourId).groupRepId), stats);
        SearchUtil.computePathCost(path, stats, problem);
        int pathCost = stats.getPathCost();

        // Assign values
        this.neighborId[groupLoc][idx] = neighbourId;
        this.lowestCost[groupLoc][idx] = pathCost;
        this.paths[groupLoc][idx] = SearchUtil.compressPath(SubgoalDB.convertPathToIds(path), searchAlg, tmp, path.size());

        // Index of last element of array after resizing
        idx = this.neighborId[neighbourLoc].length;

        // Need to increase size of arrays of neighbour
        increaseArrayLengthBy1AtIndex(neighborId, neighbourLoc, idx);
        increaseArrayLengthBy1AtIndex(paths, neighbourLoc, idx);
        increaseArrayLengthBy1AtIndex(lowestCost, neighbourLoc, idx);

        // Need to compute new paths between regions
        path = astar.computePath(new SearchState(groups.get(neighbourId).groupRepId), new SearchState(groups.get(regionId).groupRepId), stats);
        SearchUtil.computePathCost(path, stats, problem);
        pathCost = stats.getPathCost();

        // Assign values
        this.neighborId[neighbourLoc][idx] = neighbourId;
        this.lowestCost[neighbourLoc][idx] = pathCost;
        this.paths[neighbourLoc][idx] = SearchUtil.compressPath(SubgoalDB.convertPathToIds(path), searchAlg, tmp, path.size());
    }

    public void recomputeCornerBlocker(int regionId, int neighbourId, MapSearchProblem problem, TreeMap<Integer, GroupRecord> groups) throws Exception {
        // In the blocker case, we have two regions that were previously neighbours but now aren't

        // If we have run out of free space, increase the size of the arrays
        resizeFreeSpace();

        // Grab location of region and neighbour
        int groupLoc = regionId - GameMap.START_NUM;
        int neighbourLoc = neighbourId - GameMap.START_NUM;

        System.out.println(Arrays.toString(this.neighborId[groupLoc]));

        // Update region’s neighbourhood
        int indexOfNeighborLoc = -1;
        for (int i = 0; i < this.neighborId[groupLoc].length; i++) {
            // Need to find index of neighbourLoc
            if (this.neighborId[groupLoc][i] == neighbourLoc) {
                indexOfNeighborLoc = i;
                break;
            }
        }

        // If the region to eliminate was not stored as a neighbour of its neighbour
        if (indexOfNeighborLoc == -1) {
            // If we get here, then the neighbour lists must be messed up, because the region and its neighbour
            // are not neighbours in the neighborId array
            throw new Exception("There is an issue with the neighbours of region: " + regionId);
        }
        
        this.neighborId[groupLoc] = copyArrayExceptIndex(this.neighborId[groupLoc], indexOfNeighborLoc);
        this.lowestCost[groupLoc] = copyArrayExceptIndex(this.lowestCost[groupLoc], indexOfNeighborLoc);
        this.paths[groupLoc][indexOfNeighborLoc] = null;

        int indexOfGroupLoc = -1;
        for (int i = 0; i < this.neighborId[neighbourLoc].length; i++) {
            // Need to find index of groupLoc
            if (this.neighborId[neighbourLoc][i] == groupLoc) {
                indexOfGroupLoc = i;
                break;
            }
        }

        if (indexOfGroupLoc == -1) {
            throw new Exception("There is an issue with the neighbours of region: " + neighbourId);
        }

        this.neighborId[neighbourLoc] = copyArrayExceptIndex(this.neighborId[neighbourLoc], indexOfGroupLoc);
        this.lowestCost[neighbourLoc] = copyArrayExceptIndex(this.lowestCost[neighbourLoc], indexOfGroupLoc);
        this.paths[neighbourLoc][indexOfGroupLoc] = null;
    }

    /**
     * @return regionId that is free to use
     * @throws Exception if the indexing is off
     */
    public int popFreeSpace() throws Exception {
        // Return lowest freeSpace index from the end of the array
        logger.debug("Free space before popping: " + Arrays.toString(freeSpace));
        if (freeSpace[freeSpaceCount - 1] == 0) {
            throw new Exception("Indexing is off");
        }
        this.numGroups++;
        return freeSpace[freeSpaceCount-- - 1] + GameMap.START_NUM;
    }

    /**
     * @param regionId id of a region (indexing starts at 50)
     * @throws Exception if existing free space is being overwritten
     */
    public void pushFreeSpace(int regionId) throws Exception {
        logger.debug("Free space before pushing: " + Arrays.toString(freeSpace));
        if (freeSpace[freeSpaceCount] != 0) {
            throw new Exception("Overwriting existing free space!");
        }
        // Write into freeSpace
        this.numGroups--;
        freeSpace[freeSpaceCount++] = regionId - GameMap.START_NUM;
    }

    /**
     * @param arr   array to copy
     * @param index index to exclude while copying
     * @return array of arr.length - 1 without the element at index
     */
    private static int[] copyArrayExceptIndex(int[] arr, int index) {
        int[] newArr = new int[arr.length - 1];
        int newIndex = 0;
        for (int i = 0; i < arr.length; i++) {
            if (i != index) {
                newArr[newIndex++] = arr[i];
            }
        }
        return newArr;
    }
}
