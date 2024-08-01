package map;

import java.util.Set;
import java.util.TreeSet;

public class Region {
    private final int regionId;
    private final Set<Integer> neighborIds;
    private int regionRepresentative;
    private int numStates;

    public Region(int regionId, int numStates) {
        this.regionId = regionId;
        this.numStates = numStates;
        this.neighborIds = new TreeSet<>();
    }

    public Region(int regionId, int regionRepresentative, int numStates) {
        this.regionId = regionId;
        this.regionRepresentative = regionRepresentative;
        this.numStates = numStates;
        this.neighborIds = new TreeSet<>();
    }

    public Region(int regionId, int regionRepresentative, int numStates, Set<Integer> neighborIds) {
        this.regionId = regionId;
        this.regionRepresentative = regionRepresentative;
        this.numStates = numStates;
        this.neighborIds = neighborIds;
    }

    public int getRegionId() {
        return regionId;
    }

    public int getRegionRepresentative() {
        return regionRepresentative;
    }

    public void setRegionRepresentative(int regionRepresentative) {
        this.regionRepresentative = regionRepresentative;
    }

    public int getNumStates() {
        return numStates;
    }

    public void setNumStates(int numStates) {
        this.numStates = numStates;
    }

    public Set<Integer> getNeighborIds() {
        return neighborIds;
    }

    public void addNeighborId(int neighborId) {
        neighborIds.add(neighborId);
    }

    public void resetNeighborIds() {
        neighborIds.clear();
    }

    public void deleteNeighborIds(Set<Integer> neighborIds) {
        this.neighborIds.removeAll(neighborIds);
    }

    public void incrementNumStates() {
        numStates++;
    }

    public void decrementNumStates() {
        numStates--;
    }

    // We're not outputting the region rep because this method is used in a test
    // The region reps may not be equal before and after wall changes
    @Override
    public String toString() {
        return "Region{" +
                "regionId=" + regionId +
                ", numStates=" + numStates +
                ", neighborIds=" + neighborIds +
                "}\n";
    }
}
