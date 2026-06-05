package com.sibam.service;

import com.sibam.persistence.User;
import com.sibam.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Servis za lokalne uporabniške zapise, povezane s Firebase identiteto.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Poišče uporabnika po Firebase uid ali ga ustvari ob prvi prijavi.
     *
     * @param firebaseUid uid iz Firebase tokena
     * @param email email iz Firebase tokena
     * @param fullName prikazno ime uporabnika, če je na voljo
     * @return obstoječ ali novo ustvarjen uporabnik
     */
    public User getOrCreateUser(String firebaseUid, String email, String fullName) {
        Optional<User> existingUser = userRepository.findByFirebaseUid(firebaseUid);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User newUser = new User();
        newUser.setFirebaseUid(firebaseUid);
        newUser.setEmail(email);
        newUser.setFullName(fullName);
        newUser.setCreatedAt(OffsetDateTime.now());
        return userRepository.save(newUser);
    }

    /**
     * Poišče lokalnega uporabnika po Firebase uid.
     *
     * @param firebaseUid uid iz Firebase tokena
     * @return Optional z uporabnikom, če obstaja
     */
    public Optional<User> getUserByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid);
    }
}
