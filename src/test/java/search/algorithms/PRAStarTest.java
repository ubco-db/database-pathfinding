package search.algorithms;

import map.GameMap;
import map.Region;
import org.junit.jupiter.api.Test;
import search.SearchState;
import stats.SearchStats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PRAStarTest {
    @Test
    void partitionCaseWorksProperly() {
        int[][] states = {{32, 42, 32}, {32, 32, 32}, {32, 42, 32}};
        GameMap map = new GameMap(states);

        PRAStar praStar = new PRAStar(map, 16);
        int[] regionReps = praStar.getAbstractedMap().getRegionReps();
        int count;

        // Before the partition, there should be one region with 7 states
        assertEquals(1, praStar.getAbstractedMap().getNumRegions());
        // region id to region mapping should have one element
        assertEquals(1, praStar.getAbstractedMap().getRegionIdToRegionMap().size());
        for (Region region : praStar.getAbstractedMap().getRegionIdToRegionMap().values()) {
            assertEquals(7, region.getNumStates());
        }
        // Region rep array should have one element
        count = 0;
        for (int i = 0; i < praStar.getAbstractedMap().getRegionReps().length; i++) {
            if (regionReps[i] != -1) {
                count++;
            }
        }
        assertEquals(1, count);

        // Add the wall
        praStar.addWall(4);

        // After the partition there should be two regions with 3 states each
        assertEquals(2, praStar.getAbstractedMap().getNumRegions());
        // region id to region mapping should have two elements
        assertEquals(2, praStar.getAbstractedMap().getRegionIdToRegionMap().size());
        for (Region region : praStar.getAbstractedMap().getRegionIdToRegionMap().values()) {
            assertEquals(3, region.getNumStates());
        }
        // Region rep array should have two elements
        count = 0;
        for (int i = 0; i < praStar.getAbstractedMap().getRegionReps().length; i++) {
            if (regionReps[i] != -1) {
                count++;
            }
        }
        assertEquals(2, count);
    }

    @Test
    void mergeCaseWorksProperly() {
        int[][] states = {{32, 42, 32}, {32, 42, 32}, {32, 42, 32}};
        GameMap map = new GameMap(states);

        PRAStar praStar = new PRAStar(map, 16);
        int[] regionReps = praStar.getAbstractedMap().getRegionReps();
        int count;

        // Before the merge, there should be two regions with 3 states each
        assertEquals(2, praStar.getAbstractedMap().getNumRegions());
        // region id to region mapping should have two elements
        assertEquals(2, praStar.getAbstractedMap().getRegionIdToRegionMap().size());
        for (Region region : praStar.getAbstractedMap().getRegionIdToRegionMap().values()) {
            assertEquals(3, region.getNumStates());
        }
        // Region rep array should have two elements
        count = 0;
        for (int i = 0; i < praStar.getAbstractedMap().getRegionReps().length; i++) {
            if (regionReps[i] != -1) {
                count++;
            }
        }
        assertEquals(2, count);

        // Remove the wall
        praStar.removeWall(4);

        // After the merge, there should be one region with 7 states
        assertEquals(1, praStar.getAbstractedMap().getNumRegions());
        // region id to region mapping should have one element
        assertEquals(1, praStar.getAbstractedMap().getRegionIdToRegionMap().size());
        for (Region region : praStar.getAbstractedMap().getRegionIdToRegionMap().values()) {
            assertEquals(7, region.getNumStates());
        }
        // Region rep array should have one element
        count = 0;
        for (int i = 0; i < praStar.getAbstractedMap().getRegionReps().length; i++) {
            if (regionReps[i] != -1) {
                count++;
            }
        }
        assertEquals(1, count);
    }

    @Test
    void addingAndRemovingWallWorksProperlyOnGameMap() {
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        PRAStar praStar = new PRAStar(gameMap, 16);
        int[] regionReps = praStar.getAbstractedMap().getRegionReps();
        int count;

        // There should be 85 regions on the base map
        assertEquals(85, praStar.getAbstractedMap().getNumRegions());
        praStar.addWall(4651);
        // Placing a wall on this state should eliminate a region
        assertEquals(84, praStar.getAbstractedMap().getNumRegions());
        praStar.removeWall(4651);
        // Addition and removal should cancel out, leaving us with 85 regions
        assertEquals(85, praStar.getAbstractedMap().getNumRegions());
    }

    @Test
    void addingAndRemovingWallWorksProperly() {
        int[][] states = {{32, 32, 32}, {32, 42, 42}, {32, 42, 32}};
        GameMap gameMap = new GameMap(states);
        PRAStar praStar = new PRAStar(gameMap, 3);
        int[] regionReps = praStar.getAbstractedMap().getRegionReps();
        int count;

        // There should be 2 regions on the base map
        assertEquals(2, praStar.getAbstractedMap().getNumRegions());
        praStar.addWall(8);
        // Placing a wall on this state should eliminate a region
        assertEquals(1, praStar.getAbstractedMap().getNumRegions());
        praStar.removeWall(8);
        // Addition and removal should cancel out, leaving us with 2 regions
        assertEquals(2, praStar.getAbstractedMap().getNumRegions());
    }

    @Test
    void partitionIntoMultipleRegionsWorksProperly() {
        int[][] states = {{42, 32, 42}, {32, 32, 32}, {32, 32, 42}};
        int count;

        GameMap gameMap = new GameMap(states);

        PRAStar praStar = new PRAStar(gameMap, 16);
        int[] regionReps = praStar.getAbstractedMap().getRegionReps();

        // Before the partition, there should be one region with six states
        assertEquals(1, praStar.getAbstractedMap().getNumRegions());
        // region id to region mapping should have one element
        assertEquals(1, praStar.getAbstractedMap().getRegionIdToRegionMap().size());
        for (Region region : praStar.getAbstractedMap().getRegionIdToRegionMap().values()) {
            assertEquals(6, region.getNumStates());
        }
        // Region rep array should have one element
        count = 0;
        for (int i = 0; i < praStar.getAbstractedMap().getRegionReps().length; i++) {
            if (regionReps[i] != -1) {
                count++;
            }
        }
        assertEquals(1, count);

        // Add the wall
        praStar.addWall(4);

        // After the partition, there should be three regions
        assertEquals(3, praStar.getAbstractedMap().getNumRegions());
        // region id to region mapping should have three elements
        assertEquals(3, praStar.getAbstractedMap().getRegionIdToRegionMap().size());
        List<Integer> numStates = new ArrayList<>(3);
        for (Region region : praStar.getAbstractedMap().getRegionIdToRegionMap().values()) {
            numStates.add(region.getNumStates());
        }
        assertTrue(numStates.containsAll(Arrays.asList(1, 1, 3)));
        // Region rep array should have three elements
        count = 0;
        for (int i = 0; i < praStar.getAbstractedMap().getRegionReps().length; i++) {
            if (regionReps[i] != 0) {
                count++;
            }
        }
        assertEquals(3, count);
    }

    @Test
    void mergeOfMultipleRegionsWorksProperly() {
        int[][] states = {{42, 32, 42}, {32, 42, 32}, {32, 32, 42}};
        int count;

        GameMap gameMap = new GameMap(states);
        PRAStar praStar = new PRAStar(gameMap, 16);
        int[] regionReps = praStar.getAbstractedMap().getRegionReps();

        // Before the merge, there should be three regions
        assertEquals(3, praStar.getAbstractedMap().getNumRegions());
        // region id to region mapping should have three elements
        assertEquals(3, praStar.getAbstractedMap().getRegionIdToRegionMap().size());
        List<Integer> numStates = new ArrayList<>(3);
        for (Region region : praStar.getAbstractedMap().getRegionIdToRegionMap().values()) {
            numStates.add(region.getNumStates());
        }
        assertTrue(numStates.containsAll(Arrays.asList(1, 1, 3)));
        // Region rep array should have three elements
        count = 0;
        for (int i = 0; i < praStar.getAbstractedMap().getRegionReps().length; i++) {
            if (regionReps[i] != -1) {
                count++;
            }
        }
        assertEquals(3, count);

        // Remove the wall
        praStar.removeWall(4);

        // After the merge, there should be one region with six states
        assertEquals(1, praStar.getAbstractedMap().getNumRegions());
        // region id to region mapping should have one element
        assertEquals(1, praStar.getAbstractedMap().getRegionIdToRegionMap().size());
        for (Region region : praStar.getAbstractedMap().getRegionIdToRegionMap().values()) {
            assertEquals(6, region.getNumStates());
        }
        // Region rep array should have one element
        count = 0;
        for (int i = 0; i < praStar.getAbstractedMap().getRegionReps().length; i++) {
            if (regionReps[i] != -1) {
                count++;
            }
        }
        assertEquals(1, count);
    }

    @Test
    void mapContainsSameNumberOfRegionsBeforeAndAfterAddingAndRemovingTheSameWall() {
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        PRAStar praStar = new PRAStar(gameMap, 16);

        // Get all open state ids
        List<Integer> goalIds = new ArrayList<>(gameMap.getNumOpenStates());
        for (int r = 0; r < gameMap.getNumRows(); r++) {
            for (int c = 0; c < gameMap.getNumCols(); c++) {
                if (!gameMap.isWall(r, c)) {
                    goalIds.add(gameMap.getStateId(r, c));
                }
            }
        }

        // Check that there are 85 regions on the map
        assertEquals(85, praStar.getAbstractedMap().getNumRegions());

        // Place a wall and immediately remove it
        for (int goalId : goalIds) {
            praStar.addWall(goalId);
            praStar.removeWall(goalId);
        }

        // Check that there are 85 regions on the map
        assertEquals(85, praStar.getAbstractedMap().getNumRegions());
    }

    //    @Test
    void regionIdToRegionMapStaysTheSame() {
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        PRAStar praStar = new PRAStar(gameMap, 16);

        // Get all open state ids
        List<Integer> goalIds = new ArrayList<>(gameMap.getNumOpenStates());
        for (int r = 0; r < gameMap.getNumRows(); r++) {
            for (int c = 0; c < gameMap.getNumCols(); c++) {
                if (!gameMap.isWall(r, c)) {
                    goalIds.add(gameMap.getStateId(r, c));
                }
            }
        }

        // Get toString of map, contains 50=Region{regionId=50, regionRepresentative=4486, numStates=5, neighborIds=[51, 55, 58]}
        String before = praStar.getAbstractedMap().getRegionIdToRegionMap().toString();
        for (int goalId : goalIds) {
            praStar.addWall(goalId);
            praStar.removeWall(goalId);
            String after = praStar.getAbstractedMap().getRegionIdToRegionMap().toString();
            assertEquals(before, after);
        }
    }

    void pathsStayTheSame() {
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        PRAStar praStar = new PRAStar(gameMap, 16);

        // Get all open state ids
        List<SearchState> goalStates = new ArrayList<>(gameMap.getNumOpenStates());
        for (int r = 0; r < gameMap.getNumRows(); r++) {
            for (int c = 0; c < gameMap.getNumCols(); c++) {
                if (!gameMap.isWall(r, c)) {
                    goalStates.add(new SearchState(gameMap.getStateId(r, c)));
                }
            }
        }

        List<SearchState> path1, path2;
        for (SearchState start : goalStates) {
            for (SearchState goal : goalStates) {
                path1 = praStar.findPath(start, goal, new SearchStats());
                praStar.addWall(goal.getStateId());
                praStar.removeWall(goal.getStateId());
                path2 = praStar.findPath(start, goal, new SearchStats());
                assertEquals(path1, path2);
            }
        }
    }
}
