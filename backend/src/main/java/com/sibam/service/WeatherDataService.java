package com.sibam.service;

import com.sibam.integration.weather.WeatherClient;
import com.sibam.persistence.WeatherSnapshot;
import com.sibam.repository.WeatherSnapshotRepository;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;

/**
 * Servis za zajem trenutnega vremena v Mariboru.
 *
 * Podatke pridobi iz OpenWeatherMap prek WeatherClient in jih shrani kot
 * časovne posnetke za ML ter vremensko prilagajanje routinga.
 */
@Service
public class WeatherDataService {

    private final WeatherClient weatherClient;
    private final WeatherSnapshotRepository weatherSnapshotRepository;

    public WeatherDataService(WeatherClient weatherClient, WeatherSnapshotRepository weatherSnapshotRepository) {
        this.weatherClient = weatherClient;
        this.weatherSnapshotRepository = weatherSnapshotRepository;
    }

    /**
     * Asinhrono pridobi trenutno vreme in shrani posnetek v bazo.
     *
     * Metoda uporablja reactive Mono iz WeatherClient, preklopi na boundedElastic
     * scheduler in s subscribe sproži zapis v WeatherSnapshotRepository.
     *
     * @param fetchedAt čas zajema, ki se shrani ob vremenskem posnetku
     */
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
