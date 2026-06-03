import { describe, expect, it } from "vitest";
import { RouteOptions } from "../../components/MainAppComponents/RouteOptions";
import {
  findActiveStepIndex,
  render,
  routeLegs,
  routeOptions,
  screen,
} from "../frontendPlanFixtures";

describe("aktivni-step", () => {
  it("aktivni step se izracuna glede na trenutno lokacijo uporabnika", () => {
    expect(findActiveStepIndex(0)).toBe(0);
  });

  it("najblizja polyline tocka doloci trenutni leg", () => {
    expect(findActiveStepIndex(1, [routeLegs[1]])).toBe(0);
  });

  it("startPolylineIndex in endPolylineIndex dolocita trenutni step", () => {
    const step = { instruction: "Test", startPolylineIndex: 3, endPolylineIndex: 5 };
    const inside = 4 >= step.startPolylineIndex && 4 <= step.endPolylineIndex;

    expect(inside).toBe(true);
  });

  it("trenutni step se prikaze nad zemljevidom", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} activeStepIndex={1} />);

    expect(screen.getByText("Pelji se z avtobusom")).toBeInTheDocument();
  });

  it("pretekli, trenutni in prihodnji stepi imajo pravilne vizualne statuse", () => {
    render(<RouteOptions routes={routeOptions} legs={routeLegs} activeStepIndex={1} />);
    const badges = screen.getAllByText(/^[123]$/);

    expect(badges[0].className).toContain("bg-neutral-400");
    expect(badges[1].className).toContain("scale-110");
    expect(badges[2].className).toContain("bg-red-700");
  });

  it("aktivni step se ne prikaze, ko pot ni aktivna ali lokacija ni znana", () => {
    expect(findActiveStepIndex(-1)).toBe(-1);
  });
});
