# ML dokumentacija

Ta mapa opisuje strojno učenje in podatkovni tok v aplikaciji SibaM.

## Dokumenti

- [Podatkovni tok](podatkovni-tok.md) - kako podatki pridejo iz zunanjih API-jev v bazo, Supabase Storage in modele.
- [Modeli](modeli.md) - kateri modeli obstajajo, katere značilke uporabljajo in kaj napovedujejo.
- [Operativni postopek](operativni-postopek.md) - kako se pipeline zažene, katere spremenljivke potrebuje in kaj preveriti ob napakah.

## Kratek povzetek

ML sistem uporablja tri podatkovne plasti v Supabase Storage:

| Plast | Namen | Primeri poti |
| ----- | ----- | ------------ |
| `bronze` | Surovi izvozi iz staging tabel | `bikes/snapshots_<date>.parquet`, `weather/weather_<date>.parquet`, `buses/trips_<date>.parquet` |
| `silver` | Združeni in očiščeni učni podatki | `bikes/latest.parquet`, `buses/latest.parquet` |
| `gold` | ONNX modeli za produkcijsko inferenco | `models/model_bikes.onnx`, `models/model_bus_delay.onnx` |

Trenutno sta implementirani dve ML področji:

- napoved razpoložljivosti MBajk koles in stojal;
- napoved zamude avtobusa.

Backend modele naloži iz `gold/models` ob zagonu aplikacije in jih uporablja prek ONNX Runtime.
