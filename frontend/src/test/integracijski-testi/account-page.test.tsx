import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "../../components/ThemeProvider";

const authMock = vi.hoisted(() => ({
  currentUser: { email: "firebase@example.com", displayName: "Test User" },
  signOut: vi.fn(),
}));

const authState = vi.hoisted(() => ({
  user: { email: "firebase@example.com", displayName: "Test User" } as null | {
    email: string;
    displayName: string;
  },
  onAuthStateChanged: vi.fn((_auth: unknown, callback: (user: null | {
    email: string;
    displayName: string;
  }) => void) => {
    callback(authState.user);
    return vi.fn();
  }),
}));

const sessionMock = vi.hoisted(() => ({
  userSession: {
    id: "123e4567-e89b-12d3-a456-426614174000",
    email: "session@example.com",
    name: "Test User",
  } as null | { id: string; email: string; name: string },
  getAuthToken: vi.fn().mockResolvedValue("id-token"),
  fetchUserSession: vi.fn().mockResolvedValue({
    id: "123e4567-e89b-12d3-a456-426614174000",
    email: "session@example.com",
    name: "Test User",
  }),
}));

vi.mock("firebase/app", () => ({
  initializeApp: vi.fn(() => ({})),
}));

vi.mock("firebase/auth", () => ({
  getAuth: vi.fn(() => authMock),
  onAuthStateChanged: authState.onAuthStateChanged,
}));

vi.mock("../../components/Authorization/UserSessionProvider", () => ({
  useUserSession: () => sessionMock,
}));

vi.mock("sonner", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

import { AccountPage } from "../../components/Pages/AccountPageComponents/AccountPage";
import { toast } from "sonner";

const locationId = "123e4567-e89b-12d3-a456-426614174101";
const routeId = "123e4567-e89b-12d3-a456-426614174102";

function mockAccountFetch({
  locations = [
    {
      id: locationId,
      name: "Dom",
      latitude: 46.5547,
      longitude: 15.6459,
      color: "#b91c1c",
      logo: "home",
    },
  ],
  routes = [
    {
      id: routeId,
      name: "Pot domov",
      journey: {
        origin: { lat: 46.5547, lng: 15.6459 },
        destination: { lat: 46.562, lng: 15.65 },
        legs: [{ mode: "WALK", polyline: [{ lat: 46.5547, lon: 15.6459 }] }],
      },
      duration: "600000",
      distance: "1200",
      originLabel: "Glavni trg",
      destinationLabel: "Tabor",
      modes: ["WALK"],
      createdAt: "2026-05-21T12:00:00.000Z",
    },
  ],
  deleteOk = true,
  locationsOk = true,
  routesOk = true,
}: {
  locations?: unknown[];
  routes?: unknown[];
  deleteOk?: boolean;
  locationsOk?: boolean;
  routesOk?: boolean;
} = {}) {
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string, init?: RequestInit) => {
      if (init?.method === "DELETE") {
        return Promise.resolve({ ok: deleteOk, status: deleteOk ? 204 : 500 });
      }

      if (url.includes("/api/locations/")) {
        return Promise.resolve({
          ok: locationsOk,
          status: locationsOk ? 200 : 500,
          json: () => Promise.resolve(locations),
        });
      }

      if (url.includes("/api/paths/")) {
        return Promise.resolve({
          ok: routesOk,
          status: routesOk ? 200 : 500,
          json: () => Promise.resolve(routes),
        });
      }

      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    }),
  );
}

function renderAccountPage() {
  return render(
    <MemoryRouter initialEntries={["/account"]}>
      <ThemeProvider>
        <AccountPage />
      </ThemeProvider>
    </MemoryRouter>,
  );
}

describe("integracijski AccountPage", () => {
  afterEach(() => {
    authState.user = { email: "firebase@example.com", displayName: "Test User" };
    sessionMock.userSession = {
      id: "123e4567-e89b-12d3-a456-426614174000",
      email: "session@example.com",
      name: "Test User",
    };
    sessionMock.getAuthToken.mockResolvedValue("id-token");
    sessionMock.fetchUserSession.mockResolvedValue({
      id: "123e4567-e89b-12d3-a456-426614174000",
      email: "session@example.com",
      name: "Test User",
    });
  });

  it("prijavljen uporabnik vidi email in naložene shranjene podatke", async () => {
    authState.user = { email: "firebase@example.com", displayName: "Test User" };
    mockAccountFetch();

    renderAccountPage();

    expect(await screen.findByText("firebase@example.com")).toBeInTheDocument();
    expect(await screen.findByText("Dom")).toBeInTheDocument();
    expect(await screen.findByText("Pot domov")).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledWith(
      "https://api.test/api/locations/123e4567-e89b-12d3-a456-426614174000",
      expect.objectContaining({ headers: { Authorization: "Bearer id-token" } }),
    );
  });

  it("uporabi fetchUserSession, ko session ni v contextu", async () => {
    sessionMock.userSession = null;
    mockAccountFetch();

    renderAccountPage();

    expect(await screen.findByText("Dom")).toBeInTheDocument();
    expect(sessionMock.fetchUserSession).toHaveBeenCalledWith("id-token");
  });

  it("neprijavljen Firebase uporabnik sprozi preusmeritev brez sesutja", async () => {
    authState.user = null;
    mockAccountFetch({ locations: [], routes: [] });

    renderAccountPage();

    expect(await screen.findByText("Ni še shranjenih lokacij.")).toBeInTheDocument();
  });

  it("prazni API seznami prikažejo prazna stanja", async () => {
    mockAccountFetch({ locations: [], routes: [] });

    renderAccountPage();

    expect(await screen.findByText("Ni še shranjenih lokacij.")).toBeInTheDocument();
    expect(await screen.findByText("Ni še shranjenih poti.")).toBeInTheDocument();
  });

  it("brez auth tokena prikaze prazna stanja brez API klica za podatke", async () => {
    sessionMock.getAuthToken.mockResolvedValueOnce(null).mockResolvedValueOnce(null);
    mockAccountFetch();

    renderAccountPage();

    expect(await screen.findByText("Ni še shranjenih lokacij.")).toBeInTheDocument();
    expect(await screen.findByText("Ni še shranjenih poti.")).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
  });

  it("napaki pri nalaganju lokacij in poti prikazeta toast", async () => {
    mockAccountFetch({ locationsOk: false, routesOk: false });

    renderAccountPage();

    await waitFor(() =>
      expect(toast.error).toHaveBeenCalledWith("Shranjene lokacije niso bile naložene."),
    );
    expect(toast.error).toHaveBeenCalledWith("Shranjene poti niso bile naložene.");
  });

  it("neveljavne lokacije in poti filtrira ter uporabi privzete vrednosti", async () => {
    mockAccountFetch({
      locations: [
        { id: locationId, name: "  ", latitude: "46.5547", longitude: "15.6459", icon: "invalid" },
        { id: "bad", name: "Neveljavna", latitude: "x", longitude: 15.6 },
      ],
      routes: [
        {
          id: routeId,
          name: "  ",
          journey: {
            duration: "600000",
            distance: "1200",
            origin_address: "Center",
            destination_address: "Tabor",
            legs: [
              { mode: "WALK", polyline: [{ lat: 46.5547, lon: 15.6459 }] },
              { mode: "", polyline: [{ lat: 46.55, lon: 15.64 }] },
            ],
          },
        },
        { id: "ignored", name: "Brez poti" },
      ],
    });

    renderAccountPage();

    expect(await screen.findByText("Shranjena lokacija")).toBeInTheDocument();
    expect(await screen.findByText("Shranjena pot")).toBeInTheDocument();
    expect(screen.queryByText("Neveljavna")).not.toBeInTheDocument();
    expect(screen.queryByText("Brez poti")).not.toBeInTheDocument();
  });

  it("brisanje shranjene lokacije po uspešnem DELETE odstrani kartico", async () => {
    mockAccountFetch();
    renderAccountPage();

    await screen.findByText("Dom");
    fireEvent.click(screen.getByRole("button", { name: "Izbriši lokacijo Dom" }));

    await waitFor(() => expect(screen.queryByText("Dom")).not.toBeInTheDocument());
    expect(fetch).toHaveBeenCalledWith(
      `https://api.test/api/locations/${locationId}`,
      expect.objectContaining({ method: "DELETE" }),
    );
    expect(toast.success).toHaveBeenCalledWith("Lokacija je izbrisana.");
  });

  it("neuspešen DELETE lokacije prikaže toast in ohrani kartico", async () => {
    mockAccountFetch({ deleteOk: false });
    renderAccountPage();

    await screen.findByText("Dom");
    fireEvent.click(screen.getByRole("button", { name: "Izbriši lokacijo Dom" }));

    await waitFor(() => expect(toast.error).toHaveBeenCalledWith("Lokacije ni bilo mogoče izbrisati."));
    expect(screen.getByText("Dom")).toBeInTheDocument();
  });

  it("brisanje brez tokena prikaze ustrezna toast sporocila", async () => {
    mockAccountFetch();
    renderAccountPage();

    await screen.findByText("Dom");
    sessionMock.getAuthToken.mockResolvedValue(null);
    fireEvent.click(screen.getByRole("button", { name: "Izbriši lokacijo Dom" }));
    fireEvent.click(screen.getByRole("button", { name: "Izbriši pot Pot domov" }));

    await waitFor(() =>
      expect(toast.error).toHaveBeenCalledWith("Za brisanje lokacije moraš biti prijavljen."),
    );
    expect(toast.error).toHaveBeenCalledWith("Za brisanje poti moraš biti prijavljen.");
  });

  it("uspešen DELETE poti odstrani kartico", async () => {
    mockAccountFetch();
    renderAccountPage();

    await screen.findByText("Pot domov");
    fireEvent.click(screen.getByRole("button", { name: "Izbriši pot Pot domov" }));

    await waitFor(() => expect(screen.queryByText("Pot domov")).not.toBeInTheDocument());
    expect(toast.success).toHaveBeenCalledWith("Pot je izbrisana.");
  });

  it("neuspešen DELETE poti prikaže toast in ohrani kartico", async () => {
    mockAccountFetch({ deleteOk: false });
    renderAccountPage();

    await screen.findByText("Pot domov");
    fireEvent.click(screen.getByRole("button", { name: "Izbriši pot Pot domov" }));

    await waitFor(() => expect(toast.error).toHaveBeenCalledWith("Poti ni bilo mogoče izbrisati."));
    expect(screen.getByText("Pot domov")).toBeInTheDocument();
  });

  it("odjava poklice Firebase signOut", async () => {
    mockAccountFetch();
    renderAccountPage();

    await screen.findByText("firebase@example.com");
    fireEvent.click(screen.getByRole("button", { name: "Odjava" }));

    expect(authMock.signOut).toHaveBeenCalled();
  });
});
