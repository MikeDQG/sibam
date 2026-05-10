package com.sibam.integration.marprom;

import com.sibam.dto.marprom.lines.MarpromLinesResponse;
import com.sibam.dto.marprom.routes.MarpromRoutesResponseDto;
import com.sibam.dto.marprom.schedules.MarpromScheduleResponse;
import com.sibam.dto.marprom.stops.MarpromStopsResponse;
import com.sibam.dto.marprom.trips.MarpromTripsResponseDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
    public Mono<MarpromStopsResponse> getAllStops() {
        return this.webClient.get()
                .uri("/GetAllStopPoints")
                .retrieve()
                .bodyToMono(MarpromStopsResponse.class);
    }

    /**
     * Pridobi linije za specifičen datum (ekvivalent GetLines)
     */
    public Mono<MarpromLinesResponse> getLines(String date) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/GetLines")
                        .queryParam("Date", date)
                        .build())
                .retrieve()
                .bodyToMono(MarpromLinesResponse.class);
    }

    // TODO: odstrani, ko bos datum handlal na visjem nivoju
    public Mono<MarpromLinesResponse> getLines() {
        return this.getLines(getFormattedDateHelper());
    }

    /**
     * Pridobi trase dolocene linije za specifičen datum (ekvivalent GetRoutes)
     */
    public Mono<MarpromRoutesResponseDto> getRoutes(int lineId, String date) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/GetRoutes")
                        .queryParam("Date", date)
                        .queryParam("IncludeShape", true)
                        .queryParam("lineId", lineId)
                        .build())
                .retrieve()
                .bodyToMono(MarpromRoutesResponseDto.class);
    }

    // TODO: odstrani, ko bos datum handlal na visjem nivoju
    public Mono<MarpromRoutesResponseDto> getRoutes(int lineId) {
        return this.getRoutes(lineId, getFormattedDateHelper());
    }

    /**
     * Pridobi trase dolocene linije za specifičen datum (ekvivalent GetRoutes)
     */
    public Mono<MarpromScheduleResponse> getStopScheduleForLine(int lineId, String date) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/GetStopPointSheduleForLine")
                        .queryParam("Date", date)
                        .queryParam("lineId", lineId)
                        .build())
                .retrieve()
                .bodyToMono(MarpromScheduleResponse.class);
    }

    // TODO: odstrani, ko bos datum handlal na visjem nivoju
    public Mono<MarpromScheduleResponse> getStopScheduleForLine(int lineId) {
        return this.getStopScheduleForLine(lineId, getFormattedDateHelper());
    }

    /**
     * Pridobi trase dolocene linije za specifičen datum (ekvivalent GetRoutes)
     */
    public Mono<MarpromTripsResponseDto> getTrips(int lineId, String date) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/GetTrips")
                        .queryParam("IncludeShape", true)
                        .queryParam("Date", date)
                        .queryParam("lineId", lineId)
                        .build())
                .retrieve()
                .bodyToMono(MarpromTripsResponseDto.class);
    }

    // TODO: odstrani, ko bos datum handlal na visjem nivoju
    public Mono<MarpromTripsResponseDto> getTrips(int lineId) {
        return this.getTrips(lineId, getFormattedDateHelper());
    }
}