package util;

/*
FILE: 			StringFunc.java
PROVIDES: 		A static class with some useful string functions.
PROGRAM BY:     Ramon Lawrence (ramon-lawrence@ubc.ca)
CREATION DATE:  April 2006
MODIFIED:		January 2010
*/

public class StringFunc {
    public static String spaces(int count) {
        if (count <= 0) return "";
        return " ".repeat(count);
    }

    public static String pad(String st, int size) {
        if (st.length() >= size) return st;

        return st + StringFunc.spaces(size - st.length());
    }
}