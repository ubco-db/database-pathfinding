import map.GameMap;
import search.SearchState;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class DrawWallsThatChangeDB {
    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;


    public static void main(String[] args) {

        int startId = 13411;

        ArrayList<SearchState> wallsThatChangeRegioning = readFromFile("wallsThatChangeRegioning.txt");
        HashMap<SearchState, Double> wallsThatChangeDat = readDataFromFile("dat_differingFiles.txt");
        HashMap<SearchState, Double> wallsThatChangeDati2 = readDataFromFile("dati2_differingFiles.txt");

        GameMap map = new GameMap(PATH_TO_MAP);
        map.wallsHeatMap("dynamic/databases/adding_walls/heatMapWallsThatChangeDB.png", wallsThatChangeDat, wallsThatChangeDati2, new SearchState(startId));
    }

    public static ArrayList<SearchState> readFromFile(String filename) {
        ArrayList<SearchState> searchStates = new ArrayList<>();
        try {
            // Specify the path to your text file
            File file = new File("dynamic/databases/adding_walls/" + filename);
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                try {
                    // Parse the line as an integer and add it to the ArrayList
                    int id = Integer.parseInt(line);
                    searchStates.add(new SearchState(id));
                } catch (NumberFormatException e) {
                    System.err.println("Skipping non-integer input: " + line);
                }
            }

            scanner.close();
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        }
        return searchStates;
    }

    public static HashMap<SearchState, Double> readDataFromFile(String fileName) {
        HashMap<SearchState, Double> dataMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader("dynamic/databases/adding_walls/" + fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    try {
                        int key = Integer.parseInt(parts[0]);
                        double value = Double.parseDouble(parts[1]);
                        dataMap.put(new SearchState(key), value);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing integers on this line: " + line);
                    }
                } else {
                    System.err.println("Skipping line with improper format: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataMap;
    }

}
