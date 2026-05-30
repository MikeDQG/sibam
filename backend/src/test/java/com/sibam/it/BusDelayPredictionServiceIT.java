package com.sibam.it;

import ai.onnxruntime.OrtException;
import com.sibam.integration.supabase.SupabaseStorageClient;
import com.sibam.service.BikePredictionService;
import com.sibam.service.BusDelayPredictionService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "SUPABASE_URL", matches = ".+")
public class BusDelayPredictionServiceIT {
    private BusDelayPredictionService service;

    @BeforeAll
    public void loadModel() throws Exception {
        SupabaseStorageClient client = new SupabaseStorageClient();
        ReflectionTestUtils.setField(client, "supabaseUrl", System.getenv("SUPABASE_URL"));
        ReflectionTestUtils.setField(client, "serviceKey", System.getenv("SUPABASE_SERVICE_KEY"));

        service = new BusDelayPredictionService(client);
        service.load();
    }

    @Test
    void modelAndDirectionMappingLoadWithoutError() {
        assertThat(service).isNotNull();
    }

    @Test
    void unknownStopReturnsZeroWithoutCallingModel() throws OrtException {
        // stopId 9999 is not in stop_direction_mapping.json → direction = -1 → guard returns 0 immediately, no calling model
        int delay = service.predictDelay(9999, 1, 8, 1, 0, 20.0f, 0.0f, 2.5f, 9999);

        assertThat(delay).isEqualTo(0);
    }

    @Test
    void knownStopRunsModelAndReturnsReasonableDelay() throws OrtException {
        int delay = service.predictDelay(84, 3, 8, 1, 0, 20.0f, 0.0f, 2.5f, 17);

        // -900s = 15 min early, 3600s = 1 hour late — else broken model
        assertThat(delay).isBetween(-900, 3600);
    }

    @Test
    void predictionIsValidForMultipleInputCombinations() throws OrtException {
        int morningPeak  = service.predictDelay(84, 3, 8,  1, 0, 20.0f, 0.0f, 2.5f, 17);
        int earlyMorning = service.predictDelay(84, 3, 22,  1, 0, 20.0f, 0.0f, 2.5f, 17);

        assertThat(morningPeak).isBetween(-900, 3600);
        assertThat(earlyMorning).isBetween(-900, 3600);
    }
}
