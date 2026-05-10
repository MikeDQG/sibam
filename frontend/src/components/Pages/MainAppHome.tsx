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
    <main className='relative min-h-screen overflow-hidden bg-[#282827] text-[#d8d5ca]'>
      <div
        className="absolute inset-0 bg-[url('/MainApp/map-screen.png')] bg-cover bg-center"
        aria-hidden='true'
      />

      <div className='absolute left-7 right-10 top-10 z-20 flex h-20 items-center rounded-[18px] bg-[#292928] shadow-xl'>
        <div className='flex h-full w-24 items-center justify-center'>
          <div className='flex h-15 w-15 items-center justify-center rounded-full bg-[#df0b42] text-white'>
            <div className='flex flex-col items-center leading-none'>
              <Bus size={25} strokeWidth={2.4} />
              <Bike size={25} strokeWidth={2.4} />
            </div>
          </div>
        </div>

        <div className='h-12 w-px bg-[#a29f96]' />

        <div className='flex min-w-0 flex-1 items-center gap-8 px-14'>
          <Search size={36} className='shrink-0 text-[#9c9a94]' />
          <span className='truncate text-[29px] font-normal text-[#9c9a94]'>
            Kam šibaš?
          </span>
        </div>

        <button
          type='button'
          className='mr-4 flex h-15 w-15 items-center justify-center rounded-full bg-[#171717] text-[#bbb8ae]'
          aria-label='Profil'
        >
          <UserRound size={36} strokeWidth={1.7} />
        </button>
      </div>

      <div className='absolute left-7 top-[135px] z-20 flex h-10 items-center gap-4 rounded-lg bg-[#df0b42] px-4 text-white shadow-lg'>
        <CloudRain size={31} strokeWidth={2.4} />
        <span className='text-2xl font-medium'>15 °C</span>
      </div>

      <div className='absolute right-10 top-[130px] z-20 flex flex-col gap-2'>
        <MapControl label='Povečaj'>
          <Plus size={40} strokeWidth={3} />
        </MapControl>
        <MapControl label='Pomanjšaj'>
          <Minus size={40} strokeWidth={3} />
        </MapControl>
        <MapControl label='Moja lokacija'>
          <LocateFixed size={38} strokeWidth={2.4} />
        </MapControl>
      </div>

      <section className='absolute bottom-0 left-0 right-0 z-30 rounded-t-[18px] bg-[#292927] px-[6.3%] pb-13 pt-13 shadow-2xl'>
        <div className='absolute left-1/2 top-4 h-1.5 w-17 -translate-x-1/2 rounded-full bg-[#aaa69d]' />

        <div className='grid grid-cols-3 gap-9'>
          {routeOptions.map((option) => (
            <button
              key={option.title}
              type='button'
              className={`h-[160px] rounded-[18px] border px-12 py-7 text-left shadow-lg ${option.className}`}
            >
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

        <div className='mt-6 flex flex-col gap-5'>
          {routeSteps.map((step) => {
            const Icon = step.icon;

            return (
              <div key={step.text} className='flex items-center gap-8'>
                <div
                  className={`flex h-13 w-13 shrink-0 items-center justify-center rounded-full ${step.className}`}
                >
                  <Icon size={31} strokeWidth={1.8} className='text-[#aaa69d]' />
                </div>
                <p className='text-[31px] font-bold leading-tight text-[#d8d5ca]'>
                  {step.text}
                </p>
              </div>
            );
          })}
        </div>
      </section>
    </main>
  );
};

const MapControl = ({
  children,
  label,
}: {
  children: React.ReactNode;
  label: string;
}) => {
  return (
    <button
      type='button'
      className='flex h-16 w-16 items-center justify-center rounded-xl bg-[#3a3936] text-[#aaa69d] shadow-lg'
      aria-label={label}
    >
      {children}
    </button>
  );
};
