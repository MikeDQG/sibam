# Operativni postopek ML pipeline-a

Ta dokument opisuje praktičen vrstni red zagona ML pipeline-a in najpogostejše kontrole.

## Zahteve

Python odvisnosti so zapisane v [requirements.txt](../../ml/requirements.txt).

Potrebne okoljske spremenljivke:

| Spremenljivka          | Namen                                               |
| ---------------------- | --------------------------------------------------- |
| `SUPABASE_DB_URL`      | PostgreSQL povezava za izvoz staging tabel v bronze |
| `SUPABASE_URL`         | Supabase projekt URL                                |
| `SUPABASE_SERVICE_KEY` | Supabase service key za Storage operacije           |

Za backend zajem podatkov so potrebne še backend spremenljivke za zunanje API-je in bazo, opisane v [varnost-backend.md](../security/varnost-backend.md).

## Avtomatizirani nočni zagon

ML pipeline se samodejno izvaja vsako noč prek GitHub Actions workflowa [`.github/workflows/ml-retrain.yml`](../../.github/workflows/ml-retrain.yml).

**Urnik:** vsak dan ob 1:00 UTC (3:00 po poletnem slovenskem času, CEST).

Workflow izvede korake 2–5 spodaj (export → silver → trening) v tem vrstnem redu. Workflow je mogoče sprožiti tudi ročno prek GitHub Actions (`workflow_dispatch`).

Korak 6 (nalaganje novih modelov v backend) je avtomatiziran — backend ob 3:30 UTC samodejno prenese nove modele iz Supabase Storage brez ponovnega zagona.

Za nadzor poslednjega zagona: Actions → ML Retrain.

## Vrstni red zagona

Ukazi se izvajajo iz korena repozitorija `sibam`.

### 1. Zbiranje podatkov v backendu

Backend schedulerji morajo biti vklopljeni:

```properties
schedulers.fetch-bike-ingestion.on=true
schedulers.fetch-weather-ingestion.on=true
schedulers.fetch-bus-ingestion.on=true
```

Schedulerji zbirajo podatke v staging tabele. Delujejo med 05:00 in 23:00.

### 2. Izvoz staging tabel v bronze

```bash
python ml/export_to_lake.py
```

Rezultat:

- `bronze/bikes/stations.parquet`
- `bronze/bikes/snapshots_<date>.parquet`
- `bronze/weather/weather_<date>.parquet`
- `bronze/buses/trips_<date>.parquet`
- `bronze/buses/delays_<date>.parquet`

Po uspehu se starejši staging podatki počistijo. Če skripta odpove, staging podatkov ne počisti.

### 3. Transformacija v silver

```bash
python ml/transform_to_silver.py
```

Rezultat:

- `silver/bikes/latest.parquet`
- `silver/buses/latest.parquet`

### 4. Trening MBajk modelov

```bash
python ml/train_bikes.py
```

Rezultat v `gold/models`:

- `model_bikes.onnx`
- `model_stands.onnx`
- `model_available_bike.onnx`
- `model_available_stand.onnx`

### 5. Trening modela zamud avtobusov

```bash
python ml/train_buses.py
```

Rezultat v `gold/models`:

- `model_bus_delay.onnx`

### 6. Nalaganje novih modelov v backend

Backend ob **3:30 UTC** samodejno prenese nove ONNX modele iz `gold/models` v Supabase Storage in jih zamenja v pomnilniku brez ponovnega zagona. Ponovni zagon backenda ni potreben.

## Kontrole po zagonu

Preden se modeli štejejo za uporabne, preveri:

- da `bronze` vsebuje nove Parquet datoteke za bikes, weather in buses;
- da `silver/bikes/latest.parquet` in `silver/buses/latest.parquet` obstajata;
- da trening izpiše razumne metrike (`MAE` za regresije, `ROC-AUC` za klasifikacije);
- da so novi ONNX modeli v `gold/models`;
- da backend ob okoli 3:30 UTC v logu izpiše `ML models reloaded successfully`;
- da endpointa `POST /predict/bikes` in `POST /api/bus-delay/predict` vrneta odgovor.

## Znane omejitve

- Predikcijska endpointa trenutno nimata Bean Validation anotacij za vhodne DTO-je.
- MBajk trening uporablja naključno delitev podatkov, zato lahko pri časovnih vzorcih pride do optimistične ocene uspešnosti.
- Bus model uporablja preprost `Ridge` model; za kompleksnejše vzorce zamud bo morda potreben naprednejši model.
- `generate_synthetic.py` lahko ustvari sintetične zimske avtobusne podatke, vendar trenutni `train_buses.py` neposredno bere `silver/buses/latest.parquet`; sintetične podatke je treba vključiti premišljeno, da ne popačijo evalvacije.
- `direction` je odvisen od pravilnosti `stop_direction_mapping.json`. Če mapping manjka, backend za avtobusno zamudo vrne `0`.

## Odprte izboljšave

- Shranjevati verzije modelov namesto prepisovanja `latest` oziroma istih ONNX imen.
- Dodati metapodatke modela: datum treninga, število vrstic, metrika, git commit.
- Dodati validacijo vhodnih značilk na predikcijskih endpointih.
- Dodati monitoring drift-a podatkov in primerjavo metrik med verzijami modelov.
