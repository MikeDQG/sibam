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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        "supabase.url=http://localhost",
        "supabase.service-key=test",
        "supabase.service-role-key=test",
        "supabase.cache.enabled=false",
        "spring.task.scheduling.enabled=false"
})
@ActiveProfiles("ci")
class SibamApplicationTests {

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
