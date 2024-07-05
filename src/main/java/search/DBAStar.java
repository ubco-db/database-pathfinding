package search;

import database.SubgoalDB;
import database.SubgoalDBRecord;
import database.SubgoalDynamicDB3;
import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.ArrayList;

public class DBAStar implements SearchAlgorithm {
    private final SubgoalDynamicDB3 database;
    private final SearchProblem problem;
    private final GameMap map;
    private final ArrayList<SearchState> subgoals;

    private static final Logger logger = LogManager.getLogger(DBAStar.class);

    public DBAStar(SearchProblem problem, GameMap abstractMap, SubgoalDynamicDB3 database) {
        this.database = database;
        this.problem = problem;
        this.map = abstractMap;
        this.subgoals = new ArrayList<SearchState>();
    }

    public ArrayList<SearchState> computePath(SearchState start, SearchState goal, StatsRecord stats) {
        SearchState currentStart = start, currentGoal;

        ArrayList<SearchState> path = new ArrayList<>();

        HillClimbing subgoalSearchAlg = new HillClimbing(problem, 10000); // TODO: Consider passing cutoff as parameter // 1185
        AStar astar = new AStar(problem); // 14831

        long startTime = System.nanoTime(), endTime;

        this.subgoals.clear();

        // Search the database for records
        int startRegionId = map.getRegionFromState(start.id);
        int goalRegionId = map.getRegionFromState(goal.id);

        // If the start region and goal region are the same, or if they are neighbours, use AStar instead
        if (startRegionId == goalRegionId) { // 15509
            path = astar.computePath(start, goal, stats); // 40904
            return path;
        }

        // Get search states for region reps of start and goal region
        SearchState startRegionCenter = new SearchState(map.getRegionRepFromState(start.getId()));
        SearchState goalRegionCenter = new SearchState(map.getRegionRepFromState(goal.getId()));

        // Compute path from start to startRegionCenter and goalRegionCenter to goal
        ArrayList<SearchState> pathStart = astar.computePath(start, startRegionCenter, stats); // 51341
        ArrayList<SearchState> pathEnd = astar.computePath(goalRegionCenter, goal, stats); // 53090

        ArrayList<SubgoalDBRecord> records = database.findNearest(problem, startRegionId - GameMap.START_NUM, goalRegionId - GameMap.START_NUM, subgoalSearchAlg, stats); // 292727

        ArrayList<SearchState> newPath;
        SubgoalDBRecord currentRecord;
        int[] subgoalList;
        int currentIndex;

        if (records != null && !records.isEmpty()) {
            currentRecord = records.getFirst();
//            logger.debug(currentRecord);
//            logger.debug("Using subgoal record from database: " + currentRecord.toString());
            subgoalList = currentRecord.getSubgoalList();
            currentIndex = -1;

            currentGoal = new SearchState(currentRecord.getStartId());
            subgoals.add(currentGoal);
            endTime = System.nanoTime();
            stats.updateMaxTime(endTime - startTime);
            SearchUtil.computePathCost(path, stats, problem); // Compute path costs up to this point

            while (true) {
                if (currentStart == start) { // start optimizations
                    if (subgoalList == null || subgoalList.length == 0) {
                        newPath = subgoalSearchAlg.computePath(currentStart, currentGoal, stats); // 2244
                    } else {
                        SearchState front_Op = new SearchState(subgoalList[0]);
                        newPath = subgoalSearchAlg.computePath(currentStart, front_Op, stats); // 32021
                        if (newPath != null) {
                            currentGoal = front_Op;
                            currentIndex++;
                        }
                    }
                    if (newPath == null) {
                        if (!pathStart.isEmpty()) {
                            newPath = pathStart;
                        } else newPath = astar.computePath(currentStart, currentGoal, stats);
                    }
                } else if (currentGoal == goal) { // end optimizations
                    newPath = subgoalSearchAlg.computePath(currentStart, currentGoal, stats); // 32004
                    if (newPath == null && currentStart != goalRegionCenter) {
                        currentGoal = goalRegionCenter;
                        newPath = subgoalSearchAlg.computePath(currentStart, currentGoal, stats); // 4064
                    } else if (newPath == null) {
                        if (!pathEnd.isEmpty()) {
                            newPath = pathEnd;
                        } else newPath = astar.computePath(currentStart, currentGoal, stats);
                    }
                } else { // regular case;
                    newPath = subgoalSearchAlg.computePath(currentStart, currentGoal, stats); // 47555
                }

                if (newPath == null) {
                    logger.warn("DBAStar: Unable to find subgoal path between " + problem.idToString(currentStart.id) + " and " + problem.idToString(currentGoal.id));
                    map.outputImage("error.png", path, subgoals);
                    System.out.println(start.id);
                    System.out.println(goal.id);
                    SearchState[] searchStates = {new SearchState(currentStart), new SearchState(currentGoal)};
                    map.drawPoints("error2.png", searchStates, Color.RED);
                    return null;
                }

                path = SearchUtil.mergePaths(path, newPath); // 12573

                SearchState curr = newPath.getLast();

                if (curr.equals(goal)) break;

                if (!curr.equals(currentGoal)) { // Must have been interrupted
                    currentStart = newPath.getLast();
                    logger.warn("Detect interruption.  Trying database lookup at: " + currentStart);
                    logger.info("Length of path at interrupt: " + path.size());
                    records = database.findNearest(problem, currentStart, goal, subgoalSearchAlg, 1, stats, null);
                    if (!records.isEmpty()) {
                        currentRecord = records.getFirst();
                        subgoalList = currentRecord.getSubgoalList();
                        // System.out.println("Using subgoal record from database: "+currentRecord.toString());
                        currentIndex = -1;
                        // Always go for start
                        currentGoal = new SearchState(currentRecord.getStartId());
                    } else currentGoal = goal;

                    continue;
                }

                if (currentGoal == goal) break;

                currentStart = currentGoal;
                currentIndex++;
                if (subgoalList == null) {
                    if (currentGoal.id == currentRecord.getGoalId()) currentGoal = goal;
                    else currentGoal = new SearchState(currentRecord.getGoalId());
                } else if (currentIndex < subgoalList.length) {
                    currentGoal = new SearchState(subgoalList[currentIndex]);
                    subgoals.add(currentGoal);
                } else currentGoal = goal; // Go towards global goal
            }

            SearchUtil.computePathCost(path, stats, problem); // 1558

            endTime = System.nanoTime();
            stats.updateMaxTime(endTime - startTime);
        }
        return path;
    }

    private boolean areNeighbours(int startRegionId, int goalRegionId) {
        try {
            return map.getGroups().get(startRegionId).getNeighborIds().contains(goalRegionId); // 15509
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isPath(SearchState start, SearchState goal, StatsRecord stats) {
        return computePath(start, goal, stats) != null;
    }

    @Override
    public boolean isPath(int startId, int goalId, StatsRecord stats) {
        return computePath(new SearchState(startId), new SearchState(goalId), stats) != null;
    }

    public ArrayList<SearchState> getSubgoals() {
        return subgoals;
    }

    public SubgoalDB getDatabase() {
        return database;
    }

    public SearchProblem getProblem() {
        return problem;
    }

    public GameMap getMap() {
        return map;
    }
}
