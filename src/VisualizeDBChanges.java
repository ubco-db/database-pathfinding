import database.DBStats;
import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDynamicDB2;
import dynamic.Walls;
import map.GameMap;
import search.*;

import java.util.ArrayList;
import java.util.HashMap;

public class VisualizeDBChanges {
    final static String DB_PATH = "dynamic/databases/";
    final static String DBA_STAR_DB_PATH = DB_PATH + "DBA/";
    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;

    final static int CUTOFF = 250; // The maximum # of moves for hill-climbing checks.
    final static int GRID_SIZE = 16;
    final static int NUM_NEIGHBOUR_LEVELS = 1; // # of neighbor levels for HCDPS

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        GameMap map = new GameMap(PATH_TO_MAP);

        // fix start, colour it (yellow)
        int startId = 13411;
        ArrayList<Integer> goalIds = new ArrayList<>();

        // iterate over all goals (open spots no wall)
        for (int i = 16; i < map.rows; i++) {
            for (int j = 0; j < map.cols; j++) {
                if (!map.isWall(i, j)) {
                    goalIds.add(map.getId(i, j));
                }
            }
        }

        // remove startId from list of goals
        goalIds.remove((Integer) startId);

        // print number of goals (6175)
        System.out.println("Number of goals: " + goalIds.size());

        // compute paths to all goals, store in HashMap of arrays (goal state as key)
        HashMap<Integer,ArrayList<SearchState>> paths = new HashMap<>();
        for (int goalId: goalIds) {
            System.out.println("Goal Id:" + goalId); // 11922 throws an index out of bounds
            paths.put(goalId, getDBAStarPath(map, startId, goalId));
        }

        // add wall
        ArrayList<SearchState> wallLocation = new ArrayList<>();
        Walls.addWall(PATH_TO_MAP, wallLocation, map);
        int wallId = 13558;
        wallLocation.add(new SearchState(wallId));
        map = new GameMap(PATH_TO_MAP);

        // remove wallId from list of goals
        goalIds.remove((Integer) wallId);

        // iterate over all goals (open spots no wall, remove the spot where a wall was added)
        // compare each path with stored path to same location, if identical, do nothing, if not, colour it (purple)
        ArrayList<SearchState> newPath;
        ArrayList<SearchState> oldPath;
        for (int goalId: goalIds) {
            newPath = getDBAStarPath(map, startId, goalId);
            oldPath = paths.get(goalId);
            // compare each path with stored path to the same location
            boolean pathEqual = isPathEqual(newPath, oldPath);
        }

        // remove wall
        Walls.removeWall(PATH_TO_MAP, wallLocation, map);
        long timeTaken = System.currentTimeMillis() - startTime;
        System.out.println("This run took: " + timeTaken);
    }

    private static boolean isPathEqual(ArrayList<SearchState> newPath, ArrayList<SearchState> oldPath) {
        // if path length differs, they are not equal
        if (newPath.size() != oldPath.size()) return false;

        for (int i = 0; i < newPath.size(); i++) {
            // comparing SearchStates (have an equals-method)
            if (!newPath.get(i).equals(oldPath.get(i))) return false;
        }
        return true;
    }

    private static String getImageName(String wallStatus) {
        return DBA_STAR_DB_PATH + wallStatus + MAP_FILE_NAME;
    }

    private static ArrayList<SearchState> getDBAStarPath(GameMap map, int startId, int goalId) {
        long currentTime;
        StatsRecord stats = new StatsRecord();

        SearchProblem problem = new MapSearchProblem(map);
        GenHillClimbing pathCompressAlgDba = new GenHillClimbing(problem, 10000);

        SubgoalDynamicDB2 database = new SubgoalDynamicDB2(); // DP matrix in adjacency list representation (computed at run-time)

        // GreedyHC map abstraction
        DBStats dbStats = new DBStats();
        DBStats.init(dbStats);

        DBStatsRecord rec = new DBStatsRecord(dbStats.getSize());
        rec.addStat(0, "dbaStar (" + NUM_NEIGHBOUR_LEVELS + ")");
        rec.addStat(1, GRID_SIZE);
        rec.addStat(3, CUTOFF);
        rec.addStat(4, MAP_FILE_NAME);
        rec.addStat(5, map.rows);
        rec.addStat(6, map.cols);

        currentTime = System.currentTimeMillis();
        map = map.sectorAbstract2(GRID_SIZE);
        long resultTime = System.currentTimeMillis() - currentTime;
        rec.addStat(12, resultTime);
        rec.addStat(10, resultTime);
        rec.addStat(11, map.states);
        rec.addStat(7, map.states);
        dbStats.addRecord(rec);

        SearchProblem tmpProb = new MapSearchProblem(map);
        GameDB gameDB = new GameDB(tmpProb);

        currentTime = System.currentTimeMillis();
        database.computeIndex(tmpProb, rec);
        rec.addStat(23, System.currentTimeMillis() - currentTime);

        database = gameDB.computeDynamicDB(database, pathCompressAlgDba, rec, NUM_NEIGHBOUR_LEVELS);

        database.init();

        map.computeComplexity(rec);
        dbStats.addRecord(rec);
        database.setProblem(problem);
        database.verify(pathCompressAlgDba);

        SearchState start = new SearchState(startId);
        SearchState goal = new SearchState(goalId);

        DBAStar dbaStar = new DBAStar(problem, map, database);
        // ArrayList<SearchState> subgoals = dbaStar.getSubgoals();
        return dbaStar.computePath(start, goal, stats);
    }
}
