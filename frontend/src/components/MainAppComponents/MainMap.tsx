import { AdvancedMarker, Map } from "@vis.gl/react-google-maps";

type MapCenter = {
    lat: number;
    lng: number;
};

type MainMapProps = {
    center: MapCenter;
    zoom: number;
    onCameraChanged?: (center: MapCenter, zoom: number) => void;
    markerPosition?: MapCenter | null;
};

export const MainMap = ({
    center,
    zoom,
    onCameraChanged,
    markerPosition,
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
            </Map>
        </div>
    );
};
