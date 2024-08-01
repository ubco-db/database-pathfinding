package search.algorithms;

import map.GameMap;
import org.junit.jupiter.api.Test;
import search.MapSearchProblem;
import search.SearchState;
import search.SearchUtil;
import stats.SearchStats;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PRAStarWithCachingAndHCCompressionTest {
    // TODO: This is not a good test, since asserting the paths are not null does not tell us much
    // Have not been able to think of a good way to improve it since paths found using HC will differ
    // from those found using A*
    @Test
    void canFindPathFrom10219To13905UsingCachedSubgoals() {
        SearchState start = new SearchState(10219);
        SearchState goal = new SearchState(13905);

        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        PRAStarWithCachingAndHCCompression praStarWithCachingAndHCCompression = new PRAStarWithCachingAndHCCompression(gameMap, 16);

        SearchStats before = new SearchStats();
        List<SearchState> pathBefore = praStarWithCachingAndHCCompression.findPath(start, goal, before);

        praStarWithCachingAndHCCompression.printCache();

        SearchStats after = new SearchStats();
        List<SearchState> pathAfter = praStarWithCachingAndHCCompression.findPath(start, goal, after);

        assertNotNull(pathBefore);

        assertTrue(SearchUtil.isContinuousPath(pathBefore, mapSearchProblem));

        assertNotNull(pathAfter);

        assertTrue(SearchUtil.isContinuousPath(pathAfter, mapSearchProblem));
    }

    @Test
    void canFindPathFrom17101To12824UsingCachedSubgoals() {
        SearchState start = new SearchState(17101);
        SearchState goal = new SearchState(12824);

        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        PRAStarWithCachingAndHCCompression praStarWithCachingAndHCCompression = new PRAStarWithCachingAndHCCompression(gameMap, 16);

        SearchStats before = new SearchStats();
        List<SearchState> pathBefore = praStarWithCachingAndHCCompression.findPath(start, goal, before);

        praStarWithCachingAndHCCompression.printCache();

        SearchStats after = new SearchStats();
        List<SearchState> pathAfter = praStarWithCachingAndHCCompression.findPath(start, goal, after);

        assertEquals(pathBefore, pathAfter);
        assertTrue(SearchUtil.isContinuousPath(pathBefore, mapSearchProblem));
        assertTrue(SearchUtil.isContinuousPath(pathAfter, mapSearchProblem));
    }

    @Test
    void canFindPathFrom8195To10861UsingCachedSubgoals() {
        SearchState start = new SearchState(8195);
        SearchState goal = new SearchState(10861);

        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        PRAStarWithCachingAndHCCompression praStarWithCachingAndHCCompression = new PRAStarWithCachingAndHCCompression(gameMap, 16);

        SearchStats before = new SearchStats();
        List<SearchState> pathBefore = praStarWithCachingAndHCCompression.findPath(start, goal, before);

        praStarWithCachingAndHCCompression.printCache();

        SearchStats after = new SearchStats();
        List<SearchState> pathAfter = praStarWithCachingAndHCCompression.findPath(start, goal, after);

        assertTrue(SearchUtil.isContinuousPath(pathBefore, mapSearchProblem));
        assertEquals(pathBefore, pathAfter);
        assertTrue(SearchUtil.isContinuousPath(pathAfter, mapSearchProblem));
    }

    // TODO: This is not a good test, since asserting the paths are not null does not tell us much
    // Have not been able to think of a good way to improve it since paths found using HC will differ
    // from those found using A*
    @Test
    void canFindPathFrom12794To9667UsingCachedSubgoals() {
        SearchState start = new SearchState(12794);
        SearchState goal = new SearchState(9667);

        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);

        PRAStarWithCachingAndHCCompression praStarWithCachingAndHCCompression = new PRAStarWithCachingAndHCCompression(gameMap, 16);

        SearchStats before = new SearchStats();
        List<SearchState> pathBefore = praStarWithCachingAndHCCompression.findPath(start, goal, before);

        SearchStats after = new SearchStats();
        List<SearchState> pathAfter = praStarWithCachingAndHCCompression.findPath(start, goal, after);

        assertNotNull(pathBefore);
        assertTrue(SearchUtil.isContinuousPath(pathBefore, mapSearchProblem));
        assertNotNull(pathAfter);
        assertTrue(SearchUtil.isContinuousPath(pathAfter, mapSearchProblem));
    }

    @Test
    void pathGetsCached() {
        SearchState start = new SearchState(10219);
        SearchState goal = new SearchState(13905);

        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        PRAStarWithCachingAndHCCompression praStarWithCachingAndHCCompression = new PRAStarWithCachingAndHCCompression(gameMap, 16);

        SearchStats before = new SearchStats();
        praStarWithCachingAndHCCompression.findPath(start, goal, before);
        System.out.println("States expanded before caching: " + before.getNumStatesExpanded());
        System.out.println("Time to find path after caching: " + before.getTimeToFindPathOnline());

        Map<String, int[]> cache = praStarWithCachingAndHCCompression.getCache();

        assertEquals(8, cache.size());
        assertEquals(Set.of("10531 10103", "10103 8195", "8195 10861", "10861 13244", "13244 15182", "15182 15347", "15347 15213", "15213 13442"), cache.keySet());

        SearchStats after = new SearchStats();
        praStarWithCachingAndHCCompression.findPath(start, goal, after);
        System.out.println("States expanded after caching: " + after.getNumStatesExpanded());
        System.out.println("Time to find path after caching: " + after.getTimeToFindPathOnline());

        System.out.printf("\n%d fewer states expanded (%.2f%%)\n", before.getNumStatesExpanded() - after.getNumStatesExpanded(), (((double) before.getNumStatesExpanded() - after.getNumStatesExpanded()) / before.getNumStatesExpanded()) * 100);
        System.out.printf("Speedup: %.2f%%\n", (((double) before.getTimeToFindPathOnline() - after.getTimeToFindPathOnline()) / before.getTimeToFindPathOnline()) * 100);
    }

    @Test
    void cacheGetsInvalidatedAfterMapChange() {
        SearchState start = new SearchState(10219);
        SearchState goal = new SearchState(13905);

        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        PRAStarWithCachingAndHCCompression praStarWithCachingAndHCCompression = new PRAStarWithCachingAndHCCompression(gameMap, 16);

        SearchStats before = new SearchStats();
        praStarWithCachingAndHCCompression.findPath(start, goal, before);
        System.out.println("States expanded before caching: " + before.getNumStatesExpanded());
        System.out.println("Time to find path before caching: " + before.getTimeToFindPathOnline());

        Map<String, int[]> cache = praStarWithCachingAndHCCompression.getCache();

        assertEquals(8, cache.size());
        assertEquals(Set.of("10531 10103", "10103 8195", "8195 10861", "10861 13244", "13244 15182", "15182 15347", "15347 15213", "15213 13442"), cache.keySet());

        praStarWithCachingAndHCCompression.addWall(13244);

        assertEquals(6, cache.size());
        assertEquals(Set.of("10531 10103", "10103 8195", "8195 10861", "15182 15347", "15347 15213", "15213 13442"), cache.keySet());

        SearchStats after = new SearchStats();
        praStarWithCachingAndHCCompression.findPath(start, goal, after);
        System.out.println("States expanded after caching and invalidating part of cache: " + after.getNumStatesExpanded());
        System.out.println("Time to find path after caching and invalidating part of cache: " + after.getTimeToFindPathOnline());

        System.out.printf("\n%d fewer states expanded (%.2f%%)\n", before.getNumStatesExpanded() - after.getNumStatesExpanded(), (((double) before.getNumStatesExpanded() - after.getNumStatesExpanded()) / before.getNumStatesExpanded()) * 100);
        System.out.printf("Speedup: %.2f%%\n", (((double) before.getTimeToFindPathOnline() - after.getTimeToFindPathOnline()) / before.getTimeToFindPathOnline()) * 100);
    }

    // TODO: Is path length being the same actually guaranteed between A* and Hillclimbing
    // I believe it is, but I am not 100% sure
    @Test
    void pathLengthStaysTheSame() {
        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        MapSearchProblem mapSearchProblem = new MapSearchProblem(gameMap);
        PRAStarWithCachingAndHCCompression praStarWithCachingAndHCCompression = new PRAStarWithCachingAndHCCompression(gameMap, 16);

        int[] regionReps = praStarWithCachingAndHCCompression.getAbstractedMap().getRegionReps();
        int numReps = praStarWithCachingAndHCCompression.getAbstractedMap().getNumRegions();

        SearchStats before = new SearchStats();
        SearchStats after = new SearchStats();

        List<SearchState> pathBefore, pathAfter;

        for (int i = 0; i < numReps; i++) {
            SearchState start = new SearchState(regionReps[i]);
            for (int j = 0; j < numReps; j++) {
                if (i != j) {
                    SearchState goal = new SearchState(regionReps[j]);
                    pathBefore = praStarWithCachingAndHCCompression.findPath(start, goal, before);

                    int wallId = regionReps[j];

                    praStarWithCachingAndHCCompression.addWall(wallId);
                    praStarWithCachingAndHCCompression.removeWall(wallId);

                    pathAfter = praStarWithCachingAndHCCompression.findPath(start, goal, after);
                    assertTrue(SearchUtil.isContinuousPath(pathBefore, mapSearchProblem));
                    assertTrue(SearchUtil.isContinuousPath(pathAfter, mapSearchProblem));
                    assertEquals(pathBefore.size(), pathAfter.size());
                }
            }
        }
    }
}
