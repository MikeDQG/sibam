# ML inferenca

Backend naloži vnaprej natrenirane ONNX modele ob zagonu in jih uporablja za napovedi v realnem času. Modeli se prenesejo iz Supabase Storage bucket `gold/models/` — backend sam ne trenira modelov.

Za opis treninga, podatkovnega toka in evaluacije modelov glej [`docs/strojno-ucenje/`](../../strojno-ucenje/kazalo.md). Za HTTP vmesnik napovedi glej [`api/napovedi.md`](../api/napovedi.md).

---

## Nalaganje modelov ob zagonu

Razred: [SupabaseStorageClient.java](../../../backend/src/main/java/com/sibam/integration/supabase/SupabaseStorageClient.java)

Oba servisna razreda naložita modele v metodi z `@PostConstruct`, ki se izvede takoj po inicializaciji Spring konteksta. Prenos poteka prek Supabase Storage REST API s `service-role-key` žetonom:

```
GET {supabase.url}/storage/v1/object/gold/models/{filename}
Authorization: Bearer <service-role-key>
```

Model se prenese kot bajt niz in se posreduje neposredno v `OrtEnvironment.createSession(bytes, ...)` — ne shrani se na disk. Če prenos kateregakoli modela ne uspe (HTTP != 200 ali omrežna napaka), aplikacija ob zagonu vrže izjemo in ne zažene.

Po zagonu modeli ostanejo v pomnilniku kot `OrtSession` objekti in se vsako noč samodejno zamenjajo z novimi (glej spodaj).

---

## Nočno posodabljanje modelov

Razred: [SchedulerService.java](../../../backend/src/main/java/com/sibam/scheduler/SchedulerService.java)

`SchedulerService` vsak dan ob **3:30 UTC** (2,5h po zagonu ML pipeline-a ob 1:00 UTC) pokliče `reloadModels()` in `reloadModel()` na obeh servisih. Vsak servis prenese nove modele iz Supabase Storage, nato zapre stare `OrtSession` in zamenja reference.

Metodi `predict` in `reload` sta na obeh servisih označeni kot `synchronized`. Java zagotavlja, da se naenkrat izvaja samo ena izmed njiju — obe si delita isti zaklenjen objekt (instanca servisa). To pomeni:

- Če inferenca teče, bo reload počakal, da se zaključi, in šele nato zamenjal sejo.
- Če reload teče, bo naslednji klic `predict` počakal, da reload dokonča zamenjavo, in šele nato prebral novo sejo.

Brez tega bi lahko prišlo do situacije, kjer reload zapre staro `OrtSession` medtem, ko jo inferenca še aktivno uporablja — kar bi povzročilo izjemo pri zagonu ONNX modela.

**Obnašanje ob napaki reloada:** če prenos kateregakoli modela iz Supabase ne uspe (omrežna napaka, HTTP != 200), `reloadModels()` oziroma `reloadModel()` vržeta izjemo. Scheduler jo ujame, zapiše v log in nadaljuje. Stare `OrtSession` ostanejo nedotaknjene in aktivne — backend normalno streže napovedi z modeli iz prejšnjega reloada oziroma iz zagona.

---

## Napovedi za kolesa — `BikePredictionService`

Razred: [BikePredictionService.java](../../../backend/src/main/java/com/sibam/service/BikePredictionService.java)

### Modeli

Ob zagonu se naložijo štirje ONNX modeli iz `gold/models/`:

| Model                          | Datoteka                     | Tip          | Izhod                                   |
| ------------------------------ | ---------------------------- | ------------ | --------------------------------------- |
| Napoved prostih koles          | `model_bikes.onnx`           | Regressor    | `float` — zaokrožen na `int`, minimum 0 |
| Napoved prostih stojal         | `model_stands.onnx`          | Regressor    | `float` — zaokrožen na `int`, minimum 0 |
| Verjetnost dostopnosti kolesa  | `model_available_bike.onnx`  | Klasifikator | verjetnost razreda `1` (0–1)            |
| Verjetnost dostopnosti stojala | `model_available_stand.onnx` | Klasifikator | verjetnost razreda `1` (0–1)            |

### Vhodne značilke

Vhodni tenzor oblike `[1, 7]` — en vzorec, sedem značilk v točno tem vrstnem redu:

| Indeks | Značilka        | Tip             |
| ------ | --------------- | --------------- |
| 0      | `stationNumber` | float           |
| 1      | `hour`          | float           |
| 2      | `dayOfWeek`     | float           |
| 3      | `isWeekend`     | float (0 ali 1) |
| 4      | `temperature`   | float (°C)      |
| 5      | `rain`          | float (mm)      |
| 6      | `windSpeed`     | float (m/s)     |

### Inferenca

- **Regressor** (`runRegressor`): bere `output[0][0]` iz prvega izhoda seje, zaokroži z `Math.round`, omeji na minimum 0.
- **Klasifikator** (`runClassifier`): bere verjetnostni slovar iz drugega izhoda seje (`result.get(1)`), vrne verjetnost razreda `1L`.

---

## Napoved zamude avtobusa — `BusDelayPredictionService`

Razred: [BusDelayPredictionService.java](../../../backend/src/main/java/com/sibam/service/BusDelayPredictionService.java)

### Model

En ONNX model iz `gold/models/model_bus_delay.onnx` — regressor, ki vrne zamudo v sekundah.

### Preslikava smeri (`stop_direction_mapping.json`)

Ob zagonu se iz `resources/stop_direction_mapping.json` naloži preslikava ključev `"{routeId}_{stopId}"` → `direction` (celo število). `direction` je odvisen od smeri vožnje na liniji in je izpeljan iz podatkov Marprom v fazi treninga.

Če ključ `"{routeId}_{stopId}"` ni v preslikavi, servis **takoj vrne `0`** brez klica ONNX modela. To je tiha degradacija — napovedi za neznane kombinacije linije in postaje so vedno 0.

### Vhodne značilke

Vhodni tenzor oblike `[1, 9]`:

| Indeks | Značilka       | Tip                   |
| ------ | -------------- | --------------------- |
| 0      | `routeId`      | float                 |
| 1      | `stopSequence` | float                 |
| 2      | `hour`         | float                 |
| 3      | `dayOfWeek`    | float                 |
| 4      | `isWeekend`    | float (0 ali 1)       |
| 5      | `temperature`  | float (°C)            |
| 6      | `rain`         | float (mm)            |
| 7      | `windSpeed`    | float (m/s)           |
| 8      | `direction`    | float (iz preslikave) |

### Inferenca

Izhod `output[0][0]` zaokrožen z `Math.round` — rezultat so sekunde zamude.

---

## Integracija v routing odgovor

Napovedi se vgradijo neposredno v posamezne etape (`Leg`) odgovora `/compute`:

| VAO                    | Polje v `Leg`        | Kdaj je prisotno                 |
| ---------------------- | -------------------- | -------------------------------- |
| `BikeLegPredictionVao` | `bikePrediction`     | samo pri etapah z načinom `BIKE` |
| `BusLegDelayVao`       | `busDelayPrediction` | samo pri etapah z načinom `BUS`  |

Polja so anotirana z `@JsonInclude(NON_NULL)` — če napoved ni izvedena, polje v JSON odgovoru ne nastopa.

### `BikeLegPredictionVao`

| Polje                             | Tip     | Opis                                                |
| --------------------------------- | ------- | --------------------------------------------------- |
| `predictedBikesAtPickup`          | Integer | Napovedano število koles pri prevzemu               |
| `pickupBikeAvailableProbability`  | Double  | Verjetnost, da bo kolo razpoložljivo pri prevzemu   |
| `predictedStandsAtReturn`         | Integer | Napovedano število stojal pri vrnitvi               |
| `returnStandAvailableProbability` | Double  | Verjetnost, da bo stojalo razpoložljivo pri vrnitvi |

### `BusLegDelayVao`

| Polje                           | Tip     | Opis                                                     |
| ------------------------------- | ------- | -------------------------------------------------------- |
| `predictedBoardingDelaySeconds` | Integer | Napovedana zamuda avtobusa ob vstopni postaji v sekundah |

---

## Znane omejitve

- `stop_direction_mapping.json` se ob nočnem reloadu ne posodobi — je statična datoteka v `resources/`; posodobitev zahteva nov deployment.
- Ob neuspešnem reloadu scheduler le zapiše napako v log; ni alarma ali ponovnega poskusa.
- Neznane kombinacije `routeId` + `stopId` vrnejo zamudo 0 brez opozorila.
- Ni validacije vhodnih vrednosti pred inferenco — vrednosti izven treniranega obsega lahko dajo nesmiselne napovedi.
