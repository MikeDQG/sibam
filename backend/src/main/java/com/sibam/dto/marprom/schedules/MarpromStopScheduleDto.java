package com.sibam.dto.marprom.schedules;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sibam.dto.marprom.stops.MarpromStopDto;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarpromStopScheduleDto(
        @JsonProperty("StopPoint") MarpromStopDto stopPoint,
        @JsonProperty("ScheduleForLine") List<MarpromLineScheduleDto> scheduleForLine
) {}