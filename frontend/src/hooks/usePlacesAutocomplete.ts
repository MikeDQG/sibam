import { useState, useRef, useCallback } from "react";

export type PlaceSuggestion = {
    placeId: string;
    mainText: string;
    secondaryText: string;
};

export const MARIBOR_BOUNDS = {
    low: { latitude: 46.49, longitude: 15.520363 },
    high: { latitude: 46.63, longitude: 15.76 },
};

export function usePlacesAutocomplete(apiKey: string) {
    const [value, setValue] = useState("");
    const [predictions, setPredictions] = useState<PlaceSuggestion[]>([]);
    const [isOpen, setIsOpen] = useState(false);
    const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

    const fetchAutocomplete = useCallback(
        async (input: string) => {
            if (!input.trim()) {
                setPredictions([]);
                setIsOpen(false);
                return;
            }
            try {
                const res = await fetch(
                    "https://places.googleapis.com/v1/places:autocomplete",
                    {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json",
                            "X-Goog-Api-Key": apiKey,
                        },
                        body: JSON.stringify({
                            input,
                            includedRegionCodes: ["si"],
                            languageCode: "sl",
                            locationRestriction: { rectangle: MARIBOR_BOUNDS },
                        }),
                    },
                );
                const data = await res.json();
                const preds: PlaceSuggestion[] = (data.suggestions ?? [])
                    .filter(
                        (s: { placePrediction?: { placeId?: string } }) =>
                            s.placePrediction,
                    )
                    .map(
                        (s: {
                            placePrediction: {
                                placeId: string;
                                structuredFormat?: {
                                    mainText?: { text?: string };
                                    secondaryText?: { text?: string };
                                };
                                text?: { text?: string };
                            };
                        }) => ({
                            placeId: s.placePrediction.placeId,
                            mainText:
                                s.placePrediction.structuredFormat?.mainText
                                    ?.text ??
                                s.placePrediction.text?.text ??
                                "",
                            secondaryText:
                                s.placePrediction.structuredFormat?.secondaryText
                                    ?.text ?? "",
                        }),
                    );
                setPredictions(preds);
                setIsOpen(preds.length > 0);
            } catch {
                setPredictions([]);
                setIsOpen(false);
            }
        },
        [apiKey],
    );

    function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
        const v = e.target.value;
        setValue(v);
        if (debounceTimer.current) clearTimeout(debounceTimer.current);
        debounceTimer.current = setTimeout(() => fetchAutocomplete(v), 300);
    }

    function clear() {
        setValue("");
        setPredictions([]);
        setIsOpen(false);
    }

    function closeDropdown() {
        setPredictions([]);
        setIsOpen(false);
    }

    return { value, setValue, predictions, isOpen, setIsOpen, handleChange, clear, closeDropdown };
}
