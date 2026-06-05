package com.sibam.api;

import com.sibam.engine.VaoSerializer;
import com.sibam.dto.ApiErrorResponse;
import com.sibam.graph.bootstrap.GraphBootstrap;
import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.output.RouteAlternativesResponse;
import com.sibam.graph.routing.RouteAlternativeService;
import com.sibam.graph.routing.RouteAccessDistanceException;
import com.sibam.graph.routing.RoutingTimeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Controller za javni API izračuna poti med izvorom in ciljem.
 *
 * Sprejme koordinate, časovne nastavitve in dovoljene načine prevoza, po potrebi
 * osveži graf z MBajk podatki ter vrne alternative v obliki, ki jo uporablja
 * frontend zemljevid.
 */
@RestController
@RequestMapping("/compute")
public class ComputePathController {

    private static final Logger log = LoggerFactory.getLogger(ComputePathController.class);
    private static final ZoneId ROUTING_ZONE = ZoneId.of("Europe/Ljubljana");
    
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

    /**
     * Izračuna alternativne multimodalne poti za podane koordinate.
     *
     * @param originLat zemljepisna širina izvora
     * @param originLon zemljepisna dolžina izvora
     * @param destinationLat zemljepisna širina cilja
     * @param destinationLon zemljepisna dolžina cilja
     * @param originAddress naslov izvora v camelCase obliki, če je podan
     * @param destinationAddress naslov cilja v camelCase obliki, če je podan
     * @param originAddressSnake naslov izvora v snake_case obliki, če je podan
     * @param destinationAddressSnake naslov cilja v snake_case obliki, če je podan
     * @param leaveNow ali naj se kot čas odhoda uporabi trenutni čas
     * @param leaveAt čas odhoda v načinu DEPART_AT, če ni uporabljen leaveNow
     * @param arriveBy želeni čas prihoda; ima prednost pred leaveAt
     * @param date datum usmerjanja; privzeto je današnji datum v Europe/Ljubljana
     * @param bike ali so dovoljene MBajk BIKE etape
     * @param bus ali so dovoljene Marprom BUS etape
     * @param userId identifikator uporabnika; trenutno ne vpliva na izračun poti
     * @return HTTP odgovor z alternativami ali napako, če je izvor/cilj predaleč od grafa
     */
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
        RouteAlternativesResponse response;
        try {
            response = routeAlternativeService.findAlternatives(
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

    /**
     * Razreši datum usmerjanja iz zahtevka ali uporabi današnji lokalni datum.
     *
     * @param date datum v ISO obliki yyyy-MM-dd
     * @return datum, uporabljen pri iskanju voznih redov
     */
    private LocalDate resolveRoutingDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now(ROUTING_ZONE);
        }
        return LocalDate.parse(date);
    }

    /**
     * Razreši časovni način zahtevka za routing.
     *
     * @param leaveNow ali naj se uporabi trenutni čas
     * @param leaveAt čas odhoda v obliki HH:mm
     * @param arriveBy želeni čas prihoda v obliki HH:mm
     * @return čas in način DEPART_AT ali ARRIVE_BY
     */
    private RoutingTimeRequest resolveRoutingTime(boolean leaveNow, String leaveAt, String arriveBy) {
        if (!leaveNow && arriveBy != null && !arriveBy.isBlank()) {
            return new RoutingTimeRequest(LocalTime.parse(arriveBy), RoutingTimeMode.ARRIVE_BY);
        }

        if (leaveNow || leaveAt == null || leaveAt.isBlank()) {
            return new RoutingTimeRequest(LocalTime.now(ROUTING_ZONE), RoutingTimeMode.DEPART_AT);
        }

        return new RoutingTimeRequest(LocalTime.parse(leaveAt), RoutingTimeMode.DEPART_AT);
    }

    /**
     * Izbere naslov iz camelCase ali snake_case parametra.
     *
     * @param camelCaseAddress naslov iz novejšega API parametra
     * @param snakeCaseAddress naslov iz kompatibilnega snake_case parametra
     * @return izbran naslov ali null, če ni podan
     */
    private String resolveAddress(String camelCaseAddress, String snakeCaseAddress) {
        if (camelCaseAddress != null && !camelCaseAddress.isBlank()) {
            return camelCaseAddress;
        }

        return snakeCaseAddress;
    }

    private record RoutingTimeRequest(LocalTime time, RoutingTimeMode mode) {
    }
}
