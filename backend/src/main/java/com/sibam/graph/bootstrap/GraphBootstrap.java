package com.sibam.graph.bootstrap;

import com.sibam.graph.builder.GraphBuilder;
import com.sibam.graph.model.Graph;
import com.sibam.graph.storage.GraphSerializer;
import com.sibam.graph.storage.GraphStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GraphBootstrap {

    private final GraphStore graphStore;
    private final GraphBuilder graphBuilder;
    private final GraphSerializer graphSerializer;

    @Value("${app.ml-only-scheduled-ingestion:false}")
    private boolean mlOnlyScheduledIngestion;

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
        if (mlOnlyScheduledIngestion) {
            System.out.println("ML-only mode enabled: skipping graph initialization/build.");
            return;
        }
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
        if (mlOnlyScheduledIngestion) {
            // In ML-only mode we skip any graph work
            return;
        }
        if (graphStore.getGraph() == null) {
            init();
        }
    }

    public void refresh() {
        if (mlOnlyScheduledIngestion) {
            System.out.println("ML-only mode enabled: skipping graph refresh.");
            return;
        }
        Graph graph = graphBuilder.build();
        graphSerializer.save(graph);
        graphStore.replaceGraph(graph);
    }
}
