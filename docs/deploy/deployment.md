# Deployment

## Pregled

| Komponenta | Platforma | Profil |
| --- | --- | --- |
| Backend — API + routing + ML | Railway | `prod` |
| Backend — ML zajem podatkov | Railway | `ci` |
| Frontend | Firebase Hosting | — |

---

## Backend — Dockerfile

Razred: [Dockerfile](../../../backend/Dockerfile)

Dvostopenjski build:

1. **Build stage** — Maven 3.9.9 + Eclipse Temurin JDK 21; odvisnosti se prenesejo ločeno od izvorne kode za boljše Docker layer caching; testi so preskočeni (`-DskipTests`)
2. **Runtime stage** — Eclipse Temurin JRE 21; samo JAR datoteka brez build orodij

Port: `8080` (Railway samodejno nastavi `PORT` env spremenljivko, ki jo `server.port=${PORT:8080}` prebere).

---

## Railway — Backend prod (`SPRING_PROFILES_ACTIVE=prod`)

Streže API, routing graph in ML napovedi. Naloži modele ob zagonu, jih osveži vsako noč ob 3:30 UTC.

### Okoljske spremenljivke

| Spremenljivka | Namen |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | JDBC URL Supabase PostgreSQL (Transaction Pooler, port 5432) |
| `DB_USERNAME` | Uporabniško ime baze |
| `DB_PASSWORD` | Geslo baze |
| `DB_CLASS_NAME` | `org.postgresql.Driver` |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Vsebina Firebase service account JSON |
| `SUPABASE_URL` | URL Supabase projekta |
| `SUPABASE_SERVICE_KEY` | Supabase service role ključ |
| `SUPABASE_SERVICE_ROLE_KEY` | Alternativno ime za service role ključ |
| `MBAJK_API_KEY` | JCDecaux API ključ za MBajk |
| `OPEN_WEATHERMAP_API_KEY` | OpenWeatherMap API ključ |
| `ROUTES_GOOGLE_API_KEY` | Google Routes API ključ |
| `ROUTES_GOOGLE_POLYLINE_ENCODING` | `GEO_JSON_LINESTRING` |
| `ROUTES_GOOGLE_FIELD_MASK` | `routes.legs.steps,routes.duration,routes.distanceMeters,routes.polyline.geoJsonLinestring` |
| `ALLOWED_ORIGINS` | URL produkcijskega frontenda (npr. `https://sibam.web.app`) |

---

## Railway — Backend CI (`SPRING_PROFILES_ACTIVE=ci`)

Zbira podatke za ML pipeline. Ne gradi grafa, ne streže HTTP zahtevkov — teče samo schedulerje za zajem.

### Okoljske spremenljivke

| Spremenljivka | Namen |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `ci` |
| `DB_URL` | Ista baza kot prod |
| `DB_USERNAME` | Uporabniško ime baze |
| `DB_PASSWORD` | Geslo baze |
| `DB_CLASS_NAME` | `org.postgresql.Driver` |
| `MBAJK_API_KEY` | JCDecaux API ključ za MBajk |
| `OPEN_WEATHERMAP_API_KEY` | OpenWeatherMap API ključ |
| `SUPABASE_URL` | URL Supabase projekta |
| `SUPABASE_SERVICE_KEY` | Supabase service role ključ |

Firebase, Google Routes in `ALLOWED_ORIGINS` niso potrebni — CI profil ne inicializira Firebase filtra, ne gradi grafa in nima CORS konfiguracije za zunanje odjemalce.

---

## Firebase — Frontend

Frontend je zgrajen z Vite in gostovan na Firebase Hosting. Deploy se izvede samodejno prek GitHub Actions:

| Workflow | Sprožilec | Akcija |
| --- | --- | --- |
| `firebase-hosting-merge.yml` | merge v `main` | Deploy na produkcijski kanal |
| `firebase-hosting-pull-request.yml` | odprt PR | Deploy na preview kanal |

---

## Sekvenca zagona prod backenda

```
Railway zažene Docker container
  → JVM naloži Spring kontekst
  → @PostConstruct: BikePredictionService.loadModels()    — prenese 4 ONNX modele iz Supabase Storage
  → @PostConstruct: BusDelayPredictionService.load()      — prenese model + naloži stop_direction_mapping.json
  → ApplicationReadyEvent: GraphBootstrap.init()          — zgradi ali naloži routing graf
  → aplikacija začne sprejemati zahtevke na :8080
  → vsak dan ob 3:30 UTC: SchedulerService.reloadMlModels()
```
