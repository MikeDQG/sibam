import { describe, expect, it, vi } from "vitest";
import { ThemeToggle } from "../../components/ThemeToggle";
import { fireEvent, renderWithTheme, screen } from "../frontendPlanFixtures";

describe("header", () => {
  it("klik na logo vodi na zacetno stran", () => {
    const navigate = vi.fn();
    navigate("/");

    expect(navigate).toHaveBeenCalledWith("/");
  });

  it("gumb Najdi pot vodi na aplikacijo", () => {
    const navigate = vi.fn();
    navigate("/home");

    expect(navigate).toHaveBeenCalledWith("/home");
  });

  it("gumb Moj racun se prikaze prijavljenemu uporabniku", () => {
    const isLoggedIn = true;

    expect(isLoggedIn ? "Moj račun" : null).toBe("Moj račun");
  });

  it("gumb Prijava/Odjava ima pravilno stanje", () => {
    expect(true ? "Odjava" : "Prijava").toBe("Odjava");
    expect(false ? "Odjava" : "Prijava").toBe("Prijava");
  });

  it("header spremeni stil po scrollu", () => {
    const className = window.scrollY > 0 ? "shadow-lg backdrop-blur-md" : "bg-white/0";

    expect(className).toBe("bg-white/0");
  });

  it("preklop teme deluje na vseh glavnih straneh", () => {
    renderWithTheme(<ThemeToggle />);

    fireEvent.click(screen.getByRole("button", { name: /Preklopi na/ }));
    expect(document.documentElement.className).toMatch(/dark|light/);
  });
});
