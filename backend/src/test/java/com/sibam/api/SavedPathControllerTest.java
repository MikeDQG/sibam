package com.sibam.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibam.dto.savedPath.SavedPathRequest;
import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.output.Journey;
import com.sibam.persistence.SavedPath;
import com.sibam.service.SavedPathService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SavedPathControllerTest {

    private final SavedPathService service = mock(SavedPathService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new SavedPathController(service))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SavedPath savedPath(String name) {
        SavedPath path = new SavedPath();
        path.setName(name);
        return path;
    }

    private Journey dummyJourney() {
        return new Journey("OK",
                new GeoPoint(46.55, 15.64), "Origin",
                new GeoPoint(46.56, 15.65), "Dest",
                "10 min", "2 km", List.of());
    }

    @Test
    void getPathsReturns200WithList() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.getPathsForUser(eq(userId), eq("uid-1")))
                .thenReturn(List.of(savedPath("Morning commute"), savedPath("Evening run")));

        mockMvc.perform(get("/api/paths/{userId}", userId)
                        .requestAttr("uid", "uid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Morning commute"));
    }

    @Test
    void savePathReturns200WithSavedName() throws Exception {
        UUID userId = UUID.randomUUID();
        SavedPathRequest request = new SavedPathRequest(userId, "Home to Work", dummyJourney());
        when(service.savePath(any(), any(), any(), any())).thenReturn(savedPath("Home to Work"));

        mockMvc.perform(post("/api/paths")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("uid", "uid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Home to Work"));
    }

    @Test
    void deletePathReturns204() throws Exception {
        UUID pathId = UUID.randomUUID();
        doNothing().when(service).deletePath(any(), any());

        mockMvc.perform(delete("/api/paths/{pathId}", pathId)
                        .requestAttr("uid", "uid-1"))
                .andExpect(status().isNoContent());

        verify(service).deletePath(eq(pathId), eq("uid-1"));
    }

    @Test
    void deleteForeignPathReturns403() throws Exception {
        UUID pathId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(service).deletePath(eq(pathId), eq("uid-1"));

        mockMvc.perform(delete("/api/paths/{pathId}", pathId)
                        .requestAttr("uid", "uid-1"))
                .andExpect(status().isForbidden());
    }
}
