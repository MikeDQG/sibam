import { describe, expect, it, vi } from "vitest";
import { DestinationSearch } from "../../components/MainAppComponents/MainAppControlOverlayComponents/DestinationSearch";
import { DirectionsInputs } from "../../components/MainAppComponents/MainAppControlOverlayComponents/DirectionsInputs";
import { MapControls } from "../../components/MainAppComponents/MainAppControlOverlayComponents/MapControls";
import { RouteControls } from "../../components/MainAppComponents/MainAppControlOverlayComponents/RouteControls";
import {
  createAutocomplete,
  fireEvent,
  mariborCenter,
  render,
  renderWithTheme,
  screen,
  tabor,
} from "../frontendPlanFixtures";

describe("overlay", () => {
  it("iskanje cilja v enojnem search baru", () => {
    const destination = createAutocomplete("Glavni trg");
    render(
      <DestinationSearch
        destination={destination}
        selectedPlace=''
        onDestinationFocus={vi.fn()}
        onDestinationClear={vi.fn()}
        onSavedRoutesToggle={vi.fn()}
        onShowDirectionsClick={vi.fn()}
      />,
    );

    expect(screen.getByRole("textbox", { name: "Kam šibaš?" })).toHaveValue("Glavni trg");
  });

  it("preklop iz search bara v nacin iskanja poti", () => {
    const onShowDirectionsClick = vi.fn();
    render(
      <DestinationSearch
        destination={createAutocomplete("Tabor")}
        selectedPlace='Tabor'
        onDestinationFocus={vi.fn()}
        onDestinationClear={vi.fn()}
        onSavedRoutesToggle={vi.fn()}
        onShowDirectionsClick={onShowDirectionsClick}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Navodila za pot" }));
    expect(onShowDirectionsClick).toHaveBeenCalledTimes(1);
  });

  it("Escape v enojnem search baru zapre dropdown", () => {
    const destination = createAutocomplete("Tabor", { setIsOpen: vi.fn() });
    render(
      <DestinationSearch
        destination={destination}
        selectedPlace='Tabor'
        onDestinationFocus={vi.fn()}
        onDestinationClear={vi.fn()}
        onSavedRoutesToggle={vi.fn()}
        onShowDirectionsClick={vi.fn()}
      />,
    );

    fireEvent.keyDown(screen.getByRole("textbox", { name: "Kam šibaš?" }), {
      key: "Escape",
    });

    expect(destination.setIsOpen).toHaveBeenCalledWith(false);
  });

  it("gumba za ciscenje cilja in shranjene poti sprozita callbacke", () => {
    const onDestinationClear = vi.fn();
    const onSavedRoutesToggle = vi.fn();
    render(
      <DestinationSearch
        destination={createAutocomplete("Tabor")}
        selectedPlace=''
        onDestinationFocus={vi.fn()}
        onDestinationClear={onDestinationClear}
        onSavedRoutesToggle={onSavedRoutesToggle}
        onShowDirectionsClick={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Počisti" }));
    fireEvent.click(screen.getByRole("button", { name: "Shranjene poti" }));

    expect(onDestinationClear).toHaveBeenCalledTimes(1);
    expect(onSavedRoutesToggle).toHaveBeenCalledTimes(1);
  });

  it("vnos izhodisca in cilja", () => {
    const origin = createAutocomplete("Glavni trg");
    const destination = createAutocomplete("Tabor");
    render(
      <DirectionsInputs
        origin={origin}
        destination={destination}
        onOriginFocus={vi.fn()}
        onDestinationFocus={vi.fn()}
        onOriginClear={vi.fn()}
        onDestinationClear={vi.fn()}
        onSwap={vi.fn()}
      />,
    );

    expect(screen.getByRole("textbox", { name: "Kje štartaš?" })).toHaveValue("Glavni trg");
    expect(screen.getByRole("textbox", { name: "Kam šibaš?" })).toHaveValue("Tabor");
  });

  it("zamenjava izhodisca in cilja", () => {
    const onSwap = vi.fn();
    render(
      <DirectionsInputs
        origin={createAutocomplete("A")}
        destination={createAutocomplete("B")}
        onOriginFocus={vi.fn()}
        onDestinationFocus={vi.fn()}
        onOriginClear={vi.fn()}
        onDestinationClear={vi.fn()}
        onSwap={onSwap}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Zamenjaj smeri" }));
    expect(onSwap).toHaveBeenCalledTimes(1);
  });

  it("omogocanje in onemogocanje gumba Najdi pot", () => {
    const { rerender } = render(
      <RouteControls
        useBus
        useBike
        timeMode='depart'
        selectedTime='08:15'
        hasRoute={false}
        isRouteActive={false}
        originCoords={null}
        destinationCoords={null}
        onToggleBus={vi.fn()}
        onToggleBike={vi.fn()}
        onToggleTimeMode={vi.fn()}
        onSelectedTimeChange={vi.fn()}
        onRouteRequest={vi.fn()}
        onSavedRoutesToggle={vi.fn()}
      />,
    );

    expect(screen.getByRole("button", { name: "Najdi pot" })).toBeDisabled();
    rerender(
      <RouteControls
        useBus
        useBike
        timeMode='depart'
        selectedTime='08:15'
        hasRoute={false}
        isRouteActive={false}
        originCoords={mariborCenter}
        destinationCoords={tabor}
        onToggleBus={vi.fn()}
        onToggleBike={vi.fn()}
        onToggleTimeMode={vi.fn()}
        onSelectedTimeChange={vi.fn()}
        onRouteRequest={vi.fn()}
        onSavedRoutesToggle={vi.fn()}
      />,
    );
    expect(screen.getByRole("button", { name: "Najdi pot" })).toBeEnabled();
  });

  it("preklop avtobus/kolo", () => {
    const onToggleBus = vi.fn();
    const onToggleBike = vi.fn();
    render(
      <RouteControls
        useBus
        useBike
        timeMode='depart'
        selectedTime='08:15'
        hasRoute={false}
        isRouteActive={false}
        originCoords={mariborCenter}
        destinationCoords={tabor}
        onToggleBus={onToggleBus}
        onToggleBike={onToggleBike}
        onToggleTimeMode={vi.fn()}
        onSelectedTimeChange={vi.fn()}
        onRouteRequest={vi.fn()}
        onSavedRoutesToggle={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /bus/i }));
    fireEvent.click(screen.getByRole("button", { name: /kolo/i }));
    expect(onToggleBus).toHaveBeenCalledTimes(1);
    expect(onToggleBike).toHaveBeenCalledTimes(1);
  });

  it("preklop Odhod ob/Prihod do", () => {
    const onToggleTimeMode = vi.fn();
    render(
      <RouteControls
        useBus
        useBike
        timeMode='depart'
        selectedTime='08:15'
        hasRoute={false}
        isRouteActive={false}
        originCoords={mariborCenter}
        destinationCoords={tabor}
        onToggleBus={vi.fn()}
        onToggleBike={vi.fn()}
        onToggleTimeMode={onToggleTimeMode}
        onSelectedTimeChange={vi.fn()}
        onRouteRequest={vi.fn()}
        onSavedRoutesToggle={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Odhod ob" }));
    expect(onToggleTimeMode).toHaveBeenCalledTimes(1);
  });

  it("izbira casa", () => {
    const onSelectedTimeChange = vi.fn();
    render(
      <RouteControls
        useBus
        useBike
        timeMode='depart'
        selectedTime='08:15'
        hasRoute={false}
        isRouteActive={false}
        originCoords={mariborCenter}
        destinationCoords={tabor}
        onToggleBus={vi.fn()}
        onToggleBike={vi.fn()}
        onToggleTimeMode={vi.fn()}
        onSelectedTimeChange={onSelectedTimeChange}
        onRouteRequest={vi.fn()}
        onSavedRoutesToggle={vi.fn()}
      />,
    );

    fireEvent.change(screen.getByDisplayValue("08:15"), { target: { value: "09:30" } });
    expect(onSelectedTimeChange).toHaveBeenCalledWith("09:30");
  });

  it("odpiranje shranjenih poti", () => {
    const onSavedRoutesToggle = vi.fn();
    render(
      <RouteControls
        useBus
        useBike
        timeMode='depart'
        selectedTime='08:15'
        hasRoute={false}
        isRouteActive={false}
        originCoords={mariborCenter}
        destinationCoords={tabor}
        onToggleBus={vi.fn()}
        onToggleBike={vi.fn()}
        onToggleTimeMode={vi.fn()}
        onSelectedTimeChange={vi.fn()}
        onRouteRequest={vi.fn()}
        onSavedRoutesToggle={onSavedRoutesToggle}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Shranjene poti" }));
    expect(onSavedRoutesToggle).toHaveBeenCalledTimes(1);
  });

  it("navigacija na profil ali prijavo", () => {
    const onProfileClick = vi.fn();
    renderWithTheme(
      <MapControls
        isLoggedIn={false}
        showDirections={false}
        isRouteActive={false}
        onProfileClick={onProfileClick}
        onLogoutClick={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Profil" }));
    expect(onProfileClick).toHaveBeenCalledTimes(1);
  });

  it("zoom in, zoom out in locate akcije", () => {
    const onZoomIn = vi.fn();
    const onZoomOut = vi.fn();
    const onLocate = vi.fn();
    renderWithTheme(
      <MapControls
        isLoggedIn
        showDirections={false}
        isRouteActive={false}
        onProfileClick={vi.fn()}
        onLogoutClick={vi.fn()}
        onZoomIn={onZoomIn}
        onZoomOut={onZoomOut}
        onLocate={onLocate}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Povečaj" }));
    fireEvent.click(screen.getByRole("button", { name: "Pomanjšaj" }));
    fireEvent.click(screen.getByRole("button", { name: "Moja lokacija" }));
    expect(onZoomIn).toHaveBeenCalledTimes(1);
    expect(onZoomOut).toHaveBeenCalledTimes(1);
    expect(onLocate).toHaveBeenCalledTimes(1);
  });
});
