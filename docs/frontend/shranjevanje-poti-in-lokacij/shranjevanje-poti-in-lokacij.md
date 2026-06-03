# Shranjevanje poti in lokacij

Ta dokument opisuje frontend tokove za shranjevanje lokacij, shranjevanje poti in brisanje shranjenih podatkov.

## Tok shranjevanja lokacije

1. Desni klik na zemljevid nastavi `mapLocationDraft`.
2. `MainMap` prikaze `MapLocationPopup`.
3. Uporabnik vnese ime ter izbere barvo in ikono.
4. `MainAppHome.handleMapLocationSave` pridobi auth token.
5. Frontend poslje `POST /api/locations`.
6. Po uspehu se nova lokacija doda v `savedLocations` state.

Payload vsebuje:

```ts
{
  userId,
  name,
  address,
  latitude,
  longitude,
  color,
  icon,
  logo
}
```

## Tok shranjevanja poti

1. Po izracunu poti `RouteOptions` omogoci `Shrani pot`.
2. Uporabnik vnese ime poti.
3. `MainAppHome.handleRouteSave` pridobi auth token in session.
4. Frontend poslje `POST /api/paths`.
5. Payload vsebuje `userId`, `name` in `journey: routePath`.
6. Po uspehu se response normalizira v `SavedAccountRoute` in doda v `savedRoutes`.

## Brisanje shranjenih podatkov

Shranjeno lokacijo je mogoce hitro izbrisati neposredno na glavnem zemljevidu:

1. Uporabnik klikne marker shranjene lokacije.
2. `MainMap` zapre odprte popupe poti ali ustvarjanja lokacije in nastavi lokacijo za potrditveno okno.
3. Nad lokacijo se odpre `InfoWindow` z besedilom `Izbrisi shranjeno lokacijo?`.
4. Klik na `Izbrisi` poklice `MainAppHome.handleSavedLocationDelete`.
5. Po uspesnem brisanju se lokacija odstrani iz `savedLocations` state-a, zato marker izgine z zemljevida brez ponovnega nalaganja strani.

Marker pri hoverju zamenja izbrano ikono lokacije z ikono kosa, zato je brisanje vidno kot hitro dejanje na zemljevidu. Brisanje se vseeno izvede sele po potrditvi v `InfoWindow`.

Lokacije se brisejo z:

```text
DELETE /api/locations/{locationId}
```

Poti se brisejo z:

```text
DELETE /api/paths/{routeId}
```

Oba klica zahtevata `Authorization: Bearer <token>`. Po uspesnem brisanju se lokalni state posodobi brez ponovnega nalaganja celotnega seznama.
