package com.sibam.api;

import com.sibam.persistence.User;
import com.sibam.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {

    private final UserService service = mock(UserService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new UserController(service))
            .build();

    private User user(String uid, String email) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setFirebaseUid(uid);
        u.setEmail(email);
        u.setFullName("Test User");
        return u;
    }

    @Test
    void loginOrRegisterReturns200WithUser() throws Exception {
        when(service.getOrCreateUser("uid-1", "test@example.com", "Test User"))
                .thenReturn(user("uid-1", "test@example.com"));

        mockMvc.perform(post("/api/users/me")
                        .requestAttr("uid", "uid-1")
                        .requestAttr("email", "test@example.com")
                        .requestAttr("fullName", "Test User"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firebaseUid").value("uid-1"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getMeReturns200WhenUserExists() throws Exception {
        when(service.getUserByFirebaseUid("uid-1"))
                .thenReturn(Optional.of(user("uid-1", "test@example.com")));

        mockMvc.perform(get("/api/users/me")
                        .requestAttr("uid", "uid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firebaseUid").value("uid-1"));
    }

    @Test
    void getMeReturns404WhenUserNotFound() throws Exception {
        when(service.getUserByFirebaseUid("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me")
                        .requestAttr("uid", "unknown"))
                .andExpect(status().isNotFound());
    }
}
