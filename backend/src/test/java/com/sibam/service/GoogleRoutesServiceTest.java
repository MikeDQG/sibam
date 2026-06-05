package com.sibam.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibam.graph.model.GeoPoint;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

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
        assertThat(result.steps().get(0).startPolylineIndex()).isZero();
        assertThat(result.steps().get(0).endPolylineIndex()).isEqualTo(1);
        assertThat(result.steps().get(1).instruction()).isEqualTo("Turn left onto Pohorska ulica");
        assertThat(result.steps().get(1).maneuver()).isEqualTo("TURN_LEFT");
        assertThat(result.steps().get(1).durationSeconds()).isEqualTo(48);
        assertThat(result.steps().get(1).startPolylineIndex()).isEqualTo(1);
        assertThat(result.steps().get(1).endPolylineIndex()).isEqualTo(2);

        String serializedStep = new ObjectMapper().writeValueAsString(result.steps().getFirst());
        assertThat(serializedStep)
                .doesNotContain("polyline")
                .contains("\"startPolylineIndex\":0")
                .contains("\"endPolylineIndex\":1");
    }

    @Test
    void parseRouteDetailsReturnsFallbackWhenRoutesArrayIsEmpty() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest   = new GeoPoint(46.56, 15.65);
        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                "{\"routes\":[]}", origin, dest
        );

        assertThat(result.polyline()).containsExactly(origin, dest);
        assertThat(result.steps()).isNull();
    }

    @Test
    void parseRouteDetailsReturnsFallbackWhenPolylineCoordinatesAreEmpty() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest   = new GeoPoint(46.56, 15.65);
        String response = """
                {"routes":[{"polyline":{"geoJsonLinestring":{"coordinates":[]}},"legs":[]}]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(response, origin, dest);

        assertThat(result.polyline()).containsExactly(origin, dest);
        assertThat(result.steps()).isEmpty();
    }

    @Test
    void parseRouteDetailsWithEncodedPolylineFormat() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "ENCODED_POLYLINE");

        // "_p~iF~ps|U_ulLnnqC_mqNvxq`@" encodes 3 known points
        String response = """
                {"routes":[{
                  "polyline":{"encodedPolyline":"_p~iF~ps|U_ulLnnqC_mqNvxq`@"},
                  "legs":[]
                }]}
                """;

        GeoPoint origin = new GeoPoint(38.5, -120.2);
        GeoPoint dest   = new GeoPoint(40.7, -120.95);
        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(response, origin, dest);

        assertThat(result.polyline()).hasSizeGreaterThan(1);
        assertThat(result.steps()).isEmpty();
    }

    @Test
    void parseRouteDetailsWithNoLegsProducesNoSteps() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                  "legs":[]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                response, new GeoPoint(46.55, 15.64), new GeoPoint(46.56, 15.65)
        );

        assertThat(result.polyline()).hasSize(2);
        assertThat(result.steps()).isEmpty();
    }

    @Test
    void fetchRouteDetailsReturnsFallbackWhenApiKeyIsBlank() {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);
        GoogleRoutesService.RouteDetails result = service.fetchRouteDetails(
                origin, dest, com.sibam.graph.model.EdgeType.WALK
        );

        assertThat(result.polyline()).containsExactly(origin, dest);
        assertThat(result.steps()).isNull();
    }

    @Test
    void fetchPolylineFallsBackToOriginAndDestinationWhenApiKeyIsBlank() {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest   = new GeoPoint(46.56, 15.65);

        assertThat(service.fetchPolyline(origin, dest)).containsExactly(origin, dest);
    }

    @Test
    void fetchCoupledRouteDetailsReturnsEmptyForTooFewPoints() {
        GoogleRoutesService service = new GoogleRoutesService();

        assertThat(service.fetchRouteDetails(List.of(), com.sibam.graph.model.EdgeType.WALK)).isEmpty();
        assertThat(service.fetchRouteDetails(
                List.of(new GeoPoint(46.55, 15.64)),
                com.sibam.graph.model.EdgeType.WALK
        )).isEmpty();
    }

    @Test
    void fetchCoupledRouteDetailsFallsBackPerSegmentWhenApiKeyIsBlank() {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        GeoPoint first = new GeoPoint(46.55, 15.64);
        GeoPoint second = new GeoPoint(46.56, 15.65);
        GeoPoint third = new GeoPoint(46.57, 15.66);

        List<GoogleRoutesService.RouteDetails> result = service.fetchRouteDetails(
                List.of(first, second, third),
                com.sibam.graph.model.EdgeType.BIKE
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).polyline()).containsExactly(first, second);
        assertThat(result.get(0).steps()).isNull();
        assertThat(result.get(1).polyline()).containsExactly(second, third);
        assertThat(result.get(1).steps()).isNull();
    }

    @Test
    void parseCoupledRouteDetailsSplitsLegsIntoSegmentDetails() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        GeoPoint first = new GeoPoint(46.55, 15.64);
        GeoPoint second = new GeoPoint(46.56, 15.65);
        GeoPoint third = new GeoPoint(46.57, 15.66);
        String response = """
                {"routes":[{"legs":[
                  {"steps":[{
                    "distanceMeters": 10,
                    "duration": "3s",
                    "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"First leg"}
                  }]},
                  {"steps":[{
                    "distanceMeters": 20,
                    "staticDuration": "4s",
                    "polyline":{"geoJsonLinestring":{"coordinates":[[15.65,46.56],[15.66,46.57]]}},
                    "navigationInstruction":{"maneuver":"TURN_RIGHT","instructions":"Second leg"}
                  }]}
                ]}]}
                """;

        List<GoogleRoutesService.RouteDetails> result = service.parseRouteDetails(
                response,
                List.of(first, second, third)
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).polyline()).containsExactly(first, second);
        assertThat(result.get(0).steps()).hasSize(1);
        assertThat(result.get(0).steps().getFirst().instruction()).isEqualTo("First leg");
        assertThat(result.get(0).steps().getFirst().durationSeconds()).isEqualTo(3);
        assertThat(result.get(1).polyline()).containsExactly(second, third);
        assertThat(result.get(1).steps()).hasSize(1);
        assertThat(result.get(1).steps().getFirst().instruction()).isEqualTo("Second leg");
        assertThat(result.get(1).steps().getFirst().durationSeconds()).isEqualTo(4);
    }

    @Test
    void routeDetailsDefensivelyCopiesPolylineAndSteps() {
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint destination = new GeoPoint(46.56, 15.65);
        List<GeoPoint> polyline = new java.util.ArrayList<>(List.of(origin, destination));
        List<com.sibam.graph.model.output.NavigationStep> steps = new java.util.ArrayList<>(List.of(
                new com.sibam.graph.model.output.NavigationStep("Go", "DEPART", 1, 1, 0, 1)
        ));

        GoogleRoutesService.RouteDetails details = new GoogleRoutesService.RouteDetails(polyline, steps);
        polyline.clear();
        steps.clear();

        assertThat(details.polyline()).containsExactly(origin, destination);
        assertThat(details.steps()).hasSize(1);
        assertThat(new GoogleRoutesService.RouteDetails(null, null).polyline()).isEmpty();
        assertThat(new GoogleRoutesService.RouteDetails(null, null).steps()).isNull();
    }

    @Test
    void parseRouteDetailsWithStepHavingNoDuration() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                  "legs":[{"steps":[{
                    "distanceMeters": 100,
                    "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"Go straight"}
                  }]}]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                response, new GeoPoint(46.55, 15.64), new GeoPoint(46.56, 15.65)
        );

        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).durationSeconds()).isZero();
        assertThat(result.steps().get(0).instruction()).isEqualTo("Go straight");
    }

    @Test
    void parseRouteDetailsWithDecimalDuration() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                  "legs":[{"steps":[{
                    "distanceMeters": 50,
                    "duration": "7.5s",
                    "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"Walk"}
                  }]}]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                response, new GeoPoint(46.55, 15.64), new GeoPoint(46.56, 15.65)
        );

        assertThat(result.steps().get(0).durationSeconds()).isEqualTo(8);
    }

    @Test
    void parseRouteDetailsWithStepHavingNoPolylineUsesClosestFallbackRange() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56],[15.66,46.57]]}},
                  "legs":[{"steps":[{
                    "distanceMeters": 100,
                    "duration": "10s",
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"Walk forward"}
                  }]}]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                response, new GeoPoint(46.55, 15.64), new GeoPoint(46.57, 15.66)
        );

        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).startPolylineIndex()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void parseRouteDetailsWithEncodedPolylineAndStepPolyline() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "ENCODED_POLYLINE");

        String response = """
                {"routes":[{
                  "polyline":{"encodedPolyline":"_p~iF~ps|U_ulLnnqC_mqNvxq`@"},
                  "legs":[{"steps":[{
                    "distanceMeters": 200,
                    "duration": "30s",
                    "polyline":{"encodedPolyline":"_p~iF~ps|U_ulLnnqC"},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"Go north"}
                  }]}]
                }]}
                """;

        GeoPoint origin = new GeoPoint(38.5, -120.2);
        GeoPoint dest   = new GeoPoint(40.7, -120.95);
        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(response, origin, dest);

        assertThat(result.polyline()).hasSizeGreaterThan(1);
        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).instruction()).isEqualTo("Go north");
    }

    @Test
    void parseRouteDetailsWithStepPolylineNotMatchingAnyRoutePointFallsBack() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                  "legs":[{"steps":[{
                    "distanceMeters": 100,
                    "duration": "15s",
                    "polyline":{"geoJsonLinestring":{"coordinates":[[99.0,99.0],[99.1,99.1]]}},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"Somewhere"}
                  }]}]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                response, new GeoPoint(46.55, 15.64), new GeoPoint(46.56, 15.65)
        );

        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).startPolylineIndex()).isGreaterThanOrEqualTo(0);
        assertThat(result.steps().get(0).endPolylineIndex()).isGreaterThanOrEqualTo(0);
    }
}
