package com.sibam.graph.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BikeNodeTest {

    @Test
    void constructorWithAllFieldsSetsEachCorrectly() {
        BikeNode node = new BikeNode(1_000_001, 46.55, 15.64, "Gosposvetska", 1, 5, 10);

        assertThat(node.getId()).isEqualTo(1_000_001);
        assertThat(node.getLat()).isEqualTo(46.55);
        assertThat(node.getLon()).isEqualTo(15.64);
        assertThat(node.getStationName()).isEqualTo("Gosposvetska");
        assertThat(node.getStationNumber()).isEqualTo(1);
        assertThat(node.getFreeBikes()).isEqualTo(5);
        assertThat(node.getFreeStands()).isEqualTo(10);
    }

    @Test
    void shortConstructorUsesIdAsStationNumberAndZeroAvailability() {
        BikeNode node = new BikeNode(42, 46.55, 15.64, "Trg svobode");

        assertThat(node.getStationNumber()).isEqualTo(42);
        assertThat(node.getFreeBikes()).isZero();
        assertThat(node.getFreeStands()).isZero();
    }

    @Test
    void nodeWithNoBikesHasZeroFreeBikes() {
        BikeNode node = new BikeNode(1, 46.55, 15.64, "Empty", 1, 0, 15);
        assertThat(node.getFreeBikes()).isZero();
        assertThat(node.getFreeStands()).isEqualTo(15);
    }

    @Test
    void nodeWithNoStandsHasZeroFreeStands() {
        BikeNode node = new BikeNode(1, 46.55, 15.64, "Full", 1, 8, 0);
        assertThat(node.getFreeStands()).isZero();
        assertThat(node.getFreeBikes()).isEqualTo(8);
    }
}
