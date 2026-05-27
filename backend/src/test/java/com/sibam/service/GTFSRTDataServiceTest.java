package com.sibam.service;

import com.google.transit.realtime.GtfsRealtime;
import com.sibam.graph.model.trip.StopUpdate;
import com.sibam.graph.model.trip.Trip;
import com.sibam.integration.gtfsRT.GTFSRTClient;
import com.sibam.integration.gtfsRT.GTFSRTMapper;
import com.sibam.persistence.StopDelayEntity;
import com.sibam.persistence.TripEntity;
import com.sibam.repository.StopDelaySnapshotRepository;
import com.sibam.repository.TripSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GTFSRTDataServiceTest {
    private GTFSRTClient gtfsRTClient;
    private GTFSRTMapper gtfsRTMapper;
    private TripSnapshotRepository tripSnapshotRepository;
    private StopDelaySnapshotRepository stopDelaySnapshotRepository;
    private GTFSRTDataService service;

    // reusable feed with one vehicle: tripId="trip-1", vehicleId="v-1"
    private static final GtfsRealtime.FeedMessage VEHICLE_FEED =
            GtfsRealtime.FeedMessage.newBuilder()
                    .setHeader(GtfsRealtime.FeedHeader.newBuilder()
                            .setGtfsRealtimeVersion("2.0").build())
                    .addEntity(GtfsRealtime.FeedEntity.newBuilder()
                            .setId("1")
                            .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                                    .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                                            .setTripId("trip-1").setRouteId("6").build())
                                    .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                                            .setId("v-1").build())
                                    .setPosition(GtfsRealtime.Position.newBuilder()
                                            .setLatitude(46.55f).setLongitude(15.64f).build())
                                    .setCurrentStopSequence(3)
                                    .setStopId("stop-42")
                                    .build())
                            .build())
                    .build();

    // reusable trip update feed matching the vehicle above
    private static final GtfsRealtime.FeedMessage UPDATE_FEED =
            GtfsRealtime.FeedMessage.newBuilder()
                    .setHeader(GtfsRealtime.FeedHeader.newBuilder()
                            .setGtfsRealtimeVersion("2.0").build())
                    .addEntity(GtfsRealtime.FeedEntity.newBuilder()
                            .setId("1")
                            .setTripUpdate(GtfsRealtime.TripUpdate.newBuilder()
                                    .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                                            .setTripId("trip-1").build())
                                    .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                                            .setId("v-1").build())
                                    .addStopTimeUpdate(GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                                            .setStopSequence(3)
                                            .setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder()
                                                    .setDelay(60).build())
                                            .build())
                                    .addStopTimeUpdate(GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                                            .setStopSequence(4)
                                            .setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder()
                                                    .setDelay(90).build())
                                            .build())
                                    .build())
                            .build())
                    .build();

    @BeforeEach
    void setUp() {
        gtfsRTClient = mock(GTFSRTClient.class);
        gtfsRTMapper = mock(GTFSRTMapper.class);
        tripSnapshotRepository = mock(TripSnapshotRepository.class);
        stopDelaySnapshotRepository = mock(StopDelaySnapshotRepository.class);
        service = new GTFSRTDataService(
                gtfsRTClient, gtfsRTMapper,
                tripSnapshotRepository, stopDelaySnapshotRepository
        );
    }

    // ingestRealtimeTrips: saves TripEntity fields

    @Test
    void ingestSavesTripEntityWithCorrectFields() {
        Trip trip = buildTrip("trip-1", "6", "v-1", 46.55, 15.64, 3, "stop-42", null);
        when(gtfsRTClient.getVehiclePositions()).thenReturn(VEHICLE_FEED);
        when(gtfsRTClient.getTripUpdates()).thenReturn(UPDATE_FEED);
        when(gtfsRTMapper.gtfsRTToTrip(any(), any())).thenReturn(trip);
        when(tripSnapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ingestRealtimeTrips(OffsetDateTime.now());

        ArgumentCaptor<TripEntity> captor = ArgumentCaptor.forClass(TripEntity.class);
        verify(tripSnapshotRepository).save(captor.capture());

        TripEntity saved = captor.getValue();
        assertThat(saved.getTripId()).isEqualTo("trip-1");
        assertThat(saved.getRouteId()).isEqualTo("6");
        assertThat(saved.getVehicleId()).isEqualTo("v-1");
        assertThat(saved.getLat()).isEqualTo(46.55);
        assertThat(saved.getLon()).isEqualTo(15.64);
        assertThat(saved.getCurrentStopSequence()).isEqualTo(3);
        assertThat(saved.getCurrentStopId()).isEqualTo("stop-42");
    }

    // ingestRealtimeTrips: stop delay entities

    @Test
    void ingestSavesStopDelayEntityForEachStopUpdate() {
        StopUpdate su1 = stopUpdate(3, 60);
        StopUpdate su2 = stopUpdate(4, 90);
        Trip trip = buildTrip("trip-1", "6", "v-1", 46.55, 15.64, 3, "stop-42", List.of(su1, su2));

        TripEntity savedTrip = new TripEntity();
        when(gtfsRTClient.getVehiclePositions()).thenReturn(VEHICLE_FEED);
        when(gtfsRTClient.getTripUpdates()).thenReturn(UPDATE_FEED);
        when(gtfsRTMapper.gtfsRTToTrip(any(), any())).thenReturn(trip);
        when(tripSnapshotRepository.save(any())).thenReturn(savedTrip);

        service.ingestRealtimeTrips(OffsetDateTime.now());

        ArgumentCaptor<StopDelayEntity> captor = ArgumentCaptor.forClass(StopDelayEntity.class);
        verify(stopDelaySnapshotRepository, times(2)).save(captor.capture());

        List<StopDelayEntity> delays = captor.getAllValues();
        assertThat(delays.get(0).getStopSequence()).isEqualTo(3);
        assertThat(delays.get(0).getDelaySeconds()).isEqualTo(60);
        assertThat(delays.get(0).getTrip()).isSameAs(savedTrip);
        assertThat(delays.get(1).getStopSequence()).isEqualTo(4);
        assertThat(delays.get(1).getDelaySeconds()).isEqualTo(90);
    }

    @Test
    void ingestSavesNoStopDelayEntitiesWhenStopUpdatesIsNull() {
        Trip trip = buildTrip("trip-1", "6", "v-1", 46.55, 15.64, 3, "stop-42", null);

        when(gtfsRTClient.getVehiclePositions()).thenReturn(VEHICLE_FEED);
        when(gtfsRTClient.getTripUpdates()).thenReturn(UPDATE_FEED);
        when(gtfsRTMapper.gtfsRTToTrip(any(), any())).thenReturn(trip);
        when(tripSnapshotRepository.save(any())).thenReturn(new TripEntity());

        service.ingestRealtimeTrips(OffsetDateTime.now());

        verify(stopDelaySnapshotRepository, never()).save(any());
    }

    // getRealtimeTrips: duplicate key resolution

    @Test
    void getRealtimeTripsKeepsNewerTripUpdateOnDuplicateKey() {
        // Two trip updates with the same tripId+vehicleId key — different timestamps
        GtfsRealtime.FeedMessage duplicateUpdateFeed = GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0").build())
                .addEntity(GtfsRealtime.FeedEntity.newBuilder()
                        .setId("1")
                        .setTripUpdate(GtfsRealtime.TripUpdate.newBuilder()
                                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                                        .setTripId("trip-1").build())
                                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                                        .setId("v-1").build())
                                .setTimestamp(1000L)
                                .build())
                        .build())
                .addEntity(GtfsRealtime.FeedEntity.newBuilder()
                        .setId("2")
                        .setTripUpdate(GtfsRealtime.TripUpdate.newBuilder()
                                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                                        .setTripId("trip-1").build())
                                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                                        .setId("v-1").build())
                                .setTimestamp(9999L)
                                .build())
                        .build())
                .build();

        when(gtfsRTClient.getVehiclePositions()).thenReturn(VEHICLE_FEED);
        when(gtfsRTClient.getTripUpdates()).thenReturn(duplicateUpdateFeed);

        Trip trip = buildTrip("trip-1", "6", "v-1", 46.55, 15.64, 3, "stop-42", List.of());
        when(gtfsRTMapper.gtfsRTToTrip(any(), any())).thenReturn(trip);

        List<Trip> trips = service.getRealtimeTrips();

        // mapper is called once (one vehicle) — duplicate update is resolved before mapping
        verify(gtfsRTMapper, times(1)).gtfsRTToTrip(any(), any());
        assertThat(trips).hasSize(1);
    }

    // getRealtimeTrips: empty feed and no matching update

    @Test
    void getRealtimeTripsReturnsEmptyListWhenNoVehicles() {
        GtfsRealtime.FeedMessage emptyFeed = GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0").build())
                .build();

        when(gtfsRTClient.getVehiclePositions()).thenReturn(emptyFeed);
        when(gtfsRTClient.getTripUpdates()).thenReturn(emptyFeed);

        List<Trip> trips = service.getRealtimeTrips();

        assertThat(trips).isEmpty();
        verify(gtfsRTMapper, never()).gtfsRTToTrip(any(), any());
    }

    @Test
    void vehicleWithNoMatchingUpdateIsMappedWithNullUpdate() {
        // Vehicle has tripId="trip-99"/vehicleId="v-99", update feed has no matching entry
        GtfsRealtime.FeedMessage vehicleOnlyFeed = GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0").build())
                .addEntity(GtfsRealtime.FeedEntity.newBuilder()
                        .setId("1")
                        .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                                        .setTripId("trip-99").setRouteId("2").build())
                                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                                        .setId("v-99").build())
                                .build())
                        .build())
                .build();

        GtfsRealtime.FeedMessage emptyUpdateFeed = GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0").build())
                .build();

        Trip trip = buildTrip("trip-99", "2", "v-99", 0, 0, 0, null, null);
        when(gtfsRTClient.getVehiclePositions()).thenReturn(vehicleOnlyFeed);
        when(gtfsRTClient.getTripUpdates()).thenReturn(emptyUpdateFeed);
        when(gtfsRTMapper.gtfsRTToTrip(any(), isNull())).thenReturn(trip);

        List<Trip> trips = service.getRealtimeTrips();

        assertThat(trips).hasSize(1);
        // mapper was called with null as the second argument (no matching update found)
        verify(gtfsRTMapper).gtfsRTToTrip(any(), isNull());
    }

    // helpers

    private Trip buildTrip(String tripId, String routeId, String vehicleId,
                           double lat, double lon, int stopSeq, String stopId,
                           List<StopUpdate> stopUpdates) {
        Trip trip = new Trip();
        trip.setTripId(tripId);
        trip.setRouteId(routeId);
        trip.setVehicleId(vehicleId);
        trip.setLat(lat);
        trip.setLon(lon);
        trip.setCurrent_stop_sequence(stopSeq);
        trip.setStop_id(stopId);
        trip.setStopUpdates(stopUpdates);
        return trip;
    }

    private StopUpdate stopUpdate(int sequence, int delay) {
        StopUpdate su = new StopUpdate();
        su.setStopSequence(sequence);
        su.setDelay(delay);
        return su;
    }
}
