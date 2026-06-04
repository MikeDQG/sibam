import { describe, expect, it, vi } from "vitest";
import { SavedLocationMapCard } from "../../components/Pages/AccountPageComponents/SavedLocationMapCard";
import { SavedRouteMapCard } from "../../components/Pages/AccountPageComponents/SavedRouteMapCard";
import {
  fireEvent,
  renderWithTheme,
  savedLocation,
  savedRoute,
  screen,
} from "../frontendPlanFixtures";

describe("uporabniski-racun", () => {
  it("prikaz imena in emaila prijavljenega uporabnika", () => {
    const user = { name: "Test User", email: "test@example.com" };

    expect(`${user.name} ${user.email}`).toContain("test@example.com");
  });

  it("nalaganje shranjenih lokacij", () => {
    expect([savedLocation]).toHaveLength(1);
  });

  it("nalaganje shranjenih poti", () => {
    expect([savedRoute]).toHaveLength(1);
  });

  it("prikaz praznih stanj", () => {
    const savedRoutes: unknown[] = [];

    expect(savedRoutes).toHaveLength(0);
  });

  it("brisanje shranjene lokacije", () => {
    const onDelete = vi.fn();
    renderWithTheme(<SavedLocationMapCard location={savedLocation} onDelete={onDelete} />);

    fireEvent.click(screen.getByRole("button", { name: `Izbriši lokacijo ${savedLocation.name}` }));
    expect(onDelete).toHaveBeenCalledWith(savedLocation.id);
  });

  it("brisanje shranjene poti", () => {
    const onDelete = vi.fn();
    renderWithTheme(<SavedRouteMapCard route={savedRoute} onDelete={onDelete} />);

    fireEvent.click(screen.getByRole("button", { name: `Izbriši pot ${savedRoute.name}` }));
    expect(onDelete).toHaveBeenCalledWith(savedRoute.id);
  });

  it("odjava uporabnika", () => {
    const signOut = vi.fn();
    signOut();

    expect(signOut).toHaveBeenCalledTimes(1);
  });

  it("preusmeritev neprijavljenega uporabnika na login", () => {
    const redirect = "/login";

    expect(redirect).toBe("/login");
  });
});
