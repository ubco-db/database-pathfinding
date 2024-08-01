package search.algorithms;

import map.GameMap;
import org.junit.jupiter.api.Test;
import search.SearchState;
import stats.SearchStats;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PRAStarWithCachingTest {
    @Test
    void pathGetsCached() {
        SearchState start = new SearchState(10219);
        SearchState goal = new SearchState(13905);

        GameMap gameMap = new GameMap("src/test/resources/maps/012.map");
        PRAStarWithCaching praStarWithCaching = new PRAStarWithCaching(gameMap, 16);

        SearchStats before = new SearchStats();
        praStarWithCaching.findPath(start, goal, before);
        System.out.println("States expanded before caching: " + before.getNumStatesExpanded());
        System.out.println("Time to find path after caching: " + before.getTimeToFindPathOnline());

        Map<String, List<SearchState>> cache = praStarWithCaching.getCache();

        assertEquals(8, cache.size());
        assertEquals(Set.of("10531 10103", "10103 8195", "8195 10861", "10861 13244", "13244 15182", "15182 15347", "15347 15213", "15213 13442"), cache.keySet());

        SearchStats after = new SearchStats();
        praStarWithCaching.findPath(start, goal, after);
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
        PRAStarWithCaching praStarWithCaching = new PRAStarWithCaching(gameMap, 16);

        SearchStats before = new SearchStats();
        praStarWithCaching.findPath(start, goal, before);
        System.out.println("States expanded before caching: " + before.getNumStatesExpanded());
        System.out.println("Time to find path before caching: " + before.getTimeToFindPathOnline());

        Map<String, List<SearchState>> cache = praStarWithCaching.getCache();

        assertEquals(8, cache.size());
        assertEquals(Set.of("10531 10103", "10103 8195", "8195 10861", "10861 13244", "13244 15182", "15182 15347", "15347 15213", "15213 13442"), cache.keySet());

        praStarWithCaching.addWall(13244);

        assertEquals(6, cache.size());
        assertEquals(Set.of("10531 10103", "10103 8195", "8195 10861", "15182 15347", "15347 15213", "15213 13442"), cache.keySet());

        SearchStats after = new SearchStats();
        praStarWithCaching.findPath(start, goal, after);
        System.out.println("States expanded after caching and invalidating part of cache: " + after.getNumStatesExpanded());
        System.out.println("Time to find path after caching and invalidating part of cache: " + after.getTimeToFindPathOnline());

        System.out.printf("\n%d fewer states expanded (%.2f%%)\n", before.getNumStatesExpanded() - after.getNumStatesExpanded(), (((double) before.getNumStatesExpanded() - after.getNumStatesExpanded()) / before.getNumStatesExpanded()) * 100);
        System.out.printf("Speedup: %.2f%%\n", (((double) before.getTimeToFindPathOnline() - after.getTimeToFindPathOnline()) / before.getTimeToFindPathOnline()) * 100);
    }
}
