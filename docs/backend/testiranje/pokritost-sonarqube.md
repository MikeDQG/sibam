# Pokritost in SonarQube

SonarCloud se zažene ob vsakem pushu na `main`. Pokritost meri JaCoCo. Izključitve so nastavljene v `pom.xml` z dvema ločenima lastnostima.

---

## `sonar.exclusions` — popolnoma izključeno

Datoteke, ki so v celoti izključene iz analize — SonarQube zanje ne prijavlja težav in jih ne vključi v pokritost.

| Vzorec                                                         | Razlog                                                |
| -------------------------------------------------------------- | ----------------------------------------------------- |
| `**/SibamApplication.java`                                     | Vstopna točka — ena vrstica kode                      |
| `**/repository/**`                                             | Spring Data vmesniki — implementacijo generira Spring |
| `**/dto/**`                                                    | Čisti zapisi za prenos podatkov brez logike           |
| `**/persistence/**`                                            | JPA entitete — samo getterji/setterji in anotacije    |
| `**/integration/**/GTFSRTClient.java` in ostali `*Client.java` | Tanki HTTP ovoji brez poslovne logike                 |

---

## `sonar.coverage.exclusions` — izključeno samo iz pokritosti

Datoteke so vključene v analizo kode (SonarQube še vedno prijavlja težave), a se ne upoštevajo pri izračunu odstotka pokritosti.

Vse zgornje + dodatno:

| Vzorec                                      | Razlog                                                                                                                         |
| ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `**/config/**`                              | Spring konfiguracijska koda — težko testirati brez polnega contexta                                                            |
| `**/graph/model/**`                         | Podatkovne strukture (vozlišča, robovi) — pokrite posredno prek višjih testov                                                  |
| `**/api/dataObjects/**`                     | Zapisi za ovijanje odgovorov brez logike                                                                                       |
| `**/graph/routing/CostFunction.java`        | Vmesnik brez implementacije                                                                                                    |
| `**/service/BikePredictionService.java`     | Naloži ONNX modele pri `@PostConstruct` — ni unit testabilno brez pravih binarnih modelov; pokrita z `BikePredictionServiceIT` |
| `**/service/BusDelayPredictionService.java` | Enako; poslovni del (iskanje smeri) je pokrit z `BusDelayPredictionServiceTest`                                                |

---

## Zakaj dve ločeni listi

`sonar.exclusions` odstrani datoteke, kjer ni smiselne kode za analizo (generirano, boilerplate) — ni smisla prijavljati težav ali meriti pokritosti.

`sonar.coverage.exclusions` ohrani datoteke v analizi za zaznavanje težav (npr. varnostnih), jih pa ne kaznuje v skupnem odstotku pokritosti, ker jih je objektivno nemogoče pokriti z unit testi (ONNX, Spring config).
