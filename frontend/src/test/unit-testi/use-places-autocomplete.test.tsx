import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MARIBOR_BOUNDS, usePlacesAutocomplete } from "../../hooks/usePlacesAutocomplete";

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe("usePlacesAutocomplete", () => {
  it("po debounce klice Google autocomplete in normalizira predloge", async () => {
    vi.useFakeTimers();
    const fetchMock = vi.fn().mockResolvedValue({
      json: () =>
        Promise.resolve({
          suggestions: [
            {
              placePrediction: {
                placeId: "place-1",
                structuredFormat: {
                  mainText: { text: "Glavni trg" },
                  secondaryText: { text: "Maribor" },
                },
              },
            },
            {
              placePrediction: {
                placeId: "place-2",
                text: { text: "Tabor, Maribor" },
              },
            },
            { queryPrediction: { text: "brez placePrediction" } },
          ],
        }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const { result } = renderHook(() => usePlacesAutocomplete("places-key"));

    act(() => {
      result.current.handleChange({
        target: { value: "Glavni" },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    expect(result.current.value).toBe("Glavni");

    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });
    vi.useRealTimers();

    await waitFor(() => expect(result.current.isOpen).toBe(true));
    expect(result.current.predictions).toEqual([
      { placeId: "place-1", mainText: "Glavni trg", secondaryText: "Maribor" },
      { placeId: "place-2", mainText: "Tabor, Maribor", secondaryText: "" },
    ]);
    expect(fetchMock).toHaveBeenCalledWith(
      "https://places.googleapis.com/v1/places:autocomplete",
      expect.objectContaining({
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Goog-Api-Key": "places-key",
        },
        body: JSON.stringify({
          input: "Glavni",
          includedRegionCodes: ["si"],
          languageCode: "sl",
          locationRestriction: { rectangle: MARIBOR_BOUNDS },
        }),
      }),
    );
  });

  it("prazen vnos zapre dropdown in ne klice omrezja", async () => {
    vi.useFakeTimers();
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    const { result } = renderHook(() => usePlacesAutocomplete("places-key"));

    act(() => {
      result.current.setIsOpen(true);
      result.current.handleChange({
        target: { value: "   " },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });

    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.predictions).toEqual([]);
    expect(result.current.isOpen).toBe(false);
  });

  it("clear in closeDropdown pocistita stanje", () => {
    const { result } = renderHook(() => usePlacesAutocomplete("places-key"));

    act(() => {
      result.current.setValue("Tabor");
      result.current.setIsOpen(true);
      result.current.clear();
    });

    expect(result.current.value).toBe("");
    expect(result.current.predictions).toEqual([]);
    expect(result.current.isOpen).toBe(false);

    act(() => {
      result.current.setIsOpen(true);
      result.current.closeDropdown();
    });

    expect(result.current.isOpen).toBe(false);
  });

  it("napaka pri fetchu zapre dropdown", async () => {
    vi.useFakeTimers();
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("network")));

    const { result } = renderHook(() => usePlacesAutocomplete("places-key"));

    act(() => {
      result.current.handleChange({
        target: { value: "Tabor" },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });
    vi.useRealTimers();

    await waitFor(() => expect(result.current.predictions).toEqual([]));
    expect(result.current.isOpen).toBe(false);
  });
});
