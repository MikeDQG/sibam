# Testni nacrt frontenda

Ta dokument vsebuje pregled glavnih elementov aplikacije, ki jih je smiselno testirati. Namenjen je kot osnova za unit, integration in end-to-end teste.

## Array testnih podrocij

```ts
export const testnaPodrocja = [
  {
    id: "zemljevid",
    naziv: "Zemljevid",
    komponente: ["MainMap", "RoutePolyline", "MapLocationPopup"],
    kajTestirati: [
      "prikaz osnovnega zemljevida",
      "prikaz trenutne lokacije uporabnika",
      "prikaz shranjenih lokacij",
      "prikaz izracunane poti",
      "prikaz prestopnih ikon za avtobus in MBajk",
      "klik na markerje in polyline odseke",
      "desni klik za ustvarjanje nove shranjene lokacije",
      "brisanje shranjene lokacije prek zemljevida",
    ],
  },
  {
    id: "overlay",
    naziv: "Kontrolni overlay",
    komponente: [
      "MainAppControlOverlay",
      "DestinationSearch",
      "DirectionsInputs",
      "RouteControls",
      "MapControls",
      "SearchInputRow",
      "ProfileButton",
      "MapZoomLocateButtons",
    ],
    kajTestirati: [
      "iskanje cilja v enojnem search baru",
      "preklop iz search bara v nacin iskanja poti",
      "vnos izhodisca in cilja",
      "zamenjava izhodisca in cilja",
      "omogocanje in onemogocanje gumba Najdi pot",
      "preklop avtobus/kolo",
      "preklop Odhod ob/Prihod do",
      "izbira casa",
      "odpiranje shranjenih poti",
      "navigacija na profil ali prijavo",
      "zoom in, zoom out in locate akcije",
    ],
  },
  {
    id: "searchbar",
    naziv: "Search bar in autocomplete",
    komponente: ["usePlacesAutocomplete", "DestinationSearch", "SearchInputRow"],
    kajTestirati: [
      "vpis besedila sprozi autocomplete",
      "prazno besedilo zapre dropdown",
      "izbira predloga nastavi labelo in koordinate",
      "clear gumb pocisti vrednost in koordinate",
      "Escape zapre dropdown",
      "napaka Google Places API-ja ne porusi UI-ja",
      "rezultati so omejeni na podrocje Maribora",
    ],
  },
  {
    id: "iskanje-poti",
    naziv: "Iskanje poti",
    komponente: ["MainAppControlOverlay", "RouteControls", "MainAppHome"],
    kajTestirati: [
      "GET /compute se poklice s pravilnimi query parametri",
      "gumb Najdi pot je disabled brez izhodisca ali cilja",
      "gumb Najdi pot je enabled, ko sta znani obe koordinati",
      "loading overlay se prikaze med racunanjem poti",
      "uspesen response nastavi routePath",
      "napacen response prikaze napako poti",
      "ze izracunana pot preklopi gumb v Zacni",
      "aktivna pot preklopi gumb v Koncaj",
    ],
  },
  {
    id: "prebrani-stepi-poti",
    naziv: "Prebrani stepi poti",
    komponente: ["RouteOptions", "MainAppHome"],
    kajTestirati: [
      "stepi se preberejo iz vseh leg-ov v pravilnem vrstnem redu",
      "HTML oznake v navodilih se odstranijo",
      "prazni ali neveljavni stepi se ne prikazejo",
      "vsak step prikaze pravilen nacin poti",
      "seznam stepov ostane prikazan tudi brez aktivnega sledenja",
    ],
  },
  {
    id: "aktivni-step",
    naziv: "Izbira aktivnega stepa",
    komponente: ["MainAppHome", "RouteOptions"],
    kajTestirati: [
      "aktivni step se izracuna glede na trenutno lokacijo uporabnika",
      "najblizja polyline tocka doloci trenutni leg",
      "startPolylineIndex in endPolylineIndex dolocita trenutni step",
      "trenutni step se prikaze nad zemljevidom",
      "pretekli, trenutni in prihodnji stepi imajo pravilne vizualne statuse",
      "aktivni step se ne prikaze, ko pot ni aktivna ali lokacija ni znana",
    ],
  },
  {
    id: "lokacija-uporabnika",
    naziv: "Lokacija uporabnika",
    komponente: ["MainAppHome", "MainMap", "MainAppControlOverlay"],
    kajTestirati: [
      "uspesna pridobitev lokacije nastavi userLocationPosition",
      "lokacija izven podrocja Maribora se ne uporabi kot trenutna lokacija",
      "gumb locate centrira zemljevid na uporabnika",
      "med aktivnim sledenjem se zemljevid samodejno centrira",
      "napaka geolokacije se obravnava brez sesutja aplikacije",
    ],
  },
  {
    id: "route-options",
    naziv: "Route sheet in moznosti poti",
    komponente: ["RouteOptions"],
    kajTestirati: [
      "route sheet se odpre po izracunu poti",
      "prikaz trajanja, razdalje in navodil",
      "zapiranje in odpiranje sheeta",
      "gumb Shrani pot je na voljo za izracunano pot",
      "pri shranjeni poti brez variant se prikaze ustrezno sporocilo",
    ],
  },
  {
    id: "route-popup",
    naziv: "Popupi na poti",
    komponente: ["RoutePopup"],
    kajTestirati: [
      "klik na WALK/BUS/BIKE odsek prikaze pravilen naslov",
      "prikaz trajanja in razdalje odseka",
      "avtobusni popup prikaze odhod in napoved zamude",
      "MBajk popup prikaze prosta kolesa ali stojala",
      "verjetnosti napovedi so pravilno formatirane in obarvane",
      "popup se zapre na close akcijo",
    ],
  },
  {
    id: "shranjevanje-lokacije",
    naziv: "Shranjevanje nove lokacije",
    komponente: ["MainMap", "MapLocationPopup", "MainAppHome"],
    kajTestirati: [
      "desni klik odpre obrazec za novo lokacijo",
      "ime, barva in ikona so obvezni",
      "shrani gumb je disabled brez veljavnih podatkov",
      "POST /api/locations se poklice s pravilnim payloadom",
      "po uspesnem shranjevanju se lokacija doda v state",
      "nova lokacija se prikaze na zemljevidu in v profilu",
      "napaka pri shranjevanju prikaze uporabniku sporocilo",
    ],
  },
  {
    id: "shranjevanje-poti",
    naziv: "Shranjevanje poti",
    komponente: ["RouteOptions", "MainAppHome", "SavedRouteMapCard"],
    kajTestirati: [
      "shrani pot se prikaze sele po izracunu poti",
      "uporabnik mora vnesti ime poti",
      "POST /api/paths se poklice s pravilnim payloadom",
      "journey se shrani kot celoten routePath objekt",
      "po uspehu se shranjena pot doda v savedRoutes",
      "shranjena pot se prikaze v profilu in dropdownu shranjenih poti",
      "napaka pri shranjevanju prikaze uporabniku sporocilo",
    ],
  },
  {
    id: "uporabniski-racun",
    naziv: "Uporabniski racun",
    komponente: [
      "AccountPage",
      "SavedLocationMapCard",
      "SavedRouteMapCard",
      "UserSessionProvider",
    ],
    kajTestirati: [
      "prikaz imena in emaila prijavljenega uporabnika",
      "nalaganje shranjenih lokacij",
      "nalaganje shranjenih poti",
      "prikaz praznih stanj",
      "brisanje shranjene lokacije",
      "brisanje shranjene poti",
      "odjava uporabnika",
      "preusmeritev neprijavljenega uporabnika na login",
    ],
  },
  {
    id: "saved-route-card",
    naziv: "Kartica shranjene poti",
    komponente: ["SavedRouteMapCard"],
    kajTestirati: [
      "prikaz imena poti",
      "prikaz trajanja in razdalje",
      "prikaz izhodisca in cilja",
      "prikaz nacinov poti v slovenscini in v CAPS",
      "prikaz datuma shranjevanja",
      "mini zemljevid narise pot",
      "brisanje poti sprozi pravilen callback",
    ],
  },
  {
    id: "saved-location-card",
    naziv: "Kartica shranjene lokacije",
    komponente: ["SavedLocationMapCard"],
    kajTestirati: [
      "prikaz imena lokacije",
      "prikaz izbrane barve in ikone",
      "mini zemljevid prikaze marker lokacije",
      "brisanje lokacije sprozi pravilen callback",
      "disabled stanje med brisanjem",
    ],
  },
  {
    id: "avtentikacija",
    naziv: "Prijava, registracija in odjava",
    komponente: ["Login", "Register", "Header", "UserSessionProvider"],
    kajTestirati: [
      "prijava z emailom in geslom",
      "prijava z Google racunom",
      "registracija z emailom in geslom",
      "validacija zahtev gesla",
      "ujemanje ponovljenega gesla",
      "prikaz Firebase napak",
      "sinhronizacija uporabniske seje z backendom",
      "odjava pocisti sejo in preusmeri uporabnika",
    ],
  },
  {
    id: "header",
    naziv: "Header in navigacija",
    komponente: ["Header", "ThemeToggle"],
    kajTestirati: [
      "klik na logo vodi na zacetno stran",
      "gumb Najdi pot vodi na aplikacijo",
      "gumb Moj racun se prikaze prijavljenemu uporabniku",
      "gumb Prijava/Odjava ima pravilno stanje",
      "header spremeni stil po scrollu",
      "preklop teme deluje na vseh glavnih straneh",
    ],
  },
  {
    id: "vreme",
    naziv: "Vremenski widget",
    komponente: ["WeatherWidget"],
    kajTestirati: [
      "prikaz temperature in vremenskega stanja",
      "loading stanje",
      "fallback ob napaki API-ja",
      "formatiranje podatkov",
    ],
  },
  {
    id: "napake-in-loading",
    naziv: "Napake in loading stanja",
    komponente: ["RouteErrorBox", "RouteLoadingOverlay", "sonner"],
    kajTestirati: [
      "loading overlay se prikaze in lahko zapre",
      "napake pri izracunu poti se prikazejo uporabniku",
      "toast sporocila za shranjevanje in brisanje",
      "aplikacija ostane uporabna po neuspelem API klicu",
    ],
  },
  {
    id: "responsive-ui",
    naziv: "Responsive UI",
    komponente: ["MainAppControlOverlay", "RouteControls", "AccountPage"],
    kajTestirati: [
      "layout na mobilnih sirinah",
      "layout na desktop sirinah",
      "gumbi in tekst se ne prekrivajo",
      "dropdowni in route sheet ostanejo uporabni na majhnem zaslonu",
      "profil kartice se pravilno razporedijo po gridu",
    ],
  },
];
```

## Podrobnejsi testni scenariji za splosne sklope

## Pokritost in SonarQube

Pokritost frontenda se meri z Vitest ukazom `npm test`, ki ustvari `coverage/lcov.info`. SonarQube bere isti LCOV report, zato morajo biti Vitest in Sonar izkljucitve usklajene.

Iz coverage se izkljucijo `src/test/**`, `src/main.tsx` in `src/vite-env.d.ts`, ker gre za testne helperje, testni setup oziroma entrypoint brez poslovne logike. Podrobnosti so opisane v `pokritost-sonarqube.md`.

Za datoteke, ki so bile prej prikazane kot 0% pokrite, obstajajo naslednji namenski testi:

- `header.test.tsx` testira dejanski `Header` z router in Firebase mocki.
- `landing-page.test.tsx` testira `App`, `LandingPage`, `FeaturesSection`, `Footer` in hero navigacijo.
- `sonner.test.tsx` testira konfiguracijo `Toaster`.
- `use-places-autocomplete.test.tsx` testira hook za Google Places autocomplete.
- `main-app-home.test.tsx` testira orkestracijo `MainAppHome` prek mockanih mej sistema.

### Zemljevid

Testirati moramo, da se zemljevid pravilno inicializira, da sprejme center, zoom in markerje ter da ne odpove, ko podatki manjkajo. Posebej pomembno je preveriti route polyline, ker se pot rise po vec odsekih z razlicnimi stili. Za vsak nacin poti mora biti potrjeno, da se uporabi pravilen stil linije in da se prestopne ikone prikazejo samo tam, kjer jih pricakujemo.

Pri interakcijah moramo preveriti klik na polyline, klik na MBajk in avtobusne ikone, klik na shranjeno lokacijo ter desni klik za ustvarjanje nove lokacije. Test mora potrditi tudi, da se ob kliku odpre pravilen popup in da se prejsnji popup po potrebi zapre.

### Overlay in search bar

Overlay je glavni kontrolni del aplikacije, zato ga je treba testirati kot uporabniski tok. Preveriti moramo enojni search bar, preklop v navodila, dva inputa za izhodisce in cilj, swap smeri, clear gumbe, shranjene poti, cas odhoda/prihoda in izbiro transportnih nacinov.

Pri search baru je kljucno, da input tekst ni dovolj za iskanje poti. Test mora preveriti, da je `Najdi pot` enabled sele po izbiri predloga oziroma po nastavitvi koordinat. Pri swapu mora test pokriti primer, kjer je izbran samo cilj, uporabnik zamenja inputa in nato vnese se manjkajoco drugo lokacijo.

### Iskanje poti

Testirati moramo, da frontend na `GET /compute` poslje pravilne koordinate, naslove, cas in izbiro transporta. Za `depart` mora poslati `leaveAt`, za `arrive` pa `arriveBy`. Ce je uporabnik prijavljen, mora biti zraven tudi `userId`.

Pri response-u moramo preveriti uspesen scenarij, napako backend-a, loading stanje in zakljucek loadinga v `finally`. Pomembno je tudi preveriti prehode gumbov: `Najdi pot`, `Zacni` in `Koncaj`.

### Prebrani stepi poti

Testirati moramo, da se navodila berejo iz vseh `legs` in `steps` v pravilnem vrstnem redu. HTML oznake v navodilih se morajo odstraniti, prazen tekst pa se ne sme prikazati. Ker se aktivni step primerja z indeksom prikazanih stepov, mora test potrditi, da se indeks racuna po filtriranem seznamu vidnih navodil.

### Aktivni step

Aktivni step je odvisen od trenutne lokacije uporabnika in polyline tock poti. Testirati moramo primer, kjer je uporabnik znotraj intervala `startPolylineIndex` in `endPolylineIndex`, ter primere, kjer aktivnega stepa ni: pot ni aktivna, lokacija uporabnika ni znana, najblizji leg nima ujemajocega stepa ali step nima navodila.

Preveriti je treba tudi vizualno stanje seznama: pretekli stepi, trenutni step in prihodnji stepi morajo imeti razlicne statuse.

### Lokacija uporabnika

Testirati moramo uspesno geolokacijo, manjkajoco geolokacijo, napako browser API-ja in lokacijo izven dovoljenega obmocja. Pri aktivnem sledenju mora test potrditi, da se zemljevid centrira na uporabnika, navaden locate gumb pa mora delovati tudi brez aktivne poti.

### Shranjevanje nove lokacije

Testirati moramo celoten tok od desnega klika do prikaza nove lokacije. Obrazec mora zahtevati ime, barvo in ikono. API payload mora vsebovati uporabnika, ime, naslov, koordinate, barvo in ikono. Po uspehu se mora lokacija pojaviti brez osvezitve strani, po napaki pa mora uporabnik dobiti sporocilo.

### Shranjevanje poti

Testirati moramo, da poti ni mogoce shraniti brez izracunane poti in brez imena. Payload mora vsebovati `userId`, `name` in `journey`, kjer je `journey` celoten `routePath`. Po uspehu se mora nova pot prikazati v profilu in v dropdownu shranjenih poti.

### Uporabniski racun

Testirati moramo zascito strani za neprijavljenega uporabnika, prikaz prijavljenega uporabnika, nalaganje shranjenih lokacij in poti ter prazna stanja. Za brisanje je treba testirati optimistic oziroma lokalno posodobitev state-a: po uspesnem `DELETE` mora kartica izginiti brez ponovnega nalaganja.

### Avtentikacija

Testirati moramo prijavo, registracijo, Google prijavo, validacijo gesla, prikaz napak in sinhronizacijo seje z backendom. Posebej je treba preveriti, da uporabnik po uspesni prijavi pride na pravo stran in da odjava pocisti stanje aplikacije.

### Responsive UI

Testirati moramo glavne tokove na mobilni in desktop sirini. Posebej pomembni so overlay, route controls, dropdowni, route sheet in kartice v profilu. Testi morajo preveriti, da se elementi ne prekrivajo, da so gumbi klikljivi in da se tekst ne preliva izven komponent.
