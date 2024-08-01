package map;

import java.util.List;

public class Sector {
    private final int sectorId;
    private final List<Region> regions;

    public Sector(int sectorId, List<Region> regions) {
        this.sectorId = sectorId;
        this.regions = regions;
    }

    public int getNumRegions() {
        return regions.size();
    }

    public Region getFirstRegion() {
        return regions.getFirst();
    }

    public List<Region> getRegions() {
        return regions;
    }
}
