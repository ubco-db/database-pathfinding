package search;

import map.AbstractedMap;
import map.Region;
import map.Sector;

import java.util.*;

/**
 * Supports searches of abstract regions used by PRA.
 */
public class RegionSearchProblem extends SearchProblem {
    private final AbstractedMap abstractedMap;

    private final List<Integer> neighbourIds;

    public RegionSearchProblem(AbstractedMap abstractedMap) {
        this.abstractedMap = abstractedMap;

        this.neighbourIds = new ArrayList<>();
    }

    /**
     * Given a search state s in base space find and return its region
     * representative.
     * <p>
     * This code assumed that we cannot simply look up the region id stored in the states array. This could be the case
     * if we're not storing the abstracted map for memory reasons. In that case, the code would work with an unabstracted
     * states array (from GameMap).
     *
     * @param s SearchState to find region representative for
     * @return SearchState containing region representative
     */
    public SearchState findRegionRepresentative(SearchState s) {
        int sid = s.getStateId();
        int row = abstractedMap.getRowFromStateId(sid);
        int col = abstractedMap.getColFromStateId(sid);
        int sectorId = abstractedMap.getSectorId(row, col);

        Sector sector = abstractedMap.getSector(sectorId);
        int startRow = abstractedMap.getStartRowOfSector(sectorId);
        int startCol = abstractedMap.getStartColOfSector(sectorId);
        int endRow = abstractedMap.getEndRowOfSector(sectorId);
        int endCol = abstractedMap.getEndColOfSector(sectorId);

        // If the sector only has one region, can just return the rep of that region
        if (sector.getNumRegions() == 1) {
            int regionRep = sector.getFirstRegion().getRegionRepresentative();
            return new SearchState(regionRep);
        } else {
            // Check if state itself is a region rep
            for (Region region : sector.getRegions()) {
                if (sid == region.getRegionRepresentative()) {
                    return new SearchState(sid);
                }
            }

            // If not, need to perform constrained BFS in sector
            Queue<Integer> stateIds = new LinkedList<>();
            stateIds.add(sid);
            HashSet<Integer> visited = new HashSet<>();
            visited.add(sid);

            while (!stateIds.isEmpty()) {
                int id = stateIds.remove();

                for (int neighbourId : abstractedMap.getStateNeighbourIds(id)) {
                    int nr = abstractedMap.getRowFromStateId(neighbourId);
                    int nc = abstractedMap.getColFromStateId(neighbourId);
                    if (abstractedMap.isInRangeAndNotWall(nr, nc, startRow, startCol, endRow, endCol) && !visited.contains(neighbourId)) {
                        stateIds.add(neighbourId);
                        visited.add(neighbourId);

                        // See if we've matched a region id
                        for (Region region : sector.getRegions()) {
                            if (neighbourId == region.getRegionRepresentative()) {
                                return new SearchState(neighbourId);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Given a search state s in base space find and return its region
     * representative.
     * <p>
     * This code does not make the same assumption as above.
     *
     * @param s SearchState to find region representative for
     * @return SearchState containing region representative
     */
    public SearchState findRegionRepresentativeFromMap(SearchState s) {
        if (abstractedMap.isWall(s.getStateId())) {
            throw new RuntimeException("Cannot find region id for wall state.");
        }
        int regionId = abstractedMap.getRegionIdFromMap(s.getStateId());
        return new SearchState(abstractedMap.getRegionById(regionId).getRegionRepresentative());
    }

    @Override
    public void getNeighbours(SearchState currentState, List<SearchState> neighbours) {
        if (!neighbours.isEmpty()) {
            throw new RuntimeException();
        }

        neighbourIds.clear();
        getNeighbourIds(currentState.getStateId(), neighbourIds);

        for (int neighbourId : neighbourIds) {
            neighbours.add(new SearchState(abstractedMap.getRegionById(neighbourId).getRegionRepresentative()));
        }
    }

    @Override
    public int getNeighbours(SearchState currentState, SearchState[] neighbours) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public int getMoveCost(SearchState start, SearchState goal) {
        return getMoveCost(start.getStateId(), goal.getStateId());
    }

    /**
     * @param startStateId state id of the start region center
     * @param goalStateId  state id of the goal region center
     * @return returns the move cost between neighbouring regions (= octile distance)
     */
    @Override
    public int getMoveCost(int startStateId, int goalStateId) {
        return abstractedMap.getOctileDistance(startStateId, goalStateId);
    }

    @Override
    public int getOctileDistance(int startStateId, int goalStateId) {
        return abstractedMap.getOctileDistance(startStateId, goalStateId);
    }

    @Override
    public int getMaxSize() {
        return abstractedMap.getNumRegions();
    }

    @Override
    public void getNeighbourIds(int regionRepStateId, List<Integer> neighbourIds) {
        Region region = abstractedMap.getRegionFromRepresentative(regionRepStateId);
        neighbourIds.addAll(region.getNeighborIds());
    }

    @Override
    public int getNeighbourIds(int currentId, int[] neighbourIds) {
        throw new RuntimeException("Not implemented yet");
    }

    public List<Integer> getNeighbourIdsFromRegionId(int regionId) {
        Region region = abstractedMap.getRegionById(regionId);
        return new ArrayList<>(region.getNeighborIds());
    }

    public boolean areInNeighbouringRegionsOrTheSameRegion(int sid1, int sid2) {
        int r1 = abstractedMap.getRegionIdFromMap(sid1);
        int r2 = abstractedMap.getRegionIdFromMap(sid2);

        return r1 == r2 || getNeighbourIdsFromRegionId(r1).contains(r2);
    }
}
