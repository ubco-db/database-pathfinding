package map;

import util.ExpandArray;

import java.util.HashSet;

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

    public int getGroupRepId() {
        return groupRepId;
    }

    public void setGroupRepId(int groupRepId) {
        this.groupRepId = groupRepId;
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

    public String toString() {
        StringBuilder buf = new StringBuilder(100);
        buf.append("Id: ").append(groupId).append(" Rep. Id: ").append(groupRepId).append(" Size: ").append(numStates);
        buf.append(" States: ").append(states.toString());
        if (neighborIds != null) buf.append(" Neighbors: ").append(neighborIds);
        return buf.toString();
    }
}