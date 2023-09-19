package database;

import map.GameMap;
import map.GroupRecord;
import search.AStar;
import search.GenHillClimbing;
import search.Path;
import search.SearchAbstractAlgorithm;
import search.SearchAlgorithm;
import search.SearchProblem;
import search.SearchState;
import search.SearchUtil;
import search.StatsRecord;
import util.ExpandArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

public class GameDB {
    private SearchProblem problem;
    private HashMap<Integer, GroupRecord> groups;

    public GameDB(SearchProblem problem) {
        this.problem = problem;
    }

    public SubgoalDB computeRandomDB(int num, SearchAbstractAlgorithm searchAlg, DBStatsRecord dbstat, SubgoalDB sgdb) {
        System.out.println("Creating random database with " + num + " entries.");
        long startTime = System.currentTimeMillis();

        AStar astar = new AStar(problem);

        int maxDistance = 100000;
        int minDistance = 10;
        Random generator = new Random();
        int dist;
        SearchState start, goal;

        // Generate a random start/end.  Distance away must be in between cutoffs.
        // Also, number of subgoals must be at least 1.
        long currentTime;
        long totalATime = 0, totalSGTime = 0;
        int numSubgoals = 0;
        int numWithoutSGs = 0, numDup = 0, numFail = 0;
        int numRandom = 0;
        StatsRecord stats = new StatsRecord();
        ArrayList<SearchState> subgoals = new ArrayList<SearchState>(5000);
        for (int i = 0; i < num; i++) {
            do {
                start = problem.generateRandomState(generator);
                goal = problem.generateRandomState(generator);

                dist = problem.computeDistance(start, goal);
                numRandom++;
            } while (dist < minDistance || dist > maxDistance);

            // Compute optimal path for set of points
            currentTime = System.currentTimeMillis();
            ArrayList<SearchState> path = astar.computePath(start, goal, stats);
            // System.out.println("Time to compute A*:"+(System.currentTimeMillis()-currentTime));
            totalATime += System.currentTimeMillis() - currentTime;

            currentTime = System.currentTimeMillis();

            if (path != null) SearchUtil.computeSubgoalsBinary(path, searchAlg, subgoals);
            else {
                i--;
                // System.out.println("Start and goal not connected.");
                numFail++;
                continue;
            }

            if (subgoals.size() == 2)// Only start and end, no subgoals
            {
                i--;
                // System.out.println("No subgoals between start and goal.");
                numWithoutSGs++;
                continue;
            }

            int[] subgoal = new int[subgoals.size() - 2];

            for (int k = 1; k < subgoals.size() - 1; k++) {
                SearchState s = (SearchState) subgoals.get(k);
                subgoal[k - 1] = s.id;
            }
            numSubgoals += subgoal.length + 1;

            // System.out.println("Time to compute subgoals: "+(System.currentTimeMillis()-currentTime));

            SubgoalDBRecord rec = new SubgoalDBRecord(i, start.id, goal.id, subgoal, 0);
            if (!sgdb.addRecord(rec)) {    // Duplicate entry
                i--;
                // System.out.println("Duplicate entry");
                numDup++;
                continue;
            }

//			if (sgdb instanceof SubgoalDBExact)
//				((SubgoalDBExact) sgdb).computeCoverage();

            totalSGTime += System.currentTimeMillis() - currentTime;
            //if (i % 1 == 0)
            //	System.out.println("#random: "+numRandom+" #WGS: "+numWithoutSGs+" #dup: "+numDup+" Fail: "+numFail+" Added record "+i+" Record: "+rec.toString(problem));
        }
        System.out.println("Generated random database with " + num + " entries in time: " + (System.currentTimeMillis() - startTime) / 1000);
        System.out.println("Time performing A*: " + totalATime + " Time performing subgoal calculation: " + totalSGTime);
        System.out.println("Number of subgoals: " + numSubgoals);
        dbstat.addStat(8, num);
        dbstat.addStat(9, numSubgoals);
        long overallTime = (System.currentTimeMillis() - startTime);
        dbstat.addStat(10, overallTime);
        dbstat.addStat(13, totalATime);
        dbstat.addStat(14, totalSGTime);
        return sgdb;
    }


    /**
     * Given a list of group identifiers produced by any map abstraction, generates a knnLRTA* style database of optimal A* compressed paths between each
     * group member.
     * @param searchAlg
     * @param dbstat
     * @param sgdb
     * @return
     */
//	public SubgoalDB computeCoverDB(SearchAbstractAlgorithm searchAlg, DBStatsRecord dbstat, SubgoalDB sgdb, HashMap<Integer,GroupRecord> groups, int cutoff, double minOpt, double maxOpt, int maxRecords)
//	{
//		System.out.println("Creating cover database on map with  "+groups.size()+" trialheads.");
//		long startTime = System.currentTimeMillis();
//
//		AStar astar = new AStar(problem);
//
//		SearchState start, goal;
//		long currentTime;
//		long totalATime = 0, totalSGTime = 0;
//		int numSubgoals = 0;
//		StatsRecord stats = new StatsRecord();
//		ArrayList<SearchState> subgoals = new ArrayList<SearchState>(5000);
//		int count = 0;
//		ArrayList<SearchState> path;
//
//		ArrayList<Path> problems = new ArrayList<Path>();
//
//		// Iterate through all possible trailhead combinations
//		Iterator<Integer> it1 = groups.keySet().iterator();
//		while (it1.hasNext())
//		{
//			GroupRecord startRec = groups.get(it1.next());
//			Iterator<Integer> it2 = groups.keySet().iterator();
//			while (it2.hasNext())
//			{
//				GroupRecord goalRec = groups.get(it2.next());
//				if (startRec == goalRec)
//					continue;		// No path between state and itself
//
//				start = new SearchState(startRec.getGroupRepId());
//				goal = new SearchState(goalRec.getGroupRepId());
//
//				// Compute optimal path for set of points
//				currentTime = System.currentTimeMillis();
//				ArrayList<SearchState> apath = astar.computePath(start, goal, stats);
//				totalATime += System.currentTimeMillis()-currentTime;
//				currentTime = System.currentTimeMillis();
//
//				if (apath == null)
//					continue;
//
//				// Filter with cover algorithm
//				// Find path between start and goal using LRTA* (will stop if over a certain cost)
//				LRTA lrta = new LRTA(problem, goal, 1, 90000000, false, 0);
//				int optimalCost = stats.getPathCost();
//				lrta.setMaxCost((int) (optimalCost*maxOpt));
//				path = lrta.computePath(start, goal, stats);
//				double cost = 0;
//				if (path == null)	// Must had an early termination
//					cost = optimalCost*maxOpt;
//				else
//					cost = stats.getPathCost();
//
//				// Otherwise add record to list of possiblities
//		//		if (stats.getPathCost()/optimalCost >= minOpt)		// Difficulty of problem must be higher than minimum cutoff
//					problems.add(new Path(apath, -1*cost/optimalCost, stats.getPathLength(), start, goal));
//			}
//		}
//
//		Collections.sort(problems);	// Sort in ascending order
//
//		// Select only required number of paths and compress them
//		for (int i=0; i < maxRecords && i < problems.size(); i++)
//		{
//			Path p = problems.get(i);
//			path = p.getStates();
//
//			currentTime = System.currentTimeMillis();
//			SearchUtil.computeSubgoalsBinary(path, searchAlg, subgoals);
//
//			totalSGTime += System.currentTimeMillis()-currentTime;
//
//			int []subgoal = new int[subgoals.size()-2];
//
//			for (int k=1; k < subgoals.size()-1; k++)
//			{	SearchState s = (SearchState) subgoals.get(k);
//				subgoal[k-1] = s.id;
//			}
//			numSubgoals += subgoal.length+1;
//
//			SubgoalDBRecord rec = new SubgoalDBRecord(count++, p.getStart().id, p.getGoal().id, subgoal, 0);
//			sgdb.addRecord(rec);
//			//System.out.println("Selected record: "+i+" Subopt: "+p.getCost()+" "+" start: "+p.getStart().id+" rec: "+rec);
//			//SearchUtil.printPath(path);
//		}
//
//		System.out.println("Generated random database with "+sgdb.getSize()+" records in time: "+(System.currentTimeMillis()-startTime)/1000);
//		System.out.println("Time performing A*: "+totalATime+" Time performing subgoal calculation: "+totalSGTime);
//		System.out.println("Number of subgoals: "+numSubgoals);
//		dbstat.addStat(8, count);
//		dbstat.addStat(9, numSubgoals);
//		long overallTime = (System.currentTimeMillis()-startTime);
//		dbstat.addStat(10, overallTime);
//		dbstat.addStat(13, totalATime);
//		dbstat.addStat(14, totalSGTime);
//		return sgdb;
//	}


    /**
     * Given a previously constructed HCDPS exact database, filters the records by removing simple ones.
     * User specifies number of records to keep, and the rest of the records are dropped.
     * @param searchAlg
     * @param dbstat
     * @param sgdb
     * @return
     */
//	public SubgoalDB computeCoverDB2(SearchAbstractAlgorithm searchAlg, DBStatsRecord dbstat, SubgoalDBExact sgdb, HashMap<Integer,GroupRecord> groups, int cutoff, double minOpt, double maxOpt, int maxRecords)
//	{
//		System.out.println("Filtering HCDPS database on map with  "+groups.size()+" trialheads.");
//		long startTime = System.currentTimeMillis();
//
//		AStar astar = new AStar(problem);
//
//		SearchState start, goal;
//		long currentTime;
//		long totalATime = 0, totalLRTATime = 0;
//		int numSubgoals = 0;
//		StatsRecord stats = new StatsRecord();
//		ArrayList<SearchState> subgoals = new ArrayList<SearchState>(5000);
//		int count = 0;
//		ArrayList<SearchState> path;
//
//		ArrayList<Path> problems = new ArrayList<Path>();
//		GenHillClimbing pathCompressAlg = new GenHillClimbing(problem, 10000);
//		// Iterate through all possible trailhead combinations
//		Iterator<Integer> it1 = groups.keySet().iterator();
//		while (it1.hasNext())
//		{
//			GroupRecord startRec = groups.get(it1.next());
//			Iterator<Integer> it2 = groups.keySet().iterator();
//			while (it2.hasNext())
//			{
//				GroupRecord goalRec = groups.get(it2.next());
//				if (startRec == goalRec)
//					continue;		// No path between state and itself
//
//				start = new SearchState(startRec.getGroupRepId());
//				goal = new SearchState(goalRec.getGroupRepId());
//
//				// Compute optimal path for set of points
//				currentTime = System.currentTimeMillis();
//				// Instead of A* use pre-constructed HCDPS path as estimate for optimal
//				//ArrayList<SearchState> apath = astar.computePath(start, goal, stats);
//				// Retrieve record from database
//				ArrayList<SubgoalDBRecord> recs = sgdb.findNearest(problem, start, goal, searchAlg, 1, stats, null);
//				ArrayList<SearchState> apath = recs.get(0).computePath(pathCompressAlg);
//
//				totalATime += System.currentTimeMillis()-currentTime;
//				currentTime = System.currentTimeMillis();
//
//				if (apath == null)
//					continue;
//
//				// Filter with cover algorithm
//				// Find path between start and goal using LRTA* (will stop if over a certain cost)
//				LRTA lrta = new LRTA(problem, goal, 1, 90000000, false, 0);
//				int optimalCost = stats.getPathCost();
//				lrta.setMaxCost((int) (optimalCost*maxOpt));
//				path = lrta.computePath(start, goal, stats);
//				double cost = 0;
//				if (path == null)	// Must had an early termination
//					cost = optimalCost*maxOpt;
//				else
//					cost = stats.getPathCost();
//				totalLRTATime += System.currentTimeMillis()-currentTime;
//				// Otherwise add record to list of possiblities
//		//		if (stats.getPathCost()/optimalCost >= minOpt)		// Difficulty of problem must be higher than minimum cutoff
//					problems.add(new Path(apath, -1*cost/optimalCost, stats.getPathLength(), start, goal));
//			}
//		}
//
//		Collections.sort(problems);	// Sort in ascending order
//
//		ArrayList<SubgoalDBRecord> keptRecords = new ArrayList<SubgoalDBRecord>();
//
//		// Select only required number of paths and compress them
//		for (int i=0; i < maxRecords && i < problems.size(); i++)
//		{
//			Path p = problems.get(i);
//			// Lookup record in existing database
//			SubgoalDBRecord rec = sgdb.getRecord(p.getStart(), p.getGoal());
//			if (rec == null)
//			{	System.out.println("Problem");
//				rec = sgdb.getRecord(p.getStart(), p.getGoal());
//			}
//			if (rec.getSubgoalList() == null)
//				numSubgoals++;
//			else
//				numSubgoals += rec.getSubgoalList().length+1;
//
//			keptRecords.add(rec);
//			//System.out.println("Selected record: "+i+" Subopt: "+p.getCost()+" "+" start: "+p.getStart().id+" rec: "+rec);
//			//SearchUtil.printPath(path);
//		}
//
//		sgdb.replaceRecords(keptRecords);
//		sgdb.init();		// Redo the lookup matrix for records as now may be missing some
//		System.out.println("Generated filtered database with "+sgdb.getSize()+" records in time: "+(System.currentTimeMillis()-startTime)/1000);
//		System.out.println("Time performing A* estimation: "+totalATime+" Time performing subgoal calculation: "+totalLRTATime);
//		System.out.println("Number of subgoals: "+numSubgoals);
//		dbstat.addStat(8, count);
//		dbstat.addStat(9, numSubgoals);
//		//long overallTime = (System.currentTimeMillis()-startTime);
//		//dbstat.addStat(10, overallTime);
//		dbstat.addStat(13, totalATime);
//		dbstat.addStat(14, totalLRTATime);
//		return sgdb;
//	}
    /*
     * Computes a tree of subgoals as a database
     */
//	public SubgoalTreeDB computeTreeDB(int num, SearchAbstractAlgorithm searchAlg, DBStatsRecord dbstat, SubgoalTreeDB sgdb)
//	{
//		System.out.println("Creating tree subgoal database.");
//		long startTime = System.currentTimeMillis();
//
//		int numnodes = 0, numtrees = 0;
//
//		int[] node = new int [1000];
//		int[] parentn = new int[1000];
//		int count = 0;
//		ExpandArray neighbors = new ExpandArray();
//
//		BitSet closedList = new BitSet(problem.getMaxSize());
//
//		// Go through all states and make a tree for them
//		problem.initIterator();
//		SearchState goal = new SearchState();
//		while (problem.nextState(goal))
//		{
//			// Perform Dijkstra's from this state and identify states for subgoals
//			PriorityQueue<SearchState> openList = new PriorityQueue<SearchState>();
//			HashMap<Integer, SearchState> openListLookup = new HashMap<Integer, SearchState>();
//			closedList.clear();
//
//			goal.g = 0;				// Using g for g(s) in the algorithm
//			goal.h = 0;				// Using h for g_sub(s)
//			goal.stateData = goal;	// Equivalent to Sub(s)
//			goal.cost = 0;			// Just used so priority queue sorts on it (smallest to largest) - equivalent to goal.g all times
//			count = 0;
//
//			openList.add(goal);
//	        while (openList.size() > 0)
//	        {
//	            // Find the lowest-cost state so far
//	        	SearchState curr = openList.remove();
//
//	        	closedList.set(curr.id);
//
//	        	if (curr != goal)
//	        	{
//	        		SearchState parent = curr.prev;
//	        		curr.stateData = parent.stateData;
//	        		curr.h = parent.h + problem.getMoveCost(parent, curr);
//	        	}
//
//	            // Expand the current state
//	        	//ArrayList<SearchState> neighbors = problem.getNeighbors(curr);
//	        	problem.getNeighbors(curr.id, neighbors);
//	    		for (int i=0; i < neighbors.num(); i++)
//	    		{	// SearchState next = neighbors.get(i);
//	    			int nextid = neighbors.get(i);
//
//	    			if (closedList.get(nextid))
//	    				continue;
//
//	    			int moveCost = problem.getMoveCost(curr.id, nextid);
//	    			int newG = curr.g + moveCost;
//
//	    			// 	Add state to open list.  If already there, update its cost only
//	    			SearchState state = openListLookup.get(GameMap.ints[nextid]);
//
//	    			if (state == null)
//	    			{	state = new SearchState(nextid);
//	    				state.g = Integer.MAX_VALUE;
//	    				state.cost = Integer.MAX_VALUE;
//	    			}
//	    			else
//	    			{	if (state.g > newG)
//	    				{	openList.remove(state);
//	    				}
//	    			}
//
//	    			if (state.g > newG)
//	    			{	state.g = newG;
//	    				state.h = curr.h + moveCost;
//	    				state.cost = state.g;
//	    				state.prev = curr;
//	    				if (state.h != problem.computeDistance(state.id, ((SearchState) curr.stateData).id) )
//	    				{	curr.h = 0;
//
//	    					// Add node to tree
//	    					node[count] = curr.id;
//	    					parentn[count++] = ((SearchState) curr.stateData).id;
//
//	    					numnodes++;
//	    					curr.stateData = curr;
//	    				}
//	    				openList.add(state);
//	    				openListLookup.put(GameMap.ints[state.id], state);
//	    			}
//	    		}
//	        } // end while
//
//	        // Add the tree
//	        sgdb.addtree(goal.id, node, parentn, count);
//	        numtrees++;
//	        if (numtrees % 100 == 0)
//	        	System.out.println("Generated tree: "+numtrees + " Num nodes: "+count);
//		}
//
//		System.out.println("Generated tree database entries in time: "+(System.currentTimeMillis()-startTime)/1000);
//		System.out.println("Number of trees: "+numtrees+"\tNumber of nodes: "+numnodes);
//		dbstat.addStat(8, numtrees);
//		dbstat.addStat(9, numnodes);
//		long overallTime = (System.currentTimeMillis()-startTime);
//		dbstat.addStat(10, overallTime);
//		dbstat.addStat(13, 0);
//		dbstat.addStat(14, 0);
//		return sgdb;
//	}

    /**
     * Computes a database based on a problem that has undergone clique abstraction.  Assumes at least one level of abstraction has been performed.
     * (i.e. Does not work on base maps)
     * Each database entry stores a start and goal which is the centroid of the abstract state, a search depth, and a subgoal to exit the current state.
     * The database has an entry for all pairs of abstract states as required by the DLRTA* algorithm.
     */
//	public SubgoalDBDLRTA computeAbstractDB(DBStatsRecord dbstat, SubgoalDBDLRTA database)
//	{
//		System.out.println("Creating database based on clique abstraction.");
//		long startTime = System.currentTimeMillis();
//		long aStarTime = 0;
//		ArrayList<SearchState> path;
//		ArrayList<Integer> subgoal=null;
//		AStar astar = new AStar(problem);
//		StatsRecord stats = new StatsRecord();
//
//		int i, count = 0;
//
//		groups = problem.getGroups();
//
//		// Compute all pairs subgoal and minimum lookahead
//		// Compute the neighbors of all groups
//		Iterator<Map.Entry<Integer,GroupRecord>> it = groups.entrySet().iterator();
//		Iterator<Map.Entry<Integer,GroupRecord>> it2;
//
//		while (it.hasNext())
//		{
//			GroupRecord startState = it.next().getValue();
//			it2 = groups.entrySet().iterator();
//			while (it2.hasNext())
//			{
//				GroupRecord goalState = it2.next().getValue();
//
//				if (goalState == startState)
//					continue;
//
//				// Compute optimal A* path between start and goal
//				long start = System.currentTimeMillis();
//				path = astar.computePath(new SearchState(startState.groupRepId), new SearchState(goalState.groupRepId), stats);
//				aStarTime += (System.currentTimeMillis() - start);
//
//				if (path != null)
//				{	// What to do if two abstract states are not reachable?
//					int currentGroup = startState.groupId - GameMap.START_NUM;	// TODO: Can this be avoided or be made consistent?
//					SearchState state=null;
//					// Find first state on path that is in next abstract state and make that the subgoal
//					for (i=0; i < path.size(); i++)
//					{	state = path.get(i);
//						if (database.getAbstractState(state.id) != currentGroup)
//							break;
//					}
//					if (database.getAbstractState(state.id) != currentGroup)
//					{	// TODO: Not sure what to do in case where no element in the path does not leave group.  I believe this case is not possible by construction.
//						subgoal = new ArrayList<Integer>();
//						subgoal.add(state.id);
//					}
//
//					// Not computing optimal lookahead depth.  Assumption is that it is always one for now.
//					//int depth = 1;
//
//					int[] subgoals = new int[subgoal.size()];
//					for (int k=0; k < subgoal.size(); k++)
//						subgoals[k] = (int) subgoal.get(k);
//
//					int goalGroup= goalState.groupId-GameMap.START_NUM;
//					// currentGroup = currentGroup-1;
//					// SubgoalDBRecord rec = new SubgoalDBRecord(i,startState.groupRepId, goalState.groupRepId, subgoals, depth);
//					SubgoalDBRecord rec = new SubgoalDBRecord(count, startState.groupRepId, goalState.groupRepId, subgoals,
//							(currentGroup+1)*10000+goalGroup);
//					database.addRecord(rec);
//					count++;
//				//	if (count % 1 == 0)
//				//		System.out.println("Added record "+count+" between: "+(currentGroup)+" and "+(goalGroup)+" Record: "+rec.toString(problem));
//					// System.out.println("Added record "+count+" Record: "+rec);
//				}
//			}
//		}
//
//		dbstat.addStat(2, count);
//		dbstat.addStat(8, count);		// Records
//		dbstat.addStat(9, count*2);		// Subgoals (one subgoal + end per record)
//		dbstat.addStat(13, aStarTime);
//		long overallTime = (System.currentTimeMillis()-startTime)/1000;
//		dbstat.addStat(10, overallTime);
//		System.out.println("Generated database with "+(count)+" entries in time: "+overallTime);
//		return database;
//	}
    public SubgoalDB computeDBDP2(SubgoalDB db, SearchAlgorithm astarj, DBStatsRecord dbstats, int numLevels) {
        groups = problem.getGroups();

        long current = System.currentTimeMillis();
        problem.computeNeighbors();
        long neighborTime = System.currentTimeMillis() - current;
        dbstats.addStat(18, neighborTime);

        // Generate subgoal databases using the groups
        computeHCDBDP(db, astarj, dbstats, numLevels);

        return db;
    }

    public static HashSet<Integer> getNeighbors(HashMap<Integer, GroupRecord> groups, GroupRecord startGroup, int numLevels) {
        HashSet<Integer> neighbors = startGroup.getComputedNeighborIds();

        if (neighbors == null) {
            // This supports Level 1 (immediate neighbors)
            neighbors = new HashSet<Integer>(startGroup.getNeighborIds().size());
            neighbors.addAll(startGroup.getNeighborIds());

            // The loop is for level 2 and above
            HashSet<Integer> neighbors2 = new HashSet<Integer>();
            BitSet done = new BitSet(groups.size());
            GroupRecord neighborGroup;

            for (int i = 1; i < numLevels; i++) {
                Iterator<Integer> it = neighbors.iterator();
                while (it.hasNext()) {
                    int neighborId = (Integer) it.next();
                    if (done.get(neighborId)) continue;    // Already processed this neighbors set

                    done.set(neighborId);
                    neighborGroup = groups.get(neighborId);
                    // Do not itself if already there
                    Iterator<Integer> it2 = neighborGroup.getNeighborIds().iterator();
                    while (it2.hasNext()) {
                        int val = it2.next();
                        if (val != startGroup.groupId) neighbors2.add(val);
                    }

                    // neighbors2.addAll(neighborGroup.getNeighborIds());	// TODO: May remove. THis code would add a neighbor of itself.
                }
                neighbors.addAll(neighbors2);
                neighbors2.clear();
            }

            startGroup.setComputedNeighborIds(neighbors);
        }

        return neighbors;
    }

    public static long computeBasePaths(SearchProblem problem, HashMap<Integer, GroupRecord> groups, SubgoalDB db, SearchAlgorithm searchAlg, int[][] lowestCost, int[][][] paths, int[][] neighbor, int numGroups, int numLevels, boolean asSubgoals, DBStatsRecord dbstats) {
        int goalGroupLoc, startGroupLoc;
        GroupRecord startGroup, goalGroup;
        HashSet<Integer> neighbors;
        AStar astar = new AStar(problem);
        ArrayList<SearchState> path;
        StatsRecord stats = new StatsRecord();
        int numBase = 0;

        System.out.println("Number of groups: " + numGroups);
        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < numGroups; i++)
            Arrays.fill(neighbor[i], 0, numGroups, -1);

        int[] tmp = new int[5000];
        // Now make database entries for each of the lowest cost by merging the paths
        System.out.println("Creating base paths to neighbors.");
        // Base case: Generate paths to all neighbors
        int numStates = 0;
        for (int i = 0; i < numGroups; i++) {
            startGroup = groups.get(i + GameMap.START_NUM);

            neighbors = GameDB.getNeighbors(groups, startGroup, numLevels);
            Iterator<Integer> it = neighbors.iterator();
            // System.out.println("Doing group: "+i+" Neighbors: "+neighbors.size());
            // Generate for each neighbor group
            while (it.hasNext()) {
                // Compute shortest path between center representative of both groups
                int goalGroupId = (Integer) it.next();
                goalGroup = groups.get(goalGroupId);

                path = astar.computePath(new SearchState(startGroup.groupRepId), new SearchState(goalGroup.groupRepId), stats);
                numBase++;

                goalGroupLoc = goalGroupId - GameMap.START_NUM;
                startGroupLoc = i;

                // Save information
                SearchUtil.computePathCost(path, stats, problem);
                int pathCost = stats.getPathCost();
                lowestCost[startGroupLoc][goalGroupLoc] = pathCost;
                neighbor[startGroupLoc][goalGroupLoc] = goalGroupLoc;
                if (asSubgoals) {
                    paths[startGroupLoc][goalGroupLoc] = SubgoalDB.convertPathToIds(path);
                    paths[startGroupLoc][goalGroupLoc] = SearchUtil.compressPath(paths[startGroupLoc][goalGroupLoc], searchAlg, tmp, path.size());
                    numStates += paths[startGroupLoc][goalGroupLoc].length;
                } else {
                    paths[startGroupLoc][goalGroupLoc] = SubgoalDB.convertPathToIds(path);
                    numStates += path.size();
                }
            }
            //	System.out.println("Done group: "+i+" Num paths (so far): "+numBase);
        }

        long endTime = System.currentTimeMillis();
        long baseTime = endTime - currentTime;
        System.out.println("Time to compute base paths: " + (baseTime));
        System.out.println("Base neighbors generated paths: " + numBase + " Number of states: " + numStates);
        dbstats.addStat(9, numStates);        // Set number of subgoals.  Will be changed by a version that pre-computes all paths but will not be changed for the dynamic version.
        dbstats.addStat(8, numBase);        // # of records (only corresponds to base paths)
        return baseTime;
    }

    public SubgoalDynamicDB computeDynamicDB(SubgoalDynamicDB db, SearchAbstractAlgorithm searchAlg, DBStatsRecord dbstats, int numLevels) {
        groups = problem.getGroups();

        long current = System.currentTimeMillis();
        problem.computeNeighbors();
        long neighborTime = System.currentTimeMillis() - current;
        dbstats.addStat(18, neighborTime);

        // Generate subgoal databases using the groups
        db.compute(problem, groups, searchAlg, dbstats, numLevels);

        return db;
    }

    public SubgoalDynamicDB2 computeDynamicDB(SubgoalDynamicDB2 db, SearchAbstractAlgorithm searchAlg, DBStatsRecord dbstats, int numLevels) {
        groups = problem.getGroups();

        long current = System.currentTimeMillis();
        problem.computeNeighbors();
        long neighborTime = System.currentTimeMillis() - current;
        dbstats.addStat(18, neighborTime);

        // Generate subgoal databases using the groups
        db.compute(problem, groups, searchAlg, dbstats, numLevels);

        return db;
    }

    public SubgoalDynamicDB3 computeDynamicDB(SubgoalDynamicDB3 db, SearchAbstractAlgorithm searchAlg, DBStatsRecord dbstats, int numLevels) {
        groups = problem.getGroups();

        long current = System.currentTimeMillis();
        problem.computeNeighbors();
        long neighborTime = System.currentTimeMillis() - current;
        dbstats.addStat(18, neighborTime);

        // Generate subgoal databases using the groups
        db.compute(problem, groups, searchAlg, dbstats, numLevels);


        int maxSize = groups.size() * groups.size();
        IndexDB idb = db.getNeighborIndexDB();
        System.out.println("Neighbor entries: " + maxSize + "\nNumber of DB states: " + idb.getTotalCells() + " Number of records in index DB:  " + idb.getCount() + "\n % of problem size: " + (idb.getCount() * 100.0 / idb.getTotalCells()) + "\n % of problem total size: " + (idb.getCount() * 100.0 / (maxSize)));

        return db;
    }

    /**
     * Creates a databases combining all pairs of groups in map using dynamic programming instead of generating all combinations.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public SubgoalDB computeHCDBDP(SubgoalDB db, SearchAlgorithm searchAlg, DBStatsRecord dbstats, int numLevels) {
        GroupRecord startGroup, goalGroup;
        int[] path;
        int SIZE_CUTOFF = 1;
        int count = 0;
        int numSubgoals = 0;
        int numGroups = groups.size();
        int[][] lowestCost = new int[numGroups][numGroups];
        int[][][] paths = new int[numGroups][numGroups][];
        int[][] neighbor = new int[numGroups][numGroups];
        HashSet<Integer> neighbors;
        long startTime = System.currentTimeMillis();

        long baseTime = computeBasePaths(problem, groups, db, searchAlg, lowestCost, paths, neighbor, numGroups, numLevels, true, dbstats);

        long endTime, currentTime = System.currentTimeMillis();

        System.out.println("Performing dynamic programming to generate paths.");

        // Compute neighbors once and store in arrays for fast access
        int[][] tmpNeighbors = new int[numGroups][];
        for (int i = 0; i < numGroups; i++) {
            count = 0;
            for (int j = 0; j < numGroups; j++)
                if (neighbor[i][j] >= 0) count++;
            tmpNeighbors[i] = new int[count];
            count = 0;
            for (int j = 0; j < numGroups; j++)
                if (neighbor[i][j] >= 0) tmpNeighbors[i][count++] = neighbor[i][j];
        }

        boolean changed = true;
        int numUpdates = 0;
        while (changed) {
			/*
			System.out.println("\nCurrent matrix: ");
			for (int i=0; i < numGroups; i++)
			{	
				for (int j=0; j < numGroups; j++)
				{
					System.out.print(lowestCost[i][j]+" ("+neighbor[i][j]+")\t");
				}
				System.out.println();
			}
			*/
            changed = false;

            for (int i = 0; i < numGroups; i++) {
                // Process all neighbors of this node
                for (int k = 0; k < tmpNeighbors[i].length; k++) {
                    // int neighborId = tmpNeighbors[i][k] - GameMap.START_NUM;
                    int neighborId = tmpNeighbors[i][k];
                    // Compute new costs for all locations based on value of neighbor
                    for (int j = 0; j < numGroups; j++) {
                        if (i == j) continue;
                        if (lowestCost[neighborId][j] > 0 && (lowestCost[i][j] == 0 || lowestCost[i][j] > lowestCost[i][neighborId] + lowestCost[neighborId][j])) {
                            changed = true;
                            lowestCost[i][j] = lowestCost[i][neighborId] + lowestCost[neighborId][j];
                            neighbor[i][j] = neighborId;
                            numUpdates++;
                        }
                    }
                }
            }
        }
		
		/*
		// Previous code recomputed neighbors each time
		// Now the dynamic programming portion
		// Idea: Keep updating from neighbors until no further changes are made
		boolean changed = true;
		int numUpdates = 0;
		while (changed)
		{
			changed = false;
			
			for (int i=0; i < numGroups; i++)
			{	
				startGroup = groups.get(i+GameMap.START_NUM);
				// Process all neighbors of this node
				neighbors = GameDB.getNeighbors(groups, startGroup, numLevels);
				Iterator<Integer> it = neighbors.iterator();
				while (it.hasNext())
				{
					int neighborId = (Integer) it.next()- GameMap.START_NUM;
					// Compute new costs for all locations based on value of neighbor
					for (int j=0; j < numGroups; j++)
					{	if (i==j)
							continue;
						if (lowestCost[neighborId][j]>0 && (lowestCost[i][j]==0 || lowestCost[i][j] > lowestCost[i][neighborId]+lowestCost[neighborId][j]))
						{
							changed = true;
							lowestCost[i][j] = lowestCost[i][neighborId]+lowestCost[neighborId][j];
							neighbor[i][j] = neighborId;
							numUpdates++;
						}
					}
				}
			}
		}
				*/
        System.out.println("Number of cost updates: " + numUpdates);
        endTime = System.currentTimeMillis();
        long dpTime = endTime - currentTime;
        System.out.println("Time to compute paths via dynamic programming: " + dpTime);
        currentTime = System.currentTimeMillis();

        int[] subgoals, tmp = new int[5000];
        // Now make database entries for each of the lowest cost by merging the paths
        int i, j;
        int totalCost = 0, pathSize;
        path = new int[5000];
        for (i = 0; i < numGroups; i++) {
            startGroup = groups.get(i + GameMap.START_NUM);
            if (startGroup.getSize() < SIZE_CUTOFF) continue;

            for (j = 0; j < numGroups; j++) {
                if (i == j) continue;

                goalGroup = groups.get(j + GameMap.START_NUM);
                if (goalGroup.getSize() < SIZE_CUTOFF) continue;

                // This code builds only the path required on demand (may incur more time as have to continually merge paths but may save time by avoiding storing/copying lists to do construction)
                pathSize = mergePaths3(i, j, paths, neighbor, path, 0);

                if (pathSize == 0) continue;        // No path between two states

                // Does not include start and goal
                subgoals = SearchUtil.computeSubgoalsBinaryByIds(path, searchAlg, tmp, pathSize);
                if (subgoals == null) numSubgoals++;
                else numSubgoals += subgoals.length + 1;

                SubgoalDBRecord rec = new SubgoalDBRecord(count, startGroup.groupRepId, goalGroup.groupRepId, subgoals, (startGroup.groupId - GameMap.START_NUM + 1) * 10000 + goalGroup.groupId - GameMap.START_NUM);    // TODO: This will probably need to be changed.

                if (!db.addRecord(rec))
                    System.out.println("Failed to add record connecting HC states: " + i + " and " + j);

                count++;
                if (count % 500 == 0)
                    System.out.println("Added record " + count + " between: " + (i + GameMap.START_NUM) + " and " + (j + GameMap.START_NUM) + " Record: " + rec.toString(problem));
                totalCost += lowestCost[i][j];
            }
        }

        System.out.println("Total database records: " + count);
        System.out.println("# of subgoals (including end but not start): " + numSubgoals);
        endTime = System.currentTimeMillis();
        long overallTime = endTime - startTime;
        long recordTime = endTime - currentTime;
        System.out.println("Time to compute records: " + recordTime);
        System.out.println("Total DB compute time: " + overallTime);
        dbstats.addStat(8, count);
        dbstats.addStat(9, numSubgoals);
        long neighborTime = (Long) dbstats.getStat(18);
        dbstats.addStat(10, overallTime + neighborTime);
        dbstats.addStat(15, dpTime);
        dbstats.addStat(16, baseTime);
        dbstats.addStat(17, recordTime);

        // Verify by computing all costs
        System.out.println("Total costs: " + totalCost);
        return db;
    }

    @SuppressWarnings("unchecked")
    void mergePaths2(int i, int j, ArrayList[][] paths, int[][] neighbor, ArrayList<SearchState> path) {
        int neighborId = neighbor[i][j];
        if (neighborId == -1) return;

        if (neighborId == j) {    // Path already stored as direct neighbour
            for (int k = 0; k < paths[i][j].size(); k++)
                path.add((SearchState) paths[i][j].get(k));
            return;
        }
        if (paths[i][neighborId] == null) return;

        // path.addAll(paths[i][neighborId]);
        for (int k = 0; k < paths[i][neighborId].size(); k++)
            path.add((SearchState) paths[i][neighborId].get(k));

        if (paths[neighborId][j] != null) SearchUtil.mergePaths(path, paths[neighborId][j]);
        else {
            mergePaths2(neighborId, j, paths, neighbor, path);
            // SearchUtil.mergePaths(path, paths[neighborId][j]);
        }
    }

    public static int mergePaths3(int i, int j, int[][][] paths, int[][] neighbor, int[] path, int lastOffset) {
        int neighborId = neighbor[i][j];                    // Lookup neighbor of i to go to that has shortest path to j
        if (neighborId == -1)                                // If no neighbor, i and j must not be connected.
            return 0;

        int start = 0;
        if (lastOffset > 0) start = 1;

        for (int k = start; k < paths[i][neighborId].length; k++)        // Copy (but do not include duplicate start node - start from 1 instead of 0).
            path[lastOffset + k] = paths[i][neighborId][k];

        if (neighborId == j)                                // If neighbor is j itself, then this is the end of the path.
        {
            return lastOffset + paths[i][j].length;        // Current size of path
        }

        // Otherwise this is only the next step on the path.  Call the method recursively to continue to build the path.
        lastOffset += paths[i][neighborId].length - 1;

        return mergePaths3(neighborId, j, paths, neighbor, path, lastOffset);
    }

    private static int findInArray(int[] ar, int key) {
        for (int i = 0; i < ar.length; i++)
            if (ar[i] == key) return i;
        return -1;
    }

    // This version support matrix as an adjacency list and dynamically calculates the best path
    public static int mergePaths4(int startGroupId, int goalGroupId, int[][][] paths, int[][] neighbor, int[][] lowestCost, int[][] neighborId, int[] path, int lastOffset, IndexDB db, SearchProblem problem) {
        // Find if this is a neighbor
        int neighborLoc = findInArray(neighborId[startGroupId], goalGroupId);
        if (neighborLoc != -1)                                                    // Direct neighbor with path stored - just return the path
        {
            for (int k = 0; k < paths[startGroupId][neighborLoc].length; k++)
                path[k] = paths[startGroupId][neighborLoc][k];
            return paths[startGroupId][neighborLoc].length;
        }

        // Otherwise we need to search to find it
        // Quick implementation using Dysktra's algorithm but could use A* as well
        int numGroups = neighbor.length;
        int[] distance = new int[numGroups];
        int[] previous = new int[numGroups];
        boolean[] visited = new boolean[numGroups];
        int[] nodes = new int[numGroups];
        int count = 0;
        Arrays.fill(distance, Integer.MAX_VALUE);
        // Initialize distance and previous with neighbors
        for (int i = 0; i < neighborId[startGroupId].length; i++) {
            neighborLoc = neighborId[startGroupId][i];
            previous[neighborLoc] = startGroupId;
            distance[neighborLoc] = lowestCost[startGroupId][i];
            nodes[count++] = neighborLoc;
        }

        // Using basic unsorted array.  Not great for performance.
        while (count > 0) {    // Find lowest distance (linear search)
            int minCost = Integer.MAX_VALUE, minLoc = -1;
            for (int k = 0; k < count; k++) {
                int neighborid = nodes[k];
                int cost = distance[neighborid];
                if (cost < minCost) {
                    minCost = cost;
                    minLoc = k;
                }
            }

            int neighborid = nodes[minLoc];
            if (minLoc == -1) return -1;            // Unreachable

            if (neighborid == goalGroupId)        // Goal node found - stop algorithm so do not expand to all nodes
                break;

            // Remove this node from the queue
            nodes[minLoc] = nodes[--count];
            visited[neighborid] = true;

            // Process all neighbors of this node
            for (int i = 0; i < neighborId[neighborid].length; i++) {
                neighborLoc = neighborId[neighborid][i];
                if (!visited[neighborLoc]) {
                    int dist = lowestCost[neighborid][i] + distance[neighborid];
                    if (dist < distance[neighborLoc]) {
                        distance[neighborLoc] = dist;
                        previous[neighborLoc] = neighborid;
                    }
                    // TODO: Inefficient.  Only add node if not currently on list.  Do so by searching for it.
                    boolean found = false;
                    for (int k = 0; k < count; k++)
                        if (nodes[k] == neighborLoc) {
                            found = true;
                            break;
                        }

                    if (!found) nodes[count++] = neighborLoc;
                }
            }
        }

        // Print path
        count = 0;
        int currentId = goalGroupId;
        while (currentId != startGroupId) {
            distance[count++] = currentId;
            currentId = previous[currentId];
        }
        distance[count++] = startGroupId;

        // Now produce the actual path
        int pathLen = 0;

        // Add first segment of the path
        // nextId is group id.  nextLoc is location of that id is adjacency list (array)
        int nextId = distance[count - 2], lastId = startGroupId;
        int nextLoc = findInArray(neighborId[lastId], nextId);
        for (int k = 0; k < paths[lastId][nextLoc].length; k++)        // Copy (but do not include duplicate start node - start from 1 instead of 0).
            path[k] = paths[lastId][nextLoc][k];
        pathLen += paths[lastId][nextLoc].length;
        lastId = nextId;
        for (int i = count - 3; i >= 0; i--) {
            nextId = distance[i];
            nextLoc = findInArray(neighborId[lastId], nextId);
            for (int k = 1; k < paths[lastId][nextLoc].length; k++)        // Copy (but do not include duplicate start node - start from 1 instead of 0).
                path[pathLen - 1 + k] = paths[lastId][nextLoc][k];
            pathLen += paths[lastId][nextLoc].length - 1;
            lastId = nextId;
        }
        return pathLen;
			 /*
		
		int currentGroupId = startGroupId;
		int goalGroupSeedId = db.getSeedId(goalGroupId);		
		
		while (true)
		{
			// Select minimum neighbor
			int minCost = 100000, minLoc = -1;			
			for (int k=0; k < neighborId[currentGroupId].length; k++)
			{	int neighborSeedId = db.getSeedId(neighbor[currentGroupId][k]);
				int cost = lowestCost[currentGroupId][k] + problem.computeDistance(neighborSeedId, goalGroupSeedId);		
				if (cost < minCost)
				{	minCost = cost;
					minLoc = k;					
				}
			}
			
			if (minLoc == -1)
				// No neighbor - failure
				return 0;
			
			// Copy path to get to this neighbor
			int start = 0;
			if (lastOffset > 0)
				start = 1;
			for (int k=start; k < paths[currentGroupId][minLoc].length; k++)		// Copy (but do not include duplicate start node - start from 1 instead of 0).
				path[k] = paths[currentGroupId][minLoc][k];
			lastOffset += paths[currentGroupId][minLoc].length-start;
	
			if (goalGroupId == neighbor[currentGroupId][minLoc])
			{	// Found it
				return lastOffset;
			}
			currentGroupId = neighbor[currentGroupId][minLoc];	// Advance to neighbor and continue
		}
*/
    }

    /**
     * This version handles the matrix as a compressed RLE array.
     *
     * @param startGroupId
     * @param goalGroupId
     * @param paths
     * @param neighbors
     * @param path
     * @param lastOffset
     * @param neighborIds
     * @param numGroups
     * @return
     */
    public static int mergePaths5(int startGroupId, int goalGroupId, int[][][] paths, IndexDB neighbors, int[] path, int lastOffset, int[][] neighborIds, int numGroups) {
        int neighborId = neighbors.find(startGroupId * numGroups + goalGroupId);    // Lookup neighbor of i to go to that has shortest path to j
        if (neighborId == -1)                                                    // If no neighbor, i and j must not be connected.
            return 0;

        // Find path to this neighbor
        int neighborLoc = findInArray(neighborIds[startGroupId], neighborId);
		/*if (neighborLoc == -1)													// If no neighbor, i and j must not be connected.
			return 0;*/

        // Copy path to our answer path
        int start = 0;
        if (lastOffset > 0) start = 1;

        for (int k = start; k < paths[startGroupId][neighborLoc].length; k++)        // Copy (but do not include duplicate start node - start from 1 instead of 0).
            path[lastOffset + k] = paths[startGroupId][neighborLoc][k];

        if (neighborId == goalGroupId)                                            // If neighbor is goal, then this is the end of the path.
            return lastOffset + paths[startGroupId][neighborLoc].length;            // Current size of path

        // Otherwise this is only the next step on the path.  Call the method recursively to continue to build the path.
        lastOffset += paths[startGroupId][neighborLoc].length - 1;

        return mergePaths5(neighborId, goalGroupId, paths, neighbors, path, lastOffset, neighborIds, numGroups);
    }

    public class DBRecord {
        int fromGroup;
        int toGroup;
        int subgoalRow;
        int subgoalCol;
    }
}