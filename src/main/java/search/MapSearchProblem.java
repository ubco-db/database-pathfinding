package search;

import map.GameMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MapSearchProblem extends SearchProblem {
    private final GameMap gameMap;

    public MapSearchProblem(GameMap gameMap) {
        this.gameMap = gameMap;
    }

    public static List<SearchState> getOpenStateList(GameMap gameMap) {
        List<SearchState> openStates = new ArrayList<>(gameMap.getNumOpenStates());

        for (int r = 0; r < gameMap.getNumRows(); r++) {
            for (int c = 0; c < gameMap.getNumCols(); c++) {
                if (!gameMap.isWall(r, c)) {
                    openStates.add(new SearchState(gameMap.getStateId(r, c)));
                }
            }
        }

        return openStates;
    }

    @Override
    public void getNeighbours(SearchState currentState, List<SearchState> neighbours) {
        if (!neighbours.isEmpty()) {
            throw new RuntimeException();
        }

        int stateId = currentState.getStateId();

        int row = gameMap.getRowFromStateId(stateId);
        int col = gameMap.getColFromStateId(stateId);

        boolean isOpenNorth = false, isOpenEast = false, isOpenSouth = false, isOpenWest = false;

        if (gameMap.isInBoundsAndNotWall(row - 1, col)) { // north
            neighbours.add(new SearchState(gameMap.getStateId(row - 1, col)));
            isOpenNorth = true;
        }
        if (gameMap.isInBoundsAndNotWall(row, col + 1)) { // east
            neighbours.add(new SearchState(gameMap.getStateId(row, col + 1)));
            isOpenEast = true;
        }
        if (gameMap.isInBoundsAndNotWall(row + 1, col)) { // south
            neighbours.add(new SearchState(gameMap.getStateId(row + 1, col)));
            isOpenSouth = true;
        }
        if (gameMap.isInBoundsAndNotWall(row, col - 1)) { // west
            neighbours.add(new SearchState(gameMap.getStateId(row, col - 1)));
            isOpenWest = true;
        }

        // Diagonal states are only open if either of the corresponding cardinal states are open
        if ((isOpenNorth || isOpenEast) && gameMap.isInBoundsAndNotWall(row - 1, col + 1)) { // north-east
            neighbours.add(new SearchState(gameMap.getStateId(row - 1, col + 1)));
        }
        if ((isOpenSouth || isOpenEast) && gameMap.isInBoundsAndNotWall(row + 1, col + 1)) { // south-east
            neighbours.add(new SearchState(gameMap.getStateId(row + 1, col + 1)));
        }
        if ((isOpenSouth || isOpenWest) && gameMap.isInBoundsAndNotWall(row + 1, col - 1)) { // south-west
            neighbours.add(new SearchState(gameMap.getStateId(row + 1, col - 1)));
        }
        if ((isOpenNorth || isOpenWest) && gameMap.isInBoundsAndNotWall(row - 1, col - 1)) { // north-west
            neighbours.add(new SearchState(gameMap.getStateId(row - 1, col - 1)));
        }
    }

    @Override
    public int getNeighbours(SearchState currentState, SearchState[] neighbours) {
        int i = 0;

        int stateId = currentState.getStateId();

        int row = gameMap.getRowFromStateId(stateId);
        int col = gameMap.getColFromStateId(stateId);

        boolean isOpenNorth = false, isOpenEast = false, isOpenSouth = false, isOpenWest = false;

        if (gameMap.isInBoundsAndNotWall(row - 1, col)) { // north
            neighbours[i++] = new SearchState(gameMap.getStateId(row - 1, col));
            isOpenNorth = true;
        }
        if (gameMap.isInBoundsAndNotWall(row, col + 1)) { // east
            neighbours[i++] = new SearchState(gameMap.getStateId(row, col + 1));
            isOpenEast = true;
        }
        if (gameMap.isInBoundsAndNotWall(row + 1, col)) { // south
            neighbours[i++] = new SearchState(gameMap.getStateId(row + 1, col));
            isOpenSouth = true;
        }
        if (gameMap.isInBoundsAndNotWall(row, col - 1)) { // west
            neighbours[i++] = new SearchState(gameMap.getStateId(row, col - 1));
            isOpenWest = true;
        }

        // Diagonal states are only open if either of the corresponding cardinal states are open
        if ((isOpenNorth || isOpenEast) && gameMap.isInBoundsAndNotWall(row - 1, col + 1)) { // north-east
            neighbours[i++] = new SearchState(gameMap.getStateId(row - 1, col + 1));
        }
        if ((isOpenSouth || isOpenEast) && gameMap.isInBoundsAndNotWall(row + 1, col + 1)) { // south-east
            neighbours[i++] = new SearchState(gameMap.getStateId(row + 1, col + 1));
        }
        if ((isOpenSouth || isOpenWest) && gameMap.isInBoundsAndNotWall(row + 1, col - 1)) { // south-west
            neighbours[i++] = new SearchState(gameMap.getStateId(row + 1, col - 1));
        }
        if ((isOpenNorth || isOpenWest) && gameMap.isInBoundsAndNotWall(row - 1, col - 1)) { // north-west
            neighbours[i++] = new SearchState(gameMap.getStateId(row - 1, col - 1));
        }

        return i;
    }

    @Override
    public void getNeighbourIds(int currentId, List<Integer> neighbourIds) {
        gameMap.getStateNeighbourIds(currentId, neighbourIds);
    }

    @Override
    public int getNeighbourIds(int currentId, int[] neighbourIds) {
        return gameMap.getStateNeighbourIds(currentId, neighbourIds);
    }

    public int getNeighbourIds(int currentId, int[] neighbourIds, HashSet<Integer> closedSet) {
        return gameMap.getStateNeighbourIds(currentId, neighbourIds, closedSet);
    }

    /**
     * @param startId stateId of start
     * @param goalId  stateId of goal
     * @return The cost of moving from one adjacent state to the next
     */
    @Override
    public int getMoveCost(int startId, int goalId) {
        if (startId == goalId) return 0;

        int numCols = gameMap.getNumCols();

        int diff = startId - goalId;
        int bit31 = diff >> 31;
        diff = (diff ^ bit31) - bit31;

        if (diff == 1 || diff == numCols) {
            return EDGE_COST_CARDINAL;
        } else if (diff == numCols - 1 || diff == numCols + 1) {
            return EDGE_COST_DIAGONAL;
        } else {
            throw new RuntimeException("startId=" + startId + " goalId=" + goalId + " diff=" + diff + " on " + gameMap.getName());
        }
    }

    @Override
    public int getMoveCost(SearchState start, SearchState goal) {
        return getMoveCost(start.getStateId(), goal.getStateId());
    }

    public boolean isAdjacent(int startId, int goalId) {
        int numCols = gameMap.getNumCols();

        int diff = startId - goalId;
        int bit31 = diff >> 31;
        diff = (diff ^ bit31) - bit31;

        return diff == 1 || diff == numCols || diff == numCols - 1 || diff == numCols + 1;
    }

    public boolean isAdjacent(SearchState start, SearchState goal) {
        return isAdjacent(start.getStateId(), goal.getStateId());
    }

    /**
     * @param startId stateId of the start state
     * @param goalId  stateId of the goal state
     * @return the octile distance (movement in all eight directions) between start and goal state
     */
    @Override
    public int getOctileDistance(int startId, int goalId) {
        return gameMap.getOctileDistance(startId, goalId);
    }

    @Override
    public int getMaxSize() {
        return gameMap.getNumRows() * gameMap.getNumCols();
    }

    public boolean isTouchingWall(SearchState currentState) {
        int stateId = currentState.getStateId();

        int row = gameMap.getRowFromStateId(stateId);
        int col = gameMap.getColFromStateId(stateId);

        for (int r = -1; r <= 1; r++) {
            for (int c = -1; c <= 1; c++) {
                if (r == 0 && col == 0) {
                    continue;
                }

                if (gameMap.isOutOfBoundsOrWall(row + r, col + c)) {
                    return true;
                }
            }
        }

        return false;
    }
}
