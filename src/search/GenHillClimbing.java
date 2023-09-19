package search;

import util.ExpandArray;
import util.HeuristicFunction;

import java.util.ArrayList;

/**
 * Returns a path if can get to (r2,c2) from (r1,c1) using only greedy movements (hill-climbing).
 * Include start and end in the path.
 * Cutoff is maximum # of moves that can make and be HC reachable
 */
public class GenHillClimbing implements SearchAbstractAlgorithm
{
	private SearchProblem problem;	
	private int cutoff;
	private ExpandArray path = new ExpandArray(100);
	private ExpandArray neighbors = new ExpandArray(10);	
	private ExpandArray neighbors2 = new ExpandArray(10);
	private boolean doneCall;
	private boolean tieBreak;
	
	private int nextH, nextId;

	private HeuristicFunction heuristic;

	public GenHillClimbing(SearchProblem problem, int cutoff, boolean tieBreak, HeuristicFunction heuristic)
	{	this.problem = problem;		
		this.cutoff = cutoff;
		this.tieBreak = tieBreak;
		this.heuristic = heuristic;
	}

	public GenHillClimbing(SearchProblem problem, int cutoff, HeuristicFunction heuristic)
	{	this(problem, cutoff, false, heuristic);
	}

	public GenHillClimbing(SearchProblem problem, int cutoff, boolean tieBreak)
	{	// standard de facto standard heuristic
		this(problem, cutoff, tieBreak, new HeuristicFunction()
		{
			public int apply(int startId, int goalId, int ncols) {
				int startRow = startId/ncols;
				int goalRow = goalId/ncols;
				int diffRow = startRow - goalRow;
				
				int bit31 = diffRow >> 31;				// Compute its absolute value
				diffRow = (diffRow ^ bit31) - bit31;
						
				int diffCol = ((startId - startRow* ncols) - (goalId - goalRow* ncols));
				bit31 = diffCol >> 31;				// Compute its absolute value
				diffCol = (diffCol ^ bit31) - bit31;		
							
				if (diffRow > diffCol)						// TODO: Any way to avoid the if statement here?
					return diffCol * 14 + (diffRow-diffCol) * 10;
				else
					return diffRow * 14 + (diffCol-diffRow) * 10;		
			}
		});
	}
	
	public GenHillClimbing(SearchProblem problem, int cutoff)
	{	this(problem, cutoff, false);
	}
		
	/*
	 * Returns minimum distance of a node's neighbors.
	 */
	private void getMinDistance(int currId, int goalId, ExpandArray neighbors)
	{
		int cost, minCost, h, tmpId, moveCost,  nextMoveCost;			
		
		nextId = neighbors.get(0);
		//nextH = problem.computeDistance(nextId, goalId);
		nextH = problem.computeDistance(nextId, goalId, heuristic);
		nextMoveCost = problem.getMoveCost(currId, nextId);
		minCost = nextMoveCost+nextH; 		
		
		for (int i=1; i < neighbors.num(); i++)
		{	tmpId = neighbors.get(i);
			//h = problem.computeDistance(tmpId, goalId);
			h = problem.computeDistance(tmpId, goalId, heuristic);
			moveCost = problem.getMoveCost(currId,tmpId);
			cost = moveCost+h;			
			
			if (cost < minCost || (cost == minCost && moveCost > nextMoveCost))	// Break ties in favor of higher move cost (in order received)
			{	minCost = cost;
				nextId = tmpId;
				nextH = h;
				nextMoveCost = moveCost;
			}			
		}		
	}
	
	private ExpandArray computeIdPath(int startId, int goalId, StatsRecord stats)
	{			
		int currId = startId;
		int currH;
		int count = 0;		
		
		//currH = problem.computeDistance(currId, goalId);
		currH = problem.computeDistance(currId, goalId, heuristic);
		path.clear();		
		while (count < cutoff)
		{
			path.add(currId);					
			if (currId == goalId)			
			{	
				return path;			
			}
			
			// Expand neighbors and pick smallest
			problem.getNeighbors(currId, neighbors);		
			if (neighbors.num() == 0)
			{							
				return null;		// No path possible			
			}
			
			doneCall = false;
			getMinDistance(currId, goalId, neighbors);
			
			// Need to check if at a plateau here (need to remember cost up to this point)
		//	System.out.println("\t"+problem.idToString(nextId));
			if (nextH >= currH)	// If current node is better than its best child in a local minima.  No HC path is possible.
			{	
				return null;
			}
			
			currId = nextId;
			currH = nextH;
			count++;
		}		
		return null;	// No path found
	}	
	
	public ArrayList<SearchState> computePath(SearchState start, SearchState goal, StatsRecord stats)
	{	// Now create objects for path
		ExpandArray path = computeIdPath(start.id, goal.id, stats);
		if (path == null)
			return null;
		ArrayList<SearchState> result = new ArrayList<SearchState>(path.num());
		for (int i=0; i < path.num(); i++)
			result.add(new SearchState(path.get(i)));
		SearchUtil.computePathCost(result, stats, problem);
		return result;
		
	}
	public boolean isPath(SearchState start, SearchState goal, StatsRecord stats)
	{
		return computeIdPath(start.id, goal.id, stats) != null;
	}
	
	public boolean isPath(int startId, int goalId, StatsRecord stats)
	{
		return computeIdPath(startId, goalId, stats) != null;
	}
	
	public int getCutoff()
	{
		return cutoff;
	}

	public void setCutoff(int cutoff)
	{
		this.cutoff = cutoff;
	}
	
	public int isPath(int startId, int goalId, StatsRecord stats, SavedSearch database)
	{	
		return computePath(startId, goalId, stats, database);
		/*
		// This computes the cost
		int lastId = path.get(0);
		int cost = 0;
		for (int i=1; i < path.num(); i++)
		{	int currentId = path.get(i);
			//cost += problem.computeDistance(lastId, currentId);
			cost += problem.computeDistance(lastId, currentId, heuristic);
			lastId = currentId;
		}
		return cost;
		*/		
	}	
			
	private int computePath(int startId, int goalId, StatsRecord stats, SavedSearch database)
	{			
		int currId = startId;
		int currH;
		int count = 0;		
		
		// currH = problem.computeDistance(currId, goalId);		
		currH = problem.computeDistance(currId, goalId, heuristic);		
		while (count < cutoff)
		{						
			if (currId == goalId)			
				return count+1;			
			
			// Expand neighbors and pick smallest
			problem.getNeighbors(currId, neighbors);		
			if (neighbors.num() == 0)
				return -1;		// No path possible			
						
			getMinDistance(currId, goalId, neighbors);
			
			// Need to check if at a plateau here (need to remember cost up to this point)
			if (nextH >= currH)	// If current node is better than its best child in a local minima.  No HC path is possible.
        		return -1;
			
			// Check database
			int val = database.getFoundVal(nextId);
			if (val != -1)
			{	if (val+count+1 > cutoff)
				{	// System.out.println("Ended search early. Val: "+(val+count+1));
					return -1;
				}
				else
				{	// System.out.println("Ended search early success. Val: "+(val+count+1));
					/*
					// Verify a find
					ExpandArray path = computeIdPath(startId, goalId, stats);
					if (path == null)
					{	System.out.println("ERROR: NO path exists.");		
						computePath(startId, goalId, stats, database);
						return -1;
					}
					// Check path cost
					ArrayList<SearchState> result = new ArrayList<SearchState>(path.num());
					for (int i=0; i < path.num(); i++)
						result.add(new SearchState(path.get(i)));
					SearchUtil.computePathCost(result, stats, problem);
					
					if (stats.getPathLength() != val+count+1)
					{	System.out.println("ERROR: Incorrect path length.");					
						return -1;
					}
					*/					
					return val+count+1;			
				}
			}
					
			currId = nextId;
			currH = nextH;
			count++;
		}
		
		return -1;	// No path found
	}	
}
