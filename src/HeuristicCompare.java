import util.HeuristicFunction;

import java.util.ArrayList;

/**
 * Does a comparison of different heuristics for A*.
 */
public class HeuristicCompare {

    public static ArrayList<HeuristicFunction> createHeuristics() {
        ArrayList<HeuristicFunction> heuristicList = new ArrayList<HeuristicFunction>();
        // f0 - Manhattan distance
        heuristicList.add(
                new HeuristicFunction() {
                    public int apply(int startId, int goalId, int ncols) {
                        int startRow = startId / ncols;
                        int goalRow = goalId / ncols;
                        int diffRow = startRow - goalRow;

                        int bit31 = diffRow >> 31;                // Compute its absolute value
                        diffRow = (diffRow ^ bit31) - bit31;

                        int diffCol = ((startId - startRow * ncols) - (goalId - goalRow * ncols));
                        bit31 = diffCol >> 31;                // Compute its absolute value
                        diffCol = (diffCol ^ bit31) - bit31;

                        // return Math.abs(diffRow) *10 + Math.abs(diffCol)*10;
                        return Math.abs(diffRow) * 1 + Math.abs(diffCol) * 1;
                    }
                }
        );

        // f1 - brc202d
        heuristicList.add(
                new HeuristicFunction() {
                    public int apply(int startId, int goalId, int ncols) {
                        int startRow = startId / ncols; //y
                        int goalRow = goalId / ncols; //y_g
                        int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                        int startCol = startId - startRow * ncols; //x
                        int goalCol = goalId - goalRow * ncols; //x_g
                        int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                        double p1 = diffCol * Math.sqrt(1.0 * goalRow / startCol);
                        double max = p1 < diffRow ? diffRow : p1;

                        return (int) Math.round(Math.pow(max, 4));
                    }
                }
        );

        // f2 - den000d
        heuristicList.add(
                new HeuristicFunction() {
                    public int apply(int startId, int goalId, int ncols) {
                        int startRow = startId / ncols; //y
                        int goalRow = goalId / ncols; //y_g
                        int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                        int startCol = startId - startRow * ncols; //x
                        int goalCol = goalId - goalRow * ncols; //x_g
                        int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                        int max = Math.max(diffCol, diffRow);
                        return (int) Math.round(44.9 * max + diffRow);
                    }
                }
        );

        // f3 - den501d
        heuristicList.add(
                new HeuristicFunction() {
                    public int apply(int startId, int goalId, int ncols) {
                        int startRow = startId / ncols; //y
                        int goalRow = goalId / ncols; //y_g
                        int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                        int startCol = startId - startRow * ncols; //x
                        int goalCol = goalId - goalRow * ncols; //x_g
                        int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                        double p1 = 1.0 * goalRow / (startRow - 8.3) * diffRow;
                        double max = p1 - diffCol < 0 ? diffCol : p1;

                        return (int) Math.round(Math.pow(max, 2));
                    }
                }
        );

        // f4 - lak505d
        heuristicList.add(
                new HeuristicFunction() {
                    public int apply(int startId, int goalId, int ncols) {
                        int startRow = startId / ncols; //y
                        int goalRow = goalId / ncols; //y_g
                        int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                        int startCol = startId - startRow * ncols; //x
                        int goalCol = goalId - goalRow * ncols; //x_g
                        int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                        int min = Math.min(goalRow, diffRow);

                        int max1 = Math.max(100, min + startRow);

                        int max2 = diffCol - diffRow < 0 ? diffRow : diffCol;

                        return max1 * max1 * max2;
                    }
                }
        );

        // f5 - orz103d
        heuristicList.add(
                new HeuristicFunction() {
                    public int apply(int startId, int goalId, int ncols) {
                        int startRow = startId / ncols; //y
                        int goalRow = goalId / ncols;
                        int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                        int startCol = startId - startRow * ncols; //x
                        int goalCol = goalId - goalRow * ncols; //x_g
                        int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                        double p1 = 11.5 * Math.sqrt(((startRow + 1) + diffRow) * ((startRow + 1) + diffRow) * diffRow);
                        double p2 = diffCol * (goalRow + 1); //goalRow (heuristic indexes from 1 not 0 so add +1)
                        double max = Math.max(p1, p2);
                        return (int) Math.round(max);
                    }
                }
        );

        // f6 - ost000a
        heuristicList.add(
                new HeuristicFunction() {
                    public int apply(int startId, int goalId, int ncols) {
                        int startRow = startId / ncols; //y
                        int goalRow = goalId / ncols; //y_g
                        int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                        int startCol = startId - startRow * ncols; //x
                        int goalCol = goalId - goalRow * ncols; //x_g
                        int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                        int min = Math.max(diffCol, goalCol);

                        int max = Math.max(diffRow, diffCol + min);

                        return max * max;
                    }
                }
        );

        // f7 - wall-hugging: max (deltaX ^ 2, deltaY ^ 2)
        heuristicList.add(
                new HeuristicFunction() {
                    public int apply(int startId, int goalId, int ncols) {
                        int startRow = startId / ncols; //y
                        int goalRow = goalId / ncols; //y_g
                        int diffRow = startRow - goalRow < 0 ? goalRow - startRow : startRow - goalRow; //delta_y

                        int startCol = startId - startRow * ncols; //x
                        int goalCol = goalId - goalRow * ncols; //x_g
                        int diffCol = startCol - goalCol < 0 ? goalCol - startCol : startCol - goalCol; //delta_x

                        if (diffRow > diffCol)
                            return diffRow * diffRow;
                        else
                            return diffCol * diffCol;
                    }
                }
        );

        return heuristicList;
    }
}
