package com.sibam.service;

import com.sibam.persistence.SavedLocation;
import com.sibam.persistence.User;
import com.sibam.repository.SavedLocationRepository;
import com.sibam.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servis za poslovna pravila shranjenih lokacij.
 *
 * Pred branjem ali spremembo podatkov preveri, da Firebase uid iz zahtevka
 * pripada uporabniku, na katerega se zapis nanaša.
 */
@Service
public class SavedLocationService {
    private final SavedLocationRepository savedLocationRepository;
    private final UserRepository userRepository;


    public SavedLocationService(SavedLocationRepository savedLocationRepository, UserRepository userRepository) {
        this.savedLocationRepository = savedLocationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Vrne lokacije uporabnika po preverjanju lastništva.
     *
     * @param userId interni ID uporabnika
     * @param uid Firebase uid iz zahtevka
     * @return seznam shranjenih lokacij
     */
    public List<SavedLocation> getLocationsForUser(UUID userId, String uid) {
        User user = findUserOrThrow(userId);
        if (!user.getFirebaseUid().equals(uid)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return savedLocationRepository.findByUserId(userId);
    }

    /**
     * Ustvari novo shranjeno lokacijo za uporabnika.
     *
     * @return shranjena JPA entiteta lokacije
     */
    public SavedLocation saveLocation(UUID userId, String name, String address, Double latitude, Double longitude, String color, String logo, String uid) {
        User user = findUserOrThrow(userId);
        if (!user.getFirebaseUid().equals(uid)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        SavedLocation location = new SavedLocation();
        location.setUser(user);
        location.setName(name);
        location.setAddress(address);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setColor(color);
        location.setLogo(logo);
        location.setCreatedAt(OffsetDateTime.now());
        return savedLocationRepository.save(location);
    }

    /**
     * Posodobi shranjeno lokacijo, če pripada trenutnemu Firebase uporabniku.
     *
     * @return posodobljena JPA entiteta lokacije
     */
    public SavedLocation updateLocation(UUID locationId, String name, String address, Double latitude, Double longitude, String color, String logo, String uid) {
        SavedLocation location = findLocationOrThrow(locationId);
        if (!location.getUser().getFirebaseUid().equals(uid)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        location.setAddress(address);
        location.setName(name);
        location.setLongitude(longitude);
        location.setLatitude(latitude);
        location.setColor(color);
        location.setLogo(logo);
        return savedLocationRepository.save(location);
    }

    /**
     * Izbriše shranjeno lokacijo po preverjanju lastništva.
     *
     * @param locationId ID lokacije za brisanje
     * @param uid Firebase uid iz zahtevka
     */
    public void deleteLocation(UUID locationId, String uid) {
        SavedLocation location = findLocationOrThrow(locationId);
        if (!location.getUser().getFirebaseUid().equals(uid)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        savedLocationRepository.deleteById(locationId);
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private SavedLocation findLocationOrThrow(UUID locationId) {
        return savedLocationRepository.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
    }
}
