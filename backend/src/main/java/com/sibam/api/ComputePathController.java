package com.sibam.api;

import com.sibam.engine.VaoSerializer;
import com.sibam.dto.ApiErrorResponse;
import com.sibam.graph.bootstrap.GraphBootstrap;
import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.output.RouteAlternativesResponse;
import com.sibam.graph.routing.RouteAlternativeService;
import com.sibam.graph.routing.RouteAccessDistanceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
@RestController
@RequestMapping("/compute")
public class ComputePathController {

    private static final Logger log = LoggerFactory.getLogger(ComputePathController.class);
    
    private final VaoSerializer vaoSerializer;
    private final GraphBootstrap graphBootstrap;
    private final RouteAlternativeService routeAlternativeService;

    public ComputePathController(
            VaoSerializer vaoSerializer,
            GraphBootstrap graphBootstrap,
            RouteAlternativeService routeAlternativeService
    ) {
        this.vaoSerializer = vaoSerializer;
        this.graphBootstrap = graphBootstrap;
        this.routeAlternativeService = routeAlternativeService;
    }

    @GetMapping
    public ResponseEntity<?> computePath(
            @RequestParam double originLat,
            @RequestParam double originLon,
            @RequestParam double destinationLat,
            @RequestParam double destinationLon,
            @RequestParam(required = false) String originAddress,
            @RequestParam(required = false) String destinationAddress,
            @RequestParam(name = "origin_address", required = false) String originAddressSnake,
            @RequestParam(name = "destination_address", required = false) String destinationAddressSnake,
            @RequestParam boolean leaveNow,
            @RequestParam(required = false) String leaveAt,
            @RequestParam(required = false) String arriveBy,
            @RequestParam boolean bike,
            @RequestParam boolean bus,
            @RequestParam(required = false) String userId
            ) {
        if (bike) {
            graphBootstrap.refresh();
        } else {
            graphBootstrap.ensureInitialized();
        }

        LocalTime startTime = resolveStartTime(leaveNow, leaveAt);
        String resolvedOriginAddress = resolveAddress(originAddress, originAddressSnake);
        String resolvedDestinationAddress = resolveAddress(destinationAddress, destinationAddressSnake);
        RouteAlternativesResponse response;
        try {
            response = routeAlternativeService.findAlternatives(
                    originLat,
                    originLon,
                    destinationLat,
                    destinationLon,
                    resolvedOriginAddress,
                    resolvedDestinationAddress,
                    startTime,
                    bike,
                    bus
            );
        } catch (RouteAccessDistanceException e) {
            log.info("Failed to compute path: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiErrorResponse(
                    "error",
                    "IZVEN_OBMOCJA_POTI", // "OUTSIDE_SERVICE_AREA",
                    e.getMessage(),
                    e.getEndpoint(),
                    e.getDistanceMeters(),
                    e.getMaxDistanceMeters()
            ));
        }

        log.info("Received path computation request with origin ({}, {})", originLat, originLon);

        if (response == null) {
            return ResponseEntity.ok(new RouteAlternativesResponse(
                    "not_found",
                    new GeoPoint(originLat, originLon),
                    resolvedOriginAddress,
                    new GeoPoint(destinationLat, destinationLon),
                    resolvedDestinationAddress,
                    java.util.List.of()
            ));
        }

        return ResponseEntity.ok(response);
    }


    private LocalTime resolveStartTime(boolean leaveNow, String leaveAt) {
        if (leaveNow || leaveAt == null || leaveAt.isBlank()) {
            return LocalTime.now();
        }

        return LocalTime.parse(leaveAt);
    }

    private String resolveAddress(String camelCaseAddress, String snakeCaseAddress) {
        if (camelCaseAddress != null && !camelCaseAddress.isBlank()) {
            return camelCaseAddress;
        }

        return snakeCaseAddress;
    }
}
