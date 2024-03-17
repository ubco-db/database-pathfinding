package util;

import map.GameMap;
import search.SearchState;

import java.util.*;

public final class MapHelpers {

    private MapHelpers() throws Exception {
        throw new Exception("This is a utility class and should not be instantiated.");
    }

    public static boolean isContinuousWall(int n1, int n2) {
        // if n1 and n2 is are walls, adding a wall between them leads to a continuous wall segment
        // this assumes n1 and n2 are across from each other
        return n1 == '*' && n2 == '*';
    }

    public static boolean isBetweenWallAndOtherRegion(int n1, int n2, int r) {
        // if n1 is a wall and n2 is a different region or vice versa, the wall addition between them may constitute a partition
        // this assumes n1 and n2 are across from each other
        return n1 == '*' && n2 != r || n1 != r && n2 == '*';
    }

    public static boolean isOpenDiagonal(int n1, int nDiag, int n2) {
        // if n1 and n2 are walls, but nDiag is not, placing a new wall may make it impossible to reach nDiag
        return n1 == '*' && n2 == '*' && nDiag != '*';
    }


    public static boolean isUniqueTouchPoint(int n1, int nDiag, int n2) {
        // if n1 and n2, and nDiag are all different regions, placing a wall may make it so nDiag's region becomes unreachable
        return n1 != n2 && n2 != nDiag;
    }

    public static boolean isValid(int[][] map, boolean[][] visited, int row, int col, int r) {
        int rows = map.length;
        int cols = map[0].length;

        // checking map[row][col] == r to ensure we are still in the region
        return row >= 0 && row < rows && col >= 0 && col < cols && map[row][col] != '*' && map[row][col] == r && !visited[row][col];
    }

    // TODO: could likely optimize using A* (also, can I trust this code?)
    public static boolean isPathPossible(int[][] map, int[] start, int[] end, int r) {
        int rows = map.length;
        int cols = map[0].length;

        boolean[][] visited = new boolean[rows][cols];

        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {-1, 1}, {1, -1}, {1, 1}, {-1, -1}};

        Queue<int[]> queue = new LinkedList<>();
        queue.add(start);
        visited[start[0]][start[1]] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();

            if (Arrays.equals(current, end)) {
                return true;  // Path exists
            }

            for (int[] dir : directions) {
                int newRow = current[0] + dir[0];
                int newCol = current[1] + dir[1];

                if (isValid(map, visited, newRow, newCol, r)) {
                    visited[newRow][newCol] = true;
                    queue.add(new int[]{newRow, newCol});
                }
            }
        }

        return false;  // No path exists
    }

    /**
     * @param map        GameMap object
     * @param neighbours the eight squares touching the state to find region id for
     * @param sectorId   sector number of state to find region id for
     * @return region id
     * @throws Exception if no neighbours are found
     */
    public static int getRegionIdFromNeighbourStates(GameMap map, ArrayList<SearchState> neighbours, int sectorId, int gridSize) throws Exception {
        for (SearchState neighbour : neighbours) {
            // Need to use !isWall instead of isOpenCell, because the cells are not empty, they have their regions written into them
            if (!map.isWall(neighbour.id) && map.findSectorId(neighbour.id) == sectorId) {
                return map.squares[map.getRow(neighbour.id)][map.getCol(neighbour.id)];
            }
        }
        throw new Exception("No neighbours to extrapolate region id from!");
    }

    /**
     * @param map                 GameMap object
     * @param neighbours          the eight squares touching the state
     * @param openStatesToSectors empty HashMap
     * @return true if all 8 neighbours of a state are walls, otherwise false
     */
    public static boolean isSurroundedByWalls(GameMap map, ArrayList<SearchState> neighbours, Map<Integer, Integer> openStatesToSectors, int gridSize) {
        // Return true if all 8 neighbours of the cell are walls, else return false

        for (SearchState neighbour : neighbours) {
            // Need to use !isWall instead of isOpenCell, because the cells are not empty, they have their regions written into them
            if (!map.isWall(neighbour.id)) {
                // Fill HashMap with state id to sector id mapping
                openStatesToSectors.put(neighbour.id, map.findSectorId(neighbour.id));
            }
        }

        return openStatesToSectors.isEmpty();
    }

    public static Set<Integer> getRegionsTouchingWall(GameMap map, ArrayList<SearchState> neighbours) {
        Set<Integer> regionsTouchingWall = new HashSet<>();
        for (SearchState neighbour : neighbours) {
            regionsTouchingWall.add(map.squares[map.getRow(neighbour.id)][map.getCol(neighbour.id)]);
        }
        return regionsTouchingWall;
    }

    public static boolean isPathEqual(ArrayList<SearchState> newPath, ArrayList<SearchState> oldPath) {
        // if path length differs, they are not equal
        if (newPath == null || oldPath == null)
            return false;
        if (newPath.size() != oldPath.size()) return false;

        for (int i = 0; i < newPath.size(); i++) {
            // comparing SearchStates (have an equals-method)
            if (!newPath.get(i).equals(oldPath.get(i))) return false;
        }
        return true;
    }

    public static double getPathDiff(ArrayList<SearchState> newPath, ArrayList<SearchState> oldPath) {
        // Convert ArrayLists to sets
        Set<Integer> set1 = new HashSet<>();
        for (SearchState s : newPath) {
            set1.add(s.getId());
        }

        Set<Integer> set2 = new HashSet<>();
        for (SearchState s : oldPath) {
            set2.add(s.getId());
        }

        // Calculate intersection and union
        Set<Integer> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<Integer> union = new HashSet<>(set1);
        union.addAll(set2);

        // Calculate Jaccard similarity coefficient
        double jaccardSimilarity = (double) intersection.size() / union.size();

        // Return the percentage difference
        return (1 - jaccardSimilarity) * 100;
    }
}
