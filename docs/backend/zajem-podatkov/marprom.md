# Zajem podatkov — Marprom

Marprom podatki se zbirajo iz dveh virov z različnima namenoma:

| Vir                    | Odjemalec                              | Namen                                                                               |
| ---------------------- | -------------------------------------- | ----------------------------------------------------------------------------------- |
| Marprom proxy REST API | `MarpromClient` + `TransitDataService` | Statični podatki za gradnjo routing grafa (postajališča, linije, trase, vozni redi) |
| GTFS-RT protobuf feed  | `GTFSRTClient` + `GTFSRTDataService`   | Podatki o zamudah v realnem času za ML staging tabele                               |

---

## `MarpromClient`

Razred: [MarpromClient.java](../../../backend/src/main/java/com/sibam/integration/marprom/MarpromClient.java)

Reaktiven HTTP odjemalec (Spring WebClient) na proxy `https://marprom-proxy.derp.si/OBA`. Proxy ne zahteva API ključa. Buffer je nastavljen na 10 MB, ker so odgovori (zlasti `GetRoutes` s polilinjami) lahko veliki.

Vsi klici podpirajo datum v obliki `yyyy-MM-dd`. Preobremenitve brez datuma interně uporabijo današnji datum — te metode so označene s `TODO` in so namenjene samo začasni rabi.

| Metoda                                 | Endpoint                                           | Opis                                 |
| -------------------------------------- | -------------------------------------------------- | ------------------------------------ |
| `getAllStops()`                        | `GET /GetAllStopPoints`                            | Vsa postajališča v Mariboru          |
| `getLines(date)`                       | `GET /GetLines?Date=…`                             | Vse avtobusne linije za datum        |
| `getRoutes(lineId, date)`              | `GET /GetRoutes?lineId=…&Date=…&IncludeShape=true` | Trase linije z polilinijami za datum |
| `getStopScheduleForLine(lineId, date)` | `GET /GetStopPointSheduleForLine?lineId=…&Date=…`  | Vozni red po postajališčih za linijo |
| `getTrips(lineId, date)`               | `GET /GetTrips?lineId=…&Date=…&IncludeShape=true`  | Potovanja linije za datum            |

---

## `TransitDataService`

Razred: [TransitDataService.java](../../../backend/src/main/java/com/sibam/service/TransitDataService.java)

Blokirajoč servis (`.block()` na Mono) za uporabo pri gradnji routing grafa ob zagonu. Kliče `MarpromClient` in vrne deseializirane DTO objekte.

| Metoda                                  | Opis                                                                            |
| --------------------------------------- | ------------------------------------------------------------------------------- |
| `getBusStops()`                         | Vrne seznam vseh postajališč                                                    |
| `getLines(date?)`                       | Vrne vse linije za datum (privzeto danes)                                       |
| `getRoutes(lineId, date?)`              | Vrne trase posamezne linije                                                     |
| `getAllRoutes(date?)`                   | Zbere trase za vse linije — kliče `getLines()` in za vsako linijo `getRoutes()` |
| `getStopScheduleForLine(lineId, date?)` | Vrne vozni red postajališč za linijo                                            |
| `getTrips(lineId, date?)`               | Vrne potovanja linije                                                           |

`getAllRoutes()` je najdražja operacija — naredi N+1 klicev (en za linije, en za vsako linijo). Kliče se samo ob gradnji grafa, ne med normalnim delovanjem.

---

## `GTFSRTClient`

Razred: [GTFSRTClient.java](../../../backend/src/main/java/com/sibam/integration/gtfsRT/GTFSRTClient.java)

Blokirajoč HTTP odjemalec, ki pridobi GTFS-RT protobuf sporočila iz `https://rt.gtfs.derp.si/sources/marprom/`.

| Metoda                  | Endpoint             | Opis                                                         |
| ----------------------- | -------------------- | ------------------------------------------------------------ |
| `getVehiclePositions()` | `/vehicle_positions` | Trenutne pozicije vozil — koordinate, smer, trenutna postaja |
| `getTripUpdates()`      | `/trip_updates`      | Napovedi zamud po postajališčih za aktivna potovanja         |

Oba odgovora sta v binarnem protobuf formatu (`GtfsRealtime.FeedMessage`). Odjemalec ju deserializira in vrne neposredno. Ob omrežni napaki vrže `RuntimeException`.

---

## `GTFSRTDataService`

Razred: [GTFSRTDataService.java](../../../backend/src/main/java/com/sibam/service/GTFSRTDataService.java)

### `getRealtimeTrips()`

Združi podatke iz obeh GTFS-RT feedov v seznam `Trip` objektov:

```
getVehiclePositions()  → seznam VehiclePosition
getTripUpdates()       → slovar TripUpdate po ključu "{tripId}_{vehicleId}"

za vsak VehiclePosition:
  poišči TripUpdate po ključu
  → GTFSRTMapper.gtfsRTToTrip(vehicle, update)  → Trip
```

Ob podvojenih ključih v `trip_updates` se ohrani tisti z novejšim `timestamp`.

### `ingestRealtimeTrips(OffsetDateTime fetchedAt)`

Za vsak `Trip` iz `getRealtimeTrips()`:

```
ustvari TripEntity → shrani → pridobi ID
za vsak StopUpdate v trip.stopUpdates:
  ustvari StopDelayEntity z referenco na TripEntity → shrani
```

En klic producira en `TripEntity` in N `StopDelayEntity` zapisov (po ena za vsako prihodnjo postajo z zamudo).

---

## Scheduler

Scheduler: [SchedulerService.java](../../../backend/src/main/java/com/sibam/scheduler/SchedulerService.java), metoda `fetchBusIngestion()`

| Lastnost         | Vrednost                                 |
| ---------------- | ---------------------------------------- |
| Interval         | vsako minuto (`fixedRate = 60 000 ms`)   |
| Deluje med       | 05:00–23:00 (po lokalnem času Ljubljana) |
| Config zastavica | `schedulers.fetch-bus-ingestion.on`      |

Scheduler zunaj operativnih ur ne kliče servisa. Izjeme so ujete in zapisane v log.

---

## Znane omejitve

- `TransitDataService` vsebuje `System.out.println` izpise namenjene razvoju — niso nadomestek za strukturirano logiranje.
- Preobremenitve brez datumskega parametra v `MarpromClient` interně generirajo današnji datum; označene so s `TODO` za kasnejšo refaktoracijo.
- `GTFSRTClient` uporablja blokirajoče `.block()` na WebClient — primerno za redke klice (1×/min), ne za visoko-frekvenčne scenarije.
