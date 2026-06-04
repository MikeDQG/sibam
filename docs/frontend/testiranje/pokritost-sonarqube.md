# Pokritost frontenda v SonarQube

Ta dokument opisuje, kako se meri pokritost frontend kode in katere datoteke se namerno izkljucijo iz SonarQube coverage porocila.

## Orodja

Frontend uporablja Vitest z V8 coverage providerjem. Ukaz:

```bash
npm test
```

v direktoriju `frontend/` zazene:

```bash
vitest run --coverage
```

Vitest ustvari `frontend/coverage/lcov.info`, SonarQube pa ta report prebere prek nastavitve:

```properties
sonar.javascript.lcov.reportPaths=coverage/lcov.info
```

## Izkljucitve iz coverage

Testne helper datoteke, setup datoteke in entrypoint aplikacije niso del produkcijske poslovne logike, zato se ne smejo pojavljati kot 0% pokrite produkcijske datoteke.

Iz Sonar coverage so izkljucene:

- `src/test/**`
- `src/main.tsx`
- `src/vite-env.d.ts`

Enake izkljucitve so smiselno usklajene z `frontend/vitest.config.ts`, kjer Vitest coverage ne meri testnih datotek, setup helperjev in Vite env deklaracij.

## Testne datoteke

Sonar testne datoteke prepozna prek:

```properties
sonar.test.inclusions=src/test/**,src/**/*.test.ts,src/**/*.test.tsx,src/**/*.spec.ts,src/**/*.spec.tsx
```

To je pomembno, ker so v projektu testi in testni helperji organizirani pod `src/test/`, ne samo kot `*.test.tsx` datoteke ob komponentah.

## Trenutno pokriti sklopi

Za odpravo nizke pokritosti so dodani oziroma razsirjeni naslednji testi:

- `src/test/unit-testi/header.test.tsx` renderja dejanski `Header` in preverja navigacijo, prijavo/odjavo, prikaz gumba `Moj račun`, scroll stil in preklop teme.
- `src/test/unit-testi/landing-page.test.tsx` preverja `App`, `LandingPage`, `FeaturesSection`, `Footer` in navigacijo iz hero gumba v glavno aplikacijo.
- `src/test/unit-testi/sonner.test.tsx` preverja, da `Toaster` poda temo, ikone, stil in toast nastavitve knjiznici `sonner`.
- `src/test/unit-testi/theme-provider.test.tsx` preverja inicializacijo teme iz `localStorage`, fallback na sistemsko temo, preklop teme, `setTheme` in napako pri uporabi `useTheme` zunaj providerja.
- `src/test/unit-testi/use-places-autocomplete.test.tsx` preverja debounce, preklic prejsnjega debounce timerja, Google Places request, normalizacijo predlogov, prazen response brez `suggestions`, prazen vnos, `clear`, `closeDropdown` in napako pri fetchu.
- `src/test/integracijski-testi/main-app-home.test.tsx` pokriva geolokacijo, manjkajoc browser geolocation API, lokacijo izven Maribora, callbacke zemljevida in overlaya, route popup selection, izracunano pot, napako poti, shranjevanje in brisanje lokacij, napake brisanja, shranjevanje poti, neveljaven shranjen route response, shranjene poti ter aktivno sledenje.
- `src/lib/text.test.ts` neposredno pokriva normalizacijo whitespace-a, odstranjevanje HTML oznak z `DOMParser` in fallback brez `DOMParser`.
- `src/test/unit-testi/vreme.test.tsx` pokriva vse veje ikon v `WeatherWidget`, loading stanje, zaokrozevanje temperature in napako API-ja.
- `src/test/integracijski-testi/auth-forms.test.tsx` pokriva dodatne Firebase napake, Google auth napake, prikaz/skritje gesel, validacijo imena/emaila in navigacijo med prijavo ter registracijo.
- `src/test/integracijski-testi/user-session-provider.test.tsx` pokriva neuspesen fetch/sync seje, sync brez imena, odjavljen auth state in uporabo hooka zunaj providerja.
- `src/test/integracijski-testi/account-page.test.tsx` pokriva manjkajoc auth token, napake nalaganja, filtriranje neveljavnih podatkov, brisanje lokacij/poti, odjavo in obnovitev seje prek `fetchUserSession`.
- `src/test/integracijski-testi/main-app-control-overlay.test.tsx` pokriva shranjene lokacije, trenutno lokacijo, prazno stanje shranjenih poti, shranjene poti, izbiro datuma, fallback compute response, napake Places API-ja, network napake, swap, clear, profile/logout, transport toggles, ponovno iskanje po spremembi parametrov ze izbrane poti in zapiranje loading overlaya.
- `src/test/unit-testi/iskanje-poti.test.tsx` pokriva query parametre za `/compute`, stanja gumba `Najdi pot`/`Zacni`/`Koncaj`, zastarelo pot po spremembi parametrov in disabled stanje, kadar za ponovno iskanje manjkajo koordinate.
- `src/test/unit-testi/route-options.test.tsx`, `route-popup.test.tsx`, `zemljevid.test.tsx`, `route-polyline.test.tsx` in `responsive-ui.test.tsx` dodatno pokrivajo route sheet, popupe, zemljevid, polyline klike in map controls veje.

## Pricakovani rezultat

Po zagonu `npm test` mora `frontend/coverage/lcov.info` vsebovati pokritost za produkcijske datoteke, testne helperje in entrypoint datoteke pa mora SonarQube obravnavati kot izkljucene oziroma testne datoteke.

Pokritost se vzdrzuje z namenskimi testi za sklope, ki imajo uporabnisko logiko, API veje, validacije, prikaz napak, navigacijo, odzive zemljevida in stanja komponent. Pri dodajanju nove funkcionalnosti se testni sklop razsiri v isti testni datoteki oziroma v novem testu ob modulu, ce obstojece datoteke tega scenarija ne pokrivajo jasno.

Ce Sonar se vedno prikaze `src/test/**` ali `src/main.tsx` kot 0% pokrite datoteke, je treba preveriti, ali analiza uporablja posodobljen `frontend/sonar-project.properties` in sveze ustvarjen `coverage/lcov.info`.
