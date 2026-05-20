package com.sibam.api;

import com.sibam.engine.VaoSerializer;
import com.sibam.graph.bootstrap.GraphBootstrap;
import com.sibam.graph.routing.AStarRouter;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ComputePathControllerTest {

    @Test
    void fallbackResponseKeepsRequestedCoordinates() throws Exception {
        VaoSerializer vaoSerializer = mock(VaoSerializer.class);
        GraphBootstrap graphBootstrap = mock(GraphBootstrap.class);
        AStarRouter aStarRouter = mock(AStarRouter.class);
        ComputePathController controller = new ComputePathController(vaoSerializer, graphBootstrap, aStarRouter);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        double originLat = 46.538077;
        double originLon = 15.603520;
        double destinationLat = 46.561754;
        double destinationLon = 15.629752;

        when(aStarRouter.findJourney(
                eq(originLat),
                eq(originLon),
                eq(destinationLat),
                eq(destinationLon),
                eq(null),
                eq(null),
                any(LocalTime.class),
                eq(true),
                eq(true)
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
                .andExpect(jsonPath("$.origin.lat").value(originLat))
                .andExpect(jsonPath("$.origin.lon").value(originLon))
                .andExpect(jsonPath("$.destination.lat").value(destinationLat))
                .andExpect(jsonPath("$.destination.lon").value(destinationLon));

        verify(graphBootstrap).refresh();
    }
}
