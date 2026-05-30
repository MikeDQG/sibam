package com.sibam.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherConditionDto(
        @JsonProperty("main") String main,
        @JsonProperty("description") String description
) {}
