package com.sibam.api;

import com.sibam.engine.VaoSerializer;
import com.sibam.dto.ApiErrorResponse;
import com.sibam.graph.bootstrap.GraphBootstrap;
import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.output.Journey;
import com.sibam.graph.routing.AStarRouter;
import com.sibam.graph.routing.RouteAccessDistanceException;
import com.sibam.graph.routing.RoutingTimeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/compute")
public class ComputePathController {

    private static final Logger log = LoggerFactory.getLogger(ComputePathController.class);
    private static final ZoneId ROUTING_ZONE = ZoneId.of("Europe/Ljubljana");
    
    private final VaoSerializer vaoSerializer;
    private final GraphBootstrap graphBootstrap;
    private final AStarRouter aStarRouter;

    public ComputePathController(
            VaoSerializer vaoSerializer,
            GraphBootstrap graphBootstrap,
            AStarRouter aStarRouter
    ) {
        this.vaoSerializer = vaoSerializer;
        this.graphBootstrap = graphBootstrap;
        this.aStarRouter = aStarRouter;
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
            @RequestParam(required = false) String date,
            @RequestParam boolean bike,
            @RequestParam boolean bus,
            @RequestParam(required = false) String userId
            ) {
        if (bike) {
            graphBootstrap.refresh();
        } else {
            graphBootstrap.ensureInitialized();
        }

        LocalDate routingDate = resolveRoutingDate(date);
        RoutingTimeRequest routingTime = resolveRoutingTime(leaveNow, leaveAt, arriveBy);
        String resolvedOriginAddress = resolveAddress(originAddress, originAddressSnake);
        String resolvedDestinationAddress = resolveAddress(destinationAddress, destinationAddressSnake);
        Journey journey;
        try {
            journey = aStarRouter.findJourney(
                    originLat,
                    originLon,
                    destinationLat,
                    destinationLon,
                    resolvedOriginAddress,
                    resolvedDestinationAddress,
                    routingTime.time(),
                    routingDate,
                    bike,
                    bus,
                    routingTime.mode()
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

        if (journey == null) {
            return ResponseEntity.ok(new Journey(
                    "not_found",
                    new GeoPoint(originLat, originLon),
                    resolvedOriginAddress,
                    new GeoPoint(destinationLat, destinationLon),
                    resolvedDestinationAddress,
                    "0",
                    "0",
                    List.of()
            ));
        }

        return ResponseEntity.ok(journey);
    }


    private LocalDate resolveRoutingDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now(ROUTING_ZONE);
        }
        return LocalDate.parse(date);
    }

    private RoutingTimeRequest resolveRoutingTime(boolean leaveNow, String leaveAt, String arriveBy) {
        if (!leaveNow && arriveBy != null && !arriveBy.isBlank()) {
            return new RoutingTimeRequest(LocalTime.parse(arriveBy), RoutingTimeMode.ARRIVE_BY);
        }

        if (leaveNow || leaveAt == null || leaveAt.isBlank()) {
            return new RoutingTimeRequest(LocalTime.now(ROUTING_ZONE), RoutingTimeMode.DEPART_AT);
        }

        return new RoutingTimeRequest(LocalTime.parse(leaveAt), RoutingTimeMode.DEPART_AT);
    }

    private String resolveAddress(String camelCaseAddress, String snakeCaseAddress) {
        if (camelCaseAddress != null && !camelCaseAddress.isBlank()) {
            return camelCaseAddress;
        }

        return snakeCaseAddress;
    }

    private record RoutingTimeRequest(LocalTime time, RoutingTimeMode mode) {
    }
}
