import {
  Bike,
  Bus,
  LocateFixed,
  Minus,
  Route,
  Plus,
  Search,
  UserRound,
  X,
  LogOut,
  ArrowUpDown,
  Bookmark,
} from "lucide-react";
import { WeatherWidget } from "./WeatherWidget";
import { RouteLoadingOverlay } from "./RouteLoadingOverlay";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import type { RoutePath } from "./RoutePolyline";
import { useNavigate } from "react-router-dom";
import { auth } from "../../firebase";
import { onAuthStateChanged } from "firebase/auth";
import { useState, useEffect, useRef } from "react";
import { ThemeToggle } from "../ThemeToggle";
import {
  usePlacesAutocomplete,
  MARIBOR_BOUNDS,
  type PlaceSuggestion,
} from "../../hooks/usePlacesAutocomplete";
import { LocationIconGlyph, type LocationIcon } from "./MapLocationPopup";
import type { SavedAccountRoute } from "../Pages/AccountPageComponents/SavedRouteMapCard";

const apiUrl = import.meta.env.VITE_API_URL;
const placesApiKey = import.meta.env.VITE_PLACES_API_KEY as string;

type MainAppControlOverlayProps = {
  onZoomIn?: () => void;
  onZoomOut?: () => void;
  onLocate?: () => void;
  currentLocation?: { lat: number; lng: number } | null;
  onPlaceSelect?: (place: { lat: number; lng: number } | null) => void;
  onDestinationSelect?: (place: { lat: number; lng: number } | null) => void;
  onPathReceive?: (path: RoutePath) => void;
  onPathError?: (error: RouteComputeError) => void;
  hasRoute?: boolean;
  isRouteActive?: boolean;
  onStartRoute?: () => void;
  onEndRoute?: () => void;
  savedLocations?: SavedSearchLocation[];
  savedRoutes?: SavedAccountRoute[];
  onSavedRouteSelect?: (route: SavedAccountRoute) => void;
};

export type RouteComputeError = {
  code: string;
  message?: string;
};

type ComputePathErrorResponse = {
  code?: unknown;
  message?: unknown;
};

type Coordinates = {
  lat: number;
  lng: number;
};

type SavedSearchLocation = {
  id: string;
  name: string;
  position: Coordinates;
  color: string;
  icon: LocationIcon;
};

const currentLocationLabel = "Trenutna lokacija";

function isInsideMaribor({ lat, lng }: Coordinates) {
  return (
    lat >= MARIBOR_BOUNDS.low.latitude &&
    lat <= MARIBOR_BOUNDS.high.latitude &&
    lng >= MARIBOR_BOUNDS.low.longitude &&
    lng <= MARIBOR_BOUNDS.high.longitude
  );
}

export const MainAppControlOverlay = ({
  onZoomIn,
  onZoomOut,
  onLocate,
  currentLocation,
  onPlaceSelect,
  onDestinationSelect,
  onPathReceive,
  onPathError,
  hasRoute = false,
  isRouteActive = false,
  onStartRoute,
  onEndRoute,
  savedLocations = [],
  savedRoutes = [],
  onSavedRouteSelect,
}: MainAppControlOverlayProps) => {
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [locationError, setLocationError] = useState(false);
  const [selectedPlace, setSelectedPlace] = useState("");
  const [showDirections, setShowDirections] = useState(false);
  const [originCoords, setOriginCoords] = useState<{
    lat: number;
    lng: number;
  } | null>(null);
  const [destinationCoords, setDestinationCoords] = useState<{
    lat: number;
    lng: number;
  } | null>(null);
  const [useBus, setUseBus] = useState(true);
  const [useBike, setUseBike] = useState(true);
  const [isLoadingRoute, setIsLoadingRoute] = useState(false);
  const [isSavedRoutesOpen, setIsSavedRoutesOpen] = useState(false);
  const [timeMode, setTimeMode] = useState<"depart" | "arrive">("depart");
  const [selectedTime, setSelectedTime] = useState(() => {
    const now = new Date();
    now.setMinutes(now.getMinutes() + 1);
    return `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
  });
  const containerRef = useRef<HTMLDivElement>(null);

  const origin = usePlacesAutocomplete(placesApiKey);
  const destination = usePlacesAutocomplete(placesApiKey);
  const { setIsOpen: setOriginIsOpen } = origin;
  const { setIsOpen: setDestinationIsOpen } = destination;
  const canUseCurrentLocation =
    currentLocation !== null &&
    currentLocation !== undefined &&
    isInsideMaribor(currentLocation);
  const hasSavedLocations = savedLocations.length > 0;
  const hasSavedRoutes = savedRoutes.length > 0;

  function getCurrentLocationCoords() {
    if (!currentLocation || !canUseCurrentLocation) return null;

    return {
      lat: currentLocation.lat,
      lng: currentLocation.lng,
    };
  }

  function formatSavedRouteDuration(value?: string | number | null) {
    const duration = typeof value === "number" ? value : Number(value);
    if (!Number.isFinite(duration)) return null;

    return `${Math.round(duration / 60000)} min`;
  }

  function formatSavedRouteDistance(value?: string | number | null) {
    const distance = typeof value === "number" ? value : Number(value);
    if (!Number.isFinite(distance)) return null;

    const meters = Math.round(distance);
    const kilometers = Math.floor(meters / 1000);
    const remainingMeters = meters % 1000;

    if (kilometers <= 0) return `${remainingMeters} m`;
    if (remainingMeters === 0) return `${kilometers} km`;

    return `${kilometers} km ${remainingMeters} m`;
  }

  function getJourneyPoint(
    point:
      | {
          lat?: number;
          lon?: number;
          lng?: number;
        }
      | null
      | undefined,
  ) {
    if (!point) return null;

    const lat = point.lat;
    const lng = point.lng ?? point.lon;
    if (
      typeof lat !== "number" ||
      typeof lng !== "number" ||
      !Number.isFinite(lat) ||
      !Number.isFinite(lng)
    ) {
      return null;
    }

    return { lat, lng };
  }

  function getRouteEndpoints(route: SavedAccountRoute) {
    const firstLeg = route.journey.legs[0];
    const lastLeg = route.journey.legs.at(-1);

    return {
      origin:
        getJourneyPoint(
          route.journey.origin as Parameters<typeof getJourneyPoint>[0],
        ) ?? getJourneyPoint(firstLeg?.polyline[0]),
      destination:
        getJourneyPoint(
          route.journey.destination as Parameters<typeof getJourneyPoint>[0],
        ) ?? getJourneyPoint(lastLeg?.polyline.at(-1)),
    };
  }

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setIsLoggedIn(!!user);
    });
    return () => unsubscribe();
  }, []);

  function handleClear() {
    origin.clear();
    setLocationError(false);
    setSelectedPlace("");
    setOriginCoords(null);
    onPlaceSelect?.(null);
  }

  async function handleOriginSelect(prediction: PlaceSuggestion) {
    origin.setValue(prediction.mainText);
    origin.closeDropdown();
    try {
      const res = await fetch(
        `https://places.googleapis.com/v1/places/${prediction.placeId}`,
        {
          headers: {
            "X-Goog-Api-Key": placesApiKey,
            "X-Goog-FieldMask": "location",
          },
        },
      );
      const data = await res.json();
      if (data.location) {
        setLocationError(false);
        setSelectedPlace(prediction.mainText);
        const coords = {
          lat: data.location.latitude,
          lng: data.location.longitude,
        };
        setOriginCoords(coords);
        onPlaceSelect?.(coords);
      } else {
        setLocationError(true);
      }
    } catch {
      setLocationError(true);
    }
  }

  function handleCurrentLocationSelect(kind: "origin" | "destination") {
    const coords = getCurrentLocationCoords();
    if (!coords) return;

    setLocationError(false);

    if (kind === "origin") {
      origin.setValue(currentLocationLabel);
      origin.closeDropdown();
      setOriginCoords(coords);
      onPlaceSelect?.(coords);
      return;
    }

    destination.setValue(currentLocationLabel);
    destination.closeDropdown();
    setDestinationCoords(coords);
    setSelectedPlace(currentLocationLabel);
    onDestinationSelect?.(coords);
  }

  function handleCurrentOriginSelect() {
    handleCurrentLocationSelect("origin");
  }

  function handleCurrentDestinationSelect() {
    handleCurrentLocationSelect("destination");
  }

  function handleSavedLocationSelect(
    kind: "origin" | "destination",
    location: SavedSearchLocation,
  ) {
    const coords = {
      lat: location.position.lat,
      lng: location.position.lng,
    };

    setLocationError(false);

    if (kind === "origin") {
      origin.setValue(location.name);
      origin.closeDropdown();
      setOriginCoords(coords);
      onPlaceSelect?.(coords);
      return;
    }

    destination.setValue(location.name);
    destination.closeDropdown();
    setDestinationCoords(coords);
    setSelectedPlace(location.name);
    onDestinationSelect?.(coords);
  }

  function handleSavedRouteSelect(route: SavedAccountRoute) {
    const endpoints = getRouteEndpoints(route);

    setShowDirections(true);
    setIsSavedRoutesOpen(false);
    origin.closeDropdown();
    destination.closeDropdown();
    setLocationError(false);

    origin.setValue(route.originLabel || "Začetek poti");
    destination.setValue(route.destinationLabel || "Konec poti");
    setOriginCoords(endpoints.origin);
    setDestinationCoords(endpoints.destination);
    setSelectedPlace(route.destinationLabel || route.name);
    onSavedRouteSelect?.(route);
  }

  function handleShowDirectionsClick() {
    destination.setIsOpen(false);
    origin.setIsOpen(canUseCurrentLocation || hasSavedLocations);
    setIsSavedRoutesOpen(false);
    setShowDirections(true);
  }

  function renderSavedRoutesDropdown() {
    if (!isSavedRoutesOpen) return null;

    return (
      <div className='max-h-72 overflow-y-auto rounded-lg bg-white text-neutral-900 shadow-lg dark:bg-neutral-700 dark:text-white'>
        {hasSavedRoutes ? (
          <ul>
            <li className='border-b border-border px-3 pb-1.5 pt-2 text-[11px] font-semibold uppercase tracking-wide text-muted-foreground dark:border-neutral-600'>
              Shranjene poti
            </li>
            {savedRoutes.map((route) => {
              const duration = formatSavedRouteDuration(route.duration);
              const distance = formatSavedRouteDistance(route.distance);

              return (
                <li
                  key={route.id}
                  onMouseDown={() => handleSavedRouteSelect(route)}
                  className='flex cursor-pointer items-start gap-3 border-b border-border px-3 py-2 last:border-0 hover:bg-muted dark:border-neutral-600 dark:hover:bg-neutral-600'>
                  <span className='mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-red-700 text-white shadow-sm'>
                    <Route size={15} />
                  </span>
                  <span className='min-w-0 text-left'>
                    <span className='block truncate text-sm font-semibold leading-tight'>
                      {route.name}
                    </span>
                    {(duration || distance) && (
                      <span className='mt-0.5 block text-xs leading-tight text-muted-foreground dark:text-neutral-300'>
                        {[duration, distance].filter(Boolean).join(" • ")}
                      </span>
                    )}
                    {(route.originLabel || route.destinationLabel) && (
                      <span className='mt-0.5 block truncate text-xs leading-tight text-muted-foreground dark:text-neutral-300'>
                        {[route.originLabel, route.destinationLabel]
                          .filter(Boolean)
                          .join(" → ")}
                      </span>
                    )}
                  </span>
                </li>
              );
            })}
          </ul>
        ) : (
          <p className='px-3 py-2 text-sm text-muted-foreground dark:text-neutral-300'>
            Ni še shranjenih poti.
          </p>
        )}
      </div>
    );
  }

  async function handleDestinationSelect(prediction: PlaceSuggestion) {
    destination.setValue(prediction.mainText);
    destination.closeDropdown();
    try {
      const res = await fetch(
        `https://places.googleapis.com/v1/places/${prediction.placeId}`,
        {
          headers: {
            "X-Goog-Api-Key": placesApiKey,
            "X-Goog-FieldMask": "location",
          },
        },
      );
      const data = await res.json();
      if (data.location) {
        setLocationError(false);
        setSelectedPlace(prediction.mainText);
        const coords = {
          lat: data.location.latitude,
          lng: data.location.longitude,
        };
        setDestinationCoords(coords);
        onDestinationSelect?.(coords);
      } else {
        setLocationError(true);
      }
    } catch {
      setLocationError(true);
    }
  }

  function handleSwap() {
    const tempValue = origin.value;
    origin.setValue(destination.value);
    destination.setValue(tempValue);
    if (originCoords && destinationCoords) {
      onPlaceSelect?.(destinationCoords);
      onDestinationSelect?.(originCoords);
      setOriginCoords(destinationCoords);
      setDestinationCoords(originCoords);
    }
  }

  function handleOriginFocus() {
    destination.setIsOpen(false);
    setIsSavedRoutesOpen(false);
    if (
      canUseCurrentLocation ||
      hasSavedLocations ||
      origin.predictions.length > 0
    ) {
      origin.setIsOpen(true);
    }
  }

  function handleDestinationFocus() {
    origin.setIsOpen(false);
    setIsSavedRoutesOpen(false);
    if (
      canUseCurrentLocation ||
      hasSavedLocations ||
      destination.predictions.length > 0
    ) {
      destination.setIsOpen(true);
    }
  }

  function renderLocationDropdown(kind: "origin" | "destination") {
    const autocomplete = kind === "origin" ? origin : destination;

    {
      /* upravljamo select uporabnika */
    }
    const handleCurrentSelect =
      kind === "origin"
        ? handleCurrentOriginSelect
        : handleCurrentDestinationSelect;

    const handlePredictionSelect =
      kind === "origin" ? handleOriginSelect : handleDestinationSelect;

    if (
      !autocomplete.isOpen ||
      (!canUseCurrentLocation &&
        !hasSavedLocations &&
        autocomplete.predictions.length === 0)
    ) {
      return null;
    }

    return (
      <ul className='overflow-hidden rounded-lg bg-white text-neutral-900 shadow-lg dark:bg-neutral-700 dark:text-white'>
        {/* trenutna lokacija uporabnika */}
        {canUseCurrentLocation && (
          <li
            onMouseDown={handleCurrentSelect}
            className='flex cursor-pointer items-center gap-3 border-b border-border px-3 py-2 last:border-0 hover:bg-muted dark:border-neutral-600 dark:hover:bg-neutral-600'>
            <LocateFixed size={16} className='shrink-0 text-muted-foreground' />
            <p className='text-sm font-medium leading-tight'>
              {currentLocationLabel}
            </p>
          </li>
        )}

        {/* shranjene lokacije */}
        {hasSavedLocations && (
          <>
            <li className='border-b border-border px-3 pb-1.5 pt-2 text-[11px] font-semibold uppercase tracking-wide text-muted-foreground dark:border-neutral-600'>
              Shranjene lokacije
            </li>
            {savedLocations.map((location) => (
              <li
                key={`${kind}-${location.id}`}
                onMouseDown={() => handleSavedLocationSelect(kind, location)}
                className='flex cursor-pointer items-center gap-3 border-b border-border px-3 py-2 last:border-0 hover:bg-muted dark:border-neutral-600 dark:hover:bg-neutral-600'>
                <span
                  className='flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-white shadow-sm'
                  style={{ backgroundColor: location.color }}>
                  <LocationIconGlyph icon={location.icon} size={15} />
                </span>
                <p className='min-w-0 truncate text-sm font-medium leading-tight'>
                  {location.name}
                </p>
              </li>
            ))}
          </>
        )}

        {/* predikcije autocomplete-a */}
        <div className='border-t-3 border-border dark:border-neutral-600'>
          {autocomplete.predictions.map((prediction) => (
            <li
              key={prediction.placeId}
              onMouseDown={() => handlePredictionSelect(prediction)}
              className='cursor-pointer border-b border-border px-3 py-2 last:border-0 hover:bg-muted dark:border-neutral-600 dark:hover:bg-neutral-600'>
              <p className='text-sm font-medium leading-tight'>
                {prediction.mainText}
              </p>
              <p className='mt-0.5 text-xs leading-tight text-muted-foreground'>
                {prediction.secondaryText}
              </p>
            </li>
          ))}
        </div>
      </ul>
    );
  }

  async function handleRouteRequest() {
    if (isRouteActive) {
      onEndRoute?.();
      return;
    }

    if (hasRoute) {
      onStartRoute?.();
      return;
    }

    if (!originCoords || !destinationCoords) return;

    setIsLoadingRoute(true);
    try {
      const params = new URLSearchParams({
        originLat: String(originCoords.lat),
        originLon: String(originCoords.lng),
        destinationLat: String(destinationCoords.lat),
        destinationLon: String(destinationCoords.lng),
        originAddress: origin.value,
        destinationAddress: destination.value,
        leaveNow: "false",
        bike: String(useBike),
        bus: String(useBus),
      });

      if (timeMode === "depart") {
        params.set("leaveAt", selectedTime);
      } else {
        params.set("arriveBy", selectedTime);
      }

      if (auth.currentUser?.uid) {
        params.set("userId", auth.currentUser.uid);
      }

      const res = await fetch(`${apiUrl}/compute?${params}`);
      if (!res.ok) {
        onPathError?.(await readComputePathError(res));
        return;
      }

      const journey = (await res.json()) as RoutePath;

      console.log("Received route path: ", journey);
      onPathReceive?.(journey);
    } catch (error) {
      onPathError?.({
        code: "ROUTE_REQUEST_FAILED",
        message:
          error instanceof Error
            ? error.message
            : "Route request failed before the server response was read.",
      });
    } finally {
      setIsLoadingRoute(false);
    }
  }

  async function readComputePathError(
    response: Response,
  ): Promise<RouteComputeError> {
    try {
      const data = (await response.json()) as ComputePathErrorResponse;
      return {
        code:
          typeof data.code === "string" && data.code.trim()
            ? data.code
            : `HTTP_${response.status}`,
        message: typeof data.message === "string" ? data.message : undefined,
      };
    } catch {
      return {
        code: `HTTP_${response.status}`,
        message: response.statusText || "Route request failed.",
      };
    }
  }
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setOriginIsOpen(false);
        setDestinationIsOpen(false);
        setIsSavedRoutesOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [setDestinationIsOpen, setOriginIsOpen]);

  return (
    <>
      {isLoadingRoute && (
        <RouteLoadingOverlay onDismiss={() => setIsLoadingRoute(false)} />
      )}
      <div className='pointer-events-none absolute inset-x-4 top-4 z-20 flex flex-row items-start gap-2'>
        <div className='mt-3 flex min-w-0 flex-1 flex-row items-start gap-2 max-[699px]:w-full max-[699px]:flex-none'>
          {/* logotip */}
          <img
            src='/logo.svg'
            alt='ŠibaM'
            className='pointer-events-auto h-10 w-auto shrink-0 cursor-pointer max-[699px]:hidden'
            onClick={() => navigate("/")}
          />

          {/* searchbar */}
          <div
            ref={containerRef}
            className='pointer-events-auto flex min-w-0 flex-1 flex-col gap-1 min-[700px]:w-[38rem] min-[700px]:flex-none'>
            {showDirections ? (
              <>
                <div className='relative'>
                  <div className='overflow-hidden rounded-lg bg-white/95 text-neutral-900 shadow-md dark:bg-neutral-700 dark:text-white'>
                    <div className='relative flex h-10 items-center pr-10'>
                      <Search
                        size={16}
                        className='pointer-events-none absolute left-3 z-10 shrink-0 text-muted-foreground'
                      />
                      <Input
                        type='text'
                        value={origin.value}
                        onChange={origin.handleChange}
                        onFocus={handleOriginFocus}
                        onKeyDown={(e) =>
                          e.key === "Escape" && origin.setIsOpen(false)
                        }
                        placeholder='Kje štartaš?'
                        className='h-full w-auto flex-1 rounded-none border-0 bg-transparent pl-8 pr-2 text-sm font-normal shadow-none dark:bg-transparent focus-visible:ring-0 focus-visible:outline-none'
                        aria-label='Kje štartaš?'
                      />
                      {origin.value && (
                        <button
                          type='button'
                          onClick={handleClear}
                          className='mr-1 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-muted-foreground hover:text-foreground dark:hover:text-white'
                          aria-label='Počisti'>
                          <X size={13} />
                        </button>
                      )}
                    </div>
                    <div className='h-px bg-border dark:bg-neutral-600' />
                    <div className='relative flex h-10 items-center pr-10'>
                      <Search
                        size={16}
                        className='pointer-events-none absolute left-3 z-10 shrink-0 text-muted-foreground'
                      />
                      <Input
                        type='text'
                        value={destination.value}
                        onChange={destination.handleChange}
                        onFocus={handleDestinationFocus}
                        onKeyDown={(e) =>
                          e.key === "Escape" && destination.setIsOpen(false)
                        }
                        placeholder='Kam šibaš?'
                        className='h-full w-auto flex-1 rounded-none border-0 bg-transparent pl-8 pr-2 text-sm font-normal shadow-none dark:bg-transparent focus-visible:ring-0 focus-visible:outline-none'
                        aria-label='Kam šibaš?'
                      />
                      {destination.value && (
                        <button
                          type='button'
                          onClick={() => {
                            destination.clear();
                            setDestinationCoords(null);
                            onDestinationSelect?.(null);
                          }}
                          className='mr-1 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-muted-foreground hover:text-foreground dark:hover:text-white'
                          aria-label='Počisti'>
                          <X size={13} />
                        </button>
                      )}
                    </div>
                  </div>
                  <button
                    type='button'
                    onClick={handleSwap}
                    className='absolute right-2 top-1/2 z-10 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-full bg-muted text-muted-foreground shadow-md transition-colors hover:text-foreground dark:bg-neutral-600 dark:text-neutral-300 dark:hover:text-white'
                    aria-label='Zamenjaj smeri'>
                    <ArrowUpDown size={16} />
                  </button>
                </div>
                {renderLocationDropdown("origin")}
                {renderLocationDropdown("destination")}
                {renderSavedRoutesDropdown()}
                <div className='flex items-center gap-2 max-[615px]:grid max-[615px]:grid-cols-2 max-[615px]:items-stretch max-[430px]:!grid-cols-1'>
                  <div className='flex items-center gap-2 max-[615px]:order-3 max-[615px]:col-start-1 max-[615px]:row-start-3 max-[615px]:grid max-[615px]:grid-cols-2 max-[430px]:!row-start-4'>
                    <button
                      type='button'
                      onClick={() => setUseBus((v) => !v)}
                      className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm shadow-md transition-colors max-[615px]:justify-center max-[615px]:px-2 ${useBus ? "bg-red-700 text-white" : "bg-white text-muted-foreground dark:bg-neutral-700 dark:text-neutral-400"}`}>
                      <Bus size={14} />
                      Bus
                    </button>
                    <button
                      type='button'
                      onClick={() => setUseBike((v) => !v)}
                      className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm shadow-md transition-colors max-[615px]:justify-center max-[615px]:px-2 ${useBike ? "bg-red-700 text-white" : "bg-white text-muted-foreground dark:bg-neutral-700 dark:text-neutral-400"}`}>
                      <Bike size={14} />
                      Kolo
                    </button>
                  </div>
                  <div className='flex min-w-[190px] overflow-hidden rounded-lg bg-white text-neutral-900 shadow-md max-[615px]:order-2 max-[615px]:col-start-1 max-[615px]:row-start-2 max-[615px]:w-full max-[430px]:!row-start-3 dark:bg-neutral-700 dark:text-white'>
                    <button
                      type='button'
                      onClick={() =>
                        setTimeMode((m) =>
                          m === "depart" ? "arrive" : "depart",
                        )
                      }
                      className='min-w-[5.6rem] flex-1 whitespace-nowrap px-3 py-1.5 text-sm transition-colors hover:bg-muted max-[615px]:px-2 dark:text-white dark:hover:bg-neutral-600'>
                      {timeMode === "depart" ? "Odhod ob" : "Prihod do"}
                    </button>
                    <div className='w-px bg-border dark:bg-neutral-600' />
                    <input
                      type='time'
                      value={selectedTime}
                      onChange={(e) => setSelectedTime(e.target.value)}
                      className='w-[5.5rem] bg-transparent px-2 py-1.5 text-sm focus:outline-none max-[615px]:w-[4.7rem] max-[615px]:px-1.5 dark:text-white'
                    />
                  </div>
                  <button
                    type='button'
                    onClick={handleRouteRequest}
                    disabled={
                      !hasRoute &&
                      !isRouteActive &&
                      (!originCoords || !destinationCoords)
                    }
                    className='ml-auto flex cursor-pointer items-center justify-center gap-1.5 whitespace-nowrap rounded-md bg-neutral-50 px-4 py-1.5 text-sm font-bold text-red-700 shadow-md transition-colors hover:bg-neutral-50 disabled:cursor-not-allowed disabled:bg-neutral-200 disabled:opacity-40 disabled:hover:bg-neutral-200 max-[615px]:order-4 max-[615px]:col-start-2 max-[615px]:row-start-2 max-[615px]:ml-0 max-[615px]:rounded-lg max-[615px]:px-3 max-[430px]:!col-start-1 max-[430px]:!row-start-2 max-[430px]:w-full dark:bg-neutral-200 dark:hover:bg-neutral-50 dark:disabled:bg-neutral-200 dark:disabled:hover:bg-neutral-200'>
                    {isRouteActive
                      ? "Končaj"
                      : hasRoute
                        ? "Začni"
                        : "Najdi pot"}
                  </button>
                  <button
                    type='button'
                    onClick={() => {
                      origin.setIsOpen(false);
                      destination.setIsOpen(false);
                      setIsSavedRoutesOpen((isOpen) => !isOpen);
                    }}
                    className='flex h-8 min-w-9 items-center justify-center whitespace-nowrap rounded-md bg-white/95 text-neutral-900 shadow-md transition-colors hover:text-red-700 max-[615px]:order-1 max-[615px]:col-span-2 max-[615px]:h-auto max-[615px]:w-full max-[615px]:gap-1.5 max-[615px]:px-3 max-[615px]:py-1.5 max-[615px]:text-sm max-[615px]:font-semibold max-[430px]:!col-span-1 dark:bg-neutral-700 dark:text-white dark:hover:text-red-200'
                    aria-label='Shranjene poti'>
                    <Bookmark size={16} />
                    <span className='hidden max-[615px]:inline'>
                      Shranjene poti
                    </span>
                  </button>
                </div>
              </>
            ) : (
              <>
                <div className='relative flex h-10 items-center rounded-lg bg-white/95 text-neutral-900 shadow-md dark:bg-neutral-700 dark:text-white'>
                  <Search
                    size={16}
                    className='pointer-events-none absolute left-3 z-10 shrink-0 text-muted-foreground'
                  />
                  <Input
                    type='text'
                    value={destination.value}
                    onChange={destination.handleChange}
                    onFocus={handleDestinationFocus}
                    onKeyDown={(e) =>
                      e.key === "Escape" && destination.setIsOpen(false)
                    }
                    placeholder='Kam šibaš?'
                    className='h-full w-auto flex-1 rounded-lg border-0 bg-transparent pl-8 pr-2 text-sm font-normal shadow-none dark:bg-transparent focus-visible:ring-0 focus-visible:outline-none'
                    aria-label='Kam šibaš?'
                  />
                  {destination.value && (
                    <button
                      type='button'
                      onClick={() => {
                        destination.clear();
                        setSelectedPlace("");
                        setDestinationCoords(null);
                        onDestinationSelect?.(null);
                      }}
                      className='mr-2 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-muted-foreground hover:text-foreground dark:hover:text-white'
                      aria-label='Počisti'>
                      <X size={13} />
                    </button>
                  )}
                  <button
                    type='button'
                    onClick={() => {
                      origin.setIsOpen(false);
                      destination.setIsOpen(false);
                      setIsSavedRoutesOpen((isOpen) => !isOpen);
                    }}
                    className='mr-2 flex h-6 w-6 cursor-pointer shrink-0 items-center justify-center rounded-sm text-muted-foreground transition-colors hover:text-red-700 dark:hover:text-red-200'
                    aria-label='Shranjene poti'>
                    <Bookmark size={16} />
                  </button>
                  {selectedPlace && !showDirections && (
                    <button
                      type='button'
                      onClick={handleShowDirectionsClick}
                      className='mr-2 flex h-6 w-6 cursor-pointer shrink-0 rotate-45 items-center justify-center rounded-sm bg-red-700 text-white shadow-sm transition-colors hover:bg-red-600'
                      aria-label='Navodila za pot'>
                      <Route size={14} className='-rotate-45' />
                    </button>
                  )}
                </div>
                {renderLocationDropdown("destination")}
                {renderSavedRoutesDropdown()}
              </>
            )}
            {locationError && (
              <p className='rounded-lg bg-card px-3 py-2 text-xs text-red-600 shadow-lg dark:bg-neutral-700 dark:text-red-400'>
                Lokacije ni bilo mogoče najti. Prosimo poskusite znova.
              </p>
            )}
          </div>

          {/* vreme */}
          <div className='shrink-0'>
            <WeatherWidget />
          </div>
        </div>

        {/* Desni panel */}
        {isLoggedIn ? (
          <div
            className={`pointer-events-auto absolute right-0 flex shrink-0 flex-row gap-2 min-[700px]:top-3 max-[450px]:!fixed max-[450px]:!bottom-12 max-[450px]:!left-4 max-[450px]:!right-auto max-[450px]:!top-auto max-[450px]:flex-col ${showDirections ? "max-[700px]:top-24" : "max-[700px]:top-14"}`}>
            <Button
              type='button'
              onClick={() => navigate("/account")}
              className='flex h-10 w-10 items-center justify-center rounded-md bg-red-700 text-white shadow-lg hover:text-red-200'
              aria-label='Profil'>
              <UserRound strokeWidth={1.7} />
            </Button>
            <div className='flex flex-col gap-2'>
              <Button
                type='button'
                onClick={() => {
                  auth.signOut();
                  navigate("/login");
                }}
                aria-label='Odjava'
                className='flex h-10 w-10 items-center justify-center rounded-md bg-red-700 text-white shadow-lg hover:text-red-200'>
                <LogOut />
              </Button>
              <ThemeToggle />
              <Button
                type='button'
                onClick={onZoomIn}
                className='flex h-10 w-10 items-center justify-center rounded-md bg-white/85 text-neutral-900 shadow-lg hover:text-red-700 dark:bg-neutral-700 dark:text-white dark:hover:text-red-200'
                aria-label='Povečaj'>
                <Plus size={20} />
              </Button>
              <Button
                type='button'
                onClick={onZoomOut}
                className='flex h-10 w-10 items-center justify-center rounded-md bg-white/85 text-neutral-900 shadow-lg hover:text-red-700 dark:bg-neutral-700 dark:text-white dark:hover:text-red-200'
                aria-label='Pomanjšaj'>
                <Minus size={20} />
              </Button>
              <Button
                type='button'
                onClick={onLocate}
                disabled={isRouteActive}
                title={
                  isRouteActive
                    ? "Med sledenjem poti se zemljevid centrira samodejno."
                    : undefined
                }
                className='flex h-10 w-10 items-center justify-center rounded-md bg-white/85 text-neutral-900 shadow-lg hover:text-red-700 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-neutral-700 dark:text-white dark:hover:text-red-200'
                aria-label='Moja lokacija'>
                <LocateFixed size={20} />
              </Button>
            </div>
          </div>
        ) : (
          <div
            className={`pointer-events-auto absolute right-0 flex shrink-0 flex-col gap-2 min-[700px]:top-2 max-[450px]:!fixed max-[450px]:!bottom-12 max-[450px]:!left-4 max-[450px]:!right-auto max-[450px]:!top-auto ${showDirections ? "max-[699px]:top-24" : "max-[699px]:top-14"}`}>
            <Button
              type='button'
              onClick={() => navigate("/login")}
              className='flex h-10 w-10 items-center justify-center rounded-md bg-red-700 text-white shadow-lg hover:text-red-200'
              aria-label='Profil'>
              <UserRound strokeWidth={1.7} />
            </Button>
            <ThemeToggle />
            <Button
              type='button'
              onClick={onZoomIn}
              className='flex h-10 w-10 items-center justify-center rounded-md bg-white/85 text-neutral-900 shadow-lg hover:text-red-700 dark:bg-neutral-700 dark:text-white dark:hover:text-red-200'
              aria-label='Povečaj'>
              <Plus size={20} />
            </Button>
            <Button
              type='button'
              onClick={onZoomOut}
              className='flex h-10 w-10 items-center justify-center rounded-md bg-white/85 text-neutral-900 shadow-lg hover:text-red-700 dark:bg-neutral-700 dark:text-white dark:hover:text-red-200'
              aria-label='Pomanjšaj'>
              <Minus size={20} />
            </Button>
            <Button
              type='button'
              onClick={onLocate}
              disabled={isRouteActive}
              title={
                isRouteActive
                  ? "Med sledenjem poti se zemljevid centrira samodejno."
                  : undefined
              }
              className='flex h-10 w-10 items-center justify-center rounded-md bg-white/85 text-neutral-900 shadow-lg hover:text-red-700 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-neutral-700 dark:text-white dark:hover:text-red-200'
              aria-label='Moja lokacija'>
              <LocateFixed size={20} />
            </Button>
          </div>
        )}
      </div>
    </>
  );
};
