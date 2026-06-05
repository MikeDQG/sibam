# Zajem podatkov — Vreme

Backend vsako uro pridobi trenutne vremenske podatke za Maribor in jih shrani v bazo. Vremenski posnetek se uporablja na dveh mestih: kot penalizacija v routing grafu (dežuje → višji strošek hoje in kolesarjenja) in kot vhodna značilka ML modelov za napoved zamud in razpoložljivosti koles.

Za opis vpliva vremena na routing glej [`graf-in-algoritem/algoritem-usmerjanja.md`](../graf-in-algoritem/algoritem-usmerjanja.md).

---

## `WeatherClient`

Razred: [WeatherClient.java](../../../backend/src/main/java/com/sibam/integration/weather/WeatherClient.java)

Reaktiven HTTP odjemalec (Spring WebClient), ki kliče OpenWeatherMap API.

**Endpoint:** `GET https://api.openweathermap.org/data/2.5/weather?lat=46.5547&lon=15.6459&appid=…&units=metric`

**Ključ:** `OPEN_WEATHERMAP_API_KEY` iz okoljske spremenljivke (`openweathermap.api.key`)

Koordinate so hardkodirane na center Maribora (`46.5547, 15.6459`) — celotna aplikacija pokriva samo to mesto.

Parameter `units=metric` zagotavlja, da so temperature v °C in hitrost vetra v m/s.

**Vrne:** reaktiven `Mono<WeatherResponseDto>` z naslednjimi polji:

| Polje                 | DTO pot           | Opis                     |
| --------------------- | ----------------- | ------------------------ |
| Temperatura           | `main.temp`       | °C                       |
| Občutena temperatura  | `main.feelsLike`  | °C                       |
| Vlažnost              | `main.humidity`   | %                        |
| Hitrost vetra         | `wind.speed`      | m/s                      |
| Padavine (zadnja ura) | `rain.oneHour`    | mm; `null` če ni padavin |
| Opis stanja           | `weather[0].main` | npr. `"Rain"`, `"Clear"` |

---

## `WeatherDataService`

Razred: [WeatherDataService.java](../../../backend/src/main/java/com/sibam/service/WeatherDataService.java)

### `ingestWeatherData(OffsetDateTime fetchedAt)`

Pokliče `WeatherClient.getCurrentWeather()` in asinhrono (na `Schedulers.boundedElastic()`) shrani `WeatherSnapshot` v bazo. En klic ustvari en zapis.

Preslikava vrednosti:

```
response.main().temp()        → snapshot.temperature
response.main().feelsLike()   → snapshot.feelsLike
response.main().humidity()    → snapshot.humidity
response.wind().speed()       → snapshot.windSpeed
response.rain()?.oneHour()    → snapshot.rain  (null če ni padavin)
response.weather()[0].main()  → snapshot.condition  (null če seznam prazen)
fetchedAt                     → snapshot.recordedAt
```

Zadnji `WeatherSnapshot` se bere prek `WeatherSnapshotRepository.findFirstByOrderByRecordedAtDesc()` — uporablja ga routing ob vsaki zahtevi za izračun poti in ML inference.

---

## Scheduler

Scheduler: [SchedulerService.java](../../../backend/src/main/java/com/sibam/scheduler/SchedulerService.java), metoda `fetchWeatherIngestion()`

| Lastnost         | Vrednost                                 |
| ---------------- | ---------------------------------------- |
| Interval         | vsako uro (`fixedRate = 3 600 000 ms`)   |
| Deluje med       | 05:00–23:00 (po lokalnem času Ljubljana) |
| Config zastavica | `schedulers.fetch-weather-ingestion.on`  |

Scheduler zunaj operativnih ur ne kliče servisa. Izjeme so ujete in zapisane v log.
