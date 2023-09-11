import map.GameMap;
import scenario.Problem;
import scenario.Scenario;
import scenario.StatsCompare;
import search.*;
import util.HeuristicFunction;

import java.util.ArrayList;

/**
 * Does a comparison of A* algorithms with different heuristic functions on scenarios.
 */
public class EvaluateHeuristic {

    @SuppressWarnings("unchecked")
    public static void main(String[] argv) {
        String[] scenarios = {
                "012_100",              //0
                "maze_5_1250_hard",     //1
                "mm_8_1024",            //2 -- TODO: running out of heap space in this scenario (because paths not found)
                "mm_cs_4_1000",         //3 -- TODO: ArrayIndexOutOfBoundsException: Index 9784627 out of bounds for length 6000000
                "mm_de_5_1000",         //4 -- TODO: ArrayIndexOutOfBoundsException: Index 13505876 out of bounds for length 6000000
        };

        /*
         * Run configuration variables are below.
         */
        int scenarioToRun = 0;      // 4; // Index into scenarios array. Change this to run a different scenario.

        ArrayList<HeuristicFunction> heuristicList = new ArrayList<>();

        int algorithmsLength = populateHeuristicList(heuristicList).size();

        String[] algNames = new String[algorithmsLength];
        for (int i = 0; i < algorithmsLength; i++)
            algNames[i] = "A* with f" + i;
        /*
         * End of configuration variables.
         */

        String scenarioFileName = scenarios[scenarioToRun];
        String scenarioName = "scenarios/" + scenarioFileName + ".txt";
        SearchProblem problem = null;

        // Load the scenario information
        Scenario scenario = new Scenario(scenarioName);
        int numProblems = scenario.getNumProblems();
        String lastMapName = null; // Stores last map name. Used to detect when
        // switch to new map in a scenario.

        // Store information algorithm statistics.
        ArrayList<StatsRecord>[] problemStats = new ArrayList[algorithmsLength];
        StatsRecord[] overallStats = new StatsRecord[algorithmsLength];
        GameMap baseMap; // The base map for a scenario problem.

        for (int i = 0; i < algorithmsLength; i++) {
            problemStats[i] = new ArrayList<StatsRecord>(
                    scenario.getNumProblems());
            overallStats[i] = new StatsRecord();
        }

        // These variables track if A* statistics match those in the scenario file.
        int count = 0;

        ArrayList<SearchState> path;
        StatsRecord stats;

        int startProblem = 0;

        for (int i = startProblem; i < numProblems; i++) {

            Problem p = scenario.getProblem(i);

            String mapName = p.getMapName();

            // Load map and/or database if different from last problem
            if (lastMapName == null || !lastMapName.equals(mapName)) {
                baseMap = new GameMap(mapName);
                problem = new MapSearchProblem(baseMap);
                lastMapName = mapName;
            }

            // Create start and goal states for the problem
            SearchState start = new SearchState(p.getStart());
            SearchState goal = new SearchState(p.getGoal());

            System.out.println("\n\nPerforming problem: " + (i + 1)
                    + " on map: " + mapName + " Start: "
                    + problem.idToString(p.getStart().id) + " Goal: "
                    + problem.idToString(p.getGoal().id));
            boolean validProblem = true;
            // Run each algorithm on the problem
            for (int j = 0; j < algorithmsLength; j++) {
                stats = new StatsRecord();

                long currentTime = System.currentTimeMillis();

                AStarHeuristic astarh = new AStarHeuristic(problem, heuristicList.get(j));
                path = astarh.computePath(start, goal, stats);

                if (path == null) {
                    System.out.println("A*H_f" + j + " is unable to find path between "
                            + start + " and " + goal);
                    j = 3;
                    validProblem = false;
                    for (int k = 0; k < algorithmsLength; k++)
                        problemStats[k].add(new StatsRecord());
                    continue; // Do not try to do the other algorithms
                }

                stats.setTime(System.currentTimeMillis() - currentTime);
                StatsCompare.mergeRecords(overallStats[j], stats);
                problemStats[j].add(stats);

                // Count the # of revisits
                int revis = SearchUtil.countRevisits(path);

                if (revis > 0) {
                    System.out.println("Revisits: " + revis);
                    SearchUtil.distanceRevisits(path);
                }
                stats.setRevisits(revis);
            } // end test algorithms on a problem


            if (!validProblem)
                continue;

            count++;

        } // end problem loop

        System.out.println("\n\nOverall results of " + count + " problems.");
        StatsCompare.compareRecords(overallStats, algNames);

    }

    private static ArrayList<HeuristicFunction> populateHeuristicList(ArrayList<HeuristicFunction> heuristicList) {
        // f0
        heuristicList.add(
                new HeuristicFunction() {
                    public int apply(int startId, int goalId, int ncols) {
                        int startRow = startId / ncols;
                        int goalRow = goalId / ncols;
                        int diffRow = startRow - goalRow;

                        int bit31 = diffRow >> 31;                   // Compute its absolute value
                        diffRow = (diffRow ^ bit31) - bit31;

                        int diffCol = ((startId - startRow * ncols) - (goalId - goalRow * ncols));
                        bit31 = diffCol >> 31;                      // Compute its absolute value
                        diffCol = (diffCol ^ bit31) - bit31;

                        if (diffRow > diffCol)
                            return diffCol * 14 + (diffRow - diffCol) * 10;
                        else
                            return diffRow * 14 + (diffCol - diffRow) * 10;
                    }
                }
        );


        // f1
        heuristicList.add(
                new HeuristicFunction() {
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
                }
        );

        // f2
        heuristicList.add(
                new HeuristicFunction() {
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
                }
        );

        // f3
        heuristicList.add(
                new HeuristicFunction() {
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
                }
        );

        // f4
        heuristicList.add(
                new HeuristicFunction() {
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
                }
        );

        // f5
        heuristicList.add(
                new HeuristicFunction() {
                    public int apply(int startId, int goalId, int ncols) {
                        int startRow = startId / ncols; //y
                        int goalRow = goalId / ncols; //y_g
                        int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                        int startCol = startId - startRow * ncols; //x
                        int goalCol = goalId - goalRow * ncols; //x_g
                        int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                        double p1 = 11.5 * Math.sqrt((startRow + diffRow) * (startRow + diffRow) * diffRow);
                        double p2 = diffCol * goalRow;
                        double max = Math.max(p1, p2);
                        return (int) Math.round(max);
                    }
                }
        );

        // f6
        heuristicList.add(
                new HeuristicFunction() {
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
                }
        );

        return heuristicList;
    }
}
