import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.algorithms.DBAStar;
import search.algorithms.PRAStar;
import stats.SearchStats;

import java.util.Arrays;

public class OfflineExperimentsOnBgAndDa {

    private static final Logger logger = LogManager.getLogger(OfflineExperimentsOnBgAndDa.class);

    public static void main(String[] args) {
        int[] gridSizes = {16, 32, 64, 128};
        String[] mapStringsBG = {"012", "516", "603", "701"};
        String[] mapStringsDA = {"hrt000d", "orz100d", "orz103d", "orz300d", "orz700d", "orz702d", "orz900d", "ost000a", "ost000t", "ost100d"};

        String[] fullTitles = {"", "numPaths", "totalPathLength", "totalPathCost", "numSubgoals", "pathWithSubgoals", "numStatesExpandedBFS", "numStatesExpanded", "numStatesExpandedHCCompression", "timeToAbstractRegions", "timeToDetermineNeighbourhoods", "totalAbstractionTime", "timeToFindPathsOffline", "timeToPerformHCCompression", "timeToGenerateDatabase", "totalTime"};
        String[] timesOnly = {"", "timeToAbstractRegions", "timeToDetermineNeighbourhoods", "totalAbstractionTime", "timeToFindPathsOffline", "timeToPerformHCCompression", "timeToGenerateDatabase", "totalTime"};
        String[] statesExpandedOnly = {"", "numStatesExpandedBFS", "numStatesExpanded", "numStatesExpandedHCCompression"};

        String[] columnTitles = fullTitles;
        String[] mapStrings = mapStringsBG;

        final int COUNT = 10;

        GameMap gameMap;
        long start;

        for (String mapString : mapStrings) {
            String dbaStatsString, praStatsString;

            logger.info("{} map", mapString);
            logger.info("gridSize{},{}", getColumnTitles("DBA*", columnTitles), getColumnTitles("PRA*", columnTitles));

            for (int gridSize : gridSizes) {
                SearchStats praStats = new SearchStats();
                SearchStats dbaStats = new SearchStats();

                for (int i = 0; i < COUNT; i++) {
                    gameMap = new GameMap("src/main/resources/maps/" + mapString + ".map");
                    start = System.nanoTime();
                    DBAStar dbaStar = new DBAStar(gameMap, gridSize, false);
                    dbaStar.getSearchStats().setTotalTime(System.nanoTime() - start);

                    dbaStats.addAll(dbaStar.getSearchStats());

                    gameMap = new GameMap("src/main/resources/maps/" + mapString + ".map");
                    start = System.nanoTime();
                    PRAStar praStar = new PRAStar(gameMap, gridSize);
                    praStar.getSearchStats().setTotalTime(System.nanoTime() - start);

                    praStats.addAll(praStar.getSearchStats());
                }

                dbaStats.divideBy(COUNT);
                praStats.divideBy(COUNT);

                dbaStatsString = dbaStats.getData();
                praStatsString = praStats.getData();

                logger.info("{},{},,{}", gridSize, dbaStatsString, praStatsString);
            }
        }
    }

    static String getColumnTitles(String algorithm, String[] titles) {
        String[] columnTitles = new String[titles.length];
        System.arraycopy(titles, 0, columnTitles, 0, titles.length);
        for (int i = 0; i < columnTitles.length; i++) {
            if (!columnTitles[i].isEmpty()) {
                columnTitles[i] = columnTitles[i] + algorithm;
            }
        }
        String temp = Arrays.toString(columnTitles);
        return temp.substring(1, temp.length() - 1);
    }
}
