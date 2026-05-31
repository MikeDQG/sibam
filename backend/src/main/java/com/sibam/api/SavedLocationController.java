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
    public ResponseEntity<List<SavedLocation>> getLocations(
            @PathVariable UUID userId,
            @RequestAttribute("uid") String uid) {
        return ResponseEntity.ok(locationService.getLocationsForUser(userId, uid));
    }

    @PostMapping
    public ResponseEntity<SavedLocation> saveLocation(
            @RequestBody SavedLocationRequest request,
            @RequestAttribute("uid") String uid) {
        return ResponseEntity.ok(locationService.saveLocation(
                request.userId(), request.name(), request.address(), request.latitude(), request.longitude(), request.color(), request.logo(), uid
        ));
    }

    @PutMapping("/{locationId}")
    public ResponseEntity<SavedLocation> updateLocation(
            @PathVariable UUID locationId,
            @RequestBody SavedLocationRequest request,
            @RequestAttribute("uid") String uid) {
        return ResponseEntity.ok(locationService.updateLocation(
                locationId, request.name(), request.address(), request.latitude(), request.longitude(), request.color(), request.logo(), uid
        ));
    }

    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> deleteLocation(
            @PathVariable UUID locationId,
            @RequestAttribute("uid") String uid) {
        locationService.deleteLocation(locationId, uid);
        return ResponseEntity.noContent().build();
    }

}
