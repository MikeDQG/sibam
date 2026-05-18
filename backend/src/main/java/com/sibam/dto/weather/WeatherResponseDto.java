package com.sibam.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherResponseDto(
        @JsonProperty("main") WeatherMainDto main,
        @JsonProperty("wind") WeatherWindDto wind,
        @JsonProperty("rain") WeatherRainDto rain,
        @JsonProperty("weather") List<WeatherConditionDto> weather
) {}
