package com.sibam.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibam.graph.model.GeoPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleRoutesService {

    private static final String ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${routes.google.api-key:}")
    private String apiKey;

    @Value("${routes.google.field-mask:routes.duration,routes.distanceMeters,routes.polyline.geoJsonLinestring}")
    private String fieldMask;

    @Value("${routes.google.polyline-encoding:GEO_JSON_LINESTRING}")
    private String polylineEncoding; // ENCODED_POLYLINE or GEO_JSON_LINESTRING

    private WebClient webClient = WebClient.builder().baseUrl(ENDPOINT).build();

    public List<GeoPoint> fetchPolyline(GeoPoint origin, GeoPoint destination) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Google Routes API key missing; skipping polyline fetch.");
            return List.of(origin, destination);
        }

        try {
            String body = buildRequestBody(origin, destination);

            String response = webClient.post()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", fieldMask)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(ex -> {
                        log.warn("Google Routes API call failed: {}", ex.toString());
                        return Mono.empty();
                    })
                    .block();

            if (response == null || response.isBlank()) {
                return List.of(origin, destination);
            }

            return parsePolyline(response, origin, destination);
        } catch (Exception e) {
            log.warn("Failed to fetch polyline from Google Routes: {}", e.toString());
            return List.of(origin, destination);
        }
    }

    private String buildRequestBody(GeoPoint origin, GeoPoint destination) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"origin\":{\"location\":{\"latLng\":{")
                .append("\"latitude\":").append(origin.lat()).append(",")
                .append("\"longitude\":").append(origin.lon())
                .append("}}},");
        sb.append("\"destination\":{\"location\":{\"latLng\":{")
                .append("\"latitude\":").append(destination.lat()).append(",")
                .append("\"longitude\":").append(destination.lon())
                .append("}}}");
        if ("GEO_JSON_LINESTRING".equalsIgnoreCase(polylineEncoding)) {
            sb.append(",\"polylineEncoding\":\"GEO_JSON_LINESTRING\"");
        } else {
            sb.append(",\"polylineEncoding\":\"ENCODED_POLYLINE\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private List<GeoPoint> parsePolyline(String response, GeoPoint fallbackStart, GeoPoint fallbackEnd) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode routes = root.path("routes");
        if (!routes.isArray() || routes.isEmpty()) {
            return List.of(fallbackStart, fallbackEnd);
        }
        JsonNode first = routes.get(0);
        JsonNode polyline = first.path("polyline");

        if ("GEO_JSON_LINESTRING".equalsIgnoreCase(polylineEncoding)) {
            JsonNode linestring = polyline.path("geoJsonLinestring");
            JsonNode coords = linestring.path("coordinates");
            if (!coords.isArray() || coords.isEmpty()) {
                return List.of(fallbackStart, fallbackEnd);
            }
            List<GeoPoint> points = new ArrayList<>();
            for (JsonNode c : coords) {
                if (c.isArray() && c.size() >= 2) {
                    double lon = c.get(0).asDouble();
                    double lat = c.get(1).asDouble();
                    points.add(new GeoPoint(lat, lon));
                }
            }
            if (points.isEmpty()) {
                return List.of(fallbackStart, fallbackEnd);
            }
            return points;
        } else {
            String encoded = polyline.path("encodedPolyline").asText(null);
            if (encoded == null || encoded.isBlank()) {
                return List.of(fallbackStart, fallbackEnd);
            }
            return decodeEncodedPolyline(encoded);
        }
    }

    // Decodes a Google Encoded Polyline string into a list of GeoPoints
    private List<GeoPoint> decodeEncodedPolyline(String encoded) {
        List<GeoPoint> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lon = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20 && index < len);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20 && index < len);
            int dlon = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lon += dlon;

            double latitude = lat / 1E5;
            double longitude = lon / 1E5;
            poly.add(new GeoPoint(latitude, longitude));
        }
        if (poly.isEmpty()) {
            return List.of();
        }
        return poly;
    }
}
