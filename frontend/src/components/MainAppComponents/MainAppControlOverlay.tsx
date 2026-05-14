import {
    CloudRain,
    LocateFixed,
    Minus,
    Plus,
    Search,
    UserRound,
    X,
} from "lucide-react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { useNavigate } from "react-router-dom";
import { auth } from "../../firebase";
import { onAuthStateChanged } from "firebase/auth";
import { useState, useEffect, useRef, useCallback } from "react";

const placesApiKey = import.meta.env.VITE_PLACES_API_KEY as string;

const MARIBOR_BOUNDS = {
    low: { latitude: 46.49, longitude: 15.520363 },
    high: { latitude: 46.63, longitude: 15.76 },
};

type PlaceSuggestion = {
    placeId: string;
    mainText: string;
    secondaryText: string;
};

type MainAppControlOverlayProps = {
    onZoomIn?: () => void;
    onZoomOut?: () => void;
    onLocate?: () => void;
    onPlaceSelect?: (place: { lat: number; lng: number }) => void;
};

export const MainAppControlOverlay = ({
    onZoomIn,
    onZoomOut,
    onLocate,
    onPlaceSelect,
}: MainAppControlOverlayProps) => {
    const navigate = useNavigate();
    const [isLoggedIn, setIsLoggedIn] = useState(false);

    const [inputValue, setInputValue] = useState("");
    const [predictions, setPredictions] = useState<PlaceSuggestion[]>([]);
    const [isOpen, setIsOpen] = useState(false);
    const [locationError, setLocationError] = useState(false);
    const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
    const containerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, (user) => {
            setIsLoggedIn(!!user);
        });
        return () => unsubscribe();
    }, []);

    const fetchPredictions = useCallback(async (value: string) => {
        if (!value.trim()) {
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
                        "X-Goog-Api-Key": placesApiKey,
                    },
                    body: JSON.stringify({
                        input: value,
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
    }, []);

    function handleInputChange(e: React.ChangeEvent<HTMLInputElement>) {
        const value = e.target.value;
        setInputValue(value);
        setLocationError(false);
        if (debounceTimer.current) clearTimeout(debounceTimer.current);
        debounceTimer.current = setTimeout(() => fetchPredictions(value), 300);
    }

    function handleClear() {
        setInputValue("");
        setPredictions([]);
        setIsOpen(false);
        setLocationError(false);
    }

    async function handleSelect(prediction: PlaceSuggestion) {
        setInputValue(prediction.mainText);
        setIsOpen(false);
        setPredictions([]);
        try {
            const res = await fetch(
                `https://places.googleapis.com/v1/places/${prediction.placeId}`,
                {
                    headers: {
                        "X-Goog-Api-Key": placesApiKey,
                        "X-Goog-FieldMask": "location",
                    },
                },
            );
            const data = await res.json();
            if (data.location) {
                setLocationError(false);
                onPlaceSelect?.({
                    lat: data.location.latitude,
                    lng: data.location.longitude,
                });
            } else {
                setLocationError(true);
            }
        } catch {
            setLocationError(true);
        }
    }

    useEffect(() => {
        function handleClickOutside(e: MouseEvent) {
            if (
                containerRef.current &&
                !containerRef.current.contains(e.target as Node)
            ) {
                setIsOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () =>
            document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    return (
        <>
            <div
                ref={containerRef}
                className="absolute left-5 right-5 top-3 z-20 flex flex-col gap-2">
                <div className="relative flex h-11 items-center">
                    <img
                        src="/logo.svg"
                        alt="ŠibaM"
                        className="pointer-events-auto absolute left-2 z-10 h-8 w-auto cursor-pointer"
                        onClick={() => navigate("/")}
                    />
                    <div className="absolute left-13 z-10 h-6 w-px bg-neutral-500" />
                    <Search
                        size={25}
                        className="pointer-events-none absolute left-16 z-10 shrink-0"
                    />
                    <Input
                        type="text"
                        value={inputValue}
                        onChange={handleInputChange}
                        onKeyDown={(e) =>
                            e.key === "Escape" && setIsOpen(false)
                        }
                        placeholder="Kam šibaš?"
                        className="h-full rounded-lg border-0 bg-neutral-700 pl-24 pr-20 text-xl font-normal shadow-md md:text-xl"
                        aria-label="Kam šibaš?"
                    />
                    {inputValue && (
                        <button
                            type="button"
                            onClick={handleClear}
                            className="absolute right-14 z-10 flex h-6 w-6 items-center justify-center rounded-full text-neutral-400 hover:text-white"
                            aria-label="Počisti">
                            <X size={16} />
                        </button>
                    )}
                    <button
                        type="button"
                        onClick={() =>
                            navigate(isLoggedIn ? "/account" : "/login")
                        }
                        className="absolute right-4 z-10 flex h-10 w-10 items-center justify-center rounded-full"
                        aria-label="Profil">
                        <UserRound size={25} strokeWidth={1.7} />
                    </button>
                </div>

                {isOpen && predictions.length > 0 && (
                    <ul className="overflow-hidden rounded-lg bg-neutral-700 shadow-lg">
                        {predictions.map((prediction) => (
                            <li
                                key={prediction.placeId}
                                onMouseDown={() => handleSelect(prediction)}
                                className="cursor-pointer border-b border-neutral-600 px-4 py-2.5 last:border-0 hover:bg-neutral-600">
                                <p className="text-sm font-medium leading-tight text-white">
                                    {prediction.mainText}
                                </p>
                                <p className="mt-0.5 text-xs leading-tight text-neutral-400">
                                    {prediction.secondaryText}
                                </p>
                            </li>
                        ))}
                    </ul>
                )}

                {locationError && (
                    <p className="rounded-lg bg-neutral-700 px-4 py-2.5 text-sm text-red-400 shadow-lg">
                        Lokacije ni bilo mogoče najti. Prosimo poskusite znova.
                    </p>
                )}

                <div className="flex h-8 w-fit items-center gap-4 rounded-sm bg-red-700/80 px-4 text-white shadow-lg">
                    <CloudRain size={20} />
                    <span className="text-sm">15 °C</span>
                </div>
            </div>

            <div className="absolute right-5 top-18 z-20 flex flex-col gap-2">
                <Button
                    type="button"
                    onClick={onZoomIn}
                    className="flex h-9 w-9 items-center justify-center rounded-md hover:text-red-200 bg-neutral-700 text-foreground shadow-lg"
                    aria-label="Povečaj">
                    <Plus size={20} />
                </Button>
                <Button
                    type="button"
                    onClick={onZoomOut}
                    className="flex h-9 w-9 items-center justify-center rounded-md hover:text-red-200 bg-neutral-700 text-foreground shadow-lg"
                    aria-label="Pomanjšaj">
                    <Minus size={20} />
                </Button>
                <Button
                    type="button"
                    onClick={onLocate}
                    className="flex h-9 w-9 items-center justify-center rounded-md hover:text-red-200 bg-neutral-700 text-foreground shadow-lg"
                    aria-label="Moja lokacija">
                    <LocateFixed size={20} />
                </Button>
            </div>
        </>
    );
};
