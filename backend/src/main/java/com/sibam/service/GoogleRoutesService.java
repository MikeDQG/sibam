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

/**
 * Servis za pridobivanje polilinij in navigacijskih korakov iz Google Routes
 * Compute Routes API.
 *
 * WALK in BIKE etape pošilja kot peš navigacijo, pri zaporednih etapah pa uporabi
 * en skupen zahtevek z vmesnimi točkami in rezultat razdeli nazaj na posamezne
 * etape.
 */
@Service
public class GoogleRoutesService {

    private static final Logger log = LoggerFactory.getLogger(GoogleRoutesService.class);
    private static final String ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes";
    private static final double COORDINATE_MATCH_TOLERANCE = 0.00001;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${routes.google.api-key:}")
    private String apiKey;

    @Value("${routes.google.field-mask:routes.legs.steps,routes.duration,routes.distanceMeters,routes.polyline.geoJsonLinestring}")
    private String fieldMask;

    @Value("${routes.google.polyline-encoding:GEO_JSON_LINESTRING}")
    private String polylineEncoding; // ENCODED_POLYLINE or GEO_JSON_LINESTRING

    private WebClient webClient = WebClient.builder().baseUrl(ENDPOINT).build();

    /**
     * Pridobi polilinijo za privzeto WALK etapo.
     *
     * @param origin začetna točka etape
     * @param destination končna točka etape
     * @return polilinija iz Google Routes ali ravna fallback povezava
     */
    public List<GeoPoint> fetchPolyline(GeoPoint origin, GeoPoint destination) {
        return fetchPolyline(origin, destination, EdgeType.WALK);
    }

    /**
     * Pridobi polilinijo za WALK ali BIKE etapo.
     *
     * @param origin začetna točka etape
     * @param destination končna točka etape
     * @param edgeType tip grafa, za katerega se pridobiva navigacija
     * @return polilinija iz Google Routes ali ravna fallback povezava
     */
    public List<GeoPoint> fetchPolyline(GeoPoint origin, GeoPoint destination, EdgeType edgeType) {
        RouteDetails details = fetchRouteDetails(origin, destination, edgeType);
        if (details == null || details.polyline().isEmpty()) {
            return List.of(origin, destination);
        }

        return details.polyline();
    }

    /**
     * Izvede en Compute Routes zahtevek za eno WALK/BIKE etapo.
     *
     * @param origin začetna točka etape
     * @param destination končna točka etape
     * @param edgeType tip etape; BIKE se pri Google Routes obravnava kot WALK
     * @return polilinija in navigacijski koraki ali fallback podatki ob napaki
     */
    public RouteDetails fetchRouteDetails(GeoPoint origin, GeoPoint destination, EdgeType edgeType) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Google Routes API key missing; using fallback route details.");
            return fallbackRouteDetails(origin, destination);
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
                log.warn("Google Routes API returned an empty response; using fallback route details.");
                return fallbackRouteDetails(origin, destination);
            }

            return parseRouteDetails(response, origin, destination);
        } catch (Exception e) {
            log.warn("Failed to fetch route details from Google Routes; using fallback route details: {}", e.toString());
            return fallbackRouteDetails(origin, destination);
        }
    }

    /**
     * Izvede sklopljen Compute Routes zahtevek za zaporedje WALK/BIKE etap.
     *
     * @param points zaporedje začetne, vmesnih in končne točke
     * @param edgeType tip etap, ki se pošlje v Google Routes
     * @return seznam rezultatov, razdeljen po segmentih med sosednjimi točkami
     */
    public List<RouteDetails> fetchRouteDetails(List<GeoPoint> points, EdgeType edgeType) {
        if (points == null || points.size() < 2) {
            return List.of();
        }

        if (points.size() == 2) {
            return List.of(fetchRouteDetails(points.get(0), points.get(1), edgeType));
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Google Routes API key missing; using fallback details for {} coupled route legs.", points.size() - 1);
            return fallbackRouteDetails(points);
        }

        try {
            String body = buildRequestBody(points, edgeType);

            String response = webClient.post()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", fieldMask)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(ex -> {
                        log.warn("Google Routes API coupled call failed: {}", ex.toString());
                        return Mono.empty();
                    })
                    .block();

            if (response == null || response.isBlank()) {
                log.warn("Google Routes API returned an empty coupled response; using fallback route details.");
                return fallbackRouteDetails(points);
            }

            return parseRouteDetails(response, points);
        } catch (Exception e) {
            log.warn("Failed to fetch coupled route details from Google Routes; using fallback route details: {}", e.toString());
            return fallbackRouteDetails(points);
        }
    }

    private String buildRequestBody(GeoPoint origin, GeoPoint destination, EdgeType edgeType) {
        return buildRequestBody(List.of(origin, destination), edgeType);
    }

    /**
     * Sestavi JSON telo za Google Routes z izvorom, ciljem in vmesnimi točkami.
     *
     * @param points zaporedje točk poti
     * @param edgeType tip etape za izbiro travelMode
     * @return JSON telo zahtevka
     */
    private String buildRequestBody(List<GeoPoint> points, EdgeType edgeType) {
        GeoPoint origin = points.getFirst();
        GeoPoint destination = points.getLast();
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
        if (points.size() > 2) {
            sb.append(",\"intermediates\":[");
            for (int i = 1; i < points.size() - 1; i++) {
                if (i > 1) {
                    sb.append(",");
                }
                GeoPoint waypoint = points.get(i);
                sb.append("{\"location\":{\"latLng\":{")
                        .append("\"latitude\":").append(waypoint.lat()).append(",")
                        .append("\"longitude\":").append(waypoint.lon())
                        .append("}}}");
            }
            sb.append("]");
        }
        if ("GEO_JSON_LINESTRING".equalsIgnoreCase(polylineEncoding)) {
            sb.append(",\"polylineEncoding\":\"GEO_JSON_LINESTRING\"");
        } else {
            sb.append(",\"polylineEncoding\":\"ENCODED_POLYLINE\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Preslika tip grafa v travelMode Google Routes API-ja.
     *
     * @param edgeType tip roba v internem grafu
     * @return travelMode, trenutno WALK tudi za BIKE
     */
    private String travelMode(EdgeType edgeType) {
        if (edgeType == EdgeType.BIKE) {
            return "WALK";
        }

        return "WALK";
    }

    /**
     * Parsira odgovor Google Routes za enosegmentni zahtevek.
     *
     * @param response JSON odgovor Compute Routes API-ja
     * @param fallbackStart začetna točka za fallback
     * @param fallbackEnd končna točka za fallback
     * @return polilinija in koraki za etapo
     */
    RouteDetails parseRouteDetails(String response, GeoPoint fallbackStart, GeoPoint fallbackEnd) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode routes = root.path("routes");
        if (!routes.isArray() || routes.isEmpty()) {
            log.warn("Google Routes response contained no routes; using fallback route details.");
            return fallbackRouteDetails(fallbackStart, fallbackEnd);
        }
        JsonNode first = routes.get(0);
        List<GeoPoint> routePolyline = parsePolyline(first.path("polyline"), fallbackStart, fallbackEnd);
        List<NavigationStep> steps = parseSteps(first.path("legs"), routePolyline);

        return new RouteDetails(routePolyline, steps);
    }

    /**
     * Parsira odgovor Google Routes za sklopljen zahtevek z več etapami.
     *
     * @param response JSON odgovor Compute Routes API-ja
     * @param fallbackPoints točke, med katerimi se rezultat razdeli
     * @return seznam rezultatov po posameznih etapah
     */
    List<RouteDetails> parseRouteDetails(String response, List<GeoPoint> fallbackPoints) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode routes = root.path("routes");
        if (!routes.isArray() || routes.isEmpty()) {
            log.warn("Google Routes coupled response contained no routes; using fallback route details.");
            return fallbackRouteDetails(fallbackPoints);
        }

        JsonNode legs = routes.get(0).path("legs");
        if (!legs.isArray() || legs.isEmpty()) {
            return fallbackRouteDetails(fallbackPoints);
        }

        List<RouteDetails> details = new ArrayList<>();
        int segmentCount = fallbackPoints.size() - 1;
        for (int i = 0; i < segmentCount; i++) {
            JsonNode leg = i < legs.size() ? legs.get(i) : null;
            GeoPoint fallbackStart = fallbackPoints.get(i);
            GeoPoint fallbackEnd = fallbackPoints.get(i + 1);
            details.add(parseLegRouteDetails(leg, fallbackStart, fallbackEnd));
        }

        return List.copyOf(details);
    }

    private RouteDetails fallbackRouteDetails(GeoPoint origin, GeoPoint destination) {
        return new RouteDetails(List.of(origin, destination), null);
    }

    private List<RouteDetails> fallbackRouteDetails(List<GeoPoint> points) {
        List<RouteDetails> details = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            details.add(fallbackRouteDetails(points.get(i), points.get(i + 1)));
        }
        return List.copyOf(details);
    }

    /**
     * Pretvori en Google leg v interno polilinijo in seznam navigacijskih korakov.
     *
     * @param leg Google Routes leg za en segment
     * @param fallbackStart začetna točka za fallback
     * @param fallbackEnd končna točka za fallback
     * @return navigacijski podatki za segment
     */
    private RouteDetails parseLegRouteDetails(JsonNode leg, GeoPoint fallbackStart, GeoPoint fallbackEnd) {
        if (leg == null || leg.isMissingNode() || leg.isNull()) {
            return fallbackRouteDetails(fallbackStart, fallbackEnd);
        }

        JsonNode routeSteps = leg.path("steps");
        if (!routeSteps.isArray() || routeSteps.isEmpty()) {
            return new RouteDetails(List.of(fallbackStart, fallbackEnd), List.of());
        }

        List<GeoPoint> legPolyline = new ArrayList<>();
        List<StepDraft> drafts = new ArrayList<>();
        for (JsonNode step : routeSteps) {
            List<GeoPoint> stepPolyline = parseOptionalPolyline(step.path("polyline"));
            if (stepPolyline.isEmpty()) {
                stepPolyline = List.of(
                        legPolyline.isEmpty() ? fallbackStart : legPolyline.getLast(),
                        fallbackEnd
                );
            }

            int startIndex = legPolyline.isEmpty() ? 0 : legPolyline.size() - 1;
            appendPolyline(legPolyline, stepPolyline);
            int endIndex = Math.max(startIndex, legPolyline.size() - 1);
            JsonNode instruction = step.path("navigationInstruction");
            drafts.add(new StepDraft(
                    instruction.path("instructions").asText(null),
                    instruction.path("maneuver").asText(null),
                    step.path("distanceMeters").asInt(0),
                    durationSeconds(step),
                    startIndex,
                    endIndex
            ));
        }

        if (legPolyline.isEmpty()) {
            legPolyline.add(fallbackStart);
            legPolyline.add(fallbackEnd);
        }

        List<NavigationStep> steps = drafts.stream()
                .map(draft -> new NavigationStep(
                        draft.instruction(),
                        draft.maneuver(),
                        draft.distanceMeters(),
                        draft.durationSeconds(),
                        draft.startPolylineIndex(),
                        draft.endPolylineIndex()
                ))
                .toList();
        return new RouteDetails(legPolyline, steps);
    }

    /**
     * Parsira polilinijo iz Google Routes odgovora.
     *
     * @param polyline JSON vozlišče s polilinijo
     * @param fallbackStart začetna točka za fallback
     * @param fallbackEnd končna točka za fallback
     * @return seznam koordinat v internem GeoPoint formatu
     */
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

    /**
     * Parsira Google navigacijske korake in jih poveže z indeksi glavne polilinije.
     *
     * @param legs Google Routes legs iz odgovora
     * @param routePolyline glavna polilinija poti
     * @return navigacijski koraki za frontend
     */
    private List<NavigationStep> parseSteps(JsonNode legs, List<GeoPoint> routePolyline) {
        if (!legs.isArray() || legs.isEmpty()) {
            return List.of();
        }

        List<NavigationStep> steps = new ArrayList<>();
        int previousEndIndex = 0;
        for (JsonNode leg : legs) {
            JsonNode routeSteps = leg.path("steps");
            if (!routeSteps.isArray()) {
                continue;
            }

            for (JsonNode step : routeSteps) {
                JsonNode instruction = step.path("navigationInstruction");
                List<GeoPoint> stepPolyline = parseOptionalPolyline(step.path("polyline"));
                PolylineRange range = matchStepPolylineRange(routePolyline, stepPolyline, previousEndIndex);
                previousEndIndex = range.endPolylineIndex();
                steps.add(new NavigationStep(
                        instruction.path("instructions").asText(null),
                        instruction.path("maneuver").asText(null),
                        step.path("distanceMeters").asInt(0),
                        durationSeconds(step),
                        range.startPolylineIndex(),
                        range.endPolylineIndex()
                ));
            }
        }

        return List.copyOf(steps);
    }

    /**
     * Poišče območje glavne polilinije, ki pripada posameznemu Google koraku.
     *
     * @param routePolyline glavna polilinija poti
     * @param stepPolyline polilinija koraka
     * @param previousEndIndex indeks konca prejšnjega koraka
     * @return začetni in končni indeks koraka
     */
    private PolylineRange matchStepPolylineRange(
            List<GeoPoint> routePolyline,
            List<GeoPoint> stepPolyline,
            int previousEndIndex
    ) {
        if (routePolyline == null || routePolyline.isEmpty()) {
            log.warn("Cannot match Google navigation step polyline because route polyline is empty.");
            return new PolylineRange(0, 0);
        }

        int fallbackStart = Math.min(Math.max(previousEndIndex, 0), routePolyline.size() - 1);
        int fallbackEnd = Math.min(fallbackStart + 1, routePolyline.size() - 1);
        if (stepPolyline == null || stepPolyline.isEmpty()) {
            log.warn("Google navigation step has no polyline; using fallback range {}-{}.", fallbackStart, fallbackEnd);
            return new PolylineRange(fallbackStart, fallbackEnd);
        }

        GeoPoint stepStart = stepPolyline.getFirst();
        GeoPoint stepEnd = stepPolyline.getLast();
        int startIndex = findMatchingPointIndex(routePolyline, stepStart, fallbackStart);
        int endIndex = findMatchingPointIndex(routePolyline, stepEnd, Math.max(startIndex, fallbackEnd));

        if (endIndex < startIndex) {
            int closestEnd = findClosestPointIndex(routePolyline, stepEnd, startIndex);
            log.warn(
                    "Google navigation step polyline matched out of order ({}-{}); using closest forward end index {}.",
                    startIndex,
                    endIndex,
                    closestEnd
            );
            endIndex = closestEnd;
        }

        return new PolylineRange(startIndex, endIndex);
    }

    private int findMatchingPointIndex(List<GeoPoint> routePolyline, GeoPoint point, int fallbackIndex) {
        for (int i = 0; i < routePolyline.size(); i++) {
            if (matchesWithinTolerance(routePolyline.get(i), point)) {
                return i;
            }
        }

        int closestIndex = findClosestPointIndex(routePolyline, point, 0);
        log.warn(
                "Google navigation step polyline point could not be matched within tolerance; using closest route polyline index {}.",
                closestIndex
        );
        return closestIndex >= 0 ? closestIndex : fallbackIndex;
    }

    private int findClosestPointIndex(List<GeoPoint> routePolyline, GeoPoint point, int minIndex) {
        if (routePolyline == null || routePolyline.isEmpty()) {
            return -1;
        }

        int start = Math.min(Math.max(minIndex, 0), routePolyline.size() - 1);
        int closestIndex = start;
        double closestDistance = Double.MAX_VALUE;
        for (int i = start; i < routePolyline.size(); i++) {
            GeoPoint candidate = routePolyline.get(i);
            double distance = Math.pow(candidate.lat() - point.lat(), 2)
                    + Math.pow(candidate.lon() - point.lon(), 2);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        return closestIndex;
    }

    private boolean matchesWithinTolerance(GeoPoint a, GeoPoint b) {
        return Math.abs(a.lat() - b.lat()) < COORDINATE_MATCH_TOLERANCE
                && Math.abs(a.lon() - b.lon()) < COORDINATE_MATCH_TOLERANCE;
    }

    private List<GeoPoint> parseOptionalPolyline(JsonNode polyline) {
        if (polyline == null || polyline.isMissingNode() || polyline.isNull()) {
            return List.of();
        }

        if ("GEO_JSON_LINESTRING".equalsIgnoreCase(polylineEncoding)) {
            JsonNode coords = polyline.path("geoJsonLinestring").path("coordinates");
            if (!coords.isArray() || coords.isEmpty()) {
                return List.of();
            }
            List<GeoPoint> points = new ArrayList<>();
            for (JsonNode c : coords) {
                if (c.isArray() && c.size() >= 2) {
                    points.add(new GeoPoint(c.get(1).asDouble(), c.get(0).asDouble()));
                }
            }
            return List.copyOf(points);
        }

        String encoded = polyline.path("encodedPolyline").asText(null);
        if (encoded == null || encoded.isBlank()) {
            return List.of();
        }
        return decodeEncodedPolyline(encoded);
    }

    private void appendPolyline(List<GeoPoint> target, List<GeoPoint> source) {
        for (GeoPoint point : source) {
            if (!target.isEmpty() && target.getLast().equals(point)) {
                continue;
            }

            target.add(point);
        }
    }

    private int durationSeconds(JsonNode step) {
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
            return (int) Math.max(0, Math.round(Double.parseDouble(seconds)));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /**
     * Dekodira Google encoded polyline zapis v seznam koordinat.
     *
     * @param encoded encoded polyline niz
     * @return seznam točk ali prazen seznam, če ni podatkov
     */
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

    /**
     * Rezultat Google navigacije za eno etapo.
     *
     * @param polyline polilinija etape
     * @param steps navigacijski koraki ali null, če niso bili zahtevani oziroma na voljo
     */
    public record RouteDetails(
            List<GeoPoint> polyline,
            List<NavigationStep> steps
    ) {
        public RouteDetails {
            polyline = polyline == null ? List.of() : List.copyOf(polyline);
            steps = steps == null ? null : List.copyOf(steps);
        }
    }

    private record PolylineRange(int startPolylineIndex, int endPolylineIndex) {
    }

    private record StepDraft(
            String instruction,
            String maneuver,
            int distanceMeters,
            int durationSeconds,
            int startPolylineIndex,
            int endPolylineIndex
    ) {
    }
}
