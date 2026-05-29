package com.sibam.it;

import com.sibam.dto.prediction.BikePredictionRequest;
import com.sibam.dto.prediction.BikePredictionResponse;
import com.sibam.integration.supabase.SupabaseStorageClient;
import com.sibam.service.BikePredictionService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // @BeforeAll can be non-static so it can assign to instance fields
@EnabledIfEnvironmentVariable(named = "SUPABASE_URL", matches = ".+") // whole test class is skipped when env var isn't set
public class BikePredictionServiceIT {
    private BikePredictionService service;

    @BeforeAll
    public void loadModels() throws Exception {
        // Build SupabaseStorageClient without Spring — inject fields manually
        SupabaseStorageClient client = new SupabaseStorageClient();
        ReflectionTestUtils.setField(client, "supabaseUrl", System.getenv("SUPABASE_URL"));
        ReflectionTestUtils.setField(client, "serviceKey", System.getenv("SUPABASE_SERVICE_KEY"));

        // Call loadModels() manually — Spring would normally trigger this via @PostConstruct
        service = new BikePredictionService(client);
        service.loadModels();
    }

    @Test
    void allFourModelsLoadWithoutError() {
        assertThat(service).isNotNull();
    }

    @Test
    void predictReturnsProbabilitiesBetweenZeroAndOne() throws Exception {
        BikePredictionRequest request = new BikePredictionRequest(15, 8, 1, 0, 20.0f, 0.0f, 2.5f);
        BikePredictionResponse response = service.predict(request);

        //outputs must be valid
        assertThat(response.bikeAvailableProbability()).isBetween(0.0, 1.0);
        assertThat(response.standAvailableProbability()).isBetween(0.0, 1.0);
    }

    @Test
    void predictReturnsNonNegativeCounts() throws Exception {
        BikePredictionRequest request = new BikePredictionRequest(15, 8, 1, 0, 20.0f, 0.0f, 2.5f);
        BikePredictionResponse response = service.predict(request);

        // if negative -> broken model
        assertThat(response.predictedBikes()).isGreaterThanOrEqualTo(0);
        assertThat(response.predictedStands()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void predictHandlesWeekendAndRain() throws Exception {
        BikePredictionRequest request = new BikePredictionRequest(3, 17, 6, 1, 12.0f, 5.0f, 8.0f);
        BikePredictionResponse response = service.predict(request);

        assertThat(response.bikeAvailableProbability()).isBetween(0.0, 1.0);
        assertThat(response.standAvailableProbability()).isBetween(0.0, 1.0);
    }


}
