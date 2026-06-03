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

function mockOverlayFetch({ computeOk = true } = {}) {
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) => {
      if (url.includes("openweathermap")) {
        return Promise.resolve({
          json: () => Promise.resolve({ main: { temp: 20 }, weather: [{ main: "Clear" }] }),
        });
      }

      if (url.includes("places/place-origin")) {
        return Promise.resolve({
          json: () => Promise.resolve({ location: { latitude: 46.5547, longitude: 15.6459 } }),
        });
      }

      if (url.includes("places/place-destination")) {
        return Promise.resolve({
          json: () => Promise.resolve({ location: { latitude: 46.562, longitude: 15.65 } }),
        });
      }

      if (url.includes("/compute?")) {
        return Promise.resolve(
          computeOk
            ? {
                ok: true,
                json: () => Promise.resolve({ routes: [routePath] }),
              }
            : {
                ok: false,
                status: 500,
                json: () => Promise.resolve({ code: "NO_ROUTE", message: "Ni poti." }),
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
});
