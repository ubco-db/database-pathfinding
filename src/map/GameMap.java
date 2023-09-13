package map;

import search.SearchState;
import util.HeuristicFunction;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;


/**
 * A data structure for a 2D map.
 * Maps can be read from files and saved back to disk.
 * Maps can also be displayed on the screen.
 */
public class GameMap {
    public static final char WALL_CHAR = '*';
    public static final char EMPTY_CHAR = ' ';

    public int rows, cols;
    public int[][] squares;

    public Point startPoint;
    public Point goalPoint;

    public HashMap<Integer, Color> colors;
    public int states;

    private Random generator;

    public ArrayList<SearchState> path;

    private ArrayList<MapMask> masks;
    private int currentMask = -1;

    private int panelHeight = 950;
    private int panelWidth = 950;
    private int heightOffset = 30;
    private int cellHeight;
    private int cellWidth;

    public static int computeDistance(int startId, int goalId, int ncols, HeuristicFunction heuristic) {
        return heuristic.apply(startId, goalId, ncols);
    }

    public void mapInit() {
        colors = new HashMap<Integer, Color>();
        states = rows * cols;
        generator = new Random();
        masks = new ArrayList<MapMask>();
        cellHeight = panelHeight / rows;
        cellWidth = panelWidth / cols;
        if (cellHeight <= 0)
            cellHeight = 1;
        if (cellWidth <= 0)
            cellWidth = 1;
        if (cellHeight > cellWidth) {
            cellHeight = cellWidth;
        } else
            cellWidth = cellHeight;
    }

    public GameMap(String fileName) {       // Loads a map in Vadim's format into data structure
        load(fileName);
        new Random();
    }

    public void load(String fileName) {
        try (Scanner sc = new Scanner(new File(fileName))) {

            sc.nextLine();                  // Drop first line which is formatted
            String st = sc.nextLine();      // Number of rows. e.g. height 139
            rows = Integer.parseInt(st.substring(7).trim());
            st = sc.nextLine();             // Number of cols. e.g. width 148
            cols = Integer.parseInt(st.substring(6).trim());
            sc.nextLine();
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
            System.out.println("Did not find input file: " + e);
        }
    }

    public boolean isNotWall(int r, int c) {
        return squares[r][c] != WALL_CHAR;
    }

    public boolean isValid(int r, int c) {
        return (c >= 0 && r >= 0 && r < rows && c < cols);
    }

    private static final ArrayList<SearchState> result = new ArrayList<>(8); // QUESTION: Should this be 4 if we can only move 4 ways?
    private static final HashMap<Integer, SearchState> createdStates = new HashMap<>();
    public static Integer[] ints;

    static {
        int size = 6000000;

        // int size = 10000000; // For Map2 // QUESTION: Why does Map2 need a different size?

        ints = new Integer[size];
        for (int i = 0; i < size; i++)
            ints[i] = i;
    }

    private static SearchState getState(int id) {
        Integer i = ints[id];
        SearchState s = createdStates.get(i);
        if (s == null) {
            s = new SearchState(id);
            createdStates.put(i, s);
        }
        return s;
    }

    // QUESTION: why is this only 4-way?
    public ArrayList<SearchState> getNeighbors(int r, int c) {
        // 4-way pathfinding
        result.clear();
        if (isValid(r - 1, c) && isNotWall(r - 1, c))     // Above
            result.add(getState(this.getId(r - 1, c)));
        if (isValid(r + 1, c) && isNotWall(r + 1, c))     // Bottom
            result.add(getState(this.getId(r + 1, c)));
        if (isValid(r, c - 1) && isNotWall(r, c - 1))     // Left
            result.add(getState(this.getId(r, c - 1)));
        if (isValid(r, c + 1) && isNotWall(r, c + 1))     // Right
            result.add(getState(this.getId(r, c + 1)));
        return result;
    }

    // QUESTION: What is Id used for?
    public int getId(int row, int col) {
        // assigns ids to grid from left to right
        return row * this.cols + col;
    }

    public int getRow(int sid) {
        return sid / this.cols;
    }

    public int getCol(int sid) {
        return sid % this.cols;
    }


    // Returns the number of visitable states on the map (does not count walls)
    public int size() {
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

    // Draws a map on the screen
    public void draw(Graphics2D g2) {
        // Make sure everything is square
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Color col;
                int val = this.squares[i][j];
                // Just draw points for speed?
                if (val == WALL_CHAR)
                    g2.setColor(Color.BLACK);
                else if (val == EMPTY_CHAR)
                    g2.setColor(Color.WHITE);
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
                g2.fillRect(j * cellWidth, i * cellHeight + heightOffset, cellWidth, cellHeight);
            }
        }

        // Draw the current mask
        if (currentMask >= 0 && currentMask < masks.size()) {
            MapMask mask = masks.get(currentMask);
            mask.init();
            while (mask.hasNext()) {
                ChangeRecord rec = mask.next();
                g2.setColor(rec.color);
                g2.fillRect(rec.col * cellWidth, rec.row * cellHeight + heightOffset, cellWidth, cellHeight);
            }
        }
    }

    public Point getSquare(Point clickPoint) {
        if (this.cellHeight == 0)
            return null;
        int x = clickPoint.x;
        int y = clickPoint.y - this.heightOffset;
        int row = y / cellHeight;
        int col = x / cellWidth;
        if (row >= this.rows || col >= this.cols)
            return null;
        return new Point(row, col);
    }

    public void addMask(SparseMask mask) {
        masks.add(mask);
    }

    public void clearMasks() {
        currentMask = -1;
        this.masks.clear();
    }

    public void resetMask() {
        currentMask = 0;
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
            System.out.println("Error with output file: " + e);
        }
    }

    public void rotate() {
        // Rotate 90 degrees
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

    public static int computeDistance(int startId, int goalId, int ncols) {
        // Computes octile distance (quick estimate with no square root) from this state to goal state
        int startRow = startId / ncols;
        int goalRow = goalId / ncols;
        int diffRow = startRow - goalRow;

        int bit31 = diffRow >> 31;                // Compute its absolute value
        diffRow = (diffRow ^ bit31) - bit31;

        int diffCol = ((startId - startRow * ncols) - (goalId - goalRow * ncols));
        bit31 = diffCol >> 31;                // Compute its absolute value
        diffCol = (diffCol ^ bit31) - bit31;

        // return Math.abs(diffRow) *10 + Math.abs(diffCol)*10;
        return Math.abs(diffRow) + Math.abs(diffCol);
    }
}
