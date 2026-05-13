package com.sibam.api;

import com.sibam.persistence.User;
import com.sibam.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/me")
    public ResponseEntity<User> loginOrRegister(
            @RequestAttribute("uid") String uid,
            @RequestAttribute("email") String email) {
        User user = userService.getOrCreateUser(uid, email);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me")
    public ResponseEntity<User> getMe(
            @RequestAttribute("uid") String uid) {
        return userService.getUserByFirebaseUid(uid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}