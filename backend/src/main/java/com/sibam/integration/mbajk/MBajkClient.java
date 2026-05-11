package com.sibam.integration.mbajk;

import com.sibam.dto.mbajk.BikeStopDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class MBajkClient {

    private final WebClient webClient;
    private static final String BASE_URL = "https://api.jcdecaux.com/vls/v3/stations?apiKey=frifk0jbxfefqqniqez09tw4jvk37wyf823b5j1i&contract=maribor";

    public MBajkClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
    }

    public Mono<List<BikeStopDto>> getAllBikes() {
        return this.webClient.get().uri(BASE_URL).retrieve().bodyToMono(new ParameterizedTypeReference<List<BikeStopDto>>() {});
    }
}
