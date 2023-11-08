package database;

import search.SearchProblem;
import search.SearchState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Represents the static abstraction as an index database. Currently, simply using a sorted array and RLE compression.
 *
 * @author rlawrenc
 */
public class IndexDB {
    private int[] nodeIds;
    private int[] seedIds;
    private int count;
    private int totalCells;
    private int[] hashTable;
    private int numRegions;
    private int[][] groups;

    private static final int MAX_SIZE = 1000000;
    private static final int HT_STEP_SIZE = 1000;

    public IndexDB() {
        count = 0;
        nodeIds = new int[MAX_SIZE];
        seedIds = new int[MAX_SIZE];
        numRegions = 0;
    }

    /**
     * Add assumes that have already done RLE compression and this is next record in compressed order.
     * It does not alter the compression or fix compression in any way if new data is added.
     * In effect, compression occurs outside of this class.  A good thing?
     *
     * @param nodeId
     * @param seedId
     */
    public void add(int nodeId, int seedId) {
        nodeIds[count] = nodeId;
        seedIds[count] = seedId;
        count++;
    }

    /**
     * Hash table structure.
     * Provides quick lookup in sorted array controlled by htStepSize.
     * # of hash table entries is maximum id/htStepSize.
     * If step size is 1000, hash table has entries array locations for ids: 0, 1000, 2000, 3000.
     */
    public void buildHT() {    // Builds a HT from an existing sorted array
        int maxId = nodeIds[count - 1];
        hashTable = new int[maxId / HT_STEP_SIZE + 1];
        hashTable[0] = 0;
        for (int i = 1; i < maxId / HT_STEP_SIZE; i++) {
            hashTable[i] = findLoc(i * HT_STEP_SIZE);
        }
        System.out.println("Hash table size: " + hashTable.length);
    }

    // Section 5.2 hcdps.pdf
    public int findHT(int nodeId) {

        int htloc = nodeId / HT_STEP_SIZE; // ground level state id / k = hash table entry that maps to the RLE entry for ground-level state
        if (htloc >= hashTable.length) htloc = hashTable.length - 1;

        int loc = hashTable[htloc];

        while (loc < count - 1 && nodeId >= nodeIds[loc + 1]) { // scan hashmap to find correct location
            loc++;
        }

        if (loc == count) loc--;
        return seedIds[loc];
    }

    public int find(int nodeId) {
        return seedIds[findLoc(nodeId)];
    }

    public int findLoc(int nodeId) {
        int loc = Arrays.binarySearch(nodeIds, 0, count, nodeId);
        if (loc < 0) { // Not an exact match with record in index, find appropriate record based on range
            loc = (loc + 2) * -1;
            if (loc > nodeIds.length) loc = 0;
            if (loc < 0) loc = 0;
        }
        return loc;
    }

    public int getCount() {
        return count;
    }

    public int getHTSize() {
        return hashTable.length;
    }

    public int getTotalCells() {
        return totalCells;
    }

    public void setTotalCells(int totalCells) {
        this.totalCells = totalCells;
    }

    /**
     * Verifies the compressed mapping for the search problem (index and hash table).
     *
     * @param problem
     * @return boolean
     */
    public boolean verify(SearchProblem problem) {
        System.out.println("Verifying base RLE compression of mapping.");
        // Check if all problem entries make sense
        // Do it brute force - search for each one
        int mistakes = 0, mistakesHT = 0;
        SearchState state = new SearchState();
        problem.initIterator();
        while (problem.nextState(state)) {
            int foundVal = find(state.id);
            if (state.cost != foundVal) mistakes++;

            // Check HT as well
            foundVal = findHT(state.id);
            if (state.cost != foundVal) mistakesHT++;
        }
        System.out.println("States in error: " + mistakes + " Hash table errors: " + mistakesHT);
        return (mistakes == 0) && (mistakesHT == 0);
    }

    /**
     * Saves the compressed mapping to a file.
     *
     * @param fileName
     */
    public void export(String fileName) {
        try (PrintWriter out = new PrintWriter(fileName)) {
            out.println(count);
            out.println(numRegions);

            // Write out group id to group seed id mapping
            for (int i = 0; i < numRegions; i++)
                out.println(groups[i][0] + " " + groups[i][1]);
            // Write out node and seeds array
            for (int i = 0; i < count; i++)
                out.print(nodeIds[i] + " ");
            out.println();
            for (int i = 0; i < count; i++)
                out.print(seedIds[i] + " ");
            out.println();
        } catch (FileNotFoundException e) {
            System.out.println("Error with output file: " + e);
        }
    }

    public void print() {
        System.out.println(count);
        System.out.println(numRegions);

        // Write out group id to group seed id mapping
        for (int i = 0; i < numRegions; i++)
            System.out.println(groups[i][0] + " " + groups[i][1]);
        // Write out node and seeds array
        for (int i = 0; i < count; i++)
            System.out.print(nodeIds[i] + " ");
        System.out.println();
        for (int i = 0; i < count; i++)
            System.out.print(seedIds[i] + " ");
        System.out.println();

        System.out.println("Hash table: ");
        for (int j : hashTable) System.out.print(j + "\t");
        System.out.println();
    }

    /**
     * Loads compressed mapping from file into memory.
     *
     * @param fileName
     * @return boolean
     */
    public boolean load(String fileName) {
        Scanner sc = null;
        boolean success = true;

        try {
            sc = new Scanner(new File(fileName));
            long currentTime = System.currentTimeMillis();
            count = sc.nextInt();
            numRegions = sc.nextInt();

            nodeIds = new int[count];
            seedIds = new int[count];
            groups = new int[numRegions][2];

            // Write out group id to group seed id mapping
            for (int i = 0; i < numRegions; i++) {
                groups[i][0] = sc.nextInt();
                groups[i][1] = sc.nextInt();
            }

            for (int i = 0; i < count; i++)
                nodeIds[i] = sc.nextInt();

            for (int i = 0; i < count; i++)
                seedIds[i] = sc.nextInt();

            System.out.println("Loaded " + count + " records in " + (System.currentTimeMillis() - currentTime));
        } catch (FileNotFoundException e) {
            System.out.println("Did not find input file: " + e);
            success = false;
        } finally {
            if (sc != null) sc.close();
        }
        return success;
    }

    public int getNumRegions() {
        return numRegions;
    }

    public void setNumRegions(int numRegions) {
        this.numRegions = numRegions;
    }

    public void setGroups(int[][] groups) {
        this.groups = groups;
    }

    public int getSeedId(int groupId) {
        for (int[] group : groups) if (group[0] == groupId) return group[1];
        return -1;
    }

    public int[][] getGroups() {
        return groups;
    }
}
