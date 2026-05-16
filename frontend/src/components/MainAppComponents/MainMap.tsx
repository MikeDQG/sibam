import { X } from "lucide-react";
import { APIProvider, AdvancedMarker, Map } from "@vis.gl/react-google-maps";
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
  selectedLeg?: {
    leg: RouteLeg;
    position: MapPoint;
  } | null;
  onLegClick?: (leg: RouteLeg, position: MapPoint) => void;
  onRoutePopupClose?: () => void;
  onCameraChanged?: (center: MapCenter, zoom: number) => void;
};

const formatDuration = (duration?: string) => {
  if (!duration) return null;

  const minutes = Math.round(Number(duration) / 60000);
  if (!Number.isFinite(minutes)) return null;

  return `${minutes} min`;
};

const formatDistance = (distance?: string) => {
  if (!distance) return null;

  const meters = Number(distance);
  if (!Number.isFinite(meters)) return null;

  if (meters >= 1000) {
    return `${(meters / 1000).toFixed(1)} km`;
  }

  return `${Math.round(meters)} m`;
};

export const MainMap = ({
  center,
  zoom,
  legs,
  selectedLeg,
  onLegClick,
  onRoutePopupClose,
  onCameraChanged,
}: MainMapProps) => {
  const hasApiKey = apiKey && apiKey !== "your_google_maps_api_key";

  if (!hasApiKey) {
    return (
      <div className='absolute inset-0 z-0 flex items-center justify-center bg-neutral-800 text-neutral-300'>
        Manjka Google Maps API key
      </div>
    );
  }

  const opcijeNaslov = {
    WALK: "Peš",
    BIKE: "Kolo",
    BUS: "Avtobus",
  };

  return (
    <div className='absolute inset-0 z-0'>
      <APIProvider apiKey={apiKey} region='SI' language='sl'>
        <Map
          center={center}
          zoom={zoom}
          onCameraChanged={(event) => {
            onCameraChanged?.(event.detail.center, event.detail.zoom);
          }}
          colorScheme='DARK'
          gestureHandling='greedy'
          disableDefaultUI
          mapId={mapId}
          reuseMaps>
          {legs && <RoutePolyline legs={legs} onLegClick={onLegClick} />}
          {selectedLeg && (
            <AdvancedMarker
              position={selectedLeg.position}
              zIndex={1000}
              clickable
              anchorLeft='-50%'
              anchorTop='-100%'>
              <div className='relative mb-3 min-w-52 rounded-lg border border-white/10 bg-neutral-800 px-4 py-3 text-sm text-white shadow-2xl'>
                <button
                  type='button'
                  onClick={onRoutePopupClose}
                  className='absolute right-2 top-2 flex h-7 w-7 items-center justify-center rounded-md text-neutral-300 hover:bg-white/10 hover:text-white'
                  aria-label='Zapri podatke o poti'>
                  <X size={16} />
                </button>

                <strong className='block pr-8 text-base font-semibold'>
                  {opcijeNaslov[
                    selectedLeg.leg.tip as keyof typeof opcijeNaslov
                  ] || selectedLeg.leg.tip}
                </strong>

                <div className='mt-2 space-y-1.5'>
                  <RoutePopupRow
                    label='Trajanje'
                    value={formatDuration(selectedLeg.leg.trajanje)}
                  />
                  <RoutePopupRow
                    label='Razdalja'
                    value={formatDistance(selectedLeg.leg.dolzina)}
                  />
                  <RoutePopupRow
                    label='Kolesa'
                    value={selectedLeg.leg.prosti_bajki}
                  />
                  <RoutePopupRow
                    label='Mesta za oddajo'
                    value={selectedLeg.leg.prosta_mesta}
                  />
                </div>

                <div className='absolute left-1/2 top-full h-3 w-3 -translate-x-1/2 -translate-y-1/2 rotate-45 border-b border-r border-white/10 bg-neutral-800' />
              </div>
            </AdvancedMarker>
          )}
        </Map>
      </APIProvider>
    </div>
  );
};

type RoutePopupRowProps = {
  label: string;
  value?: string | null;
};

const RoutePopupRow = ({ label, value }: RoutePopupRowProps) => {
  if (!value) return null;

  return (
    <div className='flex justify-between gap-5'>
      <span className='text-neutral-300'>{label}</span>
      <span className='font-medium'>{value}</span>
    </div>
  );
};
