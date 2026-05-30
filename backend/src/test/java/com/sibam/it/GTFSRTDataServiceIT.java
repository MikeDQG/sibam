package com.sibam.it;

import com.google.transit.realtime.GtfsRealtime;
import com.sibam.persistence.StopDelayEntity;
import com.sibam.persistence.TripEntity;
import com.sibam.repository.StopDelaySnapshotRepository;
import com.sibam.repository.TripSnapshotRepository;
import com.sibam.service.GTFSRTDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Transactional
class GTFSRTDataServiceIT extends AbstractDatabaseIT {
    @Autowired
    GTFSRTDataService gtfsRTDataService;

    @Autowired
    TripSnapshotRepository tripSnapshotRepository;

    @Autowired
    StopDelaySnapshotRepository stopDelaySnapshotRepository;

    private GtfsRealtime.FeedMessage vehicleFeed(String tripId, String vehicleId, String routeId) {
        return GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0").build())
                .addEntity(GtfsRealtime.FeedEntity.newBuilder()
                        .setId("e1")
                        .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                                        .setTripId(tripId).setRouteId(routeId).build())
                                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                                        .setId(vehicleId).build())
                                .setPosition(GtfsRealtime.Position.newBuilder()
                                        .setLatitude(46.55f).setLongitude(15.64f).build())
                                .setCurrentStopSequence(2)
                                .setStopId("stop-10")
                                .build())
                        .build())
                .build();
    }

    private GtfsRealtime.FeedMessage updateFeed(String tripId, String vehicleId, int delay) {
        return GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0").build())
                .addEntity(GtfsRealtime.FeedEntity.newBuilder()
                        .setId("e2")
                        .setTripUpdate(GtfsRealtime.TripUpdate.newBuilder()
                                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                                        .setTripId(tripId).build())
                                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                                        .setId(vehicleId).build())
                                .addStopTimeUpdate(GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                                        .setStopSequence(2)
                                        .setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder()
                                                .setDelay(delay).build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private GtfsRealtime.FeedMessage emptyFeed() {
        return GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0").build())
                .build();
    }

    @Test
    void ingestRealtimeTripsCreatesTripsInDb() {
        when(gtfsRTClient.getVehiclePositions()).thenReturn(vehicleFeed("t1", "v1", "6"));
        when(gtfsRTClient.getTripUpdates()).thenReturn(emptyFeed());

        gtfsRTDataService.ingestRealtimeTrips(OffsetDateTime.now());

        List<TripEntity> trips = tripSnapshotRepository.findAll();
        assertThat(trips).hasSize(1);
        assertThat(trips.get(0).getTripId()).isEqualTo("t1");
        assertThat(trips.get(0).getRouteId()).isEqualTo("6");
        assertThat(trips.get(0).getVehicleId()).isEqualTo("v1");
    }

    @Test
    void ingestRealtimeTripsCreatesStopDelayEntities() {
        when(gtfsRTClient.getVehiclePositions()).thenReturn(vehicleFeed("t2", "v2", "8"));
        when(gtfsRTClient.getTripUpdates()).thenReturn(updateFeed("t2", "v2", 120));

        gtfsRTDataService.ingestRealtimeTrips(OffsetDateTime.now());

        List<StopDelayEntity> delays = stopDelaySnapshotRepository.findAll();
        assertThat(delays).hasSize(1);
        assertThat(delays.get(0).getDelaySeconds()).isEqualTo(120);
        assertThat(delays.get(0).getStopSequence()).isEqualTo(2);
        assertThat(delays.get(0).getTrip()).isNotNull();
    }

    @Test
    void ingestRealtimeTripsWithEmptyFeedSavesNothing() {
        when(gtfsRTClient.getVehiclePositions()).thenReturn(emptyFeed());
        when(gtfsRTClient.getTripUpdates()).thenReturn(emptyFeed());

        gtfsRTDataService.ingestRealtimeTrips(OffsetDateTime.now());

        assertThat(tripSnapshotRepository.findAll()).isEmpty();
        assertThat(stopDelaySnapshotRepository.findAll()).isEmpty();
    }
    @Test
    void ingestMultipleTripsCreatesAllEntities() {
        // Two vehicles, one with a delay update, one without
        GtfsRealtime.FeedMessage twoVehicles = GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0").build())
                .addEntity(GtfsRealtime.FeedEntity.newBuilder().setId("e1")
                        .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                                        .setTripId("t3").setRouteId("3").build())
                                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                                        .setId("v3").build())
                                .build())
                        .build())
                .addEntity(GtfsRealtime.FeedEntity.newBuilder().setId("e2")
                        .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                                        .setTripId("t4").setRouteId("4").build())
                                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                                        .setId("v4").build())
                                .build())
                        .build())
                .build();

        when(gtfsRTClient.getVehiclePositions()).thenReturn(twoVehicles);
        when(gtfsRTClient.getTripUpdates()).thenReturn(emptyFeed());

        gtfsRTDataService.ingestRealtimeTrips(OffsetDateTime.now());

        assertThat(tripSnapshotRepository.findAll()).hasSize(2);
    }



}
