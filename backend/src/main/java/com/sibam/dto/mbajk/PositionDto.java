package com.sibam.dto.mbajk;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PositionDto(
        @JsonProperty("latitude") double latitude,
        @JsonProperty("longitude") double longitude
) {}