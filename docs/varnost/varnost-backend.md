# Varnost backenda

Ta dokument opisuje implementirano varnost backenda aplikacije šibaM.

---

## Pregled varnostne arhitekture

Backend je implementiran v Spring Boot brez Spring Security. Varnost temelji na dveh glavnih komponentah:

- **`FirebaseAuthFilter`** - servlet filter, ki preverja Firebase ID token iz `Authorization` glave, kadar je glava prisotna v formatu `Bearer <token>`;
- **servisna plast** (`SavedLocationService`, `SavedPathService`) - preverja lastništvo podatkov s primerjavo Firebase UID-ja iz tokena z vrednostjo `firebaseUid` v bazi.

Tok zahtevka:

```
Frontend (JWT token)
    → FirebaseAuthFilter (preveri token pri Firebase, nastavi uid/email/fullName)
    → Controller (prebere @RequestAttribute)
    → Service (preveri lastništvo)
    → Baza (vrne samo podatke lastnika)
```

Backend ne hrani gesel in ne izdaja lastnih žetonov. Avtentikacija je delegirana Firebase Authentication.

---

## Vir identitete: Firebase Authentication

Backend ne preverja gesel neposredno. Frontend se avtenticira pri Firebase (Google Sign-In, e-pošta/geslo ali drug Firebase ponudnik) in prejme kratkotrajen Firebase ID token (JWT). Ta token pošlje backendu z zahtevki v `Authorization` glavi. Backend token preveri prek Firebase Admin SDK in ne izdaja ali hrani lastnih žetonov.

---

## Inicializacija Firebase Admin SDK

Razred: [FirebaseConfig.java](../../backend/src/main/java/com/sibam/config/FirebaseConfig.java)

Firebase Admin SDK se inicializira ob zagonu aplikacije z metodo `@PostConstruct initializeFirebase()`. Prioritetni vrstni red nalaganja service account podatkov:

1. **Okoljska spremenljivka `FIREBASE_SERVICE_ACCOUNT_JSON`** - vsebina JSON datoteke service accounta kot niz. To je predvidena metoda v produkciji.
2. **Fallback: `firebase-service-account.json`** - datoteka v `backend/src/main/resources/`. Namenjena je samo lokalnemu razvoju; **ne sme biti dodana v git**.

Če nobena od možnosti ni na voljo, aplikacija izpiše napako in se zažene brez Firebase inicializacije. Zahtevki z Bearer tokenom bodo nato zavrnjeni s 401, ker `FirebaseAuth.getInstance()` oziroma `verifyIdToken(...)` ne more uspešno preveriti tokena.

**Produkcija:** service account JSON se nastavi kot okoljska spremenljivka na deploy platformi (npr. Render, Railway, Fly.io). Vrednost vsebuje zasebni ključ Google Cloud service accounta s pravicami za Firebase Auth. Nikoli ne sme biti javno izpostavljena ali commitana v repozitorij.

**Trenutno stanje v delovnem drevesu:** `backend/src/main/resources/firebase-service-account.json` lokalno obstaja, vendar je pot dodana v `.gitignore` in datoteka ni sledena v gitu. Vseeno jo je treba obravnavati kot skrivnost: ne pošiljati v PR, ne lepiti v dokumentacijo in ob morebitni izpostavitvi rotirati ključ.

---

## Preverjanje Bearer tokena

Razred: [FirebaseAuthFilter.java](../../backend/src/main/java/com/sibam/config/FirebaseAuthFilter.java)

Filter razširja `OncePerRequestFilter` in se izvede enkrat za vsak zahtevek.

**Tok preverjanja:**

1. Prebere glavo `Authorization`.
2. Če gre za zaščiteno pot (`/api/users`, `/api/locations`, `/api/paths`) in glave ni ali ne začne z `Bearer `, filter vrne `401 Unauthorized`.
3. Če gre za javno pot in glave ni ali ne začne z `Bearer `, zahtevek preide naprej brez nastavljenih atributov.
4. Če glave je in začne z `Bearer `, token ekstrahira (podniz po poziciji 7).
5. Pokliče `FirebaseAuth.getInstance().verifyIdToken(token)` - Firebase Admin SDK validira podpis, rok veljavnosti, izdajatelja in pripadajoči Firebase projekt.
6. Ob uspešni verifikaciji nastavi request atribute in zahtevek prepusti naprej.
7. Ob neuspešni verifikaciji (neveljaven podpis, potek veljavnosti, napačen format) vrne `401 Unauthorized` in zaustavi verigo filtrov.

**Opomba:** `OPTIONS` zahtevki so izvzeti iz eksplicitne auth kontrole v filtru, da lahko CORS preflight zahtevke obdela Spring CORS konfiguracija.

---

## Nastavljanje identitete v request context

Ob uspešni verifikaciji filter nastavi naslednje request atribute:

| Atribut    | Vir                                              | Opis                                                                             |
| ---------- | ------------------------------------------------ | -------------------------------------------------------------------------------- |
| `uid`      | `decodedToken.getUid()`                          | Firebase UID - primarni identifikator uporabnika                                 |
| `email`    | `decodedToken.getEmail()`                        | E-poštni naslov iz Firebase                                                      |
| `fullName` | `decodedToken.getName()` oz. glava `X-Full-Name` | Polno ime; če Firebase token ne vsebuje imena, se prebere iz glave `X-Full-Name` |

Kontrolerji te vrednosti berejo prek `@RequestAttribute`:

```java
@GetMapping("/me")
public ResponseEntity<User> getMe(@RequestAttribute("uid") String uid) { ... }
```

**Varnostna opomba:** `uid` in `email` izhajata iz preverjenega Firebase tokena. `X-Full-Name` pa je navadna HTTP glava in je uporabna samo kot nezaupan prikazni fallback, kadar ime ni prisotno v Firebase tokenu. Ne sme se uporabljati za avtorizacijo ali varnostne odločitve.

---

## Sinhronizacija uporabnika z backend bazo

Razred: [UserController.java](../../backend/src/main/java/com/sibam/api/UserController.java), [UserService.java](../../backend/src/main/java/com/sibam/service/UserService.java)

Backend vzdržuje lastno tabelo `users` z dodatnimi podatki o uporabniku. Firebase UID je primarni ključ za identifikacijo.

### `POST /api/users/me` - prijava ali registracija

Ob prvem klicu ustvari nov zapis v bazi, ob ponovnih klicih vrne obstoječega. Kliče `UserService.getOrCreateUser(uid, email, fullName)`:

1. Poišče uporabnika po `firebaseUid`.
2. Če ga ni, ustvari nov `User` z atributi: `firebaseUid`, `email`, `fullName`, `createdAt`.
3. Vrne uporabnikov zapis.

### `GET /api/users/me` - pridobitev podatkov prijavljenega uporabnika

Poišče in vrne uporabnika po `firebaseUid` iz tokena. Če uporabnik ne obstaja v bazi (ni se še prijavil prek `POST /me`), vrne 404.

### Shranjevani podatki v entiteti `User`

| Polje         | Opis                              |
| ------------- | --------------------------------- |
| `id`          | UUID - interni ID v bazi          |
| `firebaseUid` | Firebase UID (unikatno, not null) |
| `email`       | E-poštni naslov                   |
| `fullName`    | Polno ime                         |
| `createdAt`   | Čas prvega vpisa                  |

---

## Zaščiteni endpointi

Naslednji endpointi so namenjeni samo prijavljenim uporabnikom, ker filter zahteva veljaven Firebase Bearer token, kontrolerji berejo `@RequestAttribute("uid")`, servisna plast pa preverja lastništvo. Ob manjkajočem, napačnem ali neveljavnem Bearer tokenu filter vrne 401.

| Endpoint                      | Metoda | Opis                       | Identiteta v zahtevku       |
| ----------------------------- | ------ | -------------------------- | --------------------------- |
| `/api/users/me`               | POST   | Prijava / registracija     | `uid`, `email`, `fullName`  |
| `/api/users/me`               | GET    | Pridobi podatke o sebi     | `uid`                       |
| `/api/locations/{userId}`     | GET    | Pridobi shranjene lokacije | `uid` (preverja lastništvo) |
| `/api/locations`              | POST   | Shrani lokacijo            | `uid` (preverja lastništvo) |
| `/api/locations/{locationId}` | PUT    | Posodobi lokacijo          | `uid` (preverja lastništvo) |
| `/api/locations/{locationId}` | DELETE | Izbriše lokacijo           | `uid` (preverja lastništvo) |
| `/api/paths/{userId}`         | GET    | Pridobi shranjene poti     | `uid` (preverja lastništvo) |
| `/api/paths`                  | POST   | Shrani pot                 | `uid` (preverja lastništvo) |
| `/api/paths/{pathId}`         | DELETE | Izbriše pot                | `uid` (preverja lastništvo) |

---

## Javni endpointi in nezaščitene funkcionalnosti

Naslednji endpointi so dostopni brez avtentikacije, ker ne zahtevajo `@RequestAttribute("uid")`. Če zahtevek nima `Authorization` glave, ga `FirebaseAuthFilter` prepusti naprej.

| Endpoint                 | Metoda | Opis                         | Razlog za javni dostop                                 |
| ------------------------ | ------ | ---------------------------- | ------------------------------------------------------ |
| `/compute`               | GET    | Izračun prometnih alternativ | Storitev je namenjena vsem, brez uporabniških podatkov |
| `/predict/bikes`         | POST   | Napoved zasedenih koles      | Statistična predikcija, brez vezave na uporabnika      |
| `/api/bus-delay/predict` | POST   | Napoved zamude avtobusa      | Statistična predikcija, brez vezave na uporabnika      |
| `/simple-api/test`       | GET    | Test endpoint                | Razvojni artefakt - **v produkciji odstraniti**        |
| `/actuator/health`       | GET    | Health check                 | Izpostavljen prek Spring Boot Actuator konfiguracije   |

Javni endpointi sprejemajo nepreverjene vnose (koordinate, čas, ID postaje). Obliko parametrov delno preverja Spring, vsebinska validacija (obseg koordinat, veljavnost postaje, smiselnost časa) pa je omejena oziroma ni sistematično implementirana (gl. razdelek Validacija).

`/compute` sprejme tudi opcijski parameter `userId`, vendar ga trenutna implementacija ne uporablja za avtorizacijo ali branje uporabniških podatkov. To je treba ohraniti dokumentirano, ker lahko parameter daje napačen vtis, da endpoint deluje v kontekstu prijavljenega uporabnika.

---

## Avtorizacija in preverjanje lastništva podatkov

Samo avtentikacija ni dovolj - backend preverja, ali je avtenticiran uporabnik tudi lastnik zahtevanega vira.

### Mehanizem preverjanja

Firebase `uid` iz tokena se primerja z `firebaseUid`, shranjenim v entiteti `User` v bazi. Vsaka shranjena lokacija in pot ima referenco na lastnika.

### `SavedLocationService`

Razred: [SavedLocationService.java](../../backend/src/main/java/com/sibam/service/SavedLocationService.java)

| Operacija                              | Preverjanje                                                   |
| -------------------------------------- | ------------------------------------------------------------- |
| `getLocationsForUser(userId, uid)`     | Poišče `User` po `userId`, primerja `user.firebaseUid == uid` |
| `saveLocation(userId, ..., uid)`       | Enako - preverja, da `userId` pripada klicatelju              |
| `updateLocation(locationId, ..., uid)` | Poišče lokacijo, primerja `location.user.firebaseUid == uid`  |
| `deleteLocation(locationId, uid)`      | Enako                                                         |

### `SavedPathService`

Razred: [SavedPathService.java](../../backend/src/main/java/com/sibam/service/SavedPathService.java)

Identičen vzorec preverjanja kot `SavedLocationService`:

| Operacija                      | Preverjanje                             |
| ------------------------------ | --------------------------------------- |
| `getPathsForUser(userId, uid)` | Primerja `user.firebaseUid == uid`      |
| `savePath(userId, ..., uid)`   | Enako                                   |
| `deletePath(pathId, uid)`      | Primerja `path.user.firebaseUid == uid` |

Ob neujemanju vse metode vržejo `ResponseStatusException(HttpStatus.FORBIDDEN)`.

---

## Odgovori ob nepooblaščenem ali prepovedanem dostopu

| Status                        | Razlog                                                           | Povzročitelj                                                              |
| ----------------------------- | ---------------------------------------------------------------- | ------------------------------------------------------------------------- |
| **400 Bad Request**           | Napačen format parametrov ali telesa zahtevka                    | Spring MVC / Jackson                                                      |
| **401 Unauthorized**          | Token manjka na zaščiteni poti, ni v Bearer formatu, je potekel ali ga Firebase ne prepozna | `FirebaseAuthFilter`                                      |
| **403 Forbidden**             | Token je veljaven, a uporabnik ni lastnik zahtevanega vira       | `SavedLocationService` / `SavedPathService` via `ResponseStatusException` |
| **404 Not Found**             | Uporabnik, lokacija ali pot ne obstaja v bazi                    | `UserController`, `SavedLocationService`, `SavedPathService`              |

**Opomba:** servisna plast za shranjene lokacije in poti zdaj manjkajoče uporabnike ali vire pretvori v `ResponseStatusException(HttpStatus.NOT_FOUND)`, namesto da bi nenadzorovano vrgla `NoSuchElementException`.

---

## CORS politika

Razred: [WebConfig.java](../../backend/src/main/java/com/sibam/config/WebConfig.java)

CORS je konfiguriran globalno za vse poti (`/**`):

```java
registry.addMapping("/**")
        .allowedOrigins(allowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
```

| Nastavitev         | Vrednost                                                    |
| ------------------ | ----------------------------------------------------------- |
| Dovoljene metode   | `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`                   |
| Dovoljene glave    | Vse (`*`)                                                   |
| `allowCredentials` | `true` - dovoljeni so credentialed CORS zahtevki            |
| Dovoljeni origin-i | Brani iz `allowed.origins` (razlikuje se po profilu)        |

### Razlika po profilu

| Profil             | `allowed.origins`                                                 |
| ------------------ | ----------------------------------------------------------------- |
| `local` / privzeto | `http://localhost:5173`, `http://localhost:4173`                  |
| `prod`             | `${ALLOWED_ORIGINS}` - okoljska spremenljivka na deploy platformi |

`FirebaseAuthFilter` prav tako bere `allowed.origins` in ob zavrnitvi zahtevka (401) nastavi CORS glavo `Access-Control-Allow-Origin`, samo če je `Origin` glave zahtevka v dovoljeni množici. S tem prepreči, da bi brskalniki neznanih origin-ov prejeli CORS odgovor.

---

## Upravljanje skrivnosti in okoljskih spremenljivk

Backend bere vse občutljive vrednosti iz okoljskih spremenljivk. Nobena skrivnost ne sme biti dodana v git.

| Spremenljivka                   | Namen                                        | Profil   |
| ------------------------------- | -------------------------------------------- | -------- |
| `DB_URL`                        | JDBC URL podatkovne baze                     | prod, ci |
| `DB_USERNAME`                   | Uporabniško ime za bazo                      | prod, ci |
| `DB_PASSWORD`                   | Geslo za bazo                                | prod, ci |
| `DB_CLASS_NAME`                 | JDBC gonilnik (npr. `org.postgresql.Driver`) | prod, ci |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Firebase service account JSON                | prod, ci |
| `MBAJK_API_KEY`                 | API ključ za MBajk kolesarsko storitev       | prod, ci |
| `OPEN_WEATHERMAP_API_KEY`       | API ključ za vremenske podatke               | prod, ci |
| `SUPABASE_URL`                  | URL Supabase projekta                        | prod     |
| `SUPABASE_SERVICE_KEY`          | Supabase service role ključ                  | prod, ci |
| `SUPABASE_SERVICE_ROLE_KEY`     | Alternativno ime za service role ključ       | prod     |
| `ROUTES_GOOGLE_API_KEY`         | Google Routes API ključ                      | prod     |
| `ROUTES_GOOGLE_POLYLINE_ENCODING` | Način kodiranja polilinij                | prod     |
| `ROUTES_GOOGLE_FIELD_MASK`      | Field mask za Google Routes odgovor          | prod     |
| `ALLOWED_ORIGINS`               | CORS dovoljeni origin-i, ločeni z vejico     | prod     |

**Pravila:**

- Ključi se ne smejo logirati (niti naključno prek Spring debug logov).
- Ključi se ne smejo commitati v repozitorij, niti v `application-local.properties`.
- V CI/CD se nastavijo kot zaščitene okoljske spremenljivke (secrets).

> **Varnostna opomba:** `backend/src/main/resources/application-local.properties` trenutno lokalno vsebuje hardkodirane vrednosti za razvoj. Datoteka je v `.gitignore` in ni sledena v gitu, vendar mora ostati lokalna. Če so bile njene vrednosti ali service account datoteka kadarkoli javno izpostavljene, je treba ključe rotirati in odstraniti iz zgodovine.

---

## Dostop do podatkovne baze

Backend dostopa do PostgreSQL baze prek Hibernate/JPA z JDBC gonilnikom. Baza je gostovana pri Supabase; v produkciji se uporablja Supabase Transaction Pooler (port 5432) z HikariCP connection poolom (maks. 3 povezave).

Vse tabele z uporabniškimi podatki so vezane na entiteto `User` prek tujega ključa. Servisna plast zagotavlja, da uporabnik dostopa samo do lastnih podatkov:

- `SavedLocation.user` → `User`
- `SavedPath.user` → `User`

Direktnega SQL-ja v pregledani backend kodi ni; backend uporablja JPA repozitorije in Hibernate poizvedbe, kar pomembno zmanjša tveganje za SQL injection.

---

## Validacija vhodnih podatkov

### Implementirana validacija

| Endpoint / DTO              | Validacija                                                                                          |
| --------------------------- | --------------------------------------------------------------------------------------------------- |
| `SavedLocationRequest`      | Brez `@Valid` anotacij - Spring samo preveri, da JSON ustreza tipu (`UUID`, `Double`, `String`)     |
| `SavedPathRequest`          | Enako                                                                                               |
| `BikePredictionRequest`     | Brez validacij - vrednosti se posredujejo naprej ML modelu                                          |
| `BusDelayPredictionRequest` | Brez validacij                                                                                      |
| `/compute` parametri        | Spring preverja obvezne parametre (`required = true` je privzeto), format časa/datuma, `double` tip |

### Priporočene dopolnitve

- `SavedLocationRequest`: validacija obsega koordinat (lat med -90 in 90, lon med -180 in 180), nepraznost `name`, največja dolžina polj.
- `SavedPathRequest`: validacija, da `name` ni prazen, da `journey` vsebuje vsaj en korak.
- `/compute`: validacija, da koordinate padejo v servisno območje (Maribor), preden se pokliče računanje grafa.
- Predikcijski endpointi: obseg vrednosti (ura 0–23, dan 0–6, temperatura v razumnem obsegu).

---

## Napake, izjeme in razkrivanje podatkov

Backend uporablja `ResponseStatusException` za eksplicitne napake (401 v filtru, 403 pri tujih virih, 404 pri manjkajočih uporabnikih/lokacijah/poteh). Za nepredvidene scenarije se zanaša na Spring Boot privzeto obravnavo napak.

Spring Boot v produkciji privzeto ne razkriva stack trace-ov v JSON odgovorih (`server.error.include-stacktrace=never`, če ni prepisano). Odgovor vsebuje tipične Spring Boot atribute, kot so `timestamp`, `status`, `error` in `path`, brez stack trace-a.

**Priporočilo:** za še bolj enotne JSON napake je smiselno dodati globalni `@ControllerAdvice`, vendar trenutna varnostno pomembna stanja že vračajo namenske HTTP statuse.

---

## Logiranje in varovanje občutljivih podatkov

Backend prek SLF4J / Logback logira samo splošne informacije o delovanju.

**Ne sme se logirati:**

- Firebase ID tokenov (JWT vrednosti)
- `FIREBASE_SERVICE_ACCOUNT_JSON` ali katerekoli vrednosti service accounta
- API ključev (MBajk, OpenWeatherMap, Google Routes, Supabase)
- Gesel za bazo
- Katerekoli vrednosti iz `Authorization` glave zahtevka

`FirebaseConfig` logira samo uspeh/neuspeh inicializacije Firebase brez vsebine kredencial. `ComputePathController` trenutno logira izhodiščne koordinate zahtevka (`origin lat/lon`). Koordinate lahko predstavljajo lokacijski oziroma osebni podatek, zato je priporočljivo v produkciji loge zmanjšati, zaokrožiti ali odstraniti.

V produkciji je log nivo nastavljen privzeto; v lokalnem razvoju (`application-local.properties`) je `logging.level.com.sibam=DEBUG`.

---

## Integracije z zunanjimi storitvami

### Firebase Authentication

- **Namen:** verifikacija ID tokenov
- **Ključ:** service account JSON (`FIREBASE_SERVICE_ACCOUNT_JSON`)
- **Tok podatkov:** backend pošilja token Googlovim strežnikom za verifikacijo; Firebase ne prejme uporabniških podatkov iz baze.

### Supabase

- **Namen:** shranjevanje grafa in predpomnjenje podatkov; PostgreSQL baza podatkov
- **Ključi:** `SUPABASE_URL`, `SUPABASE_SERVICE_KEY`
- **Tok podatkov:** backend piše/bere podatke iz Supabase. Service role ključ daje širok strežniški dostop - mora ostati tajen in nikoli ne sme priti v frontend.

### Google Routes API

- **Namen:** izračun kolesarskih poti (zadnji kilometer)
- **Ključ:** `ROUTES_GOOGLE_API_KEY`
- **Tok podatkov:** backend pošilja koordinate Googlu in prejme geometrijo poti. API ključ mora biti omejen na Routes API in, kjer je mogoče, na IP naslove produkcijskega strežnika oziroma drugo strežniško omejitev.

### MBajk

- **Namen:** pridobivanje podatkov o kolesarskih postajah v realnem času
- **Ključ:** `MBAJK_API_KEY`
- **Tok podatkov:** backend periodično kliče MBajk API, podatki se shranijo v bazo za ML in routing.

### OpenWeatherMap

- **Namen:** vremenski podatki za prilagoditev uteži v routerju
- **Ključ:** `OPEN_WEATHERMAP_API_KEY`
- **Tok podatkov:** backend kliče API z lokacijo (Maribor); zunanji ponudnik ne prejme podatkov o uporabnikih.

### Marprom / GTFS-RT

- **Namen:** podatki o avtobusnih linijah in zamudah
- **Ključ:** ni ločenega ključa (javni GTFS feed)
- **Tok podatkov:** backend bere javno dostopne GTFS datoteke; ni prenosa zasebnih podatkov.

---

## Produkcijska konfiguracija

### `application.properties` (osnova)

- Skupne nastavitve za vse profile
- `allowed.origins` = `http://localhost:5173,http://localhost:4173` (prepisano v prod)
- `spring.profiles.active=local` (prepisano na strežniku z env `SPRING_PROFILES_ACTIVE=prod`)
- `schedules.refresh.enabled=false` (scheduler izklopljen v osnovi)

### `application-prod.properties`

- Vse skrivnosti brane iz okoljskih spremenljivk
- `allowed.origins=${ALLOWED_ORIGINS}` - produkcijska domena frontenda
- `supabase.cache.enabled=true`
- `schedules.refresh.enabled=true` - scheduler aktiviran
- HikariCP nastavitve optimizirane za Supabase Transaction Pooler

### `application-ci.properties`

- Enaka konfiguracija baze kot prod (okoljske spremenljivke)
- Schedulerji za ingestijo podatkov aktivirani (`schedulers.fetch-bike-ingestion.on=true` itd.)
- `app.ml-only-scheduled-ingestion=true` - aplikacija v CI načinu zažene samo schedulerje za ML, brez gradnje grafa

### `application-local.properties`

- Hardkodirane vrednosti za lokalni razvoj
- Vsebuje lokalne razvojne vrednosti in je vključena v `.gitignore`; ne sme biti sledena v gitu

---

## Testiranje varnosti

### Obstoječi testi

Razred: [FirebaseAuthFilterTest.java](../../backend/src/test/java/com/sibam/config/FirebaseAuthFilterTest.java)

| Test                                                 | Scenarij                                 | Pričakovani rezultat                    |
| ---------------------------------------------------- | ---------------------------------------- | --------------------------------------- |
| `requestWithNoAuthHeaderPassesThroughToChain`        | Brez `Authorization` glave               | Zahtevek preide, status 200             |
| `protectedRequestWithNoAuthHeaderReturns401`         | Zaščitena pot brez `Authorization` glave | Status 401, filter chain ni poklican    |
| `requestWithNonBearerAuthHeaderPassesThroughToChain` | `Authorization: Basic ...`               | Zahtevek preide, status 200             |
| `protectedRequestWithNonBearerAuthHeaderReturns401`  | Zaščitena pot z `Basic` auth glavo       | Status 401, filter chain ni poklican    |
| `publicRequestWithNoAuthHeaderStillPassesThroughToChain` | Javna pot brez auth glave             | Zahtevek preide, status 200             |
| `corsPreflightForProtectedPathPassesThroughToChain`  | `OPTIONS` na zaščiteno pot               | Zahtevek preide do CORS obdelave        |
| `validBearerTokenForProtectedRequestSetsIdentityAndPassesThrough` | Veljaven Bearer token na zaščiteni poti | Nastavi `uid`, `email`, `fullName`; zahtevek preide |
| `invalidBearerTokenReturns401`                       | `Authorization: Bearer neveljaven-token` | Status 401, filter chain ni poklican    |
| `invalidTokenWithAllowedOriginSetsCorsCorsHeaders`   | Neveljaven token + dovoljen `Origin`     | Status 401, CORS glave nastavljene      |
| `invalidTokenWithUnknownOriginDoesNotSetCorsHeaders` | Neveljaven token + neznan `Origin`       | Status 401, CORS glave niso nastavljene |

Razred: [SavedLocationControllerTest.java](../../backend/src/test/java/com/sibam/api/SavedLocationControllerTest.java)

| Test                                  | Scenarij                                         | Pričakovani rezultat |
| ------------------------------------- | ------------------------------------------------ | -------------------- |
| `getLocationsForForeignUserReturns403` | Veljaven uporabniški kontekst za tuj `userId`   | Status 403           |
| `deleteForeignLocationReturns403`      | Veljaven uporabniški kontekst za tujo lokacijo  | Status 403           |

Razred: [SavedPathControllerTest.java](../../backend/src/test/java/com/sibam/api/SavedPathControllerTest.java)

| Test                           | Scenarij                                       | Pričakovani rezultat |
| ------------------------------ | ---------------------------------------------- | -------------------- |
| `deleteForeignPathReturns403`  | Veljaven uporabniški kontekst za tujo pot      | Status 403           |

Razred: [WebConfigTest.java](../../backend/src/test/java/com/sibam/config/WebConfigTest.java)

| Test                                             | Scenarij                                  | Pričakovani rezultat       |
| ------------------------------------------------ | ----------------------------------------- | -------------------------- |
| `corsPreflightWithAllowedOriginReturnsCorsHeaders` | CORS preflight z dovoljenim origin-om   | Status 200 + CORS glave    |
| `corsPreflightWithUnknownOriginIsForbidden`        | CORS preflight z nedovoljenim origin-om | Status 403                 |

### Scenariji, ki jih je smiselno še dodati

- Integracijski test celotne poti `Authorization: Bearer <valid Firebase ID token>` -> `FirebaseAuthFilter` -> kontroler -> servis. Trenutni unit testi Firebase SDK mockajo, kar je primerno za hitro regresijsko testiranje, ne nadomešča pa end-to-end preverjanja s testnim Firebase projektom ali emulatorjem.

---

## Znane omejitve in odprta varnostna vprašanja

### Implementirano

- Verifikacija Firebase ID tokenov pri vsaki zahtevi
- Ekspliciten `401 Unauthorized` za zaščitene poti brez Bearer tokena
- Preverjanje lastništva pri vseh operacijah na lokacijah in poteh
- `404 Not Found` za manjkajoče uporabnike, lokacije in poti v servisni plasti
- CORS omejen na konfigurirane origin-e
- Ločena konfiguracija za lokalno in produkcijsko okolje
- Skrivnosti brane iz okoljskih spremenljivk v produkciji

### Odprta tveganja in priporočena izboljšanja

| Tveganje                                     | Opis                                                                           | Priporočilo                                                                          |
| -------------------------------------------- | ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------ |
| Lokalni kredenciali na disku                 | `application-local.properties` in `firebase-service-account.json` lokalno vsebujeta skrivnosti; trenutno sta ignorirana in nista sledena v gitu | Ohraniti v `.gitignore`, ne vključiti v PR; ob izpostavitvi rotirati vrednosti       |
| Brez validacije vhodnih podatkov             | DTO-ji nimajo `@Valid` anotacij; koordinate in drugi parametri niso preverjeni | Dodati Bean Validation (`@NotNull`, `@Min`, `@Max`, `@Size`)                         |
| Test endpoint v produkciji                   | `GET /simple-api/test` je javen in dostopen brez avtentikacije                 | Razred `SimpleController` odstraniti pred produkcijsko namestitvijo                  |
| Brez rate limitinga                          | Ni omejevanja ponovnih klicev na nobenem endpointu                             | Dodati rate limiting (npr. Bucket4j ali infrastrukturni reverse proxy)               |
| `allowedHeaders("*")`                        | CORS dovoljuje vse glave, vključno s potencialno nevarnimi                     | Omejiti na konkretno potrebne glave (`Authorization`, `Content-Type`, `X-Full-Name`) |
| Logiranje koordinat                          | `/compute` logira izhodiščne koordinate, ki lahko pomenijo lokacijski podatek  | V produkciji odstraniti ali anonimizirati koordinatne loge                           |
| `X-Full-Name` kot fallback                   | Ime iz HTTP glave ni kriptografsko preverjeno                                  | Uporabljati samo za prikaz, ne za avtorizacijo; prednost ima ime iz Firebase tokena   |
