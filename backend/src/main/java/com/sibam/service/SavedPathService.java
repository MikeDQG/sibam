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

/**
 * Servis za poslovna pravila shranjenih poti.
 *
 * Hrani izračunane Journey objekte in pred dostopom preveri ujemanje internega
 * uporabnika s Firebase uid iz zahtevka.
 */
@Service
public class SavedPathService {
    private final SavedPathRepository savedPathRepository;
    private final UserRepository userRepository;

    public SavedPathService(SavedPathRepository savedPathRepository, UserRepository userRepository) {
        this.savedPathRepository = savedPathRepository;
        this.userRepository = userRepository;
    }

    /**
     * Vrne shranjene poti uporabnika po preverjanju lastništva.
     *
     * @param userId interni ID uporabnika
     * @param uid Firebase uid iz zahtevka
     * @return seznam shranjenih poti
     */
    public List<SavedPath> getPathsForUser(UUID userId, String uid) {
        User user = findUserOrThrow(userId);
        if (!user.getFirebaseUid().equals(uid)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return savedPathRepository.findByUserId(userId);
    }

    /**
     * Shrani poimenovano pot z že izračunanim Journey objektom.
     *
     * @param userId interni ID uporabnika
     * @param name ime poti
     * @param journey izračunana pot iz routing odgovora
     * @param uid Firebase uid iz zahtevka
     * @return shranjena pot
     */
    public SavedPath savePath(UUID userId, String name, Journey journey, String uid) {
        User user = findUserOrThrow(userId);
        if (!user.getFirebaseUid().equals(uid)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        SavedPath path = new SavedPath();
        path.setUser(user);
        path.setName(name);
        path.setJourney(journey);
        path.setCreatedAt(OffsetDateTime.now());
        return savedPathRepository.save(path);
    }

    /**
     * Izbriše shranjeno pot po preverjanju lastništva.
     *
     * @param pathId ID poti za brisanje
     * @param uid Firebase uid iz zahtevka
     */
    public void deletePath(UUID pathId, String uid) {
        SavedPath path = findPathOrThrow(pathId);
        if (!path.getUser().getFirebaseUid().equals(uid)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        savedPathRepository.deleteById(pathId);
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private SavedPath findPathOrThrow(UUID pathId) {
        return savedPathRepository.findById(pathId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Path not found"));
    }
}
