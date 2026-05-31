package com.sibam.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibam.dto.prediction.BusDelayPredictionRequest;
import com.sibam.service.BusDelayPredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BusDelayPredictionControllerTest {
    private final BusDelayPredictionService service = mock(BusDelayPredictionService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new BusDelayPredictionController(service))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void predictReturnsDelayInSeconds() throws Exception {
        when(service.predictDelay(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyFloat(), anyFloat(), anyFloat(), anyInt()))
                .thenReturn(120);

        BusDelayPredictionRequest request = new BusDelayPredictionRequest(
                6, 4, 8, 1, 0, 18.5f, 0.0f, 3.2f, 1042
        );

        mockMvc.perform(post("/api/bus-delay/predict")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictedDelaySeconds").value(120));
    }

    @Test
    void predictAllowsNegativeDelayForEarlyBus() throws Exception {
        when(service.predictDelay(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyFloat(), anyFloat(), anyFloat(), anyInt()))
                .thenReturn(-45);

        BusDelayPredictionRequest request = new BusDelayPredictionRequest(
                6, 4, 8, 1, 0, 18.5f, 0.0f, 3.2f, 1042
        );

        mockMvc.perform(post("/api/bus-delay/predict")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictedDelaySeconds").value(-45));
    }

    @Test
    void predictReturnsZeroForOnTimeBus() throws Exception {
        when(service.predictDelay(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyFloat(), anyFloat(), anyFloat(), anyInt()))
                .thenReturn(0);

        BusDelayPredictionRequest request = new BusDelayPredictionRequest(
                6, 4, 8, 1, 0, 18.5f, 0.0f, 3.2f, 1042
        );

        mockMvc.perform(post("/api/bus-delay/predict")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictedDelaySeconds").value(0));
    }

    @Test
    void missingRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/bus-delay/predict")
                        .contentType("application/json"))
                .andExpect(status().isBadRequest());
    }
}
