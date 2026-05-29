package com.sibam.it;

import com.sibam.persistence.User;
import com.sibam.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class UserServiceIT extends AbstractDatabaseIT {

    @Autowired
    UserService userService;

    @Test
    void getOrCreateUserCreatesNewUser() {
        User user = userService.getOrCreateUser("uid-create-1", "test@example.com", "Test User");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getFirebaseUid()).isEqualTo("uid-create-1");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getFullName()).isEqualTo("Test User");
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void getOrCreateUserReturnsExistingUserOnSecondCall() {
        User first = userService.getOrCreateUser("uid-idempotent", "first@example.com", "First");
        User second = userService.getOrCreateUser("uid-idempotent", "second@example.com", "Second");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getEmail()).isEqualTo("first@example.com");
    }

    @Test
    void getUserByFirebaseUidReturnsUserAfterCreation() {
        userService.getOrCreateUser("uid-find-1", "find@example.com", "Find Me");

        Optional<User> found = userService.getUserByFirebaseUid("uid-find-1");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("find@example.com");
    }

    @Test
    void getUserByFirebaseUidReturnsEmptyForUnknownUid() {
        Optional<User> found = userService.getUserByFirebaseUid("uid-nonexistent");

        assertThat(found).isEmpty();
    }
}
