package util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class MapHelpers {
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
}
