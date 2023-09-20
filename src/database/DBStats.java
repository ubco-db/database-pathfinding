package database;

import util.StringFunc;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Stores statistics on database generation.
 *
 * @author rlawrenc
 */
public class DBStats {
    private final ArrayList<DBStatsRecord> stats;
    private final ArrayList<String> statsNames;

    public DBStats() {
        stats = new ArrayList<DBStatsRecord>();
        statsNames = new ArrayList<String>();
    }

    public void addRecord(DBStatsRecord rec) {
        stats.add(rec);
    }

    public void outputNames(PrintWriter out) {
        for (String statsName : statsNames) {
            out.print(StringFunc.pad(statsName, 10) + "\t");
        }
        out.println();
    }

    public void outputData(PrintWriter out) {
        for (DBStatsRecord stat : stats) stat.output(out);
    }

    public static void init(DBStats db) {    // Initializes record with default layout that we will use
        db.statsNames.add("Algorithm");        // 0
        db.statsNames.add("Level");            // 1
        db.statsNames.add("Records");
        db.statsNames.add("Cutoff");
        db.statsNames.add("Map");
        db.statsNames.add("Rows");
        db.statsNames.add("Cols");
        db.statsNames.add("States");
        db.statsNames.add("Records");
        db.statsNames.add("Subgoals");
        db.statsNames.add("OverallTime");
        db.statsNames.add("Areas");
        db.statsNames.add("AbstractTime");
        db.statsNames.add("A* Time");
        db.statsNames.add("SubgoalTime");
        db.statsNames.add("DPTime");
        db.statsNames.add("BaseTime");
        db.statsNames.add("RecordTime");
        db.statsNames.add("NeighborTime");
        db.statsNames.add("MapComplexity");
        db.statsNames.add("MaxStateSize");
        db.statsNames.add("MinStateSize");
        db.statsNames.add("AvgStateSize");
        db.statsNames.add("IndexTime");
        db.statsNames.add("IndexSize");
        db.statsNames.add("HTSize");
    }

    public int getSize() {
        return statsNames.size();
    }
}
