package com.sibam.dto.mbajk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TotalStandsDto(
        @JsonProperty("availabilities") AvailabilitiesDto availabilities,
        @JsonProperty("capacity") int capacity
) {}
