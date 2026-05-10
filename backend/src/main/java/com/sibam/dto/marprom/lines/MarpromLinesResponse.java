package com.sibam.dto.marprom.lines;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sibam.dto.marprom.lines.MarpromLineDto;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarpromLinesResponse(
        @JsonProperty("Lines") List<MarpromLineDto> lines
) {}
