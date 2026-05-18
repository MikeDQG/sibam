package com.sibam.engine;

import com.sibam.dto.marprom.lines.MarpromLineDto;
import com.sibam.dto.marprom.routes.MarpromRouteDto;
import com.sibam.dto.marprom.routes.MarpromShapeNodeDto;
import com.sibam.dto.marprom.stops.MarpromStopDto;
import com.sibam.engine.vao.BusStopVao;
import com.sibam.engine.vao.RouteVao;
import com.sibam.engine.vao.LineScheduleVao;
import com.sibam.engine.vao.RouteScheduleVao;
import com.sibam.engine.vao.StopScheduleVao;
import com.sibam.dto.marprom.schedules.MarpromStopScheduleDto;
import com.sibam.dto.marprom.schedules.MarpromLineScheduleDto;
import com.sibam.dto.marprom.schedules.MarpromRouteScheduleDto;
import com.sibam.engine.vao.ShapeNodeVao;
import com.sibam.service.TransitDataService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MarpromDtoToVaoMapper {

    private final TransitDataService transitDataService;

    public MarpromDtoToVaoMapper(TransitDataService transitDataService) {
        this.transitDataService = transitDataService;
    }

    public Map<Integer, BusStopVao> mapBusStops() {
        List<MarpromStopDto> stops = transitDataService.getBusStops();
        Map<Integer, BusStopVao> busStopsMap = new HashMap<Integer, BusStopVao>();
        if (stops == null) {
            return busStopsMap;
        }
        for (MarpromStopDto stop : stops) {
            busStopsMap.put(stop.id(), new BusStopVao(
                    stop.id(), stop.name(), stop.address(), stop.lat(), stop.lon()
            ));
        }
        return busStopsMap;
    }

    public Map<Integer, RouteVao> mapRoutes() {
        List<MarpromRouteDto> allRoutes = transitDataService.getAllRoutes();
        List<MarpromLineDto> lines = transitDataService.getLines();
        Map<Integer, RouteVao> routesMap = new HashMap<Integer, RouteVao>();
        if (allRoutes == null || lines == null) {
            return routesMap;
        }

        Map<Integer, BusStopVao> busStopsMap = mapBusStops();

        for (MarpromRouteDto route : allRoutes) {
            MarpromLineDto line = lines.stream().filter(l -> l.lineId() == route.lineId()).findFirst().orElse(null);
            if (line == null) {
                continue;
            }
            List<MarpromShapeNodeDto> shapeNodes = route.shapeNodes();
            List<ShapeNodeVao> shapeNodeVaos = new ArrayList<>();
            List<BusStopVao> busStops = new ArrayList<>();
            for (MarpromShapeNodeDto shapeNode : shapeNodes) {
                shapeNodeVaos.add(new ShapeNodeVao(
                        shapeNode.sequenceNo(), shapeNode.lat(), shapeNode.lon(), shapeNode.stopPointId()));
                if (shapeNode.isBusStop()) {
                    BusStopVao bs = busStopsMap.get(shapeNode.stopPointId());
                    if (bs != null) {
                        busStops.add(bs);
                    }
                }
            }
            routesMap.put(route.routeId(), new RouteVao(
                    route.routeId(), route.lineId(), line.code(), line.destination(), route.headsignName(), shapeNodeVaos, busStops
            ));
        }
        return routesMap;
    }

    public Map<Integer, StopScheduleVao> mapSchedules() {
        List<MarpromLineDto> lines = transitDataService.getLines();
        Map<Integer, StopScheduleVao> schedulesByStop = new HashMap<>();
        if (lines == null) {
            return schedulesByStop;
        }

        // Helper: for merging line schedules per stop
        Map<Integer, Map<Integer, LineScheduleVao>> tmpByStopLine = new HashMap<>();

        for (MarpromLineDto line : lines) {
            List<MarpromStopScheduleDto> stopSchedules = transitDataService.getStopScheduleForLine(line.lineId());
            if (stopSchedules == null) continue;

            for (MarpromStopScheduleDto stopSchedule : stopSchedules) {
                int stopId = stopSchedule.stopPoint().id();

                // Map route schedules
                List<LineScheduleVao> lineSchedules = new ArrayList<>();
                for (MarpromLineScheduleDto lsd : stopSchedule.scheduleForLine()) {
                    List<RouteScheduleVao> routeSchedules = new ArrayList<>();
                    for (MarpromRouteScheduleDto rsd : lsd.routeAndSchedules()) {
                        routeSchedules.add(new RouteScheduleVao(rsd.direction(), rsd.departures()));
                    }
                    lineSchedules.add(new LineScheduleVao(lsd.lineId(), routeSchedules));
                }

                // Merge into tmp map per stop -> per line
                Map<Integer, LineScheduleVao> byLine = tmpByStopLine.computeIfAbsent(stopId, k -> new HashMap<>());
                for (LineScheduleVao lsv : lineSchedules) {
                    byLine.put(lsv.lineId(), lsv); // replace or insert
                }

                // Build/update StopScheduleVao shell (without line list yet)
                StopScheduleVao existing = schedulesByStop.get(stopId);
                if (existing == null) {
                    schedulesByStop.put(stopId, new StopScheduleVao(
                            stopId,
                            stopSchedule.stopPoint().name(),
                            stopSchedule.stopPoint().address(),
                            new ArrayList<>()
                    ));
                }
            }
        }

        // Now finalize StopScheduleVao lists from tmpByStopLine
        for (Map.Entry<Integer, Map<Integer, LineScheduleVao>> e : tmpByStopLine.entrySet()) {
            int stopId = e.getKey();
            Map<Integer, LineScheduleVao> byLine = e.getValue();
            List<LineScheduleVao> list = new ArrayList<>(byLine.values());
            StopScheduleVao base = schedulesByStop.get(stopId);
            if (base == null) continue;
            schedulesByStop.put(stopId, new StopScheduleVao(
                    base.stopPointId(), base.name(), base.address(), list
            ));
        }

        return schedulesByStop;
    }
}
