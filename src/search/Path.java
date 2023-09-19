package search;

import java.util.ArrayList;

/**
 * Stores path information
 * @author rlawrenc
 *
 */
public class Path implements Comparable {

	private ArrayList<SearchState> states;
	private double cost;
	private int length;
	private SearchState start, goal;
	
	public Path(ArrayList<SearchState> states, double cost, int length, SearchState start, SearchState goal)
	{	this.states = states;
		this.cost = cost;
		this.length = length;
		this.start = start;
		this.goal = goal;
	}
	
	public int compareTo(Object p) {
		if (p instanceof Path)
			return (int) (this.cost - ((Path) p).cost);
		return -1;
	}

	public ArrayList<SearchState> getStates() {
		return states;
	}

	public void setStates(ArrayList<SearchState> states) {
		this.states = states;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public SearchState getStart() {
		return start;
	}

	public void setStart(SearchState start) {
		this.start = start;
	}

	public SearchState getGoal() {
		return goal;
	}

	public void setGoal(SearchState goal) {
		this.goal = goal;
	}
	
}
