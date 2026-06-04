package com.sibam.api;

import com.sibam.engine.VaoSerializer;
import com.sibam.graph.bootstrap.GraphBootstrap;
import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.RouteAlternativeLabel;
import com.sibam.graph.model.output.RouteAlternative;
import com.sibam.graph.model.output.RouteAlternativesResponse;
import com.sibam.graph.routing.RouteAlternativeService;
import com.sibam.graph.routing.RouteAccessDistanceException;
import com.sibam.graph.routing.RoutingTimeMode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ComputePathControllerTest {

    @Test
    void returnsRoutesArray() throws Exception {
        VaoSerializer vaoSerializer = mock(VaoSerializer.class);
        GraphBootstrap graphBootstrap = mock(GraphBootstrap.class);
        RouteAlternativeService routeAlternativeService = mock(RouteAlternativeService.class);
        ComputePathController controller = new ComputePathController(vaoSerializer, graphBootstrap, routeAlternativeService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        RouteAlternative route = new RouteAlternative(
                1,
                new GeoPoint(1, 1),
                null,
                new GeoPoint(2, 2),
                null,
                RouteAlternativeLabel.FASTEST.displayName(),
                List.of(RouteAlternativeLabel.FASTEST.displayName()),
                100,
                500,
                List.of("WALK"),
                List.of()
        );
        when(routeAlternativeService.findAlternatives(
                anyDouble(),
                anyDouble(),
                anyDouble(),
                anyDouble(),
                any(),
                any(),
                any(LocalTime.class),
                any(LocalDate.class),
                eq(false),
                eq(false),
                eq(RoutingTimeMode.DEPART_AT)
        )).thenReturn(new RouteAlternativesResponse(
                "success",
                List.of(route)
        ));

        mockMvc.perform(get("/compute")
                        .param("originLat", "1")
                        .param("originLon", "1")
                        .param("destinationLat", "2")
                        .param("destinationLon", "2")
                        .param("leaveNow", "true")
                        .param("bike", "false")
                        .param("bus", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes").isArray())
                .andExpect(jsonPath("$.routes[0].rank").value(1))
                .andExpect(jsonPath("$.routes[0].origin.lat").value(1))
                .andExpect(jsonPath("$.routes[0].origin.lon").value(1))
                .andExpect(jsonPath("$.routes[0].destination.lat").value(2))
                .andExpect(jsonPath("$.routes[0].destination.lon").value(2))
                .andExpect(jsonPath("$.routes[0].labels[0]").value(RouteAlternativeLabel.FASTEST.displayName()))
                .andExpect(jsonPath("$.routes[0].label").value(RouteAlternativeLabel.FASTEST.displayName()))
                .andExpect(jsonPath("$.origin").doesNotExist())
                .andExpect(jsonPath("$.destination").doesNotExist())
                .andExpect(jsonPath("$.route").doesNotExist())
                .andExpect(jsonPath("$.bestRoute").doesNotExist());
    }

    @Test
    void fallbackResponseKeepsRequestedCoordinates() throws Exception {
        VaoSerializer vaoSerializer = mock(VaoSerializer.class);
        GraphBootstrap graphBootstrap = mock(GraphBootstrap.class);
        RouteAlternativeService routeAlternativeService = mock(RouteAlternativeService.class);
        ComputePathController controller = new ComputePathController(vaoSerializer, graphBootstrap, routeAlternativeService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        double originLat = 46.538077;
        double originLon = 15.603520;
        double destinationLat = 46.561754;
        double destinationLon = 15.629752;

        when(routeAlternativeService.findAlternatives(
                eq(originLat),
                eq(originLon),
                eq(destinationLat),
                eq(destinationLon),
                eq(null),
                eq(null),
                any(LocalTime.class),
                any(LocalDate.class),
                eq(true),
                eq(true),
                eq(RoutingTimeMode.DEPART_AT)
        )).thenReturn(null);

        mockMvc.perform(get("/compute")
                        .param("originLat", String.valueOf(originLat))
                        .param("originLon", String.valueOf(originLon))
                        .param("destinationLat", String.valueOf(destinationLat))
                        .param("destinationLon", String.valueOf(destinationLon))
                        .param("leaveNow", "true")
                        .param("bike", "true")
                        .param("bus", "true")
                        .param("userId", "sjdfb7e4tfg74g74"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes").isArray())
                .andExpect(jsonPath("$.routes").isEmpty())
                .andExpect(jsonPath("$.origin").doesNotExist())
                .andExpect(jsonPath("$.destination").doesNotExist());

        verify(graphBootstrap).refresh();
    }

    @Test
    void returnsBadRequestWhenEndpointIsOutsideServiceArea() throws Exception {
        VaoSerializer vaoSerializer = mock(VaoSerializer.class);
        GraphBootstrap graphBootstrap = mock(GraphBootstrap.class);
        RouteAlternativeService routeAlternativeService = mock(RouteAlternativeService.class);
        ComputePathController controller = new ComputePathController(vaoSerializer, graphBootstrap, routeAlternativeService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        double originLat = 46.538077;
        double originLon = 15.603520;
        double destinationLat = 46.561754;
        double destinationLon = 15.629752;

        when(routeAlternativeService.findAlternatives(
                eq(originLat),
                eq(originLon),
                eq(destinationLat),
                eq(destinationLon),
                eq(null),
                eq(null),
                any(LocalTime.class),
                any(LocalDate.class),
                eq(true),
                eq(true),
                eq(RoutingTimeMode.DEPART_AT)
        )).thenThrow(new RouteAccessDistanceException("origin", 3200.0, 3000));

        mockMvc.perform(get("/compute")
                        .param("originLat", String.valueOf(originLat))
                        .param("originLon", String.valueOf(originLon))
                        .param("destinationLat", String.valueOf(destinationLat))
                        .param("destinationLon", String.valueOf(destinationLon))
                        .param("leaveNow", "true")
                        .param("bike", "true")
                        .param("bus", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("IZVEN_OBMOCJA_POTI"))
                .andExpect(jsonPath("$.endpoint").value("origin"))
                .andExpect(jsonPath("$.distanceMeters").value(3200.0))
                .andExpect(jsonPath("$.maxDistanceMeters").value(3000));
    }

    @Test
    void forwardsArriveByTimeModeToRouter() throws Exception {
        VaoSerializer vaoSerializer = mock(VaoSerializer.class);
        GraphBootstrap graphBootstrap = mock(GraphBootstrap.class);
        RouteAlternativeService routeAlternativeService = mock(RouteAlternativeService.class);
        ComputePathController controller = new ComputePathController(vaoSerializer, graphBootstrap, routeAlternativeService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        double originLat = 46.538077;
        double originLon = 15.603520;
        double destinationLat = 46.561754;
        double destinationLon = 15.629752;

        when(routeAlternativeService.findAlternatives(
                eq(originLat),
                eq(originLon),
                eq(destinationLat),
                eq(destinationLon),
                eq(null),
                eq(null),
                eq(LocalTime.of(10, 30)),
                eq(LocalDate.parse("2026-06-04")),
                eq(true),
                eq(true),
                eq(RoutingTimeMode.ARRIVE_BY)
        )).thenReturn(null);

        mockMvc.perform(get("/compute")
                        .param("originLat", String.valueOf(originLat))
                        .param("originLon", String.valueOf(originLon))
                        .param("destinationLat", String.valueOf(destinationLat))
                        .param("destinationLon", String.valueOf(destinationLon))
                        .param("leaveNow", "false")
                        .param("arriveBy", "10:30")
                        .param("date", "2026-06-04")
                        .param("bike", "true")
                        .param("bus", "true"))
                .andExpect(status().isOk());

        verify(graphBootstrap).refresh();
    }
}
