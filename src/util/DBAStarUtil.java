package util;

import database.DBStats;
import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDynamicDB2;
import map.GameMap;
import map.GroupRecord;
import search.*;

import java.util.*;

import static util.MapHelpers.*;
import static util.MapHelpers.isPathPossible;

public final class DBAStarUtil {

    private final int startNum;

    private final int gridSize;
    private final int numNeighbourLevels;
    private final int cutoff;
    private final String mapFileName;
    private final String dbaStarDbPath;

    /**
     *
     * @param gridSize sectors on the map will be gridSize by gridSize
     * @param numNeighbourLevels compute base path to regions up to numNeighbourLevels away
     * @param cutoff for HillClimbing algorithm
     * @param mapFileName name of the .map file to use
     * @param dbaStarDbPath location where the database and other output will be generated
     */
    public DBAStarUtil(int gridSize, int numNeighbourLevels, int cutoff, String mapFileName, String dbaStarDbPath) {
        // startNum is used as an offset for values in the squares array, region indexing in the array starts at startNum
        this.startNum = 50;
        this.gridSize = gridSize;
        this.numNeighbourLevels = numNeighbourLevels;
        this.cutoff = cutoff;
        this.mapFileName = mapFileName;
        this.dbaStarDbPath = dbaStarDbPath;
    }

    public DBAStar computeDBAStar(GameMap map, String wallStatus) {
        long currentTime;

        SearchProblem problem = new MapSearchProblem(map);
        HillClimbing pathCompressAlgDba = new HillClimbing(problem, 10000);

        // Load abstract map and database
        System.out.println("Loading database.");

        SubgoalDynamicDB2 database = new SubgoalDynamicDB2(); // DP matrix in adjacency list representation (computed at run-time)

        String fileName = getDBName(wallStatus);

        System.out.println("Loading map and performing abstraction...");

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

        // System.out.println("Exporting map with areas.");
        // map.outputImage(getImageName(wallStatus, false), null, null);

        // System.out.println("Exporting map with areas and centroids.");
        // map.computeCentroidMap().outputImage(getImageName(wallStatus, true), null, null);

        SearchProblem tmpProb = new MapSearchProblem(map);
        GameDB gameDB = new GameDB(tmpProb);

        currentTime = System.currentTimeMillis();
        database.computeIndex(tmpProb, rec);
        rec.addStat(23, System.currentTimeMillis() - currentTime);

        System.out.println("Generating gameDB.");
        currentTime = System.currentTimeMillis();

        database = gameDB.computeDynamicDB(database, pathCompressAlgDba, rec, numNeighbourLevels);
        System.out.println("Time to compute DBAStar gameDB: " + (System.currentTimeMillis() - currentTime));

        database.init();

        database.exportDB(fileName);
        map.computeComplexity(rec);
        dbStats.addRecord(rec);
        database.setProblem(problem);
        System.out.println("Verifying database.");
        database.verify(pathCompressAlgDba);
        System.out.println("Database verification complete.");
        System.out.println("Databases loaded.");

        return new DBAStar(problem, map, database);
    }

    /**
     *
     * @param startId state id for start of path
     * @param goalId state id for goal
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

    public void getDBAStarPath(int startId, int goalId, String wallStatus, DBAStar dbaStar) {
        GameMap map = dbaStar.getMap();

        AStar aStar = new AStar(dbaStar.getProblem());

        StatsRecord dbaStats = new StatsRecord();
        ArrayList<SearchState> path = dbaStar.computePath(new SearchState(startId), new SearchState(goalId), dbaStats);

        StatsRecord aStarStats = new StatsRecord();
        ArrayList<SearchState> optimalPath = aStar.computePath(new SearchState(startId), new SearchState(goalId), aStarStats);

        System.out.println("AStar path cost: " + aStarStats.getPathCost() + " DBAStar path cost: " + dbaStats.getPathCost());
        System.out.println("Suboptimality: " + ((((double) dbaStats.getPathCost()) / aStarStats.getPathCost()) - 1) * 100.0);

        if (path == null || path.isEmpty()) {
            System.out.printf("No path was found between %d and %d!%n", startId, goalId);
        }
        map.computeCentroidMap().outputImage(dbaStarDbPath + wallStatus + mapFileName + "_path.png", path, dbaStar.getSubgoals());
        map.computeCentroidMap().outputImage(dbaStarDbPath + wallStatus + mapFileName + "_optimal_path.png", optimalPath, dbaStar.getSubgoals());
    }

    public void recomputeWallAddition(int wallLoc, GameMap map, MapSearchProblem problem, SubgoalDynamicDB2 dbBW) throws Exception {
        SearchState wall = new SearchState(wallLoc);
        int regionId = map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)];

        boolean priorWall = map.isWall(wallLoc);

        // Add wall to existing map and to map inside problem
        map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = '*';
        priorWall = priorWall && problem.getMap().isWall(wallLoc);
        problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = '*';

        if (!priorWall && map.isWall(wallLoc) && problem.getMap().isWall(wallLoc)) {
            System.out.println("Wall at " + wallLoc + " set successfully!");
        } else {
            throw new Exception("Wall addition failed! There is a wall at " + wallLoc + " already");
        }

        // Get the id of the region rep of the region the wall was added in
        int regionRepId = map.getAbstractProblem().findRegionRep(wall, map).getId();
        System.out.println("regionRepId: " + regionRepId);

        if (regionRepId == wallLoc) {
            System.out.println("Wall on region rep!");
        }

        TreeMap<Integer, GroupRecord> groups = new MapSearchProblem(map).getGroups();

        System.out.println("regionId: " + regionId);

        // get the neighbour ids regions using the region id
        GroupRecord groupRecord = groups.get(regionId);

        boolean isElimination;

        int wallRow = map.getRow(wallLoc);
        int wallCol = map.getCol(wallLoc);

        int numSectorsPerRow = (int) Math.ceil(map.cols * 1.0 / gridSize);
        int sectorId = wallRow / gridSize * numSectorsPerRow + wallCol / gridSize;

        int startRow = (sectorId / numSectorsPerRow) * gridSize;
        int startCol = (sectorId % numSectorsPerRow) * gridSize;

        if (groupRecord.getNumStates() == 1) { // scenario when there is only one state in the region
            // need to tombstone region, and make sure it doesn't have neighbours or shortest paths anymore
            groups.remove(regionId);
            map.rebuildAbstractProblem(map, gridSize, startRow, startCol, new ArrayList<>(List.of(regionId)));
            isElimination = true;
        } else {
            // scenario when the regioning doesn't change significantly (region id stays the same)
            int newRegionRep = map.recomputeCentroid(groupRecord, wallLoc);
            regionRepId = newRegionRep;
            System.out.println("New rep at: " + newRegionRep);
            // get back new region rep and change the record
            groupRecord.setGroupRepId(newRegionRep);
            groups.replace(regionId, groupRecord);
            map.rebuildAbstractProblem(map, gridSize, startRow, startCol, new ArrayList<>(List.of(regionId)));
            isElimination = false;
        }

        // scenario where map is partitioned by wall addition
        boolean potentialHorizontalPartition = false;
        boolean potentialVerticalPartition = false;
        boolean potentialDiagonalPartition = false;

        // Need to check entire region to make sure it has not become partitioned by wall addition
        // idea: get neighbours of wall
        int wallRowId = map.getRow(wallLoc);
        int wallColId = map.getCol(wallLoc);
        System.out.println(map.squares[wallRowId][wallColId]);
        // it will have eight neighbours
        // TODO: having the regions marked on the map is not part of DBA*, should I use this?

        int neighborNorth = map.squares[wallRowId - 1][wallColId];

        // Need to check boundaries for bottom row of map (if not on map, treat square as wall)
        int neighborNorthEast = map.isValid(wallRowId - 1, wallColId + 1) ? map.squares[wallRowId - 1][wallColId + 1] : 42;
        int neighborEast = map.isValid(wallRowId, wallColId + 1) ? map.squares[wallRowId][wallColId + 1] : 42;
        int neighborSouthEast = map.isValid(wallRowId + 1, wallColId + 1) ? map.squares[wallRowId + 1][wallColId + 1] : 42;
        int neighborSouth = map.isValid(wallRowId + 1, wallColId) ? map.squares[wallRowId + 1][wallColId] : 42;
        int neighborSouthWest = map.isValid(wallRowId + 1, wallColId - 1) ? map.squares[wallRowId + 1][wallColId - 1] : 42;

        int neighborWest = map.squares[wallRowId][wallColId - 1];
        int neighborNorthWest = map.squares[wallRowId - 1][wallColId - 1];
        // if the region has become partitioned, it would have to have neighbors that are across from each other be walls or in different regions
        // (this is a necessary condition, but not sufficient)

        // In order for a partition to happen, either the newly placed wall touches two walls, or it touches a wall and
        // a state that is not in the region the wall was placed in (this is a necessary condition, but not sufficient)
        // TODO: is there an exception to this?

        // need to check !isElimination, because the algorithm sees the elimination case as a partition
        if (!isElimination && (isContinuousWall(neighborNorth, neighborSouth) || isBetweenWallAndOtherRegion(neighborNorth, neighborSouth, regionId))) {
            potentialVerticalPartition = true;
        }
        if (!isElimination && (isContinuousWall(neighborWest, neighborEast) || isBetweenWallAndOtherRegion(neighborWest, neighborEast, regionId))) {
            potentialHorizontalPartition = true;
        }
        // TODO: address diagonal partition (maybe split into two cases?)
        if (!isElimination && (isOpenDiagonal(neighborNorth, neighborNorthEast, neighborEast)
                || isOpenDiagonal(neighborEast, neighborSouthEast, neighborSouth)
                || isOpenDiagonal(neighborSouth, neighborSouthWest, neighborWest)
                || isOpenDiagonal(neighborWest, neighborNorthWest, neighborNorth))) {
            potentialDiagonalPartition = true;
        }

        System.out.println("WALL IS PARTITIONING MAP: " + (potentialHorizontalPartition || potentialVerticalPartition || potentialDiagonalPartition));
        if (potentialHorizontalPartition) System.out.println("HORIZONTALLY");
        if (potentialVerticalPartition) System.out.println("VERTICALLY");
        if (potentialDiagonalPartition) System.out.println("DIAGONALLY");

        // potentialPartition because the wall was added such that it is either surrounded by a wall on either side or
        // a wall on one and a different region on the other
        // this may still not be a partition (see adding wall at 11928)

        // If it has become partitioned, need to check if both partitions are still reachable from the rest of the map

        // CASE: region has become partitioned
        // check if we can find a path from one side of the region to the other
        boolean verticalPartition = false, horizontalPartition = false;
        // TODO: what if either path start or path goal are walls or in a different region?
        if (potentialVerticalPartition) {
            // check that we can still reach west to east without leaving the region
            verticalPartition = !isPathPossible(map.squares, new int[]{wallRowId, wallColId - 1}, new int[]{wallRowId, wallColId + 1}, regionId);
        }
        if (potentialHorizontalPartition) {
            // check that we can still reach north to south without leaving the region
            horizontalPartition = !isPathPossible(map.squares, new int[]{wallRowId - 1, wallColId}, new int[]{wallRowId + 1, wallColId}, regionId);
        }

        boolean isPartition = verticalPartition || horizontalPartition || potentialDiagonalPartition;

        ArrayList<Integer> neighborIds = new ArrayList<>(groupRecord.getNeighborIds());

        GroupRecord[] newRecs = null;

        if (isPartition) {
            System.out.println("Group size before removal: " + groups.size());
            // TODO: set neighbours of new regions using this

            int endRow = Math.min(startRow + gridSize, map.rows);
            int endCol = Math.min(startCol + gridSize, map.cols);

            // reset region (necessary in order for me to be able to reuse the regionId)
            // TODO: could bypass validity check if use values calculated above for loop
            for (int r = 0; r < gridSize; r++) {
                for (int c = 0; c < gridSize; c++) {
                    int row = startRow + r;
                    int col = startCol + c;
                    if (map.isValid(row, col) && !map.isWall(row, col) && map.squares[row][col] == regionId) {
                        map.squares[row][col] = ' '; // 32
                    }
                }
            }

            // Perform abstraction (go over sector and recompute regions)
            int numRegionsInSector = map.sectorReAbstract2(gridSize, startRow, startCol, endRow, endCol, regionId, map);

            System.out.println("Num regions: " + numRegionsInSector);

            int count = 0;
            newRecs = new GroupRecord[numRegionsInSector];

            groups.remove(regionId); // remove region from groups and recreate it later
            System.out.println("Group size after removal: " + groups.size());

            // Traverse cells in sector to re-create the groups
            for (int i = startRow; i < endRow; i++) {
                for (int j = startCol; j < endCol; j++) {
                    int groupId = map.squares[i][j];

                    if (groupId != GameMap.EMPTY_CHAR && groupId != GameMap.WALL_CHAR) {
                        // See if group already exists
                        GroupRecord rec = groups.get(groupId);
                        if (rec == null) {    // New group
                            GroupRecord newRec = new GroupRecord();
                            newRec.setNumStates(1);
                            newRec.groupId = groupId;
                            newRec.groupRepId = map.getId(i, j);
                            newRec.states = new ExpandArray(10);
                            newRec.states.add(newRec.groupRepId);
                            map.addGroup(groupId, newRec);
                            newRecs[count++] = newRec;
                        } else {    // Update group
                            rec.setNumStates(rec.getSize() + 1);
                            rec.states.add(map.getId(i, j));
                        }
                    }
                }
            }

            System.out.println("Group size after addition: " + groups.size());

            ArrayList<Integer> regionIds = new ArrayList<>();

            // Recompute region reps for newly added regions
            // a newRec should never be null, if it is, something went wrong with the group generation in sectorReAbstract2
            for (GroupRecord newRec : newRecs) {
                map.recomputeCentroid2(newRec, wallLoc);
                // Add regions that didn't exist before to list
                neighborIds.add(newRec.groupId);
                regionIds.add(newRec.groupId);
            }

            // VISUAL CHECK:
//            map.computeCentroidMap().outputImage(dbaStarDbPath + "TEST" + mapFileName + ".png", null, null);

            // Rebuild abstract problem
            map.rebuildAbstractProblem(map, gridSize, startRow, startCol, regionIds);

            // Set neighbours - TODO: check if this is working properly
            map.recomputeNeighbors(gridSize, startRow, startCol, endRow, endCol, neighborIds);
        }

        if (!isPartition) {
            neighborIds.add(groupRecord.groupId); // need to pass this so updates work both ways (for partition this is already added)
        }

        // Initialize pathCompressAlgDba
        HillClimbing pathCompressAlgDba = new HillClimbing(problem, 10000);

        // Update regions for neighborIds in the database
        dbBW.recomputeBasePathsAfterWallChange(problem, groups, neighborIds, pathCompressAlgDba, dbBW.getLowestCost(), dbBW.getPaths(),
                dbBW.getNeighbor(), neighborIds.size(), numNeighbourLevels, isElimination, isPartition);

        // Re-generate index database (TODO: optimize)
        dbBW.regenerateIndexDB(isPartition, isElimination, regionId, regionRepId, groups.size(), map, newRecs);

        // For checking recomputed database against AW database
        dbBW.exportDB(dbaStarDbPath + "BW_Recomp_" + mapFileName + "_DBA-STAR_G" + gridSize + "_N" + numNeighbourLevels + "_C" + cutoff + ".dat");
    }

    public void recomputeWallRemoval(int wallLoc, GameMap map, MapSearchProblem problem, SubgoalDynamicDB2 dbBW) throws Exception {
        long startTimeRecomp = System.currentTimeMillis();

        SearchState wall = new SearchState(wallLoc);

        boolean priorWall = map.isWall(wallLoc);

        int wallRow = map.getRow(wallLoc);
        int wallCol = map.getCol(wallLoc);

        // Remove wall from existing map and map inside problem
        map.squares[wallRow][wallCol] = ' '; // TODO: add correct region id here later
        priorWall = priorWall && problem.getMap().isWall(wallLoc);
        problem.getMap().squares[wallRow][wallCol] = ' '; // TODO: add correct region id here later

        if (priorWall && !map.isWall(wallLoc) && !problem.getMap().isWall(wallLoc)) {
            System.out.println("Wall at " + wallLoc + " removed successfully!");
        } else {
            System.out.printf("ERROR: No wall found at (%d, %d)%n", wallRow, wallCol);
        }

        TreeMap<Integer, GroupRecord> groups = new MapSearchProblem(map).getGroups();

        // Grab neighbouring states
        ArrayList<SearchState> neighbours = map.getNeighbors(wallRow, wallCol);
        Map<Integer, Integer> openStatesToSectors = new HashMap<>();
        Set<Integer> regionsTouchingWall = getRegionsTouchingWall(map, neighbours);

        if (isSurroundedByWalls(map, neighbours, openStatesToSectors)) {
            // Case 1: If a wall is encased by walls, we necessarily have a new, isolated region

            // Assign new region id to the location on the map
            int groupId = groups.size() + startNum;

            map.squares[wallRow][wallCol] = groupId;
            problem.getMap().squares[wallRow][wallCol] = groupId;

            // There should not be a group record with the new region id
            GroupRecord rec = groups.get(groupId);
            if (rec != null) System.out.println("Error! Record already exists!");

            // Create a new group record for the new region
            GroupRecord newRec = new GroupRecord();
            newRec.setNumStates(1);
            newRec.groupId = groupId;
            // Group rep id does not need to be computed using compute centroids logic since it must be where the wall was removed
            newRec.groupRepId = map.getId(wallRow, wallCol);
            newRec.states = new ExpandArray(1);
            newRec.states.add(newRec.groupRepId);
            map.addGroup(groupId, newRec);
            groups.put(groupId, newRec);

            // Initialize pathCompressAlgDba
            HillClimbing pathCompressAlgDba = new HillClimbing(problem, 10000);

            // Update regions for neighborIds in the database (only region requiring updates is new region, since it has no neighbours)
            ArrayList<Integer> neighborIds = new ArrayList<>();
            neighborIds.add(newRec.groupId);

            // Value of isPartition actually makes no difference here since that logic is skipped, set to true for consistency with code below
            dbBW.recomputeBasePathsAfterWallChange(problem, groups, neighborIds, pathCompressAlgDba, dbBW.getLowestCost(),
                    dbBW.getPaths(), dbBW.getNeighbor(), neighborIds.size(), numNeighbourLevels, false, true);

            // Re-generate index database (TODO: optimize)
            // groupId and regionRepId are identical in this case, isPartition because groupsMapping needs to be resized
            dbBW.regenerateIndexDB(true, false, groupId, groupId, groups.size(), map, new GroupRecord[]{newRec});

            dbBW.exportDB(dbaStarDbPath + "BW_Recomp_" + mapFileName + "_DBA-STAR_G" + gridSize + "_N" + numNeighbourLevels + "_C" + cutoff + ".dat");
        } else {
            // TODO: reverse partition case, later: optimize by looking at openStatesToSectors, if same sector, different regions are touching wall
            // Case 2: If a wall is not encased by walls, we need to check its sector membership, and the sector membership of the adjacent open spaces

            // Check sector membership of space where wall was
            int numSectorsPerRow = (int) Math.ceil(map.cols * 1.0 / gridSize);
            int sectorId = wallRow / gridSize * numSectorsPerRow + wallCol / gridSize;
            System.out.println("Wall was removed in sector: " + sectorId);

            /*
            TODO: check if openStatesToSectors.size() == 1, in this case, we know the state is only surrounded by states
             of the same region, and we don’t have to recompute that region. We can instead just assign it, then recompute
             the region rep, check if it’s the same, if no, recompute neighbourhood
             */

            // Check if it matches sector membership of surrounding open spaces
            if (openStatesToSectors.containsValue(sectorId)) {
                System.out.println("Removed wall in existing sector!");
                // TODO: Wall touches region that is in same sector as wall -> add wall to region and recompute neighbourhood (may have formed path)

                int regionRepId = map.getAbstractProblem().findRegionRep(wall, map).getId();

                // We cannot find the region id through the region rep, because the region rep may have been removed and
                // added back, in which case the square is blank, so we find it through its neighbours in the same sector
                int regionId = getRegionIdFromNeighbourStates(map, neighbours, sectorId);

                System.out.println("Existing region, region rep id: " + regionRepId + " region id: " + regionId);

                map.squares[wallRow][wallCol] = regionId;
                problem.getMap().squares[wallRow][wallCol] = regionId;

                // Compute start and end of current sector
                int startRow = (sectorId / numSectorsPerRow) * gridSize;
                int startCol = (sectorId % numSectorsPerRow) * gridSize;
                int endRow = Math.min(startRow + gridSize, map.rows);
                int endCol = Math.min(startCol + gridSize, map.cols);

                System.out.println("Start of current sector: " + map.getId(startRow, startCol));
                System.out.println("End of current sector: " + map.getId(endRow, endCol));
                System.out.println("Regions touching wall: " + regionsTouchingWall);

                // Nuking regions that touch where the wall was on the map (keeping other regions in the sector)
                Set<Integer> regionsInCurrentSector = new HashSet<>();
                // TODO: see other for loop
                for (int r = 0; r < gridSize; r++) {
                    for (int c = 0; c < gridSize; c++) {
                        int row = startRow + r;
                        int col = startCol + c;
                        // Only nuke regions that touch where the wall was
                        if (map.isValid(row, col) && !map.isWall(row, col) && regionsTouchingWall.contains(map.squares[row][col])) {
                            regionsInCurrentSector.add(map.squares[row][col]);
                            map.squares[row][col] = ' '; // 32
                        }
                    }
                }

                System.out.println("Number of groups: " + groups.size());

                // Put neighbours of old regions into set
                HashSet<Integer> neighbouringRegions = new HashSet<>();

                ArrayList<Integer> regionsInCurrentSectorList = new ArrayList<>();
                // Delete old regions from groups array:
                for (Integer region : regionsInCurrentSector) {
                    regionsInCurrentSectorList.add(region);
                    neighbouringRegions.addAll(groups.get(region).getNeighborIds());
                    groups.remove(region);
                    System.out.println("Removed region " + region);
                }

                // Remove regionsInCurrentSector from list of neighbours since we care about neighbours outside the sector
                neighbouringRegions.removeAll(regionsInCurrentSector);

                System.out.println("Number of groups after removal: " + groups.size());

                // TODO: Recompute regions in sector

                // Perform abstraction (go over sector and recompute regions)
                int numRegionsInSector = map.sectorReAbstract2(gridSize, startRow, startCol, endRow, endCol, regionId, map);

                System.out.println("After sectorReAbstract");
                for (int i = startRow; i < endRow; i++) {
                    for (int j = startCol; j < endCol; j++) {
                        System.out.print(map.squares[i][j] + " ");
                    }
                    System.out.println();
                }

                System.out.println("Num regions: " + numRegionsInSector);

                int count = 0;
                GroupRecord[] newRecs = new GroupRecord[numRegionsInSector];

                // Traverse cells in sector to re-create the groups
                for (int i = startRow; i < endRow; i++) {
                    for (int j = startCol; j < endCol; j++) {
                        int groupId = map.squares[i][j];

                        if (groupId != GameMap.EMPTY_CHAR && groupId != GameMap.WALL_CHAR) {
                            // See if group already exists
                            GroupRecord rec = groups.get(groupId);
                            if (rec == null) {    // New group
                                GroupRecord newRec = new GroupRecord();
                                newRec.setNumStates(1);
                                newRec.groupId = groupId;
                                newRec.groupRepId = map.getId(i, j);
                                newRec.states = new ExpandArray(10);
                                newRec.states.add(newRec.groupRepId);
                                map.addGroup(groupId, newRec);
                                newRecs[count++] = newRec;
                            } else {    // Update group
                                rec.setNumStates(rec.getSize() + 1);
                                rec.states.add(map.getId(i, j));
                            }
                        }
                    }
                }

                System.out.println(Arrays.toString(newRecs));

                // Recompute region reps for newly added regions
                for (GroupRecord newRec : newRecs) {
                    map.recomputeCentroid2(newRec, wallLoc);
                    // Add regions that didn't exist before to list
                    neighbouringRegions.add(newRec.groupId);
                    regionsInCurrentSector.add(newRec.groupId);
                }

                System.out.println("Group size after addition: " + groups.size());

                // VISUAL CHECK:
//                map.computeCentroidMap().outputImage(dbaStarDbPath + "TEST" + mapFileName + ".png", null, null);

                // add regions touching wall not contained in regionIds
//                for (Integer regionTouchingWall: regionsTouchingWall) {
//                    if (!regionIds.contains(regionTouchingWall)) regionIds.add(regionTouchingWall);
//                }

                map.rebuildAbstractProblem(map, gridSize, startRow, startCol, regionsInCurrentSectorList);

                // Set neighbours
                ArrayList<Integer> neighborIds = new ArrayList<>(neighbouringRegions);
                map.recomputeNeighbors(gridSize, startRow, startCol, endRow, endCol, neighborIds);

                // Get database and initialize pathCompressAlgDba
                HillClimbing pathCompressAlgDba = new HillClimbing(problem, 10000);

                // Update regions for neighborIds in the database
                dbBW.recomputeBasePathsAfterWallChange(problem, groups, neighborIds, pathCompressAlgDba, dbBW.getLowestCost(), dbBW.getPaths(),
                        dbBW.getNeighbor(), neighborIds.size(), numNeighbourLevels, false, true);

                // Re-generate index database (TODO: optimize)
                dbBW.regenerateIndexDB(false, true, regionId, regionRepId, groups.size(), map, newRecs);

                // For checking recomputed database against AW database
                dbBW.exportDB(dbaStarDbPath + "BW_Recomp_" + mapFileName + "_DBA-STAR_G" + gridSize + "_N" + numNeighbourLevels + "_C" + cutoff + ".dat");
            } else {
                System.out.println("Removed wall in new sector!");
                /*
                Case 3: Basically like case 1, but need to recompute paths to neighbours
                TODO: Wall touches region but it is not in same sector as wall -> new, connected, region (recompute neighbourhood)
                 */
                // Assign new region id to the location on the map

                int groupId = groups.lastKey() + 1;

                map.squares[wallRow][wallCol] = groupId;
                problem.getMap().squares[wallRow][wallCol] = groupId;

                // There should not be a group record with the new region id
                GroupRecord rec = groups.get(groupId);
                if (rec != null) System.out.println("Error! Record already exists!");

                // Create a new group record for the new region
                GroupRecord newRec = new GroupRecord();
                newRec.setNumStates(1);
                newRec.groupId = groupId;
                // Group rep id does not need to be computed using compute centroids logic since it must be where the wall was removed
                newRec.groupRepId = map.getId(wallRow, wallCol);
                newRec.states = new ExpandArray(1);
                newRec.states.add(newRec.groupRepId);
                map.addGroup(groupId, newRec);
                groups.put(groupId, newRec);

                // Get database and initialize pathCompressAlgDba
                HillClimbing pathCompressAlgDba = new HillClimbing(problem, 10000);

                // Grab neighbour states from openStatesToSectors, check which regions they belong to, get the reps for those regions, use set to ensure uniqueness
                Set<Integer> neighbouringRegions = new HashSet<>();

                for (Integer stateId : openStatesToSectors.keySet()) {
                    // state id - region id
                    System.out.println(stateId + " - " + map.squares[map.getRow(stateId)][map.getCol(stateId)]);
                    // Add region id for region
                    neighbouringRegions.add(map.squares[map.getRow(stateId)][map.getCol(stateId)]);
                }

                // Update regions for neighborIds in the database
                ArrayList<Integer> neighborIds = new ArrayList<>(neighbouringRegions);
                neighborIds.add(newRec.groupId);

                // TODO: Do I need to rebuild abstract problem and recompute neighbours?

                // Compute start and end of new sector
                int startRow = (sectorId / numSectorsPerRow) * gridSize;
                int startCol = (sectorId % numSectorsPerRow) * gridSize;
                int endRow = Math.min(startRow + gridSize, map.rows);
                int endCol = Math.min(startCol + gridSize, map.cols);

                // Rebuild abstract problem
                map.rebuildAbstractProblem(map, gridSize, startRow, startCol, new ArrayList<>(List.of(newRec.groupId)));

                // Set neighbours
                map.recomputeNeighbors(gridSize, startRow, startCol, endRow, endCol, neighborIds);

                // Value of isPartition actually makes no difference here since that logic is skipped, set to true for consistency with code below
                dbBW.recomputeBasePathsAfterWallChange(problem, groups, neighborIds, pathCompressAlgDba, dbBW.getLowestCost(), dbBW.getPaths(),
                        dbBW.getNeighbor(), neighborIds.size(), numNeighbourLevels, false, true);

                // Re-generate index database (TODO: optimize)
                // groupId and regionRepId are identical in this case, isPartition because groupsMapping needs to be resized
                dbBW.regenerateIndexDB(true, false, groupId, groupId, groups.size(), map, new GroupRecord[]{newRec});

                // For checking recomputed database against AW database
                dbBW.exportDB(dbaStarDbPath + "BW_Recomp_" + mapFileName + "_DBA-STAR_G" + gridSize + "_N" + numNeighbourLevels + "_C" + cutoff + ".dat");

                // TODO: Figure out why I need to do this and where subgoals come from

                // FIXME: clear subgoals
                // dbaStarBW.getSubgoals().clear();
            }
        }

//        getDBAStarPath(startId, goalId, "BW_Recomp", dbaStarBW);
//
//        System.out.println("Exporting map with areas and centroids.");
//        map.computeCentroidMap().outputImage(getImageName("BW_Recomp", true), null, null);

//        map.outputImage(dbaStarDbPath + "AfterRemoval_" + wallLoc + "_" + mapFileName + ".png", null, null);
        // TODO: add check here for num regions in sector

        if (groups.size() != 85) throw new Exception("Group size not 85!");
    }

    private String getDBName(String wallStatus) {
        return dbaStarDbPath + wallStatus + mapFileName + "_DBA-STAR_G" + gridSize + "_N" + numNeighbourLevels + "_C" + cutoff + ".dat";
    }
}