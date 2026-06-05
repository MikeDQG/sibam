package com.sibam.api;

import com.sibam.dto.savedPath.SavedPathRequest;
import com.sibam.persistence.SavedPath;
import com.sibam.service.SavedPathService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller za shranjene poti uporabnika.
 *
 * Endpointi uporabljajo Firebase uid iz request atributa, servisni sloj pa
 * preveri, ali zahteva dostopa do podatkov pravega uporabnika.
 */
@RestController
@RequestMapping("/api/paths")
public class SavedPathController {
    private final SavedPathService savedPathService;

    public SavedPathController(SavedPathService savedPathService) {
        this.savedPathService = savedPathService;
    }

    /**
     * Vrne vse shranjene poti izbranega uporabnika.
     *
     * @param userId interni ID uporabnika
     * @param uid Firebase uid iz request atributa
     * @return seznam shranjenih poti
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<SavedPath>> getPaths(
            @PathVariable UUID userId,
            @RequestAttribute("uid") String uid) {
        return ResponseEntity.ok(savedPathService.getPathsForUser(userId, uid));
    }

    /**
     * Shrani pot, ki jo je frontend izračunal oziroma prikazal uporabniku.
     *
     * @param request ime poti, uporabnik in serializiran Journey
     * @param uid Firebase uid iz request atributa
     * @return shranjena pot
     */
    @PostMapping
    public ResponseEntity<SavedPath> savePath(
            @RequestBody SavedPathRequest request,
            @RequestAttribute("uid") String uid) {
        return ResponseEntity.ok(savedPathService.savePath(
                request.userId(), request.name(), request.journey(), uid
        ));
    }

    /**
     * Izbriše shranjeno pot uporabnika.
     *
     * @param pathId ID poti za brisanje
     * @param uid Firebase uid iz request atributa
     * @return prazen 204 odgovor
     */
    @DeleteMapping("/{pathId}")
    public ResponseEntity<Void> deletePath(
            @PathVariable UUID pathId,
            @RequestAttribute("uid") String uid) {
        savedPathService.deletePath(pathId, uid);
        return ResponseEntity.noContent().build();
    }

}

