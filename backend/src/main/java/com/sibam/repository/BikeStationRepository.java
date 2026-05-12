package com.sibam.repository;

import com.sibam.persistence.BikeStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BikeStationRepository extends JpaRepository<BikeStation, UUID> {
    Optional<BikeStation> findByNumber(int number);
}
