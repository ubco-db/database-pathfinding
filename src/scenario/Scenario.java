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
     * @param fileName
     */
    public Scenario(String fileName) {
        try (Scanner sc = new Scanner(new File(fileName))) {

            String st = sc.nextLine();
            if (st.contains("version")) {
                // Nathan's format: bucket number, map name, map width, map height, start x, start y, goal x, goal y, optimal length
                // Does not know number of problems beforehand
                problems = new ArrayList<Problem>(1000);
                int count = 0;
                while (sc.hasNextLine()) {
                    st = sc.nextLine();
                    if (st.equals(""))
                        continue;
                    StringTokenizer tokenizer = new StringTokenizer(st);
                    tokenizer.nextToken();
                    String mapName = tokenizer.nextToken();
                    int mapw = Integer.parseInt(tokenizer.nextToken());
                    tokenizer.nextToken();
                    int startX = Integer.parseInt(tokenizer.nextToken());
                    int startY = Integer.parseInt(tokenizer.nextToken());
                    int startId = startY * mapw + startX;
                    int goalX = Integer.parseInt(tokenizer.nextToken());
                    int goalY = Integer.parseInt(tokenizer.nextToken());
                    int goalId = goalY * mapw + goalX;
                    int cost = (int) Math.round(Double.parseDouble(tokenizer.nextToken()) * 10);
                    Problem p = new Problem(mapName, new SearchState(startId), new SearchState(goalId), cost, 0);
                    problems.add(p);
                    count++;
                }
                System.out.println("# of problems: " + count);
            } else {    // our format
                int numProblems = Integer.parseInt(st);
                problems = new ArrayList<Problem>(numProblems);

                for (int i = 0; i < numProblems; i++) {
                    st = sc.nextLine();
                    StringTokenizer tokenizer = new StringTokenizer(st);
                    tokenizer.nextToken();
                    String mapName = tokenizer.nextToken();
                    int startId = Integer.parseInt(tokenizer.nextToken());
                    int goalId = Integer.parseInt(tokenizer.nextToken());
                    int cost = (int) Math.round(Double.parseDouble(tokenizer.nextToken()) * 10);
                    double difficulty = Double.parseDouble(tokenizer.nextToken());
                    Problem p = new Problem(mapName, new SearchState(startId), new SearchState(goalId), cost, difficulty);
                    problems.add(p);
                }
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
