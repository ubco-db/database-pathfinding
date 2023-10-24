import map.GameMap;
import search.SearchState;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class DrawWallsThatChangeDB {
    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;


    public static void main(String[] args) {

        int startId = 13411;

        ArrayList<SearchState> wallsThatChangeRegioning = readFromFile("wallsThatChangeRegioning.txt");
        ArrayList<SearchState> wallsThatChangeDat = readFromFile("dat_differingFiles.txt");
        ArrayList<SearchState> wallsThatChangeDati2 = readFromFile("dati2_differingFiles.txt");

        GameMap map = new GameMap(PATH_TO_MAP);
        map.showWallsThatChangeDatabase("dynamic/databases/adding_walls/wallsThatChangeDB.png", wallsThatChangeRegioning, wallsThatChangeDat, wallsThatChangeDati2, new SearchState(startId));
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
}
