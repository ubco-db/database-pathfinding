package search;

import map.GameMap;
import map.GroupRecord;
import util.ExpandArray;
import util.HeuristicFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * Supports searches of abstract regions used by PRA.
 *
 * @author rlawrenc
 */
public class RegionSearchProblem extends SearchProblem {
    private int[] numRegions; // Number of regions in each sector
    private int[][] edges; // edges[i][j] is an edge from region i to region j
    private GameMap map;
    private int[] regionCenter; // The node id of each region representative
    private int gridSize;

    private class Sector {
        public int number;
        public int numRegions;
        public ArrayList<Region> regions;
    }

    private class Region {
        public int regionId;
        public int regionRepId;
        public int sectorId;
        public int[] edges;
    }

    private ArrayList<Sector> sectors;
    private HashMap<Integer, Region> regions;

    public RegionSearchProblem(int[] numRegions, int[][] edges, int[] regionCenter, GameMap map, int gridSize) {
        HashMap<Integer, GroupRecord> groups = map.getGroups();
        regions = new HashMap<Integer, Region>();

        // Build the sector list
        sectors = new ArrayList<Sector>();
        int count = 50;
        for (int i = 0; i < numRegions.length; i++) {
            Sector s = new Sector();
            s.number = i;
            s.numRegions = numRegions[i];
            s.regions = new ArrayList<Region>(numRegions[i]);
            sectors.add(s);

            for (int j = 0; j < numRegions[i]; j++) {
                Region r = new Region();
                r.sectorId = i;
                r.regionRepId = groups.get(count).groupRepId;
                r.regionId = map.getCell(map.getRow(r.regionRepId), map.getCol(r.regionRepId)); // Equivalent to START_NUM
                // r.edges = Arrays.copyOf(edges[i], edges[i].length);
                if (edges[count - 50] == null) r.edges = null;
                else {
                    r.edges = new int[edges[count - 50].length];
                    for (int k = 0; k < r.edges.length; k++)
                        r.edges[k] = edges[count - 50][k] + 50;
                }
                regions.put(r.regionId, r);
                s.regions.add(r);
                count++;
            }
        }

        this.numRegions = numRegions;
        this.edges = edges;
        this.map = map;
        this.regionCenter = regionCenter;
        this.gridSize = gridSize;
    }

    public int computeDistance(SearchState start, SearchState goal) {
        return computeDistance(start.id, goal.id);
    }

    public int computeDistance(int startId, int goalId) {
        return GameMap.computeDistance(startId, goalId, map.cols);
    }

    private class Node {
        int id;
        Node prev;

        // ArrayList<Node> children;

        private Node(int id, Node prev) {
            this.id = id;
            this.prev = prev;
            // this.children = new ArrayList<Node>();
        }
    }

    public SearchState findRegion2(SearchState s, ArrayList<SearchState> pathStart, int startEnd) {
        // for end

        // Determine the sector that it is in
        // pathStart.add(0, new SearchState(s.id));

        int row = map.getRow(s.id);
        int col = map.getCol(s.id);
        int numSectorsPerRow = (int) Math.ceil(map.cols * 1.0 / gridSize);
        int sectorId = row / gridSize * numSectorsPerRow + col / gridSize;

        Sector sec = sectors.get(sectorId);

        SearchState regionState = new SearchState();

        if (sec.numRegions == 1) { // Only one region in sector - return its
            // representative
            Region reg = sec.regions.get(0);
            regionState.id = reg.regionRepId;
            regionState.stateData = new Integer(reg.regionId);
            return regionState;
        } else { // Have to search for which region representative this node is
            // in (BFS)
            // Check if state itself is a region representative
            for (int j = 0; j < sec.numRegions; j++) {
                if (s.id == sec.regions.get(j).regionRepId) {
                    regionState.id = sec.regions.get(j).regionRepId;
                    regionState.stateData = new Integer(sec.regions.get(j).regionId);
                    return regionState;
                }
            }

            // Not a direct match so do a BFS from state to find state
            // representative
            Queue<Integer> stateIds = new LinkedList<Integer>();
            ExpandArray neighbors = new ExpandArray(10);

            int maxr, maxc;
            int sectorRow = sectorId / numSectorsPerRow;
            int sectorCol = sectorId % numSectorsPerRow;
            maxr = sectorRow * gridSize + gridSize;
            if (maxr > map.rows) maxr = map.rows;
            maxc = sectorCol * gridSize + gridSize;
            if (maxc > map.cols) maxc = map.cols;

            stateIds.add(s.id);
            HashSet<Integer> visited = new HashSet<Integer>();
            visited.add(s.id);
            // String gd = "";
            ArrayList<Node> tree = null;
            Node curr = null;
            while (!stateIds.isEmpty()) {
                int id = stateIds.remove();
                if (curr == null) {
                    tree = new ArrayList<Node>();
                    curr = new Node(id, null);
                    tree.add(0, curr);
                } else {
                    int i = 0;
                    while (tree.get(i).id != id) {
                        i++;
                    }
                    curr = tree.get(i);
                }
                /*
                 * System.out.println("Node:(" + map.getRow(id) + ", " +
                 * map.getCol(id) + ")"); gd += "\t";
                 */

                row = map.getRow(id);
                col = map.getCol(id);

                // Generate neighbors and add to list if in region
                map.getNeighbors(row, col, neighbors);

                for (int n = 0; n < neighbors.num(); n++) {
                    int nid = neighbors.get(n);
                    int nr = map.getRow(nid);
                    int nc = map.getCol(nid);
                    Node newNode = null;
                    if (map.isInRange(nr, nc, maxr, maxc, gridSize)) {
                        if (!visited.contains(nid)) {
                            newNode = new Node(nid, curr);
                            tree.add(0, newNode);
                            /*
                             * System.out.println(gd + "Node:(" +
                             * map.getRow(nid) + ", " + map.getCol(nid) + ")");
                             */
                            stateIds.add(nid);
                            visited.add(nid);
                        }

                        // See if match a region id
                        for (int j = 0; j < sec.numRegions; j++) {
                            if (nid == sec.regions.get(j).regionRepId) {
                                regionState.id = sec.regions.get(j).regionRepId;
                                regionState.stateData = new Integer(sec.regions.get(j).regionId);
                                if (startEnd == 0) {
                                    while (newNode.prev != null) {
                                        pathStart.add(0, new SearchState(newNode.id));
                                        newNode = newNode.prev;
                                    }
                                    pathStart.add(0, new SearchState(newNode.id));
                                    return regionState;
                                } else {
                                    while (newNode.prev != null) {
                                        pathStart.add(new SearchState(newNode.id));
                                        newNode = newNode.prev;
                                    }
                                    pathStart.add(new SearchState(newNode.id));
                                    return regionState;
                                }

                            }
                        }
                    }
                }// gd = gd.substring(0, gd.length()-1);
            }// gd += "\t";
        }
        return null;
    }

    public ArrayList<SearchState> getNeighbors(SearchState state) {
        ArrayList<SearchState> res = new ArrayList<SearchState>();

        // Find what group the state is in
        // int groupId = (Integer) state.stateData;
        /*
         * HashMap<Integer, GroupRecord> groups = map.getGroups();
         * Iterator<Map.Entry<Integer, GroupRecord>> it =
         * groups.entrySet().iterator();
         *
         * GroupRecord saved = null; while (it.hasNext()) { GroupRecord rec =
         * it.next().getValue(); if (rec.groupRepId == state.id) saved = rec; }
         *
         * if (saved == null) {
         * System.out.println("Unable to find group state: "+state); return
         * null; } int groupId = saved.groupId;
         */
        /*
         * HashMap<Integer, GroupRecord> groups = map.getGroups(); if
         * (edges[groupId] != null) { for (int i=0; i < edges[groupId].length;
         * i++) { int toGroupId = edges[groupId][i]; GroupRecord rec =
         * groups.get(toGroupId); int toGroupRepId = rec.groupRepId; SearchState
         * s = new SearchState(toGroupRepId); s.stateData = new
         * Integer(toGroupId); res.add(s); } }
         */

        Region reg = regions.get(state.stateData);
        for (int i = 0; i < reg.edges.length; i++) {
            int toRegionId = reg.edges[i];
            Region toRegion = regions.get(toRegionId);
            SearchState s = new SearchState(toRegion.regionRepId);
            s.stateData = new Integer(toRegionId);
            res.add(s);
        }
        return res;
    }

    public int getMaxSize() {
        return numRegions.length;
    }

    public void initIterator() {
        // stateId = 0;
    }

    public boolean nextState(SearchState state) {
        return getNextState(state);
    }

    private boolean getNextState(SearchState state) {
        /*
         * if (stateId >= graph.size()) return false;
         *
         * state.cost = searchSpace.getState(stateId)-GameMap.START_NUM;
         * state.id = stateId++; return true;
         */
        return false;
    }

    public SearchState generateRandomState(Random generator) {
        // return new SearchState(generator.nextInt(graph.size()));
        return null;
    }

    public String idToString(int id) {
        return "" + id;
    }

    public HashMap<Integer, GroupRecord> getGroups() {
        return this.searchSpace.getGroups();
    }

    public void computeNeighbors() {
        this.searchSpace.computeNeighbors();
    }

    public int getMoveCost(int startId, int goalId) {
        if (startId == goalId) return 0;
        return computeDistance(startId, goalId);
    }

    public int getMoveCost(SearchState start, SearchState goal) {
        return getMoveCost(start.id, goal.id);
    }

    public void getNeighbors(int stateId, ExpandArray neighbors) {
        ArrayList<SearchState> neighborList = getNeighbors(new SearchState(stateId));
        neighbors.clear();
        for (SearchState searchState : neighborList) neighbors.add(searchState.id);
    }

    public boolean isNeighbor(int fromStateId, int toStateId) { // TODO:
        // Probably can
        // do this
        // faster
        // without
        // computing all
        // the
        // neighbors?
        ArrayList<SearchState> neighborList = getNeighbors(new SearchState(
                fromStateId));
        for (SearchState searchState : neighborList)
            if (searchState.id == toStateId)
                return true;
        return false;
    }

    @Override
    public int computeDistance(SearchState start, SearchState goal, HeuristicFunction heuristic) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int computeDistance(int startId, int goalId, HeuristicFunction heuristic) {
        // TODO Auto-generated method stub
        return 0;
    }

}
