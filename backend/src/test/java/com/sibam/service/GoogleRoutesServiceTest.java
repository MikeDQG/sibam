package com.sibam.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.GeoPoint;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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

    // ── WebClient mock helpers ────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static WebClient stubWebClient(Mono<String> responseMono) {
        WebClient client = mock(WebClient.class);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class, RETURNS_SELF);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(client.post()).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(responseMono);
        return client;
    }

    private static GoogleRoutesService serviceWithClient(Mono<String> responseMono) {
        return serviceWithClient("GEO_JSON_LINESTRING", responseMono);
    }

    private static GoogleRoutesService serviceWithClient(String encoding, Mono<String> responseMono) {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "fieldMask", "routes.legs");
        ReflectionTestUtils.setField(service, "polylineEncoding", encoding);
        ReflectionTestUtils.setField(service, "webClient", stubWebClient(responseMono));
        return service;
    }

    // ── fetchRouteDetails(GeoPoint, GeoPoint, EdgeType) with API key ─────────

    @Test
    void fetchRouteDetailsWithApiKeyParsesSuccessfulResponse() {
        String json = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                  "legs":[]
                }]}
                """;
        GoogleRoutesService service = serviceWithClient(Mono.just(json));
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);

        GoogleRoutesService.RouteDetails result = service.fetchRouteDetails(origin, dest, EdgeType.WALK);

        assertThat(result.polyline()).hasSize(2);
        assertThat(result.steps()).isEmpty();
    }

    @Test
    void fetchRouteDetailsWithApiKeyReturnsFallbackOnEmptyResponse() {
        GoogleRoutesService service = serviceWithClient(Mono.empty());
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);

        GoogleRoutesService.RouteDetails result = service.fetchRouteDetails(origin, dest, EdgeType.WALK);

        assertThat(result.polyline()).containsExactly(origin, dest);
        assertThat(result.steps()).isNull();
    }

    @Test
    void fetchRouteDetailsWithApiKeyReturnsFallbackOnWebClientError() {
        GoogleRoutesService service = serviceWithClient(Mono.error(new RuntimeException("network error")));
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);

        GoogleRoutesService.RouteDetails result = service.fetchRouteDetails(origin, dest, EdgeType.WALK);

        assertThat(result.polyline()).containsExactly(origin, dest);
        assertThat(result.steps()).isNull();
    }

    @Test
    void fetchRouteDetailsWithApiKeyReturnsFallbackOnException() {
        WebClient throwingClient = mock(WebClient.class);
        when(throwingClient.post()).thenThrow(new RuntimeException("unexpected"));

        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "fieldMask", "routes");
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        ReflectionTestUtils.setField(service, "webClient", throwingClient);

        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);

        GoogleRoutesService.RouteDetails result = service.fetchRouteDetails(origin, dest, EdgeType.WALK);

        assertThat(result.polyline()).containsExactly(origin, dest);
        assertThat(result.steps()).isNull();
    }

    @Test
    void fetchRouteDetailsWithBikeEdgeTypeUsesWalkTravelMode() {
        String json = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                  "legs":[]
                }]}
                """;
        GoogleRoutesService service = serviceWithClient(Mono.just(json));
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);

        GoogleRoutesService.RouteDetails result = service.fetchRouteDetails(origin, dest, EdgeType.BIKE);

        assertThat(result.polyline()).hasSize(2);
    }

    @Test
    void fetchRouteDetailsWithEncodedPolylineEncodingUsesEncodedPolylineBranch() {
        String json = """
                {"routes":[{"polyline":{"encodedPolyline":"_p~iF~ps|U"},"legs":[]}]}
                """;
        GoogleRoutesService service = serviceWithClient("ENCODED_POLYLINE", Mono.just(json));
        GeoPoint origin = new GeoPoint(38.5, -120.2);
        GeoPoint dest = new GeoPoint(40.7, -120.95);

        GoogleRoutesService.RouteDetails result = service.fetchRouteDetails(origin, dest, EdgeType.WALK);

        assertThat(result.polyline()).hasSizeGreaterThanOrEqualTo(1);
    }

    // ── fetchPolyline null/empty RouteDetails branches (via spy) ─────────────

    @Test
    void fetchPolylineReturnsFallbackWhenRouteDetailsIsNull() {
        GoogleRoutesService spy = spy(new GoogleRoutesService());
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);
        doReturn(null).when(spy).fetchRouteDetails(eq(origin), eq(dest), any(EdgeType.class));

        assertThat(spy.fetchPolyline(origin, dest)).containsExactly(origin, dest);
    }

    @Test
    void fetchPolylineReturnsFallbackWhenPolylineIsEmpty() {
        GoogleRoutesService spy = spy(new GoogleRoutesService());
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);
        GoogleRoutesService.RouteDetails emptyDetails = new GoogleRoutesService.RouteDetails(List.of(), null);
        doReturn(emptyDetails).when(spy).fetchRouteDetails(eq(origin), eq(dest), any(EdgeType.class));

        assertThat(spy.fetchPolyline(origin, dest)).containsExactly(origin, dest);
    }

    // ── fetchRouteDetails(List, EdgeType) with API key ────────────────────────

    @Test
    void fetchCoupledRouteDetailsWithTwoPointsAndApiKeyDelegatesToSingleFetch() {
        String json = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                  "legs":[]
                }]}
                """;
        GoogleRoutesService service = serviceWithClient(Mono.just(json));
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);

        List<GoogleRoutesService.RouteDetails> result = service.fetchRouteDetails(List.of(a, b), EdgeType.WALK);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).polyline()).hasSize(2);
    }

    @Test
    void fetchCoupledRouteDetailsWithApiKeyParsesMultiLegResponse() {
        String json = """
                {"routes":[{"legs":[
                  {"steps":[{"distanceMeters":10,"duration":"3s",
                    "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"First"}}]},
                  {"steps":[{"distanceMeters":20,"duration":"4s",
                    "polyline":{"geoJsonLinestring":{"coordinates":[[15.65,46.56],[15.66,46.57]]}},
                    "navigationInstruction":{"maneuver":"ARRIVE","instructions":"Second"}}]}
                ]}]}
                """;
        GoogleRoutesService service = serviceWithClient(Mono.just(json));
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);
        GeoPoint c = new GeoPoint(46.57, 15.66);

        List<GoogleRoutesService.RouteDetails> result = service.fetchRouteDetails(List.of(a, b, c), EdgeType.WALK);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).steps()).hasSize(1);
        assertThat(result.get(0).steps().get(0).instruction()).isEqualTo("First");
        assertThat(result.get(1).steps().get(0).instruction()).isEqualTo("Second");
    }

    @Test
    void fetchCoupledRouteDetailsWithFourPointsIncludesMultipleIntermediates() {
        String json = """
                {"routes":[{"legs":[
                  {"steps":[]},{"steps":[]},{"steps":[]}
                ]}]}
                """;
        GoogleRoutesService service = serviceWithClient(Mono.just(json));
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);
        GeoPoint c = new GeoPoint(46.57, 15.66);
        GeoPoint d = new GeoPoint(46.58, 15.67);

        List<GoogleRoutesService.RouteDetails> result = service.fetchRouteDetails(List.of(a, b, c, d), EdgeType.WALK);

        assertThat(result).hasSize(3);
    }

    @Test
    void fetchCoupledRouteDetailsWithApiKeyReturnsFallbackOnEmptyResponse() {
        GoogleRoutesService service = serviceWithClient(Mono.empty());
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);
        GeoPoint c = new GeoPoint(46.57, 15.66);

        List<GoogleRoutesService.RouteDetails> result = service.fetchRouteDetails(List.of(a, b, c), EdgeType.WALK);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).polyline()).containsExactly(a, b);
        assertThat(result.get(1).polyline()).containsExactly(b, c);
    }

    @Test
    void fetchCoupledRouteDetailsWithApiKeyReturnsFallbackOnWebClientError() {
        GoogleRoutesService service = serviceWithClient(Mono.error(new RuntimeException("timeout")));
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);
        GeoPoint c = new GeoPoint(46.57, 15.66);

        List<GoogleRoutesService.RouteDetails> result = service.fetchRouteDetails(List.of(a, b, c), EdgeType.WALK);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).polyline()).containsExactly(a, b);
    }

    @Test
    void fetchCoupledRouteDetailsWithApiKeyReturnsFallbackOnException() {
        WebClient throwingClient = mock(WebClient.class);
        when(throwingClient.post()).thenThrow(new RuntimeException("crash"));

        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "fieldMask", "routes");
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        ReflectionTestUtils.setField(service, "webClient", throwingClient);

        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);
        GeoPoint c = new GeoPoint(46.57, 15.66);

        List<GoogleRoutesService.RouteDetails> result = service.fetchRouteDetails(List.of(a, b, c), EdgeType.WALK);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).polyline()).containsExactly(a, b);
    }

    // ── parseRouteDetails(String, List) edge cases ───────────────────────────

    @Test
    void parseCoupledRouteDetailsEmptyRoutesArrayReturnsFallback() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);
        GeoPoint c = new GeoPoint(46.57, 15.66);

        List<GoogleRoutesService.RouteDetails> result = service.parseRouteDetails(
                "{\"routes\":[]}", List.of(a, b, c)
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).polyline()).containsExactly(a, b);
        assertThat(result.get(1).polyline()).containsExactly(b, c);
    }

    @Test
    void parseCoupledRouteDetailsEmptyLegsArrayReturnsFallback() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);

        List<GoogleRoutesService.RouteDetails> result = service.parseRouteDetails(
                "{\"routes\":[{\"legs\":[]}]}", List.of(a, b)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).polyline()).containsExactly(a, b);
        assertThat(result.get(0).steps()).isNull();
    }

    @Test
    void parseCoupledRouteDetailsFewerLegsThanSegmentsUsesNullLegFallback() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);
        GeoPoint c = new GeoPoint(46.57, 15.66);

        String response = """
                {"routes":[{"legs":[
                  {"steps":[{"distanceMeters":10,"duration":"2s",
                    "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"Go"}}]}
                ]}]}
                """;

        List<GoogleRoutesService.RouteDetails> result = service.parseRouteDetails(
                response, List.of(a, b, c)
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(1).polyline()).containsExactly(b, c);
        assertThat(result.get(1).steps()).isNull();
    }

    // ── parseLegRouteDetails edge cases ──────────────────────────────────────

    @Test
    void parseLegRouteDetailsEmptyStepsReturnsEmptyStepsList() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);

        List<GoogleRoutesService.RouteDetails> result = service.parseRouteDetails(
                "{\"routes\":[{\"legs\":[{\"steps\":[]}]}]}", List.of(a, b)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).polyline()).containsExactly(a, b);
        assertThat(result.get(0).steps()).isEmpty();
    }

    @Test
    void parseLegRouteDetailsStepWithNoPolylineUsesLegPolylineFallback() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);

        String response = """
                {"routes":[{"legs":[{"steps":[
                  {"polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.645,46.545]]}},"navigationInstruction":{"maneuver":"DEPART","instructions":"Step1"}},
                  {"navigationInstruction":{"maneuver":"ARRIVE","instructions":"Step2 no polyline"}}
                ]}]}]}
                """;

        List<GoogleRoutesService.RouteDetails> result = service.parseRouteDetails(response, List.of(a, b));

        assertThat(result.get(0).steps()).hasSize(2);
        assertThat(result.get(0).steps().get(1).instruction()).isEqualTo("Step2 no polyline");
    }

    @Test
    void parseLegRouteDetailsFirstStepWithNoPolylineUsesFallbackStart() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);

        String response = """
                {"routes":[{"legs":[{"steps":[
                  {"navigationInstruction":{"maneuver":"DEPART","instructions":"No polyline first step"}}
                ]}]}]}
                """;

        List<GoogleRoutesService.RouteDetails> result = service.parseRouteDetails(response, List.of(a, b));

        assertThat(result.get(0).polyline()).contains(a).contains(b);
        assertThat(result.get(0).steps()).hasSize(1);
    }

    // ── parseSteps edge cases ─────────────────────────────────────────────────

    @Test
    void parseStepsSkipsLegWhereStepsIsNotAnArray() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                  "legs":[
                    {"steps":"not-an-array"},
                    {"steps":[{"distanceMeters":50,"duration":"5s",
                      "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                      "navigationInstruction":{"maneuver":"ARRIVE","instructions":"Done"}}]}
                  ]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                response, new GeoPoint(46.55, 15.64), new GeoPoint(46.56, 15.65)
        );

        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).instruction()).isEqualTo("Done");
    }

    // ── parsePolyline edge cases ──────────────────────────────────────────────

    @Test
    void parsePolylineGeoJsonWithAllNonArrayCoordsReturnsFallback() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);

        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":["not","arrays"]}},
                  "legs":[]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(response, origin, dest);

        assertThat(result.polyline()).containsExactly(origin, dest);
    }

    @Test
    void parsePolylineEncodedPolylineWithBlankValueReturnsFallback() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "ENCODED_POLYLINE");
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);

        String response = """
                {"routes":[{"polyline":{"encodedPolyline":""},"legs":[]}]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(response, origin, dest);

        assertThat(result.polyline()).containsExactly(origin, dest);
    }

    @Test
    void parseOptionalPolylineEncodedPolylineWithBlankReturnsEmptyStepPolyline() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "ENCODED_POLYLINE");
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);

        String response = """
                {"routes":[{
                  "polyline":{"encodedPolyline":"_p~iF~ps|U"},
                  "legs":[{"steps":[{
                    "distanceMeters":50,
                    "duration":"5s",
                    "polyline":{"encodedPolyline":""},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"Blank step polyline"}
                  }]}]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(response, origin, dest);

        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).instruction()).isEqualTo("Blank step polyline");
    }

    // ── durationSeconds edge cases ────────────────────────────────────────────

    @Test
    void parseRouteDetailsWithDurationWithoutSuffixParsedAsSeconds() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                  "legs":[{"steps":[{
                    "distanceMeters":100,
                    "duration":"10",
                    "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"No suffix"}
                  }]}]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                response, new GeoPoint(46.55, 15.64), new GeoPoint(46.56, 15.65)
        );

        assertThat(result.steps().get(0).durationSeconds()).isEqualTo(10);
    }

    @Test
    void parseRouteDetailsWithInvalidDurationStringReturnsZero() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                  "legs":[{"steps":[{
                    "distanceMeters":100,
                    "duration":"invalid",
                    "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"Bad duration"}
                  }]}]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                response, new GeoPoint(46.55, 15.64), new GeoPoint(46.56, 15.65)
        );

        assertThat(result.steps().get(0).durationSeconds()).isZero();
    }

    // ── null apiKey and blank response branches ──────────────────────────────

    @Test
    void fetchRouteDetailsWithNullApiKeyReturnsFallback() {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "apiKey", null);
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);

        GoogleRoutesService.RouteDetails result = service.fetchRouteDetails(origin, dest, EdgeType.WALK);

        assertThat(result.polyline()).containsExactly(origin, dest);
        assertThat(result.steps()).isNull();
    }

    @Test
    void fetchRouteDetailsWithApiKeyReturnsFallbackOnBlankResponse() {
        GoogleRoutesService service = serviceWithClient(Mono.just("   "));
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);

        GoogleRoutesService.RouteDetails result = service.fetchRouteDetails(origin, dest, EdgeType.WALK);

        assertThat(result.polyline()).containsExactly(origin, dest);
        assertThat(result.steps()).isNull();
    }

    @Test
    void fetchCoupledRouteDetailsWithNullApiKeyReturnsFallback() {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "apiKey", null);
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);
        GeoPoint c = new GeoPoint(46.57, 15.66);

        List<GoogleRoutesService.RouteDetails> result = service.fetchRouteDetails(List.of(a, b, c), EdgeType.WALK);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).polyline()).containsExactly(a, b);
    }

    @Test
    void fetchCoupledRouteDetailsWithApiKeyReturnsFallbackOnBlankResponse() {
        GoogleRoutesService service = serviceWithClient(Mono.just("   "));
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);
        GeoPoint c = new GeoPoint(46.57, 15.66);

        List<GoogleRoutesService.RouteDetails> result = service.fetchRouteDetails(List.of(a, b, c), EdgeType.WALK);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).polyline()).containsExactly(a, b);
    }

    @Test
    void fetchCoupledRouteDetailsWithNullPointsReturnsEmpty() {
        GoogleRoutesService service = new GoogleRoutesService();
        assertThat(service.fetchRouteDetails((List<GeoPoint>) null, EdgeType.WALK)).isEmpty();
    }

    // ── routes/legs not-array JSON structure guards ───────────────────────────

    @Test
    void parseRouteDetailsWhenRoutesNodeIsNotArrayReturnsFallback() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                "{\"routes\":\"notAnArray\"}", origin, dest
        );

        assertThat(result.polyline()).containsExactly(origin, dest);
    }

    @Test
    void parseCoupledRouteDetailsWhenRoutesNodeIsNotArrayReturnsFallback() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);

        List<GoogleRoutesService.RouteDetails> result = service.parseRouteDetails(
                "{\"routes\":\"notAnArray\"}", List.of(a, b)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).polyline()).containsExactly(a, b);
    }

    @Test
    void parseCoupledRouteDetailsWhenLegsNodeIsNotArrayReturnsFallback() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);

        List<GoogleRoutesService.RouteDetails> result = service.parseRouteDetails(
                "{\"routes\":[{\"legs\":\"notAnArray\"}]}", List.of(a, b)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).polyline()).containsExactly(a, b);
    }

    @Test
    void parseLegRouteDetailsWhenLegIsJsonNullReturnsFallback() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);
        GeoPoint c = new GeoPoint(46.57, 15.66);

        List<GoogleRoutesService.RouteDetails> result = service.parseRouteDetails(
                "{\"routes\":[{\"legs\":[null,{\"steps\":[]}]}]}", List.of(a, b, c)
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).polyline()).containsExactly(a, b);
        assertThat(result.get(0).steps()).isNull();
        assertThat(result.get(1).steps()).isEmpty();
    }

    @Test
    void parseLegRouteDetailsWhenStepsNodeIsNotArrayReturnsEmptyStepsList() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint a = new GeoPoint(46.55, 15.64);
        GeoPoint b = new GeoPoint(46.56, 15.65);

        List<GoogleRoutesService.RouteDetails> result = service.parseRouteDetails(
                "{\"routes\":[{\"legs\":[{\"steps\":\"notAnArray\"}]}]}", List.of(a, b)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).polyline()).containsExactly(a, b);
        assertThat(result.get(0).steps()).isEmpty();
    }

    // ── encoded polyline null-field edge cases ────────────────────────────────

    @Test
    void parsePolylineEncodedWithMissingEncodedFieldReturnsFallback() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "ENCODED_POLYLINE");
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);

        String response = """
                {"routes":[{"polyline":{},"legs":[]}]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(response, origin, dest);

        assertThat(result.polyline()).containsExactly(origin, dest);
    }

    @Test
    void parseOptionalPolylineEncodedWithMissingFieldReturnsEmptyList() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "ENCODED_POLYLINE");
        GeoPoint origin = new GeoPoint(38.5, -120.2);
        GeoPoint dest = new GeoPoint(40.7, -120.95);

        String response = """
                {"routes":[{
                  "polyline":{"encodedPolyline":"_p~iF~ps|U"},
                  "legs":[{"steps":[{
                    "distanceMeters":50,
                    "duration":"5s",
                    "polyline":{},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"Missing encoded field"}
                  }]}]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(response, origin, dest);

        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).instruction()).isEqualTo("Missing encoded field");
    }

    // ── GeoJSON coordinate size < 2 ──────────────────────────────────────────

    @Test
    void parseRouteDetailsIgnoresGeoJsonCoordsWithFewerThanTwoElements() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");
        GeoPoint origin = new GeoPoint(46.55, 15.64);
        GeoPoint dest = new GeoPoint(46.56, 15.65);

        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64],[15.65,46.56]]}},
                  "legs":[]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(response, origin, dest);

        assertThat(result.polyline()).hasSize(1);
        assertThat(result.polyline().get(0)).isEqualTo(new GeoPoint(46.56, 15.65));
    }

    @Test
    void parseOptionalPolylineIgnoresGeoJsonCoordsWithFewerThanTwoElements() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                  "legs":[{"steps":[{
                    "distanceMeters":50,
                    "duration":"5s",
                    "polyline":{"geoJsonLinestring":{"coordinates":[[15.64],[15.65,46.56]]}},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"Partial coord"}
                  }]}]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                response, new GeoPoint(46.55, 15.64), new GeoPoint(46.56, 15.65)
        );

        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).instruction()).isEqualTo("Partial coord");
    }

    // ── durationSeconds blank-string branches ─────────────────────────────────

    @Test
    void parseRouteDetailsWithBlankDurationAndBlankStaticDurationReturnsZero() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                  "legs":[{"steps":[{
                    "distanceMeters":50,
                    "duration":"",
                    "staticDuration":"",
                    "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"Blank durations"}
                  }]}]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                response, new GeoPoint(46.55, 15.64), new GeoPoint(46.56, 15.65)
        );

        assertThat(result.steps().get(0).durationSeconds()).isZero();
        assertThat(result.steps().get(0).instruction()).isEqualTo("Blank durations");
    }

    @Test
    void parseRouteDetailsWithBlankDurationFallsBackToStaticDuration() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                  "legs":[{"steps":[{
                    "distanceMeters":50,
                    "duration":"",
                    "staticDuration":"12s",
                    "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56]]}},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"Blank then static"}
                  }]}]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                response, new GeoPoint(46.55, 15.64), new GeoPoint(46.56, 15.65)
        );

        assertThat(result.steps().get(0).durationSeconds()).isEqualTo(12);
    }

    // ── matchStepPolylineRange out-of-order correction ────────────────────────

    @Test
    void parseRouteDetailsStepPolylineReversedTriggersOutOfOrderCorrection() throws Exception {
        GoogleRoutesService service = new GoogleRoutesService();
        ReflectionTestUtils.setField(service, "polylineEncoding", "GEO_JSON_LINESTRING");

        // Route polyline: A→B→C; step goes B→A (reversed) — triggers endIndex < startIndex path
        String response = """
                {"routes":[{
                  "polyline":{"geoJsonLinestring":{"coordinates":[[15.64,46.55],[15.65,46.56],[15.66,46.57]]}},
                  "legs":[{"steps":[{
                    "distanceMeters":100,
                    "duration":"10s",
                    "polyline":{"geoJsonLinestring":{"coordinates":[[15.65,46.56],[15.64,46.55]]}},
                    "navigationInstruction":{"maneuver":"DEPART","instructions":"Reversed"}
                  }]}]
                }]}
                """;

        GoogleRoutesService.RouteDetails result = service.parseRouteDetails(
                response, new GeoPoint(46.55, 15.64), new GeoPoint(46.57, 15.66)
        );

        assertThat(result.steps()).hasSize(1);
        int start = result.steps().get(0).startPolylineIndex();
        int end = result.steps().get(0).endPolylineIndex();
        assertThat(end).isGreaterThanOrEqualTo(start);
    }
}
