import map.AbstractedMap;
import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.SearchState;
import search.algorithms.DBAStar;
import search.algorithms.PRAStar;
import stats.SearchStats;

import java.util.Arrays;

public class OnlineExperimentsOnBgAndDa {

    private static final Logger logger = LogManager.getLogger(OnlineExperimentsOnBgAndDa.class);

    // TODO: Run this for multiple trials
    public static void main(String[] args) {
        int[] gridSizes = {16, 32, 64, 128};
        String[] mapStringsBG = {"012", "516", "603", "701"};
        String[] mapStringsDA = {"hrt000d", "orz100d", "orz103d", "orz300d", "orz700d", "orz702d", "orz900d", "ost000a", "ost000t", "ost100d"};

        String[] fullTitles = {"gridSize", "totalNumberOfStatesExpandedDBA", "dbaPathfindingTime", "dbaTotalTime", "totalNumberOfStatesExpandedPRA", "praPathfindingTime", "praTotalTime"};

        String[] mapStrings = mapStringsDA;

        GameMap gameMap;
        SearchState start, goal;

        long startTime;

        for (String mapString : mapStrings) {
            logger.info("{} map", mapString);

            String temp = Arrays.toString(fullTitles);
            logger.info(temp.substring(1, temp.length() - 1));

            gameMap = new GameMap("src/main/resources/maps/" + mapString + ".map");
            for (int gridSize : gridSizes) {
                AbstractedMap abstractedMap = new AbstractedMap(gameMap, gridSize);
                int[] regionReps = abstractedMap.getRegionReps();
                int numRegionReps = abstractedMap.getNumRegions();

                long dbaPathfindingTime = 0;

                DBAStar dbaStar = new DBAStar(gameMap, gridSize, false);
                SearchStats dbaStats = new SearchStats();

                startTime = System.nanoTime();
                for (int i = 0; i < numRegionReps; i++) {
                    int startRegionRep = regionReps[i];
                    start = new SearchState(startRegionRep);
                    for (int neighbourRegion : abstractedMap.getRegionById(i + AbstractedMap.START_NUM).getNeighborIds()) {
                        int goalRegionRep = regionReps[neighbourRegion - AbstractedMap.START_NUM];
                        goal = new SearchState(goalRegionRep);

                        dbaStar.findPath(start, goal, dbaStats);
                        dbaPathfindingTime += dbaStats.getTimeToFindPathOnline();
                    }
                }

                logger.info("{}, {}, {}, {}", gridSize, dbaStats.getNumStatesExpandedHC(), dbaPathfindingTime / 1_000_000.0, (System.nanoTime() - startTime) / 1_000_000.0);
            }

            gameMap = new GameMap("src/main/resources/maps/" + mapString + ".map");
            for (int gridSize : gridSizes) {
                AbstractedMap abstractedMap = new AbstractedMap(gameMap, gridSize);
                int[] regionReps = abstractedMap.getRegionReps();
                int numRegionReps = abstractedMap.getNumRegions();

                long praPathfindingTime = 0;

                PRAStar praStar = new PRAStar(gameMap, gridSize);

                SearchStats praStats = new SearchStats();
                startTime = System.nanoTime();

                for (int i = 0; i < numRegionReps; i++) {
                    int startRegionRep = regionReps[i];
                    start = new SearchState(startRegionRep);
                    for (int neighbourRegion : abstractedMap.getRegionById(i + AbstractedMap.START_NUM).getNeighborIds()) {
                        int goalRegionRep = regionReps[neighbourRegion - AbstractedMap.START_NUM];
                        goal = new SearchState(goalRegionRep);

                        praStar.findPath(start, goal, praStats);
                        praPathfindingTime += praStats.getTimeToFindPathOnline();
                    }
                }

                logger.info("{}, {}, {}", praStats.getNumStatesExpanded(), praPathfindingTime / 1_000_000.0, (System.nanoTime() - startTime) / 1_000_000.0);
            }
        }
    }
}
