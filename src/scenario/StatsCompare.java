package scenario;

import search.StatsRecord;


public class StatsCompare {

    public StatsCompare() {
        super();
    }

    /*
     * Compares the results from three statistic records and outputs the result.
     */
    public static void compareRecords(StatsRecord[] recArr, String[] algName) {
        int arrLen = recArr.length;
        if (arrLen == 0)
            return;

        long[] val = new long[arrLen];
        long[] cost = new long[arrLen];
        StringBuilder sb1 = new StringBuilder(); // Path length
        sb1.append("Path length:\n");
        StringBuilder sb2 = new StringBuilder(); // Path cost
        sb2.append("Path cost:\n");
        StringBuilder sb3 = new StringBuilder(); // States expanded
        sb3.append("States expanded:\n");
        StringBuilder sb4 = new StringBuilder(); // States updated
        sb4.append("States updated:\n");
        StringBuilder sb5 = new StringBuilder(); // time
        sb5.append("Time:\n");
        StringBuilder sb6 = new StringBuilder(); // Max. open list
        sb6.append("Max. open list:\n");
        StringBuilder sb7 = new StringBuilder(); // Max. closed list
        sb7.append("Max. closed list:\n");
        StringBuilder sb8 = new StringBuilder(); // Max. mem size
        sb8.append("Max. mem size:\n");

        for (int i = 0; i < arrLen; i++) {
            // path length results
            val[i] = recArr[i].getPathLength();
            sb1.append(algName[i]).append(": ").append(val[i]);
            double sub = 0;
            if (val[0] != 0)
                sub = 1000 * val[i] / val[0] / 1000.0;

            sb1.append(",  Suboptimal: ").append(sub).append("\n");

            // path cost results
            cost[i] = recArr[i].getPathCost();
            sb2.append(algName[i]).append(": ").append(cost[i]);
            sub = 0;
            if (cost[0] != 0)
                sub = 1000 * cost[i] / cost[0] / 1000.0;

            sb2.append(",  Suboptimal: ").append(sub).append("\n");

            // states Expanded
            double speedup = 0;
            if (i >= 15) {    // Compute speedup over baseline
                // None baseline heuristic
                // Find two A* weights that bracket this cost
                long len = recArr[i].getPathLength();
                int j;
                for (j = 0; j < 15; j++) {
                    if (len < recArr[j].getPathLength()) {
                        if (j == 0) {
                            speedup = (double) recArr[j].getStatesExpanded() / recArr[i].getStatesExpanded();
                        } else {
                            double slope = ((double) len - recArr[j - 1].getPathLength()) / (recArr[j].getPathLength() - recArr[j - 1].getPathLength());
                            speedup = (recArr[j - 1].getStatesExpanded() + (recArr[j].getStatesExpanded() - recArr[j - 1].getStatesExpanded()) * slope) / recArr[i].getStatesExpanded();
                        }
                        break;
                    }
                }
                if (j >= 15)
                    speedup = recArr[i].getStatesExpanded() / recArr[14].getStatesExpanded();

                sb3.append(algName[i]).append(", ").append(recArr[i].getStatesExpanded()).append(",  Speedup, ").append(speedup).append("\n");
            } else {
                sb3.append(algName[i]).append(", ").append(recArr[i].getStatesExpanded()).append("\n");
            }

            // states Updated
            sb4.append(algName[i]).append(": ").append(recArr[i].getStatesUpdated()).append("\n");

            // time
            sb5.append(algName[i]).append(": ").append(recArr[i].getTime()).append("\n");

            // Max. open list
            sb6.append(algName[i]).append(": ").append(recArr[i].getOpenListSize()).append("\n");

            // Max. close list
            sb7.append(algName[i]).append(": ").append(recArr[i].getClosedListSize()).append("\n");

            // Max. mem size:
            sb8.append(algName[i]).append(": ").append(recArr[i].getMaxMemSize()).append("\n");

        }

        System.out.println(sb1);
        System.out.println(sb2);
        System.out.println(sb3);
        System.out.println(sb4);
        System.out.println(sb5);
        System.out.println(sb6);
        System.out.println(sb7);
        System.out.println(sb8);
    }

    /**
     * Updates statistics in record 1 with those from record 2.
     *
     * @param rec1 - record 1
     * @param rec2 - record 2
     */
    public static void mergeRecords(StatsRecord rec1, StatsRecord rec2) {
        rec1.setPathCost(rec1.getPathCost() + rec2.getPathCost());
        rec1.setPathLength(rec1.getPathLength() + rec2.getPathLength());
        rec1.incrementStatesExpanded(rec2.getStatesExpanded());
        rec1.incrementStatesUpdated(rec2.getStatesUpdated());
        rec1.setTime(rec1.getTime() + rec2.getTime());
        if (rec1.getOpenListSize() < rec2.getOpenListSize())
            rec1.setOpenListSize(rec2.getOpenListSize());
        if (rec1.getClosedListSize() < rec2.getClosedListSize())
            rec1.setClosedListSize(rec2.getClosedListSize());
        if (rec1.getMaxMemSize() < rec2.getMaxMemSize())
            rec1.setMaxMemSize(rec2.getMaxMemSize());
        rec1.setSubgoals(rec1.getSubgoals() + rec2.getSubgoals());
    }
}
