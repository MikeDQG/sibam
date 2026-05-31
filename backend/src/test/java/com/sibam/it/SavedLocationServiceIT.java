package com.sibam.it;

import com.sibam.persistence.SavedLocation;
import com.sibam.persistence.User;
import com.sibam.repository.UserRepository;
import com.sibam.service.SavedLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class SavedLocationServiceIT extends AbstractDatabaseIT{
    @Autowired
    SavedLocationService savedLocationService;

    @Autowired
    UserRepository userRepository;

    private User user;

    @BeforeEach
    void createUser() {
        User u = new User();
        u.setFirebaseUid("loc-uid-fixture");
        u.setEmail("loc@example.com");
        u.setFullName("Loc User");
        u.setCreatedAt(OffsetDateTime.now());
        user = userRepository.save(u);
    }

    @Test
    void saveLocationPersistsAndIsReturnedByGetLocations() {
        savedLocationService.saveLocation(
                user.getId(), "Home", "Partizanska 1", 46.55, 15.65, "#FF0000", null, user.getFirebaseUid()
        );

        List<SavedLocation> locations = savedLocationService.getLocationsForUser(
                user.getId(), user.getFirebaseUid()
        );

        assertThat(locations).hasSize(1);
        assertThat(locations.get(0).getName()).isEqualTo("Home");
        assertThat(locations.get(0).getAddress()).isEqualTo("Partizanska 1");
        assertThat(locations.get(0).getLatitude()).isEqualTo(46.55);
        assertThat(locations.get(0).getLongitude()).isEqualTo(15.65);
        assertThat(locations.get(0).getColor()).isEqualTo("#FF0000");
    }

    @Test
    void updateLocationModifiesFields() {
        SavedLocation saved = savedLocationService.saveLocation(
                user.getId(), "Work", "Gosposvetska 2", 46.56, 15.66, "#0000FF", null, user.getFirebaseUid()
        );

        savedLocationService.updateLocation(
                saved.getId(), "Office", "Gosposvetska 5", 46.57, 15.67, "#00FF00", null, user.getFirebaseUid()
        );

        List<SavedLocation> locations = savedLocationService.getLocationsForUser(
                user.getId(), user.getFirebaseUid()
        );

        assertThat(locations).hasSize(1);
        assertThat(locations.get(0).getName()).isEqualTo("Office");
        assertThat(locations.get(0).getAddress()).isEqualTo("Gosposvetska 5");
        assertThat(locations.get(0).getColor()).isEqualTo("#00FF00");
    }

    @Test
    void deleteLocationRemovesItFromDatabase() {
        SavedLocation saved = savedLocationService.saveLocation(
                user.getId(), "Gym", "Koroška cesta 1", 46.54, 15.63, "#FFFF00", null, user.getFirebaseUid()
        );

        savedLocationService.deleteLocation(saved.getId(), user.getFirebaseUid());

        List<SavedLocation> locations = savedLocationService.getLocationsForUser(
                user.getId(), user.getFirebaseUid()
        );
        assertThat(locations).isEmpty();
    }

    @Test
    void getLocationsThrowsForbiddenForWrongUid() {
        assertThatThrownBy(() ->
                savedLocationService.getLocationsForUser(user.getId(), "wrong-uid")
        ).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void saveLocationThrowsForbiddenForWrongUid() {
        assertThatThrownBy(() ->
                savedLocationService.saveLocation(
                        user.getId(), "Fake", "Fake St", 46.0, 15.0, "#000", null, "wrong-uid"
                )
        ).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void updateLocationThrowsForbiddenForWrongUid() {
        SavedLocation saved = savedLocationService.saveLocation(
                user.getId(), "Secret", "Secret St", 46.0, 15.0, "#111", null, user.getFirebaseUid()
        );

        assertThatThrownBy(() ->
                savedLocationService.updateLocation(
                        saved.getId(), "Hacked", "Hacker Ave", 0.0, 0.0, "#000", null, "wrong-uid"
                )
        ).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void deleteLocationThrowsForbiddenForWrongUid() {
        SavedLocation saved = savedLocationService.saveLocation(
                user.getId(), "Locked", "Locked St", 46.0, 15.0, "#222", null, user.getFirebaseUid()
        );

        assertThatThrownBy(() ->
                savedLocationService.deleteLocation(saved.getId(), "wrong-uid")
        ).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }



}
