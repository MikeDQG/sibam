package com.sibam.repository;

import com.sibam.persistence.SavedLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SavedLocationRepository extends JpaRepository<SavedLocation, String> {
    List<SavedLocation> findByUserId(String userId);
}