import map.AbstractedMap;
import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.MapSearchProblem;
import search.RegionSearchProblem;
import search.SearchState;
import search.SearchUtil;
import search.algorithms.*;
import stats.SearchStats;

import java.util.List;
import java.util.Random;

public class OnlineExperimentsOnBgAndDaRandomPaths {
    public static final Logger logger = LogManager.getLogger(OnlineExperimentsOnBgAndDaRandomPaths.class);

    private static final int NUM_PATHS = 10_000;

    public static void main(String[] args) throws Exception {
        String[] mapStringsDA = {"hrt000d", "orz100d", "orz103d", "orz300d", "orz700d", "orz702d", "orz900d", "ost000a", "ost000t", "ost100d"};
        String[] mapStringsBG = {"012", "516", "603", "701"};
        int[] gridSizes = {16, 32, 64, 128};

        String[] mapStrings = mapStringsDA;
        GameMap gameMap;

        final int COUNT = 3;
        logger.info("Number of goals: {}", NUM_PATHS);

        for (String mapString : mapStrings) {
            logger.info("{} map", mapString);
            logger.info("grid, DBA totalTime, DBA numStatesExpanded, DBA numStatesExpandedHC, DBA Path Cost, PRA-C totalTime, PRA-C numStatesExpanded, PRA-C numStatesExpandedHC, PRA-C Path Cost, PRA-P totalTime, PRA-P numStatesExpanded, PRA-P numStatesExpandedHC, PRA-P Path Cost, PRA totalTime, PRA numStatesExpanded, PRA numStatesExpandedHC,"
                    + " PRA Path Cost, A* totalTime, A* numStatesExpanded, A* numStatesExpandedHC, A* Path Cost");

            for (int gridSize : gridSizes) {
                gameMap = new GameMap("src/main/resources/maps/" + mapString + ".map");

                AbstractedMap abstractedMap = new AbstractedMap(gameMap, gridSize);
                RegionSearchProblem regionSearchProblem = new RegionSearchProblem(abstractedMap);

                List<SearchState> openStates = MapSearchProblem.getOpenStateList(gameMap);

                Random random = new Random();
                random.setSeed(56256902 + gridSize);

                SearchStats aStats = new SearchStats();
                SearchStats dbaStats = new SearchStats();
                SearchStats praPStats = new SearchStats();
                SearchStats praCStats = new SearchStats();
                SearchStats praStats = new SearchStats();

                for (int i = 0; i < COUNT; i++) {
                    // Find random starts and random goals
                    SearchState[] randomStarts = new SearchState[NUM_PATHS];
                    SearchState[] randomGoals = new SearchState[NUM_PATHS];

                    for (int j = 0; j < NUM_PATHS; j++) {
                        int r1 = random.nextInt(openStates.size());
                        int r2 = random.nextInt(openStates.size());

                        randomStarts[j] = openStates.get(r1);
                        randomGoals[j] = openStates.get(r2);

                        while (regionSearchProblem.areInNeighbouringRegionsOrTheSameRegion(randomStarts[j].getStateId(), randomGoals[j].getStateId())) {
                            r2 = random.nextInt(openStates.size());
                            randomGoals[j] = openStates.get(r2);
                        }
                    }

                    MapSearchProblem problem = new MapSearchProblem(new GameMap(gameMap));

                    dbaStats.addAll(timeAlgorithm(new DBAStar(new GameMap(gameMap), gridSize, false), randomGoals, randomStarts, problem));
                    praCStats.addAll(timeAlgorithm(new PRAStarWithCachingAndHCCompression(new GameMap(gameMap), gridSize), randomGoals, randomStarts, problem));
                    praPStats.addAll(timeAlgorithm(new PRAStarWithCaching(new GameMap(gameMap), gridSize), randomGoals, randomStarts, problem));
                    praStats.addAll(timeAlgorithm(new PRAStar(new GameMap(gameMap), gridSize), randomGoals, randomStarts, problem));
                    aStats.addAll(timeAlgorithm(new AStar(new MapSearchProblem(new GameMap(gameMap))), randomGoals, randomStarts, problem));
                }

                dbaStats.divideBy(COUNT);
                praCStats.divideBy(COUNT);
                praPStats.divideBy(COUNT);
                praStats.divideBy(COUNT);
                aStats.divideBy(COUNT);

                String dbaStatsString = String.format("%d, %d, %d, %d", dbaStats.getTotalTime(), dbaStats.getNumStatesExpanded(), dbaStats.getNumStatesExpandedHC(), dbaStats.getPathCost());
                String praPStatsString = String.format("%d, %d, %d, %d", praCStats.getTotalTime(), praCStats.getNumStatesExpanded(), praCStats.getNumStatesExpandedHC(), praCStats.getPathCost());
                String praCStatsString = String.format("%d, %d, %d, %d", praPStats.getTotalTime(), praPStats.getNumStatesExpanded(), praPStats.getNumStatesExpandedHC(), praPStats.getPathCost());
                String praStatsString = String.format("%d, %d, %d, %d", praStats.getTotalTime(), praStats.getNumStatesExpanded(), praStats.getNumStatesExpandedHC(), praStats.getPathCost());
                String aStatsString = String.format("%d, %d, %d, %d", aStats.getTotalTime(), aStats.getNumStatesExpanded(), aStats.getNumStatesExpandedHC(), aStats.getPathCost());

                logger.info("{}, {}, {}, {}, {}, {}", gridSize, dbaStatsString, praPStatsString, praCStatsString, praStatsString, aStatsString);
            }
        }
    }

    private static SearchStats timeAlgorithm(SearchAlgorithmWithStats searchAlgorithmWithStats, SearchState[] randomGoals, SearchState[] randomStarts, MapSearchProblem problem) {
        long startTime = System.currentTimeMillis();

        SearchStats searchStats = new SearchStats();
        List<SearchState> path;
        for (int j = 0; j < NUM_PATHS; j++) {
            path = searchAlgorithmWithStats.findPath(randomStarts[j], randomGoals[j], searchStats);
            if (path == null)
                continue;
            searchStats.setPathCost(searchStats.getPathCost() + SearchUtil.findPathCost(path, problem));
        }

        searchStats.setTotalTime(System.currentTimeMillis() - startTime);

        return searchStats;
    }
}