package search;

import map.GameMap;
import map.GroupRecord;
import util.ExpandArray;
import util.HeuristicFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Supports grid-based search problems (for game maps).
 *
 * @author rlawrenc
 */
public class MapSearchProblem extends SearchProblem {
    private final GameMap map;
    private int row;
    private int col;

    public MapSearchProblem(GameMap map) {
        this.map = map;
    }

    public int computeDistance(SearchState start, SearchState goal) {
        // return GameMap.computeDistance(map.getRow(start.id), map.getCol(start.id), map.getRow(goal.id), map.getCol(goal.id));
        return GameMap.computeDistance(start.id, goal.id, map.cols);

    }

    public int computeDistance(SearchState start, SearchState goal, HeuristicFunction heuristic) {
        // return GameMap.computeDistance(map.getRow(start.id), map.getCol(start.id), map.getRow(goal.id), map.getCol(goal.id));
        return GameMap.computeDistance(start.id, goal.id, map.cols, heuristic);

    }

    public int computeDistance(int startId, int goalId) {
        //return GameMap.computeDistance(map.getRow(startId), map.getCol(startId), map.getRow(goalId), map.getCol(goalId));
        return GameMap.computeDistance(startId, goalId, map.cols);
    }

    public int computeDistance(int startId, int goalId, HeuristicFunction heuristic) {
        //return GameMap.computeDistance(map.getRow(startId), map.getCol(startId), map.getRow(goalId), map.getCol(goalId));
        return GameMap.computeDistance(startId, goalId, map.cols, heuristic);
    }

    public ArrayList<SearchState> getNeighbors(SearchState state) {
        return map.getNeighbors(map.getRow(state.id), map.getCol(state.id));
    }

    public int getMaxSize() {
        return map.rows * map.cols;
    }


    public GameMap getMap() {
        return map;
    }

    public void initIterator() {
        row = 0;
        col = 0;
    }

    public boolean nextState(SearchState state) {
        return getNextState(state);
    }

    private boolean getNextState(SearchState state) {
        for (; row < map.rows; row++) {
            for (; col < map.cols; col++) {
                if (map.squares[row][col] != GameMap.WALL_CHAR) {
                    state.id = map.getId(row, col);
                    state.cost = map.squares[row][col++] - GameMap.START_NUM;
                    return true;
                }
            }
            col = 0;
        }
        return false;
    }

    public SearchState generateRandomState(Random generator) {
        int row, col;
        int maxRow = map.rows;
        int maxCol = map.cols;

        do {
            row = generator.nextInt(maxRow);
            col = generator.nextInt(maxCol);
        } while (map.isWall(row, col));
        return new SearchState(map.getId(row, col));
    }

    public String idToString(int id) {
        return "(" + map.getRow(id) + ", " + map.getCol(id) + ")";
    }
	/*
	public IndexDB generateIndexDBZOrder()
	{	// Creates a sorted array as the index DB.  Each entry has a node id and a seed id.
		// Searching is performed using binary search.
		IndexDB db = new IndexDB();
		
		int id = -1;
		int lastCell = -1;
		int numCells = 0;
		// IDEA: Scan cells in order of index number.  When hit new cell that is not a wall and different state than last, add entry to the DB.				
		
		// Begin by storing the ids of the matrix
		int[]ids = new int[rows*cols];
		// Z-index version
		int idCount = 0;
		for (int i=0; i < rows; i++)
		{	for (int j=0; j < cols; j++)
			{	ids[idCount++] = this.getZId(i,j);			
			}
		}
		
		Arrays.sort(ids,0,idCount);
		for (int i=0; i < idCount; i++)
		{
			id = ids[i];
			int r = getZRow(id);
			int c = getZCol(id);
				
			// System.out.println("Id: "+id+" Row: "+r+" Col: "+c);
			if (this.squares[r][c] != WALL_CHAR)
			{	numCells++;
				if (this.squares[r][c] != lastCell)				
				{	db.add(id,this.squares[r][c]);	// TODO: For now encoding state # rather than seed it as not sure I have seed it at this point.
					lastCell = this.squares[r][c];
				}			
			}
		}	
		
		db.setTotalCells(numCells);		
		return db;
	}
	*/

    public HashMap<Integer, GroupRecord> getGroups() {
        return map.getGroups();
    }

    public void computeNeighbors() {
        map.computeNeighbors();
    }


    public int getMoveCost(int startId, int goalId) {    // Assumes they are not the same state (as move cost would be zero then)

        return 1; // Update: Only allowing 4 directional movement
		/*
		 // Original code
		int moveCost = (Math.abs(map.getRow(startId)-map.getRow(goalId)) + Math.abs(map.getCol(startId)-map.getCol(goalId)))*10;
		if (moveCost == 20)
			moveCost = 14;		// Diagonal movement	
		 return moveCost;
		*/		
		/*
		// This was current code for a diagonal movement
		
		int diff = startId-goalId;
		int bit31 = diff >> 31;
		diff = (diff ^ bit31) - bit31;
				
		if (diff == 1 || diff == map.cols)
			return 10;
		else
			return 14;	
		*/

        // if (startId == goalId)
        //	return 0;
        // return 50;
	
		/*
		boolean startOdd = startId % 2 == 0;
		boolean goalOdd = goalId % 2 == 0;
		if (startOdd && goalOdd)
			return 30;
		else if (startOdd || goalOdd)
			return 50;
		else
			return 60;
	*/
		/*
		// Certain probability of a really expensive edge
		if ( (startId+goalId) % 10 == 0)		// Expensive edge
			return 100;
		// This below is same formula for regular edges
		if (diff == 1 || diff == map.cols)
			return 10;
		else
			return 14;	
			*/
		/*
		int rowDiff = map.getRow(startId) - map.getRow(goalId);
		int colDiff = map.getCol(startId) - map.getCol(goalId);
		int cost = 0;
		if (rowDiff > 0)
			cost = 30;
		else if (rowDiff < 0)
			cost = 10;
		if (colDiff > 0)
			cost += 30;
		else if (colDiff < 0)
			cost += 10;
		return cost;
		*/
    }

    public int getMoveCost(SearchState start, SearchState goal) {
        return getMoveCost(start.id, goal.id);
		/*
		int c = getMoveCost(start.id, goal.id);
		int moveCost = (Math.abs(map.getRow(start.id)-map.getRow(goal.id)) + Math.abs(map.getCol(start.id)-map.getCol(goal.id)))*10;
		if (moveCost == 20)
			moveCost = 14;		// Diagonal movement
		
		if (c != moveCost)
		{	System.out.println("ERROR: Costs do not match;");
			System.exit(1);
		}
		return moveCost;
		*/
        // return 50;			// Hard-code that all moves cost the same
		
		/*
		boolean startOdd = start.id % 2 == 0;
		boolean goalOdd = goal.id % 2 == 0;
		if (startOdd && goalOdd)
			return 30;
		else if (startOdd || goalOdd)
			return 50;
		else
			return 60;	
			*/
		/*
		int rowDiff = map.getRow(start.id) - map.getRow(goal.id);
		int colDiff = map.getCol(start.id) - map.getCol(goal.id);
		int cost = 0;
		if (rowDiff > 0)
			cost = 30;
		else if (rowDiff < 0)
			cost = 10;
		if (colDiff > 0)
			cost += 30;
		else if (colDiff < 0)
			cost += 10;
		return cost;
		*/

    }

    public void getNeighbors(int stateId, ExpandArray neighbors) {
        map.getNeighbors(map.getRow(stateId), map.getCol(stateId), neighbors);
    }

    // Computes the groups in a map
    public void computeGroups() {
        map.computeGroups();
    }
}
