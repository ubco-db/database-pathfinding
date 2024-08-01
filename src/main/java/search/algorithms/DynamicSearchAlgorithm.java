package search.algorithms;

import map.GameMap;

public interface DynamicSearchAlgorithm extends SearchAlgorithmWithStats {
    void addWall(int stateId) throws Exception;

    void removeWall(int stateId) throws Exception;

    GameMap getGameMap();
}
