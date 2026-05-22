package com.sibam.repository;

import com.sibam.persistence.SavedLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SavedLocationRepository extends JpaRepository<SavedLocation, UUID> {
    List<SavedLocation> findByUserId(UUID userId);
}