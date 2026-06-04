# API – Uporabniki

Osnovni URL: `/api/users`

Vsi endpointi zahtevajo veljaven Firebase Bearer token.

---

## POST `/api/users/me` — prijava ali registracija

Ob prvem klicu ustvari nov zapis v bazi, ob ponovnih klicih vrne obstoječega. Frontend ta endpoint pokliče ob vsaki prijavi.

**Avtentikacija:** zaščiten — zahteva `Authorization: Bearer <token>`

**Telo zahtevka:** ni

**Odgovor `200 OK`:**

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "firebaseUid": "abc123firebaseuid",
  "email": "user@example.com",
  "fullName": "Ime Priimek",
  "createdAt": "2026-01-15T10:30:00Z"
}
```

| Polje | Tip | Opis |
| --- | --- | --- |
| `id` | UUID | Interni ID v bazi |
| `firebaseUid` | String | Firebase UID — primarni identifikator za ownership preverjanje |
| `email` | String | E-poštni naslov iz Firebase tokena |
| `fullName` | String | Polno ime iz Firebase tokena ali glave `X-Full-Name` |
| `createdAt` | OffsetDateTime | Čas prvega vpisa |

---

## GET `/api/users/me` — pridobi podatke prijavljenega uporabnika

**Avtentikacija:** zaščiten — zahteva `Authorization: Bearer <token>`

**Telo zahtevka:** ni

**Odgovor `200 OK`:** enaka struktura kot pri POST `/me`

**Odgovor `404 Not Found`:** uporabnik ne obstaja v bazi — ni še klical `POST /me`

---

## HTTP statusi

| Status | Razlog |
| --- | --- |
| `200 OK` | Uspešen vpis ali pridobitev |
| `401 Unauthorized` | Token manjka, ni v Bearer formatu, je potekel |
| `404 Not Found` | Samo pri `GET /me` — uporabnik še ni registriran |
