package search.algorithms;

import map.AbstractedMap;
import map.GameMap;
import map.Region;
import org.junit.jupiter.api.Test;
import search.MapSearchProblem;
import search.SearchState;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DBAStarTest {
    @Test
    void handlesEliminationCaseProperly() throws Exception {
        int wallId = 0;

        GameMap gameMap = new GameMap(new int[][]{{32}});
        DBAStar dbaStar = new DBAStar(gameMap, 16, false);

        Map<Integer, Region> regionMap = new TreeMap<>();

        assertEquals(1, dbaStar.getAbstractedMap().getNumRegions());

        regionMap.put(50, new Region(AbstractedMap.START_NUM, 0, 1, new HashSet<>()));
        assertEquals(1, regionMap.size());
        assertEquals(regionMap.toString(), dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString());

        dbaStar.addWall(wallId);

        // The state on the map should be a wall
        assertEquals(42, dbaStar.getAbstractedMap().getRegionIdFromMap(wallId));
        // The first of the free region ids should be the region id of the region the wall eliminated
        assertEquals(AbstractedMap.START_NUM, dbaStar.getAbstractedMap().getFreeRegionIds().peek());
        // The number of regions should be 0
        assertEquals(0, dbaStar.getAbstractedMap().getNumRegions());

//        regionMap.put(50, null);
        regionMap.remove(50);
        assertEquals(0, regionMap.size());
        assertEquals(regionMap, dbaStar.getAbstractedMap().getRegionIdToRegionMap());

        dbaStar.removeWall(wallId);

        assertEquals(50, dbaStar.getAbstractedMap().getRegionIdFromMap(wallId));
        assertEquals(1, dbaStar.getAbstractedMap().getNumRegions());

        regionMap.put(50, new Region(AbstractedMap.START_NUM, 0, 1, new HashSet<>()));
        assertEquals(1, regionMap.size());
        assertEquals(regionMap.toString(), dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString());
    }

    @Test
    void handlesPartitionCaseProperly() throws Exception {
        int wallId = 4;

        GameMap gameMap = new GameMap(new int[][]{{32, 42, 32}, {32, 32, 32}, {32, 42, 32}});
        DBAStar dbaStar = new DBAStar(gameMap, 3, false);

        Map<Integer, Region> regionMap = new TreeMap<>();

        assertEquals(1, dbaStar.getAbstractedMap().getNumRegions());

        regionMap.put(50, new Region(50, 4, 7, new HashSet<>()));
        assertEquals(1, regionMap.size());
        assertEquals(regionMap.toString(), dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString());

        dbaStar.addWall(wallId);

        assertEquals(42, dbaStar.getAbstractedMap().getRegionIdFromMap(wallId));

        assertEquals(2, dbaStar.getAbstractedMap().getNumRegions());

        regionMap.put(50, new Region(50, 3, 3, new HashSet<>()));
        regionMap.put(51, new Region(51, 5, 3, new HashSet<>()));
        assertEquals(2, regionMap.size());
        assertEquals(regionMap.toString(), dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString());
    }

    @Test
    void handlesPartitionCaseProperlyRealMap() throws Exception {
        int wallId = 14325;

        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        DBAStar dbaStar = new DBAStar(gameMap, 16, false);

        assertEquals(85, dbaStar.getAbstractedMap().getNumRegions());

        int regionId = 119;
        Region partitionedRegion = dbaStar.getAbstractedMap().getRegionIdToRegionMap().get(regionId);

        assertEquals(5, partitionedRegion.getNeighborIds().size());
        assertEquals(Set.of(118, 108, 120, 128, 130), partitionedRegion.getNeighborIds());
        assertEquals(15213, partitionedRegion.getRegionRepresentative());

        dbaStar.addWall(wallId);

        assertEquals(86, dbaStar.getAbstractedMap().getNumRegions());

        Region leftPartition = dbaStar.getAbstractedMap().getRegionIdToRegionMap().get(119);
        Region rightPartition = dbaStar.getAbstractedMap().getRegionIdToRegionMap().get(135);

        assertEquals(4, leftPartition.getNeighborIds().size());
        assertEquals(Set.of(118, 108, 128, 130), leftPartition.getNeighborIds());

        assertEquals(2, rightPartition.getNeighborIds().size());
        assertEquals(Set.of(108, 120), rightPartition.getNeighborIds());

        assertEquals(15656, leftPartition.getRegionRepresentative());
        assertEquals(14775, rightPartition.getRegionRepresentative());
    }

    @Test
    void handlesMergeCaseProperlyRealMap() throws Exception {
        int wallId = 14325;
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        gameMap.setStateValue(wallId, GameMap.WALL_CHAR);

        DBAStar dbaStar = new DBAStar(gameMap, 16, false);

        assertEquals(86, dbaStar.getAbstractedMap().getNumRegions());

        Region leftPartition = dbaStar.getAbstractedMap().getRegionIdToRegionMap().get(119);
        Region rightPartition = dbaStar.getAbstractedMap().getRegionIdToRegionMap().get(120);

        assertEquals(4, leftPartition.getNeighborIds().size());

        assertEquals(2, rightPartition.getNeighborIds().size());

        assertEquals(15656, leftPartition.getRegionRepresentative());
        assertEquals(14775, rightPartition.getRegionRepresentative());

        dbaStar.removeWall(wallId);

        Region mergedRegion = dbaStar.getAbstractedMap().getRegionIdToRegionMap().get(119);

        assertEquals(5, mergedRegion.getNeighborIds().size());
        assertEquals(Set.of(108, 118, 121, 129, 131), mergedRegion.getNeighborIds());
        assertEquals(Arrays.toString(new int[]{108 - AbstractedMap.START_NUM, 118 - AbstractedMap.START_NUM, 121 - AbstractedMap.START_NUM, 129 - AbstractedMap.START_NUM, 131 - AbstractedMap.START_NUM}), Arrays.toString(dbaStar.getSubgoalDB().getNeighboursForRegion(119)));

        assertEquals(15213, mergedRegion.getRegionRepresentative());
    }

    @Test
    void handlesMergeCaseProperly() throws Exception {
        int wallId = 4;

        GameMap gameMap = new GameMap(new int[][]{{32, 42, 32}, {32, 42, 32}, {32, 42, 32}});
        DBAStar dbaStar = new DBAStar(gameMap, 3, false);

        Map<Integer, Region> regionMap = new TreeMap<>();
        regionMap.put(50, new Region(50, 3, 3, new HashSet<>()));
        regionMap.put(51, new Region(51, 5, 3, new HashSet<>()));
        assertEquals(2, regionMap.size());
        assertEquals(regionMap.toString(), dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString());

        dbaStar.removeWall(wallId);

        regionMap.put(50, new Region(50, 4, 7, new HashSet<>()));
//        regionMap.put(51, null);
        // TODO: Decide how to handle deletion
        regionMap.remove(51);
        assertEquals(regionMap.toString(), dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString());
    }

    @Test
    void handlesWallPlacementAndRemovalProperly() throws Exception {
        int wallId = 4;

        GameMap gameMap = new GameMap(new int[][]{{32, 32, 32}, {32, 32, 32}, {32, 32, 32}});
        DBAStar dbaStar = new DBAStar(gameMap, 16, false);
        dbaStar.addWall(wallId);

        assertEquals(42, dbaStar.getAbstractedMap().getRegionIdFromMap(wallId));

        // Check that there is one region with region rep 0 and size 1

        dbaStar.removeWall(wallId);

        assertEquals(50, dbaStar.getAbstractedMap().getRegionIdFromMap(wallId));
    }

    @Test
    void shouldAllBeShortestPathChanges() throws Exception {
        int wallId1 = 13881;
        int wallId2 = 19757;

        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        DBAStar dbaStar = new DBAStar(gameMap, 16, false);

        assertEquals(85, dbaStar.getAbstractedMap().getNumRegions());

        int regionId1 = dbaStar.getAbstractedMap().getRegionIdFromMap(wallId1);
        Region region1 = dbaStar.getAbstractedMap().getRegionIdToRegionMap().get(regionId1);

        assertEquals(Set.of(119, 94, 95, 109), region1.getNeighborIds());

        dbaStar.addWall(wallId1);
        dbaStar.removeWall(wallId1);

        assertEquals(Set.of(119, 94, 95, 109), region1.getNeighborIds());

        assertEquals(85, dbaStar.getAbstractedMap().getNumRegions());

        int regionId2 = dbaStar.getAbstractedMap().getRegionIdFromMap(wallId2);
        Region region2 = dbaStar.getAbstractedMap().getRegionIdToRegionMap().get(regionId2);

        assertEquals(Set.of(125, 127, 133), region2.getNeighborIds());

        dbaStar.addWall(wallId2);
        dbaStar.removeWall(wallId2);

        assertEquals(Set.of(125, 127, 133), region2.getNeighborIds());
    }

    @Test
    void shouldMergeProperly() throws Exception {
        int wallId = 19040;

        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        DBAStar dbaStar = new DBAStar(gameMap, 16, false);

        int regionId = dbaStar.getAbstractedMap().getRegionIdFromMap(wallId);
        Region region = dbaStar.getAbstractedMap().getRegionIdToRegionMap().get(regionId);

        assertEquals(Set.of(129, 133, 127), region.getNeighborIds());

        dbaStar.addWall(wallId);

        assertEquals(Set.of(129, 133), region.getNeighborIds());

        dbaStar.removeWall(wallId);

        assertEquals(Set.of(129, 133, 127), dbaStar.getAbstractedMap().getRegionIdToRegionMap().get(regionId).getNeighborIds());
    }

    @Test
    void shouldHaveSameNeighboursBeforeAndAfter012() throws Exception {
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        DBAStar dbaStar = new DBAStar(gameMap, 16, false);

        String regionMapStringBefore = dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString();
        String regionMapStringAfter;

        for (SearchState openState : MapSearchProblem.getOpenStateList(gameMap)) {
            dbaStar.addWall(openState.getStateId());
            dbaStar.removeWall(openState.getStateId());

            regionMapStringAfter = dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString();

            assertEquals(regionMapStringBefore, regionMapStringAfter);
        }
    }

    @Test
    void shouldHaveSameNeighboursBeforeAndAfter516() throws Exception {
        GameMap gameMap = new GameMap("src/test/resources/maps/516.map");
        DBAStar dbaStar = new DBAStar(gameMap, 16, false);

        String regionMapStringBefore = dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString();
        String regionMapStringAfter;

        for (SearchState openState : MapSearchProblem.getOpenStateList(gameMap)) {
            dbaStar.addWall(openState.getStateId());
            dbaStar.removeWall(openState.getStateId());

            regionMapStringAfter = dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString();

            assertEquals(regionMapStringBefore, regionMapStringAfter);
        }
    }

    @Test
    void shouldHaveSameNeighboursBeforeAndAfter701() throws Exception {
        GameMap gameMap = new GameMap("src/test/resources/maps/701.map");
        DBAStar dbaStar = new DBAStar(gameMap, 16, false);

        String regionMapStringBefore = dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString();
        String regionMapStringAfter;

        for (SearchState openState : MapSearchProblem.getOpenStateList(gameMap)) {
            dbaStar.addWall(openState.getStateId());
            dbaStar.removeWall(openState.getStateId());

            regionMapStringAfter = dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString();

            assertEquals(regionMapStringBefore, regionMapStringAfter);
        }
    }

    @Test
    void shouldHaveSameNeighboursBeforeAndAfter603() throws Exception {
        GameMap gameMap = new GameMap("src/test/resources/maps/603.map");
        DBAStar dbaStar = new DBAStar(gameMap, 16, false);

        String regionMapStringBefore = dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString();
        String regionMapStringAfter;

        for (SearchState openState : MapSearchProblem.getOpenStateList(gameMap)) {
            dbaStar.addWall(openState.getStateId());
            dbaStar.removeWall(openState.getStateId());

            regionMapStringAfter = dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString();

            assertEquals(regionMapStringBefore, regionMapStringAfter);
        }
    }

    @Test
    void shouldHaveSameNeighboursBeforeAndAfterDAOMaps() throws Exception {
        String[] mapStringsDA = {"hrt000d", "orz100d", "orz103d", "orz300d", "orz700d", "orz702d", "orz900d", "ost000a", "ost000t", "ost100d"};

        for (String mapString : mapStringsDA) {
            System.out.println(mapString);
            GameMap gameMap = new GameMap("src/test/resources/maps/" + mapString + ".map");
            DBAStar dbaStar = new DBAStar(gameMap, 32, false);

            String regionMapStringBefore = dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString();
            String regionMapStringAfter;

            for (SearchState openState : MapSearchProblem.getOpenStateList(gameMap)) {
                dbaStar.addWall(openState.getStateId());
                dbaStar.removeWall(openState.getStateId());

                regionMapStringAfter = dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString();

                assertEquals(regionMapStringBefore, regionMapStringAfter);
            }
        }
    }

    @Test
    void shouldHaveSameNeighboursBeforeAndAfterWallAt187328_orz300d() throws Exception {
        GameMap gameMap = new GameMap("src/test/resources/maps/orz300d.map");
        DBAStar dbaStar = new DBAStar(gameMap, 32, false);

        String regionMapStringBefore = dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString();
        String regionMapStringAfter;

        System.out.println(187328);
        dbaStar.addWall(187328);
        dbaStar.removeWall(187328);

        regionMapStringAfter = dbaStar.getAbstractedMap().getRegionIdToRegionMap().toString();

        assertEquals(regionMapStringBefore, regionMapStringAfter);
    }
}
