package search;

import java.util.ArrayList;
import java.util.HashMap;

public class SearchUtil {

    public static int countRevisits(ArrayList<SearchState> path) {
        if (path == null)
            return 0;
        else {
            HashMap<Integer, Integer> states = new HashMap<>(path.size());
            int revisits = 0;
            for (SearchState searchState : path) {
                int id = searchState.id;
                if (states.containsKey(id))
                    revisits++;
                else
                    states.put(id, id);
            }
            return revisits;
        }
    }

    /*
     * Returns the total amount of distance between revisits.
     */
    public static int distanceRevisits(ArrayList<SearchState> path) {
        int total = 0;
        if (path == null)
            return 0;
        else {
            HashMap<Integer, Integer> states = new HashMap<>(path.size());
            for (int i = 0; i < path.size(); i++) {
                int id = path.get(i).id;
                if (states.containsKey(id)) {
                    int lastLoc = states.get(id);
                    total += (i - lastLoc - 1);
                    //	System.out.println("Distance: "+(i-lastLoc-1));
                }
                states.put(id, i);    // Always put the latest version as may revisit a state multiple times
            }
            return total;
        }
    }
}
