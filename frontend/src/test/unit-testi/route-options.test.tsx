import { describe, expect, it, vi } from "vitest";
import { RouteOptions } from "../../components/MainAppComponents/RouteOptions";
import { fireEvent, render, routeLegs, routeOptions, screen, waitFor } from "../frontendPlanFixtures";

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
    render(<RouteOptions routes={[]} legs={[]} isSavedRoute />);

    expect(screen.getByText("Navodila za to shranjeno pot niso na voljo.")).toBeInTheDocument();
  });

  it("shrani pot z vnesenim imenom in zaklene gumb med shranjevanjem", async () => {
    const onSaveRoute = vi.fn().mockResolvedValue(undefined);
    render(
      <RouteOptions
        routes={routeOptions}
        legs={routeLegs}
        canSaveRoute
        onSaveRoute={onSaveRoute}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Shrani pot" }));
    expect(screen.getByRole("button", { name: "Shrani" })).toBeDisabled();

    fireEvent.change(screen.getByRole("textbox", { name: "Ime poti" }), {
      target: { value: "  Pot domov  " },
    });
    fireEvent.click(screen.getByRole("button", { name: "Shrani" }));

    await waitFor(() => expect(onSaveRoute).toHaveBeenCalledWith("Pot domov"));
    expect(screen.getByRole("button", { name: "Shrani pot" })).toBeInTheDocument();
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
