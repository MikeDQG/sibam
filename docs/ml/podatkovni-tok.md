# Podatkovni tok ML sistema

Ta dokument opisuje tok podatkov od zunanjih virov do produkcijske napovedi.

## Pregled

```text
Zunanji API-ji
  -> Java schedulerji
  -> PostgreSQL staging tabele
  -> export_to_lake.py
  -> Supabase Storage bronze
  -> transform_to_silver.py
  -> Supabase Storage silver
  -> train_bikes.py / train_buses.py
  -> Supabase Storage gold/models
  -> Java backend ONNX inference
```

Obstoječi diagram je shranjen tudi kot slika: [ml_pipeline_diagram.drawio.png](../diagrami/ml_pipeline_diagram.drawio.png).

## 1. Zajem podatkov v backendu

Zajem izvaja `SchedulerService` v backendu. Schedulerji delujejo samo med 05:00 in 23:00 ter samo, če so v konfiguraciji omogočeni.

| Scheduler metoda          | Frekvenca      | Vir            | Servis               | Shramba                                   |
| ------------------------- | -------------- | -------------- | -------------------- | ----------------------------------------- |
| `fetchBikeIngestion()`    | vsakih 5 minut | MBajk API      | `MBajkDataService`   | `bike_stations`, `bike_station_snapshots` |
| `fetchWeatherIngestion()` | vsako uro      | OpenWeatherMap | `WeatherDataService` | `weather_snapshots`                       |
| `fetchBusIngestion()`     | vsako minuto   | GTFS-RT        | `GTFSRTDataService`  | `trip_snapshots`, `stop_delay_snapshots`  |

Konfiguracijske zastavice:

- `schedulers.fetch-bike-ingestion.on`
- `schedulers.fetch-weather-ingestion.on`
- `schedulers.fetch-bus-ingestion.on`

V `application-ci.properties` so schedulerji vklopljeni za zbiranje podatkov, v produkcijski konfiguraciji so trenutno nastavljeni na `false`.

## 2. Staging tabele v PostgreSQL

Backend najprej shrani podatke v PostgreSQL tabele. Te tabele so kratkoročna staging shramba za ML:

| Tabela                   | Namen                                                              |
| ------------------------ | ------------------------------------------------------------------ |
| `bike_stations`          | Statični podatki MBajk postaj: številka, ime, lokacija, kapaciteta |
| `bike_station_snapshots` | Časovni posnetki razpoložljivih koles in stojal                    |
| `weather_snapshots`      | Časovni posnetki vremena v Mariboru                                |
| `trip_snapshots`         | Posnetki vozil/voženj iz GTFS-RT                                   |
| `stop_delay_snapshots`   | Zamude po postajah za posamezen trip snapshot                      |

## 3. Izvoz v bronze plast

Skripta: [export_to_lake.py](../../ml/export_to_lake.py)

Skripta prebere staging tabele iz PostgreSQL in jih izvozi kot Parquet datoteke v Supabase Storage bucket `bronze`.

| Vir                      | Bronze pot                              |
| ------------------------ | --------------------------------------- |
| `bike_stations`          | `bronze/bikes/stations.parquet`         |
| `bike_station_snapshots` | `bronze/bikes/snapshots_<date>.parquet` |
| `weather_snapshots`      | `bronze/weather/weather_<date>.parquet` |
| `trip_snapshots`         | `bronze/buses/trips_<date>.parquet`     |
| `stop_delay_snapshots`   | `bronze/buses/delays_<date>.parquet`    |

Po uspešnem izvozu skripta počisti staging podatke, starejše od treh dni. Če izvoz spodleti, staging tabel ne počisti.

## 4. Transformacija v silver plast

Skripta: [transform_to_silver.py](../../ml/transform_to_silver.py)

Silver plast združi surove izvoze v učne datasete.

### Kolesa

Za MBajk se združijo:

- snapshoti postaj iz `bronze/bikes/snapshots_*`;
- statični podatki postaj iz `bronze/bikes/stations*`;
- najbližji vremenski snapshot iz `bronze/weather/weather_*`.

Rezultat:

```text
silver/bikes/latest.parquet
```

Pomembna polja:

- `number`, `name`, `latitude`, `longitude`, `capacity`;
- `bikes`, `stands`, `mechanical_bikes`, `electrical_bikes`;
- `recorded_at`, `recorded_at_local`;
- `temperature`, `wind_speed`, `rain`, `condition`.

### Avtobusi

Za avtobuse se združijo:

- trip snapshoti iz `bronze/buses/trips_*`;
- delay snapshoti iz `bronze/buses/delays_*`;
- najbližji vremenski snapshot iz `bronze/weather/weather_*`.

Rezultat:

```text
silver/buses/latest.parquet
```

Pomembna polja:

- `route_id`, `stop_sequence`, `delay_seconds`;
- `bearing`, `current_stop_id`;
- `recorded_at`, `recorded_at_local`;
- `temperature`, `wind_speed`, `rain`.

## 5. Trening in gold plast

Trening skripte berejo iz `silver` in shranijo ONNX modele v `gold/models`.

| Skripta          | Vhod                                                          | Izhod                                                                                              |
| ---------------- | ------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| `train_bikes.py` | `silver/bikes/latest.parquet`                                 | `model_bikes.onnx`, `model_stands.onnx`, `model_available_bike.onnx`, `model_available_stand.onnx` |
| `train_buses.py` | `silver/buses/latest.parquet` + `stop_direction_mapping.json` | `model_bus_delay.onnx`                                                                             |

## 6. Produkcijska inferenca v backendu

Backend pri zagonu naloži modele iz Supabase Storage:

- `BikePredictionService` naloži štiri MBajk modele iz `gold/models`;
- `BusDelayPredictionService` naloži `model_bus_delay.onnx` in `stop_direction_mapping.json`.

Inferenca je na voljo prek endpointov:

| Endpoint                      | Namen                                                        |
| ----------------------------- | ------------------------------------------------------------ |
| `POST /predict/bikes`         | napoved števila koles/stojal in verjetnosti razpoložljivosti |
| `POST /api/bus-delay/predict` | napoved zamude avtobusa v sekundah                           |
