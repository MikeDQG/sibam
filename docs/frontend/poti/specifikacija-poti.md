# Specifikacija poti v frontendu

Ta dokument opisuje, kako frontend prikazuje poti, ikone, popupe, navodila po korakih, aktivni korak, shranjene lokacije in shranjene poti. Najprej je opisan uporabniski potek, nato pa implementacija na visjem arhitekturnem nivoju.

## Kako deluje

### Prikaz poti

Pot je sestavljena iz zaporedja `legs`. Vsak `leg` predstavlja en odsek poti z nacinom premikanja, na primer `WALK`, `BIKE` ali `BUS`.

Vsak `leg` vsebuje svoj `polyline`, torej seznam koordinat, po katerih se odsek narise na zemljevidu. Frontend vsak `leg` narise kot svojo Google Maps polyline linijo:

- `WALK`: rumena pikcasta linija.
- `BIKE`: oranzna crtasta linija.
- `BUS`: temno rdeca polna linija.

Ko se pot prikaze, se zemljevid prilagodi mejam vseh polyline tock, da je celotna pot vidna.

### Prikaz ikon na poti

Poleg polyline linij se prikazejo tudi ikone za pomembne prestopne tocke:

- Pri `BIKE` leg-u se na prvi tocki prikaze ikona MBajk postaje za prevzem kolesa.
- Pri `BUS` leg-u se na prvi tocki prikaze ikona avtobusne postaje.
- Pri `BIKE` leg-u se lahko na zadnji tocki prikaze se ikona za oddajo kolesa. Ta ikona se prikaze, kadar naslednji leg ni `BUS`.
- Pri `WALK` leg-u se prestopna ikona ne prikazuje.

Zacetek in cilj poti se prikazeta kot standardna Google Maps markerja, ce sta koordinati znani.

### Klikabilni deli poti

Klikabilni elementi na glavnem zemljevidu so:

- posamezni polyline odseki poti;
- ikona MBajk prevzema;
- ikona MBajk oddaje;
- ikona avtobusne postaje;
- shranjene lokacije na zemljevidu;
- desni klik na zemljevid, ki odpre ustvarjanje nove shranjene lokacije.

Na mini zemljevidih shranjenih poti v profilu so polyline linije in prestopne ikone samo informativne in niso klikabilne.

### Popupi ob kliku elementov poti

Klik na polyline odsek poti odpre popup s povzetkom tega odseka:

- naslov nacina poti: `Pes`, `Kolo` ali `Avtobus`;
- trajanje odseka;
- razdalja odseka.

Klik na avtobusno ikono odpre popup z informacijami o avtobusu:

- naslov `Avtobus`;
- linijo avtobusa iz `leg.code` na levi in smer/naziv linije iz `leg.headsignName` na desni, ce sta podana;
- cas odhoda avtobusa, ce je podan v `leg.departure`.
- pricakovana zamuda v minutah, ce je podana v `leg.busDelayPrediction.predictedBoardingDelaySeconds`.

Zamuda avtobusa je v podatkih podana v sekundah. `RoutePopup` jo pri prikazu pretvori v minute z `Math.floor(predictedBoardingDelaySeconds / 60)`, zato se vrednosti pod polno minuto prikazejo kot `0`. Vrstica se prikaze z labelom `Pricakovana zamuda (min)`. Ce `busDelayPrediction` manjka, ce `predictedBoardingDelaySeconds` ni stevilo ali vrednost ni koncna, se vrstica za zamudo ne prikaze.

Ce je avtobusni leg neposredno po kolesarskem leg-u, popup lahko prikaze tudi stevilo prostih stojal za oddajo kolesa iz prejsnjega kolesarskega leg-a.

Klik na MBajk ikono za prevzem prikaze:

- naslov `Kolo`;
- stevilo prostih koles, ce je podano v `leg.freeBikes`.
- napoved stevila prostih koles, ce je podana v `leg.bikePrediction.predictedBikesAtPickup`;
- verjetnost, da bo za uporabnika ostalo prosto kolo, ce je podana v `leg.bikePrediction.pickupBikeAvailableProbability`.

Klik na MBajk ikono za oddajo prikaze:

- naslov `Kolo`;
- stevilo prostih stojal, ce je podano v `leg.freeStands`.
- napoved stevila prostih stojal, ce je podana v `leg.bikePrediction.predictedStandsAtReturn`;
- verjetnost, da bo za uporabnika prosto mesto za oddajo, ce je podana v `leg.bikePrediction.returnStandAvailableProbability`.

Verjetnost napovedi se obarva glede na vrednost:

- nad 80 %: zeleno;
- od 50 % do 79 %: rumeno;
- od 25 % do 49 %: oranzno;
- pod 25 %: rdece.

Klik na shranjeno lokacijo na glavnem zemljevidu odpre potrditveni popup za brisanje lokacije.

### Stepi v poti

Vsak `leg` lahko vsebuje `steps`. Step predstavlja eno tekstovno navodilo znotraj tega lega.

Step-i se prikazejo v spodnjem route sheetu v razdelku `Navodila za pot`. Vrstni red je:

1. vsi step-i prvega leg-a;
2. vsi step-i drugega leg-a;
3. tako naprej do zadnjega leg-a.

To pomeni, da frontend bere navodila v istem vrstnem redu, kot so vrnjeni `legs` in `steps` v response-u. Besedilo navodila se pred prikazom normalizira: HTML oznake se odstranijo, whitespace pa se stisne v en presledek.

Step-i brez prikazljivega navodila se iz seznama izlocijo. Indeks prikazanega step-a zato temelji na filtriranem seznamu vidnih navodil, ne nujno na originalnem indeksu v `leg.steps`.

Vsak prikazan step dobi tudi oznako nacina poti:

- `WALK` -> `Pes`;
- `BIKE` -> `Kolo`;
- `BUS` -> `Bus`.

Med aktivnim sledenjem poti ima seznam navodil tri vizualna stanja:

- prihodnji step-i ostanejo z rdecim krogcem;
- step-i, ki so pred aktualnim step-om, dobijo siv krogec;
- aktualni step je vizualno povzdignjen z rahlim scale efektom, senco in ring poudarkom.

### Aktivni step

Aktivni step se prikazuje med aktivnim sledenjem poti, torej po pritisku na `Zacni`.

Frontend najprej poisce najblizjo polyline tocko trenutni lokaciji uporabnika. Iskanje poteka po vseh leg-ih in po vseh polyline tockah. Ko je najden najblizji `leg` in `polylineIndex`, se znotraj tega leg-a poisce step, pri katerem velja:

```ts
startPolylineIndex <= polylineIndex && polylineIndex <= endPolylineIndex
```

Ce tak step obstaja in ima navodilo, se nad zemljevidom prikaze card z aktualnim navodilom. Card vsebuje:

- oznako nacina poti;
- label `Aktualni korak`;
- tekst navodila.

Poleg podatkov za card `getActiveRouteStep` izracuna tudi `stepIndex`. To je indeks aktualnega step-a v celotnem prikazanem seznamu navodil v route sheetu. Indeks se izracuna tako, da frontend prehodi vse lege in njihove step-e v istem vrstnem redu kot pri prikazu seznama, pri tem pa preskoci step-e brez prikazljivega navodila.

`RouteOptions` prejme `activeStepIndex` in ga uporabi za stiliranje seznama:

- ce je indeks step-a manjsi od `activeStepIndex`, je step obravnavan kot opravljen oziroma mimo uporabnika;
- ce je indeks enak `activeStepIndex`, je step trenutni aktivni step;
- ce je indeks vecji od `activeStepIndex`, step ostane prihodnji step.

Neaktivni step-i so vsi step-i, katerih interval `startPolylineIndex` do `endPolylineIndex` ne vsebuje trenutno najblizjega polyline indexa uporabnika. Ti step-i ostanejo vidni v celotnem seznamu navodil v route sheetu, njihov vizualni status pa je odvisen od primerjave z `activeStepIndex`.

Ce pot ni aktivna, ce lokacija uporabnika ni znana, ce ni najdenega ujemajocega step-a ali ce step nima navodila, se card za aktualni step ne prikaze. V tem primeru `RouteOptions` ne prejme aktivnega indeksa in seznam navodil ostane v osnovnem stanju.

### Shranjevanje in brisanje lokacije

Novo lokacijo uporabnik ustvari z desnim klikom na zemljevid. Odpre se popup `Nova lokacija`, kjer uporabnik izbere:

- ime lokacije;
- barvo;
- ikono.

Shranjevanje je dovoljeno samo, ce so ime, barva in ikona podani. Ob shranjevanju frontend poslje lokacijo na backend skupaj z uporabnikovim `userId`, imenom, naslovom, koordinatami, barvo in ikono.

Shranjene lokacije se prikazujejo:

- na glavnem zemljevidu kot markerji z izbrano barvo, ikono in imenom;
- v dropdownu iskalnika kot mozna izhodišča ali cilji;
- v profilu uporabnika.

Lokacijo je mogoce izbrisati:

- na glavnem zemljevidu s klikom na shranjeno lokacijo in potrditvijo brisanja;
- v profilu uporabnika s klikom na gumb za brisanje.

Brisanje zahteva prijavljenega uporabnika in avtentikacijski token.

### Shranjevanje poti

Pot lahko uporabnik shrani po tem, ko je pot izracunana. V spodnjem route sheetu se pri route option karticah prikaze gumb `Shrani pot`. Ob kliku uporabnik vnese ime poti in potrdi shranjevanje.

Frontend shrani celoten `routePath` objekt kot `journey`, skupaj z `userId` in imenom poti. To pomeni, da shranjena pot vsebuje ze izracunane `legs`, `polyline`, `steps`, trajanje, razdaljo in naslovne podatke, ce jih backend vrne.

Shranjene poti se prikazujejo:

- v profilu uporabnika kot kartice z mini zemljevidom;
- v dropdownu pri iskalniku poti pod `Shranjene poti`.

Shranjeno pot je mogoce izbrisati v profilu uporabnika.

### Parametri pri iskanju poti

Ko uporabnik klikne `Najdi pot`, frontend poklice endpoint `GET /compute` z query parametri:

- `originLat`: latitude izhodisca;
- `originLon`: longitude izhodisca;
- `destinationLat`: latitude cilja;
- `destinationLon`: longitude cilja;
- `originAddress`: tekst izhodisca iz inputa;
- `destinationAddress`: tekst cilja iz inputa;
- `leaveNow`: trenutno vedno `false`;
- `bike`: `true` ali `false`, glede na toggle `Kolo`;
- `bus`: `true` ali `false`, glede na toggle `Bus`;
- `date`: izbrani datum iz date kontrol;
- `leaveAt`: izbrana ura, kadar je izbran nacin `Odhod ob`;
- `arriveBy`: izbrana ura, kadar je izbran nacin `Prihod do`;
- `userId`: Firebase UID, ce je uporabnik prijavljen.

Frontend poslje samo enega od parametrov `leaveAt` ali `arriveBy`, odvisno od izbranega time mode-a.

### Izbira trenutne lokacije kot parameter poti

Moznost `Trenutna lokacija` se v dropdownu za zacetek ali konec poti prikaze samo, ce je trenutna lokacija uporabnika znana in je znotraj podprtega obmocja Maribora (`MARIBOR_BOUNDS`).

Ce je trenutna lokacija zunaj mej podprtega obmocja, je ni mogoce uporabiti kot zacetek ali konec poti. V tem primeru se `Trenutna lokacija` v izboru ne prikaze, uporabnik pa mora izbrati lokacijo prek iskalnika ali shranjene lokacije.

### Izbira shranjene lokacije kot parameter poti

Shranjene lokacije se prikazejo v dropdownu iskalnika, ce obstajajo. Ko uporabnik izbere shranjeno lokacijo:

- ime lokacije se nastavi v ustrezen input;
- koordinate lokacije se nastavijo kot `originCoords` ali `destinationCoords`;
- pri iskanju poti se te koordinate posljejo kot `originLat`/`originLon` ali `destinationLat`/`destinationLon`;
- ime lokacije se poslje kot `originAddress` ali `destinationAddress`.

Shranjena lokacija se torej pri iskanju poti obnasa enako kot lokacija, izbrana prek Places autocomplete-a, razlika je samo v viru podatkov.

### Izbira shranjene poti za prikaz

Shranjeno pot uporabnik izbere v dropdownu `Shranjene poti`. Ob izbiri se:

- odpre directions nacin iskalnika;
- input izhodisca napolni z `originLabel` ali privzetim tekstom;
- input cilja napolni z `destinationLabel` ali privzetim tekstom;
- iz shranjene poti se dolocita zacetek in cilj;
- `routePath` se nastavi neposredno na `route.journey`;
- zemljevid se prilagodi shranjeni poti.

Ker je shranjena pot ze izracunan `journey`, za njo niso na voljo razlicni nacini poti oziroma route option alternative. V route sheetu se zato prikaze sporocilo:

```text
Razlicni nacini za shranjeno pot niso na voljo.
```

Za shranjeno pot prav tako ni smiselno ponovno shranjevanje iste poti prek route option kartic, ker route option kartice niso prikazane. Se vedno pa je mogoc prikaz poti na zemljevidu, prikaz navodil, klik na odseke poti in zagon sledenja poti.

Ce uporabnik pri ze izbrani shranjeni poti spremeni parameter, ki vpliva na izracun poti, se pot obravnava kot zastarela. Gumb `Zacni` se zato spremeni nazaj v `Najdi pot`, naslednji klik pa ponovno poklice `/compute` z novimi parametri.

## Implementacija

### Glavne komponente

`MainAppHome` je glavni state owner za zemljevid, trenutno pot, trenutno lokacijo uporabnika, shranjene lokacije, shranjene poti, popupe in aktivno sledenje poti.

`MainAppControlOverlay` skrbi za:

- Places autocomplete;
- izbiro trenutne lokacije;
- izbiro shranjene lokacije;
- izbiro shranjene poti;
- klic `/compute`;
- hranjenje `originCoords`, `destinationCoords`, mode toggle stanja in time mode stanja;
- hranjenje signature zadnjega uspesnega izracuna poti;
- povezovanje overlay podkomponent s callbacki iz `MainAppHome`.

Vizualni deli overlaya so razdeljeni v `MainAppControlOverlayComponents`:

- `DestinationSearch` prikaze enovrsticni cilj pred odpiranjem navodil za pot.
- `DirectionsInputs` prikaze inputa za izhodisce in cilj ter gumb za zamenjavo smeri.
- `SearchInputRow` je skupna vrstica za Places autocomplete input.
- `RouteControls` prikaze toggle `Bus`/`Kolo`, izbiro `Odhod ob` ali `Prihod do`, casovni input, gumb `Najdi pot`/`Zacni`/`Koncaj` in gumb za shranjene poti.
- `MapControls`, `MapZoomLocateButtons` in `ProfileButton` pokrivajo desni panel z map kontrolami, temo in profilom oziroma prijavo/odjavo.
- `types.ts` hrani skupne tipe za overlay podkomponente.

Podkomponente ne izvajajo `/compute` in ne spreminjajo globalnega route state-a neposredno. To ostane v `MainAppControlOverlay` in `MainAppHome`; podkomponente samo sprozijo handlerje, ki jih dobijo prek propsov.

`MainMap` skrbi za:

- prikaz Google Maps zemljevida;
- marker trenutne lokacije uporabnika;
- marker izhodisca in cilja;
- route polyline;
- prestopne ikone;
- popupe za pot;
- shranjene lokacije na zemljevidu;
- popup za ustvarjanje in brisanje lokacij.

`RoutePolyline` skrbi za risanje posameznih leg polyline linij in za click handler na polyline odsekih.

`RoutePopup` skrbi za vsebino popupov, ki se odprejo ob kliku na pot ali route ikone.

`RouteOptions` skrbi za spodnji route sheet, route option kartice, shranjevanje poti in seznam vseh stepov.

`MapLocationPopup` skrbi za formo za ustvarjanje nove shranjene lokacije.

`SavedRouteMapCard` skrbi za prikaz shranjene poti v profilu uporabnika.

### Podatkovni model poti

Osnovni model je `RoutePath`:

```ts
export type RoutePath = {
  legs: RouteLeg[];
  [key: string]: unknown;
};
```

Vsak `RouteLeg` vsebuje:

- `mode`;
- `duration`;
- `distance`;
- podatke za bus ali bike, kot so `departure`, `freeBikes`, `freeStands`;
- opcijska `code` in `headsignName` pri `BUS` legih;
- opcijski `busDelayPrediction` pri `BUS` legih:
  - `predictedBoardingDelaySeconds`;
- opcijski `bikePrediction` pri `BIKE` legih:
  - `pickupBikeAvailableProbability`;
  - `predictedBikesAtPickup`;
  - `predictedStandsAtReturn`;
  - `returnStandAvailableProbability`;
- `polyline`;
- opcijske `steps`.

Vsak `RouteStep` vsebuje:

- `instruction`;
- `maneuver`;
- `distanceMeters`;
- `durationSeconds`;
- `startPolylineIndex`;
- `endPolylineIndex`.

`startPolylineIndex` in `endPolylineIndex` sta lokalna indexa znotraj `polyline` arraya istega leg-a, ne globalna indexa cez celoten journey.

### Tok iskanja poti

1. Uporabnik izbere izhodisce in cilj.
2. `MainAppControlOverlay` hrani `originCoords`, `destinationCoords`, mode toggle in time mode.
3. Klik na `Najdi pot` sestavi `URLSearchParams`.
4. Frontend poklice `GET ${apiUrl}/compute?...`.
5. Uspešen response se parse-a kot `RoutePath`.
6. `onPathReceive` posreduje rezultat v `MainAppHome`.
7. `MainAppHome` nastavi `routePath`.
8. `MainMap` dobi `routePath.legs` in jih prikaze prek `RoutePolyline`.
9. `RouteOptions` dobi iste `legs` in iz njih prikaze navodila.

Ce backend vrne napako, se response prebere v `RouteComputeError`, `routePath` se pocisti, route sheet pa prikaze `RouteErrorBox`.

Po uspesnem izracunu `MainAppControlOverlay` shrani signature parametrov, ki so bili uporabljeni za `/compute`: koordinati izhodisca in cilja, naslova, `Bus`, `Kolo`, nacin casa, uro in datum. Dokler se trenutni parametri ujemajo s tem signature-om, gumb po izracunu prikazuje `Zacni`.

Ce se po izracunu spremeni katerikoli od teh parametrov, trenutna pot ni vec usklajena z nastavitvami v overlayu. `RouteControls` zato gumb preklopi nazaj na `Najdi pot`. Klik na tak gumb ponovno poslje `/compute`; `onStartRoute` se poklice samo, kadar pot obstaja in trenutni parametri niso zastareli.

### Tok aktivnega sledenja poti

1. Ko obstaja `routePath`, gumb `Najdi pot` postane `Zacni`.
2. Ce uporabnik pred zacetkom sledenja spremeni parametre poti, se gumb vrne v `Najdi pot` in pot se mora najprej ponovno izracunati.
3. Klik na `Zacni` nastavi `isFollowingRoute` na `true`.
4. Med aktivnim sledenjem se uporabnikova lokacija polling-a in opazuje prek Geolocation API.
5. Zemljevid se centrira na uporabnika in zoom se dvigne vsaj na `17`.
6. `MainAppHome` z `getActiveRouteStep` izracuna aktualni step in njegov `stepIndex`.
7. Ce obstaja aktualni step, se prikaze card z navodilom.
8. `RouteOptions` prejme `activeStepIndex` in posodobi videz krogcev v seznamu navodil.
9. Klik na `Koncaj` ustavi aktivno sledenje in ponovno prilagodi pogled poti.

### Nalaganje shranjenih podatkov

Ob odprtju glavne strani `MainAppHome` nalozi:

- shranjene lokacije prek `GET /api/locations/{session.id}`;
- shranjene poti prek `GET /api/paths/{session.id}`.

Ob odprtju profila `AccountPage` nalozi iste tipe podatkov za prikaz in brisanje v uporabniskem profilu.

Shranjene poti se normalizirajo samo, ce imajo `journey` in vsaj en `leg` z nepraznim `polyline`.

## Opombe in omejitve

- Aktivni step je odvisen od najblizje polyline tocke, zato se lahko pri zelo redkem polyline-u ali slabem GPS signalu preklop zgodi pozneje ali prej od pricakovanega.
- `startPolylineIndex` in `endPolylineIndex` morata biti prisotna na step-u; step brez teh indexov se ne more uporabiti kot aktivni step.
- Sivi krogci za opravljene step-e temeljijo na `activeStepIndex`, zato so na voljo samo med aktivnim sledenjem poti, ko frontend lahko izracuna aktualni step.
- Shranjena pot se ne preracuna ponovno. Prikaze se tocno tisti `journey`, ki je bil shranjen.
- Mini zemljevidi shranjenih poti v profilu uporabljajo neinteraktivno route polyline logiko.
