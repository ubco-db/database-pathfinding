import database.DBStats;
import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDB;
import database.SubgoalDynamicDB2;
import map.GameMap;
import scenario.Problem;
import scenario.Scenario;
import scenario.StatsCompare;
import search.DBAStar;
import search.GenHillClimbing;
import search.MapSearchProblem;
import search.SearchAbstractAlgorithm;
import search.SearchProblem;
import search.SearchState;
import search.SearchUtil;
import search.StatsRecord;
import search.SubgoalSearch;

import java.util.ArrayList;

public class EvaluateDynamicScenario {
    public static void main(String[] args) {
        // IDEA: copy over DBA* code from EvaluateScenario
        // perform DBA*
        // build wall
        // recompute database
        // compare databases
        final String SCENARIO_FILE_NAME = "012_100";
        final String SCENARIO_NAME = "scenarios/" + SCENARIO_FILE_NAME + ".txt";
        final String DB_PATH = "dynamic/databases/";
        final String DBA_STAR_DB_PATH = DB_PATH + "DBA/";
        String imageDir = "dynamic/images/";

        String[] abbrv = {"dba"};
        String[] algNames = {"DBA*"};

        final int CUTOFF = 250; // The maximum # of moves for hill-climbing checks.
        final int GRID_SIZE = 16;
        int NUM_NEIGHBOUR_LEVELS = 1; // # of neighbor levels for HCDPS

        long[] revisits = new long[3];      // Count the # of times that the path revisits a state
        long[] distrevisits = new long[3];  // Sum of distance between state revisits.

        String lastMapName = null;
        GameMap baseMap = null; // The base map for a scenario problem.
        SearchProblem problem = null;
        String mapFileName = null;

        boolean showPaths = false;          // If true, paths computed by each algorithm are printed to standard output.
        boolean showImage = true;           // If true, will produce a PNG image for the path produced by regardless if the path is good or not.

        StatsRecord stats;
        DBStatsRecord rec;
        boolean mapSwitch;
        int dbaStarRecords = 0;
        ArrayList<SearchState> path;

        Scenario scenario = new Scenario(SCENARIO_NAME);
        int numProblems = scenario.getNumProblems();

        SubgoalDB[] databases = new SubgoalDB[1];
        DBStats[] dbStats = new DBStats[1];
        GameMap[] maps = new GameMap[1];
        ArrayList<SearchState>[] subgoals = new ArrayList[1];
        StatsRecord[] overallStats = new StatsRecord[1];
        ArrayList<Integer> noSubgoal = new ArrayList<>();
        ArrayList<StatsRecord>[] problemStats = new ArrayList[0];
        problemStats[0] = new ArrayList<StatsRecord>(scenario.getNumProblems());
        ArrayList<SearchState>[] paths = new ArrayList[0];

        int count = 0;
        int startProblem = 0;
        int numDigits = 2;
        // if (numProblems > 100) numDigits = 3;

        boolean validProblem = true;

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

            stats = new StatsRecord();

            SearchAbstractAlgorithm alg;
            GenHillClimbing pathCompressAlg;
            SubgoalSearch subgoalSearch;

            long currentTime = System.currentTimeMillis();

            alg = new GenHillClimbing(problem, CUTOFF);
            GenHillClimbing pathCompressAlgDba = new GenHillClimbing(problem, 10000);

            if (mapSwitch) { // Load abstract map and database
                System.out.println("Loading database.");
                String fname2, mapfname2;

                databases[0] = new SubgoalDynamicDB2();   // DP matrix in adjacency list representation (computed at run-time)

                fname2 = DBA_STAR_DB_PATH + mapFileName + "_DBA-STAR_G" + GRID_SIZE + "_N" + NUM_NEIGHBOUR_LEVELS + "_C" + CUTOFF + ".dat";
                mapfname2 = DBA_STAR_DB_PATH + mapFileName + "_DBA-STAR_map_C" + CUTOFF + ".txt";

                if (!databases[0].exists(fname2) || !databases[0].load(fname2)) {
                    System.out.println("Loading map and performing abstraction...");

                    // GreedyHC map abstraction
                    if (dbStats[0] == null) {
                        dbStats[0] = new DBStats();
                        DBStats.init(dbStats[0]);
                    }
                    rec = new DBStatsRecord(dbStats[0].getSize());
                    rec.addStat(0, "dbaStar (" + NUM_NEIGHBOUR_LEVELS + ")");
                    rec.addStat(1, GRID_SIZE);
                    rec.addStat(3, CUTOFF);
                    rec.addStat(4, mapFileName);
                    rec.addStat(5, baseMap.rows);
                    rec.addStat(6, baseMap.cols);

                    currentTime = System.currentTimeMillis();
                    maps[0] = baseMap.sectorAbstract2(GRID_SIZE);
                    long resultTime = System.currentTimeMillis() - currentTime;
                    rec.addStat(12, resultTime);
                    rec.addStat(10, resultTime);
                    rec.addStat(11, maps[0].states);
                    rec.addStat(7, maps[0].states);
                    dbStats[0].addRecord(rec);

                    System.out.println("Exporting map with areas.");
                    maps[0].outputImage(DBA_STAR_DB_PATH + mapFileName + "_DBA.png", null, null);

                    System.out.println("Exporting map with areas and centroids.");
                    maps[0].computeCentroidMap().outputImage(DBA_STAR_DB_PATH + mapFileName + "_DBA_Centroid.png", null, null);

                    SearchProblem tmpProb = new MapSearchProblem(maps[0]);
                    GameDB database = new GameDB(tmpProb);

                    currentTime = System.currentTimeMillis();
                    // ((SubgoalDBExact) databases[0]).computeIndex(tmpProb, rec);
                    ((SubgoalDynamicDB2) databases[0]).computeIndex(tmpProb, rec);
                    rec.addStat(23, System.currentTimeMillis() - currentTime);

                    System.out.println("Generating database.");
                    currentTime = System.currentTimeMillis();

                    databases[0] = database.computeDynamicDB((SubgoalDynamicDB2) databases[0], pathCompressAlgDba, rec, NUM_NEIGHBOUR_LEVELS);
                    System.out.println("Time to compute DBAStar database: " + (System.currentTimeMillis() - currentTime));

                    ((SubgoalDynamicDB2) databases[0]).init();

                    databases[0].exportDB(fname2);
                    maps[0].computeComplexity(rec);
                    dbStats[0].addRecord(rec);
                } else { // Load map
                    maps[0] = baseMap.sectorAbstract2(GRID_SIZE);
                }
                databases[0].setProblem(problem);
                System.out.println("Verifying database.");
                databases[0].verify(pathCompressAlgDba);
                System.out.println("Database verification complete.");
                System.out.println("Databases loaded.");
                dbaStarRecords = databases[0].getSize();
            }
            currentTime = System.currentTimeMillis();

            DBAStar dbaStar = new DBAStar(problem, maps[0], databases[0]);
            path = dbaStar.computePath(start, goal, stats);
            subgoals[0] = dbaStar.getSubgoals();

            if (subgoals[0].size() == 0)
                noSubgoal.add(i + 1); // Keep track of problems where we found no subgoal
            else stats.setSubgoals(subgoals[0].size());


            stats.setTime(System.currentTimeMillis() - currentTime);
            StatsCompare.mergeRecords(overallStats[0], stats);
            problemStats[0].add(stats);
            paths[0] = path;

            // Count the # of revisits
            int revis = SearchUtil.countRevisits(path);
            int drevis = 0;

            if (revis > 0) {
                System.out.println("Revisits: " + revis);
                drevis = SearchUtil.distanceRevisits(path);
            }
            stats.setRevisits(revis);

            revisits[0] += revis;
            distrevisits[0] += drevis;
            if (showPaths) {
                System.out.println("Path: ");
                SearchUtil.printPath(problem, path);
            }
            if (showImage)
                baseMap.outputImage(imageDir + padNum(i + 1, numDigits) + "_" + abbrv[0] + ".png", path, subgoals[0]);

            if (!validProblem) continue;

            int loc = i - startProblem;
            StatsRecord rec1 = problemStats[0].get(loc), rec2 = problemStats[1].get(loc), rec3 = problemStats[2].get(loc);
            StatsCompare.compareRecords(rec1, rec2, rec3, algNames);
            count++;
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
