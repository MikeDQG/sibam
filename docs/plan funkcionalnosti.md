# 🚀 Projekt "ŠibaM" (Maribor Multimodalna pot)

Aplikacija za optimizacijo poti v Mariboru, ki združuje javni mestni promet (Marprom) in sistem izposoje koles (MBajk) v enoten, inteligenten graf.

## 🏗️ Tehnološki sklad (Tech Stack)

* **Backend:** Java (Spring Boot)
* **Frontend:** React / Next.js
* **Baza:** PostgreSQL + PostGIS (opcijsko)
* **Zemljevidi:** Google Maps / Mapbox
* **Algoritmi:** Time-dependent A*, R-Tree za prostorsko iskanje

---

## 📅 Faze implementacije

### 1. Pridobivanje podatkov (GET)

* **Marprom:** Pridobivanje podatkov o voznih redih (idealno v GTFS formatu ali preko scrapinga).
* **MBajk:** Integracija uradnega API-ja za realne podatke o prostih kolesih in ključavnicah na 29+ postajah.

### 2. Shranjevanje in Serializacija (SAVE)

* **Format:** Uporaba binarne serializacije (**Kryo**) za bliskovito branje objektov ob zagonu strežnika.
* **Struktura:** Shranjevanje v `Stop` in `Route` objekte, ki vsebujejo koordinate, ID-je in sezname odhodov.

### 3. Gradnja statičnega grafa (BUILD)

Zgradimo ogrodje, ki se ne spreminja pogosto.

* **Vozlišča (Nodes):** Bus postajališča, MBajk postaje.
* **Povezave (Edges):**
* **Bus povezave:** Med zaporednimi postajami iste linije.
* **Bike povezave:** Med vsemi MBajk postajami (zračna razdalja).
* **Peš/Prestopne povezave:** Avtomatska generacija povezav med vozlišči različnih tipov, ki so si blizu (< 250m), kar omogoča multimodalnost.



### 4. Procesiranje zahtev (PROCESS)

Jedro aplikacije, ki teče ob vsakem uporabniškem iskanju.

* **4.1. Prostorsko iskanje (R-Tree):**
* Hitra identifikacija 5–10 najbližjih postajališč glede na uporabnikov "Origin" in "Destination".
* Dinamično dodajanje teh začasnih peš povezav v graf.


* **4.2. Time-Dependent A* Algoritem:**
* **Uteži:** Ne fiksne razdalje, temveč čas.
* **Cost Function:** `(T_prihoda + T_čakanja_na_naslednji_bus + T_vožnje)`.
* **Real-time filter:** Če MBajk postaja nima koles, se njena utež nastavi na $\infty$.



### 5. Generiranje navodil (ROUTE DIRECTIONS)

* **Segmentacija:** Razbijanje izračunane poti na logične dele (Hoja -> Bus -> Kolo).
* **Google Maps API:** Klicanje `Compute Routes` API-ja za vsak segment posebej, da pridobimo:
* `Polyline` (natančna črta na cesti).
* `Steps` (besedilna navodila za zavoje).



### 6. Prikaz in Uporabniška izkušnja (DISPLAY)

* **Vizualizacija:** Različne barve in stili črt za različne načine prevoza (npr. modra za bus, zelena za kolo).
* **Interaktivni Timeline:** Vertikalni prikaz korakov s časi odhodov in prihodov.

---

## 🛠️ Ključni koncepti za optimizacijo

| Problem | Rešitev |
| --- | --- |
| **Hitrost iskanja postaj** | **R-Tree** ali Grid-based iskanje. |
| **Multimodalni prestop** | **Virtualne prestopne povezave** v fazi gradnje grafa. |
| **Vozni redi** | **Time-dependent cost function** (Dijkstra, ki pozna uro). |
| **Stroški API-jev** | **Caching** polylines in navodil za pogoste relacije. |

---

## 🚀 Naslednji koraki

1. Priprava `Dockerfile` za backend in frontend.
2. Implementacija R-Tree iskanja v Javi.
3. Testiranje A* algoritma na podmnožici linije 1 in 6.

---

**Moj nasvet za začetek:**
Začni z **Fazo 3.1 in 3.2 (brez voznih redov)**. Najprej poskrbi, da tvoj algoritem najde najkrajšo pot med dvema točkama v Mariboru, kot da so avtobusi "teleporti", ki so vedno na voljo. Ko bo to delovalo, dodaj v `Cost Function` še preverjanje ure in voznega reda.


Želiš, da ti pomagam pripraviti specifikacijo za **Javanski razred `Edge**`, ki bo podpiral te različne tipe povezav?
