package com.sibam.dto.marprom.lines;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record MarpromLineDto(
        @JsonProperty("LineId") int lineId,
        @JsonProperty("Code") String code,
        @JsonProperty("Description") String destination,
        @JsonProperty("Color") String color,
        @JsonProperty("routes") List<MarpromRouteShortDto> routes
) {}
