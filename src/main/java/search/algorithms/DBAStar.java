package search.algorithms;

import database.SubgoalDB;
import database.SubgoalDBRecord;
import map.AbstractedMap;
import map.GameMap;
import map.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.MapSearchProblem;
import search.RegionSearchProblem;
import search.SearchState;
import search.SearchUtil;
import stats.SearchStats;

import java.util.*;

public class DBAStar implements DynamicSearchAlgorithm {
    private final GameMap gameMap;
    private final AbstractedMap abstractedMap;
    private final RegionSearchProblem regionSearchProblem;
    private final SubgoalDB subgoalDB;
    private final SearchStats searchStats;
    private final HillClimbingWithClosedSet hillClimbing;
    private final CompressAStar compressAStar;

    private List<SearchState> subgoals;

    private static final Logger logger = LogManager.getLogger(DBAStar.class);

    private final Set<Integer> neighbouringRegionsInSameSector = new TreeSet<>(Comparator.reverseOrder());
    private final List<Integer> neighbourStates = new ArrayList<>(8);
    private final Set<Integer> neighborIdsSet = new HashSet<>();

    private final int[] directNeighbours = new int[8];

    private final boolean compressed;

    public DBAStar(GameMap gameMap, int gridSize, boolean compressed) {
        this.searchStats = new SearchStats();

        this.gameMap = gameMap;
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        this.abstractedMap = new AbstractedMap(gameMap, gridSize, searchStats);
        this.regionSearchProblem = new RegionSearchProblem(abstractedMap);

        long start = System.nanoTime();
        this.compressAStar = new CompressAStar(mapSearchProblem);
        this.hillClimbing = new HillClimbingWithClosedSet(mapSearchProblem);
        this.subgoalDB = new SubgoalDB(abstractedMap.getRegionIdToRegionMap(), searchStats, compressAStar, hillClimbing);
        searchStats.setTimeToGenerateDatabase(System.nanoTime() - start);

        this.compressed = compressed;
    }

    public List<SearchState> findPath(SearchState start, SearchState goal, SearchStats searchStats) {
        long startTime = System.nanoTime();

        // Get start and goal region ids
        int startRegion = abstractedMap.getRegionIdFromMap(start.getStateId());
        int goalRegion = abstractedMap.getRegionIdFromMap(goal.getStateId());

        // If start and goal are the same region, use A* search to find the path rather than DBA*
        if (startRegion == goalRegion) {
            return compressAStar.findPath(start, goal, searchStats);
        }

        // Find start and goal region representatives
        SearchState startRegionRep = regionSearchProblem.findRegionRepresentativeFromMap(start);
        SearchState goalRegionRep = regionSearchProblem.findRegionRepresentativeFromMap(goal);

        // Initialize ArrayList to hold subgoals (they are stored, so they can be visualized on the map)
        subgoals = new ArrayList<>();

        // Get record of hill-climbing-reachable subgoals from the database
        SubgoalDBRecord record = subgoalDB.getRecord(startRegion, goalRegion, compressed, hillClimbing, searchStats);
        // The array of subgoals contains state ids the agent can hill-climb along
        // It does not include the start region representative and the goal region representative (to save memory since
        // we have computed those already)

        if (record == null) {
            return null;
        }

        int[] subgoalsFromRecord = record.getSubgoals();

//        logger.info("Subgoals: {}", Arrays.toString(subgoalsFromRecord));

        // Set current start to be the start state given by the user
        SearchState currentStart = start;
        // Set current goal to be the region rep of the start region
        SearchState currentGoal = startRegionRep;
        // Add current goal to subgoals
        subgoals.add(currentGoal);

        // Declare ArrayList to hold complete path and path fragments (path fragments are paths between subgoals)
        List<SearchState> path = new ArrayList<>();
        List<SearchState> newPathFragment;

        int currentIndex = -1;

        // Performs completely unoptimized DBA* - move between all subgoals, including region reps of start and goal regions
        while (true) {
            if (currentStart == start || currentGoal == goal) {
                newPathFragment = compressAStar.findPath(currentStart, currentGoal, searchStats);
            } else {
                newPathFragment = hillClimbing.findPath(currentStart, currentGoal, searchStats);
            }

            if (newPathFragment == null) {
                newPathFragment = compressAStar.findPath(currentStart, currentGoal, searchStats);
            }

            SearchUtil.mergePaths(path, newPathFragment);

            SearchState curr = newPathFragment.getLast();

            if (curr.equals(goal)) break;

            currentStart = currentGoal;
            currentIndex++;

            if (currentIndex < subgoalsFromRecord.length) {
                currentGoal = new SearchState(subgoalsFromRecord[currentIndex]);
                subgoals.add(currentGoal);
            } else if (currentIndex == subgoalsFromRecord.length) {
                currentGoal = goalRegionRep;
                subgoals.add(currentGoal);
            } else {
                currentGoal = goal; // Go towards global goal
            }
        }

        long endTime = System.nanoTime();
        searchStats.setTimeToFindPathOnline(endTime - startTime);

        return path;
    }

    // TODO: Add comments and simplify

    /**
     * This version skips going to the start region rep, which leads to lower suboptimality
     */
//    @Override
//    public List<SearchState> findPath(SearchState start, SearchState goal, SearchStats searchStats) {
//        long startTime = System.nanoTime();
//
//        // Get start and goal region ids
//        int startRegion = abstractedMap.getRegionIdFromMap(start.getStateId());
//        int goalRegion = abstractedMap.getRegionIdFromMap(goal.getStateId());
//
//        // If start and goal are the same region, use A* search to find the path rather than DBA*
//        if (startRegion == goalRegion) {
//            return compressAStar.findPath(start, goal, searchStats);
//        }
//
//        // Find start and goal region representatives
//        SearchState startRegionRep = regionSearchProblem.findRegionRepresentativeFromMap(start);
//        // SearchState goalRegionRep = regionSearchProblem.findRegionRepresentativeFromMap(goal);
//
//        // Initialize ArrayList to hold subgoals (they are stored, so they can be visualized on the map)
//        subgoals = new ArrayList<>();
//
//        // Get record of hill-climbing-reachable subgoals from the database
//        SubgoalDBRecord record = subgoalDB.getRecord(startRegion, goalRegion, compressed, hillClimbing, searchStats);
//        // The array of subgoals contains state ids the agent can hill-climb along
//        // It does not include the start region representative and the goal region representative (to save memory since
//        // we have computed those already)
//        if (record == null) {
//            return null;
//        }
//
//        int[] subgoalsFromRecord = record.getSubgoals();
//
////        logger.info("Subgoals: {}", Arrays.toString(subgoalsFromRecord));
//
//        // Set current start to be the start state given by the user
//        SearchState currentStart = start;
//        // Set current goal to be the region rep of the start region
//        SearchState currentGoal = startRegionRep;
//        // Add current goal to subgoals
//        subgoals.add(currentGoal);
//
//        // Declare ArrayList to hold complete path and path fragments (path fragments are paths between subgoals)
//        List<SearchState> path = new ArrayList<>();
//        List<SearchState> newPathFragment;
//
//        int currentIndex = -1;
//
//        while (true) {
//            if (currentStart == start) { // Start optimizations
//                // If the record did not contain any subgoals
//                if (subgoalsFromRecord == null || subgoalsFromRecord.length == 0) {
//                    // Try to hill-climb directly from start to goal
//                    newPathFragment = hillClimbing.findPath(currentStart, goal, searchStats);
//                } else {
//                    // Attempt to climb from the start to the first subgoal directly, skipping the region rep
//                    // of the start region. This leads to smoother paths.
//                    SearchState firstSubgoal = new SearchState(subgoalsFromRecord[0]);
//                    newPathFragment = hillClimbing.findPath(currentStart, firstSubgoal, searchStats);
//                    if (newPathFragment != null) {
//                        currentGoal = firstSubgoal;
//                        currentIndex++;
//                    }
//                }
//                if (newPathFragment == null) {
//                    newPathFragment = compressAStar.findPath(currentStart, currentGoal, searchStats);
//                }
//            } else if (currentGoal == goal) { // End optimizations
//                // Try to hill-climb from the currentStart to the global goal
//                newPathFragment = hillClimbing.findPath(currentStart, currentGoal, searchStats);
//                // If the hill-climbing fails
//                if (newPathFragment == null) {
//                    newPathFragment = compressAStar.findPath(currentStart, currentGoal, searchStats);
//                }
//            } else { // Regular case
//                newPathFragment = hillClimbing.findPath(currentStart, currentGoal, searchStats);
//            }
//
//            if (newPathFragment == null) {
//                logger.error("DBAStar: Unable to find subgoal path between {} and {}", currentStart.getStateId(), currentGoal.getStateId());
//                return null;
//            }
//
//            SearchUtil.mergePaths(path, newPathFragment);
//
//            SearchState curr = newPathFragment.getLast();
//
//            if (curr.equals(goal)) break;
//
//            if (!curr.equals(currentGoal)) { // Must have been interrupted
//                currentStart = newPathFragment.getLast();
//                logger.warn("Detect interruption. Trying database lookup at: {}", currentStart);
//                logger.info("Length of path at interrupt: {}", path.size());
//                record = subgoalDB.getRecord(currentStart.getStateId(), goal.getStateId(), compressed, hillClimbing, searchStats);
//                if (record != null) {
//                    subgoalsFromRecord = record.getSubgoals();
//                    // System.out.println("Using subgoal record from database: "+currentRecord.toString());
//                    currentIndex = -1;
//                    // Always go for start
//                    currentGoal = new SearchState(record.getStartRegionRep());
//                } else {
//                    currentGoal = goal;
//                }
//                continue;
//            }
//
//            if (currentGoal == goal) {
//                break;
//            }
//
//            currentStart = currentGoal;
//            currentIndex++;
//
//            if (subgoalsFromRecord == null) {
//                if (currentGoal.getStateId() == record.getGoalRegionRep()) {
//                    currentGoal = goal;
//                } else {
//                    currentGoal = new SearchState(record.getGoalRegionRep());
//                }
//            } else if (currentIndex < subgoalsFromRecord.length) {
//                currentGoal = new SearchState(subgoalsFromRecord[currentIndex]);
//                subgoals.add(currentGoal);
//            } else {
//                currentGoal = goal; // Go towards global goal
//            }
//        }
//
//        long endTime = System.nanoTime();
//        searchStats.setTimeToFindPathOnline(endTime - startTime);
//        searchStats.setPathLength(path.size());
//
//        return path;
//    }
    public List<SearchState> getSubgoals() {
        return subgoals;
    }

    public AbstractedMap getAbstractedMap() {
        return abstractedMap;
    }

    public SearchStats getSearchStats() {
        return searchStats;
    }

    public void addWall(int wallId) throws Exception {
        // Get region id of wall state before adding wall
        final int REGION_ID = abstractedMap.getRegionIdFromMap(wallId);
        Map<Integer, Region> regionHashMap = abstractedMap.getRegionIdToRegionMap();
        final Region region = regionHashMap.get(REGION_ID);

        // Place wall
        SearchUtil.placeWall(wallId, gameMap, abstractedMap);

        // Elimination case
        if (region.getNumStates() == 1) {
//            System.out.println("Elimination");
            Set<Integer> neighbourIds = region.getNeighborIds();

            for (Integer neighbourId : neighbourIds) {
                abstractedMap.getRegionById(neighbourId).getNeighborIds().remove(REGION_ID);
            }

            abstractedMap.removeRegion(REGION_ID);

            // Database changes
            subgoalDB.recomputeBasePathsAfterElimination(REGION_ID);
        } else {
            // Get representative of region wall was placed in
            final int REGION_REP = abstractedMap.getRegionRepFromRegionId(REGION_ID);

            // Get sector id from wall id
            final int SECTOR_ID = abstractedMap.getSectorId(wallId);

            // Get sector bounds from sector id
            final int START_ROW = abstractedMap.getStartRowOfSector(SECTOR_ID);
            final int END_ROW = abstractedMap.getEndRowOfSector(SECTOR_ID);
            final int START_COL = abstractedMap.getStartColOfSector(SECTOR_ID);
            final int END_COL = abstractedMap.getEndColOfSector(SECTOR_ID);

            // Get wall row and col
            final int WALL_ROW = gameMap.getRowFromStateId(wallId);
            final int WALL_COL = gameMap.getColFromStateId(wallId);

            // Get direct neighbours of wall state
            final int NEIGHBOR_NE = abstractedMap.getNeighbourNE(WALL_ROW, WALL_COL);
            final int NEIGHBOR_SE = abstractedMap.getNeighbourSE(WALL_ROW, WALL_COL);
            final int NEIGHBOR_SW = abstractedMap.getNeighbourSW(WALL_ROW, WALL_COL);
            final int NEIGHBOR_NW = abstractedMap.getNeighbourNW(WALL_ROW, WALL_COL);

            final boolean IS_AT_SECTOR_EDGE = WALL_ROW == START_ROW || WALL_ROW == END_ROW - 1 || WALL_COL == START_COL || WALL_COL == END_COL - 1;

            int count = 0;
            abstractedMap.getDirectNeighbourValues(directNeighbours, WALL_ROW, WALL_COL);

            boolean isPotentialPartition = IS_AT_SECTOR_EDGE || ((abstractedMap.getR2Sum(directNeighbours) >= 2 && abstractedMap.getC2Sum(directNeighbours) >= 2)
                    || (abstractedMap.getR1Sum(directNeighbours) != 0 && abstractedMap.getR2Sum(directNeighbours) != 0 && abstractedMap.getR3Sum(directNeighbours) != 0)
                    || (abstractedMap.getC1Sum(directNeighbours) != 0 && abstractedMap.getC2Sum(directNeighbours) != 0 && abstractedMap.getC3Sum(directNeighbours) != 0));
            boolean isPartition = false;

            // Find first neighbour that is in same region as where the wall was placed
            // Must exist since this is not elimination, must touch by construction

            // TODO: Test
            if (isPotentialPartition) {
                // Get neighbour states
                // Take first neighbour state that has same region id as state where wall was placed to start BFS
                int sid = -1;
                List<Integer> neighbourStateIds = abstractedMap.getStateNeighbourIds(WALL_ROW, WALL_COL);
                for (int neighbourStateId : neighbourStateIds) {
                    if (abstractedMap.getRegionIdFromMap(neighbourStateId) == REGION_ID) {
                        sid = neighbourStateId;
                        break;
                    }
                }

                if (sid == -1) {
                    throw new Exception("State without neighbours in same region!");
                }

                // Run BFS in region and count states
                Queue<Integer> stateIds = new LinkedList<>();
                List<Integer> neighbors = new ArrayList<>(8);

                stateIds.add(sid);
                HashSet<Integer> visited = new HashSet<>();
                visited.add(sid);
                while (!stateIds.isEmpty()) {
                    int id = stateIds.remove();

                    int row = gameMap.getRowFromStateId(id);
                    int col = gameMap.getColFromStateId(id);

                    abstractedMap.getStateNeighbourIds(row, col, neighbors);

                    for (int nid : neighbors) {
                        int nr = gameMap.getRowFromStateId(nid);
                        int nc = gameMap.getColFromStateId(nid);
                        if (abstractedMap.isInRange(nr, nc, START_ROW, START_COL, END_ROW, END_COL) && !visited.contains(nid)) {
                            stateIds.add(nid);
                            visited.add(nid);
                        }
                    }
                }

                if (visited.size() != region.getNumStates() - 1) {
                    isPartition = true;
                } else {
                    if (IS_AT_SECTOR_EDGE) {
                        neighbors.clear();
                        abstractedMap.getStateNeighbourIds(WALL_ROW, WALL_COL, neighbors);
                        List<Integer> neighboursInDifferentRegions = new ArrayList<>();

                        int goalInRegion = -1;
                        for (int nid : neighbors) {
                            if (abstractedMap.getRegionIdFromMap(nid) == REGION_ID) {
                                neighboursInDifferentRegions.add(nid);
                            } else {
                                goalInRegion = nid;
                            }
                        }

                        for (int nid : neighboursInDifferentRegions) {
                            if (compressAStar.findPath(new SearchState(nid), new SearchState(goalInRegion), new SearchStats()) == null) {
                                isPartition = true;
                            }
                        }
                    }
                }
            }

            // If we are placing a wall in the corner of a sector, we may have a pathblocker case
            if (!isPartition && IS_AT_SECTOR_EDGE) {
                final boolean IS_NORTH_EDGE = WALL_ROW == START_ROW;
                final boolean IS_EAST_EDGE = WALL_COL == END_COL;
                final boolean IS_SOUTH_EDGE = WALL_ROW == END_ROW;
                final boolean IS_WEST_EDGE = WALL_COL == START_COL;

                final boolean IS_TOP_LEFT_CORNER = IS_NORTH_EDGE && IS_WEST_EDGE;
                final boolean IS_TOP_RIGHT_CORNER = IS_NORTH_EDGE && IS_EAST_EDGE;
                final boolean IS_BOTTOM_RIGHT_CORNER = IS_SOUTH_EDGE && IS_EAST_EDGE;
                final boolean IS_BOTTOM_LEFT_CORNER = IS_SOUTH_EDGE && IS_WEST_EDGE;

                int neighbourRegion = 0;
                int neighbourRegionRep = 0;

                // Corner cases (check if wall is in a sector corner and get the region id of the corner this corner is
                // touching, if applicable

                // TODO: Test
                // If the wall is in a corner and the state diagonal to it is not a wall, we have a corner blocker
                // This means the two regions that are currently neighbours shouldn't be anymore
                if (IS_TOP_LEFT_CORNER && (NEIGHBOR_NW != GameMap.WALL_CHAR)) {
                    neighbourRegion = NEIGHBOR_NW;
                    neighbourRegionRep = abstractedMap.getRegionRepFromRegionId(NEIGHBOR_NW);
                } else if (IS_TOP_RIGHT_CORNER && (NEIGHBOR_NE != GameMap.WALL_CHAR)) {
                    neighbourRegion = NEIGHBOR_NE;
                    neighbourRegionRep = abstractedMap.getRegionRepFromRegionId(NEIGHBOR_NE);
                } else if (IS_BOTTOM_RIGHT_CORNER && (NEIGHBOR_SE != GameMap.WALL_CHAR)) {
                    neighbourRegion = NEIGHBOR_SE;
                    neighbourRegionRep = abstractedMap.getRegionRepFromRegionId(NEIGHBOR_SE);
                } else if (IS_BOTTOM_LEFT_CORNER && (NEIGHBOR_SW != GameMap.WALL_CHAR)) {
                    neighbourRegion = NEIGHBOR_SW;
                    neighbourRegionRep = abstractedMap.getRegionRepFromRegionId(NEIGHBOR_SW);
                }

                if (neighbourRegion == REGION_ID) {
                    throw new Exception("NeighbourRegion id calculation went wrong!");
                }

                if (neighbourRegionRep == -1) {
                    throw new Exception("Region rep for region " + neighbourRegion + "does not exist!");
                }

                // Pathblocker corner case
                if (neighbourRegion != 0) {
//                    System.out.println("Pathblocker");
                    // Get the neighbours of the region
                    Set<Integer> neighbours = region.getNeighborIds();
                    // Update region’s neighbourhood in groups map
                    neighbours.remove(neighbourRegion);

                    // Get the neighbours of its soon-to-be ex-neighbor
                    Region neighborRegion = regionHashMap.get(neighbourRegion);
                    Set<Integer> neighboursOfEx = neighborRegion.getNeighborIds();
                    // Update old neighbour’s neighbourhood in groups map
                    neighboursOfEx.remove(REGION_ID);

                    // If the wall was placed on the region rep
                    if (REGION_REP == wallId) {
                        // Recompute region representative
                        abstractedMap.computeRegionRepresentative(REGION_ID, region, START_ROW, END_ROW, START_COL, END_COL);
                    }

                    region.decrementNumStates();

                    // Database changes
                    subgoalDB.recomputeCornerBlocker(REGION_ID, neighbourRegion);

                    return;
                }

                // TODO: Pathblocker edge case
            }

            // Region Partition case

            if (isPartition) {
//                System.out.println("Partition");
                Set<Integer> neighborIds = region.getNeighborIds();

                // Wipe sector on the abstract map
                abstractedMap.wipeSectorPartitionCase(START_ROW, END_ROW, START_COL, END_COL, REGION_ID);

                // Re-abstract sector on the abstract map
                // Add parts region was partitioned into as neighbourIds for database recomputation
                neighborIds.addAll(abstractedMap.abstractStatesToGenerateRegions(SECTOR_ID, START_ROW, END_ROW, START_COL, END_COL));

                // Re-compute neighbourhood
                abstractedMap.computeRegionNeighbourhoodAndStoreRegionReps(START_ROW, END_ROW, START_COL, END_COL);

                // Database changes
                subgoalDB.recomputeBasePathsAfterPartition(regionHashMap, neighborIds, compressAStar, hillClimbing, searchStats);

                return;
            }

//            System.out.println("Shortest Path Change");

            region.decrementNumStates();

            // If the wall was placed on the region rep
            if (REGION_REP == wallId) {
                // Recompute region representative
                abstractedMap.computeRegionRepresentative(REGION_ID, region, START_ROW, END_ROW, START_COL, END_COL);
            }

            // Database changes
            subgoalDB.recomputeBasePaths(REGION_ID, regionHashMap, compressAStar, hillClimbing, searchStats);
        }
    }

    public void removeWall(int wallId) throws Exception {
        // Remove wall
        SearchUtil.placeOpenState(wallId, gameMap, abstractedMap);

        final int WALL_ROW = gameMap.getRowFromStateId(wallId);
        final int WALL_COL = gameMap.getColFromStateId(wallId);

        // Get eight neighbours of state where wall was removed
        neighbourStates.clear();
        abstractedMap.getStateNeighbourIds(WALL_ROW, WALL_COL, neighbourStates);

        // Check whether all eight neighbour states are walls
        boolean isSurroundedByWalls = true;
        for (int neighbourState : neighbourStates) {
            if (neighbourState != -1 && !gameMap.isWall(neighbourState)) {
                isSurroundedByWalls = false;
                break;
            }
        }

        Map<Integer, Region> regionHashMap = abstractedMap.getRegionIdToRegionMap();

        if (isSurroundedByWalls) {
//            System.out.println("New, isolated");
            final int REGION_ID = abstractedMap.getFreeRegionId();

            Region region = regionHashMap.get(REGION_ID);

            if (region != null) {
                throw new Exception("Error! Region already exists!");
            }

            // Assign the new regionId inside the states array
            abstractedMap.setState(WALL_ROW, WALL_COL, REGION_ID);

            abstractedMap.addRegion(REGION_ID, wallId, 1);

            // Database changes
            subgoalDB.recomputeBasePathsIfSolitary(REGION_ID);
        } else {
            // Check what sector the non-wall neighbourStates are in
            final int SECTOR_ID = abstractedMap.getSectorId(WALL_ROW, WALL_COL);

            // Store region ids of eight neighbour states
            Set<Integer> neighbouringRegions = new TreeSet<>();
            // Store region ids of neighbour states that are in the same sector as the wall being removed
            neighbouringRegionsInSameSector.clear();

            int neighbourRegionId = -1;

            for (int neighbourState : neighbourStates) {
                if (!abstractedMap.isWall(neighbourState)) {
                    // Get sector id of current neighbour state of wall (if it is not a wall)
                    int neighbourSector = abstractedMap.getSectorId(neighbourState);
                    // Get region id of current neighbour state of wall (if it is not a wall)
                    int neighbourRegion = abstractedMap.getRegionIdFromMap(neighbourState);
                    // Store neighbour regions in set
                    neighbouringRegions.add(neighbourRegion);
                    // If the neighbour state is in the same sector as the wall
                    if (neighbourSector == SECTOR_ID) {
                        // If there is multiple regions with the same sector id touching the wall being removed, we would have a merge case
                        neighbouringRegionsInSameSector.add(neighbourRegion);
                        neighbourRegionId = neighbourRegion;
                    }
                }
            }

            // If the new region is in a different sector than any of its neighbours, we have a new, connected region
            if (neighbouringRegionsInSameSector.isEmpty()) {
//                System.out.println("New, connected");
                // Get new regionId using freeSpace
                int regionId = abstractedMap.getFreeRegionId();

                // There should not be a group record with the new region id
                Region region = regionHashMap.get(regionId);
                if (region != null) {
                    throw new Exception("Error! Region already exists!");
                }

                // Assign the new regionId inside the states array
                abstractedMap.setState(WALL_ROW, WALL_COL, regionId);

                // Update region’s neighbourhood in groups map & update neighbourhood of all its neighbours in groups map
                for (Integer neighbouringRegion : neighbouringRegions) {
                    regionHashMap.get(neighbouringRegion).getNeighborIds().add(regionId);
                }

                // Create a new region object to store information on the new region
                abstractedMap.addRegion(regionId, wallId, 1, neighbouringRegions);

                // Database changes
                subgoalDB.recomputeBasePathsIfConnected(regionId, regionHashMap, abstractedMap.getRegionById(regionId).getNeighborIds(), compressAStar, hillClimbing, searchStats);
                return;
            }

            // Start of sector
            final int START_ROW = abstractedMap.getStartRowOfSector(SECTOR_ID);
            final int START_COL = abstractedMap.getStartColOfSector(SECTOR_ID);
            // End of sector
            final int END_ROW = abstractedMap.getEndRowOfSector(SECTOR_ID);
            final int END_COL = abstractedMap.getEndColOfSector(SECTOR_ID);

            // Since we have neither a new, solitary region, nor a new, connected region, our removed wall must be part
            // of an existing region

            if (neighbourRegionId == -1) {
                throw new Exception("neighbourRegionId has not been assigned!");
            }

            // Get region id from neighbours in same sector
            // In the merge case, which one is assigned here is random, but will be overwritten anyway
            // In the unblocker case, there is only one choice
            final int REGION_ID = neighbourRegionId;

            abstractedMap.setState(WALL_ROW, WALL_COL, REGION_ID);

            // Get region rep
            final int REGION_REP = abstractedMap.getRegionRepFromRegionId(REGION_ID);

            Region region = regionHashMap.get(REGION_ID);

            if (region == null) {
                throw new Exception("No region found for " + REGION_ID + "!");
            }

            // If the neighbours stored in the group record differ from those stored in the neighbouringRegions,
            // we must have an unblocker case, or a region merge case
            Set<Integer> neighboursFromGroupRec = region.getNeighborIds();
            // Removing the set of neighbours of the region from the neighbour states of the wall. If there are any elements
            // left, the wall must have at least one neighbour state that is not currently considered a neighbour
            neighbouringRegions.removeAll(neighboursFromGroupRec);
            neighbouringRegions.remove(REGION_ID);

            // TODO: Find more efficient way of passing neighbour ids
            if (!neighbouringRegions.isEmpty()) {
//                System.out.println("Merge");
                // Wipe sector on the abstract map
                Set<Integer> oldRegions = abstractedMap.wipeSectorMergeCase(START_ROW, END_ROW, START_COL, END_COL, neighbouringRegionsInSameSector);

                neighborIdsSet.clear();
                // Grab neighbours of old regions
                int smallest = Integer.MAX_VALUE;
                for (int oldRegionId : oldRegions) {
                    neighborIdsSet.addAll(regionHashMap.get(oldRegionId).getNeighborIds());

                    smallest = Math.min(smallest, oldRegionId);

                    // Remove region ids from mapping
//                    regionHashMap.put(oldRegionId, null);
                    regionHashMap.remove(oldRegionId);
                }

                // Re-abstract sector on the abstract map
                abstractedMap.abstractStatesToGenerateRegions(SECTOR_ID, START_ROW, END_ROW, START_COL, END_COL);

                // Re-compute neighbourhood
                abstractedMap.computeRegionNeighbourhoodAndStoreRegionReps(START_ROW, END_ROW, START_COL, END_COL);

                neighborIdsSet.addAll(neighbouringRegions);
                neighborIdsSet.removeAll(neighbouringRegionsInSameSector);

                neighborIdsSet.add(smallest);

                // Database changes
                subgoalDB.recomputeBasePathsAfterPartition(regionHashMap, neighborIdsSet, compressAStar, hillClimbing, searchStats);

                return;
            }

//            System.out.println("Shortest Path Change");

            region.incrementNumStates();

            // If the wall was placed on the region rep
            if (REGION_REP == wallId) {
                // Recompute region representative
                abstractedMap.computeRegionRepresentative(REGION_ID, region, START_ROW, END_ROW, START_COL, END_COL);
            }

            // Database changes
            subgoalDB.recomputeBasePaths(REGION_ID, regionHashMap, compressAStar, hillClimbing, searchStats);
        }
    }

    public SubgoalDB getSubgoalDB() {
        return subgoalDB;
    }

    @Override
    public GameMap getGameMap() {
        return gameMap;
    }

    public HillClimbingWithClosedSet getHillClimbing() {
        return hillClimbing;
    }

    public CompressAStar getCompressAStar() {
        return compressAStar;
    }
}
