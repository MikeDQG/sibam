package com.sibam.graph.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModeTest {

    @Test
    void walkingModeHasCorrectValue() {
        assertThat(Mode.WALKING.getMode()).isEqualTo("WALKING");
    }

    @Test
    void busModeHasCorrectValue() {
        assertThat(Mode.BUS.getMode()).isEqualTo("BUS");
    }

    @Test
    void bikingModeHasCorrectValue() {
        assertThat(Mode.BIKING.getMode()).isEqualTo("BIKING");
    }

    @Test
    void valuesContainsAllThreeModes() {
        assertThat(Mode.values()).containsExactlyInAnyOrder(Mode.WALKING, Mode.BUS, Mode.BIKING);
    }
}
