import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactElement } from "react";
import { ThemeProvider } from "../components/ThemeProvider";
import type { RouteLeg, RoutePath } from "../components/MainAppComponents/RoutePolyline";
import type { PlacesAutocomplete } from "../components/MainAppComponents/MainAppControlOverlayComponents/types";
import { getInstructionText } from "../lib/text";
import { vi } from "vitest";

export { fireEvent, render, screen, waitFor };

export const mariborCenter = { lat: 46.5547, lng: 15.6459 };
export const tabor = { lat: 46.562, lng: 15.65 };

export const routeLegs: RouteLeg[] = [
  {
    mode: "WALK",
    duration: "300000",
    distance: "350",
    polyline: [
      { lat: 46.5547, lon: 15.6459 },
      { lat: 46.555, lon: 15.646 },
    ],
    steps: [
      { instruction: "<b>Pojdi</b> proti postaji", startPolylineIndex: 0, endPolylineIndex: 1 },
      { instruction: "", startPolylineIndex: 1, endPolylineIndex: 1 },
    ],
  },
  {
    mode: "BUS",
    duration: "900000",
    distance: "1800",
    code: "6",
    headsignName: "Bresternica - Avtobusna postaja",
    departure: "1767355200000",
    busDelayPrediction: { predictedBoardingDelaySeconds: 180 },
    polyline: [
      { lat: 46.555, lon: 15.646 },
      { lat: 46.56, lon: 15.649 },
    ],
    steps: [{ instruction: "Pelji se z avtobusom", startPolylineIndex: 0, endPolylineIndex: 1 }],
  },
  {
    mode: "BIKE",
    duration: "660000",
    distance: "300",
    freeBikes: "4",
    freeStands: "7",
    bikePrediction: {
      pickupBikeAvailableProbability: 0.92,
      predictedBikesAtPickup: 3.4,
      predictedStandsAtReturn: 5.2,
      returnStandAvailableProbability: 0.65,
    },
    polyline: [
      { lat: 46.56, lon: 15.649 },
      { lat: 46.562, lon: 15.65 },
    ],
    steps: [{ instruction: "Vzemi kolo", startPolylineIndex: 0, endPolylineIndex: 1 }],
  },
];

export const routePath: RoutePath = {
  origin: mariborCenter,
  destination: tabor,
  label: "Najhitrejša pot",
  rank: 1,
  totalDurationSeconds: 1860,
  totalDistanceMeters: 2450,
  duration: "1860000",
  distance: "2450",
  modes: ["WALK", "BUS", "BIKE", "WALK"],
  legs: routeLegs,
};

export const alternativeRoutePath: RoutePath = {
  ...routePath,
  label: "Brez kolesa",
  rank: 2,
  totalDurationSeconds: 2100,
  totalDistanceMeters: 3200,
  duration: "2100000",
  distance: "3200",
  modes: ["WALK", "BUS"],
  legs: [routeLegs[0], routeLegs[1]],
};

export const savedRoute = {
  id: "route-1",
  name: "Domov iz centra",
  journey: routePath,
  duration: "1860000",
  distance: "2450",
  originLabel: "Glavni trg",
  destinationLabel: "Tabor",
  modes: ["WALK", "BUS", "BIKE"],
  createdAt: "2026-05-21T12:00:00.000Z",
};

export const savedLocation = {
  id: "location-1",
  name: "Fakulteta",
  position: mariborCenter,
  color: "#2563eb",
  icon: "school" as const,
};

export const routeOptions = [
  routePath,
  alternativeRoutePath,
];

export function renderWithTheme(ui: ReactElement) {
  return render(<ThemeProvider>{ui}</ThemeProvider>);
}

export function createAutocomplete(
  value = "",
  overrides: Partial<PlacesAutocomplete> = {},
) {
  return {
    value,
    predictions: [],
    isOpen: false,
    setValue: vi.fn(),
    setIsOpen: vi.fn(),
    handleChange: vi.fn(),
    clear: vi.fn(),
    closeDropdown: vi.fn(),
    ...overrides,
  } as unknown as PlacesAutocomplete;
}

export function visibleRouteSteps(legs: RouteLeg[]) {
  return legs.flatMap((leg) =>
    (leg.steps ?? [])
      .map((step) => ({
        mode: leg.mode,
        instruction: getInstructionText(step.instruction),
        startPolylineIndex: step.startPolylineIndex,
        endPolylineIndex: step.endPolylineIndex,
      }))
      .filter((step) => step.instruction.length > 0),
  );
}

export function buildComputeParams({
  timeMode = "depart",
  selectedTime = "08:15",
  useBike = true,
  useBus = true,
  userId,
}: {
  timeMode?: "depart" | "arrive";
  selectedTime?: string;
  useBike?: boolean;
  useBus?: boolean;
  userId?: string;
}) {
  const params = new URLSearchParams({
    originLat: String(mariborCenter.lat),
    originLon: String(mariborCenter.lng),
    destinationLat: String(tabor.lat),
    destinationLon: String(tabor.lng),
    originAddress: "Glavni trg",
    destinationAddress: "Tabor",
    leaveNow: "false",
    bike: String(useBike),
    bus: String(useBus),
  });

  params.set(timeMode === "depart" ? "leaveAt" : "arriveBy", selectedTime);
  if (userId) params.set("userId", userId);
  return params;
}

export function findActiveStepIndex(userPointIndex: number, legs = routeLegs) {
  const steps = visibleRouteSteps(legs);
  return steps.findIndex(
    (step) =>
      typeof step.startPolylineIndex === "number" &&
      typeof step.endPolylineIndex === "number" &&
      userPointIndex >= step.startPolylineIndex &&
      userPointIndex <= step.endPolylineIndex,
  );
}

export function createLocationPayload() {
  return {
    userId: "user-1",
    name: "Dom",
    address: "Glavni trg",
    position: mariborCenter,
    color: "#b91c1c",
    icon: "home",
  };
}

export function createPathPayload() {
  return {
    userId: "user-1",
    name: "Pot domov",
    journey: routePath,
  };
}
