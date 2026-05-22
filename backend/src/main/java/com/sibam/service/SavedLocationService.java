package com.sibam.service;

import com.sibam.persistence.SavedLocation;
import com.sibam.persistence.User;
import com.sibam.repository.SavedLocationRepository;
import com.sibam.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SavedLocationService {
    private final SavedLocationRepository savedLocationRepository;
    private final UserRepository userRepository;


    public SavedLocationService(SavedLocationRepository savedLocationRepository, UserRepository userRepository) {
        this.savedLocationRepository = savedLocationRepository;
        this.userRepository = userRepository;
    }

    public List<SavedLocation> getLocationsForUser (UUID userId) {
        return savedLocationRepository.findByUserId(userId);
    }

    public SavedLocation saveLocation (UUID userId, String name, String address, Double latitude, Double longtitude) {
        User user = userRepository.findById(userId).orElseThrow();
        SavedLocation location = new SavedLocation();
        location.setUser(user);
        location.setName(name);
        location.setAddress(address);
        location.setLatitude(latitude);
        location.setLongitude(longtitude);
        location.setCreatedAt(OffsetDateTime.now());
        return savedLocationRepository.save(location);
    }

    public SavedLocation updateLocation(UUID locationId, String name, String address, Double latitude, Double longitude) {
        SavedLocation location = savedLocationRepository.findById(locationId).orElseThrow();
        location.setAddress(address);
        location.setName(name);
        location.setLongitude(longitude);
        location.setLatitude(latitude);
        return savedLocationRepository.save(location);
    }

    public void deleteLocation (UUID locationId) {
       savedLocationRepository.deleteById(locationId);
    }
}
