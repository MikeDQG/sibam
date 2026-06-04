package com.sibam.it;

import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.output.Journey;
import com.sibam.persistence.SavedPath;
import com.sibam.persistence.User;
import com.sibam.repository.UserRepository;
import com.sibam.service.SavedPathService;
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
class SavedPathServiceIT extends AbstractDatabaseIT {

    @Autowired
    SavedPathService savedPathService;

    @Autowired
    UserRepository userRepository;

    private User user;

    @BeforeEach
    void createUser() {
        User u = new User();
        u.setFirebaseUid("path-uid-fixture");
        u.setEmail("path@example.com");
        u.setFullName("Path User");
        u.setCreatedAt(OffsetDateTime.now());
        user = userRepository.save(u);
    }

    private Journey dummyJourney() {
        return new Journey("OK",
                new GeoPoint(46.55, 15.64), "Origin St",
                new GeoPoint(46.56, 15.65), "Dest St",
                "10 min", "2 km", List.of());
    }

    @Test
    void savePathPersistsAndIsReturnedByGetPaths() {
        savedPathService.savePath(user.getId(), "Home to Work", dummyJourney(), user.getFirebaseUid());

        List<SavedPath> paths = savedPathService.getPathsForUser(user.getId(), user.getFirebaseUid());

        assertThat(paths).hasSize(1);
        assertThat(paths.get(0).getName()).isEqualTo("Home to Work");
    }

    @Test
    void savePathStoresJourneyData() {
        Journey journey = dummyJourney();
        SavedPath saved = savedPathService.savePath(user.getId(), "Trip", journey, user.getFirebaseUid());

        assertThat(saved.getJourney()).isNotNull();
        assertThat(saved.getJourney().status()).isEqualTo("OK");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void deletePathRemovesItFromDatabase() {
        SavedPath saved = savedPathService.savePath(user.getId(), "To Delete", dummyJourney(), user.getFirebaseUid());

        savedPathService.deletePath(saved.getId(), user.getFirebaseUid());

        List<SavedPath> paths = savedPathService.getPathsForUser(user.getId(), user.getFirebaseUid());
        assertThat(paths).isEmpty();
    }

    @Test
    void multiplePathsAreReturnedForSameUser() {
        savedPathService.savePath(user.getId(), "Path A", dummyJourney(), user.getFirebaseUid());
        savedPathService.savePath(user.getId(), "Path B", dummyJourney(), user.getFirebaseUid());

        List<SavedPath> paths = savedPathService.getPathsForUser(user.getId(), user.getFirebaseUid());

        assertThat(paths).hasSize(2);
        assertThat(paths).extracting(SavedPath::getName).containsExactlyInAnyOrder("Path A", "Path B");
    }

    @Test
    void getPathsThrowsForbiddenForWrongUid() {
        assertThatThrownBy(() ->
                savedPathService.getPathsForUser(user.getId(), "wrong-uid")
        ).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void savePathThrowsForbiddenForWrongUid() {
        assertThatThrownBy(() ->
                savedPathService.savePath(user.getId(), "Hack", dummyJourney(), "wrong-uid")
        ).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void deletePathThrowsForbiddenForWrongUid() {
        SavedPath saved = savedPathService.savePath(user.getId(), "Protected", dummyJourney(), user.getFirebaseUid());

        assertThatThrownBy(() ->
                savedPathService.deletePath(saved.getId(), "wrong-uid")
        ).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }
}
