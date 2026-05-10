import {
  CloudRain,
  LocateFixed,
  Minus,
  Plus,
  Search,
  UserRound,
} from "lucide-react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";

export const MainAppControlOverlay = () => {
  return (
    <>
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

      {/* controls */}
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
    </>
  );
};
