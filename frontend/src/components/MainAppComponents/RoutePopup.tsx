import { AdvancedMarker } from "@vis.gl/react-google-maps";
import { X } from "lucide-react";
import type { ReactNode } from "react";
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

const formatCount = (count?: number) => {
  if (typeof count !== "number" || !Number.isFinite(count)) return null;

  return Math.round(count).toString();
};

const formatProbability = (probability?: number) => {
  if (typeof probability !== "number" || !Number.isFinite(probability)) {
    return null;
  }

  return `${Math.round(probability * 100)} %`;
};

const getProbabilityColorClass = (probability?: number) => {
  if (typeof probability !== "number" || !Number.isFinite(probability)) {
    return "";
  }

  const percentage = probability * 100;
  if (percentage > 80) return "text-emerald-600 dark:text-emerald-400";
  if (percentage >= 50) return "text-yellow-600 dark:text-yellow-300";
  if (percentage >= 25) return "text-orange-600 dark:text-orange-400";

  return "text-red-600 dark:text-red-400";
};

const formatPredictionValue = (
  count?: number,
  probability?: number,
  probabilityLabel = "Natančnost napovedi",
) => {
  const formattedCount = formatCount(count);
  if (!formattedCount) return null;

  const formattedProbability = formatProbability(probability);

  return (
    <span className='inline-flex items-baseline gap-2'>
      <span>{formattedCount}</span>
      {formattedProbability && (
        <span
          className={`group/probability relative cursor-help text-xs font-semibold ${getProbabilityColorClass(probability)}`}
          aria-label={probabilityLabel}
          title={probabilityLabel}>
          {formattedProbability}
          <span className='pointer-events-none absolute bottom-full right-0 z-10 mb-1 hidden w-max max-w-52 rounded-md bg-neutral-900 px-2 py-1 text-xs font-medium text-white shadow-lg group-hover/probability:block dark:bg-white dark:text-neutral-900'>
            {probabilityLabel}
          </span>
        </span>
      )}
    </span>
  );
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
  const bikeReturnLeg =
    showBusAfterBikeReturnPlaces || showBikeReturnIconPlaces
      ? (selectedLeg.previousLeg ?? selectedLeg.leg)
      : null;

  return (
    <AdvancedMarker
      position={selectedLeg.position}
      zIndex={1000}
      clickable
      anchorLeft='-50%'
      anchorTop='-100%'>
      <div className='relative mb-3 min-w-52 rounded-lg border border-border bg-card px-4 py-3 text-sm text-card-foreground shadow-2xl dark:border-white/10 dark:bg-neutral-800 dark:text-white'>
        <button
          type='button'
          onClick={onClose}
          className='absolute right-2 top-2 flex h-7 w-7 items-center justify-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground dark:text-neutral-300 dark:hover:bg-white/10 dark:hover:text-white'
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
            label='Pričakovana zamuda (min)'
            value={
              showBusIconDetails &&
              selectedLeg.leg.mode === "BUS" &&
              typeof selectedLeg.leg.busDelayPrediction
                ?.predictedBoardingDelaySeconds === "number" &&
              Number.isFinite(
                selectedLeg.leg.busDelayPrediction
                  .predictedBoardingDelaySeconds,
              )
                ? Math.round(
                    selectedLeg.leg.busDelayPrediction
                      .predictedBoardingDelaySeconds / 60,
                  ).toString()
                : null
            }
          />
          <RoutePopupRow
            label='Mesta za oddajo kolesa'
            value={
              bikeReturnLeg
                ? (bikeReturnLeg.freeStands ?? selectedLeg.leg.freeStands)
                : null
            }
          />
          <RoutePopupRow
            label='Mesta za oddajo ob tovjem prihodu'
            value={
              bikeReturnLeg
                ? formatPredictionValue(
                    bikeReturnLeg.bikePrediction?.predictedStandsAtReturn,
                    bikeReturnLeg.bikePrediction
                      ?.returnStandAvailableProbability,
                    "Natančnost napovedi mest ob oddaji",
                  )
                : null
            }
          />
          <RoutePopupRow
            label='Prosta kolesa'
            value={showBikePickupPlaces ? selectedLeg.leg.freeBikes : null}
          />
          <RoutePopupRow
            label='Prosta kolesa ob tvojem prihodu'
            value={
              showBikePickupPlaces
                ? formatPredictionValue(
                    selectedLeg.leg.bikePrediction?.predictedBikesAtPickup,
                    selectedLeg.leg.bikePrediction
                      ?.pickupBikeAvailableProbability,
                    "Natančnost napovedi koles ob prihodu",
                  )
                : null
            }
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

        <div className='absolute left-1/2 top-full h-3 w-3 -translate-x-1/2 -translate-y-1/2 rotate-45 border-b border-r border-border bg-card dark:border-white/10 dark:bg-neutral-800' />
      </div>
    </AdvancedMarker>
  );
};

type RoutePopupRowProps = {
  label: string;
  value?: ReactNode | null;
};

const RoutePopupRow = ({ label, value }: RoutePopupRowProps) => {
  if (!value) return null;

  return (
    <div className='flex justify-between gap-5'>
      <span className='text-muted-foreground dark:text-neutral-300'>
        {label}
      </span>
      <span className='font-medium'>{value}</span>
    </div>
  );
};
