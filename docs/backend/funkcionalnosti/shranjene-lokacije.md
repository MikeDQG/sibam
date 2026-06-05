# Shranjene lokacije

Uporabnik lahko shrani, ureja in briše poimensko označene geografske lokacije (npr. Dom, Služba). Vsaka lokacija je vezana na točno enega lastnika — servisna plast to preverja pri vsaki operaciji.

Za HTTP vmesnik glej [`api/lokacije.md`](../api/lokacije.md).

---

## Podatkovna entiteta `SavedLocation`

Razred: [SavedLocation.java](../../../backend/src/main/java/com/sibam/persistence/SavedLocation.java), tabela: `saved_locations`

| Polje       | Stolpec      | Tip            | Opis                                         |
| ----------- | ------------ | -------------- | -------------------------------------------- |
| `id`        | `id`         | UUID           | Primarni ključ, generiran ob shranitvi       |
| `user`      | `user_id`    | User (FK)      | Lastnik lokacije                             |
| `name`      | `name`       | String         | Ime lokacije, not null                       |
| `address`   | `address`    | String         | Naslov za prikaz                             |
| `latitude`  | `latitude`   | Double         | Zemljepisna širina, not null                 |
| `longitude` | `longitude`  | Double         | Zemljepisna dolžina, not null                |
| `color`     | `color`      | String         | HEX barva za prikaz na karti                 |
| `logo`      | `logo`       | String         | Identifikator ikone                          |
| `createdAt` | `created_at` | OffsetDateTime | Čas shranitve; ob posodobitvi se ne spremeni |

---

## Preverjanje lastništva

Razred: [SavedLocationService.java](../../../backend/src/main/java/com/sibam/service/SavedLocationService.java)

Vsaka operacija primerja `firebaseUid` iz preverjenega tokena z vrednostjo `user.firebaseUid` v bazi. Ob neujemanju servis vrže `403 Forbidden`.

Dve obliki preverjanja glede na to, kateri ID je v poti zahtevka:

- **Po `userId`** (GET, POST): servis najprej poišče `User` po `userId`, nato primerja `user.firebaseUid == uid`.
- **Po `locationId`** (PUT, DELETE): servis najprej poišče `SavedLocation` po `locationId`, nato primerja `location.user.firebaseUid == uid`.

V obeh primerih manjkajoč `User` ali `SavedLocation` vrže `404 Not Found` pred preverjanjem lastništva.

---

## Operacije

### Pridobitev lokacij

```
getLocationsForUser(userId, uid)
  → findById(userId)              → 404 če user ne obstaja
  → user.firebaseUid != uid       → 403
  → findByUserId(userId)          → vrne seznam
```

### Shranjevnaje lokacije

```
saveLocation(userId, name, address, lat, lon, color, logo, uid)
  → findById(userId)              → 404 če user ne obstaja
  → user.firebaseUid != uid       → 403
  → ustvari SavedLocation z createdAt = now()
  → shrani in vrne
```

### Brisanje lokacije

```
deleteLocation(locationId, uid)
  → findById(locationId)          → 404 če lokacija ne obstaja
  → location.user.firebaseUid != uid → 403
  → deleteById(locationId)
```

---
