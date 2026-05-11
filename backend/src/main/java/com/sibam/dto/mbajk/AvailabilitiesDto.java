package com.sibam.dto.mbajk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AvailabilitiesDto(
        @JsonProperty("bikes") int bikes,
        @JsonProperty("stands") int stands,
        @JsonProperty("mechanicalBikes") int mechanicalBikes,
        @JsonProperty("electricalBikes") int electricalBikes
) {}