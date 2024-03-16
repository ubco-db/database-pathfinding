package map;

import java.util.ArrayList;
import java.util.HashSet;

public class GroupRecord {
    public int groupId;
    public int groupRepId;
    private int numStates;
    private HashSet<Integer> neighborIds;
    private HashSet<Integer> computedNeighborIds;

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

    public String toString() {
        StringBuilder buf = new StringBuilder(100);
        buf.append("Id: ").append(groupId).append(" Rep. Id: ").append(groupRepId).append(" Size: ").append(numStates);
        if (neighborIds != null) buf.append(" Neighbors: ").append(neighborIds);
        return buf.toString();
    }

    public void decrementNumStates() throws Exception {
        if (numStates < 0) {
            throw new Exception("numStates cannot be negative!");
        }
        numStates--;
    }

    public void incrementNumStates() {
        numStates++;
    }

    /* SETTERS */

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public void setGroupRepId(int groupRepId) {
        this.groupRepId = groupRepId;
    }

    public void setNumStates(int numStates) throws Exception {
        if (numStates < 0) {
            throw new Exception("numStates cannot be negative!");
        }
        this.numStates = numStates;
    }

    public void setNeighborIds(HashSet<Integer> neighborIds) {
        this.neighborIds = neighborIds;
    }

    public void setNeighbors(ArrayList<Integer> neighbors) {
        this.neighborIds = new HashSet<>(neighbors.size());
        neighborIds.addAll(neighbors);
    }

    public void setComputedNeighborIds(HashSet<Integer> immediateNeighborIds) {
        this.computedNeighborIds = immediateNeighborIds;
    }

    /* GETTERS */

    public int getGroupId() {
        return groupId;
    }

    public int getGroupRepId() {
        return groupRepId;
    }

    public int getNumStates() {
        return numStates;
    }

    public HashSet<Integer> getNeighborIds() {
        return neighborIds;
    }

    public HashSet<Integer> getComputedNeighborIds() {
        return computedNeighborIds;
    }
}