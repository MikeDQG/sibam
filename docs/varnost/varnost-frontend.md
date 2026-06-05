# Varnost frontenda

Ta dokument opisuje varnostni tok frontenda aplikacije SibaM: kako se upravlja stanje uporabniske seje, kaj mora frontend pridobiti pred klicem zascitenih backend endpointov in kaj se zgodi, ko podatki seje niso dostopni.

## Avtentikacija

Frontend uporablja Firebase Authentication. Konfiguracija Firebase aplikacije je v `frontend/src/firebase.ts`, vrednosti pa se berejo iz Vite okoljskih spremenljivk:

- `VITE_FIREBASE_API_KEY`
- `VITE_FIREBASE_AUTH_DOMAIN`
- `VITE_FIREBASE_PROJECT_ID`
- `VITE_FIREBASE_STORAGE_BUCKET`
- `VITE_FIREBASE_MESSAGING_SENDER_ID`
- `VITE_FIREBASE_APP_ID`

Prijava in registracija sta implementirani v `Login.tsx` in `Register.tsx`. Podprta sta:

- prijava z emailom in geslom prek Firebase metode `signInWithEmailAndPassword`;
- registracija z emailom in geslom prek `createUserWithEmailAndPassword`;
- prijava oziroma registracija z Google racunom prek `signInWithPopup` in `GoogleAuthProvider`.

Frontend gesel ne posilja na backend. Geslo se uporabi samo pri Firebase Authentication. Po uspesni prijavi ali registraciji frontend od Firebase uporabnika pridobi ID token z `user.getIdToken()`.

Pri registraciji z emailom frontend pred ustvarjanjem uporabnika preveri osnovne zahteve za geslo:

- vsaj 12 znakov;
- vsaj ena velika crka;
- vsaj ena mala crka;
- vsaj en poseben znak;
- ponovljeno geslo se mora ujemati.

Ta validacija izboljsa uporabnisko izkusnjo, vendar ni nadomestilo za Firebase in backend validacijo.

## Upravljanje stanja seje

Globalno stanje seje je v `frontend/src/components/Authorization/UserSessionProvider.tsx`. Provider je ovit okoli celotne aplikacije v `frontend/src/main.tsx`, zato lahko komponente dostopajo do seje prek hooka `useUserSession()`.

Provider hrani `userSession`, ki vsebuje:

- `id`: identifikator uporabnika, ki ga vrne backend;
- `name`: ime uporabnika ali `null`;
- `email`: email uporabnika.

`UserSessionProvider` izpostavi naslednje operacije:

- `getAuthToken()`: prebere trenutnega Firebase uporabnika iz `auth.currentUser` in vrne Firebase ID token; ce uporabnik ni prijavljen, vrne `null`;
- `fetchUserSession(token)`: z `Authorization: Bearer <token>` poklice `GET /api/users/me`, normalizira backend uporabnika in shrani `userSession`;
- `syncUserSession(token, fullName?)`: z `Authorization: Bearer <token>` poklice `POST /api/users/me`; pri registraciji lahko poslje tudi `X-Full-Name`;
- `clearUserSession()`: pocisti lokalno stanje seje.

Provider poslusa Firebase spremembe z `onAuthStateChanged`. Ko Firebase sporoci prijavljenega uporabnika, provider pridobi ID token in sinhronizira backend sejo prek `GET /api/users/me`. Ko Firebase sporoci, da uporabnika ni, se lokalni `userSession` pocisti.

## Sinhronizacija z backend uporabnikom

Firebase je vir identitete, backend pa hrani aplikacijskega uporabnika in njegove podatke. Zato po uspesni prijavi ali registraciji frontend ne uporablja samo Firebase `uid`, ampak sejo sinhronizira z backendom.

Tok je:

1. Uporabnik se prijavi ali registrira prek Firebase.
2. Frontend pridobi Firebase ID token z `getIdToken()`.
3. Frontend poklice `POST /api/users/me` prek `syncUserSession`.
4. Backend preveri token in vrne aplikacijskega uporabnika.
5. Frontend shrani normaliziran `userSession`.

Pri kasnejsih zascitenih akcijah komponenta najprej uporabi obstojeci `userSession`. Ce ta ni na voljo, vendar je Firebase token dostopen, komponenta ponovno poklice `fetchUserSession(token)`.

## Zahteve za klic zascitenih backend endpointov

Za klice, ki berejo ali spreminjajo uporabniske podatke, mora frontend pridobiti:

- Firebase ID token;
- backend uporabnisko sejo oziroma `session.id`;
- po potrebi JSON telo zahtevka;
- header `Authorization: Bearer <token>`;
- pri `POST` zahtevkih z JSON telesom tudi `Content-Type: application/json`.

Primeri zascitenih klicev:

- `GET /api/users/me` za pridobivanje backend seje;
- `POST /api/users/me` za ustvarjanje ali sinhronizacijo backend uporabnika;
- `GET /api/locations/{session.id}` za shranjene lokacije;
- `POST /api/locations` za shranjevanje lokacije;
- `DELETE /api/locations/{locationId}` za brisanje lokacije;
- `GET /api/paths/{session.id}` za shranjene poti;
- `POST /api/paths` za shranjevanje poti;
- `DELETE /api/paths/{routeId}` za brisanje poti.

Pri shranjevanju lokacije frontend v telo zahtevka poslje `userId: session.id`, ime, naslov, koordinate, barvo in ikono. Pri shranjevanju poti poslje `userId: session.id`, ime poti in objekt `journey`, ki predstavlja izracunano pot.

## Obnasanje ob manjkajoci seji

Ce `getAuthToken()` vrne `null`, frontend obravnava uporabnika kot neprijavljenega.

Pri branju shranjenih lokacij in poti:

- seznam shranjenih lokacij ali poti se nastavi na prazen seznam;
- zasciten backend klic se ne izvede.

Pri akcijah, ki zahtevajo prijavo:

- shranjevanje lokacije prikaze obvestilo, da mora biti uporabnik prijavljen;
- brisanje lokacije prikaze obvestilo, da mora biti uporabnik prijavljen; (akcija je skrita)
- shranjevanje poti prikaze obvestilo, da mora biti uporabnik prijavljen;
- brisanje poti prikaze obvestilo, da mora biti uporabnik prijavljen. (akcija je skrita)

Ce je token dostopen, `userSession` pa ni, frontend poskusi obnoviti sejo z `fetchUserSession(token)`. Sele ko je backend seja pridobljena, se uporabi `session.id` za uporabniske endpoint-e.

## Obnasanje ob neveljavni ali nedostopni backend seji

Ce klic `GET /api/users/me` ne uspe, `UserSessionProvider`:

- pocisti lokalni `userSession`;
- odjavi uporabnika iz Firebase z `signOut(auth)`;
- preusmeri uporabnika na `/login`, razen ce je ze na `/login` ali `/register`.

To pomeni, da se pri neveljavnem tokenu, izbrisani backend seji ali nedostopni seji lokalno stanje ne sme uporabljati naprej kot veljavna identiteta.

Komponente, ki berejo shranjene lokacije ali poti, ob napakah prikazejo uporabnisko obvestilo:

- "Shranjene lokacije niso bile nalozene."
- "Shranjene poti niso bile nalozene."

Pri neuspelih spremembah podatkov se lokalni seznam ne posodobi, razen ce backend najprej potrdi uspeh zahtevka.

## Dostop do strani

`/home` je dostopen tudi neprijavljenim uporabnikom. Neprijavljen uporabnik lahko uporablja javne funkcionalnosti, kot je izracun poti, ne more pa shranjevati ali brisati svojih lokacij in poti.

`/account` je zascitena stran na nivoju frontenda. `AccountPage` poslusa `onAuthStateChanged`; ce Firebase uporabnik ni prijavljen, uporabnika preusmeri na `/login`.

Frontend preusmeritev ni samostojen varnostni mehanizem. Backend mora se vedno preveriti Firebase token pri vsakem zascitenem zahtevku.

## Javne in delno personalizirane zahteve

Izracun poti prek `GET /compute` se lahko izvede brez avtentikacije. Ce je uporabnik prijavljen, frontend v query parametre doda `userId` iz `auth.currentUser.uid`. Ta parameter se uporablja za personalizacijo ali povezovanje zahteve z uporabnikom, vendar sam po sebi ni dokaz identitete. Za zascitene uporabniske operacije se uporablja `Authorization: Bearer <token>`.

Klici na zunanje storitve, kot je Google Places, uporabljajo locene API kljuce in ne Firebase uporabniske seje.

## Pravila za nove frontend klice

Pri dodajanju novega zascitenega backend klica mora komponenta ali helper:

1. pridobiti token prek `getAuthToken()`;
2. prekiniti akcijo, ce token ni dostopen;
3. pridobiti `userSession` ali jo obnoviti prek `fetchUserSession(token)`, ce endpoint potrebuje backend `userId`;
4. poslati `Authorization: Bearer <token>`;
5. pri JSON telesu poslati `Content-Type: application/json`;
6. ob neuspehu backend klica ne posodabljati lokalnega stanja kot da je operacija uspela;
7. uporabniku prikazati jasno napako, brez razkrivanja tokenov ali internih podatkov.

Tokenov se ne zapisuje v UI, `console.log`, URL-je ali trajno lokalno shrambo. Frontend jih pridobi iz Firebase SDK takrat, ko jih potrebuje za zahtevek.
