import { describe, expect, it, vi } from "vitest";
import { RouteControls } from "../../components/MainAppComponents/MainAppControlOverlayComponents/RouteControls";
import { SavedLocationMapCard } from "../../components/Pages/AccountPageComponents/SavedLocationMapCard";
import {
  mariborCenter,
  render,
  renderWithTheme,
  savedLocation,
  screen,
  tabor,
} from "../frontendPlanFixtures";

describe("responsive-ui", () => {
  it("layout na mobilnih sirinah", () => {
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
        onSavedRoutesToggle={vi.fn()}
      />,
    );

    expect(screen.getByRole("button", { name: "Najdi pot" }).className).toContain("max-[430px]:w-full");
  });

  it("layout na desktop sirinah", () => {
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
        onSavedRoutesToggle={vi.fn()}
      />,
    );

    expect(screen.getByRole("button", { name: "Najdi pot" }).className).toContain("ml-auto");
  });

  it("gumbi in tekst se ne prekrivajo", () => {
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
        onSavedRoutesToggle={vi.fn()}
      />,
    );

    expect(screen.getByRole("button", { name: "Najdi pot" })).toHaveClass("whitespace-nowrap");
  });

  it("dropdowni in route sheet ostanejo uporabni na majhnem zaslonu", () => {
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
        onSavedRoutesToggle={vi.fn()}
      />,
    );

    expect(screen.getByRole("button", { name: "Shranjene poti" }).className).toContain("max-[615px]:w-full");
  });

  it("profil kartice se pravilno razporedijo po gridu", () => {
    renderWithTheme(<SavedLocationMapCard location={savedLocation} />);

    expect(screen.getByRole("article", { name: savedLocation.name })).toHaveClass("rounded-lg");
  });
});
