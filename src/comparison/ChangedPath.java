package comparison;

import search.SearchState;

public class ChangedPath {
    SearchState goal;
    Double percentageOfPathToGoalChanged;

    public ChangedPath(SearchState goal, Double percentageOfPathToGoalChanged) {
        this.goal = goal;
        this.percentageOfPathToGoalChanged = percentageOfPathToGoalChanged;
    }

    public SearchState getGoal() {
        return goal;
    }

    public Double getPercentageOfPathToGoalChanged() {
        return percentageOfPathToGoalChanged;
    }
}
