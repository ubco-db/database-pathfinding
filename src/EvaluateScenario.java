import database.DBStats;
import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDB;
import database.SubgoalDBExact;
import database.SubgoalDynamicDB2;
import map.GameMap;
import scenario.Problem;
import scenario.Scenario;
import scenario.StatsCompare;
import search.AStar;
import search.AStarHeuristic;
import search.GenHillClimbing;
import search.DBAStar;
import search.MapSearchProblem;
import search.SearchAbstractAlgorithm;
import search.SearchProblem;
import search.SearchState;
import search.SearchUtil;
import search.StatsRecord;
import search.SubgoalSearch;
import util.HeuristicFunction;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Does a comparison of A versus other algorithms on scenarios.
 *
 * @author rlawrenc
 */
public class EvaluateScenario {

    @SuppressWarnings("unchecked")
    public static void main(String[] argv) {
        String[] scenarios = {"012_100",        //0
                "mm_8_1024",                    //1
                "mm_cs_4_1000",                 //2
                "mm_de_5_1000",                 //3
                "mm_cs_4_1000_hard",            //4
                "mm_de_5_1000_hard",            //5
                "small",                        //6
                "maze_5_1250_hard",             //7
                "smallRoom",                    //8
        };
        String[] algorithmNames = {"A*", "HCDPS+", "Cover2", "DBA*", "A*+heuristic"};
        String[] abbrv = {"a", "hcdps+", "cover2", "dba", "AHrt"};

        /*
         * Run configuration variables are below.
         */

        int scenarioToRun = 0;              // Index into scenarios array (8 scenarios total). Change this to run a different scenario.
        int[] algorithms = {0, 1, 3};       // Select up to three algorithms to run

        int heuristicId = 1;                // (0~5) heuristic function id passing to A* with arbitrary heuristic
        int cutoff = 250;                   // For knnLRTA* the maximum # of moves for hill-climbing checks.
        double badProblemSubOpt = 3;        // If any algorithm has a path cost suboptimality worse than this for a given problem, the problem results are output as images.
        int numNeighborLevels = 1;          // # of neighbor levels for HCDPS
        int gridSize = 16;                  // Used by PRA* to define grid size
        int trailHeads = 1000;              // Maximum # of trailheads used by cover algorithm
        int maxRecords = 500000;            // Maximum # of records after the cover algorithm
        int HCDPSRecords = 0;
        int dbaStarRecords = 0;

        long[] revisits = new long[3];      // Count the # of times that the path revisits a state
        long[] distrevisits = new long[3];  // Sum of distance between state revisits.
        // E.g. If path goes to state A then B then A, distance is 1.
        // Sum all those distances to see how far you travel before a revisit.

        // boolean buildPath = true;        // Algorithms will build path not just compute cost of path.

        boolean showPaths = false;          // If true, paths computed by each algorithm are printed to standard output.
        boolean showImage = true;           // If true, will produce a PNG image for the path produced by regardless if the path is good or not.

        int dbType = 2;                     // 2 - adjacency list representation (DP computed at run-time)

        String imageDir = "images/";
        String dbPath = "databases/";
        String dbaStarDatabasePath = dbPath + "DBA/";
        String coverDatabasePath = dbPath + "cover/";
        String hcDatabasePath;
        hcDatabasePath = dbPath + "HC/";
        String binaryOutputPath = "results/";

        String[] algNames = new String[algorithms.length];
        for (int i = 0; i < algorithms.length; i++)
            algNames[i] = algorithmNames[algorithms[i]];

        /*
         * End of configuration variables.
         */


        /*
         * Heuristic functions list
         */

        ArrayList<HeuristicFunction> heuristicList = new ArrayList<>();

        // f1
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

        // f2
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

        // f3
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

        // f4
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

        // f5
        heuristicList.add(new HeuristicFunction() {
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
        });

        // f6
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

        /*
         * End of heuristic functions list
         */

        String scenarioFileName = scenarios[scenarioToRun];
        String scenarioName = "scenarios/" + scenarioFileName + ".txt";

        String mapFileName = null;
        DBStatsRecord rec;
        SearchProblem problem = null;

        // Load the scenario information
        Scenario scenario = new Scenario(scenarioName);
        int numProblems = scenario.getNumProblems();
        String lastMapName = null; // Stores last map name. Used to detect when switch to new map in a scenario.

        // Store information on bad problems, subgoal databases if needed by the algorithm, and algorithm statistics.
        ArrayList<StatsRecord>[] badProblems = new ArrayList[algorithms.length];
        ArrayList<StatsRecord>[] problemStats = new ArrayList[algorithms.length];
        StatsRecord[] overallStats = new StatsRecord[algorithms.length];
        DBStats[] dbStats = new DBStats[algorithms.length];
        ArrayList<Integer> badProblemNum = new ArrayList<>();
        ArrayList<Integer> noSubgoal = new ArrayList<>();
        SubgoalDB[] databases = new SubgoalDB[algorithms.length];
        GameMap[] maps = new GameMap[algorithms.length];
        GameMap baseMap = null; // The base map for a scenario problem.
        ArrayList<SearchState>[] paths = new ArrayList[algorithms.length];
        ArrayList<SearchState>[] subgoals = new ArrayList[algorithms.length];

        for (int i = 0; i < algorithms.length; i++) {
            badProblems[i] = new ArrayList<StatsRecord>();
            if (algorithms[i] == 1) // HCDPS
                databases[i] = new SubgoalDB();
            else databases[i] = null;
            problemStats[i] = new ArrayList<StatsRecord>(scenario.getNumProblems());
            overallStats[i] = new StatsRecord();
            subgoals[i] = new ArrayList<SearchState>();
            dbStats[i] = null;
        }

        // These variables track if A* statistics match those in the scenario file.
        int count = 0, countAStarCosts = 0, countAStarDiff = 0;

        // This is used to nicely format the output of problem numbers.
        int numDigits = 2;
        if (numProblems > 100) numDigits = 3;

        long startTime = System.currentTimeMillis();
        boolean mapSwitch = false;

        ArrayList<SearchState> path = null;
        StatsRecord stats;

        int startProblem = 0;

        for (int i = startProblem; i < numProblems; i++) {

            Problem p = scenario.getProblem(i);

            String mapName = p.getMapName();
            stats = new StatsRecord();
            mapSwitch = false;

            // Load map and/or database if different from last problem
            if (lastMapName == null || !lastMapName.equals(mapName)) {
                mapSwitch = true;
                baseMap = new GameMap(mapName);
                problem = new MapSearchProblem(baseMap);
                mapFileName = mapName;
                int slashIndex = mapName.lastIndexOf('/');
                if (slashIndex >= 0) mapFileName = mapName.substring(slashIndex + 1);
                mapFileName = mapFileName.substring(0, mapFileName.indexOf('.'));
                lastMapName = mapName;
            }

            // Create start and goal states for the problem
            SearchState start = new SearchState(p.getStart());
            SearchState goal = new SearchState(p.getGoal());

            System.out.println("\n\nPerforming problem: " + (i + 1) + " on map: " + mapName + " Start: " + problem.idToString(p.getStart().id) + " Goal: " + problem.idToString(p.getGoal().id));
            boolean validProblem = true;
            // Run each algorithm on the problem
            for (int j = 0; j < algorithms.length; j++) {
                stats = new StatsRecord();

                // System.gc();
                // try {Thread.sleep(10); }
                // catch (Exception e) {}

                SearchAbstractAlgorithm alg;
                GenHillClimbing pathCompressAlg;
                SubgoalSearch subgoalSearch;

                long currentTime = System.currentTimeMillis();

                switch (algorithms[j]) {
                    case 0: // A*
                        AStar astar = new AStar(problem);
                        path = astar.computePath(start, goal, stats);

                        if (path == null) {
                            System.out.println("A* is unable to find path between " + start + " and " + goal);
                            j = 3;
                            validProblem = false;
                            // problemStats[0].add(stats);
                            problemStats[0].add(new StatsRecord()); // Filler records for all // QUESTION: what does that mean?
                            problemStats[1].add(new StatsRecord());
                            problemStats[2].add(new StatsRecord());
                            continue; // Do not try to do the other algorithms // QUESTION: should this be a 'break'?
                        }

                        // Verify that A* is getting path that is expected
                        if (p.getOptimalTravelCost() != stats.getPathCost())
                            System.out.println("A* path costs is different than expected.  Expected: " + p.getOptimalTravelCost() + "\tActual: " + stats.getPathCost());
                        else {
                            int difficulty = (int) (p.getAStarDifficulty() * 1000);
                            System.out.println("Expected: " + difficulty + " Actual: " + (stats.getStatesExpanded() * 1000 / stats.getPathLength()));
                            countAStarCosts++;

                            if (difficulty == stats.getStatesExpanded() * 1000 / stats.getPathLength()) {
                                System.out.println("A* path cost and difficulty are as expected.");
                                countAStarDiff++;
                            } else {
                                System.out.println("A* path cost is as expected BUT A* difficulty does not match.");
                            }
                        }
                        break;
                    case 1: // HCDPS - Hill-climbing dynamic programming search with  no-precomputed database (just dynamic programming table and path fragments that are built up on the fly)
                        alg = new GenHillClimbing(problem, cutoff);
                        pathCompressAlg = new GenHillClimbing(problem, 10000);
                        // Allow unlimited hill-climbing when compressing records in the database (between record subgoals)

                        if (mapSwitch) { // Load abstract map and database
                            System.out.println("Loading database.");
                            String fname, mapfname;

                            databases[j] = new SubgoalDynamicDB2();   // DP matrix in adjacency list representation (computed at run-time)

                            fname = hcDatabasePath + mapFileName + "_HCDLD" + numNeighborLevels + "_" + cutoff + ".dat" + dbType;
                            mapfname = hcDatabasePath + mapFileName + "_HCE_map_cut_" + cutoff + ".txt";

                            if (!databases[j].exists(fname) || !databases[j].load(fname)) {
                                // Generate database
                                // Always generate map abstraction with database (as needs some of the info - specifically the groups)
                                System.out.println("Loading map and performing abstraction...");
                                // GreedyHC map abstraction
                                if (dbStats[j] == null) {
                                    dbStats[j] = new DBStats();
                                    DBStats.init(dbStats[j]);
                                }
                                rec = new DBStatsRecord(dbStats[j].getSize());
                                rec.addStat(0, "HCDPS+ (" + numNeighborLevels + " - " + dbType + ")");
                                rec.addStat(1, numNeighborLevels);
                                rec.addStat(3, cutoff);
                                rec.addStat(4, mapFileName);
                                rec.addStat(5, baseMap.rows);
                                rec.addStat(6, baseMap.cols);
                                GameMap greedyMap = baseMap.reachableGridAbstract(cutoff * 2, alg, rec);

                                System.out.println("Greedy abstraction.  States: " + greedyMap.states + " Time: " + (System.currentTimeMillis() - currentTime));
                                // System.exit(1);
                                maps[j] = greedyMap;
                                maps[j].save(mapfname);

                                System.out.println("Exporting map with areas.");
                                maps[j].outputImage(hcDatabasePath + mapFileName + "_HC.png", null, null);

                                System.out.println("Exporting map with areas and centroids.");
                                maps[j].computeCentroidMap().outputImage(hcDatabasePath + mapFileName + "_HC_Centroid.png", null, null);

                                SearchProblem tmpProb = new MapSearchProblem(greedyMap);
                                GameDB database = new GameDB(tmpProb);

                                // Computes index on abstract state to ground-level state mapping
                                currentTime = System.currentTimeMillis();
                                ((SubgoalDynamicDB2) databases[j]).computeIndex(tmpProb, rec);

                                rec.addStat(23, System.currentTimeMillis() - currentTime);

                                System.out.println("Generating database.");
                                currentTime = System.currentTimeMillis();
                                databases[j] = database.computeDynamicDB((SubgoalDynamicDB2) databases[j], pathCompressAlg, rec, numNeighborLevels);

                                System.out.println("Time to compute HCDPS database: " + (System.currentTimeMillis() - currentTime));

                                ((SubgoalDynamicDB2) databases[j]).init();

                                databases[j].exportDB(fname);
                                maps[j].computeComplexity(rec);
                                dbStats[j].addRecord(rec);
                            } else { // Load map
                                System.out.println("Loading map.");
                                maps[j] = new GameMap();
                                maps[j].loadMap(mapfname);
                                databases[j].setProblem(problem);
                            }
                            System.out.println("Verifying database.");
                            databases[j].verify(pathCompressAlg);
                            System.out.println("Database verification complete.");
                            System.out.println("Databases loaded.");
                        }
                        currentTime = System.currentTimeMillis();
                        /* Same algorithm, just different database. */
                        subgoalSearch = new SubgoalSearch(problem, databases[j], cutoff, pathCompressAlg, alg);
                        path = subgoalSearch.computePath(start, goal, stats);
                        subgoals[j] = subgoalSearch.getSubgoals();
                        if (subgoals[j].size() == 0)
                            noSubgoal.add(i + 1); // Keep track of problems where we found no subgoal
                        else stats.setSubgoals(subgoals[j].size());
                        break;
                    case 2: // Cover2 (based on HCDPS regioning)

                        alg = new GenHillClimbing(problem, cutoff);
                        pathCompressAlg = new GenHillClimbing(problem, 10000);
                        if (mapSwitch) { // Load abstract map and database
                            String mapfname = coverDatabasePath + mapFileName + "_L" + numNeighborLevels + "_C" + cutoff + "_map.txt";
                            String dbfname = coverDatabasePath + mapFileName + "_L" + numNeighborLevels + "_C" + cutoff + "_R" + maxRecords + "_db.txt";
                            databases[j] = new SubgoalDBExact(); // For regular HCDPS
                            // databases[j] = new SubgoalDynamicDB3(); // For dynamic HCDPS (dHCDPS)

                            if (!databases[j].exists(dbfname) || !databases[j].load(dbfname)) {
                                if (dbStats[j] == null) {
                                    dbStats[j] = new DBStats();
                                    DBStats.init(dbStats[j]);
                                }
                                rec = new DBStatsRecord(dbStats[j].getSize());
                                rec.addStat(0, "Cover " + trailHeads);
                                rec.addStat(1, trailHeads);
                                rec.addStat(3, cutoff);
                                rec.addStat(4, mapFileName);
                                rec.addStat(5, baseMap.rows);
                                rec.addStat(6, baseMap.cols);

                                currentTime = System.currentTimeMillis();
                                maps[j] = baseMap.coverAbstract(trailHeads, alg);
                                long resultTime = System.currentTimeMillis() - currentTime;

                                rec.addStat(12, resultTime);
                                rec.addStat(10, resultTime);
                                int areas = maps[j].states;
                                rec.addStat(11, areas);
                                rec.addStat(7, areas);
                                System.out.println("Time to generate abstraction: " + resultTime);
                                maps[j].save(mapfname);
                                GameMap tmp = maps[j].computeCentroidMap();
                                tmp.outputImage(coverDatabasePath + mapFileName + "_C" + cutoff + ".png", null, null);

                                SearchProblem tmpProb = new MapSearchProblem(maps[j]);
                                GameDB database = new GameDB(tmpProb);

                                currentTime = System.currentTimeMillis();
                                ((SubgoalDBExact) databases[j]).computeIndex(tmpProb, rec); // This computes mapping from base states to abstract states
                                rec.addStat(23, System.currentTimeMillis() - currentTime);

                                System.out.println("Generating database.");
                                currentTime = System.currentTimeMillis();

                                // This would compute a standard HCDPS full-path database
                                databases[j] = database.computeDBDP2(databases[j], pathCompressAlg, rec, numNeighborLevels);
                                // This version computes an dHCDPS style database (only stores base paths)
                                // databases[j] =
                                // database.computeDynamicDB((SubgoalDynamicDB3)
                                // databases[j], pathCompressAlg,
                                // rec,numNeighborLevels);
                                System.out.println("Time to compute HCDPS database: " + (System.currentTimeMillis() - currentTime));
                                ((SubgoalDBExact) databases[j]).init();

                                // Perform curation on map trailhead paths eliminating ones that are easy
                                // database.computeCoverDB2(alg, rec,
                                // (SubgoalDBExact) databases[j],
                                // maps[j].getGroups(), cutoff, minOpt, maxOpt,
                                // maxRecords);

                                long overallTime = (System.currentTimeMillis() - currentTime);
                                rec.addStat(10, overallTime); // Make sure overall time includes HCDPS generation as well as cover algorithm

                                databases[j].exportDB(dbfname);
                                databases[j].verify(alg);
                                dbStats[j].addRecord(rec);
                            }
                        }
                        // Using HCDPS style lookup
                        subgoalSearch = new SubgoalSearch(problem, databases[j], cutoff, pathCompressAlg, alg);
                        currentTime = System.currentTimeMillis();
                        path = subgoalSearch.computePath(start, goal, stats);
                        subgoals[j] = subgoalSearch.getSubgoals();
                        if (subgoals[j].size() == 0) noSubgoal.add(i + 1);
                        else stats.setSubgoals(subgoals[j].size());
                        break;
                    case 3: // DBA*
                        alg = new GenHillClimbing(problem, cutoff);
                        GenHillClimbing pathCompressAlgj2 = new GenHillClimbing(problem, 10000);

                        if (mapSwitch) { // Load abstract map and database
                            System.out.println("Loading database.");
                            String fname2, mapfname2;

                            databases[j] = new SubgoalDynamicDB2();   // DP matrix in adjacency list representation (computed at run-time)

                            fname2 = dbaStarDatabasePath + mapFileName + "_DBA-STAR_G" + gridSize + "_N" + numNeighborLevels + "_C" + cutoff + ".dat";
                            mapfname2 = dbaStarDatabasePath + mapFileName + "_DBA-STAR_map_C" + cutoff + ".txt";

                            if (!databases[j].exists(fname2) || !databases[j].load(fname2)) {
                                System.out.println("Loading map and performing abstraction...");

                                // GreedyHC map abstraction
                                if (dbStats[j] == null) {
                                    dbStats[j] = new DBStats();
                                    DBStats.init(dbStats[j]);
                                }
                                rec = new DBStatsRecord(dbStats[j].getSize());
                                rec.addStat(0, "dbaStar (" + numNeighborLevels + ")");
                                rec.addStat(1, gridSize);
                                rec.addStat(3, cutoff);
                                rec.addStat(4, mapFileName);
                                rec.addStat(5, baseMap.rows);
                                rec.addStat(6, baseMap.cols);

                                currentTime = System.currentTimeMillis();
                                maps[j] = baseMap.sectorAbstract2(gridSize);
                                long resultTime = System.currentTimeMillis() - currentTime;
                                rec.addStat(12, resultTime);
                                rec.addStat(10, resultTime);
                                rec.addStat(11, maps[j].states);
                                rec.addStat(7, maps[j].states);
                                dbStats[j].addRecord(rec);

                                System.out.println("Exporting map with areas.");
                                maps[j].outputImage(dbaStarDatabasePath + mapFileName + "_J2.png", null, null);

                                System.out.println("Exporting map with areas and centroids.");
                                maps[j].computeCentroidMap().outputImage(dbaStarDatabasePath + mapFileName + "_J2_Centroid.png", null, null);

                                SearchProblem tmpProb = new MapSearchProblem(maps[j]);
                                GameDB database = new GameDB(tmpProb);

                                currentTime = System.currentTimeMillis();
                                // ((SubgoalDBExact) databases[j]).computeIndex(tmpProb, rec);
                                ((SubgoalDynamicDB2) databases[j]).computeIndex(tmpProb, rec);
                                rec.addStat(23, System.currentTimeMillis() - currentTime);

                                System.out.println("Generating database.");
                                currentTime = System.currentTimeMillis();

                                databases[j] = database.computeDynamicDB((SubgoalDynamicDB2) databases[j], pathCompressAlgj2, rec, numNeighborLevels);
                                System.out.println("Time to compute DBAStar database: " + (System.currentTimeMillis() - currentTime));

                                ((SubgoalDynamicDB2) databases[j]).init();

                                databases[j].exportDB(fname2);
                                maps[j].computeComplexity(rec);
                                dbStats[j].addRecord(rec);
                            } else { // Load map
                                maps[j] = baseMap.sectorAbstract2(gridSize);
                            }
                            databases[j].setProblem(problem);
                            System.out.println("Verifying database.");
                            databases[j].verify(pathCompressAlgj2);
                            System.out.println("Database verification complete.");
                            System.out.println("Databases loaded.");
                            dbaStarRecords = databases[j].getSize();
                        }
                        currentTime = System.currentTimeMillis();

                        DBAStar dbaStar = new DBAStar(problem, maps[j], databases[j]);
                        path = dbaStar.computePath(start, goal, stats);
                        subgoals[j] = dbaStar.getSubgoals();

                        if (subgoals[j].size() == 0)
                            noSubgoal.add(i + 1); // Keep track of problems where we found no subgoal
                        else stats.setSubgoals(subgoals[j].size());
                        break;

                    case 4: // A* with arbitrary heuristic
                        AStarHeuristic astarh = new AStarHeuristic(problem, heuristicList.get(heuristicId));
                        path = astarh.computePath(start, goal, stats);

                        if (path == null) {
                            System.out.println("A*H is unable to find path between " + start + " and " + goal);
                            j = 3;
                            validProblem = false;
                            // problemStats[0].add(stats);
                            problemStats[0].add(new StatsRecord()); // Filler records for all
                            problemStats[1].add(new StatsRecord());
                            problemStats[2].add(new StatsRecord());
                            continue; // Do not try to do the other algorithms // QUESTION: Should this be a 'break'?
                        }

                        // Verify that A* is getting path that is expected
                        if (p.getOptimalTravelCost() != stats.getPathCost())
                            System.out.println("A*H path costs is different than expected.  Expected: " + p.getOptimalTravelCost() + "\tActual: " + stats.getPathCost());
                        else {
                            int difficulty = (int) (p.getAStarDifficulty() * 1000);
                            System.out.println("Expected: " + difficulty + " Actual: " + (stats.getStatesExpanded() * 1000 / stats.getPathLength()));
                            countAStarCosts++;
                            if (difficulty == stats.getStatesExpanded() * 1000 / stats.getPathLength()) {
                                System.out.println("A*H path cost and difficulty are as expected.");
                                countAStarDiff++;
                            } else System.out.println("A*H path cost is as expected BUT A* difficulty does not match.");
                        }
                        break;
                }

                stats.setTime(System.currentTimeMillis() - currentTime);
                StatsCompare.mergeRecords(overallStats[j], stats);
                problemStats[j].add(stats);
                paths[j] = path;

                // Count the # of revisits
                int revis = SearchUtil.countRevisits(path);
                int drevis = 0;

                if (revis > 0) {
                    System.out.println("Revisits: " + revis);
                    drevis = SearchUtil.distanceRevisits(path);
                }
                stats.setRevisits(revis);

                revisits[j] += revis;
                distrevisits[j] += drevis;
                if (showPaths) {
                    System.out.println("Path: ");
                    SearchUtil.printPath(problem, path);
                }
                if (showImage)
                    baseMap.outputImage(imageDir + padNum(i + 1, numDigits) + "_" + abbrv[algorithms[j]] + ".png", path, subgoals[j]);
            } // end test algorithms on a problem


            if (!validProblem) continue;

            int loc = i - startProblem;
            StatsRecord rec1 = problemStats[0].get(loc), rec2 = problemStats[1].get(loc), rec3 = problemStats[2].get(loc);
            StatsCompare.compareRecords(rec1, rec2, rec3, algNames);
            count++;

			/*
			// Bad problem for either DLRTA* or knnLRTA
			boolean badAlg2 = true;
			if (rec2.getPathCost() > badProblemSubOpt * rec1.getPathCost()
					|| rec2.getPathCost() < rec1.getPathCost()
					|| (badAlg2 && (rec3.getPathCost() > badProblemSubOpt
							* rec1.getPathCost() || rec3.getPathCost() < rec1
							.getPathCost()))) { // A bad problem.
				badProblems[0].add(rec1);
				badProblems[1].add(rec2);
				badProblems[2].add(rec3);
				badProblemNum.add(i + 1);
				if (paths[1].size() < 40000)
					baseMap.outputImage(imageDir + padNum(i + 1, numDigits)
							+ "_" + abbrv[algorithms[1]] + ".png", paths[1],
							subgoals[1]);
				if (paths[2].size() < 40000)
					baseMap.outputImage(imageDir + padNum(i + 1, numDigits)
							+ "_" + abbrv[algorithms[2]] + ".png", paths[2],
							subgoals[2]);
			}
		 */
        } // end problem loop


        //TODO

//        ArrayList<SearchState> a = new ArrayList<SearchState>();
//        a.add(new SearchState(7003));
//        a.add(new SearchState(7151));
//        a.add(new SearchState(7299));
//        a.add(new SearchState(7447));
//        a.add(new SearchState(7595));
//        a.add(new SearchState(7743));
//        //Walls.removeWall("C:\\Users\\45222098\\javaworkspace\\GeneralSearch\\maps\\dMap\\012.map",a,baseMap);
//        Walls.addWall("maps/dMap/012.map", a, baseMap);
//        String dfname = dbaStarDatabasePath + mapFileName + "_DBA-STAR_G" + gridSize + "_N" + numNeighborLevels + "_C" + cutoff + ".dat";
//        SubgoalDB recomputeDB = new SubgoalDynamicDB3();
//        recomputeDB.load(dfname);
//        RegionSearchProblem.recompute(baseMap, a, gridSize);

        System.out.println("\n\nOverall results of " + count + " problems.");
        StatsCompare.compareRecords(overallStats[0], overallStats[1],
                overallStats[2], algNames);

        System.out.println("Revisits: " + revisits[0] + "\t" + revisits[1]
                + "\t" + revisits[2] + "\t");
        System.out.println("Distance: " + distrevisits[0] + "\t"
                + distrevisits[1] + "\t" + distrevisits[2] + "\t");
        System.out.println("Avg. dist: " + distrevisits[0] * 1.0 / revisits[0]
                + "\t" + distrevisits[1] * 1.0 / revisits[1] + "\t"
                + distrevisits[2] * 1.0 / revisits[2] + "\t");
        System.out.println("% revisit: " + revisits[0] * 100.0
                / overallStats[0].getPathLength() + "\t" + revisits[1] * 100.0
                / overallStats[1].getPathLength() + "\t" + revisits[2] * 100.0
                / overallStats[2].getPathLength() + "\t");
        System.out.println("Experiment time: "
                + (System.currentTimeMillis() - startTime) / 1000);
        System.out.println("# of problems where A* costs matching expected: "
                + countAStarCosts + " where difficulty matches: "
                + countAStarDiff);

        System.out.println("# of problems with no subgoal found in databases: "
                + noSubgoal.size());

        System.out.println("# of bad problems: " + badProblems[2].size());
        for (int i = 0; i < badProblems[2].size(); i++) {
            int problemNum = badProblemNum.get(i);
            boolean foundSubgoal = !noSubgoal.contains(problemNum);
            System.out.println("\nProblem #: " + badProblemNum.get(i)
                    + " Used a subgoal in DB: " + foundSubgoal);
            StatsCompare.compareRecords(badProblems[0].get(i),
                    badProblems[1].get(i), badProblems[2].get(i), algNames);
        }

        // Output binary results
        try {
            for (int k = 0; k < 3; k++) {
                String binaryOutputName = binaryOutputPath + scenarioFileName;
                String extendAlgName = "";
                binaryOutputName = binaryOutputName + "_" + abbrv[algorithms[k]];
                if (algorithms[k] == 1) // HCDPS+
                    extendAlgName += "_L" + numNeighborLevels + "_C" + cutoff + "_D" + dbType;
                else if (algorithms[k] == 2) // Cover2
                    extendAlgName += "_L" + numNeighborLevels + "_C" + cutoff + "_R" + maxRecords;
                binaryOutputName += extendAlgName + ".txt";

                PrintWriter outFile = new PrintWriter(binaryOutputName);
                outFile.println("Num\tLen\t SubL\t Cost\t SubC\t SE\t SU\t Time\t OL\t CL\t MM\tMT");
                for (int i = 0; i < problemStats[0].size(); i++) {
                    outFile.print((i + 1) + "\t");
                    problemStats[k].get(i).outputCSV(problemStats[0].get(i), outFile);
                }
                outFile.close();

                // Output database generation statistics if any
                if (dbStats[k] != null) {
                    binaryOutputName = binaryOutputPath + scenarioFileName + "_" + abbrv[algorithms[k]] + extendAlgName + "_generation.txt";
                    PrintWriter outFile2 = new PrintWriter(binaryOutputName);
                    dbStats[k].outputNames(outFile2);
                    dbStats[k].outputData(outFile2);
                    outFile2.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String padNum(int num, int digits) {
        StringBuilder st = new StringBuilder("" + num);

        for (int i = 0; i < digits - st.length(); i++) {
            st.insert(0, "0");
        }

        return st.toString();
    }
}
