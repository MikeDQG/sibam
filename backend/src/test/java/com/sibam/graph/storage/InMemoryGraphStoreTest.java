package com.sibam.graph.storage;

import com.sibam.graph.model.Graph;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryGraphStoreTest {

    @Test
    void initialGraphIsNull() {
        InMemoryGraphStore store = new InMemoryGraphStore();
        assertThat(store.getGraph()).isNull();
    }

    @Test
    void replaceGraphStoresGraph() {
        InMemoryGraphStore store = new InMemoryGraphStore();
        Graph graph = new Graph(Map.of(), Map.of());

        store.replaceGraph(graph);

        assertThat(store.getGraph()).isSameAs(graph);
    }

    @Test
    void replaceGraphOverwritesPreviousGraph() {
        InMemoryGraphStore store = new InMemoryGraphStore();
        Graph first = new Graph(Map.of(), Map.of());
        Graph second = new Graph(Map.of(), Map.of());

        store.replaceGraph(first);
        store.replaceGraph(second);

        assertThat(store.getGraph()).isSameAs(second);
    }
}
