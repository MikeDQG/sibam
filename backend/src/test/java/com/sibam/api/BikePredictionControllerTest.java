package com.sibam.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibam.dto.prediction.BikePredictionRequest;
import com.sibam.dto.prediction.BikePredictionResponse;
import com.sibam.service.BikePredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class BikePredictionControllerTest {
    private final BikePredictionService service = mock(BikePredictionService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new BikePredictionController(service))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void predictBikesReturnsAllFourFields() throws Exception {
        BikePredictionResponse response = new BikePredictionResponse(5, 3, 0.87, 0.65);
        when(service.predict(any(BikePredictionRequest.class))).thenReturn(response);

        BikePredictionRequest request = new BikePredictionRequest(
                15, 8, 1, 0, 20.0f, 0.0f, 2.5f
        );

        mockMvc.perform(post("/predict/bikes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictedBikes").value(5))
                .andExpect(jsonPath("$.predictedStands").value(3))
                .andExpect(jsonPath("$.bikeAvailableProbability").value(0.87))
                .andExpect(jsonPath("$.standAvailableProbability").value(0.65));
    }

    @Test
    void predictBikesWithZeroAvailability() throws Exception {
        BikePredictionResponse response = new BikePredictionResponse(0, 12, 0.02, 0.98);
        when(service.predict(any(BikePredictionRequest.class))).thenReturn(response);

        BikePredictionRequest request = new BikePredictionRequest(
                15, 8, 1, 0, 20.0f, 0.0f, 2.5f
        );

        mockMvc.perform(post("/predict/bikes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictedBikes").value(0))
                .andExpect(jsonPath("$.predictedStands").value(12));
    }

    @Test
    void missingRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/predict/bikes")
                        .contentType("application/json"))
                .andExpect(status().isBadRequest());
    }
}
