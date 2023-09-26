package map;

import java.util.ArrayList;


/**
 * Implements a sparse mask that overlays different color squares on a 2D map.
 * Usually used to visualize a path or regions in the map.
 * Sparse mask uses an array of changes.  This becomes less efficient as the percentage of changes to the map increases.
 */
public class SparseMask extends MapMask {
    private ArrayList<ChangeRecord> changes;
    private int iteratorLoc;

    public SparseMask() {
        changes = new ArrayList<ChangeRecord>();
    }

    public SparseMask(SparseMask mask) {
        changes = new ArrayList<ChangeRecord>(mask.changes.size());
        for (int i = 0; i < mask.changes.size(); i++) {
            this.changes.add(new ChangeRecord(mask.changes.get(i)));
        }
    }

    public void clear() {
        changes.clear();
    }

    public void add(ChangeRecord rec) {
        changes.add(rec);
    }

    public void init() {
        iteratorLoc = 0;
    }

    public boolean hasNext() {
        return iteratorLoc < changes.size();
    }

    public ChangeRecord next() {
        return changes.get(iteratorLoc++);
    }
}
