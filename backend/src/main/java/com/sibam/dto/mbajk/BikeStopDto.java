package com.sibam.dto.mbajk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BikeStopDto(
        @JsonProperty("number") int number,
        @JsonProperty("name") String name,
        @JsonProperty("address") String address,
        @JsonProperty("position") PositionDto position,
        @JsonProperty("status") String status,
        @JsonProperty("lastUpdate") OffsetDateTime lastUpdate,
        @JsonProperty("totalStands") TotalStandsDto totalStands
) {}
