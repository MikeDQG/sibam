package com.sibam.model.trip;

import lombok.Data;

@Data
public class StopUpdate {
    int stopSequence;
    String stopId;
    int delay;
    int uncertainty;
}
