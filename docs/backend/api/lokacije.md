# API – Shranjene lokacije

Osnovni URL: `/api/locations`

Vsi endpointi zahtevajo veljaven Firebase Bearer token. Servisna plast preverja lastništvo — uporabnik lahko dostopa samo do svojih lokacij.

---

## GET `/api/locations/{userId}` — pridobi vse lokacije uporabnika

**Avtentikacija:** zaščiten

**Parametri poti:**

| Parameter | Tip | Opis |
| --- | --- | --- |
| `userId` | UUID | Interni ID uporabnika (iz `User.id`, ne Firebase UID) |

**Odgovor `200 OK`:**

```json
[
  {
    "id": "uuid-lokacije",
    "user": { "id": "uuid-uporabnika", ... },
    "name": "Dom",
    "address": "Ulica 1, Maribor",
    "latitude": 46.5547,
    "longitude": 15.6459,
    "color": "#FF5733",
    "logo": "home",
    "createdAt": "2026-01-15T10:30:00Z"
  }
]
```

**Odgovor `403 Forbidden`:** `userId` v poti ne pripada klicatelju

---

## POST `/api/locations` — shrani novo lokacijo

**Avtentikacija:** zaščiten

**Telo zahtevka:**

```json
{
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Dom",
  "address": "Ulica 1, Maribor",
  "latitude": 46.5547,
  "longitude": 15.6459,
  "color": "#FF5733",
  "logo": "home"
}
```

| Polje | Tip | Obvezno | Opis |
| --- | --- | --- | --- |
| `userId` | UUID | da | Interni ID uporabnika |
| `name` | String | da | Ime lokacije |
| `address` | String | ne | Naslov za prikaz |
| `latitude` | Double | da | Zemljepisna širina |
| `longitude` | Double | da | Zemljepisna dolžina |
| `color` | String | ne | HEX barva za prikaz na karti |
| `logo` | String | ne | Identifikator ikone |

**Odgovor `200 OK`:** shranjena lokacija z generiranim `id`

**Odgovor `403 Forbidden`:** `userId` v telesu ne pripada klicatelju

---

## PUT `/api/locations/{locationId}` — posodobi lokacijo

**Avtentikacija:** zaščiten

**Parametri poti:**

| Parameter | Tip | Opis |
| --- | --- | --- |
| `locationId` | UUID | ID lokacije za posodobitev |

**Telo zahtevka:** enaka struktura kot pri POST

**Odgovor `200 OK`:** posodobljena lokacija

**Odgovor `403 Forbidden`:** lokacija ne pripada klicatelju

**Odgovor `404 Not Found`:** lokacija ne obstaja

---

## DELETE `/api/locations/{locationId}` — zbriši lokacijo

**Avtentikacija:** zaščiten

**Parametri poti:**

| Parameter | Tip | Opis |
| --- | --- | --- |
| `locationId` | UUID | ID lokacije za brisanje |

**Odgovor `204 No Content`:** uspešno brisanje

**Odgovor `403 Forbidden`:** lokacija ne pripada klicatelju

**Odgovor `404 Not Found`:** lokacija ne obstaja

---

## HTTP statusi

| Status | Razlog |
| --- | --- |
| `200 OK` | Uspešna pridobitev, shranitev ali posodobitev |
| `204 No Content` | Uspešno brisanje |
| `401 Unauthorized` | Token manjka ali je neveljaven |
| `403 Forbidden` | Uporabnik ni lastnik vira |
| `404 Not Found` | Lokacija ali uporabnik ne obstaja |
