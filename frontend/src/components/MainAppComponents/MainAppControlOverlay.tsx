import {
  Bike,
  Bus,
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
import { WeatherWidget } from "./WeatherWidget";
import { RouteLoadingOverlay } from "./RouteLoadingOverlay";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import type { RoutePath } from "./RoutePolyline";
import { useNavigate } from "react-router-dom";
import { auth } from "../../firebase";
import { onAuthStateChanged } from "firebase/auth";
import { useState, useEffect, useRef } from "react";
import {
  usePlacesAutocomplete,
  type PlaceSuggestion,
} from "../../hooks/usePlacesAutocomplete";

const placesApiKey = import.meta.env.VITE_PLACES_API_KEY as string;

type MainAppControlOverlayProps = {
  onZoomIn?: () => void;
  onZoomOut?: () => void;
  onLocate?: () => void;
  onPlaceSelect?: (place: { lat: number; lng: number } | null) => void;
  onDestinationSelect?: (place: { lat: number; lng: number } | null) => void;
  onPathReceive?: (path: RoutePath) => void;
};

export const MainAppControlOverlay = ({
  onZoomIn,
  onZoomOut,
  onLocate,
  onPlaceSelect,
  onDestinationSelect,
  onPathReceive,
}: MainAppControlOverlayProps) => {
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [locationError, setLocationError] = useState(false);
  const [selectedPlace, setSelectedPlace] = useState("");
  const [showDirections, setShowDirections] = useState(false);
  const [originCoords, setOriginCoords] = useState<{
    lat: number;
    lng: number;
  } | null>(null);
  const [destinationCoords, setDestinationCoords] = useState<{
    lat: number;
    lng: number;
  } | null>(null);
  const [useBus, setUseBus] = useState(true);
  const [useBike, setUseBike] = useState(true);
  const [isLoadingRoute, setIsLoadingRoute] = useState(false);
  const [timeMode, setTimeMode] = useState<"depart" | "arrive">("depart");
  const [selectedTime, setSelectedTime] = useState(() => {
    const now = new Date();
    now.setMinutes(now.getMinutes() + 1);
    return `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
  });
  const containerRef = useRef<HTMLDivElement>(null);

  const origin = usePlacesAutocomplete(placesApiKey);
  const destination = usePlacesAutocomplete(placesApiKey);
  const { setIsOpen: setOriginIsOpen } = origin;
  const { setIsOpen: setDestinationIsOpen } = destination;

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setIsLoggedIn(!!user);
    });
    return () => unsubscribe();
  }, []);

  function handleClear() {
    origin.clear();
    setLocationError(false);
    setSelectedPlace("");
    setOriginCoords(null);
    onPlaceSelect?.(null);
  }

  async function handleSelect(prediction: PlaceSuggestion) {
    origin.setValue(prediction.mainText);
    origin.closeDropdown();
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
    destination.setValue(prediction.mainText);
    destination.closeDropdown();
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
    const tempValue = origin.value;
    origin.setValue(destination.value);
    destination.setValue(tempValue);
    if (originCoords && destinationCoords) {
      onPlaceSelect?.(destinationCoords);
      onDestinationSelect?.(originCoords);
      setOriginCoords(destinationCoords);
      setDestinationCoords(originCoords);
    }
  }

  async function handleRouteRequest() {
    if (!originCoords || !destinationCoords) return;

    setIsLoadingRoute(true);
    try {
      const params = new URLSearchParams({
        originLat: String(originCoords.lat),
        originLon: String(originCoords.lng),
        destinationLat: String(destinationCoords.lat),
        destinationLon: String(destinationCoords.lng),
        originAddress: origin.value,
        destinationAddress: destination.value,
        leaveNow: "false",
        bike: String(useBike),
        bus: String(useBus),
      });

      if (timeMode === "depart") {
        params.set("leaveAt", selectedTime);
      } else {
        params.set("arriveBy", selectedTime);
      }

      if (auth.currentUser?.uid) {
        params.set("userId", auth.currentUser.uid);
      }

      const res = await fetch(
        `${import.meta.env.VITE_API_URL}/compute?${params}`,
      );
      if (!res.ok) {
        throw new Error("Route request failed");
      }

      const journey = (await res.json()) as RoutePath;
      onPathReceive?.(journey);
    } finally {
      setIsLoadingRoute(false);
    }
  }

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setOriginIsOpen(false);
        setDestinationIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [setDestinationIsOpen, setOriginIsOpen]);

  return (
    <>
      {isLoadingRoute && (
        <RouteLoadingOverlay onDismiss={() => setIsLoadingRoute(false)} />
      )}
      <div className='pointer-events-none absolute inset-x-4 top-4 z-20 flex flex-row items-start gap-2'>
        <div className='mt-3 flex min-w-0 flex-1 flex-col gap-2 max-[700px]:w-[80vw] max-[700px]:flex-none min-[700px]:flex-row min-[700px]:items-start'>
          {/* logotip */}
          <img
            src='/logo.svg'
            alt='ŠibaM'
            className='pointer-events-auto h-10 w-auto shrink-0 cursor-pointer'
            onClick={() => navigate("/")}
          />

          {/* searchbar */}
          <div
            ref={containerRef}
            className='pointer-events-auto flex w-full min-w-0 flex-col gap-1 min-[700px]:w-110 min-[700px]:shrink-0'>
            {showDirections ? (
              <>
                <div className='relative'>
                  <div className='overflow-hidden rounded-lg bg-neutral-700 shadow-md'>
                    <div className='relative flex h-10 items-center pr-10'>
                      <Search
                        size={16}
                        className='pointer-events-none absolute left-3 z-10 shrink-0 text-neutral-400'
                      />
                      <Input
                        type='text'
                        value={origin.value}
                        onChange={origin.handleChange}
                        onKeyDown={(e) =>
                          e.key === "Escape" && origin.setIsOpen(false)
                        }
                        placeholder='Kje štartaš?'
                        className='h-full w-auto flex-1 rounded-none border-0 bg-transparent pl-8 pr-2 text-sm font-normal shadow-none focus-visible:ring-0 focus-visible:outline-none'
                        aria-label='Kje štartaš?'
                      />
                      {origin.value && (
                        <button
                          type='button'
                          onClick={handleClear}
                          className='mr-1 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-neutral-400 hover:text-white'
                          aria-label='Počisti'>
                          <X size={13} />
                        </button>
                      )}
                    </div>
                    <div className='h-px bg-neutral-600' />
                    <div className='relative flex h-10 items-center pr-10'>
                      <Search
                        size={16}
                        className='pointer-events-none absolute left-3 z-10 shrink-0 text-neutral-400'
                      />
                      <Input
                        type='text'
                        value={destination.value}
                        onChange={destination.handleChange}
                        onKeyDown={(e) =>
                          e.key === "Escape" && destination.setIsOpen(false)
                        }
                        placeholder='Kam šibaš?'
                        className='h-full w-auto flex-1 rounded-none border-0 bg-transparent pl-8 pr-2 text-sm font-normal shadow-none focus-visible:ring-0 focus-visible:outline-none'
                        aria-label='Kam šibaš?'
                      />
                      {destination.value && (
                        <button
                          type='button'
                          onClick={() => {
                            destination.clear();
                            setDestinationCoords(null);
                            onDestinationSelect?.(null);
                          }}
                          className='mr-1 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-neutral-400 hover:text-white'
                          aria-label='Počisti'>
                          <X size={13} />
                        </button>
                      )}
                    </div>
                  </div>
                  <button
                    type='button'
                    onClick={handleSwap}
                    className='absolute right-2 top-1/2 z-10 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-full bg-neutral-600 text-neutral-300 shadow-md transition-colors hover:text-white'
                    aria-label='Zamenjaj smeri'>
                    <ArrowUpDown size={16} />
                  </button>
                </div>
                {origin.isOpen && origin.predictions.length > 0 && (
                  <ul className='overflow-hidden rounded-lg bg-neutral-700 shadow-lg'>
                    {origin.predictions.map((prediction) => (
                      <li
                        key={prediction.placeId}
                        onMouseDown={() => handleSelect(prediction)}
                        className='cursor-pointer border-b border-neutral-600 px-3 py-2 last:border-0 hover:bg-neutral-600'>
                        <p className='text-sm font-medium leading-tight text-white'>
                          {prediction.mainText}
                        </p>
                        <p className='mt-0.5 text-xs leading-tight text-neutral-400'>
                          {prediction.secondaryText}
                        </p>
                      </li>
                    ))}
                  </ul>
                )}
                {destination.isOpen && destination.predictions.length > 0 && (
                  <ul className='overflow-hidden rounded-lg bg-neutral-700 shadow-lg'>
                    {destination.predictions.map((prediction) => (
                      <li
                        key={prediction.placeId}
                        onMouseDown={() => handleDestinationSelect(prediction)}
                        className='cursor-pointer border-b border-neutral-600 px-3 py-2 last:border-0 hover:bg-neutral-600'>
                        <p className='text-sm font-medium leading-tight text-white'>
                          {prediction.mainText}
                        </p>
                        <p className='mt-0.5 text-xs leading-tight text-neutral-400'>
                          {prediction.secondaryText}
                        </p>
                      </li>
                    ))}
                  </ul>
                )}
                <div className='flex items-center gap-2'>
                  <button
                    type='button'
                    onClick={() => setUseBus((v) => !v)}
                    className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm shadow-md transition-colors ${useBus ? "bg-red-700 text-white" : "bg-neutral-700 text-neutral-400"}`}>
                    <Bus size={14} />
                    Bus
                  </button>
                  <button
                    type='button'
                    onClick={() => setUseBike((v) => !v)}
                    className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm shadow-md transition-colors ${useBike ? "bg-red-700 text-white" : "bg-neutral-700 text-neutral-400"}`}>
                    <Bike size={14} />
                    Kolo
                  </button>
                  <div className='flex overflow-hidden rounded-lg bg-neutral-700 shadow-md'>
                    <button
                      type='button'
                      onClick={() =>
                        setTimeMode((m) =>
                          m === "depart" ? "arrive" : "depart",
                        )
                      }
                      className='whitespace-nowrap px-3 py-1.5 text-sm text-white transition-colors hover:bg-neutral-600'>
                      {timeMode === "depart" ? "Odhod ob" : "Prihod do"}
                    </button>
                    <div className='w-px bg-neutral-600' />
                    <input
                      type='time'
                      value={selectedTime}
                      onChange={(e) => setSelectedTime(e.target.value)}
                      className='bg-transparent px-2 py-1.5 text-sm text-white focus:outline-none'
                    />
                  </div>
                  <button
                    type='button'
                    onClick={handleRouteRequest}
                    disabled={!originCoords || !destinationCoords}
                    className='ml-auto flex items-center gap-1.5 whitespace-nowrap rounded-md bg-neutral-200 px-4 py-1.5 text-sm font-bold text-red-700 shadow-md transition-colors hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-40'>
                    Najdi pot
                  </button>
                </div>
              </>
            ) : (
              <>
                <div className='relative flex h-10 items-center rounded-lg bg-neutral-700 shadow-md'>
                  <Search
                    size={16}
                    className='pointer-events-none absolute left-3 z-10 shrink-0 text-neutral-400'
                  />
                  <Input
                    type='text'
                    value={destination.value}
                    onChange={destination.handleChange}
                    onKeyDown={(e) =>
                      e.key === "Escape" && destination.setIsOpen(false)
                    }
                    placeholder='Kam šibaš?'
                    className='h-full w-auto flex-1 rounded-lg border-0 bg-transparent pl-8 pr-2 text-sm font-normal shadow-none focus-visible:ring-0 focus-visible:outline-none'
                    aria-label='Kam šibaš?'
                  />
                  {destination.value && (
                    <button
                      type='button'
                      onClick={() => {
                        destination.clear();
                        setSelectedPlace("");
                        setDestinationCoords(null);
                        onDestinationSelect?.(null);
                      }}
                      className='mr-2 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-neutral-400 hover:text-white'
                      aria-label='Počisti'>
                      <X size={13} />
                    </button>
                  )}
                  {selectedPlace && !showDirections && (
                    <button
                      type='button'
                      onClick={() => setShowDirections(true)}
                      className='mr-2 flex h-6 w-6 shrink-0 rotate-45 items-center justify-center rounded-sm bg-red-700 text-white shadow-sm transition-colors hover:bg-red-600'
                      aria-label='Navodila za pot'>
                      <Route size={14} className='-rotate-45' />
                    </button>
                  )}
                </div>
                {destination.isOpen && destination.predictions.length > 0 && (
                  <ul className='overflow-hidden rounded-lg bg-neutral-700 shadow-lg'>
                    {destination.predictions.map((prediction) => (
                      <li
                        key={prediction.placeId}
                        onMouseDown={() => handleDestinationSelect(prediction)}
                        className='cursor-pointer border-b border-neutral-600 px-3 py-2 last:border-0 hover:bg-neutral-600'>
                        <p className='text-sm font-medium leading-tight text-white'>
                          {prediction.mainText}
                        </p>
                        <p className='mt-0.5 text-xs leading-tight text-neutral-400'>
                          {prediction.secondaryText}
                        </p>
                      </li>
                    ))}
                  </ul>
                )}
              </>
            )}
            {locationError && (
              <p className='rounded-lg bg-neutral-700 px-3 py-2 text-xs text-red-400 shadow-lg'>
                Lokacije ni bilo mogoče najti. Prosimo poskusite znova.
              </p>
            )}
          </div>

          {/* vreme */}
          <WeatherWidget />
        </div>

        {/* Desni panel */}
        {isLoggedIn ? (
          <div className='pointer-events-auto absolute right-0 top-4 flex shrink-0 flex-row gap-2'>
            <Button
              type='button'
              onClick={() => navigate("/account")}
              className='flex h-9 w-9 items-center justify-center rounded-md bg-red-700 text-foreground shadow-lg hover:text-red-200'
              aria-label='Profil'>
              <UserRound strokeWidth={1.7} />
            </Button>
            <div className='flex flex-col gap-2'>
              <Button
                type='button'
                onClick={() => {
                  auth.signOut();
                  navigate("/login");
                }}
                aria-label='Odjava'
                className='flex h-9 w-9 items-center justify-center rounded-md bg-red-700 text-foreground shadow-lg hover:text-red-200'>
                <LogOut />
              </Button>
              <Button
                type='button'
                onClick={onZoomIn}
                className='flex h-9 w-9 items-center justify-center rounded-md bg-neutral-700 text-foreground shadow-lg hover:text-red-200'
                aria-label='Povečaj'>
                <Plus size={20} />
              </Button>
              <Button
                type='button'
                onClick={onZoomOut}
                className='flex h-9 w-9 items-center justify-center rounded-md bg-neutral-700 text-foreground shadow-lg hover:text-red-200'
                aria-label='Pomanjšaj'>
                <Minus size={20} />
              </Button>
              <Button
                type='button'
                onClick={onLocate}
                className='flex h-9 w-9 items-center justify-center rounded-md bg-neutral-700 text-foreground shadow-lg hover:text-red-200'
                aria-label='Moja lokacija'>
                <LocateFixed size={20} />
              </Button>
            </div>
          </div>
        ) : (
          <div className='pointer-events-auto absolute right-0 top-3 flex shrink-0 flex-col gap-2'>
            <Button
              type='button'
              onClick={() => navigate("/login")}
              className='flex h-9 w-9 items-center justify-center rounded-md bg-red-700 text-foreground shadow-lg hover:text-red-200'
              aria-label='Profil'>
              <UserRound strokeWidth={1.7} />
            </Button>
            <Button
              type='button'
              onClick={onZoomIn}
              className='flex h-9 w-9 items-center justify-center rounded-md bg-neutral-700 text-foreground shadow-lg hover:text-red-200'
              aria-label='Povečaj'>
              <Plus size={20} />
            </Button>
            <Button
              type='button'
              onClick={onZoomOut}
              className='flex h-9 w-9 items-center justify-center rounded-md bg-neutral-700 text-foreground shadow-lg hover:text-red-200'
              aria-label='Pomanjšaj'>
              <Minus size={20} />
            </Button>
            <Button
              type='button'
              onClick={onLocate}
              className='flex h-9 w-9 items-center justify-center rounded-md bg-neutral-700 text-foreground shadow-lg hover:text-red-200'
              aria-label='Moja lokacija'>
              <LocateFixed size={20} />
            </Button>
          </div>
        )}
      </div>
    </>
  );
};
