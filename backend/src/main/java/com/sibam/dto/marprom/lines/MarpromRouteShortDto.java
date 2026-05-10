package com.sibam.dto.marprom.lines;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarpromRouteShortDto(
        @JsonProperty("RouteId") int routeId,
        @JsonProperty("LineId") int lineId,
        @JsonProperty("HeadsignName") String headsignName
) {}