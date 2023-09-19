package map;

/**
 * A point on a 2D map.
 *
 * @author rlawrenc
 */
public class MapPoint {

    private int row;
    private int col;


    public MapPoint(int row, int col) {
        super();
        this.row = row;
        this.col = col;
    }

    public MapPoint() {

    }


    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public String toString() {
        return "(" + row + "," + col + ")";
    }

    public String altToString() {
        return row + ":" + col;
    }
}
