# Integracijski testi frontenda

Ta dokument opisuje, katere integracijske teste je smiselno dodati poleg obstoječih testov iz `testni-nacrt-frontenda.md`.

Integracijski testi morajo preverjati predvsem dejanske povezave med več komponentami, stanjem aplikacije, routerjem, Firebase sejo, geolokacijo in backend API klici.

## Namen

Integracijski testi naj odgovorijo na vprašanje, ali glavne uporabniške poti delujejo skupaj:

- uporabnik izbere izhodišče in cilj, izračuna pot in dobi route,
- uporabnik shrani ali izbriše lokacijo oziroma pot,
- uporabnik se prijavi, registrira, odjavi in sinhronizira sejo z backendom,
- profil pravilno naloži in prikaže shranjene podatke,
- geolokacija vpliva na zemljevid in aktivno sledenje poti.

## Testni pristop

Integracijski test naj rendera čim bolj realno komponento, mocka pa samo zunanje meje sistema.

Mockati je treba:

- Firebase auth funkcije: `onAuthStateChanged`, `signInWithEmailAndPassword`, `signInWithPopup`, `createUserWithEmailAndPassword`, `signOut`,
- `fetch` za backend API in Google Places API,
- `navigator.geolocation`,
- Google Maps React komponente oziroma `@vis.gl/react-google-maps`,
- router navigacijo z `MemoryRouter`,
- toast sporočila iz `sonner`.

Ne mockamo poslovne logike, ki jo želimo preveriti. Če test preverja `MainAppControlOverlay`, naj dejansko klikne gumbe in inpute te komponente. Če test preverja `AccountPage`, naj dejansko naloži podatke prek mockanega `fetch` in preveri DOM.

## Skupni testni harness

Priporočljivo je dodati skupen helper, na primer:

```txt
frontend/src/test/renderIntegrationTest.tsx
```

Helper naj omogoča render komponent z:

- `MemoryRouter`,
- `ThemeProvider`,
- po potrebi `UserSessionProvider` ali mockanim `useUserSession`,
- osnovnimi fixture podatki za uporabnika, lokacije, poti in route response,
- resetiranimi mocki po vsakem testu.

Primer uporabe:

```tsx
renderIntegration(
  <MainAppControlOverlay
    onPathReceive={onPathReceive}
    onPathError={onPathError}
  />,
  { route: "/home" },
);
```

## Popravek testnega mocka za zemljevid

V trenutnem testnem izpisu se pojavi opozorilo, da je `button` gnezden znotraj drugega `button`. Razlog je testni mock za `AdvancedMarker`, ki marker renderja kot `button`, medtem ko nekatere komponente znotraj markerja že vsebujejo gumbe.

Mock naj bo raje `div`:

```tsx
AdvancedMarker: ({ children, position, onClick }) => (
  <div
    role={onClick ? "button" : undefined}
    tabIndex={onClick ? 0 : undefined}
    data-testid='mock-marker'
    data-position={JSON.stringify(position)}
    onClick={onClick}>
    {children}
  </div>
);
```

S tem testni DOM bolje posnema realen marker in ne ustvarja umetnih HTML opozoril.

## Prioritetni sklopi

### 1. `UserSessionProvider`

Komponenta skrbi za sejo uporabnika in komunikacijo z backendom. Ker je centralna za prijavo, profil, shranjevanje in brisanje, jo je smiselno testirati posebej.

Priporočeni testi:

- `getAuthToken` vrne `null`, ko Firebase uporabnik ne obstaja,
- `getAuthToken` vrne token, ko Firebase uporabnik obstaja,
- `fetchUserSession` pošlje `GET /api/users/me` z `Authorization` headerjem,
- `fetchUserSession` normalizira `id`, `email` in `fullName`,
- neuspešen `fetchUserSession` počisti sejo in preusmeri na `/login`,
- `syncUserSession` pošlje `POST /api/users/me`,
- `syncUserSession` doda `X-Full-Name`, kadar je ime podano,
- `onAuthStateChanged` ob prijavljenem uporabniku naloži sejo,
- `onAuthStateChanged` ob odjavljenem uporabniku počisti sejo.

### 2. `AccountPage`

`AccountPage` trenutno vsebuje veliko API in auth logike: preusmeritev neprijavljenega uporabnika, nalaganje shranjenih podatkov in brisanje.

Priporočeni testi:

- neprijavljen uporabnik je preusmerjen na `/login`,
- prijavljen uporabnik vidi email in ime,
- uspešen `GET /api/locations/:userId` prikaže shranjene lokacije,
- uspešen `GET /api/paths/:userId` prikaže shranjene poti,
- neveljavne lokacije ali poti se filtrirajo oziroma ne podrejo UI-ja,
- prikaže se loading stanje za lokacije in poti,
- prikažejo se prazna stanja, ko API vrne prazen seznam,
- neuspešno nalaganje lokacij prikaže toast in prazno stanje,
- neuspešno nalaganje poti prikaže toast in prazno stanje,
- `DELETE /api/locations/:id` odstrani lokacijo iz DOM-a,
- `DELETE /api/paths/:id` odstrani pot iz DOM-a,
- med brisanjem je ustrezna kartica disabled,
- neuspešen DELETE prikaže toast in kartica ostane prikazana.

### 3. `MainAppControlOverlay`

Ta komponenta povezuje autocomplete, route controls, shranjene lokacije, shranjene poti in izračun poti. Testi naj uporabljajo realne interakcije, ne samo ročno sestavljenih query parametrov.

Priporočeni testi:

- klik na `Navodila za pot` preklopi enojni search v dva inputa,
- izbira Google Places predloga za izhodišče nastavi labelo in koordinate,
- izbira Google Places predloga za cilj nastavi labelo in koordinate,
- izbira trenutne lokacije deluje samo, če je lokacija znotraj Maribora,
- izbira shranjene lokacije nastavi pravilen input in koordinate,
- swap zamenja vrednosti in koordinate,
- `Najdi pot` ostane disabled, dokler manjka ena koordinata,
- `GET /compute` vsebuje `originLat`, `originLon`, `destinationLat`, `destinationLon`, naslove, `bike`, `bus` in čas,
- pri `depart` se pošlje `leaveAt`,
- pri `arrive` se pošlje `arriveBy`,
- če je uporabnik prijavljen, se pošlje `userId`,
- uspešen response pokliče `onPathReceive`,
- napaka response-a pokliče `onPathError`,
- network napaka pokliče `onPathError` s kodo `ROUTE_REQUEST_FAILED`,
- loading overlay se pokaže in izgine v `finally`,
- shranjena pot iz dropdowna nastavi route in zapre dropdown.

### 4. `MainAppHome`

`MainAppHome` je orkestrator glavne aplikacije. Tu je smiselno testirati tokove, ki povezujejo mapo, overlay, route sheet, geolokacijo in API shranjevanja.

Priporočeni testi:

- začetni `getCurrentPosition` nastavi center in uporabnikovo lokacijo,
- lokacija izven Maribora nastavi fallback center in pokaže toast,
- napaka geolokacije ne podre aplikacije,
- `watchPosition` se registrira in ob unmountu počisti z `clearWatch`,
- med aktivnim sledenjem nova lokacija centrira zemljevid in poveča zoom,
- `handlePathReceive` prikaže pot in route sheet,
- `handlePathError` odstrani pot in prikaže napako,
- klik na polyline nastavi `RoutePopup`,
- klik na bus ikono nastavi bus popup,
- klik na bike pickup/return ikono nastavi MBajk popup,
- desni klik na zemljevid odpre obrazec za novo lokacijo,
- uspešen `POST /api/locations` doda novo lokacijo v UI,
- neuspešen `POST /api/locations` prikaže toast,
- uspešen `DELETE /api/locations/:id` odstrani lokacijo iz UI,
- uspešen `POST /api/paths` doda shranjeno pot v dropdown/profilni state,
- poskus shranjevanja poti brez prijave prikaže toast,
- poskus shranjevanja poti brez izračunane poti prikaže toast.

Za stabilnost je pri teh testih smiselno mockati `MainMap` kot testno komponento, ki izpostavi gumbe za klic callbackov, na primer `onMapContextSelect`, `onLegClick`, `onBusIconClick` in `onBikeIconClick`.

### 5. `Login` in `Register`

Trenutni testi lahko preverjajo auth logiko bolj realistično z renderjem obrazcev.

Priporočeni testi za `Login`:

- uporabnik vnese email in geslo,
- klik `Prijavi se` pokliče `signInWithEmailAndPassword`,
- po uspehu se pokliče `syncUserSession`,
- po uspehu se navigira na `/home`,
- Firebase napaka `auth/invalid-credential` prikaže slovensko sporočilo,
- Firebase napaka `auth/invalid-email` prikaže slovensko sporočilo,
- Google prijava pokliče `signInWithPopup`,
- neuspešna Google prijava prikaže napako.

Priporočeni testi za `Register`:

- validacija prikaže zahteve gesla,
- prekratko geslo blokira registracijo,
- manjkajoče ime prikaže napako,
- neujemanje gesel prikaže napako,
- neveljaven email prikaže napako,
- uspešna registracija pokliče `createUserWithEmailAndPassword`,
- po uspehu se pokliče `syncUserSession` z imenom,
- po uspehu se navigira na `/account`,
- Google registracija pokliče `signInWithPopup`,
- Firebase napake se prikažejo v slovenščini.

## Predlagan vrstni red implementacije

1. Popraviti `AdvancedMarker` mock, da ne renderja gnezdenih gumbov.
2. Dodati skupen `renderIntegration` helper.
3. Dodati integracijske teste za `UserSessionProvider`.
4. Dodati integracijske teste za `AccountPage`.
5. Dodati integracijske teste za `MainAppControlOverlay`, posebej za `/compute`.
6. Dodati integracijske teste za `MainAppHome`, posebej za geolokacijo, shranjevanje in brisanje.
7. Nadgraditi auth teste za `Login` in `Register`, da renderjajo dejanske obrazce.

## Kdaj uporabiti unit in kdaj integracijski test

Unit test je primeren za:

- formatiranje trajanja, razdalje, datumov in načinov poti,
- prikaz posamezne kartice,
- majhne kontrole, kot so `RouteControls`, `SearchInputRow`, `MapZoomLocateButtons`.

Integracijski test je primeren za:

- več komponent, ki skupaj spreminjajo state,
- API klice in odzive,
- auth/session tokove,
- geolokacijo,
- shranjevanje in brisanje podatkov,
- tokove, kjer uporabnik klikne več korakov zapored.

## Merilo kakovosti

Integracijski test je uporaben, če bi padel ob realni regresiji. Primeri regresij:

- `GET /compute` pošlje `lng` namesto `lon`,
- `arriveBy` se ne pošlje pri načinu `Prihod do`,
- shranjena pot se ne doda v `savedRoutes`,
- neprijavljen uporabnik lahko pride do profila,
- backend session sync ne pošlje `Authorization`,
- geolokacija izven Maribora premakne zemljevid izven območja pokritosti,
- route loading overlay ostane prikazan po napaki.

Testi, ki samo kličejo `vi.fn()` ali preverjajo ročno sestavljen fixture, imajo manjšo vrednost. Take teste je smiselno postopno zamenjati z realnimi renderji komponent.
