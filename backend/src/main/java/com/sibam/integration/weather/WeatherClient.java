package com.sibam.integration.weather;

import com.sibam.dto.weather.WeatherResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class WeatherClient {

    private final WebClient webClient;
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final double MARIBOR_LAT = 46.5547;
    private static final double MARIBOR_LON = 15.6459;

    @Value("${openweathermap.api.key}")
    private String apiKey;

    public WeatherClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
    }

    public Mono<WeatherResponseDto> getCurrentWeather() {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("lat", MARIBOR_LAT)
                        .queryParam("lon", MARIBOR_LON)
                        .queryParam("appid", apiKey)
                        .queryParam("units", "metric")
                        .build())
                .retrieve()
                .bodyToMono(WeatherResponseDto.class);
    }
}
