package com.sibam.dto.savedPath;

import com.sibam.graph.model.output.Journey;

import java.util.UUID;

public record SavedPathRequest (
        UUID userId,
        String name,
        Journey journey
) {}
