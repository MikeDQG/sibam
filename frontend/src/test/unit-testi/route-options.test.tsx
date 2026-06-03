import { describe, expect, it } from "vitest";
import { RouteOptions } from "../../components/MainAppComponents/RouteOptions";
import { fireEvent, render, routeLegs, routeOptions, screen } from "../frontendPlanFixtures";

describe("route-options", () => {
  it("route sheet se odpre po izracunu poti", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} />);

    expect(screen.getByRole("button", { name: /poti/i })).toBeInTheDocument();
  });

  it("prikaz trajanja, razdalje in navodil", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} />);

    expect(screen.getByText("Najhitrejša pot")).toBeInTheDocument();
    expect(screen.getByText("31 min")).toBeInTheDocument();
    expect(screen.getByText("Pojdi proti postaji")).toBeInTheDocument();
  });

  it("zapiranje in odpiranje sheeta", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} />);
    const toggle = screen.getByRole("button", { name: /poti/i });

    fireEvent.click(toggle);
    expect(toggle).toBeInTheDocument();
  });

  it("gumb Shrani pot je na voljo za izracunano pot", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} canSaveRoute />);

    expect(screen.getByRole("button", { name: "Shrani pot" })).toBeEnabled();
  });

  it("pri shranjeni poti brez variant se prikaze ustrezno sporocilo", () => {
    render(<RouteOptions routes={[]} legs={[]} isSavedRoute />);

    expect(screen.getByText("Različni načini za shranjeno pot niso na voljo.")).toBeInTheDocument();
  });

});
