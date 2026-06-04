import { describe, expect, it, vi } from "vitest";
import { RouteOptions } from "../../components/MainAppComponents/RouteOptions";
import {
  alternativeRoutePath,
  fireEvent,
  render,
  routeLegs,
  routeOptions,
  routePath,
  screen,
  waitFor,
} from "../frontendPlanFixtures";

describe("route-options", () => {
  it("route sheet se odpre po izracunu poti", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} />);

    expect(screen.getByRole("button", { name: /poti/i })).toBeInTheDocument();
  });

  it("prikaz trajanja, razdalje in navodil", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} />);

    expect(screen.getByText("Najhitrejša pot")).toBeInTheDocument();
    expect(screen.getByText("31 min")).toBeInTheDocument();
    expect(screen.getByText("Brez kolesa")).toBeInTheDocument();
    expect(screen.getByText("35 min")).toBeInTheDocument();
    expect(screen.getByText("Pojdi proti postaji")).toBeInTheDocument();
  });

  it("klik na route card izbere pravo pot", () => {
    const onRouteSelect = vi.fn();
    render(
      <RouteOptions
        routes={routeOptions}
        legs={routeLegs}
        onRouteSelect={onRouteSelect}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /Brez kolesa/i }));

    expect(onRouteSelect).toHaveBeenCalledWith(alternativeRoutePath, 1);
  });

  it("aktivni card ima rdece ozadje, neaktivni pa sivo", () => {
    render(
      <RouteOptions
        routes={routeOptions}
        legs={routeLegs}
        activeRouteIndex={1}
      />,
    );

    expect(screen.getByRole("button", { name: /Brez kolesa/i })).toHaveClass(
      "bg-red-700",
    );
    expect(
      screen.getByRole("button", { name: /Najhitrejša pot/i }),
    ).toHaveClass("bg-neutral-100");
  });

  it("zapiranje in odpiranje sheeta", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} />);
    const toggle = screen.getByRole("button", { name: /poti/i });

    fireEvent.click(toggle);
    expect(toggle).toBeInTheDocument();
  });

  it("gumb Shrani pot je na voljo za izracunano pot", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} canSaveRoute />);

    expect(screen.getAllByRole("button", { name: "Shrani pot" })[0]).toBeEnabled();
  });

  it("pri shranjeni poti prikaze eno kartico brez gumba za shranjevanje", () => {
    render(<RouteOptions routes={[routePath]} legs={routeLegs} isSavedRoute />);

    expect(screen.getByText("Najhitrejša pot")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Shrani pot" })).not.toBeInTheDocument();
  });

  it("prikaze napako izracuna in odpre sheet", () => {
    render(
      <RouteOptions
        routes={routeOptions}
        computeError={{ code: "NO_ROUTE", message: "Ni poti." }}
      />,
    );

    expect(screen.getByRole("alert")).toHaveTextContent("NO_ROUTE");
    expect(screen.getByText("Ni poti.")).toBeInTheDocument();
  });

  it("za shranjeno pot brez navodil prikaze prazno stanje navodil", () => {
    render(<RouteOptions routes={[routePath]} legs={[]} isSavedRoute />);

    expect(screen.getByText("Navodila za to shranjeno pot niso na voljo.")).toBeInTheDocument();
  });

  it("shrani pot z vnesenim imenom za izbrani card in zaklene gumb med shranjevanjem", async () => {
    const onSaveRoute = vi.fn().mockResolvedValue(undefined);
    render(
      <RouteOptions
        routes={routeOptions}
        legs={routeLegs}
        canSaveRoute
        onSaveRoute={onSaveRoute}
      />,
    );

    fireEvent.click(screen.getAllByRole("button", { name: "Shrani pot" })[1]);
    expect(screen.getByRole("button", { name: "Shrani" })).toBeDisabled();

    fireEvent.change(screen.getByRole("textbox", { name: "Ime poti" }), {
      target: { value: "  Pot domov  " },
    });
    fireEvent.click(screen.getByRole("button", { name: "Shrani" }));

    await waitFor(() =>
      expect(onSaveRoute).toHaveBeenCalledWith(
        "Pot domov",
        alternativeRoutePath,
      ),
    );
    expect(screen.getAllByRole("button", { name: "Shrani pot" })[0]).toBeInTheDocument();
  });

  it("drag sheeta ne sprozi toggla ob pointer premiku", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} />);
    const toggle = screen.getByRole("button", { name: /poti/i });
    const captures = new Set<number>();

    Object.assign(toggle, {
      setPointerCapture: vi.fn((id: number) => captures.add(id)),
      hasPointerCapture: vi.fn((id: number) => captures.has(id)),
      releasePointerCapture: vi.fn((id: number) => captures.delete(id)),
    });

    fireEvent.pointerDown(toggle, { pointerId: 1, clientY: 100 });
    fireEvent.pointerMove(toggle, { pointerId: 1, clientY: 140 });
    fireEvent.pointerUp(toggle, { pointerId: 1 });
    fireEvent.click(toggle);

    expect(toggle).toBeInTheDocument();
  });
});
