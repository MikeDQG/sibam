package com.sibam.graph.bootstrap;

import com.sibam.graph.builder.GraphBuilder;
import com.sibam.graph.model.Graph;
import com.sibam.graph.storage.GraphSerializer;
import com.sibam.graph.storage.GraphStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GraphBootstrap {

    private final GraphStore graphStore;
    private final GraphBuilder graphBuilder;
    private final GraphSerializer graphSerializer;

    public GraphBootstrap(
            GraphStore graphStore,
            GraphBuilder graphBuilder,
            GraphSerializer graphSerializer
    ) {
        this.graphStore = graphStore;
        this.graphBuilder = graphBuilder;
        this.graphSerializer = graphSerializer;
    }

//    @EventListener(ApplicationReadyEvent.class)
    public void init() {

        Graph graph;

        if (graphSerializer.exists()) {
            graph = graphSerializer.load();
        } else {
            graph = graphBuilder.build();
            graphSerializer.save(graph);
        }

        graphStore.replaceGraph(graph);

        System.out.println("Graph initialized successfully.");
    }

    public void ensureInitialized() {
        if (graphStore.getGraph() == null) {
            init();
        }
    }
}
