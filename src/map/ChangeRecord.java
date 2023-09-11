package map;

import java.awt.*;

public class ChangeRecord {
    public ChangeRecord(int row, int col, Color color, int num) {
        this.row = row;
        this.col = col;
        this.color = color;
        this.num = num;
    }

    int row;
    int col;
    public Color color;
    int num;

    public ChangeRecord(ChangeRecord rec) {
        this.row = rec.row;
        this.col = rec.col;
        this.color = rec.color;
        this.num = rec.num;
    }
}