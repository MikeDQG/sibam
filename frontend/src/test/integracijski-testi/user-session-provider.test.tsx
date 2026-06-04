import { act, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useEffect } from "react";

const authMock = vi.hoisted(() => ({
  currentUser: null as null | { getIdToken: () => Promise<string> },
}));

const authState = vi.hoisted(() => ({
  callback: null as null | ((user: null | { getIdToken: () => Promise<string> }) => void),
  onAuthStateChanged: vi.fn((_auth: unknown, callback: (user: null | { getIdToken: () => Promise<string> }) => void) => {
    authState.callback = callback;
    return vi.fn();
  }),
  signOut: vi.fn().mockResolvedValue(undefined),
}));

vi.mock("firebase/app", () => ({
  initializeApp: vi.fn(() => ({})),
}));

vi.mock("firebase/auth", () => ({
  getAuth: vi.fn(() => authMock),
  onAuthStateChanged: authState.onAuthStateChanged,
  signOut: authState.signOut,
}));

import {
  UserSessionProvider,
  useUserSession,
} from "../../components/Authorization/UserSessionProvider";

const userId = "123e4567-e89b-12d3-a456-426614174000";

function SessionProbe() {
  const {
    userSession,
    getAuthToken,
    fetchUserSession,
    syncUserSession,
    clearUserSession,
  } = useUserSession();

  return (
    <div>
      <p data-testid='session-email'>{userSession?.email ?? "none"}</p>
      <button type='button' onClick={() => void getAuthToken().then((token) => window.localStorage.setItem("token", token ?? "none"))}>
        Token
      </button>
      <button type='button' onClick={() => void fetchUserSession("id-token")}>
        Fetch session
      </button>
      <button type='button' onClick={() => void fetchUserSession("id-token").catch((error) => window.localStorage.setItem("fetch-error", error.message))}>
        Fetch session with error
      </button>
      <button type='button' onClick={() => void syncUserSession("id-token", " Test User ")}>
        Sync session
      </button>
      <button type='button' onClick={() => void syncUserSession("id-token").then(() => window.localStorage.setItem("synced-without-name", "yes"))}>
        Sync session without name
      </button>
      <button type='button' onClick={() => void syncUserSession("id-token").catch((error) => window.localStorage.setItem("sync-error", error.message))}>
        Sync session with error
      </button>
      <button type='button' onClick={clearUserSession}>
        Clear session
      </button>
    </div>
  );
}

function AuthStateProbe() {
  const { userSession } = useUserSession();

  useEffect(() => {
    window.localStorage.setItem("session-email", userSession?.email ?? "none");
  }, [userSession]);

  return <p>{userSession?.email ?? "none"}</p>;
}

function renderProvider(children = <SessionProbe />) {
  return render(<UserSessionProvider>{children}</UserSessionProvider>);
}

describe("integracijski UserSessionProvider", () => {
  it("getAuthToken vrne null, ko Firebase uporabnik ne obstaja", async () => {
    authMock.currentUser = null;
    renderProvider();

    screen.getByRole("button", { name: "Token" }).click();

    await waitFor(() => expect(window.localStorage.getItem("token")).toBe("none"));
  });

  it("getAuthToken vrne token, ko Firebase uporabnik obstaja", async () => {
    authMock.currentUser = { getIdToken: vi.fn().mockResolvedValue("id-token") };
    renderProvider();

    screen.getByRole("button", { name: "Token" }).click();

    await waitFor(() => expect(window.localStorage.getItem("token")).toBe("id-token"));
  });

  it("fetchUserSession pošlje Authorization header in normalizira uporabnika", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ id: userId, email: "test@example.com", fullName: "Test User" }),
      }),
    );
    renderProvider();

    screen.getByRole("button", { name: "Fetch session" }).click();

    await screen.findByText("test@example.com");
    expect(fetch).toHaveBeenCalledWith(
      "https://api.test/api/users/me",
      expect.objectContaining({
        headers: { Authorization: "Bearer id-token" },
      }),
    );
  });

  it("syncUserSession pošlje POST in X-Full-Name header", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ id: userId, email: "test@example.com", fullName: "Test User" }),
      }),
    );
    renderProvider();

    screen.getByRole("button", { name: "Sync session" }).click();

    await screen.findByText("test@example.com");
    expect(fetch).toHaveBeenCalledWith(
      "https://api.test/api/users/me",
      expect.objectContaining({
        method: "POST",
        headers: {
          Authorization: "Bearer id-token",
          "X-Full-Name": "Test User",
        },
      }),
    );
  });

  it("syncUserSession brez imena ne poslje X-Full-Name headerja", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ id: userId, email: "test@example.com" }),
      }),
    );
    renderProvider();

    screen.getByRole("button", { name: "Sync session without name" }).click();

    await waitFor(() => expect(window.localStorage.getItem("synced-without-name")).toBe("yes"));
    expect(fetch).toHaveBeenCalledWith(
      "https://api.test/api/users/me",
      expect.objectContaining({
        method: "POST",
        headers: {
          Authorization: "Bearer id-token",
        },
      }),
    );
  });

  it("fetchUserSession ob neuspehu pocisti sejo in odjavi uporabnika", async () => {
    window.history.pushState({}, "", "/login");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
      }),
    );
    renderProvider();

    screen.getByRole("button", { name: "Fetch session with error" }).click();

    await waitFor(() => expect(window.localStorage.getItem("fetch-error")).toContain("401"));
    expect(authState.signOut).toHaveBeenCalled();
  });

  it("syncUserSession ob neuspehu vrze napako", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
      }),
    );
    renderProvider();

    screen.getByRole("button", { name: "Sync session with error" }).click();

    await waitFor(() => expect(window.localStorage.getItem("sync-error")).toContain("500"));
  });

  it("onAuthStateChanged ob prijavljenem uporabniku naloži sejo", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ id: userId, email: "test@example.com", fullName: "Test User" }),
      }),
    );
    renderProvider(<AuthStateProbe />);

    await act(async () => {
      await authState.callback?.({ getIdToken: vi.fn().mockResolvedValue("id-token") });
    });

    await waitFor(() => expect(window.localStorage.getItem("session-email")).toBe("test@example.com"));
  });

  it("onAuthStateChanged ob odjavljenem uporabniku pocisti sejo", async () => {
    renderProvider(<AuthStateProbe />);

    await act(async () => {
      await authState.callback?.(null);
    });

    await waitFor(() => expect(window.localStorage.getItem("session-email")).toBe("none"));
  });

  it("clearUserSession počisti trenutno sejo", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ id: userId, email: "test@example.com", fullName: "Test User" }),
      }),
    );
    renderProvider();

    screen.getByRole("button", { name: "Fetch session" }).click();
    await screen.findByText("test@example.com");
    screen.getByRole("button", { name: "Clear session" }).click();

    await waitFor(() =>
      expect(screen.getByTestId("session-email")).toHaveTextContent("none"),
    );
  });

  it("useUserSession zunaj providerja vrze napako", () => {
    function InvalidConsumer() {
      useUserSession();
      return null;
    }

    expect(() => render(<InvalidConsumer />)).toThrow(
      "useUserSession must be used within UserSessionProvider",
    );
  });
});
