# Shranjene poti

Uporabnik lahko shrani celoten `Journey` objekt, ki ga vrne `/compute`, pod izbranim imenom. Shranjene poti se hranijo kot JSONB v bazi in se vrnejo nespremenjene. Posodabljanje shranjenih poti ni podprto — samo shranjevanje in brisanje.

Za HTTP vmesnik glej [`api/poti.md`](../api/poti.md). Za strukturo `Journey` objekta glej [`api/usmerjanje.md`](../api/usmerjanje.md).

---

## Podatkovna entiteta `SavedPath`

Razred: [SavedPath.java](../../../backend/src/main/java/com/sibam/persistence/SavedPath.java), tabela: `saved_paths`

| Polje       | Stolpec      | Tip            | Opis                                                       |
| ----------- | ------------ | -------------- | ---------------------------------------------------------- |
| `id`        | `id`         | UUID           | Primarni ključ, generiran ob shranitvi                     |
| `user`      | `user_id`    | User (FK)      | Lastnik poti, not null                                     |
| `name`      | `name`       | String         | Ime, ki ga določi uporabnik, not null                      |
| `journey`   | `journey`    | JSONB          | Celoten `Journey` objekt iz `/compute`, shranjen kot JSONB |
| `createdAt` | `created_at` | OffsetDateTime | Čas shranitve                                              |

`journey` se shrani z `@JdbcTypeCode(SqlTypes.JSON)` in se bere nazaj kot `Journey` objekt brez transformacije. Verzioniranje ali validacija vsebine ob shranitvi ni implementirana.

---

## Preverjanje lastništva

Razred: [SavedPathService.java](../../../backend/src/main/java/com/sibam/service/SavedPathService.java)

Enak vzorec kot pri lokacijah:

- **Po `userId`** (GET, POST): poišče `User`, primerja `user.firebaseUid == uid`.
- **Po `pathId`** (DELETE): poišče `SavedPath`, primerja `path.user.firebaseUid == uid`.

Manjkajoč `User` ali `SavedPath` vrže `404 Not Found` pred preverjanjem lastništva. Neujemanje vrže `403 Forbidden`.

---

## Operacije

### Pridobitev poti

```
getPathsForUser(userId, uid)
  → findById(userId)              → 404 če user ne obstaja
  → user.firebaseUid != uid       → 403
  → findByUserId(userId)          → vrne seznam
```

### Shranjevanje poti

```
savePath(userId, name, journey, uid)
  → findById(userId)              → 404 če user ne obstaja
  → user.firebaseUid != uid       → 403
  → ustvari SavedPath z createdAt = now()
  → shrani journey kot JSONB brez transformacije
  → vrne shranjeni objekt
```

### Brisanje poti

```
deletePath(pathId, uid)
  → findById(pathId)              → 404 če pot ne obstaja
  → path.user.firebaseUid != uid  → 403
  → deleteById(pathId)
```
