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

@Service
public class RouteAlternativeService {

    private final AStarRouter aStarRouter;
    private final int maxRoutes;
    private final double maxSimilarity;
    private final double maxSlowdownMultiplier;
    private final int nodePenaltySeconds;

    public RouteAlternativeService(
            AStarRouter aStarRouter,
            @Value("${routing.alternatives.max-routes:3}") int maxRoutes,
            @Value("${routing.alternatives.max-similarity:0.8}") double maxSimilarity,
            @Value("${routing.alternatives.max-slowdown-multiplier:1.4}") double maxSlowdownMultiplier,
            @Value("${routing.alternatives.node-penalty-seconds:180}") int nodePenaltySeconds
    ) {
        this.aStarRouter = aStarRouter;
        this.maxRoutes = maxRoutes;
        this.maxSimilarity = maxSimilarity;
        this.maxSlowdownMultiplier = maxSlowdownMultiplier;
        this.nodePenaltySeconds = nodePenaltySeconds;
    }

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
        GeoPoint origin = new GeoPoint(originLat, originLon);
        GeoPoint destination = new GeoPoint(destinationLat, destinationLon);
        for (int i = 0; i < qualityFiltered.size(); i++) {
            alternatives.add(toAlternative(
                    i + 1,
                    qualityFiltered.get(i),
                    origin,
                    originAddress,
                    destination,
                    destinationAddress
            ));
        }

        return new RouteAlternativesResponse(
                alternatives.isEmpty() ? "not_found" : "success",
                alternatives
        );
    }

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

    private RouteAlternative toAlternative(
            int rank,
            Candidate candidate,
            GeoPoint origin,
            String originAddress,
            GeoPoint destination,
            String destinationAddress
    ) {
        Journey journey = candidate.routeCandidate().journey();
        List<String> modes = journey.legs().stream().map(Leg::mode).toList();
        List<String> labels = labelsFor(rank, candidate, modes);
        return new RouteAlternative(
                rank,
                origin,
                originAddress,
                destination,
                destinationAddress,
                labels.getFirst(),
                labels,
                candidate.durationSeconds(),
                parseInt(journey.distance()),
                modes,
                journey.legs()
        );
    }

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
