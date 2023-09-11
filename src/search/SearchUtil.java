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
}
