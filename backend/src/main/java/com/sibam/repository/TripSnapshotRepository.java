package com.sibam.repository;

import com.sibam.persistence.TripEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TripSnapshotRepository extends JpaRepository<TripEntity, UUID> {
}
