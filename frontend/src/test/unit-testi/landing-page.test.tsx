import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import App from "../../App";
import { FeaturesSection } from "../../components/LandingPageComponents/FeaturesSection";
import { Footer } from "../../components/LandingPageComponents/Footer";
import { Header } from "../../components/LandingPageComponents/Header";
import { LandingPage } from "../../components/Pages/LandingPage";
import { fireEvent, renderWithTheme, screen } from "../frontendPlanFixtures";

const authState = vi.hoisted(() => ({
  user: null as { uid: string } | null,
  signOut: vi.fn(),
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

vi.mock("../../components/Authorization/Login", () => ({
  Login: () => <main>Prijavna stran</main>,
}));

vi.mock("../../components/Authorization/Register", () => ({
  Register: () => <main>Registracija</main>,
}));

vi.mock("../../components/Pages/MainAppHome", () => ({
  MainAppHome: () => <main>Aplikacija za poti</main>,
}));

vi.mock("../../components/Pages/AccountPageComponents/AccountPage", () => ({
  AccountPage: () => <main>Uporabniški račun</main>,
}));

function renderRoute(path = "/") {
  window.history.pushState({}, "", path);
  return renderWithTheme(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  );
}

describe("landing page", () => {
  it("LandingPage sestavi header, hero, funkcionalnosti in footer", () => {
    renderWithTheme(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>,
    );

    expect(screen.getByRole("banner")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "šibaM" })).toBeInTheDocument();
    expect(screen.getByText("Funkcionalnosti")).toBeInTheDocument();
    expect(screen.getByText("© 2026 Šibam. All rights reserved.")).toBeInTheDocument();
  });

  it("FeaturesSection prikaže vse sklope in njihove postavke", () => {
    renderWithTheme(<FeaturesSection />);

    expect(screen.getByRole("heading", { name: /Načrtovanje poti/ })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Pametno multimodalno načrtovanje poti" })).toBeInTheDocument();
    expect(screen.getByText("Najhitrejša pot - prioriteta je čas prihoda")).toBeInTheDocument();
    expect(screen.getByText("pregled razpoložljivih Mbajk koles")).toBeInTheDocument();
  });

  it("HeroSection gumb odpre glavno aplikacijo", () => {
    renderRoute("/");

    fireEvent.click(screen.getByRole("button", { name: "Najdi pot" }));

    expect(screen.getByText("Aplikacija za poti")).toBeInTheDocument();
  });

  it("Footer prikaže avtorske pravice", () => {
    renderWithTheme(<Footer />);

    expect(screen.getByText("© 2026 Šibam. All rights reserved.")).toBeInTheDocument();
  });

  it("Header skrije gumb Najdi pot na zacetni strani", () => {
    authState.user = null;

    renderWithTheme(
      <MemoryRouter initialEntries={["/"]}>
        <Header />
      </MemoryRouter>,
    );

    expect(screen.queryByRole("button", { name: "Najdi pot" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Prijava" })).toBeInTheDocument();
  });

  it("App usmeri znane poti na pravilne strani", () => {
    renderRoute("/account");

    expect(screen.getByText("Uporabniški račun")).toBeInTheDocument();
  });
});
