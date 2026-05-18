package com.sibam.api.dataObjects;

import com.sibam.graph.model.output.Journey;

import java.util.List;

public record ComputedJourneysResponse(List<Journey> journeys) implements ResponseDataObject {
}
