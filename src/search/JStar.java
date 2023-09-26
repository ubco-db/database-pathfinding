package search;

import database.SubgoalDB;
import database.SubgoalDBRecord;
import map.GameMap;

import java.util.ArrayList;

public class JStar implements SearchAlgorithm {
    private SubgoalDB database;
    private SearchProblem problem;
    private GameMap map;
    private RegionSearchProblem abstractProblem;
    private ArrayList<SearchState> subgoals;

    public JStar(SearchProblem problem, GameMap abstractmap, SubgoalDB database) {
        this.database = database;
        this.problem = problem;
        this.map = abstractmap;
        abstractProblem = abstractmap.getAbstractProblem();
        this.subgoals = new ArrayList<SearchState>();
    }

    public ArrayList<SearchState> computePath(SearchState start, SearchState goal, StatsRecord stats) {
        ArrayList<SubgoalDBRecord> used = new ArrayList<SubgoalDBRecord>();
        ArrayList<SearchState> newPath;
        SearchState currentStart = start;
        SearchState currentGoal = goal;
        SubgoalDBRecord currentRecord = null;
        int currentIndex = 0;
        int[] subgoalList = null;
        int cutoff = 10000;
        GenHillClimbing subgoalSearchAlg = new GenHillClimbing(problem, cutoff);
        AStar astar = new AStar(problem);
        subgoals.clear();

        long startTime, endTime, startTime2;


        ArrayList<SearchState> path = new ArrayList<SearchState>();

        ArrayList<SearchState> pathStart = new ArrayList<SearchState>();

        ArrayList<SearchState> pathEnd = new ArrayList<SearchState>();

        startTime = System.nanoTime();
        SearchState startRegionCenter = abstractProblem.findRegion2(start, pathStart, 0);
        SearchState goalRegionCenter = abstractProblem.findRegion2(goal, pathEnd, 1);

        // Search the database for records
        startTime2 = System.nanoTime();
        ArrayList<SubgoalDBRecord> records = database.findNearest(problem, startRegionCenter, goalRegionCenter, subgoalSearchAlg, 1, stats, null);

/*		System.out.println("The Record:");
		System.out.println(records.toString());
		System.out.println("Record End");*/

        endTime = System.nanoTime();

        if (records.size() >= 1) {
            currentRecord = records.get(0);
            used.add(currentRecord);
            // System.out.println("Using subgoal record from database: "+currentRecord.toString(problem));
            // System.out.println("Using subgoal record from database: "+currentRecord.getId());
            subgoalList = currentRecord.getSubgoalList();
            currentIndex = -1;

            currentGoal = new SearchState(currentRecord.getStartId());
            subgoals.add(currentGoal);
            endTime = System.nanoTime();
            stats.updateMaxTime(endTime - startTime);
            SearchUtil.computePathCost(path, stats, problem); // Compute path
            // costs up to
            // this point

            while (true) {

                if (currentStart == start) {

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
                        if (pathStart.size() != 0) {
                            newPath = pathStart;
                        } else newPath = astar.computePath(currentStart, currentGoal, stats);
                    }

                } else if (currentGoal == goal) {
                    newPath = subgoalSearchAlg.computePath(currentStart, currentGoal, stats);
                    if (newPath == null && currentStart != goalRegionCenter) {
                        currentGoal = goalRegionCenter;
                        newPath = subgoalSearchAlg.computePath(currentStart, currentGoal, stats);
                    } else if (newPath == null) {

                        if (pathEnd.size() != 0) {
                            newPath = pathEnd;
                        } else newPath = astar.computePath(currentStart, currentGoal, stats);
                    }

                } else {
                    newPath = subgoalSearchAlg.computePath(currentStart, currentGoal, stats);
                }

                if (newPath == null) { // TODO: Not sure what to do in this case
                    System.out.println("JStar Unable to find subgoal path between " + problem.idToString(currentStart.id) + " and " + problem.idToString(currentGoal.id));
                    currentGoal = goal;
                    return null;
                }

                path = SearchUtil.mergePaths(path, newPath);

                SearchState curr = newPath.get(newPath.size() - 1);

                if (curr.equals(goal)) break;

                if (!curr.equals(currentGoal)) { // Must have been interrupted
                    int times = cutoff;
                    if (times > 100) times = 100;

                    currentStart = newPath.get(newPath.size() - 1);
                    System.out.println("Detect interruption.  Trying database lookup at: " + currentStart);
                    System.out.println("Length of path at interrupt: " + path.size());
                    records = database.findNearest(problem, currentStart, goal, subgoalSearchAlg, 1, stats, null);
                    if (records.size() >= 1) {
                        currentRecord = records.get(0);
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
        // return null;
    }

}
