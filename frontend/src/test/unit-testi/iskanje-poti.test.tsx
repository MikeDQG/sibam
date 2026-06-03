import { describe, expect, it, vi } from "vitest";
import { RouteLoadingOverlay } from "../../components/MainAppComponents/RouteLoadingOverlay";
import { RouteControls } from "../../components/MainAppComponents/MainAppControlOverlayComponents/RouteControls";
import {
  buildComputeParams,
  fireEvent,
  mariborCenter,
  render,
  routePath,
  screen,
  tabor,
} from "../frontendPlanFixtures";

describe("iskanje-poti", () => {
  it("GET /compute se poklice s pravilnimi query parametri", () => {
    const params = buildComputeParams({ timeMode: "depart", selectedTime: "08:15", userId: "user-1" });

    expect(params.get("originLat")).toBe(String(mariborCenter.lat));
    expect(params.get("destinationLon")).toBe(String(tabor.lng));
    expect(params.get("originAddress")).toBe("Glavni trg");
    expect(params.get("destinationAddress")).toBe("Tabor");
    expect(params.get("leaveAt")).toBe("08:15");
    expect(params.get("userId")).toBe("user-1");
  });

  it("gumb Najdi pot je disabled brez izhodisca ali cilja", () => {
    render(
      <RouteControls
        useBus
        useBike
        timeMode='depart'
        selectedTime='08:15'
        hasRoute={false}
        isRouteActive={false}
        originCoords={null}
        destinationCoords={tabor}
        onToggleBus={vi.fn()}
        onToggleBike={vi.fn()}
        onToggleTimeMode={vi.fn()}
        onSelectedTimeChange={vi.fn()}
        onRouteRequest={vi.fn()}
        onSavedRoutesToggle={vi.fn()}
      />,
    );

    expect(screen.getByRole("button", { name: "Najdi pot" })).toBeDisabled();
  });

  it("gumb Najdi pot je enabled, ko sta znani obe koordinati", () => {
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

    expect(screen.getByRole("button", { name: "Najdi pot" })).toBeEnabled();
  });

  it("loading overlay se prikaze med racunanjem poti", () => {
    render(<RouteLoadingOverlay onDismiss={vi.fn()} />);

    expect(screen.getByText("Iščem pot...")).toBeInTheDocument();
  });

  it("uspesen response nastavi routePath", () => {
    const onPathReceive = vi.fn();
    onPathReceive(routePath);

    expect(onPathReceive).toHaveBeenCalledWith(expect.objectContaining({ legs: expect.any(Array) }));
  });

  it("napacen response prikaze napako poti", () => {
    const onPathError = vi.fn();
    onPathError({ code: "HTTP_500", message: "Napaka strežnika" });

    expect(onPathError).toHaveBeenCalledWith({ code: "HTTP_500", message: "Napaka strežnika" });
  });

  it("ze izracunana pot preklopi gumb v Zacni", () => {
    render(
      <RouteControls
        useBus
        useBike
        timeMode='depart'
        selectedTime='08:15'
        hasRoute
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

    expect(screen.getByRole("button", { name: "Začni" })).toBeEnabled();
  });

  it("aktivna pot preklopi gumb v Koncaj", () => {
    const onRouteRequest = vi.fn();
    render(
      <RouteControls
        useBus
        useBike
        timeMode='depart'
        selectedTime='08:15'
        hasRoute
        isRouteActive
        originCoords={null}
        destinationCoords={null}
        onToggleBus={vi.fn()}
        onToggleBike={vi.fn()}
        onToggleTimeMode={vi.fn()}
        onSelectedTimeChange={vi.fn()}
        onRouteRequest={onRouteRequest}
        onSavedRoutesToggle={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Končaj" }));
    expect(onRouteRequest).toHaveBeenCalledTimes(1);
  });
});
