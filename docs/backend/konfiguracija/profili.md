# Spring profili

Backend podpira tri profile. Aktiven profil se nastavi z `SPRING_PROFILES_ACTIVE`. Privzeto (`application.properties`) je aktiven profil `local`.

---

## `local`

Namenjen lokalnemu razvoju. Vrednosti se berejo iz `backend/src/main/resources/application-local.properties`, ki je v `.gitignore` in **ni sledena v gitu**.

| Lastnost | Vrednost |
| --- | --- |
| Baza | lokalna ali razvojna Supabase instanca (hardkodirano v `application-local.properties`) |
| Firebase | `firebase-service-account.json` iz `resources/` (gitignored) |
| Schedulerji za zajem | izklopljeni |
| Gradnja grafa | aktivna ob zagonu |
| ML model reload | izkloplj (`schedulers.reload-ml-models.on` privzeto `false`) |
| CORS | `http://localhost:5173`, `http://localhost:4173` |

---

## `prod`

Produkcijski profil za Railway deployment, ki streže API, routing in ML napovedi.

| Lastnost | Vrednost |
| --- | --- |
| Schedulerji za zajem | izklopljeni (`false`) — zajem teče na ločenem CI deploymentu |
| ML model reload | vklopljen (`schedulers.reload-ml-models.on=true`) ob 3:30 UTC |
| Gradnja grafa | aktivna ob zagonu |
| Supabase cache | vklopljen (`supabase.cache.enabled=true`) |
| CORS | `${ALLOWED_ORIGINS}` iz env |
| HikariCP | maks. 3 povezave (optimizirano za Supabase Transaction Pooler) |

---

## `ci`

Profil za Railway deployment, ki **samo** zbira podatke za ML pipeline. Ne gradi grafa in ne streže API zahtevkov.

| Lastnost | Vrednost |
| --- | --- |
| Schedulerji za zajem | vklopljeni (bikes, weather, bus) |
| Gradnja grafa | preskočena (`app.ml-only-scheduled-ingestion=true`) |
| ML model reload | izklopljen |
| Google Routes API | ni potreben |
| Firebase | ni potreben |
| CORS / `ALLOWED_ORIGINS` | ni potreben |

`app.ml-only-scheduled-ingestion=true` pove aplikaciji, naj preskoči gradnjo grafa in vse z njim povezane inicializacije ter zažene samo schedulerje za zajem podatkov.

---

## Skupne nastavitve (`application.properties`)

Relevantne nastavitve, ki veljajo za vse profile, razen če jih profil prepiše:

| Nastavitev | Vrednost | Opis |
| --- | --- | --- |
| `server.port` | `${PORT:8080}` | Railway samodejno nastavi `PORT` |
| `spring.jpa.hibernate.ddl-auto` | `none` | Hibernate ne upravlja sheme — migracije so ročne |
| `schedules.refresh.cron` | `0 0 3 * * *` | Osvežitev grafa vsak dan ob 3:00 po Ljubljani |
| `routing.max-access-distance-meters` | `3000` | Maksimalna razdalja od točke do grafa |
| `routing.alternatives.max-routes` | `3` | Največ alternativnih poti v odgovoru |
