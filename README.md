<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="assets/logo-celi-belo-besedilo.svg">
    <source media="(prefers-color-scheme: light)" srcset="assets/logo-celi-crno-besedilo.svg">
    <img alt="ŠibaM logo" src="assets/logo-celi-crno-besedilo.svg" width="300"/>
  </picture>
  <br>
  <em>Multimodalna mobilnost Maribora.</em>
  <br>
  <em>Avtorji: Gal Badrov, Miha Govedič, Kaja Vidmar</em>
  <br>
  <a href="https://sibam.atlassian.net/jira/software/projects/SCRUM/boards/1?atlOrigin=eyJpIjoiZGJkYjM2NDQxN2I1NDdlZDkxNDlmNjM0MzJmNGFmNDUiLCJwIjoiaiJ9">Jira projekt</a>
</p>

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/logo-celi-belo-besedilo.svg">
  <source media="(prefers-color-scheme: light)" srcset="assets/logo-celi-crno-besedilo.svg">
  <img alt="ŠibaM logo" src="assets/logo-celi-crno-besedilo.svg" width="300"/>
</picture>

šibaM je spletna aplikacija za načrtovanje multimodalnih poti po Mariboru in okolici. Združuje podatke o avtobusnih prevozih (Marprom/GTFS), mestnih kolesih (mBajk) in vremenskih razmerah ter uporabniku predlaga optimalno pot glede na čas, vreme in zelene površine.

---

## O projektu

Aplikacija pokriva območje mestnega potniškega prometa Marprom — Mestno občino Maribor ter okoliška naselja Kamnica, Limbuš, Pekre, Razvanje, Zrkovci, Malečnik, Bresternica, Zgornji Duplek in Rošpoh.

**Glavne funkcionalnosti:**

- Načrtovanje multimodalne poti (avtobus + kolo + peš),
- prilagajanje poti glede na vremenske razmere,
- načrtovanje zelene poti po mestu,
- uporabniški račun s shranjenimi lokacijami.

---

## Namestitev in zagon

### Predpogoji

- Node.js
- npm ali yarn

### Frontend

1. Pojdi v mapo frontend:

```
cd frontend
```

2. Namesti odvisnosti:

```
npm install
```

3. Zaženi razvojni strežnik:

```
npm run dev
```

### Backend

_Navodila bodo dodana._

### Baza podatkov

_Navodila bodo dodana._

---

## Za razvijalce

### Commit sporočila

- Sporočila pišemo v **slovenščini**,
- vsako commit sporočilo mora vsebovati Jira identifikator taska.

**Format:**

```
SCRUM-XX Kratek opis spremembe
```

**Primeri:**

```
SCRUM-15 Dodano zbiranje podatkov iz GTFS API-ja
SCRUM-6 Vzpostavljen backend repozitorij
SCRUM-19 Implementirana Firebase avtorizacija
```

**Navodila za dobre commit opise:**

- Opis naj bo jasen in konkreten — povej kaj si naredil, ne samo "popravki" ali "spremembe",
- piši v pretekliku (npr. "Dodano", "Implementirano", "Popravljeno"),
- en commit = ena sprememba, ne mešaj več različnih stvari v en commit.

---

### Standardi kodiranja

- Koda se piše v angleščini,
- spremenljivke in funkcije: camelCase (npr. `fetchBikeData`),
- razredi: PascalCase (npr. `RouteCalculator`),
- konstante: UPPER_SNAKE_CASE (npr. `MAX_ROUTE_DISTANCE`),
- datoteke: kebab-case (npr. `route-calculator.ts`).

---

### Strategija vej (Branching)

- **main** — produkcijska veja, samo stabilna koda,
- **development** — aktivni razvoj, sem mergamo vse spremembe.

**Pravila:**

- Nikoli ne pushaj direktno na main,
- vse spremembe gredo preko development,
- za vsak večji task ustvari svojo vejo iz development.

**Format imenovanja vej:**

```
SCRUM-XX-kratek-opis
```

**Primeri:**

```
SCRUM-15-gtfs-podatki
SCRUM-19-firebase-avtorizacija
SCRUM-6-backend-setup
```

## Sledenje napakam (Bug Tracking)

Napake se dodajajo v Jiro kot **Bug** tip taska.

### Opis buga

V opis vsakega buga vključi naslednje informacije:

- Časovne informacije (kdaj je bila napaka odkrita),
- osebo, ki je izvedla testiranje,
- kontekst (na kateri strani, ob kateri akciji),
- opis incidenta,
- korake ponovitve (kako reproducirati napako),
- oceno resnosti (Nizka / Srednja / Visoka / Kritična),
- tveganje.

Polje **Priority** uporabi za oceno prioritete, polje **Assignee** pa za določitev odgovorne osebe za popravilo. Status incidenta se sledi preko Jira statusa (To Do → In Progress → In Review → Done).

### Kdaj dodati bug v sprint

Bug se ustvari v Backlogu in se ga prestavi v sprint, v katerem je planiran popravek.
