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

    public int getSize() {
        return statsNames.size();
    }
}
