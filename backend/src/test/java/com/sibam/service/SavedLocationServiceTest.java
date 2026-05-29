package com.sibam.service;

import com.sibam.persistence.SavedLocation;
import com.sibam.persistence.User;
import com.sibam.repository.SavedLocationRepository;
import com.sibam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SavedLocationServiceTest {
    private SavedLocationRepository savedLocationRepository;
    private UserRepository userRepository;
    private SavedLocationService service;

    @BeforeEach
    void setUp() {
        savedLocationRepository = mock(SavedLocationRepository.class);
        userRepository = mock(UserRepository.class);
        service = new SavedLocationService(savedLocationRepository, userRepository);
    }

    //    saveLocation

    @Test
    void saveLocationPersistsAllFields() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setFirebaseUid("uid-ok");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(savedLocationRepository.save(any(SavedLocation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SavedLocation result = service.saveLocation(
                userId, "Dom", "Partizanska 1", 46.556, 15.646, "#FF5733", null, "uid-ok"
        );

        assertThat(result.getName()).isEqualTo("Dom");
        assertThat(result.getAddress()).isEqualTo("Partizanska 1");
        assertThat(result.getLatitude()).isEqualTo(46.556);
        assertThat(result.getLongitude()).isEqualTo(15.646);
        assertThat(result.getColor()).isEqualTo("#FF5733");
        assertThat(result.getCreatedAt()).isNotNull();
        verify(savedLocationRepository).save(any(SavedLocation.class));
    }

    @Test
    void saveLocationThrowsForbiddenOnUidMismatch() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setFirebaseUid("real-uid");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                service.saveLocation(userId, "Dom", "Partizanska 1", 46.556, 15.646, "#FF5733", null, "wrong-uid")
        ).isInstanceOf(ResponseStatusException.class);

        verify(savedLocationRepository, never()).save(any());
    }

    // updateLocation

    @Test
    void updateLocationChangesFields() {
        UUID locationId = UUID.randomUUID();
        User user = new User();
        user.setFirebaseUid("uid-ok");
        SavedLocation location = new SavedLocation();
        location.setUser(user);
        location.setName("Old");

        when(savedLocationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(savedLocationRepository.save(any(SavedLocation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SavedLocation result = service.updateLocation(
                locationId, "New", "Nova ulica 5", 46.0, 15.0, "#000", null, "uid-ok"
        );

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getAddress()).isEqualTo("Nova ulica 5");
    }

    @Test
    void updateLocationThrowsForbiddenOnUidMismatch() {
        UUID locationId = UUID.randomUUID();
        User user = new User();
        user.setFirebaseUid("real-uid");
        SavedLocation location = new SavedLocation();
        location.setUser(user);

        when(savedLocationRepository.findById(locationId)).thenReturn(Optional.of(location));

        assertThatThrownBy(() ->
                service.updateLocation(locationId, "New", "Addr", 46.0, 15.0, "#000", null, "wrong-uid")
        ).isInstanceOf(ResponseStatusException.class);

        verify(savedLocationRepository, never()).save(any());
    }

    // deleteLocation

    @Test
    void deleteLocationCallsDeleteById() {
        UUID locationId = UUID.randomUUID();
        User user = new User();
        user.setFirebaseUid("uid-ok");
        SavedLocation location = new SavedLocation();
        location.setUser(user);

        when(savedLocationRepository.findById(locationId)).thenReturn(Optional.of(location));

        service.deleteLocation(locationId, "uid-ok");

        verify(savedLocationRepository).deleteById(locationId);
    }

    @Test
    void deleteLocationThrowsForbiddenAndNeverDeletes() {
        UUID locationId = UUID.randomUUID();
        User user = new User();
        user.setFirebaseUid("real-uid");
        SavedLocation location = new SavedLocation();
        location.setUser(user);

        when(savedLocationRepository.findById(locationId)).thenReturn(Optional.of(location));

        assertThatThrownBy(() ->
                service.deleteLocation(locationId, "wrong-uid")
        ).isInstanceOf(ResponseStatusException.class);

        verify(savedLocationRepository, never()).deleteById(any());
    }

    // getLocationsForUser

    @Test
    void getLocationsForUserReturnsRepositoryList() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setFirebaseUid("uid-ok");

        SavedLocation loc = new SavedLocation();
        loc.setName("Work");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(savedLocationRepository.findByUserId(userId)).thenReturn(List.of(loc));

        List<SavedLocation> result = service.getLocationsForUser(userId, "uid-ok");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Work");
    }

    @Test
    void getLocationsForUserThrowsForbiddenOnUidMismatch() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setFirebaseUid("real-uid");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                service.getLocationsForUser(userId, "wrong-uid")
        ).isInstanceOf(ResponseStatusException.class);

        verify(savedLocationRepository, never()).findByUserId(any());
    }
}
