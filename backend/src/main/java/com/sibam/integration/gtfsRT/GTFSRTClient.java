package com.sibam.integration.gtfsRT;

import com.google.transit.realtime.GtfsRealtime;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
