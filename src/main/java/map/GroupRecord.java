package map;

import util.ExpandArray;

import java.util.ArrayList;
import java.util.HashSet;

public class GroupRecord {
    public int groupId;
    public int groupRepId;
    public ArrayList<Integer> states;
    private int numStates;
    private HashSet<Integer> neighborIds;
    private HashSet<Integer> computedNeighborIds;

    public GroupRecord() {
        states = new ArrayList<Integer>();
    }

    public int getSize() {
        return numStates;
    }

    public void setNeighborIds(HashSet<Integer> neighborIds) {
        this.neighborIds = neighborIds;
    }

    // Returns true if two groups are neighbors by virtue of one group having a base state that is in the neighbor list of the other.
    // Since assuming non-directed edges (this works either way as both states should have the same pair of nodes as neighbors in the neighbor lists).
    public boolean isNeighbor(GroupRecord g) {
        HashSet<Integer> otherGroupNeighbors = g.getNeighborIds();
        if (otherGroupNeighbors == null || neighborIds == null)
            return false;

        for (int nodeId : neighborIds) {
            if (otherGroupNeighbors.contains(nodeId))
                return true;
        }
        return false;
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

    public int getGroupRepId() {
        return groupRepId;
    }

    public void setGroupRepId(int groupRepId) {
        this.groupRepId = groupRepId;
    }

    public int getNumStates() {
        return numStates;
    }

    public void setNumStates(int numStates) {
        this.numStates = numStates;
    }

    public HashSet<Integer> getComputedNeighborIds() {
        return computedNeighborIds;
    }

    public void setComputedNeighborIds(HashSet<Integer> immediateNeighborIds) {
        this.computedNeighborIds = immediateNeighborIds;
    }

    public void setStates(ArrayList<Integer> states) {
        this.states = states;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(100);
        buf.append("Id: ").append(groupId).append(" Rep. Id: ").append(groupRepId).append(" Size: ").append(numStates);
        buf.append(" States: ").append(states.toString());
        if (neighborIds != null) buf.append(" Neighbors: ").append(neighborIds);
        return buf.toString();
    }
}