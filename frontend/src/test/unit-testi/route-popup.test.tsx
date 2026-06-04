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

  it("razdaljo nad kilometrom formatira v kilometre", () => {
    render(<RoutePopup selectedLeg={{ leg: { ...routeLegs[0], distance: "2450" }, position: { lat: 1, lng: 1 }, source: "path" }} />);

    expect(screen.getByText("2.5 km")).toBeInTheDocument();
  });

  it("avtobusni popup prikaze odhod in napoved zamude", () => {
    render(<RoutePopup selectedLeg={{ leg: routeLegs[1], position: { lat: 1, lng: 1 }, source: "busIcon" }} />);

    expect(screen.getByText("Odhod avtobusa")).toBeInTheDocument();
    expect(screen.getByText("Pričakovana zamuda (min)")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
  });

  it("avtobusno zamudo pod polno minuto zaokrozi navzdol", () => {
    render(
      <RoutePopup
        selectedLeg={{
          leg: {
            ...routeLegs[1],
            busDelayPrediction: { predictedBoardingDelaySeconds: 59 },
          },
          position: { lat: 1, lng: 1 },
          source: "busIcon",
        }}
      />,
    );

    expect(screen.getByText("Pričakovana zamuda (min)")).toBeInTheDocument();
    expect(screen.getByText("0")).toBeInTheDocument();
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

  it("bike return popup prikaze stojala in rumeno verjetnost", () => {
    render(
      <RoutePopup
        selectedLeg={{
          leg: { ...routeLegs[2], bikePrediction: { ...routeLegs[2].bikePrediction, returnStandAvailableProbability: 0.65 } },
          previousLeg: routeLegs[2],
          position: { lat: 1, lng: 1 },
          source: "bikeReturnIcon",
        }}
      />,
    );

    expect(screen.getByText("Mesta za oddajo kolesa")).toBeInTheDocument();
    expect(screen.getByText("65 %")).toHaveClass("text-yellow-600");
  });

  it("bus po vrnitvi kolesa uporabi podatke prejsnjega bike lega", () => {
    render(
      <RoutePopup
        selectedLeg={{
          leg: routeLegs[1],
          previousLeg: { ...routeLegs[2], bikePrediction: { ...routeLegs[2].bikePrediction, returnStandAvailableProbability: 0.2 } },
          position: { lat: 1, lng: 1 },
          source: "busIcon",
        }}
      />,
    );

    expect(screen.getByText("Mesta za oddajo kolesa")).toBeInTheDocument();
    expect(screen.getByText("20 %")).toHaveClass("text-red-600");
  });

  it("nizko-srednjo verjetnost prikaze oranzno in neveljavne napovedi skrije", () => {
    render(
      <RoutePopup
        selectedLeg={{
          leg: {
            ...routeLegs[2],
            bikePrediction: {
              pickupBikeAvailableProbability: 0.3,
              predictedBikesAtPickup: Number.NaN,
            },
          },
          position: { lat: 1, lng: 1 },
          source: "bikePickupIcon",
        }}
      />,
    );

    expect(screen.getByText("30 %")).toHaveClass("text-orange-600");
    expect(screen.queryByText("Predvideno št. prostih koles ob tvojem prihodu")).not.toBeInTheDocument();
  });

  it("neveljavne vrednosti trajanja, razdalje in casa skrije", () => {
    render(
      <RoutePopup
        selectedLeg={{
          leg: {
            ...routeLegs[1],
            duration: "ni-stevilka",
            distance: "ni-stevilka",
            departure: "ni-stevilka",
            busDelayPrediction: { predictedBoardingDelaySeconds: Number.NaN },
          },
          position: { lat: 1, lng: 1 },
          source: "path",
        }}
      />,
    );

    expect(screen.queryByText("Trajanje")).not.toBeInTheDocument();
    expect(screen.queryByText("Pot")).not.toBeInTheDocument();
  });

  it("popup se zapre na close akcijo", () => {
    const onClose = vi.fn();
    render(<RoutePopup selectedLeg={{ leg: routeLegs[0], position: { lat: 1, lng: 1 }, source: "path" }} onClose={onClose} />);

    fireEvent.click(screen.getByRole("button", { name: "Zapri podatke o poti" }));
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
