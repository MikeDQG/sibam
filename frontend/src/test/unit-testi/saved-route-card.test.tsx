import { describe, expect, it, vi } from "vitest";
import { SavedRouteMapCard } from "../../components/Pages/AccountPageComponents/SavedRouteMapCard";
import {
  fireEvent,
  renderWithTheme,
  savedRoute,
  screen,
} from "../frontendPlanFixtures";

describe("saved-route-card", () => {
  it("prikaz imena poti", () => {
    renderWithTheme(<SavedRouteMapCard route={savedRoute} onDelete={vi.fn()} />);

    expect(screen.getByText(savedRoute.name)).toBeInTheDocument();
  });

  it("prikaz trajanja in razdalje", () => {
    renderWithTheme(<SavedRouteMapCard route={savedRoute} onDelete={vi.fn()} />);

    expect(screen.getByText("31 min • 2 km 450 m")).toBeInTheDocument();
  });

  it("prikaz izhodisca in cilja", () => {
    renderWithTheme(<SavedRouteMapCard route={savedRoute} onDelete={vi.fn()} />);

    expect(screen.getByText("Glavni trg → Tabor")).toBeInTheDocument();
  });

  it("prikaz nacinov poti v slovenscini in v CAPS", () => {
    renderWithTheme(<SavedRouteMapCard route={savedRoute} onDelete={vi.fn()} />);

    expect(screen.getByText(/PEŠ \+ AVTOBUS \+ KOLO/)).toBeInTheDocument();
  });

  it("prikaz datuma shranjevanja", () => {
    renderWithTheme(<SavedRouteMapCard route={savedRoute} onDelete={vi.fn()} />);

    expect(screen.getByText(/21\. maj 2026/)).toBeInTheDocument();
  });

  it("mini zemljevid narise pot", () => {
    renderWithTheme(<SavedRouteMapCard route={savedRoute} onDelete={vi.fn()} />);

    expect(screen.getByTestId("mock-map")).toBeInTheDocument();
    expect(screen.getByAltText("Avtobusna postaja")).toBeInTheDocument();
  });

  it("brisanje poti sprozi pravilen callback", () => {
    const onDelete = vi.fn();
    renderWithTheme(<SavedRouteMapCard route={savedRoute} onDelete={onDelete} />);

    fireEvent.click(screen.getByRole("button", { name: `Izbriši pot ${savedRoute.name}` }));
    expect(onDelete).toHaveBeenCalledWith(savedRoute.id);
  });
});
