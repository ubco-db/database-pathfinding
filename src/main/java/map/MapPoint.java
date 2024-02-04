package map;

/**
 * A point on a 2D map.
 *
 * @author rlawrenc
 */
public class MapPoint {

    private final int row;
    private final int col;


    public MapPoint(int row, int col) {
        super();
        this.row = row;
        this.col = col;
    }


    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public String toString() {
        return "(" + row + "," + col + ")";
    }
}
