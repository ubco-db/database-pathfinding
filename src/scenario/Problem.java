package scenario;

import search.SearchState;

import java.io.IOException;
import java.io.PrintWriter;


/**
 * Information about a search problem.
 * @author rlawrenc
 *
 */
public class Problem {
	private String mapName;
	private SearchState start;
	private SearchState goal;
	private int optimalTravelCost;
	private double aStarDifficulty;
	
	
	public Problem(String mapName, SearchState start, SearchState goal, int optimalTravelCost, double starDifficulty) {
		super();
		this.mapName = mapName;
		this.start = start;
		this.goal = goal;
		this.optimalTravelCost = optimalTravelCost;
		aStarDifficulty = starDifficulty;
	}
	
	public String getMapName() {
		return mapName;
	}
	public void setMapName(String mapName) {
		this.mapName = mapName;
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
	public int getOptimalTravelCost() {
		return optimalTravelCost;
	}
	public void setOptimalTravelCost(int optimalTravelCost) {
		this.optimalTravelCost = optimalTravelCost;
	}
	public double getAStarDifficulty() {
		return aStarDifficulty;
	}
	public void setAStarDifficulty(double starDifficulty) {
		aStarDifficulty = starDifficulty;
	}

	public void write(PrintWriter out) throws IOException
	{
		out.println(this.toString());
	}

	public String toString()
	{	return mapName + "\t" + start +"\t" + goal + "\t" + optimalTravelCost + "\t" + this.aStarDifficulty;
	}
}
