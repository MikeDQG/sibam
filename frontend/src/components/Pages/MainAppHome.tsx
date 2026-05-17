import { useEffect, useState } from "react";
import { Bike, Bus, Footprints } from "lucide-react";
import { MainAppControlOverlay } from "../MainAppComponents/MainAppControlOverlay";
import { MainMap } from "../MainAppComponents/MainMap";
import { RouteOptions } from "../MainAppComponents/RouteOptions";
import type { RoutePopupSelection } from "../MainAppComponents/RoutePopup";
import type { MapPoint, RouteLeg } from "../MainAppComponents/RoutePolyline";
import pathMock from "../../mock/pathMock.json";

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
  const [selectedLeg, setSelectedLeg] = useState<RoutePopupSelection | null>(
    null,
  );

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
    setSelectedLeg({ leg, position, source: "path" });
  }

  function handleBusIconClick(
    leg: RouteLeg,
    position: MapPoint,
    previousLeg?: RouteLeg,
  ) {
    setSelectedLeg({ leg, position, previousLeg, source: "busIcon" });
  }

  return (
    <main className='relative min-h-screen overflow-hidden'>
      {/* map */}
      <MainMap
        center={center}
        zoom={zoom}
        legs={pathMock.legs}
        selectedLeg={selectedLeg}
        onLegClick={handleLegClick}
        onBusIconClick={handleBusIconClick}
        onRoutePopupClose={() => setSelectedLeg(null)}
        onCameraChanged={handleCameraChanged}
      />

      {/* control overlay */}
      <MainAppControlOverlay
        onZoomIn={handleZoomIn}
        onZoomOut={handleZoomOut}
        onLocate={handleLocate}
      />

      {/* route options */}
      <RouteOptions routes={routeOptions} />
    </main>
  );
};
