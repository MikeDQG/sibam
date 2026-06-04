import { Bookmark, Route, Search, X } from "lucide-react";
import { Input } from "../../ui/input";
import type { PlacesAutocomplete } from "./types";

type DestinationSearchProps = {
  destination: PlacesAutocomplete;
  selectedPlace: string;
  onDestinationFocus: () => void;
  onDestinationClear: () => void;
  onSavedRoutesToggle: () => void;
  onShowDirectionsClick: () => void;
};

export function DestinationSearch({
  destination,
  selectedPlace,
  onDestinationFocus,
  onDestinationClear,
  onSavedRoutesToggle,
  onShowDirectionsClick,
}: DestinationSearchProps) {
  return (
    <div className='relative flex h-10 items-center rounded-lg bg-white/95 text-neutral-900 shadow-md dark:bg-neutral-700 dark:text-white'>
      <Search
        size={16}
        className='pointer-events-none absolute left-3 z-10 shrink-0 text-muted-foreground'
      />
      <Input
        type='text'
        value={destination.value}
        onChange={destination.handleChange}
        onFocus={onDestinationFocus}
        onKeyDown={(e) => e.key === "Escape" && destination.setIsOpen(false)}
        placeholder='Kam šibaš?'
        className='h-full w-auto flex-1 rounded-lg border-0 bg-transparent pl-8 pr-2 text-sm font-normal shadow-none dark:bg-transparent focus-visible:ring-0 focus-visible:outline-none'
        aria-label='Kam šibaš?'
      />
      {destination.value && (
        <button
          type='button'
          onClick={onDestinationClear}
          className='mr-2 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-muted-foreground hover:text-foreground dark:hover:text-white'
          aria-label='Počisti'>
          <X size={13} />
        </button>
      )}
      <button
        type='button'
        onClick={onSavedRoutesToggle}
        className='mr-2 flex h-6 w-6 cursor-pointer shrink-0 items-center justify-center rounded-sm text-muted-foreground transition-colors hover:text-red-700 dark:hover:text-red-200'
        aria-label='Shranjene poti'>
        <Bookmark size={16} />
      </button>
      {selectedPlace && (
        <button
          type='button'
          onClick={onShowDirectionsClick}
          className='mr-2 flex h-6 w-6 cursor-pointer shrink-0 rotate-45 items-center justify-center rounded-sm bg-red-700 text-white shadow-sm transition-colors hover:bg-red-600'
          aria-label='Navodila za pot'>
          <Route size={14} className='-rotate-45' />
        </button>
      )}
    </div>
  );
}
