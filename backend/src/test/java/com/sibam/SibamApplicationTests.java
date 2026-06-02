package com.sibam;

import com.sibam.integration.gtfsRT.GTFSRTClient;
import com.sibam.integration.mbajk.MBajkClient;
import com.sibam.integration.weather.WeatherClient;
import com.sibam.service.BikePredictionService;
import com.sibam.service.BusDelayPredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class SibamApplicationTests {

    @MockitoBean
    BikePredictionService bikePredictionService;

    @MockitoBean
    BusDelayPredictionService busDelayPredictionService;

    @MockitoBean
    MBajkClient mbajkClient;

    @MockitoBean
    GTFSRTClient gtfsRTClient;

    @MockitoBean
    WeatherClient weatherClient;

    @Test
    void contextLoads() {
    }

}
