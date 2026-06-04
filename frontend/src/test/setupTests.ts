import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { createElement, type ReactNode } from "react";
import { afterEach, vi } from "vitest";

vi.stubEnv("VITE_GOOGLE_MAPS_API_KEY", "test-google-maps-key");
vi.stubEnv("VITE_GOOGLE_MAPS_MAP_ID", "test-map-id");
vi.stubEnv("VITE_PLACES_API_KEY", "test-places-key");
vi.stubEnv("VITE_API_URL", "https://api.test");

Object.defineProperty(globalThis, "google", {
  writable: true,
  value: {
    maps: {
      Polyline: vi.fn(function Polyline() {
        return {
        setMap: vi.fn(),
        addListener: vi.fn(() => ({ remove: vi.fn() })),
        };
      }),
      LatLngBounds: vi.fn(function LatLngBounds() {
        return {
        extend: vi.fn(),
        isEmpty: vi.fn(() => false),
        };
      }),
      SymbolPath: {
        CIRCLE: "circle",
      },
    },
  },
});

if (!window.matchMedia) {
  Object.defineProperty(window, "matchMedia", {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });
}

vi.mock("@vis.gl/react-google-maps", () => ({
  APIProvider: ({ children }: { children?: ReactNode }) =>
    createElement("div", { "data-testid": "mock-api-provider" }, children),
  Map: ({
    children,
    center,
    defaultCenter,
    zoom,
    defaultZoom,
    onCameraChanged,
    onContextmenu,
  }: {
    children?: ReactNode;
    center?: unknown;
    defaultCenter?: unknown;
    zoom?: number;
    defaultZoom?: number;
    onCameraChanged?: (event: {
      detail: { center: { lat: number; lng: number }; zoom: number };
    }) => void;
    onContextmenu?: (event: {
      stop: () => void;
      detail: { latLng: { lat: number; lng: number } };
    }) => void;
  }) =>
    createElement(
      "div",
      {
        "data-testid": "mock-map",
        "data-center": JSON.stringify(center ?? defaultCenter),
        "data-zoom": String(zoom ?? defaultZoom),
        onClick: () =>
          onCameraChanged?.({
            detail: { center: { lat: 46.55, lng: 15.65 }, zoom: 14 },
          }),
        onContextMenu: (event: Event) => {
          event.preventDefault();
          onContextmenu?.({
            stop: vi.fn(),
            detail: { latLng: { lat: 46.56, lng: 15.66 } },
          });
        },
      },
      children,
    ),
  AdvancedMarker: ({
    children,
    position,
    onClick,
  }: {
    children?: ReactNode;
    position: unknown;
    onClick?: () => void;
  }) =>
    createElement(
      "div",
      {
        role: onClick ? "button" : undefined,
        tabIndex: onClick ? 0 : undefined,
        "data-testid": "mock-marker",
        "data-position": JSON.stringify(position),
        onClick,
      },
      children,
    ),
  InfoWindow: ({
    children,
    position,
    onCloseClick,
  }: {
    children?: ReactNode;
    position: unknown;
    onCloseClick?: () => void;
  }) =>
    createElement(
      "div",
      { "data-testid": "mock-info-window", "data-position": JSON.stringify(position) },
      children,
      createElement("button", { type: "button", onClick: onCloseClick }, "Zapri"),
    ),
  useMap: () => ({
    fitBounds: vi.fn(),
  }),
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});
