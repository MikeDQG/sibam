import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { RoutePolyline } from "../../components/MainAppComponents/RoutePolyline";
import { routeLegs } from "../frontendPlanFixtures";

type ClickHandler = (event: {
  latLng?: { toJSON: () => { lat: number; lng: number } };
}) => void;

type GoogleMapsMock = {
  google: {
    maps: {
      Polyline: unknown;
      LatLngBounds: unknown;
      SymbolPath: { CIRCLE: string };
    };
  };
};

const polylineState = vi.hoisted(() => ({
  handlers: [] as ClickHandler[],
  removeListeners: [] as ReturnType<typeof vi.fn>[],
  setMapCalls: [] as unknown[],
}));

vi.mock("@vis.gl/react-google-maps", () => ({
  useMap: () => ({
    fitBounds: vi.fn(),
  }),
}));

describe("RoutePolyline", () => {
  beforeEach(() => {
    polylineState.handlers = [];
    polylineState.removeListeners = [];
    polylineState.setMapCalls = [];

    (globalThis as typeof globalThis & GoogleMapsMock).google.maps.Polyline = vi.fn(function Polyline() {
      return {
        setMap: vi.fn((map: unknown) => polylineState.setMapCalls.push(map)),
        addListener: vi.fn((_eventName: string, handler: ClickHandler) => {
          const remove = vi.fn();
          polylineState.handlers.push(handler);
          polylineState.removeListeners.push(remove);
          return { remove };
        }),
      };
    });
    (globalThis as typeof globalThis & GoogleMapsMock).google.maps.LatLngBounds = vi.fn(function LatLngBounds() {
      return {
        extend: vi.fn(),
        isEmpty: vi.fn(() => false),
      };
    });
    (globalThis as typeof globalThis & GoogleMapsMock).google.maps.SymbolPath = {
      CIRCLE: "circle",
    };
  });

  it("klik na polyline vrne latLng iz dogodka", () => {
    const onLegClick = vi.fn();
    render(<RoutePolyline legs={routeLegs} onLegClick={onLegClick} />);

    polylineState.handlers[0]({
      latLng: { toJSON: () => ({ lat: 46.55, lng: 15.65 }) },
    });

    expect(onLegClick).toHaveBeenCalledWith(routeLegs[0], {
      lat: 46.55,
      lng: 15.65,
    });
  });

  it("klik brez latLng uporabi prvo tocko lega kot fallback in pocisti listenerje", () => {
    const onLegClick = vi.fn();
    const { unmount } = render(
      <RoutePolyline legs={routeLegs} onLegClick={onLegClick} />,
    );

    polylineState.handlers[0]({});
    unmount();

    expect(onLegClick).toHaveBeenCalledWith(routeLegs[0], {
      lat: routeLegs[0].polyline[0].lat,
      lng: routeLegs[0].polyline[0].lon,
    });
    expect(polylineState.removeListeners[0]).toHaveBeenCalled();
    expect(polylineState.setMapCalls).toContain(null);
  });

  it("ne registrira click listenerja za neinteraktivno polyline", () => {
    render(<RoutePolyline legs={routeLegs} interactive={false} />);

    expect(polylineState.handlers).toHaveLength(0);
  });
});
