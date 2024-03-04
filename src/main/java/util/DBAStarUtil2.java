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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

import static util.MapHelpers.*;

public class DBAStarUtil2 {
    private final int startNum;
    private final int gridSize;
    private final int numNeighbourLevels;
    private final int cutoff;
    private final String mapFileName;
    private final String dbaStarDbPath;

    private static final Logger logger = LogManager.getLogger(DBAStarUtil2.class);

    /**
     * @param gridSize      sectors on the map will be gridSize by gridSize
     * @param cutoff        for HillClimbing algorithm
     * @param mapFileName   name of the .map file to use
     * @param dbaStarDbPath location where the database and other output will be generated
     */
    public DBAStarUtil2(int gridSize, int cutoff, String mapFileName, String dbaStarDbPath) {
        // startNum is used as an offset for values in the squares array, region indexing in the array starts at startNum
        this.startNum = 50;
        this.gridSize = gridSize;
        this.numNeighbourLevels = 1;
        this.cutoff = cutoff;
        this.mapFileName = mapFileName;
        this.dbaStarDbPath = dbaStarDbPath;
    }

    public DBAStar computeDBAStarDatabaseUsingSubgoalDynamicDB3(GameMap map, String wallStatus) {
        long currentTime;

        SearchProblem problem = new MapSearchProblem(map);
        HillClimbing pathCompressAlgDba = new HillClimbing(problem, 10000);

        // Load abstract map and database
        logger.debug("Loading database.");

        SubgoalDynamicDB3 database = new SubgoalDynamicDB3(); // DP matrix in adjacency list representation (computed at run-time)

        String fileName = getDBName(wallStatus);

        logger.debug("Loading map and performing abstraction...");

        // GreedyHC map abstraction
        DBStats dbStats = new DBStats();
        DBStats.init(dbStats);

        DBStatsRecord rec = new DBStatsRecord(dbStats.getSize());
        rec.addStat(0, "dbaStar (" + numNeighbourLevels + ")");
        rec.addStat(1, gridSize);
        rec.addStat(3, cutoff);
        rec.addStat(4, mapFileName);
        rec.addStat(5, map.rows);
        rec.addStat(6, map.cols);

        currentTime = System.currentTimeMillis();
        map = map.sectorAbstract2(gridSize);

        long resultTime = System.currentTimeMillis() - currentTime;
        rec.addStat(12, resultTime);
        rec.addStat(10, resultTime);
        rec.addStat(11, map.states);
        rec.addStat(7, map.states);
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
     * @param startId state id for start of path
     * @param goalId  state id for start of path
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
     * @param dbaStar    DBAStar object
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
        map.computeCentroidMap().outputImage(dbaStarDbPath + wallStatus + mapFileName + "_path.png", path, dbaStar.getSubgoals());
        map.computeCentroidMap().outputImage(dbaStarDbPath + wallStatus + mapFileName + "_optimal_path.png", optimalPath, dbaStar.getSubgoals());
    }


    /**
     * @param wallStatus used to name output files, either BW = before wall, AW = after wall, or RW = removed wall
     * @return String
     */
    private String getDBName(String wallStatus) {
        return dbaStarDbPath + wallStatus + mapFileName + "_DBA-STAR_G" + gridSize + "_N" + numNeighbourLevels + "_C" + cutoff + ".dat";
    }

    /**
     * @param wallLoc   state id where wall will be place
     * @param dbaStarBW DBAStar object returned from computeDBAStarDatabaseUsingSubgoalDynamicDB3
     */
    public void recomputeWallAdditionUsingSubgoalDynamicDB3(int wallLoc, DBAStar dbaStarBW) throws Exception {
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
        if (!PRIOR_WALL && map.isWall(wallLoc) && problem.getMap().isWall(wallLoc)) {
            logger.info("Wall at " + wallLoc + " set successfully!");
        } else {
            throw new Exception("Wall addition failed! There is a wall at " + wallLoc + " already");
        }

        // Get representative of region wall was placed in
        final int REGION_REP = map.getRegionRepFromRegionId(REGION_ID);

        // If the region rep is tombstoned
        if (REGION_REP == -1) {
            throw new Exception("Region rep for region " + REGION_ID + "does not exist!");
        }

        // Get groups containing information on all regions from map
        TreeMap<Integer, GroupRecord> groups = new MapSearchProblem(map).getGroups();

        // Get group record containing information on the region
        GroupRecord groupRecord = groups.get(REGION_REP);

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
                groups.get(neighbourRep).getNeighborIds().remove(REGION_ID);
            }
            // Tombstone group record in groups map
            map.addGroup(REGION_ID, null);
            // Tombstone region in region reps array
            map.tombstoneRegionRepUsingRegionId(REGION_ID);

            // TODO: Database changes
        } else {
            // Other cases

            final int NUM_SECTORS_PER_ROW = (int) Math.ceil(map.cols * 1.0 / gridSize);
            final int SECTOR_ID = WALL_ROW / gridSize * NUM_SECTORS_PER_ROW + WALL_COL / gridSize;

            // Start of sector
            final int START_ROW = (SECTOR_ID / NUM_SECTORS_PER_ROW) * gridSize;
            final int START_COL = (SECTOR_ID % NUM_SECTORS_PER_ROW) * gridSize;
            // End of sector
            final int END_ROW = Math.min(START_ROW + gridSize, map.rows);
            final int END_COL = Math.min(START_COL + gridSize, map.cols);

            // Eight neighbours (states touching wall state)
            int NEIGHBOR_N = map.squares[WALL_ROW - 1][WALL_COL];

            // Need to check boundaries for bottom row of map (if not on map, treat square as wall)
            // TODO: Do I really need to check all of these?
            int NEIGHBOR_NE = map.isValid(WALL_ROW - 1, WALL_COL + 1) ? map.squares[WALL_ROW - 1][WALL_COL + 1] : 42;
            int NEIGHBOR_E = map.isValid(WALL_ROW, WALL_COL + 1) ? map.squares[WALL_ROW][WALL_COL + 1] : 42;
            int NEIGHBOR_SE = map.isValid(WALL_ROW + 1, WALL_COL + 1) ? map.squares[WALL_ROW + 1][WALL_COL + 1] : 42;
            int NEIGHBOR_S = map.isValid(WALL_ROW + 1, WALL_COL) ? map.squares[WALL_ROW + 1][WALL_COL] : 42;
            int NEIGHBOR_SW = map.isValid(WALL_ROW + 1, WALL_COL - 1) ? map.squares[WALL_ROW + 1][WALL_COL - 1] : 42;

            int NEIGHBOR_W = map.squares[WALL_ROW][WALL_COL - 1];
            int NEIGHBOR_NW = map.squares[WALL_ROW - 1][WALL_COL - 1];

            boolean isAtSectorEdge = WALL_ROW == START_ROW || WALL_ROW == END_ROW || WALL_COL == START_COL || WALL_COL == END_COL;

            // If we are placing a wall at the edge of a sector, we may have a pathblocker case
            // A maximum of (4 * 16 - 4) / (16 * 16) = 60 / 256 states per sector are affected
            if (isAtSectorEdge) {
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
                if (isTopLeftCorner && !map.isWall(NEIGHBOR_NW)) {
                    neighbourRegion = map.squares[WALL_ROW - 1][WALL_COL - 1];
                    neighbourRegionRep = map.getRegionRepFromState(NEIGHBOR_NW);
                } else if (isTopRightCorner && !map.isWall(NEIGHBOR_NE)) {
                    neighbourRegion = map.squares[WALL_ROW - 1][WALL_COL + 1];
                    neighbourRegionRep = map.getRegionRepFromState(NEIGHBOR_NE);
                } else if (isBottomRightCorner && !map.isWall(NEIGHBOR_SE)) {
                    neighbourRegion = map.squares[WALL_ROW + 1][WALL_COL + 1];
                    neighbourRegionRep = map.getRegionRepFromState(NEIGHBOR_SE);
                } else if (isBottomLeftCorner && !map.isWall(NEIGHBOR_SW)) {
                    neighbourRegion = map.squares[WALL_ROW + 1][WALL_COL - 1];
                    neighbourRegionRep = map.getRegionRepFromState(NEIGHBOR_SW);
                }

                if (neighbourRegion == REGION_ID) {
                    throw new Exception("NeighbourRegion id calculation went wrong!");
                }

                if (neighbourRegionRep == -1) {
                    throw new Exception("Region rep for region " + REGION_ID + "does not exist!");
                }

                // Pathblocker corner case
                // TODO: Figure out a better way to combine code for edge case and corner case
                if (neighbourRegion != 0) {
                    // Eliminate the state in the states ArrayList inside the groups map
                    groupRecord.states.remove((Integer) wallLoc);

                    // Get the neighbours of the region
                    HashSet<Integer> neighbours = groupRecord.getNeighborIds();
                    // Update region’s neighbourhood in groups map
                    neighbours.remove(neighbourRegion);

                    // Get the neighbours of its soon-to-be ex-neighbor
                    GroupRecord neighborRecord = groups.get(REGION_REP);
                    HashSet<Integer> neighboursOfEx = neighborRecord.getNeighborIds();
                    // Update old neighbour’s neighbourhood in groups map
                    neighboursOfEx.remove(REGION_ID);

                    // TODO: Database changes
                }

                // Reset values before edge tests
                neighbourRegion = 0;
                neighbourRegionRep = 0;

                // Edge cases (check if wall is at the edge of a sector and whether the region bordering this edge has
                // any other touching points with the region)

                // TODO: Check my math here!
                // If the wall is at an edge and the state next to it is not a wall, and this was the only touching point
                // between two regions, we have an edge blocker
                // This means the two regions that are currently neighbours shouldn't be anymore
                if (isNorthEdge && !map.isWall(NEIGHBOR_N)) {
                    if (hasNoOtherPointOfContactHorizontally(map, REGION_ID, START_COL, WALL_ROW, WALL_COL, WALL_ROW - 1)) {
                        neighbourRegion = map.squares[WALL_ROW - 1][WALL_COL];
                        neighbourRegionRep = map.getRegionRepFromState(NEIGHBOR_N);
                    }
                } else if (isEastEdge && !map.isWall(NEIGHBOR_E)) {
                    if (hasNoOtherPointOfContactVertically(map, REGION_ID, START_ROW, WALL_ROW, WALL_COL, WALL_COL + 1)) {
                        neighbourRegion = map.squares[WALL_ROW][WALL_COL + 1];
                        neighbourRegionRep = map.getRegionRepFromState(NEIGHBOR_E);
                    }
                } else if (isSouthEdge && !map.isWall(NEIGHBOR_S)) {
                    if (hasNoOtherPointOfContactHorizontally(map, REGION_ID, START_COL, WALL_ROW, WALL_COL, WALL_ROW + 1)) {
                        neighbourRegion = map.squares[WALL_ROW + 1][WALL_COL];
                        neighbourRegionRep = map.getRegionRepFromState(NEIGHBOR_S);
                    }
                } else if (isWestEdge && !map.isWall(NEIGHBOR_W)) {
                    if (hasNoOtherPointOfContactVertically(map, REGION_ID, START_ROW, WALL_ROW, WALL_COL, WALL_COL - 1)) {
                        neighbourRegion = map.squares[WALL_ROW][WALL_COL - 1];
                        neighbourRegionRep = map.getRegionRepFromState(NEIGHBOR_W);
                    }
                }

                if (neighbourRegion == REGION_ID) {
                    throw new Exception("NeighbourRegion id calculation went wrong!");
                }

                if (neighbourRegionRep == -1) {
                    throw new Exception("Region rep for region " + REGION_ID + "does not exist!");
                }

                // Pathblocker edge case
                // TODO: Figure out a better way to combine code for edge case and corner case
                if (neighbourRegion != 0) {
                    // Eliminate the state in the states ArrayList inside the groups map
                    groupRecord.states.remove((Integer) wallLoc);

                    // Get the neighbours of the region
                    HashSet<Integer> neighbours = groupRecord.getNeighborIds();
                    // Update region’s neighbourhood in groups map
                    neighbours.remove(neighbourRegion);

                    // Get the neighbours of its soon-to-be ex-neighbor
                    GroupRecord neighborRecord = groups.get(REGION_REP);
                    HashSet<Integer> neighboursOfEx = neighborRecord.getNeighborIds();
                    // Update old neighbour’s neighbourhood in groups map
                    neighboursOfEx.remove(REGION_ID);

                    // TODO: Database changes
                }
            }

            // Wall on Region Representative case
            // TODO: Consider blocker and wall on rep case
            if (wallLoc == REGION_REP) {
                // Eliminate the state in the states ArrayList inside the groups map
                groupRecord.states.remove((Integer) wallLoc);

                // Compute new region rep for the region by finding center of the region, and updating group record with
                // this information and update region reps array to contain new region rep
                map.recomputeCentroid2(groupRecord, wallLoc);

                // TODO: Database changes
            }

            // Region Partition case

            // If wall is between two walls, or between a wall and another region, we may have partition
            boolean isVerticalWall = isContinuousWall(NEIGHBOR_S, NEIGHBOR_N);
            boolean isHorizontalWall = isContinuousWall(NEIGHBOR_W, NEIGHBOR_E);
            boolean isBetweenVertical = !isVerticalWall && isBetweenWallAndOtherRegion(NEIGHBOR_S, NEIGHBOR_N, REGION_ID);
            boolean isBetweenHorizontal = !isHorizontalWall && isBetweenWallAndOtherRegion(NEIGHBOR_W, NEIGHBOR_E, REGION_ID);

            boolean verticalPartition = false;
            boolean horizontalPartition = false;
            boolean diagonalPartition;

            // TODO: Do the start points for these paths make sense?
            if (isVerticalWall || isBetweenVertical) {
                // Check that we can still reach west to east without leaving the region
                verticalPartition = !isPathPossible(map.squares, new int[]{WALL_ROW, WALL_COL - 1}, new int[]{WALL_ROW, WALL_COL + 1}, REGION_ID);
            }

            if (isHorizontalWall || isBetweenHorizontal) {
                // Check that we can still reach north to south without leaving the region
                horizontalPartition = !isPathPossible(map.squares, new int[]{WALL_ROW - 1, WALL_COL}, new int[]{WALL_ROW + 1, WALL_COL}, REGION_ID);
            }

            // If the diagonal state is open, but the cardinal ones surrounding it are not, we may have partition
            boolean isOpenDiagonalNW = isOpenDiagonal(NEIGHBOR_W, NEIGHBOR_NW, NEIGHBOR_N);
            boolean isOpenDiagonalNE = isOpenDiagonal(NEIGHBOR_N, NEIGHBOR_NE, NEIGHBOR_E);
            boolean isOpenDiagonalSE = isOpenDiagonal(NEIGHBOR_E, NEIGHBOR_SE, NEIGHBOR_S);
            boolean isOpenDiagonalSW = isOpenDiagonal(NEIGHBOR_S, NEIGHBOR_SW, NEIGHBOR_W);

            boolean partitionNW = false;
            boolean partitionNE = false;
            boolean partitionSE = false;
            boolean partitionSW = false;

            // TODO: Do the start points for these paths make sense? Can we collapse cases?
            if (isOpenDiagonalNW) {
                // Check whether we can still reach southeast to northwest
                partitionNW = !isPathPossible(map.squares, new int[]{WALL_ROW + 1, WALL_COL + 1}, new int[]{WALL_ROW - 1, WALL_COL - 1}, REGION_ID);
            }
            if (isOpenDiagonalNE) {
                // Check whether we can still reach southwest to northeast
                partitionNE = !isPathPossible(map.squares, new int[]{WALL_ROW + 1, WALL_COL - 1}, new int[]{WALL_ROW - 1, WALL_COL + 1}, REGION_ID);
            }
            if (isOpenDiagonalSE) {
                // Check whether we can still reach northwest to southeast
                partitionSE = !isPathPossible(map.squares, new int[]{WALL_ROW - 1, WALL_COL - 1}, new int[]{WALL_ROW + 1, WALL_COL + 1}, REGION_ID);
            }
            if (isOpenDiagonalSW) {
                // Check whether we can still reach northeast to southwest
                partitionSW = !isPathPossible(map.squares, new int[]{WALL_ROW - 1, WALL_COL + 1}, new int[]{WALL_ROW + 1, WALL_COL - 1}, REGION_ID);
            }

            diagonalPartition = partitionNW || partitionNE || partitionSE || partitionSW;

            if (verticalPartition || horizontalPartition || diagonalPartition) {
                // Get neighbours
                ArrayList<Integer> neighborIds = new ArrayList<>(groupRecord.getNeighborIds());

                // Reset region (necessary in order for me to be able to reuse the regionId)
                for (int row = START_ROW; row < END_ROW; row++) {
                    for (int col = START_COL; col < END_COL; col++) {
                        if (!map.isWall(row, col) && map.squares[row][col] == REGION_ID) {
                            map.squares[row][col] = ' '; // 32
                        }
                    }
                }

                // Perform abstraction (go over sector and recompute regions)
                int numRegionsInSector = map.sectorReAbstract2(gridSize, START_ROW, START_COL, END_ROW, END_COL, REGION_ID, map);

                // Tombstone group record in groups map (recreate it later)
                map.addGroup(REGION_ID, null);

                int count = 0;
                GroupRecord[] newRecs = new GroupRecord[ numRegionsInSector];

                // Re-create groups
                for (int row = START_ROW; row < END_ROW; row++) {
                    for (int col = START_COL; col < END_COL; col++) {
                        int groupId = map.squares[row][col];
                        if (groupId != GameMap.EMPTY_CHAR && groupId != GameMap.WALL_CHAR) {
                            // See if group already exists
                            GroupRecord rec = groups.get(groupId);
                            if (rec == null) {    // New group
                                GroupRecord newRec = new GroupRecord();
                                newRec.setNumStates(1);
                                newRec.groupId = groupId;
                                newRec.groupRepId = map.getId(row, col);
                                newRec.states = new ArrayList<>(10);
                                newRec.states.add(newRec.groupRepId);
                                map.addGroup(groupId, newRec);
                                newRecs[count++] = newRec;
                            } else {    // Update group
                                rec.setNumStates(rec.getSize() + 1);
                                rec.states.add(map.getId(row, col));
                            }
                        }
                    }
                }

                // Recompute region reps for newly added regions
                // a newRec should never be null, if it is, something went wrong with the group generation in sectorReAbstract2
                for (GroupRecord newRec : newRecs) {
                    map.recomputeCentroid2(newRec, wallLoc);
                    neighborIds.add(newRec.groupId);
                }

                // Recompute neighbourhood
                map.recomputeNeighbors(gridSize, START_ROW, START_COL, END_ROW, END_COL, neighborIds);

                // TODO: Database changes
            }

            // Eliminate the state in the states ArrayList inside the groups map
            groupRecord.states.remove((Integer) wallLoc);

            // Compute newRegionRep to detect whether a shift has happened
            int newRegionRep = map.recomputeCentroid2(groupRecord, wallLoc);

            // Wall That Moves Region Representative case
            if (newRegionRep != REGION_REP) {
                // TODO: Database changes
            }

            // Wall That Changes Shortest Path
            // TODO: Database changes
        }
    }

    /**
     * @param wallLoc   state id where wall will be removed
     * @param dbaStarBW DBAStar object returned from computeDBAStarDatabaseUsingSubgoalDynamicDB3
     */
    public void recomputeWallRemovalUsingSubgoalDynamicDB3(int wallLoc, DBAStar dbaStarBW) throws Exception {
        // Extract map, problem, and database from dbaStarBW
        GameMap map = dbaStarBW.getMap();
        MapSearchProblem problem = (MapSearchProblem) dbaStarBW.getProblem();
        SubgoalDynamicDB3 dbBW = (SubgoalDynamicDB3) dbaStarBW.getDatabase();

        final int WALL_ROW = map.getRow(wallLoc);
        final int WALL_COL = map.getCol(wallLoc);

        // Check whether there is a wall at the location the wall should be removed
        final boolean PRIOR_WALL = map.isWall(wallLoc) && problem.getMap().isWall(wallLoc);

        // Remove wall from map and from map inside problem, set the square to empty (=32) for now
        map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = ' ';
        problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = ' ';

        // Check whether the wall addition worked as intended
        if (PRIOR_WALL && !map.isWall(wallLoc) && !problem.getMap().isWall(wallLoc)) {
            logger.info("Wall at " + wallLoc + " removed successfully!");
        } else {
            throw new Exception("Wall removal failed! There is no wall to remove at" + wallLoc);
        }

        int[] neighbourStates = map.getNeighborIds(WALL_ROW, WALL_COL);

        // Check whether all eight neighbour states are walls, if so, we have a new, solitary region
        boolean isSurroundedByWalls = false;
        for (int neighbourState : neighbourStates) {
            if (map.isWall(neighbourState)) {
                isSurroundedByWalls = true;
                break;
            }
        }

        // If all eight neighbour states of where the wall is to be removed are walls, we will have a new, solitary region
        if (isSurroundedByWalls) {
            // Get new regionId using freeSpace
            int regionId = dbBW.popFreeSpace();

            TreeMap<Integer, GroupRecord> groups = new MapSearchProblem(map).getGroups();

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
            newRec.setNumStates(1);
            newRec.groupId = regionId;
            // Group rep id does not need to be computed using compute centroids logic since it must be where the wall was removed
            newRec.groupRepId = map.getId(WALL_ROW, WALL_COL);
            newRec.states = new ArrayList<>(1);
            newRec.states.add(newRec.groupRepId);
            // Add the new group record to the groups map
            map.addGroup(regionId, newRec);

            // Update regionReps array
            map.addRegionRep(regionId, newRec.groupRepId);

            // TODO: Database changes
        } else {
            // Check what sector the non-wall neighbourStates are in

            for (int neighbourState : neighbourStates) {
                if (!map.isWall(neighbourState)) {

                }
            }
        }
    }

    private boolean hasNoOtherPointOfContactVertically(GameMap map, int regionId, int edgeStartRow, int wallRow, int wallCol, int neighborCol) throws Exception {
        int neighbourRegion = map.squares[neighborCol][wallRow];

        if (neighbourRegion == 42) {
            throw new Exception("Neighbour state is a wall!");
        }

        // Examine the entire edge of the regions touching
        // If the region and its neighbour touch in more than the one spot where we placed the wall, we do not have a
        // blocker case and return false

        // TODO: Check this logic
        for (int i = edgeStartRow; i < gridSize; i++) {
            if (wallRow != i) {
                int stateInWallSector = map.squares[i][wallCol];
                int stateBorderingWallSector = map.squares[i][neighborCol];

                if (stateInWallSector == regionId && stateBorderingWallSector == neighbourRegion) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean hasNoOtherPointOfContactHorizontally(GameMap map, int regionId, int edgeStartCol, int wallRow, int wallCol, int neighborRow) throws Exception {
        int neighbourRegion = map.squares[wallCol][neighborRow];

        if (neighbourRegion == 42) {
            throw new Exception("Neighbour state is a wall!");
        }

        // Examine the entire edge of the regions touching
        // If the region and its neighbour touch in more than the one spot where we placed the wall, we do not have a
        // blocker case and return false

        // TODO: Check this logic
        for (int i = edgeStartCol; i < gridSize; i++) {
            if (wallCol != i) {
                int stateInWallSector = map.squares[wallRow][i];
                int stateBorderingWallSector = map.squares[neighborRow][i];

                if (stateInWallSector == regionId && stateBorderingWallSector == neighbourRegion) {
                    return false;
                }
            }
        }

        return true;
    }
}
