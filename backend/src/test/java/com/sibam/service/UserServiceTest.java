package com.sibam.service;

import com.sibam.persistence.User;
import com.sibam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private UserRepository userRepository;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UserService(userRepository);
    }

    @Test
    void getOrCreateUserReturnsExistingUserWithoutSaving() {
        User existing = new User();
        existing.setFirebaseUid("uid-123");
        existing.setEmail("test@example.com");

        when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Optional.of(existing));

        User result = service.getOrCreateUser("uid-123", "test@example.com", "Test User");

        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getOrCreateUserCreatesAndSavesNewUser() {
        when(userRepository.findByFirebaseUid("new-uid")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.getOrCreateUser("new-uid", "new@example.com", "New User");

        assertThat(result.getFirebaseUid()).isEqualTo("new-uid");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getFullName()).isEqualTo("New User");
        assertThat(result.getCreatedAt()).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUserByFirebaseUidDelegatestoRepository() {
        User user = new User();
        user.setFirebaseUid("uid-abc");

        when(userRepository.findByFirebaseUid("uid-abc")).thenReturn(Optional.of(user));

        Optional<User> result = service.getUserByFirebaseUid("uid-abc");

        assertThat(result).isPresent();
        assertThat(result.get().getFirebaseUid()).isEqualTo("uid-abc");
    }

    @Test
    void getUserByFirebaseUidReturnsEmptyWhenNotFound() {
        when(userRepository.findByFirebaseUid("unknown")).thenReturn(Optional.empty());

        Optional<User> result = service.getUserByFirebaseUid("unknown");

        assertThat(result).isEmpty();
    }

}
