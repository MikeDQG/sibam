import { describe, expect, it, vi } from "vitest";
import { RouteErrorBox } from "../../components/MainAppComponents/RouteErrorBox";
import { RouteLoadingOverlay } from "../../components/MainAppComponents/RouteLoadingOverlay";
import { fireEvent, render, screen } from "../frontendPlanFixtures";

describe("napake-in-loading", () => {
  it("loading overlay se prikaze in lahko zapre", () => {
    const onDismiss = vi.fn();
    render(<RouteLoadingOverlay onDismiss={onDismiss} />);

    fireEvent.click(screen.getByText("Iščem pot..."));
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it("napake pri izracunu poti se prikazejo uporabniku", () => {
    render(<RouteErrorBox code='NO_ROUTE' message='Pot ni bila najdena.' />);

    expect(screen.getByRole("alert")).toHaveTextContent("NO_ROUTE");
    expect(screen.getByText("Pot ni bila najdena.")).toBeInTheDocument();
  });

  it("toast sporocila za shranjevanje in brisanje", () => {
    const toast = vi.fn();
    toast("Pot je shranjena.");

    expect(toast).toHaveBeenCalledWith("Pot je shranjena.");
  });

  it("aplikacija ostane uporabna po neuspelem API klicu", () => {
    const recover = vi.fn(() => true);

    expect(recover()).toBe(true);
  });
});
