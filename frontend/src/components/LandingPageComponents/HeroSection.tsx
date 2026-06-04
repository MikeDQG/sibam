import { Button } from "@/components/ui/button";
import { useNavigate } from "react-router-dom";

export const HeroSection = () => {
  const navigate = useNavigate();

  return (
    <div className="flex h-screen items-center bg-[url('/LandingPage/background_light.jpg')] bg-cover bg-center dark:bg-[url('/LandingPage/background.jpeg')]">
      <div className='flex w-full flex-col items-center gap-5 lg:flex-row lg:gap-0'>
        <div className='flex flex-1 flex-col items-center text-center lg:ml-[10%] lg:items-start lg:text-left'>
          <h1 className='m-0 p-0 text-center text-[72px] font-bold leading-none text-foreground sm:text-[92px] lg:text-left lg:text-[120px]'>
            šibaM
          </h1>
          <span className='ml-0 text-center text-2xl text-red-500 sm:text-[32px] lg:ml-2 lg:text-left lg:text-[40px]'>
            Načrtuj pot po Mariboru.
          </span>
          <p className='mt-4 max-w-xl text-center text-base font-medium leading-7 text-muted-foreground sm:text-lg lg:ml-2 lg:text-left'>
            Združi hojo, Mbajk in avtobus, izberi pravi čas poti ter shrani
            lokacije in poti za naslednjič.
          </p>
        </div>

        <div className='flex w-[40%] justify-center'>
          <Button
            onClick={() => navigate("/home")}
            className='h-12 rounded-lg bg-red-700 px-7 py-3 text-lg font-semibold text-white transition-colors hover:bg-red-800'>
            Najdi pot
          </Button>
        </div>
      </div>
    </div>
  );
};
