package com.sibam.api;

import com.sibam.persistence.User;
import com.sibam.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller za prijavo in osnovne podatke trenutnega uporabnika.
 *
 * FirebaseAuthFilter pred klicem nastavi uid, email in opcijsko ime, servis pa
 * po potrebi ustvari lokalni uporabniški zapis.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Vrne obstoječega uporabnika ali ga ustvari ob prvi prijavi.
     *
     * @param uid Firebase uid iz request atributa
     * @param email email iz Firebase tokena
     * @param fullName prikazno ime, če je prisotno v tokenu
     * @return lokalni uporabniški zapis
     */
    @PostMapping("/me")
    public ResponseEntity<User> loginOrRegister(
            @RequestAttribute("uid") String uid,
            @RequestAttribute("email") String email,
            @RequestAttribute(value = "fullName", required = false) String fullName)
    {
        User user = userService.getOrCreateUser(uid, email, fullName);
        return ResponseEntity.ok(user);
    }

    /**
     * Vrne lokalni zapis trenutno prijavljenega uporabnika.
     *
     * @param uid Firebase uid iz request atributa
     * @return uporabnik ali 404, če zapis še ne obstaja
     */
    @GetMapping("/me")
    public ResponseEntity<User> getMe(
            @RequestAttribute("uid") String uid) {
        return userService.getUserByFirebaseUid(uid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
