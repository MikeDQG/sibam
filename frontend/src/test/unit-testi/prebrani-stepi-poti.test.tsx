import { describe, expect, it } from "vitest";
import { RouteOptions } from "../../components/MainAppComponents/RouteOptions";
import {
  render,
  routeLegs,
  routeOptions,
  screen,
  visibleRouteSteps,
} from "../frontendPlanFixtures";

describe("prebrani-stepi-poti", () => {
  it("stepi se preberejo iz vseh leg-ov v pravilnem vrstnem redu", () => {
    expect(visibleRouteSteps(routeLegs).map((step) => step.instruction)).toEqual([
      "Pojdi proti postaji",
      "Pelji se z avtobusom",
      "Vzemi kolo",
    ]);
  });

  it("HTML oznake v navodilih se odstranijo", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} />);

    expect(screen.getByText("Pojdi proti postaji")).toBeInTheDocument();
    expect(screen.queryByText("<b>Pojdi</b> proti postaji")).not.toBeInTheDocument();
  });

  it("prazni ali neveljavni stepi se ne prikazejo", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} />);

    expect(screen.getAllByRole("listitem")).toHaveLength(3);
  });

  it("vsak step prikaze pravilen nacin poti", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} />);

    expect(screen.getByText("Peš")).toBeInTheDocument();
    expect(screen.getAllByText("Bus").length).toBeGreaterThan(0);
    expect(screen.getByText("Kolo")).toBeInTheDocument();
  });

  it("seznam stepov ostane prikazan tudi brez aktivnega sledenja", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} activeStepIndex={null} />);

    expect(screen.getByText("Navodila za pot")).toBeInTheDocument();
    expect(screen.getByText("Vzemi kolo")).toBeInTheDocument();
  });
});
