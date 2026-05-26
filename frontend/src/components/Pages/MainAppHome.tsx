import { useEffect, useState } from "react";
import { Bike, Bus, Footprints } from "lucide-react";
import { MainAppControlOverlay } from "../MainAppComponents/MainAppControlOverlay";
import { MainMap } from "../MainAppComponents/MainMap";
import { RouteOptions } from "../MainAppComponents/RouteOptions";
import type { RoutePopupSelection } from "../MainAppComponents/RoutePopup";
import type {
  MapPoint,
  RouteLeg,
  RoutePath,
} from "../MainAppComponents/RoutePolyline";

const routeOptions = [
  {
    title: "Najhitrejša",
    time: "18 min",
    className:
      "border-sky-500 bg-red-50 text-red-950 ring-4 ring-sky-500 dark:bg-[#941d38] dark:text-white",
    icons: [Bus, Footprints, Bike],
  },
  {
    title: "Najbolj zelena",
    time: "24 min",
    className:
      "border-emerald-200 bg-emerald-50 text-emerald-950 dark:border-neutral-500 dark:bg-[#1d431b] dark:text-white",
    icons: [Footprints, Bike],
  },
  {
    title: "Brez kolesa",
    time: "22 min",
    className:
      "border-neutral-200 bg-white text-neutral-950 dark:border-neutral-600 dark:bg-[#2c2c2a] dark:text-white",
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

type MapLocationDraft = {
  position: MapCenter;
  name: string;
  color: string;
};

const defaultLocationColor = "#b91c1c";

export const MainAppHome = () => {
  const [center, setCenter] = useState<MapCenter>(fallbackCenter);
  const [zoom, setZoom] = useState(14);
  const [selectedLeg, setSelectedLeg] = useState<RoutePopupSelection | null>(
    null,
  );
  const [markerPosition, setMarkerPosition] = useState<MapCenter | null>(null);
  const [destinationMarkerPosition, setDestinationMarkerPosition] =
    useState<MapCenter | null>(null);
  const [routePath, setRoutePath] = useState<RoutePath | null>(null);
  const [mapLocationDraft, setMapLocationDraft] =
    useState<MapLocationDraft | null>(null);

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

  function handleLegClick(leg: RouteLeg, position: MapPoint) {
    setMapLocationDraft(null);
    setSelectedLeg({ leg, position, source: "path" });
  }

  function handleBusIconClick(
    leg: RouteLeg,
    position: MapPoint,
    previousLeg?: RouteLeg,
  ) {
    setMapLocationDraft(null);
    setSelectedLeg({ leg, position, previousLeg, source: "busIcon" });
  }

  function handleBikeIconClick(
    leg: RouteLeg,
    position: MapPoint,
    source: "bikePickupIcon" | "bikeReturnIcon",
  ) {
    setMapLocationDraft(null);
    setSelectedLeg({ leg, position, source });
  }

  function handlePlaceSelect(place: { lat: number; lng: number } | null) {
    setRoutePath(null);
    setSelectedLeg(null);
    setMapLocationDraft(null);

    if (!place) {
      setMarkerPosition(null);
      return;
    }

    setCenter({ lat: place.lat, lng: place.lng });
    setZoom(16);
    setMarkerPosition({ lat: place.lat, lng: place.lng });
  }

  function handleDestinationSelect(place: { lat: number; lng: number } | null) {
    setRoutePath(null);
    setSelectedLeg(null);
    setMapLocationDraft(null);
    setDestinationMarkerPosition(place);
  }

  function handlePathReceive(path: RoutePath) {
    setRoutePath(path);
    setSelectedLeg(null);
    setMapLocationDraft(null);
  }

  function handleMapContextSelect(position: MapCenter) {
    setSelectedLeg(null);
    setCenter(position);
    setZoom((currentZoom) => Math.max(currentZoom, 16));
    setMapLocationDraft({
      position,
      name: "",
      color: defaultLocationColor,
    });
  }

  function handleMapLocationColorChange(color: string) {
    setMapLocationDraft((currentDraft) =>
      currentDraft ? { ...currentDraft, color } : currentDraft,
    );
  }

  function handleMapLocationSave(name: string) {
    setMapLocationDraft((currentDraft) =>
      currentDraft ? { ...currentDraft, name } : currentDraft,
    );
    setMapLocationDraft(null);
  }

  return (
    <main className='relative min-h-screen overflow-hidden'>
      {/* map */}
      <MainMap
        center={center}
        zoom={zoom}
        legs={routePath?.legs}
        selectedLeg={selectedLeg}
        onLegClick={handleLegClick}
        onBusIconClick={handleBusIconClick}
        onBikeIconClick={handleBikeIconClick}
        onRoutePopupClose={() => setSelectedLeg(null)}
        onCameraChanged={handleCameraChanged}
        onMapContextSelect={handleMapContextSelect}
        mapLocationDraft={mapLocationDraft}
        onMapLocationColorChange={handleMapLocationColorChange}
        onMapLocationSave={handleMapLocationSave}
        onMapLocationPopupClose={() => setMapLocationDraft(null)}
        markerPosition={markerPosition}
        destinationMarkerPosition={destinationMarkerPosition}
      />

      {/* control overlay */}
      <MainAppControlOverlay
        onZoomIn={handleZoomIn}
        onZoomOut={handleZoomOut}
        onLocate={handleLocate}
        onPlaceSelect={handlePlaceSelect}
        onDestinationSelect={handleDestinationSelect}
        onPathReceive={handlePathReceive}
      />

      {/* route options */}
      <RouteOptions routes={routeOptions} />
    </main>
  );
};
