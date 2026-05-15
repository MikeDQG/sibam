package com.sibam.api;

import com.sibam.dto.location.SavedLocationRequest;
import com.sibam.persistence.SavedLocation;
import com.sibam.service.SavedLocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/locations")
public class SavedLocationController {
    private final SavedLocationService locationService;

    public SavedLocationController(SavedLocationService locationService){
        this.locationService = locationService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<SavedLocation>> getLocations(@PathVariable UUID userId) {
        return ResponseEntity.ok(locationService.getLocationsForUser(userId));
    }

    @PostMapping
    public ResponseEntity<SavedLocation> saveLocation(@RequestBody SavedLocationRequest request) {
        return ResponseEntity.ok(locationService.saveLocation(
                request.userId(), request.name(), request.address(), request.latitude(), request.longitude()
        ));
    }
    
    @PutMapping("/{locationId}")
    public ResponseEntity<SavedLocation> updateLocation(@PathVariable UUID locationId, @RequestBody SavedLocationRequest request) {
        return ResponseEntity.ok(locationService.updateLocation(
                locationId, request.name(), request.address(), request.latitude(), request.longitude()
        ));
    }

    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> deleteLocation(@PathVariable UUID locationId) {
        locationService.deleteLocation(locationId);
        return ResponseEntity.noContent().build();
    }

}
