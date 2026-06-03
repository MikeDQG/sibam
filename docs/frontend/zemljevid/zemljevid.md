# Zemljevid v frontendu

Ta dokument opisuje, kako frontend prikazuje glavni zemljevid, katero knjiznico uporablja, katere podatke prejme komponenta `MainMap` in kako se na zemljevid dodajajo ikone ter ostali elementi.

## Prikaz zemljevida

Glavni zemljevid prikazuje komponenta `MainMap` v `frontend/src/components/MainAppComponents/MainMap.tsx`.

`MainMap` najprej preveri, ali obstaja `VITE_GOOGLE_MAPS_API_KEY`. Ce API kljuc manjka ali ima placeholder vrednost, komponenta ne prikaze zemljevida, ampak fallback sporocilo:

```text
Manjka Google Maps API key
```

Ce je API kljuc nastavljen, komponenta prikaze full-screen zemljevid v absolutno pozicioniranem wrapperju:

```tsx
<div className='absolute inset-0 z-0'>
  <APIProvider apiKey={apiKey} region='SI' language='sl'>
    <Map ...>
      ...
    </Map>
  </APIProvider>
</div>
```

Zemljevid uporablja `center` in `zoom`, ki ju dobi od parent komponente. Ko uporabnik premakne ali zoomira zemljevid, `onCameraChanged` posreduje novo sredisce in zoom nazaj v parent state.

## Razlika med `MainMap` in `Map`

`MainMap` je nasa aplikacijska React komponenta. V njej je zbrana logika, ki je specificna za SibaM aplikacijo:

- preverjanje Google Maps API kljuca;
- povezava zemljevida s temo aplikacije;
- prikaz uporabnikove lokacije;
- prikaz zacetka in cilja poti;
- prikaz polyline poti prek `RoutePolyline`;
- prikaz ikon za avtobus in MBajk;
- prikaz shranjenih lokacij;
- odpiranje popupov za pot, ustvarjanje lokacije in brisanje lokacije;
- callbacki proti `MainAppHome`, kjer je glavni state.

`Map` je komponenta iz knjiznice `@vis.gl/react-google-maps`. Predstavlja dejanski Google Maps zemljevid in skrbi za osnovno map funkcionalnost, kot so kamera, zoom, premikanje zemljevida, desni klik, tema zemljevida, `mapId` in Google Maps instance.

Poenostavljeno: `Map` je nizkonivojski Google Maps prikaz, `MainMap` pa je nas wrapper okoli njega, ki doda aplikacijsko logiko in vse elemente, ki jih SibaM prikazuje na zemljevidu.

## Uporabljena knjiznica

Za Google Maps integracijo frontend uporablja knjiznico:

```ts
@vis.gl/react-google-maps
```

Iz te knjiznice `MainMap` uporablja:

- `APIProvider` za inicializacijo Google Maps API-ja;
- `Map` za prikaz Google Maps zemljevida;
- `AdvancedMarker` za prikaz markerjev, ikon in custom HTML elementov na zemljevidu;
- `InfoWindow` za potrditvene in urejevalne popupe;
- `useMap` v pomocni komponenti `FitBounds`, kjer se dostopa do Google Maps instance.

Poti se ne risejo z React komponento iz `@vis.gl/react-google-maps`, ampak v `RoutePolyline` prek Google Maps objekta `google.maps.Polyline`.

## Parametri komponente `MainMap`

`MainMap` prejme naslednje glavne skupine parametrov:

- `center`: trenutno sredisce zemljevida kot `{ lat, lng }`.
- `zoom`: trenutni zoom zemljevida.
- `onCameraChanged`: callback za posodobitev `center` in `zoom` v parent komponenti.
- `markerPosition`: marker zacetka poti.
- `destinationMarkerPosition`: marker cilja poti.
- `userLocationPosition`: marker trenutne lokacije uporabnika.
- `legs`: seznam odsekov poti, ki se posreduje v `RoutePolyline`.
- `routeFitBoundsTrigger`: signal, s katerim parent sprozi ponovno prilagoditev pogleda poti.
- `selectedLeg`: trenutno izbran odsek poti oziroma izbrana ikona na poti za prikaz `RoutePopup`.
- `onLegClick`: callback za klik na polyline odsek poti.
- `onBusIconClick`: callback za klik na avtobusno ikono.
- `onBikeIconClick`: callback za klik na MBajk ikono.
- `onRoutePopupClose`: zapiranje popupov poti.
- `onMapContextSelect`: callback za desni klik na zemljevid, ki zacne ustvarjanje nove shranjene lokacije.
- `mapLocationDraft`: podatki za lokacijo, ki jo uporabnik trenutno ustvarja.
- `onMapLocationColorChange`: sprememba barve nove shranjene lokacije.
- `onMapLocationIconChange`: sprememba ikone nove shranjene lokacije.
- `onMapLocationSave`: shranjevanje nove lokacije.
- `onMapLocationPopupClose`: zapiranje popup-a za novo lokacijo.
- `savedLocations`: seznam shranjenih lokacij za prikaz markerjev na zemljevidu.
- `deletingSavedLocationId`: id lokacije, ki se trenutno brise.
- `onSavedLocationDelete`: callback za brisanje shranjene lokacije.

## Nastavitve komponente `Map`

`Map` uporablja naslednje pomembne nastavitve:

- `center` in `zoom` dolocata trenutno pozicijo kamere.
- `onCameraChanged` vrne novo kamero parent komponenti.
- `onContextmenu` prestreze desni klik, ustavi default vedenje in sprozi ustvarjanje nove lokacije.
- `colorScheme` se nastavi na `DARK` ali `LIGHT` glede na trenutno temo aplikacije.
- `gestureHandling='greedy'` omogoca neposredno upravljanje zemljevida z gestami.
- `draggable`, `scrollwheel` in `keyboardShortcuts` so vklopljeni.
- `disableDefaultUI` odstrani privzete Google Maps kontrole.
- `zoomControl={false}` izklopi privzeti zoom control, ker ima aplikacija svoje kontrole.
- `clickableIcons={false}` izklopi privzete klikabilne Google Maps POI ikone.
- `mapId` se prebere iz `VITE_GOOGLE_MAPS_MAP_ID`, fallback je `DEMO_MAP_ID`.
- `reuseMaps` omogoca ponovno uporabo map instance.

## Zunanje kontrole zemljevida

Privzete Google Maps kontrole so izklopljene z `disableDefaultUI` in `zoomControl={false}`. Namesto njih aplikacija uporablja custom kontrole v komponenti `MainAppControlOverlay`.

Te kontrole niso renderirane znotraj Google Maps UI-ja, ampak kot navaden React overlay nad zemljevidom. Wrapper overlaya uporablja `pointer-events-none`, posamezni gumbi pa `pointer-events-auto`, da so gumbi klikabilni, preostali del overlaya pa ne blokira interakcije z zemljevidom.

Za neposredno upravljanje zemljevida so pomembni trije gumbi:

- `Povečaj` z ikono `Plus`, ki poklice `onZoomIn`.
- `Pomanjšaj` z ikono `Minus`, ki poklice `onZoomOut`.
- `Moja lokacija` z ikono `LocateFixed`, ki poklice `onLocate`.

`MainAppControlOverlay` teh akcij ne izvaja sam. Gumbi samo poklicejo callbacke, ki jih prejme od `MainAppHome`:

```tsx
<MainAppControlOverlay
  onZoomIn={handleZoomIn}
  onZoomOut={handleZoomOut}
  onLocate={handleLocate}
  ...
/>
```

Dejansko spreminjanje stanja zemljevida je v `MainAppHome`:

```ts
function handleZoomIn() {
  setZoom((currentZoom) => Math.min(currentZoom + 1, 20));
}

function handleZoomOut() {
  setZoom((currentZoom) => Math.max(currentZoom - 1, 3));
}

function handleLocate() {
  if (isFollowingRoute) return;

  locateUser({ zoomToUser: true, showOutOfCoverageToast: true });
}
```

`handleZoomIn` in `handleZoomOut` spremenita `zoom` state, ki se nato posreduje v `MainMap` in naprej v `Map`. Zoom je omejen med `3` in `20`.

`handleLocate` poklice `locateUser`, ki prek Geolocation API-ja pridobi trenutno lokacijo uporabnika. Ce je uporabnik znotraj podprtega obmocja, se `center` nastavi na uporabnikovo lokacijo in zoom se pri rocni lokaciji nastavi na `16`. Ce je uporabnik izven obmocja, aplikacija prikaze Maribor in po potrebi toast sporocilo.

Trenutna lokacija se lahko uporabi kot zacetek ali konec poti samo, ce je znotraj podprtega obmocja Maribora. Ce je uporabnikova trenutna lokacija zunaj teh mej, se moznost `Trenutna lokacija` v dropdownu za izbiro poti ne prikaze.

Med aktivnim sledenjem poti je gumb `Moja lokacija` onemogocen. Takrat aplikacija ze samodejno centrira zemljevid na uporabnika, dodatni rocni geolocation klici pa lahko povzrocijo nepotrebno preskakovanje kamere oziroma obcutek glitchanja.

V istem desnem overlay panelu sta tudi `ThemeToggle` in profil oziroma prijava/odjava. `ThemeToggle` ne premika zemljevida, vpliva pa na `theme`, ki ga `MainMap` prebere prek `useTheme` in pretvori v `colorScheme='DARK'` ali `colorScheme='LIGHT'` za komponento `Map`.

## Risanje elementov na zemljevid

Na zemljevidu se prikazujejo:

- trenutna lokacija uporabnika;
- marker zacetka poti;
- marker cilja poti;
- polyline odseki poti;
- ikone za MBajk prevzem in oddajo;
- ikone avtobusnih postaj;
- popup za izbran odsek poti ali ikono poti;
- shranjene lokacije;
- potrditveni popup za brisanje shranjene lokacije;
- popup za ustvarjanje nove shranjene lokacije.

### `AdvancedMarker`

`AdvancedMarker` uporabljamo za elemente, ki morajo biti vezani na koordinato zemljevida, hkrati pa lahko vsebujejo custom HTML oziroma React vsebino.

Osnovni markerji zacetka in cilja uporabljajo samo `position`:

```tsx
{markerPosition && <AdvancedMarker position={markerPosition} />}
{destinationMarkerPosition && (
  <AdvancedMarker position={destinationMarkerPosition} />
)}
```

Trenutna lokacija uporabnika uporablja custom vsebino markerja:

```tsx
<AdvancedMarker
  position={userLocationPosition}
  anchorLeft='-50%'
  anchorTop='-50%'>
  <div className='relative flex h-8 w-8 items-center justify-center'>
    <div className='absolute h-8 w-8 rounded-full bg-blue-500/20' />
    <div className='h-4 w-4 rounded-full border-2 border-white bg-blue-600 shadow-lg' />
  </div>
</AdvancedMarker>
```

`anchorLeft` in `anchorTop` dolocata, kako se vizualni element poravna glede na koordinato. Pri trenutni lokaciji uporabljamo `-50%` in `-50%`, da je marker centriran na tocki.

Za MBajk in avtobusne ikone `AdvancedMarker` vsebuje sliko:

```tsx
<AdvancedMarker position={markerPosition} clickable={true}>
  <img
    src={leg.mode === "BIKE" ? "pathIcons/mBajk.png" : "pathIcons/marprom.png"}
    alt={leg.mode === "BIKE" ? "Bajk postaja za prevzem" : "Avtobusna postaja"}
    className='h-7 w-7 rounded-full'
  />
</AdvancedMarker>
```

Klik na te markerje ne brise ali premika elementa, ampak poklice `onBikeIconClick` ali `onBusIconClick`, da parent komponenta nastavi `selectedLeg` in prikaze ustrezen `RoutePopup`.

Shranjene lokacije imajo se bolj prilagojen `AdvancedMarker`: marker vsebuje barvni krog, izbrano ikono lokacije in label z imenom. Pri hoverju se ikona zamenja z ikono kosa, klik pa odpre potrditveni `InfoWindow` za brisanje.

### `InfoWindow`

`InfoWindow` uporabljamo za popupe, ki so vezani na koordinato:

- potrditveni popup za brisanje shranjene lokacije;
- popup `MapLocationPopup` za ustvarjanje nove lokacije.

Pri ustvarjanju lokacije se `InfoWindow` odpre na koordinati `mapLocationDraft.position`, ki nastane ob desnem kliku na zemljevid.

### `RoutePolyline`

Polyline poti se risejo loceno v komponenti `RoutePolyline`. `MainMap` ji posreduje `legs`, `routeFitBoundsTrigger` in `onLegClick`.

`RoutePolyline` za vsak `leg` ustvari `google.maps.Polyline`, pretvori `polyline` tocke iz `{ lat, lon }` v `{ lat, lng }`, nastavi barvo glede na `mode` in registrira click listener za prikaz popup-a odseka poti.
