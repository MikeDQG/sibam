package com.sibam.graph.routing;

import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.RouteAlternativeLabel;
import com.sibam.graph.model.output.Journey;
import com.sibam.graph.model.output.Leg;
import com.sibam.graph.model.output.RouteAlternativesResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteAlternativeServiceTest {

    private static final LocalDate TEST_DATE = LocalDate.parse("2026-06-04");

    @Test
    void busOnlyRequestDoesNotReturnBikeLegs() {
        AStarRouter router = mock(AStarRouter.class);
        RouteAlternativeService service = service(router);
        when(router.findJourneyCandidate(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                any(), any(), any(LocalTime.class), any(LocalDate.class), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(candidate(journey(1000, "BIKE"), 1, 2, 3))
                .thenReturn(candidate(journey(1100, "WALK", "BUS"), 1, 4, 5))
                .thenReturn(null);

        RouteAlternativesResponse response = service.findAlternatives(1, 1, 2, 2, null, null, LocalTime.NOON, TEST_DATE, false, true, RoutingTimeMode.DEPART_AT);

        assertThat(response.routes()).hasSize(1);
        assertThat(response.routes().getFirst().modes()).doesNotContain("BIKE");
        assertThat(response.routes().getFirst().origin()).isEqualTo(new GeoPoint(1, 1));
        assertThat(response.routes().getFirst().destination()).isEqualTo(new GeoPoint(2, 2));
    }

    @Test
    void bikeOnlyRequestDoesNotReturnBusLegs() {
        AStarRouter router = mock(AStarRouter.class);
        RouteAlternativeService service = service(router);
        when(router.findJourneyCandidate(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                any(), any(), any(LocalTime.class), any(LocalDate.class), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(candidate(journey(1000, "BUS"), 1, 2, 3))
                .thenReturn(candidate(journey(1100, "WALK", "BIKE"), 1, 4, 5))
                .thenReturn(null);

        RouteAlternativesResponse response = service.findAlternatives(1, 1, 2, 2, null, null, LocalTime.NOON, TEST_DATE, true, false, RoutingTimeMode.DEPART_AT);

        assertThat(response.routes()).hasSize(1);
        assertThat(response.routes().getFirst().modes()).doesNotContain("BUS");
    }

    @Test
    void busAndBikeRequestMayReturnMixedAlternativesSortedByRealDuration() {
        AStarRouter router = mock(AStarRouter.class);
        RouteAlternativeService service = service(router);
        when(router.findJourneyCandidate(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                any(), any(), any(LocalTime.class), any(LocalDate.class), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(candidate(journey(1500, "WALK", "BIKE"), 1, 2, 3))
                .thenReturn(candidate(journey(1000, "WALK", "BUS"), 1, 4, 5))
                .thenReturn(candidate(journey(1200, "WALK", "BUS", "BIKE"), 1, 6, 7));

        RouteAlternativesResponse response = service.findAlternatives(1, 1, 2, 2, null, null, LocalTime.NOON, TEST_DATE, true, true, RoutingTimeMode.DEPART_AT);

        assertThat(response.routes()).hasSize(3);
        assertThat(response.routes()).extracting("totalDurationSeconds")
                .containsExactly(1000L, 1200L, 1500L);
        assertThat(response.routes().stream().flatMap(route -> route.modes().stream()).toList())
                .contains("BUS", "BIKE");
    }

    @Test
    void fastestBikeRouteUsesFastestLabel() {
        AStarRouter router = mock(AStarRouter.class);
        RouteAlternativeService service = service(router);
        when(router.findJourneyCandidate(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                any(), any(), any(LocalTime.class), any(LocalDate.class), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(candidate(journey(1000, "WALK", "BIKE"), 1, 2, 3))
                .thenReturn(null);

        RouteAlternativesResponse response = service.findAlternatives(1, 1, 2, 2, null, null, LocalTime.NOON, TEST_DATE, true, true, RoutingTimeMode.DEPART_AT);

        assertThat(response.routes()).hasSize(1);
        assertThat(response.routes().getFirst().rank()).isEqualTo(1);
        assertThat(response.routes().getFirst().label()).isEqualTo(RouteAlternativeLabel.FASTEST.displayName());
    }

    @Test
    void duplicateRoutesAreFilteredOut() {
        AStarRouter router = mock(AStarRouter.class);
        RouteAlternativeService service = service(router);
        when(router.findJourneyCandidate(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                any(), any(), any(LocalTime.class), any(LocalDate.class), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(candidate(journey(1000, "BUS"), 1, 2, 3, 4))
                .thenReturn(candidate(journey(1010, "BUS"), 1, 2, 3, 4))
                .thenReturn(candidate(journey(1200, "BIKE"), 1, 5, 6, 7));

        RouteAlternativesResponse response = service.findAlternatives(1, 1, 2, 2, null, null, LocalTime.NOON, TEST_DATE, true, true, RoutingTimeMode.DEPART_AT);

        assertThat(response.routes()).hasSize(2);
    }

    @Test
    void returnsNotFoundStatusWhenNoRoutesAvailable() {
        AStarRouter router = mock(AStarRouter.class);
        RouteAlternativeService service = service(router);
        when(router.findJourneyCandidate(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                any(), any(), any(LocalTime.class), any(LocalDate.class), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(null);

        RouteAlternativesResponse response = service.findAlternatives(1, 1, 2, 2, null, null, LocalTime.NOON, TEST_DATE, true, true, RoutingTimeMode.DEPART_AT);

        assertThat(response.status()).isEqualTo("not_found");
        assertThat(response.routes()).isEmpty();
    }

    @Test
    void stopsAcceptingRoutesAfterMaxRoutesReached() {
        AStarRouter router = mock(AStarRouter.class);
        RouteAlternativeService service = service(router); // maxRoutes = 3
        when(router.findJourneyCandidate(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                any(), any(), any(LocalTime.class), any(LocalDate.class), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(candidate(journey(1000, "BUS"), 1, 2))
                .thenReturn(candidate(journey(1100, "BUS"), 3, 4))
                .thenReturn(candidate(journey(1200, "BUS"), 5, 6))
                .thenReturn(candidate(journey(1300, "BUS"), 7, 8));

        RouteAlternativesResponse response = service.findAlternatives(1, 1, 2, 2, null, null, LocalTime.NOON, TEST_DATE, true, true, RoutingTimeMode.DEPART_AT);

        assertThat(response.routes()).hasSize(3);
    }

    @Test
    void qualityFilterDropsRoutesTooMuchSlowerThanFastest() {
        AStarRouter router = mock(AStarRouter.class);
        // maxSlowdownMultiplier = 1.4 → fastest=1000s, cutoff=1400s → 3000s route is dropped
        RouteAlternativeService service = new RouteAlternativeService(router, 3, 0.8, 1.4, 180);
        when(router.findJourneyCandidate(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                any(), any(), any(LocalTime.class), any(LocalDate.class), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(candidate(journey(1000, "BUS"), 1, 2))
                .thenReturn(candidate(journey(3000, "BIKE"), 3, 4))
                .thenReturn(null);

        RouteAlternativesResponse response = service.findAlternatives(1, 1, 2, 2, null, null, LocalTime.NOON, TEST_DATE, true, true, RoutingTimeMode.DEPART_AT);

        assertThat(response.routes()).hasSize(1);
        assertThat(response.routes().getFirst().modes()).containsExactly("BUS");
    }

    @Test
    void returnsNotFoundStatusWithEmptyRoutesWhenNoCandidate() {
        AStarRouter router = mock(AStarRouter.class);
        RouteAlternativeService service = service(router);
        when(router.findJourneyCandidate(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                any(), any(), any(LocalTime.class), any(LocalDate.class), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(null);

        RouteAlternativesResponse response = service.findAlternatives(46.55, 15.64, 46.56, 15.65, "Origin", "Dest", LocalTime.NOON, TEST_DATE, true, true, RoutingTimeMode.DEPART_AT);

        assertThat(response.status()).isEqualTo("not_found");
        assertThat(response.routes()).isEmpty();
    }

    private RouteAlternativeService service(AStarRouter router) {
        return new RouteAlternativeService(router, 3, 0.8, 2.0, 180);
    }

    private AStarRouter.RouteCandidate candidate(Journey journey, Integer... nodeIds) {
        return new AStarRouter.RouteCandidate(
                journey,
                new PathResult(List.of(nodeIds), List.of(), (int) (Long.parseLong(journey.duration()) / 1000))
        );
    }

    private Journey journey(long durationSeconds, String... modes) {
        List<Leg> legs = java.util.Arrays.stream(modes)
                .map(mode -> new Leg(
                        mode,
                        new GeoPoint(1, 1),
                        new GeoPoint(2, 2),
                        String.valueOf(durationSeconds * 1000),
                        "1000",
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        "0",
                        String.valueOf(durationSeconds * 1000)
                ))
                .toList();
        return new Journey(
                "success",
                new GeoPoint(1, 1),
                null,
                new GeoPoint(2, 2),
                null,
                String.valueOf(durationSeconds * 1000),
                "1000",
                legs
        );
    }
}
