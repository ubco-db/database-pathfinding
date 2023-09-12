package search;

import java.util.ArrayList;
import java.util.HashSet;

public class SearchUtil {

    public static int countRevisits(ArrayList<SearchState> path) {
        if (path == null)
            return 0;
        else {
            HashSet<Integer> states = new HashSet<>();
            int revisits = 0;

            for (SearchState searchState: path) {
                // adding to set if id not already contained, add returns false if id is already contained
                if (!states.add(searchState.id)) {
                    revisits++;
                }
            }

            return revisits;
        }
    }
}
