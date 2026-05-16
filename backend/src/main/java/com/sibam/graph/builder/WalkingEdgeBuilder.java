package com.sibam.graph.builder;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.Node;

public class WalkingEdgeBuilder {

    public Edge build(Node from, Node to) {

        int distance = calculateDistance(from, to);

        int walkingSeconds = distance / 1;

        return new Edge(
                from.getId(),
                to.getId(),
                EdgeType.WALK,
                distance,
                walkingSeconds
        );
    }

    private int calculateDistance(Node from, Node to) {
        return 250;
    }
}