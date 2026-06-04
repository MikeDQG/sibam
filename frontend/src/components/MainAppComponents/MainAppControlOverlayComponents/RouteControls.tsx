import type React from "react";
import { Bike, Bookmark, Bus } from "lucide-react";
import type { Coordinates, TimeMode } from "./types";

type RouteControlsProps = {
  useBus: boolean;
  useBike: boolean;
  timeMode: TimeMode;
  selectedTime: string;
  selectedDate?: string;
  dateButtonRef?: React.RefObject<HTMLButtonElement | null>;
  hasRoute: boolean;
  isRouteStale?: boolean;
  isRouteActive: boolean;
  originCoords: Coordinates | null;
  destinationCoords: Coordinates | null;
  onToggleBus: () => void;
  onToggleBike: () => void;
  onToggleTimeMode: () => void;
  onSelectedTimeChange: (value: string) => void;
  onDateButtonClick?: () => void;
  onRouteRequest: () => void;
  onSavedRoutesToggle: () => void;
  dateDropdown?: React.ReactNode;
};

function getRouteActionLabel(
  hasRoute: boolean,
  isRouteStale: boolean,
  isRouteActive: boolean,
) {
  if (isRouteActive) return "Končaj";
  if (hasRoute && !isRouteStale) return "Začni";

  return "Najdi pot";
}

export function RouteControls({
  useBus,
  useBike,
  timeMode,
  selectedTime,
  selectedDate = new Date().toISOString().slice(0, 10),
  dateButtonRef,
  hasRoute,
  isRouteStale = false,
  isRouteActive,
  originCoords,
  destinationCoords,
  onToggleBus,
  onToggleBike,
  onToggleTimeMode,
  onSelectedTimeChange,
  onDateButtonClick,
  onRouteRequest,
  onSavedRoutesToggle,
  dateDropdown,
}: RouteControlsProps) {
  const isRouteRequestDisabled =
    !isRouteActive &&
    (!hasRoute || isRouteStale) &&
    (!originCoords || !destinationCoords);

  return (
    <div className='flex items-center gap-2 max-[615px]:grid max-[615px]:grid-cols-2 max-[615px]:items-stretch max-[430px]:!grid-cols-1'>
      <div className='flex items-center gap-2 max-[615px]:order-3 max-[615px]:col-start-1 max-[615px]:row-start-3 max-[615px]:grid max-[615px]:grid-cols-2 max-[430px]:!row-start-4'>
        <TransportToggleButton
          active={useBus}
          icon={<Bus size={14} />}
          label='Bus'
          onClick={onToggleBus}
        />
        <TransportToggleButton
          active={useBike}
          icon={<Bike size={14} />}
          label='Kolo'
          onClick={onToggleBike}
        />
      </div>
      <div className='flex min-w-[190px] overflow-hidden rounded-lg bg-white text-neutral-900 shadow-md max-[615px]:order-2 max-[615px]:col-start-1 max-[615px]:row-start-2 max-[615px]:w-full max-[430px]:!row-start-3 dark:bg-neutral-700 dark:text-white'>
        <button
          type='button'
          onClick={onToggleTimeMode}
          className='min-w-[5.6rem] flex-1 whitespace-nowrap px-3 py-1.5 text-sm transition-colors hover:bg-muted max-[615px]:px-2 dark:text-white dark:hover:bg-neutral-600'>
          {timeMode === "depart" ? "Odhod ob" : "Prihod do"}
        </button>
        <div className='w-px bg-border dark:bg-neutral-600' />
        <input
          type='time'
          value={selectedTime}
          onChange={(e) => onSelectedTimeChange(e.target.value)}
          className='w-[5.5rem] bg-transparent px-2 py-1.5 text-sm focus:outline-none max-[615px]:w-[4.7rem] max-[615px]:px-1.5 dark:text-white'
        />
        {onDateButtonClick && (
          <>
            <div className='w-px bg-border dark:bg-neutral-600' />
            <button
              ref={dateButtonRef}
              type='button'
              onClick={onDateButtonClick}
              className='whitespace-nowrap px-3 py-1.5 text-sm transition-colors hover:bg-muted max-[615px]:px-2 dark:text-white dark:hover:bg-neutral-600'>
              <span className='max-[430px]:hidden'>
                {new Date(`${selectedDate}T00:00:00`).toLocaleDateString("sl-SI", {
                  weekday: "short",
                  day: "numeric",
                  month: "numeric",
                })}
              </span>
              <span className='hidden max-[430px]:inline'>
                {new Date(`${selectedDate}T00:00:00`).toLocaleDateString("sl-SI", {
                  day: "numeric",
                  month: "numeric",
                })}
              </span>
            </button>
          </>
        )}
      </div>
      {dateDropdown}
      <button
        type='button'
        onClick={onRouteRequest}
        disabled={isRouteRequestDisabled}
        className='ml-auto flex cursor-pointer items-center justify-center gap-1.5 whitespace-nowrap rounded-md bg-neutral-50 px-4 py-1.5 text-sm font-bold text-red-700 shadow-md transition-colors hover:bg-neutral-50 disabled:cursor-not-allowed disabled:bg-neutral-200 disabled:opacity-40 disabled:hover:bg-neutral-200 max-[615px]:order-4 max-[615px]:col-start-2 max-[615px]:row-start-2 max-[615px]:ml-0 max-[615px]:rounded-lg max-[615px]:px-3 max-[430px]:!col-start-1 max-[430px]:!row-start-2 max-[430px]:w-full dark:bg-neutral-200 dark:hover:bg-neutral-50 dark:disabled:bg-neutral-200 dark:disabled:hover:bg-neutral-200'>
        {getRouteActionLabel(hasRoute, isRouteStale, isRouteActive)}
      </button>
      <SavedRoutesButton onClick={onSavedRoutesToggle} />
    </div>
  );
}

type TransportToggleButtonProps = {
  active: boolean;
  icon: React.ReactNode;
  label: string;
  onClick: () => void;
};

function TransportToggleButton({
  active,
  icon,
  label,
  onClick,
}: TransportToggleButtonProps) {
  return (
    <button
      type='button'
      onClick={onClick}
      className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm shadow-md transition-colors max-[615px]:justify-center max-[615px]:px-2 ${active ? "bg-red-700 text-white" : "bg-white text-muted-foreground dark:bg-neutral-700 dark:text-neutral-400"}`}>
      {icon}
      {label}
    </button>
  );
}

function SavedRoutesButton({ onClick }: { onClick: () => void }) {
  return (
    <button
      type='button'
      onClick={onClick}
      className='flex h-8 min-w-9 items-center justify-center whitespace-nowrap rounded-md bg-white/95 text-neutral-900 shadow-md transition-colors hover:text-red-700 max-[615px]:order-1 max-[615px]:col-span-2 max-[615px]:h-auto max-[615px]:w-full max-[615px]:gap-1.5 max-[615px]:px-3 max-[615px]:py-1.5 max-[615px]:text-sm max-[615px]:font-semibold max-[430px]:!col-span-1 dark:bg-neutral-700 dark:text-white dark:hover:text-red-200'
      aria-label='Shranjene poti'>
      <Bookmark size={16} />
      <span className='hidden max-[615px]:inline'>Shranjene poti</span>
    </button>
  );
}
