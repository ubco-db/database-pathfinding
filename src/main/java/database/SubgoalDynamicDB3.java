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

    /**
     * Recomputes the dynamic programming table and base paths.
     * DP table is stored as an adjacency list representation
     *
     * @param regionId      id of the region where the wall was placed
     * @param problem       SearchProblem used for AStar and computing path cost
     * @param groups        TreeMap storing all the groups information
     * @param isElimination boolean indicating whether this is the elimination case
     * @param isPartition   boolean indicating whether this is the partition case
     * @param regionIds     only needed for partition case, contains the ids of all the regions the original region was
     *                      split into after the partition
     * @param neighborIds   only needed for partition case, contains all the neighbors of the new regions
     * @throws Exception if neighborhood is not mutual (issue inside neighborIds array)
     */
    public void recomputeBasePathsAfterWallAddition(int regionId, SearchProblem problem, TreeMap<Integer, GroupRecord> groups,
                                                    boolean isElimination, boolean isPartition, ArrayList<Integer> regionIds,
                                                    ArrayList<Integer> neighborIds) throws Exception {
        // If a wall is added, a region may have been removed (partition/elimination case)
        // Even if a region has not been removed, the wall addition may change paths or lowest costs, so we
        // will need to check for updates there either way

        // If we have run out of free space, increase the size of the arrays
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

        if (isElimination) {
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
        } else if (isPartition) {
            // TODO: Region will be split in two (or more)
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
                        logger.error("There is an issue with the neighbours of region: " + regionId);
                        throw new Exception("There is an issue with the neighbours of region: " + regionId);
                    }
                    // Update lowestCost of neighbour
                    this.lowestCost[neighbourLoc][indexToUpdate] = pathCost;

                    this.paths[groupLoc][i] = SearchUtil.compressPath(SubgoalDB.convertPathToIds(path), searchAlg, tmp, path.size());
                }

                // saveDB("checkingResults3.txt");
            }

        } else {
            // Recompute paths between regions (adding the wall may have changed the lowest cost or removed a path/neighbour)
            // Number of regions should stay consistent (no need to update numGroups, freeSpace, freeSpaceCount)

            // Find array location of region
            int groupLoc = regionId - GameMap.START_NUM;

            AStar astar = new AStar(problem);
            StatsRecord stats = new StatsRecord();
            ArrayList<SearchState> path;

            // neighborsFromGroups should contain the true neighbours of any group since it gets updated in map.recomputeNeighbors
            HashSet<Integer> neighborsFromGroups = groups.get(regionId).getNeighborIds();

            if (neighborsFromGroups.size() != this.neighborId[groupLoc].length) {
                // This is the "Blocker" case, e.g. wall @14288, where the addition of the wall leads to two regions no
                // longer being neighbours without a partition taking place
                logger.warn("NeighborId array length is incorrect!");
            }

            // Iterate over neighbours of the region
            for (int i = 0; i < this.neighborId[groupLoc].length; i++) {
                // Grab location of neighbour
                int neighbourLoc = this.neighborId[groupLoc][i];

                // Check whether groups has the same neighbours as neighborId (groups is most up-to-date)
                if (!neighborsFromGroups.contains(neighbourLoc + GameMap.START_NUM)) {
                    logger.info("Removing region: " + (neighbourLoc + GameMap.START_NUM) + " from neighbour array of " + regionId);

                    // Copying into smaller arrays here
                    this.neighborId[groupLoc] = copyArrayExceptIndex(this.neighborId[groupLoc], i);
                    this.lowestCost[groupLoc] = copyArrayExceptIndex(this.lowestCost[groupLoc], i);
                    // Simply setting path to null (not copying paths array)
                    this.paths[groupLoc][i] = null;

                    // Iterate over neighbours of neighbour to find region to eliminate
                    int indexOfRegionToEliminate = -1;
                    for (int j = 0; j < this.neighborId[neighbourLoc].length; j++) {
                        if (this.neighborId[neighbourLoc][j] == (regionId - GameMap.START_NUM)) {
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
                } else {
                    // If nothing changed about the neighbours

                    // TODO: Update lowestCost and paths where necessary
                    // This will find a path regardless of whether they are neighbours anymore (which is why we are doing all of the above)
                    path = astar.computePath(new SearchState(groups.get(groupLoc + GameMap.START_NUM).groupRepId), new SearchState(groups.get(neighbourLoc + GameMap.START_NUM).groupRepId), stats);
                    SearchUtil.computePathCost(path, stats, problem);
                    int pathCost = stats.getPathCost();

                    // Check where they are not identical, then go to that neighbour and update there too
                    if (lowestCost[groupLoc][i] != pathCost) {
                        logger.debug("Lowest cost of path between " + regionId + " and " + (neighbourLoc + GameMap.START_NUM) + " has changed from " + lowestCost[groupLoc][i] + " to " + pathCost);
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
                            logger.error("There is an issue with the neighbours of region: " + regionId);
                            throw new Exception("There is an issue with the neighbours of region: " + regionId);
                        }
                        // Update lowestCost of neighbour
                        this.lowestCost[neighbourLoc][indexToUpdate] = pathCost;
                    }

                    // TODO: Is there some check I can do or do I just update all paths?

                    // TODO: Do I need this / Is this all that's needed?
                    // this.paths[groupLoc][i] = SearchUtil.compressPath(SubgoalDB.convertPathToIds(path), searchAlg, tmp, path.size());

                }
            }

            // saveDB("checkingResults2.txt");
        }

        // this.numGroups = groups.size();
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
     * Recomputes the dynamic programming table and base paths.
     * DP table is stored as an adjacency list representation
     *
     * @param regionId           id of the region where the wall was placed
     * @param problem            SearchProblem used for AStar and computing path cost
     * @param groups             TreeMap storing all the groups information
     * @param isSolitary         boolean indicating whether the region has any neighbours
     * @param isReversePartition boolean indicating whether this is the reverse partition case
     */
    public void recomputeBasePathsAfterWallRemoval(int regionId, SearchProblem problem, TreeMap<Integer, GroupRecord> groups,
                                                   boolean isSolitary, boolean isReversePartition) {
        // If a wall is removed, a region may have been added
        // Even if a region has not been added, the wall removal may change paths or lowest costs, so we
        // will need to check for updates there either way

        // If we have run out of free space, increase the size of the arrays
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

        if (isSolitary) {
            // Case where new region has no neighbours (e.g. is surrounded by walls)

            // freeSpace has already been updated in DBAStarUtil (needed the information for map updates)

            // Find array location of region
            int groupLoc = regionId - GameMap.START_NUM;

            // Create arrays for new group
            this.neighborId[groupLoc] = new int[0];
            this.lowestCost[groupLoc] = new int[0];
            this.paths[groupLoc] = new int[0][];
        } else if (isReversePartition) {
            // TODO: Case where two regions merge into one

            // Will need to recompute neighborIds, lowestCosts, and paths
        } else {
            // Case where new region has neighbours

            // freeSpace has already been updated in DBAStarUtil (needed the information for map updates)

            // Find array location of region
            int groupLoc = regionId - GameMap.START_NUM;

            // Get neighbours of the new region (updated in map.recomputeNeighbors)
            HashSet<Integer> neighbours = groups.get(regionId).getNeighborIds();
            // Create an int array with the same size as the HashSet
            int[] neighbourArray = new int[neighbours.size()];

            // Iterate through the HashSet and copy its elements to the array
            int index = 0;
            for (Integer neighbour : neighbours) {
                neighbourArray[index++] = neighbour - GameMap.START_NUM;
            }

            // Create arrays for new group
            this.neighborId[groupLoc] = neighbourArray;
            this.lowestCost[groupLoc] = new int[neighbours.size()];
            this.paths[groupLoc] = new int[neighbours.size()][];

            AStar astar = new AStar(problem);
            StatsRecord stats = new StatsRecord();
            ArrayList<SearchState> path;
            int pathCost;

            // Need to ensure we compute paths to connect new region to existing ones
            for (int i = 0; i < this.neighborId[groupLoc].length; i++) {
                // Grab location of neighbour
                int neighbourLoc = this.neighborId[groupLoc][i];
                int[] tmp = new int[5000];
                // TODO: May want to pass this as parameter
                SearchAlgorithm searchAlg = new HillClimbing(problem, 10000);

                // Compute path from groupLoc to neighbourLoc
                path = astar.computePath(new SearchState(groups.get(regionId).groupRepId), new SearchState(groups.get(neighbourLoc + GameMap.START_NUM).groupRepId), stats);
                SearchUtil.computePathCost(path, stats, problem);
                pathCost = stats.getPathCost();

                // Store path and lowest cost
                this.lowestCost[groupLoc][i] = pathCost;
                this.paths[groupLoc][i] = SearchUtil.compressPath(SubgoalDB.convertPathToIds(path), searchAlg, tmp, path.size());

                // Increase size of arrays and store path from neighbourLoc to groupLoc, and cost. Also add new region as neighbour of its neighbours

                // Compute path from neighbourLoc to groupLoc
                // TODO: Should I use new instances of tmp and stats here?
                path = astar.computePath(new SearchState(groups.get(neighbourLoc + GameMap.START_NUM).groupRepId), new SearchState(groups.get(regionId).groupRepId), stats);
                SearchUtil.computePathCost(path, stats, problem);
                pathCost = stats.getPathCost();

                // This should be the same for neighborId, lowestCost, and paths
                int originalArraySize = this.neighborId[neighbourLoc].length;

                // Increase size of neighbourIds array by 1 for current neighbourLoc
                int[] largerNeighbourId = new int[originalArraySize + 1];
                System.arraycopy(this.neighborId[neighbourLoc], 0, largerNeighbourId, 0, originalArraySize);
                this.neighborId[neighbourLoc] = largerNeighbourId;

                // Add new region as neighbour to its neighbours
                this.neighborId[neighbourLoc][originalArraySize] = groupLoc;

                // Increase size of lowestCost array by 1 for current neighbourLoc
                int[] largerLowestCost = new int[originalArraySize + 1];
                System.arraycopy(this.lowestCost[neighbourLoc], 0, largerLowestCost, 0, originalArraySize);
                this.lowestCost[neighbourLoc] = largerLowestCost;

                // Add lowestCost of path
                this.lowestCost[neighbourLoc][originalArraySize] = pathCost; // TODO: Is pathCost always the same in both directions?

                // Increase size of paths array by 1 for current neighbourLoc
                int[][] largerPaths = new int[originalArraySize + 1][];
                System.arraycopy(this.paths[neighbourLoc], 0, largerPaths, 0, originalArraySize);
                this.paths[neighbourLoc] = largerPaths;

                // Add path, TODO: Could I just reverse the other path?
                this.paths[neighbourLoc][originalArraySize] = SearchUtil.compressPath(SubgoalDB.convertPathToIds(path), searchAlg, tmp, path.size());
            }

            saveDB("RemovalOf2284.txt");
        }
    }

    /**
     * @return regionId that is free to use
     * @throws Exception if the indexing is off
     */
    public int popFreeSpace() throws Exception {
        // Return lowest freeSpace index from the end of the array
        System.out.println(Arrays.toString(freeSpace));
        if (freeSpace[freeSpaceCount - 1] == 0) {
            throw new Exception("Indexing is off");
        }
        this.numGroups++;
        return freeSpace[freeSpaceCount-- - 1];
    }

    /**
     * @param regionId id of a region (indexing starts at 50)
     * @throws Exception if existing free space is being overwritten
     */
    public void pushFreeSpace(int regionId) throws Exception {
        System.out.println(Arrays.toString(freeSpace));
        if (freeSpace[freeSpaceCount] != 0) {
            throw new Exception("Overwriting existing free space!");
        }
        // Write into freeSpace
        this.numGroups--;
        freeSpace[freeSpaceCount++] = regionId;
    }

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
