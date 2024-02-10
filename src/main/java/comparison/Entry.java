package comparison;

import java.util.ArrayList;

public class Entry implements Comparable<Entry> {
    private final String output;
    private final double percentageChanged;
    private final int wallId;

    public Entry(double percentageChanged, int wallId, ArrayList<ChangedPath> changedPaths) {
        this.percentageChanged = percentageChanged;
        this.wallId = wallId;
        double[] amountChanged = getAmountChanged(changedPaths);
        String percentChangeOfPathToGoal = String.format("%.2f percent of goals had a 0-25%% change in their paths, %.2f percent had a 25-50%% change, %.2f percent had a 50-75%% change, and %.2f percent had a 75-100%% change", amountChanged[0], amountChanged[1], amountChanged[2], amountChanged[3]);
        this.output = String.format("Wall at: %d. Percentage of goals changed: %.2f.", wallId, percentageChanged) + "\t" + percentChangeOfPathToGoal;
    }

    private double[] getAmountChanged(ArrayList<ChangedPath> changedPaths) {
        int n = changedPaths.size();
        if (n == 0) return new double[]{0, 0, 0, 0};

        double[] quartiles = new double[4];
        for (ChangedPath changedPath : changedPaths) {
            double p = changedPath.getPercentageOfPathToGoalChanged();
            if (p < 25) quartiles[0]++;
            else if (p < 50) quartiles[1]++;
            else if (p < 75) quartiles[2]++;
            else quartiles[3]++;
        }
        return new double[]{quartiles[0] / n * 100, quartiles[1] / n * 100, quartiles[2] / n * 100, quartiles[3] / n * 100};
    }

    public String getOutput() {
        return output;
    }

    public int getWallId() {
        return wallId;
    }

    public double getPercentageChanged() {
        return percentageChanged;
    }

    @Override
    public int compareTo(Entry o) {
        return Double.compare(o.getPercentageChanged(), this.getPercentageChanged());
    }
}
