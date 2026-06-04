import { APIProvider } from "@vis.gl/react-google-maps";
import { onAuthStateChanged } from "firebase/auth";
import { ArrowLeft, Trash } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { toast } from "sonner";
import { auth } from "../../../firebase";
import type { RoutePath } from "../../MainAppComponents/RoutePolyline";
import {
  isLocationIcon,
  LocationIconGlyph,
  type LocationIcon,
} from "../../MainAppComponents/MapLocationPopup";
import { useUserSession } from "../../Authorization/UserSessionProvider";
import {
  SavedLocationMapCard,
  type SavedAccountLocation,
} from "./SavedLocationMapCard";
import {
  SavedRouteMapCard,
  type SavedAccountRoute,
} from "./SavedRouteMapCard";
import { buildApiUrl, parseUuid } from "../../../lib/api";

const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;

const defaultLocationColor = "#b91c1c";
const defaultLocationIcon: LocationIcon = "home";

type SavedLocationResponse = {
  id: unknown;
  name?: string | null;
  latitude?: number | string | null;
  longitude?: number | string | null;
  color?: string | null;
  icon?: string | null;
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

function toCoordinate(value: SavedLocationResponse["latitude"]) {
  const coordinate = typeof value === "number" ? value : Number(value);
  return Number.isFinite(coordinate) ? coordinate : null;
}

function normalizeSavedLocation(
  location: SavedLocationResponse,
): SavedAccountLocation | null {
  const lat = toCoordinate(location.latitude);
  const lng = toCoordinate(location.longitude);

  if (lat === null || lng === null) return null;

  return {
    id: parseUuid(location.id, "location.id"),
    name: location.name?.trim() || "Shranjena lokacija",
    position: { lat, lng },
    color: location.color ?? defaultLocationColor,
    icon: isLocationIcon(location.icon) ? location.icon : defaultLocationIcon,
  };
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
    duration: journey?.duration,
    distance: journey?.distance,
    originLabel: journey?.origin_address ?? journey?.originAddress,
    destinationLabel:
      journey?.destination_address ?? journey?.destinationAddress,
    modes: Array.from(
      new Set(
        (journey?.legs ?? [])
          .map((leg) => leg.mode?.trim())
          .filter((mode): mode is string => Boolean(mode)),
      ),
    ),
    createdAt: route.createdAt,
  };
}

export const AccountPage = () => {
  const [email, setEmail] = useState<string | null>(null);
  const navigate = useNavigate();
  const [fullName, setFullName] = useState<string | null>(null);
  const [savedLocations, setSavedLocations] = useState<SavedAccountLocation[]>(
    [],
  );
  const [isLoadingLocations, setIsLoadingLocations] = useState(true);
  const [deletingLocationIds, setDeletingLocationIds] = useState<Set<string>>(
    () => new Set(),
  );
  const [savedRoutes, setSavedRoutes] = useState<SavedAccountRoute[]>([]);
  const [isLoadingRoutes, setIsLoadingRoutes] = useState(true);
  const [deletingRouteIds, setDeletingRouteIds] = useState<Set<string>>(
    () => new Set(),
  );
  const { userSession, getAuthToken, fetchUserSession } = useUserSession();
  const hasApiKey = apiKey && apiKey !== "your_google_maps_api_key";

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      if (user) {
        setEmail(user.email);
        setFullName(user.displayName?.split(" ")[0] ?? null);
      } else {
        navigate("/login");
      }
    });
    return () => unsubscribe();
  }, [navigate]);

  useEffect(() => {
    let isActive = true;

    async function fetchSavedLocations() {
      setIsLoadingLocations(true);

      try {
        const token = await getAuthToken();

        if (!token) {
          if (isActive) {
            setSavedLocations([]);
            setIsLoadingLocations(false);
          }
          return;
        }

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
            .map(normalizeSavedLocation)
            .filter((location): location is SavedAccountLocation =>
              Boolean(location),
            ),
        );
      } catch {
        if (!isActive) return;
        setSavedLocations([]);
        toast.error("Shranjene lokacije niso bile naložene.");
      } finally {
        if (isActive) {
          setIsLoadingLocations(false);
        }
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
      setIsLoadingRoutes(true);

      try {
        const token = await getAuthToken();

        if (!token) {
          if (isActive) {
            setSavedRoutes([]);
            setIsLoadingRoutes(false);
          }
          return;
        }

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
        setSavedRoutes([]);
        toast.error("Shranjene poti niso bile naložene.");
      } finally {
        if (isActive) {
          setIsLoadingRoutes(false);
        }
      }
    }

    void fetchSavedRoutes();

    return () => {
      isActive = false;
    };
  }, [fetchUserSession, getAuthToken, userSession]);

  async function handleDeleteLocation(locationId: string) {
    if (deletingLocationIds.has(locationId)) return;

    setDeletingLocationIds((currentIds) => new Set(currentIds).add(locationId));

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
      setDeletingLocationIds((currentIds) => {
        const nextIds = new Set(currentIds);
        nextIds.delete(locationId);
        return nextIds;
      });
    }
  }

  async function handleDeleteRoute(routeId: string) {
    if (deletingRouteIds.has(routeId)) return;

    setDeletingRouteIds((currentIds) => new Set(currentIds).add(routeId));

    try {
      const token = await getAuthToken();

      if (!token) {
        toast.error("Za brisanje poti moraš biti prijavljen.");
        return;
      }

      const response = await fetch(
        buildApiUrl("api", "paths", parseUuid(routeId, "route.id")),
        {
          method: "DELETE",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        },
      );

      if (!response.ok) {
        throw new Error(`Delete route request failed: ${response.status}`);
      }

      setSavedRoutes((currentRoutes) =>
        currentRoutes.filter((route) => route.id !== routeId),
      );
      toast.success("Pot je izbrisana.");
    } catch {
      toast.error("Poti ni bilo mogoče izbrisati.");
    } finally {
      setDeletingRouteIds((currentIds) => {
        const nextIds = new Set(currentIds);
        nextIds.delete(routeId);
        return nextIds;
      });
    }
  }

  return (
    <div className="flex min-h-screen w-full flex-col items-center bg-[url('/LandingPage/background_light.jpg')] bg-cover bg-center px-6 pt-25 dark:bg-[url('/LandingPage/background.jpeg')]">
      <div className='flex w-full max-w-[80vw] flex-col gap-6 rounded-lg bg-card/95 p-10 text-card-foreground shadow-xl backdrop-blur-sm dark:bg-neutral-700/95'>
        <button
          type='button'
          onClick={() => navigate("/home")}
          className='flex w-fit cursor-pointer items-center gap-2 text-sm font-medium text-foreground transition-colors hover:text-muted-foreground dark:text-white dark:hover:text-neutral-300'>
          <ArrowLeft size={18} />
          Domov
        </button>
        <h1 className='text-3xl font-semibold'>
          Zdravo{fullName ? `, ${fullName}` : ""}!
        </h1>
        <p className='text-sm text-muted-foreground'>
          Prijavljen si z emailom{" "}
          <span className='font-medium text-foreground dark:text-white'>
            {email}
          </span>
        </p>

        <div className='border-t border-border pt-4 dark:border-neutral-600'>
          <h2 className='mb-4 text-xl font-semibold'>Shranjene lokacije</h2>
          <div>
            {isLoadingLocations ? (
              <div className='rounded-md bg-muted p-4 text-center text-sm text-muted-foreground dark:bg-neutral-600 dark:text-neutral-400'>
                Nalaganje shranjenih lokacij ...
              </div>
            ) : savedLocations.length > 0 && hasApiKey ? (
              <APIProvider apiKey={apiKey} region='SI' language='sl'>
                <div className='grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4'>
                  {savedLocations.map((location) => (
                    <SavedLocationMapCard
                      key={location.id}
                      location={location}
                      isDeleting={deletingLocationIds.has(location.id)}
                      onDelete={handleDeleteLocation}
                    />
                  ))}
                </div>
              </APIProvider>
            ) : savedLocations.length > 0 ? (
              <div className='grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4'>
                {savedLocations.map((location) => (
                  <div
                    key={location.id}
                    className='group mb-0 overflow-hidden rounded-lg border border-border bg-muted text-center transition-[margin] duration-200 sm:mb-11 sm:hover:mb-0 dark:border-neutral-600 dark:bg-neutral-800'>
                    <div className='flex aspect-square flex-col items-center justify-center gap-2 rounded-t-lg rounded-b-none p-4 transition-[border-radius] duration-200 sm:rounded-lg sm:group-hover:rounded-b-none'>
                      <div
                        className='flex h-11 w-11 items-center justify-center rounded-full border-2 border-white text-white shadow-lg'
                        style={{ backgroundColor: location.color }}>
                        <LocationIconGlyph icon={location.icon} size={23} />
                      </div>
                      <span className='text-sm font-semibold'>
                        {location.name}
                      </span>
                    </div>
                    <button
                      type='button'
                      disabled={deletingLocationIds.has(location.id)}
                      onClick={() => handleDeleteLocation(location.id)}
                      className='flex h-11 max-h-11 w-full cursor-pointer items-center justify-center overflow-hidden rounded-b-lg bg-card/95 text-foreground transition-[max-height,color] duration-200 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-60 sm:max-h-0 sm:group-hover:max-h-11 dark:bg-neutral-800/95 dark:text-white dark:hover:text-red-600'
                      aria-label={`Izbriši lokacijo ${location.name}`}>
                      <Trash size={20} />
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <div className='rounded-md bg-muted p-4 text-center text-sm text-muted-foreground dark:bg-neutral-600 dark:text-neutral-400'>
                Ni še shranjenih lokacij.
              </div>
            )}
          </div>
        </div>

        <div className='border-t border-border pt-4 dark:border-neutral-600'>
          <h2 className='mb-4 text-xl font-semibold'>Shranjene poti</h2>
          <div>
            {isLoadingRoutes ? (
              <div className='rounded-md bg-muted p-4 text-center text-sm text-muted-foreground dark:bg-neutral-600 dark:text-neutral-400'>
                Nalaganje shranjenih poti ...
              </div>
            ) : savedRoutes.length > 0 && hasApiKey ? (
              <APIProvider apiKey={apiKey} region='SI' language='sl'>
                <div className='grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4'>
                  {savedRoutes.map((route) => (
                    <SavedRouteMapCard
                      key={route.id}
                      route={route}
                      isDeleting={deletingRouteIds.has(route.id)}
                      onDelete={handleDeleteRoute}
                    />
                  ))}
                </div>
              </APIProvider>
            ) : savedRoutes.length > 0 ? (
              <div className='rounded-md bg-muted p-4 text-center text-sm text-muted-foreground dark:bg-neutral-600 dark:text-neutral-400'>
                Za prikaz shranjenih poti manjka Google Maps API key.
              </div>
            ) : (
              <div className='rounded-md bg-muted p-4 text-center text-sm text-muted-foreground dark:bg-neutral-600 dark:text-neutral-400'>
                Ni še shranjenih poti.
              </div>
            )}
          </div>
        </div>

        <button
          onClick={() => {
            auth.signOut();
            navigate("/login");
          }}
          className='h-10 w-fit cursor-pointer flex items-center justify-center rounded-lg bg-red-700 px-7 py-3 text-lg font-semibold text-white transition-colors hover:bg-red-800'>
          Odjava
        </button>
      </div>
    </div>
  );
};
