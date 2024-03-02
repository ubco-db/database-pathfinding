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

        int wallRow = map.getRow(wallLoc);
        int wallCol = map.getCol(wallLoc);

        // Get id of region that wall was placed in
        int regionId = map.squares[wallRow][wallCol];

        // Check whether there is already a wall at the location the wall should be placed
        boolean priorWall = map.isWall(wallLoc) && problem.getMap().isWall(wallLoc);

        // Add wall to map and to map inside problem
        map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = '*';
        problem.getMap().squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = '*';

        // Check whether the wall addition worked as intended
        if (!priorWall && map.isWall(wallLoc) && problem.getMap().isWall(wallLoc)) {
            logger.info("Wall at " + wallLoc + " set successfully!");
        } else {
            throw new Exception("Wall addition failed! There is a wall at " + wallLoc + " already");
        }

        // Get representative of region wall was placed in
        int regionRep = map.getRegionRepFromRegionId(regionId);

        // If the region rep is tombstoned
        if (regionRep == -1) {
            throw new Exception("Region rep for region " + regionId + "does not exist!");
        }

        // Get groups containing information on all regions from map
        TreeMap<Integer, GroupRecord> groups = new MapSearchProblem(map).getGroups();

        // Get group record containing information on the region
        GroupRecord groupRecord = groups.get(regionRep);

        // Elimination case
        if (groupRecord.getNumStates() == 1) {
            // Get the neighbours of the region that will be eliminated
            HashSet<Integer> neighbours = groupRecord.getNeighborIds();

            for (Integer neighbour : neighbours) {
                // Get the region rep of the current neighbour
                int neighbourRep = map.getRegionRepFromRegionId(neighbour);
                // If the region rep is tombstoned
                if (neighbourRep == -1) {
                    throw new Exception("Region rep for region " + regionId + "does not exist!");
                }
                // Remove the region as a neighbour of its neighbours
                groups.get(neighbourRep).getNeighborIds().remove(regionId);
            }
            // Tombstone group record in groups map
            groups.put(regionId, null);
            // Tombstone region in region reps array
            map.tombstoneRegionRepUsingRegionId(regionId);
        } else {

        }

        int numSectorsPerRow = (int) Math.ceil(map.cols * 1.0 / gridSize);
        int sectorId = wallRow / gridSize * numSectorsPerRow + wallCol / gridSize;

        // Start of sector
        int startRow = (sectorId / numSectorsPerRow) * gridSize;
        int startCol = (sectorId % numSectorsPerRow) * gridSize;
        // End of sector
        int endRow = Math.min(startRow + gridSize, map.rows);
        int endCol = Math.min(startCol + gridSize, map.cols);

        // Eight neighbours (states touching wall state)
        int neighborNorth = map.squares[wallRow - 1][wallCol];

        // Need to check boundaries for bottom row of map (if not on map, treat square as wall)
        // TODO: Do I really need to check all of these?
        int neighborNorthEast = map.isValid(wallRow - 1, wallCol + 1) ? map.squares[wallRow - 1][wallCol + 1] : 42;
        int neighborEast = map.isValid(wallRow, wallCol + 1) ? map.squares[wallRow][wallCol + 1] : 42;
        int neighborSouthEast = map.isValid(wallRow + 1, wallCol + 1) ? map.squares[wallRow + 1][wallCol + 1] : 42;
        int neighborSouth = map.isValid(wallRow + 1, wallCol) ? map.squares[wallRow + 1][wallCol] : 42;
        int neighborSouthWest = map.isValid(wallRow + 1, wallCol - 1) ? map.squares[wallRow + 1][wallCol - 1] : 42;

        int neighborWest = map.squares[wallRow][wallCol - 1];
        int neighborNorthWest = map.squares[wallRow - 1][wallCol - 1];

        // Pathblocker cases

        boolean isAtSectorEdge = wallRow == startRow || wallRow == endRow || wallCol == startCol || wallCol == endCol;

        if (isAtSectorEdge) {
            // TODO: Eliminate the state in the states ArrayList inside the groups map

            boolean isNorthEdge = wallRow == startRow;
            boolean isEastEdge = wallCol == endCol;
            boolean isSouthEdge = wallRow == endRow;
            boolean isWestEdge = wallCol == startCol;

            boolean isTopLeftCorner = isNorthEdge && isWestEdge;
            boolean isTopRightCorner = isNorthEdge && isEastEdge;
            boolean isBottomRightCorner = isSouthEdge && isEastEdge;
            boolean isBottomLeftCorner = isSouthEdge && isWestEdge;

            int neighbourRegion = regionId;
            int neighbourRegionRep = -1;

            // Corner cases (check if wall is in a sector corner and get the region id of the corner this corner is
            // touching, if applicable

            // TODO: Check my math here!
            if (isTopLeftCorner && !map.isWall(neighborNorthWest)) {
                neighbourRegion = map.squares[wallRow - 1][wallCol - 1];
                neighbourRegionRep = map.getRegionRepFromState(neighborNorthWest);
            } else if (isTopRightCorner && !map.isWall(neighborNorthEast)) {
                neighbourRegion = map.squares[wallRow - 1][wallCol + 1];
                neighbourRegionRep = map.getRegionRepFromState(neighborNorthEast);
            } else if (isBottomRightCorner && !map.isWall(neighborSouthEast)) {
                neighbourRegion = map.squares[wallRow + 1][wallCol + 1];
                neighbourRegionRep = map.getRegionRepFromState(neighborSouthEast);
            } else if (isBottomLeftCorner && !map.isWall(neighborSouthWest)) {
                neighbourRegion = map.squares[wallRow + 1][wallCol - 1];
                neighbourRegionRep = map.getRegionRepFromState(neighborSouthWest);
            }

            // TODO: deal with edge cases

            // Edge case

            if (isNorthEdge && hasNoOtherPointOfContact()) {

            } else if (isEastEdge && hasNoOtherPointOfContact()) {

            } else if (isSouthEdge && hasNoOtherPointOfContact()) {

            } else if (isWestEdge && hasNoOtherPointOfContact()) {

            }

            if (neighbourRegion == regionId) {
                throw new Exception("NeighbourRegion id calculation went wrong!");
            }

            if (neighbourRegionRep == -1) {
                throw new Exception("Region rep for region " + regionId + "does not exist!");
            }

            // Update region’s neighbourhood in groups map

            // Get the neighbours of the region
            HashSet<Integer> neighbours = groupRecord.getNeighborIds();

            neighbours.remove(neighbourRegion);

            // Update old neighbour’s neighbourhood in groups map

            // Get the neighbours of its soon-to-be ex-neighbor
            GroupRecord neighborRecord = groups.get(regionRep);
            HashSet<Integer> neighboursOfEx = neighborRecord.getNeighborIds();

            neighboursOfEx.remove(regionId);
        }

        // Region Partition case



        // Wall on Region Representative case

        if (wallLoc == regionRep) {

        }

        // Wall That Moves Region Representative case

        // Wall That Changes Shortest Path
    }

    /**
     * @param wallLoc   state id where wall will be removed
     * @param dbaStarBW DBAStar object returned from computeDBAStarDatabaseUsingSubgoalDynamicDB3
     */
    public void recomputeWallRemovalUsingSubgoalDynamicDB3(int wallLoc, DBAStar dbaStarBW) {

    }

    private boolean hasNoOtherPointOfContact() {
        // TODO: Implement
    }
}
