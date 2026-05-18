# Struktura Multimodalnega Grafa (ŠibaM)

Dokument opisuje arhitekturo statičnega grafa, ki se uporablja v projektu ŠibaM za iskanje multimodalnih poti v Mariboru. Cilj te strukture je zagotoviti izjemno hitro iskanje poti (v milisekundah) ob hkratnem ohranjanju vseh potrebnih metapodatkov za končni izpis uporabniku.

## 1. Pregled arhitekture
Graf je zasnovan kot **usmerjen graf (Directed Graph)**, kjer so vozlišča geografske točke (postajališča), robovi pa predstavljajo možne premike med njimi. Celoten graf živi v delovnem pomnilniku (RAM) kot objekt razreda `MultimodalGraph`.

## 2. Ključni razredi

### `MultimodalGraph`
Osrednji vsebnik, ki drži celotno mrežo.
- **`nodes` (Map<String, StopNode>)**: Hitro iskanje vozlišča po njegovem ID-ju (npr. `BUS_192`).
- **`routes` (Map<Integer, RouteInfo>)**: Shramba metapodatkov o trasah avtobusov, ki se uporablja za rekonstrukcijo poti in izračun kazni za prestopanje.

### `StopNode`
Predstavlja točko na zemljevidu, kjer se potovanje lahko začne, konča ali se zamenja prevozno sredstvo.
- **`id`**: Unikaten identifikator (npr. `BUS_2186` ali `BIKE_12`).
- **`name`**: Človeku berljivo ime postaje.
- **`lat`, `lon`**: Geografske koordinate.
- **`type`**: Tip postaje (Enum: `BUS_STOP`, `BIKE_STATION`).
- **`outgoingEdges`**: Seznam vseh povezav, ki vodijo iz te postaje.

### `Edge` (Abstraktni razred)
Osnovni razred za vse vrste povezav. Vsebuje:
- **`targetNodeId`**: Kam povezava vodi.
- **`baseWeight`**: Osnovna teža (razdalja v metrih ali fiksni čas).

### `TransitEdge` (Razširja `Edge`)
Povezava, ki jo predstavlja avtobusna linija. To je **časovno odvisen rob**.
- **`lineId` / `routeId`**: Povezava na metapodatke o liniji.
- **`departures` (List<LocalTime>)**: Seznam vseh predvidenih odhodov s te postaje za to specifično traso.
- **`travelTimeSeconds`**: Čas, ki ga avtobus potrebuje do naslednje postaje.

### `WalkingEdge` (Razširja `Edge`)
Povezava, ki omogoča pešačenje.
- Uporablja se za:
    - Prestopanje med avtobusnimi postajami, ki so blizu skupaj.
    - Dostop od avtobusne postaje do MBajk postaje.
    - Dostop od uporabnikove trenutne lokacije do prve postaje.
- Teža se izračuna kot: `razdalja / hitrost_hoje`.

### `RouteInfo` (Record)
Vsebuje podatke, ki niso nujni za samo iskanje, so pa nujni za uporabnika:
- **`lineCode`**: npr. "G1", "6".
- **`headsign`**: Smer vožnje (npr. "Tezno").
- **`color`**: Barva linije za izris na zemljevidu.

## 3. Odločitve pri načrtovanju (Design Decisions)

1. **Vse v RAM-u**: Zaradi majhnosti Maribora celoten graf zasede le nekaj megabajtov, kar omogoča iskanje brez zamudnih klicev na bazo podatkov.
2. **Ločitev DTO in Grafa**: Razredi v grafu so optimizirani za iskanje, medtem ko so DTO razredi optimizirani za branje JSON-ov iz Marprom API-ja.
3. **Implicitna multimodalnost**: Multimodalnost se doseže s `WalkingEdge` povezavami, ki "zašijejo" različne tipe vozlišč v enotno mrežo.
