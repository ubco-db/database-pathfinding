package database;

import map.GameMap;
import map.GroupRecord;
import search.*;

import java.util.*;
import java.util.Map.Entry;

/**
 * A database of records organized as a 2D matrix from state representatives.
 * Example record[i][j] would navigate from representative of abstract region i to representative of abstract region j.
 * This version is only suitable for algorithms that perform state abstraction.
 * Database also stores RLE compressed version of mapping of state space to abstract state space so region representatives can be found in constant time.
 *
 * @author rlawrenc
 */
public class SubgoalDBExact extends SubgoalDB {
    protected IndexDB db;                               // Store base to abstract state mapping
    protected SubgoalDBRecord[][] recordMatrix;         // Stores records to navigate between region representatives

    public SubgoalDBExact() {
        super();
    }


    /**
     * Builds compressed RLE index mapping base states to abstraction state representatives.
     * The problem must already encode the mapping in its internal structure.
     *
     * @param problem - required to enumerate through problem states
     */
    public void computeIndex(SearchProblem problem, DBStatsRecord rec) {
        this.problem = problem;

        generateIndexDB();                      // Generate the RLE compressed index mapping
        db.buildHT();                           // Build a hash table over the RLE index to allow constant time access

        rec.addStat(24, db.getCount());
        rec.addStat(25, db.getHTSize());

        int maxSize = problem.getMaxSize();
        System.out.println("Problem states: " + maxSize + "\nNumber of DB states: " + db.getTotalCells() + " Number of records in index DB:  " + db.getCount() + "\n % of problem size: " + (db.getCount() * 100.0 / db.getTotalCells()) + "\n % of problem total size: " + (db.getCount() * 100.0 / (maxSize)));
    }

    /**
     * Saves database of records and abstraction mapping.
     */
    public void exportDB(String fileName) {
        super.exportDB(fileName);                // Export the base database of records
        db.export(fileName + "i");       // Export the mapping
    }

    /**
     * Returns a single record for a given search problem based on abstract states of problem start and end.
     */
    public ArrayList<SubgoalDBRecord> findNearest(SearchProblem problem, SearchState start, SearchState goal, SearchAlgorithm searchAlg, int max, StatsRecord stats, ArrayList<SubgoalDBRecord> used) {
        int startSeedId = db.findHT(start.id);
        int goalSeedId = db.findHT(goal.id);

        ArrayList<SubgoalDBRecord> result = new ArrayList<>(1);
        SubgoalDBRecord rec = recordMatrix[startSeedId][goalSeedId];
        if (rec == null) {
            if (startSeedId == goalSeedId) {    // This is possible as there is no guarantee that you can go from any two states in a region (need to direct through seed).
                // Build a record to route in the region with the seed as a subgoal.
                int seedId = db.getSeedId(startSeedId);
                rec = new SubgoalDBRecord(start.id, goal.id, new int[]{seedId}, 0);
                result.add(rec);
                return result;
            }
            System.out.println("ERROR in findNearest.  Start seed: " + startSeedId + " Goal seed: " + goalSeedId);
            rec = recordMatrix[startSeedId][goalSeedId];
        } else result.add(recordMatrix[startSeedId][goalSeedId]);
        return result;
    }


    /**
     * Initializes the record matrix from the database records for querying.
     * The load populates the list of records but not the matrix lookup form.
     */
    public void init() {
        int numRegions = db.getNumRegions();
        // Fill in the record matrix to look up a record based on startSeedId, goalSeedId
        System.out.println("Creating lookup matrix of size: " + numRegions + " x " + numRegions + " = " + numRegions * numRegions);
        recordMatrix = new SubgoalDBRecord[numRegions][numRegions];
        for (SubgoalDBRecord rec : records) {
            int compositeGroupId = rec.getSearchDepth();
            int startId = compositeGroupId / 10000 - 1;
            int goalId = compositeGroupId % 10000;
            recordMatrix[startId][goalId] = rec;
        }
    }

    /**
     * Loads the record list and the mapping abstraction.
     */
    public boolean load(String fileName) {
        db = new IndexDB();
        boolean success = db.load(fileName + "i");
        if (!success) return false;
        db.buildHT();

        success = super.load(fileName);
        if (!success) return false;

        init();

        //System.out.println("Abstraction RLE: ");
        //db.print();
        return true;
    }

    /**
     * Given a search problem with all base states mapped to abstract state ids (either directly in the problem or using a SearchSpace object),
     * creates an RLE compressed index of the mapping to save space.
     */
    private void generateIndexDB() {
        db = new IndexDB();
        HashMap<Integer, Integer> distinctStates = new HashMap<>(5000);
        SearchState state = new SearchState();
        int lastStateVal = -1;
        int numStates = 0;
        // IDEA: Scan cells in order of index number.  When hit new cell that is in a different state than last, add entry to the DB.
        // NOTE: Using the state.cost variable to pass back the abstract state id.
        problem.initIterator();
        while (problem.nextState(state)) {
            numStates++;
            if (state.cost != lastStateVal) {
                db.add(state.id, (int) state.cost);
                lastStateVal = (int) state.cost;
            }
            if (!distinctStates.containsKey(lastStateVal)) distinctStates.put(lastStateVal, lastStateVal);
        }

        db.setTotalCells(numStates);
        db.setNumRegions(distinctStates.size());

        // Also store group (abstract region) id to group seed id (base state id of region representative) mapping
        TreeMap<Integer, GroupRecord> groups = problem.getGroups();
        Iterator<Entry<Integer, GroupRecord>> it = groups.entrySet().iterator();
        Entry<Integer, GroupRecord> e;
        int[][] groupsMapping = new int[groups.size()][2];
        int count = 0;
        while (it.hasNext()) {
            e = it.next();
            groupsMapping[count][0] = e.getKey() - GameMap.START_NUM;        // TODO: This is a hack and assume all group numbers start counting from whatever the START_NUM is (currently 50).
            groupsMapping[count++][1] = e.getValue().groupRepId;
        }
        db.setGroups(groupsMapping);
    }

    public void regenerateIndexDB(boolean isPartition, boolean isElimination, int regionId, int regionRepId, int numRegions, GameMap map, GroupRecord[] newRecs) {
        // TODO: need to update state.id to state.cost mapping
        int[][] groupsMapping = db.getGroups();
        // TODO: this matches the .dati2 AW completely now, even though it shouldn't
        setProblem(new MapSearchProblem(map));

        db = new IndexDB();
        HashMap<Integer, Integer> distinctStates = new HashMap<>(5000);
        SearchState state = new SearchState();
        int lastStateVal = -1;
        int numStates = 0;
        // IDEA: Scan cells in order of index number.  When hit new cell that is in a different state than last, add entry to the DB.
        // NOTE: Using the state.cost variable to pass back the abstract state id.
        problem.initIterator();
        while (problem.nextState(state)) {
            numStates++;
            if (state.cost != lastStateVal) {
                db.add(state.id, (int) state.cost);
                lastStateVal = (int) state.cost;
            }
            if (!distinctStates.containsKey(lastStateVal)) distinctStates.put(lastStateVal, lastStateVal);
        }

        db.setTotalCells(numStates);

        // db.setTotalCells(db.getTotalCells() - 1); // TODO: change this to # of walls
        db.setNumRegions(numRegions);

        // Do I need to shrink the array in the elimination case?
        // TODO: I think I will need to skip the wall here potentially
        if (isPartition) {
            if (groupsMapping.length < numRegions) {
                int[][] resizedGroupsMapping = new int[numRegions][];
                System.arraycopy(groupsMapping, 0, resizedGroupsMapping, 0, groupsMapping.length);
                groupsMapping = resizedGroupsMapping;
                for (GroupRecord newRec: newRecs) {
                    groupsMapping[newRec.groupId - GameMap.START_NUM]  = new int[]{newRec.groupId - GameMap.START_NUM, newRec.groupRepId};
                }
            }
        }

        if (isElimination) { // tombstone record
            groupsMapping[regionId - GameMap.START_NUM] = null;
            // db.setNumRegions(groupsMapping.length - 1);
        }

//        if (!(isElimination || isPartition)){ // update groupsMapping/groupsArr
//            groupsMapping[regionId - GameMap.START_NUM] = new int[]{regionId - GameMap.START_NUM, regionRepId};
//        }

        // write groupsArr back to db
        db.setGroups(groupsMapping);
    }

    /**
     * Verifies all subgoals in the record can be reached via hill-climbing from the previous one.
     * Verification is performed for all records.
     * Uses SubgoalDB verify to verify records and also verifies the index mapping.
     *
     * @param searchAlg
     */
    public void verify(SearchAlgorithm searchAlg) {
        super.verify(searchAlg);
        db.verify(problem);
    }

}
