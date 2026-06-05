package com.sibam.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibam.engine.vao.BusStopVao;
import com.sibam.engine.vao.DailyScheduleCacheVao;
import com.sibam.engine.vao.LineScheduleVao;
import com.sibam.engine.vao.RouteVao;
import com.sibam.engine.vao.RouteScheduleVao;
import com.sibam.engine.vao.StopScheduleVao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VaoSerializerTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-06-03");
    private static final ZoneId ZONE = ZoneId.of("Europe/Ljubljana");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void dateWindowReturnsTodayThroughSixDaysAhead() {
        List<LocalDate> dates = VaoSerializer.dateWindow(TODAY, 6);

        assertThat(dates).hasSize(7);
        assertThat(dates.getFirst()).isEqualTo(TODAY);
        assertThat(dates.getLast()).isEqualTo(TODAY.plusDays(6));
    }

    @Test
    void refreshGeneratesMissingFinalDateAndKeepsExistingDates() throws Exception {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);

        for (int i = 0; i < 6; i++) {
            LocalDate date = TODAY.plusDays(i);
            writeCachedSchedule(tempDir, date, "existing-" + date, schedule(10 + i), List.of(10 + i));
        }

        Map<Integer, StopScheduleVao> finalSchedule = schedule(99);
        when(mapper.mapSchedules(TODAY.plusDays(6))).thenReturn(finalSchedule);
        when(mapper.hashSchedule(finalSchedule)).thenReturn("generated-final");
        when(mapper.mapActiveRouteIds(TODAY.plusDays(6))).thenReturn(List.of(99));

        serializer.refreshWeeklyScheduleCache();

        assertThat(serializer.getScheduleDates())
                .containsExactly(
                        TODAY,
                        TODAY.plusDays(1),
                        TODAY.plusDays(2),
                        TODAY.plusDays(3),
                        TODAY.plusDays(4),
                        TODAY.plusDays(5),
                        TODAY.plusDays(6)
                );
        assertThat(Files.exists(scheduleFile(tempDir, TODAY.plusDays(6)))).isTrue();
        verify(mapper).mapSchedules(TODAY.plusDays(6));
        verify(mapper, never()).mapSchedules(TODAY);
    }

    @Test
    void refreshRemovesYesterdayFromActiveCache() throws Exception {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);
        ReflectionTestUtils.setField(serializer, "refreshDaysAhead", 0);

        writeCachedSchedule(tempDir, TODAY.minusDays(1), "yesterday", schedule(1), List.of(1));
        writeCachedSchedule(tempDir, TODAY, "today", schedule(2), List.of(2));

        serializer.refreshWeeklyScheduleCache();

        assertThat(Files.exists(scheduleFile(tempDir, TODAY.minusDays(1)))).isFalse();
        assertThat(Files.exists(scheduleFile(tempDir, TODAY))).isTrue();
        assertThat(serializer.getScheduleDates()).containsExactly(TODAY);
    }

    @Test
    void refreshDeduplicatesIdenticalGeneratedSchedulePayloads() throws Exception {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);
        ReflectionTestUtils.setField(serializer, "refreshDaysAhead", 1);

        Map<Integer, StopScheduleVao> schedule = schedule(42);
        when(mapper.mapSchedules(TODAY)).thenReturn(schedule);
        when(mapper.mapSchedules(TODAY.plusDays(1))).thenReturn(schedule);
        when(mapper.hashSchedule(schedule)).thenReturn("same-schedule");
        when(mapper.mapActiveRouteIds(TODAY)).thenReturn(List.of(42));
        when(mapper.mapActiveRouteIds(TODAY.plusDays(1))).thenReturn(List.of(42));

        serializer.refreshWeeklyScheduleCache();

        try (var files = Files.list(tempDir.resolve("marprom").resolve("schedules").resolve("variants"))) {
            assertThat(files.filter(Files::isRegularFile).toList()).hasSize(1);
        }
        assertThat(serializer.getWeeklyScheduleCache().uniqueScheduleCount()).isEqualTo(1);
    }

    private VaoSerializer serializer(MarpromDtoToVaoMapper mapper, Path cacheDir) {
        Clock clock = Clock.fixed(Instant.parse("2026-06-03T10:00:00Z"), ZONE);
        VaoSerializer serializer = new VaoSerializer(mapper, clock, cacheDir);
        ReflectionTestUtils.setField(serializer, "refreshDaysAhead", 6);
        return serializer;
    }

    private void writeCachedSchedule(
            Path cacheDir,
            LocalDate date,
            String scheduleKey,
            Map<Integer, StopScheduleVao> schedule,
            List<Integer> activeRouteIds
    ) throws Exception {
        Files.createDirectories(cacheDir.resolve("marprom").resolve("schedules").resolve("days"));
        Files.createDirectories(cacheDir.resolve("marprom").resolve("schedules").resolve("variants"));
        OBJECT_MAPPER.writeValue(scheduleFile(cacheDir, date).toFile(),
                new DailyScheduleCacheVao(date.toString(), scheduleKey, activeRouteIds));
        OBJECT_MAPPER.writeValue(cacheDir.resolve("marprom").resolve("schedules").resolve("variants").resolve(scheduleKey + ".json").toFile(), schedule);
    }

    private static Path scheduleFile(Path cacheDir, LocalDate date) {
        return cacheDir.resolve("marprom").resolve("schedules").resolve("days").resolve(date + ".json");
    }

    @Test
    void getSchedulesMapForDateReturnsVariantMatchingDateKey() throws Exception {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);

        Map<Integer, StopScheduleVao> mondaySchedule = schedule(10);
        Map<Integer, StopScheduleVao> tuesdaySchedule = schedule(20);
        writeCachedSchedule(tempDir, TODAY, "mon-key", mondaySchedule, List.of(10));
        writeCachedSchedule(tempDir, TODAY.plusDays(1), "tue-key", tuesdaySchedule, List.of(20));

        for (int i = 2; i <= 6; i++) {
            writeCachedSchedule(tempDir, TODAY.plusDays(i), "mon-key", mondaySchedule, List.of(10));
        }

        serializer.refreshWeeklyScheduleCache();

        assertThat(serializer.getSchedulesMap(TODAY)).isEqualTo(mondaySchedule);
        assertThat(serializer.getSchedulesMap(TODAY.plusDays(1))).isEqualTo(tuesdaySchedule);
    }

    @Test
    void getSchedulesMapForDateFallsBackToCurrentScheduleWhenKeyMissing() throws Exception {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);

        Map<Integer, StopScheduleVao> s = schedule(5);
        when(mapper.mapSchedules(any(LocalDate.class))).thenReturn(s);
        when(mapper.hashSchedule(s)).thenReturn("k");
        when(mapper.mapActiveRouteIds(any(LocalDate.class))).thenReturn(List.of(5));

        serializer.refreshWeeklyScheduleCache();

        LocalDate farFuture = TODAY.plusDays(100);
        assertThat(serializer.getSchedulesMap(farFuture)).isNotNull();
    }

    @Test
    void isRouteActiveOnDateReturnsTrueWhenRouteIsInActiveList() {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);

        Map<Integer, StopScheduleVao> s = schedule(1);
        when(mapper.mapSchedules(any(LocalDate.class))).thenReturn(s);
        when(mapper.hashSchedule(s)).thenReturn("k");
        when(mapper.mapActiveRouteIds(any(LocalDate.class))).thenReturn(List.of(42, 99));

        serializer.refreshWeeklyScheduleCache();

        assertThat(serializer.isRouteActiveOnDate(42, TODAY)).isTrue();
        assertThat(serializer.isRouteActiveOnDate(99, TODAY)).isTrue();
    }

    @Test
    void isRouteActiveOnDateReturnsFalseWhenRouteNotInActiveList() {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);

        Map<Integer, StopScheduleVao> s = schedule(1);
        when(mapper.mapSchedules(any(LocalDate.class))).thenReturn(s);
        when(mapper.hashSchedule(s)).thenReturn("k");
        when(mapper.mapActiveRouteIds(any(LocalDate.class))).thenReturn(List.of(42));

        serializer.refreshWeeklyScheduleCache();

        assertThat(serializer.isRouteActiveOnDate(7, TODAY)).isFalse();
    }

    @Test
    void isRouteActiveOnDateReturnsTrueWhenNoCacheIsPresent() {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);
        ReflectionTestUtils.setField(serializer, "refreshDaysAhead", 0);

        Map<Integer, StopScheduleVao> s = schedule(1);
        when(mapper.mapSchedules(any(LocalDate.class))).thenReturn(s);
        when(mapper.hashSchedule(s)).thenReturn("k");
        when(mapper.mapActiveRouteIds(any(LocalDate.class))).thenReturn(null);

        serializer.refreshWeeklyScheduleCache();

        assertThat(serializer.isRouteActiveOnDate(999, TODAY)).isTrue();
    }

    @Test
    void getScheduleDatesReturnsSortedDates() throws Exception {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);

        for (int i = 0; i <= 6; i++) {
            LocalDate date = TODAY.plusDays(i);
            writeCachedSchedule(tempDir, date, "key-" + date, schedule(i), List.of(i));
        }

        serializer.refreshWeeklyScheduleCache();

        List<LocalDate> dates = serializer.getScheduleDates();
        assertThat(dates).hasSize(7).isSorted();
        assertThat(dates.getFirst()).isEqualTo(TODAY);
    }

    @Test
    void getScheduleDatesReturnsEmptyWhenCacheIsEmpty() {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);

        assertThat(serializer.getScheduleDates()).isEmpty();
    }

    @Test
    void dateWindowThrowsForNegativeDaysAhead() {
        assertThatThrownBy(() -> VaoSerializer.dateWindow(TODAY, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dateWindowForZeroDaysAheadReturnsOnlyToday() {
        List<LocalDate> dates = VaoSerializer.dateWindow(TODAY, 0);
        assertThat(dates).containsExactly(TODAY);
    }

    @Test
    void refreshWeeklyScheduleNightlyDoesNothingWhenDisabled() {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);
        ReflectionTestUtils.setField(serializer, "scheduledRefreshEnabled", false);

        serializer.refreshWeeklyScheduleCacheNightly();

        verify(mapper, never()).mapSchedules(any(LocalDate.class));
    }

    @Test
    void saveToDiskWritesStaticAndTodayScheduleSnapshots() {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);
        ReflectionTestUtils.setField(serializer, "busStopsMap", Map.of(
                1, new BusStopVao(1, "Stop", "Address", 46.55, 15.64)
        ));
        ReflectionTestUtils.setField(serializer, "routesMap", Map.of(
                10, new RouteVao(10, 1, "1", "Center", "Center", List.of(), List.of())
        ));
        ReflectionTestUtils.setField(serializer, "schedulesMap", schedule(7));

        serializer.saveToDisk();

        Path vaoDir = tempDir.resolve("marprom").resolve("vao");
        assertThat(vaoDir.resolve("busStops.json")).exists();
        assertThat(vaoDir.resolve("routes.json")).exists();
        assertThat(vaoDir.resolve("schedules.json")).exists();
        assertThat(serializer.cacheExists()).isFalse();
    }

    @Test
    void loadFromDiskLoadsStaticCacheAndScheduleForToday() throws Exception {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer writer = serializer(mapper, tempDir);
        BusStopVao stop = new BusStopVao(1, "Stop", "Address", 46.55, 15.64);
        RouteVao route = new RouteVao(10, 1, "1", "Center", "Center", List.of(), List.of(stop));
        Map<Integer, StopScheduleVao> todaySchedule = schedule(44);

        ReflectionTestUtils.setField(writer, "busStopsMap", Map.of(1, stop));
        ReflectionTestUtils.setField(writer, "routesMap", Map.of(10, route));
        writer.saveToDisk();
        writeCachedSchedule(tempDir, TODAY, "today-key", todaySchedule, List.of(10));

        VaoSerializer reader = serializer(mapper, tempDir);
        ReflectionTestUtils.setField(reader, "refreshDaysAhead", 0);

        assertThat(reader.loadFromDisk()).isTrue();
        assertThat(reader.getBusStopsMap()).containsEntry(1, stop);
        assertThat(reader.getRoutesMap()).containsEntry(10, route);
        assertThat(reader.getSchedulesMap()).isEqualTo(todaySchedule);
        verify(mapper, never()).mapBusStops();
        verify(mapper, never()).mapRoutes();
        verify(mapper, never()).mapSchedules(any(LocalDate.class));
    }

    @Test
    void loadFromDiskReturnsFalseWhenStaticCacheIsMissing() {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);

        assertThat(serializer.loadFromDisk()).isFalse();
        assertThat(serializer.cacheExists()).isFalse();
    }

    @Test
    void refreshWeeklyScheduleCacheFallsBackToNewestCachedScheduleWhenGenerationFails() throws Exception {
        MarpromDtoToVaoMapper mapper = mock(MarpromDtoToVaoMapper.class);
        VaoSerializer serializer = serializer(mapper, tempDir);
        ReflectionTestUtils.setField(serializer, "refreshDaysAhead", 0);

        Map<Integer, StopScheduleVao> cachedSchedule = schedule(77);
        writeCachedSchedule(tempDir, TODAY.minusDays(2), "old-key", cachedSchedule, List.of(123));
        when(mapper.mapSchedules(TODAY)).thenThrow(new IllegalStateException("Marprom unavailable"));

        serializer.refreshWeeklyScheduleCache();

        assertThat(serializer.getSchedulesMap()).isEqualTo(cachedSchedule);
        assertThat(serializer.getSchedulesMap(TODAY)).isEqualTo(cachedSchedule);
        assertThat(serializer.isRouteActiveOnDate(123, TODAY)).isTrue();
        assertThat(serializer.isRouteActiveOnDate(999, TODAY)).isFalse();
    }

    private Map<Integer, StopScheduleVao> schedule(int stopId) {
        return Map.of(stopId, new StopScheduleVao(
                stopId,
                "Stop " + stopId,
                "Address " + stopId,
                List.of(new LineScheduleVao(
                        stopId,
                        List.of(new RouteScheduleVao("A->B", List.of("08:00")))
                ))
        ));
    }
}
