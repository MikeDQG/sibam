package com.sibam.service;

import com.sibam.dto.marprom.lines.MarpromLineDto;
import com.sibam.dto.marprom.lines.MarpromLinesResponse;
import com.sibam.dto.marprom.routes.MarpromRouteDto;
import com.sibam.dto.marprom.routes.MarpromRoutesResponseDto;
import com.sibam.dto.marprom.schedules.MarpromScheduleResponse;
import com.sibam.dto.marprom.stops.MarpromStopDto;
import com.sibam.dto.marprom.stops.MarpromStopsResponse;
import com.sibam.dto.marprom.trips.MarpromTripDto;
import com.sibam.dto.marprom.trips.MarpromTripsResponseDto;
import com.sibam.integration.marprom.MarpromClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransitDataServiceTest {

    private MarpromClient marpromClient;
    private TransitDataService service;

    @BeforeEach
    void setUp() {
        marpromClient = mock(MarpromClient.class);
        service = new TransitDataService(marpromClient);
    }

    // --- getBusStops ---

    @Test
    void getBusStopsReturnsNullWhenResponseIsNull() {
        when(marpromClient.getAllStops()).thenReturn(Mono.empty());

        assertThat(service.getBusStops()).isNull();
    }

    @Test
    void getBusStopsReturnsListWhenResponseHasData() {
        MarpromStopDto stop = new MarpromStopDto(1, "Trg svobode", "Trg svobode 1", 46.55, 15.64);
        when(marpromClient.getAllStops()).thenReturn(Mono.just(new MarpromStopsResponse(List.of(stop))));

        List<MarpromStopDto> result = service.getBusStops();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Trg svobode");
    }

    // --- getLines ---

    @Test
    void getLinesReturnsNullWhenResponseIsNull() {
        when(marpromClient.getLines("2026-01-01")).thenReturn(Mono.empty());

        assertThat(service.getLines("2026-01-01")).isNull();
    }

    @Test
    void getLinesReturnsListWhenResponseHasData() {
        MarpromLineDto line = new MarpromLineDto(1, "6", "Pobrežje", "#FF0000", List.of());
        when(marpromClient.getLines("2026-01-01")).thenReturn(Mono.just(new MarpromLinesResponse(List.of(line))));

        List<MarpromLineDto> result = service.getLines("2026-01-01");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("6");
    }

    // --- getRoutes ---

    @Test
    void getRoutesReturnsNullWhenResponseIsNull() {
        when(marpromClient.getRoutes(1, "2026-01-01")).thenReturn(Mono.empty());

        assertThat(service.getRoutes(1, "2026-01-01")).isNull();
    }

    @Test
    void getRoutesReturnsListWhenResponseHasData() {
        MarpromRouteDto route = new MarpromRouteDto(101, 1, "Pobrežje", List.of());
        when(marpromClient.getRoutes(1, "2026-01-01")).thenReturn(Mono.just(new MarpromRoutesResponseDto(List.of(route))));

        List<MarpromRouteDto> result = service.getRoutes(1, "2026-01-01");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).headsignName()).isEqualTo("Pobrežje");
    }

    // --- getAllRoutes ---

    @Test
    void getAllRoutesAggregatesRoutesAcrossAllLines() {
        MarpromLineDto line1 = new MarpromLineDto(1, "6", "Pobrežje", "#F00", List.of());
        MarpromLineDto line2 = new MarpromLineDto(2, "7", "Tezno", "#0F0", List.of());
        when(marpromClient.getLines("2026-01-01")).thenReturn(Mono.just(new MarpromLinesResponse(List.of(line1, line2))));

        MarpromRouteDto route1 = new MarpromRouteDto(101, 1, "Pobrežje", List.of());
        MarpromRouteDto route2 = new MarpromRouteDto(201, 2, "Tezno", List.of());
        when(marpromClient.getRoutes(1, "2026-01-01")).thenReturn(Mono.just(new MarpromRoutesResponseDto(List.of(route1))));
        when(marpromClient.getRoutes(2, "2026-01-01")).thenReturn(Mono.just(new MarpromRoutesResponseDto(List.of(route2))));

        List<MarpromRouteDto> result = service.getAllRoutes("2026-01-01");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MarpromRouteDto::routeId).containsExactlyInAnyOrder(101, 201);
    }

    @Test
    void getAllRoutesReturnsEmptyListWhenLinesResponseIsNull() {
        when(marpromClient.getLines("2026-01-01")).thenReturn(Mono.empty());

        List<MarpromRouteDto> result = service.getAllRoutes("2026-01-01");

        assertThat(result).isEmpty();
    }

    @Test
    void getAllRoutesSkipsLineWhenItsRoutesResponseIsNull() {
        MarpromLineDto line1 = new MarpromLineDto(1, "6", "Pobrežje", "#F00", List.of());
        MarpromLineDto line2 = new MarpromLineDto(2, "7", "Tezno", "#0F0", List.of());
        when(marpromClient.getLines("2026-01-01")).thenReturn(Mono.just(new MarpromLinesResponse(List.of(line1, line2))));

        MarpromRouteDto route1 = new MarpromRouteDto(101, 1, "Pobrežje", List.of());
        when(marpromClient.getRoutes(1, "2026-01-01")).thenReturn(Mono.just(new MarpromRoutesResponseDto(List.of(route1))));
        when(marpromClient.getRoutes(2, "2026-01-01")).thenReturn(Mono.empty());

        List<MarpromRouteDto> result = service.getAllRoutes("2026-01-01");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).routeId()).isEqualTo(101);
    }

    // --- getStopScheduleForLine ---

    @Test
    void getStopScheduleForLineReturnsNullWhenResponseIsNull() {
        when(marpromClient.getStopScheduleForLine(1, "2026-01-01")).thenReturn(Mono.empty());

        assertThat(service.getStopScheduleForLine(1, "2026-01-01")).isNull();
    }

    @Test
    void getStopScheduleForLineReturnsEmptyListWhenSchedulesIsEmpty() {
        when(marpromClient.getStopScheduleForLine(1, "2026-01-01"))
                .thenReturn(Mono.just(new MarpromScheduleResponse(List.of())));

        assertThat(service.getStopScheduleForLine(1, "2026-01-01")).isEmpty();
    }

    // --- getTrips ---

    @Test
    void getTripsReturnsNullWhenResponseIsNull() {
        when(marpromClient.getTrips(42)).thenReturn(Mono.empty());

        assertThat(service.getTrips(42)).isNull();
    }

    @Test
    void getTripsReturnsListWhenResponseHasData() {
        MarpromTripDto trip = new MarpromTripDto(1001, 1, 101, 5, "08:00", 1, "07:55");
        when(marpromClient.getTrips(42)).thenReturn(Mono.just(new MarpromTripsResponseDto(List.of(trip))));

        List<MarpromTripDto> result = service.getTrips(42);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tripId()).isEqualTo(1001);
    }
}
