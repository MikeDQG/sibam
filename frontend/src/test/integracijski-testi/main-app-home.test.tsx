import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
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
  savedRouteWithoutEndpoints: {
    id: "123e4567-e89b-12d3-a456-426614174204",
    name: "Pot brez endpointov",
    journey: {
      duration: "900000",
      distance: "1800",
      legs: [
        {
          mode: "BUS",
          duration: "900000",
          distance: "1800",
          polyline: [
            { lat: 46.5547, lon: 15.6459 },
            { lat: 46.562, lon: 15.65 },
          ],
          steps: [
            {
              instruction: "Pelji se brez endpointov",
              startPolylineIndex: "0",
              endPolylineIndex: "1",
            },
          ],
        },
      ],
    },
    createdAt: "2026-05-21T12:00:00.000Z",
  },
  savedRoute: {
    id: "123e4567-e89b-12d3-a456-426614174203",
    name: "Pot do Tabora",
    journey: {
      origin: { lat: 46.5547, lng: 15.6459 },
      destination: { lat: 46.562, lng: 15.65 },
      duration: "900000",
      distance: "1800",
      origin_address: "Glavni trg",
      destinationAddress: "Tabor",
      legs: [
        {
          mode: "BUS",
          duration: "900000",
          distance: "1800",
          polyline: [
            { lat: 46.5547, lon: 15.6459 },
            { lat: 46.562, lon: 15.65 },
          ],
          steps: [
            {
              instruction: "Pelji se do Tabora",
              startPolylineIndex: 0,
              endPolylineIndex: 1,
            },
          ],
        },
      ],
    },
    createdAt: "2026-05-21T12:00:00.000Z",
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
    legs?: typeof integrationData.routePath.legs;
    selectedLeg?: unknown;
    onCameraChanged?: (center: { lat: number; lng: number }, zoom: number) => void;
    onMapContextSelect?: (position: { lat: number; lng: number }) => void;
    onLegClick?: (
      leg: typeof integrationData.routePath.legs[number],
      position: { lat: number; lon: number },
    ) => void;
    onBusIconClick?: (
      leg: typeof integrationData.routePath.legs[number],
      position: { lat: number; lon: number },
      previousLeg?: typeof integrationData.routePath.legs[number],
    ) => void;
    onBikeIconClick?: (
      leg: typeof integrationData.routePath.legs[number],
      position: { lat: number; lon: number },
      source: "bikePickupIcon",
    ) => void;
    onRoutePopupClose?: () => void;
    onMapLocationColorChange?: (color: string) => void;
    onMapLocationIconChange?: (icon: "school") => void;
    onMapLocationSave?: (draft: {
      position: { lat: number; lng: number };
      name: string;
      color: string;
      icon: "home";
    }) => void;
    onMapLocationPopupClose?: () => void;
    onSavedLocationDelete?: (locationId: string) => void;
  }) => (
    <div
      data-testid='main-map'
      data-center={JSON.stringify(props.center)}
      data-zoom={String(props.zoom)}
      data-user-location={JSON.stringify(props.userLocationPosition ?? null)}>
      <button
        type='button'
        onClick={() => props.onCameraChanged?.({ lat: 46.56, lng: 15.66 }, 18)}>
        Premakni kamero
      </button>
      <button
        type='button'
        onClick={() => props.onMapContextSelect?.({ lat: 46.5547, lng: 15.6459 })}>
        Desni klik zemljevida
      </button>
      <button type='button' onClick={() => props.onMapLocationColorChange?.("#16a34a")}>
        Spremeni barvo brez drafta
      </button>
      <button type='button' onClick={() => props.onMapLocationIconChange?.("school")}>
        Spremeni ikono brez drafta
      </button>
      {props.legs?.[0] && (
        <>
          <button
            type='button'
            onClick={() => props.onLegClick?.(props.legs![0], { lat: 46.5547, lon: 15.6459 })}>
            Klikni leg
          </button>
          <button
            type='button'
            onClick={() =>
              props.onBusIconClick?.(props.legs![0], { lat: 46.5547, lon: 15.6459 })
            }>
            Klikni bus ikono
          </button>
          <button
            type='button'
            onClick={() =>
              props.onBikeIconClick?.(props.legs![0], { lat: 46.5547, lon: 15.6459 }, "bikePickupIcon")
            }>
            Klikni kolo ikono
          </button>
        </>
      )}
      {Boolean(props.selectedLeg) && (
        <button type='button' onClick={props.onRoutePopupClose}>
          Zapri route popup
        </button>
      )}
      {props.mapLocationDraft && (
        <>
          <span data-testid='draft-location'>
            {props.mapLocationDraft.color} {props.mapLocationDraft.icon}
          </span>
          <button type='button' onClick={() => props.onMapLocationColorChange?.("#2563eb")}>
            Spremeni barvo lokacije
          </button>
          <button type='button' onClick={() => props.onMapLocationIconChange?.("school")}>
            Spremeni ikono lokacije
          </button>
          <button type='button' onClick={props.onMapLocationPopupClose}>
            Zapri draft lokacijo
          </button>
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
        </>
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
    onZoomIn?: () => void;
    onZoomOut?: () => void;
    onLocate?: () => void;
    onPlaceSelect?: (place: { lat: number; lng: number } | null) => void;
    onDestinationSelect?: (place: { lat: number; lng: number } | null) => void;
    onStartRoute?: () => void;
    onEndRoute?: () => void;
    savedRoutes?: Array<typeof integrationData.savedRoute | typeof integrationData.savedRouteWithoutEndpoints>;
    onSavedRouteSelect?: (
      route: typeof integrationData.savedRoute | typeof integrationData.savedRouteWithoutEndpoints,
    ) => void;
  }) => (
    <div>
      <span data-testid='overlay-route-state'>
        {props.hasRoute ? "has-route" : "no-route"}{" "}
        {props.isRouteActive ? "active" : "inactive"}
      </span>
      <button type='button' onClick={props.onZoomIn}>
        Približaj
      </button>
      <button type='button' onClick={props.onZoomOut}>
        Oddalji
      </button>
      <button type='button' onClick={props.onLocate}>
        Najdi mojo lokacijo
      </button>
      <button
        type='button'
        onClick={() => props.onPlaceSelect?.({ lat: 46.557, lng: 15.646 })}>
        Izberi izhodišče
      </button>
      <button type='button' onClick={() => props.onPlaceSelect?.(null)}>
        Počisti izhodišče
      </button>
      <button
        type='button'
        onClick={() => props.onDestinationSelect?.({ lat: 46.562, lng: 15.65 })}>
        Izberi cilj
      </button>
      <button type='button' onClick={() => props.onPathReceive?.(integrationData.routePath)}>
        Vrni izračunano pot
      </button>
      <button
        type='button'
        onClick={() =>
          props.onPathReceive?.({
            ...integrationData.routePath,
            legs: [
              {
                ...integrationData.routePath.legs[0],
                mode: "BIKE",
                steps: [
                  {
                    instruction: "Pelji s kolesom",
                    startPolylineIndex: 0,
                    endPolylineIndex: 1,
                  },
                ],
              },
            ],
          })
        }>
        Vrni kolesarsko pot
      </button>
      <button
        type='button'
        onClick={() =>
          props.onPathReceive?.({
            ...integrationData.routePath,
            legs: [
              {
                ...integrationData.routePath.legs[0],
                mode: "SCOOTER",
                steps: [
                  {
                    instruction: "Pelji drugace",
                    startPolylineIndex: 0,
                    endPolylineIndex: 1,
                  },
                ],
              },
            ],
          })
        }>
        Vrni neznano pot
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
      {props.savedRoutes?.map((route) => (
        <button key={route.id} type='button' onClick={() => props.onSavedRouteSelect?.(route)}>
          Izberi shranjeno pot {route.name}
        </button>
      ))}
    </div>
  ),
}));

vi.mock("../../components/MainAppComponents/RouteOptions", () => ({
  RouteOptions: (props: {
    legs?: typeof integrationData.routePath.legs;
    computeError?: { code: string; message?: string } | null;
    canSaveRoute?: boolean;
    hasFetchedRoute?: boolean;
    isSavedRoute?: boolean;
    activeStepIndex?: number | null;
    onSaveRoute?: (name: string) => Promise<void>;
  }) => (
    <div>
      <span data-testid='route-options-state'>
        {props.canSaveRoute ? "can-save" : "cannot-save"}{" "}
        {props.hasFetchedRoute ? "fetched" : "not-fetched"}{" "}
        {props.isSavedRoute ? "saved-route" : "computed-route"} active:
        {props.activeStepIndex ?? "none"}
      </span>
      {props.computeError && (
        <div role='alert'>
          {props.computeError.code} {props.computeError.message}
        </div>
      )}
      {props.legs?.flatMap((leg) =>
        leg.steps?.map((step) => <p key={step.instruction}>{step.instruction}</p>) ?? [],
      )}
      <button type='button' onClick={() => props.onSaveRoute?.("Pot domov")}>
        Shrani trenutno pot
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

function mockHomeFetch({
  token = "id-token",
  savedLocations = [],
  savedRoutes = [],
  fetchLocationsOk = true,
  fetchRoutesOk = true,
  postLocationOk = true,
  deleteLocationOk = true,
  postPathOk = true,
  postPathJourney = integrationData.routePath,
}: {
  token?: string | null;
  savedLocations?: unknown[];
  savedRoutes?: unknown[];
  fetchLocationsOk?: boolean;
  fetchRoutesOk?: boolean;
  postLocationOk?: boolean;
  deleteLocationOk?: boolean;
  postPathOk?: boolean;
  postPathJourney?: unknown;
} = {}) {
  sessionMock.getAuthToken.mockResolvedValue(token);
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string, init?: RequestInit) => {
      if (init?.method === "POST" && url.endsWith("/api/locations")) {
        if (!postLocationOk) {
          return Promise.resolve({ ok: false, statusText: "Bad Request" });
        }

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
        return Promise.resolve({ ok: deleteLocationOk, status: deleteLocationOk ? 204 : 500 });
      }

      if (url.includes("/api/locations/")) {
        return Promise.resolve({
          ok: fetchLocationsOk,
          status: fetchLocationsOk ? 200 : 500,
          json: () => Promise.resolve(savedLocations),
        });
      }

      if (url.includes("/api/paths/")) {
        return Promise.resolve({
          ok: fetchRoutesOk,
          status: fetchRoutesOk ? 200 : 500,
          json: () => Promise.resolve(savedRoutes),
        });
      }

      if (url.endsWith("/api/paths")) {
        if (!postPathOk) {
          return Promise.resolve({ ok: false, status: 500 });
        }

        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              id: "123e4567-e89b-12d3-a456-426614174202",
              name: "Pot domov",
              journey: postPathJourney,
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
  afterEach(() => {
    sessionMock.userSession = integrationData.session;
    sessionMock.getAuthToken.mockResolvedValue("id-token");
    sessionMock.fetchUserSession.mockResolvedValue(integrationData.session);
  });

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

  it("geolokacija izven obmocja pri lociranju prikaze Maribor in toast samo enkrat", async () => {
    const { getCurrentPosition } = mockGeolocation({
      latitude: 46.7,
      longitude: 15.9,
    });
    mockHomeFetch();

    renderHome();

    await waitFor(() =>
      expect(screen.getByTestId("main-map")).toHaveAttribute(
        "data-center",
        JSON.stringify({ lat: 46.5547, lng: 15.6459 }),
      ),
    );

    fireEvent.click(screen.getByRole("button", { name: "Najdi mojo lokacijo" }));
    fireEvent.click(screen.getByRole("button", { name: "Najdi mojo lokacijo" }));

    await waitFor(() =>
      expect(toast.info).toHaveBeenCalledWith(
        "Trenutno si izven območja pokritosti. Prikazujemo Maribor.",
      ),
    );
    expect(toast.info).toHaveBeenCalledTimes(1);
    expect(getCurrentPosition).toHaveBeenCalledTimes(3);
  });

  it("napaka geolokacije pusti fallback center", async () => {
    const getCurrentPosition = vi.fn((_success: PositionCallback, error: PositionErrorCallback) =>
      error({} as GeolocationPositionError),
    );
    const watchPosition = vi.fn(() => 77);
    Object.defineProperty(navigator, "geolocation", {
      configurable: true,
      value: {
        getCurrentPosition,
        watchPosition,
        clearWatch: vi.fn(),
      },
    });
    mockHomeFetch();

    renderHome();

    await waitFor(() =>
      expect(screen.getByTestId("main-map")).toHaveAttribute(
        "data-center",
        JSON.stringify({ lat: 46.5547, lng: 15.6459 }),
      ),
    );
    expect(getCurrentPosition).toHaveBeenCalled();
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

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("NO_ROUTE");
    expect(alert).toHaveTextContent("Ni poti.");
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

  it("brisanje shranjene lokacije odstrani lokacijo in pokliče DELETE", async () => {
    mockGeolocation();
    mockHomeFetch({
      savedLocations: [
        {
          id: locationId,
          name: "Dom",
          latitude: 46.5547,
          longitude: 15.6459,
          color: "#b91c1c",
          logo: "home",
        },
      ],
    });
    renderHome();

    fireEvent.click(await screen.findByRole("button", { name: "Izbriši Dom" }));

    await waitFor(() => expect(toast.success).toHaveBeenCalledWith("Lokacija je izbrisana."));
    expect(fetch).toHaveBeenCalledWith(
      `https://api.test/api/locations/${locationId}`,
      expect.objectContaining({ method: "DELETE" }),
    );
  });

  it("neprijavljenemu uporabniku zavrne brisanje lokacije", async () => {
    mockGeolocation();
    mockHomeFetch({
      token: "id-token",
      savedLocations: [
        {
          id: locationId,
          name: "Dom",
          latitude: 46.5547,
          longitude: 15.6459,
          color: "#b91c1c",
          logo: "home",
        },
      ],
    });
    renderHome();
    await screen.findByRole("button", { name: "Izbriši Dom" });
    sessionMock.getAuthToken.mockResolvedValue(null);

    fireEvent.click(screen.getByRole("button", { name: "Izbriši Dom" }));

    await waitFor(() =>
      expect(toast.error).toHaveBeenCalledWith("Za brisanje lokacije moraš biti prijavljen."),
    );
  });

  it("kontrole zemljevida spreminjajo center, zoom in draft lokacijo", async () => {
    const { getCurrentPosition } = mockGeolocation();
    mockHomeFetch();
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Približaj" }));
    expect(screen.getByTestId("main-map")).toHaveAttribute("data-zoom", "15");

    fireEvent.click(screen.getByRole("button", { name: "Oddalji" }));
    expect(screen.getByTestId("main-map")).toHaveAttribute("data-zoom", "14");

    fireEvent.click(screen.getByRole("button", { name: "Premakni kamero" }));
    expect(screen.getByTestId("main-map")).toHaveAttribute(
      "data-center",
      JSON.stringify({ lat: 46.56, lng: 15.66 }),
    );

    fireEvent.click(screen.getByRole("button", { name: "Najdi mojo lokacijo" }));
    expect(getCurrentPosition).toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Desni klik zemljevida" }));
    expect(await screen.findByTestId("draft-location")).toHaveTextContent("#b91c1c home");

    fireEvent.click(screen.getByRole("button", { name: "Spremeni barvo lokacije" }));
    fireEvent.click(screen.getByRole("button", { name: "Spremeni ikono lokacije" }));
    expect(screen.getByTestId("draft-location")).toHaveTextContent("#2563eb school");

    fireEvent.click(screen.getByRole("button", { name: "Zapri draft lokacijo" }));
    expect(screen.queryByTestId("draft-location")).not.toBeInTheDocument();
  });

  it("izbira izhodisca, cilja in napaka ponastavijo stanje poti", async () => {
    mockGeolocation();
    mockHomeFetch();
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Vrni izračunano pot" }));
    expect(await screen.findByText("Pojdi proti postaji")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Izberi izhodišče" }));
    expect(screen.getByTestId("overlay-route-state")).toHaveTextContent("no-route");
    expect(screen.getByTestId("main-map")).toHaveAttribute(
      "data-center",
      JSON.stringify({ lat: 46.557, lng: 15.646 }),
    );

    fireEvent.click(screen.getByRole("button", { name: "Izberi cilj" }));
    expect(screen.getByTestId("overlay-route-state")).toHaveTextContent("no-route");

    fireEvent.click(screen.getByRole("button", { name: "Počisti izhodišče" }));
    expect(screen.getByTestId("overlay-route-state")).toHaveTextContent("no-route");
  });

  it("zacetek in konec sledenja prikazeta aktualni korak", async () => {
    mockGeolocation();
    mockHomeFetch();
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Vrni izračunano pot" }));
    fireEvent.click(screen.getByRole("button", { name: "Začni sledenje" }));

    expect(await screen.findByText("Aktualni korak")).toBeInTheDocument();
    expect(screen.getAllByText("Pojdi proti postaji")).toHaveLength(2);
    expect(screen.getByTestId("overlay-route-state")).toHaveTextContent("active");

    fireEvent.click(screen.getByRole("button", { name: "Končaj sledenje" }));
    expect(screen.queryByText("Aktualni korak")).not.toBeInTheDocument();
    expect(screen.getByTestId("overlay-route-state")).toHaveTextContent("inactive");
  });

  it("med sledenjem GPS update centrira zemljevid, locate gumb pa ne sprozi novega lociranja", async () => {
    let watchSuccess: PositionCallback | null = null;
    const getCurrentPosition = vi.fn((success: PositionCallback) =>
      success({
        coords: {
          latitude: 46.5547,
          longitude: 15.6459,
          accuracy: 5,
        },
      } as GeolocationPosition),
    );
    Object.defineProperty(navigator, "geolocation", {
      configurable: true,
      value: {
        getCurrentPosition,
        watchPosition: vi.fn((success: PositionCallback) => {
          watchSuccess = success;
          return 77;
        }),
        clearWatch: vi.fn(),
      },
    });
    mockHomeFetch();
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Vrni izračunano pot" }));
    fireEvent.click(screen.getByRole("button", { name: "Začni sledenje" }));
    const callsBeforeLocate = getCurrentPosition.mock.calls.length;
    fireEvent.click(screen.getByRole("button", { name: "Najdi mojo lokacijo" }));
    expect(getCurrentPosition).toHaveBeenCalledTimes(callsBeforeLocate);

    act(() => {
      watchSuccess?.({
        coords: {
          latitude: 46.555,
          longitude: 15.646,
          accuracy: 5,
        },
      } as GeolocationPosition);
    });

    await waitFor(() =>
      expect(screen.getByTestId("main-map")).toHaveAttribute(
        "data-center",
        JSON.stringify({ lat: 46.555, lng: 15.646 }),
      ),
    );
    expect(screen.getByTestId("main-map")).toHaveAttribute("data-zoom", "17");
  });

  it("podpira razlicne mode labele pri aktivnem koraku", async () => {
    mockGeolocation();
    mockHomeFetch();
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Vrni kolesarsko pot" }));
    fireEvent.click(screen.getByRole("button", { name: "Začni sledenje" }));
    expect(await screen.findByText("Kolo")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Vrni neznano pot" }));
    fireEvent.click(screen.getByRole("button", { name: "Začni sledenje" }));
    expect(await screen.findByText("SCOOTER")).toBeInTheDocument();
  });

  it("kliki na elemente poti odprejo in zaprejo route popup selection", async () => {
    mockGeolocation();
    mockHomeFetch();
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Vrni izračunano pot" }));
    fireEvent.click(await screen.findByRole("button", { name: "Klikni leg" }));
    expect(screen.getByRole("button", { name: "Zapri route popup" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Zapri route popup" }));
    expect(screen.queryByRole("button", { name: "Zapri route popup" })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Klikni bus ikono" }));
    expect(screen.getByRole("button", { name: "Zapri route popup" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Klikni kolo ikono" }));
    expect(screen.getByRole("button", { name: "Zapri route popup" })).toBeInTheDocument();
  });

  it("izbira shranjene poti nastavi stanje shranjene poti", async () => {
    mockGeolocation();
    mockHomeFetch({ savedRoutes: [integrationData.savedRoute] });
    renderHome();

    fireEvent.click(
      await screen.findByRole("button", { name: "Izberi shranjeno pot Pot do Tabora" }),
    );

    expect(screen.getByText("Pelji se do Tabora")).toBeInTheDocument();
    expect(screen.getByTestId("route-options-state")).toHaveTextContent("saved-route");
    expect(screen.getByTestId("overlay-route-state")).toHaveTextContent("has-route");
  });

  it("shranjena pot brez endpointov uporabi fallback iz polyline in bus labelo", async () => {
    mockGeolocation();
    mockHomeFetch({ savedRoutes: [integrationData.savedRouteWithoutEndpoints] });
    renderHome();

    fireEvent.click(
      await screen.findByRole("button", { name: "Izberi shranjeno pot Pot brez endpointov" }),
    );

    expect(screen.getByTestId("route-options-state")).toHaveTextContent("saved-route");
    fireEvent.click(screen.getByRole("button", { name: "Začni sledenje" }));
    expect(await screen.findByText("Bus")).toBeInTheDocument();
    expect(screen.getAllByText("Pelji se brez endpointov")).toHaveLength(2);
  });

  it("zacetek sledenja deluje tudi brez znane uporabnikove lokacije", async () => {
    Object.defineProperty(navigator, "geolocation", {
      configurable: true,
      value: undefined,
    });
    mockHomeFetch();
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Vrni izračunano pot" }));
    fireEvent.click(screen.getByRole("button", { name: "Začni sledenje" }));

    expect(screen.getByTestId("overlay-route-state")).toHaveTextContent("active");
    expect(screen.getByTestId("main-map")).toHaveAttribute("data-zoom", "14");
  });

  it("shranjevanje poti pošlje POST in doda shranjeno pot", async () => {
    mockGeolocation();
    mockHomeFetch();
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Vrni izračunano pot" }));
    fireEvent.click(screen.getByRole("button", { name: "Shrani trenutno pot" }));

    await waitFor(() => expect(toast.success).toHaveBeenCalledWith("Pot je shranjena."));
    expect(fetch).toHaveBeenCalledWith(
      "https://api.test/api/paths",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          "Content-Type": "application/json",
          Authorization: "Bearer id-token",
        }),
      }),
    );
  });

  it("pri manjkajoci seji jo pridobi pred nalaganjem in shranjevanjem", async () => {
    sessionMock.userSession = null as never;
    mockGeolocation();
    mockHomeFetch();
    renderHome();

    await waitFor(() => expect(sessionMock.fetchUserSession).toHaveBeenCalledWith("id-token"));
    fireEvent.click(screen.getByRole("button", { name: "Desni klik zemljevida" }));
    fireEvent.click(await screen.findByRole("button", { name: "Shrani draft lokacijo" }));
    fireEvent.click(screen.getByRole("button", { name: "Vrni izračunano pot" }));
    fireEvent.click(screen.getByRole("button", { name: "Shrani trenutno pot" }));

    await waitFor(() => expect(toast.success).toHaveBeenCalledWith("Pot je shranjena."));
    expect(sessionMock.fetchUserSession).toHaveBeenCalled();
  });

  it("ne doda poti, ce POST vrne neveljaven journey", async () => {
    mockGeolocation();
    mockHomeFetch({ postPathJourney: { legs: [{ mode: "WALK", polyline: [] }] } });
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Vrni izračunano pot" }));
    fireEvent.click(screen.getByRole("button", { name: "Shrani trenutno pot" }));

    await waitFor(() => expect(toast.success).toHaveBeenCalledWith("Pot je shranjena."));
    expect(
      screen.queryByRole("button", { name: "Izberi shranjeno pot Pot domov" }),
    ).not.toBeInTheDocument();
  });

  it("shranjevanje poti brez izracunane poti prikaze napako", async () => {
    mockGeolocation();
    mockHomeFetch();
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Shrani trenutno pot" }));

    await waitFor(() => expect(toast.error).toHaveBeenCalledWith("Najprej izračunaj pot."));
  });

  it("neprijavljenemu uporabniku zavrne shranjevanje lokacije in poti", async () => {
    mockGeolocation();
    mockHomeFetch({ token: null });
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Desni klik zemljevida" }));
    fireEvent.click(await screen.findByRole("button", { name: "Shrani draft lokacijo" }));
    await waitFor(() =>
      expect(toast.error).toHaveBeenCalledWith("Za shranjevanje lokacije moraš biti prijavljen."),
    );

    fireEvent.click(screen.getByRole("button", { name: "Vrni izračunano pot" }));
    fireEvent.click(screen.getByRole("button", { name: "Shrani trenutno pot" }));
    await waitFor(() =>
      expect(toast.error).toHaveBeenCalledWith("Za shranjevanje poti moraš biti prijavljen."),
    );
  });

  it("napake pri nalaganju in shranjevanju prikazejo toast sporocila", async () => {
    mockGeolocation();
    mockHomeFetch({ postLocationOk: false, postPathOk: false, deleteLocationOk: false });
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Desni klik zemljevida" }));
    fireEvent.click(await screen.findByRole("button", { name: "Shrani draft lokacijo" }));
    await waitFor(() =>
      expect(toast.error).toHaveBeenCalledWith("Lokacije ni bilo mogoče shraniti. Poskusite znova."),
    );

    fireEvent.click(screen.getByRole("button", { name: "Vrni izračunano pot" }));
    fireEvent.click(screen.getByRole("button", { name: "Shrani trenutno pot" }));
    await waitFor(() =>
      expect(toast.error).toHaveBeenCalledWith("Poti ni bilo mogoče shraniti. Poskusite znova."),
    );
  });

  it("napaka pri brisanju shranjene lokacije prikaze toast", async () => {
    mockGeolocation();
    mockHomeFetch({
      deleteLocationOk: false,
      savedLocations: [
        {
          id: locationId,
          name: "Dom",
          latitude: 46.5547,
          longitude: 15.6459,
          color: "#b91c1c",
          logo: "home",
        },
      ],
    });
    renderHome();

    fireEvent.click(await screen.findByRole("button", { name: "Izbriši Dom" }));

    await waitFor(() => expect(toast.error).toHaveBeenCalledWith("Lokacije ni bilo mogoče izbrisati."));
  });

  it("ignorira spremembo barve in ikone, ko draft lokacije ni odprt", async () => {
    mockGeolocation();
    mockHomeFetch();
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Spremeni barvo brez drafta" }));
    fireEvent.click(screen.getByRole("button", { name: "Spremeni ikono brez drafta" }));

    expect(screen.queryByTestId("draft-location")).not.toBeInTheDocument();
  });

  it("napake pri nalaganju shranjenih lokacij in poti prikazejo toast sporocila", async () => {
    mockGeolocation();
    mockHomeFetch({ fetchLocationsOk: false, fetchRoutesOk: false });

    renderHome();

    await waitFor(() =>
      expect(toast.error).toHaveBeenCalledWith("Shranjene lokacije niso bile naložene."),
    );
    expect(toast.error).toHaveBeenCalledWith("Shranjene poti niso bile naložene.");
  });

  it("deluje tudi, ko geolokacija v brskalniku ni na voljo", async () => {
    Object.defineProperty(navigator, "geolocation", {
      configurable: true,
      value: undefined,
    });
    mockHomeFetch();

    renderHome();

    expect(screen.getByTestId("main-map")).toHaveAttribute(
      "data-center",
      JSON.stringify({ lat: 46.5547, lng: 15.6459 }),
    );
  });

  it("filtrira neveljavne shranjene lokacije in poti", async () => {
    mockGeolocation();
    mockHomeFetch({
      savedLocations: [
        {
          id: locationId,
          name: "Dom",
          latitude: 46.5547,
          longitude: 15.6459,
          color: null,
          logo: "neznano",
        },
        {
          id: "123e4567-e89b-12d3-a456-426614174299",
          name: "Brez koordinat",
          latitude: Number.NaN,
          longitude: 15.6459,
        },
      ],
      savedRoutes: [
        {
          id: "123e4567-e89b-12d3-a456-426614174206",
          name: "Brez journey",
        },
        {
          id: "123e4567-e89b-12d3-a456-426614174204",
          name: "   ",
          journey: {
            ...integrationData.routePath,
            duration: "900000",
            distance: "1800",
            originAddress: "Center",
            destination_address: "Tabor",
          },
          createdAt: "2026-05-21T12:00:00.000Z",
        },
        {
          id: "123e4567-e89b-12d3-a456-426614174205",
          name: "Brez polyline",
          journey: {
            legs: [{ mode: "WALK", polyline: [] }],
          },
        },
        {
          id: "123e4567-e89b-12d3-a456-426614174207",
          name: "Brez legs",
          journey: {},
        },
      ],
    });

    renderHome();

    expect(await screen.findByRole("button", { name: "Izbriši Dom" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Izbriši Brez koordinat" })).not.toBeInTheDocument();
    expect(
      await screen.findByRole("button", { name: "Izberi shranjeno pot Shranjena pot" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Izberi shranjeno pot Brez polyline" }),
    ).not.toBeInTheDocument();
  });
});
