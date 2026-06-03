import { describe, expect, it, vi } from "vitest";
import { WeatherWidget } from "../../components/MainAppComponents/WeatherWidget";
import { render, screen, waitFor } from "../frontendPlanFixtures";

describe("vreme", () => {
  it("prikaz temperature in vremenskega stanja", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ main: { temp: 21.4 }, weather: [{ main: "Clear" }] }),
      }),
    );
    render(<WeatherWidget />);

    expect(await screen.findByText("21 °C")).toBeInTheDocument();
  });

  it("loading stanje", () => {
    vi.stubGlobal("fetch", vi.fn().mockReturnValue(new Promise(() => {})));
    render(<WeatherWidget />);

    expect(screen.queryByText(/°C/)).not.toBeInTheDocument();
  });

  it("fallback ob napaki API-ja", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("weather")));
    render(<WeatherWidget />);

    await waitFor(() => expect(screen.queryByText(/°C/)).not.toBeInTheDocument());
  });

  it("formatiranje podatkov", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ main: { temp: 19.8 }, weather: [{ main: "Rain" }] }),
      }),
    );
    render(<WeatherWidget />);

    expect(await screen.findByText("20 °C")).toBeInTheDocument();
  });
});
