package com.sibam.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibam.engine.vao.DailyScheduleCacheVao;
import com.sibam.engine.vao.LineScheduleVao;
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
