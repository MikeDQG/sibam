package com.sibam.api;

import com.sibam.dto.location.SavedLocationRequest;
import com.sibam.persistence.SavedLocation;
import com.sibam.service.SavedLocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller za shranjene uporabniške lokacije.
 *
 * Vsi endpointi uporabljajo Firebase uid iz avtentikacijskega filtra, da servis
 * preveri lastništvo uporabniških podatkov.
 */
@RestController
@RequestMapping("/api/locations")
public class SavedLocationController {
    private final SavedLocationService locationService;

    public SavedLocationController(SavedLocationService locationService){
        this.locationService = locationService;
    }

    /**
     * Vrne vse shranjene lokacije izbranega uporabnika.
     *
     * @param userId interni ID uporabnika
     * @param uid Firebase uid iz request atributa
     * @return seznam shranjenih lokacij
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<SavedLocation>> getLocations(
            @PathVariable UUID userId,
            @RequestAttribute("uid") String uid) {
        return ResponseEntity.ok(locationService.getLocationsForUser(userId, uid));
    }

    /**
     * Ustvari novo shranjeno lokacijo za uporabnika.
     *
     * @param request podatki lokacije iz zahtevka
     * @param uid Firebase uid iz request atributa
     * @return shranjena lokacija
     */
    @PostMapping
    public ResponseEntity<SavedLocation> saveLocation(
            @RequestBody SavedLocationRequest request,
            @RequestAttribute("uid") String uid) {
        return ResponseEntity.ok(locationService.saveLocation(
                request.userId(), request.name(), request.address(), request.latitude(), request.longitude(), request.color(), request.logo(), uid
        ));
    }

    /**
     * Posodobi obstoječo shranjeno lokacijo.
     *
     * @param locationId ID lokacije, ki se posodablja
     * @param request novi podatki lokacije
     * @param uid Firebase uid iz request atributa
     * @return posodobljena lokacija
     */
    @PutMapping("/{locationId}")
    public ResponseEntity<SavedLocation> updateLocation(
            @PathVariable UUID locationId,
            @RequestBody SavedLocationRequest request,
            @RequestAttribute("uid") String uid) {
        return ResponseEntity.ok(locationService.updateLocation(
                locationId, request.name(), request.address(), request.latitude(), request.longitude(), request.color(), request.logo(), uid
        ));
    }

    /**
     * Izbriše shranjeno lokacijo uporabnika.
     *
     * @param locationId ID lokacije za brisanje
     * @param uid Firebase uid iz request atributa
     * @return prazen 204 odgovor
     */
    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> deleteLocation(
            @PathVariable UUID locationId,
            @RequestAttribute("uid") String uid) {
        locationService.deleteLocation(locationId, uid);
        return ResponseEntity.noContent().build();
    }

}
