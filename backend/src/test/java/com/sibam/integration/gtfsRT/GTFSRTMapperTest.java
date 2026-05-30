package com.sibam.integration.gtfsRT;

import com.google.transit.realtime.GtfsRealtime;
import com.sibam.graph.model.trip.Trip;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class GTFSRTMapperTest {

    private final GTFSRTMapper mapper = new GTFSRTMapper();

    @Test
    void vehiclePositionFieldsAreMappedCorrectly() {
        GtfsRealtime.VehiclePosition vehicle = GtfsRealtime.VehiclePosition.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                        .setTripId("trip-42")
                        .setRouteId("6")
                        .build())
                .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder()
                        .setId("v-1")
                        .setLabel("BUS-006")
                        .build())
                .setPosition(GtfsRealtime.Position.newBuilder()
                        .setLatitude(46.5547f)
                        .setLongitude(15.6459f)
                        .setBearing(180f)
                        .build())
                .setCurrentStopSequence(3)
                .setStopId("stop-1042")
                .setTimestamp(1700000000L)
                .build();

        Trip trip = mapper.gtfsRTToTrip(vehicle, null);

        assertThat(trip.getTripId()).isEqualTo("trip-42");
        assertThat(trip.getRouteId()).isEqualTo("6");
        assertThat(trip.getVehicleId()).isEqualTo("v-1");
        assertThat(trip.getVehicleLabel()).isEqualTo("BUS-006");
        assertThat(trip.getLat()).isCloseTo(46.5547, offset(0.001));
        assertThat(trip.getLon()).isCloseTo(15.6459, offset(0.001));
        assertThat(trip.getBearing()).isEqualTo(180f);
        assertThat(trip.getCurrent_stop_sequence()).isEqualTo(3);
        assertThat(trip.getStop_id()).isEqualTo("stop-1042");
        assertThat(trip.getTimestamp()).isEqualTo(1700000000L);
    }

    @Test
    void nullTripUpdateProducesNoStopUpdates() {
        GtfsRealtime.VehiclePosition vehicle = GtfsRealtime.VehiclePosition.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("trip-1").build())
                .build();

        Trip trip = mapper.gtfsRTToTrip(vehicle, null);

        assertThat(trip.getStopUpdates()).isNull();
    }

    @Test
    void stopUpdatesAreMappedFromTripUpdate() {
        GtfsRealtime.VehiclePosition vehicle = GtfsRealtime.VehiclePosition.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("trip-1").build())
                .build();

        GtfsRealtime.TripUpdate tripUpdate = GtfsRealtime.TripUpdate.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("trip-1").build())
                .addStopTimeUpdate(GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                        .setStopSequence(5)
                        .setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder()
                                .setDelay(120).build())
                        .build())
                .addStopTimeUpdate(GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                        .setStopSequence(6)
                        .setArrival(GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder()
                                .setDelay(-30).build())
                        .build())
                .build();

        Trip trip = mapper.gtfsRTToTrip(vehicle, tripUpdate);

        assertThat(trip.getStopUpdates()).hasSize(2);
        assertThat(trip.getStopUpdates().get(0).getStopSequence()).isEqualTo(5);
        assertThat(trip.getStopUpdates().get(0).getDelay()).isEqualTo(120);
        assertThat(trip.getStopUpdates().get(1).getStopSequence()).isEqualTo(6);
        assertThat(trip.getStopUpdates().get(1).getDelay()).isEqualTo(-30);
    }

    @Test
    void stopUpdateWithoutArrivalEventHasZeroDelay() {
        GtfsRealtime.VehiclePosition vehicle = GtfsRealtime.VehiclePosition.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("trip-1").build())
                .build();

        GtfsRealtime.TripUpdate tripUpdate = GtfsRealtime.TripUpdate.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setTripId("trip-1").build())
                .addStopTimeUpdate(GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                        .setStopSequence(3)
                        // no arrival set deliberately — hasArrival() == false
                        .build())
                .build();

        Trip trip = mapper.gtfsRTToTrip(vehicle, tripUpdate);

        assertThat(trip.getStopUpdates()).hasSize(1);
        assertThat(trip.getStopUpdates().get(0).getDelay()).isEqualTo(0);
    }

    @Test
    void tripUpdateFallsBackToTripIdWhenVehicleHasNone() {
        GtfsRealtime.VehiclePosition vehicleNoTrip = GtfsRealtime.VehiclePosition.newBuilder()
                .build();

        GtfsRealtime.TripUpdate tripUpdate = GtfsRealtime.TripUpdate.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                        .setTripId("fallback-trip")
                        .setRouteId("9")
                        .build())
                .build();

        Trip trip = mapper.gtfsRTToTrip(vehicleNoTrip, tripUpdate);

        assertThat(trip.getTripId()).isEqualTo("fallback-trip");
        assertThat(trip.getRouteId()).isEqualTo("9");
    }
}
