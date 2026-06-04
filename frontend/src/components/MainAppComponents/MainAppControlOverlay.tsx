import { LocateFixed, Route } from "lucide-react";
import { WeatherWidget } from "./WeatherWidget";
import { RouteLoadingOverlay } from "./RouteLoadingOverlay";
import type { RoutePath } from "./RoutePolyline";
import { useNavigate } from "react-router-dom";
import { auth } from "../../firebase";
import { onAuthStateChanged } from "firebase/auth";
import { useState, useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import {
  usePlacesAutocomplete,
  MARIBOR_BOUNDS,
  type PlaceSuggestion,
} from "../../hooks/usePlacesAutocomplete";
import { LocationIconGlyph, type LocationIcon } from "./MapLocationPopup";
import type { SavedAccountRoute } from "../Pages/AccountPageComponents/SavedRouteMapCard";
import { DestinationSearch } from "./MainAppControlOverlayComponents/DestinationSearch";
import { DirectionsInputs } from "./MainAppControlOverlayComponents/DirectionsInputs";
import { MapControls } from "./MainAppControlOverlayComponents/MapControls";
import { RouteControls } from "./MainAppControlOverlayComponents/RouteControls";
import type {
  Coordinates,
  TimeMode,
} from "./MainAppControlOverlayComponents/types";

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

function toDateString(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function getWeekDates(): Array<{value: string; label: string}> {
  return Array.from({length: 7}, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() + i);
    return {
      value: toDateString(d),
      label: d.toLocaleDateString("sl-SI", {weekday: "short", day: "numeric", month: "numeric"}),
    };
  });
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
      ) ??
      getJourneyPoint(firstLeg?.polyline[0]),
    destination:
      getJourneyPoint(
        route.journey.destination as Parameters<typeof getJourneyPoint>[0],
      ) ?? getJourneyPoint(lastLeg?.polyline.at(-1)),
  };
}

function buildRouteRequestSignature({
  originCoords,
  destinationCoords,
  originAddress,
  destinationAddress,
  useBike,
  useBus,
  timeMode,
  selectedTime,
  selectedDate,
}: {
  originCoords: Coordinates | null;
  destinationCoords: Coordinates | null;
  originAddress: string;
  destinationAddress: string;
  useBike: boolean;
  useBus: boolean;
  timeMode: TimeMode;
  selectedTime: string;
  selectedDate: string;
}) {
  if (!originCoords || !destinationCoords) return null;

  return JSON.stringify({
    originLat: originCoords.lat,
    originLon: originCoords.lng,
    destinationLat: destinationCoords.lat,
    destinationLon: destinationCoords.lng,
    originAddress,
    destinationAddress,
    bike: useBike,
    bus: useBus,
    timeMode,
    selectedTime,
    selectedDate,
  });
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
  const [lastRouteRequestSignature, setLastRouteRequestSignature] = useState<
    string | null
  >(null);
  const [isSavedRoutesOpen, setIsSavedRoutesOpen] = useState(false);
  const [timeMode, setTimeMode] = useState<TimeMode>("depart");
  const [selectedTime, setSelectedTime] = useState(() => {
    const now = new Date();
    now.setMinutes(now.getMinutes() + 1);
    return `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
  });
  const [selectedDate, setSelectedDate] = useState(() => toDateString(new Date()));
  const [isDateOpen, setIsDateOpen] = useState(false);
  const [dateDropdownPos, setDateDropdownPos] = useState<{top: number; left: number} | null>(null);
  const dateBtnRef = useRef<HTMLButtonElement>(null);
  const dateMenuRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const origin = usePlacesAutocomplete(placesApiKey);
  const destination = usePlacesAutocomplete(placesApiKey);
  const originInput = {
    ...origin,
    handleChange(event: Parameters<typeof origin.handleChange>[0]) {
      origin.handleChange(event);
      setOriginCoords(null);
      onPlaceSelect?.(null);
    },
  };
  const destinationInput = {
    ...destination,
    handleChange(event: Parameters<typeof destination.handleChange>[0]) {
      destination.handleChange(event);
      setDestinationCoords(null);
      setSelectedPlace("");
      onDestinationSelect?.(null);
    },
  };
  const { setIsOpen: setOriginIsOpen } = origin;
  const { setIsOpen: setDestinationIsOpen } = destination;
  const canUseCurrentLocation =
    currentLocation !== null &&
    currentLocation !== undefined &&
    isInsideMaribor(currentLocation);
  const hasSavedLocations = savedLocations.length > 0;
  const hasSavedRoutes = savedRoutes.length > 0;
  const routeRequestSignature = buildRouteRequestSignature({
    originCoords,
    destinationCoords,
    originAddress: origin.value,
    destinationAddress: destination.value,
    useBike,
    useBus,
    timeMode,
    selectedTime,
    selectedDate,
  });
  const isRouteStale =
    hasRoute &&
    lastRouteRequestSignature !== null &&
    routeRequestSignature !== lastRouteRequestSignature;

  function getCurrentLocationCoords() {
    if (!currentLocation || !canUseCurrentLocation) return null;

    return {
      lat: currentLocation.lat,
      lng: currentLocation.lng,
    };
  }

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setIsLoggedIn(!!user);
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    if (!hasRoute) {
      setLastRouteRequestSignature(null);
      return;
    }

    if (routeRequestSignature && lastRouteRequestSignature === null) {
      setLastRouteRequestSignature(routeRequestSignature);
    }
  }, [hasRoute, lastRouteRequestSignature, routeRequestSignature]);

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
    setLastRouteRequestSignature(
      buildRouteRequestSignature({
        originCoords: endpoints.origin,
        destinationCoords: endpoints.destination,
        originAddress: route.originLabel || "Začetek poti",
        destinationAddress: route.destinationLabel || "Konec poti",
        useBike,
        useBus,
        timeMode,
        selectedTime,
        selectedDate,
      }),
    );
    onSavedRouteSelect?.(route);
  }

  function handleShowDirectionsClick() {
    destination.setIsOpen(false);
    origin.setIsOpen(canUseCurrentLocation || hasSavedLocations);
    setIsSavedRoutesOpen(false);
    setShowDirections(true);
  }

  function handleDestinationClear() {
    destination.clear();
    setDestinationCoords(null);
    onDestinationSelect?.(null);
  }

  function handleDestinationSearchClear() {
    handleDestinationClear();
    setSelectedPlace("");
  }

  function handleSavedRoutesToggle() {
    origin.setIsOpen(false);
    destination.setIsOpen(false);
    setIsSavedRoutesOpen((isOpen) => !isOpen);
  }

  function handleToggleTimeMode() {
    setTimeMode((mode) => (mode === "depart" ? "arrive" : "depart"));
  }

  function handleProfileClick() {
    navigate(isLoggedIn ? "/account" : "/login");
  }

  function handleLogoutClick() {
    auth.signOut();
    navigate("/login");
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
                <li key={route.id} className='border-b border-border last:border-0 dark:border-neutral-600'>
                  <button
                    type='button'
                    onMouseDown={() => handleSavedRouteSelect(route)}
                    className='flex w-full cursor-pointer items-start gap-3 px-3 py-2 text-left hover:bg-muted dark:hover:bg-neutral-600'>
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
                  </button>
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
    const tempCoords = originCoords;

    origin.setValue(destination.value);
    destination.setValue(tempValue);

    setOriginCoords(destinationCoords);
    setDestinationCoords(tempCoords);
    onPlaceSelect?.(destinationCoords);
    onDestinationSelect?.(tempCoords);
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
          <li className='border-b border-border last:border-0 dark:border-neutral-600'>
            <button
              type='button'
              onMouseDown={handleCurrentSelect}
              className='flex w-full cursor-pointer items-center gap-3 px-3 py-2 text-left hover:bg-muted dark:hover:bg-neutral-600'>
              <LocateFixed
                size={16}
                className='shrink-0 text-muted-foreground'
              />
              <span className='text-sm font-medium leading-tight'>
                {currentLocationLabel}
              </span>
            </button>
          </li>
        )}

        {/* shranjene lokacije */}
        {hasSavedLocations && (
          <>
            <li className='border-b border-border px-3 pb-1.5 pt-2 text-[11px] font-semibold uppercase tracking-wide text-muted-foreground dark:border-neutral-600'>
              Shranjene lokacije
            </li>
            {savedLocations.map((location) => (
              <li key={`${kind}-${location.id}`} className='border-b border-border last:border-0 dark:border-neutral-600'>
                <button
                  type='button'
                  onMouseDown={() => handleSavedLocationSelect(kind, location)}
                  className='flex w-full cursor-pointer items-center gap-3 px-3 py-2 text-left hover:bg-muted dark:hover:bg-neutral-600'>
                  <span
                    className='flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-white shadow-sm'
                    style={{ backgroundColor: location.color }}>
                    <LocationIconGlyph icon={location.icon} size={15} />
                  </span>
                  <span className='min-w-0 truncate text-sm font-medium leading-tight'>
                    {location.name}
                  </span>
                </button>
              </li>
            ))}
          </>
        )}

        {/* predikcije autocomplete-a */}
        {autocomplete.predictions.map((prediction, index) => (
          <li
            key={prediction.placeId}
            className={`border-b border-border last:border-0 dark:border-neutral-600 ${
              index === 0 ? "border-t-3" : ""
            }`}>
            <button
              type='button'
              onMouseDown={() => handlePredictionSelect(prediction)}
              className='w-full cursor-pointer px-3 py-2 text-left hover:bg-muted dark:hover:bg-neutral-600'>
              <span className='block text-sm font-medium leading-tight'>
                {prediction.mainText}
              </span>
              <span className='mt-0.5 block text-xs leading-tight text-muted-foreground'>
                {prediction.secondaryText}
              </span>
            </button>
          </li>
        ))}
      </ul>
    );
  }

  function handleDateButtonClick() {
    if (!isDateOpen && dateBtnRef.current) {
      const rect = dateBtnRef.current.getBoundingClientRect();
      setDateDropdownPos({ top: rect.bottom + 4, left: rect.left });
    }
    setIsDateOpen((v) => !v);
  }

  function renderDateDropdown() {
    if (!isDateOpen || !dateDropdownPos) return null;
    return createPortal(
      <div
        ref={dateMenuRef}
        style={{ position: "fixed", top: dateDropdownPos.top, left: dateDropdownPos.left }}
        className='z-[9999] overflow-hidden rounded-lg bg-white shadow-lg dark:bg-neutral-700'>
        {getWeekDates().map(({ value, label }) => (
          <button
            key={value}
            type='button'
            onClick={() => { setSelectedDate(value); setIsDateOpen(false); }}
            className={`block w-full whitespace-nowrap px-4 py-2 text-left text-sm transition-colors ${selectedDate === value ? "bg-red-700 text-white" : "text-neutral-900 hover:bg-muted dark:text-white dark:hover:bg-neutral-600"}`}>
            {label}
          </button>
        ))}
      </div>,
      document.body
    );
  }

  async function handleRouteRequest() {
    if (isRouteActive) {
      onEndRoute?.();
      return;
    }

    if (hasRoute && !isRouteStale) {
      onStartRoute?.();
      return;
    }

    if (!originCoords || !destinationCoords || !routeRequestSignature) return;

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

      params.set("date", selectedDate);

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

      const journey = normalizeRouteResponse(await res.json());

      console.log("Received route path: ", journey);
      setLastRouteRequestSignature(routeRequestSignature);
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

  function normalizeRouteResponse(data: unknown): RoutePath {
    const response = data as {
      routes?: RoutePath[];
      status?: string;
    };

    const firstRoute = response.routes?.[0];
    if (firstRoute && Array.isArray(firstRoute.legs)) {
      return {
        ...firstRoute,
        routes: response.routes,
      };
    }

    return {
      ...response,
      legs: [],
    };
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

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      const target = e.target as Node;
      if (
        !dateBtnRef.current?.contains(target) &&
        !dateMenuRef.current?.contains(target)
      ) {
        setIsDateOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <>
      {isLoadingRoute && (
        <RouteLoadingOverlay onDismiss={() => setIsLoadingRoute(false)} />
      )}
      <div className='pointer-events-none absolute inset-x-4 top-4 z-20 flex flex-row items-start gap-2'>
        <div className='mt-3 flex min-w-0 flex-1 flex-row items-start gap-2 max-[699px]:w-full max-[699px]:flex-none'>
          {/* logotip */}
          <button
            type='button'
            className='pointer-events-auto h-10 w-auto shrink-0 cursor-pointer max-[699px]:hidden'
            onClick={() => navigate("/")}
            aria-label='Domov'>
            <img src='/logo.svg' alt='ŠibaM' className='h-full w-auto' />
          </button>

          {/* searchbar */}
          <div
            ref={containerRef}
            className='pointer-events-auto flex min-w-0 flex-1 flex-col gap-1 min-[700px]:w-[38rem] min-[700px]:flex-none'>
            {showDirections ? (
              <>
                <DirectionsInputs
                  origin={originInput}
                  destination={destinationInput}
                  onOriginFocus={handleOriginFocus}
                  onDestinationFocus={handleDestinationFocus}
                  onOriginClear={handleClear}
                  onDestinationClear={handleDestinationClear}
                  onSwap={handleSwap}
                />
                {renderLocationDropdown("origin")}
                {renderLocationDropdown("destination")}
                {renderSavedRoutesDropdown()}
                <RouteControls
                  useBus={useBus}
                  useBike={useBike}
                  timeMode={timeMode}
                  selectedTime={selectedTime}
                  selectedDate={selectedDate}
                  dateButtonRef={dateBtnRef}
                  hasRoute={hasRoute}
                  isRouteStale={isRouteStale}
                  isRouteActive={isRouteActive}
                  originCoords={originCoords}
                  destinationCoords={destinationCoords}
                  onToggleBus={() => setUseBus((value) => !value)}
                  onToggleBike={() => setUseBike((value) => !value)}
                  onToggleTimeMode={handleToggleTimeMode}
                  onSelectedTimeChange={setSelectedTime}
                  onDateButtonClick={handleDateButtonClick}
                  onRouteRequest={handleRouteRequest}
                  onSavedRoutesToggle={handleSavedRoutesToggle}
                  dateDropdown={renderDateDropdown()}
                />
              </>
            ) : (
              <>
                <DestinationSearch
                  destination={destinationInput}
                  selectedPlace={selectedPlace}
                  onDestinationFocus={handleDestinationFocus}
                  onDestinationClear={handleDestinationSearchClear}
                  onSavedRoutesToggle={handleSavedRoutesToggle}
                  onShowDirectionsClick={handleShowDirectionsClick}
                />
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

        <MapControls
          isLoggedIn={isLoggedIn}
          showDirections={showDirections}
          isRouteActive={isRouteActive}
          onProfileClick={handleProfileClick}
          onLogoutClick={handleLogoutClick}
          onZoomIn={onZoomIn}
          onZoomOut={onZoomOut}
          onLocate={onLocate}
        />
      </div>
    </>
  );
};
