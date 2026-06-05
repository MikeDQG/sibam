# ML modeli

Ta dokument opisuje modele, značilke in izhode, ki jih uporablja SibaM.

## MBajk modeli

Skripta: [train_bikes.py](../../ml/train_bikes.py)

Trenirajo se štirje modeli:

| Model | Tip | Cilj |
| ----- | --- | ---- |
| `model_bikes.onnx` | `GradientBoostingRegressor` | napove število razpoložljivih koles |
| `model_stands.onnx` | `GradientBoostingRegressor` | napove število prostih stojal |
| `model_available_bike.onnx` | `GradientBoostingClassifier` | napove verjetnost, da sta na voljo vsaj 2 kolesi |
| `model_available_stand.onnx` | `GradientBoostingClassifier` | napove verjetnost, da sta na voljo vsaj 2 stojali |

### Značilke

Vrstni red značilk je pomemben, ker enak vrstni red uporablja tudi Java backend pri ONNX inferenci:

| Značilka | Opis |
| -------- | ---- |
| `number` | številka MBajk postaje |
| `hour` | ura v lokalnem času |
| `day_of_week` | dan v tednu, kjer je ponedeljek 0 |
| `is_weekend` | 1 za soboto/nedeljo, sicer 0 |
| `temperature` | temperatura iz vremenskega snapshota |
| `rain` | količina padavin; manjkajoča vrednost se nastavi na 0 |
| `wind_speed` | hitrost vetra |

### Ciljne spremenljivke

| Cilj | Izvor |
| ---- | ----- |
| `bikes` | število razpoložljivih koles v snapshotu |
| `stands` | število prostih stojal v snapshotu |
| `bike_available` | 1, če je `bikes >= 2`, sicer 0 |
| `stand_available` | 1, če je `stands >= 2`, sicer 0 |

### Evalvacija

Regresijska modela izpišeta `MAE`, klasifikacijska modela pa `ROC-AUC`. Delitev učne/testne množice je naključna z `test_size=0.2` in `random_state=42`.

## Model zamude avtobusa

Skripta: [train_buses.py](../../ml/train_buses.py)

Trenutni model je:

| Model | Tip | Cilj |
| ----- | --- | ---- |
| `model_bus_delay.onnx` | `Ridge` regresija | napoved zamude avtobusa v sekundah |

### Značilke

| Značilka | Opis |
| -------- | ---- |
| `route_id_enc` | ID linije, pretvorjen v število |
| `stop_sequence` | zaporedna številka postaje na vožnji |
| `hour` | ura v lokalnem času |
| `day_of_week` | dan v tednu, kjer je ponedeljek 0 |
| `is_weekend` | 1 za soboto/nedeljo, sicer 0 |
| `temperature` | temperatura iz vremenskega snapshota |
| `rain` | količina padavin |
| `wind_speed` | hitrost vetra |
| `direction` | smer linije iz `stop_direction_mapping.json` |

Cilj je `delay_seconds`.

### Čiščenje podatkov

Pred treningom se odstranijo vrstice:

- z zamudo manj kot -900 sekund ali več kot 3600 sekund;
- brez znane smeri (`direction == -1`);
- z uro `0`.

### Evalvacija

Model uporablja časovno delitev podatkov: prvih 80 % zapisov po `recorded_at` je trening množica, zadnjih 20 % pa test množica. To zmanjša tveganje data leakage, ker se model ne testira na naključno premešanih zapisih iz istega časovnega obdobja.

## Direction mapping

Datoteka:

- [ml/stop_direction_mapping.json](../../ml/stop_direction_mapping.json)
- [backend/src/main/resources/stop_direction_mapping.json](../../backend/src/main/resources/stop_direction_mapping.json)

Mapping uporablja ključ oblike:

```text
<route_id>_<stop_id>
```

Vrednost je celoštevilska oznaka smeri. Pri treningu se mapping uporabi za značilko `direction`; pri inferenci ga uporablja `BusDelayPredictionService`. Če smer ni najdena, backend trenutno vrne napoved `0`.

## ONNX inferenca

Backend uporablja ONNX Runtime:

- `BikePredictionService` sestavi tensor oblike `{1, 7}`;
- `BusDelayPredictionService` sestavi tensor oblike `{1, 9}`;
- vhodno ime tensorja je `float_input`.

Zato morata trening skripti in backend vedno uporabljati isti vrstni red značilk.
