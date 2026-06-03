import { AdvancedMarker, Map } from "@vis.gl/react-google-maps";
import { Trash } from "lucide-react";
import { Fragment } from "react";
import {
  RoutePolyline,
  type RoutePath,
} from "../../MainAppComponents/RoutePolyline";
import { useTheme } from "../../ThemeProvider";

const mapId = import.meta.env.VITE_GOOGLE_MAPS_MAP_ID ?? "DEMO_MAP_ID";

export type SavedAccountRoute = {
  id: string;
  name: string;
  journey: RoutePath;
  duration?: string | null;
  distance?: string | null;
  originLabel?: string | null;
  destinationLabel?: string | null;
  modes: string[];
  createdAt?: string | null;
};

type SavedRouteMapCardProps = {
  route: SavedAccountRoute;
  isDeleting?: boolean;
  onDelete: (routeId: string) => void;
};

type RoutePoint = {
  lat: number;
  lng: number;
};

function formatRouteDate(value?: string | null) {
  if (!value) return null;

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;

  return new Intl.DateTimeFormat("sl-SI", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(date);
}

function formatRouteDuration(value?: string | number | null) {
  const duration = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(duration)) return null;

  return `${Math.round(duration / 60000)} min`;
}

function formatRouteDistance(value?: string | number | null) {
  const distance = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(distance)) return null;

  const meters = Math.round(distance);
  const kilometers = Math.floor(meters / 1000);
  const remainingMeters = meters % 1000;

  if (kilometers <= 0) return `${remainingMeters} m`;
  if (remainingMeters === 0) return `${kilometers} km`;

  return `${kilometers} km ${remainingMeters} m`;
}

function formatRouteMode(mode: string) {
  switch (mode) {
    case "WALK":
      return "PEŠ";
    case "BIKE":
      return "KOLO";
    case "BUS":
      return "AVTOBUS";
    default:
      return mode;
  }
}

function getRouteCenter(journey: RoutePath) {
  const firstPoint = journey.legs
    .flatMap((leg) => leg.polyline)
    .find((point) => Number.isFinite(point.lat) && Number.isFinite(point.lon));

  return {
    lat: firstPoint?.lat ?? 46.5547,
    lng: firstPoint?.lon ?? 15.6459,
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
): RoutePoint | null {
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

  return {
    lat,
    lng,
  };
}

function getRouteEndpoints(journey: RoutePath) {
  const firstLeg = journey.legs[0];
  const lastLeg = journey.legs.at(-1);
  const firstPolylinePoint = firstLeg?.polyline[0];
  const lastPolylinePoint = lastLeg?.polyline.at(-1);

  return {
    origin:
      getJourneyPoint(
        journey.origin as Parameters<typeof getJourneyPoint>[0],
      ) ?? getJourneyPoint(firstPolylinePoint),
    destination:
      getJourneyPoint(
        journey.destination as Parameters<typeof getJourneyPoint>[0],
      ) ?? getJourneyPoint(lastPolylinePoint),
  };
}

function SavedRouteEndpointMarkers({ journey }: { journey: RoutePath }) {
  const { origin, destination } = getRouteEndpoints(journey);

  return (
    <>
      {origin && (
        <AdvancedMarker
          position={origin}
          clickable={false}
          anchorLeft='-50%'
          anchorTop='-100%'
        />
      )}
      {destination && (
        <AdvancedMarker
          position={destination}
          clickable={false}
          anchorLeft='-50%'
          anchorTop='-100%'
        />
      )}
    </>
  );
}

function SavedRouteTransferMarkers({ journey }: { journey: RoutePath }) {
  return (
    <>
      {journey.legs.map((leg, index) => {
        const firstPoint = leg.polyline[0];
        if (!firstPoint || leg.mode === "WALK") return null;

        const nextLeg = journey.legs[index + 1];
        const lastPoint = leg.polyline.at(-1);
        const showBikeReturnMarker =
          leg.mode === "BIKE" && nextLeg?.mode !== "BUS" && lastPoint;
        const iconSrc =
          leg.mode === "BIKE" ? "pathIcons/mBajk.png" : "pathIcons/marprom.png";
        const iconAlt =
          leg.mode === "BIKE" ? "Bajk postaja za prevzem" : "Avtobusna postaja";

        return (
          <Fragment key={index}>
            <AdvancedMarker
              position={{ lat: firstPoint.lat, lng: firstPoint.lon }}
              clickable={false}
              anchorLeft='-50%'
              anchorTop='-50%'>
              <img
                src={iconSrc}
                alt={iconAlt}
                className='h-7 w-7 rounded-full'
              />
            </AdvancedMarker>

            {showBikeReturnMarker && (
              <AdvancedMarker
                position={{ lat: lastPoint.lat, lng: lastPoint.lon }}
                clickable={false}
                anchorLeft='-50%'
                anchorTop='-50%'>
                <img
                  src='pathIcons/mBajk.png'
                  alt='Bajk postaja za oddajo'
                  className='h-7 w-7 rounded-full'
                />
              </AdvancedMarker>
            )}
          </Fragment>
        );
      })}
    </>
  );
}

export function SavedRouteMapCard({
  route,
  isDeleting = false,
  onDelete,
}: SavedRouteMapCardProps) {
  const { theme } = useTheme();
  const createdAt = formatRouteDate(route.createdAt);
  const center = getRouteCenter(route.journey);
  const formattedDuration = formatRouteDuration(route.duration);
  const formattedDistance = formatRouteDistance(route.distance);

  return (
    <article
      className='group mb-0 overflow-hidden rounded-lg border border-border bg-muted text-center shadow-sm transition-[margin] duration-200 sm:mb-11 sm:hover:mb-0 dark:border-neutral-600 dark:bg-neutral-800'
      aria-label={route.name}>
      <div className='bg-card/95 px-4 py-3 text-left text-card-foreground dark:bg-neutral-800/95 dark:text-white'>
        <h3 className='max-h-10 overflow-hidden text-sm font-semibold leading-tight'>
          {route.name}
        </h3>
        {(formattedDuration || formattedDistance) && (
          <p className='mt-0.5 text-xs text-muted-foreground dark:text-neutral-300'>
            {[formattedDuration, formattedDistance].filter(Boolean).join(" • ")}
          </p>
        )}
        {(route.originLabel || route.destinationLabel) && (
          <p className='mt-0.5 max-h-8 overflow-hidden text-xs leading-tight text-muted-foreground dark:text-neutral-300'>
            {[route.originLabel, route.destinationLabel]
              .filter(Boolean)
              .join(" → ")}
          </p>
        )}
        {(route.modes.length > 0 || createdAt) && (
          <p className='mt-1 text-[11px] font-medium text-red-700 dark:text-red-300'>
            {[
              route.modes.map(formatRouteMode).join(" + "),
              createdAt,
            ]
              .filter(Boolean)
              .join(" • ")}
          </p>
        )}
      </div>
      <div className='aspect-square overflow-hidden rounded-b-none transition-[border-radius] duration-200 sm:rounded-b-lg sm:group-hover:rounded-b-none'>
        <Map
          defaultCenter={center}
          defaultZoom={13}
          colorScheme={theme === "dark" ? "DARK" : "LIGHT"}
          gestureHandling='greedy'
          draggable
          scrollwheel
          zoomControl
          minZoom={10}
          maxZoom={19}
          disableDefaultUI
          keyboardShortcuts={false}
          clickableIcons={false}
          mapId={mapId}
          reuseMaps>
          <RoutePolyline legs={route.journey.legs} interactive={false} />
          <SavedRouteEndpointMarkers journey={route.journey} />
          <SavedRouteTransferMarkers journey={route.journey} />
        </Map>
      </div>
      <button
        type='button'
        disabled={isDeleting}
        onClick={() => onDelete(route.id)}
        className='flex h-11 max-h-11 w-full cursor-pointer items-center justify-center overflow-hidden rounded-b-lg bg-card/95 text-foreground transition-[max-height,color] duration-200 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-60 sm:max-h-0 sm:group-hover:max-h-11 dark:bg-neutral-800/95 dark:text-white dark:hover:text-red-600'
        aria-label={`Izbriši pot ${route.name}`}>
        <Trash size={20} />
      </button>
    </article>
  );
}
