package com.sibam;

import com.sibam.integration.gtfsRT.GTFSRTClient;
import com.sibam.integration.mbajk.MBajkClient;
import com.sibam.integration.weather.WeatherClient;
import com.sibam.service.BikePredictionService;
import com.sibam.service.BusDelayPredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "supabase.url=http://dummy",
        "supabase.service-key=dummy",
        "mbajk.api.key=dummy",
        "openweathermap.api.key=dummy",
        "schedulers.fetch-bike-ingestion.on=false",
        "schedulers.fetch-weather-ingestion.on=false",
        "schedulers.fetch-bus-ingestion.on=false"
})
class SibamApplicationTests {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

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
