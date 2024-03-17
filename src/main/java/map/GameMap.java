package map;

import comparison.ChangedPath;
import database.DBStatsRecord;
import database.SubgoalDynamicDB3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.SavedSearch;
import search.SearchAbstractAlgorithm;
import search.SearchState;
import search.StatsRecord;
import util.CircularQueue;
import util.ExpandArray;
import util.HeuristicFunction;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Queue;
import java.util.Map.Entry;


/**
 * A data structure for a 2D map.
 * Maps can be read from files and saved back to disk.
 * Maps can also be displayed on the screen.
 */
public class GameMap {
    public static final int EDGECOST_CARDINAL = 10;
    public static final int EDGECOST_DIAGONAL = 14;
    public static final int START_NUM = 50;
    public static final char WALL_CHAR = '*';
    public static final char EMPTY_CHAR = ' ';

    private final int PANEL_HEIGHT = 950;
    private final int PANEL_WIDTH = 950;
    private final int HEIGHT_OFFSET = 30;
    private int cellHeight;
    private int cellWidth;

    public int rows, cols;
    public int[][] squares;
    public HashMap<Integer, Color> colors;
    public int states;

    public Point startPoint;
    public Point goalPoint;
    public ArrayList<SearchState> path;

    private Random generator;

    private ArrayList<MapMask> masks;
    private int currentMask = -1;

    private int numGroups;
    private GroupRecord[] groups;                     // A faster lookup mechanism

    private int gridSize;

    private int numSectorsPerRow;

    private int[] numRegions;                         // Number of regions in each sector (only used for sector abstraction)

    private static final Logger logger = LogManager.getLogger(GameMap.class);

    public GameMap() {
    }
    public GameMap(int r, int c) {
        rows = r;
        cols = c;
        squares = new int[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                squares[i][j] = ' ';

        mapInit();
    }

    public GameMap(GameMap map, HashMap<Integer, Integer> visitedStates) {
        rows = map.rows;
        cols = map.cols;
        squares = new int[rows][cols];
        int numEmpty = 0;
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++) {
                squares[i][j] = map.squares[i][j];
                if (!isWall(i, j)) numEmpty++;
            }

        mapInit();

        // Determine total visited states and total count
        Iterator<Entry<Integer, Integer>> it = visitedStates.entrySet().iterator();
        int num = 0, count = 0, max = 0;
        while (it.hasNext()) {
            Entry<Integer, Integer> et = it.next();
            int visits = et.getValue();
            num++;
            count += visits;
            if (visits > max) max = visits;
        }
        int step = max / 10;
        if (step == 0) step = 1;
        logger.debug("Number of states visited: " + num + " # empty states: " + numEmpty + " Total visits: " + count + " Max visits: " + max + " Step size: " + step);

        // Now copy over values for the visited states to map (base is 100)
        it = visitedStates.entrySet().iterator();
        int base = 100;
        while (it.hasNext()) {
            Entry<Integer, Integer> et = it.next();
            int stateId = et.getKey();
            int visits = et.getValue();
            int val = (visits / step) * step;
            if (val >= step * 10) val = step * 9;
            val = base + val;
            squares[getRow(stateId)][getCol(stateId)] = val;
            // System.out.println("Updated state row: "+getRow(stateId)+" Col: "+getCol(stateId)+" to: "+val);
        }

        // Define heat colors
        colors.put(base, Color.BLUE);
        colors.put(base + step, Color.BLUE);
        colors.put(base + step * 2, Color.GREEN);
        colors.put(base + step * 3, Color.GREEN);
        colors.put(base + step * 4, Color.YELLOW);
        colors.put(base + step * 5, Color.YELLOW);
        colors.put(base + step * 6, Color.ORANGE);
        colors.put(base + step * 7, Color.ORANGE);
        colors.put(base + step * 8, Color.RED);
        colors.put(base + step * 9, Color.RED);
    }

    public GameMap(String fileName, int gridSize) {    // Loads a map in Vadim's format into data structure
        this.gridSize = gridSize;
        this.numSectorsPerRow = (int) Math.ceil(this.cols * 1.0 / this.gridSize);
        load(fileName);
        generator = new Random();
        generator.setSeed(56256902);
    }

    public void load(String fileName) {
        try (Scanner sc = new Scanner(new File(fileName))) {

            String st = sc.nextLine();     // Drop first line which is formatted
            if (!st.contains("type")) {    // Map is in binary format
                sc.close();
                this.loadMap(fileName);
                return;
            }

            st = sc.nextLine();            // Number of rows. e.g. height 139
            rows = Integer.parseInt(st.substring(7).trim());
            st = sc.nextLine();            // Number of cols. e.g. width 148
            cols = Integer.parseInt(st.substring(6).trim());
            sc.nextLine();                  // Just says map
            squares = new int[rows][cols];
            mapInit();
            states = 0;

            for (int i = 0; i < rows; i++) {
                st = sc.nextLine();
                for (int j = 0; j < cols; j++) {
                    if (st.charAt(j) == '@' || st.charAt(j) == 'O' || st.charAt(j) == 'W' || st.charAt(j) == 'T')
                        squares[i][j] = WALL_CHAR;
                    else {
                        squares[i][j] = EMPTY_CHAR;
                        states++;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("Did not find input file: " + e);
        }
    }

    /**
     * This version loads a number-based map.
     *
     * @param fileName
     */
    public void loadMap(String fileName) {
        try (Scanner sc = new Scanner(new File(fileName))) {

            String st = sc.nextLine();      // Number of rows. e.g. height 139
            rows = Integer.parseInt(st.substring(7).trim());
            st = sc.nextLine();            // Number of cols. e.g. width 148
            cols = Integer.parseInt(st.substring(6).trim());
            squares = new int[rows][cols];
            mapInit();
            states = 0;

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    squares[i][j] = sc.nextInt();
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("Did not find input file: " + e);
        } catch (Exception e) {
            logger.error("IO Error: " + e);
        }
    }

    public static int computeDistance(int startId, int goalId, int ncols, HeuristicFunction heuristic) {
        return heuristic.apply(startId, goalId, ncols);
    }


    // Computes octile distance (quick estimate with no square root) from this state to goal state
    public static int computeDistance(int startRow, int startCol, int goalRow, int goalCol) {
        int diffRow = (goalRow - startRow);
        if (diffRow < 0) diffRow = startRow - goalRow;
        int diffCol = (goalCol - startCol);
        if (diffCol < 0) diffCol = startCol - goalCol;

        if (diffRow > diffCol) return diffCol * EDGECOST_DIAGONAL + (diffRow - diffCol) * EDGECOST_CARDINAL;
        else return diffRow * EDGECOST_DIAGONAL + (diffCol - diffRow) * EDGECOST_CARDINAL;
    }

    // Computes octile distance (quick estimate with no square root) from this state to goal state
    public static int computeDistance(int startId, int goalId, int ncols) {
//        int startRow = startId / ncols;
//        int goalRow = goalId / ncols;
//        int diffRow = startRow - goalRow;
//
//        int bit31 = diffRow >> 31;                // Compute its absolute value
//        diffRow = (diffRow ^ bit31) - bit31;
//
//        int diffCol = ((startId - startRow * ncols) - (goalId - goalRow * ncols));
//        bit31 = diffCol >> 31;                     // Compute its absolute value
//        diffCol = (diffCol ^ bit31) - bit31;
//
//        // return Math.abs(diffRow) *10 + Math.abs(diffCol)*10;
//        return Math.abs(diffRow) + Math.abs(diffCol);

        // Note: This version uses diagonals
        int startRow = startId / ncols;
        int goalRow = goalId / ncols;
        int diffRow = startRow - goalRow;

        int bit31 = diffRow >> 31;                // Compute its absolute value
        diffRow = (diffRow ^ bit31) - bit31;

        int diffCol = ((startId - startRow * ncols) - (goalId - goalRow * ncols));
        bit31 = diffCol >> 31;                    // Compute its absolute value
        diffCol = (diffCol ^ bit31) - bit31;

        return Math.min(diffRow, diffCol) * EDGECOST_DIAGONAL + ((diffRow + diffCol) - 2 * Math.min(diffRow, diffCol)) * EDGECOST_CARDINAL;
    }

    public TreeMap<Integer, GroupRecord> getGroups() {
        if (groups == null) {    // Compute groups
            computeGroups();
        }
        return groups;
    }

    public void mapInit() {
        colors = new HashMap<Integer, Color>();
        states = rows * cols;
        generator = new Random();
        generator.setSeed(56256902);
        masks = new ArrayList<MapMask>();
        cellHeight = PANEL_HEIGHT / rows;
        cellWidth = PANEL_WIDTH / cols;
        if (cellHeight <= 0) cellHeight = 1;
        if (cellWidth <= 0) cellWidth = 1;
        if (cellHeight > cellWidth) {
            cellHeight = cellWidth;
        } else cellWidth = cellHeight;
    }

    // Rotate 90 degrees
    public void rotate() {
        int curRows = rows, curCols = cols;

        int[][] vals = new int[curCols][curRows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                vals[j][rows - 1 - i] = this.squares[i][j];
            }
        }
        this.squares = vals;
        this.rows = curCols;
        this.cols = curRows;
    }

    public void save(String fileName) {
        try (PrintWriter out = new PrintWriter(fileName)) {
            out.println("height " + rows);
            out.println("width " + cols);

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    out.print(this.squares[i][j]);
                    out.print("\t");
                }
                out.println();
            }
        } catch (FileNotFoundException e) {
            logger.error("Error with output file: " + e);
        }
    }

    public void print() {
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                System.out.print((char) squares[i][j]);
        System.out.println();
    }

    public boolean isWall(int r, int c) {
        return squares[r][c] == WALL_CHAR;
    }

    public boolean isWall(int id) {
        return squares[getRow(id)][getCol(id)] == WALL_CHAR;
    }

    // Display the coverage as a mask on the current map
    // 0-20 - red, 21-49 - orange, 50-80 - yellow, 81-99 - blue 100 - green
    public SparseMask createCoverageMask(byte[][] coverage) {
        SparseMask currentMask = new SparseMask();
        Color col;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!isWall(r, c)) {    // Determine all cells that are reachable from this one via database entries
                    int val = coverage[r][c];
                    if (val <= 20) col = Color.red;
                    else if (val <= 49) col = Color.orange;
                    else if (val <= 80) col = Color.yellow;
                    else if (val <= 99) col = Color.blue;
                    else col = Color.green;
                    ChangeRecord rec = new ChangeRecord(r, c, col, 1);
                    currentMask.add(rec);
                }
            }
        }
        return currentMask;
    }

    public boolean isValid(int r, int c) {
        return (c >= 0 && r >= 0 && r < rows && c < cols);
    }

    public boolean isValid(int id) {
        return (getCol(id) >= 0 && getRow(id) >= 0 && getRow(id) < rows && getCol(id) < cols);
    }

    /**
     * Returns true if cell is valid and is unassigned.
     */
    public boolean isOpenCell(int r, int c) {
        return (c >= 0 && r >= 0 && r < rows && c < cols && squares[r][c] == EMPTY_CHAR);
    }

    public boolean isOpenInRange(int r, int c, int maxR, int maxC) {
        return (c >= maxC - gridSize && r >= maxR - gridSize && r < maxR && c < maxC && squares[r][c] == EMPTY_CHAR);
    }

    public boolean isRegionInRange(int r, int c, int maxR, int maxC, int regionId) {
        return (c >= maxC - gridSize && r >= maxR - gridSize && r < maxR && c < maxC && squares[r][c] == regionId);
    }

    public boolean isInRange(int r, int c, int maxR, int maxC) {
        return (c >= maxC - gridSize && r >= maxR - gridSize && r < maxR && c < maxC);
    }

    public GameMap copyMap() {
        GameMap result = new GameMap(this.rows, this.cols);
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                if (isWall(i, j)) result.squares[i][j] = this.squares[i][j];
                else result.squares[i][j] = EMPTY_CHAR;

        return result;
    }

    public GameMap copyEntireMap() {
        GameMap result = new GameMap(this.rows, this.cols);
        for (int i = 0; i < rows; i++)
            if (cols >= 0) System.arraycopy(this.squares[i], 0, result.squares[i], 0, cols);

        // Copy all colors
        result.colors.putAll(this.colors);
        return result;
    }

    // Assumes first source val is at row and col and expands out.  Assumes all with same sourceval are reachable from this starting point.
    public void copyMatrixNew(int row, int col, GameMap source, GameMap dest, int sourceval, int destval) {
        Stack<MapPoint> currentSet = new Stack<>();
        HashMap<Integer, Integer> visited = new HashMap<>();
        currentSet.add(new MapPoint(row, col));
        int count = 0;
        while (!currentSet.isEmpty()) {
            MapPoint curPoint = currentSet.pop();
            int id = getId(curPoint.getRow(), curPoint.getCol());
            if (visited.containsKey(id)) continue;

            visited.put(id, null);
            count++;
            // Expand in all directions
            int r = curPoint.getRow();
            int c = curPoint.getCol();

            if (source.squares[r][c] == sourceval)
                // Add check to make sure square is not already assigned
                if (dest.squares[r][c] == ' ') dest.squares[r][c] = destval;

            if (source.isValid(r - 1, c) && !source.isWall(r - 1, c))
                if (source.squares[r - 1][c] == sourceval)    // Above
                    currentSet.add(new MapPoint(r - 1, c));

            if (source.isValid(r - 1, c + 1) && !source.isWall(r - 1, c + 1))
                if (source.squares[r - 1][c + 1] == sourceval) // Top right
                    currentSet.add(new MapPoint(r - 1, c + 1));

            if (source.isValid(r, c + 1) && !source.isWall(r, c + 1))
                if (source.squares[r][c + 1] == sourceval) // Right
                    currentSet.add(new MapPoint(r, c + 1));

            if (source.isValid(r + 1, c + 1) && !source.isWall(r + 1, c + 1))
                if (source.squares[r + 1][c + 1] == sourceval) // Bottom right
                    currentSet.add(new MapPoint(r + 1, c + 1));

            if (source.isValid(r + 1, c) && !source.isWall(r + 1, c))
                if (source.squares[r + 1][c] == sourceval) // Bottom
                    currentSet.add(new MapPoint(r + 1, c));

            if (source.isValid(r + 1, c - 1) && !source.isWall(r + 1, c - 1))
                if (squares[r + 1][c - 1] == sourceval) // Bottom left
                    currentSet.add(new MapPoint(r + 1, c - 1));

            if (source.isValid(r, c - 1) && !source.isWall(r, c - 1))
                if (source.squares[r][c - 1] == sourceval) // Left
                    currentSet.add(new MapPoint(r, c - 1));

            if (source.isValid(r - 1, c - 1) && !source.isWall(r - 1, c - 1))
                if (source.squares[r - 1][c - 1] == sourceval) // Top left
                    currentSet.add(new MapPoint(r - 1, c - 1));
        }

        // System.out.println("Num: "+sourceval+" Copied: "+count);
    }

    /*
     * Returns the set of all neighbors that border any of the cells with value val.
     */
    public Set<Integer> findAllNeighborsOrg(int val) {
        Set<Integer> result = new HashSet<>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (squares[r][c] == val) {    // Check all eight neighbours
                    if (isValid(r - 1, c) && !isWall(r - 1, c) && squares[r - 1][c] != val)    // Above
                        result.add(squares[r - 1][c]);
                    if (isValid(r - 1, c + 1) && !isWall(r - 1, c + 1) && squares[r - 1][c + 1] != val) // Top right
                        result.add(squares[r - 1][c + 1]);
                    if (isValid(r, c + 1) && !isWall(r, c + 1) && squares[r][c + 1] != val) // Right
                        result.add(squares[r][c + 1]);
                    if (isValid(r + 1, c + 1) && !isWall(r + 1, c + 1) && squares[r + 1][c + 1] != val) // Bottom right
                        result.add(squares[r + 1][c + 1]);
                    if (isValid(r + 1, c) && !isWall(r + 1, c) && squares[r + 1][c] != val) // Bottom
                        result.add(squares[r + 1][c]);
                    if (isValid(r + 1, c - 1) && !isWall(r + 1, c - 1) && squares[r + 1][c - 1] != val) // Bottom left
                        result.add(squares[r + 1][c - 1]);
                    if (isValid(r, c - 1) && !isWall(r, c - 1) && squares[r][c - 1] != val) // Left
                        result.add(squares[r][c - 1]);
                    if (isValid(r - 1, c - 1) && !isWall(r - 1, c - 1) && squares[r - 1][c - 1] != val) // Top left
                        result.add(squares[r - 1][c - 1]);

                }
        return result;
    }

    public GameMap neighbourCondense() {    // Condenses a group into another group if it has a unique neighbor
        GameMap baseMap = this.copyMap();

        baseMap.states = this.states;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (baseMap.squares[r][c] == EMPTY_CHAR) {    // Determine value of this cell in other map
                    int val = this.squares[r][c];
                    // Find ids of all cells in the other map with this value
                    Set<Integer> neighborSet = findAllNeighborsOrg(val);

                    if (neighborSet.size() == 1) {    // Move this set into neighbor
                        int neighborVal = neighborSet.iterator().next();
                        copyMatrixNew(r, c, this, baseMap, val, neighborVal);
                        baseMap.states--;
                    } else {    // Just copy over
                        copyMatrixNew(r, c, this, baseMap, val, val);
                    }
                }
            }
        }
        return baseMap;

    }

    public void addMask(SparseMask mask) {
        masks.add(mask);
    }

    /**
     * This version allows a state to expand to another one as long as it is HC-reachable from this state.
     * It will only keep a state though if it that state is closer to it from all the other seeds.
     */
    private GroupRecord expandSpot2(int seedRow, int seedCol, int currentNum, GameMap baseMap, SearchAbstractAlgorithm searchAlg, SavedSearch database, SavedSearch currentSet) {
        // BitSet currentSet = new BitSet(rows*cols);				// TODO: This is a major memory consumer - but clearing seems to be slower?  True?
        CircularQueue expandSet = new CircularQueue(1000);

        GroupRecord group = new GroupRecord();

        // Pick a color and assign group id to start square
        currentNum++;
        Color color = new Color(generator.nextFloat(), generator.nextFloat(), generator.nextFloat());
        baseMap.colors.put(currentNum, color);
        int currentCh = 49 + currentNum;
        baseMap.squares[seedRow][seedCol] = currentCh;
        int curPos = 0;
        group.groupId = currentCh;
        int id, seedId = baseMap.getId(seedRow, seedCol);
        group.setGroupRepId(seedId);

        StatsRecord stats = new StatsRecord();
        // currentSet.set(seedId);
        currentSet.clear();
        currentSet.setFound(seedId, 0);
        expandSet.insert(seedId);

        while ((id = expandSet.remove()) != -1) {
            int r, startRow = baseMap.getRow(id);
            int startCol = baseMap.getCol(id);

            // Expand in all directions
            for (int i = 1; i <= 3; i++) {
                if (i == 1) r = -1;
                else if (i == 2) r = 1;
                else r = 0;

                for (int c = -1; c <= 1; c++) {
                    if (r != 0 || c != 0) {
                        int row = r + startRow;
                        int col = c + startCol;
                        id = baseMap.getId(row, col);

                        // if (isOpenCell(row, col) && !currentSet.get(id))
                        if (isOpenCell(row, col) && !currentSet.isFound(id)) {        // Mutual reachable check below

							/*
							// This code works for no search database
							if (!searchAlg.isPath(id, seedId, stats))
								continue;
							if (!searchAlg.isPath(seedId, id, stats))
								continue;
							*/

                            int cost1 = searchAlg.isPath(id, seedId, stats, database);
                            if (cost1 == -1) continue;
                            // Do not check if can HC the other way - handle that case in search method
                            if (!searchAlg.isPath(seedId, id, stats))    // This is performing the two-way check.
                                continue;
                            if (cost1 > 0)
                                database.setFound(id, cost1);        // Store cost to get to the goal from this cell

                            // expandSet.insert(id);	// This would expand always whether or not the cell is closer.
                            // currentSet.set(id);			// Potentially better solution but at much higher abstraction cost.
                            if (baseMap.squares[row][col] == GameMap.EMPTY_CHAR || baseMap.isCloser2(row, col, seedRow, seedCol))        // Only change owner if new one is closer
                            {
                                baseMap.squares[row][col] = currentCh;
                                expandSet.insert(id);        // Only expand if closer
                                // currentSet.set(id);
                                currentSet.setFound(id, 0);
                            }
                        }
                    }
                }
            }

            curPos++;
        }
        return group;
    }


    private static final ArrayList<SearchState> result = new ArrayList<SearchState>(8);
    private static final HashMap<Integer, SearchState> createdStates = new HashMap<>();
    public static Integer[] ints;

    static {
        int size = 6000000;

        // int size = 10000000; //For Map2

        ints = new Integer[size];
        for (int i = 0; i < size; i++)
            ints[i] = i;
    }

    private static SearchState getState(int id) {    // Integer i = new Integer(id);
        Integer i = ints[id];
        SearchState s = createdStates.get(i);
        if (s == null) {
            s = new SearchState(id);
            createdStates.put(i, s);
        }
        return s;
    }

    /*
     * Original version with more flexible diagonal moves.
     */
    public ArrayList<SearchState> getNeighbors(int r, int c) {
        // 8-way pathfinding
        result.clear();
        if (isValid(r - 1, c - 1) && !isWall(r - 1, c - 1)) // Top left
            result.add(getState(this.getId(r - 1, c - 1)));
        if (isValid(r - 1, c) && !isWall(r - 1, c))    // Above
            result.add(getState(this.getId(r - 1, c)));
        if (isValid(r - 1, c + 1) && !isWall(r - 1, c + 1)) // Top right
            result.add(getState(this.getId(r - 1, c + 1)));
        if (isValid(r + 1, c - 1) && !isWall(r + 1, c - 1)) // Bottom left
            result.add(getState(this.getId(r + 1, c - 1)));
        if (isValid(r + 1, c) && !isWall(r + 1, c))// Bottom
            result.add(getState(this.getId(r + 1, c)));
        if (isValid(r + 1, c + 1) && !isWall(r + 1, c + 1)) // Bottom right
            result.add(getState(this.getId(r + 1, c + 1)));
        if (isValid(r, c - 1) && !isWall(r, c - 1)) // Left
            result.add(getState(this.getId(r, c - 1)));
        if (isValid(r, c + 1) && !isWall(r, c + 1)) // Right
            result.add(getState(this.getId(r, c + 1)));
        return result;
        // 4-way pathfinding
//        result.clear();
//        if (isValid(r - 1, c) && !isWall(r - 1, c))    // Above
//            result.add(getState(this.getId(r - 1, c)));
//        if (isValid(r + 1, c) && !isWall(r + 1, c))// Bottom
//            result.add(getState(this.getId(r + 1, c)));
//        if (isValid(r, c - 1) && !isWall(r, c - 1)) // Left
//            result.add(getState(this.getId(r, c - 1)));
//        if (isValid(r, c + 1) && !isWall(r, c + 1)) // Right
//            result.add(getState(this.getId(r, c + 1)));
//        return result;
    }

    public int[] getNeighborIds(int r, int c) {
        int[] result = new int[8];
        if (isValid(r - 1, c - 1) && !isWall(r - 1, c - 1)) // Top left
            result[0] = this.getId(r - 1, c - 1);
        if (isValid(r - 1, c) && !isWall(r - 1, c)) // Above
            result[1] = this.getId(r - 1, c);
        if (isValid(r - 1, c + 1) && !isWall(r - 1, c + 1)) // Top right
            result[2] = this.getId(r - 1, c + 1);
        if (isValid(r + 1, c - 1) && !isWall(r + 1, c - 1)) // Bottom left
            result[3] = this.getId(r + 1, c - 1);
        if (isValid(r + 1, c) && !isWall(r + 1, c)) // Bottom
            result[4] = this.getId(r + 1, c);
        if (isValid(r + 1, c + 1) && !isWall(r + 1, c + 1)) // Bottom right
            result[5] = this.getId(r + 1, c + 1);
        if (isValid(r, c - 1) && !isWall(r, c - 1)) // Left
            result[6] = this.getId(r, c - 1);
        if (isValid(r, c + 1) && !isWall(r, c + 1)) // Right
            result[7] = this.getId(r, c + 1);
        return result;
    }

    // This version always diagonal traversal even if just have opening on diagonal but not in two cardinal directions.
    public void getNeighbors(int r, int c, ExpandArray result) {
        result.clear();
        if (isValid(r - 1, c - 1) && !isWall(r - 1, c - 1)) // Top left
            result.add(this.getId(r - 1, c - 1));
        if (isValid(r - 1, c) && !isWall(r - 1, c))    // Above
            result.add(this.getId(r - 1, c));
        if (isValid(r - 1, c + 1) && !isWall(r - 1, c + 1)) // Top right
            result.add(this.getId(r - 1, c + 1));
        if (isValid(r + 1, c - 1) && !isWall(r + 1, c - 1)) // Bottom left
            result.add(this.getId(r + 1, c - 1));
        if (isValid(r + 1, c) && !isWall(r + 1, c))// Bottom
            result.add(this.getId(r + 1, c));
        if (isValid(r + 1, c + 1) && !isWall(r + 1, c + 1)) // Bottom right
            result.add(this.getId(r + 1, c + 1));
        if (isValid(r, c - 1) && !isWall(r, c - 1)) // Left
            result.add(this.getId(r, c - 1));
        if (isValid(r, c + 1) && !isWall(r, c + 1)) // Right
            result.add(this.getId(r, c + 1));
    }

    public void computeNeighbors() {    // Only computes the neighbor group ids for each group not the list of neighbor cells
        // IDEA: Perform one pass through map updating group records everytime encounter new neighbor

        // Create a neighbor set for each group
        for (Entry<Integer, GroupRecord> integerGroupRecordEntry : groups.entrySet()) {
            GroupRecord rec = integerGroupRecordEntry.getValue();
            rec.setNeighborIds(new HashSet<Integer>());
        }

        long currentTime = System.currentTimeMillis();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!isWall(r, c)) {
                    int val = squares[r][c];
                    GroupRecord rec = groups.get(val);
                    if (rec == null) {
                        logger.warn("Unable to find group: " + val + " for row: " + r + " col: " + c + " id: " + getId(r, c));
                        continue;
                    }
                    if (isValid(r - 1, c) && !isWall(r - 1, c) && squares[r - 1][c] != val)    // Above
                        rec.getNeighborIds().add(squares[r - 1][c]);
                    if (isValid(r - 1, c + 1) && !isWall(r - 1, c + 1) && squares[r - 1][c + 1] != val) // Top right
                        rec.getNeighborIds().add(squares[r - 1][c + 1]);
                    if (isValid(r, c + 1) && !isWall(r, c + 1) && squares[r][c + 1] != val) // Right
                        rec.getNeighborIds().add(squares[r][c + 1]);
                    if (isValid(r + 1, c + 1) && !isWall(r + 1, c + 1) && squares[r + 1][c + 1] != val) // Bottom right
                        rec.getNeighborIds().add(squares[r + 1][c + 1]);
                    if (isValid(r + 1, c) && !isWall(r + 1, c) && squares[r + 1][c] != val) // Bottom
                        rec.getNeighborIds().add(squares[r + 1][c]);
                    if (isValid(r + 1, c - 1) && !isWall(r + 1, c - 1) && squares[r + 1][c - 1] != val) // Bottom left
                        rec.getNeighborIds().add(squares[r + 1][c - 1]);
                    if (isValid(r, c - 1) && !isWall(r, c - 1) && squares[r][c - 1] != val) // Left
                        rec.getNeighborIds().add(squares[r][c - 1]);
                    if (isValid(r - 1, c - 1) && !isWall(r - 1, c - 1) && squares[r - 1][c - 1] != val) // Top left
                        rec.getNeighborIds().add(squares[r - 1][c - 1]);
                }
            }
        }
        long endTime = System.currentTimeMillis();
        logger.debug("Time to compute neighbors: " + (endTime - currentTime));
    }

    // TODO: recompute neighbours in more efficient way, should be possible since I know neighbours of original region and ids of new regions
    public void recomputeNeighbors(int startRow, int startCol, int endRow, int endCol, ArrayList<Integer> neighborIds) {    // Only computes the neighbor group ids for each group not the list of neighbor cells
        // IDEA: Perform one pass through map updating group records everytime encounter new neighbor

        // Create a neighbor set for each group that needs recomputation (original region and all its neighbours)
        for (int neighborId : neighborIds) {
            groups.get(neighborId).setNeighborIds(new HashSet<>());
        }

        // Need to recompute neighbours for original region and all its neighbours, so passing entire 3 sector x 3 sector area
        // TODO: am I actually doing this correctly?

        long currentTime = System.currentTimeMillis();

        // Make sure I am in bounds
        int startOuterLoop = Math.max(startRow - gridSize, 0);
        int endOuterLoop = Math.min(endRow + gridSize, rows);
        int startInnerLoop = Math.max(startCol - gridSize, 0);
        int endInnerLoop = Math.min(endCol + gridSize, cols);

        for (int r = startOuterLoop; r < endOuterLoop; r++) {
            for (int c = startInnerLoop; c < endInnerLoop; c++) {
                if (!isWall(r, c)) {
                    int val = squares[r][c];
                    GroupRecord rec = groups.get(val);
                    if (rec == null) {
                        logger.warn("Unable to find group: " + val + " for row: " + r + " col: " + c + " id: " + getId(r, c));
                        continue;
                    }
                    if (isValid(r - 1, c) && !isWall(r - 1, c) && squares[r - 1][c] != val)    // Above
                        rec.getNeighborIds().add(squares[r - 1][c]);
                    if (isValid(r - 1, c + 1) && !isWall(r - 1, c + 1) && squares[r - 1][c + 1] != val) // Top right
                        rec.getNeighborIds().add(squares[r - 1][c + 1]);
                    if (isValid(r, c + 1) && !isWall(r, c + 1) && squares[r][c + 1] != val) // Right
                        rec.getNeighborIds().add(squares[r][c + 1]);
                    if (isValid(r + 1, c + 1) && !isWall(r + 1, c + 1) && squares[r + 1][c + 1] != val) // Bottom right
                        rec.getNeighborIds().add(squares[r + 1][c + 1]);
                    if (isValid(r + 1, c) && !isWall(r + 1, c) && squares[r + 1][c] != val) // Bottom
                        rec.getNeighborIds().add(squares[r + 1][c]);
                    if (isValid(r + 1, c - 1) && !isWall(r + 1, c - 1) && squares[r + 1][c - 1] != val) // Bottom left
                        rec.getNeighborIds().add(squares[r + 1][c - 1]);
                    if (isValid(r, c - 1) && !isWall(r, c - 1) && squares[r][c - 1] != val) // Left
                        rec.getNeighborIds().add(squares[r][c - 1]);
                    if (isValid(r - 1, c - 1) && !isWall(r - 1, c - 1) && squares[r - 1][c - 1] != val) // Top left
                        rec.getNeighborIds().add(squares[r - 1][c - 1]);
                }
            }
        }
        long endTime = System.currentTimeMillis();
        logger.debug("Time to recompute neighbors: " + (endTime - currentTime));
    }

    public int generateRandomState() {
        int r, c;
        do {
            r = generator.nextInt(rows);
            c = generator.nextInt(cols);
        } while (this.isWall(r, c));

        return getId(r, c);
    }

    // Creates a new map where each centroid point of a group is white.
    public GameMap computeCentroidMap() {
        GameMap newMap = copyEntireMap();
        for (Entry<Integer, GroupRecord> integerGroupRecordEntry : groups.entrySet()) {
            GroupRecord rec = integerGroupRecordEntry.getValue();
            if (rec != null) {
                newMap.squares[getRow(rec.getGroupRepId())][getCol(rec.getGroupRepId())] = 32;
            }
        }
        return newMap;
    }


	// This version has no cutoff and inserts random seeds.
    public GameMap reachableAbstract(SearchAbstractAlgorithm searchAlg, DBStatsRecord dbstat) {
        // Idea: Keep adding squares to current one until no longer all elements are reachable in greedy fashion
        int currentNum = 0;
        GameMap baseMap = this.copyMap();
        SavedSearch currentSet = new SavedSearch(65536);
        GroupRecord group;
        long totalSize = 0;

        groups = new TreeMap<Integer, GroupRecord>();

        long currentTime = System.currentTimeMillis();

        // Phase 1: Just do 10,000 random points
        int numSeeds = 10000;
        for (int i = 0; i < numSeeds; i++) {
            int r = generator.nextInt(rows);
            int c = generator.nextInt(cols);

            if (!baseMap.isWall(r, c) && baseMap.squares[r][c] == EMPTY_CHAR) {
                group = expandSpot2(r, c, currentNum, baseMap, searchAlg, null, currentSet);
                // groups.put(group.groupId, group);
                addGroup(group.groupId, group);
                totalSize += group.getNumStates();
                currentNum++;
            }
        }

        // Phase 2: Handle any other points not previously found.
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (!baseMap.isWall(i, j) && baseMap.squares[i][j] == EMPTY_CHAR) {
                    group = expandSpot2(i, j, currentNum, baseMap, searchAlg, null, currentSet);
                    // groups.put(group.groupId, group);
                    totalSize += group.getNumStates();
                    addGroup(group.groupId, group);
                    currentNum++;
                }
            }
        }

        baseMap.states = currentNum;
        logger.debug("Number of areas: " + currentNum + " Average area size: " + (totalSize * 1.0 / currentNum));
        long endTime = System.currentTimeMillis();
        logger.debug("Time to compute map abstraction: " + (endTime - currentTime));
        dbstat.addStat(12, endTime - currentTime);
        dbstat.addStat(11, currentNum);
        dbstat.addStat(7, currentNum);
        baseMap.groups = groups;

        return baseMap;
    }

    public void addGroup(int id, GroupRecord group) {
        groups.put(id, group);
        // Also add it to groups arrays
        id -= GameMap.START_NUM;
        if (groupsArray == null) {
            int size = group.groupId;
            if (size < 100) size = 100;
            groupsArray = new GroupRecord[size];
        } else if (groupsArray.length - 1 < id) {    // Resize array
            int size = id * 2;
            GroupRecord[] tmp = new GroupRecord[size];
            System.arraycopy(groupsArray, 0, tmp, 0, groupsArray.length);
            groupsArray = tmp;
        }
        logger.debug("Groups array after adding: " + Arrays.toString(groupsArray));
        groupsArray[id] = group;
    }


    /**
     * This code iterates over the map and uses BFS within each sector (sectors are gridSize x gridSize squares) to
     * compute regions and mark them on the baseMap (number them starting at START_NUM = 50)
     *
     * @return GameMap where squares has had numbers assigned to open spots to demarcate regions
     */
    public GameMap sectorAbstract2() throws Exception {
        int currentNum = START_NUM - 1;    // Start # above 42 which is a wall
        GameMap baseMap = this.copyMap();
        int maxr, maxc;
        int numSectors = (int) (Math.ceil(rows * 1.0 / gridSize) * Math.ceil(cols * 1.0 / gridSize));

        baseMap.numRegions = new int[numSectors];
        int totalRegions = 0;
        ExpandArray neighbors = new ExpandArray(10);

        // For # of rows of sectors
        for (int i = 0; i < (int) Math.ceil(rows * 1.0 / gridSize); i++) {
            int startRow = i * gridSize; // top row of current sector
            maxr = i * gridSize + gridSize; // bottom row current sector
            if (maxr > this.rows) maxr = rows; // if bottom of the sector is outside the map

            // For # of cols of sectors
            for (int j = 0; j < (int) Math.ceil(cols * 1.0 / gridSize); j++) {
                int startCol = j * gridSize; // first col of current sector
                maxc = j * gridSize + gridSize; // last col of current sector
                if (maxc > this.cols) maxc = cols; // if last col of the sector is outside the map

                int numRegionsInSector = 0;
                for (int r = 0; r < gridSize; r++) { // for each row in this sector
                    for (int c = 0; c < gridSize; c++) { // for each col in this sector
                        int row = startRow + r; // pointer to a row
                        int col = startCol + c; // pointer to a col

                        // If this state is valid and isn't a wall and is ' '
                        if (baseMap.isValid(row, col) && !baseMap.isWall(row, col) && baseMap.squares[row][col] == ' ') {
                            // Open cell for abstraction - perform constrained BFS within this sector to label all nodes in sector
                            currentNum++;
                            numRegionsInSector++;

                            Queue<Integer> stateIds = new LinkedList<>();
                            stateIds.add(baseMap.getId(row, col));
                            baseMap.squares[row][col] = currentNum;

                            while (!stateIds.isEmpty()) {
                                int id = stateIds.remove();
                                row = baseMap.getRow(id); //Row of state
                                col = baseMap.getCol(id); //Col of state

                                // Generate neighbors and add to list if in region
                                baseMap.getNeighbors(row, col, neighbors);

                                // For number of neighbors
                                for (int n = 0; n < neighbors.num(); n++) {

                                    int nid = neighbors.get(n); // ID of neighbor state
                                    int nr = baseMap.getRow(nid); // Row of that neighbor
                                    int nc = baseMap.getCol(nid); // Col of that neighbor

                                    // Check if neighbor is in range
                                    if (baseMap.isOpenInRange(nr, nc, maxr, maxc)) {
                                        // Add neighbor
                                        baseMap.squares[nr][nc] = currentNum;
                                        stateIds.add(nid);
                                    }
                                }
                            }

                            // Put in new group
                            Color color = new Color(generator.nextFloat(), generator.nextFloat(), generator.nextFloat());
                            baseMap.colors.put(currentNum, color);
                        }
                    }
                }
                totalRegions += numRegionsInSector; // Increment region count
                baseMap.numRegions[i * (int) Math.ceil(cols * 1.0 / gridSize) + j] = numRegionsInSector;
            }
        }

        baseMap.states = totalRegions;
        logger.debug("Number of areas: " + (baseMap.states));

        baseMap.computeGroups();
        return baseMap;
    }

    public int sectorReAbstractWithFreeSpace(int startRow, int startCol, int endRow, int endCol, int regionId, GameMap map, SubgoalDynamicDB3 dbBW) throws Exception {

        if (regionId == 32) {
            throw new Exception("Error in sectorReAbstract2: Region id must not equal 32 (empty space)");
        }

        int currentNum = -1;
        int numSectors = (int) (Math.ceil(rows * 1.0 / gridSize) * Math.ceil(cols * 1.0 / gridSize));

        map.numRegions = Arrays.copyOf(map.numRegions, numSectors);

        int totalRegions = map.states;
        int numRegionsInSector = 0;

        ExpandArray neighbors = new ExpandArray(10);

        // If bottom of the grid exceeds the map
        if (endRow > this.rows) endRow = rows;

        // If last col of the grid exceeds the map
        if (endCol > this.cols) endCol = cols;

        for (int r = 0; r < gridSize; r++) {
            // For each col in this sector
            for (int c = 0; c < gridSize; c++) {
                int row = startRow + r; // pointer to a row
                int col = startCol + c; // pointer to a col

                // if this state is valid and isn't a wall and is in the region to be recomputed:
                // open cell for abstraction - perform constrained BFS within this sector to label all nodes in sector
                if (map.isValid(row, col) && !map.isWall(row, col) && map.squares[row][col] == ' ') {
                    // grab next free spot from the database
                    currentNum = dbBW.popFreeSpace();
                    numRegionsInSector++;

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
                            if (map.isOpenInRange(nr, nc, endRow, endCol)) {
                                // Add neighbor
                                map.squares[nr][nc] = currentNum;
                                stateIds.add(nid);
                            }
                        }
                    }
                }
            }

            // Put in new group
            Color color = new Color(generator.nextFloat(), generator.nextFloat(), generator.nextFloat());
            map.colors.put(currentNum, color);
        }
        totalRegions += numRegionsInSector; // Increment region count
        // TODO: check this works properly now
        map.numRegions[(startRow / gridSize) * (int) Math.ceil(cols * 1.0 / gridSize) + (endRow / gridSize)] = numRegionsInSector;

        map.states = totalRegions;
        logger.debug("Number of areas: " + (map.states));

        return numRegionsInSector;
    }

    // Draws a map on the screen
    public void draw(Graphics2D g2) {
        // Make sure everything is square
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Color col;
                int val = this.squares[i][j];
                // Just draw points for speed?
                if (val == WALL_CHAR) g2.setColor(Color.BLACK);
                else if (val == EMPTY_CHAR) g2.setColor(Color.WHITE);
                else {    // Lookup color used for this number
                    col = colors.get(val);
                    if (col == null) {    // TODO: How to pick color.  Picking a random color for now.
                        // Should not go in here with map is pre-colored.
                        // System.out.println("Here in for value: "+val);
                        col = new Color(generator.nextFloat(), generator.nextFloat(), generator.nextFloat());
                        colors.put(val, col);
                    }
                    g2.setColor(col);
                }
                g2.fillRect(j * cellWidth, i * cellHeight + HEIGHT_OFFSET, cellWidth, cellHeight);
            }
        }

        // Draw the current mask
        if (currentMask >= 0 && currentMask < masks.size()) {
            MapMask mask = masks.get(currentMask);
            mask.init();
            while (mask.hasNext()) {
                ChangeRecord rec = mask.next();
                g2.setColor(rec.color);
                g2.fillRect(rec.col * cellWidth, rec.row * cellHeight + HEIGHT_OFFSET, cellWidth, cellHeight);
            }
        }
    }

    public int getId(int row, int col) {
        return row * this.cols + col;
    }

    public int getRow(int sid) {
        return sid / this.cols;
    }

    public int getCol(int sid) {
        return sid % this.cols;
    }

    public Point getSquare(Point clickPoint) {
        if (this.cellHeight == 0) return null;
        int x = clickPoint.x;
        int y = clickPoint.y - this.HEIGHT_OFFSET;
        int row = y / cellHeight;
        int col = x / cellWidth;
        if (row >= this.rows || col >= this.cols) return null;
        return new Point(row, col);
    }

    public int getCell(int row, int col) {
        return squares[row][col];
    }

    public void resetMask() {
        currentMask = 0;
    }

    public boolean nextMask() {
        if (currentMask >= 0 && currentMask < masks.size()) {
            currentMask++;
            return true;
        }
        return false;
    }

    public boolean prevMask() {
        if (currentMask > 0) {
            currentMask--;
            return true;
        }
        return false;
    }

    public void clearMasks() {
        currentMask = -1;
        this.masks.clear();
    }

    /**
     * Outputs the map and an optional path as a PNG file.
     *
     * @param fileName
     * @param subgoals
     */
    public void outputImage(String fileName, ArrayList<SearchState> path, ArrayList<SearchState> subgoals) {
        boolean largeMap = false;
        int width = 7;
        if (this.cols > 1000 || this.rows > 1000) largeMap = true;

        if (path != null) {    // Make a mask for the map for the path
            SparseMask currentMask = new SparseMask();
            Color color, pathColor = Color.RED;
            HashMap<String, String> used = new HashMap<>();

            for (int i = 0; i < path.size(); i++) {
                SearchState current = path.get(i);
                int row = getRow(current.id);
                int col = getCol(current.id);

                ChangeRecord rec;
                if (i == 0) {
                    color = Color.green;
                    rec = new ChangeRecord(row, col, color, 1);
                    currentMask.add(rec);
                    if (largeMap)        // Make start and end more visible
                    {
                        for (int k = -width; k <= width; k++)
                            for (int l = -width; l <= width; l++) {
                                if (row + k < rows && col + l < cols && !isWall(row + k, col + l)) {
                                    rec = new ChangeRecord(row + k, col + l, color, 1);
                                    currentMask.add(rec);
                                }
                            }
                    }
                } else if (i == path.size() - 1) {
                    color = Color.blue;
                    rec = new ChangeRecord(row, col, color, 1);
                    currentMask.add(rec);
                    if (largeMap)        // Make start and end more visible
                    {
                        for (int k = -width; k <= width; k++)
                            for (int l = -width; l <= width; l++) {
                                if (row + k < rows && col + l < cols && !isWall(row + k, col + l)) {
                                    rec = new ChangeRecord(row + k, col + l, color, 1);
                                    currentMask.add(rec);
                                }
                            }
                    }
                } else {
                    color = pathColor;
                    if (subgoals != null && subgoals.contains(current))
                        color = Color.ORANGE;    // Show subgoals in a different color
                    rec = new ChangeRecord(row, col, color, 1);
                    if (used.containsKey(rec.toString())) continue;
                    currentMask.add(rec);
                    used.put(rec.toString(), null);
                }
            }
            // Special case for subgoals:
            for (int i = 0; subgoals != null && i < subgoals.size(); i++) {
                SearchState current = subgoals.get(i);
                int row = getRow(current.id);
                int col = getCol(current.id);

                ChangeRecord rec;
                color = Color.ORANGE;    // Show subgoals in a different color
                rec = new ChangeRecord(row, col, color, 1);
                if (used.containsKey(rec.toString())) continue;
                currentMask.add(rec);
                used.put(rec.toString(), null);
            }
            addMask(currentMask);
            this.currentMask = this.masks.size() - 1;
        }
        // Create an image to save
        RenderedImage rendImage = createImage();

        // Write generated image to a file
        try {
            // Save as PNG
            File file = new File(fileName);
            ImageIO.write(rendImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showChanges(String fileName, ArrayList<ChangedPath> changedPaths, SearchState start, SearchState wall) {
        if (changedPaths != null && start != null) {    // Make a mask for the map for the path
            Color color;
            SparseMask currentMask = new SparseMask();
            HashMap<String, String> used = new HashMap<>();

            // colour changed goals in red
            for (ChangedPath current : changedPaths) {
                int row = getRow(current.getGoal().getId());
                int col = getCol(current.getGoal().getId());

                ChangeRecord rec;
                color = new Color(255, 255 - (int) (255 * current.getPercentageOfPathToGoalChanged() / 100), 255);
                rec = new ChangeRecord(row, col, color, 1);
                if (used.containsKey(rec.toString())) continue;
                currentMask.add(rec);
                used.put(rec.toString(), null);
            }

            // colour start in green
            color = Color.GREEN;
            ChangeRecord rec = new ChangeRecord(getRow(start.getId()), getCol(start.getId()), color, 1);
            currentMask.add(rec);

            // colour added wall in blue
            color = Color.BLUE;
            rec = new ChangeRecord(getRow(wall.getId()), getCol(wall.getId()), color, 1);
            currentMask.add(rec);

            addMask(currentMask);
            this.currentMask = this.masks.size() - 1;
        }
        // Create an image to save
        RenderedImage rendImage = createImage();

        // Write generated image to a file
        try {
            // Save as PNG
            File file = new File(fileName);
            ImageIO.write(rendImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showHeatMap(String fileName, HashMap<SearchState, Double> wallImpactMap, SearchState start) {
        if (wallImpactMap != null && start != null) {    // Make a mask for the map for the path
            Color color;
            SparseMask currentMask = new SparseMask();
            HashMap<String, String> used = new HashMap<>();

            // colour changed goals in red
            for (SearchState current : wallImpactMap.keySet()) {
                int row = getRow(current.getId());
                int col = getCol(current.getId());

                ChangeRecord rec;
                color = new Color(255, 255 - (int) (255 * wallImpactMap.get(current) / 100), 255);
                rec = new ChangeRecord(row, col, color, 1);
                if (used.containsKey(rec.toString())) continue;
                currentMask.add(rec);
                used.put(rec.toString(), null);
            }

            // colour start in green
            color = Color.GREEN;
            ChangeRecord rec = new ChangeRecord(getRow(start.getId()), getCol(start.getId()), color, 1);
            currentMask.add(rec);

            addMask(currentMask);
            this.currentMask = this.masks.size() - 1;
        }
        // Create an image to save
        RenderedImage rendImage = createImage();

        // Write generated image to a file
        try {
            // Save as PNG
            File file = new File(fileName);
            ImageIO.write(rendImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showWallsThatChangeRegioning(String fileName, ArrayList<SearchState> wallsThatChangeRegioning, SearchState start) {
        if (wallsThatChangeRegioning != null && start != null) {    // Make a mask for the map for the path
            Color color;
            SparseMask currentMask = new SparseMask();
            HashMap<String, String> used = new HashMap<>();

            // colour changed goals in red
            for (SearchState current : wallsThatChangeRegioning) {
                int row = getRow(current.getId());
                int col = getCol(current.getId());

                ChangeRecord rec;
                color = Color.RED;
                rec = new ChangeRecord(row, col, color, 1);
                if (used.containsKey(rec.toString())) continue;
                currentMask.add(rec);
                used.put(rec.toString(), null);
            }

            // colour start in green
            color = Color.GREEN;
            ChangeRecord rec = new ChangeRecord(getRow(start.getId()), getCol(start.getId()), color, 1);
            currentMask.add(rec);

            addMask(currentMask);
            this.currentMask = this.masks.size() - 1;
        }
        // Create an image to save
        RenderedImage rendImage = createImage();

        // Write generated image to a file
        try {
            // Save as PNG
            File file = new File(fileName);
            ImageIO.write(rendImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void wallsHeatMap(String fileName, HashMap<SearchState, Double> wallsThatChangeDat, HashMap<SearchState, Double> wallsThatChangeDati2, SearchState start) {
        if (wallsThatChangeDat != null && wallsThatChangeDati2 != null && start != null) {    // Make a mask for the map for the path
            Color color;
            SparseMask currentMask = new SparseMask();
            HashMap<String, String> used = new HashMap<>();

            double maxDat = wallsThatChangeDat.values().stream().max(Double::compare).get();
            logger.debug("Max dat: " + maxDat);
            double maxDati2 = wallsThatChangeDati2.values().stream().max(Double::compare).get();
            logger.debug("Max dati2: " + maxDati2);

            for (SearchState current : wallsThatChangeDat.keySet()) {
                int row = getRow(current.getId());
                int col = getCol(current.getId());

                ChangeRecord rec;
                color = new Color(255, 255 - (int) (255 * Math.log(wallsThatChangeDat.get(current)) / Math.log(maxDat)), 255);
                rec = new ChangeRecord(row, col, color, 1);
                if (used.containsKey(rec.toString())) continue;
                currentMask.add(rec);
                used.put(rec.toString(), null);
            }

//            for (SearchState current : wallsThatChangeDati2.keySet()) {
//                int row = getRow(current.getId());
//                int col = getCol(current.getId());
//
//                ChangeRecord rec;
//                color = new Color(255, 255 - (int) (255 * Math.log(wallsThatChangeDati2.get(current)) / Math.log(maxDati2)), 255);
//                rec = new ChangeRecord(row, col, color, 1);
//                if (used.containsKey(rec.toString())) continue;
//                currentMask.add(rec);
//                used.put(rec.toString(), null);
//            }

            // colour start in green
            color = Color.GREEN;
            ChangeRecord rec = new ChangeRecord(getRow(start.getId()), getCol(start.getId()), color, 1);
            currentMask.add(rec);

            addMask(currentMask);
            this.currentMask = this.masks.size() - 1;
        }
        // Create an image to save
        RenderedImage rendImage = createImage();

        // Write generated image to a file
        try {
            // Save as PNG
            File file = new File(fileName);
            ImageIO.write(rendImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void drawPoint(String fileName, SearchState point) {
        if (point != null) {    // Make a mask for the map for the path
            Color color;
            SparseMask currentMask = new SparseMask();
            HashMap<String, String> used = new HashMap<>();

            // colour point in red
            color = Color.RED;
            ChangeRecord rec = new ChangeRecord(getRow(point.getId()), getCol(point.getId()), color, 1);
            currentMask.add(rec);

            addMask(currentMask);
            this.currentMask = this.masks.size() - 1;
        }
        // Create an image to save
        RenderedImage rendImage = createImage();

        // Write generated image to a file
        try {
            // Save as PNG
            File file = new File(fileName);
            ImageIO.write(rendImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void drawPoints(String fileName, SearchState[] points) {
        if (points != null) {    // Make a mask for the map for the path
            Color color;
            SparseMask currentMask = new SparseMask();
            HashMap<String, String> used = new HashMap<>();

            // colour point in red
            color = Color.RED;

            for (SearchState point : points) {
                ChangeRecord rec = new ChangeRecord(getRow(point.getId()), getCol(point.getId()), color, 1);
                currentMask.add(rec);
            }

            addMask(currentMask);
            this.currentMask = this.masks.size() - 1;
        }
        // Create an image to save
        RenderedImage rendImage = createImage();

        // Write generated image to a file
        try {
            // Save as PNG
            File file = new File(fileName);
            ImageIO.write(rendImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showWallsThatChangeDatabase(String fileName, ArrayList<SearchState> wallsThatChangeRegioning, ArrayList<SearchState> wallsThatChangeDat, ArrayList<SearchState> wallsThatChangeDati2, SearchState start) {
        if (wallsThatChangeRegioning != null && wallsThatChangeDat != null && wallsThatChangeDati2 != null && start != null) {    // Make a mask for the map for the path
            Color color;
            SparseMask currentMask = new SparseMask();
            HashMap<String, String> used = new HashMap<>();

            // colour walls that change regioning in red
            for (SearchState current : wallsThatChangeRegioning) {
                int row = getRow(current.getId());
                int col = getCol(current.getId());

                ChangeRecord rec;
                color = Color.RED;
                rec = new ChangeRecord(row, col, color, 1);
                if (used.containsKey(rec.toString())) continue;
                currentMask.add(rec);
                used.put(rec.toString(), null);
            }

            // colour walls that change .dati2 in cyan
            for (SearchState current : wallsThatChangeDati2) {
                int row = getRow(current.getId());
                int col = getCol(current.getId());

                ChangeRecord rec;
                color = Color.CYAN;
                rec = new ChangeRecord(row, col, color, 1);
                if (used.containsKey(rec.toString())) continue;
                currentMask.add(rec);
                used.put(rec.toString(), null);
            }

            // colour walls that change .dat in blue
            for (SearchState current : wallsThatChangeDat) {
                int row = getRow(current.getId());
                int col = getCol(current.getId());

                ChangeRecord rec;
                color = Color.BLUE;
                rec = new ChangeRecord(row, col, color, 1);
                if (used.containsKey(rec.toString())) continue;
                currentMask.add(rec);
                used.put(rec.toString(), null);
            }

            // colour start in green
            color = Color.GREEN;
            ChangeRecord rec = new ChangeRecord(getRow(start.getId()), getCol(start.getId()), color, 1);
            currentMask.add(rec);

            addMask(currentMask);
            this.currentMask = this.masks.size() - 1;
        }
        // Create an image to save
        RenderedImage rendImage = createImage();

        // Write generated image to a file
        try {
            // Save as PNG
            File file = new File(fileName);
            ImageIO.write(rendImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public RenderedImage createImage() {

        BufferedImage bufferedImage = new BufferedImage((this.cols) * cellHeight, (this.rows + 10) * cellHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = bufferedImage.createGraphics();

        this.draw(g2d);
        g2d.dispose();

        return bufferedImage;
    }

    public void computeComplexity(DBStatsRecord stats) {    // Computes the complexity of a map according to a given map abstraction algorithm
        HashMap<Integer, Integer> groups = new HashMap<>();
        int val;
        Integer count;

        // Determine the size of each region (not keeping track of state membership though)
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                val = squares[r][c];
                if (val != GameMap.WALL_CHAR) {
                    count = groups.get(val);
                    if (count == null) groups.put(val, 1);
                    else groups.put(val, count + 1);
                }
            }
        }

        // Analyze the region statistics
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0;

        for (Integer integer : groups.values()) {
            val = integer;
            if (val < min) min = val;
            if (val > max) max = val;
            sum = sum + val;
        }

        logger.debug("Statistics: ");
        logger.debug("Number of regions: " + groups.size());
        double avg = sum / groups.size();
        logger.debug("Minimum region: " + min + " Maximum region: " + max + " Total states: " + sum + " Avg. region: " + avg);
        stats.addStat(19, 1.0 / avg);    // Map complexity
        stats.addStat(20, max);        // Max state size
        stats.addStat(21, min);        // Min state size
        stats.addStat(22, avg);        // Avg state size
    }

    public int size() {    // Returns the number of visitable states on the map (does not count walls)
        int size = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (this.squares[i][j] != WALL_CHAR) {
                    size++;
                }
            }
        }
        return size;
    }

    public int computeDifficulty(StatsRecord stats) {
		/*
		AStar astar = new AStar(this);
		// ArrayList<SearchState> path, tmpPath;
		StatsRecord tmp = new StatsRecord();
		SearchAlgorithm alg = new HillClimbing(this, 25);
		int totalSubgoals = 0;
		int numPaths = 0;
		*/
		/*
		//This is just too inefficient.
		// Computes the difficulty of a map by doing all pairs of A* and also computes number of subgoals as a measure
		for (int i=0; i < rows; i++)
		{	for (int j=0; j < cols; j++)
			{
				if (this.isWall(i, j))
					continue;

				// This is the start point.  Compute all paths to all our states.
				for (int r=0; r < rows; r++)
				{	for (int c=0; c < cols; c++)
					{
						if (this.isWall(r, c))
							continue;

						tmp.clear();

			//			path = astar.computePath(i, j, r, c, tmp);
			//			if (path == null)
			//				continue;

			//			stats.merge(tmp);
						numPaths++;
						if (numPaths % 100 == 0)
							System.out.println("Done path: "+numPaths);

						// Now compress the path.  The path fragments may already been compressed or not during base path construction.
			//			tmpPath =  SearchUtil.computeSubgoals(this, path, alg);

			//			totalSubgoals += tmpPath.size()-2;
					}
				}
			}
		}
		*/
		/*
		System.out.println("Number of paths: "+numPaths);
		System.out.println("Number of subgoals: "+totalSubgoals);
		System.out.println(stats);
		return totalSubgoals;
		*/
        return 0;
    }

    /**
     * Returns true if given seed (pt) is closer to cell (row, col) that its current seed.
     * This version is optimized for speed and does not use MapPoint opbject.
     * TODO: Could make this faster by storing distance somewhere.
     */
    public boolean isCloser2(int row, int col, int newSeedRow, int newSeedCol) {
        int currentSeedId = getCell(row, col);
        // GroupRecord record = groups.get(currentSeedId);			// TODO: Major memory consumer.  Can we replace HashMap with a no object data sturcture?
        GroupRecord record = groupsArray[currentSeedId];
        return (GameMap.computeDistance(row, col, newSeedRow, newSeedCol) < GameMap.computeDistance(row, col, getRow(record.getGroupRepId()), getCol(record.getGroupRepId())));
    }

    /**
     * Compute group records from abstract regions in a map.
     */
    // 3
    public void computeGroups() throws Exception {
        long currentTime = System.currentTimeMillis();
        groups = new TreeMap<Integer, GroupRecord>();

        // Traverse all cells to create the groups
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int groupId = squares[i][j];

                if (groupId != GameMap.EMPTY_CHAR && groupId != GameMap.WALL_CHAR) {
                    // See if group already exists
                    GroupRecord rec = groups.get(groupId);
                    if (rec == null) {    // New group
                        GroupRecord newrec = new GroupRecord();
                        newrec.groupId = groupId;
                        newrec.groupRepId = this.getId(i, j);
                        newrec.setNumStates(1);
                        this.addGroup(groupId, newrec);
                    } else {    // Update group
                        rec.incrementNumStates();
                    }
                }
            }
        }
        long endTime = System.currentTimeMillis();
        logger.debug("Time to compute groups: " + (endTime - currentTime) + " Groups: " + groups.size());

        // Compute centroids
        computeCentroids();
    }

    // Compute centroids of all groups
    // 4
    public void computeCentroids() {
        // TODO: How large should I make this array and where will I resize it?
        regionReps = new int[(int) (groups.size() * 1.1)];

        long currentTime = System.currentTimeMillis();

        for (Entry<Integer, GroupRecord> integerGroupRecordEntry : groups.entrySet()) {    // Find centroid for each record
            GroupRecord rec = integerGroupRecordEntry.getValue();
            // This won't be the actual region rep id yet since that hasn't been computed, so it's just the first state
            // encountered in the region. That's enough to find the sector, and I need to pass sector bounds to my method

            int stateId = rec.groupRepId;
            int row = this.getRow(stateId);
            int col = this.getCol(stateId);

            final int NUM_SECTORS_PER_ROW = (int) Math.ceil(this.cols * 1.0 / this.gridSize);
            final int SECTOR_ID = row / gridSize * NUM_SECTORS_PER_ROW + col / this.gridSize;

            // Start of sector
            final int START_ROW = (SECTOR_ID / NUM_SECTORS_PER_ROW) * gridSize;
            final int START_COL = (SECTOR_ID % NUM_SECTORS_PER_ROW) * gridSize;
            // End of sector
            final int END_ROW = Math.min(START_ROW + gridSize, this.rows);
            final int END_COL = Math.min(START_COL + gridSize, this.cols);

            recomputeCentroid(rec.groupId, rec, START_ROW, END_ROW, START_COL, END_COL, gridSize);
        }

        long endTime = System.currentTimeMillis();
        logger.debug("Time to compute centroids: " + (endTime - currentTime));
    }

    public int recomputeCentroid(int regionId, GroupRecord rec, int startRow, int endRow, int startCol, int endCol, int gridSize) {
        int[] rows = new int[gridSize * gridSize];
        int[] cols = new int[gridSize * gridSize];
        long sumRow = 0, sumCol = 0;
        int numStates = 0;
        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                // If the state is in the region
                if (squares[row][col] == regionId) {
                    rows[numStates] = row;
                    cols[numStates] = col;
                    sumRow += row;
                    sumCol += col;
                    numStates++;
                }
            }
        }

        // Compute center of region
        int centroidRow = Math.round(sumRow / numStates);
        int centroidCol = Math.round(sumCol / numStates);

        // If the computed center is a wall or outside the region
        if (this.isWall(centroidRow, centroidCol) || squares[centroidRow][centroidCol] != regionId) {
            // Find the point that is in the group that is closest
            int minDist = 10000, minRow = -1, minCol = -1;
            for (int stateNum = 0; stateNum < numStates; stateNum++) {
                int row = rows[stateNum];
                int col = cols[stateNum];
                int dist = GameMap.computeDistance(centroidRow, centroidCol, row, col);
                if (dist < minDist) {
                    minRow = row;
                    minCol = col;
                    minDist = dist;
                }
            }
            centroidRow = minRow;
            centroidCol = minCol;
        }
        int regionRep = this.getId(centroidRow, centroidCol);

        rec.setGroupRepId(regionRep);
        regionReps[rec.groupId - GameMap.START_NUM] = regionRep;
        logger.debug("Region id: " + regionId + " - region rep: " + regionRep);

        return regionRep;
    }

    public int getRegionRepFromRegionId(int regionId) {
        if (regionId == 42) {
            logger.error("Cannot find region id from wall!");
        }
        return regionReps[regionId - GameMap.START_NUM];
    }

    private int getRegionRepFromRowAndCol(int row, int col) {
        return getRegionRepFromRegionId(this.squares[row][col]);
    }

    public int getRegionRepFromState(int sid) {
        return getRegionRepFromRowAndCol(this.getRow(sid), this.getCol(sid));
    }

    public int getRegionFromRowAndCol(int row, int col) {
        return this.squares[row][col];
    }

    public int getRegionFromState(int sid) {
        return getRegionFromRowAndCol(this.getRow(sid), this.getCol(sid));
    }

    public int getGridSize() {
        return gridSize;
    }

    public int findStartRowOfSector(int sectorId) {
        return (sectorId / this.numSectorsPerRow) * this.gridSize;
    }

    public int findStartColOfSector(int sectorId) {
        return (sectorId % this.numSectorsPerRow) * this.gridSize;
    }

    public int findEndRowOfSector(int startRow) {
        return Math.min(startRow + this.gridSize, this.rows);
    }

    public int findEndColOfSector(int startCol) {
        return Math.min(startCol + this.gridSize, this.cols);
    }

    public int findSectorId(int wallRow, int wallCol) {
        return wallRow / gridSize * this.numSectorsPerRow + wallCol / this.gridSize;
    }

    public int findSectorId(int sid) {
        int wallRow = getRow(sid);
        int wallCol = getCol(sid);
        return wallRow / gridSize * this.numSectorsPerRow + wallCol / this.gridSize;
    }
}
