package map;

import util.ExpandArray;

import java.util.HashSet;
import java.util.Iterator;

public class GroupRecord {
    public int groupId;
    public int groupRepId;
    public ExpandArray states;
    private int numStates;
    private HashSet<Integer> neighborIds;
    private HashSet<Integer> computedNeighborIds;

    public GroupRecord() {
        states = new ExpandArray();
    }

    public int getSize() {
        return numStates;
    }

    public void setNeighborIds(HashSet<Integer> neighborIds) {
        this.neighborIds = neighborIds;
    }


    public void setNeighbors(ExpandArray neighbors) {
        this.neighborIds = new HashSet<Integer>(neighbors.num());
        for (int k = 0; k < neighbors.num(); k++) {
            neighborIds.add(neighbors.get(k));
        }
    }

    public HashSet<Integer> getNeighborIds() {
        return neighborIds;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getGroupRepId() {
        return groupRepId;
    }

    public void setGroupRepId(int groupRepId) {
        this.groupRepId = groupRepId;
    }

    public void setNumStates(int numStates) {
        this.numStates = numStates;
    }

    // Returns true if two groups are neighbors by virtue of one group having a base state that is in the neighbor list of the other.
    // Since assuming non-directed edges (this works either way as both states should have the same pair of nodes as neighbors in the neighbor lists).
    public boolean isNeighbor(GroupRecord g) {
        HashSet<Integer> otherGroupNeighbors = g.getNeighborIds();
        if (otherGroupNeighbors == null || neighborIds == null)
            return false;

        Iterator<Integer> it = neighborIds.iterator();
        while (it.hasNext()) {
            int nodeId = it.next();
            if (otherGroupNeighbors.contains(nodeId))
                return true;
        }
        return false;
    }

    public HashSet<Integer> getComputedNeighborIds() {
        return computedNeighborIds;
    }

    public void setComputedNeighborIds(HashSet<Integer> immediateNeighborIds) {
        this.computedNeighborIds = immediateNeighborIds;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(100);
        buf.append("Id: " + groupId + " Rep. Id: " + groupRepId + " Size: " + numStates);
        buf.append(" States: " + states.toString());
        if (neighborIds != null)
            buf.append(" Neighbors: " + neighborIds.toString());
        return buf.toString();
    }
}