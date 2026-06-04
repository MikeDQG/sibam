package com.sibam.service;

import com.sibam.integration.supabase.SupabaseStorageClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BusDelayPredictionServiceTest {

    /*
     * BusDelayPredictionService.@PostConstruct downloads an ONNX model from Supabase
     * and loads a JSON resource — neither is available in unit tests. These tests
     * bypass @PostConstruct and directly inject the state needed to exercise
     * branches that don't touch the ONNX session.
     */

    private BusDelayPredictionService serviceWithMapping(Map<String, Integer> mapping) {
        BusDelayPredictionService service = new BusDelayPredictionService(mock(SupabaseStorageClient.class));
        ReflectionTestUtils.setField(service, "directionMapping", mapping);
        return service;
    }

    @Test
    void predictDelayReturnsZeroWhenRouteStopKeyNotInMapping() throws Exception {
        BusDelayPredictionService service = serviceWithMapping(Map.of("1_10", 0));

        // key "99_999" is not in the mapping → early return 0
        int result = service.predictDelay(99, 1, 12, 1, 0, 15.0f, 0.0f, 2.0f, 999);

        assertThat(result).isZero();
    }

    @Test
    void predictDelayReturnsZeroWhenMappingIsEmpty() throws Exception {
        BusDelayPredictionService service = serviceWithMapping(Map.of());

        int result = service.predictDelay(1, 3, 8, 2, 0, 20.0f, 0.0f, 1.5f, 42);

        assertThat(result).isZero();
    }
}
