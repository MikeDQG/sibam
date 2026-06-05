package com.sibam;

import com.sibam.engine.VaoSerializer;
import com.sibam.graph.bootstrap.GraphBootstrap;
import com.sibam.integration.gtfsRT.GTFSRTClient;
import com.sibam.integration.mbajk.MBajkClient;
import com.sibam.integration.weather.WeatherClient;
import com.sibam.repository.BikeStationRepository;
import com.sibam.repository.BikeStationSnapshotRepository;
import com.sibam.repository.SavedLocationRepository;
import com.sibam.repository.SavedPathRepository;
import com.sibam.repository.StopDelaySnapshotRepository;
import com.sibam.repository.TripSnapshotRepository;
import com.sibam.repository.UserRepository;
import com.sibam.repository.WeatherSnapshotRepository;
import com.sibam.scheduler.SchedulerService;
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
        "supabase.service-role-key=dummy",
        "supabase.cache.enabled=false",
        "mbajk.api.key=dummy",
        "openweathermap.api.key=dummy",
        "routes.google.api-key=dummy",
        "spring.task.scheduling.enabled=false",
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
    VaoSerializer vaoSerializer;

    @MockitoBean
    GraphBootstrap graphBootstrap;

    @MockitoBean
    SchedulerService schedulerService;

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

    @MockitoBean
    WeatherSnapshotRepository weatherSnapshotRepository;

    @MockitoBean
    SavedLocationRepository savedLocationRepository;

    @MockitoBean
    SavedPathRepository savedPathRepository;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    BikeStationRepository bikeStationRepository;

    @MockitoBean
    BikeStationSnapshotRepository bikeStationSnapshotRepository;

    @MockitoBean
    StopDelaySnapshotRepository stopDelaySnapshotRepository;

    @MockitoBean
    TripSnapshotRepository tripSnapshotRepository;

    @Test
    void contextLoads() {
    }
}
