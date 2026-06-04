import { describe, expect, it, vi } from "vitest";

import { Header } from "../../components/LandingPageComponents/Header";
import { fireEvent, renderWithTheme, screen, waitFor } from "../frontendPlanFixtures";

const routerState = vi.hoisted(() => ({
  navigate: vi.fn(),
  pathname: "/",
}));

const authState = vi.hoisted(() => ({
  user: null as { uid: string } | null,
  signOut: vi.fn(),
}));

vi.mock("react-router-dom", () => ({
  useNavigate: () => routerState.navigate,
  useLocation: () => ({ pathname: routerState.pathname }),
}));

vi.mock("../../firebase", () => ({
  auth: {
    signOut: authState.signOut,
  },
}));

vi.mock("firebase/auth", () => ({
  onAuthStateChanged: vi.fn((_auth, callback: (user: { uid: string } | null) => void) => {
    callback(authState.user);
    return vi.fn();
  }),
}));

describe("header", () => {
  it("klik na logo vodi na zacetno stran", async () => {
    routerState.pathname = "/home";
    authState.user = null;

    renderWithTheme(<Header />);

    fireEvent.click(screen.getByRole("button", { name: "Domov" }));

    expect(routerState.navigate).toHaveBeenCalledWith("/");
  });

  it("gumb Najdi pot vodi na aplikacijo", () => {
    routerState.pathname = "/login";
    authState.user = null;

    renderWithTheme(<Header />);

    fireEvent.click(screen.getByRole("button", { name: "Najdi pot" }));

    expect(routerState.navigate).toHaveBeenCalledWith("/home");
  });

  it("gumb Moj racun se prikaze prijavljenemu uporabniku", async () => {
    routerState.pathname = "/home";
    authState.user = { uid: "user-1" };

    renderWithTheme(<Header />);

    expect(await screen.findByRole("button", { name: "Moj račun" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Moj račun" }));
    expect(routerState.navigate).toHaveBeenCalledWith("/account");
  });

  it("gumb Prijava vodi na login za odjavljenega uporabnika", () => {
    routerState.pathname = "/";
    authState.user = null;

    renderWithTheme(<Header />);

    fireEvent.click(screen.getByRole("button", { name: "Prijava" }));

    expect(authState.signOut).not.toHaveBeenCalled();
    expect(routerState.navigate).toHaveBeenCalledWith("/login");
  });

  it("gumb Odjava odjavi uporabnika in ga preusmeri na login", async () => {
    routerState.pathname = "/home";
    authState.user = { uid: "user-1" };

    renderWithTheme(<Header />);

    fireEvent.click(await screen.findByRole("button", { name: "Odjava" }));

    expect(authState.signOut).toHaveBeenCalled();
    expect(routerState.navigate).toHaveBeenCalledWith("/login");
  });

  it("header spremeni stil po scrollu", async () => {
    routerState.pathname = "/";
    authState.user = null;

    renderWithTheme(<Header />);

    const header = screen.getByRole("banner");
    expect(header).toHaveClass("bg-white/0");

    Object.defineProperty(window, "scrollY", {
      configurable: true,
      value: 120,
    });
    fireEvent.scroll(window);

    await waitFor(() => expect(header).toHaveClass("shadow-lg"));
  });

  it("preklop teme deluje v headerju", () => {
    routerState.pathname = "/";
    authState.user = null;

    renderWithTheme(<Header />);

    fireEvent.click(screen.getByRole("button", { name: /Preklopi na/ }));
    expect(document.documentElement.className).toMatch(/dark|light/);
  });
});
