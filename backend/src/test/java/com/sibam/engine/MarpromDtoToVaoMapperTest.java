package com.sibam.engine;

import com.sibam.dto.marprom.lines.MarpromLineDto;
import com.sibam.dto.marprom.routes.MarpromRouteDto;
import com.sibam.dto.marprom.routes.MarpromShapeNodeDto;
import com.sibam.dto.marprom.schedules.MarpromLineScheduleDto;
import com.sibam.dto.marprom.schedules.MarpromRouteScheduleDto;
import com.sibam.dto.marprom.schedules.MarpromStopScheduleDto;
import com.sibam.dto.marprom.stops.MarpromStopDto;
import com.sibam.engine.vao.BusStopVao;
import com.sibam.engine.vao.LineScheduleVao;
import com.sibam.engine.vao.RouteVao;
import com.sibam.engine.vao.StopScheduleVao;
import com.sibam.service.TransitDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarpromDtoToVaoMapperTest {

    private TransitDataService transitDataService;
    private MarpromDtoToVaoMapper mapper;

    @BeforeEach
    void setUp() {
        transitDataService = mock(TransitDataService.class);
        mapper = new MarpromDtoToVaoMapper(transitDataService);
    }

    // mapBusStops

    @Test
    void mapBusStopsBuildsCorrectlyKeyedMap() {
        MarpromStopDto stop = new MarpromStopDto(42, "Slomskov trg", "Slomškov trg 1", 46.556, 15.646);
        when(transitDataService.getBusStops()).thenReturn(List.of(stop));

        Map<Integer, BusStopVao> result = mapper.mapBusStops();

        assertThat(result).containsKey(42);
        BusStopVao vao = result.get(42);
        assertThat(vao.id()).isEqualTo(42);
        assertThat(vao.name()).isEqualTo("Slomskov trg");
        assertThat(vao.address()).isEqualTo("Slomškov trg 1");
        assertThat(vao.lat()).isEqualTo(46.556);
        assertThat(vao.lon()).isEqualTo(15.646);
    }

    @Test
    void mapBusStopsReturnsEmptyMapWhenServiceReturnsNull() {
        when(transitDataService.getBusStops()).thenReturn(null);

        Map<Integer, BusStopVao> result = mapper.mapBusStops();

        assertThat(result).isEmpty();
    }

    @Test
    void mapBusStopsHandlesMultipleStops() {
        MarpromStopDto stop1 = new MarpromStopDto(1, "Postaja A", "Ulica 1", 46.0, 15.0);
        MarpromStopDto stop2 = new MarpromStopDto(2, "Postaja B", "Ulica 2", 46.1, 15.1);
        when(transitDataService.getBusStops()).thenReturn(List.of(stop1, stop2));

        Map<Integer, BusStopVao> result = mapper.mapBusStops();

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys(1, 2);
    }

    //  mapRoutes

    @Test
    void mapRoutesReturnsEmptyMapWhenRoutesAreNull() {
        when(transitDataService.getAllRoutes()).thenReturn(null);
        when(transitDataService.getLines()).thenReturn(List.of());

        Map<Integer, RouteVao> result = mapper.mapRoutes();

        assertThat(result).isEmpty();
    }

    @Test
    void mapRoutesSkipsRouteWithNoMatchingLine() {
        MarpromShapeNodeDto node = new MarpromShapeNodeDto(1, 46.0, 15.0, null, null);
        MarpromRouteDto route = new MarpromRouteDto(101, 6, "Pekrska", List.of(node));
        // line list has no lineId=6 → route is skipped
        MarpromLineDto otherLine = new MarpromLineDto(9, "9", "Tezno", "#0F0", List.of());

        when(transitDataService.getAllRoutes()).thenReturn(List.of(route));
        when(transitDataService.getLines()).thenReturn(List.of(otherLine));
        when(transitDataService.getBusStops()).thenReturn(List.of());

        Map<Integer, RouteVao> result = mapper.mapRoutes();

        assertThat(result).isEmpty();
    }

    @Test
    void mapRoutesBuildsVaoWithMatchingLine() {
        MarpromShapeNodeDto node = new MarpromShapeNodeDto(1, 46.5, 15.6, null, null);
        MarpromRouteDto route = new MarpromRouteDto(101, 6, "Pekrska cesta", List.of(node));
        MarpromLineDto line = new MarpromLineDto(6, "6", "Pekrska cesta", "#FF0000", List.of());

        when(transitDataService.getAllRoutes()).thenReturn(List.of(route));
        when(transitDataService.getLines()).thenReturn(List.of(line));
        when(transitDataService.getBusStops()).thenReturn(List.of());

        Map<Integer, RouteVao> result = mapper.mapRoutes();

        assertThat(result).containsKey(101);
        RouteVao vao = result.get(101);
        assertThat(vao.routeId()).isEqualTo(101);
        assertThat(vao.LineId()).isEqualTo(6);
        assertThat(vao.code()).isEqualTo("6");
        assertThat(vao.headsignName()).isEqualTo("Pekrska cesta");
    }

    @Test
    void mapRoutesIncludesShapeNodesThatAreBusStops() {
        // stopPointId != null → isBusStop() == true
        MarpromShapeNodeDto busStopNode = new MarpromShapeNodeDto(1, 46.5, 15.6, 42, 99);
        MarpromShapeNodeDto nonStopNode = new MarpromShapeNodeDto(2, 46.51, 15.61, null, null);
        MarpromRouteDto route = new MarpromRouteDto(101, 6, "Pekrska", List.of(busStopNode, nonStopNode));
        MarpromLineDto line = new MarpromLineDto(6, "6", "Pekrska", "#FF0000", List.of());
        MarpromStopDto stop = new MarpromStopDto(42, "Postaja", "Ulica", 46.5, 15.6);

        when(transitDataService.getAllRoutes()).thenReturn(List.of(route));
        when(transitDataService.getLines()).thenReturn(List.of(line));
        when(transitDataService.getBusStops()).thenReturn(List.of(stop));

        Map<Integer, RouteVao> result = mapper.mapRoutes();

        RouteVao vao = result.get(101);
        assertThat(vao.busStops()).hasSize(1);
        assertThat(vao.busStops().get(0).id()).isEqualTo(42);
        assertThat(vao.shapeNodes()).hasSize(2);
    }

    //  mapSchedules

    @Test
    void mapSchedulesReturnsEmptyMapWhenLinesIsNull() {
        when(transitDataService.getLines()).thenReturn(null);

        Map<Integer, StopScheduleVao> result = mapper.mapSchedules();

        assertThat(result).isEmpty();
    }

    @Test
    void mapSchedulesBuildsCorrectStructureForSingleLine() {
        MarpromStopDto stopDto = new MarpromStopDto(100, "Slomskov trg", "Slomškov trg 1", 46.556, 15.646);
        MarpromLineDto line6 = new MarpromLineDto(6, "6", "Pekrska cesta", "#FF0000", List.of());

        MarpromRouteScheduleDto rsd = new MarpromRouteScheduleDto("A→B", List.of("08:00", "09:00"));
        MarpromLineScheduleDto lsd = new MarpromLineScheduleDto(6, List.of(rsd));
        MarpromStopScheduleDto ssd = new MarpromStopScheduleDto(stopDto, List.of(lsd));

        when(transitDataService.getLines()).thenReturn(List.of(line6));
        when(transitDataService.getStopScheduleForLine(6)).thenReturn(List.of(ssd));

        Map<Integer, StopScheduleVao> result = mapper.mapSchedules();

        assertThat(result).containsKey(100);
        StopScheduleVao vao = result.get(100);
        assertThat(vao.stopPointId()).isEqualTo(100);
        assertThat(vao.name()).isEqualTo("Slomskov trg");
        assertThat(vao.scheduleForLine()).hasSize(1);
        assertThat(vao.scheduleForLine().get(0).lineId()).isEqualTo(6);
        assertThat(vao.scheduleForLine().get(0).routeAndSchedules().get(0).departures())
                .containsExactly("08:00", "09:00");
    }

    @Test
    void mapSchedulesMergesTwoLinesAtSameStop() {
        MarpromStopDto stopDto = new MarpromStopDto(100, "Slomskov trg", "Slomškov trg 1", 46.556, 15.646);
        MarpromLineDto line6 = new MarpromLineDto(6, "6", "Pekrska cesta", "#FF0000", List.of());
        MarpromLineDto line9 = new MarpromLineDto(9, "9", "Tezno", "#00FF00", List.of());

        MarpromRouteScheduleDto rsd6 = new MarpromRouteScheduleDto("A→B", List.of("08:00"));
        MarpromLineScheduleDto lsd6 = new MarpromLineScheduleDto(6, List.of(rsd6));
        MarpromStopScheduleDto ssd6 = new MarpromStopScheduleDto(stopDto, List.of(lsd6));

        MarpromRouteScheduleDto rsd9 = new MarpromRouteScheduleDto("X→Y", List.of("09:30"));
        MarpromLineScheduleDto lsd9 = new MarpromLineScheduleDto(9, List.of(rsd9));
        MarpromStopScheduleDto ssd9 = new MarpromStopScheduleDto(stopDto, List.of(lsd9));

        when(transitDataService.getLines()).thenReturn(List.of(line6, line9));
        when(transitDataService.getStopScheduleForLine(6)).thenReturn(List.of(ssd6));
        when(transitDataService.getStopScheduleForLine(9)).thenReturn(List.of(ssd9));

        Map<Integer, StopScheduleVao> result = mapper.mapSchedules();

        assertThat(result).containsKey(100);
        StopScheduleVao vao = result.get(100);
        // both lines should be present after merging
        assertThat(vao.scheduleForLine()).hasSize(2);
        assertThat(vao.scheduleForLine().stream().map(LineScheduleVao::lineId).toList())
                .containsExactlyInAnyOrder(6, 9);
    }

    @Test
    void mapSchedulesSkipsLineWhenStopScheduleIsNull() {
        MarpromLineDto line6 = new MarpromLineDto(6, "6", "Pekrska cesta", "#FF0000", List.of());

        when(transitDataService.getLines()).thenReturn(List.of(line6));
        when(transitDataService.getStopScheduleForLine(6)).thenReturn(null);

        Map<Integer, StopScheduleVao> result = mapper.mapSchedules();

        assertThat(result).isEmpty();
    }
}
