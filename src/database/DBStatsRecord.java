package database;

import util.StringFunc;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Stores statistics on database generation.
 * @author rlawrenc
 *
 */
public class DBStatsRecord 
{
	private ArrayList<Object> stats;	

	public DBStatsRecord()
	{
		stats = new ArrayList<Object>();
	}
	
	public DBStatsRecord(int size)
	{
		stats = new ArrayList<Object>(size);
		// Fill with blank-data
		for (int i=0; i < size; i++)
			stats.add("0");
	}
	
	public void clearStats()
	{
		stats.clear();		
	}
	
	// Assumes array has already been filled with empty values
	public void addStat(int pos, Object value)
	{	stats.set(pos, value);		
	}
	
	
	public Object getStat(int pos)
	{	return stats.get(pos);		
	}
	
	public void output(PrintWriter out, ArrayList<String> statsNames)
	{
		for (int i=0; i < stats.size(); i++)
		{	out.print(StringFunc.pad(stats.get(i).toString(),10)+"\t");			
		}
		out.println();
	}
}
