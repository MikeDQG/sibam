package com.sibam.integration.mbajk;

import com.sibam.dto.mbajk.BikeStopDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive odjemalec za JCDecaux MBajk API.
 *
 * Vir uporablja pogodbo maribor in vrača trenutno stanje vseh kolesarskih
 * postaj, ki se pozneje shrani v staging tabele in vgradi v graf.
 */
@Component
public class MBajkClient {

    private final WebClient webClient;
    private static final String BASE_URL = "https://api.jcdecaux.com/vls/v3/stations";

    @Value("${mbajk.api.key}")
    private String apiKey;

    public MBajkClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
    }

    /**
     * Pridobi vse MBajk postaje iz JCDecaux API-ja.
     *
     * @return Mono s seznamom postaj in trenutno razpoložljivostjo
     */
    public Mono<List<BikeStopDto>> getAllBikes() {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("apiKey", apiKey)
                        .queryParam("contract", "maribor")
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<BikeStopDto>>() {});
    }
}
