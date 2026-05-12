package com.sibam.service;

import com.sibam.integration.weather.WeatherClient;
import com.sibam.model.WeatherSnapshot;
import com.sibam.repository.WeatherSnapshotRepository;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;

@Service
public class WeatherDataService {

    private final WeatherClient weatherClient;
    private final WeatherSnapshotRepository weatherSnapshotRepository;

    public WeatherDataService(WeatherClient weatherClient, WeatherSnapshotRepository weatherSnapshotRepository) {
        this.weatherClient = weatherClient;
        this.weatherSnapshotRepository = weatherSnapshotRepository;
    }

    public void ingestWeatherData(OffsetDateTime fetchedAt) {
        weatherClient.getCurrentWeather()
                .publishOn(Schedulers.boundedElastic())
                .subscribe(response -> {
                    WeatherSnapshot snapshot = new WeatherSnapshot();
                    snapshot.setTemperature(response.main().temp());
                    snapshot.setFeelsLike(response.main().feelsLike());
                    snapshot.setHumidity(response.main().humidity());
                    snapshot.setWindSpeed(response.wind().speed());
                    snapshot.setRain(response.rain() != null ? response.rain().oneHour() : null);
                    snapshot.setCondition(response.weather() != null && !response.weather().isEmpty()
                            ? response.weather().get(0).main()
                            : null);
                    snapshot.setRecordedAt(fetchedAt);
                    weatherSnapshotRepository.save(snapshot);
                });
    }
}
