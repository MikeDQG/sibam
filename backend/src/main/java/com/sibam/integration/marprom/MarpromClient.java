package com.sibam.integration.marprom;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class MarpromClient {

    private final WebClient webClient;
    private static final String BASE_URL = "https://marprom-proxy.derp.si/OBA"; //

    public MarpromClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
    }

    /**
     * Pridobi vsa postajališča (ekvivalent GetAllStopPoints v Pythonu)
     */
    public Mono<Map> getAllStops() {
        return this.webClient.get()
                .uri("/GetAllStopPoints")
                .retrieve()
                .bodyToMono(Map.class);
    }

    /**
     * Pridobi linije za specifičen datum (ekvivalent GetLines)
     */
    public Mono<Map> getLines(String date) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/GetLines")
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .bodyToMono(Map.class);
    }

    /**
     * TODO: change this to public when finished
     */
    private Mono<Map> getLines() {
        String date = "2026-05-06";
        return this.getLines(date);
    }
}