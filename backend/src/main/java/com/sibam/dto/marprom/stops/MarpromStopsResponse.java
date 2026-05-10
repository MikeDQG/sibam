package com.sibam.dto.marprom.stops;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MarpromStopsResponse(@JsonProperty("StopPoints") List<MarpromStopDto> stops) {
}
