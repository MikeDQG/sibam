package com.sibam.engine;

import com.sibam.engine.vao.BusStopVao;
import com.sibam.engine.vao.DailyScheduleCacheVao;
import com.sibam.engine.vao.RouteVao;
import com.sibam.engine.vao.StopScheduleVao;
import com.sibam.engine.vao.WeeklyScheduleCacheVao;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class VaoSerializer {

    private final MarpromDtoToVaoMapper marpromDtoToVaoMapper;

    private Map<Integer, BusStopVao> busStopsMap = new HashMap<>();
    private Map<Integer, RouteVao> routesMap = new HashMap<>();
    private Map<Integer, StopScheduleVao> schedulesMap = new HashMap<>();
    private WeeklyScheduleCacheVao weeklyScheduleCache = emptyWeeklyScheduleCache();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache location (relative to working directory): data/cache/vao
    private final Path cacheDir = Path.of("data", "cache", "vao");
    private final Path busStopsFile = cacheDir.resolve("busStops.json");
    private final Path routesFile = cacheDir.resolve("routes.json");
    private final Path schedulesFile = cacheDir.resolve("schedules.json");
    private final Path scheduleDatesDir = cacheDir.resolve("schedules");
    private final Path scheduleVariantsDir = scheduleDatesDir.resolve("variants");
    private int lastFetchedScheduleDateCount = 0;

    public VaoSerializer(MarpromDtoToVaoMapper marpromDtoToVaoMapper) {
        this.marpromDtoToVaoMapper = marpromDtoToVaoMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void fetchData() {
        if (staticCacheExists() && loadStaticFromDisk()) {
            System.out.println("Static VAO cache loaded from disk.");
        } else {
            busStopsMap = marpromDtoToVaoMapper.mapBusStops();
            routesMap = marpromDtoToVaoMapper.mapRoutes();
            saveStaticToDisk();
        }

        refreshWeeklyScheduleCache();
        schedulesMap = getSchedulesMap(LocalDate.now());
        saveTodayScheduleSnapshot();
        logWeeklyScheduleCache(lastFetchedScheduleDateCount == 0 ? "loaded" : "refreshed");
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
    public Map<Integer, StopScheduleVao> getSchedulesMap(LocalDate date) {
        if (weeklyScheduleCache == null
                || weeklyScheduleCache.dateToScheduleKey() == null
                || weeklyScheduleCache.uniqueSchedules() == null) {
            return schedulesMap;
        }

        String scheduleKey = weeklyScheduleCache.dateToScheduleKey().get(date.toString());
        Map<Integer, StopScheduleVao> schedule = weeklyScheduleCache.uniqueSchedules().get(scheduleKey);
        return schedule == null ? schedulesMap : schedule;
    }

    public List<LocalDate> getScheduleDates() {
        if (weeklyScheduleCache == null || weeklyScheduleCache.dates() == null) {
            return List.of();
        }
        return weeklyScheduleCache.dates().stream()
                .map(LocalDate::parse)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    public WeeklyScheduleCacheVao getWeeklyScheduleCache() {
        return weeklyScheduleCache;
    }

    public boolean isRouteActiveOnDate(int routeId, LocalDate date) {
        if (weeklyScheduleCache == null || weeklyScheduleCache.activeRouteIdsByDate() == null) {
            return true;
        }
        List<Integer> routeIds = weeklyScheduleCache.activeRouteIdsByDate().get(date.toString());
        return routeIds == null || routeIds.isEmpty() || routeIds.contains(routeId);
    }

    /**
     * Persist current VAO maps to disk as JSON files.
     */
    public synchronized void saveToDisk() {
        saveStaticToDisk();
        saveTodayScheduleSnapshot();
    }

    private synchronized void saveStaticToDisk() {
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

            System.out.println("Static VAO cache saved to disk at: " + cacheDir.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save static VAO cache: " + e.getMessage());
        }
    }

    private synchronized void saveTodayScheduleSnapshot() {
        try {
            if (Files.notExists(cacheDir)) {
                Files.createDirectories(cacheDir);
            }

            Path tmpSchedules = Files.createTempFile(cacheDir, "schedules", ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpSchedules.toFile(), schedulesMap);
            Files.move(tmpSchedules, schedulesFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        } catch (IOException e) {
            System.err.println("Failed to save today's schedule snapshot: " + e.getMessage());
        }
    }

    /**
     * Load VAO maps from disk if present. Returns true on success.
     */
    public synchronized boolean loadFromDisk() {
        if (!staticCacheExists()) return false;
        boolean staticLoaded = loadStaticFromDisk();
        if (!staticLoaded) {
            return false;
        }
        refreshWeeklyScheduleCache();
        schedulesMap = getSchedulesMap(LocalDate.now());
        return true;
    }

    private synchronized boolean loadStaticFromDisk() {
        if (!staticCacheExists()) return false;
        try {
            Map<Integer, BusStopVao> loadedStops = objectMapper.readValue(
                    busStopsFile.toFile(), new TypeReference<Map<Integer, BusStopVao>>(){});
            Map<Integer, RouteVao> loadedRoutes = objectMapper.readValue(
                    routesFile.toFile(), new TypeReference<Map<Integer, RouteVao>>(){});

            if (loadedStops != null) this.busStopsMap = loadedStops; else this.busStopsMap = new HashMap<>();
            if (loadedRoutes != null) this.routesMap = loadedRoutes; else this.routesMap = new HashMap<>();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to load static VAO cache, will refetch: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check whether cache files exist on disk.
     */
    public boolean cacheExists() {
        return staticCacheExists() && hasDateScheduleFiles(currentSevenDayWindow());
    }

    private boolean staticCacheExists() {
        return Files.exists(busStopsFile)
                && Files.exists(routesFile);
    }

    private boolean hasDateScheduleFiles(List<LocalDate> dates) {
        return dates.stream().allMatch(date -> Files.exists(dailyScheduleFile(date)));
    }

    private void refreshWeeklyScheduleCache() {
        lastFetchedScheduleDateCount = 0;
        List<LocalDate> dates = currentSevenDayWindow();
        Map<String, String> dateToScheduleKey = new LinkedHashMap<>();
        Map<String, Map<Integer, StopScheduleVao>> uniqueSchedules = new LinkedHashMap<>();
        Map<String, List<Integer>> activeRouteIdsByDate = new LinkedHashMap<>();

        try {
            Files.createDirectories(scheduleDatesDir);
            Files.createDirectories(scheduleVariantsDir);
            deleteScheduleFilesOutsideWindow(dates);

            for (LocalDate date : dates) {
                DailyScheduleCacheVao daily = loadDailySchedule(date);
                Map<Integer, StopScheduleVao> schedule = daily == null ? null : loadScheduleVariant(daily.scheduleKey());

                if (daily == null || schedule == null) {
                    schedule = marpromDtoToVaoMapper.mapSchedules(date);
                    String scheduleKey = marpromDtoToVaoMapper.hashSchedule(schedule);
                    List<Integer> activeRouteIds = marpromDtoToVaoMapper.mapActiveRouteIds(date);
                    daily = new DailyScheduleCacheVao(date.toString(), scheduleKey, activeRouteIds);
                    saveScheduleVariant(scheduleKey, schedule);
                    saveDailySchedule(daily);
                    lastFetchedScheduleDateCount++;
                }

                dateToScheduleKey.put(daily.date(), daily.scheduleKey());
                uniqueSchedules.putIfAbsent(daily.scheduleKey(), schedule);
                activeRouteIdsByDate.put(daily.date(), daily.activeRouteIds() == null ? List.of() : daily.activeRouteIds());
            }

            deleteUnreferencedScheduleVariants(Set.copyOf(dateToScheduleKey.values()));
            weeklyScheduleCache = new WeeklyScheduleCacheVao(
                    dates.stream().map(LocalDate::toString).toList(),
                    dateToScheduleKey,
                    uniqueSchedules,
                    activeRouteIdsByDate
            );
        } catch (IOException e) {
            System.err.println("Failed to refresh dated Marprom schedule cache: " + e.getMessage());
            weeklyScheduleCache = emptyWeeklyScheduleCache();
        }
    }

    private DailyScheduleCacheVao loadDailySchedule(LocalDate date) throws IOException {
        Path file = dailyScheduleFile(date);
        if (!Files.exists(file)) {
            return null;
        }
        return objectMapper.readValue(file.toFile(), DailyScheduleCacheVao.class);
    }

    private Map<Integer, StopScheduleVao> loadScheduleVariant(String scheduleKey) throws IOException {
        if (scheduleKey == null || scheduleKey.isBlank()) {
            return null;
        }
        Path file = scheduleVariantFile(scheduleKey);
        if (!Files.exists(file)) {
            return null;
        }
        return objectMapper.readValue(file.toFile(), new TypeReference<Map<Integer, StopScheduleVao>>(){});
    }

    private void saveDailySchedule(DailyScheduleCacheVao daily) throws IOException {
        Path target = dailyScheduleFile(LocalDate.parse(daily.date()));
        Path tmp = Files.createTempFile(scheduleDatesDir, daily.date(), ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), daily);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void saveScheduleVariant(String scheduleKey, Map<Integer, StopScheduleVao> schedule) throws IOException {
        Path target = scheduleVariantFile(scheduleKey);
        if (Files.exists(target)) {
            return;
        }
        Path tmp = Files.createTempFile(scheduleVariantsDir, scheduleKey, ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), schedule);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void deleteScheduleFilesOutsideWindow(List<LocalDate> dates) throws IOException {
        Set<String> activeDates = dates.stream().map(LocalDate::toString).collect(java.util.stream.Collectors.toSet());
        if (!Files.exists(scheduleDatesDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(scheduleDatesDir)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> !activeDates.contains(path.getFileName().toString().replace(".json", "")))
                    .forEach(this::deleteQuietly);
        }
    }

    private void deleteUnreferencedScheduleVariants(Set<String> referencedKeys) throws IOException {
        if (!Files.exists(scheduleVariantsDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(scheduleVariantsDir)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> !referencedKeys.contains(path.getFileName().toString().replace(".json", "")))
                    .forEach(this::deleteQuietly);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Failed to delete stale schedule cache file " + path.toAbsolutePath() + ": " + e.getMessage());
        }
    }

    private Path dailyScheduleFile(LocalDate date) {
        return scheduleDatesDir.resolve(date + ".json");
    }

    private Path scheduleVariantFile(String scheduleKey) {
        return scheduleVariantsDir.resolve(scheduleKey + ".json");
    }

    private List<LocalDate> currentSevenDayWindow() {
        LocalDate today = LocalDate.now();
        return java.util.stream.IntStream.range(0, 7)
                .mapToObj(today::plusDays)
                .toList();
    }

    private WeeklyScheduleCacheVao emptyWeeklyScheduleCache() {
        return new WeeklyScheduleCacheVao(List.of(), Map.of(), Map.of(), Map.of());
    }

    private void logWeeklyScheduleCache(String mode) {
        int fetchedDates = weeklyScheduleCache == null || weeklyScheduleCache.dates() == null
                ? 0
                : weeklyScheduleCache.dates().size();
        int uniqueVariants = weeklyScheduleCache == null ? 0 : weeklyScheduleCache.uniqueScheduleCount();
        int linesProcessed = routesMap == null
                ? 0
                : (int) routesMap.values().stream().map(RouteVao::LineId).distinct().count();

        System.out.printf(
                "Weekly Marprom schedule cache %s: dates=%s, lines=%s, uniqueScheduleVariants=%s, file=%s%n",
                mode,
                fetchedDates,
                linesProcessed,
                uniqueVariants,
                scheduleDatesDir.toAbsolutePath()
        );
        System.out.printf("Weekly Marprom schedule cache fetchedDates=%s%n", lastFetchedScheduleDateCount);
    }
}
