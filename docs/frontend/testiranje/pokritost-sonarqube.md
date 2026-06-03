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
- `src/test/unit-testi/use-places-autocomplete.test.tsx` preverja debounce, Google Places request, normalizacijo predlogov, prazen vnos, `clear`, `closeDropdown` in napako pri fetchu.
- `src/test/integracijski-testi/main-app-home.test.tsx` pokriva geolokacijo, callbacke zemljevida in overlaya, izracunano pot, napako poti, shranjevanje in brisanje lokacij, shranjevanje poti, shranjene poti ter aktivno sledenje.

## Pricakovani rezultat

Po zagonu `npm test` naj bodo posebej problematicne datoteke pokrite vsaj tako:

- `src/App.tsx`: 100% lines
- `src/components/LandingPageComponents/FeaturesSection.tsx`: 100% lines
- `src/components/LandingPageComponents/Footer.tsx`: 100% lines
- `src/components/LandingPageComponents/Header.tsx`: 100% lines
- `src/components/LandingPageComponents/HeroSection.tsx`: 100% lines
- `src/components/Pages/LandingPage.tsx`: 100% lines
- `src/components/ui/sonner.tsx`: 100% lines
- `src/hooks/usePlacesAutocomplete.ts`: 100% lines
- `src/components/Pages/MainAppHome.tsx`: nad 80% lines

Ce Sonar se vedno prikaze `src/test/**` ali `src/main.tsx` kot 0% pokrite datoteke, je treba preveriti, ali analiza uporablja posodobljen `frontend/sonar-project.properties` in sveze ustvarjen `coverage/lcov.info`.
