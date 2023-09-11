package util;

/*
FILE: 			StringFunc.java
PROVIDES: 		A static class with some useful string functions.
PROGRAM BY:     Ramon Lawrence (ramon-lawrence@ubc.ca)
CREATION DATE:  April 2006
MODIFIED:		January 2010
*/

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class StringFunc {
    public static String spaces(int count) {
        if (count <= 0)
            return "";
        StringBuffer sp = new StringBuffer(count);
        for (int i = 0; i < count; i++)
            sp.append(" ");
        return sp.toString();
    }

    public static String pad(String st, int size) {
        if (st.length() >= size)
            return st;

        return st + StringFunc.spaces(size - st.length());
    }

    public static String formatSQLValue(Object obj) {
        if (obj == null)
            return "null";

        if (obj instanceof Number)
            return obj.toString();
        else
            return "'" + obj.toString() + "'";
    }

    public static void printResultSet(ResultSet rst) throws SQLException {
        ResultSetMetaData meta = rst.getMetaData();

        int i = 0;
        // System.out.println("Total columns: " + meta.getColumnCount());
        System.out.print(meta.getColumnName(1));
        for (int j = 2; j <= meta.getColumnCount(); j++)
            System.out.print(", " + meta.getColumnName(j));
        System.out.println();

        while (rst.next()) {
            if (meta.getColumnType(1) == java.sql.Types.INTEGER)
                System.out.print(rst.getInt(meta.getColumnName(1)));
            else
                System.out.print(rst.getString(meta.getColumnName(1)));
            for (int j = 2; j <= meta.getColumnCount(); j++)
                //     System.out.print(", " + rst.getObject(j));		// Not supported by SimpleDB
                if (meta.getColumnType(j) == java.sql.Types.INTEGER)
                    System.out.print(", " + rst.getInt(meta.getColumnName(j)));
                else
                    System.out.print(", " + rst.getString(meta.getColumnName(j)));
            System.out.println();
            i++;
        }
    }

    /**
     * Converts a ResultSet to a string (unlimited rows).
     *
     * @param rst
     * @return
     * @throws SQLException
     */
    public static String resultSetToString(ResultSet rst) throws SQLException {
        return resultSetToString(rst, Integer.MAX_VALUE);
    }

    /**
     * Converts a ResultSet to a string with a given number of rows displayed.
     * Total rows are determined but only the first few are put into a string.
     *
     * @param rst
     * @param maxrows
     * @return
     * @throws SQLException
     */
    public static String resultSetToString(ResultSet rst, int maxrows) throws SQLException {
        StringBuffer buf = new StringBuffer(5000);
        int rowCount = 0;
        ResultSetMetaData meta = rst.getMetaData();
        buf.append("Total columns: " + meta.getColumnCount());
        buf.append("\n");
        buf.append(meta.getColumnName(1));
        for (int j = 2; j <= meta.getColumnCount(); j++)
            buf.append(", " + meta.getColumnName(j));
        buf.append("\n");
        while (rst.next()) {
            if (rowCount < maxrows) {
                for (int j = 0; j < meta.getColumnCount(); j++) {
                    buf.append(rst.getObject(j + 1));
                    if (j != meta.getColumnCount() - 1)
                        buf.append(", ");
                }
                buf.append("\n");
            }
            rowCount++;
        }
        buf.append("Total results: " + rowCount);
        return buf.toString();
    }
}