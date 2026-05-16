package com.sibam.graph.routing;

import java.util.List;

public class PathResult {

    private final List<Integer> nodeIds;
    private final int totalCostSeconds;

    public PathResult(
            List<Integer> nodeIds,
            int totalCostSeconds
    ) {
        this.nodeIds = nodeIds;
        this.totalCostSeconds = totalCostSeconds;
    }

    public List<Integer> getNodeIds() {
        return nodeIds;
    }

    public int getTotalCostSeconds() {
        return totalCostSeconds;
    }
}
