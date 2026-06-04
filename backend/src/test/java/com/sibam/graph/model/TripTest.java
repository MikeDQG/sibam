package com.sibam.graph.model;

import com.sibam.graph.model.trip.StopUpdate;
import com.sibam.graph.model.trip.Trip;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TripTest {

    @Test
    void fullConstructorSetsAllFields() {
        StopUpdate stopUpdate = new StopUpdate();
        stopUpdate.setStopSequence(3);
        stopUpdate.setDelay(60);

        Trip trip = new Trip(
                "trip-1", "route-6", "BUS-006", "v-1",
                46.55, 15.64, 180.0f, 3, "stop-42", 1700000000L,
                List.of(stopUpdate)
        );

        assertThat(trip.getTripId()).isEqualTo("trip-1");
        assertThat(trip.getRouteId()).isEqualTo("route-6");
        assertThat(trip.getVehicleLabel()).isEqualTo("BUS-006");
        assertThat(trip.getVehicleId()).isEqualTo("v-1");
        assertThat(trip.getLat()).isEqualTo(46.55);
        assertThat(trip.getLon()).isEqualTo(15.64);
        assertThat(trip.getBearing()).isEqualTo(180.0f);
        assertThat(trip.getCurrent_stop_sequence()).isEqualTo(3);
        assertThat(trip.getStop_id()).isEqualTo("stop-42");
        assertThat(trip.getTimestamp()).isEqualTo(1700000000L);
        assertThat(trip.getStopUpdates()).hasSize(1);
        assertThat(trip.getStopUpdates().get(0).getDelay()).isEqualTo(60);
    }

    @Test
    void defaultConstructorProducesNullFields() {
        Trip trip = new Trip();
        assertThat(trip.getTripId()).isNull();
        assertThat(trip.getRouteId()).isNull();
        assertThat(trip.getBearing()).isNull();
        assertThat(trip.getStopUpdates()).isNull();
    }

    @Test
    void settersUpdateAllFields() {
        Trip trip = new Trip();
        trip.setTripId("t1");
        trip.setRouteId("r1");
        trip.setVehicleId("v1");
        trip.setVehicleLabel("LABEL");
        trip.setLat(46.0);
        trip.setLon(15.0);
        trip.setBearing(90.0f);
        trip.setCurrent_stop_sequence(5);
        trip.setStop_id("s5");
        trip.setTimestamp(999L);
        trip.setStopUpdates(List.of());

        assertThat(trip.getTripId()).isEqualTo("t1");
        assertThat(trip.getRouteId()).isEqualTo("r1");
        assertThat(trip.getVehicleId()).isEqualTo("v1");
        assertThat(trip.getVehicleLabel()).isEqualTo("LABEL");
        assertThat(trip.getLat()).isEqualTo(46.0);
        assertThat(trip.getLon()).isEqualTo(15.0);
        assertThat(trip.getBearing()).isEqualTo(90.0f);
        assertThat(trip.getCurrent_stop_sequence()).isEqualTo(5);
        assertThat(trip.getStop_id()).isEqualTo("s5");
        assertThat(trip.getTimestamp()).isEqualTo(999L);
        assertThat(trip.getStopUpdates()).isEmpty();
    }
}
