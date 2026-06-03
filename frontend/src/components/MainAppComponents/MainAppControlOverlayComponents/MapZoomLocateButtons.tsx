import { LocateFixed, Minus, Plus } from "lucide-react";
import { Button } from "../../ui/button";

type MapZoomLocateButtonsProps = {
  onZoomIn?: () => void;
  onZoomOut?: () => void;
  onLocate?: () => void;
  isRouteActive: boolean;
};

export function MapZoomLocateButtons({
  onZoomIn,
  onZoomOut,
  onLocate,
  isRouteActive,
}: MapZoomLocateButtonsProps) {
  return (
    <>
      <Button
        type='button'
        onClick={onZoomIn}
        className='flex h-10 w-10 items-center justify-center rounded-md bg-white/85 text-neutral-900 shadow-lg hover:text-red-700 dark:bg-neutral-700 dark:text-white dark:hover:text-red-200'
        aria-label='Povečaj'>
        <Plus size={20} />
      </Button>
      <Button
        type='button'
        onClick={onZoomOut}
        className='flex h-10 w-10 items-center justify-center rounded-md bg-white/85 text-neutral-900 shadow-lg hover:text-red-700 dark:bg-neutral-700 dark:text-white dark:hover:text-red-200'
        aria-label='Pomanjšaj'>
        <Minus size={20} />
      </Button>
      <Button
        type='button'
        onClick={onLocate}
        disabled={isRouteActive}
        title={
          isRouteActive
            ? "Med sledenjem poti se zemljevid centrira samodejno."
            : undefined
        }
        className='flex h-10 w-10 items-center justify-center rounded-md bg-white/85 text-neutral-900 shadow-lg hover:text-red-700 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-neutral-700 dark:text-white dark:hover:text-red-200'
        aria-label='Moja lokacija'>
        <LocateFixed size={20} />
      </Button>
    </>
  );
}
