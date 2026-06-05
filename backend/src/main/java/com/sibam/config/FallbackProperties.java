package com.sibam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class FallbackProperties {

    private final Duration realtimeMaxAge;

    public FallbackProperties(
            @Value("${fallback.realtime-max-age-minutes:60}") long realtimeMaxAgeMinutes
    ) {
        this.realtimeMaxAge = Duration.ofMinutes(Math.max(1, realtimeMaxAgeMinutes));
    }

    public Duration realtimeMaxAge() {
        return realtimeMaxAge;
    }
}
