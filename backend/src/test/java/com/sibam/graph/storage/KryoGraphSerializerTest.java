package com.sibam.graph.storage;

import com.sibam.cache.ArtifactCacheService;
import com.sibam.cache.LocalArtifactStorage;
import com.sibam.cache.SupabaseArtifactStorage;
import com.sibam.graph.model.BusNode;
import com.sibam.graph.model.Edge;
import com.sibam.graph.model.Graph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class KryoGraphSerializerTest {

    // In-memory file store backing the mocked LocalArtifactStorage
    private final Map<String, byte[]> fileStore = new HashMap<>();

    private LocalArtifactStorage localStorage;
    private ArtifactCacheService artifactCacheService;
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneId.of("Europe/Ljubljana"));

    @BeforeEach
    void setUp() throws IOException {
        fileStore.clear();

        localStorage = mock(LocalArtifactStorage.class);
        when(localStorage.root()).thenReturn(Path.of("/tmp/test-graph"));
        when(localStorage.resolve(anyString())).thenAnswer(inv -> Path.of("/tmp/test-graph/" + inv.getArgument(0)));

        doAnswer(inv -> { fileStore.put(inv.getArgument(0), inv.getArgument(1)); return null; })
                .when(localStorage).write(anyString(), any(byte[].class));
        when(localStorage.read(anyString()))
                .thenAnswer(inv -> fileStore.get(inv.getArgument(0).toString()));
        when(localStorage.exists(anyString()))
                .thenAnswer(inv -> fileStore.containsKey(inv.getArgument(0).toString()));
        when(localStorage.validate(anyString()))
                .thenAnswer(inv -> {
                    byte[] b = fileStore.get(inv.getArgument(0).toString());
                    return b != null && b.length > 0;
                });

        SupabaseArtifactStorage supabaseStorage = mock(SupabaseArtifactStorage.class);
        when(supabaseStorage.enabled()).thenReturn(false);
        when(supabaseStorage.bucket()).thenReturn("cache");

        artifactCacheService = mock(ArtifactCacheService.class);
        when(artifactCacheService.localStorage()).thenReturn(localStorage);
        when(artifactCacheService.supabaseStorage()).thenReturn(supabaseStorage);
    }

    private KryoGraphSerializer enabled() {
        return new KryoGraphSerializer(artifactCacheService, clock, true);
    }

    private KryoGraphSerializer disabled() {
        return new KryoGraphSerializer(artifactCacheService, clock, false);
    }

    private Graph simpleGraph() {
        BusNode node = new BusNode(1, 46.55, 15.64, "Stop A");
        return new Graph(Map.of(1, node), Map.of(1, new ArrayList<Edge>()));
    }

    // --- disabled mode ---

    @Test
    void saveDoesNothingWhenDisabled() throws IOException {
        disabled().save(simpleGraph());
        verify(localStorage, never()).write(anyString(), any());
    }

    @Test
    void loadReturnsNullWhenDisabled() {
        assertThat(disabled().load()).isNull();
    }

    @Test
    void existsReturnsFalseWhenDisabled() {
        assertThat(disabled().exists()).isFalse();
    }

    // --- enabled mode ---

    @Test
    void saveWritesGraphAndManifest() throws IOException {
        enabled().save(simpleGraph());

        verify(localStorage).write(eq("marprom/graph/graph.bin"), any());
        verify(localStorage).write(eq("marprom/graph/manifest.json"), any());
    }

    @Test
    void saveAndLoadRoundtrip() {
        KryoGraphSerializer serializer = enabled();
        serializer.save(simpleGraph());

        Graph loaded = serializer.load();

        assertThat(loaded).isNotNull();
        assertThat(loaded.getNodes()).containsKey(1);
        assertThat(loaded.getNodes().get(1).getLat()).isEqualTo(46.55);
    }

    @Test
    void loadReturnsNullWhenFileIsMissing() {
        // fileStore is empty — read returns null by default
        assertThat(enabled().load()).isNull();
    }

    @Test
    void existsReturnsTrueWhenLocalFileIsValid() {
        KryoGraphSerializer serializer = enabled();
        serializer.save(simpleGraph());

        assertThat(serializer.exists()).isTrue();
    }

    @Test
    void existsReturnsFalseWhenNoFilePresent() {
        assertThat(enabled().exists()).isFalse();
    }

    @Test
    void existsRestoresFromSupabaseWhenLocalFileMissing() throws IOException {
        byte[] graphBytes = serialize(simpleGraph());
        when(artifactCacheService.restoreFromSupabase("marprom/graph/graph.bin"))
                .thenAnswer(inv -> {
                    fileStore.put("marprom/graph/graph.bin", graphBytes);
                    return true;
                });

        assertThat(enabled().exists()).isTrue();
        verify(artifactCacheService).restoreFromSupabase("marprom/graph/graph.bin");
    }

    @Test
    void existsReturnsFalseWhenSupabaseRestoreFails() throws IOException {
        when(artifactCacheService.restoreFromSupabase(anyString())).thenReturn(false);
        assertThat(enabled().exists()).isFalse();
    }

    private byte[] serialize(Graph graph) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(graph);
        }
        return out.toByteArray();
    }
}
