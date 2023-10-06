package dynamic;

import map.GameMap;
import search.SearchState;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class Walls {

    public static void addWall(String mapName, ArrayList<SearchState> a, GameMap map) {
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(mapName, "rw");


            for (SearchState searchState : a) {
                file.seek(0); //Go to start of file

                // Go to (Row-1)th row
                for (int j = 0; j < 4 + map.getRow(searchState.id); j++) {
                    file.readLine();
                }

                // Go to col
                for (int j = 0; j < map.getCol(searchState.id); j++) {
                    file.readByte();
                }

                //Change space to Wall
                file.writeBytes("@");
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void removeWall(String mapName, ArrayList<SearchState> a, GameMap map) {
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(mapName, "rw");

            // Go to row
            for (SearchState searchState : a) {
                file.seek(0); //Go to start of file

                for (int j = 0; j < 4 + map.getRow(searchState.id); j++) {
                    file.readLine();
                }
                // Go to col
                for (int j = 0; j < map.getCol(searchState.id); j++) {
                    file.readByte();
                }

                //Change wall to space
                file.writeBytes(".");
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
