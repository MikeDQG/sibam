package com.sibam.graph.bootstrap;

import com.sibam.graph.builder.GraphBuilder;
import com.sibam.graph.model.BusNode;
import com.sibam.graph.model.Graph;
import com.sibam.graph.storage.GraphSerializer;
import com.sibam.graph.storage.GraphStore;
import com.sibam.graph.storage.InMemoryGraphStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GraphBootstrapTest {

    private GraphStore graphStore;
    private GraphBuilder graphBuilder;
    private GraphSerializer graphSerializer;
    private GraphBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        graphStore = new InMemoryGraphStore();
        graphBuilder = mock(GraphBuilder.class);
        graphSerializer = mock(GraphSerializer.class);
        bootstrap = new GraphBootstrap(graphStore, graphBuilder, graphSerializer);
    }

    private Graph dummyGraph() {
        return new Graph(
                Map.of(1, new BusNode(1, 46.55, 15.64, "Stop")),
                Map.of(1, new ArrayList<>())
        );
    }

    // --- init() ---

    @Test
    void initLoadsFromSerializerWhenItExists() {
        Graph cached = dummyGraph();
        when(graphSerializer.exists()).thenReturn(true);
        when(graphSerializer.load()).thenReturn(cached);

        bootstrap.init();

        assertThat(graphStore.getGraph()).isSameAs(cached);
        verify(graphBuilder, never()).build();
        verify(graphSerializer, never()).save(any());
    }

    @Test
    void initBuildsAndSavesWhenSerializerExistsButLoadReturnsNull() {
        Graph built = dummyGraph();
        when(graphSerializer.exists()).thenReturn(true);
        when(graphSerializer.load()).thenReturn(null);
        when(graphBuilder.build()).thenReturn(built);

        bootstrap.init();

        assertThat(graphStore.getGraph()).isSameAs(built);
        verify(graphSerializer).save(built);
    }

    @Test
    void initBuildsAndSavesWhenSerializerDoesNotExist() {
        Graph built = dummyGraph();
        when(graphSerializer.exists()).thenReturn(false);
        when(graphBuilder.build()).thenReturn(built);

        bootstrap.init();

        assertThat(graphStore.getGraph()).isSameAs(built);
        verify(graphBuilder).build();
        verify(graphSerializer).save(built);
    }

    // --- ensureInitialized() ---

    @Test
    void ensureInitializedCallsInitWhenGraphIsNull() {
        Graph built = dummyGraph();
        when(graphSerializer.exists()).thenReturn(false);
        when(graphBuilder.build()).thenReturn(built);

        bootstrap.ensureInitialized();

        assertThat(graphStore.getGraph()).isNotNull();
        verify(graphBuilder).build();
    }

    @Test
    void ensureInitializedDoesNothingWhenGraphAlreadySet() {
        Graph existing = dummyGraph();
        graphStore.replaceGraph(existing);

        bootstrap.ensureInitialized();

        assertThat(graphStore.getGraph()).isSameAs(existing);
        verifyNoInteractions(graphBuilder, graphSerializer);
    }

    // --- refresh() ---

    @Test
    void refreshBuildsNewGraphSavesAndReplacesStore() {
        Graph old = dummyGraph();
        Graph fresh = dummyGraph();
        graphStore.replaceGraph(old);
        when(graphBuilder.build()).thenReturn(fresh);

        bootstrap.refresh();

        assertThat(graphStore.getGraph()).isSameAs(fresh);
        verify(graphSerializer).save(fresh);
    }
}
