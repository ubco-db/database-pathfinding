package search;

import java.util.ArrayList;


import database.SubgoalDB;
import database.SubgoalDBExact;
import database.SubgoalDBRecord;

import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DBAStar implements SearchAlgorithm {
    private final SubgoalDB database;
    private final SearchProblem problem;
    private final GameMap map;
    private final RegionSearchProblem abstractProblem;
    private final ArrayList<SearchState> subgoals;

    private static final Logger logger = LogManager.getLogger(DBAStar.class);

    public DBAStar(SearchProblem problem, GameMap abstractMap, SubgoalDB database) {
        this.database = database;
        this.problem = problem;
        this.map = abstractMap;
        abstractProblem = abstractMap.getAbstractProblem();
        this.subgoals = new ArrayList<SearchState>();
    }

    public ArrayList<SearchState> computePath(SearchState start, SearchState goal, StatsRecord stats) {
        ArrayList<SubgoalDBRecord> used = new ArrayList<>();
        ArrayList<SearchState> newPath;

        SearchState currentStart = start;
        SearchState currentGoal;

        SubgoalDBRecord currentRecord;

        int currentIndex;
        int[] subgoalList;
        int cutoff = 10000;

        HillClimbing subgoalSearchAlg = new HillClimbing(problem, cutoff);
        AStar astar = new AStar(problem);
        subgoals.clear();

        long startTime, endTime;

        ArrayList<SearchState> path = new ArrayList<>();

        ArrayList<SearchState> pathStart = new ArrayList<>();
        ArrayList<SearchState> pathEnd = new ArrayList<>();

        startTime = System.nanoTime();
        SearchState startRegionCenter = abstractProblem.findRegion2(start, pathStart, 0);
        SearchState goalRegionCenter = abstractProblem.findRegion2(goal, pathEnd, 1);

//        System.out.println("Start region center: " + startRegionCenter);
//        System.out.println("Goal region center: " + goalRegionCenter);

        // Search the database for records
        ArrayList<SubgoalDBRecord> records = database.findNearest(problem, startRegionCenter, goalRegionCenter, subgoalSearchAlg, 1, stats, null);

        // Need this condition for complete separation case, not entirely sure why
        if (startRegionCenter != null && goalRegionCenter != null) {
            records = database.findNearest(problem, startRegionCenter, goalRegionCenter, subgoalSearchAlg, 1, stats, null);
        }
        endTime = System.nanoTime();

        if (records != null && !records.isEmpty()) {
            currentRecord = records.getFirst();
            logger.debug(currentRecord);
            used.add(currentRecord);
            logger.debug("Using subgoal record from database: " + currentRecord.toString());
            subgoalList = currentRecord.getSubgoalList();
            currentIndex = -1;

            currentGoal = new SearchState(currentRecord.getStartId());
            subgoals.add(currentGoal);
            endTime = System.nanoTime();
            stats.updateMaxTime(endTime - startTime);
            // TODO
            SearchUtil.computePathCost(path, stats, problem); // Compute path costs up to this point

            while (true) {
                if (currentStart == start) { // start optimizations
                    if (subgoalList == null || subgoalList.length == 0) {
                        newPath = subgoalSearchAlg.computePath(currentStart, currentGoal, stats);
                    } else {
                        SearchState front_Op = new SearchState(subgoalList[0]);
                        newPath = subgoalSearchAlg.computePath(currentStart, front_Op, stats);
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
                    newPath = subgoalSearchAlg.computePath(currentStart, currentGoal, stats);
                    if (newPath == null && currentStart != goalRegionCenter) {
                        currentGoal = goalRegionCenter;
                        newPath = subgoalSearchAlg.computePath(currentStart, currentGoal, stats);
                    } else if (newPath == null) {
                        if (!pathEnd.isEmpty()) {
                            newPath = pathEnd;
                        } else newPath = astar.computePath(currentStart, currentGoal, stats);
                    }
                } else { // regular case;
                    newPath = subgoalSearchAlg.computePath(currentStart, currentGoal, stats);
                }

                if (newPath == null) {
                    logger.warn("DBAStar: Unable to find subgoal path between " + problem.idToString(currentStart.id) + " and " + problem.idToString(currentGoal.id));
                    return null;
                }

                path = SearchUtil.mergePaths(path, newPath);

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
                        used.add(currentRecord);
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

            SearchUtil.computePathCost(path, stats, problem);

            endTime = System.nanoTime();
            stats.updateMaxTime(endTime - startTime);
        }
        return path;
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

    public GameMap getDBAStarMap() {
        return map;
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