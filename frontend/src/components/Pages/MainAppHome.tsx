import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Bike, Bus, Footprints } from "lucide-react";
import { toast } from "sonner";
import {
  MainAppControlOverlay,
  type RouteComputeError,
} from "../MainAppComponents/MainAppControlOverlay";
import { MainMap, type SavedMapLocation } from "../MainAppComponents/MainMap";
import {
  isLocationIcon,
  type LocationIcon,
} from "../MainAppComponents/MapLocationPopup";
import { RouteOptions } from "../MainAppComponents/RouteOptions";
import type { RoutePopupSelection } from "../MainAppComponents/RoutePopup";
import type {
  MapPoint,
  RouteLeg,
  RoutePath,
} from "../MainAppComponents/RoutePolyline";
import { useUserSession } from "../Authorization/UserSessionProvider";
import { MARIBOR_BOUNDS } from "../../hooks/usePlacesAutocomplete";
import { buildApiUrl, parseUuid } from "../../lib/api";
import { getInstructionText } from "../../lib/text";
import type { SavedAccountRoute } from "./AccountPageComponents/SavedRouteMapCard";

const routeOptions = [
  {
    title: "Najhitrejša",
    time: "18 min",
    className:
      "border-red-200 bg-red-50 text-red-950 ring-4 dark:bg-[#941d38] dark:text-white",
    icons: [Bus, Footprints, Bike],
  },
  // {
  //   title: "Najbolj zelena",
  //   time: "24 min",
  //   className:
  //     "border-emerald-200 bg-emerald-50 text-emerald-950 dark:border-neutral-500 dark:bg-[#1d431b] dark:text-white",
  //   icons: [Footprints, Bike],
  // },
  {
    title: "Brez kolesa",
    time: "22 min",
    className:
      "border-neutral-200 bg-white text-neutral-950 dark:border-neutral-600 dark:bg-[#2c2c2a] dark:text-white",
    icons: [Bus, Footprints],
  },
];

const fallbackCenter = {
  lat: 46.5547,
  lng: 15.6459,
};

type MapCenter = {
  lat: number;
  lng: number;
};

type MapLocationDraft = {
  position: MapCenter;
  name: string;
  color: string;
  icon: LocationIcon;
};

type ActiveRouteStep = {
  instruction: string;
  mode: string;
  stepIndex: number;
};

type ClosestRoutePoint = {
  leg: RouteLeg;
  legIndex: number;
  polylineIndex: number;
  distance: number;
};

type SavedLocationResponse = {
  id: unknown;
  name: string;
  latitude: number;
  longitude: number;
  color?: string | null;
  logo?: string | null;
};

type SavedRouteResponse = {
  id: unknown;
  name?: string | null;
  journey?: RoutePath & {
    duration?: string | null;
    distance?: string | null;
    origin_address?: string | null;
    originAddress?: string | null;
    destination_address?: string | null;
    destinationAddress?: string | null;
  };
  createdAt?: string | null;
};

const defaultLocationColor = "#b91c1c";
const defaultLocationIcon: LocationIcon = "home";

function isInsideMaribor({ lat, lng }: MapCenter) {
  return (
    lat >= MARIBOR_BOUNDS.low.latitude &&
    lat <= MARIBOR_BOUNDS.high.latitude &&
    lng >= MARIBOR_BOUNDS.low.longitude &&
    lng <= MARIBOR_BOUNDS.high.longitude
  );
}

function normalizeSavedRoute(
  route: SavedRouteResponse,
): SavedAccountRoute | null {
  const journey = route.journey;
  const hasDrawableRoute = journey?.legs?.some((leg) => leg.polyline.length);

  if (!journey || !hasDrawableRoute) return null;

  return {
    id: parseUuid(route.id, "route.id"),
    name: route.name?.trim() || "Shranjena pot",
    journey,
    duration: journey.duration,
    distance: journey.distance,
    originLabel: journey.origin_address ?? journey.originAddress,
    destinationLabel: journey.destination_address ?? journey.destinationAddress,
    modes: Array.from(
      new Set(
        (journey.legs ?? [])
          .map((leg) => leg.mode?.trim())
          .filter((mode): mode is string => Boolean(mode)),
      ),
    ),
    createdAt: route.createdAt,
  };
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
): MapCenter | null {
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

function getRouteEndpoints(path: RoutePath) {
  const firstLeg = path.legs[0];
  const lastLeg = path.legs.at(-1);

  return {
    origin:
      getJourneyPoint(path.origin as Parameters<typeof getJourneyPoint>[0]) ??
      getJourneyPoint(firstLeg?.polyline[0]),
    destination:
      getJourneyPoint(
        path.destination as Parameters<typeof getJourneyPoint>[0],
      ) ?? getJourneyPoint(lastLeg?.polyline.at(-1)),
  };
}

function getModeLabel(mode: string) {
  switch (mode) {
    case "WALK":
      return "Peš";
    case "BIKE":
      return "Kolo";
    case "BUS":
      return "Bus";
    default:
      return mode;
  }
}

function toFiniteNumber(value: unknown) {
  const numberValue = typeof value === "number" ? value : Number(value);
  return Number.isFinite(numberValue) ? numberValue : null;
}

function getSquaredDistance(firstPoint: MapCenter, secondPoint: MapCenter) {
  const latDistance = firstPoint.lat - secondPoint.lat;
  const lngDistance = firstPoint.lng - secondPoint.lng;

  return latDistance * latDistance + lngDistance * lngDistance;
}

function getActiveRouteStep(
  legs: RouteLeg[] | undefined,
  currentLocation: MapCenter | null,
): ActiveRouteStep | null {
  if (!legs?.length || !currentLocation) return null;

  let closestPoint: ClosestRoutePoint | null = null;

  for (const [legIndex, leg] of legs.entries()) {
    for (const [polylineIndex, point] of leg.polyline.entries()) {
      const distance = getSquaredDistance(currentLocation, {
        lat: point.lat,
        lng: point.lon,
      });

      if (!closestPoint || distance < closestPoint.distance) {
        closestPoint = {
          leg,
          legIndex,
          polylineIndex,
          distance,
        };
      }
    }
  }

  if (!closestPoint) return null;

  const activeStep = closestPoint.leg.steps?.find((step) => {
    const startPolylineIndex = toFiniteNumber(step.startPolylineIndex);
    const endPolylineIndex = toFiniteNumber(step.endPolylineIndex);

    if (startPolylineIndex === null || endPolylineIndex === null) {
      return false;
    }

    return (
      closestPoint.polylineIndex >= startPolylineIndex &&
      closestPoint.polylineIndex <= endPolylineIndex
    );
  });

  if (!activeStep) return null;

  const instruction = getInstructionText(activeStep.instruction);
  if (!instruction) return null;

  let stepIndex = 0;
  for (const [legIndex, leg] of legs.entries()) {
    for (const step of leg.steps ?? []) {
      const stepInstruction = getInstructionText(step.instruction);
      if (!stepInstruction) continue;

      if (legIndex === closestPoint.legIndex && step === activeStep) {
        return {
          instruction,
          mode: closestPoint.leg.mode,
          stepIndex,
        };
      }

      stepIndex += 1;
    }
  }

  return {
    instruction,
    mode: closestPoint.leg.mode,
    stepIndex: -1,
  };
}

export const MainAppHome = () => {
  const { userSession, getAuthToken, fetchUserSession } = useUserSession();
  const [center, setCenter] = useState<MapCenter>(fallbackCenter);
  const [zoom, setZoom] = useState(14);
  const [selectedLeg, setSelectedLeg] = useState<RoutePopupSelection | null>(
    null,
  );
  const [markerPosition, setMarkerPosition] = useState<MapCenter | null>(null);
  const [userLocationPosition, setUserLocationPosition] =
    useState<MapCenter | null>(null);
  const [displayedUserLocationPosition, setDisplayedUserLocationPosition] =
    useState<MapCenter | null>(null);
  const [destinationMarkerPosition, setDestinationMarkerPosition] =
    useState<MapCenter | null>(null);
  const [routePath, setRoutePath] = useState<RoutePath | null>(null);
  const [isShowingSavedRoute, setIsShowingSavedRoute] = useState(false);
  const [isFollowingRoute, setIsFollowingRoute] = useState(false);
  const [routeFitBoundsTrigger, setRouteFitBoundsTrigger] = useState(0);
  const [routeComputeError, setRouteComputeError] =
    useState<RouteComputeError | null>(null);
  const [mapLocationDraft, setMapLocationDraft] =
    useState<MapLocationDraft | null>(null);
  const [savedLocations, setSavedLocations] = useState<SavedMapLocation[]>([]);
  const [savedRoutes, setSavedRoutes] = useState<SavedAccountRoute[]>([]);
  const [deletingSavedLocationId, setDeletingSavedLocationId] = useState<
    string | null
  >(null);
  const hasShownOutOfCoverageToast = useRef(false);
  const displayedUserLocationPositionRef = useRef<MapCenter | null>(null);
  const userLocationAnimationFrameRef = useRef<number | null>(null);
  const activeRouteStep = useMemo(
    () =>
      isFollowingRoute
        ? getActiveRouteStep(routePath?.legs, userLocationPosition)
        : null,
    [isFollowingRoute, routePath?.legs, userLocationPosition],
  );

  function updateDisplayedUserLocationPosition(position: MapCenter | null) {
    displayedUserLocationPositionRef.current = position;
    setDisplayedUserLocationPosition(position);
  }

  const applyUserLocation = useCallback(
    (position: GeolocationPosition, source: "watch" | "poll" = "watch") => {
      const userPosition = {
        lat: position.coords.latitude, //46.5545008
        lng: position.coords.longitude, //15.64980425
      };

      if (import.meta.env.DEV) {
        console.log("User location update", {
          source,
          ...userPosition,
          accuracy: position.coords.accuracy,
        });
      }

      setUserLocationPosition(userPosition);

      if (isFollowingRoute) {
        setCenter(userPosition);
        setZoom((currentZoom) => Math.max(currentZoom, 17));
      }
    },
    [isFollowingRoute],
  );

  // iskanje userjeve lokacije
  function locateUser({
    zoomToUser = false,
    showOutOfCoverageToast = false,
  } = {}) {
    if (!navigator.geolocation) return;

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const userPosition = {
          lat: position.coords.latitude, //46.5575
          lng: position.coords.longitude, // 15.6456
        };

        setUserLocationPosition(userPosition);

        if (!isInsideMaribor(userPosition)) {
          setCenter(fallbackCenter);
          setZoom(14);
          if (showOutOfCoverageToast && !hasShownOutOfCoverageToast.current) {
            hasShownOutOfCoverageToast.current = true;
            toast.info(
              "Trenutno si izven območja pokritosti. Prikazujemo Maribor.",
            );
          }
          return;
        }

        setCenter(userPosition);

        if (zoomToUser) {
          setZoom(16);
        }
      },
      () => {
        setCenter(fallbackCenter);
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 60000,
      },
    );
  }

  useEffect(() => {
    locateUser();
  }, []);

  useEffect(() => {
    if (!navigator.geolocation) return;

    const watchId = navigator.geolocation.watchPosition(
      (position) => applyUserLocation(position, "watch"),
      () => undefined,
      {
        enableHighAccuracy: true,
        timeout: 3000,
        maximumAge: 0,
      },
    );

    return () => navigator.geolocation.clearWatch(watchId);
  }, [applyUserLocation]);

  useEffect(() => {
    if (!navigator.geolocation || !isFollowingRoute) return;

    let isActive = true;
    let isRequestPending = false;

    const requestCurrentLocation = () => {
      if (isRequestPending) return;

      isRequestPending = true;
      // console.log("polling current location");

      navigator.geolocation.getCurrentPosition(
        (position) => {
          isRequestPending = false;
          if (!isActive) return;

          applyUserLocation(position, "poll");
        },
        (/*error*/) => {
          isRequestPending = false;
          if (!isActive) return;

          // console.log("poll location error", error);
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 5000,
        },
      );
    };

    requestCurrentLocation();

    const intervalId = window.setInterval(requestCurrentLocation, 3000);

    return () => {
      isActive = false;
      window.clearInterval(intervalId);
    };
  }, [applyUserLocation, isFollowingRoute]);

  useEffect(() => {
    if (userLocationAnimationFrameRef.current !== null) {
      cancelAnimationFrame(userLocationAnimationFrameRef.current);
      userLocationAnimationFrameRef.current = null;
    }

    if (!userLocationPosition) {
      updateDisplayedUserLocationPosition(null);
      return;
    }

    if (!isFollowingRoute) {
      updateDisplayedUserLocationPosition(userLocationPosition);
      return;
    }

    const startPosition =
      displayedUserLocationPositionRef.current ?? userLocationPosition;
    const endPosition = userLocationPosition;
    const startTime = performance.now();
    const durationMs = 700;

    function animateFrame(now: number) {
      const progress = Math.min((now - startTime) / durationMs, 1);
      const easedProgress = 1 - Math.pow(1 - progress, 3);

      updateDisplayedUserLocationPosition({
        lat:
          startPosition.lat +
          (endPosition.lat - startPosition.lat) * easedProgress,
        lng:
          startPosition.lng +
          (endPosition.lng - startPosition.lng) * easedProgress,
      });

      if (progress < 1) {
        userLocationAnimationFrameRef.current =
          requestAnimationFrame(animateFrame);
      } else {
        userLocationAnimationFrameRef.current = null;
      }
    }

    userLocationAnimationFrameRef.current = requestAnimationFrame(animateFrame);

    return () => {
      if (userLocationAnimationFrameRef.current !== null) {
        cancelAnimationFrame(userLocationAnimationFrameRef.current);
        userLocationAnimationFrameRef.current = null;
      }
    };
  }, [isFollowingRoute, userLocationPosition]);

  useEffect(() => {
    let isActive = true;

    async function fetchSavedLocations() {
      const token = await getAuthToken();

      if (!token) {
        setSavedLocations([]);
        return;
      }

      try {
        const session = userSession ?? (await fetchUserSession(token));

        const response = await fetch(
          buildApiUrl("api", "locations", session.id),
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          },
        );

        if (!response.ok) {
          throw new Error(`Saved locations request failed: ${response.status}`);
        }

        const locations = (await response.json()) as SavedLocationResponse[];

        if (!isActive) return;

        setSavedLocations(
          locations
            .filter(
              (location) =>
                Number.isFinite(location.latitude) &&
                Number.isFinite(location.longitude),
            )
            .map((location) => ({
              id: parseUuid(location.id, "location.id"),
              name: location.name,
              position: {
                lat: location.latitude,
                lng: location.longitude,
              },
              color: location.color ?? defaultLocationColor,
              icon: isLocationIcon(location.logo)
                ? location.logo
                : defaultLocationIcon,
            })),
        );
      } catch {
        if (!isActive) return;
        toast.error("Shranjene lokacije niso bile naložene.");
      }
    }

    void fetchSavedLocations();

    return () => {
      isActive = false;
    };
  }, [fetchUserSession, getAuthToken, userSession]);

  useEffect(() => {
    let isActive = true;

    async function fetchSavedRoutes() {
      const token = await getAuthToken();

      if (!token) {
        setSavedRoutes([]);
        return;
      }

      try {
        const session = userSession ?? (await fetchUserSession(token));

        const response = await fetch(buildApiUrl("api", "paths", session.id), {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (!response.ok) {
          throw new Error(`Saved routes request failed: ${response.status}`);
        }

        const routes = (await response.json()) as SavedRouteResponse[];

        if (!isActive) return;

        setSavedRoutes(
          routes
            .map(normalizeSavedRoute)
            .filter((route): route is SavedAccountRoute => Boolean(route)),
        );
      } catch {
        if (!isActive) return;
        toast.error("Shranjene poti niso bile naložene.");
      }
    }

    void fetchSavedRoutes();

    return () => {
      isActive = false;
    };
  }, [fetchUserSession, getAuthToken, userSession]);

  // handlanje overlay kontrol
  function handleZoomIn() {
    setZoom((currentZoom) => Math.min(currentZoom + 1, 20));
  }

  function handleZoomOut() {
    setZoom((currentZoom) => Math.max(currentZoom - 1, 3));
  }

  function handleLocate() {
    if (isFollowingRoute) return;

    locateUser({ zoomToUser: true, showOutOfCoverageToast: true });
  }

  function handleCameraChanged(nextCenter: MapCenter, nextZoom: number) {
    setCenter(nextCenter);
    setZoom(nextZoom);
  }

  function handleLegClick(leg: RouteLeg, position: MapPoint) {
    setMapLocationDraft(null);
    setSelectedLeg({ leg, position, source: "path" });
  }

  function handleBusIconClick(
    leg: RouteLeg,
    position: MapPoint,
    previousLeg?: RouteLeg,
  ) {
    setMapLocationDraft(null);
    setSelectedLeg({ leg, position, previousLeg, source: "busIcon" });
  }

  function handleBikeIconClick(
    leg: RouteLeg,
    position: MapPoint,
    source: "bikePickupIcon" | "bikeReturnIcon",
  ) {
    setMapLocationDraft(null);
    setSelectedLeg({ leg, position, source });
  }

  function handlePlaceSelect(place: { lat: number; lng: number } | null) {
    setRoutePath(null);
    setIsShowingSavedRoute(false);
    setIsFollowingRoute(false);
    setRouteComputeError(null);
    setSelectedLeg(null);
    setMapLocationDraft(null);

    if (!place) {
      setMarkerPosition(null);
      return;
    }

    setCenter({ lat: place.lat, lng: place.lng });
    setZoom(16);
    setMarkerPosition({ lat: place.lat, lng: place.lng });
  }

  function handleDestinationSelect(place: { lat: number; lng: number } | null) {
    setRoutePath(null);
    setIsShowingSavedRoute(false);
    setIsFollowingRoute(false);
    setRouteComputeError(null);
    setSelectedLeg(null);
    setMapLocationDraft(null);
    setDestinationMarkerPosition(place);
  }

  function handlePathReceive(path: RoutePath) {
    setRoutePath(path);
    setIsShowingSavedRoute(false);
    setIsFollowingRoute(false);
    setRouteComputeError(null);
    setSelectedLeg(null);
    setMapLocationDraft(null);
  }

  function handleSavedRouteSelect(route: SavedAccountRoute) {
    const endpoints = getRouteEndpoints(route.journey);

    setRoutePath(route.journey);
    setIsShowingSavedRoute(true);
    setIsFollowingRoute(false);
    setRouteComputeError(null);
    setSelectedLeg(null);
    setMapLocationDraft(null);
    setMarkerPosition(endpoints.origin);
    setDestinationMarkerPosition(endpoints.destination);
    setRouteFitBoundsTrigger((currentTrigger) => currentTrigger + 1);
  }

  function handleStartRouteFollowing() {
    if (userLocationPosition) {
      setCenter(userLocationPosition);
      setZoom((currentZoom) => Math.max(currentZoom, 17));
    }

    setIsFollowingRoute(true);
  }

  function handlePathError(error: RouteComputeError) {
    setRoutePath(null);
    setIsShowingSavedRoute(false);
    setIsFollowingRoute(false);
    setRouteComputeError(error);
    setSelectedLeg(null);
    setMapLocationDraft(null);
  }

  function handleEndRouteFollowing() {
    setIsFollowingRoute(false);
    setSelectedLeg(null);
    setMapLocationDraft(null);
    setRouteFitBoundsTrigger((currentTrigger) => currentTrigger + 1);
  }

  function handleMapContextSelect(position: MapCenter) {
    setSelectedLeg(null);
    setCenter(position);
    setZoom((currentZoom) => Math.max(currentZoom, 16));
    setMapLocationDraft({
      position,
      name: "",
      color: defaultLocationColor,
      icon: defaultLocationIcon,
    });
  }

  function handleMapLocationColorChange(color: string) {
    setMapLocationDraft((currentDraft) =>
      currentDraft ? { ...currentDraft, color } : currentDraft,
    );
  }

  function handleMapLocationIconChange(icon: LocationIcon) {
    setMapLocationDraft((currentDraft) =>
      currentDraft ? { ...currentDraft, icon } : currentDraft,
    );
  }

  async function handleMapLocationSave(draft: MapLocationDraft) {
    try {
      const token = await getAuthToken();

      if (!token) {
        toast.error("Za shranjevanje lokacije moraš biti prijavljen.");
        return;
      }

      const session = userSession ?? (await fetchUserSession(token));
      const locationName = draft.name.trim();

      const data = {
        userId: session.id,
        name: locationName,
        address: locationName,
        latitude: draft.position.lat,
        longitude: draft.position.lng,
        color: draft.color,
        icon: draft.icon,
        logo: draft.icon,
      };

      const response = await fetch(buildApiUrl("api", "locations"), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(data),
      });

      if (!response.ok) {
        throw new Error(response.statusText);
      }

      const savedLocation = (await response.json()) as SavedLocationResponse;

      setSavedLocations((currentLocations) => [
        ...currentLocations,
        {
          id: parseUuid(savedLocation.id, "location.id"),
          name: savedLocation.name,
          position: {
            lat: savedLocation.latitude,
            lng: savedLocation.longitude,
          },
          color: draft.color,
          icon: isLocationIcon(savedLocation.logo)
            ? savedLocation.logo
            : draft.icon,
        },
      ]);

      toast.success("Lokacija je shranjena.");
      setMapLocationDraft(null);
    } catch {
      toast.error("Lokacije ni bilo mogoče shraniti. Poskusite znova.");
    }
  }

  async function handleSavedLocationDelete(locationId: string) {
    if (deletingSavedLocationId === locationId) return;

    setDeletingSavedLocationId(locationId);

    try {
      const token = await getAuthToken();

      if (!token) {
        toast.error("Za brisanje lokacije moraš biti prijavljen.");
        return;
      }

      const response = await fetch(
        buildApiUrl("api", "locations", parseUuid(locationId, "location.id")),
        {
          method: "DELETE",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        },
      );

      if (!response.ok) {
        throw new Error(`Delete location request failed: ${response.status}`);
      }

      setSavedLocations((currentLocations) =>
        currentLocations.filter((location) => location.id !== locationId),
      );
      toast.success("Lokacija je izbrisana.");
    } catch {
      toast.error("Lokacije ni bilo mogoče izbrisati.");
    } finally {
      setDeletingSavedLocationId(null);
    }
  }

  async function handleRouteSave(name: string) {
    try {
      const token = await getAuthToken();

      if (!token) {
        toast.error("Za shranjevanje poti moraš biti prijavljen.");
        return;
      }

      if (!routePath) {
        toast.error("Najprej izračunaj pot.");
        return;
      }

      const session = userSession ?? (await fetchUserSession(token));

      const response = await fetch(buildApiUrl("api", "paths"), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          userId: session.id,
          name,
          journey: routePath,
        }),
      });

      if (!response.ok) {
        // console.log("Route failed to save: ", await response.text());
        throw new Error(`Save route request failed: ${response.status}`);
      }

      const savedRoute = normalizeSavedRoute(
        (await response.json()) as SavedRouteResponse,
      );
      if (savedRoute) {
        setSavedRoutes((currentRoutes) => [...currentRoutes, savedRoute]);
      }

      toast.success("Pot je shranjena.");
    } catch {
      toast.error("Poti ni bilo mogoče shraniti. Poskusite znova.");
    }
  }

  return (
    <main className='relative min-h-screen overflow-hidden'>
      {/* map */}
      <MainMap
        center={center}
        zoom={zoom}
        legs={routePath?.legs}
        selectedLeg={selectedLeg}
        onLegClick={handleLegClick}
        onBusIconClick={handleBusIconClick}
        onBikeIconClick={handleBikeIconClick}
        onRoutePopupClose={() => setSelectedLeg(null)}
        onCameraChanged={handleCameraChanged}
        onMapContextSelect={handleMapContextSelect}
        mapLocationDraft={mapLocationDraft}
        onMapLocationColorChange={handleMapLocationColorChange}
        onMapLocationIconChange={handleMapLocationIconChange}
        onMapLocationSave={handleMapLocationSave}
        onMapLocationPopupClose={() => setMapLocationDraft(null)}
        savedLocations={savedLocations}
        deletingSavedLocationId={deletingSavedLocationId}
        onSavedLocationDelete={handleSavedLocationDelete}
        markerPosition={markerPosition}
        userLocationPosition={displayedUserLocationPosition}
        destinationMarkerPosition={destinationMarkerPosition}
        routeFitBoundsTrigger={routeFitBoundsTrigger}
      />

      {/* control overlay */}
      <MainAppControlOverlay
        onZoomIn={handleZoomIn}
        onZoomOut={handleZoomOut}
        onLocate={handleLocate}
        currentLocation={userLocationPosition}
        onPlaceSelect={handlePlaceSelect}
        onDestinationSelect={handleDestinationSelect}
        onPathReceive={handlePathReceive}
        onPathError={handlePathError}
        hasRoute={Boolean(routePath)}
        isRouteActive={isFollowingRoute}
        onStartRoute={handleStartRouteFollowing}
        onEndRoute={handleEndRouteFollowing}
        savedLocations={savedLocations}
        savedRoutes={savedRoutes}
        onSavedRouteSelect={handleSavedRouteSelect}
      />

      {activeRouteStep && (
        <div className='pointer-events-none fixed inset-x-4 bottom-16 z-[25] flex justify-center'>
          <div className='pointer-events-auto w-full max-w-md rounded-lg border border-border bg-card/95 px-4 py-3 text-card-foreground shadow-2xl backdrop-blur-sm dark:border-neutral-600 dark:bg-neutral-800/95 dark:text-white'>
            <div className='flex items-center gap-2'>
              <span className='rounded-full bg-red-700 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide text-white'>
                {getModeLabel(activeRouteStep.mode)}
              </span>
              <span className='text-xs font-medium text-muted-foreground dark:text-neutral-300'>
                Aktualni korak
              </span>
            </div>
            <p className='mt-2 text-sm font-semibold leading-snug'>
              {activeRouteStep.instruction}
            </p>
          </div>
        </div>
      )}

      {/* route options */}
      <RouteOptions
        routes={routeOptions}
        legs={routePath?.legs}
        computeError={routeComputeError}
        canSaveRoute={Boolean(routePath)}
        hasFetchedRoute={Boolean(routePath)}
        isSavedRoute={isShowingSavedRoute}
        activeStepIndex={activeRouteStep?.stepIndex ?? null}
        onSaveRoute={handleRouteSave}
      />
    </main>
  );
};
