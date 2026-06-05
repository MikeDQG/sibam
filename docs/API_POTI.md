# API – Shranjene poti

Osnovni URL: `http://localhost:8080`

Vsi zahtevki zahtevajo Firebase avtentikacijo:

```
Authorization: Bearer <firebase-token>
```

---

## Shrani pot

**POST** `/api/paths`

```json
{
  "userId": "uuid-uporabnika",
  "name": "Ime poti",
  "journey": { ...objekt iz /compute... }
}
```

**Odgovor:** shranjena pot z generiranim `id`

---

## Pridobi vse shranjene poti

**GET** `/api/paths/{userId}`

**Odgovor:**

```json
[
  {
    "id": "uuid-poti",
    "name": "Ime poti",
    "journey": { ... },
    "createdAt": "2026-05-28T19:00:00Z"
  }
]
```

---

## Izbriši pot

**DELETE** `/api/paths/{pathId}`

**Odgovor:** `204 No Content`

---

## Primer uporabe

1. Pokliči `/compute` in shrani odgovor (`journey`)
2. Pokliči `POST /api/paths` z `userId`, imenom in `journey`
3. Iz odgovora vzemi `id`
4. Z `id` pokliči `DELETE /api/paths/{id}` za brisanje
