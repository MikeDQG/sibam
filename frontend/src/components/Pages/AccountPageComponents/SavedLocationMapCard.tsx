import { AdvancedMarker, Map } from "@vis.gl/react-google-maps";
import { Trash } from "lucide-react";
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
  isDeleting?: boolean;
  onDelete?: (locationId: string) => void;
};

export function SavedLocationMapCard({
  location,
  isDeleting = false,
  onDelete,
}: SavedLocationMapCardProps) {
  const { theme } = useTheme();
  const [zoom, setZoom] = useState(13);

  return (
    <article
      className={`group overflow-hidden rounded-lg border border-border bg-muted shadow-sm transition-[margin] duration-200 dark:border-neutral-600 dark:bg-neutral-800 ${
        onDelete ? "mb-0 sm:mb-11 sm:hover:mb-0" : ""
      }`}
      aria-label={location.name}>
      <div
        className={`aspect-square overflow-hidden transition-[border-radius] duration-200 ${
          onDelete
            ? "rounded-t-lg rounded-b-none sm:rounded-lg sm:group-hover:rounded-b-none"
            : "rounded-lg"
        }`}>
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
                data-location-icon={location.icon}
                style={{ backgroundColor: location.color }}>
                <LocationIconGlyph icon={location.icon} size={23} />
              </div>
              <span className='max-w-32 rounded-md bg-white/95 px-2 py-1 text-center text-xs font-semibold leading-tight text-neutral-900 shadow-md dark:bg-neutral-700/95 dark:text-white'>
                {location.name}
              </span>
            </div>
          </AdvancedMarker>
        </Map>
      </div>
      {onDelete && (
        <button
          type='button'
          disabled={isDeleting}
          onClick={(event) => {
            event.stopPropagation();
            onDelete(location.id);
          }}
          className='flex h-11 max-h-11 w-full cursor-pointer items-center justify-center overflow-hidden rounded-b-lg bg-card/95 text-foreground transition-[max-height,color] duration-200 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-60 sm:max-h-0 sm:group-hover:max-h-11 dark:bg-neutral-800/95 dark:text-white dark:hover:text-red-600'
          aria-label={`Izbriši lokacijo ${location.name}`}>
          <Trash size={20} />
        </button>
      )}
    </article>
  );
}
