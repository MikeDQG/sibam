package com.sibam.service;

import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.output.Journey;
import com.sibam.persistence.SavedPath;
import com.sibam.persistence.User;
import com.sibam.repository.SavedPathRepository;
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

class SavedPathServiceTest {

    private SavedPathRepository savedPathRepository;
    private UserRepository userRepository;
    private SavedPathService service;

    @BeforeEach
    void setUp() {
        savedPathRepository = mock(SavedPathRepository.class);
        userRepository = mock(UserRepository.class);
        service = new SavedPathService(savedPathRepository, userRepository);
    }

    private static Journey dummyJourney() {
        return new Journey("OK",
                new GeoPoint(46.55, 15.64), "Origin St",
                new GeoPoint(46.56, 15.65), "Dest St",
                "10 min", "2 km", List.of());
    }

    private User userWithUid(String uid) {
        User user = new User();
        user.setFirebaseUid(uid);
        return user;
    }

    // --- savePath ---

    @Test
    void savePathPersistsAllFields() {
        UUID userId = UUID.randomUUID();
        User user = userWithUid("uid-ok");
        Journey journey = dummyJourney();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(savedPathRepository.save(any(SavedPath.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SavedPath result = service.savePath(userId, "Home to Work", journey, "uid-ok");

        assertThat(result.getName()).isEqualTo("Home to Work");
        assertThat(result.getJourney()).isEqualTo(journey);
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getCreatedAt()).isNotNull();
        verify(savedPathRepository).save(any(SavedPath.class));
    }

    @Test
    void savePathThrowsForbiddenOnUidMismatch() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithUid("real-uid")));
        Journey journey = dummyJourney();

        assertThatThrownBy(() ->
                service.savePath(userId, "Trip", journey, "wrong-uid")
        ).isInstanceOf(ResponseStatusException.class);

        verify(savedPathRepository, never()).save(any());
    }

    // --- getPathsForUser ---

    @Test
    void getPathsForUserReturnsRepositoryList() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithUid("uid-ok")));
        SavedPath path = new SavedPath();
        path.setName("Morning commute");
        when(savedPathRepository.findByUserId(userId)).thenReturn(List.of(path));

        List<SavedPath> result = service.getPathsForUser(userId, "uid-ok");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Morning commute");
    }

    @Test
    void getPathsForUserThrowsForbiddenOnUidMismatch() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithUid("real-uid")));

        assertThatThrownBy(() ->
                service.getPathsForUser(userId, "wrong-uid")
        ).isInstanceOf(ResponseStatusException.class);

        verify(savedPathRepository, never()).findByUserId(any());
    }

    // --- deletePath ---

    @Test
    void deletePathCallsDeleteById() {
        UUID pathId = UUID.randomUUID();
        SavedPath path = new SavedPath();
        path.setUser(userWithUid("uid-ok"));

        when(savedPathRepository.findById(pathId)).thenReturn(Optional.of(path));

        service.deletePath(pathId, "uid-ok");

        verify(savedPathRepository).deleteById(pathId);
    }

    @Test
    void deletePathThrowsForbiddenAndNeverDeletes() {
        UUID pathId = UUID.randomUUID();
        SavedPath path = new SavedPath();
        path.setUser(userWithUid("real-uid"));

        when(savedPathRepository.findById(pathId)).thenReturn(Optional.of(path));

        assertThatThrownBy(() ->
                service.deletePath(pathId, "wrong-uid")
        ).isInstanceOf(ResponseStatusException.class);

        verify(savedPathRepository, never()).deleteById(any());
    }
}
