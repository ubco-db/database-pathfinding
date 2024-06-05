package util;

import database.DBStats;
import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDynamicDB3;
import map.GameMap;
import map.GroupRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.*;

import java.util.*;

import static util.MapHelpers.*;

public class DBAStarUtil {
    private final int numNeighbourLevels;
    private final int cutoff;
    private final String mapFileName;
    private final String dbaStarDbPath;
    private static final Logger logger = LogManager.getLogger(DBAStarUtil.class);

    /**
     * @param cutoff        for HillClimbing algorithm
     * @param mapFileName   name of the .map file to use
     * @param dbaStarDbPath location where the database and other output will be generated
     */
    public DBAStarUtil(int cutoff, String mapFileName, String dbaStarDbPath) {
        // startNum is used as an offset for values in the squares array, region indexing in the array starts at startNum
        this.numNeighbourLevels = 1;
        this.cutoff = cutoff;
        this.mapFileName = mapFileName;
        this.dbaStarDbPath = dbaStarDbPath;
    }

    public DBAStar computeDBAStarDatabase(GameMap map, String wallStatus) throws Exception {
        long currentTime;

        SearchProblem problem = new MapSearchProblem(map);
        HillClimbing pathCompressAlgDba = new HillClimbing(problem, 10000);

        // Load abstract map and database
        logger.debug("Loading database.");

        SubgoalDynamicDB3 database = new SubgoalDynamicDB3(); // DP matrix in adjacency list representation (computed at run-time)

        // String fileName = getDBName(wallStatus, map.getGridSize());

        logger.debug("Loading map and performing abstraction...");

        // GreedyHC map abstraction
        DBStats dbStats = new DBStats();
        DBStats.init(dbStats);

        DBStatsRecord rec = new DBStatsRecord(dbStats.getSize());
        rec.addStat(0, "dbaStar (" + numNeighbourLevels + ")");
        rec.addStat(1, map.getGridSize());
        rec.addStat(3, cutoff);
        rec.addStat(4, mapFileName);
        rec.addStat(5, map.rows);
        rec.addStat(6, map.cols);

        currentTime = System.currentTimeMillis();
        map = map.sectorAbstract2();

        long resultTime = System.currentTimeMillis() - currentTime;
        rec.addStat(12, resultTime);
        rec.addStat(10, resultTime);
        dbStats.addRecord(rec);

        SearchProblem tmpProb = new MapSearchProblem(map);
        GameDB gameDB = new GameDB(tmpProb);

        logger.debug("Generating gameDB.");
        currentTime = System.currentTimeMillis();

        database = gameDB.computeDynamicDBUsingSubgoalDynamicDB3(database, pathCompressAlgDba, rec, numNeighbourLevels);
        logger.debug("Time to compute DBAStar gameDB: " + (System.currentTimeMillis() - currentTime));

        dbStats.addRecord(rec);
        database.setProblem(problem);

        logger.debug("Databases loaded.");

        return new DBAStar(problem, map, database);
    }

    /**
     * @param startId  state id for start of path
     * @param goalId   state id for start of path
     * @param dbaStar DBAStar object
     * @return path as ArrayList of SearchStates
     */
    public ArrayList<SearchState> getDBAStarPath(int startId, int goalId, DBAStar dbaStar) {
        StatsRecord stats = new StatsRecord();
        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);

        // ArrayList<SearchState> subgoals = dbaStar.getSubgoals();
        return dbaStar.computePath(start, goal, stats);
    }

    /**
     * @param startId    state id for start of path
     * @param goalId     state id for start of path
     * @param wallStatus used to name output files, either BW = before wall, AW = after wall, or RW = removed wall
     * @param dbaStar   DBAStar object
     */
    public void getDBAStarPath(int startId, int goalId, String wallStatus, DBAStar dbaStar) {
        GameMap map = dbaStar.getMap();

        AStar aStar = new AStar(dbaStar.getProblem());

        StatsRecord dbaStats = new StatsRecord();
        ArrayList<SearchState> path = dbaStar.computePath(new SearchState(startId), new SearchState(goalId), dbaStats);

        StatsRecord aStarStats = new StatsRecord();
        ArrayList<SearchState> optimalPath = aStar.computePath(new SearchState(startId), new SearchState(goalId), aStarStats);

        logger.info("AStar path cost: " + aStarStats.getPathCost() + " DBAStar path cost: " + dbaStats.getPathCost());
        logger.info("Suboptimality: " + ((((double) dbaStats.getPathCost()) / aStarStats.getPathCost()) - 1) * 100.0);

        if (path == null || path.isEmpty()) {
            logger.warn(String.format("No path was found between %d and %d!%n", startId, goalId));
        }
        map.computeCentroidMap().outputImage(dbaStarDbPath + wallStatus + "path_" + startId + "_" + goalId + ".png", path, dbaStar.getSubgoals());
        // map.computeCentroidMap().outputImage(dbaStarDbPath + wallStatus + "_optimal_path_" + startId + "_" + goalId + ".png", optimalPath, dbaStar.getSubgoals());
    }


    /**
     * @param wallStatus used to name output files, either BW = before wall, AW = after wall, or RW = removed wall
     * @return String
     */
    private String getDBName(String wallStatus, int gridSize) {
        return dbaStarDbPath + wallStatus + mapFileName + "_DBA-STAR_G" + gridSize + "_N" + numNeighbourLevels + "_C" + cutoff + ".dat";
    }

    /**
     * @param wallLoc   state id where wall will be place
     * @param dbaStarBW DBAStar object returned from computeDBAStarDatabaseUsingSubgoalDynamicDB3
     */
    public void recomputeWallAddition(int wallLoc, DBAStar dbaStarBW) throws Exception {
        // Extract map, problem, and database from dbaStarBW
        GameMap map = dbaStarBW.getMap();
        MapSearchProblem problem = (MapSearchProblem) dbaStarBW.getProblem();
        SubgoalDynamicDB3 dbBW = (SubgoalDynamicDB3) dbaStarBW.getDatabase();

        final int WALL_ROW = map.getRow(wallLoc);
        final int WALL_COL = map.getCol(wallLoc);

        // Get id of region that wall was placed in
        final int REGION_ID = map.squares[WALL_ROW][WALL_COL];
        logger.debug("Region: " + REGION_ID);

        // Check whether there is already a wall at the location the wall should be placed
        final boolean PRIOR_WALL = map.isWall(wallLoc) && problem.getMap().isWall(wallLoc);

        // Add wall to map and to map inside problem
        map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = '*';
        problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = '*';

        // Check whether the wall addition worked as intended
        if (!PRIOR_WALL && map.isWall(wallLoc) && problem.getMap().isWall(wallLoc)) {
            logger.debug("Wall at " + wallLoc + " set successfully!");
        } else {
            throw new Exception("Wall addition failed! There is a wall at " + wallLoc + " already");
        }

        // Get representative of region wall was placed in
        final int REGION_REP = map.getRegionRepFromRegionId(REGION_ID);
        logger.debug("Region rep: " + REGION_REP);

        // If the region rep is tombstoned
        if (REGION_REP == -1) {
            throw new Exception("Region rep for region " + REGION_ID + "does not exist!");
        }

        // Get groups containing information on all regions from map
        TreeMap<Integer, GroupRecord> groups = map.getGroups();

        // Get group record containing information on the region
        GroupRecord groupRecord = groups.get(REGION_ID);

        // Elimination case
        if (groupRecord.getNumStates() == 1) {
            logger.info("Addition: Elimination Case (wall at " + wallLoc + ")");
            // Get the neighbours of the region that will be eliminated
            HashSet<Integer> neighbours = groupRecord.getNeighborIds();

            for (Integer neighbour : neighbours) {
                // Get the region rep of the current neighbour
                int neighbourRep = map.getRegionRepFromRegionId(neighbour);
                // If the region rep is tombstoned
                if (neighbourRep == -1) {
                    throw new Exception("Region rep for region " + REGION_ID + "does not exist!");
                }
                // Remove the region as a neighbour of its neighbours
                groups.get(neighbour).getNeighborIds().remove(REGION_ID);
            }
            // Tombstone group record in groups map
            map.addGroup(REGION_ID, null);

            // Database changes
            dbBW.recomputeBasePathsAfterElimination(REGION_ID);
        } else {
            // Other cases
            final int SECTOR_ID = map.findSectorId(WALL_ROW, WALL_COL);
            logger.debug("Sector: " + SECTOR_ID);

            // Start of sector
            final int START_ROW = map.findStartRowOfSector(SECTOR_ID);
            final int START_COL = map.findStartColOfSector(SECTOR_ID);
            // End of sector
            final int END_ROW = map.findEndRowOfSector(START_ROW);
            final int END_COL = map.findEndColOfSector(START_COL);

            // Eight neighbours (states touching wall state)
            int NEIGHBOR_N = map.squares[WALL_ROW - 1][WALL_COL];

            // Need to check boundaries for bottom row of map (if not on map, treat square as wall)
            // TODO: Do I really need to check all of these?
            int NEIGHBOR_NE = map.isInBounds(WALL_ROW - 1, WALL_COL + 1) ? map.squares[WALL_ROW - 1][WALL_COL + 1] : GameMap.WALL_CHAR;
            int NEIGHBOR_E = map.isInBounds(WALL_ROW, WALL_COL + 1) ? map.squares[WALL_ROW][WALL_COL + 1] : GameMap.WALL_CHAR;
            int NEIGHBOR_SE = map.isInBounds(WALL_ROW + 1, WALL_COL + 1) ? map.squares[WALL_ROW + 1][WALL_COL + 1] : GameMap.WALL_CHAR;
            int NEIGHBOR_S = map.isInBounds(WALL_ROW + 1, WALL_COL) ? map.squares[WALL_ROW + 1][WALL_COL] : GameMap.WALL_CHAR;
            int NEIGHBOR_SW = map.isInBounds(WALL_ROW + 1, WALL_COL - 1) ? map.squares[WALL_ROW + 1][WALL_COL - 1] : GameMap.WALL_CHAR;

            int NEIGHBOR_W = map.squares[WALL_ROW][WALL_COL - 1];
            int NEIGHBOR_NW = map.squares[WALL_ROW - 1][WALL_COL - 1];

            boolean isAtSectorEdge = WALL_ROW == START_ROW || WALL_ROW == END_ROW || WALL_COL == START_COL || WALL_COL == END_COL;

            // If wall is between two walls, or between a wall and another region, we may have partition
            boolean isVerticalWall = isContinuousWall(NEIGHBOR_S, NEIGHBOR_N);
            boolean isHorizontalWall = isContinuousWall(NEIGHBOR_W, NEIGHBOR_E);
            boolean isBetweenVertical = !isVerticalWall && isBetweenWallAndOtherRegion(NEIGHBOR_S, NEIGHBOR_N, REGION_ID);
            boolean isBetweenHorizontal = !isHorizontalWall && isBetweenWallAndOtherRegion(NEIGHBOR_W, NEIGHBOR_E, REGION_ID);

            // If the diagonal state is open, but the cardinal ones surrounding it are not, we may have partition
            boolean isOpenDiagonalNW = isOpenDiagonal(NEIGHBOR_W, NEIGHBOR_NW, NEIGHBOR_N);
            boolean isOpenDiagonalNE = isOpenDiagonal(NEIGHBOR_N, NEIGHBOR_NE, NEIGHBOR_E);
            boolean isOpenDiagonalSE = isOpenDiagonal(NEIGHBOR_E, NEIGHBOR_SE, NEIGHBOR_S);
            boolean isOpenDiagonalSW = isOpenDiagonal(NEIGHBOR_S, NEIGHBOR_SW, NEIGHBOR_W);

            boolean isPotentialPartition = isVerticalWall || isHorizontalWall || isBetweenVertical || isBetweenHorizontal || isOpenDiagonalNW || isOpenDiagonalNE || isOpenDiagonalSE || isOpenDiagonalSW;
            boolean isPartition = false;

            // Find first neighbour that is in same region as where the wall was placed
            // Must exist since this is not elimination, must touch by construction

            if (isPotentialPartition) {
                // Get neighbour states
                // Take first neighbour state that has same region id as state where wall was placed to start BFS
                SearchState s = null;
                ArrayList<SearchState> neighbourStates = map.getNeighbors(WALL_ROW, WALL_COL);
                for (SearchState neighbourState : neighbourStates) {
                    if (map.squares[map.getRow(neighbourState.id)][map.getCol(neighbourState.id)] == REGION_ID) {
                        s = neighbourState;
                        break;
                    }
                }

                if (s == null) {
                    throw new Exception("State without neighbours in same region!");
                }

                // Run BFS in sector and count states
                Queue<Integer> stateIds = new LinkedList<>();
                ExpandArray neighbors = new ExpandArray(10);

                stateIds.add(s.id);
                HashSet<Integer> visited = new HashSet<>();
                visited.add(s.id);
                while (!stateIds.isEmpty()) {
                    int id = stateIds.remove();

                    int row = map.getRow(id);
                    int col = map.getCol(id);

                    map.getNeighbors(row, col, neighbors);

                    for (int n = 0; n < neighbors.num(); n++) {
                        int nid = neighbors.get(n);
                        int nr = map.getRow(nid);
                        int nc = map.getCol(nid);
                        if (map.isInRange(nr, nc, END_ROW, END_COL) && !visited.contains(nid)) {
                            stateIds.add(nid);
                            visited.add(nid);
                        }
                    }
                }

                if (visited.size() != groupRecord.getNumStates() - 1) {
                    isPartition = true;
                }
            }

            // If we are placing a wall in the corner of a sector, we may have a pathblocker case
            if (!isPartition && isAtSectorEdge && !isBetweenVertical && !isBetweenHorizontal && !isVerticalWall && !isHorizontalWall) {
                boolean isNorthEdge = WALL_ROW == START_ROW;
                boolean isEastEdge = WALL_COL == END_COL;
                boolean isSouthEdge = WALL_ROW == END_ROW;
                boolean isWestEdge = WALL_COL == START_COL;

                boolean isTopLeftCorner = isNorthEdge && isWestEdge;
                boolean isTopRightCorner = isNorthEdge && isEastEdge;
                boolean isBottomRightCorner = isSouthEdge && isEastEdge;
                boolean isBottomLeftCorner = isSouthEdge && isWestEdge;

                int neighbourRegion = 0;
                int neighbourRegionRep = 0;

                // Corner cases (check if wall is in a sector corner and get the region id of the corner this corner is
                // touching, if applicable

                // TODO: Check my math here!
                // If the wall is in a corner and the state diagonal to it is not a wall, we have a corner blocker
                // This means the two regions that are currently neighbours shouldn't be anymore
                if (isTopLeftCorner && (NEIGHBOR_NW != GameMap.WALL_CHAR)) {
                    neighbourRegion = map.squares[WALL_ROW - 1][WALL_COL - 1];
                    neighbourRegionRep = map.getRegionRepFromRegionId(NEIGHBOR_NW);
                } else if (isTopRightCorner && (NEIGHBOR_NE != GameMap.WALL_CHAR)) {
                    neighbourRegion = map.squares[WALL_ROW - 1][WALL_COL + 1];
                    neighbourRegionRep = map.getRegionRepFromRegionId(NEIGHBOR_NE);
                } else if (isBottomRightCorner && (NEIGHBOR_SE != GameMap.WALL_CHAR)) {
                    neighbourRegion = map.squares[WALL_ROW + 1][WALL_COL + 1];
                    neighbourRegionRep = map.getRegionRepFromRegionId(NEIGHBOR_SE);
                } else if (isBottomLeftCorner && (NEIGHBOR_SW != GameMap.WALL_CHAR)) {
                    neighbourRegion = map.squares[WALL_ROW + 1][WALL_COL - 1];
                    neighbourRegionRep = map.getRegionRepFromRegionId(NEIGHBOR_SW);
                }

                if (neighbourRegion == REGION_ID) {
                    throw new Exception("NeighbourRegion id calculation went wrong!");
                }

                if (neighbourRegionRep == -1) {
                    throw new Exception("Region rep for region " + neighbourRegion + "does not exist!");
                }

                // Pathblocker corner case
                if (neighbourRegion != 0) {
                    logger.info("Addition: Pathblocker Corner Case (wall at " + wallLoc + ")");

                    // Get the neighbours of the region
                    HashSet<Integer> neighbours = groupRecord.getNeighborIds();
                    // Update region’s neighbourhood in groups map
                    neighbours.remove(neighbourRegion);

                    // Get the neighbours of its soon-to-be ex-neighbor
                    GroupRecord neighborRecord = groups.get(neighbourRegion);
                    HashSet<Integer> neighboursOfEx = neighborRecord.getNeighborIds();
                    // Update old neighbour’s neighbourhood in groups map
                    neighboursOfEx.remove(REGION_ID);

                    dbBW.recomputeCornerBlocker(REGION_ID, neighbourRegion);

                    // If region rep in corner:
                    int newRegionRep = map.recomputeCentroid(REGION_ID, groupRecord, START_ROW, END_ROW, START_COL, END_COL);

                    if (REGION_REP != newRegionRep) {
                        logger.info("Addition: Moved region rep from " + groupRecord.groupRepId + " to " + newRegionRep);
                        dbBW.recomputeBasePaths(REGION_ID, problem, groups);
                    }
                    return;
                }

                // TODO: Pathblocker edge case
            }

            // Region Partition case
            if (isPartition) {
                logger.info("Addition: Region Partition Case (wall at " + wallLoc + ")");
                // Get neighbours
                ArrayList<Integer> neighborIds = new ArrayList<>(groupRecord.getNeighborIds());

                // Reset region (necessary in order for me to be able to reuse the regionId)
                for (int row = START_ROW; row < END_ROW; row++) {
                    for (int col = START_COL; col < END_COL; col++) {
                        if (!map.isWall(row, col) && map.squares[row][col] == REGION_ID) {
                            map.squares[row][col] = GameMap.EMPTY_CHAR; // 32
                        }
                    }
                }

                // Adding the regionId to the freeSpace array in the database
                dbBW.pushFreeSpace(REGION_ID);

                // Perform abstraction (go over sector and recompute regions), this updates free space
                int numRegionsInSector = map.sectorReAbstractWithFreeSpace(START_ROW, START_COL, END_ROW, END_COL, REGION_ID, map, dbBW);

                // Tombstone group record in groups map (recreate it later)
                map.addGroup(REGION_ID, null);

                int count = 0;
                GroupRecord[] newRecs = new GroupRecord[numRegionsInSector];

                // Re-create groups
                for (int row = START_ROW; row < END_ROW; row++) {
                    for (int col = START_COL; col < END_COL; col++) {
                        int groupId = map.squares[row][col];
                        if (groupId != GameMap.EMPTY_CHAR && groupId != GameMap.WALL_CHAR) {
                            // See if group already exists
                            GroupRecord rec = groups.get(groupId);
                            if (rec == null) {    // New group
                                GroupRecord newRec = new GroupRecord();
                                newRec.groupId = groupId;
                                newRec.groupRepId = map.getId(row, col);
                                newRec.setNumStates(1);
                                map.addGroup(groupId, newRec);
                                newRecs[count++] = newRec;
                            } else {    // Update group
                                rec.incrementNumStates();
                            }
                        }
                    }
                }

                // Recompute region reps for newly added regions
                // a newRec should never be null, if it is, something went wrong with the group generation in sectorReAbstractWithFreeSpace
                for (GroupRecord newRec : newRecs) {
                    map.recomputeCentroid(REGION_ID, newRec, START_ROW, END_ROW, START_COL, END_COL);
                    neighborIds.add(newRec.groupId);
                }

                // Recompute neighbourhood
                map.recomputeNeighbors(START_ROW, START_COL, END_ROW, END_COL, neighborIds);

                // Database changes
                dbBW.recomputeBasePathsAfterPartition(problem, groups, neighborIds); // 60ms
                return;
            }

            groupRecord.decrementNumStates();

            // Compute newRegionRep to detect whether a shift has happened
            int newRegionRep = map.recomputeCentroid(REGION_ID, groupRecord, START_ROW, END_ROW, START_COL, END_COL);

            // Wall That Moves Region Representative case
            if (newRegionRep != REGION_REP) {
                logger.info("Addition: Wall That Moves Region Representative Case (wall at " + wallLoc + ")");
                logger.debug("New region rep: " + newRegionRep);
            } else { // Wall That Changes Shortest Path
                logger.info("Addition: Wall That Changes Shortest Path Case (wall at " + wallLoc + ")");
            }

            // Database changes
            dbBW.recomputeBasePaths(REGION_ID, problem, groups);
        }
    }

    /**
     * @param wallLoc   state id where wall will be removed
     * @param dbaStarBW DBAStar object returned from computeDBAStarDatabaseUsingSubgoalDynamicDB3
     */
    public void recomputeWallRemoval(int wallLoc, DBAStar dbaStarBW) throws Exception {
        // Extract map, problem, and database from dbaStarBW
        GameMap map = dbaStarBW.getMap();
        MapSearchProblem problem = (MapSearchProblem) dbaStarBW.getProblem();
        SubgoalDynamicDB3 dbBW = (SubgoalDynamicDB3) dbaStarBW.getDatabase();

        final int WALL_ROW = map.getRow(wallLoc);
        final int WALL_COL = map.getCol(wallLoc);

        // Check whether there is a wall at the location the wall should be removed
        final boolean PRIOR_WALL = map.isWall(wallLoc) && problem.getMap().isWall(wallLoc);

        // Remove wall from map and from map inside problem, set the square to empty (=32) for now
        map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = GameMap.EMPTY_CHAR;
        problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = GameMap.EMPTY_CHAR;

        // Check whether the wall removal worked as intended
        if (PRIOR_WALL && !map.isWall(wallLoc) && !problem.getMap().isWall(wallLoc)) {
            logger.info("Wall at " + wallLoc + " removed successfully!");
        } else {
            logger.error("PRIOR_WALL: " + PRIOR_WALL + ", !map.isWall(wallLoc): " + !map.isWall(wallLoc) + ", !problem.getMap().isWall(wallLoc): " + !problem.getMap().isWall(wallLoc));
            throw new Exception("Wall removal failed! There is no wall to remove at " + wallLoc);
        }

        // Get eight neighbours of state where wall was removed
        int[] neighbourStates = map.getNeighborIds(WALL_ROW, WALL_COL);

        // Check whether all eight neighbour states are walls
        boolean isSurroundedByWalls = true;
        for (int neighbourState : neighbourStates) {
            if (!map.isWall(neighbourState)) {
                isSurroundedByWalls = false;
                break;
            }
        }

        // If all eight neighbour states of where the wall is to be removed are walls, we will have a new, solitary region
        if (isSurroundedByWalls) {
            logger.info("Removal: New Solitary Region Case (wall at " + wallLoc + ")");
            // Get new regionId using freeSpace
            int regionId = dbBW.popFreeSpace();

            TreeMap<Integer, GroupRecord> groups = map.getGroups();

            // There should not be a group record with the new region id
            GroupRecord rec = groups.get(regionId);
            if (rec != null) {
                throw new Exception("Error! Record already exists!");
            }

            // Assign the new regionId inside the squares arrays
            map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = regionId;
            problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = regionId;

            // Create a new group record for the new region
            GroupRecord newRec = new GroupRecord();
            newRec.groupId = regionId;
            // Group rep id does not need to be computed using compute centroids logic since it must be where the wall was removed
            newRec.groupRepId = map.getId(WALL_ROW, WALL_COL);
            newRec.setNumStates(1);
            newRec.setNeighborIds(new HashSet<>());

            // Add the new group record to the groups map
            map.addGroup(regionId, newRec);

            // Database changes
            dbBW.recomputeBasePathsIfSolitary(regionId);
        } else {
            // Check what sector the non-wall neighbourStates are in
            final int SECTOR_ID = map.findSectorId(WALL_ROW, WALL_COL);
            logger.debug("Sector: " + SECTOR_ID);

            // Store region ids of eight neighbour states
            HashSet<Integer> neighbouringRegions = new HashSet<>();
            // Store region ids of neighbour states that are in the same sector as the wall being removed
            TreeSet<Integer> neighbouringRegionsInSameSector = new TreeSet<>(Comparator.reverseOrder());

            int neighbourRegionId = -1;

            for (int neighbourState : neighbourStates) {
                if (!map.isWall(neighbourState)) {
                    // Get sector id of current neighbour state of wall (if it is not a wall)
                    int neighbourSector = map.findSectorId(neighbourState);
                    // Get region id of current neighbour state of wall (if it is not a wall)
                    int neighbourRegion = map.getRegionFromState(neighbourState);
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

            TreeMap<Integer, GroupRecord> groups = map.getGroups();

            // If the new region is in a different sector than any of its neighbours, we have a new, connected region
            if (neighbouringRegionsInSameSector.isEmpty()) {
                logger.info("Removal: New Connected Region Case (wall at " + wallLoc + ")");
                // Get new regionId using freeSpace
                int regionId = dbBW.popFreeSpace();

                // There should not be a group record with the new region id
                GroupRecord rec = groups.get(regionId);
                if (rec != null) {
                    logger.error("Existing record at " + regionId + ": " + rec);
                    throw new Exception("Error! Record already exists!");
                }

                // Assign the new regionId inside the squares arrays
                map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = regionId;
                problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = regionId;

                // Create a new group record for the new region
                GroupRecord newRec = new GroupRecord();
                newRec.groupId = regionId;
                // Group rep id does not need to be computed using compute centroids logic since it must be where the wall was removed
                newRec.groupRepId = map.getId(WALL_ROW, WALL_COL);
                newRec.setNumStates(1);

                // Update region’s neighbourhood in groups map & update neighbourhood of all its neighbours in groups map
                for (Integer neighbouringRegion : neighbouringRegions) {
                    groups.get(neighbouringRegion).getNeighborIds().add(regionId);
                }
                newRec.setNeighborIds(neighbouringRegions);

                // Add the new group record to the groups map
                map.addGroup(regionId, newRec);

                // Database changes
                dbBW.recomputeBasePathsIfConnected(regionId, problem, groups, neighbouringRegions);
                return;
            }

            // Start of sector
            final int START_ROW = map.findStartRowOfSector(SECTOR_ID);
            final int START_COL = map.findStartColOfSector(SECTOR_ID);
            // End of sector
            final int END_ROW = map.findEndRowOfSector(START_ROW);
            final int END_COL = map.findEndColOfSector(START_COL);

            // Since we have neither a new, solitary region, nor a new, connected region, our removed wall must be part
            // of an existing region

            if (neighbourRegionId == -1) {
                throw new Exception("neighbourRegionId has not been assigned!");
            }

            // Get region id from neighbours in same sector
            // In the merge case, which one is assigned here is random, but will be overwritten anyway
            // In the unblocker case, there is only one choice
            int regionId = neighbourRegionId;

            // Assign the new regionId inside the squares arrays
            map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = regionId;
            problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = regionId;

            // Get region rep
            int regionRepId = map.getRegionRepFromRegionId(regionId);

            GroupRecord groupRecord = groups.get(regionId);

            if (groupRecord == null) {
                throw new Exception("No record found for " + regionId + "!");
            }

            // If the neighbours stored in the group record differ from those stored in the neighbouringRegions,
            // we must have an unblocker case, or a region merge case
            HashSet<Integer> neighboursFromGroupRec = groupRecord.getNeighborIds();
            // Removing the set of neighbours of the region from the neighbour states of the wall. If there are any elements
            // left, the wall must have at least one neighbour state that is not currently considered a neighbour
            logger.debug("neighbouringRegions before: " + neighbouringRegions);
            logger.debug("neighboursFromGroupRec before: " + neighboursFromGroupRec);
            neighbouringRegions.removeAll(neighboursFromGroupRec);
            neighbouringRegions.remove(regionId);
            if (!neighbouringRegions.isEmpty()) {
                // neighbouringRegions contains all regions the removed wall was touching
                // groupRecord.getNeighborIds() contains all neighbours of the region the wall is in

                logger.debug("neighbouringRegions after: " + neighbouringRegions);

                // The wall does not have multiple neighbours in different regions in the same sector
                // and only has one neighbour out of the sector
//                if (neighbouringRegionsInSameSector.size() == 1 && neighbouringRegions.size() == 1) {
//                    // Unblocker case
//                    logger.info("Removal: Path Unblocker Case (wall at " + wallLoc + ")");
//
//                    Iterator<Integer> iterator = neighbouringRegions.iterator();
//                    int neighbourRegion = iterator.next();
//
//                    // Get the neighbours of the region
//                    HashSet<Integer> neighbours = groupRecord.getNeighborIds();
//                    // Update region’s neighbourhood in groups map
//                    neighbours.remove(neighbourRegion);
//
//                    // Get the neighbours of its soon-to-be ex-neighbor
//                    GroupRecord neighborRecord = groups.get(neighbourRegion);
//                    HashSet<Integer> neighboursOfEx = neighborRecord.getNeighborIds();
//                    // Update old neighbour’s neighbourhood in groups map
//                    neighboursOfEx.remove(regionId);
//
//                    // Database changes
//                    dbBW.recomputeUnblocker(regionId, neighbourRegion, problem, groups);
//
//                    // If region rep in corner:
//                    int newRegionRep = map.recomputeCentroid(regionId, groupRecord, START_ROW, END_ROW, START_COL, END_COL);
//
//                    if (groupRecord.groupRepId != newRegionRep) {
//                        logger.info("Removal: Moved region rep from " + groupRecord.groupRepId + " to " + newRegionRep);
//                        dbBW.recomputeBasePaths(regionId, problem, groups);
//                    }
//                } else {
                    // If our wall touches more than two regions that are in the same sector, we have a region merge case
                    logger.info("Removal: Region Merge Case (wall at " + wallLoc + ")");

                    // Iterate over old regions inside squares array and ‘erase’ them (assign ‘32’ instead of the old region ids)
                    for (int row = START_ROW; row < END_ROW; row++) {
                        for (int col = START_COL; col < END_COL; col++) {
                            if (!map.isWall(row, col) && neighbouringRegionsInSameSector.contains(map.squares[row][col])) {
                                map.squares[row][col] = GameMap.EMPTY_CHAR; // 32
                            }
                        }
                    }

                    HashSet<Integer> neighborIdsSet = new HashSet<>();
                    logger.debug("neighbouringRegionsInSameSector: " + neighbouringRegionsInSameSector);
                    for (int neighbouringRegionInSameSector : neighbouringRegionsInSameSector) {
                        // Update freeSpace
                        dbBW.pushFreeSpace(neighbouringRegionInSameSector);
                        // Add to neighbourIds
                        if (groups.get(neighbouringRegionInSameSector) != null) {
                            neighborIdsSet.addAll(groups.get(neighbouringRegionInSameSector).getNeighborIds());
                        }
                        // Tombstone group record in groups map (recreate it later)
                        map.addGroup(neighbouringRegionInSameSector, null);
                    }

                    // Remove regions that will merge
                    // TODO: Is this necessary?
                    neighborIdsSet.addAll(neighbouringRegions);
                    neighborIdsSet.removeAll(neighbouringRegionsInSameSector);

                    ArrayList<Integer> neighborIds = new ArrayList<>(neighborIdsSet);

                    // Perform abstraction (go over sector and recompute regions), this updates free space
                    // TODO: Should I even pass regionId here?
                    int numRegionsInSector = map.sectorReAbstractWithFreeSpace(START_ROW, START_COL, END_ROW, END_COL, regionId, map, dbBW);

                    int count = 0;
                    GroupRecord[] newRecs = new GroupRecord[numRegionsInSector];

                    // Re-create groups
                    for (int row = START_ROW; row < END_ROW; row++) {
                        for (int col = START_COL; col < END_COL; col++) {
                            int groupId = map.squares[row][col];
                            if (groupId != GameMap.EMPTY_CHAR && groupId != GameMap.WALL_CHAR) {
                                // See if group already exists
                                GroupRecord rec = groups.get(groupId);
                                if (rec == null) {    // New group
                                    GroupRecord newRec = new GroupRecord();
                                    newRec.groupId = groupId;
                                    newRec.groupRepId = map.getId(row, col);
                                    newRec.setNumStates(1);
                                    newRec.setNeighborIds(new HashSet<>());
                                    map.addGroup(groupId, newRec);
                                    newRecs[count++] = newRec;
                                } else {    // Update group
                                    rec.incrementNumStates();
                                }
                            }
                        }
                    }

                    // Recompute region reps for newly added regions
                    // a newRec should never be null, if it is, something went wrong with the group generation in sectorReAbstractWithFreeSpace
                    for (GroupRecord newRec : newRecs) {
                        map.recomputeCentroid(newRec.groupId, newRec, START_ROW, END_ROW, START_COL, END_COL);
                        neighborIds.add(newRec.groupId);
                    }

                    logger.info("Neighbour ids for neighbourhood recomputation: " + neighborIds);

                    // Recompute neighbourhood
                    map.recomputeNeighbors(START_ROW, START_COL, END_ROW, END_COL, neighborIds);

                    // Database changes
                    dbBW.recomputeBasePathsAfterPartition(problem, groups, neighborIds);
//                }
                return;
            }

            groupRecord.incrementNumStates();

            // Compute newRegionRep to detect whether a shift has happened
            int newRegionRep = map.recomputeCentroid(regionId, groupRecord, START_ROW, END_ROW, START_COL, END_COL);

            // Wall That Moves Region Representative case
            if (newRegionRep != regionRepId) {
                logger.info("Removal: Wall That Moves Region Representative Case (wall at " + wallLoc + ")");
            } else { // Wall That Changes Shortest Path
                logger.debug("New region rep: " + newRegionRep);
                logger.info("Removal: Wall That Changes Shortest Path Case (wall at " + wallLoc + ")");
            }

            // Database changes
            dbBW.recomputeBasePaths(regionId, problem, groups);
        }
    }

    /**
     * @param wallLoc   state id where wall will be place
     * @param dbaStarBW DBAStar object returned from computeDBAStarDatabaseUsingSubgoalDynamicDB3
     */
    public void recomputeWallAdditionNoLogging(int wallLoc, DBAStar dbaStarBW) throws Exception {
        // Extract map, problem, and database from dbaStarBW
        GameMap map = dbaStarBW.getMap();
        MapSearchProblem problem = (MapSearchProblem) dbaStarBW.getProblem();
        SubgoalDynamicDB3 dbBW = (SubgoalDynamicDB3) dbaStarBW.getDatabase();

        final int WALL_ROW = map.getRow(wallLoc);
        final int WALL_COL = map.getCol(wallLoc);

        // Get id of region that wall was placed in
        final int REGION_ID = map.squares[WALL_ROW][WALL_COL];

        // Check whether there is already a wall at the location the wall should be placed
        final boolean PRIOR_WALL = map.isWall(wallLoc) && problem.getMap().isWall(wallLoc);

        // Add wall to map and to map inside problem
        map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = '*';
        problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = '*';

        // Check whether the wall addition worked as intended
        if (PRIOR_WALL || !map.isWall(wallLoc) || !problem.getMap().isWall(wallLoc)) {
            throw new Exception("Wall addition failed! There is a wall at " + wallLoc + " already");
        }

        // Get representative of region wall was placed in
        final int REGION_REP = map.getRegionRepFromRegionId(REGION_ID); // 8424

        // If the region rep is tombstoned
        if (REGION_REP == -1) {
            throw new Exception("Region rep for region " + REGION_ID + " does not exist!");
        }

        // Get groups containing information on all regions from map
        TreeMap<Integer, GroupRecord> groups = map.getGroups();

        // Get group record containing information on the region
        GroupRecord groupRecord = groups.get(REGION_ID);

        // Elimination case
        if (groupRecord.getNumStates() == 1) {
            // Get the neighbours of the region that will be eliminated
            HashSet<Integer> neighbours = groupRecord.getNeighborIds();

            for (Integer neighbour : neighbours) {
                // Get the region rep of the current neighbour
                int neighbourRep = map.getRegionRepFromRegionId(neighbour);
                // If the region rep is tombstoned
                if (neighbourRep == -1) {
                    throw new Exception("Region rep for region " + REGION_ID + "does not exist!");
                }
                // Remove the region as a neighbour of its neighbours
                groups.get(neighbour).getNeighborIds().remove(REGION_ID);
            }
            // Tombstone group record in groups map
            map.addGroup(REGION_ID, null);

            // Database changes
            dbBW.recomputeBasePathsAfterElimination(REGION_ID);
        } else {
            // Other cases
            final int SECTOR_ID = map.findSectorId(WALL_ROW, WALL_COL);
            logger.debug("Sector: " + SECTOR_ID);

            // Start of sector
            final int START_ROW = map.findStartRowOfSector(SECTOR_ID);
            final int START_COL = map.findStartColOfSector(SECTOR_ID);
            // End of sector
            final int END_ROW = map.findEndRowOfSector(START_ROW);
            final int END_COL = map.findEndColOfSector(START_COL);

            // Eight neighbours (states touching wall state)
            int NEIGHBOR_N = map.squares[WALL_ROW - 1][WALL_COL];

            // Need to check boundaries for bottom row of map (if not on map, treat square as wall)
            // TODO: Do I really need to check all of these?
            int NEIGHBOR_NE = map.isInBounds(WALL_ROW - 1, WALL_COL + 1) ? map.squares[WALL_ROW - 1][WALL_COL + 1] : GameMap.WALL_CHAR;
            int NEIGHBOR_E = map.isInBounds(WALL_ROW, WALL_COL + 1) ? map.squares[WALL_ROW][WALL_COL + 1] : GameMap.WALL_CHAR;
            int NEIGHBOR_SE = map.isInBounds(WALL_ROW + 1, WALL_COL + 1) ? map.squares[WALL_ROW + 1][WALL_COL + 1] : GameMap.WALL_CHAR;
            int NEIGHBOR_S = map.isInBounds(WALL_ROW + 1, WALL_COL) ? map.squares[WALL_ROW + 1][WALL_COL] : GameMap.WALL_CHAR;
            int NEIGHBOR_SW = map.isInBounds(WALL_ROW + 1, WALL_COL - 1) ? map.squares[WALL_ROW + 1][WALL_COL - 1] : GameMap.WALL_CHAR;

            int NEIGHBOR_W = map.squares[WALL_ROW][WALL_COL - 1];
            int NEIGHBOR_NW = map.squares[WALL_ROW - 1][WALL_COL - 1];

            boolean isAtSectorEdge = WALL_ROW == START_ROW || WALL_ROW == END_ROW || WALL_COL == START_COL || WALL_COL == END_COL;

            // If wall is between two walls, or between a wall and another region, we may have partition
            boolean isVerticalWall = isContinuousWall(NEIGHBOR_S, NEIGHBOR_N);
            boolean isHorizontalWall = isContinuousWall(NEIGHBOR_W, NEIGHBOR_E);
            boolean isBetweenVertical = !isVerticalWall && isBetweenWallAndOtherRegion(NEIGHBOR_S, NEIGHBOR_N, REGION_ID);
            boolean isBetweenHorizontal = !isHorizontalWall && isBetweenWallAndOtherRegion(NEIGHBOR_W, NEIGHBOR_E, REGION_ID);

            // If the diagonal state is open, but the cardinal ones surrounding it are not, we may have partition
            boolean isOpenDiagonalNW = isOpenDiagonal(NEIGHBOR_W, NEIGHBOR_NW, NEIGHBOR_N);
            boolean isOpenDiagonalNE = isOpenDiagonal(NEIGHBOR_N, NEIGHBOR_NE, NEIGHBOR_E);
            boolean isOpenDiagonalSE = isOpenDiagonal(NEIGHBOR_E, NEIGHBOR_SE, NEIGHBOR_S);
            boolean isOpenDiagonalSW = isOpenDiagonal(NEIGHBOR_S, NEIGHBOR_SW, NEIGHBOR_W);

            boolean isPotentialPartition = isVerticalWall || isHorizontalWall || isBetweenVertical || isBetweenHorizontal || isOpenDiagonalNW || isOpenDiagonalNE || isOpenDiagonalSE || isOpenDiagonalSW;
            boolean isPartition = false;

            // Find first neighbour that is in same region as where the wall was placed
            // Must exist since this is not elimination, must touch by construction

            if (isPotentialPartition) {
                // Get neighbour states
                // Take first neighbour state that has same region id as state where wall was placed to start BFS
                SearchState s = null;
                ArrayList<SearchState> neighbourStates = map.getNeighbors(WALL_ROW, WALL_COL);
                for (SearchState neighbourState : neighbourStates) {
                    if (map.squares[map.getRow(neighbourState.id)][map.getCol(neighbourState.id)] == REGION_ID) {
                        s = neighbourState;
                        break;
                    }
                }

                if (s == null) {
                    throw new Exception("State without neighbours in same region!");
                }

                // Run BFS in sector and count states
                Queue<Integer> stateIds = new LinkedList<>();
                ExpandArray neighbors = new ExpandArray(10);

                stateIds.add(s.id);
                HashSet<Integer> visited = new HashSet<>();
                visited.add(s.id);
                while (!stateIds.isEmpty()) {
                    int id = stateIds.remove();

                    int row = map.getRow(id);
                    int col = map.getCol(id);

                    map.getNeighbors(row, col, neighbors);

                    for (int n = 0; n < neighbors.num(); n++) {
                        int nid = neighbors.get(n);
                        int nr = map.getRow(nid);
                        int nc = map.getCol(nid);
                        if (map.isInRange(nr, nc, END_ROW, END_COL) && !visited.contains(nid)) {
                            stateIds.add(nid);
                            visited.add(nid);
                        }
                    }
                }

                if (visited.size() != groupRecord.getNumStates() - 1) {
                    isPartition = true;
                }
            }

            // If we are placing a wall in the corner of a sector, we may have a pathblocker case
            if (!isPartition && isAtSectorEdge && !isBetweenVertical && !isBetweenHorizontal && !isVerticalWall && !isHorizontalWall) {
                boolean isNorthEdge = WALL_ROW == START_ROW;
                boolean isEastEdge = WALL_COL == END_COL;
                boolean isSouthEdge = WALL_ROW == END_ROW;
                boolean isWestEdge = WALL_COL == START_COL;

                boolean isTopLeftCorner = isNorthEdge && isWestEdge;
                boolean isTopRightCorner = isNorthEdge && isEastEdge;
                boolean isBottomRightCorner = isSouthEdge && isEastEdge;
                boolean isBottomLeftCorner = isSouthEdge && isWestEdge;

                int neighbourRegion = 0;
                int neighbourRegionRep = 0;

                // Corner cases (check if wall is in a sector corner and get the region id of the corner this corner is
                // touching, if applicable

                // TODO: Check my math here!
                // If the wall is in a corner and the state diagonal to it is not a wall, we have a corner blocker
                // This means the two regions that are currently neighbours shouldn't be anymore
                if (isTopLeftCorner && (NEIGHBOR_NW != GameMap.WALL_CHAR)) {
                    neighbourRegion = map.squares[WALL_ROW - 1][WALL_COL - 1];
                    neighbourRegionRep = map.getRegionRepFromRegionId(NEIGHBOR_NW);
                } else if (isTopRightCorner && (NEIGHBOR_NE != GameMap.WALL_CHAR)) {
                    neighbourRegion = map.squares[WALL_ROW - 1][WALL_COL + 1];
                    neighbourRegionRep = map.getRegionRepFromRegionId(NEIGHBOR_NE);
                } else if (isBottomRightCorner && (NEIGHBOR_SE != GameMap.WALL_CHAR)) {
                    neighbourRegion = map.squares[WALL_ROW + 1][WALL_COL + 1];
                    neighbourRegionRep = map.getRegionRepFromRegionId(NEIGHBOR_SE);
                } else if (isBottomLeftCorner && (NEIGHBOR_SW != GameMap.WALL_CHAR)) {
                    neighbourRegion = map.squares[WALL_ROW + 1][WALL_COL - 1];
                    neighbourRegionRep = map.getRegionRepFromRegionId(NEIGHBOR_SW);
                }

                if (neighbourRegion == REGION_ID) {
                    throw new Exception("NeighbourRegion id calculation went wrong!");
                }

                if (neighbourRegionRep == -1) {
                    throw new Exception("Region rep for region " + neighbourRegion + "does not exist!");
                }

                // Pathblocker corner case
                if (neighbourRegion != 0) {
                    // Get the neighbours of the region
                    HashSet<Integer> neighbours = groupRecord.getNeighborIds();
                    // Update region’s neighbourhood in groups map
                    neighbours.remove(neighbourRegion);

                    // Get the neighbours of its soon-to-be ex-neighbor
                    GroupRecord neighborRecord = groups.get(neighbourRegion);
                    HashSet<Integer> neighboursOfEx = neighborRecord.getNeighborIds();
                    // Update old neighbour’s neighbourhood in groups map
                    neighboursOfEx.remove(REGION_ID);

                    dbBW.recomputeCornerBlocker(REGION_ID, neighbourRegion);

                    // If region rep in corner:
                    int newRegionRep = map.recomputeCentroid(REGION_ID, groupRecord, START_ROW, END_ROW, START_COL, END_COL);

                    if (REGION_REP != newRegionRep) {
                        dbBW.recomputeBasePaths(REGION_ID, problem, groups); // 1016
                    }

                    return;
                }

                // TODO: Pathblocker edge case
            }

            // Region Partition case

            if (isPartition) {
                // Get neighbours
                ArrayList<Integer> neighborIds = new ArrayList<>(groupRecord.getNeighborIds());

                // Reset region (necessary in order for me to be able to reuse the regionId)
                for (int row = START_ROW; row < END_ROW; row++) {
                    for (int col = START_COL; col < END_COL; col++) {
                        if (!map.isWall(row, col) && map.squares[row][col] == REGION_ID) {
                            map.squares[row][col] = GameMap.EMPTY_CHAR; // 32
                        }
                    }
                }

                // Adding the regionId to the freeSpace array in the database
                dbBW.pushFreeSpace(REGION_ID); // 1117

                // Perform abstraction (go over sector and recompute regions), this updates free space
                int numRegionsInSector = map.sectorReAbstractWithFreeSpace(START_ROW, START_COL, END_ROW, END_COL, REGION_ID, map, dbBW); // 6324

                // Tombstone group record in groups map (recreate it later)
                map.addGroup(REGION_ID, null); // 16158

                int count = 0;
                GroupRecord[] newRecs = new GroupRecord[numRegionsInSector];

                // Re-create groups
                for (int row = START_ROW; row < END_ROW; row++) {
                    for (int col = START_COL; col < END_COL; col++) {
                        int groupId = map.squares[row][col];
                        if (groupId != GameMap.EMPTY_CHAR && groupId != GameMap.WALL_CHAR) {
                            // See if group already exists
                            GroupRecord rec = groups.get(groupId);
                            if (rec == null) {    // New group
                                GroupRecord newRec = new GroupRecord();
                                newRec.groupId = groupId;
                                newRec.groupRepId = -1;
                                newRec.setNumStates(1);
                                newRec.setNeighborIds(new HashSet<>());
                                map.addGroup(groupId, newRec); // 13656
                                newRecs[count++] = newRec;
                            } else if (rec.groupRepId == -1){    // Update group
                                rec.incrementNumStates();
                            }
                        }
                    }
                }

                // Recompute region reps for newly added regions
                // a newRec should never be null, if it is, something went wrong with the group generation in sectorReAbstractWithFreeSpace
                for (GroupRecord newRec : newRecs) {
                    map.recomputeCentroid(REGION_ID, newRec, START_ROW, END_ROW, START_COL, END_COL);
                    neighborIds.add(newRec.groupId);
                }

                // Recompute neighbourhood
                map.recomputeNeighbors(REGION_ID, START_ROW, START_COL, END_ROW, END_COL, neighborIds); // 7160

                // Database changes
                dbBW.recomputeBasePathsAfterPartition(problem, groups, neighborIds); // 262039
                return;
            }

            groupRecord.decrementNumStates();

            // Compute newRegionRep (it may or may not be the same as before)
            map.recomputeCentroid(REGION_ID, groupRecord, START_ROW, END_ROW, START_COL, END_COL); // 15504

            // Database changes
            dbBW.recomputeBasePaths(REGION_ID, problem, groups); // 2085714
        }
    }

    /**
     * @param wallLoc   state id where wall will be removed
     * @param dbaStarBW DBAStar object returned from computeDBAStarDatabaseUsingSubgoalDynamicDB3
     */
    public void recomputeWallRemovalNoLogging(int wallLoc, DBAStar dbaStarBW) throws Exception {
        // Extract map, problem, and database from dbaStarBW
        GameMap map = dbaStarBW.getMap();
        MapSearchProblem problem = (MapSearchProblem) dbaStarBW.getProblem();
        SubgoalDynamicDB3 dbBW = (SubgoalDynamicDB3) dbaStarBW.getDatabase();

        final int WALL_ROW = map.getRow(wallLoc);
        final int WALL_COL = map.getCol(wallLoc);

        // Check whether there is a wall at the location the wall should be removed
        final boolean PRIOR_WALL = map.isWall(wallLoc) && problem.getMap().isWall(wallLoc);

        // Remove wall from map and from map inside problem, set the square to empty (=32) for now
        map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = GameMap.EMPTY_CHAR;
        problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = GameMap.EMPTY_CHAR;

        // Check whether the wall removal worked as intended
        if (!PRIOR_WALL || map.isWall(wallLoc) || problem.getMap().isWall(wallLoc)) {
            throw new Exception("Wall removal failed! There is no wall to remove at " + wallLoc);
        }

        // Get eight neighbours of state where wall was removed
        int[] neighbourStates = map.getNeighborIds(WALL_ROW, WALL_COL);

        // Check whether all eight neighbour states are walls
        boolean isSurroundedByWalls = true;
        for (int neighbourState : neighbourStates) {
            if (!map.isWall(neighbourState)) {
                isSurroundedByWalls = false;
                break;
            }
        }

        // If all eight neighbour states of where the wall is to be removed are walls, we will have a new, solitary region
        if (isSurroundedByWalls) {
            // Get new regionId using freeSpace
            int regionId = dbBW.popFreeSpace();

            TreeMap<Integer, GroupRecord> groups = map.getGroups();

            // There should not be a group record with the new region id
            GroupRecord rec = groups.get(regionId);
            if (rec != null) {
                throw new Exception("Error! Record already exists!");
            }

            // Assign the new regionId inside the squares arrays
            map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = regionId;
            problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = regionId;

            // Create a new group record for the new region
            GroupRecord newRec = new GroupRecord();
            newRec.groupId = regionId;
            // Group rep id does not need to be computed using compute centroids logic since it must be where the wall was removed
            newRec.groupRepId = map.getId(WALL_ROW, WALL_COL);
            newRec.setNumStates(1);
            newRec.setNeighborIds(new HashSet<>());
            // Add the new group record to the groups map
            map.addGroup(regionId, newRec);

            // Database changes
            dbBW.recomputeBasePathsIfSolitary(regionId);
        } else {
            // Check what sector the non-wall neighbourStates are in
            final int SECTOR_ID = map.findSectorId(WALL_ROW, WALL_COL);
            logger.debug("Sector: " + SECTOR_ID);

            // Store region ids of eight neighbour states
            HashSet<Integer> neighbouringRegions = new HashSet<>();
            // Store region ids of neighbour states that are in the same sector as the wall being removed
            TreeSet<Integer> neighbouringRegionsInSameSector = new TreeSet<>(Comparator.reverseOrder());

            int neighbourRegionId = -1;

            for (int neighbourState : neighbourStates) {
                if (!map.isWall(neighbourState)) {
                    // Get sector id of current neighbour state of wall (if it is not a wall)
                    int neighbourSector = map.findSectorId(neighbourState);
                    // Get region id of current neighbour state of wall (if it is not a wall)
                    int neighbourRegion = map.getRegionFromState(neighbourState);
                    // Store neighbour regions in set
                    neighbouringRegions.add(neighbourRegion); // 10782
                    // If the neighbour state is in the same sector as the wall
                    if (neighbourSector == SECTOR_ID) {
                        // If there is multiple regions with the same sector id touching the wall being removed, we would have a merge case
                        neighbouringRegionsInSameSector.add(neighbourRegion);
                        neighbourRegionId = neighbourRegion;
                    }
                }
            }

            TreeMap<Integer, GroupRecord> groups = map.getGroups();

            // If the new region is in a different sector than any of its neighbours, we have a new, connected region
            if (neighbouringRegionsInSameSector.isEmpty()) {
                // Get new regionId using freeSpace
                int regionId = dbBW.popFreeSpace();

                // There should not be a group record with the new region id
                GroupRecord rec = groups.get(regionId);
                if (rec != null) {
                    throw new Exception("Error! Record already exists!");
                }

                // Assign the new regionId inside the squares arrays
                map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = regionId;
                problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = regionId;

                // Create a new group record for the new region
                GroupRecord newRec = new GroupRecord();
                newRec.groupId = regionId;
                // Group rep id does not need to be computed using compute centroids logic since it must be where the wall was removed
                newRec.groupRepId = map.getId(WALL_ROW, WALL_COL);
                newRec.setNumStates(1);

                // Update region’s neighbourhood in groups map & update neighbourhood of all its neighbours in groups map
                for (Integer neighbouringRegion : neighbouringRegions) {
                    groups.get(neighbouringRegion).getNeighborIds().add(regionId);
                }
                newRec.setNeighborIds(neighbouringRegions);

                // Add the new group record to the groups map
                map.addGroup(regionId, newRec);

                // Database changes
                dbBW.recomputeBasePathsIfConnected(regionId, problem, groups, neighbouringRegions); // 1034
                return;
            }

            // Start of sector
            final int START_ROW = map.findStartRowOfSector(SECTOR_ID);
            final int START_COL = map.findStartColOfSector(SECTOR_ID);
            // End of sector
            final int END_ROW = map.findEndRowOfSector(START_ROW);
            final int END_COL = map.findEndColOfSector(START_COL);

            // Since we have neither a new, solitary region, nor a new, connected region, our removed wall must be part
            // of an existing region

            if (neighbourRegionId == -1) {
                throw new Exception("neighbourRegionId has not been assigned!");
            }

            // Get region id from neighbours in same sector
            // In the merge case, which one is assigned here is random, but will be overwritten anyway
            // In the unblocker case, there is only one choice
            int regionId = neighbourRegionId;

            // Assign the new regionId inside the squares arrays
            map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = regionId;
            problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = regionId;

            GroupRecord groupRecord = groups.get(regionId);

            if (groupRecord == null) {
                throw new Exception("No record found for " + regionId + "!");
            }

            // If the neighbours stored in the group record differ from those stored in the neighbouringRegions,
            // we must have an unblocker case, or a region merge case
            HashSet<Integer> neighboursFromGroupRec = groupRecord.getNeighborIds();
            // Removing the set of neighbours of the region from the neighbour states of the wall. If there are any elements
            // left, the wall must have at least one neighbour state that is not currently considered a neighbour
            neighbouringRegions.removeAll(neighboursFromGroupRec); // 3456
            neighbouringRegions.remove(regionId);
            if (!neighbouringRegions.isEmpty()) {
                // neighbouringRegions contains all regions the removed wall was touching
                // groupRecord.getNeighborIds() contains all neighbours of the region the wall is in

                // The wall does not have multiple neighbours in different regions in the same sector
                // and only has one neighbour out of the sector
//                if (neighbouringRegionsInSameSector.size() == 1 && neighbouringRegions.size() == 1) {
//                    // Unblocker case
//                    Iterator<Integer> iterator = neighbouringRegions.iterator();
//                    int neighbourRegion = iterator.next();
//
//                    // Get the neighbours of the region
//                    HashSet<Integer> neighbours = groupRecord.getNeighborIds();
//                    // Update region’s neighbourhood in groups map
//                    neighbours.remove(neighbourRegion);
//
//                    // Get the neighbours of its soon-to-be ex-neighbor
//                    GroupRecord neighborRecord = groups.get(neighbourRegion);
//                    HashSet<Integer> neighboursOfEx = neighborRecord.getNeighborIds();
//                    // Update old neighbour’s neighbourhood in groups map
//                    neighboursOfEx.remove(regionId);
//
//                    // Database changes
//                    dbBW.recomputeUnblocker(regionId, neighbourRegion, problem, groups);
//
//                    // If region rep in corner:
//                    int newRegionRep = map.recomputeCentroid(regionId, groupRecord, START_ROW, END_ROW, START_COL, END_COL);
//
//                    if (groupRecord.groupRepId != newRegionRep) {
//                        dbBW.recomputeBasePaths(regionId, problem, groups);
//                    }
//                } else {
                    // If our wall touches more than two regions that are in the same sector, we have a region merge case

                    // Iterate over old regions inside squares array and ‘erase’ them (assign ‘32’ instead of the old region ids)
                    for (int row = START_ROW; row < END_ROW; row++) {
                        for (int col = START_COL; col < END_COL; col++) {
                            if (!map.isWall(row, col) && neighbouringRegionsInSameSector.contains(map.squares[row][col])) {
                                map.squares[row][col] = GameMap.EMPTY_CHAR; // 32
                            }
                        }
                    }

                    HashSet<Integer> neighborIdsSet = new HashSet<>();
                    for (int neighbouringRegionInSameSector : neighbouringRegionsInSameSector) {
                        // Update freeSpace
                        dbBW.pushFreeSpace(neighbouringRegionInSameSector);
                        // Add to neighbourIds
                        if (groups.get(neighbouringRegionInSameSector) != null) {
                            neighborIdsSet.addAll(groups.get(neighbouringRegionInSameSector).getNeighborIds());
                        }
                        // Tombstone group record in groups map (recreate it later)
                        map.addGroup(neighbouringRegionInSameSector, null); // 6186
                    }

                    // Remove regions that will merge
                    // TODO: Is this necessary?
                    neighborIdsSet.addAll(neighbouringRegions);
                    neighborIdsSet.removeAll(neighbouringRegionsInSameSector);

                    ArrayList<Integer> neighborIds = new ArrayList<>(neighborIdsSet);

                    // Perform abstraction (go over sector and recompute regions), this updates free space
                    // TODO: Should I even pass regionId here?
                    int numRegionsInSector = map.sectorReAbstractWithFreeSpace(START_ROW, START_COL, END_ROW, END_COL, regionId, map, dbBW); // 2422

                    int count = 0;
                    GroupRecord[] newRecs = new GroupRecord[numRegionsInSector];

                    // Re-create groups
                    for (int row = START_ROW; row < END_ROW; row++) {
                        for (int col = START_COL; col < END_COL; col++) {
                            int groupId = map.squares[row][col];
                            if (groupId != GameMap.EMPTY_CHAR && groupId != GameMap.WALL_CHAR) {
                                // See if group already exists
                                GroupRecord rec = groups.get(groupId);
                                if (rec == null) {    // New group
                                    GroupRecord newRec = new GroupRecord();
                                    newRec.groupId = groupId;
                                    newRec.groupRepId = map.getId(row, col);
                                    newRec.setNumStates(1);
                                    newRec.setNeighborIds(new HashSet<>());
                                    map.addGroup(groupId, newRec); // 4041
                                    newRecs[count++] = newRec;
                                } else if (neighbouringRegionsInSameSector.contains(groupId)) {    // Update group
                                    rec.incrementNumStates();
                                }
                            }
                        }
                    }

                    // Recompute region reps for newly added regions
                    // a newRec should never be null, if it is, something went wrong with the group generation in sectorReAbstractWithFreeSpace
                    for (GroupRecord newRec : newRecs) {
                        map.recomputeCentroid(newRec.groupId, newRec, START_ROW, END_ROW, START_COL, END_COL);
                        neighborIds.add(newRec.groupId);
                    }

                    // Recompute neighbourhood
                    map.recomputeNeighbors(neighbouringRegionsInSameSector, START_ROW, START_COL, END_ROW, END_COL, neighborIds); // 2283

                    // Database changes
                    dbBW.recomputeBasePathsAfterPartition(problem, groups, neighborIds); // 103651
//                }
                return;
            }

            groupRecord.incrementNumStates();

            // Compute newRegionRep (it may or may not be the same as before)
            map.recomputeCentroid(regionId, groupRecord, START_ROW, END_ROW, START_COL, END_COL); // 14090

            // Database changes
            dbBW.recomputeBasePaths(regionId, problem, groups); // 1990480
        }
    }


    public void recomputeWallRemovalNoChecks(int wallLoc, DBAStar dbaStarBW) throws Exception {
        GameMap map = dbaStarBW.getMap();
        MapSearchProblem problem = (MapSearchProblem) dbaStarBW.getProblem();

        map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = GameMap.EMPTY_CHAR;
        problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = GameMap.EMPTY_CHAR;

        SubgoalDynamicDB3 dbBW = (SubgoalDynamicDB3) dbaStarBW.getDatabase();

        final int WALL_ROW = map.getRow(wallLoc);
        final int WALL_COL = map.getCol(wallLoc);

        final int SECTOR_ID = map.findSectorId(WALL_ROW, WALL_COL);

        // Start of sector
        final int START_ROW = map.findStartRowOfSector(SECTOR_ID);
        final int START_COL = map.findStartColOfSector(SECTOR_ID);
        // End of sector
        final int END_ROW = map.findEndRowOfSector(START_ROW);
        final int END_COL = map.findEndColOfSector(START_COL);

        // Get groups containing information on all regions from map
        TreeMap<Integer, GroupRecord> groups = map.getGroups();

        // Store region ids of eight neighbour states
        HashSet<Integer> neighbouringRegions = new HashSet<>();
        // Store region ids of neighbour states that are in the same sector as the wall being removed
        TreeSet<Integer> neighbouringRegionsInSameSector = new TreeSet<>(Comparator.reverseOrder());

        // Get eight neighbours of state where wall was removed
        int[] neighbourStates = map.getNeighborIds(WALL_ROW, WALL_COL);

        for (int neighbourState : neighbourStates) {
            if (!map.isWall(neighbourState)) {
                // Get sector id of current neighbour state of wall (if it is not a wall)
                int neighbourSector = map.findSectorId(neighbourState);
                // Get region id of current neighbour state of wall (if it is not a wall)
                int neighbourRegion = map.getRegionFromState(neighbourState);
                // Store neighbour regions in set
                neighbouringRegions.add(neighbourRegion);
                // If the neighbour state is in the same sector as the wall
                if (neighbourSector == SECTOR_ID) {
                    // If there is multiple regions with the same sector id touching the wall being removed, we would have a merge case
                    neighbouringRegionsInSameSector.add(neighbourRegion);
                }
            }
        }


        // Iterate over old regions inside squares array and ‘erase’ them (assign ‘32’ instead of the old region ids)
        for (int row = START_ROW; row < END_ROW; row++) {
            for (int col = START_COL; col < END_COL; col++) {
                if (!map.isWall(row, col) && neighbouringRegionsInSameSector.contains(map.squares[row][col])) {
                    map.squares[row][col] = GameMap.EMPTY_CHAR; // 32
                }
            }
        }

        HashSet<Integer> neighborIdsSet = new HashSet<>();
        for (int neighbouringRegionInSameSector : neighbouringRegionsInSameSector) {
            // Update freeSpace
            dbBW.pushFreeSpace(neighbouringRegionInSameSector);
            // Add to neighbourIds
            if (groups.get(neighbouringRegionInSameSector) != null) {
                neighborIdsSet.addAll(groups.get(neighbouringRegionInSameSector).getNeighborIds());
            }
            // Tombstone group record in groups map (recreate it later)
            map.addGroup(neighbouringRegionInSameSector, null);
        }

        // Remove regions that will merge
        // TODO: Is this necessary?
        neighborIdsSet.addAll(neighbouringRegions);
        neighborIdsSet.removeAll(neighbouringRegionsInSameSector);

        ArrayList<Integer> neighborIds = new ArrayList<>(neighborIdsSet);

        // Perform abstraction (go over sector and recompute regions), this updates free space
        // TODO: Should I even pass regionId here?
        int numRegionsInSector = map.sectorReAbstractWithFreeSpace(START_ROW, START_COL, END_ROW, END_COL, -1, map, dbBW);

        int count = 0;
        GroupRecord[] newRecs = new GroupRecord[numRegionsInSector];

        // Re-create groups
        for (int row = START_ROW; row < END_ROW; row++) {
            for (int col = START_COL; col < END_COL; col++) {
                int groupId = map.squares[row][col];
                if (groupId != GameMap.EMPTY_CHAR && groupId != GameMap.WALL_CHAR) {
                    // See if group already exists
                    GroupRecord rec = groups.get(groupId);
                    if (rec == null) {    // New group
                        GroupRecord newRec = new GroupRecord();
                        newRec.groupId = groupId;
                        newRec.groupRepId = map.getId(row, col);
                        newRec.setNumStates(1);
                        newRec.setNeighborIds(new HashSet<>());
                        map.addGroup(groupId, newRec);
                        newRecs[count++] = newRec;
                    } else {    // Update group
                        rec.incrementNumStates();
                    }
                }
            }
        }

        // Recompute region reps for newly added regions
        // a newRec should never be null, if it is, something went wrong with the group generation in sectorReAbstractWithFreeSpace
        for (GroupRecord newRec : newRecs) {
            map.recomputeCentroid(newRec.groupId, newRec, START_ROW, END_ROW, START_COL, END_COL);
            neighborIds.add(newRec.groupId);
        }

        // Recompute neighbourhood
        map.recomputeNeighbors(neighbouringRegionsInSameSector, START_ROW, START_COL, END_ROW, END_COL, neighborIds);

        // Database changes
        dbBW.recomputeBasePathsAfterPartition(problem, groups, neighborIds);
    }

    public void recomputeWallAdditionNoChecks(int wallLoc, DBAStar dbaStarBW) throws Exception {
        GameMap map = dbaStarBW.getMap();

        MapSearchProblem problem = (MapSearchProblem) dbaStarBW.getProblem();
        SubgoalDynamicDB3 dbBW = (SubgoalDynamicDB3) dbaStarBW.getDatabase();

        final int WALL_ROW = map.getRow(wallLoc);
        final int WALL_COL = map.getCol(wallLoc);

        // Get id of region that wall was placed in
        final int REGION_ID = map.squares[WALL_ROW][WALL_COL];

        map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = '*';
        problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = '*';

        final int SECTOR_ID = map.findSectorId(WALL_ROW, WALL_COL);

        // Start of sector
        final int START_ROW = map.findStartRowOfSector(SECTOR_ID);
        final int START_COL = map.findStartColOfSector(SECTOR_ID);
        // End of sector
        final int END_ROW = map.findEndRowOfSector(START_ROW);
        final int END_COL = map.findEndColOfSector(START_COL);

        // Get groups containing information on all regions from map
        TreeMap<Integer, GroupRecord> groups = map.getGroups();

        // Get group record containing information on the region
        GroupRecord groupRecord = groups.get(REGION_ID);

        // Get neighbours
        ArrayList<Integer> neighborIds = new ArrayList<>(groupRecord.getNeighborIds());

        // Reset region (necessary in order for me to be able to reuse the regionId)
        for (int row = START_ROW; row < END_ROW; row++) {
            for (int col = START_COL; col < END_COL; col++) {
                if (!map.isWall(row, col) && map.squares[row][col] == REGION_ID) {
                    map.squares[row][col] = GameMap.EMPTY_CHAR; // 32
                }
            }
        }

        // Adding the regionId to the freeSpace array in the database
        dbBW.pushFreeSpace(REGION_ID);

        // Perform abstraction (go over sector and recompute regions), this updates free space
        int numRegionsInSector = map.sectorReAbstractWithFreeSpace(START_ROW, START_COL, END_ROW, END_COL, REGION_ID, map, dbBW);

        // Tombstone group record in groups map (recreate it later)
        map.addGroup(REGION_ID, null);

        int count = 0;
        GroupRecord[] newRecs = new GroupRecord[numRegionsInSector];

        // Re-create groups
        for (int row = START_ROW; row < END_ROW; row++) {
            for (int col = START_COL; col < END_COL; col++) {
                int groupId = map.squares[row][col];
                if (groupId != GameMap.EMPTY_CHAR && groupId != GameMap.WALL_CHAR) {
                    // See if group already exists
                    GroupRecord rec = groups.get(groupId);
                    if (rec == null) {    // New group
                        GroupRecord newRec = new GroupRecord();
                        newRec.groupId = groupId;
                        newRec.groupRepId = map.getId(row, col);
                        newRec.setNumStates(1);
                        newRec.setNeighborIds(new HashSet<>());
                        map.addGroup(groupId, newRec);
                        newRecs[count++] = newRec;
                    } else {    // Update group
                        rec.incrementNumStates();
                    }
                }
            }
        }

        // Recompute region reps for newly added regions
        // a newRec should never be null, if it is, something went wrong with the group generation in sectorReAbstractWithFreeSpace
        for (GroupRecord newRec : newRecs) {
            map.recomputeCentroid(REGION_ID, newRec, START_ROW, END_ROW, START_COL, END_COL);
            neighborIds.add(newRec.groupId);
        }

        // Recompute neighbourhood
        map.recomputeNeighbors(REGION_ID, START_ROW, START_COL, END_ROW, END_COL, neighborIds);

        // Database changes
        dbBW.recomputeBasePathsAfterPartition(problem, groups, neighborIds);
    }
}
