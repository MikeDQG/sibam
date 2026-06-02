import {
  APIProvider,
  AdvancedMarker,
  InfoWindow,
  Map,
  useMap,
} from "@vis.gl/react-google-maps";
import { Trash } from "lucide-react";
import { Fragment, useEffect, useState } from "react";
import { useTheme } from "../ThemeProvider";
import {
  LocationIconGlyph,
  MapLocationPopup,
  type LocationIcon,
  type MapLocationDraft,
} from "./MapLocationPopup";
import { RoutePopup, type RoutePopupSelection } from "./RoutePopup";
import { RoutePolyline, type MapPoint, type RouteLeg } from "./RoutePolyline";

const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
const mapId = import.meta.env.VITE_GOOGLE_MAPS_MAP_ID ?? "DEMO_MAP_ID";

type MapCenter = {
  lat: number;
  lng: number;
};

type MainMapProps = {
  center: MapCenter;
  zoom: number;
  legs?: RouteLeg[];
  selectedLeg?: RoutePopupSelection | null;
  onLegClick?: (leg: RouteLeg, position: MapPoint) => void;
  onBusIconClick?: (
    leg: RouteLeg,
    position: MapPoint,
    previousLeg?: RouteLeg,
  ) => void;
  onBikeIconClick?: (
    leg: RouteLeg,
    position: MapPoint,
    source: "bikePickupIcon" | "bikeReturnIcon",
  ) => void;
  onRoutePopupClose?: () => void;
  onCameraChanged?: (center: MapCenter, zoom: number) => void;
  onMapContextSelect?: (position: MapCenter) => void;
  mapLocationDraft?: MapLocationDraft | null;
  onMapLocationColorChange?: (color: string) => void;
  onMapLocationIconChange?: (icon: LocationIcon) => void;
  onMapLocationSave: (draft: MapLocationDraft) => void;
  onMapLocationPopupClose?: () => void;
  savedLocations?: SavedMapLocation[];
  deletingSavedLocationId?: string | null;
  onSavedLocationDelete?: (locationId: string) => void;
  markerPosition?: MapCenter | null;
  userLocationPosition?: MapCenter | null;
  destinationMarkerPosition?: MapCenter | null;
  routeFitBoundsTrigger?: number;
};

export type SavedMapLocation = {
  id: string;
  position: MapCenter;
  name: string;
  color: string;
  icon: LocationIcon;
};

function FitBounds({
  origin,
  destination,
}: {
  origin: MapCenter;
  destination: MapCenter;
}) {
  const map = useMap();
  useEffect(() => {
    if (!map) return;
    const bounds = {
      north: Math.max(origin.lat, destination.lat),
      south: Math.min(origin.lat, destination.lat),
      east: Math.max(origin.lng, destination.lng),
      west: Math.min(origin.lng, destination.lng),
    };
    map.fitBounds(bounds, 100);
  }, [map, origin, destination]);

  return null;
}

export const MainMap = ({
  center,
  zoom,
  onCameraChanged,
  markerPosition,
  userLocationPosition,
  destinationMarkerPosition,
  legs,
  selectedLeg,
  onLegClick,
  onBusIconClick,
  onBikeIconClick,
  onRoutePopupClose,
  onMapContextSelect,
  mapLocationDraft,
  onMapLocationColorChange,
  onMapLocationIconChange,
  onMapLocationSave,
  onMapLocationPopupClose,
  savedLocations = [],
  deletingSavedLocationId,
  onSavedLocationDelete,
  routeFitBoundsTrigger,
}: MainMapProps) => {
  const hasApiKey = apiKey && apiKey !== "your_google_maps_api_key";
  const { theme } = useTheme();
  const [deletePromptLocationId, setDeletePromptLocationId] = useState<
    string | null
  >(null);
  const deletePromptLocation =
    savedLocations.find((location) => location.id === deletePromptLocationId) ??
    null;

  if (!hasApiKey) {
    return (
      <div className='absolute inset-0 z-0 flex items-center justify-center bg-muted text-muted-foreground'>
        Manjka Google Maps API key
      </div>
    );
  }

  return (
    <div className='absolute inset-0 z-0'>
      <APIProvider apiKey={apiKey} region='SI' language='sl'>
        <Map
          center={center}
          zoom={zoom}
          onCameraChanged={(event) => {
            onCameraChanged?.(event.detail.center, event.detail.zoom);
          }}
          onContextmenu={(event) => {
            event.stop();

            const position = event.detail.latLng;
            if (!position) return;

            onRoutePopupClose?.();
            onMapContextSelect?.(position);
          }}
          colorScheme={theme === "dark" ? "DARK" : "LIGHT"}
          gestureHandling='greedy'
          draggable
          scrollwheel
          keyboardShortcuts
          disableDefaultUI
          zoomControl={false}
          clickableIcons={false}
          mapId={mapId}
          reuseMaps>
          {userLocationPosition && (
            <AdvancedMarker
              position={userLocationPosition}
              anchorLeft='-50%'
              anchorTop='-50%'>
              <div className='relative flex h-8 w-8 items-center justify-center'>
                <div className='absolute h-8 w-8 rounded-full bg-blue-500/20' />
                <div className='h-4 w-4 rounded-full border-2 border-white bg-blue-600 shadow-lg' />
              </div>
            </AdvancedMarker>
          )}
          {/* zacetek in konec poti */}
          {markerPosition && <AdvancedMarker position={markerPosition} />}
          {destinationMarkerPosition && (
            <AdvancedMarker position={destinationMarkerPosition} />
          )}
          {markerPosition && destinationMarkerPosition && (
            <FitBounds
              origin={markerPosition}
              destination={destinationMarkerPosition}
            />
          )}
          {legs && (
            <RoutePolyline
              legs={legs}
              fitBoundsTrigger={routeFitBoundsTrigger}
              onLegClick={onLegClick}
            />
          )}
          {legs?.map((leg, index) => {
            const firstPoint = leg.polyline[0];
            if (!firstPoint || leg.mode === "WALK") return null;

            const nextLeg = legs[index + 1];
            const lastPoint = leg.polyline.at(-1);
            const showBikeReturnMarker =
              leg.mode === "BIKE" && nextLeg?.mode !== "BUS" && lastPoint;
            const markerPosition = {
              lat: firstPoint.lat,
              lng: firstPoint.lon,
            };

            return (
              <Fragment key={index}>
                <AdvancedMarker
                  position={markerPosition}
                  clickable={true}
                  anchorLeft='-50%'
                  anchorTop='-50%'
                  onClick={() => {
                    if (leg.mode === "BIKE") {
                      onBikeIconClick?.(
                        leg,
                        {
                          lat: firstPoint.lat - 0.00002,
                          lng: firstPoint.lon,
                        },
                        "bikePickupIcon",
                      );
                    } else {
                      onBusIconClick?.(
                        leg,
                        {
                          lat: firstPoint.lat - 0.00002,
                          lng: firstPoint.lon,
                        },
                        legs[index - 1],
                      );
                    }
                  }}>
                  <img
                    src={
                      leg.mode === "BIKE"
                        ? "pathIcons/mBajk.png"
                        : "pathIcons/marprom.png"
                    }
                    alt={
                      leg.mode === "BIKE"
                        ? "Bajk postaja za prevzem"
                        : "Avtobusna postaja"
                    }
                    className='h-7 w-7 rounded-full'
                  />
                </AdvancedMarker>

                {showBikeReturnMarker && (
                  <AdvancedMarker
                    position={{
                      lat: lastPoint.lat,
                      lng: lastPoint.lon,
                    }}
                    clickable={true}
                    anchorLeft='-50%'
                    anchorTop='-50%'
                    onClick={() => {
                      onBikeIconClick?.(
                        leg,
                        {
                          lat: lastPoint.lat - 0.00002,
                          lng: lastPoint.lon,
                        },
                        "bikeReturnIcon",
                      );
                    }}>
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
          {selectedLeg && (
            <RoutePopup selectedLeg={selectedLeg} onClose={onRoutePopupClose} />
          )}
          {savedLocations.map((location) => (
            <AdvancedMarker
              key={location.id}
              position={location.position}
              clickable={Boolean(onSavedLocationDelete)}
              anchorLeft='-50%'
              anchorTop='-20px'
              onClick={() => {
                if (!onSavedLocationDelete) return;
                onRoutePopupClose?.();
                onMapLocationPopupClose?.();
                setDeletePromptLocationId(location.id);
              }}>
              <div className='flex flex-col items-center gap-1'>
                <div
                  className='group flex h-10 w-10 cursor-pointer items-center justify-center rounded-full border-2 border-white text-white shadow-lg'
                  style={{ backgroundColor: location.color }}>
                  <LocationIconGlyph
                    icon={location.icon}
                    size={22}
                    className='group-hover:hidden'
                  />
                  <Trash className='hidden group-hover:block' size={20} />
                </div>
                <span className='max-w-28 rounded-md bg-white/95 px-2 py-0.5 text-center text-xs font-semibold leading-tight text-neutral-900 shadow-md dark:bg-neutral-700/95 dark:text-white'>
                  {location.name}
                </span>
              </div>
            </AdvancedMarker>
          ))}
          {deletePromptLocation && (
            <InfoWindow
              position={deletePromptLocation.position}
              onCloseClick={() => setDeletePromptLocationId(null)}
              className='map-location-info-window'
              shouldFocus={false}
              pixelOffset={[0, -8]}>
              <div className='w-56 text-foreground'>
                <p className='text-sm font-semibold leading-tight'>
                  Izbriši shranjeno lokacijo?
                </p>
                <p className='mt-3 text-xs leading-snug text-foreground-muted'>
                  Lokacija "{deletePromptLocation.name}" bo odstranjena iz
                  tvojih shranjenih lokacij.
                </p>
                <div className='mt-3 flex justify-end gap-2'>
                  <button
                    type='button'
                    onClick={() => setDeletePromptLocationId(null)}
                    className='rounded-md cursor-pointer bg-background/40 px-3 py-1.5 text-xs font-medium text-foreground-muted hover:bg-background/20'>
                    Prekliči
                  </button>
                  <button
                    type='button'
                    disabled={
                      deletingSavedLocationId === deletePromptLocation.id
                    }
                    onClick={() => {
                      onSavedLocationDelete?.(deletePromptLocation.id);
                      setDeletePromptLocationId(null);
                    }}
                    className='rounded-md cursor-pointer bg-red-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60'>
                    Izbriši
                  </button>
                </div>
              </div>
            </InfoWindow>
          )}
          {mapLocationDraft && (
            <InfoWindow
              position={mapLocationDraft.position}
              className='map-location-info-window'
              onCloseClick={onMapLocationPopupClose}
              shouldFocus={false}
              pixelOffset={[0, -8]}>
              <MapLocationPopup
                draft={mapLocationDraft}
                onColorChange={onMapLocationColorChange}
                onIconChange={onMapLocationIconChange}
                onSave={onMapLocationSave}
              />
            </InfoWindow>
          )}
        </Map>
      </APIProvider>
    </div>
  );
};
