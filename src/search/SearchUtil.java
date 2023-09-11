package search;

import java.util.ArrayList;
import java.util.HashMap;

public class SearchUtil 
{
	/**
	 *  Merges path 2 onto end of path 1 removing the last element of path 1 which is assumed to be the same as the first element in path2
	 * @param path1
	 * @param path2
	 * @return
	 */
	public static ArrayList<SearchState> mergePaths(ArrayList<SearchState> path1, ArrayList<SearchState> path2)
	{
		if (path1.size() > 0)
			path1.remove(path1.size()-1);	// Remove last element
		path1.ensureCapacity(path1.size()+path2.size());
		path1.addAll(path2);
		return path1;
	}
			
	
	// Merges the reverse of path2 onto the end of path 1 removing the last element of path 1.	
	public static ArrayList<SearchState> mergeReversePaths(ArrayList<SearchState> path1, ArrayList<SearchState> path2)
	{
		if (path1.size() > 0)
			path1.remove(path1.size()-1);	// Remove last element
		path1.ensureCapacity(path1.size()+path2.size());
		for (int i=path2.size()-1; i >= 0; i--)
			path1.add(path2.get(i));
		return path1;
	}
	
	public static int[] mergePathsByIds(int[] path1, int[] path2)
	{				
		if (path1.length > 0)
		{	int[] newPath = new int[path1.length+path2.length-1];
			System.arraycopy(path1, 0, newPath, 0, path1.length - 1);
			if (path2.length - 1 >= 0)
				System.arraycopy(path1, 0 + path1.length - 1, newPath, 0 + path1.length - 1, path2.length - 1);
			return newPath;
		}
		return path1;
	}
	
	public static void printPath(ArrayList<SearchState> path)
	{
		if (path == null)
			System.out.println("No path");
		else
		{
			for (SearchState searchState : path) System.out.println(searchState.id);
		}
	}
	
	
	public static int countRevisits(ArrayList<SearchState> path)
	{
		if (path == null)
			return 0;
		else
		{	HashMap<Integer, Integer> states = new HashMap<Integer, Integer>(path.size());
			int revisits = 0;
			for (SearchState searchState : path) {
				int id = searchState.id;
				if (states.containsKey(id))
					revisits++;
				else
					states.put(id, id);
			}
			return revisits;
		}		
	}
	

	/*
	 * Returns the total amount of distance between revisits.
	 */
	public static int distanceRevisits(ArrayList<SearchState> path)
	{
		int total = 0;
		if (path == null)
			return 0;
		else
		{	HashMap<Integer, Integer> states = new HashMap<Integer, Integer>(path.size());
			int revisits = 0;
			for (int i=0; i < path.size(); i++)
			{	int id = path.get(i).id;
				if (states.containsKey(id))
				{	revisits++;
					int lastLoc = states.get(id);
					total += (i-lastLoc-1);
				//	System.out.println("Distance: "+(i-lastLoc-1));
				}
				states.put(id, i);	// Always put the latest version as may revisit a state multiple times
			}
			return total;
		}		
	}
	
	public static void validatePath(SearchProblem problem, ArrayList<SearchState> path)
	{
		if (path == null)
			System.out.println("No path");
		else
		{
			for (int i=0; i < path.size()-1; i++)
			{	int nodeId = path.get(i).id;
				int nextId = path.get(i+1).id;
				
				// Verify if edge between them and print its cost
				boolean neighbor = problem.isNeighbor(nodeId, nextId);
				int cost = problem.computeDistance(nodeId, nextId);
				
				if (!neighbor)
				{	System.out.println("Cannot go from: "+nodeId+" to : "+nextId+" there is no edge.  Actual cost: "+cost);				
				}
			}
		
		}
	}
	
	public static void printPath(SearchProblem problem, ArrayList<SearchState> path)
	{
		if (path == null)
			System.out.println("No path");
		else
		{
			for (SearchState searchState : path) System.out.print(problem.idToString(searchState.id) + " ");
			System.out.println();
		}
	}
	
	public static void printPath(SearchProblem problem, int[] path)
	{
		if (path == null)
			System.out.println("No path");
		else
		{
			for (int j : path)
				if (problem != null)
					System.out.println(problem.idToString(j));
				else
					System.out.println(j);
		}
	}
	
	public static void computePathCost(ArrayList<SearchState> path, StatsRecord stats, SearchProblem problem)
	{
		int cost = 0, size = 0;
		
		if (path != null && path.size() > 0)
		{	
			SearchState last = path.get(0);
			for (int i=1; i < path.size(); i++)
			{	SearchState current = path.get(i);
				// cost += problem.computeDistance(last, current);
				cost += problem.getMoveCost(last, current);
				last = current;
			}
			size = path.size();
		}
		stats.setPathCost(cost);
		stats.setPathLength(size);			
	}
					
	/**
	 * Given an optimal A* path.  Computes the hill-climbable subgoals needed using binary rather than linear search.
	 * Adds start and end points as subgoals to be consistent with linear version.
	 * Basically compresses A* path into HC subgoal array.
	 * @return
	 */
	public static ArrayList<SearchState> computeSubgoalsBinary(ArrayList<SearchState> path, SearchAlgorithm searchAlg, ArrayList<SearchState> subgoals)
	{
		subgoals.clear();
		StatsRecord stats = new StatsRecord();
		
		int current, startIndex = 0, best = startIndex+1;
		int endIndex = path.size()-1;
		SearchState currentGoal, currentStart = path.get(startIndex);
		subgoals.add(currentStart);
		
		while (startIndex < path.size()-1)
		{										
			currentGoal = path.get(endIndex);			
			if (searchAlg.isPath(currentStart, currentGoal, stats))
				break;
			
			while (startIndex < endIndex)
			{	
				current = (startIndex+endIndex)/2;			// Get mid-point
				// Determine if can reach this point from the current start using HC
				currentGoal = path.get(current);
				if (!searchAlg.isPath(currentStart, currentGoal, stats))
					endIndex = current-1;
				else 
				{	// Can HC reach to here from current start, but can we do better?
					if (current != startIndex)
						best = current;
					startIndex = current+1;
				}	
			}		
			// Save current best as a subgoal
			SearchState bestState = path.get(best);			
			subgoals.add(bestState);
			currentStart = bestState;
			startIndex = best;
			best = startIndex+1;
			endIndex = path.size()-1;
		}
				
		subgoals.add(path.get(endIndex));
		return subgoals;
	}
	
	// Returns the farthest state on the path that is HC-reachable from the start
	public static SearchState computeBinaryReachable(ArrayList<SearchState> path, SearchAlgorithm searchAlg, SearchState currentStart, StatsRecord stats)
	{			
		int current, startIndex = 0, best = startIndex+1;
		int endIndex = path.size()-1;
		SearchState currentGoal;		
			
		while (startIndex <= endIndex)
		{	
			current = (startIndex+endIndex)/2;			// Get mid-point
			// System.out.println("Binary current: "+path.get(current)+" Start: "+path.get(startIndex)+" End: "+path.get(endIndex));
			// Determine if can reach this point from the current start using HC
			currentGoal = path.get(current);
			if (!searchAlg.isPath(currentStart, currentGoal, stats))
				endIndex = current-1;
			else
			{	// Can HC reach to here from current start, but can we do better?
				best = current;
				startIndex = current+1;
			}	
		}
		// System.out.println("Binary current: "+path.get(best));
		return path.get(best);		
	}
	/*
	// Returns the earliest state on the path that can HC-reachable to the goal
	public static SearchState computeBinaryReachableFrom(ArrayList<SearchState> path, SearchAlgorithm searchAlg, SearchState goal, StatsRecord stats)
	{	
		int endIndex = path.size()-1;
		int current, startIndex = 0, best = -1;		
		SearchState currentStart;		
			
		while (startIndex <= endIndex)
		{	
			current = (startIndex+endIndex)/2;			// Get mid-point
			System.out.println("Binary current: "+path.get(path.size()-1-current)+" Start: "+path.get(startIndex)+" End: "+path.get(endIndex));
			// Determine if can reach this point from the current start using HC
			currentStart = path.get(path.size()-1-current);
			if (!searchAlg.isPath(currentStart, goal, stats))
				endIndex = current-1;
			else
			{	// Can HC reach to here from current start, but can we do better?
				best = current;
				startIndex = current+1;
			}	
		}
		if (best == -1)
			return null;		// Note: This check is to handle the case that none of the states on the path can HC-reach goal.  Only happens if have not verified that every state in region can both HC to and from them during abstraction.
		System.out.println("Success current: "+path.get(path.size()-1-best));
		return path.get(path.size()-1-best);		
	}
	*/
	
	// Returns the earliest state on the path that can HC-reachable to the goal
	public static SearchState computeBinaryReachableFrom(ArrayList<SearchState> path, SearchAlgorithm searchAlg, SearchState goal, StatsRecord stats)
	{	
		int endIndex = path.size()-1;
		int current, startIndex = 0, best = -1;		
		SearchState currentStart;		
			
		while (startIndex <= endIndex)
		{	
			current = (startIndex+endIndex)/2;			// Get mid-point
			//System.out.println("Binary current: "+path.get(current)+" Start: "+path.get(startIndex)+" End: "+path.get(endIndex));
			// Determine if can reach this point from the current start using HC
			currentStart = path.get(current);
			if (searchAlg.isPath(currentStart, goal, stats))
			{	endIndex = current-1;
				best = current;
			}
			else
			{	// Cannot HC reach from here to goal - try farther along path				
				startIndex = current+1;
			}	
		}
		if (best == -1)
			return null;		// Note: This check is to handle the case that none of the states on the path can HC-reach goal.  Only happens if have not verified that every state in region can both HC to and from them during abstraction.
		//System.out.println("Success current: "+path.get(best));
		return path.get(best);		
	}
	/*
	 * Compresses path but path is in form of array of state ids rather than an ArrayList of states.
	 */
	public static int[] computeSubgoalsBinaryByIds(int[] path, SearchAlgorithm searchAlg, int[] tmp, int pathSize)
	{		
		StatsRecord stats = new StatsRecord();
		
		int current, startIndex = 0, best = startIndex+1;
		int endIndex = pathSize-1;
		int currentGoalId, currentStartId = path[startIndex];
		int count = 0;		
				
		while (startIndex < pathSize-1)
		{										
			currentGoalId = path[endIndex];			
			if (searchAlg.isPath(currentStartId, currentGoalId, stats))
				break;
			
			while (startIndex <= endIndex)
			{	
				current = (startIndex+endIndex)/2;			// Get mid-point
				// Determine if can reach this point from the current start using HC
				currentGoalId = path[current];
				if (!searchAlg.isPath(currentStartId, currentGoalId, stats))
					endIndex = current-1;
				else
				{	// Can HC reach to here from current start, but can we do better?
					if (current != startIndex)
						best = current;
					startIndex = current+1;
				}	
			}		
			// System.out.println(path[best]+" Goal: "+currentGoalId+" Last path entry: "+path[pathSize-1]);
			// Save current best as a subgoal 
			tmp[count++] = path[best];
			currentStartId = path[best];			
			startIndex = best;
			best = startIndex+1;
			endIndex = pathSize-1;
		}
				
		if (count == 0)
			return null;
		int[] result = new int[count];
		System.arraycopy(tmp, 0, result, 0, count);
		return result;
	}
	
	/**
	 * Similar to computeSubgoalsBinaryByIds but start and goal are both included in the compressed path.
	 * @param path
	 * @param searchAlg
	 * @param tmp
	 * @param pathSize
	 * @return
	 */
	public static int[] compressPath(int[] path, SearchAlgorithm searchAlg, int[] tmp, int pathSize)
	{		
		StatsRecord stats = new StatsRecord();
		
		int current, startIndex = 0, best = startIndex+1;
		int endIndex = pathSize-1;
		int currentGoalId, currentStartId = path[startIndex];
		int count = 0;		
		
		// Add start to path
		tmp[count++] = path[0];
		
		while (startIndex < pathSize-1)
		{										
			currentGoalId = path[endIndex];			
			if (searchAlg.isPath(currentStartId, currentGoalId, stats))
				break;
			
			while (startIndex <= endIndex)
			{	
				current = (startIndex+endIndex)/2;			// Get mid-point
				// Determine if can reach this point from the current start using HC
				currentGoalId = path[current];
				if (!searchAlg.isPath(currentStartId, currentGoalId, stats))
					endIndex = current-1;
				else
				{	// Can HC reach to here from current start, but can we do better?
					if (current != startIndex)
						best = current;
					startIndex = current+1;
				}	
			}		
			// Save current best as a subgoal 
			tmp[count++] = path[best];
			currentStartId = path[best];			
			startIndex = best;
			best = startIndex+1;
			endIndex = pathSize-1;
		}

		// Add end to path
		tmp[count++] = path[pathSize-1];		
		int[] result = new int[count];
		System.arraycopy(tmp, 0, result, 0, count);
		// System.out.println("Path states: "+pathSize+" Compressed size: "+count);
		return result;
	}
	
	public static boolean inStateList(ArrayList<SearchState> path, SearchState state)
	{
		for (SearchState s : path) {
			if (s.id == state.id)
				return true;
		}
		return false;
	}		
}
