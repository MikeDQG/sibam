package com.sibam.graph.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NodeTypeTest {

    @Test
    void busHasValueZero() {
        assertThat(NodeType.BUS.getValue()).isZero();
    }

    @Test
    void bikeHasValueOne() {
        assertThat(NodeType.BIKE.getValue()).isEqualTo(1);
    }

    @Test
    void userHasValueTwo() {
        assertThat(NodeType.USER.getValue()).isEqualTo(2);
    }

    @Test
    void fromValueReturnsBusForZero() {
        assertThat(NodeType.fromValue(0)).isEqualTo(NodeType.BUS);
    }

    @Test
    void fromValueReturnsBikeForOne() {
        assertThat(NodeType.fromValue(1)).isEqualTo(NodeType.BIKE);
    }

    @Test
    void fromValueReturnsUserForTwo() {
        assertThat(NodeType.fromValue(2)).isEqualTo(NodeType.USER);
    }

    @Test
    void fromValueReturnsNullForUnknownValue() {
        assertThat(NodeType.fromValue(99)).isNull();
    }

    @Test
    void valuesContainsAllThreeTypes() {
        assertThat(NodeType.values()).containsExactlyInAnyOrder(
                NodeType.BUS, NodeType.BIKE, NodeType.USER
        );
    }
}
