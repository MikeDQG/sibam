# Unit testi

Vsi unit testi tečejo brez Spring contexta in baze. Zunanje odvisnosti so zamockane z Mockito.

---

## Cache (`com.sibam.cache`)

**Kaj testiramo:** Logiko `ArtifactCacheService`, ki odloča ali se artefakt prebere iz lokalnega diska, prenese iz Supabase ali na novo generira. Pokrite so vse štiri poti (`LOCAL`, `SUPABASE`, `GENERATED`, neuspešna generacija). Poleg tega `LocalArtifactStorage` (zapis, branje, brisanje, validacija z `@TempDir`) in `Sha256` s poznanimi vektorji.

**Zakaj tako:** `ArtifactCacheService.ensure()` ima kompleksno razvejano logiko. Mocki za `LocalArtifactStorage` in `SupabaseArtifactStorage` pokrivajo vsako vejo ločeno brez pisanja na disk. `LocalArtifactStorage` testiramo posebej z resničnim `@TempDir`, ker njegova logika vključuje atomično premikanje datotek.

**Kaj ne testiramo:** `SupabaseArtifactStorage` — je le HTTP odjemalec brez poslovne logike.

---

## Graf — model (`com.sibam.graph.model`)

**Kaj testiramo:** `BikeNode` (konstruktorji, getterji), `NodeType` (enum `fromValue()` vklj. neznana vrednost), `Mode` (enum vrednosti), `Trip` (polni konstruktor, setterji/getterji).

**Zakaj tako:** Te razrede predpostavlja vsak razred višjega nivoja. Nezanesljivi getterji ali napačne vrednosti enuma bi povzročili napake pri usmerjanju, ki jih je težko izslediti.

---

## Graf — gradnja (`com.sibam.graph.builder`)

**Kaj testiramo:**
- `WalkingEdgeBuilder` — tip robov `WALK`, pozitivna razdalja, vsaj 1 sekunda hoje, polilinja s 2 točkama
- `BikeEdgeBuilder` — tip `BIKE`, polja shranjena
- `BusEdgeBuilder` — tip `BUS`, `RouteInfo`, polilinja, `scheduleStopPointId`
- `StaticGraphBuilder` (99 % pokritost):
  - Ustvarjanje vozlišč: `BusNode` za vsako postajališče, `BikeNode` za vsako postajo
  - `fetchData()` se pokliče, ko je `busStopsMap` null ali prazen; se ne pokliče, ko je seznam že napolnjen
  - Postajališče z `null` koordinatami je preskočeno
  - `BikeNode` z `null` razpoložljivostjo privzeto dobi `freeBikes=0` in `freeStands=0`
  - Kolesarski robovi: preskočeni, če izvorno postajališče nima prostih koles ali ciljno nima prostih stojal
  - Avtobusni robovi: zgodnji izhod pri prazni/null karti tras; preskočene trase z null točkami oblike
  - Robovi hoje: vozlišča v dosegu 500 m so dvosmerno povezana; oddaljeni pari so preskočeni

**Zakaj tako:** Gradniki robov se kličejo tisočkrat med gradnjo grafa. Napaka pri izračunu razdalje ali napačen tip roba bi pokvarila ves usmerjevalni graf. `StaticGraphBuilder` testiramo z mockiranim `VaoSerializer` in `MBajkDataService` ter resničnim `HelperService`.

---

## Graf — prostorski indeks (`com.sibam.graph.spatial`)

**Kaj testiramo:** `SpatialSearchService` (null/prazen graf, limit 0, vrne najbližje, spoštuje limit, urejeno po razdalji, obnovi indeks ob zamenjavi grafa) in `RTreeIndex` (null/prazen build, enojno vozlišče, najbližje od dveh, 20 vozlišč ki prisilijo večnivojsko drevo, obnova).

**Zakaj tako:** R-tree indeks je ključna komponenta za zmogljivost. Napaka v iskanju bi vrnila napačne postaje kot izhodišče/cilj poti.

---

## Graf — shranjevanje (`com.sibam.graph.storage`)

**Kaj testiramo:** `InMemoryGraphStore` (začetno stanje null, zapis, zamenjava) in `KryoGraphSerializer` (onemogočen način vrne null/ne piše, roundtrip serialize/deserialize, `exists` preveri disk in Supabase, obnova iz Supabase).

**Zakaj tako:** `InMemoryGraphStore` je edino stanje grafa v aplikaciji — napaka pri zamenjavi bi pomenila, da aplikacija deluje s starim grafom. `KryoGraphSerializer` ima razvejano logiko (disabled/local/supabase), ki zahteva ločeno testiranje vsake poti.

---

## Graf — usmerjanje (`com.sibam.graph.routing`)

**Kaj testiramo:**

- **`AStarRouterTest`** — ohranitev koordinat, kazen transferja, isti vozel z različno zadnjo linijo, korakalna navodila Google, transferni segment z ničelno razdaljo, zavrnitev ko je izvor predaleč, napovedovanje zamude avtobusa, spregled napake pri napovedovanju, zaporedna številka postajališča.
- **`WeightedCostFunctionTest`** — vse štiri veje (BUS, BIKE kratek, BIKE dolg, WALK dolg, WALK kratek), vrednost točno na mejnem pragu, minimum 1.
- **`WeatherRoutingAdjusterTest`** — dež (kazen za hojo/kolo, zmanjšana kazen transferja, omejitev razdalje hoje), zmrzal, vročina, hladno vreme, vrsta BUS ni prizadeta, pogoji `"drizzle"`/`"thunderstorm"` zaznani kot dež, DB izjema pade nazaj na nevtralno.
- **`RouteAlternativeServiceTest`** — filtriranje neprimernih načinov, razvrstitev po trajanju, deduplikacija podobnih poti, status `not_found`, meja `maxRoutes`, filtriranje kakovosti po multiplikatorju.

**Zakaj tako:** Usmerjevalni algoritem je osrednja logika aplikacije. Vsaka veja pokriva različno odločitev algoritma. Teste lociramo na raven, kjer kontroliramo graf — boljše od integracijskega testa s polno bazo.

---

## Graf — bootstrap (`com.sibam.graph.bootstrap`)

**Kaj testiramo:** Trije scenariji v `init()`: serializer ima veljavno predpomnjenje, serializer obstaja a `load()` vrne null (rekonstrukcija), serializer ne obstaja (rekonstrukcija). Poleg tega `ensureInitialized()` preskoči, ko je graf že nastavljen, in `refresh()` zgradi in zamenja.

**Zakaj tako:** Bootstrap je vstopna točka življenjskega cikla grafa. Napaka tukaj bi pomenila, da se aplikacija zažene brez grafa ali z zastarelim.

---

## Engine — VAO (`com.sibam.engine`)

**Kaj testiramo:** `VaoSerializer` — kompleksna logika tedenskega predpomnjenja urnikov:
- `dateWindow` — vrne seznam od danes do N dni naprej; izjema pri negativnem `daysAhead`; en datum pri `daysAhead=0`
- `refreshWeeklyScheduleCache` — generira manjkajoče datume, obdrži obstoječe; odstrani včerajšnji datum; deduplikacija identičnih urnikov
- `getSchedulesMap(date)` — vrne pravilno varianto za dani datum; pade nazaj na trenutni urnik, ko ključ ni v predpomnilniku
- `isRouteActiveOnDate` — `true` ko je linija v aktivnem seznamu; konzervativna privzeta vrednost `true` brez predpomnilnika
- `refreshWeeklyScheduleCacheNightly` — ne naredi ničesar, ko je `scheduledRefreshEnabled=false`

`MarpromDtoToVaoMapper` — preslikava DTO → VAO (posredno pokrita z integracijskim testom).

**Zakaj tako:** `VaoSerializer` vsebuje zapleten potek dela (lokalni disk → Supabase → Marprom API). Vse odvisnosti so zamenjane z mocki ali `@TempDir`.

---

## Storitve (`com.sibam.service`)

**Kaj testiramo:**

- **`UserService`** — `getOrCreate` vrne obstoječega brez klica `save`; ustvari in shrani novega z vsemi polji.
- **`SavedLocationService` / `SavedPathService`** — CRUD operacije s pravilnim preslikavanjem; vse operacije vrnejo `403` ob napačnem UID.
- **`TransitDataService`** — vse metode z null odgovorom (→ null), veljavnim odgovorom (→ seznam); `getAllRoutes` agregira trase prek linij.
- **`MBajkDataService`** — `getBikeStationVaos` s svežim posnetkom (mlajšim od 1h) — vrne realne podatke brez klica ML modela; s starim posnetkom (starejšim od 1h) — pade na ML napoved z `status="PREDICTED"`; brez posnetka — vrne nično razpoložljivost. `ingestBikesData` ustvari novo postajo in posnetek; znova uporabi obstoječo postajo brez podvajanja. CountDownLatch za asinhroni subscribe.
- **`WeatherDataService`** — `ingestWeatherData` shrani posnetek s pravilnimi polji: brez dežja, z dežjem, prazen seznam vremena, null seznam vremena, ohrani `recordedAt`. CountDownLatch.
- **`GTFSRTDataService`** — `TripEntity` s pravilnimi polji, `StopDelayEntity` s FK, prazen feed, podvojen ključ.
- **`GoogleRoutesService`** — `parseRouteDetails` z GeoJSON polilinijo in navigacijskimi koraki, prazno polje tras, encoded polyline format, brez nog, brez `Authorization` → null; decimalna vrednost trajanja (`"7.5s"`), korak brez polilinij, točka izven tolerance ujemanja.
- **`BusDelayPredictionService`** — zgodnja vrnitev `0`, ko ključ ni v preslikavi smeri (brez klica ONNX).

**Zakaj CountDownLatch za asinhrono:** `MBajkDataService` in `WeatherDataService` preneseta callback na `Schedulers.boundedElastic()`. `CountDownLatch` blokira test, dokler se subscriber ne izvede — zanesljivo brez `Thread.sleep`.

---

## Krmilniki (`com.sibam.api`)

**Kaj testiramo:** Vse REST končne točke z `MockMvc` v standalone načinu (brez Spring contexta). `@RequestAttribute("uid")` se simulira z `.requestAttr(...)`. Preverja se HTTP status in ključna polja JSON.

| Krmilnik | Pokrite končne točke |
| --- | --- |
| `BikePredictionController` | POST /predict/bikes — 200, 400 |
| `BusDelayPredictionController` | POST /api/bus-delay/predict — 200, 400 |
| `ComputePathController` | GET /compute — rezervni odgovor |
| `SavedLocationController` | GET, POST, PUT, DELETE |
| `SavedPathController` | GET, POST, DELETE |
| `UserController` | POST /me, GET /me (najden, ni najden → 404) |

**Zakaj standalone MockMvc:** Polni Spring context naloži Firebase, bazo in ONNX, kar zahteva Docker in okoljske spremenljivke. Standalone testi so 100× hitrejši in enako zanesljivi za testiranje HTTP plasti.

---

## Varnostni filter (`com.sibam.config`)

**Kaj testiramo:** `FirebaseAuthFilter` — zahteva brez `Authorization` preide naprej, brez Bearer sheme preide naprej, neveljaven token vrne `401`, `401` z dovoljenega izvora nastavi CORS glave, `401` z neznanega izvora ne nastavi CORS glav. `WebConfig` — CORS preflight z dovoljenega izvora vrne 200, z nedovoljenega 403.

**Zakaj ne testiramo veljavnega žetona:** `FirebaseAuth.getInstance().verifyIdToken()` je statičen klic na končnem razredu — ni mokabilen brez PowerMock.

---

## Razporejevalnik (`com.sibam.scheduler`)

**Kaj testiramo:** `SchedulerService` — vsak razporejevalnik preskoči klic storitve, ko je zastavica `false`; pokliče storitev, ko je zastavica `true`; preskoči zunaj operativnega okna (03:00 Ljubljana, pred 05:00); izjeme so ujete in se ne propagirajo. Vključno z `reloadMlModels()`: preskočen, ko je `schedulers.reload-ml-models.on=false`; pokliče oba servisna `reload` klica, ko je zastavica `true`; `OrtException` iz bike servisa ne propaga.

**Zakaj `Clock` injection:** `SchedulerService` sprejema `Clock` v konstruktorju — omogoča injekcijo fiksne ure v testih brez `Thread.sleep`.

---

## Integracijske preslikave (`com.sibam.integration`)

**Kaj testiramo:** `GTFSRTMapper` — preslikava vseh polj vozila, null `TripUpdate`, postajne posodobitve z zamudami, zamuda privzeto 0 brez prihodnega dogodka, rezervni `tripId` iz `TripUpdate`, neobvezna polja (bearing, stopId, timestamp).

**Zakaj ne testiramo HTTP odjemalcev:** So tanki ovoji brez poslovne logike, ki samo delegirajo WebClientu.
