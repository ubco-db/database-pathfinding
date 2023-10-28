import map.GameMap;
import search.SearchState;
public class DrawPoint {

    final static String MAP_FILE_PATH = "maps/dMap/";
    final static String MAP_FILE_NAME = "012.map";
    final static String PATH_TO_MAP = MAP_FILE_PATH + MAP_FILE_NAME;
    public static void main(String[] args) {
        GameMap map = new GameMap(PATH_TO_MAP);
        int point = 4651;
        map.drawPoint("myPoint.png", new SearchState(point));
    }
}
