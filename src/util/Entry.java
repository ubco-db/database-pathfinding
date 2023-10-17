package util;

public class Entry implements Comparable<Entry> {
    private final String output;
    private final double percentageChanged;

    public Entry(double percentageChanged, int wallId) {
        this.percentageChanged = percentageChanged;
        this.output = String.format("Wall at: %d. Percentage of goals changed: %.2f%n", wallId, percentageChanged);
    }

    public String getOutput() {
        return output;
    }

    public double getPercentageChanged() {
        return percentageChanged;
    }

    @Override
    public int compareTo(Entry o) {
        return Double.compare(o.getPercentageChanged(), this.getPercentageChanged());
    }
}
