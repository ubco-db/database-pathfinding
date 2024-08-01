package database;

import map.Region;
import search.SearchState;
import search.algorithms.CompressAStar;
import search.algorithms.HillClimbing;
import search.algorithms.HillClimbingWithClosedSet;
import stats.SearchStats;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

import static map.AbstractedMap.START_NUM;
import static search.SearchUtil.computeSubgoalsBinaryByIds;
import static search.SearchUtil.findInArray;
import static search.SearchUtil.findOptimallyCompressedPath;
import static search.SearchUtil.findPathCost;

/**
 * Database where dynamic programming table is not computed offline only the base paths between adjacency neighbors are (and their associated costs).
 * Online, a record is produced by searching the partial complete DP table for the lowest cost path between regions i and j.
 * This path consists of a series of hops between neighbors and each hop's path is combined into a path to solve the entire problem.
 * In effect, this is performing another search on the abstract region space.  The algorithm is currently using Dijkstra's but A* may be possible as well.
 * This search is no longer real-time (as number of regions cannot be bounded a priori), so any search using this database cannot also be considered real-time.
 * The savings are that no DP computation needs to be performed which speeds up things when there are a large number of regions and potentially can be useful when
 * the state space is changing.
 * DP table only stores direct neighbors (not entire matrix).
 *
 * @author rlawrenc
 */
public class SubgoalDB {
    private int numGroups;
    private int[][] neighbours;
    private int[][] lowestCost;
    private int[][][] pathSubgoals;

    public SubgoalDB(Map<Integer, Region> regionHashMap, SearchStats searchStats, CompressAStar compressAStar, HillClimbingWithClosedSet hc) {
        computeBasePaths(regionHashMap, true, searchStats, compressAStar, hc);
        // saveDB("databases/subgoals.txt");
    }

    /**
     * Returns record for start and goal for search problem between two regions.
     * Record produced dynamically from data in DP table by combining base paths between regions (non-real-time).
     */
    public SubgoalDBRecord getRecord(int startGroupId, int goalGroupId, boolean compressed, HillClimbing hillClimbing, SearchStats searchStats) {
        if (startGroupId == goalGroupId) {
            throw new IllegalArgumentException("startGroupId and goalGroupId must not be the same");
        }

        int startGroupLoc = startGroupId - START_NUM;
        int goalGroupLoc = goalGroupId - START_NUM;

        int[] path = new int[2000], tmp = new int[2000];
        int pathSize = mergePaths(startGroupLoc, goalGroupLoc, path);

        if (pathSize == 0) {
            return null;
        }

        int startRegionRep = path[0];
        int goalRegionRep = path[pathSize - 1];

        if (compressed) {
            // Compress path using hill-climbing
            path = computeSubgoalsBinaryByIds(path, hillClimbing, tmp, pathSize, searchStats);
        } else {
            path = Arrays.copyOfRange(path, 1, pathSize - 1);
        }

        return new SubgoalDBRecord(startRegionRep, goalRegionRep, path);
    }

    // TODO: Implement using A* instead?
    private int mergePaths(int startGroupLoc, int goalGroupLoc, int[] path) {
        // Find if this is a neighbor
        int neighborLoc = findInArray(neighbours[startGroupLoc], goalGroupLoc);
        if (neighborLoc != -1) { // Direct neighbor with path stored - just return the path
            System.arraycopy(pathSubgoals[startGroupLoc][neighborLoc], 0, path, 0, pathSubgoals[startGroupLoc][neighborLoc].length);
            return pathSubgoals[startGroupLoc][neighborLoc].length;
        }

        // Implementation using Dijkstra's algorithm
        int numGroups = neighbours.length;
        int[] distance = new int[numGroups];
        int[] previous = new int[numGroups];
        boolean[] visited = new boolean[numGroups];
        int[] nodes = new int[numGroups];
        int count = 0;

        Arrays.fill(distance, Integer.MAX_VALUE);
        for (int i = 0; i < neighbours[startGroupLoc].length; i++) {
            int neighbourLoc = neighbours[startGroupLoc][i];
            previous[neighbourLoc] = startGroupLoc;
            distance[neighbourLoc] = lowestCost[startGroupLoc][i];
            nodes[count++] = neighbourLoc;
        }

        while (count > 0) {
            // Find the lowest distance (using linear search)
            int minCost = Integer.MAX_VALUE;
            int minLoc = -1;

            for (int i = 0; i < count; i++) {
                int neighbourLoc = nodes[i];
                int cost = distance[neighbourLoc];
                if (cost < minCost) {
                    minCost = cost;
                    minLoc = i;
                }
            }

            if (minLoc == -1) {
                return -1;
            }

            int neighbourLoc = nodes[minLoc];

            // Goal node found
            if (neighbourLoc == goalGroupLoc) {
                break;
            }

            // Remove node from the queue
            nodes[minLoc] = nodes[--count];
            visited[neighbourLoc] = true;

            // Process all neighbours of the node
            for (int i = 0; i < neighbours[neighbourLoc].length; i++) {
                int nextNeighbourLoc = neighbours[neighbourLoc][i];
                if (!visited[nextNeighbourLoc]) {
                    int dist = lowestCost[neighbourLoc][i] + distance[neighbourLoc];
                    if (dist < distance[nextNeighbourLoc]) {
                        distance[nextNeighbourLoc] = dist;
                        previous[nextNeighbourLoc] = neighbourLoc;
                    }
                    // TODO: Improve efficiency. Only add node if not currently on list. Do so by searching for it.
                    boolean found = false;
                    for (int j = 0; j < count; j++) {
                        if (nodes[j] == nextNeighbourLoc) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        nodes[count++] = nextNeighbourLoc;
                    }
                }
            }
        }

        // TODO: Add comments and simplify
        // Print path
        count = 0;
        int currentLoc = goalGroupLoc;

        // Backtrack from goalGroupId to startGroupId
        while (currentLoc != startGroupLoc) {
            if (count >= distance.length) {
                return 0;
            }
            distance[count++] = currentLoc;
            currentLoc = previous[currentLoc];
        }
        distance[count++] = startGroupLoc; // Reverse path is in distance array

        // Now produce the actual path
        int pathLen = 0;

        // Add first segment of the path
        // nextId is group id. nextLoc is location of that id in adjacency list (array)
        int nextId = distance[count - 2], lastId = startGroupLoc;
        int nextLoc = findInArray(neighbours[lastId], nextId);

        if (nextLoc == -1) {
            return 0;
        }

        // Copy (but do not include duplicate start node - start from 1 instead of 0).
        System.arraycopy(pathSubgoals[lastId][nextLoc], 0, path, 0, pathSubgoals[lastId][nextLoc].length);
        pathLen += pathSubgoals[lastId][nextLoc].length;
        lastId = nextId;

        for (int i = count - 3; i >= 0; i--) {
            nextId = distance[i];
            nextLoc = findInArray(neighbours[lastId], nextId);
            // Copy (but do not include duplicate start node - start from 1 instead of 0).
            if (nextLoc < 0) { // if nextLoc < 0, path cannot be found
                return 0;
            }
            if (pathSubgoals[lastId][nextLoc].length >= 1)
                System.arraycopy(pathSubgoals[lastId][nextLoc], 1, path, pathLen - 1 + 1, pathSubgoals[lastId][nextLoc].length - 1);
            pathLen += pathSubgoals[lastId][nextLoc].length - 1;
            lastId = nextId;
        }
        return pathLen;
    }

    public void computeBasePaths(Map<Integer, Region> regionHashMap, boolean asSubgoals, SearchStats searchStats, CompressAStar compressAStar, HillClimbingWithClosedSet hc) {
        numGroups = regionHashMap.size();

        int arraySize = (int) Math.ceil(numGroups * 1.1);
        neighbours = new int[arraySize][];
        lowestCost = new int[arraySize][];
        pathSubgoals = new int[arraySize][][];

        Region startRegion, goalRegion;
        int startGroupLoc, goalGroupLoc;
        Set<Integer> neighbourIds;

        List<SearchState> path;

        int totalPathLength = 0, totalPathCost = 0;
        long timeToFindAStarPathsOffline = 0, timeToPerformHCCompression = 0;
        long start;

        for (int i = 0; i < numGroups; i++) {
            startRegion = regionHashMap.get(i + START_NUM);
            startGroupLoc = i;

            // TODO: Support deeper neighbourhood levels, currently only considering direct neighbours
            neighbourIds = startRegion.getNeighborIds();

            int numNeighbours = neighbourIds.size();
            neighbours[startGroupLoc] = new int[numNeighbours];
            lowestCost[startGroupLoc] = new int[numNeighbours];
            pathSubgoals[startGroupLoc] = new int[numNeighbours][];

            int count = 0;
            for (int neighbourId : neighbourIds) {
                goalRegion = regionHashMap.get(neighbourId);
                goalGroupLoc = neighbourId - START_NUM;

                int startRegionRep = startRegion.getRegionRepresentative();
                int goalRegionRep = goalRegion.getRegionRepresentative();

                start = System.nanoTime();
                path = compressAStar.findPath(new SearchState(startRegionRep), new SearchState(goalRegionRep), searchStats);
                timeToFindAStarPathsOffline += System.nanoTime() - start;

                totalPathLength += path.size();
                int pathCost = findPathCost(path, compressAStar.getSearchProblem());
                totalPathCost += pathCost;

                neighbours[startGroupLoc][count] = goalGroupLoc;
                lowestCost[startGroupLoc][count] = pathCost;

                if (asSubgoals) {
                    // Compress path using hill climbing
                    start = System.nanoTime();
                    pathSubgoals[startGroupLoc][count] = findOptimallyCompressedPath(path, hc, compressAStar, searchStats);
                    timeToPerformHCCompression += System.nanoTime() - start;

                    if (pathSubgoals[startGroupLoc][count].length > 2) {
                        searchStats.incrementNumberOfSubgoals(pathSubgoals[startGroupLoc][count].length - 2);
                        searchStats.incrementNumberOfPathsThatHaveSubgoals(1);
                    }
                } else {
                    // Store full A* id path
                    pathSubgoals[startGroupLoc][count] = getIdPath(path);
                }

                searchStats.incrementNumPaths(1);
                count++;
            }
        }

        searchStats.setPathLength(totalPathLength);
        searchStats.setTimeToFindAStarPathsOffline(timeToFindAStarPathsOffline);
        searchStats.setTimeToPerformHCCompression(timeToPerformHCCompression);
    }

    public static int[] getIdPath(List<SearchState> path) {
        int[] idPath = new int[path.size()];
        for (int i = 0; i < idPath.length; i++) {
            idPath[i] = path.get(i).getStateId();
        }
        return idPath;
    }

    private void saveDB(String fileName) {
        try (PrintWriter out = new PrintWriter(fileName)) {
            out.println(numGroups);
            for (int i = 0; i < numGroups; i++) {
                int numNeighbours = neighbours[i].length;
                out.println(numNeighbours);
                for (int j = 0; j < numNeighbours; j++) {
                    out.print(neighbours[i][j] + "\t");
                }
                out.println();
                for (int j = 0; j < numNeighbours; j++) {
                    out.print(lowestCost[i][j] + "\t");
                }
                out.println();
                for (int j = 0; j < numNeighbours; j++) {
                    int pathLength = pathSubgoals[i][j].length;
                    out.print(pathLength + "\t");
                    for (int k = 0; k < pathLength; k++) {
                        out.print("\t" + pathSubgoals[i][j][k]);
                    }
                    out.println();
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param regionId region id where wall was added
     */
    public void recomputeBasePathsAfterElimination(int regionId) {
        // This is the elimination case, where adding a wall leads to the removal of a region
        numGroups -= 1;

        // Find array location of region
        int groupLoc = regionId - START_NUM;

        // Iterate over neighbours of the region to eliminate to scrub references to it
        for (int i = 0; i < this.neighbours[groupLoc].length; i++) {
            // Grab location of neighbour
            int neighbourLoc = this.neighbours[groupLoc][i];
            // Iterate over neighbours of neighbour to find region to eliminate
            int indexOfRegionToEliminate = -1;
            for (int j = 0; j < this.neighbours[neighbourLoc].length; j++) {
                if (this.neighbours[neighbourLoc][j] == groupLoc) {
                    indexOfRegionToEliminate = j;
                    break;
                }
            }
            // If the region to eliminate was not stored as a neighbour of its neighbour
            if (indexOfRegionToEliminate != -1) {
                // Copying into smaller arrays here
                this.neighbours[neighbourLoc] = copyArrayExceptIndex(this.neighbours[neighbourLoc], indexOfRegionToEliminate);
                this.lowestCost[neighbourLoc] = copyArrayExceptIndex(this.lowestCost[neighbourLoc], indexOfRegionToEliminate);
                this.pathSubgoals[neighbourLoc] = copyArrayExceptIndex(this.pathSubgoals[neighbourLoc], indexOfRegionToEliminate);
            }
        }

        // Tombstone eliminated region
        this.neighbours[groupLoc] = null;
        this.pathSubgoals[groupLoc] = null;
        this.lowestCost[groupLoc] = null;
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

    /**
     * @param arr   array to copy
     * @param index index to exclude while copying
     * @return array of arr.length - 1 without the element at index
     */
    private static int[][] copyArrayExceptIndex(int[][] arr, int index) {
        int[][] newArr = new int[arr.length - 1][];
        int newIndex = 0;
        for (int i = 0; i < arr.length; i++) {
            if (i != index) {
                newArr[newIndex++] = arr[i];
            }
        }
        return newArr;
    }

    public void recomputeCornerBlocker(int regionId, int neighbourId) {
        // In the blocker case, we have two regions that were previously neighbours but now aren't

        // Grab location of region and neighbour
        int groupLoc = regionId - START_NUM;
        int neighbourLoc = neighbourId - START_NUM;

        // Update region’s neighbourhood
        int indexOfNeighborLoc = -1;
        for (int i = 0; i < this.neighbours[groupLoc].length; i++) {
            // Need to find index of neighbourLoc
            if (this.neighbours[groupLoc][i] == neighbourLoc) {
                indexOfNeighborLoc = i;
                break;
            }
        }

        // If the region to eliminate was not stored as a neighbour of its neighbour
        if (indexOfNeighborLoc != -1) {
            this.neighbours[groupLoc] = copyArrayExceptIndex(this.neighbours[groupLoc], indexOfNeighborLoc);
            this.lowestCost[groupLoc] = copyArrayExceptIndex(this.lowestCost[groupLoc], indexOfNeighborLoc);
            this.pathSubgoals[groupLoc] = copyArrayExceptIndex(this.pathSubgoals[groupLoc], indexOfNeighborLoc);
        }

        int indexOfGroupLoc = -1;
        for (int i = 0; i < this.neighbours[neighbourLoc].length; i++) {
            // Need to find index of groupLoc
            if (this.neighbours[neighbourLoc][i] == groupLoc) {
                indexOfGroupLoc = i;
                break;
            }
        }

        if (indexOfGroupLoc != -1) {
            this.neighbours[neighbourLoc] = copyArrayExceptIndex(this.neighbours[neighbourLoc], indexOfGroupLoc);
            this.lowestCost[neighbourLoc] = copyArrayExceptIndex(this.lowestCost[neighbourLoc], indexOfGroupLoc);
            this.pathSubgoals[neighbourLoc] = copyArrayExceptIndex(this.pathSubgoals[neighbourLoc], indexOfGroupLoc);
        }
    }

    public void recomputeBasePaths(int regionId, Map<Integer, Region> regions, CompressAStar compressAStar, HillClimbingWithClosedSet hc, SearchStats searchStats) {
        // This is for all cases where the paths change but the neighbourhood does not:
        // E.g. wall on region rep, wall that moves region rep, wall that changes shortest path

        // Find array location of region
        int groupLoc = regionId - START_NUM;
        List<SearchState> path;

        // Update region’s paths to its neighbours (and their costs)
        // Update the region’s neighbours paths to it (and their costs)
        for (int i = 0; i < this.neighbours[groupLoc].length; i++) {
            // Grab location of neighbour
            int neighbourLoc = this.neighbours[groupLoc][i];

            int startRegionRep = regions.get(regionId).getRegionRepresentative();
            int goalRegionRep = regions.get(neighbourLoc + START_NUM).getRegionRepresentative();

            path = compressAStar.findPath(new SearchState(startRegionRep), new SearchState(goalRegionRep), searchStats);
            int pathCost = path == null ? Integer.MAX_VALUE : findPathCost(path, compressAStar.getSearchProblem());

            // Update lowestCost of region
            this.lowestCost[groupLoc][i] = pathCost;
            // Update path to region

            if (pathCost == Integer.MAX_VALUE) {
                this.pathSubgoals[groupLoc][i] = null;
            } else {
                this.pathSubgoals[groupLoc][i] = findOptimallyCompressedPath(path, hc, compressAStar, searchStats);
            }

            // Need to find correct neighborId to update
            int indexToUpdate = -1;
            for (int j = 0; j < this.neighbours[neighbourLoc].length; j++) {
                if (this.neighbours[neighbourLoc][j] == groupLoc) {
                    indexToUpdate = j;
                    break;
                }
            }

            // If the region to update was not stored as a neighbour of its neighbour
            if (indexToUpdate != -1) {
                // Update lowestCost of neighbour
                this.lowestCost[neighbourLoc][indexToUpdate] = pathCost;
                // Update path to neighbour
                if (pathCost == Integer.MAX_VALUE) {
                    this.pathSubgoals[groupLoc][i] = null;
                } else {
                    path = new ArrayList<>(path.reversed());
                    this.pathSubgoals[neighbourLoc][indexToUpdate] = findOptimallyCompressedPath(path, hc, compressAStar, searchStats);
                }
            }
        }
    }

    public void recomputeBasePathsAfterPartition(Map<Integer, Region> regions, Set<Integer> neighborIds, CompressAStar compressAStar, HillClimbingWithClosedSet hc, SearchStats searchStats) {
        // This is the partition case, where adding a wall leads to the splitting of a region into two or more smaller regions
        List<SearchState> path;

        // freeSpace has already been updated in DBAStarUtil (needed the information for map updates)

        // regionIds contains the ids of all the regions the original region was split into after the partition
        // Use those to overwrite the neighborId arrays of the regions
        for (Integer id : neighborIds) {
            // Need to update neighborhoods of all the new regions
            int groupLoc = id - START_NUM;

            // Get neighbours of the new/surrounding regions (updated in map.recomputeNeighbors)
            Set<Integer> neighbours = regions.get(id).getNeighborIds();
            // Create an int array with the same size as the HashSet
            int[] neighbourArray = new int[neighbours.size()];

            // Iterate through the HashSet and copy its elements to the array
            int index = 0;
            for (Integer neighbour : neighbours) {
                neighbourArray[index++] = neighbour - START_NUM;
            }

            // Overwrite the neighbourId array of the region
            this.neighbours[groupLoc] = neighbourArray;
            // Create new lowest cost and paths arrays of correct size
            // FIXME: This is throwing away useful data, find a way to not to
            // all but the paths to the new regions should be unaffected, so throwing those away and recomputing them is a waste
            this.lowestCost[groupLoc] = new int[neighbours.size()];
            this.pathSubgoals[groupLoc] = new int[neighbours.size()][];
        }

        for (Integer id : neighborIds) {
            // Iterate over neighbours of the region
            int groupLoc = id - START_NUM;

            for (int i = 0; i < this.neighbours[groupLoc].length; i++) {
                // Grab location of neighbour
                int neighbourLoc = this.neighbours[groupLoc][i];

                int startRegionRep = regions.get(id).getRegionRepresentative();
                int goalRegionRep = regions.get(neighbourLoc + START_NUM).getRegionRepresentative();

                path = compressAStar.findPath(new SearchState(startRegionRep), new SearchState(goalRegionRep), searchStats);
                int pathCost = path == null ? Integer.MAX_VALUE : findPathCost(path, compressAStar.getSearchProblem());

                // Update lowestCost
                this.lowestCost[groupLoc][i] = pathCost;
                int indexToUpdate = -1;
                for (int j = 0; j < this.neighbours[neighbourLoc].length; j++) {
                    if (this.neighbours[neighbourLoc][j] == groupLoc) {
                        indexToUpdate = j;
                        break;
                    }
                }
                // If the region to update was not stored as a neighbour of its neighbour
                if (indexToUpdate != -1) {
                    // Update lowestCost of neighbour
                    this.lowestCost[neighbourLoc][indexToUpdate] = pathCost;

                    if (pathCost == Integer.MAX_VALUE) {
                        this.pathSubgoals[groupLoc][i] = null;
                    } else {
                        this.pathSubgoals[groupLoc][i] = findOptimallyCompressedPath(path, hc, compressAStar, searchStats);
                    }
                }
            }
        }
    }

    public void recomputeBasePathsIfSolitary(int regionId) {
        // Case where new region has no neighbours (e.g. is surrounded by walls)

        // freeSpace has already been updated in DBAStarUtil (needed the information for map updates)

        // Find array location of region
        int groupLoc = regionId - START_NUM;

        // Create arrays for new group
        this.neighbours[groupLoc] = new int[0];
        this.lowestCost[groupLoc] = new int[0];
        this.pathSubgoals[groupLoc] = new int[0][];
    }

    public void recomputeBasePathsIfConnected(int regionId, Map<Integer, Region> regions, Set<Integer> neighborIds, CompressAStar compressAStar, HillClimbingWithClosedSet hc, SearchStats searchStats) {
        // Case where new region has neighbours (e.g. is in a new sector but connected)

        // Find array location of region
        int groupLoc = regionId - START_NUM;

        int numNeighbours = neighborIds.size();

        // Create arrays for new group
        this.neighbours[groupLoc] = new int[numNeighbours];
        this.lowestCost[groupLoc] = new int[numNeighbours];
        this.pathSubgoals[groupLoc] = new int[numNeighbours][];

        List<SearchState> path;

        // Update region’s paths to its neighbours (and their costs)
        // Update the region’s neighbours paths to it (and their costs)
        int i = 0;
        for (int neighbourId : neighborIds) {
            // Grab location of neighbour
            int neighbourLoc = neighbourId - START_NUM;

            int startRegionRep = regions.get(regionId).getRegionRepresentative();
            int goalRegionRep = regions.get(neighbourLoc + START_NUM).getRegionRepresentative();

            path = compressAStar.findPath(new SearchState(startRegionRep), new SearchState(goalRegionRep), searchStats);
            int pathCost = path == null ? Integer.MAX_VALUE : findPathCost(path, compressAStar.getSearchProblem());

            this.neighbours[groupLoc][i] = neighbourLoc;
            // Update lowestCost of region
            this.lowestCost[groupLoc][i] = pathCost;
            // Update path to region
            if (pathCost == Integer.MAX_VALUE) {
                this.pathSubgoals[groupLoc][i] = null;
            } else {
                this.pathSubgoals[groupLoc][i] = findOptimallyCompressedPath(path, hc, compressAStar, searchStats);
            }

            // Need to increase size of arrays of neighbour
            int len = this.neighbours[neighbourLoc].length;

            // Resize arrays
            int[] resizedNeighbourId = new int[len + 1];
            System.arraycopy(this.neighbours[neighbourLoc], 0, resizedNeighbourId, 0, len);
            this.neighbours[neighbourLoc] = resizedNeighbourId;

            int[][] resizedPaths = new int[len + 1][];
            System.arraycopy(this.pathSubgoals[neighbourLoc], 0, resizedPaths, 0, len);
            this.pathSubgoals[neighbourLoc] = resizedPaths;

            int[] resizedCosts = new int[len + 1];
            System.arraycopy(this.lowestCost[neighbourLoc], 0, resizedCosts, 0, len);
            this.lowestCost[neighbourLoc] = resizedCosts;

            // Assign neighbourId
            this.neighbours[neighbourLoc][len] = groupLoc;
            this.lowestCost[neighbourLoc][len] = pathCost;

            if (pathCost == Integer.MAX_VALUE) {
                this.pathSubgoals[groupLoc][i] = null;
            } else {
                path = new ArrayList<>(path.reversed());
                this.pathSubgoals[neighbourLoc][len] = findOptimallyCompressedPath(path, hc, compressAStar, searchStats);
            }

            i++;
        }
    }

    public int[] getNeighboursForRegion(int regionId) {
        return neighbours[regionId - START_NUM];
    }

    public int[][][] getPathSubgoals() {
        return pathSubgoals;
    }

    public int getNumGroups() {
        return numGroups;
    }
}
