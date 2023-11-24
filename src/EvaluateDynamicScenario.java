import comparison.DBDiff;
import database.DBStats;
import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDynamicDB2;
import dynamic.Walls;
import map.GameMap;
import map.GroupRecord;
import search.*;
import util.ExpandArray;

import java.io.IOException;
import java.util.*;

public class EvaluateDynamicScenario {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "DBA/";

    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;

    final static int CUTOFF = 250; // The maximum # of moves for hill-climbing checks.
    final static int GRID_SIZE = 16;
    final static int NUM_NEIGHBOUR_LEVELS = 1; // # of neighbor levels for HCDPS
    final static int START_NUM = 50;


    public static void main(String[] args) {
        // set wall(s)
        ArrayList<SearchState> wallLocation = new ArrayList<>();
        int wallLoc = 14325; // real region partition (14325) // fake partition (11928) // wall that partitions map (6157)
        SearchState wall = new SearchState(wallLoc);
        wallLocation.add(wall);

        // set start and goal
        int startId = 13411;
        int goalId = 13901;

        // build DBAStar Database
        GameMap startingMap = new GameMap(PATH_TO_MAP);
        DBAStar dbaStarBW = computeDBAStarDatabase(startingMap, "BW"); // BW = before wall
        // getDBAStarPath(startId, goalId, "BW", dbaStarBW);

        System.out.println();
        System.out.println();
        System.out.println();

        // Use the map returned after the database is fully computed
        GameMap map = dbaStarBW.getMap();

        boolean priorWall = map.isWall(wallLoc);

        // Add wall to existing map
        map.squares[map.getRow(wallLoc)][map.getCol(wallLoc)] = '*';
        // Make new MapSearchProblem (will use map with added wall)
        MapSearchProblem problem = new MapSearchProblem(map);

        if (!priorWall && map.isWall(wallLoc)) {
            System.out.println("Wall at " + wallLoc + " set successfully!");
        }

        // Get the id of the region rep of the region the wall was added in
        int regionRepId = map.getAbstractProblem().findRegionRep(wall, map).getId();
        System.out.println("regionRepId: " + regionRepId);

        if (regionRepId == wallLoc) {
            System.out.println("Wall on region rep!");
        }

        // Get the id of the region the wall was added in using its regionRepId
        TreeMap<Integer, GroupRecord> groups = new MapSearchProblem(map).getGroups(); // stores number of states per region as well, may be useful later

        Iterator<Map.Entry<Integer, GroupRecord>> it = groups.entrySet().iterator();
        Map.Entry<Integer, GroupRecord> elem;
        HashMap<Integer, Integer> regionRepIdToRegionId = new HashMap<>();
        while (it.hasNext()) {
            elem = it.next();
            regionRepIdToRegionId.put(elem.getValue().groupRepId, elem.getKey());
        }
        int regionId = regionRepIdToRegionId.get(regionRepId);
        System.out.println("regionId: " + regionId);

        // get the neighbour ids regions using the region id
        GroupRecord groupRecord = groups.get(regionId);

        // TODO: make better check, this is for setting array length in recomputeBasePaths2
        boolean isElimination;

        if (groupRecord.getNumStates() == 1) { // scenario when there is only one state in the region
            // need to tombstone region, and make sure it doesn't have neighbours or shortest paths anymore
            groups.remove(regionId);
            isElimination = true;
        } else {
            // scenario when the regioning doesn't change significantly (region id stays the same)
            int newRegionRep = map.recomputeCentroid(groupRecord, wallLoc);
            regionRepId = newRegionRep;
            System.out.println("New rep at: " + newRegionRep);
            // get back new region rep and change the record
            groupRecord.setGroupRepId(newRegionRep);
            groups.replace(regionId, groupRecord);
            isElimination = false;
        }

        // TODO: scenario where map is partitioned by wall addition

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
        int neighborNorthEast = map.squares[wallRowId - 1][wallColId + 1];
        int neighborEast = map.squares[wallRowId][wallColId + 1];
        int neighborSouthEast = map.squares[wallRowId + 1][wallColId + 1];
        int neighborSouth = map.squares[wallRowId + 1][wallColId];
        int neighborSouthWest = map.squares[wallRowId + 1][wallColId - 1];
        int neighborWest = map.squares[wallRowId][wallColId - 1];
        int neighborNorthWest = map.squares[wallRowId - 1][wallColId - 1];
        // if the region has become partitioned, it would have to have neighbors that are across from each other be walls or in different regions
        // (this is a necessary condition, but not sufficient)

        // In order for a partition to happen, either the newly placed wall touches two walls, or it touches a wall and
        // a state that is not in the region the wall was placed in (this is a necessary condition, but not sufficient)
        // TODO: is there an exception to this?

        if (isContinuousWall(neighborNorth, neighborSouth) || isBetweenWallAndOtherRegion(neighborNorth, neighborSouth, regionId)) {
            potentialVerticalPartition = true;
        }
        if (isContinuousWall(neighborWest, neighborEast) || isBetweenWallAndOtherRegion(neighborWest, neighborEast, regionId)) {
            potentialHorizontalPartition = true;
        }
        // TODO: address diagonal partition (maybe split into two cases?)
        if (isOpenDiagonal(neighborNorth, neighborNorthEast, neighborEast)
                || isOpenDiagonal(neighborEast, neighborSouthEast, neighborSouth)
                || isOpenDiagonal(neighborSouth, neighborSouthWest, neighborWest)
                || isOpenDiagonal(neighborWest, neighborNorthWest, neighborNorth)) {
            potentialDiagonalPartition = true;
        }

        System.out.println();
        System.out.println("WALL IS PARTITIONING MAP: " + (potentialHorizontalPartition || potentialVerticalPartition));
        if (potentialHorizontalPartition || potentialVerticalPartition)
            System.out.println(potentialHorizontalPartition ? "HORIZONTALLY" : "VERTICALLY");
        System.out.println();

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

        if (verticalPartition || horizontalPartition) {
            System.out.println("Group size before removal: " + groups.size());
            // TODO: set neighbours of new regions using this
            HashSet<Integer> neighboursOfOldRegion = groups.get(regionId).getNeighborIds();
            groups.remove(regionId); // remove region from groups and recreate it later
            System.out.println("Group size after removal: " + groups.size());

            // states in a groupRecord are in order, the first one is first in the region (top-left-most)
            int stateId = groupRecord.states.get(0);

            ExpandArray neighbors = new ExpandArray(10);

            int startRow = map.getRow(stateId); // 96
            int startCol = map.getCol(stateId); // 112
            int endRow = startRow + GRID_SIZE; // 112
            int endCol = startCol + GRID_SIZE; // 128

            // TODO: put this stuff into a method in GameMap
            int currentNum = -1;
            int numRegionsInSector = 0;

            // reset region (necessary in order for me to be able to reuse the regionId)
            for (int r = 0; r < GRID_SIZE; r++) {
                for (int c = 0; c < GRID_SIZE; c++) {
                    int row = startRow + r;
                    int col = startCol + c;
                    if (!map.isWall(row, col) && map.squares[row][col] == regionId) {
                        map.squares[row][col] = ' '; // 32
                    }
                }
            }

            // Boolean flag
            boolean firstTime = true;

            for (int r = 0; r < GRID_SIZE; r++) {
                // for each col in this sector
                for (int c = 0; c < GRID_SIZE; c++) {
                    int row = startRow + r; // pointer to a row
                    int col = startCol + c; // pointer to a col

                    // if this state is valid and isn't a wall and is in the region to be recomputed:
                    // open cell for abstraction - perform constrained BFS within this sector to label all nodes in sector
                    if (map.isValid(row, col) && !map.isWall(row, col) && map.squares[row][col] == ' ') {
                        currentNum++;
                        numRegionsInSector++;

                        // For first region: Use region id of region that was deleted
                        if (firstTime) {
                            currentNum = regionId;
                        }

                        Queue<Integer> stateIds = new LinkedList<>();
                        stateIds.add(map.getId(row, col));
                        map.squares[row][col] = currentNum;

                        while (!stateIds.isEmpty()) {
                            int id = stateIds.remove();
                            row = map.getRow(id); // Row of state
                            col = map.getCol(id); // Col of state

                            // Generate neighbors and add to list if in region
                            map.getNeighbors(row, col, neighbors);

                            // For number of neighbors
                            for (int n = 0; n < neighbors.num(); n++) {

                                int nid = neighbors.get(n); // ID of neighbor state
                                int nr = map.getRow(nid); // Row of that neighbor
                                int nc = map.getCol(nid); // Col of that neighbor

                                // Check if neighbor is in range
                                if (map.isOpenInRange(nr, nc, endRow, endCol, GRID_SIZE)) {
                                    // Add neighbor
                                    map.squares[nr][nc] = currentNum;
                                    stateIds.add(nid);
                                }
                            }
                        }

                        // After the first time, we want to assign region-ids at the end of the groups map
                        if (firstTime) {
                            currentNum = groups.size() + START_NUM;
                            firstTime = false;
                        }
                    }
                }
            }

            // TODO: missing code here?

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

            System.out.println("Group size after addition: " + groups.size());

            // Recompute region reps for newly added regions
            for (GroupRecord newRec: newRecs) {
                map.recomputeCentroid2(newRec, wallLoc);
            }

            // VISUAL CHECK: TODO: Centroids are different for some reason
            map.computeCentroidMap().outputImage(DBA_STAR_DB_PATH + "TEST" + MAP_FILE_NAME + ".png", null, null);

            // Rebuild abstract problem
            map.rebuildAbstractProblem(GRID_SIZE, startRow, startCol, groups);

            // TODO: Set neighbours properly
            map.recomputeNeighbors(startRow, startCol, endRow, endCol);
        }

        ArrayList<Integer> neighborIds = new ArrayList<>(groupRecord.getNeighborIds());
        neighborIds.add(groupRecord.groupId); // need to pass this so updates work both ways

        // Get database and initialize pathCompressAlgDba
        SubgoalDynamicDB2 dbBW = (SubgoalDynamicDB2) dbaStarBW.getDatabase();
        HillClimbing pathCompressAlgDba = new HillClimbing(problem, 10000);

        // Update regions for neighborIds in the database
        dbBW.recomputeBasePaths2(problem, groups, neighborIds, pathCompressAlgDba, dbBW.getLowestCost(), dbBW.getPaths(),
                dbBW.getNeighbor(), neighborIds.size(), NUM_NEIGHBOUR_LEVELS, isElimination);

        // TODO: Update db (need to update node id to seed id mapping, potentially map?)

        int[][] groupsArr = dbBW.getDb().getGroups();

        if (groupRecord.getNumStates() == 1) { // tombstone record
            groupsArr[regionId - START_NUM] = null;
            dbBW.getDb().setNumRegions(groupsArr.length - 1);
        } else { // update groupsArr
            groupsArr[regionId - START_NUM] = new int[]{regionId - START_NUM, regionRepId};
        }

        // write groupsArr back to db
        dbBW.getDb().setGroups(groupsArr);

        // For checking recomputed database against AW database
        // TODO: ArrayIndexOutOfBounds here
        dbBW.exportDB(DBA_STAR_DB_PATH + "BW_Recomp_" + MAP_FILE_NAME + "_DBA-STAR_G" + GRID_SIZE + "_N" + NUM_NEIGHBOUR_LEVELS + "_C" + CUTOFF + ".dat");

        System.out.println();
        System.out.println();
        System.out.println();

        // recompute database
        // try to only recompute changes, then recompute entire database to see if I matched it

        // add wall to starting map
        Walls.addWall(PATH_TO_MAP, wallLocation, startingMap);
        startingMap = new GameMap(PATH_TO_MAP);

        DBAStar dbaStarAW = computeDBAStarDatabase(startingMap, "AW"); // AW = after wall
        // getDBAStarPath(startId, goalId, "AW", dbaStarAW);

        // remove wall
        Walls.removeWall(PATH_TO_MAP, wallLocation, startingMap);

        // compare databases
        try {
            String f1Name = "BW012.map_DBA-STAR_G16_N1_C250.";
            String f2Name = "AW012.map_DBA-STAR_G16_N1_C250.";
            String ext = "dati2";
            DBDiff.getDBDiff(DBA_STAR_DB_PATH, wallLoc, f1Name, f2Name, ext);
            f1Name = "BW012.map_DBA-STAR_G16_N1_C250.";
            f2Name = "AW012.map_DBA-STAR_G16_N1_C250.";
            ext = "dat";
            DBDiff.getDBDiff(DBA_STAR_DB_PATH, wallLoc, f1Name, f2Name, ext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static DBAStar computeDBAStarDatabase(GameMap map, String wallStatus) {
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
        rec.addStat(0, "dbaStar (" + NUM_NEIGHBOUR_LEVELS + ")");
        rec.addStat(1, GRID_SIZE);
        rec.addStat(3, CUTOFF);
        rec.addStat(4, MAP_FILE_NAME);
        rec.addStat(5, map.rows);
        rec.addStat(6, map.cols);

        currentTime = System.currentTimeMillis();

        // This is where region reps and groups on the map are computed
        map = map.sectorAbstract2(GRID_SIZE);

        long resultTime = System.currentTimeMillis() - currentTime;

        rec.addStat(12, resultTime);
        rec.addStat(10, resultTime);
        rec.addStat(11, map.states);
        rec.addStat(7, map.states);
        dbStats.addRecord(rec);

        System.out.println("Exporting map with areas.");
        map.outputImage(getImageName(wallStatus, false), null, null);
        System.out.println("Exporting map with areas and centroids.");
        map.computeCentroidMap().outputImage(getImageName(wallStatus, true), null, null);

        // QUESTION: Why are we passing this tmpProb?
        SearchProblem tmpProb = new MapSearchProblem(map);
        GameDB gameDB = new GameDB(tmpProb);

        currentTime = System.currentTimeMillis();

        // TODO
        database.computeIndex(tmpProb, rec);

        rec.addStat(23, System.currentTimeMillis() - currentTime);

        System.out.println("Generating gameDB.");
        currentTime = System.currentTimeMillis();

        database = gameDB.computeDynamicDB(database, pathCompressAlgDba, rec, NUM_NEIGHBOUR_LEVELS);

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

        // return database here to access and modify
        return new DBAStar(problem, map, database);
    }

    private static void getDBAStarPath(int startId, int goalId, String wallStatus, DBAStar dbaStar) {
        SearchProblem problem = dbaStar.getProblem();
        GameMap map = dbaStar.getMap();

        AStar aStar = new AStar(problem);

        StatsRecord dbaStats = new StatsRecord();
        ArrayList<SearchState> path = dbaStar.computePath(new SearchState(startId), new SearchState(goalId), dbaStats);

        StatsRecord aStarStats = new StatsRecord();
        ArrayList<SearchState> optimalPath = aStar.computePath(new SearchState(startId), new SearchState(goalId), aStarStats);

        System.out.println("AStar path cost: " + aStarStats.getPathCost() + " DBAStar path cost: " + dbaStats.getPathCost());
        System.out.println("Suboptimality: " + ((((double) dbaStats.getPathCost()) / aStarStats.getPathCost()) - 1) * 100.0);

        if (path == null || path.isEmpty()) {
            System.out.printf("No path was found between %d and %d!%n", startId, goalId);
        }
        map.computeCentroidMap().outputImage(DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME + "_path.png", path, null);
        map.computeCentroidMap().outputImage(DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME + "_optimal_path.png", optimalPath, null);
    }

    /* Helper methods */

    private static String getDBName(String wallStatus) {
        return DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME + "_DBA-STAR_G" + GRID_SIZE + "_N" + NUM_NEIGHBOUR_LEVELS + "_C" + CUTOFF + ".dat";
    }

    private static String getImageName(String wallStatus, boolean hasCentroids) {
        String lastToken = hasCentroids ? "_DBA_Centroid.png" : "_DBA.png";
        return DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME + lastToken;
    }

    private static boolean isContinuousWall(int n1, int n2) {
        // if n1 and n2 is are walls, adding a wall between them leads to a continuous wall segment
        // this assumes n1 and n2 are across from each other
        return n1 == '*' && n2 == '*';
    }

    private static boolean isBetweenWallAndOtherRegion(int n1, int n2, int r) {
        // if n1 is a wall and n2 is a different region or vice versa, the wall addition between them may constitute a partition
        // this assumes n1 and n2 are across from each other
        return n1 == '*' && n2 != r || n1 != r && n2 == '*';
    }

    private static boolean isOpenDiagonal(int n1, int nDiag, int n2) {
        // if n1 and n2 are walls, but nDiag is not, placing a new wall may make it impossible to reach nDiag
        return n1 == '*' && n2 == '*' && nDiag != '*';
    }

    private static boolean isValid(int[][] map, boolean[][] visited, int row, int col, int r) {
        int rows = map.length;
        int cols = map[0].length;

        // checking map[row][col] == r to ensure we are still in the region
        return row >= 0 && row < rows && col >= 0 && col < cols && map[row][col] != '*' && map[row][col] == r && !visited[row][col];
    }

    // TODO: could likely optimize using A*
    private static boolean isPathPossible(int[][] map, int[] start, int[] end, int r) {
        int rows = map.length;
        int cols = map[0].length;

        boolean[][] visited = new boolean[rows][cols];

        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {-1, 1}, {1, -1}, {1, 1}, {-1, -1}};

        Queue<int[]> queue = new LinkedList<>();
        queue.add(start);
        visited[start[0]][start[1]] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();

            if (Arrays.equals(current, end)) {
                return true;  // Path exists
            }

            for (int[] dir : directions) {
                int newRow = current[0] + dir[0];
                int newCol = current[1] + dir[1];

                if (isValid(map, visited, newRow, newCol, r)) {
                    visited[newRow][newCol] = true;
                    queue.add(new int[]{newRow, newCol});
                }
            }
        }

        return false;  // No path exists
    }
}
