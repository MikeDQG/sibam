package com.sibam.integration.marprom;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class MarpromClient {

    private final WebClient webClient;
    private static final String BASE_URL = "https://marprom-proxy.derp.si/OBA"; //

    public MarpromClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
    }

    private String getFormattedDateHelper() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return today.format(formatter);
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
                        .queryParam("Date", date)
                        .build())
                .retrieve()
                .bodyToMono(Map.class);
    }

    // TODO: odstrani, ko bos datum handlal na visjem nivoju
    public Mono<Map> getLines() {
        return this.getLines(getFormattedDateHelper());
    }

    /**
     * Pridobi trase dolocene linije za specifičen datum (ekvivalent GetRoutes)
     */
    public Mono<Map> getRoutes(int lineId, String date) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/GetRoutes")
                        .queryParam("Date", date)
                        .queryParam("IncludeShape", true)
                        .queryParam("lineId", lineId)
                        .build())
                .retrieve()
                .bodyToMono(Map.class);
    }

    // TODO: odstrani, ko bos datum handlal na visjem nivoju
    public Mono<Map> getRoutes(int lineId) {
        return this.getRoutes(lineId, getFormattedDateHelper());
    }

    /**
     * Pridobi trase dolocene linije za specifičen datum (ekvivalent GetRoutes)
     */
    public Mono<Map> getStopScheduleForLine(int lineId, String date) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/GetStopPointSheduleForLine")
                        .queryParam("Date", date)
                        .queryParam("lineId", lineId)
                        .build())
                .retrieve()
                .bodyToMono(Map.class);
    }

    // TODO: odstrani, ko bos datum handlal na visjem nivoju
    public Mono<Map> getStopScheduleForLine(int lineId) {
        return this.getStopScheduleForLine(lineId, getFormattedDateHelper());
    }
}