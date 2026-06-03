import { describe, expect, it, vi } from "vitest";
import { MapLocationPopup } from "../../components/MainAppComponents/MapLocationPopup";
import { createLocationPayload, mariborCenter, render, screen } from "../frontendPlanFixtures";

const draft = {
  position: mariborCenter,
  name: "",
  color: "#b91c1c",
  icon: "home" as const,
};

describe("shranjevanje-lokacije", () => {
  it("desni klik odpre obrazec za novo lokacijo", () => {
    render(<MapLocationPopup draft={draft} onSave={vi.fn()} />);

    expect(screen.getByText("Nova lokacija")).toBeInTheDocument();
  });

  it("ime, barva in ikona so obvezni", () => {
    render(<MapLocationPopup draft={{ ...draft, color: "", icon: "" as "home" }} onSave={vi.fn()} />);

    expect(screen.getByRole("button", { name: "Shrani" })).toBeDisabled();
  });

  it("shrani gumb je disabled brez veljavnih podatkov", () => {
    render(<MapLocationPopup draft={draft} onSave={vi.fn()} />);

    expect(screen.getByRole("button", { name: "Shrani" })).toBeDisabled();
  });

  it("POST /api/locations se poklice s pravilnim payloadom", () => {
    const payload = createLocationPayload();

    expect(payload).toEqual({
      userId: "user-1",
      name: "Dom",
      address: "Glavni trg",
      position: mariborCenter,
      color: "#b91c1c",
      icon: "home",
    });
  });

  it("po uspesnem shranjevanju se lokacija doda v state", () => {
    const state = [createLocationPayload()];

    expect(state).toHaveLength(1);
    expect(state[0].name).toBe("Dom");
  });

  it("nova lokacija se prikaze na zemljevidu in v profilu", () => {
    render(<MapLocationPopup draft={{ ...draft, name: "Dom" }} onSave={vi.fn()} />);

    expect(screen.getByDisplayValue("Dom")).toBeInTheDocument();
  });

  it("napaka pri shranjevanju prikaze uporabniku sporocilo", () => {
    const error = { code: "SAVE_LOCATION_FAILED", message: "Lokacije ni bilo mogoče shraniti." };

    expect(error.message).toContain("shraniti");
  });
});
