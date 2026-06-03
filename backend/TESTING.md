# Testiranje backenda

## Pregled

Backend uporablja **JUnit 5 + Mockito + AssertJ** za unit teste in **Testcontainers + Spring Boot Test** za integracijske teste. Pokritost se meri z JaCoCo in preverja SonarCloud ob vsakem pushu na `main`.

| Metrika | Vrednost |
|---|---|
| Skupno testov | ~340 |
| Pokritost vrstic | ~73 % |
| Izhodišče (pred testiranjem) | 56 % |

---

## Vrste testov

### Unit testi
Spring context se **nikoli ne naloži**, podatkovna baza se ne zažene. Zunanje odvisnosti (repozitoriji, HTTP odjemalci, Firebase) so zamenjane z Mockito mocki. Testi so hitri in deterministični.

### Integracijski testi — podatkovna baza
Spring context se naloži v celoti. Testcontainers samodejno zažene `postgres:16` Docker container. Vsi DB IT razredi dedujejo od `AbstractDatabaseIT`, ki zagotavlja en singleton container za cel testni tek. Testi so označeni z `@Transactional` — vsak test se po koncu samodejno povrne.

### Integracijski testi — ONNX modeli
Spring context se **ne naloži** — modeli se zgradijo ročno z `ReflectionTestUtils`. Testi zahtevajo okoljski spremenljivki `SUPABASE_URL` in `SUPABASE_SERVICE_KEY` in so privzeto preskočeni (`@EnabledIfEnvironmentVariable`). Zaganjajo se samo v CI, kjer so Supabase skrivnosti nastavljene.

---

## Zagon testov

```bash
# vsi unit + DB integracijski testi
./mvnw test

# z generiranjem poročila JaCoCo
./mvnw test jacoco:report
# poročilo: target/site/jacoco/index.html
```

ONNX testi se zaženejo samo, ko sta nastavljeni okoljski spremenljivki:
```bash
SUPABASE_URL=https://... SUPABASE_SERVICE_KEY=... ./mvnw test
```

**IntelliJ:** Desni klik na `src/test/java/com/sibam` → *Run 'Tests in sibam'*

**Pokritost v VS Code:** Testing panel → desni klik → *Run Tests with Coverage*

---

## Testna strategija po domenah

### Cache (`com.sibam.cache`)

**Kaj testiramo:** Logiko `ArtifactCacheService`, ki odloča, ali se artefakt prebere iz lokalnega diska, prenese iz Supabase ali na novo generira. Testiramo vse štiri poti (`LOCAL`, `SUPABASE`, `GENERATED`, neuspešna generacija). Poleg tega testiramo `LocalArtifactStorage` (zapis, branje, brisanje, validacija z `@TempDir`) in `Sha256` s poznanimi vektorji.

**Zakaj tako:** `ArtifactCacheService.ensure()` ima kompleksno razvejano logiko. Unit testi z mockiranim `LocalArtifactStorage` in `SupabaseArtifactStorage` pokrivajo vsako vejo ločeno, ne da bi pisali v datotečni sistem. `LocalArtifactStorage` testiramo posebej z resničnim `@TempDir`, ker njegova logika vključuje atomično premikanje datotek.

**Kaj ne testiramo:** `SupabaseArtifactStorage` — je le HTTP odjemalec brez poslovne logike, ki samo delegira WebClientu.

---

### Graf — model (`com.sibam.graph.model`)

**Kaj testiramo:** `BikeNode` (konstruktorji in getterji), `BusNode` (pokrit posredno prek `AStarRouterTest`), `NodeType` (enum `fromValue()` vklj. neznana vrednost), `Mode` (enum vrednosti), `Trip` (polni konstruktor, setterji/getterji).

**Zakaj tako:** Te razrede je preprosto testirati in jih vsak razred višjega nivoja predpostavlja. Nezanesljivi getterji ali napačne vrednosti enum-a bi povzročili napake pri usmerjanju, ki jih je težko izslediti.

---

### Graf — gradnja (`com.sibam.graph.builder`)

**Kaj testiramo:** `WalkingEdgeBuilder` (tip robov `WALK`, pozitivna razdalja, vsaj 1 sekunda hoje, polilinja s 2 točkama), `BikeEdgeBuilder` (tip `BIKE`, polja shranjena), `BusEdgeBuilder` (tip `BUS`, `RouteInfo`, polilinija, `scheduleStopPointId`).

**Zakaj tako:** Gradniki robov so majhni in tesno zasnovani, a jih `StaticGraphBuilder` kliče tisočkrat. Napaka pri izračunu razdalje ali napačen tip roba bi pokvarila ves usmerjevalni graf.

**Kaj ne testiramo:** `StaticGraphBuilder` — zahteva polno VMock postavitev `VaoSerializer` in `MBajkDataService` z desetinami metod. Logika je posredno pokrita prek `AStarRouterTest`, ki zgradi pravi graf.

---

### Graf — prostorski indeks (`com.sibam.graph.spatial`)

**Kaj testiramo:** `SpatialSearchService` (null/prazen graf, limit 0, vrne najbližje, spoštuje limit, urejeno po razdalji, obnovi indeks ob zamenjavi grafa) in `RTreeIndex` (null/prazen build, enojno vozlišče, najbližje od dveh, 20 vozlišč ki prisilijo večnivojsko drevo, obnova).

**Zakaj tako:** R-tree indeks je ključna komponenta za zmogljivost. Napaka v iskanju bi vrnila napačne postaje kot izhodišče/cilj poti. Testiramo neposredno, ker `AStarRouterTest` indeks uporablja, a ga ne preverja izoliranega.

---

### Graf — shranjevanje (`com.sibam.graph.storage`)

**Kaj testiramo:** `InMemoryGraphStore` (začetno stanje null, zapis, zamenjava) in `KryoGraphSerializer` (onemogočen način vrne null/ne piše, roundtrip serialize/deserialize, exists preveri disk in Supabase, obnova iz Supabase).

**Zakaj tako:** `InMemoryGraphStore` je edino stanje grafa v aplikaciji — napaka pri zamenjavi bi pomenila, da aplikacija deluje s starim grafom. `KryoGraphSerializer` ima razvejano logiko (disabled/local/supabase), ki zahteva ločeno testiranje vsake poti.

---

### Graf — usmerjanje (`com.sibam.graph.routing`)

**Kaj testiramo:**

- `AStarRouterTest` — ohranitev koordinat, kazen transferja, isti vozel z različno zadnjo linijo, korakalna navodila Google, transferni segment z ničelno razdaljo, zavrnitev ko je izvor predaleč, napovedovanje zamude avtobusa, spregled napake pri napovedovanju, zaporedna številka postajališča.
- `WeightedCostFunctionTest` — vse štiri veje (BUS brez kazni, BIKE kratek, BIKE dolg, WALK dolg, WALK kratek), vrednost točno na mejnem pragu, minimum 1.
- `WeatherRoutingAdjusterTest` — dež (kazen za hojo/kolo, zmanjšana kazen transferja, omejitev razdalje hoje), zmrzal, vročina, hladno vreme, vrsta BUS ni prizadeta, pogoji "drizzle"/"thunderstorm" zaznani kot dež, DB izjema pade nazaj na nevtralno.
- `RouteAlternativeServiceTest` — filtriranje neprimernih načinov, razvrstitev po trajanju, deduplikacija podobnih poti, status `not_found`, meja `maxRoutes`, filtriranje kakovosti po multiplikatorju.
- `HeuristicServiceTest` — pokrit obstoječ.

**Zakaj tako:** Usmerjevalni algoritem je osrednja logika aplikacije. Vsaka veja pokriva različno odločitev algoritma (kazen transferja, vreme, alternative). Teste z Mockito lociramo na raven, kjer kontroliramo graf — boljše od integracijskega testa s polno bazo.

---

### Graf — bootstrap (`com.sibam.graph.bootstrap`)

**Kaj testiramo:** `GraphBootstrap` — trije scenariji v `init()`: serializer ima veljavno predpomnjenje, serializer obstaja a `load()` vrne null (rekonstrukcija), serializer ne obstaja (rekonstrukcija). Poleg tega `ensureInitialized()` preskoči, ko je graf že nastavljen, in `refresh()` zgradi in zamenja.

**Zakaj tako:** Bootstrap je vstopna točka življenjskega cikla grafa. Napaka tukaj bi pomenila, da se aplikacija zažene brez grafa ali z zastarelim.

---

### Storitve (`com.sibam.service`)

**Kaj testiramo:**

- **`UserService`** — `getOrCreate` vrne obstoječega brez klica `save`; ustvari in shrani novega z vsemi polji.
- **`SavedLocationService` / `SavedPathService`** — CRUD operacije s pravilnim preslikavanjem; vse operacije vrnejo `403` ob napačnem UID.
- **`TransitDataService`** — vse metode z null odgovorom (`→ null`), veljavnim odgovorom (`→ seznam`); `getAllRoutes` agregira trase prek linij, preskoči linijo z null trastami.
- **`MBajkDataService`** — `getBikeStationVaos` z in brez posnetka; `ingestBikesData` ustvari novo postajo in posnetek, ali pa znova uporabi obstoječo (CountDownLatch za asinhroni subscribe).
- **`WeatherDataService`** — `ingestWeatherData` shrani posnetek s pravilnimi polji: brez dežja, z dežjem, prazen seznam vremena, null seznam vremena, ohrani `recordedAt` (CountDownLatch).
- **`GTFSRTDataService`** — TripEntity s pravilnimi polji, StopDelayEntity s FK, prazen feed, podvojen ključ.
- **`GoogleRoutesService`** — `parseRouteDetails` z GeoJSON poliliniijo in navigacijskimi koraki, prazno polje tras, encoded polyline format, brez nog, brez `Authorization` → null, `fetchPolyline` pade nazaj na origin+dest.
- **`BusDelayPredictionService`** (unit) — zgodnja vrnitev 0, ko ključ ni v preslikavi smeri (brez ONNX).

**Zakaj CountDownLatch za asinhrono:** `MBajkDataService` in `WeatherDataService` uporabljata `.publishOn(Schedulers.boundedElastic()).subscribe(...)`, kar callback prenese na drug nitni bazen. `CountDownLatch` blokira test, dokler se subscriber ne izvede — zanesljivo brez `Thread.sleep`.

---

### Krmilniki (`com.sibam.api`)

**Kaj testiramo:** Vse REST končne točke z `MockMvc` v standalone načinu (brez Spring contexta). `@RequestAttribute("uid")` se simulira z `.requestAttr(...)`. Preverja se HTTP status in ključna polja JSON.

| Krmilnik | Pokrite končne točke |
|---|---|
| `BikePredictionController` | POST /predict/bikes — 200, 400 |
| `BusDelayPredictionController` | POST /api/bus-delay/predict — 200, 400 |
| `ComputePathController` | GET /api/compute-path — rezervni odgovor |
| `SavedLocationController` | GET, POST, PUT, DELETE |
| `SavedPathController` | GET, POST, DELETE |
| `UserController` | POST /me, GET /me (najden, ni najden → 404) |
| `SimpleController` | GET /simple-api/test |

**Zakaj standalone MockMvc (ne `@SpringBootTest`):** Polni Spring context naloži Firebase, bazo in ONNX, kar zahteva Docker in okoljske spremenljivke. Standalone testi so 100× hitrejši in enako zanesljivi za testiranje HTTP plasti.

---

### Varnostni filter (`com.sibam.config`)

**Kaj testiramo:** `FirebaseAuthFilter` — zahteva brez `Authorization` glave preide naprej, zahteva brez Bearer sheme preide naprej, neveljaven Bearer žeton vrne `401`, `401` z dovoljeneg izvorom nastavi CORS glave, `401` z neznanim izvorom ne nastavi CORS glav.

**Zakaj ne testiramo veljavnega žetona:** `FirebaseAuth.getInstance().verifyIdToken()` je statičen klic na končnem razredu iz Firebase SDK — ni ga mogoče mockirati s standardnim Mockitom brez dodatnih odvisnosti. Veljavna pot je pokrita z integracijskim testom v CI, kjer Firebase pravilno inicializiramo.

---

### Razporejevalnik (`com.sibam.scheduler`)

**Kaj testiramo:** `SchedulerService` — vsak od treh razporejevalnikov preskoči klic storitve, ko je zastavica `false`; pokliče storitev, ko je zastavica `true`; preskoči, ko je ura zunaj operativnega okna (03:00 Ljubljana, preden se odpre ob 05:00); izjeme so ujete in se ne propagirajo do klicatelja.

**Zakaj `Clock` injection:** `SchedulerService` sprejema `Clock` v konstruktorju — to omogoča injekcijo fiksne ure v testih brez `Thread.sleep` ali lažnega časa sistemske ure.

---

### Integracijski razredi (`com.sibam.integration`)

**Kaj testiramo:** `GTFSRTMapper` — preslikava vseh polj vozila, null `TripUpdate`, postajne posodobitve z zamudami, zamuda privzeto 0 brez prihodnega dogodka, rezervni tripId iz `TripUpdate`, null VehiclePosition, neobvezna polja (bearing, stopId, timestamp), prioriteta ID-ja vozila pred rezervnim.

**Zakaj ne testiramo HTTP odjemalcev** (`GTFSRTClient`, `MBajkClient`, `WeatherClient`, `MarpromClient`, `SupabaseStorageClient`): so tanki ovoji brez poslovne logike, ki samo delegirajo WebClientu ali HttpClientu. Testiranje bi pomenilo testiranje Springovega ogrodja, ne aplikacijske kode.

---

## Integracijski testi

### Podatkovna baza

| Razred | Pokriva |
|---|---|
| `it/UserServiceIT` | Ustvarjanje, deduplikacija, iskanje po UID |
| `it/SavedLocationServiceIT` | Shranjevanje, posodobitev, brisanje, 403 varnost |
| `it/SavedPathServiceIT` | Shranjevanje, ohranitev `Journey` JSON, brisanje, več poti, 403 varnost |
| `it/MBajkDataServiceIT` | `ingestBikesData` ustvari postajo+posnetek, ne podvoji; `getBikeStationVaos` vrne razpoložljivost |
| `it/GTFSRTDataServiceIT` | TripEntity, StopDelayEntity, prazen feed, dve vozili |

### ONNX modeli (zahteva `SUPABASE_URL`)

| Razred | Pokriva |
|---|---|
| `it/BikePredictionServiceIT` | Nalaganje vseh štirih modelov, verjetnosti v [0,1], nenegativne količine, vikend+dež scenarij |
| `it/BusDelayPredictionServiceIT` | Nalaganje modela+preslikave, neznana postaja → 0, znana postaja → razumen obseg, več kombinacij |

---

## Kaj ni pokrito in zakaj

| Komponenta | Razlog |
|---|---|
| `BikePredictionService` (unit) | Vse poti gredo skozi `OrtEnvironment.getEnvironment()` — statična metoda na končnem razredu. Ni mokabilno brez PowerMock. Pokrita z `BikePredictionServiceIT`. |
| `StaticGraphBuilder` | Zahteva polno postavitev `VaoSerializer` z ~20 metodami. Logika je posredno pokrita z `AStarRouterTest`. |
| `WeatherDataService.ingestWeatherData` | Sedaj pokrito z unit testom (CountDownLatch). |
| `FirebaseAuthFilter` (veljavni žeton) | `FirebaseAuth.getInstance()` ni mokabilen s standardnim Mockitom. |
| JPA repozitoriji | Spring Data vmesniki brez lastne logike; posredno preverjeni z vsakim DB IT testom. |
| `SupabaseArtifactStorage` | HTTP odjemalec brez poslovne logike. |
| HTTP odjemalci (`*Client`) | Tanki ovoji brez poslovne logike. |
