package com.sibam.service;

import com.sibam.dto.mbajk.AvailabilitiesDto;
import com.sibam.dto.mbajk.BikeStopDto;
import com.sibam.dto.mbajk.PositionDto;
import com.sibam.dto.mbajk.TotalStandsDto;
import com.sibam.engine.vao.BikeStationVao;
import com.sibam.integration.mbajk.MBajkClient;
import com.sibam.persistence.BikeStation;
import com.sibam.persistence.BikeStationSnapshot;
import com.sibam.repository.BikeStationRepository;
import com.sibam.repository.BikeStationSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MBajkDataServiceTest {
    private MBajkClient mbajkClient;
    private BikeStationRepository bikeStationRepository;
    private BikeStationSnapshotRepository bikeStationSnapshotRepository;
    private MBajkDataService service;

    @BeforeEach
    void setUp() {
        mbajkClient = mock(MBajkClient.class);
        bikeStationRepository = mock(BikeStationRepository.class);
        bikeStationSnapshotRepository = mock(BikeStationSnapshotRepository.class);
        service = new MBajkDataService(mbajkClient, bikeStationRepository, bikeStationSnapshotRepository);
    }

    @Test
    void getBikeStationVaosReturnsStationWithLatestSnapshot() {
        BikeStation station = new BikeStation();
        station.setNumber(15);
        station.setName("Postaja A");
        station.setAddress("Partizanska 1");
        station.setLatitude(46.556);
        station.setLongitude(15.646);
        station.setCapacity(12);

        BikeStationSnapshot snapshot = new BikeStationSnapshot();
        snapshot.setBikes(5);
        snapshot.setStands(7);
        snapshot.setMechanicalBikes(3);
        snapshot.setElectricalBikes(2);
        snapshot.setStatus("OPEN");
        snapshot.setRecordedAt(OffsetDateTime.now());

        when(bikeStationRepository.findAll()).thenReturn(List.of(station));
        when(bikeStationSnapshotRepository.findFirstByStationOrderByRecordedAtDesc(station))
                .thenReturn(Optional.of(snapshot));

        List<BikeStationVao> result = service.getBikeStationVaos();

        assertThat(result).hasSize(1);
        BikeStationVao vao = result.get(0);
        assertThat(vao.number()).isEqualTo(15);
        assertThat(vao.name()).isEqualTo("Postaja A");
        assertThat(vao.capacity()).isEqualTo(12);
        assertThat(vao.availability().freeBikes()).isEqualTo(5);
        assertThat(vao.availability().freeStands()).isEqualTo(7);
        assertThat(vao.availability().status()).isEqualTo("OPEN");
    }

    @Test
    void getBikeStationVaosReturnsZeroAvailabilityWhenNoSnapshot() {
        BikeStation station = new BikeStation();
        station.setNumber(3);
        station.setName("Postaja B");
        station.setAddress("Koroška cesta 1");
        station.setLatitude(46.560);
        station.setLongitude(15.640);
        station.setCapacity(8);

        when(bikeStationRepository.findAll()).thenReturn(List.of(station));
        when(bikeStationSnapshotRepository.findFirstByStationOrderByRecordedAtDesc(station))
                .thenReturn(Optional.empty());

        List<BikeStationVao> result = service.getBikeStationVaos();

        assertThat(result).hasSize(1);
        BikeStationVao vao = result.get(0);
        assertThat(vao.availability().freeBikes()).isEqualTo(0);
        assertThat(vao.availability().freeStands()).isEqualTo(0);
        assertThat(vao.availability().status()).isNull();
        assertThat(vao.availability().recordedAt()).isNull();
    }

    @Test
    void getBikeStationVaosReturnsEmptyListWhenNoStations() {
        when(bikeStationRepository.findAll()).thenReturn(List.of());

        List<BikeStationVao> result = service.getBikeStationVaos();

        assertThat(result).isEmpty();
    }

    // --- ingestBikesData ---

    private BikeStopDto bikeStopDto(int number) {
        return new BikeStopDto(
                number, "Station " + number, "Street " + number,
                new PositionDto(46.55, 15.64),
                "OPEN", null,
                new TotalStandsDto(new AvailabilitiesDto(5, 7, 3, 2), 12)
        );
    }

    @Test
    void ingestBikesDataCreatesNewStationAndSnapshot() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        BikeStation saved = new BikeStation();
        saved.setNumber(15);

        when(bikeStationRepository.findByNumber(15)).thenReturn(Optional.empty());
        when(bikeStationRepository.save(any(BikeStation.class))).thenReturn(saved);
        when(bikeStationSnapshotRepository.save(any(BikeStationSnapshot.class)))
                .thenAnswer(inv -> { latch.countDown(); return inv.getArgument(0); });
        when(mbajkClient.getAllBikes()).thenReturn(Mono.just(List.of(bikeStopDto(15))));

        service.ingestBikesData(OffsetDateTime.now());
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();

        verify(bikeStationRepository).save(any(BikeStation.class));
        verify(bikeStationSnapshotRepository).save(any(BikeStationSnapshot.class));
    }

    @Test
    void ingestBikesDataReusesExistingStation() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        BikeStation existing = new BikeStation();
        existing.setNumber(15);

        when(bikeStationRepository.findByNumber(15)).thenReturn(Optional.of(existing));
        when(bikeStationSnapshotRepository.save(any(BikeStationSnapshot.class)))
                .thenAnswer(inv -> { latch.countDown(); return inv.getArgument(0); });
        when(mbajkClient.getAllBikes()).thenReturn(Mono.just(List.of(bikeStopDto(15))));

        service.ingestBikesData(OffsetDateTime.now());
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();

        verify(bikeStationRepository, never()).save(any(BikeStation.class));
        verify(bikeStationSnapshotRepository).save(any(BikeStationSnapshot.class));
    }
}
