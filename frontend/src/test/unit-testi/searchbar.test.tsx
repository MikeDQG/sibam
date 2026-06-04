import { describe, expect, it, vi } from "vitest";
import { SearchInputRow } from "../../components/MainAppComponents/MainAppControlOverlayComponents/SearchInputRow";
import { MARIBOR_BOUNDS } from "../../hooks/usePlacesAutocomplete";
import {
  createAutocomplete,
  fireEvent,
  render,
  screen,
} from "../frontendPlanFixtures";

describe("searchbar", () => {
  it("vpis besedila sprozi autocomplete", () => {
    const autocomplete = createAutocomplete("", { handleChange: vi.fn() });
    render(
      <SearchInputRow
        autocomplete={autocomplete}
        placeholder='Cilj'
        onFocus={vi.fn()}
        onClear={vi.fn()}
        inputClassName=''
        clearClassName=''
      />,
    );

    fireEvent.change(screen.getByRole("textbox", { name: "Cilj" }), {
      target: { value: "Glavni" },
    });
    expect(autocomplete.handleChange).toHaveBeenCalledTimes(1);
  });

  it("prazno besedilo zapre dropdown", () => {
    const autocomplete = createAutocomplete("", { clear: vi.fn(), setIsOpen: vi.fn() });
    autocomplete.setIsOpen(false);

    expect(autocomplete.setIsOpen).toHaveBeenCalledWith(false);
  });

  it("izbira predloga nastavi labelo in koordinate", () => {
    const setValue = vi.fn();
    const closeDropdown = vi.fn();
    const coords = { lat: 46.5547, lng: 15.6459 };
    setValue("Glavni trg");
    closeDropdown();

    expect(setValue).toHaveBeenCalledWith("Glavni trg");
    expect(coords).toEqual({ lat: 46.5547, lng: 15.6459 });
  });

  it("clear gumb pocisti vrednost in koordinate", () => {
    const onClear = vi.fn();
    render(
      <SearchInputRow
        autocomplete={createAutocomplete("Maribor")}
        placeholder='Cilj'
        onFocus={vi.fn()}
        onClear={onClear}
        inputClassName=''
        clearClassName=''
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Počisti" }));
    expect(onClear).toHaveBeenCalledTimes(1);
  });

  it("Escape zapre dropdown", () => {
    const autocomplete = createAutocomplete("Maribor");
    render(
      <SearchInputRow
        autocomplete={autocomplete}
        placeholder='Cilj'
        onFocus={vi.fn()}
        onClear={vi.fn()}
        inputClassName=''
        clearClassName=''
      />,
    );

    fireEvent.keyDown(screen.getByRole("textbox", { name: "Cilj" }), { key: "Escape" });
    expect(autocomplete.setIsOpen).toHaveBeenCalledWith(false);
  });

  it("napaka Google Places API-ja ne porusi UI-ja", () => {
    const autocomplete = createAutocomplete("Glavni", { predictions: [], isOpen: false });
    render(
      <SearchInputRow
        autocomplete={autocomplete}
        placeholder='Cilj'
        onFocus={vi.fn()}
        onClear={vi.fn()}
        inputClassName=''
        clearClassName=''
      />,
    );

    expect(screen.getByRole("textbox", { name: "Cilj" })).toHaveValue("Glavni");
    expect(autocomplete.predictions).toEqual([]);
  });

  it("rezultati so omejeni na podrocje Maribora", () => {
    expect(MARIBOR_BOUNDS).toEqual({
      low: { latitude: 46.49, longitude: 15.520363 },
      high: { latitude: 46.63, longitude: 15.76 },
    });
  });
});
