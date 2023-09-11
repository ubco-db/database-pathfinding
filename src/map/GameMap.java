package map;

import search.SearchState;
import util.HeuristicFunction;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
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

    public HashMap<Integer, Color> colors;
    public int states;

    public static int computeDistance(int startId, int goalId, int ncols, HeuristicFunction heuristic) {
        return heuristic.apply(startId, goalId, ncols);
    }

    public void mapInit() {
        colors = new HashMap<Integer, Color>();
        states = rows * cols;
    }

    public GameMap(String fileName) {    // Loads a map in Vadim's format into data structure
        load(fileName);
        new Random();
    }

    public void load(String fileName) {
        try (Scanner sc = new Scanner(new File(fileName))) {

            String st = sc.nextLine();     // Drop first line which is formatted
            if (!st.contains("type")) {    // Map is in binary format
                sc.close();
                this.loadMap(fileName);
                return;
            }

            // seems like the only maps without a type are 1-WH-flipped.map and 2-orz103d-flipped.map

            System.out.println("THIS ONE: " + fileName);

            st = sc.nextLine();            // Number of rows. e.g. height 139
            rows = Integer.parseInt(st.substring(7).trim());
            st = sc.nextLine();            // Number of cols. e.g. width 148
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

    /**
     * This version loads a number-based map.
     *
     * @param fileName
     */
    public void loadMap(String fileName) {
        try (Scanner sc = new Scanner(new File(fileName))) {

            String st = sc.nextLine();              // Number of rows. e.g. height 139
            rows = Integer.parseInt(st.substring(7).trim());
            st = sc.nextLine();                     // Number of cols. e.g. width 148
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
            System.out.println("Did not find input file: " + e);
        } catch (Exception e) {
            System.out.println("IO Error: " + e);
        }
    }

    public boolean notWall(int r, int c) {
        return squares[r][c] != WALL_CHAR;
    }

    public boolean isValid(int r, int c) {
        return (c >= 0 && r >= 0 && r < rows && c < cols);
    }

    private static final ArrayList<SearchState> result = new ArrayList<>(8);
    private static final HashMap<Integer, SearchState> createdStates = new HashMap<>();
    public static Integer[] ints;

    static {
        int size = 6000000;

        // int size = 10000000; // For Map2

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

    // TODO: why is this only 4-way?
    public ArrayList<SearchState> getNeighbors(int r, int c) {
        // 4-way pathfinding
        result.clear();
        if (isValid(r - 1, c) && notWall(r - 1, c))     // Above
            result.add(getState(this.getId(r - 1, c)));
        if (isValid(r + 1, c) && notWall(r + 1, c))     // Bottom
            result.add(getState(this.getId(r + 1, c)));
        if (isValid(r, c - 1) && notWall(r, c - 1))     // Left
            result.add(getState(this.getId(r, c - 1)));
        if (isValid(r, c + 1) && notWall(r, c + 1))     // Right
            result.add(getState(this.getId(r, c + 1)));
        return result;
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
}
