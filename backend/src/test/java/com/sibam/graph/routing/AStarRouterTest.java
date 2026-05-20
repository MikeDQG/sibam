package com.sibam.graph.routing;

import com.sibam.engine.VaoSerializer;
import com.sibam.graph.model.BusNode;
import com.sibam.graph.model.Graph;
import com.sibam.graph.model.Node;
import com.sibam.graph.model.output.Journey;
import com.sibam.graph.spatial.HelperService;
import com.sibam.graph.spatial.SpatialSearchService;
import com.sibam.graph.storage.InMemoryGraphStore;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AStarRouterTest {

    @Test
    void journeyKeepsRequestedCoordinatesAndOnlyUsesNearestNodesForEdges() {
        double originLat = 46.538077;
        double originLon = 15.603520;
        double destinationLat = 46.561754;
        double destinationLon = 15.629752;

        Node nearbyStop = new BusNode(1, 46.55947, 15.64502, "Slomskov trg");
        Graph graph = new Graph(
                Map.of(nearbyStop.getId(), nearbyStop),
                Map.of(nearbyStop.getId(), new ArrayList<>())
        );

        InMemoryGraphStore graphStore = new InMemoryGraphStore();
        graphStore.replaceGraph(graph);
        HelperService helperService = new HelperService();
        AStarRouter router = new AStarRouter(
                graphStore,
                new SpatialSearchService(helperService),
                helperService,
                mock(VaoSerializer.class)
        );

        Journey journey = router.findJourney(
                originLat,
                originLon,
                destinationLat,
                destinationLon,
                null,
                null,
                LocalTime.NOON,
                true,
                true
        );

        assertThat(journey).isNotNull();
        assertThat(journey.origin().lat()).isEqualTo(originLat);
        assertThat(journey.origin().lon()).isEqualTo(originLon);
        assertThat(journey.destination().lat()).isEqualTo(destinationLat);
        assertThat(journey.destination().lon()).isEqualTo(destinationLon);
        assertThat(journey.legs()).isNotEmpty();
        assertThat(journey.legs().getFirst().origin()).isEqualTo(journey.origin());
        assertThat(journey.legs().getLast().destination()).isEqualTo(journey.destination());
    }
}
