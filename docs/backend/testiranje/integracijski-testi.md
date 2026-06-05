# Integracijski testi

---

## Podatkovna baza

Vsi razredi dedujejo od `AbstractDatabaseIT`, ki z Testcontainers zažene en singleton `postgres:16` container za cel testni tek. Testi so označeni z `@Transactional` — vsak test se po koncu samodejno povrne.

Zagon: `./mvnw test` (Docker mora biti zagnan).

| Razred | Pokriva |
| --- | --- |
| `UserServiceIT` | Ustvarjanje novega uporabnika, deduplikacija ob ponovnem klicu, iskanje po Firebase UID, 404 za neobstoječega |
| `SavedLocationServiceIT` | Shranjevanje z vsemi polji, posodobitev, brisanje, pridobitev vseh lokacij, 403 za tuj UID |
| `SavedPathServiceIT` | Shranjevanje Journey JSON, ohranitev JSONB brez transformacije, brisanje, več poti za istega uporabnika, 403 za tuj UID |
| `MBajkDataServiceIT` | `ingestBikesData` ustvari postajo + posnetek; ne podvoji postaje ob drugem klicu; `getBikeStationVaos` vrne najnovejši posnetek, ne starejšega |
| `GTFSRTDataServiceIT` | `TripEntity` z vsemi polji, `StopDelayEntity` s FK referenco, prazen GTFS feed, dve vozili |

---

## ONNX modeli

Testa prenašata modele iz Supabase Storage in jih naložita ročno z `ReflectionTestUtils` brez Spring contexta. Preskočena sta, ko okoljski spremenljivki nista nastavljeni (`@EnabledIfEnvironmentVariable`).

Zagon (samo v CI ali z nastavljenimi spremenljivkami):

```bash
SUPABASE_URL=https://... SUPABASE_SERVICE_KEY=... ./mvnw test
```

| Razred | Pokriva |
| --- | --- |
| `BikePredictionServiceIT` | Nalaganje vseh 4 modelov brez napake; verjetnosti v [0, 1]; nenegativni napovedani količini; vikend + dež scenarij |
| `BusDelayPredictionServiceIT` | Nalaganje modela + preslikave smeri; neznana postaja → 0; znana postaja → razumen obseg zamude; več kombinacij linija/postaja |
