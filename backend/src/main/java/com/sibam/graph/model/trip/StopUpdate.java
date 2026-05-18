package com.sibam.graph.model.trip;

import lombok.Data;

@Data
public class StopUpdate {
    int stopSequence;
    int delay;
    int uncertainty;
}
