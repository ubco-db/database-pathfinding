package map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stats.SearchStats;

import java.util.*;

public class AbstractedMap extends GameMap {
    private final int gridSize;
    private final int numSectorsPerCol;
    private final int numSectorsPerRow;

    private int numRegions;

    // Region numbering inside states starts from 50. This is because '32' is used to mark an empty state and '42' is
    // used to mark a wall state.
    public final static int START_NUM = 50;

    private final Sector[] sectors;
    private final Map<Integer, Region> regionIdToRegionMap;
    private final int[] regionReps;

    private final Queue<Integer> freeRegionIds;

    private static final Logger logger = LogManager.getLogger(AbstractedMap.class);

    public AbstractedMap(GameMap gameMap, int gridSize, SearchStats searchStats) {
        // Copy GameMap data
        // Abstract map will have same number of rows and cols, but its own state array in which the open states will
        // have been assigned number corresponding to their region. This happens in abstractStatesToGenerateRegions.
        super(gameMap);

        long start, startTotalAbstraction = System.nanoTime();

        freeRegionIds = new PriorityQueue<>();

        // Store specified gridSize
        this.gridSize = gridSize;
        // Calculate how many sectors the map will be divided into
        this.numSectorsPerCol = (int) Math.ceil(getNumRows() * 1.0 / gridSize);
        this.numSectorsPerRow = (int) Math.ceil(getNumCols() * 1.0 / gridSize);
        int numSectors = numSectorsPerCol * numSectorsPerRow;

        this.sectors = new Sector[numSectors];
        this.regionIdToRegionMap = new TreeMap<>();

        // Abstract the states array (divide it into regions using BFS), also computes region representatives
        start = System.nanoTime();
        abstractStatesToGenerateRegions(searchStats);
        if (searchStats != null) {
            searchStats.setTimeToAbstractRegions(System.nanoTime() - start);
        }

        // TODO: Add to this
        freeRegionIds.add(numRegions + AbstractedMap.START_NUM);
        freeRegionIds.add(numRegions + AbstractedMap.START_NUM + 1);
        regionReps = new int[numRegions + freeRegionIds.size()];
        Arrays.fill(regionReps, -1);

        // Determine which regions are neighbours, store this information per region
        start = System.nanoTime();
        computeRegionNeighbourhoodAndStoreRegionReps();
        if (searchStats != null) {
            searchStats.setTimeToDetermineNeighbourhoods(System.nanoTime() - start);
            searchStats.setTotalAbstractionTime(System.nanoTime() - startTotalAbstraction);
        }
    }

    public AbstractedMap(GameMap gameMap, int gridSize) {
        this(gameMap, gridSize, null);
    }

    /**
     * This code iterates over the states array and uses BFS within each sector (sectors are gridSize x gridSize squares)
     * to compute regions and mark them inside the array (number them starting at START_NUM = 50)
     */
    private void abstractStatesToGenerateRegions(SearchStats searchStats) {
        // Iterate over states using BFS to assign them to regions
        int currentRegionNum = START_NUM - 1;
        int totalRegions = 0;

        int numStatesExpanded = 0;

        // Iterate over all sectors
        for (int sr = 0; sr < numSectorsPerCol; sr++) {
            int northRow = sr * gridSize;
            int southRow = sr * gridSize + gridSize;
            // Boundary checking
            if (southRow > getNumRows()) southRow = getNumRows();

            for (int sc = 0; sc < numSectorsPerRow; sc++) {
                int westCol = sc * gridSize;
                int eastCol = sc * gridSize + gridSize;
                // Boundary checking
                if (eastCol > getNumCols()) eastCol = getNumCols();

                int numRegionsInSector = 0;

                ArrayList<Region> regionsInSector = new ArrayList<>();

                // Iterate over states within the sector
                for (int r = northRow; r < southRow; r++) {
                    for (int c = westCol; c < eastCol; c++) {
                        // If the state is in bounds, not a wall, and not abstracted yet (= has not been assigned a regionId, is equal to EMPTY_CHAR)
                        if (isInBoundsAndOpenState(r, c)) {
                            currentRegionNum++;
                            numRegionsInSector++;

                            // Perform constrained BFS within Sector
                            Queue<Integer> stateIds = new LinkedList<>();
                            stateIds.add(super.getStateId(r, c));
                            this.states[r][c] = currentRegionNum;

                            int numStatesInRegion = 1;

                            while (!stateIds.isEmpty()) {
                                int stateId = stateIds.remove();
                                numStatesExpanded++;

                                // Iterate over neighbor states
                                for (int neighborStateId : getStateNeighbourIds(stateId)) {
                                    int nr = super.getRowFromStateId(neighborStateId);
                                    int nc = super.getColFromStateId(neighborStateId);

                                    if (super.isInRangeAndOpenState(nr, nc, northRow, westCol, southRow, eastCol)) {
                                        this.states[nr][nc] = currentRegionNum;
                                        stateIds.add(neighborStateId);
                                        numStatesInRegion++;
                                    }
                                }
                            }

                            // Make new region that stores its region number and the number of states contained in the region
                            Region region = new Region(currentRegionNum, numStatesInRegion);
                            // Compute the region representative for the region
                            computeRegionRepresentative(currentRegionNum, region, northRow, southRow, westCol, eastCol);
                            regionIdToRegionMap.put(currentRegionNum, region);
                            regionsInSector.add(region);
                        }
                    }
                }

                totalRegions += numRegionsInSector;

                // Store number of regions in current sector using sector id
                int sectorId = getSectorIdFromSectorRowAndCol(sr, sc);
                sectors[sectorId] = new Sector(sectorId, regionsInSector);
            }
        }

        if (searchStats != null) {
            searchStats.setNumStatesExpandedBFS(numStatesExpanded);
        }

        // Store total number of regions generated
        this.numRegions = totalRegions;
    }

    public int computeRegionRepresentative(int regionId, Region region, int startRow, int endRow, int startCol, int endCol) {
        int[] rows = new int[region.getNumStates()];
        int[] cols = new int[region.getNumStates()];

        int numStates = 0, sumRow = 0, sumCol = 0;
        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                // If the state is in the region
                if (super.getStateValue(r, c) == regionId) {
                    rows[numStates] = r;
                    cols[numStates] = c;
                    sumRow += r;
                    sumCol += c;
                    numStates++;
                }
            }
        }

        // Compute center of region
        int centroidRow = sumRow / numStates;
        int centroidCol = sumCol / numStates;

        // If the computed center is a wall or outside the region
        if (super.isWall(centroidRow, centroidCol) || this.states[centroidRow][centroidCol] != regionId) {
            // Find the point that is in the group that is closest
            int minDist = 10_000, minRow = -1, minCol = -1;
            // Iterate over row and column values inside the region
            for (int stateNum = 0; stateNum < numStates; stateNum++) {
                // If the distance between the computed region center and the current state is less than
                // the minimum distance, update the minimum distance
                int row = rows[stateNum];
                int col = cols[stateNum];
                int dist = super.getOctileDistance(row, col, centroidRow, centroidCol);
                if (dist < minDist) {
                    minRow = row;
                    minCol = col;
                    minDist = dist;
                }

            }
            centroidRow = minRow;
            centroidCol = minCol;
        }

        int regionRep = super.getStateId(centroidRow, centroidCol);

        region.setRegionRepresentative(regionRep);
        return regionRep;
    }

    private void computeRegionNeighbourhoodAndStoreRegionReps() {
        for (int r = 0; r < super.getNumRows(); r++) {
            for (int c = 0; c < super.getNumCols(); c++) {
                if (!isWall(r, c)) {
                    int regionId = this.getRegionIdFromMap(r, c);
                    Region region = regionIdToRegionMap.get(regionId);

                    // Doing this here because we needed numRegions to be set before we could initialize this array
                    regionReps[regionId - AbstractedMap.START_NUM] = region.getRegionRepresentative();

                    boolean isOpenNorth = false, isOpenEast = false, isOpenSouth = false, isOpenWest = false;

                    // If the neighbour state is not a wall and is in a different region, add it to the neighbour set

                    if (isInBoundsAndNotWall(r - 1, c) && getRegionIdFromMap(r - 1, c) != regionId) { // north
                        region.addNeighborId(getRegionIdFromMap(r - 1, c));
                        isOpenNorth = true;
                    }
                    if (isInBoundsAndNotWall(r, c + 1) && getRegionIdFromMap(r, c + 1) != regionId) { // east
                        region.addNeighborId(getRegionIdFromMap(r, c + 1));
                        isOpenEast = true;
                    }
                    if (isInBoundsAndNotWall(r + 1, c) && getRegionIdFromMap(r + 1, c) != regionId) { // south
                        region.addNeighborId(getRegionIdFromMap(r + 1, c));
                        isOpenSouth = true;
                    }
                    if (isInBoundsAndNotWall(r, c - 1) && getRegionIdFromMap(r, c - 1) != regionId) { // west
                        region.addNeighborId(getRegionIdFromMap(r, c - 1));
                        isOpenWest = true;
                    }

                    // Diagonal states are only open if the corresponding cardinal states are open
                    if ((isOpenNorth || isOpenEast) && isInBoundsAndNotWall(r - 1, c + 1) && getRegionIdFromMap(r - 1, c + 1) != regionId) { // north-east
                        region.addNeighborId(getRegionIdFromMap(r - 1, c + 1));
                    }
                    if ((isOpenSouth || isOpenEast) && isInBoundsAndNotWall(r + 1, c + 1) && getRegionIdFromMap(r + 1, c + 1) != regionId) { // south-east
                        region.addNeighborId(getRegionIdFromMap(r + 1, c + 1));
                    }
                    if ((isOpenSouth || isOpenWest) && isInBoundsAndNotWall(r + 1, c - 1) && getRegionIdFromMap(r + 1, c - 1) != regionId) { // south-west
                        region.addNeighborId(getRegionIdFromMap(r + 1, c - 1));
                    }
                    if ((isOpenNorth || isOpenWest) && isInBoundsAndNotWall(r - 1, c - 1) && getRegionIdFromMap(r - 1, c - 1) != regionId) { // north-west
                        region.addNeighborId(getRegionIdFromMap(r - 1, c - 1));
                    }
                }
            }
        }
    }

    public int getRegionIdFromMap(int row, int col) {
        return super.getStateValue(row, col);
    }

    public int getRegionIdFromMap(int stateId) {
        return super.getStateValue(super.getRowFromStateId(stateId), super.getColFromStateId(stateId));
    }

    public int getSectorId(int row, int col) {
        return row / gridSize * numSectorsPerRow + col / gridSize;
    }

    public int getSectorId(int stateId) {
        int row = super.getRowFromStateId(stateId);
        int col = super.getColFromStateId(stateId);
        return row / gridSize * numSectorsPerRow + col / gridSize;
    }

    private int getSectorIdFromSectorRowAndCol(int sr, int sc) {
        return sr * (int) Math.ceil(getNumCols() * 1.0 / gridSize) + sc;
    }

    public int getStartRowOfSector(int sectorId) {
        return (sectorId / this.numSectorsPerRow) * this.gridSize;
    }

    public int getStartColOfSector(int sectorId) {
        return (sectorId % this.numSectorsPerRow) * this.gridSize;
    }

    public int getEndRowOfSector(int sectorId) {
        return Math.min(getStartRowOfSector(sectorId) + this.gridSize, super.getNumRows());
    }

    public int getEndColOfSector(int sectorId) {
        return Math.min(getStartColOfSector(sectorId) + this.gridSize, super.getNumCols());
    }

    public Sector getSector(int sectorId) {
        return sectors[sectorId];
    }

    public Map<Integer, Region> getRegionIdToRegionMap() {
        return regionIdToRegionMap;
    }

    public int[] getRegionReps() {
        return regionReps;
    }

    public Region getRegionById(int regionId) {
        return regionIdToRegionMap.get(regionId);
    }

    public Region getRegionFromRepresentative(int repId) {
        int regionId = getRegionIdFromMap(repId);
        return regionIdToRegionMap.get(regionId);
    }

    public int getNumRegions() {
        return numRegions;
    }

    public void wipeSector(int northRow, int southRow, int westCol, int eastCol, Map<String, ?> cache) {
        Set<Integer> regionIds = new HashSet<>();

        // Reset sector - all open states are empty and no longer marked with their region ids
        for (int r = northRow; r < southRow; r++) {
            for (int c = westCol; c < eastCol; c++) {
                if (isAbstracted(r, c)) {
                    regionIds.add(this.states[r][c]);
                    this.states[r][c] = ' ';
                }
            }
        }
        // Decrement the number of regions
        decrementNumRegionsBy(regionIds.size());

        // For each region in the wiped sector, remove all of its neighbours
        for (int regionId : regionIds) {
            Region region = getRegionById(regionId);

            // Remove region ids from mapping
//            regionIdToRegionMap.put(regionId, null);
            regionIdToRegionMap.remove(regionId);

            // For each neighbour of a region in the wiped sector, remove the region in the wiped sector from its neighbours
            for (int neighbourId : region.getNeighborIds()) {
                Region neighbour = getRegionById(neighbourId);
                neighbour.deleteNeighborIds(regionIds);
                if (cache != null) {
                    removeCachedPath(neighbour.getRegionRepresentative(), region.getRegionRepresentative(), cache);
                }
            }
            // Erase region reps from array
            regionReps[regionId - AbstractedMap.START_NUM] = -1;
        }

        // Mark region ids as free
        freeRegionIds.addAll(regionIds);
    }

    private void removeCachedPath(int neighbourRep, int regionRep, Map<String, ?> cache) {
        cache.remove(neighbourRep + " " + regionRep);
        cache.remove(regionRep + " " + neighbourRep);
    }

    public void wipeSectorPartitionCase(int northRow, int southRow, int westCol, int eastCol, final int REGION_ID) {
        Set<Integer> regionIds = new HashSet<>();

        // Reset sector - all open states are empty and no longer marked with their region ids
        for (int r = northRow; r < southRow; r++) {
            for (int c = westCol; c < eastCol; c++) {
                if (!isWall(r, c) && getRegionIdFromMap(r, c) == REGION_ID) {
                    regionIds.add(this.states[r][c]);
                    this.states[r][c] = ' ';
                }
            }
        }
        // Decrement the number of regions
        decrementNumRegionsBy(regionIds.size());

        // For each region in the wiped sector, remove all of its neighbours
        for (int regionId : regionIds) {
            Region region = getRegionById(regionId);

            // Remove region ids from mapping
//            regionIdToRegionMap.put(regionId, null);
            regionIdToRegionMap.remove(regionId);

            // For each neighbour of a region in the wiped sector, remove the region in the wiped sector from its neighbours
            for (int neighbourId : region.getNeighborIds()) {
                Region neighbour = getRegionById(neighbourId);
                neighbour.deleteNeighborIds(regionIds);
            }
            // Erase region reps from array
            regionReps[regionId - AbstractedMap.START_NUM] = -1;
        }

        // Mark region ids as free
        freeRegionIds.addAll(regionIds);
    }

    public Set<Integer> wipeSectorMergeCase(int northRow, int southRow, int westCol, int eastCol, Set<Integer> neighbouringRegionsInSameSector) {
        Set<Integer> regionIds = new HashSet<>();

        // Reset sector - all open states are empty and no longer marked with their region ids
        for (int r = northRow; r < southRow; r++) {
            for (int c = westCol; c < eastCol; c++) {
                if (!isWall(r, c) && neighbouringRegionsInSameSector.contains(getStateValue(r, c))) {
                    regionIds.add(this.states[r][c]);
                    this.states[r][c] = ' ';
                }
            }
        }
        // Decrement the number of regions
        decrementNumRegionsBy(regionIds.size());

        // For each region in the wiped sector, remove all of its neighbours
        for (int regionId : regionIds) {
            Region region = getRegionById(regionId);

            // For each neighbour of a region in the wiped sector, remove the region in the wiped sector from its neighbours
            for (int neighbourId : region.getNeighborIds()) {
                Region neighbour = getRegionById(neighbourId);
                neighbour.deleteNeighborIds(regionIds);
            }
            // Erase region reps from array
            regionReps[regionId - AbstractedMap.START_NUM] = -1;
        }

        // Mark region ids as free
        freeRegionIds.addAll(regionIds);
        return regionIds;
    }

    /**
     * A state is abstracted if its value is neither wall nor open
     *
     * @param r row id of the state
     * @param c column id of the state
     * @return true if the state is abstracted, false otherwise
     */
    private boolean isAbstracted(int r, int c) {
        return !(isWall(r, c) || isOpenState(r, c));
    }

    public ArrayList<Integer> abstractStatesToGenerateRegions(int sectorId, int northRow, int southRow, int westCol, int eastCol) {
        ArrayList<Region> regionsInSector = new ArrayList<>();
        int numRegionsInSector = 0;
        int currentRegionNum;

        // Iterate over states within the sector
        for (int r = northRow; r < southRow; r++) {
            for (int c = westCol; c < eastCol; c++) {
                // If the state is in bounds, not a wall, and not abstracted yet (= has not been assigned a regionId, is equal to EMPTY_CHAR)
                if (isInBoundsAndOpenState(r, c)) {
                    currentRegionNum = getFreeRegionId();
                    numRegionsInSector++;

                    // Perform constrained BFS within Sector
                    Queue<Integer> stateIds = new LinkedList<>();
                    stateIds.add(super.getStateId(r, c));
                    this.states[r][c] = currentRegionNum;

                    int numStatesInRegion = 1;

                    while (!stateIds.isEmpty()) {
                        int stateId = stateIds.remove();
                        // Iterate over neighbor states
                        for (int neighborStateId : getStateNeighbourIds(stateId)) {
                            int nr = super.getRowFromStateId(neighborStateId);
                            int nc = super.getColFromStateId(neighborStateId);

                            if (super.isInRangeAndOpenState(nr, nc, northRow, westCol, southRow, eastCol)) {
                                this.states[nr][nc] = currentRegionNum;
                                stateIds.add(neighborStateId);
                                numStatesInRegion++;
                            }
                        }
                    }

                    // Make new region that stores its region number and the number of states contained in the region
                    Region region = new Region(currentRegionNum, numStatesInRegion);
                    // Compute the region representative for the region
                    computeRegionRepresentative(currentRegionNum, region, northRow, southRow, westCol, eastCol);
                    regionIdToRegionMap.put(currentRegionNum, region);
                    regionsInSector.add(region);
                }
            }
        }

        this.incrementNumRegionsBy(numRegionsInSector);

        // Store number of regions in current sector using sector id
        sectors[sectorId] = new Sector(sectorId, regionsInSector);

        // TODO: Find less heinous way to do this
        ArrayList<Integer> regionIds = new ArrayList<>();
        for (Region region : regionsInSector) {
            regionIds.add(region.getRegionId());
        }

        return regionIds;
    }

    public void computeRegionNeighbourhoodAndStoreRegionReps(int northRow, int southRow, int westCol, int eastCol) {
        for (int r = Math.max(northRow - 1, 0); r < Math.min(southRow + 1, super.getNumRows()); r++) {
            for (int c = Math.max(westCol - 1, 0); c < Math.min(eastCol + 1, super.getNumCols()); c++) {
                if (!isWall(r, c)) {
                    int regionId = this.getRegionIdFromMap(r, c);
                    Region region = regionIdToRegionMap.get(regionId);

                    regionReps[regionId - AbstractedMap.START_NUM] = region.getRegionRepresentative();

                    boolean isOpenNorth = false, isOpenEast = false, isOpenSouth = false, isOpenWest = false;

                    // If the neighbour state is not a wall and is in a different region, add it to the neighbour set

                    if (isInBoundsAndNotWall(r - 1, c) && getRegionIdFromMap(r - 1, c) != regionId) { // north
                        region.addNeighborId(getRegionIdFromMap(r - 1, c));
                        isOpenNorth = true;
                    }
                    if (isInBoundsAndNotWall(r, c + 1) && getRegionIdFromMap(r, c + 1) != regionId) { // east
                        region.addNeighborId(getRegionIdFromMap(r, c + 1));
                        isOpenEast = true;
                    }
                    if (isInBoundsAndNotWall(r + 1, c) && getRegionIdFromMap(r + 1, c) != regionId) { // south
                        region.addNeighborId(getRegionIdFromMap(r + 1, c));
                        isOpenSouth = true;
                    }
                    if (isInBoundsAndNotWall(r, c - 1) && getRegionIdFromMap(r, c - 1) != regionId) { // west
                        region.addNeighborId(getRegionIdFromMap(r, c - 1));
                        isOpenWest = true;
                    }

                    // Diagonal states are only open if the corresponding cardinal states are open
                    if ((isOpenNorth || isOpenEast) && isInBoundsAndNotWall(r - 1, c + 1) && getRegionIdFromMap(r - 1, c + 1) != regionId) { // north-east
                        region.addNeighborId(getRegionIdFromMap(r - 1, c + 1));
                    }
                    if ((isOpenSouth || isOpenEast) && isInBoundsAndNotWall(r + 1, c + 1) && getRegionIdFromMap(r + 1, c + 1) != regionId) { // south-east
                        region.addNeighborId(getRegionIdFromMap(r + 1, c + 1));
                    }
                    if ((isOpenSouth || isOpenWest) && isInBoundsAndNotWall(r + 1, c - 1) && getRegionIdFromMap(r + 1, c - 1) != regionId) { // south-west
                        region.addNeighborId(getRegionIdFromMap(r + 1, c - 1));
                    }
                    if ((isOpenNorth || isOpenWest) && isInBoundsAndNotWall(r - 1, c - 1) && getRegionIdFromMap(r - 1, c - 1) != regionId) { // north-west
                        region.addNeighborId(getRegionIdFromMap(r - 1, c - 1));
                    }
                }
            }
        }
    }

    private void decrementNumRegionsBy(int numRegions) {
        this.numRegions -= numRegions;
    }

    private void incrementNumRegionsBy(int numRegions) {
        this.numRegions += numRegions;
    }

    public int getRegionRepFromRegionId(int regionId) {
        return regionReps[regionId - START_NUM];
    }

    public void addRegion(int regionId, int regionRepresentative, int numStates) {
        numRegions++;
        regionIdToRegionMap.put(regionId, new Region(regionId, regionRepresentative, numStates));
    }

    public void addRegion(int regionId, int regionRepresentative, int numStates, Set<Integer> neighbourIds) {
        numRegions++;
        regionIdToRegionMap.put(regionId, new Region(regionId, regionRepresentative, numStates, neighbourIds));
    }

    public void removeRegion(int regionId) {
        numRegions--;
//        regionIdToRegionMap.put(regionId, null);
        regionIdToRegionMap.remove(regionId);
        setFreeRegionId(regionId);
    }

    public int getNeighbourN(int wallRow, int wallCol) {
        return getRegionIdFromMap(wallRow - 1, wallCol);
    }

    public int getNeighbourNE(int wallRow, int wallCol) {
        return getRegionIdFromMap(wallRow - 1, wallCol + 1);
    }

    public int getNeighbourE(int wallRow, int wallCol) {
        return getRegionIdFromMap(wallRow, wallCol + 1);
    }

    public int getNeighbourSE(int wallRow, int wallCol) {
        return getRegionIdFromMap(wallRow + 1, wallCol + 1);
    }

    public int getNeighbourS(int wallRow, int wallCol) {
        return getRegionIdFromMap(wallRow + 1, wallCol);
    }

    public int getNeighbourSW(int wallRow, int wallCol) {
        return getRegionIdFromMap(wallRow + 1, wallCol - 1);
    }

    public int getNeighbourW(int wallRow, int wallCol) {
        return getRegionIdFromMap(wallRow, wallCol - 1);
    }

    public int getNeighbourNW(int wallRow, int wallCol) {
        return getRegionIdFromMap(wallRow - 1, wallCol - 1);
    }

    public int getFreeRegionId() {
        return freeRegionIds.remove();
    }

    public void setFreeRegionId(int freeRegionId) {
        this.freeRegionIds.add(freeRegionId);
    }

    public void setState(int row, int col, int value) {
        states[row][col] = value;
    }

    public Queue<Integer> getFreeRegionIds() {
        return freeRegionIds;
    }

    public void getDirectNeighbourValues(int[] neighbourValues, int row, int col) {
        neighbourValues[0] = (row >= 1 && row < getNumRows()) && (col >= 1 && col < getNumCols()) ? states[row - 1][col - 1] : WALL_CHAR; // NW
        neighbourValues[1] = (row >= 1 && row < getNumRows()) && (col >= 0 && col < getNumCols()) ? states[row - 1][col] : WALL_CHAR; // N
        neighbourValues[2] = (row >= 1 && row < getNumRows()) && (col >= 0 && col < getNumCols() - 1) ? states[row - 1][col + 1] : WALL_CHAR; // NE

        neighbourValues[3] = (row >= 0 && row < getNumRows()) && (col >= 1 && col < getNumCols()) ? states[row][col - 1] : WALL_CHAR; // W
        neighbourValues[4] = (row >= 0 && row < getNumRows()) && (col >= 0 && col < getNumCols() - 1) ? states[row][col + 1] : WALL_CHAR; // E

        neighbourValues[5] = (row >= 0 && row < getNumRows() - 1) && (col >= 1 && col < getNumCols()) ? states[row + 1][col - 1] : WALL_CHAR; // SW
        neighbourValues[6] = (row >= 0 && row < getNumRows() - 1) && (col >= 0 && col < getNumCols()) ? states[row + 1][col] : WALL_CHAR; // S
        neighbourValues[7] = (row >= 0 && row < getNumRows() - 1) && (col >= 0 && col < getNumCols() - 1) ? states[row + 1][col + 1] : WALL_CHAR; // SE
    }

    public int getR1Sum(int[] neighbourValues) {
        int count = 0;
        for (int i = 0; i < 3; i++) {
            if (neighbourValues[i] == WALL_CHAR) {
                count++;
            }
        }
        return count;
    }

    public int getR2Sum(int[] neighbourValues) {
        int count = 1;
        for (int i = 3; i < 5; i++) {
            if (neighbourValues[i] == WALL_CHAR) {
                count++;
            }
        }
        return count;
    }

    public int getR3Sum(int[] neighbourValues) {
        int count = 0;
        for (int i = 5; i < 8; i++) {
            if (neighbourValues[i] == WALL_CHAR) {
                count++;
            }
        }
        return count;
    }

    public int getC1Sum(int[] neighbourValues) {
        int count = 0;
        if (neighbourValues[0] == WALL_CHAR) {
            count++;
        }
        if (neighbourValues[3] == WALL_CHAR) {
            count++;
        }
        if (neighbourValues[5] == WALL_CHAR) {
            count++;
        }
        return count;
    }

    public int getC2Sum(int[] neighbourValues) {
        int count = 1;
        if (neighbourValues[1] == WALL_CHAR) {
            count++;
        }
        if (neighbourValues[6] == WALL_CHAR) {
            count++;
        }
        return count;
    }

    public int getC3Sum(int[] neighbourValues) {
        int count = 0;
        if (neighbourValues[2] == WALL_CHAR) {
            count++;
        }
        if (neighbourValues[4] == WALL_CHAR) {
            count++;
        }
        if (neighbourValues[7] == WALL_CHAR) {
            count++;
        }
        return count;
    }
}
