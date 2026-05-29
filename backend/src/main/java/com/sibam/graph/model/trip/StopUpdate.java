package com.sibam.graph.model.trip;

public class StopUpdate {
    private int stopSequence;
    private int delay;
    private int uncertainty;

    public int getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(int stopSequence) {
        this.stopSequence = stopSequence;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getUncertainty() {
        return uncertainty;
    }

    public void setUncertainty(int uncertainty) {
        this.uncertainty = uncertainty;
    }
}
