import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
    onCameraChanged?: (center: { lat: number; lng: number }, zoom: number) => void;
    onMapContextSelect?: (position: { lat: number; lng: number }) => void;
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
    savedRoutes?: typeof integrationData.savedRoute[];
    onSavedRouteSelect?: (route: typeof integrationData.savedRoute) => void;
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
  postLocationOk = true,
  deleteLocationOk = true,
  postPathOk = true,
}: {
  token?: string | null;
  savedLocations?: unknown[];
  savedRoutes?: unknown[];
  postLocationOk?: boolean;
  deleteLocationOk?: boolean;
  postPathOk?: boolean;
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
        return Promise.resolve({ ok: true, json: () => Promise.resolve(savedLocations) });
      }

      if (url.includes("/api/paths/")) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(savedRoutes) });
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
  afterEach(() => {
    sessionMock.getAuthToken.mockResolvedValue("id-token");
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
});
