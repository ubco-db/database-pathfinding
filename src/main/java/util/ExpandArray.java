package util;

import java.util.HashSet;

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

    public ExpandArray(ExpandArray a) {
        count = a.count;
        values = new int[count];
        System.arraycopy(a.values, 0, values, 0, a.count);
    }

    public void addAll(ExpandArray a) {
        for (int i = 0; i < a.count; i++)
            add(a.values[i]);
    }

    public void addAllDistinct(ExpandArray a) {
        for (int i = 0; i < a.count; i++) {    // See if it already exists
            int val = a.values[i];
            boolean found = false;
            for (int j = 0; j < count; j++) {
                if (values[j] == val) {
                    found = true;
                    break;
                }
            }
            if (!found) add(val);
        }
    }

    public void addAll(HashSet<Integer> n) {
        for (Integer integer : n) {
            add(integer);
        }
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