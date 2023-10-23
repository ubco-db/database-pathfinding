import map.GameMap;
import search.*;

import java.util.ArrayList;
import java.util.HashMap;

public class ComputeAllOptimalPaths {
    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        GameMap map = new GameMap(PATH_TO_MAP);
        SearchProblem problem = new MapSearchProblem(map);
        AStar aStar = new AStar(problem);

        // fix start
        int startId = 13411;
        SearchState start = new SearchState(13411);

        ArrayList<Integer> goalIds = new ArrayList<>();
        // iterate over all goals (open spots no wall)
        for (int i = 16; i < map.rows; i++) {
            for (int j = 0; j < map.cols; j++) {
                if (!map.isWall(i, j)) {
                    goalIds.add(map.getId(i, j));
                }
            }
        }

        // compute paths to all goals, store in HashMap of arrays (goal state as key)
        HashMap<Integer, ArrayList<SearchState>> paths = new HashMap<>();
        ArrayList<StatsRecord> statsRecords = new ArrayList<>();
        for (int goalId : goalIds) {
            StatsRecord record = new StatsRecord();
            paths.put(goalId, aStar.computePath(start, new SearchState(goalId), record));
            statsRecords.add(record);
        }

        // remove startId from list of goals
        goalIds.remove((Integer) startId);

        // print number of goals (6175 on 012.map)
        System.out.println("Number of goals: " + goalIds.size());


        long timeTaken = System.currentTimeMillis() - startTime;

        long totalSeconds = timeTaken / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        System.out.println();
        System.out.println("This run took: " + minutes + " minutes, " + seconds + " seconds");
    }
}
