package com.sibam.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

@Component
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private final Set<String> allowedOrigins;

    public FirebaseAuthFilter(@Value("${allowed.origins}") String[] origins) {
        this.allowedOrigins = Set.of(origins);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);

                String fullName = decodedToken.getName();
                if (fullName == null || fullName.isBlank()) {
                    fullName = request.getHeader("X-Full-Name");
                }
                request.setAttribute("fullName", fullName);

                request.setAttribute("uid", decodedToken.getUid());
                request.setAttribute("email", decodedToken.getEmail());
            } catch (Exception e) {
                String origin = request.getHeader("Origin");
                if (origin != null && allowedOrigins.contains(origin)) {
                    response.setHeader("Access-Control-Allow-Origin", origin);
                    response.setHeader("Access-Control-Allow-Credentials", "true");
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

        }

        filterChain.doFilter(request, response);
    }
}