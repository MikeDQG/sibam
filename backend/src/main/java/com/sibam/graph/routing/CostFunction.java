package com.sibam.graph.routing;

import com.sibam.graph.model.Edge;

public interface CostFunction {

    int calculateCost(Edge edge);
}
