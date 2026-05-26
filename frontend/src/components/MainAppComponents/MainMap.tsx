import {
  APIProvider,
  AdvancedMarker,
  InfoWindow,
  Map,
  useMap,
} from "@vis.gl/react-google-maps";
import { Fragment, type ComponentType, useEffect, useState } from "react";
import { useTheme } from "../ThemeProvider";
import { Input } from "../ui/input";
import { RoutePopup, type RoutePopupSelection } from "./RoutePopup";
import { RoutePolyline, type MapPoint, type RouteLeg } from "./RoutePolyline";
import { Button } from "../ui/button";
import { IoIosSchool } from "react-icons/io";
import { FaHome, FaLandmark } from "react-icons/fa";
import { MdWork } from "react-icons/md";

const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
const mapId = import.meta.env.VITE_GOOGLE_MAPS_MAP_ID ?? "DEMO_MAP_ID";

type MapCenter = {
  lat: number;
  lng: number;
};

export type LocationIcon = "school" | "home" | "work" | "landmark";

type MapLocationDraft = {
  position: MapCenter;
  name: string;
  color: string;
  icon: LocationIcon;
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
  onMapLocationSave?: (name: string) => void;
  onMapLocationPopupClose?: () => void;
  markerPosition?: MapCenter | null;
  destinationMarkerPosition?: MapCenter | null;
};

const locationColors = [
  { label: "Rdeča", value: "#b91c1c" },
  { label: "Modra", value: "#2563eb" },
  { label: "Zelena", value: "#15803d" },
  { label: "Rumena", value: "#ca8a04" },
  { label: "Vijolična", value: "#7c3aed" },
];

const locationIcons = [
  { label: "Dom", value: "home", Icon: FaHome },
  { label: "Šola", value: "school", Icon: IoIosSchool },
  { label: "Delo", value: "work", Icon: MdWork },
  { label: "Znamenitost", value: "landmark", Icon: FaLandmark },
] satisfies {
  label: string;
  value: LocationIcon;
  Icon: ComponentType<{ size?: number; className?: string }>;
}[];

type MapLocationPopupProps = {
  draft: MapLocationDraft;
  onColorChange?: (color: string) => void;
  onIconChange?: (icon: LocationIcon) => void;
  onSave?: (name: string) => void;
};

function MapLocationPopup({
  draft,
  onColorChange,
  onIconChange,
  onSave,
}: MapLocationPopupProps) {
  const [name, setName] = useState(draft.name);

  useEffect(() => {
    setName(draft.name);
  }, [draft.position.lat, draft.position.lng, draft.name]);

  return (
    <div className='w-64 mx-2 mb-2 bg-card text-sm text-card-foreground dark:bg-neutral-800 dark:text-white'>
      <strong className='block pr-8 text-base font-semibold'>
        Nova lokacija
      </strong>

      <div className='mt-3 space-y-3'>
        <Input
          autoFocus
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder='Ime lokacije'
          className='h-9'
        />

        <div className='flex items-center gap-2'>
          {locationColors.map((color) => (
            <button
              key={color.value}
              type='button'
              onClick={() => onColorChange?.(color.value)}
              className={`h-7 w-7 rounded-full border-2 transition ${
                draft.color === color.value
                  ? "border-foreground ring-2 ring-ring/40"
                  : "border-white/80 hover:scale-105"
              }`}
              style={{ backgroundColor: color.value }}
              aria-label={color.label}
            />
          ))}
        </div>

        <div className='flex items-center gap-2'>
          {locationIcons.map(({ label, value, Icon }) => (
            <button
              key={value}
              type='button'
              onClick={() => onIconChange?.(value)}
              className={`flex h-9 w-9 items-center justify-center rounded-md border transition ${
                draft.icon === value
                  ? "border-foreground bg-muted text-foreground ring-2 ring-ring/40 dark:bg-neutral-700 dark:text-white"
                  : "border-border text-muted-foreground hover:bg-muted hover:text-foreground dark:border-white/10 dark:hover:bg-neutral-700 dark:hover:text-white"
              }`}
              aria-label={label}
              title={label}>
              <Icon size={20} />
            </button>
          ))}
        </div>
      </div>

      <div className='flex justify-end pt-4'>
        <Button type='button' onClick={() => onSave?.(name)}>
          Shrani
        </Button>
      </div>
    </div>
  );
}

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
          disableDefaultUI
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
          {mapLocationDraft && (
            <InfoWindow
              position={mapLocationDraft.position}
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
