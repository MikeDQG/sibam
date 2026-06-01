package com.sibam.graph.spatial;

import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.GeoPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceCalculatorTest {

    @Test
    void manhattanDistanceUsesAbsoluteLatitudeAndLongitudeDeltas() {
        GeoPoint from = new GeoPoint(46.55, 15.64);
        GeoPoint to = new GeoPoint(46.57, 15.60);

        assertThat(DistanceCalculator.manhattanDistance(from, to)).isCloseTo(0.06, withinTolerance());
    }

    @Test
    void walkingAndBikingDistancesUseModeSpecificCorrectionFactors() {
        GeoPoint from = new GeoPoint(46.55, 15.64);
        GeoPoint to = new GeoPoint(46.57, 15.60);
        double manhattanMeters = DistanceCalculator.manhattanMeters(from, to);

        assertThat(DistanceCalculator.correctedDistanceMeters(from, to, EdgeType.WALK))
                .isCloseTo(manhattanMeters * DistanceCalculator.WALK_DISTANCE_FACTOR, withinTolerance());
        assertThat(DistanceCalculator.correctedDistanceMeters(from, to, EdgeType.BIKE))
                .isCloseTo(manhattanMeters * DistanceCalculator.BIKE_DISTANCE_FACTOR, withinTolerance());
    }

    private org.assertj.core.data.Offset<Double> withinTolerance() {
        return org.assertj.core.data.Offset.offset(0.000001);
    }
}
