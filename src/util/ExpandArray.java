package util;

public class ExpandArray {
    private int count;
    private int[] values;

    public ExpandArray() {
        count = 0;
        values = new int[10];
    }

    public ExpandArray(int size) {
        count = 0;
        values = new int[size];
    }

    public void add(int id) {
        if (count >= values.length) {
            int[] tmp = new int[values.length * 10];
            System.arraycopy(values, 0, tmp, 0, count);
            values = tmp;
        }
        values[count++] = id;
    }

    public int get(int id) {
        return values[id];
    }

    public int num() {
        return count;
    }

    public void clear() {
        count = 0;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(100);
        buf.append("[");
        if (count > 0) buf.append(values[0]);
        for (int i = 1; i < count; i++)
            buf.append(", ").append(values[i]);
        buf.append("]");
        return buf.toString();
    }
}
