package com.sibam.graph.routing;

import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.RouteAlternativeLabel;
import com.sibam.graph.model.output.Journey;
import com.sibam.graph.model.output.Leg;
import com.sibam.graph.model.output.RouteAlternative;
import com.sibam.graph.model.output.RouteAlternativesResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Servis za izdelavo in filtriranje alternativnih poti.
 *
 * A* požene z različnimi profili uteži, penalizira že uporabljena vozlišča,
 * odstrani preveč podobne ali prepočasne poti in rezultat pretvori v javni
 * RouteAlternative odgovor.
 */
@Service
public class RouteAlternativeService {

    private static final int MAX_ALLOWED_ROUTES = 3;

    private final AStarRouter aStarRouter;
    private final int maxRoutes;
    private final double maxSimilarity;
    private final double maxSlowdownMultiplier;
    private final int nodePenaltySeconds;

    public RouteAlternativeService(
            AStarRouter aStarRouter,
            @Value("${compute.path.max-alternatives:${routing.alternatives.max-routes:3}}") int maxRoutes,
            @Value("${routing.alternatives.max-similarity:0.8}") double maxSimilarity,
            @Value("${routing.alternatives.max-slowdown-multiplier:1.4}") double maxSlowdownMultiplier,
            @Value("${routing.alternatives.node-penalty-seconds:180}") int nodePenaltySeconds
    ) {
        this.aStarRouter = aStarRouter;
        this.maxRoutes = Math.max(1, Math.min(MAX_ALLOWED_ROUTES, maxRoutes));
        this.maxSimilarity = maxSimilarity;
        this.maxSlowdownMultiplier = maxSlowdownMultiplier;
        this.nodePenaltySeconds = nodePenaltySeconds;
    }

    /**
     * Poišče do konfiguriranega števila alternativ med izvorom in ciljem.
     *
     * @param allowBike ali so dovoljene MBajk BIKE etape
     * @param allowBus ali so dovoljene Marprom BUS etape
     * @param timeMode DEPART_AT ali ARRIVE_BY način routinga
     * @return odgovor z najdenimi alternativami ali statusom not_found
     */
    public RouteAlternativesResponse findAlternatives(
            double originLat,
            double originLon,
            double destinationLat,
            double destinationLon,
            String originAddress,
            String destinationAddress,
            LocalTime startTime,
            LocalDate startDate,
            boolean allowBike,
            boolean allowBus,
            RoutingTimeMode timeMode
    ) {
        List<SearchProfile> profiles = List.of(
                new SearchProfile(RouteAlternativeLabel.FASTEST, 1.0, 1.0),
                new SearchProfile(RouteAlternativeLabel.BIKE_FRIENDLY, 0.85, 1.10),
                new SearchProfile(RouteAlternativeLabel.TRANSIT_FRIENDLY, 1.20, 0.90),
                new SearchProfile(RouteAlternativeLabel.ALTERNATIVE, 1.05, 1.05)
        );
        List<Candidate> accepted = new ArrayList<>();
        Set<Integer> penalizedNodes = new LinkedHashSet<>();

        for (SearchProfile profile : profiles) {
            if (accepted.size() >= maxRoutes) {
                break;
            }

            AStarRouter.SearchOptions options = new AStarRouter.SearchOptions(
                    profile.bikeMultiplier(),
                    profile.busMultiplier(),
                    Set.copyOf(penalizedNodes),
                    accepted.isEmpty() ? 0 : nodePenaltySeconds
            );
            AStarRouter.RouteCandidate candidate = aStarRouter.findJourneyCandidate(
                    originLat,
                    originLon,
                    destinationLat,
                    destinationLon,
                    originAddress,
                    destinationAddress,
                    startTime,
                    startDate,
                    allowBike,
                    allowBus,
                    timeMode,
                    options
            );

            if (candidate == null || candidate.journey() == null) {
                continue;
            }

            if (violatesModes(candidate.journey(), allowBike, allowBus)) {
                continue;
            }

            Candidate routeCandidate = new Candidate(profile.label(), candidate);
            if (isTooSimilar(routeCandidate, accepted)) {
                continue;
            }

            accepted.add(routeCandidate);
            penalizedNodes.addAll(candidate.pathResult().getNodeIds());
        }

        List<Candidate> sorted = accepted.stream()
                .sorted(Comparator.comparingLong(Candidate::durationSeconds))
                .toList();
        List<Candidate> qualityFiltered = filterQuality(sorted);
        List<RouteAlternative> alternatives = new ArrayList<>();
        for (int i = 0; i < qualityFiltered.size(); i++) {
            alternatives.add(toAlternative(i + 1, qualityFiltered.get(i)));
        }

        return new RouteAlternativesResponse(
                alternatives.isEmpty() ? "not_found" : "success",
                new GeoPoint(originLat, originLon),
                originAddress,
                new GeoPoint(destinationLat, destinationLon),
                destinationAddress,
                alternatives
        );
    }

    /**
     * Odstrani alternative, ki so bistveno počasnejše od najhitrejše.
     *
     * @param candidates kandidati, sortirani po trajanju
     * @return filtriran seznam, vedno vsaj z najhitrejšim kandidatom
     */
    private List<Candidate> filterQuality(List<Candidate> candidates) {
        if (candidates.size() <= 1) {
            return candidates;
        }

        long fastest = candidates.getFirst().durationSeconds();
        List<Candidate> filtered = candidates.stream()
                .filter(candidate -> candidate.durationSeconds() <= Math.round(fastest * maxSlowdownMultiplier))
                .toList();
        return filtered.isEmpty() ? List.of(candidates.getFirst()) : filtered;
    }

    /**
     * Preveri, ali pot uporablja način, ki ga je zahtevek izključil.
     *
     * @return true, če pot vsebuje nedovoljen BIKE ali BUS leg
     */
    private boolean violatesModes(Journey journey, boolean allowBike, boolean allowBus) {
        for (Leg leg : journey.legs()) {
            if (!allowBike && EdgeType.BIKE.name().equals(leg.mode())) {
                return true;
            }
            if (!allowBus && EdgeType.BUS.name().equals(leg.mode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Preveri podobnost kandidata z že sprejetimi alternativami po skupnih vozliščih.
     *
     * @param candidate nov kandidat
     * @param accepted že sprejete alternative
     * @return true, če kandidat preseže konfiguriran prag podobnosti
     */
    private boolean isTooSimilar(Candidate candidate, List<Candidate> accepted) {
        Set<Integer> nodes = new LinkedHashSet<>(candidate.routeCandidate().pathResult().getNodeIds());
        if (nodes.isEmpty()) {
            return false;
        }

        for (Candidate existing : accepted) {
            Set<Integer> existingNodes = new LinkedHashSet<>(existing.routeCandidate().pathResult().getNodeIds());
            int shared = 0;
            for (Integer node : nodes) {
                if (existingNodes.contains(node)) {
                    shared++;
                }
            }
            double similarity = (double) shared / Math.min(nodes.size(), existingNodes.size());
            if (similarity > maxSimilarity) {
                return true;
            }
        }

        return false;
    }

    /**
     * Pretvori interni kandidat v javni RouteAlternative model.
     *
     * @param rank vrstni red alternative po kakovosti
     * @param candidate interni kandidat poti
     * @return javni model alternative
     */
    private RouteAlternative toAlternative(int rank, Candidate candidate) {
        Journey journey = candidate.routeCandidate().journey();
        List<String> modes = journey.legs().stream().map(Leg::mode).toList();
        List<String> labels = labelsFor(rank, candidate, modes);
        return new RouteAlternative(
                rank,
                labels.getFirst(),
                labels,
                candidate.durationSeconds(),
                parseInt(journey.distance()),
                modes,
                journey.legs()
        );
    }

    /**
     * Določi oznake alternative glede na rang, profil iskanja in uporabljene načine.
     *
     * @return seznam oznak za frontend
     */
    private List<String> labelsFor(int rank, Candidate candidate, List<String> modes) {
        LinkedHashSet<RouteAlternativeLabel> labels = new LinkedHashSet<>();
        if (rank == 1) {
            labels.add(RouteAlternativeLabel.FASTEST);
        }
        labels.add(candidate.label());
        if (modes.contains(EdgeType.BIKE.name())) {
            labels.add(RouteAlternativeLabel.BIKE_FRIENDLY);
        }
        if (modes.contains(EdgeType.BUS.name())) {
            labels.add(RouteAlternativeLabel.TRANSIT_FRIENDLY);
        }
        if (labels.isEmpty()) {
            labels.add(RouteAlternativeLabel.ALTERNATIVE);
        }
        return labels.stream()
                .map(RouteAlternativeLabel::displayName)
                .toList();
    }

    private int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        try {
            return (int) Math.round(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record SearchProfile(RouteAlternativeLabel label, double bikeMultiplier, double busMultiplier) {
    }

    private record Candidate(RouteAlternativeLabel label, AStarRouter.RouteCandidate routeCandidate) {
        long durationSeconds() {
            String duration = routeCandidate.journey().duration();
            if (duration == null || duration.isBlank()) {
                return 0;
            }
            try {
                return Math.round(Double.parseDouble(duration) / 1000.0);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
