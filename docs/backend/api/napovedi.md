# API – Napovedi

Javna endpointa — ne zahtevata avtentikacije. Oba vračata napovedi na podlagi ONNX modelov, naloženih ob zagonu backenda. Za podrobnosti o inferenčni plasti glej [`napovedi/ml-inference.md`](../napovedi/ml-inference.md). Za opis treninga modelov glej [`docs/strojno-ucenje/`](../../strojno-ucenje/kazalo.md).

---

## POST `/predict/bikes` — napoved razpoložljivosti koles

Napove število razpoložljivih koles in stojal ter verjetnost razpoložljivosti na izbrani postaji MBajk za podani čas in vremenske pogoje.

**Avtentikacija:** javno dostopno

**Telo zahtevka:**

```json
{
  "stationNumber": 24,
  "hour": 8,
  "dayOfWeek": 1,
  "isWeekend": 0,
  "temperature": 18.5,
  "rain": 0.0,
  "windSpeed": 3.2
}
```

| Polje | Tip | Opis |
| --- | --- | --- |
| `stationNumber` | int | ID postaje MBajk (enako kot `number` v MBajk API-ju) |
| `hour` | int | Ura dneva (0–23) |
| `dayOfWeek` | int | Dan v tednu (0 = ponedeljek, 6 = nedelja) |
| `isWeekend` | int | `1` če je vikend, `0` sicer |
| `temperature` | float | Temperatura v °C |
| `rain` | float | Količina padavin v mm |
| `windSpeed` | float | Hitrost vetra v m/s |

**Odgovor `200 OK`:**

```json
{
  "predictedBikes": 5,
  "predictedStands": 3,
  "bikeAvailableProbability": 0.87,
  "standAvailableProbability": 0.61
}
```

| Polje | Tip | Opis |
| --- | --- | --- |
| `predictedBikes` | int | Napovedano število prostih koles |
| `predictedStands` | int | Napovedano število prostih stojal |
| `bikeAvailableProbability` | double | Verjetnost, da bo vsaj eno kolo razpoložljivo (0–1) |
| `standAvailableProbability` | double | Verjetnost, da bo vsaj eno stojalo razpoložljivo (0–1) |

---

## POST `/api/bus-delay/predict` — napoved zamude avtobusa

Napove zamudo avtobusa v sekundah za podano linijo, postajo, čas in vremenske pogoje.

**Avtentikacija:** javno dostopno

**Telo zahtevka:**

```json
{
  "routeId": 67,
  "stopSequence": 5,
  "hour": 8,
  "dayOfWeek": 1,
  "isWeekend": 0,
  "temperature": 18.5,
  "rain": 0.0,
  "windSpeed": 3.2,
  "stopId": 1042
}
```

| Polje | Tip | Opis |
| --- | --- | --- |
| `routeId` | int | ID linije Marprom |
| `stopSequence` | int | Zaporedna številka postaje na liniji |
| `hour` | int | Ura dneva (0–23) |
| `dayOfWeek` | int | Dan v tednu (0 = ponedeljek, 6 = nedelja) |
| `isWeekend` | int | `1` če je vikend, `0` sicer |
| `temperature` | float | Temperatura v °C |
| `rain` | float | Količina padavin v mm |
| `windSpeed` | float | Hitrost vetra v m/s |
| `stopId` | int | ID postajališča — uporabljen za določitev smeri linije iz `stop_direction_mapping.json`; če ni najden, se uporabi `direction=0` |

**Odgovor `200 OK`:**

```json
{
  "predictedDelaySeconds": 45
}
```

| Polje | Tip | Opis |
| --- | --- | --- |
| `predictedDelaySeconds` | int | Napovedana zamuda v sekundah |

---

## HTTP statusi

| Status | Razlog |
| --- | --- |
| `200 OK` | Uspešna napoved |
| `500 Internal Server Error` | ONNX model ni naložen ali je prišlo do napake pri inferenci |
