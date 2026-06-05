# API – Shranjene poti

Osnovni URL: `/api/paths`

Vsi endpointi zahtevajo veljaven Firebase Bearer token. Servisna plast preverja lastništvo — uporabnik lahko dostopa samo do svojih poti.

Objekt `journey` je neposreden izhod endpointa `/compute` in se shrani kot JSONB v bazo brez transformacije.

---

## GET `/api/paths/{userId}` — pridobi vse poti uporabnika

**Avtentikacija:** zaščiten

**Parametri poti:**

| Parameter | Tip | Opis |
| --- | --- | --- |
| `userId` | UUID | Interni ID uporabnika (iz `User.id`, ne Firebase UID) |

**Odgovor `200 OK`:**

```json
[
  {
    "id": "uuid-poti",
    "user": { "id": "uuid-uporabnika", ... },
    "name": "V službo",
    "journey": { ... },
    "createdAt": "2026-01-15T10:30:00Z"
  }
]
```

| Polje | Tip | Opis |
| --- | --- | --- |
| `id` | UUID | ID shranjene poti |
| `user` | User | Lastnik poti |
| `name` | String | Ime, ki ga je določil uporabnik |
| `journey` | Journey | Celoten objekt poti iz `/compute` (shranjen kot JSONB) |
| `createdAt` | OffsetDateTime | Čas shranitve |

**Odgovor `403 Forbidden`:** `userId` v poti ne pripada klicatelju

---

## POST `/api/paths` — shrani pot

**Avtentikacija:** zaščiten

**Telo zahtevka:**

```json
{
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "V službo",
  "journey": {
    "status": "ok",
    "origin": { "lat": 46.5547, "lon": 15.6459 },
    "origin_address": "Ulica 1, Maribor",
    "destination": { "lat": 46.5600, "lon": 15.6500 },
    "destination_address": "Ulica 2, Maribor",
    "duration": "18 min",
    "distance": "3.2 km",
    "legs": [ ... ]
  }
}
```

| Polje | Tip | Obvezno | Opis |
| --- | --- | --- | --- |
| `userId` | UUID | da | Interni ID uporabnika |
| `name` | String | da | Ime poti |
| `journey` | Journey | da | Objekt poti iz odgovora `/compute` |

**Odgovor `200 OK`:** shranjena pot z generiranim `id`

**Odgovor `403 Forbidden`:** `userId` v telesu ne pripada klicatelju

---

## DELETE `/api/paths/{pathId}` — zbriši pot

**Avtentikacija:** zaščiten

**Parametri poti:**

| Parameter | Tip | Opis |
| --- | --- | --- |
| `pathId` | UUID | ID poti za brisanje |

**Odgovor `204 No Content`:** uspešno brisanje

**Odgovor `403 Forbidden`:** pot ne pripada klicatelju

**Odgovor `404 Not Found`:** pot ne obstaja

---

## HTTP statusi

| Status | Razlog |
| --- | --- |
| `200 OK` | Uspešna pridobitev ali shranitev |
| `204 No Content` | Uspešno brisanje |
| `401 Unauthorized` | Token manjka ali je neveljaven |
| `403 Forbidden` | Uporabnik ni lastnik vira |
| `404 Not Found` | Pot ali uporabnik ne obstaja |
