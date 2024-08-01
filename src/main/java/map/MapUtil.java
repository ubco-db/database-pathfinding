package map;

public class MapUtil {

    private MapUtil() throws Exception {
        throw new Exception("This is a utility class and should not be instantiated.");
    }

    public static boolean isContinuousWall(int n1, int n2) {
        // if n1 and n2 is are walls, adding a wall between them leads to a continuous wall segment
        // this assumes n1 and n2 are across from each other
        return n1 == AbstractedMap.WALL_CHAR && n2 == AbstractedMap.WALL_CHAR;
    }

    public static boolean isBetweenWallAndOtherRegion(int n1, int n2, int r) {
        // if n1 is a wall and n2 is a different region or vice versa, the wall addition between them may constitute a partition
        // this assumes n1 and n2 are across from each other
        return n1 == AbstractedMap.WALL_CHAR && n2 != r || n1 != r && n2 == AbstractedMap.WALL_CHAR;
    }

    public static boolean isOpenDiagonal(int n1, int nDiag, int n2) {
        // if n1 and n2 are walls, but nDiag is not, placing a new wall may make it impossible to reach nDiag
        return n1 == AbstractedMap.WALL_CHAR && n2 == AbstractedMap.WALL_CHAR && nDiag != AbstractedMap.WALL_CHAR;
    }
}
