package search;


public class SearchState implements Comparable<SearchState> {
    public int id;
    public double cost;    // f
    public int g;
    public int h;
    public Object stateData;
    public boolean updated = false;

    public SearchState prev;

    public SearchState(SearchState st) {
        this.id = st.id;
        this.cost = st.cost;
        this.g = st.g;
        this.h = st.h;
        this.prev = st.prev;
        stateData = st.stateData;
        this.updated = st.updated;
    }

    public SearchState(int id) {
        this.id = id;
        cost = 0;
        g = 0;
        h = 0;
        prev = null;
        stateData = null;
    }

    public void updateCost(int g, int h) {
        this.h = h;
        this.g = g;
        this.cost = this.g + this.h;
    }

    public boolean equals(SearchState s1) {
        return this.id == s1.id;
    }

    public int compareTo(SearchState o) {
        if (this.cost == o.cost)
            return o.g - this.g;
        return (int) (this.cost - o.cost);
    }

    public String toString() {
        return "Id: " + id + " U: " + updated + " f: " + cost + " g: " + g + " h: " + h + " Cost: " + cost;
    }

}