import database.DBStats;
import database.DBStatsRecord;
import database.SubgoalDB;
import map.GameMap;
import scenario.Scenario;
import scenario.StatsCompare;
import search.AStarHeuristic;
import search.MapSearchProblem;
import search.SearchAlgorithm;
import search.SearchState;
import search.StatsRecord;
import util.HeuristicFunction;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Does a comparison of different heuristics for A*.
 */
public class HeuristicCompare {

    public static void main(String[] argv) {
        String[] algorithmNames = {"A*", "HCDPS", "HCDPS+", "A*+heuristic"};
        String[] abbrv = {"a", "hcdps", "hcdps+", "cover2", "AHrt"};

        int[] algorithms = {0, 0, 12};
        int heuristicId = 1; // (0~5) heuristic function id passing to A* with arbitrary heruistic
        double weight = 5; // For weighted A*

        // boolean buildPath = true; // Algorithms will build path not just compute cost of path.
        boolean showPaths = false; // If true, paths computed by each algorithm
        // are printed to standard output.
        boolean showImage = false; // If true, will produce a PNG image for the
        // path produced by each algorithm on each
        // problem regardless if the path is good or
        // not.

        String imageDir = "images/";
        String dbPath = "databases/";
        String binaryOutputPath = "results/";

        /*
         * Heuristic functions list
         */
        ArrayList<HeuristicFunction> heuristicList = createHeuristics();

        double[] weights = {1, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2, 3, 4, 5, 10};

        int algorithmsLength = heuristicList.size() + weights.length;

        String[] algNames = new String[algorithmsLength];
        for (int i = 0; i < algorithmsLength; i++)
            if (i < weights.length) algNames[i] = "Weighted A* w " + weights[i];
            else {
                switch (i) {
                    case 15:
                        algNames[i] = "A* with MD     ";
                        break;
                    case 16:
                        algNames[i] = "A* with brc202d";
                        break;
                    case 17:
                        algNames[i] = "A* with den000d";
                        break;
                    case 18:
                        algNames[i] = "A* with den501d";
                        break;
                    case 19:
                        algNames[i] = "A* with lak505d";
                        break;
                    case 20:
                        algNames[i] = "A* with orz103d";
                        break;
                    case 21:
                        algNames[i] = "A* with ost000a";
                        break;
                    case 22:
                        algNames[i] = "A* with wallhug";
                        break;
                }
            }


        // String mapFile = "maps/gen/1-WH.map";
        String mapFile = "maps/gen/2-orz103d.map";
        System.out.println("Map: " + mapFile);

        DBStatsRecord rec = null;

        // Create the scenario and add problems

        Scenario scenario = new Scenario();
        int numProblems = scenario.getNumProblems();

        // Store information on bad problems, subgoal databases if needed by the algorithm, and algorithm statistics.
        ArrayList<StatsRecord>[] badProblems = new ArrayList[algorithmsLength];
        ArrayList<StatsRecord>[] problemStats = new ArrayList[algorithmsLength];
        StatsRecord[] overallStats = new StatsRecord[algorithmsLength];
        DBStats[] dbStats = new DBStats[algorithmsLength];
        ArrayList<Integer> badProblemNum = new ArrayList<Integer>();
        ArrayList<Integer> noSubgoal = new ArrayList<Integer>();
        SubgoalDB[] databases = new SubgoalDB[algorithmsLength];
        GameMap[] maps = new GameMap[algorithmsLength];
        GameMap baseMap = null; // The base map for a scenario problem.
        ArrayList<SearchState>[] paths = new ArrayList[algorithmsLength];
        ArrayList<SearchState>[] subgoals = new ArrayList[algorithmsLength];

        for (int i = 0; i < algorithmsLength; i++) {
            problemStats[i] = new ArrayList<StatsRecord>(scenario.getNumProblems());
            overallStats[i] = new StatsRecord();
            subgoals[i] = new ArrayList<SearchState>();
            dbStats[i] = null;
        }

        // These variables track if A* statistics match those in the scenario
        // file.
        int count = 0, countAStarCosts = 0, countAStarDiff = 0;

        // This is used to nicely format the output of problem numbers.
        int numDigits = 2;
        if (numProblems > 100) numDigits = 3;

        long startTime = System.currentTimeMillis();
        boolean mapSwitch = false;

        ArrayList<SearchState> path = null;
        StatsRecord stats;

        GameMap map = new GameMap(mapFile);
        int numStarts = 100, numGoals = 1000;

        MapSearchProblem problem = new MapSearchProblem(map);

        SearchAlgorithm alg = null;
        HashSet<Integer> goals = new HashSet<>();
        HashSet<Integer> starts = new HashSet<>();
        int goalid, startid;

        for (int g = 0; g < numGoals; g++) {
            starts.clear();
            do {
                goalid = map.generateRandomState();
            } while (goals.contains(goalid));
            goals.add(goalid);
            SearchState goal = new SearchState(goalid);

            for (int s = 0; s < numStarts; s++) {
                do {
                    startid = map.generateRandomState();
                } while (startid == goalid || starts.contains(startid));
                starts.add(startid);

                SearchState start = new SearchState(startid);

                // Solve problem for given heuristic and algorithm
                // System.out.println("\n\nPerforming problem:  Start: "+ problem.idToString(startid) + " Goal: "+ problem.idToString(goalid));

                for (int j = 0; j < algorithmsLength; j++) {
//					if (j < weights.length)
//						alg =  new WeightedAStar(problem, weights[j]);
//					else
                    alg = new AStarHeuristic(problem, heuristicList.get(j - weights.length));

                    stats = new StatsRecord();

                    long currentTime = System.currentTimeMillis();

                    // AStarHeuristic astarh = new AStarHeuristic(problem, heuristicList.get(j));
                    // path = astarh.computePath(start, goal, stats);
                    path = alg.computePath(start, goal, stats);

                    stats.setTime(System.currentTimeMillis() - currentTime);
                    StatsCompare.mergeRecords(overallStats[j], stats);
                    problemStats[j].add(stats);
                    paths[j] = path;
                    // System.out.println(stats);
                }
            }
        }


        System.out.println("\n\nOverall results of " + (numGoals * numStarts) + " problems.");
        StatsCompare.compareRecords(overallStats, algNames);

    }

    public static ArrayList<HeuristicFunction> createHeuristics() {
        ArrayList<HeuristicFunction> heuristicList = new ArrayList<HeuristicFunction>();

        // f0 - this is Manhattan distance with diagonals
		 /*
		 heuristicList.add(
			 new HeuristicFunction()
			 {
				 public int apply(int startId, int goalId, int ncols) {
					 int startRow = startId/ncols;
					 int goalRow = goalId/ncols;
					 int diffRow = startRow - goalRow;
					 
					 int bit31 = diffRow >> 31;				// Compute its absolute value
					 diffRow = (diffRow ^ bit31) - bit31;
							 
					 int diffCol = ((startId - startRow* ncols) - (goalId - goalRow* ncols));
					 bit31 = diffCol >> 31;				// Compute its absolute value
					 diffCol = (diffCol ^ bit31) - bit31;		
								 
					 if (diffRow > diffCol)						
						 return diffCol * 14 + (diffRow-diffCol) * 10;
					 else
						 return diffRow * 14 + (diffCol-diffRow) * 10;		
				 }
			 }
		 );
		 */
        // f0 - Manhattan distance
        heuristicList.add(new HeuristicFunction() {
            public int apply(int startId, int goalId, int ncols) {
                int startRow = startId / ncols;
                int goalRow = goalId / ncols;
                int diffRow = startRow - goalRow;

                int bit31 = diffRow >> 31;                // Compute its absolute value
                diffRow = (diffRow ^ bit31) - bit31;

                int diffCol = ((startId - startRow * ncols) - (goalId - goalRow * ncols));
                bit31 = diffCol >> 31;                // Compute its absolute value
                diffCol = (diffCol ^ bit31) - bit31;

                // return Math.abs(diffRow) *10 + Math.abs(diffCol)*10;
                return Math.abs(diffRow) * 1 + Math.abs(diffCol) * 1;
            }
        });

        // f1 - brc202d
        heuristicList.add(new HeuristicFunction() {
            public int apply(int startId, int goalId, int ncols) {
                int startRow = startId / ncols; //y
                int goalRow = goalId / ncols; //y_g
                int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                int startCol = startId - startRow * ncols; //x
                int goalCol = goalId - goalRow * ncols; //x_g
                int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                double p1 = diffCol * Math.sqrt(1.0 * goalRow / startCol);
                double max = p1 < diffRow ? diffRow : p1;

                return (int) Math.round(Math.pow(max, 4));
            }
        });

        // f2 - den000d
        heuristicList.add(new HeuristicFunction() {
            public int apply(int startId, int goalId, int ncols) {
                int startRow = startId / ncols; //y
                int goalRow = goalId / ncols; //y_g
                int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                int startCol = startId - startRow * ncols; //x
                int goalCol = goalId - goalRow * ncols; //x_g
                int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                int max = Math.max(diffCol, diffRow);
                return (int) Math.round(44.9 * max + diffRow);
            }
        });

        // f3 - den501d
        heuristicList.add(new HeuristicFunction() {
            public int apply(int startId, int goalId, int ncols) {
                int startRow = startId / ncols; //y
                int goalRow = goalId / ncols; //y_g
                int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                int startCol = startId - startRow * ncols; //x
                int goalCol = goalId - goalRow * ncols; //x_g
                int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                double p1 = 1.0 * goalRow / (startRow - 8.3) * diffRow;
                double max = p1 - diffCol < 0 ? diffCol : p1;

                return (int) Math.round(Math.pow(max, 2));
            }
        });

        // f4 - lak505d
        heuristicList.add(new HeuristicFunction() {
            public int apply(int startId, int goalId, int ncols) {
                int startRow = startId / ncols; //y
                int goalRow = goalId / ncols; //y_g
                int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                int startCol = startId - startRow * ncols; //x
                int goalCol = goalId - goalRow * ncols; //x_g
                int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                int min = Math.min(goalRow, diffRow);

                int max1 = Math.max(100, min + startRow);

                int max2 = diffCol - diffRow < 0 ? diffRow : diffCol;

                return max1 * max1 * max2;
            }
        });

        // f5 - orz103d
        heuristicList.add(new HeuristicFunction() {
            public int apply(int startId, int goalId, int ncols) {
                int startRow = startId / ncols; //y
                int goalRow = goalId / ncols;
                int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                int startCol = startId - startRow * ncols; //x
                int goalCol = goalId - goalRow * ncols; //x_g
                int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                double p1 = 11.5 * Math.sqrt(((startRow + 1) + diffRow) * ((startRow + 1) + diffRow) * diffRow);
                double p2 = diffCol * (goalRow + 1); //goalRow (heuristic indexes from 1 not 0 so add +1)
                double max = Math.max(p1, p2);
                return (int) Math.round(max);
            }
        });

        // f6 - ost000a
        heuristicList.add(new HeuristicFunction() {
            public int apply(int startId, int goalId, int ncols) {
                int startRow = startId / ncols; //y
                int goalRow = goalId / ncols; //y_g
                int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                int startCol = startId - startRow * ncols; //x
                int goalCol = goalId - goalRow * ncols; //x_g
                int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                int min = Math.max(diffCol, goalCol);

                int max = Math.max(diffRow, diffCol + min);

                return max * max;
            }
        });

        // f7 - wall-hugging: max (deltaX ^ 2, deltaY ^ 2)
        heuristicList.add(new HeuristicFunction() {
            public int apply(int startId, int goalId, int ncols) {
                int startRow = startId / ncols; //y
                int goalRow = goalId / ncols; //y_g
                int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                int startCol = startId - startRow * ncols; //x
                int goalCol = goalId - goalRow * ncols; //x_g
                int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                if (diffRow > diffCol) return diffRow * diffRow;
                else return diffCol * diffCol;
            }
        });

        return heuristicList;
    }
}
