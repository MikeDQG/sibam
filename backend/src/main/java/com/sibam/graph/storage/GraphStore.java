package com.sibam.graph.storage;

import com.sibam.graph.model.Graph;

public interface GraphStore {

    Graph getGraph();

    void replaceGraph(Graph graph);
}