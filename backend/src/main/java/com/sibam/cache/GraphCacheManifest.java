package com.sibam.cache;

public record GraphCacheManifest(
        int schemaVersion,
        String generatedAt,
        String path,
        String sha256,
        ArtifactSource source,
        int graphVersion
) {
}
