package com.sibam.graph.storage;

import com.sibam.graph.model.Graph;
import org.springframework.stereotype.Service;

@Service
public class InMemoryGraphStore implements GraphStore {

    private volatile Graph graph;

    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public void replaceGraph(Graph graph) {
        this.graph = graph;
    }
}