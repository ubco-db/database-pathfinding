package search;

import java.util.Arrays;

/**
 * This class remembers which states have been previously visited in another search and some cost (or # of steps) of that search to get to a defined goal.
 * <p>
 * This is used for the abstraction so that once a state is found on the path to the goal that we already know the cost to the goal for we can stop the search immediately.
 */
public class SavedSearch {
    private int[] closedList;            // Hash table storing list of ids previously found
    private int[] costClosedList;        // Stores cost for each entry
    private int numClosed;                // Number of items in the closed list
    private int currentCL;                // Current closed length

    private static int GROWTH_FACTOR = 4;        // Amount to re-size heap when overflows

    public SavedSearch() {
        this(1024);
    }

    public SavedSearch(int size) {
        int hashSize = (int) (size * 2);
        closedList = new int[hashSize];
        currentCL = hashSize - 1;
        costClosedList = new int[hashSize];
        Arrays.fill(closedList, -1);
    }

    public void clear() {
        Arrays.fill(closedList, -1);
        numClosed = 0;
    }

    private int getInterval(int i) {
        return i / 2 + i * i / 2;
    }

    public boolean isFound(int k) {    // Returns true if given key is in closed list
        int i = hashClosed(k);

        int count = 0;
        while (closedList[i] != -1 && closedList[i] != k) {
            count++;
            i = (i + getInterval(count)) & currentCL;
        }
        return !(closedList[i] == -1);
    }

    public int getFoundVal(int k) {    // Returns the value associated with the previously visited id
        // int i = k & (closedList.length-1);
        // int i = k % closedList.length;
        int i = hashClosed(k);

        int count = 0;
        while (closedList[i] != -1 && closedList[i] != k) {
            count++;
            i = (i + getInterval(count)) & currentCL;
        }
        if (closedList[i] == -1)
            return -1;
        return costClosedList[i];
    }

    public void setFound(int k, int val) {    // Sets the key in the closed list
        if (numClosed > closedList.length * .50) {    // Resize hash table

            int[] tmp = new int[closedList.length];
            int[] tmp2 = new int[closedList.length];
            System.arraycopy(closedList, 0, tmp, 0, closedList.length);
            System.arraycopy(costClosedList, 0, tmp2, 0, closedList.length);

            closedList = new int[(int) (closedList.length * 2 * GROWTH_FACTOR)];
            costClosedList = new int[(int) (costClosedList.length * 2 * GROWTH_FACTOR)];
            System.out.println("Resized closed list to: " + closedList.length);
            currentCL = closedList.length - 1;
            Arrays.fill(closedList, -1);
            for (int i = 0; i < tmp.length; i++)
                if (tmp[i] != -1) {
                    insertClosedList(tmp[i], tmp2[i]);
                }
        }

        insertClosedList(k, val);
        numClosed++;
    }

    public int getSize() {
        return numClosed;
    }


    private int hashClosed(int key) {
        return key & currentCL;
    }

    private void insertClosedList(int k, int val) {
        int i = hashClosed(k);
        int count = 0;

        while (closedList[i] >= 0) {
            count++;
            i = (i + getInterval(count)) & currentCL;
        }
        closedList[i] = k;
        costClosedList[i] = val;
    }

}
