import { describe, expect, it, vi } from "vitest";
import { SavedLocationMapCard } from "../../components/Pages/AccountPageComponents/SavedLocationMapCard";
import {
  fireEvent,
  renderWithTheme,
  savedLocation,
  screen,
} from "../frontendPlanFixtures";

describe("saved-location-card", () => {
  it("prikaz imena lokacije", () => {
    renderWithTheme(<SavedLocationMapCard location={savedLocation} />);

    expect(screen.getByText(savedLocation.name)).toBeInTheDocument();
  });

  it("prikaz izbrane barve in ikone", () => {
    renderWithTheme(<SavedLocationMapCard location={savedLocation} />);

    expect(screen.getByText(savedLocation.name).previousElementSibling).toHaveStyle({
      backgroundColor: savedLocation.color,
    });
  });

  it("mini zemljevid prikaze marker lokacije", () => {
    renderWithTheme(<SavedLocationMapCard location={savedLocation} />);

    expect(screen.getByTestId("mock-marker")).toHaveAttribute(
      "data-position",
      JSON.stringify(savedLocation.position),
    );
  });

  it("brisanje lokacije sprozi pravilen callback", () => {
    const onDelete = vi.fn();
    renderWithTheme(<SavedLocationMapCard location={savedLocation} onDelete={onDelete} />);

    fireEvent.click(screen.getByRole("button", { name: `Izbriši lokacijo ${savedLocation.name}` }));
    expect(onDelete).toHaveBeenCalledWith(savedLocation.id);
  });

  it("disabled stanje med brisanjem", () => {
    renderWithTheme(<SavedLocationMapCard location={savedLocation} isDeleting onDelete={vi.fn()} />);

    expect(screen.getByRole("button", { name: `Izbriši lokacijo ${savedLocation.name}` })).toBeDisabled();
  });
});
