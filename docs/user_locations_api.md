## GET — fetcha vse lokacije od uporabnika

`GET http://localhost:8080/api/locations/{userId}`

- zamenjaj `{userId}` s pravim UUID

**Headers:**

`Authorization: Bearer <idToken>`

---

## POST — shrani novo lokacijo

`POST http://localhost:8080/api/locationsContent-Type: application/json`

**Headers:**

`Authorization: Bearer <idToken>`

**Body:**

`{
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Home",
  "address": "Some Street 1, Ljubljana",
  "latitude": 46.0569,
  "longitude": 14.5058,
  "color": "#FF5733"
}`

---

## PUT — update lokacijo

`PUT http://localhost:8080/api/locations/{locationId}Content-Type: application/json`

**Headers:**

`Authorization: Bearer <idToken>`

**Body:**

`{
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Work",
  "address": "Business Ave 5, Ljubljana",
  "latitude": 46.0600,
  "longitude": 14.5100,
  "color": "#FF5733"
}`

---

## DELETE — zbrisi lokacijo

`DELETE http://localhost:8080/api/locations/{locationId}`

**Headers:**

`Authorization: Bearer <idToken>`

Vrne `204 No Content` ce je success.
