package com.sibam.service;

import com.sibam.engine.vao.BikeStationVao;
import com.sibam.integration.mbajk.MBajkClient;
import com.sibam.persistence.BikeStation;
import com.sibam.persistence.BikeStationSnapshot;
import com.sibam.repository.BikeStationRepository;
import com.sibam.repository.BikeStationSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MBajkDataServiceTest {
    private BikeStationRepository bikeStationRepository;
    private BikeStationSnapshotRepository bikeStationSnapshotRepository;
    private MBajkDataService service;

    @BeforeEach
    void setUp() {
        MBajkClient mbajkClient = mock(MBajkClient.class);
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

}
