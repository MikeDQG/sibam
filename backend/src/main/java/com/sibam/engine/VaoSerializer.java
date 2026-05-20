package com.sibam.engine;

import com.sibam.engine.vao.BusStopVao;
import com.sibam.engine.vao.RouteVao;
import com.sibam.engine.vao.StopScheduleVao;
import com.sibam.service.TransitDataService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@Service
public class VaoSerializer {

    private final MarpromDtoToVaoMapper marpromDtoToVaoMapper;

    private Map<Integer, BusStopVao> busStopsMap = new HashMap<>();
    private Map<Integer, RouteVao> routesMap = new HashMap<>();
    private Map<Integer, StopScheduleVao> schedulesMap = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache location (relative to working directory): data/cache/vao
    private final Path cacheDir = Path.of("data", "cache", "vao");
    private final Path busStopsFile = cacheDir.resolve("busStops.json");
    private final Path routesFile = cacheDir.resolve("routes.json");
    private final Path schedulesFile = cacheDir.resolve("schedules.json");

    public VaoSerializer(MarpromDtoToVaoMapper marpromDtoToVaoMapper) {
        this.marpromDtoToVaoMapper = marpromDtoToVaoMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void fetchData() {
        // Try to load from cache first
        if (cacheExists() && loadFromDisk()) {
            System.out.println("VAO cache loaded from disk.");
            return;
        }

        // Fallback to fetching from upstream and then persist
        busStopsMap = marpromDtoToVaoMapper.mapBusStops();
        routesMap = marpromDtoToVaoMapper.mapRoutes();
        schedulesMap = marpromDtoToVaoMapper.mapSchedules();
        saveToDisk();
    }


    public Map<Integer, BusStopVao> getBusStopsMap() {
        return busStopsMap;
    }
    public Map<Integer, RouteVao> getRoutesMap() {
        return routesMap;
    }
    public Map<Integer, StopScheduleVao> getSchedulesMap() {
        return schedulesMap;
    }

    /**
     * Persist current VAO maps to disk as JSON files.
     */
    public synchronized void saveToDisk() {
        try {
            if (Files.notExists(cacheDir)) {
                Files.createDirectories(cacheDir);
            }

            // Write atomically via temp files
            Path tmpStops = Files.createTempFile(cacheDir, "busStops", ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpStops.toFile(), busStopsMap);
            Files.move(tmpStops, busStopsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            Path tmpRoutes = Files.createTempFile(cacheDir, "routes", ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpRoutes.toFile(), routesMap);
            Files.move(tmpRoutes, routesFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            Path tmpSchedules = Files.createTempFile(cacheDir, "schedules", ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpSchedules.toFile(), schedulesMap);
            Files.move(tmpSchedules, schedulesFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            System.out.println("VAO cache saved to disk at: " + cacheDir.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save VAO cache: " + e.getMessage());
        }
    }

    /**
     * Load VAO maps from disk if present. Returns true on success.
     */
    public synchronized boolean loadFromDisk() {
        if (!cacheExists()) return false;
        try {
            Map<Integer, BusStopVao> loadedStops = objectMapper.readValue(
                    busStopsFile.toFile(), new TypeReference<Map<Integer, BusStopVao>>(){});
            Map<Integer, RouteVao> loadedRoutes = objectMapper.readValue(
                    routesFile.toFile(), new TypeReference<Map<Integer, RouteVao>>(){});
            Map<Integer, StopScheduleVao> loadedSchedules = objectMapper.readValue(
                    schedulesFile.toFile(), new TypeReference<Map<Integer, StopScheduleVao>>(){});

            if (loadedStops != null) this.busStopsMap = loadedStops; else this.busStopsMap = new HashMap<>();
            if (loadedRoutes != null) this.routesMap = loadedRoutes; else this.routesMap = new HashMap<>();
            if (loadedSchedules != null) this.schedulesMap = loadedSchedules; else this.schedulesMap = new HashMap<>();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to load VAO cache, will refetch: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check whether cache files exist on disk.
     */
    public boolean cacheExists() {
        return Files.exists(busStopsFile) && Files.exists(routesFile) && Files.exists(schedulesFile);
    }
}
