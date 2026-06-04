package com.sibam.api;

import com.sibam.dto.savedPath.SavedPathRequest;
import com.sibam.persistence.SavedPath;
import com.sibam.service.SavedPathService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/paths")
public class SavedPathController {
    private final SavedPathService savedPathService;

    public SavedPathController(SavedPathService savedPathService) {
        this.savedPathService = savedPathService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<SavedPath>> getPaths(
            @PathVariable UUID userId,
            @RequestAttribute("uid") String uid) {
        return ResponseEntity.ok(savedPathService.getPathsForUser(userId, uid));
    }

    @PostMapping
    public ResponseEntity<SavedPath> savePath(
            @RequestBody SavedPathRequest request,
            @RequestAttribute("uid") String uid) {
        return ResponseEntity.ok(savedPathService.savePath(
                request.userId(), request.name(), request.journey(), uid
        ));
    }

    @DeleteMapping("/{pathId}")
    public ResponseEntity<Void> deletePath(
            @PathVariable UUID pathId,
            @RequestAttribute("uid") String uid) {
        savedPathService.deletePath(pathId, uid);
        return ResponseEntity.noContent().build();
    }

}

