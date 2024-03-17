package database;

import map.GameMap;
import map.GroupRecord;
import search.AStar;
import search.SearchAbstractAlgorithm;
import search.SearchAlgorithm;
import search.SearchProblem;
import search.SearchState;
import search.SearchUtil;
import search.StatsRecord;

import java.util.*;

public class GameDB {
    private final SearchProblem problem;
    private GroupRecord[] groups;
    private int numGroups;

    public GameDB(SearchProblem problem) {
        this.problem = problem;
    }

    public SubgoalDB computeRandomDB(int num, SearchAbstractAlgorithm searchAlg, DBStatsRecord dbstat, SubgoalDB sgdb) {
        System.out.println("Creating random database with " + num + " entries.");
        long startTime = System.currentTimeMillis();

        AStar astar = new AStar(problem);

        int maxDistance = 100000;
        int minDistance = 10;
        Random generator = new Random();
        int dist;
        SearchState start, goal;

        // Generate a random start/end.  Distance away must be in between cutoffs.
        // Also, number of subgoals must be at least 1.
        long currentTime;
        long totalATime = 0, totalSGTime = 0;
        int numSubgoals = 0;
        StatsRecord stats = new StatsRecord();
        ArrayList<SearchState> subgoals = new ArrayList<>(5000);
        for (int i = 0; i < num; i++) {
            do {
                start = problem.generateRandomState(generator);
                goal = problem.generateRandomState(generator);

                dist = problem.computeDistance(start, goal);
            } while (dist < minDistance || dist > maxDistance);

            // Compute optimal path for set of points
            currentTime = System.currentTimeMillis();
            ArrayList<SearchState> path = astar.computePath(start, goal, stats);
            // System.out.println("Time to compute A*:"+(System.currentTimeMillis()-currentTime));
            totalATime += System.currentTimeMillis() - currentTime;

            currentTime = System.currentTimeMillis();

            if (path != null) SearchUtil.computeSubgoalsBinary(path, searchAlg, subgoals);
            else {
                i--;
                // System.out.println("Start and goal not connected.");
                continue;
            }

            if (subgoals.size() == 2)// Only start and end, no subgoals
            {
                i--;
                // System.out.println("No subgoals between start and goal.");
                continue;
            }

            int[] subgoal = new int[subgoals.size() - 2];

            for (int k = 1; k < subgoals.size() - 1; k++) {
                SearchState s = subgoals.get(k);
                subgoal[k - 1] = s.id;
            }
            numSubgoals += subgoal.length + 1;

            // System.out.println("Time to compute subgoals: "+(System.currentTimeMillis()-currentTime));

            SubgoalDBRecord rec = new SubgoalDBRecord(start.id, goal.id, subgoal, 0);
            if (!sgdb.addRecord(rec)) {    // Duplicate entry
                i--;
                // System.out.println("Duplicate entry");
                continue;
            }

//			if (sgdb instanceof SubgoalDBExact)
//				((SubgoalDBExact) sgdb).computeCoverage();

            totalSGTime += System.currentTimeMillis() - currentTime;
            //if (i % 1 == 0)
            //	System.out.println("#random: "+numRandom+" #WGS: "+numWithoutSGs+" #dup: "+numDup+" Fail: "+numFail+" Added record "+i+" Record: "+rec.toString(problem));
        }
        System.out.println("Generated random database with " + num + " entries in time: " + (System.currentTimeMillis() - startTime) / 1000);
        System.out.println("Time performing A*: " + totalATime + " Time performing subgoal calculation: " + totalSGTime);
        System.out.println("Number of subgoals: " + numSubgoals);
        dbstat.addStat(8, num);
        dbstat.addStat(9, numSubgoals);
        long overallTime = (System.currentTimeMillis() - startTime);
        dbstat.addStat(10, overallTime);
        dbstat.addStat(13, totalATime);
        dbstat.addStat(14, totalSGTime);
        return sgdb;
    }

    public SubgoalDB computeDBDP2(SubgoalDB db, SearchAlgorithm astarj, DBStatsRecord dbstats, int numLevels) throws Exception {
        groups = problem.getGroups();

        long current = System.currentTimeMillis();
        problem.computeNeighbors();
        long neighborTime = System.currentTimeMillis() - current;
        dbstats.addStat(18, neighborTime);

        // Generate subgoal databases using the groups
        computeHCDBDP(db, astarj, dbstats, numLevels);

        return db;
    }

    public static HashSet<Integer> getNeighbors(GroupRecord[] groups, GroupRecord startGroup, int numLevels, boolean isPartition) {
        HashSet<Integer> neighbors = startGroup.getComputedNeighborIds();

        if (neighbors == null || isPartition) {
            // This supports Level 1 (immediate neighbors)
            neighbors = new HashSet<Integer>(startGroup.getNeighborIds().size());
            neighbors.addAll(startGroup.getNeighborIds());

            // The loop is for level 2 and above
            HashSet<Integer> neighbors2 = new HashSet<>();
            BitSet done = new BitSet(groups.size());
            GroupRecord neighborGroup;

            for (int i = 1; i < numLevels; i++) {
                for (int neighborId : neighbors) {
                    if (done.get(neighborId)) continue;    // Already processed this neighbors set

                    done.set(neighborId);
                    neighborGroup = groups[neighborId - GameMap.START_NUM];
                    // Do not itself if already there
                    for (int val : neighborGroup.getNeighborIds()) {
                        if (val != startGroup.groupId) neighbors2.add(val);
                    }

                    // neighbors2.addAll(neighborGroup.getNeighborIds());	// TODO: May remove. This code would add a neighbor of itself.
                }
                neighbors.addAll(neighbors2);
                neighbors2.clear();
            }

            startGroup.setComputedNeighborIds(neighbors);
        }

        return neighbors;
    }

    public static long computeBasePaths(SearchProblem problem, GroupRecord[] groups, SearchAlgorithm searchAlg, int[][] lowestCost, int[][][] paths, int[][] neighbor, int numGroups, int numLevels, boolean asSubgoals, DBStatsRecord dbstats) {
        int goalGroupLoc, startGroupLoc;
        GroupRecord startGroup, goalGroup;
        HashSet<Integer> neighbors;
        AStar astar = new AStar(problem);
        ArrayList<SearchState> path;
        StatsRecord stats = new StatsRecord();
        int numBase = 0;

        System.out.println("Number of groups: " + numGroups);
        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < numGroups; i++)
            Arrays.fill(neighbor[i], 0, numGroups, -1);

        int[] tmp = new int[5000];
        // Now make database entries for each of the lowest cost by merging the paths
        System.out.println("Creating base paths to neighbors.");
        // Base case: Generate paths to all neighbors
        int numStates = 0;
        for (int i = 0; i < numGroups; i++) {
            startGroup = groups[i];

            neighbors = GameDB.getNeighbors(groups, startGroup, numLevels, false);
            // System.out.println("Doing group: "+i+" Neighbors: "+neighbors.size());
            // Generate for each neighbor group
            for (int goalGroupId : neighbors) {
                // Compute the shortest path between center representative of both groups
                goalGroup = groups[goalGroupId - GameMap.START_NUM];

                path = astar.computePath(new SearchState(startGroup.groupRepId), new SearchState(goalGroup.groupRepId), stats);
                numBase++;

                goalGroupLoc = goalGroupId - GameMap.START_NUM;
                startGroupLoc = i;

                // Save information
                SearchUtil.computePathCost(path, stats, problem);
                int pathCost = stats.getPathCost();
                lowestCost[startGroupLoc][goalGroupLoc] = pathCost;
                neighbor[startGroupLoc][goalGroupLoc] = goalGroupLoc;
                if (asSubgoals) {
                    paths[startGroupLoc][goalGroupLoc] = SubgoalDB.convertPathToIds(path);
                    paths[startGroupLoc][goalGroupLoc] = SearchUtil.compressPath(paths[startGroupLoc][goalGroupLoc], searchAlg, tmp, path.size());
                    numStates += paths[startGroupLoc][goalGroupLoc].length;
                } else {
                    paths[startGroupLoc][goalGroupLoc] = SubgoalDB.convertPathToIds(path);
                    numStates += path.size();
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long baseTime = endTime - currentTime;
        System.out.println("Time to compute base paths: " + (baseTime));
        System.out.println("Base neighbors generated paths: " + numBase + " Number of states: " + numStates);
        dbstats.addStat(9, numStates);      // Set number of subgoals.  Will be changed by a version that pre-computes all paths but will not be changed for the dynamic version.
        dbstats.addStat(8, numBase);        // # of records (only corresponds to base paths)
        return baseTime;
    }

    public SubgoalDynamicDB3 computeDynamicDBUsingSubgoalDynamicDB3(SubgoalDynamicDB3 db, SearchAbstractAlgorithm searchAlg, DBStatsRecord dbstats, int numLevels) throws Exception {
        groups = problem.getGroups();
        numGroups = problem.getNumGroups();

        long current = System.currentTimeMillis();
        problem.computeNeighbors();
        long neighborTime = System.currentTimeMillis() - current;
        dbstats.addStat(18, neighborTime);

        // Generate subgoal databases using the groups
        db.compute(problem, groups, numGroups, searchAlg, dbstats, numLevels);

        return db;
    }

    /**
     * Creates a databases combining all pairs of groups in map using dynamic programming instead of generating all combinations.
     *
     * @return SubgoalDB
     */
    public SubgoalDB computeHCDBDP(SubgoalDB db, SearchAlgorithm searchAlg, DBStatsRecord dbStats, int numLevels) {
        GroupRecord startGroup, goalGroup;
        int[] path;
        int SIZE_CUTOFF = 1;
        int count = 0;
        int numSubgoals = 0;
        // FIXME
        int numGroups = this.numGroups;
        int[][] lowestCost = new int[numGroups][numGroups];
        int[][][] paths = new int[numGroups][numGroups][];
        int[][] neighbor = new int[numGroups][numGroups];
        long startTime = System.currentTimeMillis();

        long baseTime = computeBasePaths(problem, groups, searchAlg, lowestCost, paths, neighbor, numGroups, numLevels, true, dbStats);

        long endTime, currentTime = System.currentTimeMillis();

        System.out.println("Performing dynamic programming to generate paths.");

        // Compute neighbors once and store in arrays for fast access
        int[][] tmpNeighbors = new int[numGroups][];
        for (int i = 0; i < numGroups; i++) {
            count = 0;
            for (int j = 0; j < numGroups; j++)
                if (neighbor[i][j] >= 0) count++;
            tmpNeighbors[i] = new int[count];
            count = 0;
            for (int j = 0; j < numGroups; j++)
                if (neighbor[i][j] >= 0) tmpNeighbors[i][count++] = neighbor[i][j];
        }

        boolean changed = true;
        int numUpdates = 0;
        while (changed) {
            changed = false;

            for (int i = 0; i < numGroups; i++) {
                // Process all neighbors of this node
                for (int k = 0; k < tmpNeighbors[i].length; k++) {
                    // int neighborId = tmpNeighbors[i][k] - GameMap.START_NUM;
                    int neighborId = tmpNeighbors[i][k];
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
        currentTime = System.currentTimeMillis();

        int[] subgoals, tmp = new int[5000];
        // Now make database entries for each of the lowest cost by merging the paths
        int i, j;
        int totalCost = 0, pathSize;
        path = new int[5000];
        for (i = 0; i < numGroups; i++) {
            startGroup = groups[i];
            if (startGroup.getNumStates() < SIZE_CUTOFF) continue;

            for (j = 0; j < numGroups; j++) {
                if (i == j) continue;

                goalGroup = groups[j];
                if (goalGroup.getNumStates() < SIZE_CUTOFF) continue;

                // This code builds only the path required on demand (may incur more time as have to continually merge paths but may save time by avoiding storing/copying lists to do construction)
                pathSize = mergePaths3(i, j, paths, neighbor, path, 0);

                if (pathSize == 0) continue;        // No path between two states

                // Does not include start and goal
                subgoals = SearchUtil.computeSubgoalsBinaryByIds(path, searchAlg, tmp, pathSize);
                if (subgoals == null) numSubgoals++;
                else numSubgoals += subgoals.length + 1;

                SubgoalDBRecord rec = new SubgoalDBRecord(startGroup.groupRepId, goalGroup.groupRepId, subgoals, (startGroup.groupId - GameMap.START_NUM + 1) * 10000 + goalGroup.groupId - GameMap.START_NUM);    // TODO: This will probably need to be changed.

                if (!db.addRecord(rec))
                    System.out.println("Failed to add record connecting HC states: " + i + " and " + j);

                count++;
                if (count % 500 == 0)
                    System.out.println("Added record " + count + " between: " + (i + GameMap.START_NUM) + " and " + (j + GameMap.START_NUM) + " Record: " + rec.toString(problem));
                totalCost += lowestCost[i][j];
            }
        }

        System.out.println("Total database records: " + count);
        System.out.println("# of subgoals (including end but not start): " + numSubgoals);
        endTime = System.currentTimeMillis();
        long overallTime = endTime - startTime;
        long recordTime = endTime - currentTime;
        System.out.println("Time to compute records: " + recordTime);
        System.out.println("Total DB compute time: " + overallTime);
        dbStats.addStat(8, count);
        dbStats.addStat(9, numSubgoals);
        long neighborTime = (Long) dbStats.getStat(18);
        dbStats.addStat(10, overallTime + neighborTime);
        dbStats.addStat(15, dpTime);
        dbStats.addStat(16, baseTime);
        dbStats.addStat(17, recordTime);

        // Verify by computing all costs
        System.out.println("Total costs: " + totalCost);
        return db;
    }

    public static int mergePaths3(int i, int j, int[][][] paths, int[][] neighbor, int[] path, int lastOffset) {
        int neighborId = neighbor[i][j];                    // Lookup neighbor of i to go to that has shortest path to j
        if (neighborId == -1)                                // If no neighbor, i and j must not be connected.
            return 0;

        int start = 0;
        if (lastOffset > 0) start = 1;

        // Copy (but do not include duplicate start node - start from 1 instead of 0).
        if (paths[i][neighborId].length - start >= 0) {
            System.arraycopy(paths[i][neighborId], start, path, lastOffset + start, paths[i][neighborId].length - start);
        }

        if (neighborId == j)                                // If neighbor is j itself, then this is the end of the path.
        {
            return lastOffset + paths[i][j].length;        // Current size of path
        }

        // Otherwise this is only the next step on the path.  Call the method recursively to continue to build the path.
        lastOffset += paths[i][neighborId].length - 1;

        return mergePaths3(neighborId, j, paths, neighbor, path, lastOffset);
    }

    private static int findInArray(int[] ar, int key) {
        for (int i = 0; i < ar.length; i++) {
            if (ar[i] == key) {
                return i;
            }
        }
        return -1;
    }

    // This version support matrix as an adjacency list and dynamically calculates the best path
    public static int mergePaths4(int startGroupId, int goalGroupId, int[][][] paths, int[][] neighbor, int[][] lowestCost, int[][] neighborId, int[] path) {
        // Find if this is a neighbor
        int neighborLoc = findInArray(neighborId[startGroupId], goalGroupId);
        if (neighborLoc != -1) { // Direct neighbor with path stored - just return the path
            System.arraycopy(paths[startGroupId][neighborLoc], 0, path, 0, paths[startGroupId][neighborLoc].length);
            return paths[startGroupId][neighborLoc].length;
        }

        // Otherwise we need to search to find it
        // Quick implementation using Dijkstra's algorithm but could use A* as well
        int numGroups = neighbor.length;
        int[] distance = new int[numGroups];
        int[] previous = new int[numGroups];
        boolean[] visited = new boolean[numGroups];
        int[] nodes = new int[numGroups];
        int count = 0;
        Arrays.fill(distance, Integer.MAX_VALUE);
        // Initialize distance and previous with neighbors
        for (int i = 0; i < neighborId[startGroupId].length; i++) {
            neighborLoc = neighborId[startGroupId][i];
            previous[neighborLoc] = startGroupId;
            distance[neighborLoc] = lowestCost[startGroupId][i];
            nodes[count++] = neighborLoc;
        }

        // Using basic unsorted array.  Not great for performance.
        while (count > 0) {    // Find lowest distance (linear search)
            int minCost = Integer.MAX_VALUE, minLoc = -1;
            for (int k = 0; k < count; k++) {
                int neighborid = nodes[k];
                int cost = distance[neighborid];
                if (cost < minCost) {
                    minCost = cost;
                    minLoc = k;
                }
            }

            if (minLoc == -1) return -1;            // Unreachable

            int neighborid = nodes[minLoc];

            if (neighborid == goalGroupId)          // Goal node found - stop algorithm so do not expand to all nodes
                break;

            // Remove this node from the queue
            nodes[minLoc] = nodes[--count];
            visited[neighborid] = true;

            // Process all neighbors of this node
            for (int i = 0; i < neighborId[neighborid].length; i++) {
                neighborLoc = neighborId[neighborid][i];
                if (!visited[neighborLoc]) {
                    int dist = lowestCost[neighborid][i] + distance[neighborid];
                    if (dist < distance[neighborLoc]) {
                        distance[neighborLoc] = dist;
                        previous[neighborLoc] = neighborid;
                    }
                    // TODO: Inefficient. Only add node if not currently on list. Do so by searching for it.
                    boolean found = false;
                    for (int k = 0; k < count; k++)
                        if (nodes[k] == neighborLoc) {
                            found = true;
                            break;
                        }

                    if (!found) nodes[count++] = neighborLoc;
                }
            }
        }

        // Print path
        count = 0;
        int currentId = goalGroupId;
        while (currentId != startGroupId) { // backtrack from goalGroupId to startGroupId
            distance[count++] = currentId;
            currentId = previous[currentId];
        }
        distance[count++] = startGroupId; // reverse path is in distance array

        // Now produce the actual path
        int pathLen = 0;

        // Add first segment of the path
        // nextId is group id.  nextLoc is location of that id is adjacency list (array)
        int nextId = distance[count - 2], lastId = startGroupId;
        int nextLoc = findInArray(neighborId[lastId], nextId);
        // Copy (but do not include duplicate start node - start from 1 instead of 0).
        System.arraycopy(paths[lastId][nextLoc], 0, path, 0, paths[lastId][nextLoc].length);
        pathLen += paths[lastId][nextLoc].length;
        lastId = nextId;
        for (int i = count - 3; i >= 0; i--) {
            nextId = distance[i];
            nextLoc = findInArray(neighborId[lastId], nextId);
            // Copy (but do not include duplicate start node - start from 1 instead of 0).
            if (nextLoc < 0) { // if nextLoc < 0, path cannot be found
                return 0;
            }
            if (paths[lastId][nextLoc].length >= 1)
                System.arraycopy(paths[lastId][nextLoc], 1, path, pathLen - 1 + 1, paths[lastId][nextLoc].length - 1);
            pathLen += paths[lastId][nextLoc].length - 1;
            lastId = nextId;
        }
        return pathLen;
    }

    public static int mergePaths4(int startGroupId, int goalGroupId, int[][][] paths, int[][] lowestCost, int[][] neighborId, int[] path) {
        // Find if this is a neighbor
        int neighborLoc = findInArray(neighborId[startGroupId], goalGroupId);
        if (neighborLoc != -1) { // Direct neighbor with path stored - just return the path
            System.arraycopy(paths[startGroupId][neighborLoc], 0, path, 0, paths[startGroupId][neighborLoc].length);
            return paths[startGroupId][neighborLoc].length;
        }

        // Otherwise we need to search to find it
        // Quick implementation using Dijkstra's algorithm but could use A* as well
        int numGroups = neighborId.length;
        int[] distance = new int[numGroups];
        int[] previous = new int[numGroups];
        boolean[] visited = new boolean[numGroups];
        int[] nodes = new int[numGroups];
        int count = 0;
        Arrays.fill(distance, Integer.MAX_VALUE);
        // Initialize distance and previous with neighbors
        for (int i = 0; i < neighborId[startGroupId].length; i++) {
            neighborLoc = neighborId[startGroupId][i];
            previous[neighborLoc] = startGroupId;
            distance[neighborLoc] = lowestCost[startGroupId][i];
            nodes[count++] = neighborLoc;
        }

        // Using basic unsorted array.  Not great for performance.
        while (count > 0) {    // Find lowest distance (linear search)
            int minCost = Integer.MAX_VALUE, minLoc = -1;
            for (int k = 0; k < count; k++) {
                int neighborid = nodes[k];
                int cost = distance[neighborid];
                if (cost < minCost) {
                    minCost = cost;
                    minLoc = k;
                }
            }

            if (minLoc == -1) return -1;            // Unreachable

            int neighborid = nodes[minLoc];

            if (neighborid == goalGroupId)          // Goal node found - stop algorithm so do not expand to all nodes
                break;

            // Remove this node from the queue
            nodes[minLoc] = nodes[--count];
            visited[neighborid] = true;

            // Process all neighbors of this node
            for (int i = 0; i < neighborId[neighborid].length; i++) {
                neighborLoc = neighborId[neighborid][i];
                if (!visited[neighborLoc]) {
                    int dist = lowestCost[neighborid][i] + distance[neighborid];
                    if (dist < distance[neighborLoc]) {
                        distance[neighborLoc] = dist;
                        previous[neighborLoc] = neighborid;
                    }
                    // TODO: Inefficient. Only add node if not currently on list. Do so by searching for it.
                    boolean found = false;
                    for (int k = 0; k < count; k++)
                        if (nodes[k] == neighborLoc) {
                            found = true;
                            break;
                        }

                    if (!found) nodes[count++] = neighborLoc;
                }
            }
        }

        // Print path
        count = 0;
        int currentId = goalGroupId;
        while (currentId != startGroupId) { // backtrack from goalGroupId to startGroupId
            distance[count++] = currentId;
            currentId = previous[currentId];
        }
        distance[count++] = startGroupId; // reverse path is in distance array

        // Now produce the actual path
        int pathLen = 0;

        // Add first segment of the path
        // nextId is group id.  nextLoc is location of that id is adjacency list (array)
        int nextId = distance[count - 2], lastId = startGroupId;
        int nextLoc = findInArray(neighborId[lastId], nextId);
        // Copy (but do not include duplicate start node - start from 1 instead of 0).
        System.arraycopy(paths[lastId][nextLoc], 0, path, 0, paths[lastId][nextLoc].length);
        pathLen += paths[lastId][nextLoc].length;
        lastId = nextId;
        for (int i = count - 3; i >= 0; i--) {
            nextId = distance[i];
            nextLoc = findInArray(neighborId[lastId], nextId);
            // Copy (but do not include duplicate start node - start from 1 instead of 0).
            if (nextLoc < 0) { // if nextLoc < 0, path cannot be found
                return 0;
            }
            if (paths[lastId][nextLoc].length >= 1)
                System.arraycopy(paths[lastId][nextLoc], 1, path, pathLen - 1 + 1, paths[lastId][nextLoc].length - 1);
            pathLen += paths[lastId][nextLoc].length - 1;
            lastId = nextId;
        }
        return pathLen;
    }

    /**
     * This version handles the matrix as a compressed RLE array.
     *
     * @param startGroupId
     * @param goalGroupId
     * @param paths
     * @param neighbors
     * @param path
     * @param lastOffset
     * @param neighborIds
     * @param numGroups
     * @return
     */
    public static int mergePaths5(int startGroupId, int goalGroupId, int[][][] paths, IndexDB neighbors, int[] path, int lastOffset, int[][] neighborIds, int numGroups) {
        int neighborId = neighbors.find(startGroupId * numGroups + goalGroupId);    // Lookup neighbor of i to go to that has shortest path to j
        if (neighborId == -1)                                                    // If no neighbor, i and j must not be connected.
            return 0;

        // Find path to this neighbor
        int neighborLoc = findInArray(neighborIds[startGroupId], neighborId);
		/*if (neighborLoc == -1)													// If no neighbor, i and j must not be connected.
			return 0;*/

        // Copy path to our answer path
        int start = 0;
        if (lastOffset > 0) start = 1;

        // Copy (but do not include duplicate start node - start from 1 instead of 0).
        if (paths[startGroupId][neighborLoc].length - start >= 0)
            System.arraycopy(paths[startGroupId][neighborLoc], start, path, lastOffset + start, paths[startGroupId][neighborLoc].length - start);

        if (neighborId == goalGroupId)                                            // If neighbor is goal, then this is the end of the path.
            return lastOffset + paths[startGroupId][neighborLoc].length;            // Current size of path

        // Otherwise this is only the next step on the path.  Call the method recursively to continue to build the path.
        lastOffset += paths[startGroupId][neighborLoc].length - 1;

        return mergePaths5(neighborId, goalGroupId, paths, neighbors, path, lastOffset, neighborIds, numGroups);
    }

}