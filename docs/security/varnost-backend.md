# Varnost backenda

Ta dokument naj opise implementirano varnost backenda aplikacije SibaM. Spodnji naslovi predstavljajo teme, ki jih je potrebno ustrezno nasloviti in dokumentirati glede na trenutno backend implementacijo.

## Pregled varnostne arhitekture

Opisati je potrebno glavne varnostne komponente backenda, predvsem povezavo med Spring Boot API-jem, Firebase Authentication, podatkovno bazo in zascitenimi uporabniskimi viri.

## Vir identitete: Firebase Authentication

Dokumentirati je potrebno, da backend ne preverja gesel neposredno, ampak zaupa Firebase ID tokenom, ki jih prejme od frontenda.

## Inicializacija Firebase Admin SDK

Dokumentirati je potrebno razred `FirebaseConfig`, nacin nalaganja service account podatkov in prioritetni vrstni red:

- `FIREBASE_SERVICE_ACCOUNT_JSON` okoljska spremenljivka;
- fallback na `firebase-service-account.json` v resources.

Posebej je potrebno nasloviti, kako se service account podatki hranijo v produkciji in da ne smejo biti javno izpostavljeni.

## Preverjanje Bearer tokena

Dokumentirati je potrebno razred `FirebaseAuthFilter` in tok preverjanja:

- branje headerja `Authorization`;
- zahtevan format `Bearer <token>`;
- klic `FirebaseAuth.getInstance().verifyIdToken(token)`;
- obravnava neveljavnega tokena z odgovorom `401 Unauthorized`.

## Nastavljanje identitete v request context

Opisati je potrebno, katere vrednosti filter nastavi v request:

- `uid`;
- `email`;
- `fullName`.

Dokumentirati je potrebno tudi, da kontrolerji te vrednosti berejo prek `@RequestAttribute`.

## Sinhronizacija uporabnika z backend bazo

Dokumentirati je potrebno endpointa `POST /api/users/me` in `GET /api/users/me`:

- kako se uporabnik ustvari ali najde prek Firebase UID;
- kateri podatki se shranijo v entiteto `User`;
- kako se uporablja `X-Full-Name`, ce Firebase token ne vsebuje imena.

## Zasciteni endpointi

Navesti je potrebno endpoint-e, ki zahtevajo veljaven Firebase token:

- `POST /api/users/me`;
- `GET /api/users/me`;
- `GET /api/locations/{userId}`;
- `POST /api/locations`;
- `PUT /api/locations/{locationId}`;
- `DELETE /api/locations/{locationId}`;
- `GET /api/paths/{userId}`;
- `POST /api/paths`;
- `DELETE /api/paths/{pathId}`.

Pri vsakem je potrebno dokumentirati pricakovane varnostne pogoje in katera request identiteta se uporablja.

## Javni endpointi in nezascitene funkcionalnosti

Dokumentirati je potrebno endpoint-e, ki so dostopni brez uporabniske avtentikacije, na primer izracun poti in javni podatkovni/predikcijski endpointi, ce ne uporabljajo `@RequestAttribute("uid")`.

Pri vsakem javnem endpointu je potrebno pojasniti, zakaj avtentikacija ni zahtevana in ali sprejema nepreverjene uporabniske vhodne podatke.

## Avtorizacija in preverjanje lastnistva podatkov

Dokumentirati je potrebno, da avtentikacija sama ni dovolj. Pri uporabniskih podatkih backend preverja lastnistvo prek povezave med:

- Firebase `uid` iz tokena;
- `firebaseUid` v entiteti `User`;
- lastnikom shranjene lokacije ali poti.

Posebej je potrebno opisati preverjanja v:

- `SavedLocationService`;
- `SavedPathService`.

## Odgovori ob nepooblascenem ali prepovedanem dostopu

Dokumentirati je potrebno razliko med:

- `401 Unauthorized`: token je neveljaven ali ga ni mogoce preveriti;
- `403 Forbidden`: token je veljaven, vendar uporabnik ni lastnik zahtevanega vira;
- `404 Not Found`: uporabnik ali vir ne obstaja.

## CORS politika

Dokumentirati je potrebno konfiguracijo v `WebConfig`:

- `allowed.origins`;
- dovoljene metode `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`;
- dovoljeni headerji;
- `allowCredentials(true)`.

Treba je opisati razliko med lokalno in produkcijsko konfiguracijo `ALLOWED_ORIGINS`.

## Upravljanje skrivnosti in okoljskih spremenljivk

Dokumentirati je potrebno vse backend skrivnosti in obcutljive konfiguracije, ki se berejo iz okolja:

- `DB_URL`;
- `DB_USERNAME`;
- `DB_PASSWORD`;
- `DB_CLASS_NAME`;
- `FIREBASE_SERVICE_ACCOUNT_JSON`;
- `MBAJK_API_KEY`;
- `OPEN_WEATHERMAP_API_KEY`;
- `SUPABASE_URL`;
- `SUPABASE_SERVICE_KEY`;
- `ROUTES_GOOGLE_API_KEY`;
- `ALLOWED_ORIGINS`.

Posebej je potrebno zapisati, da se kljuci ne smejo logirati ali commitati v repozitorij.

## Dostop do podatkovne baze

Dokumentirati je potrebno, kako backend dostopa do baze, kateri podatki so povezani z uporabnikom in kako se zagotavlja, da uporabnik bere ali spreminja samo svoje podatke.

## Validacija vhodnih podatkov

Dokumentirati je potrebno, kateri request DTO-ji se uporabljajo in katera validacija trenutno obstaja oziroma manjka za:

- shranjene lokacije;
- shranjene poti;
- uporabniske podatke;
- izracun poti;
- predikcijske endpoint-e.

Ta razdelek naj jasno locuje med implementirano validacijo in priporocenimi dopolnitvami.

## Napake, izjeme in razkrivanje podatkov

Dokumentirati je potrebno, kako backend obravnava napake, kateri statusi se vracajo in ali odgovori razkrivajo interne podatke. Posebej je potrebno nasloviti uporabo `ResponseStatusException` in privzeto Spring obravnavo napak.

## Logiranje in varovanje obcutljivih podatkov

Dokumentirati je potrebno, kaj se lahko logira in kaj se ne sme:

- ne logira se Firebase ID tokenov;
- ne logira se service account JSON-a;
- ne logira se API kljucev;
- ne logira se gesel ali drugih skrivnosti.

## Integracije z zunanjimi storitvami

Dokumentirati je potrebno varnostni vidik integracij:

- Firebase;
- Supabase;
- Google Routes;
- MBajk;
- OpenWeatherMap;
- Marprom oziroma GTFS-RT, ce relevantno.

Pri vsaki integraciji je potrebno zapisati, kateri kljuci se uporabljajo, kje se konfigurirajo in ali se podatki posiljajo zunanjim ponudnikom.

## Produkcijska konfiguracija

Dokumentirati je potrebno varnostne razlike med `application.properties`, `application-prod.properties` in `application-ci.properties`, predvsem:

- izvor skrivnosti iz okolja;
- dovoljeni origin-i;
- scheduler nastavitve;
- povezava na bazo;
- nastavitve zunanjih API-jev.

## Testiranje varnosti

Dokumentirati je potrebno, kateri testi pokrivajo varnostne pogoje in katere scenarije je potrebno dodati:

- zahteva brez tokena;
- zahteva z neveljavnim tokenom;
- dostop do tujega `userId`;
- branje tuje lokacije ali poti;
- brisanje tuje lokacije ali poti;
- pravilna CORS konfiguracija.

## Znane omejitve in odprta varnostna vprasanja

Zapisati je potrebno trenutne omejitve implementacije, odprta tveganja in priporocena izboljsanja. Ta razdelek naj jasno locuje med tem, kar je ze implementirano, in tem, kar je se potrebno urediti.
