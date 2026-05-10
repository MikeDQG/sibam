import {
  Bike,
  Bus,
  CloudRain,
  Footprints,
  LocateFixed,
  Minus,
  Plus,
  Search,
  UserRound,
} from "lucide-react";
import { Input } from "../ui/input";
import { Button } from "../ui/button";

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

const routeSteps = [
  {
    icon: Footprints,
    text: "Pojdi peš do postaje Trg svobode (3 min)",
    className: "bg-[#171717]",
  },
  {
    icon: Bus,
    text: "Avtobus G3 do postaje Koroška (8 min)",
    className: "bg-[#b5163f]",
  },
  {
    icon: Bike,
    text: "Vzemi mBajk kolo → cilj (7 min)",
    className: "bg-[#1d431b]",
  },
];

export const MainAppHome = () => {
  return (
    <main className='relative min-h-screen overflow-hidden bg-white text-[#d8d5ca]'>
      <div
        className="absolute inset-0 bg-[url('/MainApp/map-screen.png')] bg-cover bg-center"
        aria-hidden='true'
      />

      {/* search bar */}
      <div className='absolute left-5 right-5 top-3 z-20 flex h-11 items-center'>
        <Search
          size={25}
          className='pointer-events-none absolute left-5 z-10 shrink-0'
        />
        <Input
          type='search'
          placeholder='Kam šibaš?'
          className='h-full rounded-lg border-0 bg-neutral-700 pl-13 text-xl font-normal shadow-md md:text-xl'
          aria-label='Kam šibaš?'
        />
        <button
          type='button'
          className='absolute right-4 z-10 flex h-10 w-10 items-center justify-center rounded-full '
          aria-label='Profil'>
          <UserRound size={25} strokeWidth={1.7} />
        </button>
      </div>

      {/* temperatura */}
      <div className='absolute left-5 top-17 z-20 flex h-8 items-center gap-4 rounded-sm bg-red-700/80 px-4 text-white shadow-lg'>
        <CloudRain size={20} />
        <span className='text-md 7'>15 °C -{">"} temporary</span>
      </div>

      <div className='absolute right-5 top-18 z-20 flex flex-col gap-2'>
        <Button
          type='button'
          className='flex h-9 w-9 items-center justify-center rounded-md hover:text-red-200 bg-neutral-700 text-foreground shadow-lg'
          aria-label='Povečaj'>
          <Plus size={20} />
        </Button>
        <Button
          type='button'
          className='flex h-9 w-9 items-center justify-center rounded-md hover:text-red-200 bg-neutral-700 text-foreground shadow-lg'
          aria-label='Pomanjšaj'>
          <Minus size={20} />
        </Button>
        <Button
          type='button'
          className='flex h-9 w-9 items-center justify-center rounded-md hover:text-red-200 bg-neutral-700 text-foreground shadow-lg'
          aria-label='Moja lokacija'>
          <LocateFixed size={20} />
        </Button>
      </div>

      <section className='absolute bottom-0 left-0 right-0 z-30 rounded-t-[18px] bg-[#292927] px-[6.3%] pb-13 pt-13 shadow-2xl'>
        <div className='absolute left-1/2 top-4 h-1.5 w-17 -translate-x-1/2 rounded-full bg-[#aaa69d]' />

        <div className='grid grid-cols-3 gap-9'>
          {routeOptions.map((option) => (
            <button
              key={option.title}
              type='button'
              className={`h-[160px] rounded-[18px] border px-12 py-7 text-left shadow-lg ${option.className}`}>
              <span className='text-[25px] font-normal text-[#b2ada4]'>
                {option.title}
              </span>
              <strong className='mt-2 block text-[32px] font-bold leading-none text-white'>
                {option.time}
              </strong>
              <div className='mt-4 flex items-center gap-3 text-[#aaa69d]'>
                {option.icons.map((Icon, iconIndex) => (
                  <Icon key={iconIndex} size={26} strokeWidth={1.7} />
                ))}
              </div>
            </button>
          ))}
        </div>

        <div className='mt-6 h-px w-full bg-[#aaa69d]' />
      </section>
    </main>
  );
};
