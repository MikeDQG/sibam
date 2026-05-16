package com.sibam.graph.storage;

import com.sibam.graph.model.Graph;

public interface GraphSerializer {

    void save(Graph graph);

    Graph load();

    boolean exists();
}
