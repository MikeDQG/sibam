package com.sibam.dto.marprom.schedules;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarpromScheduleResponse(
        @JsonProperty("Schedules") List<MarpromStopScheduleDto> schedules
) {}
