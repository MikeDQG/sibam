<p align="center">
  <img src="assets/logo.jpeg" alt="ŠibaM logo" width="200"/>
  <br>
  <em>Multimodalna mobilnost Maribora.</em>
  <br>
  <em>Avtorji: Gal Badrov, Miha Govedič, Kaja Vidmar</em>
</p>

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
