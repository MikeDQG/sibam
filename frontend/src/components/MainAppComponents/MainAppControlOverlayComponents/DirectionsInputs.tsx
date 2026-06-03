import { ArrowUpDown } from "lucide-react";
import { SearchInputRow } from "./SearchInputRow";
import type { PlacesAutocomplete } from "./types";

type DirectionsInputsProps = {
  origin: PlacesAutocomplete;
  destination: PlacesAutocomplete;
  onOriginFocus: () => void;
  onDestinationFocus: () => void;
  onOriginClear: () => void;
  onDestinationClear: () => void;
  onSwap: () => void;
};

export function DirectionsInputs({
  origin,
  destination,
  onOriginFocus,
  onDestinationFocus,
  onOriginClear,
  onDestinationClear,
  onSwap,
}: DirectionsInputsProps) {
  const inputClassName =
    "h-full w-auto flex-1 rounded-none border-0 bg-transparent pl-8 pr-2 text-sm font-normal shadow-none dark:bg-transparent focus-visible:ring-0 focus-visible:outline-none";
  const clearClassName =
    "mr-1 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-muted-foreground hover:text-foreground dark:hover:text-white";

  return (
    <div className='relative'>
      <div className='overflow-hidden rounded-lg bg-white/95 text-neutral-900 shadow-md dark:bg-neutral-700 dark:text-white'>
        <SearchInputRow
          autocomplete={origin}
          placeholder='Kje štartaš?'
          onFocus={onOriginFocus}
          onClear={onOriginClear}
          inputClassName={inputClassName}
          clearClassName={clearClassName}
        />
        <div className='h-px bg-border dark:bg-neutral-600' />
        <SearchInputRow
          autocomplete={destination}
          placeholder='Kam šibaš?'
          onFocus={onDestinationFocus}
          onClear={onDestinationClear}
          inputClassName={inputClassName}
          clearClassName={clearClassName}
        />
      </div>
      <button
        type='button'
        onClick={onSwap}
        className='absolute right-2 top-1/2 z-10 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-full bg-muted text-muted-foreground shadow-md transition-colors hover:text-foreground dark:bg-neutral-600 dark:text-neutral-300 dark:hover:text-white'
        aria-label='Zamenjaj smeri'>
        <ArrowUpDown size={16} />
      </button>
    </div>
  );
}
