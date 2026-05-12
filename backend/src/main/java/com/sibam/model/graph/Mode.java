package com.sibam.model.graph;

public enum Mode {
    WALKING("WALKING"), BUS("BUS"), BIKING("BIKING");

    private String mode;
    Mode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }
}
