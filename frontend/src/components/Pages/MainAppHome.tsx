import { useEffect, useState } from "react";
import { Bike, Bus, Footprints } from "lucide-react";
import { APIProvider } from "@vis.gl/react-google-maps";
import { MainAppControlOverlay } from "../MainAppComponents/MainAppControlOverlay";
import { MainMap } from "../MainAppComponents/MainMap";
import { RouteOptions } from "../MainAppComponents/RouteOptions";

const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string;

const routeOptions = [
    {
        title: "Najhitrejša",
        time: "18 min",
        className: "border-sky-500 bg-[#941d38] ring-4 ring-sky-500",
        icons: [Bus, Footprints, Bike],
    },
    {
        title: "Najbolj zelena",
        time: "24 min",
        className: "border-neutral-500 bg-[#1d431b]",
        icons: [Footprints, Bike],
    },
    {
        title: "Brez kolesa",
        time: "22 min",
        className: "border-neutral-600 bg-[#2c2c2a]",
        icons: [Bus, Footprints],
    },
];

const fallbackCenter = {
    lat: 46.5547,
    lng: 15.6459,
};

type MapCenter = {
    lat: number;
    lng: number;
};

export const MainAppHome = () => {
    const [center, setCenter] = useState<MapCenter>(fallbackCenter);
    const [zoom, setZoom] = useState(14);
    const [markerPosition, setMarkerPosition] = useState<MapCenter | null>(
        null,
    );
    const [destinationMarkerPosition, setDestinationMarkerPosition] =
        useState<MapCenter | null>(null);

    // iskanje userjeve lokacije
    function locateUser(zoomToUser = false) {
        if (!navigator.geolocation) return;

        navigator.geolocation.getCurrentPosition(
            (position) => {
                setCenter({
                    lat: position.coords.latitude,
                    lng: position.coords.longitude,
                });

                if (zoomToUser) {
                    setZoom(16);
                }
            },
            () => {
                setCenter(fallbackCenter);
            },
            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 60000,
            },
        );
    }

    useEffect(() => {
        locateUser();
    }, []);

    // handlanje overlay kontrol
    function handleZoomIn() {
        setZoom((currentZoom) => Math.min(currentZoom + 1, 20));
    }

    function handleZoomOut() {
        setZoom((currentZoom) => Math.max(currentZoom - 1, 3));
    }

    function handleLocate() {
        locateUser(true);
    }

    function handleCameraChanged(nextCenter: MapCenter, nextZoom: number) {
        setCenter(nextCenter);
        setZoom(nextZoom);
    }

    function handlePlaceSelect(place: { lat: number; lng: number } | null) {
        if (!place) {
            setMarkerPosition(null);
            return;
        }
        setCenter({ lat: place.lat, lng: place.lng });
        setZoom(16);
        setMarkerPosition({ lat: place.lat, lng: place.lng });
    }

    function handleDestinationSelect(place: { lat: number; lng: number } | null) {
        setDestinationMarkerPosition(place);
    }

    if (!apiKey) {
        return (
            <div className="absolute inset-0 z-0 flex items-center justify-center bg-neutral-800 text-neutral-300">
                Manjka Google Maps API key
            </div>
        );
    }

    return (
        <APIProvider apiKey={apiKey} region="SI" language="sl">
            <main className="relative min-h-screen overflow-hidden">
                <MainMap
                    center={center}
                    zoom={zoom}
                    onCameraChanged={handleCameraChanged}
                    markerPosition={markerPosition}
                    destinationMarkerPosition={destinationMarkerPosition}
                />
                <MainAppControlOverlay
                    onZoomIn={handleZoomIn}
                    onZoomOut={handleZoomOut}
                    onLocate={handleLocate}
                    onPlaceSelect={handlePlaceSelect}
                    onDestinationSelect={handleDestinationSelect}
                />
                <RouteOptions routes={routeOptions} />
            </main>
        </APIProvider>
    );
};
