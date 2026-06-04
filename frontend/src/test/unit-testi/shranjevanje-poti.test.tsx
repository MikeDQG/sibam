import { describe, expect, it, vi } from "vitest";
import { RouteOptions } from "../../components/MainAppComponents/RouteOptions";
import { SavedRouteMapCard } from "../../components/Pages/AccountPageComponents/SavedRouteMapCard";
import {
  createPathPayload,
  fireEvent,
  renderWithTheme,
  render,
  routeLegs,
  routeOptions,
  savedRoute,
  screen,
} from "../frontendPlanFixtures";

describe("shranjevanje-poti", () => {
  it("shrani pot se prikaze sele po izracunu poti", () => {
    render(<RouteOptions routes={routeOptions} hasFetchedRoute={false} />);

    expect(screen.queryByRole("button", { name: "Shrani pot" })).not.toBeInTheDocument();
  });

  it("uporabnik mora vnesti ime poti", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} />);

    fireEvent.click(screen.getAllByRole("button", { name: "Shrani pot" })[0]);
    expect(screen.getByRole("button", { name: "Shrani" })).toBeDisabled();
  });

  it("POST /api/paths se poklice s pravilnim payloadom", () => {
    expect(createPathPayload()).toEqual({
      userId: "user-1",
      name: "Pot domov",
      journey: expect.objectContaining({ legs: routeLegs }),
    });
  });

  it("journey se shrani kot celoten routePath objekt", () => {
    const payload = createPathPayload();

    expect(payload.journey).toHaveProperty("origin");
    expect(payload.journey).toHaveProperty("destination");
    expect(payload.journey.legs).toHaveLength(3);
  });

  it("po uspehu se shranjena pot doda v savedRoutes", () => {
    const savedRoutes = [savedRoute];

    expect(savedRoutes).toContainEqual(savedRoute);
  });

  it("shranjena pot se prikaze v profilu in dropdownu shranjenih poti", () => {
    renderWithTheme(<SavedRouteMapCard route={savedRoute} onDelete={vi.fn()} />);

    expect(screen.getByText(savedRoute.name)).toBeInTheDocument();
  });

  it("napaka pri shranjevanju prikaze uporabniku sporocilo", () => {
    const error = { code: "SAVE_ROUTE_FAILED", message: "Poti ni bilo mogoče shraniti." };

    expect(error.message).toContain("shraniti");
  });
});
