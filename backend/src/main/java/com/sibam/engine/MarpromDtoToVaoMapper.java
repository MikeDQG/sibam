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
import com.sibam.engine.vao.WeeklyScheduleCacheVao;
import com.sibam.dto.marprom.schedules.MarpromStopScheduleDto;
import com.sibam.dto.marprom.schedules.MarpromLineScheduleDto;
import com.sibam.dto.marprom.schedules.MarpromRouteScheduleDto;
import com.sibam.engine.vao.ShapeNodeVao;
import com.sibam.service.TransitDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper iz Marprom DTO odgovorov v interne VAO objekte.
 *
 * VAO modeli so stabilen vhod za cache, gradnjo grafa in časovno odvisne BUS
 * robove, zato mapper normalizira tudi vrstni red voznih redov.
 */
@Service
public class MarpromDtoToVaoMapper {

    private final TransitDataService transitDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MarpromDtoToVaoMapper(TransitDataService transitDataService) {
        this.transitDataService = transitDataService;
    }

    /**
     * Preslika Marprom postajališča v mapo BusStopVao po ID-ju postaje.
     *
     * @return mapa avtobusnih postaj za graf
     */
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

    /**
     * Preslika Marprom trase, shape točke in postaje v RouteVao objekte.
     *
     * @return mapa tras po routeId
     */
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

    /**
     * Preslika današnje vozne rede v mapo po postaji.
     *
     * @return vozni redi za današnji datum
     */
    public Map<Integer, StopScheduleVao> mapSchedules() {
        return mapSchedules(LocalDate.now());
    }

    /**
     * Preslika vozne rede za podan datum.
     *
     * @param date datum voznega reda
     * @return mapa voznih redov po stopId
     */
    public Map<Integer, StopScheduleVao> mapSchedules(LocalDate date) {
        return mapSchedules(date.toString());
    }

    /**
     * Preslika vozne rede za datum v obliki, ki jo pričakuje Marprom API.
     *
     * @param date datum v obliki yyyy-MM-dd
     * @return normalizirana mapa voznih redov po stopId
     */
    public Map<Integer, StopScheduleVao> mapSchedules(String date) {
        List<MarpromLineDto> lines = transitDataService.getLines(date);
        Map<Integer, StopScheduleVao> schedulesByStop = new HashMap<>();
        if (lines == null) {
            return schedulesByStop;
        }

        // Helper: for merging line schedules per stop
        Map<Integer, Map<Integer, LineScheduleVao>> tmpByStopLine = new HashMap<>();

        for (MarpromLineDto line : lines) {
            List<MarpromStopScheduleDto> stopSchedules = transitDataService.getStopScheduleForLine(line.lineId(), date);
            if (stopSchedules == null) continue;

            for (MarpromStopScheduleDto stopSchedule : stopSchedules) {
                if (stopSchedule.stopPoint() == null) {
                    continue;
                }
                int stopId = stopSchedule.stopPoint().id();

                // Map route schedules
                List<LineScheduleVao> lineSchedules = new ArrayList<>();
                if (stopSchedule.scheduleForLine() == null) {
                    continue;
                }
                for (MarpromLineScheduleDto lsd : stopSchedule.scheduleForLine()) {
                    List<RouteScheduleVao> routeSchedules = new ArrayList<>();
                    if (lsd.routeAndSchedules() != null) {
                        for (MarpromRouteScheduleDto rsd : lsd.routeAndSchedules()) {
                            routeSchedules.add(new RouteScheduleVao(rsd.direction(), safeList(rsd.departures())));
                        }
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

        return normalizeScheduleMap(schedulesByStop);
    }

    /**
     * Zgradi tedenski cache voznih redov z deduplikacijo enakih urnikov.
     *
     * @param dates datumi, ki jih mora cache pokrivati
     * @return tedenski VAO cache z unikatnimi varianti urnikov
     */
    public WeeklyScheduleCacheVao mapWeeklySchedules(List<LocalDate> dates) {
        Map<String, String> dateToScheduleKey = new LinkedHashMap<>();
        Map<String, Map<Integer, StopScheduleVao>> uniqueSchedules = new LinkedHashMap<>();
        Map<String, List<Integer>> activeRouteIdsByDate = new LinkedHashMap<>();
        List<String> dateStrings = dates.stream().map(LocalDate::toString).toList();

        for (LocalDate date : dates) {
            Map<Integer, StopScheduleVao> schedule = mapSchedules(date);
            String scheduleKey = hashSchedule(schedule);
            uniqueSchedules.putIfAbsent(scheduleKey, schedule);
            dateToScheduleKey.put(date.toString(), scheduleKey);
            activeRouteIdsByDate.put(date.toString(), mapActiveRouteIds(date));
        }

        return new WeeklyScheduleCacheVao(dateStrings, dateToScheduleKey, uniqueSchedules, activeRouteIdsByDate);
    }

    public List<Integer> mapActiveRouteIds(LocalDate date) {
        return mapActiveRouteIds(date.toString());
    }

    /**
     * Vrne aktivne routeId vrednosti za izbran datum.
     *
     * @param date datum v obliki yyyy-MM-dd
     * @return urejen seznam aktivnih tras
     */
    public List<Integer> mapActiveRouteIds(String date) {
        List<MarpromRouteDto> activeRoutes = transitDataService.getAllRoutes(date);
        return safeList(activeRoutes).stream()
                .map(MarpromRouteDto::routeId)
                .distinct()
                .sorted()
                .toList();
    }

    private Map<Integer, StopScheduleVao> normalizeScheduleMap(Map<Integer, StopScheduleVao> schedulesByStop) {
        Map<Integer, StopScheduleVao> normalized = new LinkedHashMap<>();
        schedulesByStop.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> normalized.put(entry.getKey(), normalizeStopSchedule(entry.getValue())));
        return normalized;
    }

    private StopScheduleVao normalizeStopSchedule(StopScheduleVao stopSchedule) {
        List<LineScheduleVao> lineSchedules = safeList(stopSchedule.scheduleForLine()).stream()
                .map(this::normalizeLineSchedule)
                .sorted(Comparator.comparingInt(LineScheduleVao::lineId))
                .toList();
        return new StopScheduleVao(
                stopSchedule.stopPointId(),
                stopSchedule.name(),
                stopSchedule.address(),
                lineSchedules
        );
    }

    private LineScheduleVao normalizeLineSchedule(LineScheduleVao lineSchedule) {
        List<RouteScheduleVao> routeSchedules = safeList(lineSchedule.routeAndSchedules()).stream()
                .map(routeSchedule -> new RouteScheduleVao(
                        routeSchedule.direction(),
                        safeList(routeSchedule.departures()).stream().sorted().toList()
                ))
                .sorted(Comparator.comparing(RouteScheduleVao::direction, Comparator.nullsFirst(String::compareTo)))
                .toList();
        return new LineScheduleVao(lineSchedule.lineId(), routeSchedules);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    /**
     * Izračuna SHA-256 hash normaliziranega voznega reda.
     *
     * @param schedule vozni red po postajah
     * @return heksadecimalni hash za deduplikacijo schedule variant
     */
    public String hashSchedule(Map<Integer, StopScheduleVao> schedule) {
        try {
            byte[] json = objectMapper.writeValueAsString(schedule).getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash Marprom schedule", e);
        }
    }
}
