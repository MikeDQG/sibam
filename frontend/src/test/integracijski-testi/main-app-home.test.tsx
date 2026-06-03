import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "../../components/ThemeProvider";

const integrationData = vi.hoisted(() => ({
  routePath: {
    origin: { lat: 46.5547, lng: 15.6459 },
    destination: { lat: 46.562, lng: 15.65 },
    legs: [
      {
        mode: "WALK",
        duration: "300000",
        distance: "350",
        polyline: [
          { lat: 46.5547, lon: 15.6459 },
          { lat: 46.555, lon: 15.646 },
        ],
        steps: [
          {
            instruction: "Pojdi proti postaji",
            startPolylineIndex: 0,
            endPolylineIndex: 1,
          },
        ],
      },
    ],
  },
  session: {
    id: "123e4567-e89b-12d3-a456-426614174000",
    email: "session@example.com",
    name: "Test User",
  },
}));

const sessionMock = vi.hoisted(() => ({
  userSession: integrationData.session,
  getAuthToken: vi.fn().mockResolvedValue("id-token"),
  fetchUserSession: vi.fn().mockResolvedValue(integrationData.session),
}));

vi.mock("../../components/Authorization/UserSessionProvider", () => ({
  useUserSession: () => sessionMock,
}));

vi.mock("sonner", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
}));

vi.mock("../../components/MainAppComponents/MainMap", () => ({
  MainMap: (props: {
    center: { lat: number; lng: number };
    zoom: number;
    userLocationPosition?: { lat: number; lng: number } | null;
    mapLocationDraft?: {
      position: { lat: number; lng: number };
      name: string;
      color: string;
      icon: "home";
    } | null;
    savedLocations?: { id: string; name: string }[];
    onMapContextSelect?: (position: { lat: number; lng: number }) => void;
    onMapLocationSave?: (draft: {
      position: { lat: number; lng: number };
      name: string;
      color: string;
      icon: "home";
    }) => void;
    onSavedLocationDelete?: (locationId: string) => void;
  }) => (
    <div
      data-testid='main-map'
      data-center={JSON.stringify(props.center)}
      data-zoom={String(props.zoom)}
      data-user-location={JSON.stringify(props.userLocationPosition ?? null)}>
      <button
        type='button'
        onClick={() => props.onMapContextSelect?.({ lat: 46.5547, lng: 15.6459 })}>
        Desni klik zemljevida
      </button>
      {props.mapLocationDraft && (
        <button
          type='button'
          onClick={() =>
            props.onMapLocationSave?.({
              ...props.mapLocationDraft!,
              name: "Dom",
            })
          }>
          Shrani draft lokacijo
        </button>
      )}
      {props.savedLocations?.map((location) => (
        <button
          key={location.id}
          type='button'
          onClick={() => props.onSavedLocationDelete?.(location.id)}>
          Izbriši {location.name}
        </button>
      ))}
    </div>
  ),
}));

vi.mock("../../components/MainAppComponents/MainAppControlOverlay", () => ({
  MainAppControlOverlay: (props: {
    hasRoute?: boolean;
    isRouteActive?: boolean;
    onPathReceive?: (path: typeof integrationData.routePath) => void;
    onPathError?: (error: { code: string; message?: string }) => void;
    onStartRoute?: () => void;
    onEndRoute?: () => void;
  }) => (
    <div>
      <span data-testid='overlay-route-state'>
        {props.hasRoute ? "has-route" : "no-route"}{" "}
        {props.isRouteActive ? "active" : "inactive"}
      </span>
      <button type='button' onClick={() => props.onPathReceive?.(integrationData.routePath)}>
        Vrni izračunano pot
      </button>
      <button
        type='button'
        onClick={() => props.onPathError?.({ code: "NO_ROUTE", message: "Ni poti." })}>
        Vrni napako poti
      </button>
      <button type='button' onClick={props.onStartRoute}>
        Začni sledenje
      </button>
      <button type='button' onClick={props.onEndRoute}>
        Končaj sledenje
      </button>
    </div>
  ),
}));

import { toast } from "sonner";
import { MainAppHome } from "../../components/Pages/MainAppHome";

const locationId = "123e4567-e89b-12d3-a456-426614174201";

function mockGeolocation(position = { latitude: 46.5547, longitude: 15.6459 }) {
  const getCurrentPosition = vi.fn((success: PositionCallback) =>
    success({
      coords: {
        latitude: position.latitude,
        longitude: position.longitude,
        accuracy: 5,
      },
    } as GeolocationPosition),
  );
  const watchPosition = vi.fn((success: PositionCallback) => {
    success({
      coords: {
        latitude: position.latitude,
        longitude: position.longitude,
        accuracy: 5,
      },
    } as GeolocationPosition);
    return 77;
  });
  const clearWatch = vi.fn();

  Object.defineProperty(navigator, "geolocation", {
    configurable: true,
    value: {
      getCurrentPosition,
      watchPosition,
      clearWatch,
    },
  });

  return { getCurrentPosition, watchPosition, clearWatch };
}

function mockHomeFetch() {
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string, init?: RequestInit) => {
      if (init?.method === "POST" && url.endsWith("/api/locations")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              id: locationId,
              name: "Dom",
              latitude: 46.5547,
              longitude: 15.6459,
              logo: "home",
            }),
        });
      }

      if (init?.method === "DELETE") {
        return Promise.resolve({ ok: true, status: 204 });
      }

      if (url.includes("/api/locations/")) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }

      if (url.includes("/api/paths/")) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }

      if (url.endsWith("/api/paths")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              id: "123e4567-e89b-12d3-a456-426614174202",
              name: "Pot domov",
              journey: integrationData.routePath,
            }),
        });
      }

      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    }),
  );
}

function renderHome() {
  return render(
    <ThemeProvider>
      <MainAppHome />
    </ThemeProvider>,
  );
}

describe("integracijski MainAppHome", () => {
  it("geolokacija nastavi uporabnikovo lokacijo v zemljevid", async () => {
    mockGeolocation();
    mockHomeFetch();

    renderHome();

    await waitFor(() =>
      expect(screen.getByTestId("main-map")).toHaveAttribute(
        "data-user-location",
        JSON.stringify({ lat: 46.5547, lng: 15.6459 }),
      ),
    );
  });

  it("onPathReceive iz overlaya prikaže navodila in spremeni stanje overlayja", async () => {
    mockGeolocation();
    mockHomeFetch();
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Vrni izračunano pot" }));

    expect(await screen.findByText("Pojdi proti postaji")).toBeInTheDocument();
    expect(screen.getByTestId("overlay-route-state")).toHaveTextContent("has-route");
  });

  it("onPathError iz overlaya prikaže RouteErrorBox", async () => {
    mockGeolocation();
    mockHomeFetch();
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Vrni napako poti" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("NO_ROUTE");
    expect(screen.getByText("Ni poti.")).toBeInTheDocument();
  });

  it("desni klik na zemljevid in uspešen POST /api/locations dodata lokacijo", async () => {
    mockGeolocation();
    mockHomeFetch();
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Desni klik zemljevida" }));
    fireEvent.click(await screen.findByRole("button", { name: "Shrani draft lokacijo" }));

    await waitFor(() => expect(toast.success).toHaveBeenCalledWith("Lokacija je shranjena."));
    expect(fetch).toHaveBeenCalledWith(
      "https://api.test/api/locations",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          "Content-Type": "application/json",
          Authorization: "Bearer id-token",
        }),
      }),
    );
    expect(await screen.findByRole("button", { name: "Izbriši Dom" })).toBeInTheDocument();
  });
});
