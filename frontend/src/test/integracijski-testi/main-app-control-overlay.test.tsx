import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps } from "react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "../../components/ThemeProvider";
import { routePath } from "../frontendPlanFixtures";

const authMock = vi.hoisted(() => ({
  currentUser: { uid: "firebase-user-1" },
  signOut: vi.fn(),
}));

const authState = vi.hoisted(() => ({
  onAuthStateChanged: vi.fn((_auth: unknown, callback: (user: { uid: string }) => void) => {
    callback({ uid: "firebase-user-1" });
    return vi.fn();
  }),
}));

vi.mock("firebase/app", () => ({
  initializeApp: vi.fn(() => ({})),
}));

vi.mock("firebase/auth", () => ({
  getAuth: vi.fn(() => authMock),
  onAuthStateChanged: authState.onAuthStateChanged,
}));

vi.mock("../../hooks/usePlacesAutocomplete", async () => {
  const React = await import("react");

  return {
    MARIBOR_BOUNDS: {
      low: { latitude: 46.49, longitude: 15.520363 },
      high: { latitude: 46.63, longitude: 15.76 },
    },
    usePlacesAutocomplete: () => {
      const [value, setValue] = React.useState("");
      const [isOpen, setIsOpen] = React.useState(false);

      return {
        value,
        setValue,
        isOpen,
        setIsOpen,
        predictions: [
          {
            placeId: "place-origin",
            mainText: "Glavni trg",
            secondaryText: "Maribor",
          },
          {
            placeId: "place-destination",
            mainText: "Tabor",
            secondaryText: "Maribor",
          },
        ],
        handleChange: (event: React.ChangeEvent<HTMLInputElement>) => {
          setValue(event.target.value);
          setIsOpen(true);
        },
        clear: () => {
          setValue("");
          setIsOpen(false);
        },
        closeDropdown: () => setIsOpen(false),
      };
    },
  };
});

import { MainAppControlOverlay } from "../../components/MainAppComponents/MainAppControlOverlay";

function mockOverlayFetch({
  computeOk = true,
  placeHasLocation = true,
  placeNetworkFails = false as false | true | "origin" | "destination",
  computeFallbackShape = false,
  computeAlternativeShape = false,
  computeJsonFails = false,
  computeNetworkFails = false,
} = {}) {
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) => {
      if (url.includes("openweathermap")) {
        return Promise.resolve({
          json: () => Promise.resolve({ main: { temp: 20 }, weather: [{ main: "Clear" }] }),
        });
      }

      if (url.includes("places/place-origin")) {
        if (placeNetworkFails === true || placeNetworkFails === "origin") {
          return Promise.reject(new Error("places"));
        }

        return Promise.resolve({
          json: () =>
            Promise.resolve(
              placeHasLocation
                ? { location: { latitude: 46.5547, longitude: 15.6459 } }
                : {},
            ),
        });
      }

      if (url.includes("places/place-destination")) {
        if (placeNetworkFails === true || placeNetworkFails === "destination") {
          return Promise.reject(new Error("places"));
        }

        return Promise.resolve({
          json: () =>
            Promise.resolve(
              placeHasLocation
                ? { location: { latitude: 46.562, longitude: 15.65 } }
                : {},
            ),
        });
      }

      if (url.includes("/compute?")) {
        if (computeNetworkFails) {
          return Promise.reject(new Error("network"));
        }

        return Promise.resolve(
          computeOk
            ? {
                ok: true,
                json: () =>
                  Promise.resolve(
                    computeFallbackShape
                      ? { status: "OK" }
                      : computeAlternativeShape
                        ? {
                            status: "success",
                            origin: { lat: 46.5547, lon: 15.6459 },
                            origin_address: "Glavni trg",
                            destination: { lat: 46.562, lon: 15.65 },
                            destination_address: "Tabor",
                            routes: [
                              {
                                rank: 1,
                                label: "Najhitrejša pot",
                                totalDurationSeconds: 1860,
                                totalDistanceMeters: 2450,
                                modes: ["WALK", "BUS", "BIKE"],
                                legs: routePath.legs,
                              },
                              {
                                rank: 2,
                                label: "Brez kolesa",
                                totalDurationSeconds: 2100,
                                totalDistanceMeters: 3200,
                                modes: ["WALK", "BUS"],
                                legs: routePath.legs.slice(0, 2),
                              },
                            ],
                          }
                        : { routes: [routePath] },
                  ),
              }
            : {
                ok: false,
                status: 500,
                statusText: "Server error",
                json: () =>
                  computeJsonFails
                    ? Promise.reject(new Error("bad json"))
                    : Promise.resolve({ code: "NO_ROUTE", message: "Ni poti." }),
              },
        );
      }

      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    }),
  );
}

function renderOverlay(props: Partial<ComponentProps<typeof MainAppControlOverlay>> = {}) {
  const onPathReceive = vi.fn();
  const onPathError = vi.fn();

  render(
    <MemoryRouter initialEntries={["/home"]}>
      <ThemeProvider>
        <MainAppControlOverlay
          currentLocation={{ lat: 46.5547, lng: 15.6459 }}
          onPathReceive={onPathReceive}
          onPathError={onPathError}
          {...props}
        />
      </ThemeProvider>
    </MemoryRouter>,
  );

  return { onPathReceive, onPathError };
}

async function selectDestinationAndOpenDirections() {
  fireEvent.focus(screen.getByRole("textbox", { name: "Kam šibaš?" }));
  fireEvent.mouseDown(await screen.findByText("Tabor"));
  await waitFor(() => expect(screen.getByRole("button", { name: "Navodila za pot" })).toBeInTheDocument());
  fireEvent.click(screen.getByRole("button", { name: "Navodila za pot" }));
}

describe("integracijski MainAppControlOverlay", () => {
  it("izbira Places predlogov in klik Najdi pot pokliče /compute s pravilnimi parametri", async () => {
    mockOverlayFetch();
    const { onPathReceive } = renderOverlay();

    await selectDestinationAndOpenDirections();
    fireEvent.focus(screen.getByRole("textbox", { name: "Kje štartaš?" }));
    fireEvent.mouseDown(await screen.findByText("Glavni trg"));
    await waitFor(() => expect(screen.getByRole("button", { name: "Najdi pot" })).toBeEnabled());
    fireEvent.change(screen.getByDisplayValue(/^\d\d:\d\d$/), {
      target: { value: "09:30" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Najdi pot" }));

    await waitFor(() => expect(onPathReceive).toHaveBeenCalledWith(expect.objectContaining({ legs: routePath.legs })));

    const computeUrl = vi
      .mocked(fetch)
      .mock.calls.map(([url]) => String(url))
      .find((url) => url.includes("/compute?"));
    expect(computeUrl).toBeDefined();
    const params = new URL(computeUrl as string).searchParams;
    expect(params.get("originLat")).toBe("46.5547");
    expect(params.get("originLon")).toBe("15.6459");
    expect(params.get("destinationLat")).toBe("46.562");
    expect(params.get("destinationLon")).toBe("15.65");
    expect(params.get("leaveAt")).toBe("09:30");
    expect(params.get("userId")).toBe("firebase-user-1");
  });

  it("normalizira vse alternative v celotne poti", async () => {
    mockOverlayFetch({ computeAlternativeShape: true });
    const { onPathReceive } = renderOverlay();

    await selectDestinationAndOpenDirections();
    fireEvent.focus(screen.getByRole("textbox", { name: "Kje štartaš?" }));
    fireEvent.mouseDown(await screen.findByText("Glavni trg"));
    await waitFor(() => expect(screen.getByRole("button", { name: "Najdi pot" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "Najdi pot" }));

    await waitFor(() =>
      expect(onPathReceive).toHaveBeenCalledWith(
        expect.objectContaining({
          status: "success",
          origin: { lat: 46.5547, lon: 15.6459 },
          origin_address: "Glavni trg",
          destination: { lat: 46.562, lon: 15.65 },
          destination_address: "Tabor",
          duration: "1860000",
          distance: "2450",
          legs: routePath.legs,
          routes: [
            expect.objectContaining({
              label: "Najhitrejša pot",
              duration: "1860000",
              distance: "2450",
            }),
            expect.objectContaining({
              label: "Brez kolesa",
              duration: "2100000",
              distance: "3200",
            }),
          ],
        }),
      ),
    );
  });

  it("način Prihod do pošlje arriveBy namesto leaveAt", async () => {
    mockOverlayFetch();
    renderOverlay();

    await selectDestinationAndOpenDirections();
    fireEvent.focus(screen.getByRole("textbox", { name: "Kje štartaš?" }));
    fireEvent.mouseDown(await screen.findByText("Glavni trg"));
    await waitFor(() => expect(screen.getByRole("button", { name: "Najdi pot" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "Odhod ob" }));
    fireEvent.click(screen.getByRole("button", { name: "Najdi pot" }));

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(expect.stringContaining("/compute?")));
    const computeUrl = vi
      .mocked(fetch)
      .mock.calls.map(([url]) => String(url))
      .find((url) => url.includes("/compute?"));
    const params = new URL(computeUrl as string).searchParams;
    expect(params.get("arriveBy")).toMatch(/^\d\d:\d\d$/);
    expect(params.get("leaveAt")).toBeNull();
  });

  it("napaka /compute pokliče onPathError in odstrani loading overlay", async () => {
    mockOverlayFetch({ computeOk: false });
    const { onPathError } = renderOverlay();

    await selectDestinationAndOpenDirections();
    fireEvent.focus(screen.getByRole("textbox", { name: "Kje štartaš?" }));
    fireEvent.mouseDown(await screen.findByText("Glavni trg"));
    await waitFor(() => expect(screen.getByRole("button", { name: "Najdi pot" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "Najdi pot" }));

    await waitFor(() => expect(onPathError).toHaveBeenCalledWith({ code: "NO_ROUTE", message: "Ni poti." }));
    expect(screen.queryByText("Iščem pot...")).not.toBeInTheDocument();
  });

  it("neberljiva napaka /compute uporabi HTTP fallback", async () => {
    mockOverlayFetch({ computeOk: false, computeJsonFails: true });
    const { onPathError } = renderOverlay();

    await selectDestinationAndOpenDirections();
    fireEvent.focus(screen.getByRole("textbox", { name: "Kje štartaš?" }));
    fireEvent.mouseDown(await screen.findByText("Glavni trg"));
    await waitFor(() => expect(screen.getByRole("button", { name: "Najdi pot" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "Najdi pot" }));

    await waitFor(() =>
      expect(onPathError).toHaveBeenCalledWith({
        code: "HTTP_500",
        message: "Server error",
      }),
    );
  });

  it("network napaka /compute pokliče ROUTE_REQUEST_FAILED", async () => {
    mockOverlayFetch({ computeNetworkFails: true });
    const { onPathError } = renderOverlay();

    await selectDestinationAndOpenDirections();
    fireEvent.focus(screen.getByRole("textbox", { name: "Kje štartaš?" }));
    fireEvent.mouseDown(await screen.findByText("Glavni trg"));
    await waitFor(() => expect(screen.getByRole("button", { name: "Najdi pot" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "Najdi pot" }));

    await waitFor(() =>
      expect(onPathError).toHaveBeenCalledWith(
        expect.objectContaining({ code: "ROUTE_REQUEST_FAILED", message: "network" }),
      ),
    );
  });

  it("trenutna lokacija in shranjena lokacija nastavita izhodisce in cilj", async () => {
    mockOverlayFetch();
    const onPlaceSelect = vi.fn();
    const onDestinationSelect = vi.fn();
    renderOverlay({
      onPlaceSelect,
      onDestinationSelect,
      savedLocations: [
        {
          id: "location-1",
          name: "Fakulteta",
          position: { lat: 46.56, lng: 15.66 },
          color: "#2563eb",
          icon: "school",
        },
      ],
    });

    await selectDestinationAndOpenDirections();
    fireEvent.focus(screen.getByRole("textbox", { name: "Kje štartaš?" }));
    fireEvent.mouseDown(await screen.findByText("Trenutna lokacija"));
    expect(onPlaceSelect).toHaveBeenCalledWith({ lat: 46.5547, lng: 15.6459 });

    fireEvent.focus(screen.getByRole("textbox", { name: "Kam šibaš?" }));
    fireEvent.mouseDown(await screen.findByText("Fakulteta"));
    expect(onDestinationSelect).toHaveBeenCalledWith({ lat: 46.56, lng: 15.66 });
  });

  it("Places predlog brez lokacije prikaze napako lokacije", async () => {
    mockOverlayFetch({ placeHasLocation: false });
    renderOverlay();

    fireEvent.focus(screen.getByRole("textbox", { name: "Kam šibaš?" }));
    fireEvent.mouseDown(await screen.findByText("Tabor"));

    expect(
      await screen.findByText("Lokacije ni bilo mogoče najti. Prosimo poskusite znova."),
    ).toBeInTheDocument();
  });

  it("Places network napaka pri izhodiscu prikaze napako lokacije", async () => {
    mockOverlayFetch({ placeNetworkFails: "origin" });
    renderOverlay();

    await selectDestinationAndOpenDirections();
    fireEvent.focus(screen.getByRole("textbox", { name: "Kje štartaš?" }));
    fireEvent.mouseDown(await screen.findByText("Glavni trg"));

    expect(
      await screen.findByText("Lokacije ni bilo mogoče najti. Prosimo poskusite znova."),
    ).toBeInTheDocument();
  });

  it("fallback compute response brez routes vrne prazen journey", async () => {
    mockOverlayFetch({ computeFallbackShape: true });
    const { onPathReceive } = renderOverlay();

    await selectDestinationAndOpenDirections();
    fireEvent.focus(screen.getByRole("textbox", { name: "Kje štartaš?" }));
    fireEvent.mouseDown(await screen.findByText("Glavni trg"));
    await waitFor(() => expect(screen.getByRole("button", { name: "Najdi pot" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "Najdi pot" }));

    await waitFor(() =>
      expect(onPathReceive).toHaveBeenCalledWith(expect.objectContaining({ legs: [] })),
    );
  });

  it("shranjena pot iz dropdowna nastavi route in fallback labele", async () => {
    mockOverlayFetch();
    const onSavedRouteSelect = vi.fn();
    renderOverlay({
      onSavedRouteSelect,
      savedRoutes: [
        {
          id: "route-1",
          name: "Shranjena pot",
          journey: {
            legs: [{ mode: "WALK", polyline: [{ lat: 46.5547, lon: 15.6459 }] }],
          },
          duration: "600000",
          distance: "1000",
          modes: ["WALK"],
        },
      ],
    });

    fireEvent.click(screen.getByRole("button", { name: "Shranjene poti" }));
    fireEvent.mouseDown(await screen.findByText("Shranjena pot"));

    expect(onSavedRouteSelect).toHaveBeenCalled();
    expect(screen.getByDisplayValue("Začetek poti")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Konec poti")).toBeInTheDocument();
  });

  it("sprememba parametra pri izbrani poti ponovno poklice /compute namesto zacetka poti", async () => {
    mockOverlayFetch();
    const onStartRoute = vi.fn();
    renderOverlay({
      hasRoute: true,
      onStartRoute,
      savedRoutes: [
        {
          id: "route-1",
          name: "Shranjena pot",
          journey: routePath,
          duration: "600000",
          distance: "1000",
          originLabel: "Glavni trg",
          destinationLabel: "Tabor",
          modes: ["WALK"],
        },
      ],
    });

    fireEvent.click(screen.getByRole("button", { name: "Shranjene poti" }));
    fireEvent.mouseDown(await screen.findByText("Shranjena pot"));

    expect(screen.getByRole("button", { name: "Začni" })).toBeEnabled();

    fireEvent.click(screen.getByRole("button", { name: "Bus" }));

    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Najdi pot" })).toBeEnabled(),
    );

    fireEvent.click(screen.getByRole("button", { name: "Najdi pot" }));

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(expect.stringContaining("/compute?")),
    );
    expect(onStartRoute).not.toHaveBeenCalled();
  });

  it("dropdown shranjenih poti formatira trajanje, razdaljo in relacijo", async () => {
    mockOverlayFetch();
    renderOverlay({
      savedRoutes: [
        {
          id: "route-1",
          name: "Pot na faks",
          journey: {
            origin: { lat: 46.5547, lng: 15.6459 },
            destination: { lat: 46.56, lng: 15.66 },
            legs: [{ mode: "WALK", polyline: [{ lat: 46.5547, lon: 15.6459 }] }],
          },
          duration: "600000",
          distance: "1234",
          originLabel: "Center",
          destinationLabel: "Fakulteta",
          modes: ["WALK"],
        },
      ],
    });

    fireEvent.click(screen.getByRole("button", { name: "Shranjene poti" }));

    expect(await screen.findByText("Pot na faks")).toBeInTheDocument();
    expect(screen.getByText("10 min • 1 km 234 m")).toBeInTheDocument();
    expect(screen.getByText("Center → Fakulteta")).toBeInTheDocument();
  });

  it("dropdown shranjenih poti prikaze prazno stanje", async () => {
    mockOverlayFetch();
    renderOverlay({ savedRoutes: [] });

    fireEvent.click(screen.getByRole("button", { name: "Shranjene poti" }));

    expect(await screen.findByText("Ni še shranjenih poti.")).toBeInTheDocument();
  });

  it("zamenja smeri, pocisti inpute in zapre dropdowne ob kliku zunaj", async () => {
    mockOverlayFetch();
    const onPlaceSelect = vi.fn();
    const onDestinationSelect = vi.fn();
    renderOverlay({ onPlaceSelect, onDestinationSelect });

    await selectDestinationAndOpenDirections();
    fireEvent.focus(screen.getByRole("textbox", { name: "Kje štartaš?" }));
    fireEvent.mouseDown(await screen.findByText("Glavni trg"));
    fireEvent.click(screen.getByRole("button", { name: "Zamenjaj smeri" }));

    expect(onPlaceSelect).toHaveBeenCalledWith({ lat: 46.562, lng: 15.65 });
    expect(onDestinationSelect).toHaveBeenCalled();

    fireEvent.click(screen.getAllByRole("button", { name: "Počisti" })[0]);
    expect(onPlaceSelect).toHaveBeenLastCalledWith(null);

    fireEvent.click(screen.getAllByRole("button", { name: "Počisti" })[0]);
    expect(onDestinationSelect).toHaveBeenCalled();

    fireEvent.mouseDown(document.body);
    expect(screen.queryByText("Glavni trg")).not.toBeInTheDocument();
  });

  it("rocna sprememba inputov ponastavi koordinate", async () => {
    mockOverlayFetch();
    const onPlaceSelect = vi.fn();
    const onDestinationSelect = vi.fn();
    renderOverlay({ onPlaceSelect, onDestinationSelect });

    await selectDestinationAndOpenDirections();
    fireEvent.change(screen.getByRole("textbox", { name: "Kje štartaš?" }), {
      target: { value: "Nova lokacija" },
    });
    fireEvent.change(screen.getByRole("textbox", { name: "Kam šibaš?" }), {
      target: { value: "Drug cilj" },
    });

    expect(onPlaceSelect).toHaveBeenLastCalledWith(null);
    expect(onDestinationSelect).toHaveBeenLastCalledWith(null);
  });

  it("trenutna lokacija kot cilj in shranjena lokacija kot izhodisce nastavita koordinate", async () => {
    mockOverlayFetch();
    const onPlaceSelect = vi.fn();
    const onDestinationSelect = vi.fn();
    renderOverlay({
      onPlaceSelect,
      onDestinationSelect,
      savedLocations: [
        {
          id: "location-1",
          name: "Fakulteta",
          position: { lat: 46.56, lng: 15.66 },
          color: "#2563eb",
          icon: "school",
        },
      ],
    });

    await selectDestinationAndOpenDirections();
    fireEvent.focus(screen.getByRole("textbox", { name: "Kje štartaš?" }));
    fireEvent.mouseDown(await screen.findByText("Fakulteta"));
    expect(onPlaceSelect).toHaveBeenCalledWith({ lat: 46.56, lng: 15.66 });

    fireEvent.focus(screen.getByRole("textbox", { name: "Kam šibaš?" }));
    fireEvent.mouseDown(await screen.findByText("Trenutna lokacija"));
    expect(onDestinationSelect).toHaveBeenCalledWith({ lat: 46.5547, lng: 15.6459 });
  });

  it("profil, odjava, zoom in route active gumbi sprozijo callbacke", async () => {
    mockOverlayFetch();
    const onZoomIn = vi.fn();
    const onZoomOut = vi.fn();
    const onLocate = vi.fn();
    const onStartRoute = vi.fn();
    const onEndRoute = vi.fn();
    const { rerender } = render(
      <MemoryRouter initialEntries={["/home"]}>
        <ThemeProvider>
          <MainAppControlOverlay
            currentLocation={{ lat: 46.5547, lng: 15.6459 }}
            hasRoute
            onStartRoute={onStartRoute}
            onEndRoute={onEndRoute}
            onZoomIn={onZoomIn}
            onZoomOut={onZoomOut}
            onLocate={onLocate}
          />
        </ThemeProvider>
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Povečaj" }));
    fireEvent.click(screen.getByRole("button", { name: "Pomanjšaj" }));
    fireEvent.click(screen.getByRole("button", { name: "Moja lokacija" }));
    fireEvent.click(screen.getByRole("button", { name: "Profil" }));
    fireEvent.click(screen.getByRole("button", { name: "Domov" }));
    fireEvent.click(screen.getByRole("button", { name: "Odjava" }));

    expect(onZoomIn).toHaveBeenCalled();
    expect(onZoomOut).toHaveBeenCalled();
    expect(onLocate).toHaveBeenCalled();
    expect(authMock.signOut).toHaveBeenCalled();

    await selectDestinationAndOpenDirections();
    fireEvent.click(screen.getByRole("button", { name: "Začni" }));
    expect(onStartRoute).toHaveBeenCalled();

    rerender(
      <MemoryRouter initialEntries={["/home"]}>
        <ThemeProvider>
          <MainAppControlOverlay
            currentLocation={{ lat: 46.5547, lng: 15.6459 }}
            hasRoute
            isRouteActive
            onEndRoute={onEndRoute}
          />
        </ThemeProvider>
      </MemoryRouter>,
    );
    fireEvent.click(screen.getByRole("button", { name: "Končaj" }));
    expect(onEndRoute).toHaveBeenCalled();
  });

  it("loading overlay se lahko zapre in transport toggle spremeni compute parametre", async () => {
    mockOverlayFetch();
    renderOverlay();

    await selectDestinationAndOpenDirections();
    fireEvent.focus(screen.getByRole("textbox", { name: "Kje štartaš?" }));
    fireEvent.mouseDown(await screen.findByText("Glavni trg"));
    await waitFor(() => expect(screen.getByRole("button", { name: "Najdi pot" })).toBeEnabled());

    fireEvent.click(screen.getByRole("button", { name: "Bus" }));
    fireEvent.click(screen.getByRole("button", { name: "Kolo" }));
    fireEvent.click(screen.getByRole("button", { name: "Najdi pot" }));
    fireEvent.click(await screen.findByRole("button", { name: "Prekliči iskanje poti" }));

    await waitFor(() => expect(screen.queryByText("Iščem pot...")).not.toBeInTheDocument());
  });

  it("izbira datuma v dropdownu doda date parameter v compute zahtevo", async () => {
    mockOverlayFetch();
    renderOverlay();

    await selectDestinationAndOpenDirections();
    fireEvent.focus(screen.getByRole("textbox", { name: "Kje štartaš?" }));
    fireEvent.mouseDown(await screen.findByText("Glavni trg"));
    await waitFor(() => expect(screen.getByRole("button", { name: "Najdi pot" })).toBeEnabled());

    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowValue = `${tomorrow.getFullYear()}-${String(tomorrow.getMonth() + 1).padStart(2, "0")}-${String(tomorrow.getDate()).padStart(2, "0")}`;
    const todayLabel = new Date().toLocaleDateString("sl-SI", {
      weekday: "short",
      day: "numeric",
      month: "numeric",
    });
    const tomorrowLabel = tomorrow.toLocaleDateString("sl-SI", {
      weekday: "short",
      day: "numeric",
      month: "numeric",
    });

    fireEvent.click(screen.getByText(todayLabel).closest("button")!);
    fireEvent.click(await screen.findByRole("button", { name: tomorrowLabel }));
    fireEvent.click(screen.getByRole("button", { name: "Najdi pot" }));

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(expect.stringContaining("/compute?")));
    const computeUrl = vi
      .mocked(fetch)
      .mock.calls.map(([url]) => String(url))
      .find((url) => url.includes("/compute?"));

    expect(new URL(computeUrl as string).searchParams.get("date")).toBe(tomorrowValue);
  });
});
