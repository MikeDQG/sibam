package com.sibam.repository;

import com.sibam.persistence.StopDelayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface StopDelaySnapshotRepository extends JpaRepository<StopDelayEntity, UUID> {
}
