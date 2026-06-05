package com.sibam.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibam.dto.location.SavedLocationRequest;
import com.sibam.persistence.SavedLocation;
import com.sibam.service.SavedLocationService;
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

class SavedLocationControllerTest {

    private final SavedLocationService service = mock(SavedLocationService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new SavedLocationController(service))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SavedLocation location(String name) {
        SavedLocation loc = new SavedLocation();
        loc.setName(name);
        loc.setAddress("Partizanska 1");
        loc.setLatitude(46.55);
        loc.setLongitude(15.64);
        loc.setColor("#FF0000");
        return loc;
    }

    @Test
    void getLocationsReturnsListFor200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.getLocationsForUser(eq(userId), eq("uid-1")))
                .thenReturn(List.of(location("Home"), location("Work")));

        mockMvc.perform(get("/api/locations/{userId}", userId)
                        .requestAttr("uid", "uid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Home"))
                .andExpect(jsonPath("$[1].name").value("Work"));
    }

    @Test
    void getLocationsForForeignUserReturns403() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.getLocationsForUser(eq(userId), eq("uid-1")))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/locations/{userId}", userId)
                        .requestAttr("uid", "uid-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveLocationReturns200WithSavedFields() throws Exception {
        UUID userId = UUID.randomUUID();
        SavedLocationRequest request = new SavedLocationRequest(userId, "Gym", "Koroška 1", 46.54, 15.63, "#00FF00", null);
        when(service.saveLocation(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(location("Gym"));

        mockMvc.perform(post("/api/locations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("uid", "uid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gym"));
    }

    @Test
    void updateLocationReturns200WithUpdatedFields() throws Exception {
        UUID locationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SavedLocationRequest request = new SavedLocationRequest(userId, "Office", "Nova 5", 46.56, 15.65, "#0000FF", null);
        SavedLocation updated = location("Office");
        when(service.updateLocation(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/locations/{locationId}", locationId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("uid", "uid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Office"));
    }

    @Test
    void deleteLocationReturns204() throws Exception {
        UUID locationId = UUID.randomUUID();
        doNothing().when(service).deleteLocation(any(), any());

        mockMvc.perform(delete("/api/locations/{locationId}", locationId)
                        .requestAttr("uid", "uid-1"))
                .andExpect(status().isNoContent());

        verify(service).deleteLocation(eq(locationId), eq("uid-1"));
    }

    @Test
    void deleteForeignLocationReturns403() throws Exception {
        UUID locationId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(service).deleteLocation(eq(locationId), eq("uid-1"));

        mockMvc.perform(delete("/api/locations/{locationId}", locationId)
                        .requestAttr("uid", "uid-1"))
                .andExpect(status().isForbidden());
    }
}
