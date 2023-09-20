package search;

import database.SubgoalDB;
import database.SubgoalDBRecord;

import java.util.ArrayList;


public class SubgoalSearch implements SearchAlgorithm {
    private SearchProblem problem;
    private SubgoalDB database;
    private int cutoff;
    private SearchAlgorithm subgoalSearchAlg;
    private SearchAlgorithm optCheckSearchAlg;
    private ArrayList<SearchState> subgoals;

    public SubgoalSearch(SearchProblem problem, SubgoalDB database, int cutoff, SearchAlgorithm subgoalSearchAlg, SearchAlgorithm optCheckSearchAlg) {
        this.problem = problem;
        this.database = database;
        this.cutoff = cutoff;
        this.subgoalSearchAlg = subgoalSearchAlg;
        this.optCheckSearchAlg = optCheckSearchAlg;
        this.subgoals = new ArrayList<SearchState>();
    }


    public ArrayList<SearchState> getSubgoals() {
        return subgoals;
    }


    public ArrayList<SearchState> computePath(SearchState start, SearchState goal, StatsRecord stats) {
        boolean OPTIMIZE_START_DB_PATH = true;
        boolean OPTIMIZE_END_DB_PATH = true;
        boolean OPTIMIZE_CHECK = true;
        ArrayList<SubgoalDBRecord> used = new ArrayList<>();
        ArrayList<SearchState> path, newPath;
        SearchState currentGoal, currentStart;
        SubgoalDBRecord currentRecord = null;
        int currentIndex = 0;
        int[] subgoalList = null;
        boolean startOptimizationDone = false;

        subgoals.clear();

        long startTime, endTime, startTime2;
        startTime = System.nanoTime();
        // path = null;
        path = subgoalSearchAlg.computePath(start, goal, stats);
        // path = optCheckSearchAlg.computePath(start, goal, stats);
        if (path != null) {    // System.out.println("Global goal is HC reachable from start.");
            subgoals.add(goal);
            SearchUtil.computePathCost(path, stats, problem);
            stats.updateMaxTime(System.nanoTime() - startTime);
            return path;        // Search is done using only hill climbing
        }

        endTime = System.nanoTime();
        // System.out.println("Time for HC reachable check at start: "+(endTime-startTime));

        path = new ArrayList<SearchState>();
        currentGoal = goal;        // Default to global goal by default
        currentStart = start;

        // Search the database for records
        startTime2 = System.nanoTime();
        ArrayList<SubgoalDBRecord> records = database.findNearest(problem, start, goal, subgoalSearchAlg, 1, stats, null);
        endTime = System.nanoTime();
        // System.out.println("Time for database lookup: "+(endTime-startTime2));

        if (records.size() >= 1) {
            currentRecord = records.get(0);
            used.add(currentRecord);
            // System.out.println("Using subgoal record from database: "+currentRecord.toString(problem));
            // System.out.println("Using subgoal record from database: "+currentRecord.getId());
            subgoalList = currentRecord.getSubgoalList();
            currentIndex = -1;
            if (OPTIMIZE_START_DB_PATH) {    // Try to hill-climb to first subgoal instead of database goal start as will improve optimality

                if (subgoalList == null || subgoalList.length == 0) {
                    currentGoal = new SearchState(currentRecord.getStartId());
                    subgoals.add(currentGoal);
                    startOptimizationDone = true;
                } else {
                    currentGoal = new SearchState(subgoalList[0]);
                    newPath = optCheckSearchAlg.computePath(currentStart, currentGoal, stats);
                    if (newPath != null) {    // Possible to hill climb to subgoal
                        currentIndex = 1;
                        startOptimizationDone = true;
                        currentStart = currentGoal;
                        subgoals.add(currentGoal);
                        path = SearchUtil.mergePaths(path, newPath);
                        // System.out.println("Start optimization successful.  Currently at: "+problem.idToString(currentGoal.id));
                        // Advance to next subgoal.  Note if only one have to potentially do END PATH optimization as well
                        if (currentIndex >= subgoalList.length) {    // Default is to go to goal of database record then we will hill climb from there.
                            currentGoal = new SearchState(currentRecord.getGoalId());

                            if (OPTIMIZE_END_DB_PATH) {    // newPath = AStar.reachableHillClimbing(map, currentStart, goal, cutoff, stats);
                                newPath = optCheckSearchAlg.computePath(currentStart, goal, stats);
                                if (newPath != null) {    // From last subgoal spot can reach global goal using hill climbing.  That completes the path.
                                    // Trying to go to the global goal from the last subgoal may not work but will often be shorter than going to the database goal point then our desired goal.
                                    path = SearchUtil.mergePaths(path, newPath);
                                    SearchUtil.computePathCost(path, stats, problem);
                                    // System.out.println("End optimization successful from: "+problem.idToString(currentStart.id));
                                    stats.updateMaxTime(System.nanoTime() - startTime);
                                    return path;
                                }
                            }
                        } // Ran out of subgoals in path optimization
                        else {    // Use next subgoal in list
                            currentGoal = new SearchState(subgoalList[1]);
                            subgoals.add(currentGoal);
                        } // Using next subgoal
                    } // Able to directly hill climb to first subgoal of database record from our starting point
                } // No subgoals
            } // optimizing start of db path

            if (!startOptimizationDone && OPTIMIZE_CHECK) {    // Unable to hill climb to first subgoal - try to hill climb somewhere between start and first subgoal
                // Build path from start of record to first subgoal
                subgoalList = currentRecord.getSubgoalList();
                if (subgoalList != null && subgoalList.length > 0) {
                    newPath = subgoalSearchAlg.computePath(new SearchState(currentRecord.getStartId()), new SearchState(subgoalList[0]), stats);
                    // SearchUtil.printPath(problem, newPath);
                    // Now find farthest spot on path that is HC-reachable
                    currentGoal = SearchUtil.computeBinaryReachable(newPath, optCheckSearchAlg, currentStart, stats);
                    //	System.out.println("Binary start optimization successful.  Starting point: "+problem.idToString(currentGoal.id)+" Index in path: "+newPath.indexOf(currentGoal));
                    startOptimizationDone = true;
                }
            }

            if (!startOptimizationDone) {    // If no start optimization succeeds, go to record start
                currentGoal = new SearchState(currentRecord.getStartId());
                subgoals.add(currentGoal);
            }
        } else {
            System.out.println("DATABASE RECORD NOT FOUND.");
        }

        endTime = System.nanoTime();
        // System.out.println("Total Time for processing first step: "+(endTime-startTime));
        stats.updateMaxTime(endTime - startTime);

        // IDEA: Once have one DB record follow it all the way to goal using one subgoal at a time.
        // Use only hill climbing first if possible.
        // If exhausted all subgoals, then go to global goal if it is HC reachable from last subgoal.  Otherwise, go to goal of DB record then to our desired goal.

        SearchUtil.computePathCost(path, stats, problem);        // Compute path costs up to this point

        while (true) {

            newPath = subgoalSearchAlg.computePath(currentStart, currentGoal, stats);

            if (newPath == null) {    // TODO: Not sure what to do in this case
                if (currentGoal == goal) {    // Special case to handle situation where did not guarantee HC-reachability in both directions
                    System.out.println("WARNING: Unable to reach goal " + problem.idToString(currentStart.id) + " from currentGoal: " + problem.idToString(currentGoal.id));
                    System.out.println("Finding reverse path.");
                    newPath = subgoalSearchAlg.computePath(currentGoal, currentStart, stats);
                    if (newPath == null) {
                        System.out.println("CRITICAL ERROR: Reverse path search failed.");
                        return null;
                    }
                    path = SearchUtil.mergeReversePaths(path, newPath);
                    SearchUtil.computePathCost(path, stats, problem);
                    System.out.println("Reverse path search successful");
                    return path;
                }
                System.out.println("Unable to find subgoal path between " + problem.idToString(currentStart.id) + " and " + problem.idToString(currentGoal.id));
                currentGoal = goal;
                //System.exit(1);
                return null;
            }

            path = SearchUtil.mergePaths(path, newPath);

            SearchState curr = newPath.get(newPath.size() - 1);
            if (curr.equals(goal)) break;

            if (!curr.equals(currentGoal)) {    // Must have been interrupted
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
            } else if (currentIndex == subgoalList.length) {
                // Default is to go to goal of database record then we will hill climb from there.
                currentGoal = new SearchState(currentRecord.getGoalId());
                startTime2 = System.nanoTime();

                if (OPTIMIZE_END_DB_PATH) {    // System.out.println("End optimization check from: "+problem.idToString(currentStart.id)+" to "+problem.idToString(goal.id));
                    newPath = optCheckSearchAlg.computePath(currentStart, goal, stats);
                    if (newPath != null) {    // From last subgoal spot can reach global goal using hill climbing.  That completes the path.
                        // Trying to go to the global goal from the last subgoal may not work but will often be shorter than going to the database goal point then our desired goal.
                        path = SearchUtil.mergePaths(path, newPath);
                        SearchUtil.computePathCost(path, stats, problem);
                        // System.out.println("End optimization successful from: "+problem.idToString(currentStart.id));
                        endTime = System.nanoTime();
                        // System.out.println("Time to do end check: "+(endTime-startTime2));
                        stats.updateMaxTime(endTime - startTime2);
                        return path;
                    } else {
                        if (OPTIMIZE_CHECK) {    // Unable to hill climb to goal from last subgoal - try to hill climb somewhere between last subgoal and end to global goal
                            // Build path from a node between last subgoal (current location) and end of record state to goal
                            subgoalList = currentRecord.getSubgoalList();
                            if (subgoalList != null && subgoalList.length > 0) {
                                newPath = subgoalSearchAlg.computePath(currentStart, new SearchState(currentRecord.getGoalId()), stats);
                                // SearchUtil.printPath(problem, newPath);
                                // Now find farthest spot on path that can HC-reach the global goal
                                SearchState tmp = SearchUtil.computeBinaryReachableFrom(newPath, optCheckSearchAlg, goal, stats);
                                if (tmp == null) {
                                    System.out.println("WARNING: Unable to reach goal during end optimization check: " + problem.idToString(goal.id));
                                    System.out.println("Computing path to end of record.");
                                    currentGoal = new SearchState(currentRecord.getGoalId());
                                    newPath = subgoalSearchAlg.computePath(currentStart, currentGoal, stats);
                                    path = SearchUtil.mergePaths(path, newPath);
                                    System.out.println("Finding reverse path.");
                                    newPath = subgoalSearchAlg.computePath(goal, currentGoal, stats);
                                    if (newPath == null) {
                                        System.out.println("CRITICAL ERROR: Reverse path search failed.");
                                        return null;
                                    }
                                    path = SearchUtil.mergeReversePaths(path, newPath);
                                    SearchUtil.computePathCost(path, stats, problem);
                                    endTime = System.nanoTime();
                                    System.out.println("Time to do end check: " + (endTime - startTime2));
                                    stats.updateMaxTime(endTime - startTime2);
                                    System.out.println("Reverse path search successful");
                                    return path;
                                }
                                currentGoal = tmp;
								
								/*
								if (!subgoalSearchAlg.isPath(currentStart.id, currentGoal.id, stats) || !subgoalSearchAlg.isPath(currentGoal.id, goal.id, stats))
								{
									System.out.println("HERE");
								}
								*/
                                System.out.println("Binary end optimization successful.  Starting point: " + problem.idToString(currentGoal.id) + " Index in path: " + newPath.indexOf(currentGoal));
                                // Now build rest of path.  Note that may not be able to HC from a subgoal to any arbitrary state between subgoals, so compute the path between them then go from found state.
                                int i = newPath.indexOf(currentGoal) + 1;
                                int count = newPath.size() - i;
                                for (int k = 0; k < count; k++)
                                    newPath.remove(i);            // Remove rest of path not needed
                                // SearchUtil.printPath(problem, newPath);
                                path = SearchUtil.mergePaths(path, newPath);
                                newPath = subgoalSearchAlg.computePath(currentGoal, goal, stats);            // Get to global goal
                                path = SearchUtil.mergePaths(path, newPath);
                                SearchUtil.computePathCost(path, stats, problem);
                                endTime = System.nanoTime();
                                // System.out.println("Time to do end check: "+(endTime-startTime2));
                                stats.updateMaxTime(endTime - startTime2);
                                return path;
                            }
                        }

                    }
                }
            } else if (currentIndex < subgoalList.length) {
                currentGoal = new SearchState(subgoalList[currentIndex]);
                subgoals.add(currentGoal);
            } else currentGoal = goal;            // Go towards global goal
        }

        SearchUtil.computePathCost(path, stats, problem);
        return path;

    }

    public boolean isPath(SearchState start, SearchState goal, StatsRecord stats) {
        return computePath(start, goal, stats) != null;
    }

    public boolean isPath(int startId, int goalId, StatsRecord stats) {
        return computePath(new SearchState(startId), new SearchState(goalId), stats) != null;
    }
}
