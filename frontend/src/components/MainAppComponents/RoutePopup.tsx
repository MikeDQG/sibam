import { AdvancedMarker } from "@vis.gl/react-google-maps";
import { X } from "lucide-react";
import type { MapPoint, RouteLeg } from "./RoutePolyline";

export type RoutePopupSelection = {
  leg: RouteLeg;
  position: MapPoint;
  source: "path" | "busIcon" | "bikePickupIcon" | "bikeReturnIcon";
  previousLeg?: RouteLeg;
};

type RoutePopupProps = {
  selectedLeg: RoutePopupSelection;
  onClose?: () => void;
};

const opcijeNaslov = {
  WALK: "Peš",
  BIKE: "Kolo",
  BUS: "Avtobus",
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

const formatTime = (timestamp?: string) => {
  if (!timestamp) return null;

  const date = new Date(Number(timestamp));
  if (Number.isNaN(date.getTime())) return null;

  return date.toLocaleTimeString("sl-SI", {
    hour: "2-digit",
    minute: "2-digit",
  });
};

export const RoutePopup = ({ selectedLeg, onClose }: RoutePopupProps) => {
  const showPathDetails = selectedLeg.source === "path";
  const showBusIconDetails = selectedLeg.source === "busIcon";
  const showBikePickupPlaces = selectedLeg.source === "bikePickupIcon";

  // pokazemo ce je izbran return bike ikona
  const showBikeReturnIconPlaces = selectedLeg.source === "bikeReturnIcon";

  // pokazemo ce je izbran bus ikona in je prejsna pot kolesarska --> bus po vrnitvi kolesa
  const showBusAfterBikeReturnPlaces =
    showBusIconDetails && selectedLeg.previousLeg?.mode === "BIKE";

  return (
    <AdvancedMarker
      position={selectedLeg.position}
      zIndex={1000}
      clickable
      anchorLeft='-50%'
      anchorTop='-100%'>
      <div className='relative mb-3 min-w-52 rounded-lg border border-white/10 bg-neutral-800 px-4 py-3 text-sm text-white shadow-2xl'>
        <button
          type='button'
          onClick={onClose}
          className='absolute right-2 top-2 flex h-7 w-7 items-center justify-center rounded-md text-neutral-300 hover:bg-white/10 hover:text-white'
          aria-label='Zapri podatke o poti'>
          <X size={16} />
        </button>

        <strong className='block pr-8 text-base font-semibold'>
          {opcijeNaslov[selectedLeg.leg.mode as keyof typeof opcijeNaslov] ||
            selectedLeg.leg.mode}
        </strong>

        <div className='mt-2 space-y-1.5'>
          <RoutePopupRow
            label='Odhod avtobusa'
            value={
              showBusIconDetails && selectedLeg.leg.mode === "BUS"
                ? formatTime(selectedLeg.leg.departure)
                : null
            }
          />
          <RoutePopupRow
            label='Mesta za oddajo kolesa'
            value={
              showBusAfterBikeReturnPlaces || showBikeReturnIconPlaces
                ? (selectedLeg.previousLeg?.freeStands ??
                  selectedLeg.leg.freeStands)
                : null
            }
          />
          <RoutePopupRow
            label='Prosta kolesa'
            value={showBikePickupPlaces ? selectedLeg.leg.freeBikes : null}
          />
          <RoutePopupRow
            label='Trajanje'
            value={
              showPathDetails ? formatDuration(selectedLeg.leg.duration) : null
            }
          />
          <RoutePopupRow
            label='Pot'
            value={
              showPathDetails ? formatDistance(selectedLeg.leg.distance) : null
            }
          />
        </div>

        <div className='absolute left-1/2 top-full h-3 w-3 -translate-x-1/2 -translate-y-1/2 rotate-45 border-b border-r border-white/10 bg-neutral-800' />
      </div>
    </AdvancedMarker>
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
