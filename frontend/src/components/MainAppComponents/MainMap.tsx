import {
  APIProvider,
  AdvancedMarker,
  InfoWindow,
  Map,
  useMap,
} from "@vis.gl/react-google-maps";
import { Fragment, useEffect } from "react";
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
  markerPosition?: MapCenter | null;
  destinationMarkerPosition?: MapCenter | null;
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
}: MainMapProps) => {
  const hasApiKey = apiKey && apiKey !== "your_google_maps_api_key";
  const { theme } = useTheme();

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
          {/* zacetek in konec poti */}
          {markerPosition && (
            <AdvancedMarker position={markerPosition}>
              <div
                style={{
                  width: 24,
                  height: 24,
                  borderRadius: "50%",
                  backgroundColor: "#3b82f6",
                  border: "3px solid white",
                  boxShadow: "0 5px 30px rgb(114, 114, 114)",
                }}
              />
            </AdvancedMarker>
          )}
          {destinationMarkerPosition && (
            <AdvancedMarker position={destinationMarkerPosition} />
          )}
          {markerPosition && destinationMarkerPosition && (
            <FitBounds
              origin={markerPosition}
              destination={destinationMarkerPosition}
            />
          )}
          {legs && <RoutePolyline legs={legs} onLegClick={onLegClick} />}
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
            <AdvancedMarker key={location.id} position={location.position}>
              <div className='flex -translate-y-1 flex-col items-center gap-1'>
                <div
                  className='flex h-10 w-10 items-center justify-center rounded-full border-2 border-white text-white shadow-lg'
                  style={{ backgroundColor: location.color }}>
                  <LocationIconGlyph icon={location.icon} size={22} />
                </div>
                <span className='max-w-28 rounded-md bg-white/95 px-2 py-0.5 text-center text-xs font-semibold leading-tight text-neutral-900 shadow-md dark:bg-neutral-700/95 dark:text-white'>
                  {location.name}
                </span>
              </div>
            </AdvancedMarker>
          ))}
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
