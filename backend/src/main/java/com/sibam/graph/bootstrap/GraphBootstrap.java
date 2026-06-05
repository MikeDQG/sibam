package com.sibam.graph.bootstrap;

import com.sibam.graph.builder.GraphBuilder;
import com.sibam.graph.model.Graph;
import com.sibam.graph.storage.GraphSerializer;
import com.sibam.graph.storage.GraphStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Inicializira graf ob zagonu aplikacije in ga hrani v GraphStore.
 *
 * Najprej poskusi naložiti serializiran graf, ob manjkajočem ali neveljavnem
 * cache-u pa graf zgradi iz VAO podatkov in ga ponovno shrani.
 */
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

    /**
     * Ob ApplicationReadyEvent naloži ali zgradi graf za routing.
     */
    @Order(200)
    @EventListener(ApplicationReadyEvent.class)
    public void init() {

        Graph graph;

        if (graphSerializer.exists()) {
            graph = graphSerializer.load();
            if (graph == null) {
                graph = graphBuilder.build();
                graphSerializer.save(graph);
            }
        } else {
            graph = graphBuilder.build();
            graphSerializer.save(graph);
        }

        graphStore.replaceGraph(graph);

        System.out.println("Graph initialized successfully.");
    }

    /**
     * Zagotovi, da je graf naložen pred obdelavo zahtevka.
     */
    public void ensureInitialized() {
        if (graphStore.getGraph() == null) {
            init();
        }
    }

    /**
     * Na novo zgradi graf in zamenja aktivno instanco.
     *
     * Uporablja se pri zahtevkih z BIKE načinom, da so MBajk podatki čim bolj
     * sveži pred izračunom poti.
     */
    public void refresh() {
        Graph graph = graphBuilder.build();
        graphSerializer.save(graph);
        graphStore.replaceGraph(graph);
    }
}
