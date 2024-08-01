import map.GameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.SearchState;
import search.algorithms.DBAStar;
import stats.SearchStats;

/*
I want to have one example on the impact of subgoals. In the paper, I have been using DragonAge maps grid size 32.
There are 1088 paths and 13% have subgoals. What I would like is to have data on:
    - For 13% paths, the states expanded and time for hill-climbing versus A*
    - For the other 87% paths, the states expanded and time for hill-climbing versus A*
 */
public class SubgoalsVsNoSubgoals {

    private static final Logger logger = LogManager.getLogger(SubgoalsVsNoSubgoals.class);

    public static void main(String[] args) {
        String[] mapStringsDA = {"hrt000d", "orz100d", "orz103d", "orz300d", "orz700d", "orz702d", "orz900d", "ost000a",
                "ost000t", "ost100d"};
        final int GRID_SIZE = 32;

        int numPathsWithSubgoals = 0;
        int numPathsWithoutSubgoals = 0;
        long numPaths = 0;

        SearchStats globalSubgoalSearchStats = new SearchStats();
        SearchStats globalNoSubgoalSearchStats = new SearchStats();

        int count = 10;
        for (int k = 0; k < count; k++) {
            for (String mapString : mapStringsDA) {
                GameMap gameMap = new GameMap("src/main/resources/maps/" + mapString + ".map");
                DBAStar dbaStar = new DBAStar(gameMap, GRID_SIZE, false);

                // Get subgoal-compressed paths stored in database
                int[][][] pathSubgoals = dbaStar.getSubgoalDB().getPathSubgoals();
                // Get number of regions (there are null values in the arrays for indices > numRegions)
                int numRegions = dbaStar.getSubgoalDB().getNumGroups();

                // Initialize separate search stats
                SearchStats subgoalSearchStats = new SearchStats();
                SearchStats noSubgoalSearchStats = new SearchStats();

                // Reuse start and goal (initialize with dummy values)
                SearchState start = new SearchState(0);
                SearchState goal = new SearchState(0);

                long startTime;

                // logger.info("{} map", mapString);

                // Iterate over paths in database to identify paths with subgoals, find those paths
                for (int i = 0; i < numRegions; i++) {
                    for (int j = 0; j < pathSubgoals[i].length; j++) {
                        start.initialize(pathSubgoals[i][j][0]);
                        goal.initialize(pathSubgoals[i][j][pathSubgoals[i][j].length - 1]);

                        // Paths that have subgoals in the database
                        if (pathSubgoals[i][j].length > 2) {
                            startTime = System.nanoTime();
                            dbaStar.getHillClimbing().findPath(start, goal, subgoalSearchStats);
                            subgoalSearchStats.incrementTimeToFindPathsUsingHC(System.nanoTime() - startTime);

                            startTime = System.nanoTime();
                            dbaStar.getCompressAStar().findPath(start, goal, subgoalSearchStats);
                            subgoalSearchStats.incrementTimeToFindPathsUsingAStar(System.nanoTime() - startTime);

                            numPathsWithSubgoals++;
                        } else { // Paths that do not
                            startTime = System.nanoTime();
                            dbaStar.getHillClimbing().findPath(start, goal, noSubgoalSearchStats);
                            noSubgoalSearchStats.incrementTimeToFindPathsUsingHC(System.nanoTime() - startTime);

                            startTime = System.nanoTime();
                            dbaStar.getCompressAStar().findPath(start, goal, noSubgoalSearchStats);
                            noSubgoalSearchStats.incrementTimeToFindPathsUsingAStar(System.nanoTime() - startTime);

                            numPathsWithoutSubgoals++;
                        }
                    }
                }
                numPaths += dbaStar.getSearchStats().getNumPaths();

//            logger.info(subgoalSearchStats.getHCStats());
//            logger.info(noSubgoalSearchStats.getHCStats());

                globalSubgoalSearchStats.addAll(subgoalSearchStats);
                globalNoSubgoalSearchStats.addAll(noSubgoalSearchStats);
            }
        }

        globalSubgoalSearchStats.divideBy(mapStringsDA.length * count);
        globalNoSubgoalSearchStats.divideBy(mapStringsDA.length * count);

        logger.info("With subgoals: ");
        logger.info("numStatesExpandedHC, timeTakenHC");
        logger.info(globalSubgoalSearchStats.getHCStats());
        logger.info("numStatesExpandedAStar, timeTakenAStar");
        logger.info(globalSubgoalSearchStats.getAStarStats());

        logger.info("Without subgoals: ");
        logger.info("numStatesExpandedHC, timeTakenHC");
        logger.info(globalNoSubgoalSearchStats.getHCStats());
        logger.info("numStatesExpandedAStar, timeTakenAStar");
        logger.info(globalNoSubgoalSearchStats.getAStarStats());

//        System.out.println(((double) numPathsWithSubgoals) / mapStringsDA.length);
//        System.out.println(((double) numPathsWithoutSubgoals) / mapStringsDA.length);
//        System.out.println(((double) numPaths) / mapStringsDA.length);
    }
}
