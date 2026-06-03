package com.sibam.cache;

import java.util.List;

public record ScheduleCacheManifest(
        int schemaVersion,
        String generatedAt,
        String windowStart,
        String windowEnd,
        List<Day> days
) {
    public record Day(
            String date,
            String path,
            String sha256,
            ArtifactSource source
    ) {
    }
}
