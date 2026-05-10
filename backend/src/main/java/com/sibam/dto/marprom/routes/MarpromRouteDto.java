package com.sibam.dto.marprom.routes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarpromRouteDto(
        @JsonProperty("RouteId") int routeId,
        @JsonProperty("LineId") int lineId,
        @JsonProperty("HeadsignName") String headsignName,
        @JsonProperty("ListOfShapeNodes") List<MarpromShapeNodeDto> shapeNodes
) {}
