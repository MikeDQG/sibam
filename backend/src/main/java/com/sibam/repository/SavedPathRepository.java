package com.sibam.repository;

import com.sibam.persistence.SavedPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SavedPathRepository extends JpaRepository<SavedPath, UUID> {
    List<SavedPath> findByUserId(UUID userId);
}