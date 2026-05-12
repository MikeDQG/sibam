package com.sibam.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherMainDto(
        @JsonProperty("temp") double temp,
        @JsonProperty("feels_like") double feelsLike,
        @JsonProperty("humidity") int humidity
) {}
