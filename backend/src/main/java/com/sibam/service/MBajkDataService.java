package com.sibam.service;

import com.sibam.dto.mbajk.BikeStopDto;
import com.sibam.integration.mbajk.MBajkClient;
import com.sibam.persistence.BikeStation;
import com.sibam.persistence.BikeStationSnapshot;
import com.sibam.repository.BikeStationRepository;
import com.sibam.repository.BikeStationSnapshotRepository;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;

@Service
public class MBajkDataService {

    private final MBajkClient mbajkClient;
    private final BikeStationRepository bikeStationRepository;
    private final BikeStationSnapshotRepository bikeStationSnapshotRepository;

    public MBajkDataService(MBajkClient mbajkClient,
                            BikeStationRepository bikeStationRepository,
                            BikeStationSnapshotRepository bikeStationSnapshotRepository) {
        this.mbajkClient = mbajkClient;
        this.bikeStationRepository = bikeStationRepository;
        this.bikeStationSnapshotRepository = bikeStationSnapshotRepository;
    }

    /**
     * Pridobi podatke o vseh MBajk postajah in shrani trenutno stanje v bazo.
     */
    public void ingestBikesData(OffsetDateTime fetchedAt) {
        mbajkClient.getAllBikes()
                .publishOn(Schedulers.boundedElastic())
                .subscribe(bikeStops -> bikeStops.forEach(dto -> saveBikeStop(dto, fetchedAt)));
    }

    /**
     * Shrani postajo (če še ne obstaja) in doda nov posnetek trenutne razpoložljivosti.
     */
    private void saveBikeStop(BikeStopDto dto, OffsetDateTime fetchedAt) {
        BikeStation station = bikeStationRepository.findByNumber(dto.number())
                .orElseGet(() -> {
                    BikeStation newStation = new BikeStation();
                    newStation.setNumber(dto.number());
                    newStation.setName(dto.name());
                    newStation.setAddress(dto.address());
                    newStation.setLatitude(dto.position().latitude());
                    newStation.setLongitude(dto.position().longitude());
                    newStation.setCapacity(dto.totalStands().capacity());
                    return bikeStationRepository.save(newStation);
                });

        BikeStationSnapshot snapshot = new BikeStationSnapshot();
        snapshot.setStation(station);
        snapshot.setStatus(dto.status());
        snapshot.setBikes(dto.totalStands().availabilities().bikes());
        snapshot.setStands(dto.totalStands().availabilities().stands());
        snapshot.setMechanicalBikes(dto.totalStands().availabilities().mechanicalBikes());
        snapshot.setElectricalBikes(dto.totalStands().availabilities().electricalBikes());
        snapshot.setRecordedAt(fetchedAt);
        bikeStationSnapshotRepository.save(snapshot);
    }
}
