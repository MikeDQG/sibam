package com.sibam.graph.storage;

import com.sibam.graph.model.Graph;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class InMemoryGraphStore implements GraphStore {

    private final AtomicReference<Graph> graph = new AtomicReference<>();

    @Override
    public Graph getGraph() {
        return graph.get();
    }

    @Override
    public void replaceGraph(Graph graph) {
        this.graph.set(graph);
    }
}