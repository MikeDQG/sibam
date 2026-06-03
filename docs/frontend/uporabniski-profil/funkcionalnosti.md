# Funkcionalnosti uporabniskega profila

Uporabniski profil je stran `AccountPage`, kjer prijavljen uporabnik vidi podatke o svojem racunu, shranjene lokacije, shranjene poti in gumb za odjavo.

Ce uporabnik ni prijavljen, ga `onAuthStateChanged` preusmeri na `/login`. Ce je prijavljen, se na strani prikazeta pozdrav z imenom, kadar Firebase profil vsebuje `displayName`, in tekst:

```text
Prijavljen si z emailom {email}
```

Na vrhu profila je tudi gumb `Domov`, ki uporabnika vrne na `/home`.

## Shranjene lokacije

Shranjene lokacije se nalozijo prek `GET /api/locations/{session.id}` z `Authorization: Bearer {token}`. Frontend prejete lokacije normalizira v podatke za kartice:

- `id`;
- `name`;
- `position` z `lat` in `lng`;
- `color`;
- `icon`.

Ce se lokacije se nalagajo, se prikaze stanje `Nalaganje shranjenih lokacij ...`. Ce uporabnik nima shranjenih lokacij, se prikaze `Ni se shranjenih lokacij.`. Ce Google Maps API key obstaja, se lokacije prikazejo kot `SavedLocationMapCard`; sicer se prikaze poenostavljena kartica z ikono, barvo in imenom.

Na kartici shranjene lokacije lahko uporabnik:

- vidi mini zemljevid, centriran na lokacijo;
- vidi marker z izbrano barvo, ikono in imenom lokacije;
- spreminja zoom mini zemljevida;
- izbrise lokacijo prek gumba z ikono kosa.

Mini zemljevid lokacije je namenjen pregledu lokacije. V kartici je premikanje zemljevida onemogoceno (`draggable={false}`), zoom pa je omogocen.

### Brisanje shranjene lokacije

Klik na gumb za izbris poklice `DELETE /api/locations/{locationId}` z `Authorization: Bearer {token}`. Med brisanjem je id lokacije dodan v `deletingLocationIds`, zato je gumb onemogocen in ne more sproziti vec vzporednih zahtevkov za isto lokacijo.

Po uspesnem brisanju se lokacija odstrani iz `savedLocations` state-a in prikaze se toast `Lokacija je izbrisana.`. Ob napaki se prikaze toast `Lokacije ni bilo mogoce izbrisati.`. Ce uporabnik nima veljavnega tokena, se prikaze sporocilo `Za brisanje lokacije moras biti prijavljen.`.

## Shranjene poti

Shranjene poti se nalozijo prek `GET /api/paths/{session.id}` z `Authorization: Bearer {token}`. Frontend shrani samo poti, ki imajo `journey` in vsaj en izrisljiv `polyline` odsek.

Kartica shranjene poti prikaze:

- ime poti;
- trajanje, ce je podano;
- razdaljo, ce je podana;
- zacetek in cilj poti, ce sta podana;
- uporabljene nacine poti, na primer `WALK + BUS + BIKE`;
- datum ustvarjanja, ce ga je mogoce formatirati;
- mini zemljevid z izrisano potjo, markerjema zacetka in cilja ter ikonami za prestope.

Na kartici shranjene poti lahko uporabnik:

- pregleda osnovne podatke o poti;
- pregleda pot na mini zemljevidu;
- premika in zoomira mini zemljevid;
- izbrise pot prek gumba z ikono kosa.

Ce se poti se nalagajo, se prikaze stanje `Nalaganje shranjenih poti ...`. Ce uporabnik nima shranjenih poti, se prikaze `Ni se shranjenih poti.`. Ce poti obstajajo, vendar Google Maps API key manjka, se prikaze `Za prikaz shranjenih poti manjka Google Maps API key.`.

### Brisanje shranjene poti

Klik na gumb za izbris poklice `DELETE /api/paths/{routeId}` z `Authorization: Bearer {token}`. Med brisanjem je id poti dodan v `deletingRouteIds`, zato je gumb onemogocen in se isti izbris ne more poslati veckrat hkrati.

Po uspesnem brisanju se pot odstrani iz `savedRoutes` state-a in prikaze se toast `Pot je izbrisana.`. Ob napaki se prikaze toast `Poti ni bilo mogoce izbrisati.`. Ce uporabnik nima veljavnega tokena, se prikaze sporocilo `Za brisanje poti moras biti prijavljen.`.

## Flow izbrisa na vecjih zaslonih

Na karticah shranjenih lokacij in shranjenih poti je gumb za izbris del spodnjega dela kartice. Na manjsih zaslonih je gumb vedno viden, da ga lahko uporabnik doseze brez hover interakcije.

Na vecjih zaslonih (`sm` breakpoint in vec) je gumb za izbris privzeto skrit:

```tsx
sm:max-h-0 sm:group-hover:max-h-11
```

Flow je zato:

1. Uporabnik premakne miski kazalec na kartico.
2. Kartica prek `group-hover` razkrije spodnji del z gumbom za izbris.
3. Uporabnik klikne ikono kosa.
4. Frontend poslje `DELETE` zahtevek za izbrani element.
5. Po uspehu se kartica odstrani iz seznama brez ponovnega nalaganja strani.

Pri tem se kartici na hoverju prilagodi tudi spodnji rob mini zemljevida oziroma vsebine, da se gumb za izbris vizualno poveze s kartico.

## Odjava

Na dnu profila je gumb `Odjava`. Klik na gumb poklice:

```ts
auth.signOut();
navigate("/login");
```

Uporabnik se odjavi iz Firebase seje in se preusmeri na prijavno stran.
