package com.sibam.persistence;

/**
 * Razred, namenjen shranjevanju podatkov v bazo za namene ML
 * Po potrebi dodaj anotacije
 */
// @Entity
// @Table(name = "")
public class TripEntity {
    String tripId;
    String routeId;
    String vehicleLabel;
    String vehicleId;
    double lat;
    double lon;
    Float bearing;
    int current_stop_sequence;
    String stop_id;
    Long timestamp;

    // če misliš, da nam koristi le zamuda na naslednji postaji, ohrani te "stolpce",
    // drugače pa ustvari List<StopUpdateEntity> podobno kot /model/trip/StopUpdate
    int next_stop_sequence;
    int delay;
    int uncertainty;
}
