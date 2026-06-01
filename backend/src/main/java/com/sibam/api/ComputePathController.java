package com.sibam.api;

import com.sibam.engine.VaoSerializer;
import com.sibam.graph.bootstrap.GraphBootstrap;
import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.output.Journey;
import com.sibam.graph.routing.AStarRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/compute")
public class ComputePathController {

    private static final Logger log = LoggerFactory.getLogger(ComputePathController.class);
    
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
    public Journey computePath(
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
        Journey journey = aStarRouter.findJourney(
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

        log.info("Received path computation request with origin ({}, {})", originLat, originLon);

        if (journey == null) {
            return new Journey(
                    "not_found",
                    new GeoPoint(originLat, originLon),
                    resolvedOriginAddress,
                    new GeoPoint(destinationLat, destinationLon),
                    resolvedDestinationAddress,
                    "0",
                    "0",
                    List.of()
            );
        }

        return journey;
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
