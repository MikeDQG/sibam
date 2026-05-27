# Testiranje backenda

## Pregled

Backend uporablja **JUnit 5 + Mockito + AssertJ** za unit teste, ki jih zagotavlja
`spring-boot-starter-test`. Za zagon testov ni potrebnih dodatnih odvisnosti.

Testi so čisti unit testi — Spring context se nikoli ne naloži, podatkovna baza se ne zažene.
Zunanje odvisnosti (repozitoriji, API odjemalci, Supabase) so zamenjane z Mockito mocki.

---

## Zagon testov

**IntelliJ:** Desni klik na `src/test/java/com/sibam` → _Run 'Tests in sibam'_

**Maven:**

```bash
./mvnw test
```

**CI:** Samodejno se izvede ob vsakem push in pull requestu na `main` vejo preko
GitHub Actions `Main CI`.

---

## Testni razredi

### Čista logika — brez mockov

| Razred | Pokriva |
| --- | --- |
| `integration/gtfsRT/GTFSRTMapperTest` | Preslikava GTFS-RT protobuf → `Trip` — vsa polja vozila, null `TripUpdate`, postajne posodobitve z zamudami, postajne posodobitve brez prihodnega dogodka (zamuda privzeto 0), rezervni tripId iz `TripUpdate` |

### Storitve — repozitoriji in odjemalci zamenjani z Mockito

| Razred                             | Pokriva                                                                                                                                                                                                                                                                                                                                                                                              |
| ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `service/UserServiceTest`          | `getOrCreateUser` vrne obstoječega uporabnika brez klica save; ustvari in shrani novega uporabnika z vsemi polji; `getUserByFirebaseUid` pravilno delegira                                                                                                                                                                                                                                           |
| `service/SavedLocationServiceTest` | Shranjevanje, posodobitev, brisanje, pridobivanje — vse s pravilnim preslikavanjem polj; vse štiri operacije vrnejo `403 Forbidden`, ko se Firebase UID ne ujema z lastnikom                                                                                                                                                                                                                         |
| `service/MBajkDataServiceTest`     | `getBikeStationVaos` preslika postajo z zadnjim posnetkom; vrne ničelno razpoložljivost, ko posnetka ni; vrne prazen seznam, ko postaj ni                                                                                                                                                                                                                                                            |
| `service/GTFSRTDataServiceTest`    | `ingestRealtimeTrips` shrani `TripEntity` s pravilnimi polji; shrani en `StopDelayEntity` za vsako postajno posodobitev s pravilno FK povezavo; ne shrani entitet zamud, ko so postajne posodobitve null; prazen feed vozil vrne prazen seznam; vozilo brez ujemajoče posodobitve se preslika z null posodobitvijo; podvojen ključ posodobitve potovanja se razreši v favor novejšega časovnega žiga |

### Mapper — TransitDataService mock

| Razred                             | Pokriva                                                                                                                                                                                                                                                                                                                                                                                                         |
| ---------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `engine/MarpromDtoToVaoMapperTest` | `mapBusStops` — pravilen ključ/polja v mapi, prazna mapa ob null; `mapRoutes` — prazno ob null trasah, trasa preskočena brez ujemajoče linije, pravilna polja VAO, postajalna vozlišča vključena samo ko je `isBusStop()` true; `mapSchedules` — prazno ob null linijah, pravilna struktura za eno linijo, dve liniji na isti postaji se združita v en `StopScheduleVao`, null vozni red za linijo je preskočen |

### Controllers — servici zamenjani z mocki, HTTP plast testirana z MockMvc

| Razred                                 | Pokriva                                                                                                                                          |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `api/BikePredictionControllerTest`     | `POST /predict/bikes` vrne vsa štiri polja; primer z nič kolesi; manjkače telo zahteve vrne `400`                                                |
| `api/BusDelayPredictionControllerTest` | `POST /api/bus-delay/predict` vrne pozitivno zamudo; negativno zamudo za zgodnji avtobus; nič za točen avtobus; manjkače telo zahteve vrne `400` |
| `api/ComputePathControllerTest`        | Rezervni odgovor ohrani zahtevane koordinate izvora in cilja, ko usmerjevalnik vrne null                                                         |

### Scheduler — servici zamenjani z mocki, zastavice nastavljene z ReflectionTestUtils

| Razred                           | Pokriva                                                                                                                                                                                                               |
| -------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `scheduler/SchedulerServiceTest` | Vsak od treh razporejevalnikov (`fetchBikeIngestion`, `fetchWeatherIngestion`, `fetchBusIngestion`) preskoči klic storitve, ko je njegova konfiguracijska zastavica `false`; pokliče storitev, ko je zastavica `true` |

### Routing algorithm

| Razred                          | Pokriva                                                                                                                   |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `graph/routing/AStarRouterTest` | Potovanje ohrani zahtevane koordinate; začetna in končna etapa sta pravilni; uporabljena so najbližja postajalna vozlišča |

---

## Omejitve

### SchedulerService — delovni čas ni testabilen

`isWithinOperatingHours()` neposredno kliče `OffsetDateTime.now()`, kar onemogoča
testiranje brez prave ure. Testi razporejevalnika predpostavljajo, da se izvajajo
med **05:00 in 23:00 po ljubljanskem času** (standardni pogoji CI).

### Storitve za napovedovanje — niso testabilne kot enote

`BikePredictionService` in `BusDelayPredictionService` ob zagonu preneseta ONNX modele
iz Supabase prek `@PostConstruct`. Brez aktivne Supabase povezave ju ni mogoče
instancirati v enотnem testu. Namesto tega je testirana **plast krmilnika** z zamenjano storitvijo.

### WeatherDataService — asinhroni subscribe ni testabilen

`ingestWeatherData` uporablja reaktivni `.subscribe()` callback. Testiranje
preslikavalne logike znotraj callbacka zahteva reaktivno testno ogrodje in je bilo odloženo.

---

## Kaj ni testirano (in zakaj)

| Komponenta                                | Razlog                                                                                                                          |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| JPA repozitoriji                          | So Spring Data vmesniki z izvedenimi poizvedbami brez lastne logike. Generiranje poizvedb testira samo Spring Data JPA ogrodje. |
| `SchedulerService.isWithinOperatingHours` | Glej znane omejitve zgoraj.                                                                                                     |
| `WeatherDataService.ingestWeatherData`    | Glej znane omejitve zgoraj.                                                                                                     |
| `MBajkDataService.ingestBikesData`        | Asinhroni reaktivni subscribe; isti razlog kot `WeatherDataService`.                                                            |
| Firebase / `FirebaseAuthFilter`           | Zunanji ponudnik avtentikacije. Testira ga Firebase SDK, ne koda aplikacije.                                                    |
