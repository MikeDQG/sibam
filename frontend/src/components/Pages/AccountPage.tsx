import { AdvancedMarker, APIProvider, Map } from "@vis.gl/react-google-maps";
import { onAuthStateChanged } from "firebase/auth";
import { ArrowLeft } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { toast } from "sonner";
import { auth } from "../../firebase";
import {
  isLocationIcon,
  LocationIconGlyph,
  type LocationIcon,
} from "../MainAppComponents/MapLocationPopup";
import { useTheme } from "../ThemeProvider";
import { useUserSession } from "../UserSessionProvider";

const apiUrl = import.meta.env.VITE_API_URL;
const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
const mapId = import.meta.env.VITE_GOOGLE_MAPS_MAP_ID ?? "DEMO_MAP_ID";

const defaultLocationColor = "#b91c1c";
const defaultLocationIcon: LocationIcon = "home";

type MapCenter = {
  lat: number;
  lng: number;
};

type SavedLocationResponse = {
  id: string;
  name?: string | null;
  latitude?: number | string | null;
  longitude?: number | string | null;
  color?: string | null;
  icon?: string | null;
};

type SavedAccountLocation = {
  id: string;
  name: string;
  position: MapCenter;
  color: string;
  icon: LocationIcon;
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
    id: location.id,
    name: location.name?.trim() || "Shranjena lokacija",
    position: { lat, lng },
    color: location.color ?? defaultLocationColor,
    icon: isLocationIcon(location.icon) ? location.icon : defaultLocationIcon,
  };
}

function SavedLocationMapCard({ location }: { location: SavedAccountLocation }) {
  const { theme } = useTheme();
  const [zoom, setZoom] = useState(13);

  return (
    <article
      className='aspect-square overflow-hidden rounded-lg border border-border bg-muted shadow-sm dark:border-neutral-600 dark:bg-neutral-800'
      aria-label={location.name}>
      <Map
        center={location.position}
        zoom={zoom}
        onCameraChanged={(event) => {
          setZoom(event.detail.zoom);
        }}
        colorScheme={theme === "dark" ? "DARK" : "LIGHT"}
        gestureHandling='greedy'
        draggable={false}
        scrollwheel
        zoomControl
        minZoom={10}
        maxZoom={19}
        disableDefaultUI
        keyboardShortcuts={false}
        clickableIcons={false}
        mapId={mapId}
        reuseMaps>
        <AdvancedMarker
          position={location.position}
          anchorLeft='-50%'
          anchorTop='-22px'>
          <div className='flex flex-col items-center gap-1'>
            <div
              className='flex h-11 w-11 items-center justify-center rounded-full border-2 border-white text-white shadow-lg'
              style={{ backgroundColor: location.color }}>
              <LocationIconGlyph icon={location.icon} size={23} />
            </div>
            <span className='max-w-32 rounded-md bg-white/95 px-2 py-1 text-center text-xs font-semibold leading-tight text-neutral-900 shadow-md dark:bg-neutral-700/95 dark:text-white'>
              {location.name}
            </span>
          </div>
        </AdvancedMarker>
      </Map>
    </article>
  );
}

export const AccountPage = () => {
  const [email, setEmail] = useState<string | null>(null);
  const navigate = useNavigate();
  const [fullName, setFullName] = useState<string | null>(null);
  const [savedLocations, setSavedLocations] = useState<SavedAccountLocation[]>(
    [],
  );
  const [isLoadingLocations, setIsLoadingLocations] = useState(true);
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

        const response = await fetch(`${apiUrl}/api/locations/${session.id}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

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
      } catch (error) {
        if (!isActive) return;
        console.error("Napaka pri pridobivanju shranjenih lokacij:", error);
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

  return (
    <div className="flex min-h-screen w-full flex-col items-center bg-[url('/LandingPage/background.jpeg')] bg-cover bg-center px-6 pt-25">
      <img
        src='/logo.svg'
        className='absolute left-9 top-6 h-15 w-auto cursor-pointer'
        alt='Logo'
        onClick={() => navigate("/")}
      />
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
                    />
                  ))}
                </div>
              </APIProvider>
            ) : savedLocations.length > 0 ? (
              <div className='grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4'>
                {savedLocations.map((location) => (
                  <div
                    key={location.id}
                    className='flex aspect-square flex-col items-center justify-center gap-2 rounded-lg border border-border bg-muted p-4 text-center dark:border-neutral-600 dark:bg-neutral-800'>
                    <div
                      className='flex h-11 w-11 items-center justify-center rounded-full border-2 border-white text-white shadow-lg'
                      style={{ backgroundColor: location.color }}>
                      <LocationIconGlyph icon={location.icon} size={23} />
                    </div>
                    <span className='text-sm font-semibold'>
                      {location.name}
                    </span>
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
          <h2 className='mb-4 text-xl font-semibold'>Zadnje poti</h2>
          <div className='flex flex-col gap-3'>
            <div className='rounded-md bg-muted p-4 text-center text-sm text-muted-foreground dark:bg-neutral-600 dark:text-neutral-400'>
              Ni še zadnjih poti.
            </div>
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
