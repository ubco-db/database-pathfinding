package search;

import database.DBStatsRecord;
import map.GameMap;
import map.GroupRecord;
import util.CircularQueue;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Abstract a search space into a set of ids (no edges).  The edges are computed dynamically by invoking the corresponding problem.
 *
 * @author rlawrenc
 */
public class SearchSpace {
    public static int EMPTY_CHAR = 0;

    private SearchProblem problem;
    private TreeMap<Integer, GroupRecord> groups;
    private int numAbstractStates, numStates;
    private int[] states;
    private Random generator;
    private HashMap<Integer, Color> colors;

    public SearchSpace(SearchProblem problem) {
        this.problem = problem;
        colors = new HashMap<Integer, Color>(100);
        generator = new Random();
        numStates = problem.getMaxSize();
        states = new int[numStates + 1];
    }

    public int getState(int id) {
        return states[id];
    }

    /**
     * Creates and stores an abstraction of the search space mapping each state id to a seed id.
     *
     * @param searchAlg
     * @param dbstat
     */
    public void reachableAbstract(SearchAbstractAlgorithm searchAlg, DBStatsRecord dbstat) {
        // Idea: Keep adding squares to current one until no longer all elements are reachable in greedy fashion
        int currentNum = 0;
        GroupRecord group;

        groups = new TreeMap<Integer, GroupRecord>();

        long currentTime = System.currentTimeMillis();

        // Phase 1: Just do 10,000 random states
        int numSeeds = 10000;
        SearchState seed = null;

        for (int i = 0; i < numSeeds; i++) {
            seed = problem.generateRandomState(generator);
            if (states[seed.id] != EMPTY_CHAR) continue;
            group = expandSpot(seed, currentNum, searchAlg, null);
            groups.put(group.groupId, group);
            currentNum++;
        }

        // Phase 2: Handle any other states not previously found (need to iterate through the space).
        problem.initIterator();
        while (problem.nextState(seed)) {
            if (states[seed.id] != EMPTY_CHAR) continue;
            group = expandSpot(seed, currentNum, searchAlg, null);
            groups.put(group.groupId, group);
            currentNum++;
        }

        numAbstractStates = currentNum;
        System.out.println("Number of areas: " + currentNum);
        long endTime = System.currentTimeMillis();
        System.out.println("Time to compute abstraction: " + (endTime - currentTime));
        dbstat.addStat(12, endTime - currentTime);
        dbstat.addStat(11, currentNum);
        dbstat.addStat(7, currentNum);
    }

//    /**
//     * Given this search space, performs clique abstraction one more level.
//     * If no abstraction is performed, does initial level 1 abstraction (finding cliques of base states).
//     *
//     * @return
//     */
//    public SearchSpace cliqueAbstract(DBStatsRecord dbstat) {
//        // Create new abstract search space (the abstraction result)
//        SearchSpace abstractSpace = new SearchSpace(problem);
//        problem.setSearchSpace(this);
//        abstractSpace.colors = this.colors;
//        int currentNum = GameMap.START_NUM;
//        GroupRecord group;
//        SearchState currentState = new SearchState();
//        abstractSpace.groups = new TreeMap<Integer, GroupRecord>();
//        ExpandArray neighbors = new ExpandArray(10);
//        ExpandArray neighborsAddNode = new ExpandArray(10);
//        ExpandArray clique = new ExpandArray(100);
//        ExpandArray groupNeighbors = new ExpandArray(1000);
//
//        long currentTime = System.currentTimeMillis();
//        int totalSize = 0;
//
//        if (this.groups == null || this.groups.size() == 0) {    // No abstraction (or grouping) has been performed yet on this search problem.  Perform first level of abstraction
//            // Iterate through the ResultSet building cliques of base states
//
//            problem.initIterator();
//            while (problem.nextState(currentState)) {
//                if (abstractSpace.states[currentState.id] != 0) continue;
//                // Calculate clique here.  Get all neighbors.
//                problem.getNeighbors(currentState.id, neighbors);
//                // System.out.println("State id: "+currentState.id+" Neighbors: "+neighbors);
//                groupNeighbors.clear();
//                groupNeighbors.addAll(neighbors);        // All neighbors of this node added to clique will be neighbors of the group.
//                // The state and all its neighbors are not necessarily a clique as the neighbors may not be mutually reachable.
//                // IDEA: Add start state and a neighbor at a time always checking to see if still a clique.
//                clique.clear();
//                clique.add(currentState.id);
//                boolean inClique = true;
//                for (int i = 0; i < neighbors.num(); i++) {
//                    int addNodeId = neighbors.get(i);
//                    if (abstractSpace.states[addNodeId] != 0) continue;
//                    // Check if all nodes currently in clique have an edge with this one that we are currently trying to add (are neighbors)
//                    for (int j = 0; j < clique.num(); j++) {
//                        if (!problem.isNeighbor(addNodeId, clique.get(j))) {
//                            inClique = false;
//                            break;
//                        }
//                    }
//                    if (inClique) {    // Add state to clique
//                        clique.add(addNodeId);
//                        problem.getNeighbors(addNodeId, neighborsAddNode);
//                        groupNeighbors.addAll(neighborsAddNode);
//                    }
//                }
//
//                group = new GroupRecord();
//                group.groupId = currentNum;
//                group.groupRepId = currentState.id;
//                group.states = new ArrayList<Integer>(clique);
//                group.setNumStates(clique.num());
//                group.setNeighbors(groupNeighbors);
//                abstractSpace.groups.put(group.groupId, group);
//                // Update the states array to track states that have been assigned to groups already
//                for (int k = 0; k < clique.num(); k++) {
//                    int stateid = clique.get(k);
//                    abstractSpace.states[stateid] = currentNum;
//                }
//                // System.out.println("Added group: "+group);
//                currentNum++;
//                totalSize += group.getSize();
//            }
//        } else {    // Perform level 2 or higher abstraction
//            // IDEA: Iterate through the groups.  Find all neighbors for all states in the group.  Any of those are then reachable.
//            boolean inClique;
//
//            for (Integer integer : this.groups.keySet()) {
//                GroupRecord g = groups.get(integer);
//                if (abstractSpace.states[g.groupRepId] != 0)
//                    continue;    // Group has already been merged with another group
//                GroupRecord newGroup = new GroupRecord();
//                newGroup.groupId = currentNum;
//                newGroup.groupRepId = g.groupRepId;
//                newGroup.states = new ArrayList<Integer>(g.states);
//                newGroup.setNumStates(g.getSize());
//                groupNeighbors.clear();
//                groupNeighbors.addAll(g.getNeighborIds());
//                newGroup.setNeighbors(groupNeighbors);
//                ArrayList<GroupRecord> groupsInClique = new ArrayList<GroupRecord>();
//                groupsInClique.add(g);
//                // System.out.println(" Started this group: "+g);
//                // 	Iterate through all neighbor nodes of this group.
//                HashSet<Integer> n = g.getNeighborIds();
//                for (int neighborId : n) {
//                    int neighborGroupId = states[neighborId];
//                    GroupRecord neighborGroup = groups.get(neighborGroupId);
//                    if (neighborGroupId == g.groupId || abstractSpace.states[neighborId] != 0)
//                        continue;                        // Node is already in the group or group has already been merged with another
//                    // Merge this group into the new group as long as it is connected to all members currently in the group
//                    // See if all groups currently in clique are neighbors with this one
//                    inClique = true;
//                    for (GroupRecord gInClique : groupsInClique) {
//                        if (!gInClique.isNeighbor(neighborGroup)) {
//                            inClique = false;
//                            break;
//                        }
//                    }
//                    if (inClique) {    // Add group to clique
//                        groupsInClique.add(neighborGroup);
//                        groupNeighbors.addAll(neighborGroup.getNeighborIds());
//                        newGroup.states.addAllDistinct(neighborGroup.states);
//                        // System.out.println(" Merged this group: "+neighborGroup);
//                    }
//                }    // end while
//
//                // Handle the case where the abstract state is merged with no one yet.  Merge it with someone even though it is not a clique.
//                if (groupsInClique.size() == 1) {    // Merge with one of its other groups
//                    // Find a neighbor group to merge with
//                    // System.out.println("Special case.   Merging group with no clique neighbors: "+newGroup);
//                    Iterator<Integer> it = n.iterator();
//                    GroupRecord neighborGroup;
//                    while (it.hasNext()) {
//                        int neighborId = it.next();
//                        int neighborGroupId = abstractSpace.states[neighborId];
//                        neighborGroup = abstractSpace.groups.get(neighborGroupId);  // Retrieve the group for this neighbor
//                        if (neighborGroupId == g.groupId || neighborGroup == null) continue;
//                        int prevNum = neighborGroup.getSize();
//                        neighborGroup.setNumStates(prevNum + g.getSize());
//                        groupNeighbors.addAll(neighborGroup.getNeighborIds());
//                        neighborGroup.setNeighbors(groupNeighbors);
//                        neighborGroup.states.addAll(newGroup.states);
//
//                        // Update the states array to track states that have been assigned to groups already
//                        for (int k = 0; k < newGroup.states.num(); k++) {
//                            int stateid = newGroup.states.get(k);
//                            abstractSpace.states[stateid] = neighborGroupId;
//                        }
//                        totalSize += g.getSize();
//                        // System.out.println("Merged into group: "+neighborGroup);
//                        break;    // Only need to merge with one group
//                    }
//                } else {
//                    // Now add clique of groups as new group
//                    newGroup.setNumStates(newGroup.states.num());
//                    newGroup.setNeighbors(groupNeighbors);
//                    abstractSpace.groups.put(newGroup.groupId, newGroup);
//                    // Update the states array to track states that have been assigned to groups already
//                    for (int k = 0; k < newGroup.states.num(); k++) {
//                        int stateid = newGroup.states.get(k);
//                        abstractSpace.states[stateid] = newGroup.groupId;
//                    }
//                    // System.out.println("Added group: "+newGroup);
//                    currentNum++;
//                    totalSize += newGroup.getSize();
//                }
//            }
//        }
//
//        abstractSpace.setNumAbstractStates(abstractSpace.getGroups().size());
//        System.out.println("Number of areas: " + abstractSpace.getNumAbstractStates() + " Total size: " + totalSize);
//        long endTime = System.currentTimeMillis();
//        System.out.println("Time to compute abstraction: " + (endTime - currentTime));
//        dbstat.addStat(12, endTime - currentTime);
//        dbstat.addStat(11, currentNum);
//        dbstat.addStat(7, currentNum);
//
//        return abstractSpace;
//    }

    public GroupRecord expandSpot(SearchState seed, int currentNum, SearchAbstractAlgorithm searchAlg, SavedSearch database) {
        BitSet currentSet = new BitSet(numStates);                // TODO: This is a major memory consumer - but clearing seems to be slower?  True?
        CircularQueue expandSet = new CircularQueue(1000);

        GroupRecord group = new GroupRecord();

        // Pick a color and assign group id to start square
        currentNum++;
        Color color = new Color(generator.nextFloat(), generator.nextFloat(), generator.nextFloat());
        colors.put(currentNum, color);
        int currentCh = 49 + currentNum;
        states[seed.id] = currentCh;
        int curPos = 0;
        group.groupId = currentCh;
        int id, seedId = seed.id;
        group.setGroupRepId(seedId);
        ArrayList<SearchState> neighbors;
        StatsRecord stats = new StatsRecord();
        currentSet.set(seedId);
        expandSet.insert(seedId);


        while ((id = expandSet.remove()) != -1) {
            // Expand all neighbors
            neighbors = problem.getNeighbors(new SearchState(id));
            for (SearchState next : neighbors) {
                id = next.id;
                if (!currentSet.get(id)) {        // Mutual reachable check below
					/*
		
					newState = new SearchState(id);
					// cost1 = searchAlg.isPath(newState, seed, stats, database);
					// if (cost1 == -1)	continue;
					if (!searchAlg.isPath(newState, seed, stats))
						continue;
					if (!searchAlg.isPath(seed, newState, stats))
						continue;
					*/
                    if (!searchAlg.isPath(id, seedId, stats)) continue;
                    if (!searchAlg.isPath(seedId, id, stats)) continue;

                    // System.out.println("\t\tAdded: "+problem.idToString(id));
                    // database.setFound(id,cost1);		// Store cost to get to the goal from this cell
                    // expandSet.insert(id);	// This would expand always whether or not the cell is closer.
                    // currentSet.set(id);			// Potentially better solution but at much higher abstraction cost.
                    if (states[id] == SearchSpace.EMPTY_CHAR || isCloser(searchAlg, id, seedId))        // Only change owner if new one is closer
                    {
                        states[id] = currentCh;
                        expandSet.insert(id);        // Only expand if closer
                        currentSet.set(id);
                    }
                }
            }
            curPos++;
        }
        return group;
    }

    private boolean isCloser(SearchAlgorithm searchAlg, int id, int newSeedId) {
        int currentSeedId = states[id];
        GroupRecord record = groups.get(currentSeedId);            // TODO: Major memory consumer.  Can we replace HashMap with a no object data sturcture?
        // Also using heuristic distance is flawed this should be actual distance here.
        return (problem.computeDistance(id, newSeedId) < problem.computeDistance(id, record.getGroupRepId()));
    }

    private Color getColor(int val) {
        if (val == GameMap.WALL_CHAR) return Color.BLACK;
        else if (val == SearchSpace.EMPTY_CHAR) return Color.WHITE;
        else {    // Lookup color used for this number
            Color color = (Color) colors.get(val);
            if (color == null) {    // TODO: How to pick color.  Picking a random color for now.
                // Should not go in here with map is pre-colored.
                // System.out.println("Here in for value: "+val);
                color = new Color(generator.nextFloat(), generator.nextFloat(), generator.nextFloat());
                colors.put(val, color);
            }
            return color;
        }
    }

    /*
     * Outputs search space as 2D matrix.
     */
    public void outputImage(String fileName, ArrayList<SearchState> path, ArrayList<SearchState> subgoals, ArrayList<SearchState> seeds) {
        int val, cols = (int) Math.sqrt(numStates);
        int scale = 1000 / cols;
        if (scale < 1) scale = 1;
        BufferedImage bufferedImage = new BufferedImage(cols * scale, cols * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        SearchState s;

        int row = 0, col = 0, count = 0;
        for (int i = 0; i < numStates; i++) {
            if (count == cols) { // Next row
                col = 0;
                row++;
                count = 0;
            }

            val = states[i];
            g2.setColor(getColor(val));
            g2.fillRect(row * scale, col * scale, scale, scale);
            col++;
            count++;
        }

        if (seeds != null) {
            g2.setColor(Color.WHITE);
            for (int i = 0; i < seeds.size(); i++) {
                s = seeds.get(i);
                val = states[i];
                col = s.id % cols;
                row = s.id / cols;
                if (scale > 1) g2.fillRect(row * scale + scale / 3, col * scale + scale / 3, scale / 3, scale / 3);
                else g2.fillRect(row, col, 1, 1);
            }
        }
	    
		/*
		if (path != null)
		{	// Make a mask for the map for the path
			SparseMask currentMask = new SparseMask();			
			Color color, pathColor = Color.RED;	
			HashMap<String,String> used = new HashMap<String,String>();
			
			for (int i=0; i < path.size(); i++)
			{	SearchState current = path.get(i);
				int row = getRow(current.id);
				int col = getCol(current.id);										
				
				ChangeRecord rec;
				if (i == 0)
				{	color = Color.green;
					rec= new ChangeRecord(row, col, color, 1);
					currentMask.add(rec);
					if (largeMap)		// Make start and end more visible
					{	for (int k=-width; k <=width; k++)
							for (int l=-width; l <=width; l++)
							{	if (!isWall(row+k,col+l))
								{	rec= new ChangeRecord(row+k, col+l, color, 1);
									currentMask.add(rec);
								}
							}					
					}
				}
				else if (i == path.size()-1)
				{	color = Color.blue;
					rec= new ChangeRecord(row, col, color, 1);
					currentMask.add(rec);
					if (largeMap)		// Make start and end more visible
					{	for (int k=-width; k <=width; k++)
							for (int l=-width; l <=width; l++)
							{	if (!isWall(row+k,col+l))
								{	rec= new ChangeRecord(row+k, col+l, color, 1);
									currentMask.add(rec);
								}
							}					
					}
				}
				else
				{	color = pathColor;
					if (subgoals != null && subgoals.contains(current))
						color = Color.ORANGE;	// Show subgoals in a different color
					rec= new ChangeRecord(row, col, color, 1);
					if (used.containsKey(rec.toString()))
						continue;
					currentMask.add(rec);
					used.put(rec.toString(), null);
				}
			}
			// Special case for subgoals:
			for (int i=0; subgoals != null && i < subgoals.size(); i++)
			{	SearchState current = subgoals.get(i);
				int row = getRow(current.id);
				int col = getCol(current.id);												
							
				ChangeRecord rec;		
				color = Color.ORANGE;	// Show subgoals in a different color
				rec= new ChangeRecord(row, col, color, 1);
				if (used.containsKey(rec.toString()))
					continue;
				currentMask.add(rec);
				used.put(rec.toString(), null);			
			}
			addMask(currentMask);
			this.currentMask = this.masks.size()-1;
		}
		*/

        // Write generated image to a file
        try {
            // Save as PNG
            File file = new File(fileName);
            ImageIO.write(bufferedImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        g2.dispose();
    }

    public int getNumAbstractStates() {
        return numAbstractStates;
    }

    public void setNumAbstractStates(int numAbstractStates) {
        this.numAbstractStates = numAbstractStates;
    }

    public int getNumStates() {
        return numStates;
    }

    public void setNumStates(int numStates) {
        this.numStates = numStates;
    }


    public void save(String fileName) {
        try (PrintWriter out = new PrintWriter(fileName)) {
            out.println("states " + numStates);

            for (int i = 0; i < numStates; i++) {
                out.print(states[i]);
                out.print("\t");
                if (i % 1000 == 0) out.println();
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error with output file: " + e);
        }
    }

    public ArrayList<SearchState> getSeeds() {
        ArrayList<SearchState> seeds = new ArrayList<SearchState>(groups.size());

        for (GroupRecord g : groups.values()) {
            seeds.add(new SearchState(g.groupRepId));
        }
        return seeds;
    }

    public TreeMap<Integer, GroupRecord> getGroups() {
        return groups;
    }

    public void computeNeighbors() {    // Only computes the neighbor group ids for each group not the list of neighbor cells
        // IDEA: Perform one pass through states updating group records every time encounter new neighbor

        // Create a neighbor set for each group
        for (Map.Entry<Integer, GroupRecord> integerGroupRecordEntry : groups.entrySet()) {
            GroupRecord rec = integerGroupRecordEntry.getValue();
            rec.setNeighborIds(new HashSet<Integer>());
        }

        SearchState s, state = new SearchState();

        long currentTime = System.currentTimeMillis();
        ArrayList<SearchState> neighbors = new ArrayList<SearchState>();
        int val;

        for (int i = 0; i < numStates; i++) {
            val = this.states[i];
            GroupRecord rec = groups.get(val);

            // Get all neighbors for this state and see what group they are in
            state.id = i;
            neighbors = problem.getNeighbors(state);
            for (SearchState neighbor : neighbors) {
                s = neighbor;
                if (states[s.id] != val) rec.getNeighborIds().add(states[s.id]);
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Time to compute neighbors: " + (endTime - currentTime));
    }
}
