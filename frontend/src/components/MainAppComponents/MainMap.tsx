import { AdvancedMarker, Map, useMap } from "@vis.gl/react-google-maps";
import { useEffect } from "react";

type MapCenter = {
    lat: number;
    lng: number;
};

type MainMapProps = {
    center: MapCenter;
    zoom: number;
    onCameraChanged?: (center: MapCenter, zoom: number) => void;
    markerPosition?: MapCenter | null;
    destinationMarkerPosition?: MapCenter | null;
};

function FitBounds({
    origin,
    destination,
}: {
    origin: MapCenter;
    destination: MapCenter;
}) {
    const map = useMap();
    useEffect(() => {
        if (!map) return;
        const bounds = {
            north: Math.max(origin.lat, destination.lat),
            south: Math.min(origin.lat, destination.lat),
            east: Math.max(origin.lng, destination.lng),
            west: Math.min(origin.lng, destination.lng),
        };
        map.fitBounds(bounds, 100);
    }, [map, origin, destination]);

    return null;
}

export const MainMap = ({
    center,
    zoom,
    onCameraChanged,
    markerPosition,
    destinationMarkerPosition,
}: MainMapProps) => {
    return (
        <div className="absolute inset-0 z-0">
            <Map
                center={center}
                zoom={zoom}
                onCameraChanged={(event) => {
                    onCameraChanged?.(event.detail.center, event.detail.zoom);
                }}
                colorScheme="DARK"
                gestureHandling="greedy"
                disableDefaultUI
                reuseMaps
                mapId="DEMO_MAP_ID">
                {markerPosition && <AdvancedMarker position={markerPosition} />}
                {destinationMarkerPosition && (
                    <AdvancedMarker position={destinationMarkerPosition} />
                )}
                {markerPosition && destinationMarkerPosition && (
                    <FitBounds
                        origin={markerPosition}
                        destination={destinationMarkerPosition}
                    />
                )}
            </Map>
        </div>
    );
};
