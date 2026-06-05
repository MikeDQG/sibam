package com.sibam.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FirebaseAuthFilterTest {

    private FirebaseAuthFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new FirebaseAuthFilter(new String[]{"http://localhost:3000", "https://app.example.com"});
        chain = mock(FilterChain.class);
    }

    @Test
    void requestWithNoAuthHeaderPassesThroughToChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void protectedRequestWithNoAuthHeaderReturns401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void requestWithNonBearerAuthHeaderPassesThroughToChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void protectedRequestWithNonBearerAuthHeaderReturns401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/locations/" + java.util.UUID.randomUUID());
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void publicRequestWithNoAuthHeaderStillPassesThroughToChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/compute");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void corsPreflightForProtectedPathPassesThroughToChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void protectedRequestWithNoAuthHeaderAndAllowedOriginSetsCorsHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Origin", "https://app.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://app.example.com");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
    }

    @Test
    void validBearerTokenForProtectedRequestSetsIdentityAndPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        FirebaseToken decodedToken = mock(FirebaseToken.class);

        when(decodedToken.getUid()).thenReturn("uid-1");
        when(decodedToken.getEmail()).thenReturn("test@example.com");
        when(decodedToken.getName()).thenReturn("Test User");
        when(firebaseAuth.verifyIdToken("valid-token")).thenReturn(decodedToken);

        try (var firebaseAuthStatic = mockStatic(FirebaseAuth.class)) {
            firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

            filter.doFilterInternal(request, response, chain);
        }

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute("uid")).isEqualTo("uid-1");
        assertThat(request.getAttribute("email")).isEqualTo("test@example.com");
        assertThat(request.getAttribute("fullName")).isEqualTo("Test User");
    }

    @Test
    void invalidBearerTokenReturns401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-real-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void invalidTokenWithAllowedOriginSetsCorsCorsHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-real-token");
        request.addHeader("Origin", "http://localhost:3000");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:3000");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
    }

    @Test
    void invalidTokenWithUnknownOriginDoesNotSetCorsHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-real-token");
        request.addHeader("Origin", "http://evil.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }
}
