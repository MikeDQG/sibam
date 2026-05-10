import { Bike, Bus, Footprints } from "lucide-react";
import { MainAppControlOverlay } from "../MainAppComponents/MainAppControlOverlay";
import { MainMap } from "../MainAppComponents/MainMap";
import { RouteOptions } from "../MainAppComponents/RouteOptions";

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

export const MainAppHome = () => {
  return (
    <main className='relative min-h-screen overflow-hidden'>
      {/* map */}
      <MainMap />

      {/* control overlay */}
      <MainAppControlOverlay />

      {/* route options */}
      <RouteOptions routes={routeOptions} />
    </main>
  );
};
