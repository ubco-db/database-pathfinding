package map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

public class GameMap {
    public static final char WALL_CHAR = '*';
    public static final char EMPTY_CHAR = ' ';

    private final int numRows, numCols;
    private int numOpenStates;
    final int[][] states;

    final String name;

    private static final Logger logger = LogManager.getLogger(GameMap.class);

    public GameMap(String fileName) {
        try (Scanner scanner = new Scanner(new File(fileName))) {
            // Drop first line (contains format information, e.g. type octile)
            scanner.nextLine();

            String line;
            line = scanner.nextLine();
            // Get number of rows (e.g. height 139)
            this.numRows = Integer.parseInt(line.substring(7).trim());
            line = scanner.nextLine();
            // Get number of columns (e.g. width 148)
            this.numCols = Integer.parseInt(line.substring(6).trim());

            // Drop line (just says map)
            scanner.nextLine();

            // 2D array representing game map
            states = new int[this.numRows][this.numCols];
            // Number of open (i.e. traversable) states on the map
            numOpenStates = 0;

            for (int r = 0; r < this.numRows; r++) {
                line = scanner.nextLine();
                for (int c = 0; c < this.numCols; c++) {
                    // All of these characters are considered to be non-traversable (i.e. walls)
                    if (line.charAt(c) == '@' || line.charAt(c) == 'O' || line.charAt(c) == 'W' || line.charAt(c) == 'T') {
                        states[r][c] = WALL_CHAR;
                    } else {
                        states[r][c] = EMPTY_CHAR;
                        numOpenStates++;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("File {} not found", fileName);
            throw new RuntimeException(e);
        }

        this.name = fileName;
    }

    public GameMap(int[][] states) {
        this.numRows = states.length;
        this.numCols = states[0].length;
        this.states = states;
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                if (!isWall(r, c)) {
                    numOpenStates++;
                }
            }
        }

        this.name = "Map from states";
    }

    /**
     * Constructor used for VisualGameMap
     */
    protected GameMap(int numRows, int numCols) {
        this.numRows = numRows;
        this.numCols = numCols;
        // states is a 2D array of AtomicIntegers in VisualGameMap, so the int array here is useless
        this.states = null;

        this.name = "Visual Map";
    }

    /**
     * Copy constructor to make abstract maps
     *
     * @param gameMap the GameMap object to copy
     */
    public GameMap(GameMap gameMap) {
        this.numRows = gameMap.numRows;
        this.numCols = gameMap.numCols;
        // This field won't matter for abstract maps
        this.numOpenStates = gameMap.numOpenStates;

        this.states = new int[numRows][numCols];
        for (int r = 0; r < getNumRows(); r++) {
            for (int c = 0; c < getNumCols(); c++) {
                if (gameMap.isOpenState(r, c)) {
                    states[r][c] = GameMap.EMPTY_CHAR;
                } else {
                    states[r][c] = WALL_CHAR;
                }
            }
        }

        this.name = gameMap.name;
    }

    public int getNumRows() {
        return numRows;
    }

    public int getNumCols() {
        return numCols;
    }

    public int getNumOpenStates() {
        return numOpenStates;
    }

    public int getStateId(int row, int col) {
        return row * this.numCols + col;
    }

    public int getRowFromStateId(int stateId) {
        return stateId / this.numCols;
    }

    public int getColFromStateId(int stateId) {
        return stateId % this.numCols;
    }

    public int getStateValue(int row, int col) {
        return isInBounds(row, col) ? states[row][col] : GameMap.WALL_CHAR;
    }

    public int getStateValue(int sid) {
        return getStateValue(getRowFromStateId(sid), getColFromStateId(sid));
    }

    public void setStateValue(int sid, int value) {
        setStateValue(getRowFromStateId(sid), getColFromStateId(sid), value);
    }

    public void setStateValue(int row, int col, int value) {
        states[row][col] = value;
    }

    public void placeWallAt(int sid) {
        int row = getRowFromStateId(sid);
        int col = getColFromStateId(sid);

        numOpenStates--;

        if (states[row][col] == WALL_CHAR) {
            throw new RuntimeException("There is a wall at " + sid + " (" + row + ", " + col + ") already!");
        } else {
            setStateValue(row, col, WALL_CHAR);
        }
    }

    public void placeOpenStateAt(int sid) {
        int row = getRowFromStateId(sid);
        int col = getColFromStateId(sid);

        numOpenStates++;

        if (states[row][col] == WALL_CHAR) {
            setStateValue(row, col, EMPTY_CHAR);
        } else {
            throw new RuntimeException("There is an open state at " + sid + " (" + row + ", " + col + ") already!");
        }
    }

    public boolean isInBounds(int row, int col) {
        return col >= 0 && col < this.numCols && row >= 0 && row < this.numRows;
    }

    public boolean isOutOfBounds(int row, int col) {
        return col < 0 || col >= this.numCols || row < 0 || row >= this.numRows;
    }

    public boolean isOpenState(int row, int col) {
        return getStateValue(row, col) == EMPTY_CHAR;
    }

    public boolean isInBoundsAndOpenState(int row, int col) {
        return isInBounds(row, col) && isOpenState(row, col);
    }

    public boolean isInRangeAndOpenState(int row, int col, int minRow, int minCol, int maxRow, int maxCol) {
        return isOpenState(row, col) && row < maxRow && minRow <= row && minCol <= col && col < maxCol;
    }

    public boolean isWall(int row, int col) {
        return states[row][col] == WALL_CHAR;
    }

    public boolean isWall(int sid) {
        int row = getRowFromStateId(sid);
        int col = getColFromStateId(sid);
        return isWall(row, col);
    }

    /**
     * Not the same as isOpenState for abstracted maps
     *
     * @param row the row to check
     * @param col the column to check
     * @return whether the state described by (row, col) is in bounds
     */
    public boolean isInBoundsAndNotWall(int row, int col) {
        return isInBounds(row, col) && !isWall(row, col);
    }

    public boolean isOutOfBoundsOrWall(int row, int col) {
        return isOutOfBounds(row, col) || isWall(row, col);
    }

    public boolean isInRangeAndNotWall(int row, int col, int minRow, int minCol, int maxRow, int maxCol) {
        return !isWall(row, col) && row < maxRow && minRow <= row && minCol <= col && col < maxCol;
    }

    public boolean isInRange(int row, int col, int minRow, int minCol, int maxRow, int maxCol) {
        return row < maxRow && minRow <= row && minCol <= col && col < maxCol;
    }

    public List<Integer> getStateNeighbourIds(int currentId) {
        return getStateNeighbourIds(getRowFromStateId(currentId), getColFromStateId(currentId));
    }

    public void getStateNeighbourIds(int currentId, List<Integer> neighbourIds) {
        getStateNeighbourIds(getRowFromStateId(currentId), getColFromStateId(currentId), neighbourIds);
    }

    public List<Integer> getStateNeighbourIds(int row, int col) {
        List<Integer> neighbours = new ArrayList<>(8);
        getStateNeighbourIds(row, col, neighbours);
        return neighbours;
    }

    public void getStateNeighbourIds(int row, int col, List<Integer> neighbourIds) {
        boolean isOpenNorth = false, isOpenEast = false, isOpenSouth = false, isOpenWest = false;

        if (isInBoundsAndNotWall(row - 1, col)) { // north
            neighbourIds.add(getStateId(row - 1, col));
            isOpenNorth = true;
        }
        if (isInBoundsAndNotWall(row, col + 1)) { // east
            neighbourIds.add(getStateId(row, col + 1));
            isOpenEast = true;
        }
        if (isInBoundsAndNotWall(row + 1, col)) { // south
            neighbourIds.add(getStateId(row + 1, col));
            isOpenSouth = true;
        }
        if (isInBoundsAndNotWall(row, col - 1)) { // west
            neighbourIds.add(getStateId(row, col - 1));
            isOpenWest = true;
        }

        // Diagonal states are only open if the corresponding cardinal states are open
        if ((isOpenNorth || isOpenEast) && isInBoundsAndNotWall(row - 1, col + 1)) { // north-east
            neighbourIds.add(getStateId(row - 1, col + 1));
        }
        if ((isOpenSouth || isOpenEast) && isInBoundsAndNotWall(row + 1, col + 1)) { // south-east
            neighbourIds.add(getStateId(row + 1, col + 1));
        }
        if ((isOpenSouth || isOpenWest) && isInBoundsAndNotWall(row + 1, col - 1)) { // south-west
            neighbourIds.add(getStateId(row + 1, col - 1));
        }
        if ((isOpenNorth || isOpenWest) && isInBoundsAndNotWall(row - 1, col - 1)) { // north-west
            neighbourIds.add(getStateId(row - 1, col - 1));
        }
    }

    public int getStateNeighbourIds(int currentId, int[] neighbourIds) {
        int i = 0;
        boolean isOpenNorth = false, isOpenEast = false, isOpenSouth = false, isOpenWest = false;
        int row = getRowFromStateId(currentId);
        int col = getColFromStateId(currentId);

        if (isInBoundsAndNotWall(row - 1, col)) { // north
            neighbourIds[i++] = getStateId(row - 1, col);
            isOpenNorth = true;
        }
        if (isInBoundsAndNotWall(row, col + 1)) { // east
            neighbourIds[i++] = getStateId(row, col + 1);
            isOpenEast = true;
        }
        if (isInBoundsAndNotWall(row + 1, col)) { // south
            neighbourIds[i++] = getStateId(row + 1, col);
            isOpenSouth = true;
        }
        if (isInBoundsAndNotWall(row, col - 1)) { // west
            neighbourIds[i++] = getStateId(row, col - 1);
            isOpenWest = true;
        }

        // Diagonal states are only open if the corresponding cardinal states are open
        if ((isOpenNorth || isOpenEast) && isInBoundsAndNotWall(row - 1, col + 1)) { // north-east
            neighbourIds[i++] = getStateId(row - 1, col + 1);
        }
        if ((isOpenSouth || isOpenEast) && isInBoundsAndNotWall(row + 1, col + 1)) { // south-east
            neighbourIds[i++] = getStateId(row + 1, col + 1);
        }
        if ((isOpenSouth || isOpenWest) && isInBoundsAndNotWall(row + 1, col - 1)) { // south-west
            neighbourIds[i++] = getStateId(row + 1, col - 1);
        }
        if ((isOpenNorth || isOpenWest) && isInBoundsAndNotWall(row - 1, col - 1)) { // north-west
            neighbourIds[i++] = getStateId(row - 1, col - 1);
        }

        return i;
    }

    public int getStateNeighbourIds(int currentId, int[] neighbourIds, HashSet<Integer> closedSet) {
        int i = 0;
        boolean isOpenNorth = false, isOpenEast = false, isOpenSouth = false, isOpenWest = false;
        int row = getRowFromStateId(currentId);
        int col = getColFromStateId(currentId);

        if (isInBoundsAndNotWall(row - 1, col) && !closedSet.contains(getStateId(row - 1, col))) { // north
            neighbourIds[i++] = getStateId(row - 1, col);
            isOpenNorth = true;
        }
        if (isInBoundsAndNotWall(row, col + 1) && !closedSet.contains(getStateId(row, col + 1))) { // east
            neighbourIds[i++] = getStateId(row, col + 1);
            isOpenEast = true;
        }
        if (isInBoundsAndNotWall(row + 1, col) && !closedSet.contains(getStateId(row + 1, col))) { // south
            neighbourIds[i++] = getStateId(row + 1, col);
            isOpenSouth = true;
        }
        if (isInBoundsAndNotWall(row, col - 1) && !closedSet.contains(getStateId(row, col - 1))) { // west
            neighbourIds[i++] = getStateId(row, col - 1);
            isOpenWest = true;
        }

        // Diagonal states are only open if the corresponding cardinal states are open
        if ((isOpenNorth || isOpenEast) && isInBoundsAndNotWall(row - 1, col + 1) && !closedSet.contains(getStateId(row - 1, col + 1))) { // north-east
            neighbourIds[i++] = getStateId(row - 1, col + 1);
        }
        if ((isOpenSouth || isOpenEast) && isInBoundsAndNotWall(row + 1, col + 1) && !closedSet.contains(getStateId(row + 1, col + 1))) { // south-east
            neighbourIds[i++] = getStateId(row + 1, col + 1);
        }
        if ((isOpenSouth || isOpenWest) && isInBoundsAndNotWall(row + 1, col - 1) && !closedSet.contains(getStateId(row + 1, col - 1))) { // south-west
            neighbourIds[i++] = getStateId(row + 1, col - 1);
        }
        if ((isOpenNorth || isOpenWest) && isInBoundsAndNotWall(row - 1, col - 1) && !closedSet.contains(getStateId(row - 1, col - 1))) { // north-west
            neighbourIds[i++] = getStateId(row - 1, col - 1);
        }

        return i;
    }

    public int getOctileDistance(int startId, int goalId) {
        int startRow = startId / numCols;
        int goalRow = goalId / numCols;
        int diffRow = startRow - goalRow;

        // Compute its absolute value
        int bit31 = diffRow >> 31;
        diffRow = (diffRow ^ bit31) - bit31;

        int diffCol = ((startId - startRow * numCols) - (goalId - goalRow * numCols));

        // Compute its absolute value
        bit31 = diffCol >> 31;
        diffCol = (diffCol ^ bit31) - bit31;

        return 10 * (diffRow + diffCol) - 6 * Math.min(diffRow, diffCol);
    }

    public int getOctileDistance(int startRow, int startCol, int goalRow, int goalCol) {
        int diffRow = startRow - goalRow;
        int diffCol = startCol - goalCol;

        // Compute its absolute value
        int bit31 = diffRow >> 31;
        diffRow = (diffRow ^ bit31) - bit31;

        // Compute its absolute value
        bit31 = diffCol >> 31;
        diffCol = (diffCol ^ bit31) - bit31;

        return 10 * (diffRow + diffCol) - 6 * Math.min(diffRow, diffCol);
    }

    public void printStates() {
        for (int r = 0; r < this.numRows; r++) {
            for (int c = 0; c < this.numCols; c++) {
                System.out.print(states[r][c] + " ");
            }
            System.out.println();
        }
    }

    public String getName() {
        return name;
    }
}
