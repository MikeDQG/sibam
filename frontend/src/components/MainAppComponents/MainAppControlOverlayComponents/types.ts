import type { usePlacesAutocomplete } from "../../../hooks/usePlacesAutocomplete";

export type Coordinates = {
  lat: number;
  lng: number;
};

export type PlacesAutocomplete = ReturnType<typeof usePlacesAutocomplete>;

export type TimeMode = "depart" | "arrive";
