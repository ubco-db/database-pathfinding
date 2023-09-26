package scenario;

import search.SearchState;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;


public class Scenario {
    private ArrayList<Problem> problems;

    /**
     * Load a scenario from a text file.
     *
     * @param fileName - name of the scenario to execute (scenarios are in the "scenarios" folder)
     */
    public Scenario(String fileName) {
        try (Scanner sc = new Scanner(new File(fileName))) {

            String st = sc.nextLine();
            int numProblems = Integer.parseInt(st); // first line is the number of problems in the scenario
            problems = new ArrayList<Problem>(numProblems);

            for (int i = 0; i < numProblems; i++) {
                st = sc.nextLine(); // 1	maps/smallMaps/012.map	10228	15652	126.6	8.504854
                StringTokenizer tokenizer = new StringTokenizer(st);
                tokenizer.nextToken(); // skip problem number
                String mapName = tokenizer.nextToken(); // maps/smallMaps/012.map
                int startId = Integer.parseInt(tokenizer.nextToken()); // 10228
                int goalId = Integer.parseInt(tokenizer.nextToken()); // 15652
                int cost = (int) Math.round(Double.parseDouble(tokenizer.nextToken()) * 10); // 126.6
                // QUESTION: What is this difficulty metric and how is it computed?
                double difficulty = Double.parseDouble(tokenizer.nextToken()); // 8.504854
                Problem p = new Problem(mapName, new SearchState(startId), new SearchState(goalId), cost, difficulty);
                problems.add(p);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Did not find input file: " + e);
        }
    }

    public int getNumProblems() {
        return problems.size();
    }

    public Problem getProblem(int num) {
        return problems.get(num);
    }

}
