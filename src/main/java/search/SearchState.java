package search;

public class SearchState implements Comparable<SearchState> {
    private int stateId;
    private int cost;
    private int g;
    private int h;

    private SearchState parent; // parent SearchState for reconstruction

    public SearchState(int stateId) {
        this.stateId = stateId;
        this.cost = 0;
        this.g = 0;
        this.h = 0;
        this.parent = null;
    }

    public SearchState(int stateId, int g, int h, SearchState parent) {
        this.stateId = stateId;
        this.g = g;
        this.h = h;
        this.cost = g + h;
        this.parent = parent;
    }

    public void updateCost(int g, int h) {
        this.g = g;
        this.h = h;
        this.cost = this.g + this.h;
    }

    public int getStateId() {
        return stateId;
    }

    public int getG() {
        return g;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getH() {
        return h;
    }

    public SearchState getParent() {
        return parent;
    }

    public void setParent(SearchState parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchState that = (SearchState) o;
        return stateId == that.stateId;
    }

//    @Override
//    public String toString() {
//        return "SearchState{" +
//                "stateId=" + stateId +
//                ", cost=" + cost +
//                ", g=" + g +
//                ", h=" + h +
//                ", parent=" + parent +
//                '}';
//    }

    @Override
    public String toString() {
        return stateId + "";
    }

    // Used by PriorityQueue during A*
    @Override
    public int compareTo(SearchState o) {
        // Sort descending based on cost (lowest cost first), if costs are equal, sort ascending based on g cost
        // (highest g cost first)
        if (this.cost == o.cost) {
            return o.g - this.g;
        } else {
            return this.cost - o.cost;
        }
    }

    public void initialize(int stateId) {
        this.stateId = stateId;
        this.cost = 0;
        this.g = 0;
        this.h = 0;
        this.parent = null;
    }
}
