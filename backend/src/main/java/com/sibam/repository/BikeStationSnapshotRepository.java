package com.sibam.repository;

import com.sibam.persistence.BikeStation;
import com.sibam.persistence.BikeStationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BikeStationSnapshotRepository extends JpaRepository<BikeStationSnapshot, UUID> {
    List<BikeStationSnapshot> findByStationOrderByRecordedAtDesc(BikeStation station);
}
