import { APIProvider, AdvancedMarker, Map } from "@vis.gl/react-google-maps";
import { Fragment } from "react";
import { RoutePopup, type RoutePopupSelection } from "./RoutePopup";
import { RoutePolyline, type MapPoint, type RouteLeg } from "./RoutePolyline";

const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
const mapId = import.meta.env.VITE_GOOGLE_MAPS_MAP_ID ?? "DEMO_MAP_ID";

type MapCenter = {
  lat: number;
  lng: number;
};

type MainMapProps = {
  center: MapCenter;
  zoom: number;
  legs?: RouteLeg[];
  selectedLeg?: RoutePopupSelection | null;
  onLegClick?: (leg: RouteLeg, position: MapPoint) => void;
  onBusIconClick?: (
    leg: RouteLeg,
    position: MapPoint,
    previousLeg?: RouteLeg,
  ) => void;
  onBikeIconClick?: (
    leg: RouteLeg,
    position: MapPoint,
    source: "bikePickupIcon" | "bikeReturnIcon",
  ) => void;
  onRoutePopupClose?: () => void;
  onCameraChanged?: (center: MapCenter, zoom: number) => void;
};

export const MainMap = ({
  center,
  zoom,
  legs,
  selectedLeg,
  onLegClick,
  onBusIconClick,
  onBikeIconClick,
  onRoutePopupClose,
  onCameraChanged,
}: MainMapProps) => {
  const hasApiKey = apiKey && apiKey !== "your_google_maps_api_key";

  if (!hasApiKey) {
    return (
      <div className='absolute inset-0 z-0 flex items-center justify-center bg-neutral-800 text-neutral-300'>
        Manjka Google Maps API key
      </div>
    );
  }

  return (
    <div className='absolute inset-0 z-0'>
      <APIProvider apiKey={apiKey} region='SI' language='sl'>
        <Map
          center={center}
          zoom={zoom}
          onCameraChanged={(event) => {
            onCameraChanged?.(event.detail.center, event.detail.zoom);
          }}
          colorScheme='DARK'
          gestureHandling='greedy'
          disableDefaultUI
          clickableIcons={false}
          mapId={mapId}
          reuseMaps>
          {legs && <RoutePolyline legs={legs} onLegClick={onLegClick} />}
          {legs?.map((leg, index) => {
            const firstPoint = leg.polyline[0];
            if (!firstPoint || leg.tip === "WALK") return null;

            const nextLeg = legs[index + 1];
            const lastPoint = leg.polyline.at(-1);
            const showBikeReturnMarker =
              leg.tip === "BIKE" && nextLeg?.tip !== "BUS" && lastPoint;
            const markerPosition = {
              lat: firstPoint.lat,
              lng: firstPoint.lon,
            };

            return (
              <Fragment key={index}>
                <AdvancedMarker
                  position={markerPosition}
                  clickable={true}
                  onClick={() => {
                    if (leg.tip === "BIKE") {
                      onBikeIconClick?.(
                        leg,
                        {
                          lat: firstPoint.lat - 0.00002,
                          lng: firstPoint.lon,
                        },
                        "bikePickupIcon",
                      );
                    } else {
                      onBusIconClick?.(
                        leg,
                        {
                          lat: firstPoint.lat - 0.00002,
                          lng: firstPoint.lon,
                        },
                        legs[index - 1],
                      );
                    }
                  }}>
                  <img
                    src={
                      leg.tip === "BIKE"
                        ? "pathIcons/mBajk.png"
                        : "pathIcons/marprom.png"
                    }
                    alt={
                      leg.tip === "BIKE"
                        ? "Bajk postaja za prevzem"
                        : "Avtobusna postaja"
                    }
                    className='h-7 w-7 rounded-full'
                  />
                </AdvancedMarker>

                {showBikeReturnMarker && (
                  <AdvancedMarker
                    position={{
                      lat: lastPoint.lat,
                      lng: lastPoint.lon,
                    }}
                    clickable={true}
                    onClick={() => {
                      onBikeIconClick?.(
                        leg,
                        {
                          lat: lastPoint.lat - 0.00002,
                          lng: lastPoint.lon,
                        },
                        "bikeReturnIcon",
                      );
                    }}>
                    <img
                      src='pathIcons/mBajk.png'
                      alt='Bajk postaja za oddajo'
                      className='h-7 w-7 rounded-full'
                    />
                  </AdvancedMarker>
                )}
              </Fragment>
            );
          })}
          {selectedLeg && (
            <RoutePopup selectedLeg={selectedLeg} onClose={onRoutePopupClose} />
          )}
        </Map>
      </APIProvider>
    </div>
  );
};
