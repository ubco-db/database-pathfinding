package search;

import map.AbstractedMap;
import map.GameMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionSearchProblemTest {
    @Test
    void bothFindRegionRepresentativeMethodsShouldLeadToSameResult() {
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        AbstractedMap abstractedMap = new AbstractedMap(gameMap, 16);
        RegionSearchProblem regionSearchProblem = new RegionSearchProblem(abstractedMap);

        SearchState searchState = new SearchState(14002);
        assertEquals(regionSearchProblem.findRegionRepresentativeFromMap(searchState), regionSearchProblem.findRegionRepresentative(searchState));
    }
}
