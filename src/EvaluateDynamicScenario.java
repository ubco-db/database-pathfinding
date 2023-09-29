import database.DBStats;
import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDynamicDB2;
import map.GameMap;
import scenario.Problem;
import scenario.Scenario;
import search.GenHillClimbing;
import search.MapSearchProblem;
import search.SearchAbstractAlgorithm;
import search.SearchProblem;
import search.StatsRecord;

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

        final int CUTOFF = 250; // The maximum # of moves for hill-climbing checks.
        final int GRID_SIZE = 16;
        int NUM_NEIGHBOUR_LEVELS = 1; // # of neighbor levels for HCDPS

        String lastMapName = null;
        GameMap baseMap = null; // The base map for a scenario problem.
        SearchProblem problem = null;
        String mapFileName = null;

        StatsRecord stats;
        DBStatsRecord rec;
        boolean mapSwitch;
        int dbaStarRecords = 0;

        Scenario scenario = new Scenario(SCENARIO_NAME);
        int numProblems = scenario.getNumProblems();

        SubgoalDynamicDB2 database = new SubgoalDynamicDB2();
        DBStats[] dbStats = new DBStats[1];
        GameMap[] maps = new GameMap[1];

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

            long currentTime = System.currentTimeMillis();

            SearchAbstractAlgorithm alg = new GenHillClimbing(problem, CUTOFF);
            GenHillClimbing pathCompressAlgDba = new GenHillClimbing(problem, 10000);

            if (mapSwitch) { // Load abstract map and database
                System.out.println("Loading database.");
                String fname2;

                database = new SubgoalDynamicDB2();   // DP matrix in adjacency list representation (computed at run-time)

                fname2 = DBA_STAR_DB_PATH + mapFileName + "_DBA-STAR_G" + GRID_SIZE + "_N" + NUM_NEIGHBOUR_LEVELS + "_C" + CUTOFF + ".dat";

                if (!database.exists(fname2) || !database.load(fname2)) {
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
                    GameDB gameDB = new GameDB(tmpProb);

                    currentTime = System.currentTimeMillis();
                    // ((SubgoalDBExact) database).computeIndex(tmpProb, rec);
                    database.computeIndex(tmpProb, rec);
                    rec.addStat(23, System.currentTimeMillis() - currentTime);

                    System.out.println("Generating gameDB.");
                    currentTime = System.currentTimeMillis();

                    database = gameDB.computeDynamicDB(database, pathCompressAlgDba, rec, NUM_NEIGHBOUR_LEVELS);
                    System.out.println("Time to compute DBAStar gameDB: " + (System.currentTimeMillis() - currentTime));

                    database.init();

                    database.exportDB(fname2);
                    maps[0].computeComplexity(rec);
                    dbStats[0].addRecord(rec);
                } else { // Load map
                    maps[0] = baseMap.sectorAbstract2(GRID_SIZE);
                }
                database.setProblem(problem);
                System.out.println("Verifying database.");
                database.verify(pathCompressAlgDba);
                System.out.println("Database verification complete.");
                System.out.println("Databases loaded.");
                dbaStarRecords = database.getSize();
            }
        }
    }
}
