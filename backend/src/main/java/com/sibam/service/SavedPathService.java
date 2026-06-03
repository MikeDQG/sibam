package com.sibam.service;

import com.sibam.graph.model.output.Journey;
import com.sibam.persistence.SavedPath;
import com.sibam.persistence.User;
import com.sibam.repository.SavedPathRepository;
import com.sibam.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SavedPathService {
    private final SavedPathRepository savedPathRepository;
    private final UserRepository userRepository;

    public SavedPathService(SavedPathRepository savedPathRepository, UserRepository userRepository) {
        this.savedPathRepository = savedPathRepository;
        this.userRepository = userRepository;
    }

    public List<SavedPath> getPathsForUser(UUID userId, String uid) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!user.getFirebaseUid().equals(uid)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return savedPathRepository.findByUserId(userId);
    }

    public SavedPath savePath(UUID userId, String name, Journey journey, String uid) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!user.getFirebaseUid().equals(uid)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        SavedPath path = new SavedPath();
        path.setUser(user);
        path.setName(name);
        path.setJourney(journey);
        path.setCreatedAt(OffsetDateTime.now());
        return savedPathRepository.save(path);
    }

    public void deletePath(UUID pathId, String uid) {
        SavedPath path = savedPathRepository.findById(pathId).orElseThrow();
        if (!path.getUser().getFirebaseUid().equals(uid)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        savedPathRepository.deleteById(pathId);
    }
}
