import { AdvancedMarker, Map } from "@vis.gl/react-google-maps";
import { useState } from "react";
import {
  LocationIconGlyph,
  type LocationIcon,
} from "../../MainAppComponents/MapLocationPopup";
import { useTheme } from "../../ThemeProvider";

const mapId = import.meta.env.VITE_GOOGLE_MAPS_MAP_ID ?? "DEMO_MAP_ID";

type MapCenter = {
  lat: number;
  lng: number;
};

export type SavedAccountLocation = {
  id: string;
  name: string;
  position: MapCenter;
  color: string;
  icon: LocationIcon;
};

type SavedLocationMapCardProps = {
  location: SavedAccountLocation;
};

export function SavedLocationMapCard({ location }: SavedLocationMapCardProps) {
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
