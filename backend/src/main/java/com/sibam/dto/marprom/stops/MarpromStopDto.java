package com.sibam.dto.marprom.stops;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarpromStopDto(
        @JsonProperty("StopPointId") int id,
        @JsonProperty("Name") String name,
        @JsonProperty("Description") String address,
        @JsonProperty("Lat") double lat,
        @JsonProperty("Lon") double lon
) {}