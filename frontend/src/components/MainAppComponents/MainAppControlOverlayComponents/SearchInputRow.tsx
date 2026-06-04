import { Search, X } from "lucide-react";
import { Input } from "../../ui/input";
import type { PlacesAutocomplete } from "./types";

type SearchInputRowProps = {
  autocomplete: PlacesAutocomplete;
  placeholder: string;
  onFocus: () => void;
  onClear: () => void;
  inputClassName: string;
  clearClassName: string;
};

export function SearchInputRow({
  autocomplete,
  placeholder,
  onFocus,
  onClear,
  inputClassName,
  clearClassName,
}: SearchInputRowProps) {
  return (
    <div className='relative flex h-10 items-center pr-10'>
      <Search
        size={16}
        className='pointer-events-none absolute left-3 z-10 shrink-0 text-muted-foreground'
      />
      <Input
        type='text'
        value={autocomplete.value}
        onChange={autocomplete.handleChange}
        onFocus={onFocus}
        onKeyDown={(e) => e.key === "Escape" && autocomplete.setIsOpen(false)}
        placeholder={placeholder}
        className={inputClassName}
        aria-label={placeholder}
      />
      {autocomplete.value && (
        <button
          type='button'
          onClick={onClear}
          className={clearClassName}
          aria-label='Počisti'>
          <X size={13} />
        </button>
      )}
    </div>
  );
}
