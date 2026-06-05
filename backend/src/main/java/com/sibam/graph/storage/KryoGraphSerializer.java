package com.sibam.graph.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibam.cache.ArtifactCacheService;
import com.sibam.cache.ArtifactSource;
import com.sibam.cache.GraphCacheManifest;
import com.sibam.cache.LocalArtifactStorage;
import com.sibam.cache.Sha256;
import com.sibam.graph.model.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Serializator grafa v lokalni in Supabase cache.
 *
 * Kljub imenu uporablja Java object serialization za binarni graph.bin in ob
 * vsakem shranjevanju ali nalaganju zapiše manifest s SHA-256 hash vrednostjo.
 */
@Component
public class KryoGraphSerializer implements GraphSerializer {

    private static final Logger log = LoggerFactory.getLogger(KryoGraphSerializer.class);
    private static final String GRAPH_PATH = "marprom/graph/graph.bin";
    private static final String MANIFEST_PATH = "marprom/graph/manifest.json";
    private static final int GRAPH_VERSION = 1;

    private final ArtifactCacheService artifactCacheService;
    private final LocalArtifactStorage localStorage;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final boolean enabled;

    public KryoGraphSerializer(
            ArtifactCacheService artifactCacheService,
            Clock clock,
            @Value("${graph.cache.enabled:true}") boolean enabled
    ) {
        this.artifactCacheService = artifactCacheService;
        this.localStorage = artifactCacheService.localStorage();
        this.objectMapper = new ObjectMapper();
        this.clock = clock;
        this.enabled = enabled;
    }

    /**
     * Serializira graf in ga shrani lokalno ter v Supabase cache, če je omogočen.
     *
     * @param graph graf za shranjevanje
     */
    @Override
    public void save(Graph graph) {
        if (!enabled) {
            return;
        }

        try {
            byte[] bytes = serialize(graph);
            localStorage.write(GRAPH_PATH, bytes);
            artifactCacheService.upload(GRAPH_PATH, bytes, MediaType.APPLICATION_OCTET_STREAM);
            writeManifest(bytes, ArtifactSource.GENERATED);
            log.info(
                    "Static graph cache saved: localRoot={}, supabaseEnabled={}, supabaseBucket={}, path={}, bytes={}",
                    localStorage.root().toAbsolutePath(),
                    artifactCacheService.supabaseStorage().enabled(),
                    artifactCacheService.supabaseStorage().bucket(),
                    GRAPH_PATH,
                    bytes.length
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save graph cache", e);
        }
    }

    /**
     * Naloži graf iz lokalnega cache-a.
     *
     * @return deserializiran graf ali null, če cache ni uporaben
     */
    @Override
    public Graph load() {
        if (!enabled) {
            return null;
        }

        try {
            byte[] bytes = localStorage.read(GRAPH_PATH);
            Graph graph = deserialize(bytes);
            writeManifest(bytes, ArtifactSource.LOCAL);
            log.info(
                    "Static graph cache loaded locally: localRoot={}, supabaseEnabled={}, supabaseBucket={}, path={}",
                    localStorage.root().toAbsolutePath(),
                    artifactCacheService.supabaseStorage().enabled(),
                    artifactCacheService.supabaseStorage().bucket(),
                    localStorage.resolve(GRAPH_PATH).toAbsolutePath()
            );
            return graph;
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to load local static graph cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Preveri lokalni cache in po potrebi poskusi obnovitev iz Supabase.
     *
     * @return true, če je graf cache mogoče naložiti
     */
    @Override
    public boolean exists() {
        if (!enabled) {
            return false;
        }

        if (localStorage.validate(GRAPH_PATH)) {
            return true;
        }

        try {
            if (artifactCacheService.restoreFromSupabase(GRAPH_PATH)) {
                byte[] bytes = localStorage.read(GRAPH_PATH);
                deserialize(bytes);
                writeManifest(bytes, ArtifactSource.SUPABASE);
                log.info(
                        "Static graph cache restored from Supabase: localRoot={}, supabaseBucket={}, path={}",
                        localStorage.root().toAbsolutePath(),
                        artifactCacheService.supabaseStorage().bucket(),
                        GRAPH_PATH
                );
                return true;
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to restore static graph cache from Supabase: {}", e.getMessage());
        }

        return false;
    }

    private void writeManifest(byte[] graphBytes, ArtifactSource source) throws IOException {
        GraphCacheManifest manifest = new GraphCacheManifest(
                1,
                OffsetDateTime.now(clock.withZone(ZoneId.of("Europe/Ljubljana"))).toString(),
                GRAPH_PATH,
                Sha256.hex(graphBytes),
                source,
                GRAPH_VERSION
        );
        byte[] manifestBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest);
        localStorage.write(MANIFEST_PATH, manifestBytes);
        artifactCacheService.upload(MANIFEST_PATH, manifestBytes, MediaType.APPLICATION_JSON);
    }

    private byte[] serialize(Graph graph) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(graph);
        }
        return bytes.toByteArray();
    }

    private Graph deserialize(byte[] bytes) throws IOException {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object value = input.readObject();
            if (!(value instanceof Graph graph)) {
                throw new IOException("Cached graph artifact does not contain a Graph");
            }
            return graph;
        } catch (ClassNotFoundException e) {
            throw new IOException("Cached graph artifact contains an unknown class", e);
        }
    }
}
