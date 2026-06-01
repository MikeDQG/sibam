package com.sibam.graph.routing;

import com.sibam.graph.model.BusNode;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.Node;
import com.sibam.graph.spatial.DistanceCalculator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicServiceTest {

    private final HeuristicService heuristicService = new HeuristicService();

    @Test
    void estimatesWalkAndBikeWithCorrectedManhattanDistance() {
        Node current = new BusNode(1, 46.55, 15.64, "Current");
        Node goal = new BusNode(2, 46.57, 15.60, "Goal");
        GeoPoint currentPoint = new GeoPoint(current.getLat(), current.getLon());
        GeoPoint goalPoint = new GeoPoint(goal.getLat(), goal.getLon());

        assertThat(heuristicService.estimateDistanceMeters(current, goal, EdgeType.WALK))
                .isCloseTo(
                        DistanceCalculator.correctedDistanceMeters(currentPoint, goalPoint, EdgeType.WALK),
                        org.assertj.core.data.Offset.offset(0.000001)
                );
        assertThat(heuristicService.estimateDistanceMeters(current, goal, EdgeType.BIKE))
                .isCloseTo(
                        DistanceCalculator.correctedDistanceMeters(currentPoint, goalPoint, EdgeType.BIKE),
                        org.assertj.core.data.Offset.offset(0.000001)
                );
    }
}
