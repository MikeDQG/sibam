import { describe, expect, it, vi } from "vitest";
import { RoutePopup } from "../../components/MainAppComponents/RoutePopup";
import {
  fireEvent,
  render,
  routeLegs,
  screen,
} from "../frontendPlanFixtures";

describe("route-popup", () => {
  it("klik na WALK/BUS/BIKE odsek prikaze pravilen naslov", () => {
    render(<RoutePopup selectedLeg={{ leg: routeLegs[0], position: { lat: 1, lng: 1 }, source: "path" }} />);

    expect(screen.getByText("Peš")).toBeInTheDocument();
  });

  it("prikaz trajanja in razdalje odseka", () => {
    render(<RoutePopup selectedLeg={{ leg: routeLegs[0], position: { lat: 1, lng: 1 }, source: "path" }} />);

    expect(screen.getByText("Trajanje")).toBeInTheDocument();
    expect(screen.getByText("5 min")).toBeInTheDocument();
    expect(screen.getByText("350 m")).toBeInTheDocument();
  });

  it("avtobusni popup prikaze odhod in napoved zamude", () => {
    render(<RoutePopup selectedLeg={{ leg: routeLegs[1], position: { lat: 1, lng: 1 }, source: "busIcon" }} />);

    expect(screen.getByText("Odhod avtobusa")).toBeInTheDocument();
    expect(screen.getByText("Pričakovana zamuda (min)")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
  });

  it("MBajk popup prikaze prosta kolesa ali stojala", () => {
    render(<RoutePopup selectedLeg={{ leg: routeLegs[2], position: { lat: 1, lng: 1 }, source: "bikePickupIcon" }} />);

    expect(screen.getByText("Prosta kolesa")).toBeInTheDocument();
    expect(screen.getByText("4")).toBeInTheDocument();
  });

  it("verjetnosti napovedi so pravilno formatirane in obarvane", () => {
    render(<RoutePopup selectedLeg={{ leg: routeLegs[2], position: { lat: 1, lng: 1 }, source: "bikePickupIcon" }} />);

    expect(screen.getByText("92 %")).toHaveClass("text-emerald-600");
  });

  it("popup se zapre na close akcijo", () => {
    const onClose = vi.fn();
    render(<RoutePopup selectedLeg={{ leg: routeLegs[0], position: { lat: 1, lng: 1 }, source: "path" }} onClose={onClose} />);

    fireEvent.click(screen.getByRole("button", { name: "Zapri podatke o poti" }));
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
