# Mockanje lokacije uporabnika

Ta dokument opisuje hiter rocni nacin za mockanje trenutne lokacije uporabnika pri lokalnem testiranju aktivnega sledenja poti.

## Mockanje v `MainAppHome`

Za hiter rocni test aktivnega sledenja lahko v `MainAppHome.applyUserLocation` zacasno zamenjamo koordinate iz `GeolocationPosition` funkcije `applyUserLocation` s fiksnimi koordinatami iz komentarjev v kodi.

Trenutna koda bere dejansko lokacijo naprave:

```ts
const userPosition = {
  lat: position.coords.latitude, // 46.5545008
  lng: position.coords.longitude, // 15.64980425
};
```

Za mock lokacije vrednosti zamenjamo s stevilkami iz komentarjev:

```ts
const userPosition = {
  lat: 46.5545008,
  lng: 15.64980425,
};
```

To pomeni, da se `position.coords.latitude` zamenja s `46.5545008`, `position.coords.longitude` pa s `15.64980425`. Na ta nacin se frontend obnasa, kot da je uporabnik na tej lokaciji, ne glede na dejansko lokacijo naprave.

Ta sprememba je namenjena samo lokalnemu testiranju. Pred commitom oziroma pred uporabo v produkciji je treba vrniti branje iz `position.coords.latitude` in `position.coords.longitude`.
