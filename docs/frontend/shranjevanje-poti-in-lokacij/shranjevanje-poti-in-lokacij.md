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

1. Po izracunu poti `MainAppHome` shrani vse alternative v `allFetchedRoutes`, aktivno alternativo pa v `routePath`.
2. `RouteOptions` prikaze kartico za vsako fetchano alternativo in pod vsako kartico omogoci `Shrani pot`.
3. Uporabnik odpre save flow pod konkretno kartico in vnese ime poti.
4. `MainAppHome.handleRouteSave` pridobi auth token in session.
5. Frontend poslje `POST /api/paths`.
6. Payload vsebuje `userId`, `name` in `journey` iz tiste kartice, pod katero je bil odprt save flow.
7. Po uspehu se response normalizira v `SavedAccountRoute` in doda v `savedRoutes`.

Pri vec fetchanih alternativah se nikoli ne shrani celoten `allFetchedRoutes` seznam. Shrani se samo ena izbrana pot:

```ts
{
  userId,
  name,
  journey: selectedRoute
}
```

Ce `selectedRoute` ni podan, `MainAppHome.handleRouteSave` uporabi trenutno aktivni `routePath`. Pri shranjeni poti iz dropdowna se `Shrani pot` v route sheetu ne prikaze, ker je ta pot ze shranjena.

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
