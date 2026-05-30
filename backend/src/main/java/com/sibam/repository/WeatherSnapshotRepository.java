package com.sibam.repository;

import com.sibam.persistence.WeatherSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface WeatherSnapshotRepository extends JpaRepository<WeatherSnapshot, UUID> {
}
