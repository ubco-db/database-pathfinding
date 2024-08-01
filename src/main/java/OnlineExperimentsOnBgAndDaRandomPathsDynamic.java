import map.AbstractedMap;
import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.MapSearchProblem;
import search.RegionSearchProblem;
import search.SearchState;
import search.algorithms.*;
import stats.SearchStats;

import java.util.List;
import java.util.Random;

public class OnlineExperimentsOnBgAndDaRandomPathsDynamic {
    public static final Logger logger = LogManager.getLogger(OnlineExperimentsOnBgAndDaRandomPathsDynamic.class);

    public static void main(String[] args) throws Exception {
        String[] mapStringsDA = {"hrt000d", "orz100d", "orz103d", "orz300d", "orz700d", "orz702d", "orz900d", "ost000a",
                "ost000t", "ost100d"};
        String[] mapStringsBG = {"012", "516", "603", "701"};
        int[] gridSizes = {32};

        String[] mapStrings = mapStringsDA;
        GameMap gameMap;

        final int COUNT = 5;

        final int NUM_PATHS = 1000;

        logger.info("Number of goals: {}", NUM_PATHS);

        for (String mapString : mapStrings) {
            logger.info("{} map", mapString);
            logger.info("gridSize, #Searches, DBA totalTime, DBA numStatesExpanded, DBA numStatesExpandedHC, PRA-C totalTime, PRA-C numStatesExpanded, PRA-C numStatesExpandedHC, PRA-P totalTime, PRA-P numStatesExpanded, PRA-P numStatesExpandedHC, PRA totalTime, PRA numStatesExpanded, PRA numStatesExpandedHC,");

            for (int gridSize : gridSizes) {
                gameMap = new GameMap("src/main/resources/maps/" + mapString + ".map");

                AbstractedMap abstractedMap = new AbstractedMap(gameMap, gridSize);

                RegionSearchProblem regionSearchProblem = new RegionSearchProblem(abstractedMap);

                List<SearchState> openStates = MapSearchProblem.getOpenStateList(gameMap);

                Random random = new Random();
                random.setSeed(56256902 + gridSize);

                for (int s = 0; s < 15; s++) {

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

                        dbaStats.addAll(timeAlgorithm(new DBAStar(new GameMap(gameMap), gridSize, false), randomGoals, randomStarts, s, NUM_PATHS));
                        praCStats.addAll(timeAlgorithm(new PRAStarWithCachingAndHCCompression(new GameMap(gameMap), gridSize), randomGoals, randomStarts, s, NUM_PATHS));
                        praPStats.addAll(timeAlgorithm(new PRAStarWithCaching(new GameMap(gameMap), gridSize), randomGoals, randomStarts, s, NUM_PATHS));
                        praStats.addAll(timeAlgorithm(new PRAStar(new GameMap(gameMap), gridSize), randomGoals, randomStarts, s, NUM_PATHS));
                    }

                    dbaStats.divideBy(COUNT);
                    praCStats.divideBy(COUNT);
                    praPStats.divideBy(COUNT);
                    praStats.divideBy(COUNT);

                    String dbaStatsString = String.format("%d, %d, %d", dbaStats.getTotalTime(), dbaStats.getNumStatesExpanded(), dbaStats.getNumStatesExpandedHC());
                    String praPStatsString = String.format("%d, %d, %d", praCStats.getTotalTime(), praCStats.getNumStatesExpanded(), praCStats.getNumStatesExpandedHC());
                    String praCStatsString = String.format("%d, %d, %d", praPStats.getTotalTime(), praPStats.getNumStatesExpanded(), praPStats.getNumStatesExpandedHC());
                    String praStatsString = String.format("%d, %d, %d", praStats.getTotalTime(), praStats.getNumStatesExpanded(), praStats.getNumStatesExpandedHC());

                    logger.info("{}, {}, {}, {}, {}, {}", gridSize, s + 1, dbaStatsString, praPStatsString, praCStatsString, praStatsString);
                }
            }
        }
    }

    private static SearchStats timeAlgorithm(DynamicSearchAlgorithm searchAlgorithm, SearchState[] randomGoals, SearchState[] randomStarts, int s, int numPaths) throws Exception {
        long startTime = System.currentTimeMillis();

        SearchStats searchStats = new SearchStats();

        for (int j = 0; j < numPaths; j++) {
            // System.out.println(randomStarts[j].getStateId() + " " + randomGoals[j].getStateId());
            searchAlgorithm.addWall(randomGoals[j].getStateId());
            searchAlgorithm.removeWall(randomGoals[j].getStateId());

            for (int i = 0; i < s; i++) {
                searchAlgorithm.findPath(randomStarts[j], randomGoals[j], searchStats);
            }
        }

        searchStats.setTotalTime(System.currentTimeMillis() - startTime);

        return searchStats;
    }
}