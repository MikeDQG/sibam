import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { auth } from "../../firebase";
import { onAuthStateChanged } from "firebase/auth";
import { ArrowLeft } from "lucide-react";

export const AccountPage = () => {
  const [email, setEmail] = useState<string | null>(null);
  const navigate = useNavigate();
  const [fullName, setFullName] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      if (user) {
        setEmail(user.email);
        setFullName(user.displayName?.split(" ")[0] ?? null);
      } else {
        navigate("/login");
      }
    });
    return () => unsubscribe();
  }, []);

  return (
    <div className="flex min-h-screen w-full flex-col items-center bg-[url('/LandingPage/background.jpeg')] bg-cover bg-center px-6 pt-25">
      <img
        src='/logo.svg'
        className='absolute left-9 top-6 h-15 w-auto cursor-pointer'
        alt='Logo'
        onClick={() => navigate("/")}
      />
      <div className='flex w-full max-w-[80vw] flex-col gap-6 rounded-lg bg-card/95 p-10 text-card-foreground shadow-xl backdrop-blur-sm dark:bg-neutral-700/95'>
        <button
          type='button'
          onClick={() => navigate("/home")}
          className='flex w-fit cursor-pointer items-center gap-2 text-sm font-medium text-foreground transition-colors hover:text-muted-foreground dark:text-white dark:hover:text-neutral-300'>
          <ArrowLeft size={18} />
          Domov
        </button>
        <h1 className='text-3xl font-semibold'>
          Zdravo{fullName ? `, ${fullName}` : ""}!
        </h1>
        <p className='text-sm text-muted-foreground'>
          Prijavljen si z emailom{" "}
          <span className='font-medium text-foreground dark:text-white'>
            {email}
          </span>
        </p>

        <div className='border-t border-border pt-4 dark:border-neutral-600'>
          <h2 className='mb-4 text-xl font-semibold'>Shranjene lokacije</h2>
          <div className='flex flex-col gap-3'>
            <div className='rounded-md bg-muted p-4 text-center text-sm text-muted-foreground dark:bg-neutral-600 dark:text-neutral-400'>
              Ni še shranjenih lokacij.
            </div>
          </div>
        </div>

        <div className='border-t border-border pt-4 dark:border-neutral-600'>
          <h2 className='mb-4 text-xl font-semibold'>Zadnje poti</h2>
          <div className='flex flex-col gap-3'>
            <div className='rounded-md bg-muted p-4 text-center text-sm text-muted-foreground dark:bg-neutral-600 dark:text-neutral-400'>
              Ni še zadnjih poti.
            </div>
          </div>
        </div>

        <button
          onClick={() => {
            auth.signOut();
            navigate("/login");
          }}
          className='h-10 w-fit cursor-pointer flex items-center justify-center rounded-lg bg-red-700 px-7 py-3 text-lg font-semibold text-white transition-colors hover:bg-red-800'>
          Odjava
        </button>
      </div>
    </div>
  );
};
