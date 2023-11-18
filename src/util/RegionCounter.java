package util;

public class RegionCounter {
    private static final int[][] DIRECTIONS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1}, /*{0, 0},*/ {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };

    public int countRegions(int[][] coordinates) {
        if (coordinates == null || coordinates.length == 0 || coordinates[0].length == 0) {
            return 0;
        }

        int rows = coordinates.length;
        int cols = coordinates[0].length;
        boolean[][] visited = new boolean[rows][cols];
        int regions = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (!visited[i][j]) {
                    dfs(coordinates, visited, i, j);
                    regions++;
                }
            }
        }
        return regions;
    }

    private void dfs(int[][] coordinates, boolean[][] visited, int row, int col) {
        int rows = coordinates.length;
        int cols = coordinates[0].length;

        visited[row][col] = true;

        for (int[] dir : DIRECTIONS) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols
                    && !visited[newRow][newCol]) {
                // Check if the neighboring coordinates are within bounds and unvisited
                dfs(coordinates, visited, newRow, newCol);
            }
        }
    }
}
