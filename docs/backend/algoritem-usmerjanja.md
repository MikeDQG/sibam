# Algoritem usmerjanja in stroški robov

Dokument opisuje interni algoritem za izračun multimodalnih poti, način gradnje osnovnih stroškov robov in vse dodatne kazni oziroma množitelje, ki vplivajo na izbiro poti.

Glavne implementacije:

| Področje | Razred |
| --- | --- |
| A* iskanje in časovno odvisni avtobusni robovi | [`AStarRouter`](../../backend/src/main/java/com/sibam/graph/routing/AStarRouter.java) |
| Osnovna stroškovna funkcija | [`WeightedCostFunction`](../../backend/src/main/java/com/sibam/graph/routing/WeightedCostFunction.java) |
| Vremenske prilagoditve | [`WeatherRoutingAdjuster`](../../backend/src/main/java/com/sibam/graph/routing/WeatherRoutingAdjuster.java) |
| Konfiguracija stroškov | [`RoutingConfig`](../../backend/src/main/java/com/sibam/graph/routing/RoutingConfig.java) |
| Gradnja statičnega grafa | [`StaticGraphBuilder`](../../backend/src/main/java/com/sibam/graph/builder/StaticGraphBuilder.java) |
| Hevristika A* | [`HeuristicService`](../../backend/src/main/java/com/sibam/graph/routing/HeuristicService.java) |
| Alternativne poti | [`RouteAlternativeService`](../../backend/src/main/java/com/sibam/graph/routing/RouteAlternativeService.java) |

---

## Pregled algoritma

Routing uporablja časovno odvisni A* nad usmerjenim grafom. Graf vsebuje:

| Tip vozlišča | Vir |
| --- | --- |
| `BusNode` | Marprom postajališča |
| `BikeNode` | MBajk postaje z zadnjo znano razpoložljivostjo |
| `LocationNode` | začasni izvor in cilj uporabnika, dodana samo za en zahtevek |

Pri vsakem zahtevku se izvede naslednji tok:

1. Prebere se trenutno naložen graf iz `GraphStore`.
2. Poišče se do `5` najbližjih vozlišč pri izvoru in cilju.
3. Preveri se, da sta izvor in cilj največ `routing.max-access-distance-meters` od najbližjega vozlišča grafa.
4. Ustvari se začasna kopija grafa z vozliščema `ORIGIN_NODE_ID = -1` in `DESTINATION_NODE_ID = -2`.
5. Dodajo se `WALK` robovi:
   - iz izvora do najbližjih začetnih vozlišč,
   - iz najbližjih ciljnih vozlišč do cilja,
   - neposredno iz izvora do cilja.
6. Enkrat na začetku zahtevka se prebere vremenski kontekst. Isti kontekst se uporablja v celotnem izračunu poti.
7. A* poišče najcenejšo pot glede na dovoljene načine (`bike`, `bus`), vozne rede, vreme, prestopne kazni in profil alternative.
8. Najdena pot se ponovno ovrednoti z dejanskimi odhodi/prihodi robov.
9. Robovi se združijo v etape (`Leg`), etape pa se obogatijo z navigacijo in ML napovedmi.

Za `ARRIVE_BY` način se A* ne izvaja nazaj po grafu. Namesto tega `AStarRouter` z binarnim iskanjem po minutnih korakih (`60 s`) poišče najpoznejši možen odhod, ki še prispe do zahtevanega časa prihoda.

---

## Osnovni stroški robov

Vsi osnovni stroški so izraženi v sekundah. V `Edge` se shranita `distanceMeters` in `costSeconds`; poznejša stroškovna funkcija `costSeconds` samo dodatno uteži.

### WALK rob

`WalkingEdgeBuilder` izračuna korigirano razdaljo in čas hoje:

```text
manhattanMeters = sever-jug haversine + vzhod-zahod haversine
distanceMeters = round(manhattanMeters * 1.25)
baseCostSeconds = max(1, round(distanceMeters / walkingSpeedMps))
walkingSpeedMps = 5.0 km/h * 1000 / 3600 = 1.3889 m/s
```

Statični `WALK` robovi med obstoječimi vozlišči se dodajo samo, če je zračna haversine razdalja največ `500 m`. Ta meja je konstanta v `HelperService` in ni property.

Začasni `WALK` robovi od/do uporabnikove lokacije se dodajo do najbližjih `5` vozlišč in neposredno od izvora do cilja. Izvor in cilj morata biti znotraj `routing.max-access-distance-meters` od grafa.

### BIKE rob

`StaticGraphBuilder` doda usmerjene `BIKE` robove med MBajk postajami:

```text
manhattanMeters = sever-jug haversine + vzhod-zahod haversine
distanceMeters = max(1, round(manhattanMeters * 1.15))
baseCostSeconds = max(1, round(distanceMeters / 4.5))
```

Rob `from -> to` se doda samo, če:

| Pogoj | Pomen |
| --- | --- |
| `from.freeBikes > 0` | na začetni MBajk postaji je vsaj eno prosto kolo |
| `to.freeStands > 0` | na ciljni MBajk postaji je vsaj eno prosto stojalo |
| `from != to` | ni robov iz postaje same vase |

Razpoložljivost MBajk torej vpliva na strukturo grafa, ne kot dodatni numerični strošek. ML napoved razpoložljivosti se doda v API odgovor, ni pa uporabljena kot kazen v A* strošku.

### BUS rob

`StaticGraphBuilder` doda `BUS` robove med zaporednimi postajami na Marprom trasi:

```text
distanceMeters = max(1, round(razdalja po shape točkah med postajama))
baseCostSeconds = max(1, round(distanceMeters / 6.0))
```

Rob vsebuje `RouteInfo` (`lineId`, `routeId`, `headsignName`, `lineCode`), polilinijo za prikaz in `scheduleStopPointId`, ki se uporablja pri iskanju odhodov.

Pri A* se za prvi `BUS` rob neke linije oziroma po prestopu uporabi časovno odvisna cena:

```text
departureMillis = prvi odhod iz voznega reda, ki je >= currentMillis
arrivalMillis = departureMillis + baseCostSeconds * 1000
waitAndRideSeconds = round((arrivalMillis - currentMillis) / 1000)
costSeconds = waitAndRideSeconds + transferPenaltySeconds
```

Če se naslednji `BUS` rob nadaljuje po isti trasi (`RouteInfo` je enak prejšnjemu robu), se ne išče nov odhod in ni čakanja:

```text
costSeconds = baseCostSeconds
```

Če vozni red za datum ne vsebuje aktivne trase ali ni več primernega odhoda, se rob ne more uporabiti.

---

## Property konfiguracija stroškov

Vrednosti so v `backend/src/main/resources/application.properties` in jih Spring naloži prek `RoutingConfig`. Če property manjka, `RoutingConfig` uporabi fallback iz `@Value`.

| Property | Trenutna vrednost | Fallback v kodi | Uporaba |
| --- | ---: | ---: | --- |
| `routing.transfer-penalty-seconds` | `100` | `300` | osnovna kazen pri prestopu med različnima bus linijama/trasama |
| `routing.bike-distance-threshold-meters` | `4000` | `1000` | prag za kratke/dolge `WALK` in `BIKE` robove v preferenčni funkciji |
| `routing.bike-long-distance-multiplier` | `2.0` | `5.0` | množitelj za `BIKE` robove nad pragom |
| `routing.walk-long-distance-multiplier` | `3.0` | `3.0` | množitelj za `WALK` robove nad pragom |
| `routing.bike-short-distance-multiplier` | `1.5` | `1.5` | množitelj za `BIKE` robove do praga |
| `routing.max-access-distance-meters` | `3000` | `3000` | največja dovoljena razdalja izvora/cilja od grafa |
| `routing.weather.rain-walk-multiplier` | `2.0` | `2.0` | vremenski množitelj za hojo v dežju |
| `routing.weather.rain-bike-multiplier` | `5.0` | `5.0` | vremenski množitelj za kolo v dežju |
| `routing.weather.rain-transfer-penalty-multiplier` | `0.5` | `0.5` | množitelj prestopne kazni v dežju |
| `routing.weather.rain-max-walk-distance-meters` | `500` | `500` | največja dovoljena dolžina `WALK` roba v dežju |
| `routing.weather.rain-threshold-millimeters` | `0.0` | `0.0` | meja za zaznavo dežja po količini padavin |
| `routing.weather.freezing-temperature-celsius` | `0.0` | `0.0` | meja za mraz |
| `routing.weather.cool-temperature-celsius` | `10.0` | `10.0` | meja za hladno vreme |
| `routing.weather.hot-temperature-celsius` | `30.0` | `30.0` | meja za vročino |
| `routing.weather.freezing-walk-multiplier` | `1.5` | `1.5` | vremenski množitelj za hojo pod 0 °C |
| `routing.weather.freezing-bike-multiplier` | `2.0` | `2.0` | vremenski množitelj za kolo pod 0 °C |
| `routing.weather.cool-walk-multiplier` | `1.2` | `1.2` | vremenski množitelj za hojo med 0 °C in 10 °C |
| `routing.weather.cool-bike-multiplier` | `1.3` | `1.3` | vremenski množitelj za kolo med 0 °C in 10 °C |
| `routing.weather.hot-walk-multiplier` | `1.3` | `1.3` | vremenski množitelj za hojo nad 30 °C |
| `routing.weather.hot-bike-multiplier` | `1.1` | `1.1` | vremenski množitelj za kolo nad 30 °C |
| `compute.path.max-alternatives` | `3` | `routing.alternatives.max-routes` ali `3` | največ vrnjenih alternativ, omejeno na največ 3 |
| `routing.alternatives.max-similarity` | `0.8` | `0.8` | največja dovoljena podobnost nove alternative z že sprejetimi |
| `routing.alternatives.max-slowdown-multiplier` | `1.4` | `1.4` | najpočasnejša sprejeta pot sme trajati največ 1.4-krat toliko kot najhitrejša |
| `routing.alternatives.node-penalty-seconds` | `180` | `180` | kazen za vozlišča, uporabljena v že sprejetih alternativah |

---

## Formula za končni strošek roba

Za `WALK` in `BIKE` robove se uporabi `WeightedCostFunction`:

```text
preferenceCost = applyPreferencePenalty(edgeType, distanceMeters, baseCostSeconds)
finalCost = max(1, round(preferenceCost * weatherMultiplier(edgeType, weather)))
```

Preferenčna kazen:

```text
če edgeType = WALK in distanceMeters > routing.bike-distance-threshold-meters:
    preferenceCost = round(baseCostSeconds * routing.walk-long-distance-multiplier)

če edgeType = BIKE in distanceMeters > routing.bike-distance-threshold-meters:
    preferenceCost = round(baseCostSeconds * routing.bike-long-distance-multiplier)

če edgeType = BIKE in distanceMeters <= routing.bike-distance-threshold-meters:
    preferenceCost = round(baseCostSeconds * routing.bike-short-distance-multiplier)

sicer:
    preferenceCost = baseCostSeconds
```

Za `BUS` robove se `WeightedCostFunction` pri običajnem sproščanju ne uporablja. `AStarRouter` neposredno upošteva vozni red, čakanje, vožnjo, prestopno kazen in profil alternative.

---

## Vremenske prilagoditve

Vreme se prebere iz zadnjega svežega `WeatherSnapshot`. Posnetek je svež, če ni starejši od `fallback.realtime-max-age-minutes` (`60` minut). Če posnetka ni, je zastarel ali branje iz baze ne uspe, se uporabi nevtralni kontekst:

```text
temperatureCelsius = null
raining = false
rainMm = 0
windSpeedMs = 0
```

Dež je aktiven, če velja vsaj eno:

```text
snapshot.rain > routing.weather.rain-threshold-millimeters
condition vsebuje "rain", "drizzle" ali "thunderstorm"
```

Vremenski množitelj se sestavi multiplikativno:

```text
weatherMultiplier = 1.0

če raining in edgeType = WALK:
    weatherMultiplier *= routing.weather.rain-walk-multiplier

če raining in edgeType = BIKE:
    weatherMultiplier *= routing.weather.rain-bike-multiplier

če temperature < freezing-temperature:
    WALK: weatherMultiplier *= freezing-walk-multiplier
    BIKE: weatherMultiplier *= freezing-bike-multiplier

sicer če temperature < cool-temperature:
    WALK: weatherMultiplier *= cool-walk-multiplier
    BIKE: weatherMultiplier *= cool-bike-multiplier

sicer če temperature > hot-temperature:
    WALK: weatherMultiplier *= hot-walk-multiplier
    BIKE: weatherMultiplier *= hot-bike-multiplier
```

Primer: če dežuje in je temperatura `5 °C`, je `WALK` vremenski množitelj `2.0 * 1.2 = 2.4`, `BIKE` pa `5.0 * 1.3 = 6.5`.

V dežju se `WALK` robovi daljši od `routing.weather.rain-max-walk-distance-meters` zavrnejo:

```text
če raining in edgeType = WALK in distanceMeters > rain-max-walk-distance-meters:
    rob ni dovoljen
```

Prestopna kazen med različnima avtobusnima linijama/trasama se v dežju zmanjša:

```text
transferPenalty = round(routing.transfer-penalty-seconds * routing.weather.rain-transfer-penalty-multiplier)
```

`windSpeedMs` se shrani v `WeatherRoutingContext`, vendar ne vpliva na A* strošek. Uporablja se kot vhodna značilka pri ML napovedih za etape.

---

## Prestopi

Prestopna kazen se uporabi samo pri prehodu na `BUS` rob, kadar je prejšnja avtobusna linija oziroma trasa drugačna:

```text
če previousRoute == null:
    transferPenalty = 0

če currentEdge ni BUS:
    transferPenalty = 0

če previousRoute.lineId == currentRoute.lineId ali previousRoute.routeId == currentRoute.routeId:
    transferPenalty = 0

sicer:
    transferPenalty = adjustedTransferPenaltySeconds(weather)
```

V odgovoru API se prestop na isti postaji prikaže kot ločena `TRANSFER` etapa z razdaljo `0`. Njeno trajanje je razlika med prihodom prejšnjega bus roba in odhodom naslednjega bus roba.

---

## Hevristika A*

A* uporablja:

```text
fScore = gScore + heuristicSeconds(current, goal)
```

`gScore` je nabrani strošek v sekundah. Hevristika je ocena najhitrejšega možnega časa do cilja:

```text
heuristicSeconds = distance(current, goal) / speed
```

Ker `AStarRouter` kliče `heuristicService.estimate(current, goal)` brez tipa roba, se uporablja:

```text
distance = haversineMeters(current, goal)
speed = 30.0 m/s
```

V `HeuristicService` obstajata še hitrosti za ročno ocenjevanje po tipu roba:

| Tip | Razdalja | Hitrost |
| --- | --- | ---: |
| `WALK` | korigirana razdalja za hojo | `2.0 m/s` |
| `BIKE` | korigirana razdalja za kolo | `6.0 m/s` |
| ostalo/null | haversine razdalja | `30.0 m/s` |

---

## Alternativne poti

`RouteAlternativeService` za isti zahtevek požene več iskalnih profilov:

| Oznaka profila | `bikeMultiplier` | `busMultiplier` |
| --- | ---: | ---: |
| `FASTEST` | `1.00` | `1.00` |
| `BIKE_FRIENDLY` | `0.85` | `1.10` |
| `TRANSIT_FRIENDLY` | `1.20` | `0.90` |
| `ALTERNATIVE` | `1.05` | `1.05` |

Profil se uporabi po osnovnem strošku roba:

```text
če edgeType = BIKE:
    adjusted = round(costSeconds * bikeMultiplier)
če edgeType = BUS:
    adjusted = round(costSeconds * busMultiplier)
sicer:
    adjusted = costSeconds

če je fromNodeId ali toNodeId v penalizedNodeIds:
    adjusted += routing.alternatives.node-penalty-seconds
```

Po vsaki sprejeti alternativi se njena vozlišča dodajo v `penalizedNodeIds`, da naslednji profil lažje najde drugačno pot.

Kandidat se zavrne, če je preveč podoben že sprejetim potem:

```text
similarity = sharedNodeCount / min(candidateNodeCount, existingNodeCount)
zavrni, če similarity > routing.alternatives.max-similarity
```

Po sortiranju po trajanju se zavrnejo poti, ki so prepočasne:

```text
zavrni, če durationSeconds > fastestDurationSeconds * routing.alternatives.max-slowdown-multiplier
```

Vedno se vrne največ `3` alternativ, tudi če konfiguracija zahteva več.

---

## Dejavniki, ki vplivajo na pot

| Dejavnik | Vpliv na A* strošek | Opomba |
| --- | --- | --- |
| Razdalja robov | da | osnova za `costSeconds` pri vseh tipih robov |
| Hitrost hoje `5 km/h` | da | konstanta v `HelperService` |
| Hitrost kolesa `4.5 m/s` | da | konstanta v `StaticGraphBuilder` |
| Hitrost bus roba `6.0 m/s` | da | približek za osnovni čas med postajama |
| Vozni red Marprom | da | določa čakanje in razpoložljivost `BUS` robov ob času |
| Prestop med bus linijami/trasami | da | property `routing.transfer-penalty-seconds`, vremensko prilagojen v dežju |
| Dovoljena načina `bike` in `bus` | da | izklopljen način zavrne ustrezne robove |
| Trenutno vreme: dež | da | podraži `WALK`/`BIKE`, omeji dolge `WALK` robove, zmanjša prestopno kazen |
| Trenutno vreme: temperatura | da | podraži `WALK`/`BIKE` pri mrazu, hladnem vremenu ali vročini |
| Trenutno vreme: veter | ne neposredno | uporabljen pri ML napovedih v odgovoru |
| MBajk trenutno stanje | posredno | določi, ali `BIKE` rob obstaja |
| MBajk ML napoved | ne | doda se v `bikePrediction`, ni strošek |
| Napoved zamude busa | ne | doda se v `busDelayPrediction`, ni strošek |
| Google Routes navigacija | ne | uporablja se za obogatitev etap, ne za izbiro poti |

