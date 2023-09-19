package scenario;


import search.SearchState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;


public class Scenario {
    private ArrayList<Problem> problems;

    public Scenario() {
        problems = new ArrayList<Problem>();
    }

    /**
     * Load a scenario from a text file.
     *
     * @param fileName
     */
	/*
	public Scenario(String fileName)
	{		
		Scanner sc = null;
		try
		{	sc = new Scanner(new File(fileName));
			
			String st = sc.nextLine();
			int numProblems = Integer.parseInt(st);
			problems = new ArrayList<Problem>(numProblems);
			String lastMapName = "";
			GameMap map=null;
			
			int pointOffset = 2;	//  As co-ordinates are indexed from 1 in Matlab and 1 cell padding around entire map is assumed
			for (int i=0; i < numProblems; i++)			
			{	st = sc.nextLine();				
				StringTokenizer tokenizer = new StringTokenizer(st);
				@SuppressWarnings("unused")
				String id = tokenizer.nextToken();
				String mapName = tokenizer.nextToken();
				if (!lastMapName.equals(mapName))
				{	map = new GameMap();
					map.load(mapName);
				}
				String start = tokenizer.nextToken();
				int idx = start.indexOf(':'); 
				int startRow = Integer.parseInt(start.substring(0,idx)) - pointOffset;
				int startCol = Integer.parseInt(start.substring(idx+1)) - pointOffset;				
				String goal = tokenizer.nextToken();
				idx = goal.indexOf(':'); 
				int goalRow = Integer.parseInt(goal.substring(0,idx)) - pointOffset;
				int goalCol = Integer.parseInt(goal.substring(idx+1)) - pointOffset;
				int cost = (int) Math.round(Double.parseDouble(tokenizer.nextToken())*10);
				double difficulty = Double.parseDouble(tokenizer.nextToken());
		        Problem p = new Problem(mapName, new SearchState(map.getId(startRow, startCol)), new SearchState(map.getId(goalRow,goalCol)), cost, difficulty);				
		        problems.add(p);
		        lastMapName = mapName;
			}			
		}
		catch (FileNotFoundException e)
		{	System.out.println("Did not find input file: "+e);	}
		finally
		{	if (sc != null)
				sc.close();	
		}		
		write(fileName+"2");
	}
	*/
    public Scenario(String fileName) {
        Scanner sc = null;
        try {
            sc = new Scanner(new File(fileName));

            String st = sc.nextLine();
            if (st.contains("version")) {    // Nathan's format: bucket number, map name, map width, map height, start x, start y, goal x, goal y, optimal length
                // Does not know number of problems before hand
                problems = new ArrayList<Problem>(1000);
                int count = 0;
                while (sc.hasNextLine()) {
                    st = sc.nextLine();
                    if (st.equals("")) continue;
                    StringTokenizer tokenizer = new StringTokenizer(st);
                    String bucketid = tokenizer.nextToken();
                    String mapName = tokenizer.nextToken();
                    int mapw = Integer.parseInt(tokenizer.nextToken());
                    int maph = Integer.parseInt(tokenizer.nextToken());
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
                    String id = tokenizer.nextToken();
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
        } finally {
            if (sc != null) sc.close();
        }
    }

    public int getNumProblems() {
        return problems.size();
    }

    public Problem getProblem(int num) {
        return problems.get(num);
    }

    public void addProblem(Problem p) {
        problems.add(p);
    }

    public void write(String fileName) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(fileName);
            // Write out the number of problems
            out.println(problems.size());

            for (int i = 0; i < problems.size(); i++) {
                Problem p = problems.get(i);
                out.print((i + 1) + "\t");
                out.print(p.getMapName() + "\t");
                out.print(p.getStart().id + "\t");
                out.print(p.getGoal().id + "\t");
                out.print(p.getOptimalTravelCost() / 10.0 + "\t");
                out.println(p.getAStarDifficulty());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error with output file: " + e);
        } finally {
            if (out != null) out.close();
        }
    }
}
