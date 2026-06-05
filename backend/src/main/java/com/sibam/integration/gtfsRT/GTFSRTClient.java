package com.sibam.integration.gtfsRT;

import com.google.transit.realtime.GtfsRealtime;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Odjemalec za Marprom GTFS-RT protobuf vire.
 *
 * Sinhrono prenese vehicle_positions in trip_updates ter jih parsira v
 * FeedMessage objekte knjižnice GTFS Realtime.
 */
@Component
public class GTFSRTClient {

    private static final String VEHICLE_POSITIONS_URL =
            "https://rt.gtfs.derp.si/sources/marprom/vehicle_positions";

    private static final String TRIP_UPDATES_URL =
            "https://rt.gtfs.derp.si/sources/marprom/trip_updates";

    private final WebClient webClient;

    public GTFSRTClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Prenese trenutne položaje Marprom vozil iz GTFS-RT vira.
     *
     * @return parsiran FeedMessage z VehiclePosition entitetami
     */
    public GtfsRealtime.FeedMessage getVehiclePositions() {
        try {

            byte[] response = webClient
                    .get()
                    .uri(VEHICLE_POSITIONS_URL)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Vehicle positions response is null");
            }

            return GtfsRealtime.FeedMessage.parseFrom(response);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch vehicle positions", e);
        }
    }

    /**
     * Prenese trenutne posodobitve voženj in zamud iz GTFS-RT vira.
     *
     * @return parsiran FeedMessage s TripUpdate entitetami
     */
    public GtfsRealtime.FeedMessage getTripUpdates() {
        try {

            byte[] response = webClient
                    .get()
                    .uri(TRIP_UPDATES_URL)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Trip updates response is null");
            }

            return GtfsRealtime.FeedMessage.parseFrom(response);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch trip updates", e);
        }
    }
}
