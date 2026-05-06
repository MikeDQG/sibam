package com.sibam.model.transit;

public record Stop(
        int stopId,      // StopPointId v JSON[cite: 2]
        String name,     // Name v JSON[cite: 2]
        double lat,      // Lat v JSON[cite: 2]
        double lon       // Lon v JSON[cite: 2]
) {}