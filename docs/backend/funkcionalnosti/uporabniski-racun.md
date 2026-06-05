# Uporabniški račun

Backend ne upravlja gesel in ne izvaja lastne avtentikacije. Identiteta temelji na Firebase Authentication — backend samo verificira token in sinhronizira uporabnika z lastno bazo.

Za HTTP vmesnik glej [`api/uporabniki.md`](../api/uporabniki.md). Za varnostni tok (filter, CORS) glej [`docs/varnost/varnost-backend.md`](../../varnost/varnost-backend.md).

---

## Identifikatorja uporabnika

Vsak uporabnik ima dva ločena identifikatorja:

| Identifikator | Tip    | Izvor                 | Uporaba                                                             |
| ------------- | ------ | --------------------- | ------------------------------------------------------------------- |
| `firebaseUid` | String | Firebase token        | Primarni ključ za ownership preverjanje v servisni plasti           |
| `id`          | UUID   | Baza (auto-generated) | Interni ključ; frontend ga uporablja pri klicih za lokacije in poti |

`firebaseUid` pride iz preverjenega JWT tokena in ga backend nikoli ne sprejme neposredno od klienta. `id` pa se pošlje kot path parameter pri klicih na `/api/locations/{userId}` in `/api/paths/{userId}`.

---

## Registracija in prijava — `getOrCreateUser`

Razred: [UserService.java](../../../backend/src/main/java/com/sibam/service/UserService.java)

Frontend ob vsaki prijavi pokliče `POST /api/users/me`. Backend izvede:

1. Poišče uporabnika po `firebaseUid` v tabeli `users`.
2. Če obstaja — vrne obstoječi zapis. Podatki (email, fullName) se ob ponovni prijavi **ne posodobijo**.
3. Če ne obstaja — ustvari novega z `firebaseUid`, `email`, `fullName` in `createdAt = now()`.

```
Firebase token → FirebaseAuthFilter → uid, email, fullName
    → UserService.getOrCreateUser(uid, email, fullName)
        → findByFirebaseUid(uid)
            → obstaja  → vrni obstoječega
            → ne obstaja → ustvari in shrani novega
```

### Ime uporabnika (`fullName`)

`fullName` se nastavi samo ob prvem vpisu. Vir je po prednosti:

1. Polje `name` iz Firebase ID tokena (prisotno pri Google Sign-In).
2. HTTP glava `X-Full-Name` — nezaupan fallback, ki ga filter prebere, kadar Firebase token ne vsebuje imena. Ni kriptografsko preverjen in se ne sme uporabljati za avtorizacijo.

Ob ponovnih prijavah se `fullName` v bazi ne posodobi, tudi če se v Firebaseu spremeni.

---

## Pridobitev podatkov — `getUserByFirebaseUid`

Razred: [UserService.java](../../../backend/src/main/java/com/sibam/service/UserService.java)

`GET /api/users/me` vrne `Optional<User>` iz repozitorija. Če uporabnik ne obstaja v bazi (npr. še ni klical `POST /me`), controller vrne `404 Not Found`.

---

## Podatkovna entiteta `User`

Razred: [User.java](../../../backend/src/main/java/com/sibam/persistence/User.java), tabela: `users`

| Polje         | Stolpec        | Tip            | Opis                               |
| ------------- | -------------- | -------------- | ---------------------------------- |
| `id`          | `id`           | UUID           | Primarni ključ, generiran ob vpisu |
| `firebaseUid` | `firebase_uid` | String         | Firebase UID, unikaten, not null   |
| `email`       | `email`        | String         | E-poštni naslov                    |
| `fullName`    | `full_name`    | String         | Polno ime                          |
| `createdAt`   | `created_at`   | OffsetDateTime | Čas prvega vpisa                   |

---
