package com.sibam.graph.routing;

import com.sibam.graph.model.Edge;

import java.util.List;

public class PathResult {

    private final List<Integer> nodeIds;
    private final List<Edge> edges;
    private final List<PathStepTiming> timings;
    private final int totalCostSeconds;

    public PathResult(
            List<Integer> nodeIds,
            int totalCostSeconds
    ) {
        this(nodeIds, List.of(), List.of(), totalCostSeconds);
    }

    public PathResult(
            List<Integer> nodeIds,
            List<Edge> edges,
            int totalCostSeconds
    ) {
        this(nodeIds, edges, List.of(), totalCostSeconds);
    }

    public PathResult(
            List<Integer> nodeIds,
            List<Edge> edges,
            List<PathStepTiming> timings,
            int totalCostSeconds
    ) {
        this.nodeIds = nodeIds;
        this.edges = edges;
        this.timings = timings;
        this.totalCostSeconds = totalCostSeconds;
    }

    public List<Integer> getNodeIds() {
        return nodeIds;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public List<PathStepTiming> getTimings() {
        return timings;
    }

    public int getTotalCostSeconds() {
        return totalCostSeconds;
    }
}
