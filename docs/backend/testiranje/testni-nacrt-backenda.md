# Testni načrt backenda

## Ogrodja

| Ogrodje | Namen |
| --- | --- |
| JUnit 5 | Testni runner |
| Mockito | Mocking zunanjih odvisnosti |
| AssertJ | Fluentne trditve |
| Spring Boot Test + MockMvc | Testiranje HTTP plasti brez polnega Spring contexta |
| Testcontainers (`postgres:16`) | Pravi PostgreSQL za integracijske teste |
| JaCoCo | Merjenje pokritosti |
| SonarCloud | Statična analiza ob vsakem pushu na `main` |

---

## Vrste testov

### Unit testi

Spring context se **nikoli ne naloži**, podatkovna baza se ne zažene. Zunanje odvisnosti (repozitoriji, HTTP odjemalci, Firebase, ONNX) so zamenjane z Mockito mocki. Testi so hitri in deterministični.

### Integracijski testi — podatkovna baza

Spring context se naloži v celoti. Testcontainers samodejno zažene `postgres:16` Docker container. Vsi DB IT razredi dedujejo od `AbstractDatabaseIT`, ki zagotavlja en singleton container za cel testni tek. Testi so označeni z `@Transactional` — vsak test se po koncu samodejno povrne.

### Integracijski testi — ONNX modeli

Spring context se **ne naloži** — modeli se prenesejo ročno z `ReflectionTestUtils`. Zahtevata okoljski spremenljivki `SUPABASE_URL` in `SUPABASE_SERVICE_KEY` in sta privzeto preskočena (`@EnabledIfEnvironmentVariable`). Zaženeta se samo v CI, kjer so Supabase skrivnosti nastavljene.

---

## Zagon testov

```bash
# vsi unit + DB integracijski testi
./mvnw test

# z generiranjem poročila JaCoCo
./mvnw test jacoco:report
# poročilo: target/site/jacoco/index.html
```

ONNX testi se zaženejo samo, ko sta nastavljeni okoljski spremenljivki:

```bash
SUPABASE_URL=https://... SUPABASE_SERVICE_KEY=... ./mvnw test
```

**IntelliJ:** Desni klik na `src/test/java/com/sibam` → *Run 'Tests in sibam'*

**VS Code:** Testing panel → desni klik → *Run Tests with Coverage*

---

## Kaj ni pokrito in zakaj

| Komponenta | Razlog |
| --- | --- |
| `BikePredictionService` (unit) | Vse poti gredo skozi `OrtEnvironment.getEnvironment()` — statična metoda na končnem razredu. Ni mokabilno brez PowerMock. Pokrita z `BikePredictionServiceIT`. |
| `BusDelayPredictionService` — `@PostConstruct` pot | Prenos ONNX modela in JSON preslikave iz Supabase zahteva žive poverilnice. Pokrita z `BusDelayPredictionServiceIT`. |
| `FirebaseAuthFilter` (veljavni žeton) | `FirebaseAuth.getInstance()` ni mokabilen s standardnim Mockitom. Veljavna pot je preverjena ročno. |
| JPA repozitoriji | Spring Data vmesniki brez lastne logike; posredno preverjeni z vsakim DB IT testom. |
| `SupabaseArtifactStorage` | HTTP odjemalec brez poslovne logike. |
| HTTP odjemalci (`*Client`) | Tanki ovoji brez poslovne logike — testiranje bi pomenilo testiranje Springovega ogrodja. |
