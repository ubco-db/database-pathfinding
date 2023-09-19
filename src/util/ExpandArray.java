package util;

import search.SearchState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

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
        for (int i = 0; i < a.count; i++)
            values[i] = a.values[i];
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

    public ArrayList<SearchState> convert() {
        ArrayList<SearchState> a = new ArrayList<SearchState>(count);
        for (int i = 0; i < count; i++) {
            a.add(new SearchState(values[i]));
        }
        return a;
    }

    public void addAll(HashSet<Integer> n) {
        Iterator<Integer> it = n.iterator();
        while (it.hasNext()) {
            add(it.next());
        }
    }

    public void add(int id) {
        if (count >= values.length) {
            int[] tmp = new int[values.length * 10];
            for (int i = 0; i < count; i++)
                tmp[i] = values[i];
            values = tmp;
        }
        values[count++] = id;
    }

    public int[] getValues() {
        return values;
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
        StringBuffer buf = new StringBuffer(100);
        buf.append("[");
        if (count > 0) buf.append(values[0]);
        for (int i = 1; i < count; i++)
            buf.append(", " + values[i]);
        buf.append("]");
        return buf.toString();
    }
}
