# API – Usmerjanje

Osnovni URL: `/compute`

Javen endpoint — ne zahteva avtentikacije. Izračuna alternativne poti med izvorom in ciljem z uporabo internega grafa. Podpira kombinacije hoje, kolesarjenja (MBajk) in avtobusa (Marprom).

---

## GET `/compute` — izračun alternativnih poti

**Avtentikacija:** javno dostopno

### Parametri poizvedbe

| Parameter | Tip | Obvezno | Opis |
| --- | --- | --- | --- |
| `originLat` | double | da | Zemljepisna širina izvora |
| `originLon` | double | da | Zemljepisna dolžina izvora |
| `destinationLat` | double | da | Zemljepisna širina cilja |
| `destinationLon` | double | da | Zemljepisna dolžina cilja |
| `originAddress` | String | ne | Naslov izvora za prikaz (camelCase ali `origin_address`) |
| `destinationAddress` | String | ne | Naslov cilja za prikaz (camelCase ali `destination_address`) |
| `leaveNow` | boolean | da | Če `true`, se kot čas odhoda uporabi trenutni čas |
| `leaveAt` | String | ne | Čas odhoda v formatu `HH:mm` (npr. `08:30`); ignoriran, če je `leaveNow=true` |
| `arriveBy` | String | ne | Zahtevani čas prihoda v formatu `HH:mm`; aktivira način `ARRIVE_BY` |
| `date` | String | ne | Datum usmerjanja v formatu `yyyy-MM-dd`; privzeto je danes |
| `bike` | boolean | da | Vključi MBajk kolesarski način |
| `bus` | boolean | da | Vključi avtobusni način (Marprom) |
| `userId` | String | ne | Trenutno neuporabljeno — ne vpliva na rezultat |

**Časovni način:** `arriveBy` ima prednost pred `leaveAt`. Če nobeno ni podano in `leaveNow=false`, se uporabi `leaveNow` semantika s trenutnim časom.

**Naslov:** parameter je sprejet v obeh oblikah (`originAddress` in `origin_address`). camelCase ima prednost.

**Graf:** ob klicu z `bike=true` se graf osveži z najnovejšimi podatki o razpoložljivosti koles.

---

### Odgovor `200 OK` — najdene alternative

```json
{
  "status": "ok",
  "origin": { "lat": 46.5547, "lon": 15.6459 },
  "origin_address": "Ulica 1, Maribor",
  "destination": { "lat": 46.5600, "lon": 15.6500 },
  "destination_address": "Ulica 2, Maribor",
  "routes": [
    {
      "rank": 1,
      "label": "BUS",
      "labels": ["BUS"],
      "totalDurationSeconds": 1080,
      "totalDistanceMeters": 2400,
      "modes": ["WALK", "BUS", "WALK"],
      "legs": [ ... ]
    }
  ]
}
```

### Odgovor `200 OK` — pot ni najdena

```json
{
  "status": "not_found",
  "origin": { ... },
  "destination": { ... },
  "routes": []
}
```

### Odgovor `400 Bad Request` — izvor ali cilj izven območja

```json
{
  "status": "error",
  "code": "IZVEN_OBMOCJA_POTI",
  "message": "Izvor je preveč oddaljen od grafa.",
  "endpoint": "origin",
  "distanceMeters": 1523.4,
  "maxDistanceMeters": 500
}
```

| Polje | Opis |
| --- | --- |
| `code` | Vedno `IZVEN_OBMOCJA_POTI` |
| `endpoint` | `"origin"` ali `"destination"` |
| `distanceMeters` | Dejanska razdalja od najbližjega vozlišča grafa |
| `maxDistanceMeters` | Dovoljena največja razdalja |

---

### Struktura `RouteAlternative`

| Polje | Tip | Opis |
| --- | --- | --- |
| `rank` | int | Razvrstitev alternative (1 = najboljša) |
| `label` | String | Kratica prevladujočega načina (npr. `"BUS"`, `"BIKE"`, `"WALK"`) |
| `labels` | String[] | Seznam vseh načinov v poti |
| `totalDurationSeconds` | long | Skupni čas v sekundah |
| `totalDistanceMeters` | int | Skupna razdalja v metrih |
| `modes` | String[] | Zaporedje načinov po etapah |
| `legs` | Leg[] | Posamezne etape poti |

### Struktura `Leg`

| Polje | Tip | Opis |
| --- | --- | --- |
| `mode` | String | `WALK`, `BUS`, `BIKE`, `TRANSFER` |
| `origin` | GeoPoint | Začetna točka etape |
| `destination` | GeoPoint | Končna točka etape |
| `duration` | String | Trajanje etape (npr. `"5 min"`) |
| `distance` | String | Razdalja etape (npr. `"400 m"`) |
| `polyline` | GeoPoint[] | Točke za izris na karti |
| `code` | String | Številka avtobusne linije (samo pri `BUS`) |
| `headsignName` | String | Destinacija avtobusa (samo pri `BUS`) |
| `departure` | String | Čas odhoda (samo pri `BUS`) |
| `arrival` | String | Čas prihoda (samo pri `BUS`) |
| `freeStands` | String | Prosta stojala (samo pri `BIKE`) |
| `freeBikes` | String | Prosta kolesa (samo pri `BIKE`) |
| `navigationAvailable` | Boolean | Ali so koraki za navigacijo na voljo (samo pri `BIKE`) |
| `steps` | NavigationStep[] | Koraki za navigacijo (samo pri `BIKE`, če je `navigationAvailable=true`) |
| `bikePrediction` | BikeLegPredictionVao | Napoved razpoložljivosti koles ob prihodu (samo pri `BIKE`) |
| `busDelayPrediction` | BusLegDelayVao | Napoved zamude avtobusa (samo pri `BUS`) |

---

## HTTP statusi

| Status | Razlog |
| --- | --- |
| `200 OK` | Uspešen izračun (tudi če `routes` je prazen) |
| `400 Bad Request` | Izvor ali cilj je izven območja grafa |
