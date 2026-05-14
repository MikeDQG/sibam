import {
    CloudRain,
    LocateFixed,
    Minus,
    Route,
    Plus,
    Search,
    UserRound,
    X,
    LogOut,
    ArrowUpDown,
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
    onPlaceSelect?: (place: { lat: number; lng: number } | null) => void;
    onDestinationSelect?: (place: { lat: number; lng: number } | null) => void;
};

export const MainAppControlOverlay = ({
    onZoomIn,
    onZoomOut,
    onLocate,
    onPlaceSelect,
    onDestinationSelect,
}: MainAppControlOverlayProps) => {
    const navigate = useNavigate();
    const [isLoggedIn, setIsLoggedIn] = useState(false);

    const [inputValue, setInputValue] = useState("");
    const [predictions, setPredictions] = useState<PlaceSuggestion[]>([]);
    const [isOpen, setIsOpen] = useState(false);
    const [locationError, setLocationError] = useState(false);
    const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
    const containerRef = useRef<HTMLDivElement>(null);

    const [selectedPlace, setSelectedPlace] = useState("");
    const [showDirections, setShowDirections] = useState(false);
    const [destinationValue, setDestinationValue] = useState("");
    const [destinationPredictions, setDestinationPredictions] = useState<
        PlaceSuggestion[]
    >([]);
    const [destinationOpen, setDestinationOpen] = useState(false);
    const destinationDebounceTimer = useRef<ReturnType<
        typeof setTimeout
    > | null>(null);

    const [originCoords, setOriginCoords] = useState<{
        lat: number;
        lng: number;
    } | null>(null);
    const [destinationCoords, setDestinationCoords] = useState<{
        lat: number;
        lng: number;
    } | null>(null);

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

    const fetchDestinationPredictions = useCallback(async (value: string) => {
        if (!value.trim()) {
            setDestinationPredictions([]);
            setDestinationOpen(false);
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
            setDestinationPredictions(preds);
            setDestinationOpen(preds.length > 0);
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
        setSelectedPlace("");
        setOriginCoords(null);
        onPlaceSelect?.(null);
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
                setSelectedPlace(prediction.mainText);
                const coords = {
                    lat: data.location.latitude,
                    lng: data.location.longitude,
                };
                setOriginCoords(coords);
                onPlaceSelect?.(coords);
            } else {
                setLocationError(true);
            }
        } catch {
            setLocationError(true);
        }
    }

    async function handleDestinationSelect(prediction: PlaceSuggestion) {
        setDestinationValue(prediction.mainText);
        setDestinationOpen(false);
        setDestinationPredictions([]);
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
                const coords = {
                    lat: data.location.latitude,
                    lng: data.location.longitude,
                };
                setDestinationCoords(coords);
                onDestinationSelect?.(coords);
            } else {
                setLocationError(true);
            }
        } catch {
            setLocationError(true);
        }
    }

    function handleSwap() {
        const tempValue = inputValue;
        setInputValue(destinationValue);
        setDestinationValue(tempValue);
        if (originCoords && destinationCoords) {
            onPlaceSelect?.(destinationCoords);
            onDestinationSelect?.(originCoords);
            setOriginCoords(destinationCoords);
            setDestinationCoords(originCoords);
        }
    }

    function handleDestinationInputChange(
        e: React.ChangeEvent<HTMLInputElement>,
    ) {
        const value = e.target.value;
        setDestinationValue(value);
        if (destinationDebounceTimer.current)
            clearTimeout(destinationDebounceTimer.current);
        destinationDebounceTimer.current = setTimeout(
            () => fetchDestinationPredictions(value),
            300,
        );
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
        <div className="absolute inset-x-4 top-4 z-20 flex flex-row items-start gap-2">
            {/* logotip */}
            <img
                src="/logo.svg"
                alt="ŠibaM"
                className={`${showDirections ? "mt-6" : "mt-0"} h-15 w-auto shrink-0 cursor-pointer`}
                onClick={() => navigate("/")}
            />

            {/* searchbar */}
            <div ref={containerRef} className="flex w-110 flex-col gap-1 mt-3">
                {showDirections ? (
                    <>
                        <div className="relative">
                            <div className="overflow-hidden rounded-lg bg-neutral-700 shadow-md">
                                <div className="relative flex h-10 items-center">
                                    <Search
                                        size={16}
                                        className="pointer-events-none absolute left-3 z-10 shrink-0 text-neutral-400"
                                    />
                                    <Input
                                        type="text"
                                        value={destinationValue}
                                        onChange={handleDestinationInputChange}
                                        onKeyDown={(e) =>
                                            e.key === "Escape" &&
                                            setDestinationOpen(false)
                                        }
                                        placeholder="Kje štartaš?"
                                        className="h-full rounded-none border-0 bg-transparent pl-8 pr-12 text-sm font-normal shadow-none"
                                        aria-label="Kje štartaš?"
                                    />
                                    {destinationValue && (
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setDestinationValue("");
                                                setDestinationPredictions([]);
                                                setDestinationOpen(false);
                                                setDestinationCoords(null);
                                                onDestinationSelect?.(null);
                                            }}
                                            className="absolute right-10 z-10 flex h-5 w-5 items-center justify-center rounded-full text-neutral-400 hover:text-white"
                                            aria-label="Počisti">
                                            <X size={13} />
                                        </button>
                                    )}
                                </div>
                                <div className="h-px bg-neutral-600" />
                                <div className="relative flex h-10 items-center">
                                    <Search
                                        size={16}
                                        className="pointer-events-none absolute left-3 z-10 shrink-0 text-neutral-400"
                                    />
                                    <Input
                                        type="text"
                                        value={inputValue}
                                        onChange={handleInputChange}
                                        onKeyDown={(e) =>
                                            e.key === "Escape" &&
                                            setIsOpen(false)
                                        }
                                        placeholder="Kam šibaš?"
                                        className="h-full rounded-none border-0 bg-transparent pl-8 pr-12 text-sm font-normal shadow-none"
                                        aria-label="Kam šibaš?"
                                    />
                                    {inputValue && (
                                        <button
                                            type="button"
                                            onClick={handleClear}
                                            className="absolute right-10 z-10 flex h-5 w-5 items-center justify-center rounded-full text-neutral-400 hover:text-white"
                                            aria-label="Počisti">
                                            <X size={13} />
                                        </button>
                                    )}
                                </div>
                            </div>
                            <button
                                type="button"
                                onClick={handleSwap}
                                className="absolute right-2 top-1/2 z-10 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-full bg-neutral-600 text-neutral-300 shadow-md transition-colors hover:text-white"
                                aria-label="Zamenjaj smeri">
                                <ArrowUpDown size={16} />
                            </button>
                        </div>
                        {destinationOpen &&
                            destinationPredictions.length > 0 && (
                                <ul className="overflow-hidden rounded-lg bg-neutral-700 shadow-lg">
                                    {destinationPredictions.map(
                                        (prediction) => (
                                            <li
                                                key={prediction.placeId}
                                                onMouseDown={() =>
                                                    handleDestinationSelect(
                                                        prediction,
                                                    )
                                                }
                                                className="cursor-pointer border-b border-neutral-600 px-3 py-2 last:border-0 hover:bg-neutral-600">
                                                <p className="text-sm font-medium leading-tight text-white">
                                                    {prediction.mainText}
                                                </p>
                                                <p className="mt-0.5 text-xs leading-tight text-neutral-400">
                                                    {prediction.secondaryText}
                                                </p>
                                            </li>
                                        ),
                                    )}
                                </ul>
                            )}
                        {isOpen && predictions.length > 0 && (
                            <ul className="overflow-hidden rounded-lg bg-neutral-700 shadow-lg">
                                {predictions.map((prediction) => (
                                    <li
                                        key={prediction.placeId}
                                        onMouseDown={() =>
                                            handleSelect(prediction)
                                        }
                                        className="cursor-pointer border-b border-neutral-600 px-3 py-2 last:border-0 hover:bg-neutral-600">
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
                    </>
                ) : (
                    <>
                        <div className="relative flex h-10 items-center">
                            <Search
                                size={16}
                                className="pointer-events-none absolute left-3 z-10 shrink-0 text-neutral-400"
                            />
                            <Input
                                type="text"
                                value={inputValue}
                                onChange={handleInputChange}
                                onKeyDown={(e) =>
                                    e.key === "Escape" && setIsOpen(false)
                                }
                                placeholder="Kam šibaš?"
                                className={`h-full rounded-lg border-0 bg-neutral-700 pl-8 text-sm font-normal shadow-md ${selectedPlace ? "pr-14" : "pr-8"}`}
                                aria-label="Kam šibaš?"
                            />
                            {inputValue && (
                                <button
                                    type="button"
                                    onClick={handleClear}
                                    className={`absolute z-10 flex h-5 w-5 items-center justify-center rounded-full text-neutral-400 hover:text-white ${selectedPlace ? "right-10" : "right-2"}`}
                                    aria-label="Počisti">
                                    <X size={13} />
                                </button>
                            )}
                            {selectedPlace && !showDirections && (
                                <button
                                    type="button"
                                    onClick={() => setShowDirections(true)}
                                    className="absolute right-2 z-10 flex h-6 w-6 rotate-45 items-center rounded-sm justify-center bg-red-700/80 text-white shadow-sm transition-colors hover:bg-red-600"
                                    aria-label="Navodila za pot">
                                    <Route size={14} className="-rotate-45" />
                                </button>
                            )}
                        </div>
                        {isOpen && predictions.length > 0 && (
                            <ul className="overflow-hidden rounded-lg bg-neutral-700 shadow-lg">
                                {predictions.map((prediction) => (
                                    <li
                                        key={prediction.placeId}
                                        onMouseDown={() =>
                                            handleSelect(prediction)
                                        }
                                        className="cursor-pointer border-b border-neutral-600 px-3 py-2 last:border-0 hover:bg-neutral-600">
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
                    </>
                )}
                {locationError && (
                    <p className="rounded-lg bg-neutral-700 px-3 py-2 text-xs text-red-400 shadow-lg">
                        Lokacije ni bilo mogoče najti. Prosimo poskusite znova.
                    </p>
                )}
            </div>

            {/* vreme */}
            <div className="mt-4 flex h-9 shrink-0 items-center gap-2 rounded-lg bg-red-700/80 px-3 text-white shadow-md">
                <CloudRain size={16} />
                <span className="text-sm">15 °C</span>
            </div>

            {/* Spacer */}
            <div className="flex-1" />

            {/* Desni panel */}
            {isLoggedIn ? (
                <div className="flex shrink-0 mt-4 flex-row gap-2">
                    <Button
                        type="button"
                        onClick={() => navigate("/account")}
                        className="flex h-9 w-9 items-center justify-center rounded-md bg-red-700/80 text-foreground shadow-lg hover:text-red-200"
                        aria-label="Profil">
                        <UserRound strokeWidth={1.7} />
                    </Button>
                    <div className="flex flex-col gap-2">
                        <Button
                            type="button"
                            onClick={() => {
                                auth.signOut();
                                navigate("/login");
                            }}
                            aria-label="Odjava"
                            className="flex h-9 w-9 items-center justify-center rounded-md bg-red-700/80 text-foreground shadow-lg hover:text-red-200">
                            <LogOut />
                        </Button>
                        <Button
                            type="button"
                            onClick={onZoomIn}
                            className="flex h-9 w-9 items-center justify-center rounded-md bg-neutral-700 text-foreground shadow-lg hover:text-red-200"
                            aria-label="Povečaj">
                            <Plus size={20} />
                        </Button>
                        <Button
                            type="button"
                            onClick={onZoomOut}
                            className="flex h-9 w-9 items-center justify-center rounded-md bg-neutral-700 text-foreground shadow-lg hover:text-red-200"
                            aria-label="Pomanjšaj">
                            <Minus size={20} />
                        </Button>
                        <Button
                            type="button"
                            onClick={onLocate}
                            className="flex h-9 w-9 items-center justify-center rounded-md bg-neutral-700 text-foreground shadow-lg hover:text-red-200"
                            aria-label="Moja lokacija">
                            <LocateFixed size={20} />
                        </Button>
                    </div>
                </div>
            ) : (
                <div className="flex shrink-0 flex-col gap-2">
                    <Button
                        type="button"
                        onClick={() => navigate("/login")}
                        className="flex h-9 w-9 items-center justify-center rounded-md bg-red-700/80 text-foreground shadow-lg hover:text-red-200"
                        aria-label="Profil">
                        <UserRound strokeWidth={1.7} />
                    </Button>
                    <Button
                        type="button"
                        onClick={onZoomIn}
                        className="flex h-9 w-9 items-center justify-center rounded-md bg-neutral-700 text-foreground shadow-lg hover:text-red-200"
                        aria-label="Povečaj">
                        <Plus size={20} />
                    </Button>
                    <Button
                        type="button"
                        onClick={onZoomOut}
                        className="flex h-9 w-9 items-center justify-center rounded-md bg-neutral-700 text-foreground shadow-lg hover:text-red-200"
                        aria-label="Pomanjšaj">
                        <Minus size={20} />
                    </Button>
                    <Button
                        type="button"
                        onClick={onLocate}
                        className="flex h-9 w-9 items-center justify-center rounded-md bg-neutral-700 text-foreground shadow-lg hover:text-red-200"
                        aria-label="Moja lokacija">
                        <LocateFixed size={20} />
                    </Button>
                </div>
            )}
        </div>
    );
};
