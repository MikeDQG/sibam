package com.sibam.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.output.NavigationStep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleRoutesService {

    private static final Logger log = LoggerFactory.getLogger(GoogleRoutesService.class);
    private static final String ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${routes.google.api-key:}")
    private String apiKey;

    @Value("${routes.google.field-mask:routes.legs.steps,routes.duration,routes.distanceMeters,routes.polyline.geoJsonLinestring}")
    private String fieldMask;

    @Value("${routes.google.polyline-encoding:GEO_JSON_LINESTRING}")
    private String polylineEncoding; // ENCODED_POLYLINE or GEO_JSON_LINESTRING

    private WebClient webClient = WebClient.builder().baseUrl(ENDPOINT).build();

    public List<GeoPoint> fetchPolyline(GeoPoint origin, GeoPoint destination) {
        return fetchPolyline(origin, destination, EdgeType.WALK);
    }

    public List<GeoPoint> fetchPolyline(GeoPoint origin, GeoPoint destination, EdgeType edgeType) {
        RouteDetails details = fetchRouteDetails(origin, destination, edgeType);
        if (details == null || details.polyline().isEmpty()) {
            return List.of(origin, destination);
        }

        return details.polyline();
    }

    public RouteDetails fetchRouteDetails(GeoPoint origin, GeoPoint destination, EdgeType edgeType) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Google Routes API key missing; skipping route details fetch.");
            return null;
        }

        try {
            String body = buildRequestBody(origin, destination, edgeType);

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
                return null;
            }

            return parseRouteDetails(response, origin, destination);
        } catch (Exception e) {
            log.warn("Failed to fetch route details from Google Routes: {}", e.toString());
            return null;
        }
    }

    private String buildRequestBody(GeoPoint origin, GeoPoint destination, EdgeType edgeType) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"travelMode\":\"").append(travelMode(edgeType)).append("\",");
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

    private String travelMode(EdgeType edgeType) {
        if (edgeType == EdgeType.BIKE) {
            return "WALK";
        }

        return "WALK";
    }

    RouteDetails parseRouteDetails(String response, GeoPoint fallbackStart, GeoPoint fallbackEnd) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode routes = root.path("routes");
        if (!routes.isArray() || routes.isEmpty()) {
            return new RouteDetails(List.of(fallbackStart, fallbackEnd), List.of());
        }
        JsonNode first = routes.get(0);
        List<GeoPoint> routePolyline = parsePolyline(first.path("polyline"), fallbackStart, fallbackEnd);
        List<NavigationStep> steps = parseSteps(first.path("legs"));

        return new RouteDetails(routePolyline, steps);
    }

    private List<GeoPoint> parsePolyline(JsonNode polyline, GeoPoint fallbackStart, GeoPoint fallbackEnd) {
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

    private List<NavigationStep> parseSteps(JsonNode legs) {
        if (!legs.isArray() || legs.isEmpty()) {
            return List.of();
        }

        List<NavigationStep> steps = new ArrayList<>();
        for (JsonNode leg : legs) {
            JsonNode routeSteps = leg.path("steps");
            if (!routeSteps.isArray()) {
                continue;
            }

            for (JsonNode step : routeSteps) {
                JsonNode instruction = step.path("navigationInstruction");
                steps.add(new NavigationStep(
                        instruction.path("instructions").asText(null),
                        instruction.path("maneuver").asText(null),
                        step.path("distanceMeters").asInt(0),
                        durationSeconds(step),
                        step.has("polyline") ? step.path("polyline").deepCopy() : null
                ));
            }
        }

        return List.copyOf(steps);
    }

    private long durationSeconds(JsonNode step) {
        String duration = step.path("duration").asText(null);
        if (duration == null || duration.isBlank()) {
            duration = step.path("staticDuration").asText(null);
        }

        if (duration == null || duration.isBlank()) {
            return 0;
        }

        try {
            String seconds = duration.endsWith("s")
                    ? duration.substring(0, duration.length() - 1)
                    : duration;
            return Math.max(0, Math.round(Double.parseDouble(seconds)));
        } catch (NumberFormatException ignored) {
            return 0;
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

    public record RouteDetails(
            List<GeoPoint> polyline,
            List<NavigationStep> steps
    ) {
        public RouteDetails {
            polyline = polyline == null ? List.of() : List.copyOf(polyline);
            steps = steps == null ? List.of() : List.copyOf(steps);
        }
    }
}
