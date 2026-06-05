# Algoritem usmerjanja

Dokument povzema tok izračuna poti nad internim grafom. Podrobne formule za stroške robov, vremenske uteži, prestopne kazni in alternative so v obstoječem dokumentu [`algoritem-usmerjanja.md`](./algoritem-usmerjanja.md).

Glavne implementacije:

| Področje | Razred |
| --- | --- |
| Glavni usmerjevalnik | [`AStarRouter`](../../../backend/src/main/java/com/sibam/graph/routing/AStarRouter.java) |
| Rezultat poti | [`PathResult`](../../../backend/src/main/java/com/sibam/graph/routing/PathResult.java) |
| Časi posameznih robov | [`PathStepTiming`](../../../backend/src/main/java/com/sibam/graph/routing/PathStepTiming.java) |
| Hevristika | [`HeuristicService`](../../../backend/src/main/java/com/sibam/graph/routing/HeuristicService.java) |
| Stroškovna funkcija | [`WeightedCostFunction`](../../../backend/src/main/java/com/sibam/graph/routing/WeightedCostFunction.java) |
| Vremenski kontekst | [`WeatherRoutingAdjuster`](../../../backend/src/main/java/com/sibam/graph/routing/WeatherRoutingAdjuster.java), [`WeatherRoutingContext`](../../../backend/src/main/java/com/sibam/graph/routing/WeatherRoutingContext.java) |
| Alternativne poti | [`RouteAlternativeService`](../../../backend/src/main/java/com/sibam/graph/routing/RouteAlternativeService.java) |
| Javni API | [`ComputePathController`](../../../backend/src/main/java/com/sibam/api/ComputePathController.java) |

---

## Vhod v izračun

Endpoint `/compute` sprejme:

| Podatek | Namen |
| --- | --- |
| `originLat`, `originLon` | izhodiščne koordinate |
| `destinationLat`, `destinationLon` | ciljne koordinate |
| `leaveNow`, `leaveAt`, `arriveBy`, `date` | časovni način in datum voznih redov |
| `bike`, `bus` | dovoljena multimodalna načina |
| `originAddress`, `destinationAddress` | prikazna naslova v odgovoru |

Podrobnosti API parametrov so v [`api/usmerjanje.md`](../api/usmerjanje.md).

---

## Osnovni tok

Za vsak zahtevek `AStarRouter.findJourneyCandidate()` izvede naslednje korake:

1. Prebere aktivni graf iz `GraphStore`.
2. Z `SpatialSearchService` poišče do `5` najbližjih vozlišč pri izvoru in cilju.
3. Preveri, ali sta izvor in cilj znotraj `routing.max-access-distance-meters`.
4. Ustvari kopijo grafa z začasnima vozliščema `-1` in `-2`.
5. Doda začasne `WALK` robove od izvora do najbližjih vozlišč, od najbližjih ciljnih vozlišč do cilja in neposredno od izvora do cilja.
6. Prebere vremenski kontekst.
7. Izvede A* iskanje v načinu `DEPART_AT` ali `ARRIVE_BY`.
8. Najdeno pot ponovno ovrednoti z dejanskimi časi odhodov in prihodov.
9. Pot pretvori v `Journey`, združi robove v `Leg` etape in jih obogati z navigacijo ter ML napovedmi.

Če graf ni inicializiran, `AStarRouter` vrže `IllegalStateException("Graph is not initialized")`. Inicializacijo zagotavlja `GraphBootstrap`.

---

## Časovni načini

| Način | Obnašanje |
| --- | --- |
| `DEPART_AT` | iskanje se začne ob podanem času odhoda |
| `ARRIVE_BY` | z binarnim iskanjem poišče najpoznejši odhod, ki še prispe do zahtevanega časa |

Pri `ARRIVE_BY` se graf ne preiskuje vzvratno. Algoritem preizkuša možne odhode od začetka dneva do zahtevanega prihoda v korakih po `60 s`.

```text
če kandidat prispe pravočasno:
    shrani kot najboljši rezultat
    išči poznejši odhod
sicer:
    išči zgodnejši odhod
```

---

## Stanje A*

A* ne hrani samo trenutnega vozlišča, ampak tudi zadnjo avtobusno traso:

```text
SearchState = (nodeId, lastBusRoute)
```

To je pomembno, ker ima isti `nodeId` lahko različno nadaljevanje glede na to, ali uporabnik ostaja na isti avtobusni trasi ali prestopa na drugo.

`gScore` predstavlja nabrani strošek v sekundah, `fScore` pa:

```text
fScore = gScore + heuristicSeconds(current, goal)
```

Hevristika uporablja haversine razdaljo do cilja in zelo hitro zgornjo hitrost `30.0 m/s`, zato ostane optimistična.

---

## Dovoljeni načini

Pred sproščanjem roba se preveri, ali je njegov način dovoljen:

| Rob | Pogoj |
| --- | --- |
| `WALK` | vedno dovoljen, razen če ga zavrne vremenska omejitev |
| `BIKE` | dovoljen samo, če je `bike=true` |
| `BUS` | dovoljen samo, če je `bus=true` |
| `TRANSFER` | ni del statičnega A* iskanja |

Če je v dežju `WALK` rob daljši od `routing.weather.rain-max-walk-distance-meters`, ga `WeatherRoutingAdjuster` zavrne.

---

## Sproščanje robov

### WALK in BIKE

Za `WALK` in `BIKE` robove se uporabi `WeightedCostFunction`:

```text
osnovni strošek roba
-> preferenčni množitelji za dolge/kratke razdalje
-> vremenski množitelji
-> profil alternative
-> kazen za že uporabljena vozlišča
```

Čas odhoda roba je trenutni čas stanja, čas prihoda pa:

```text
arrivalMillis = currentMillis + adjustedCostSeconds * 1000
```

### BUS

Pri `BUS` robovih se strošek izračuna časovno odvisno:

| Primer | Obnašanje |
| --- | --- |
| prvi vstop na avtobus | poišče se prvi odhod po trenutnem času |
| prestop na drugo linijo/traso | poišče se nov odhod in doda prestopna kazen |
| nadaljevanje po isti trasi | uporabi se samo osnovni čas vožnje, brez dodatnega čakanja |

Če za datum ni aktivnega voznega reda ali ni več primernega odhoda, se rob ne sprosti.

---

## Ponovno ovrednotenje poti

Po A* iskanju se pot ponovno ovrednoti z `repricePathResult()`.

Namen ponovnega ovrednotenja:

| Razlog | Pomen |
| --- | --- |
| dejanski odhodi | `BUS` etape dobijo konkretne čase odhoda iz voznega reda |
| skladni prihodi | vsaka etapa dobi izračunan `arrivalMillis` |
| odgovor API | `Leg` objekti lahko prikažejo odhod, prihod in trajanje |

Če pot nima timingov, se za začetek ponovnega ovrednotenja uporabi zahtevani čas. Sicer se uporabi odhod prvega roba iz najdene poti.

---

## Pretvorba v odgovor

Najdeni `PathResult` se pretvori v `Journey`:

1. Zaporedni robovi istega načina se združijo v etape.
2. Pri avtobusnih prestopih se doda `TRANSFER` etapa z razdaljo `0`.
3. Za `WALK` in `BIKE` etape se pripravi lokalna polilinija.
4. Zaporedne `WALK` in `BIKE` etape se poskusijo obogatiti z Google Routes navigacijo.
5. Za `BIKE` etape se izračuna napoved razpoložljivosti koles/stojal.
6. Za `BUS` etape se doda sveža GTFS-RT zamuda ali ML napoved zamude.

Če zunanja obogatitev ni dosegljiva, routing rezultat vseeno ostane veljaven; manjkajo samo dodatni podatki v etapi.

---

## Alternativne poti

`RouteAlternativeService` nad istim zahtevkom požene več profilov:

| Profil | Namen |
| --- | --- |
| `FASTEST` | osnovna najhitrejša pot |
| `BIKE_FRIENDLY` | rahlo preferira kolo |
| `TRANSIT_FRIENDLY` | rahlo preferira avtobus |
| `ALTERNATIVE` | poskusi najti drugačno pot z dodatnimi kaznimi |

Po vsaki sprejeti alternativi se uporabljena vozlišča dodajo v množico penaliziranih vozlišč. Naslednji profil tako dobi dodaten strošek za že uporabljene dele poti.

Kandidat se zavrne, če:

| Pogoj | Property |
| --- | --- |
| je preveč podoben že sprejeti poti | `routing.alternatives.max-similarity` |
| je prepočasen glede na najhitrejšo pot | `routing.alternatives.max-slowdown-multiplier` |
| presega največje število alternativ | `compute.path.max-alternatives`, omejeno na največ `3` |

---

## Napake in prazni rezultati

| Primer | Rezultat |
| --- | --- |
| graf nima vozlišč ali najbližja vozlišča niso najdena | `routes: []` |
| izvor ali cilj je predaleč od grafa | `400 Bad Request` z `IZVEN_OBMOCJA_POTI` |
| ni veljavnega avtobusnega odhoda | posamezen `BUS` rob se ne uporabi |
| nobena pot ne doseže cilja | `status: "not_found"` |
| Google Routes ali ML napoved odpove | pot se vrne brez te obogatitve oziroma z nevtralno zamudo |

---

## Povezani dokumenti

| Dokument | Vsebina |
| --- | --- |
| [`graf.md`](graf.md) | model grafa, gradnja, cache in prostorsko iskanje |
| [`algoritem-usmerjanja.md`](algoritem-usmerjanja.md) | podrobne formule stroškov, vreme, prestopi in alternative |
| [`../api/usmerjanje.md`](../api/usmerjanje.md) | javni `/compute` endpoint in struktura odgovora |
| [`../zajem-podatkov/marprom.md`](../zajem-podatkov/marprom.md) | Marprom podatki za postajališča, trase in vozne rede |
| [`../zajem-podatkov/mbajk.md`](../zajem-podatkov/mbajk.md) | MBajk podatki za kolesarske postaje |
