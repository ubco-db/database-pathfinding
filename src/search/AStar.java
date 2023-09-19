package search;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * Improved A* implementation that uses BitSet for closed list and does not remove items in the open list when they are updated.  Rather, it adds a new object to the list and makes sure to
 * detect if the node has been previously used (in closed list) in main loop.  Removing items is very costly, so this is trading speed for some extra memory. 
 */
public class AStar implements SearchAlgorithm
{	
	private SearchProblem problem;
	private ArrayList<SearchState> statesExpanded;
	private BitSet closedList;
	
	public AStar(SearchProblem problem)
	{	this.problem = problem;
		statesExpanded = null;
		closedList = new BitSet(problem.getMaxSize());	// Bitset is sized the maximum # of states (ids) in the search problem
	}
		
	public ArrayList<SearchState> computePath(SearchState start, SearchState goal, StatsRecord stats)
	{
		// Setup open and closed state list		 
		PriorityQueue<SearchState> openList = new PriorityQueue<SearchState>();			// Note: Does not allow easy updates and searching for entries thus using openListLookup HashMap with it. 
		HashMap<Integer, SearchState> openListLookup = new HashMap<Integer, SearchState>();		
		closedList.clear();
		
		start.cost = 0;		
		start.prev = null;
		openListLookup.put(start.id, start);
		openList.add(start);
		int closedListCount = 0;
		
		statesExpanded = new ArrayList<SearchState>();
		
        boolean foundPath = false;
        
        while (!foundPath && openList.size() > 0)
        {        	
            // Find the lowest-cost state so far
            SearchState best = openList.remove();                 
            openListLookup.remove(best.id);
               
            if (closedList.get(best.id))        
            	continue;					// Possible that have entry in open list that was not removed            
            
            stats.incrementStatesExpanded(1);
            statesExpanded.add(best);
            // If the best location is the finish location then we're done!
            if (best.equals(goal))
            {	goal = best;                
                foundPath = true;
                break;
            }
                                                
            // Add to closed list
            closedList.set(best.id);            
            closedListCount++;
            
            // Update all neighbours of current state.
            updateNeighbors(best, goal, stats, openList, closedList, openListLookup);         
            
            if (openList.size() > stats.getOpenListSize())
            	stats.setOpenListSize(openList.size());
            if (closedListCount + openList.size() > stats.getMaxMemSize())
            	stats.setMaxMemSize(closedListCount+openList.size());
        }
        
        // Update statistics
        if (closedListCount > stats.getClosedListSize())
        	stats.setClosedListSize(closedListCount);
        if (openList.size() > stats.getOpenListSize())
        	stats.setOpenListSize(openList.size());        
        if (closedListCount + openList.size() > stats.getMaxMemSize())
        	stats.setMaxMemSize(closedListCount+openList.size());
        
        if (!foundPath)
        	return null;
        else
        {    
        	return buildPath(goal,stats);        	
        }
	}
		
	public boolean isPath(SearchState start, SearchState goal, StatsRecord stats)
	{		
		// Setup open and closed state list		 
		PriorityQueue<SearchState> openList = new PriorityQueue<SearchState>();			// Note: Does not allow easy updates and searching for entries thus using openListLookup HashMap with it.
		closedList.clear();
		HashMap<Integer, SearchState> openListLookup = new HashMap<Integer, SearchState>();		
		
		start.cost = 0;		
		start.prev = null;
		openListLookup.put(start.id, start);
		openList.add(start);
		int closedListCount = 0;
						
        boolean foundPath = false;
        
        while (!foundPath && openList.size() > 0)
        {        	
            // Find the lowest-cost state so far
            SearchState best = openList.remove();                 
            openListLookup.remove(best.id);
           // System.out.println(problem.idToString(best.id));
			// System.out.println(((SlidingTileProblem) problem).printTiles(( (SlidingTileState) best.stateData).tiles));          
            if (closedList.get(best.id))        
            	continue;					// Possible that have entry in open list that was not removed            
            
            stats.incrementStatesExpanded(1);
            statesExpanded.add(best);
            // If the best location is the finish location then we're done!
            if (best.equals(goal))
            {	goal = best;                
                foundPath = true;
                break;
            }
                                                
            // Add to closed list
            closedList.set(best.id);            
            closedListCount++;
            
            // Update all neighbours of current state.
            updateNeighbors(best, goal, stats, openList, closedList, openListLookup);         
            
            if (openList.size() > stats.getOpenListSize())
            	stats.setOpenListSize(openList.size());
            if (closedListCount + openList.size() > stats.getMaxMemSize())
            	stats.setMaxMemSize(closedListCount+openList.size());
        }
        
        // Update statistics
        if (closedListCount > stats.getClosedListSize())
        	stats.setClosedListSize(closedListCount);
        if (openList.size() > stats.getOpenListSize())
        	stats.setOpenListSize(openList.size());        
        if (closedListCount + openList.size() > stats.getMaxMemSize())
        	stats.setMaxMemSize(closedListCount+openList.size());
        
        return foundPath;        
	}
			
	/**
	 * Code to update the neighbors of an expanded state.  
	 */
	private void updateNeighbors(SearchState current, SearchState goal, StatsRecord stats, 
			PriorityQueue<SearchState> openList, BitSet closedList, HashMap<Integer, SearchState> openListLookup)
	{	
		ArrayList<SearchState> neighbors = problem.getNeighbors(current);
		for (int i=0; i < neighbors.size(); i++)
		{	SearchState next = neighbors.get(i);
					
			if (closedList.get(next.id))
				continue;

			stats.incrementStatesUpdated(1);
								
			int newG = current.g + problem.getMoveCost(current, next);

			// 	Add state to list.  If already there, update its cost only		
			Integer stateid = new Integer(next.id);		// Build integer object once to save time
			SearchState state = openListLookup.get(stateid);
			if (state != null)
			{
				if (state.g > newG)
				{
					SearchState st = new SearchState(state);
					st.updateCost(newG, problem.computeDistance(st, goal));				
					st.prev = current;									
					openList.add(st);
					openListLookup.put(stateid, st);
				}
			}
			else
			{										
				next.updateCost(newG, problem.computeDistance(next, goal));					
				next.prev = current;
				openList.add(next);
				openListLookup.put(stateid, next);
			}			
		}						
	}	
	
	/**
	 * Builds a path as found by A*.
	 */
	public ArrayList<SearchState> buildPath(SearchState goal, StatsRecord stats)
	{
		ArrayList<SearchState> path = new ArrayList<SearchState>();
		// Construct path now
    	SearchState curr = goal;
    	int len = 0, cost = 0;
    	while (curr != null)
    	{	path.add(0,curr);
    		if (curr.prev != null)
    		//	cost += problem.computeDistance(curr, curr.prev);
    			cost += problem.getMoveCost(curr, curr.prev);
			curr = curr.prev;
    		len++;        		
    	}
    	stats.setPathCost(cost);
    	stats.setPathLength(len);
    	return path;
	}

	public ArrayList<SearchState> getStatesExpanded() {
		return statesExpanded;
	}
	
	public boolean isPath(int startId, int goalId, StatsRecord stats)
	{
		return computePath(new SearchState(startId), new SearchState(goalId), stats) != null;
	}
}

