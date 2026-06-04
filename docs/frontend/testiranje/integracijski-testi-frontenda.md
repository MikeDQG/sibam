# Integracijski testi frontenda

Ta dokument opisuje, kako so integracijski testi frontenda organizirani, katere zunanje meje sistema se mockajo in katere scenarije morajo novi testi pokrivati v prihodnje.

Integracijski testi morajo preverjati predvsem dejanske povezave med več komponentami, stanjem aplikacije, routerjem, Firebase sejo, geolokacijo in backend API klici.

## Namen

Integracijski testi preverjajo, ali glavne uporabniške poti delujejo skupaj:

- uporabnik izbere izhodišče in cilj, izračuna pot in dobi route,
- uporabnik shrani ali izbriše lokacijo oziroma pot,
- uporabnik se prijavi, registrira, odjavi in sinhronizira sejo z backendom,
- profil pravilno naloži in prikaže shranjene podatke,
- geolokacija vpliva na zemljevid in aktivno sledenje poti.

## Testni pristop

Integracijski test renderja čim bolj realno komponento in mocka samo zunanje meje sistema.

Mockati je treba:

- Firebase auth funkcije: `onAuthStateChanged`, `signInWithEmailAndPassword`, `signInWithPopup`, `createUserWithEmailAndPassword`, `signOut`,
- `fetch` za backend API in Google Places API,
- `navigator.geolocation`,
- Google Maps React komponente oziroma `@vis.gl/react-google-maps`,
- router navigacijo z `MemoryRouter`,
- toast sporočila iz `sonner`.

Ne mockamo poslovne logike, ki jo test preverja. Če test preverja `MainAppControlOverlay`, dejansko klika gumbe in inpute te komponente. Če test preverja `AccountPage`, dejansko naloži podatke prek mockanega `fetch` in preveri DOM.

## Skupni testni harness

Skupni helper za integracijske teste je:

```txt
frontend/src/test/renderIntegrationTest.tsx
```

Helper omogoča render komponent z:

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

Testni mock za `AdvancedMarker` je `div`, ne `button`. To prepreči umetna HTML opozorila, ker nekatere komponente znotraj markerja že vsebujejo gumbe.

Vzorec mocka:

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

Komponenta skrbi za sejo uporabnika in komunikacijo z backendom. Ker je centralna za prijavo, profil, shranjevanje in brisanje, se testira kot samostojen integracijski sklop.

Testi za ta sklop pokrivajo:

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

Testi za ta sklop pokrivajo:

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

Ta komponenta povezuje autocomplete, route controls, shranjene lokacije, shranjene poti in izračun poti. Testi uporabljajo realne interakcije, ne samo ročno sestavljenih query parametrov.

Testi za ta sklop pokrivajo:

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
- izbira datuma v dropdownu doda `date` parameter v `/compute` zahtevo,
- če je uporabnik prijavljen, se pošlje `userId`,
- uspešen response pokliče `onPathReceive`,
- napaka response-a pokliče `onPathError`,
- network napaka pokliče `onPathError` s kodo `ROUTE_REQUEST_FAILED`,
- loading overlay se pokaže in izgine v `finally`,
- shranjena pot iz dropdowna nastavi route in zapre dropdown,
- sprememba parametra ze izbrane ali izracunane poti gumb spremeni iz `Zacni` nazaj v `Najdi pot`,
- klik na `Najdi pot` pri zastareli poti ponovno poslje `/compute` in ne poklice `onStartRoute`,
- prazen dropdown shranjenih poti prikaže prazno stanje.

### 4. `MainAppHome`

`MainAppHome` je orkestrator glavne aplikacije. Testi pokrivajo tokove, ki povezujejo mapo, overlay, route sheet, geolokacijo in API shranjevanja.

Testi za ta sklop pokrivajo:

- začetni `getCurrentPosition` nastavi center in uporabnikovo lokacijo,
- lokacija izven Maribora nastavi fallback center in pokaže toast,
- toast za lokacijo izven Maribora se ne ponavlja ob vec zaporednih locate klicih,
- aplikacija deluje tudi, ko `navigator.geolocation` ni na voljo,
- napaka geolokacije ne podre aplikacije,
- `watchPosition` se registrira in ob unmountu počisti z `clearWatch`,
- med aktivnim sledenjem nova lokacija centrira zemljevid in poveča zoom,
- locate gumb med aktivnim sledenjem ne sprozi novega lociranja,
- `handlePathReceive` prikaže pot in route sheet,
- `handlePathError` odstrani pot in prikaže napako,
- klik na polyline nastavi `RoutePopup`,
- klik na bus ikono nastavi bus popup,
- klik na bike pickup/return ikono nastavi MBajk popup,
- zapiranje route popup-a pocisti selection,
- desni klik na zemljevid odpre obrazec za novo lokacijo,
- sprememba barve ali ikone brez odprtega drafta ne spremeni UI-ja,
- uspešen `POST /api/locations` doda novo lokacijo v UI,
- neuspešen `POST /api/locations` prikaže toast,
- uspešen `DELETE /api/locations/:id` odstrani lokacijo iz UI,
- brisanje lokacije brez tokena prikaže toast,
- neuspešen `DELETE /api/locations/:id` prikaže toast,
- uspešen `POST /api/paths` doda shranjeno pot v dropdown/profilni state,
- neveljaven response pri shranjevanju poti se ne doda med shranjene poti,
- manjkajoca seja se pred nalaganjem ali shranjevanjem pridobi prek `fetchUserSession`,
- shranjena pot brez eksplicitnih endpointov uporabi fallback iz `polyline`,
- poskus shranjevanja poti brez prijave prikaže toast,
- poskus shranjevanja poti brez izračunane poti prikaže toast.

Za stabilnost ti testi mockajo `MainMap` kot testno komponento, ki izpostavi gumbe za klic callbackov, na primer `onMapContextSelect`, `onLegClick`, `onBusIconClick` in `onBikeIconClick`.

### 5. `Login` in `Register`

Auth testi renderjajo obrazce in preverjajo Firebase klice, session sync ter navigacijo.

Testi za `Login` pokrivajo:

- uporabnik vnese email in geslo,
- klik `Prijavi se` pokliče `signInWithEmailAndPassword`,
- po uspehu se pokliče `syncUserSession`,
- po uspehu se navigira na `/home`,
- Firebase napaka `auth/invalid-credential` prikaže slovensko sporočilo,
- Firebase napaka `auth/invalid-email` prikaže slovensko sporočilo,
- Google prijava pokliče `signInWithPopup`,
- neuspešna Google prijava prikaže napako.

Testi za `Register` pokrivajo:

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

## Stanje implementacije

Trenutno stanje integracijskih testov:

- `AdvancedMarker` mock je urejen v skupnem testnem setupu in ne renderja gnezdenih gumbov.
- Skupni `renderIntegration` helper obstaja v `frontend/src/test/renderIntegrationTest.tsx`.
- `UserSessionProvider` ima integracijske teste za token, nalaganje seje, sinhronizacijo in odjavljeno stanje.
- `AccountPage` ima integracijske teste za zascito strani, nalaganje profila, shranjenih lokacij in shranjenih poti.
- `MainAppControlOverlay` ima integracijske teste za Places autocomplete, izbiro lokacij, `/compute`, loading, napake in preklop stanja poti.
- `MainAppHome` ima integracijske teste za geolokacijo, callbacke zemljevida, izracunano pot, napako poti, shranjevanje in brisanje lokacij, shranjevanje poti, izbiro shranjene poti ter aktivno sledenje.
- `Login` in `Register` imata teste za osnovne auth tokove in validacijo obrazcev.
- `AccountPage` dodatno pokriva no-token stanje, napake nalaganja, filtriranje neveljavnih shranjenih podatkov, uspesno in neuspesno brisanje lokacij/poti ter odjavo.
- `MainAppControlOverlay` dodatno pokriva shranjene poti, prazno stanje shranjenih poti, shranjene lokacije, trenutno lokacijo kot izhodisce/cilj, izbiro datuma, swap, clear, Places API napake, fallback compute response, network napake, profile/logout, transport toggles in ponovno iskanje poti po spremembi parametrov.
- `MainAppHome` dodatno pokriva manjkajoco geolokacijo, out-of-coverage toast brez ponavljanja, route popup callbacke, fallback endpointov shranjene poti, no-token/delete error stanja, obnovitev seje prek `fetchUserSession` in neveljaven saved-route response.

## Pravila za nove integracijske teste

Novi integracijski testi se dodajajo po teh pravilih:

- Test renderja realno komponento ali realen sestav komponent, razen kadar je zunanja meja sistema predraga ali nestabilna za testno okolje.
- Mockajo se samo zunanje meje: Firebase, `fetch`, Google Maps, geolokacija, router navigacija in toast sistem.
- Uporabniske akcije se sprozijo prek DOM-a z `fireEvent` oziroma Testing Library poizvedbami.
- Test preveri vidno stanje DOM-a, API klic ali callback, ki predstavlja dejanski uporabniski rezultat.
- Fixture podatki ostanejo majhni, vendar morajo vsebovati realno strukturo backend response-a.
- Testni mock ne sme podvajati poslovne logike, ki jo test zeli dokazati.
- Vsak nov vecji uporabniski tok dobi vsaj en uspesen in en neuspesen scenarij.

## Izvedeni coverage testi

Poleg prioritetnih integracijskih scenarijev so dodani tudi ciljni unit testi za datoteke, ki jih SonarQube lahko oznaci kot slabo pokrite:

- `frontend/src/test/unit-testi/landing-page.test.tsx` renderja `App` in landing page sklope ter preveri navigacijo hero gumba.
- `frontend/src/test/unit-testi/header.test.tsx` renderja `Header` in preveri navigacijo, stanje prijave, odjavo, scroll stil ter theme toggle.
- `frontend/src/test/unit-testi/sonner.test.tsx` preveri props, ki jih lokalni `Toaster` poda knjiznici `sonner`.
- `frontend/src/test/unit-testi/theme-provider.test.tsx` preveri inicializacijo teme, fallback na sistemsko temo, preklop, rocno nastavitev teme in guard za `useTheme`.
- `frontend/src/test/unit-testi/use-places-autocomplete.test.tsx` preveri debounce, preklic prejsnjega timerja, Places API request, normalizacijo predlogov, prazen response in fallback ob napaki.
- `frontend/src/test/integracijski-testi/main-app-home.test.tsx` z mockanimi `MainMap`, `MainAppControlOverlay` in `RouteOptions` preveri stanje `MainAppHome` brez odvisnosti od realnega Google zemljevida.
- `frontend/src/lib/text.test.ts` preveri normalizacijo besedila navodil, vkljucno s fallbackom brez `DOMParser`.
- `frontend/src/test/unit-testi/vreme.test.tsx`, `route-popup.test.tsx`, `route-options.test.tsx`, `zemljevid.test.tsx`, `route-polyline.test.tsx` in `responsive-ui.test.tsx` pokrijejo dodatne veje manjsih komponent, ki jih SonarQube meri kot pogoje.
- `frontend/src/test/integracijski-testi/auth-forms.test.tsx`, `user-session-provider.test.tsx`, `account-page.test.tsx` in `main-app-control-overlay.test.tsx` pokrijejo vecje auth, profilne in overlay tokove z uspesnimi ter neuspesnimi scenariji.

Za pravila izkljucitve testnih helperjev in entrypoint datotek glej `pokritost-sonarqube.md`.

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
- po spremembi `Bus`, `Kolo`, ure, datuma ali lokacij gumb ostane `Zacni` in zažene staro pot,
- shranjena pot se ne doda v `savedRoutes`,
- neprijavljen uporabnik lahko pride do profila,
- backend session sync ne pošlje `Authorization`,
- geolokacija izven Maribora premakne zemljevid izven območja pokritosti,
- route loading overlay ostane prikazan po napaki.

Testi, ki samo kličejo `vi.fn()` ali preverjajo ročno sestavljen fixture, ne izpolnjujejo standarda za integracijski test. Take teste zamenjamo z realnimi renderji komponent in preverjanjem uporabnisko vidnega rezultata.
