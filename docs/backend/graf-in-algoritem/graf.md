# Routing graf

Dokument opisuje interni graf, iz katerega backend računa multimodalne poti. Graf združuje Marprom postajališča, MBajk postaje in povezave za hojo, kolo ter avtobus.

Glavne implementacije:

| Področje | Razred |
| --- | --- |
| Model grafa | [`Graph`](../../../backend/src/main/java/com/sibam/graph/model/Graph.java) |
| Vozlišča | [`Node`](../../../backend/src/main/java/com/sibam/graph/model/Node.java), [`BusNode`](../../../backend/src/main/java/com/sibam/graph/model/BusNode.java), [`BikeNode`](../../../backend/src/main/java/com/sibam/graph/model/BikeNode.java), [`LocationNode`](../../../backend/src/main/java/com/sibam/graph/model/LocationNode.java) |
| Robovi | [`Edge`](../../../backend/src/main/java/com/sibam/graph/model/Edge.java), [`EdgeType`](../../../backend/src/main/java/com/sibam/graph/model/EdgeType.java) |
| Gradnja grafa | [`StaticGraphBuilder`](../../../backend/src/main/java/com/sibam/graph/builder/StaticGraphBuilder.java) |
| Inicializacija | [`GraphBootstrap`](../../../backend/src/main/java/com/sibam/graph/bootstrap/GraphBootstrap.java) |
| Hramba aktivnega grafa | [`InMemoryGraphStore`](../../../backend/src/main/java/com/sibam/graph/storage/InMemoryGraphStore.java) |
| Serializacija cache-a | [`KryoGraphSerializer`](../../../backend/src/main/java/com/sibam/graph/storage/KryoGraphSerializer.java) |
| Prostorsko iskanje | [`SpatialSearchService`](../../../backend/src/main/java/com/sibam/graph/spatial/SpatialSearchService.java), [`RTreeIndex`](../../../backend/src/main/java/com/sibam/graph/spatial/RTreeIndex.java) |

---

## Struktura grafa

`Graph` je usmerjen graf z dvema glavnima zbirkama:

| Polje | Tip | Namen |
| --- | --- | --- |
| `nodes` | `Map<Integer, Node>` | vse točke, po katerih lahko poteka pot |
| `adjacencyList` | `Map<Integer, List<Edge>>` | izhodni robovi za vsako vozlišče |

`getNeighbors(nodeId)` vrne izhodne robove vozlišča ali prazen seznam, če vozlišče nima sosedov.

Graf je serializabilen, ker se shrani v binarni cache. Trenutna implementacija serializatorja se imenuje `KryoGraphSerializer`, vendar uporablja Java object serialization.

---

## Tipi vozlišč

| Tip | Izvor podatkov | ID pravilo | Uporaba |
| --- | --- | --- | --- |
| `BusNode` | Marprom `BusStopVao` | ID postajališča iz Marprom podatkov | vstop, izstop in prestopi na avtobus |
| `BikeNode` | MBajk `BikeStationVao` | `1_000_000 + station.number()` | prevzem in vračilo MBajk koles |
| `LocationNode` | koordinate iz uporabniškega zahtevka | `-1` za izvor, `-2` za cilj | začasni vozlišči samo za en izračun |

`LocationNode` ni del statičnega grafa. `AStarRouter` ga doda v kopijo grafa ob posameznem zahtevku in ga poveže z najbližjimi vozlišči z `WALK` robovi.

---

## Tipi robov

| Tip roba | Smer | Vsebina | Kdaj nastane |
| --- | --- | --- | --- |
| `WALK` | dvosmerno za statične povezave, usmerjeno za začasne povezave izvora/cilja | razdalja, osnovni čas hoje | med bližnjimi vozlišči in od/do uporabnikove lokacije |
| `BIKE` | usmerjeno | razdalja, osnovni čas kolesarjenja | med MBajk postajami, če sta kolo in stojalo razpoložljiva |
| `BUS` | usmerjeno | razdalja, osnovni čas vožnje, `RouteInfo`, polilinija, `scheduleStopPointId` | med zaporednimi postajami na Marprom trasi |
| `TRANSFER` | logični izhodni model | razdalja `0`, trajanje čakanja | ustvari se pri sestavljanju odgovora, ni statični rob grafa |

`Edge` vedno hrani:

| Polje | Opis |
| --- | --- |
| `fromNodeId`, `toNodeId` | začetek in konec usmerjenega roba |
| `edgeType` | `WALK`, `BIKE`, `BUS` ali `TRANSFER` |
| `distanceMeters` | razdalja v metrih |
| `costSeconds` | osnovni strošek v sekundah |
| `routeInfo` | metapodatki avtobusne linije, samo pri `BUS` |
| `polyline` | točke za izris etape |
| `scheduleStopPointId` | postaja za iskanje odhodov v voznem redu |

Podrobne formule za osnovne stroške robov so v [`algoritem-usmerjanja.md`](algoritem-usmerjanja.md).

---

## Gradnja statičnega grafa

`StaticGraphBuilder.build()` zgradi graf v tem vrstnem redu:

1. Preveri, ali so VAO podatki za Marprom že naloženi; če niso, pokliče `vaoSerializer.fetchData()`.
2. Iz `BusStopVao` ustvari `BusNode` vozlišča.
3. Iz Marprom tras ustvari `BUS` robove med zaporednimi postajami.
4. Iz MBajk podatkov ustvari `BikeNode` vozlišča z zadnjo znano razpoložljivostjo.
5. Med MBajk postajami ustvari `BIKE` robove.
6. Med vsemi bližnjimi vozlišči ustvari dvosmerne `WALK` robove.

### BUS robovi

Za vsako traso se `shapeNodes` uredijo po `sequenceNo`. Builder nato sešteva razdaljo po shape točkah med dvema zaporednima postajama.

Rob dobi:

| Podatek | Vir |
| --- | --- |
| `distanceMeters` | seštevek haversine razdalj po shape točkah |
| `costSeconds` | `distanceMeters / 6.0 m/s` |
| `RouteInfo` | `lineId`, `routeId`, `headsignName`, `code` |
| `polyline` | shape točke med postajama |
| `scheduleStopPointId` | ID začetne postaje roba |

Če trasa nima shape točk ali shape točka nima veljavnega `stopPointId`, se tak segment preskoči.

### BIKE robovi

`BikeNode` uporablja stanje iz MBajk razpoložljivosti:

| Pogoj | Posledica |
| --- | --- |
| začetna postaja ima `freeBikes <= 0` | iz nje se ne ustvarijo `BIKE` robovi |
| ciljna postaja ima `freeStands <= 0` | vanjo se ne ustvari `BIKE` rob |
| začetna in ciljna postaja sta isti | rob se ne ustvari |

Razpoložljivost MBajk tako vpliva na topologijo grafa. Če se stanje koles spremeni, mora biti graf ponovno zgrajen, da se spremenijo tudi `BIKE` robovi.

### WALK robovi

`WALK` robovi se dodajo med vsemi pari vozlišč, ki so po haversine razdalji oddaljeni največ `helperService.getMaxWalkingDistanceMeters()`. Trenutna konstanta v `HelperService` je `500 m`.

Robovi so dvosmerni:

```text
A -> B
B -> A
```

Za začasni izvor in cilj uporabnika `AStarRouter` doda samo robove, ki so potrebni za zahtevek:

```text
origin -> najbližja vozlišča
najbližja ciljna vozlišča -> destination
origin -> destination
```

---

## Inicializacija in cache

`GraphBootstrap` se sproži na `ApplicationReadyEvent` z `@Order(200)`.

Tok inicializacije:

1. Če graf cache obstaja, ga poskusi naložiti.
2. Če cache manjka ali nalaganje vrne `null`, zgradi nov graf.
3. Nov graf shrani v cache.
4. Aktivni graf zamenja v `GraphStore`.

`KryoGraphSerializer` uporablja dve cache datoteki:

| Pot | Vsebina |
| --- | --- |
| `marprom/graph/graph.bin` | serializiran `Graph` |
| `marprom/graph/manifest.json` | manifest z SHA-256 hash vrednostjo in izvorom artefakta |

Če je `graph.cache.enabled=false`, se cache ne uporablja in graf se gradi iz podatkov.

`exists()` najprej preveri lokalni cache. Če lokalni cache ni veljaven, poskusi obnovitev iz Supabase artefaktnega cache-a.

---

## Hramba aktivnega grafa

`InMemoryGraphStore` hrani graf v `AtomicReference<Graph>`.

| Operacija | Obnašanje |
| --- | --- |
| `getGraph()` | vrne trenutno aktivno instanco grafa |
| `replaceGraph(graph)` | atomarno zamenja aktivno instanco |

To omogoča, da se graf ponovno zgradi in zamenja brez ročnega spreminjanja obstoječe instance. Zahtevki, ki so že dobili referenco na star graf, lahko zaključijo s staro instanco; novi zahtevki dobijo novo.

---

## Osveževanje grafa

`GraphBootstrap.refresh()` vedno:

1. zgradi nov graf,
2. ga shrani v cache,
3. zamenja aktivni graf v `GraphStore`.

API za usmerjanje ob zahtevku z `bike=true` osveži graf, da `BikeNode` in `BIKE` robovi čim bolj odražajo trenutno MBajk stanje.

---

## Prostorsko iskanje

`SpatialSearchService` uporablja `RTreeIndex` za najbližja vozlišča.

Indeks se rebuilda samo, ko se spremeni instanca grafa:

```text
če indexedGraph == graph:
    uporabi obstoječi R-tree
sicer:
    zgradi nov R-tree iz graph.nodes.values()
```

`RTreeIndex`:

| Lastnost | Vrednost |
| --- | --- |
| največ otrok na drevesno vozlišče | `16` |
| gradnja | listi se urejajo izmenično po lat/lon in združujejo v starše |
| iskanje | priority queue po minimalni razdalji do bounding boxa |
| rezultat | `limit` najbližjih vozlišč, urejenih po haversine razdalji |

Pri routingu se uporablja limit `5`, da se začasni izvor in cilj povežeta z več kandidati, ne samo z enim najbližjim vozliščem.

---

## Znane omejitve

- `KryoGraphSerializer` po imenu namiguje na Kryo, vendar trenutno uporablja Java object serialization.
- Statični `WALK` robovi so zgrajeni z dvojnim prehodom čez vsa vozlišča, zato je gradnja občutljiva na rast števila vozlišč.
- `BIKE` robovi so odvisni od trenutnega stanja MBajk ob gradnji grafa; napovedi razpoložljivosti se uporabijo v API odgovoru, ne pri gradnji robov.
- `TRANSFER` ni statični rob v grafu, temveč etapa, ki nastane pri pretvorbi najdene poti v odgovor.
