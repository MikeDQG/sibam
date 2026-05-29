package com.sibam.it;
import com.sibam.dto.mbajk.AvailabilitiesDto;
import com.sibam.dto.mbajk.BikeStopDto;
import com.sibam.dto.mbajk.PositionDto;
import com.sibam.dto.mbajk.TotalStandsDto;
import com.sibam.engine.vao.BikeStationVao;
import com.sibam.persistence.BikeStation;
import com.sibam.persistence.BikeStationSnapshot;
import com.sibam.repository.BikeStationRepository;
import com.sibam.repository.BikeStationSnapshotRepository;
import com.sibam.service.MBajkDataService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MBajkDataServiceIT extends AbstractDatabaseIT {
    @Autowired
    MBajkDataService mbajkDataService;

    @Autowired
    BikeStationRepository bikeStationRepository;

    @Autowired
    BikeStationSnapshotRepository bikeStationSnapshotRepository;

    @AfterEach
    void cleanup() {
        bikeStationSnapshotRepository.deleteAll();
        bikeStationRepository.deleteAll();
    }

    private BikeStopDto makeDto(int number) {
        return new BikeStopDto(
                number, "Station " + number, "Address " + number,
                new PositionDto(46.55, 15.64),
                "OPEN",
                OffsetDateTime.now(),
                new TotalStandsDto(new AvailabilitiesDto(5, 10, 5, 0), 15)

        );
    }

    @Test
    void ingestBikesDataCreatesStationAndSnapshot() {
        when(mbajkClient.getAllBikes()).thenReturn(Mono.just(List.of(makeDto(101))));

        mbajkDataService.ingestBikesData(OffsetDateTime.now());

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> bikeStationRepository.findByNumber(101).isPresent());

        BikeStation station = bikeStationRepository.findByNumber(101).orElseThrow();
        assertThat(station.getName()).isEqualTo("Station 101");
        assertThat(station.getCapacity()).isEqualTo(15);

        List<BikeStationSnapshot> snapshots = bikeStationSnapshotRepository.findAll();
        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).getBikes()).isEqualTo(5);
        assertThat(snapshots.get(0).getStands()).isEqualTo(10);
    }

    @Test
    void ingestBikesDataDoesNotDuplicateStation() {
        when(mbajkClient.getAllBikes()).thenReturn(Mono.just(List.of(makeDto(102))));

        mbajkDataService.ingestBikesData(OffsetDateTime.now());
        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> bikeStationSnapshotRepository.findAll().size() >= 1);

        mbajkDataService.ingestBikesData(OffsetDateTime.now());
        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> bikeStationSnapshotRepository.findAll().size() >= 2);

        long stationCount = bikeStationRepository.findAll().stream()
                .filter(s -> s.getNumber() == 102).count();
        assertThat(stationCount).isEqualTo(1);
    }

    @Test
    void getBikeStationVaosReturnsLatestSnapshot() {
        BikeStation station = new BikeStation();
        station.setNumber(201);
        station.setName("Test Station");
        station.setAddress("Test Address");
        station.setLatitude(46.55);
        station.setLongitude(15.64);
        station.setCapacity(20);
        station = bikeStationRepository.save(station);

        BikeStationSnapshot older = new BikeStationSnapshot();
        older.setStation(station); older.setStatus("OPEN");
        older.setBikes(3); older.setStands(17);
        older.setMechanicalBikes(2); older.setElectricalBikes(1);
        older.setRecordedAt(OffsetDateTime.now().minusMinutes(10));
        bikeStationSnapshotRepository.save(older);

        BikeStationSnapshot newer = new BikeStationSnapshot();
        newer.setStation(station); newer.setStatus("OPEN");
        newer.setBikes(8); newer.setStands(12);
        newer.setMechanicalBikes(6); newer.setElectricalBikes(2);
        newer.setRecordedAt(OffsetDateTime.now());
        bikeStationSnapshotRepository.save(newer);

        List<BikeStationVao> vaos = mbajkDataService.getBikeStationVaos();

        BikeStationVao vao = vaos.stream()
                .filter(v -> v.number() == 201).findFirst().orElseThrow();
        assertThat(vao.availability().freeBikes()).isEqualTo(8);
    }
}
