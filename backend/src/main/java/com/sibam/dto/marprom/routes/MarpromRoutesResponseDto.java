package com.sibam.dto.marprom.routes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sibam.dto.marprom.routes.MarpromRouteDto;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarpromRoutesResponseDto(
        @JsonProperty("Routes") List<MarpromRouteDto> routes
) {}
