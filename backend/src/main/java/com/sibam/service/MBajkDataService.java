package com.sibam.service;

import com.sibam.config.FallbackProperties;
import com.sibam.dto.prediction.BikePredictionRequest;
import com.sibam.dto.mbajk.BikeStopDto;
import com.sibam.engine.vao.BikeAvailabilityVao;
import com.sibam.engine.vao.BikeStationVao;
import com.sibam.integration.mbajk.MBajkClient;
import com.sibam.persistence.BikeStation;
import com.sibam.persistence.BikeStationSnapshot;
import com.sibam.repository.BikeStationRepository;
import com.sibam.repository.BikeStationSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Servis za zajem, shranjevanje in branje MBajk postaj.
 *
 * Podatke pridobi iz MBajk API-ja, jih shrani kot postaje in časovne posnetke
 * razpoložljivosti, pri zastarelih posnetkih pa lahko uporabi ML napoved.
 */
@Service
public class MBajkDataService {

    private static final Logger log = LoggerFactory.getLogger(MBajkDataService.class);
    private static final ZoneId ROUTING_ZONE = ZoneId.of("Europe/Ljubljana");

    private final MBajkClient mbajkClient;
    private final BikeStationRepository bikeStationRepository;
    private final BikeStationSnapshotRepository bikeStationSnapshotRepository;
    private final BikePredictionService bikePredictionService;
    private final FallbackProperties fallbackProperties;
    private final Clock clock;

    @Autowired
    public MBajkDataService(MBajkClient mbajkClient,
                            BikeStationRepository bikeStationRepository,
                            BikeStationSnapshotRepository bikeStationSnapshotRepository,
                            BikePredictionService bikePredictionService,
                            FallbackProperties fallbackProperties,
                            Clock clock) {
        this.mbajkClient = mbajkClient;
        this.bikeStationRepository = bikeStationRepository;
        this.bikeStationSnapshotRepository = bikeStationSnapshotRepository;
        this.bikePredictionService = bikePredictionService;
        this.fallbackProperties = fallbackProperties;
        this.clock = clock;
    }

    MBajkDataService(MBajkClient mbajkClient,
                     BikeStationRepository bikeStationRepository,
                     BikeStationSnapshotRepository bikeStationSnapshotRepository) {
        this(
                mbajkClient,
                bikeStationRepository,
                bikeStationSnapshotRepository,
                null,
                new FallbackProperties(60),
                Clock.system(ROUTING_ZONE)
        );
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

    /**
     * Vrne MBajk postaje v VAO obliki za gradnjo grafa.
     *
     * @return seznam postaj z aktualno ali napovedano razpoložljivostjo
     */
    public List<BikeStationVao> getBikeStationVaos() {
        return bikeStationRepository.findAll().stream()
                .map(this::toVao)
                .toList();
    }

    /**
     * Pretvori JPA entiteto postaje v grafni VAO.
     *
     * @param station shranjena MBajk postaja
     * @return VAO postaje z razpoložljivostjo
     */
    private BikeStationVao toVao(BikeStation station) {
        BikeAvailabilityVao availability = resolveAvailability(station);

        return new BikeStationVao(
                station.getNumber(),
                station.getName(),
                station.getAddress(),
                station.getLatitude(),
                station.getLongitude(),
                station.getCapacity(),
                availability
        );
    }

    /**
     * Izbere svež MBajk posnetek ali fallback napoved razpoložljivosti.
     *
     * @param station postaja, za katero se išče stanje
     * @return razpoložljivost za uporabo pri BIKE robovih grafa
     */
    private BikeAvailabilityVao resolveAvailability(BikeStation station) {
        Optional<BikeStationSnapshot> latest = bikeStationSnapshotRepository.findFirstByStationOrderByRecordedAtDesc(station);
        if (latest.isPresent() && isFresh(latest.get().getRecordedAt())) {
            return toAvailability(latest.get());
        }

        BikeAvailabilityVao predicted = predictedAvailability(station);
        if (predicted != null) {
            log.info("Using predicted MBajk availability for station {} because latest snapshot is {}.",
                    station.getNumber(),
                    latest.map(BikeStationSnapshot::getRecordedAt).map(Object::toString).orElse("missing"));
            return predicted;
        }

        return latest.map(this::toAvailability)
                .orElseGet(() -> new BikeAvailabilityVao(0, 0, 0, 0, null, null));
    }

    /**
     * Pretvori shranjen posnetek postaje v VAO razpoložljivosti.
     *
     * @param snapshot zadnji posnetek MBajk postaje
     * @return VAO razpoložljivosti
     */
    private BikeAvailabilityVao toAvailability(BikeStationSnapshot snapshot) {
        return new BikeAvailabilityVao(
                snapshot.getBikes(),
                snapshot.getStands(),
                snapshot.getMechanicalBikes(),
                snapshot.getElectricalBikes(),
                snapshot.getStatus(),
                snapshot.getRecordedAt()
        );
    }

    /**
     * Izvede fallback ML napoved razpoložljivosti MBajk postaje.
     *
     * @param station postaja brez svežega posnetka
     * @return napovedana razpoložljivost ali null, če napoved ni na voljo
     */
    private BikeAvailabilityVao predictedAvailability(BikeStation station) {
        if (bikePredictionService == null) {
            return null;
        }

        try {
            LocalDateTime now = LocalDateTime.now(clock.withZone(ROUTING_ZONE));
            int dayOfWeek = now.getDayOfWeek().getValue();
            var prediction = bikePredictionService.predict(new BikePredictionRequest(
                    station.getNumber(),
                    now.getHour(),
                    dayOfWeek,
                    dayOfWeek >= 6 ? 1 : 0,
                    15f,
                    0f,
                    0f
            ));
            int freeBikes = Math.max(0, prediction.predictedBikes());
            int freeStands = Math.max(0, prediction.predictedStands());
            return new BikeAvailabilityVao(freeBikes, freeStands, freeBikes, 0, "PREDICTED", OffsetDateTime.now(clock));
        } catch (Exception e) {
            log.warn("MBajk prediction unavailable for station {}; using latest stored or zero availability: {}",
                    station.getNumber(), e.getMessage());
            return null;
        }
    }

    /**
     * Preveri, ali je posnetek znotraj dovoljenega realtime okna.
     *
     * @param recordedAt čas shranjenega posnetka
     * @return true, če se posnetek lahko uporabi kot svež podatek
     */
    private boolean isFresh(OffsetDateTime recordedAt) {
        if (recordedAt == null) {
            return false;
        }

        OffsetDateTime oldestFresh = OffsetDateTime.now(clock).minus(fallbackProperties.realtimeMaxAge());
        return !recordedAt.isBefore(oldestFresh);
    }
}
