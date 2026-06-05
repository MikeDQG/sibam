package com.sibam.engine;

import com.sibam.cache.ArtifactCacheService;
import com.sibam.cache.ArtifactSource;
import com.sibam.cache.ScheduleCacheManifest;
import com.sibam.cache.Sha256;
import com.sibam.engine.vao.BusStopVao;
import com.sibam.engine.vao.DailyScheduleCacheVao;
import com.sibam.engine.vao.RouteVao;
import com.sibam.engine.vao.StopScheduleVao;
import com.sibam.engine.vao.WeeklyScheduleCacheVao;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Servis za pripravo in cacheiranje VAO podatkov za Marprom graf.
 *
 * Ob zagonu naloži statične postaje in trase iz lokalnega cache-a ali Marprom
 * API-ja, vzdržuje večdnevni cache voznih redov ter artefakte po potrebi
 * sinhronizira s Supabase Storage.
 */
@Service
public class VaoSerializer {

    private static final Logger log = LoggerFactory.getLogger(VaoSerializer.class);

    private final MarpromDtoToVaoMapper marpromDtoToVaoMapper;
    private final ArtifactCacheService artifactCacheService;
    private final Clock clock;

    private Map<Integer, BusStopVao> busStopsMap = new HashMap<>();
    private Map<Integer, RouteVao> routesMap = new HashMap<>();
    private Map<Integer, StopScheduleVao> schedulesMap = new HashMap<>();
    private WeeklyScheduleCacheVao weeklyScheduleCache = emptyWeeklyScheduleCache();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache location (relative to working directory): data/cache/vao
    private final Path cacheRoot;
    private final Path cacheDir;
    private final Path busStopsFile;
    private final Path routesFile;
    private final Path schedulesFile;
    private final Path scheduleDatesDir;
    private final Path scheduleVariantsDir;
    private final Path scheduleManifestFile;
    private int lastFetchedScheduleDateCount = 0;
    private int lastDeletedScheduleFileCount = 0;
    private List<LocalDate> lastMissingScheduleDates = List.of();
    private List<LocalDate> lastFetchedScheduleDates = List.of();

    @Value("${schedules.refresh.enabled:false}")
    private boolean scheduledRefreshEnabled;

    @Value("${schedules.cache.days-ahead:${schedules.refresh.days-ahead:6}}")
    private int refreshDaysAhead;

    @Autowired
    public VaoSerializer(
            MarpromDtoToVaoMapper marpromDtoToVaoMapper,
            ArtifactCacheService artifactCacheService,
            Clock clock,
            @Value("${cache.local-root:data/cache}") String cacheLocalRoot
    ) {
        this(marpromDtoToVaoMapper, artifactCacheService, clock, Path.of(cacheLocalRoot));
    }

    VaoSerializer(MarpromDtoToVaoMapper marpromDtoToVaoMapper, Clock clock, Path cacheRoot) {
        this(marpromDtoToVaoMapper, null, clock, cacheRoot);
    }

    VaoSerializer(
            MarpromDtoToVaoMapper marpromDtoToVaoMapper,
            ArtifactCacheService artifactCacheService,
            Clock clock,
            Path cacheRoot
    ) {
        this.marpromDtoToVaoMapper = marpromDtoToVaoMapper;
        this.artifactCacheService = artifactCacheService;
        this.clock = clock;
        this.cacheRoot = cacheRoot;
        this.cacheDir = cacheRoot.resolve("marprom").resolve("vao");
        this.busStopsFile = cacheDir.resolve("busStops.json");
        this.routesFile = cacheDir.resolve("routes.json");
        this.schedulesFile = cacheDir.resolve("schedules.json");
        this.scheduleDatesDir = cacheRoot.resolve("marprom").resolve("schedules").resolve("days");
        this.scheduleVariantsDir = cacheRoot.resolve("marprom").resolve("schedules").resolve("variants");
        this.scheduleManifestFile = cacheRoot.resolve("marprom").resolve("schedules").resolve("manifest.json");
    }

    /**
     * Ob zagonu aplikacije naloži statične VAO podatke in današnji vozni red.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void fetchData() {
        if (staticCacheExists() && loadStaticFromDisk()) {
            System.out.println("Static VAO cache loaded from disk.");
        } else {
            busStopsMap = marpromDtoToVaoMapper.mapBusStops();
            routesMap = marpromDtoToVaoMapper.mapRoutes();
            saveStaticToDisk();
        }

        refreshWeeklyScheduleCache("startup");
        schedulesMap = getSchedulesMap(today());
        saveTodayScheduleSnapshot();
    }

    /**
     * Nočno osveži večdnevni cache voznih redov, če je scheduler omogočen.
     */
    @Scheduled(cron = "${schedules.refresh.cron:0 0 3 * * *}", zone = "${schedules.refresh.zone:Europe/Ljubljana}")
    public void refreshWeeklyScheduleCacheNightly() {
        if (!scheduledRefreshEnabled) {
            return;
        }

        refreshWeeklyScheduleCache("nightly");
        schedulesMap = getSchedulesMap(today());
        saveTodayScheduleSnapshot();
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
     * Vrne vozni red za podan datum iz večdnevnega cache-a.
     *
     * @param date datum routinga
     * @return mapa voznih redov po stopId ali današnji fallback cache
     */
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

    /**
     * Vrne datume, ki jih trenutno pokriva večdnevni cache.
     *
     * @return urejen seznam datumov voznih redov
     */
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

    /**
     * Preveri, ali je Marprom trasa aktivna na izbran datum.
     *
     * @param routeId ID trase
     * @param date datum routinga
     * @return true, če je trasa aktivna ali če cache nima omejitve
     */
    public boolean isRouteActiveOnDate(int routeId, LocalDate date) {
        if (weeklyScheduleCache == null || weeklyScheduleCache.activeRouteIdsByDate() == null) {
            return true;
        }
        List<Integer> routeIds = weeklyScheduleCache.activeRouteIdsByDate().get(date.toString());
        return routeIds == null || routeIds.isEmpty() || routeIds.contains(routeId);
    }

    /**
     * Shrani trenutne VAO mape na disk kot JSON cache.
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
     * Naloži VAO mape iz diska, če cache obstaja in je berljiv.
     *
     * @return true, če je nalaganje uspelo
     */
    public synchronized boolean loadFromDisk() {
        if (!staticCacheExists()) return false;
        boolean staticLoaded = loadStaticFromDisk();
        if (!staticLoaded) {
            return false;
        }
        refreshWeeklyScheduleCache("loadFromDisk");
        schedulesMap = getSchedulesMap(today());
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
     * Preveri, ali obstajata statični cache in datumski schedule cache.
     *
     * @return true, če so potrebne cache datoteke prisotne
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

    /**
     * Ročno osveži večdnevni Marprom schedule cache.
     */
    public synchronized void refreshWeeklyScheduleCache() {
        refreshWeeklyScheduleCache("manual");
    }

    /**
     * Osveži večdnevni cache voznih redov iz lokalnih datotek, Supabase ali Marprom API-ja.
     *
     * @param mode oznaka vira klica za logiranje
     */
    private synchronized void refreshWeeklyScheduleCache(String mode) {
        lastFetchedScheduleDateCount = 0;
        lastDeletedScheduleFileCount = 0;
        lastMissingScheduleDates = List.of();
        lastFetchedScheduleDates = List.of();
        List<LocalDate> dates = currentSevenDayWindow();
        Map<String, String> dateToScheduleKey = new LinkedHashMap<>();
        Map<String, Map<Integer, StopScheduleVao>> uniqueSchedules = new LinkedHashMap<>();
        Map<String, List<Integer>> activeRouteIdsByDate = new LinkedHashMap<>();
        List<LocalDate> missingDates = new java.util.ArrayList<>();
        List<LocalDate> fetchedDates = new java.util.ArrayList<>();
        List<LocalDate> downloadedDates = new java.util.ArrayList<>();
        Map<LocalDate, DailyScheduleCacheVao> validDailySchedules = new LinkedHashMap<>();
        Map<String, Map<Integer, StopScheduleVao>> loadedScheduleVariants = new LinkedHashMap<>();
        Map<LocalDate, ArtifactSource> sourcesByDate = new LinkedHashMap<>();

        try {
            Files.createDirectories(scheduleDatesDir);
            Files.createDirectories(scheduleVariantsDir);

            for (LocalDate date : dates) {
                CachedSchedule cachedSchedule = loadValidCachedSchedule(date);
                if (cachedSchedule != null) {
                    validDailySchedules.put(date, cachedSchedule.daily());
                    loadedScheduleVariants.putIfAbsent(cachedSchedule.daily().scheduleKey(), cachedSchedule.schedule());
                    sourcesByDate.put(date, ArtifactSource.LOCAL);
                    continue;
                }

                if (restoreScheduleDateFromSupabase(date)) {
                    cachedSchedule = loadValidCachedSchedule(date);
                    if (cachedSchedule != null) {
                        validDailySchedules.put(date, cachedSchedule.daily());
                        loadedScheduleVariants.putIfAbsent(cachedSchedule.daily().scheduleKey(), cachedSchedule.schedule());
                        downloadedDates.add(date);
                        sourcesByDate.put(date, ArtifactSource.SUPABASE);
                        continue;
                    }
                }

                missingDates.add(date);
            }

            for (LocalDate date : missingDates) {
                Map<Integer, StopScheduleVao> schedule = marpromDtoToVaoMapper.mapSchedules(date);
                String scheduleKey = marpromDtoToVaoMapper.hashSchedule(schedule);
                List<Integer> activeRouteIds = marpromDtoToVaoMapper.mapActiveRouteIds(date);
                DailyScheduleCacheVao daily = new DailyScheduleCacheVao(date.toString(), scheduleKey, safeList(activeRouteIds));
                saveScheduleVariant(scheduleKey, schedule);
                saveDailySchedule(daily);
                uploadScheduleDateToSupabase(date, daily, scheduleKey, schedule);
                validDailySchedules.put(date, daily);
                loadedScheduleVariants.putIfAbsent(scheduleKey, schedule);
                fetchedDates.add(date);
                sourcesByDate.put(date, ArtifactSource.MARPROM_API);
                lastFetchedScheduleDateCount++;
            }

            for (LocalDate date : dates) {
                DailyScheduleCacheVao daily = validDailySchedules.get(date);
                Map<Integer, StopScheduleVao> schedule = loadedScheduleVariants.get(daily.scheduleKey());
                if (schedule == null) {
                    schedule = loadScheduleVariant(daily.scheduleKey());
                }

                dateToScheduleKey.put(daily.date(), daily.scheduleKey());
                uniqueSchedules.putIfAbsent(daily.scheduleKey(), schedule);
                activeRouteIdsByDate.put(daily.date(), daily.activeRouteIds() == null ? List.of() : daily.activeRouteIds());
            }

            deleteUnreferencedScheduleVariants(Set.copyOf(dateToScheduleKey.values()));
            deleteScheduleFilesOutsideWindow(dates);
            weeklyScheduleCache = new WeeklyScheduleCacheVao(
                    dates.stream().map(LocalDate::toString).toList(),
                    dateToScheduleKey,
                    uniqueSchedules,
                    activeRouteIdsByDate
            );
            lastMissingScheduleDates = List.copyOf(missingDates);
            lastFetchedScheduleDates = List.copyOf(fetchedDates);
            writeScheduleManifest(dates, sourcesByDate);
            logWeeklyScheduleCache(mode, downloadedDates);
        } catch (IOException | RuntimeException e) {
            log.error("Failed to refresh dated Marprom schedule cache; keeping previous valid cache: {}", e.getMessage(), e);
            fallbackToNewestCachedSchedule(dates);
        }
    }

    /**
     * Ob napaki osveževanja uporabi najnovejši veljaven lokalni schedule cache.
     *
     * @param dates datumsko okno, ki ga je treba pokriti
     */
    private void fallbackToNewestCachedSchedule(List<LocalDate> dates) {
        if (weeklyScheduleCache != null
                && weeklyScheduleCache.uniqueSchedules() != null
                && !weeklyScheduleCache.uniqueSchedules().isEmpty()) {
            return;
        }

        CachedSchedule cachedSchedule = loadNewestCachedSchedule();
        if (cachedSchedule == null) {
            log.warn("No cached Marprom schedule is available for fallback.");
            return;
        }

        Map<String, String> dateToScheduleKey = new LinkedHashMap<>();
        Map<String, Map<Integer, StopScheduleVao>> uniqueSchedules = new LinkedHashMap<>();
        Map<String, List<Integer>> activeRouteIdsByDate = new LinkedHashMap<>();
        for (LocalDate date : dates) {
            dateToScheduleKey.put(date.toString(), cachedSchedule.daily().scheduleKey());
            activeRouteIdsByDate.put(date.toString(), safeList(cachedSchedule.daily().activeRouteIds()));
        }
        uniqueSchedules.put(cachedSchedule.daily().scheduleKey(), cachedSchedule.schedule());

        weeklyScheduleCache = new WeeklyScheduleCacheVao(
                dates.stream().map(LocalDate::toString).toList(),
                dateToScheduleKey,
                uniqueSchedules,
                activeRouteIdsByDate
        );
        schedulesMap = cachedSchedule.schedule();
        log.warn("Using newest cached Marprom schedule from {} as fallback for requested dates {}.",
                cachedSchedule.daily().date(),
                dates);
    }

    private CachedSchedule loadNewestCachedSchedule() {
        if (!Files.exists(scheduleDatesDir)) {
            return null;
        }

        try (Stream<Path> files = Files.list(scheduleDatesDir)) {
            List<LocalDate> availableDates = files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .map(this::parseDateOrNull)
                    .filter(java.util.Objects::nonNull)
                    .sorted(Comparator.reverseOrder())
                    .toList();

            for (LocalDate date : availableDates) {
                CachedSchedule schedule = loadValidCachedSchedule(date);
                if (schedule != null) {
                    return schedule;
                }
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to inspect cached Marprom schedules for fallback: {}", e.getMessage());
        }

        return null;
    }

    private LocalDate parseDateOrNull(String value) {
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private boolean restoreScheduleDateFromSupabase(LocalDate date) throws IOException {
        if (artifactCacheService == null) {
            return false;
        }

        String dayPath = dayArtifactPath(date);
        if (!artifactCacheService.restoreFromSupabase(dayPath)) {
            return false;
        }

        DailyScheduleCacheVao daily = loadDailySchedule(date);
        if (!isValidDailySchedule(date, daily)) {
            return false;
        }

        return artifactCacheService.restoreFromSupabase(variantArtifactPath(daily.scheduleKey()));
    }

    private void uploadScheduleDateToSupabase(
            LocalDate date,
            DailyScheduleCacheVao daily,
            String scheduleKey,
            Map<Integer, StopScheduleVao> schedule
    ) throws IOException {
        if (artifactCacheService == null) {
            return;
        }

        artifactCacheService.upload(
                dayArtifactPath(date),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(daily),
                MediaType.APPLICATION_JSON
        );
        artifactCacheService.upload(
                variantArtifactPath(scheduleKey),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(schedule),
                MediaType.APPLICATION_JSON
        );
    }

    private void writeScheduleManifest(List<LocalDate> dates, Map<LocalDate, ArtifactSource> sourcesByDate) throws IOException {
        List<ScheduleCacheManifest.Day> days = new ArrayList<>();
        for (LocalDate date : dates) {
            Path file = dailyScheduleFile(date);
            days.add(new ScheduleCacheManifest.Day(
                    date.toString(),
                    dayArtifactPath(date),
                    Sha256.hex(Files.readAllBytes(file)),
                    sourcesByDate.getOrDefault(date, ArtifactSource.LOCAL)
            ));
        }

        ScheduleCacheManifest manifest = new ScheduleCacheManifest(
                1,
                OffsetDateTime.now(clock.withZone(ZoneId.of("Europe/Ljubljana"))).toString(),
                dates.getFirst().toString(),
                dates.getLast().toString(),
                days
        );
        writeJsonAtomically(scheduleManifestFile, manifest);
        if (artifactCacheService != null) {
            artifactCacheService.upload(
                    "marprom/schedules/manifest.json",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest),
                    MediaType.APPLICATION_JSON
            );
        }
    }

    private void writeJsonAtomically(Path target, Object value) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), value);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String dayArtifactPath(LocalDate date) {
        return "marprom/schedules/days/" + date + ".json";
    }

    private String variantArtifactPath(String scheduleKey) {
        return "marprom/schedules/variants/" + scheduleKey + ".json";
    }

    private CachedSchedule loadValidCachedSchedule(LocalDate date) {
        try {
            DailyScheduleCacheVao daily = loadDailySchedule(date);
            if (!isValidDailySchedule(date, daily)) {
                return null;
            }

            Map<Integer, StopScheduleVao> schedule = loadScheduleVariant(daily.scheduleKey());
            if (schedule == null) {
                return null;
            }

            return new CachedSchedule(daily, schedule);
        } catch (IOException | RuntimeException e) {
            log.warn("Marprom schedule cache for {} is invalid and will be regenerated: {}", date, e.getMessage());
            return null;
        }
    }

    private boolean isValidDailySchedule(LocalDate date, DailyScheduleCacheVao daily) {
        if (daily == null) {
            return false;
        }

        if (!date.toString().equals(daily.date())) {
            return false;
        }

        return daily.scheduleKey() != null
                && !daily.scheduleKey().isBlank()
                && daily.activeRouteIds() != null;
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
            if (Files.deleteIfExists(path)) {
                lastDeletedScheduleFileCount++;
            }
        } catch (IOException e) {
            log.warn("Failed to delete stale schedule cache file {}: {}", path.toAbsolutePath(), e.getMessage());
        }
    }

    private Path dailyScheduleFile(LocalDate date) {
        return scheduleDatesDir.resolve(date + ".json");
    }

    private Path scheduleVariantFile(String scheduleKey) {
        return scheduleVariantsDir.resolve(scheduleKey + ".json");
    }

    List<LocalDate> currentSevenDayWindow() {
        return dateWindow(today(), refreshDaysAhead);
    }

    static List<LocalDate> dateWindow(LocalDate today, int daysAhead) {
        if (daysAhead < 0) {
            throw new IllegalArgumentException("daysAhead must be greater than or equal to 0");
        }

        return IntStream.rangeClosed(0, daysAhead)
                .mapToObj(today::plusDays)
                .toList();
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneId.of("Europe/Ljubljana")));
    }

    private WeeklyScheduleCacheVao emptyWeeklyScheduleCache() {
        return new WeeklyScheduleCacheVao(List.of(), Map.of(), Map.of(), Map.of());
    }

    private void logWeeklyScheduleCache(String mode, List<LocalDate> downloadedDates) {
        List<String> expectedDates = currentSevenDayWindow().stream().map(LocalDate::toString).toList();
        List<String> foundDates = weeklyScheduleCache == null || weeklyScheduleCache.dates() == null
                ? List.of()
                : weeklyScheduleCache.dates();
        int uniqueVariants = weeklyScheduleCache == null ? 0 : weeklyScheduleCache.uniqueScheduleCount();
        int linesProcessed = routesMap == null
                ? 0
                : (int) routesMap.values().stream().map(RouteVao::LineId).distinct().count();

        log.info(
                "Weekly Marprom schedule cache {}: localRoot={}, supabaseEnabled={}, supabaseBucket={}, expectedDates={}, foundDates={}, downloadedFromSupabase={}, missingDates={}, generatedFromMarprom={}, deletedFiles={}, lines={}, uniqueScheduleVariants={}, file={}",
                mode,
                cacheRoot.toAbsolutePath(),
                artifactCacheService != null && artifactCacheService.supabaseStorage().enabled(),
                artifactCacheService == null ? null : artifactCacheService.supabaseStorage().bucket(),
                expectedDates,
                foundDates,
                downloadedDates,
                lastMissingScheduleDates,
                lastFetchedScheduleDates,
                lastDeletedScheduleFileCount,
                linesProcessed,
                uniqueVariants,
                scheduleDatesDir.toAbsolutePath()
        );
        log.info("Weekly Marprom schedule cache finalActiveDateRange={}..{} fetchedDateCount={}",
                foundDates.isEmpty() ? null : foundDates.getFirst(),
                foundDates.isEmpty() ? null : foundDates.getLast(),
                lastFetchedScheduleDateCount);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record CachedSchedule(
            DailyScheduleCacheVao daily,
            Map<Integer, StopScheduleVao> schedule
    ) {
    }
}
