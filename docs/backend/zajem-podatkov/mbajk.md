# Zajem podatkov — MBajk

Backend vsakih 5 minut pridobi trenutno stanje vseh MBajk postaj in ga shrani v bazo. Podatki se zbirajo za dva namena: gradnjo routing grafa (razpoložljivost koles ob izračunu poti) in ML trening (časovne serije za napovedovanje razpoložljivosti).

---

## `MBajkClient`

Razred: [MBajkClient.java](../../../backend/src/main/java/com/sibam/integration/mbajk/MBajkClient.java)

Reaktiven HTTP odjemalec (Spring WebClient), ki kliče JCDecaux API.

**Endpoint:** `GET https://api.jcdecaux.com/vls/v3/stations?apiKey=…&contract=maribor`

**Ključ:** `MBAJK_API_KEY` iz okoljske spremenljivke (`mbajk.api.key`)

**Vrne:** reaktiven `Mono<List<BikeStopDto>>` — seznam vseh postaj v Mariboru z njihovim trenutnim stanjem.

Relevantna polja iz `BikeStopDto`:

| Polje                                        | Opis                                 |
| -------------------------------------------- | ------------------------------------ |
| `number`                                     | Unikatna številka postaje (MBajk ID) |
| `name`                                       | Ime postaje                          |
| `address`                                    | Naslov                               |
| `position.latitude/longitude`                | Koordinate                           |
| `totalStands.capacity`                       | Skupna zmogljivost stojal            |
| `totalStands.availabilities.bikes`           | Prosta kolesa                        |
| `totalStands.availabilities.stands`          | Prosta stojala                       |
| `totalStands.availabilities.mechanicalBikes` | Mehanska kolesa                      |
| `totalStands.availabilities.electricalBikes` | Električna kolesa                    |
| `status`                                     | Stanje postaje (`OPEN`, `CLOSED`, …) |

---

## `MBajkDataService`

Razred: [MBajkDataService.java](../../../backend/src/main/java/com/sibam/service/MBajkDataService.java)

### `ingestBikesData(OffsetDateTime fetchedAt)`

Pokliče `MBajkClient.getAllBikes()` in za vsako postajo asinhronо pokliče `saveBikeStop()`. Obdelava poteka na `Schedulers.boundedElastic()` — ne blokira scheduler threada.

### `saveBikeStop(BikeStopDto, OffsetDateTime)`

Za vsak DTO iz API odgovora:

```
findByNumber(dto.number())
  → obstaja  → vrne obstoječi BikeStation
  → ne obstaja → ustvari in shrani novega BikeStation

ustvari BikeStationSnapshot z recordedAt = fetchedAt
shrani snapshot
```

`BikeStation` se torej ustvari samo ob prvem zaznanju postaje. Koordinate, ime in zmogljivost se ob kasnejših klicih **ne posodobijo** — ker JCDecaux teh podatkov ne spreminja.

### `getBikeStationVaos()`

Vrne seznam `BikeStationVao` za gradnjo routing grafa. Za vsako postajo prebere zadnji `BikeStationSnapshot` in sestavi VAO z aktualnimi podatki o razpoložljivosti. Kliče se med gradnjo grafa ob vsakem `refresh()`.

---

## Scheduler

Scheduler: [SchedulerService.java](../../../backend/src/main/java/com/sibam/scheduler/SchedulerService.java), metoda `fetchBikeIngestion()`

| Lastnost         | Vrednost                                               |
| ---------------- | ------------------------------------------------------ |
| Interval         | vsakih 5 minut (`fixedRate = 300 000 ms`)              |
| Deluje med       | 05:00–23:00 (po lokalnem času Ljubljana)               |
| Aktiviran v prod | `schedulers.fetch-bike-ingestion.on=true` v CI profilu |
| Config zastavica | `schedulers.fetch-bike-ingestion.on`                   |

Scheduler zunaj operativnih ur ne kliče servisa. Izjeme so ujete in zapisane v log — scheduler se ne ustavi.
