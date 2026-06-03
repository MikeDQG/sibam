package com.sibam.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibam.graph.model.GeoPoint;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleRoutesServiceTest {

    @Test
    void parseRouteDetailsPreservesGoogleStepInstructionsAndOrder() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        String response = """
                {
                  "routes": [
                    {
                      "legs": [
                        {
                          "steps": [
                            {
                              "distanceMeters": 40,
                              "staticDuration": "6s",
                              "polyline": {
                                "geoJsonLinestring": {
                                  "coordinates": [[15.603524, 46.5379538], [15.6040463, 46.5379618]],
                                  "type": "LineString"
                                }
                              },
                              "navigationInstruction": {
                                "maneuver": "DEPART",
                                "instructions": "Head east on Maroltova ulica toward Pohorska ulica"
                              }
                            },
                            {
                              "distanceMeters": 362,
                              "duration": "48s",
                              "polyline": {
                                "geoJsonLinestring": {
                                  "coordinates": [[15.6040463, 46.5379618], [15.611451, 46.53853]],
                                  "type": "LineString"
                                }
                              },
                              "navigationInstruction": {
                                "maneuver": "TURN_LEFT",
                                "instructions": "Turn left onto Pohorska ulica"
                              }
                            }
                          ]
                        }
                      ],
                      "distanceMeters": 794,
                      "duration": "89s",
                      "polyline": {
                        "geoJsonLinestring": {
                          "coordinates": [[15.603524, 46.5379538], [15.6040463, 46.5379618], [15.611451, 46.53853]],
                          "type": "LineString"
                        }
                      }
                    }
                  ]
                }
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                response,
                new GeoPoint(46.538077, 15.603520),
                new GeoPoint(46.538493, 15.611431)
        );

        assertThat(result.polyline()).hasSize(3);
        assertThat(result.steps()).hasSize(2);
        assertThat(result.steps().get(0).instruction())
                .isEqualTo("Head east on Maroltova ulica toward Pohorska ulica");
        assertThat(result.steps().get(0).maneuver()).isEqualTo("DEPART");
        assertThat(result.steps().get(0).distanceMeters()).isEqualTo(40);
        assertThat(result.steps().get(0).durationSeconds()).isEqualTo(6);
        assertThat(result.steps().get(0).startPolylineIndex()).isEqualTo(0);
        assertThat(result.steps().get(0).endPolylineIndex()).isEqualTo(1);
        assertThat(result.steps().get(1).instruction()).isEqualTo("Turn left onto Pohorska ulica");
        assertThat(result.steps().get(1).maneuver()).isEqualTo("TURN_LEFT");
        assertThat(result.steps().get(1).durationSeconds()).isEqualTo(48);
        assertThat(result.steps().get(1).startPolylineIndex()).isEqualTo(1);
        assertThat(result.steps().get(1).endPolylineIndex()).isEqualTo(2);

        String serializedStep = new ObjectMapper().writeValueAsString(result.steps().getFirst());
        assertThat(serializedStep).doesNotContain("polyline");
        assertThat(serializedStep).contains("\"startPolylineIndex\":0");
        assertThat(serializedStep).contains("\"endPolylineIndex\":1");
    }
}
