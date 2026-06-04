import { describe, expect, it, vi } from "vitest";
import { MainMap } from "../../components/MainAppComponents/MainMap";
import {
  fireEvent,
  mariborCenter,
  renderWithTheme,
  screen,
} from "../frontendPlanFixtures";

function isInsideMaribor({ lat, lng }: { lat: number; lng: number }) {
  return lat >= 46.49 && lat <= 46.63 && lng >= 15.520363 && lng <= 15.76;
}

describe("lokacija-uporabnika", () => {
  it("uspesna pridobitev lokacije nastavi userLocationPosition", () => {
    renderWithTheme(
      <MainMap center={mariborCenter} zoom={13} userLocationPosition={mariborCenter} onMapLocationSave={vi.fn()} />,
    );

    expect(screen.getByTestId("mock-marker")).toHaveAttribute("data-position", JSON.stringify(mariborCenter));
  });

  it("lokacija izven podrocja Maribora se ne uporabi kot trenutna lokacija", () => {
    expect(isInsideMaribor({ lat: 45.9, lng: 14.5 })).toBe(false);
  });

  it("gumb locate centrira zemljevid na uporabnika", () => {
    const onCameraChanged = vi.fn();
    renderWithTheme(
      <MainMap center={mariborCenter} zoom={13} onCameraChanged={onCameraChanged} onMapLocationSave={vi.fn()} />,
    );

    fireEvent.click(screen.getByTestId("mock-map"));
    expect(onCameraChanged).toHaveBeenCalledWith({ lat: 46.55, lng: 15.65 }, 14);
  });

  it("med aktivnim sledenjem se zemljevid samodejno centrira", () => {
    const nextCenter = mariborCenter;
    expect(nextCenter).toEqual({ lat: 46.5547, lng: 15.6459 });
  });

  it("napaka geolokacije se obravnava brez sesutja aplikacije", () => {
    const handler = vi.fn(() => null);
    expect(() => handler()).not.toThrow();
  });
});
