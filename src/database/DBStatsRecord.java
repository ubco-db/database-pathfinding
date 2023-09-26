package database;

import util.StringFunc;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Stores statistics on database generation.
 *
 * @author rlawrenc
 */
public class DBStatsRecord {
    private final ArrayList<Object> stats;

    public DBStatsRecord() {
        stats = new ArrayList<>();
    }

    public DBStatsRecord(int size) {
        stats = new ArrayList<>(size);
        // Fill with blank-data
        for (int i = 0; i < size; i++) {
            stats.add("0");
        }
    }

    // Assumes array has already been filled with empty values
    public void addStat(int pos, Object value) {
        stats.set(pos, value);
    }

    public Object getStat(int pos) {
        return stats.get(pos);
    }

    public void output(PrintWriter out) {
        for (Object stat : stats) {
            out.print(StringFunc.pad(stat.toString(), 10) + "\t");
        }
        out.println();
    }
}
