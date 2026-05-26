import { useNavigate } from "react-router";
import { Separator } from "../ui/separator";
import { Input } from "../ui/input";
import { Button } from "../ui/button";
import { ArrowLeft, Eye, EyeOff } from "lucide-react";
import { useState } from "react";
import { FaGoogle } from "react-icons/fa";
import { auth } from "../../firebase";
import { ThemeToggle } from "../ThemeToggle";
import {
  signInWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
} from "firebase/auth";
import { useUserSession } from "../UserSessionProvider";

export const Login = () => {
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const navigate = useNavigate();
  const { syncUserSession } = useUserSession();

  const handleLogin = async () => {
    try {
      const userCredential = await signInWithEmailAndPassword(
        auth,
        email,
        password,
      );
      await syncUserSession(await userCredential.user.getIdToken());
      navigate("/home");
    } catch (error: any) {
      switch (error.code) {
        case "auth/invalid-credential":
          setError("Napačen email ali geslo.");
          break;
        case "auth/invalid-email":
          setError("Vnesite veljaven email naslov.");
          break;
        case "auth/too-many-requests":
          setError("Preveč neuspešnih poskusov. Poskusite kasneje.");
          break;
        default:
          setError("Prišlo je do napake. Poskusite znova.");
      }
    }
  };

  const handleGoogleLogin = async () => {
    try {
      const provider = new GoogleAuthProvider();
      const userCredential = await signInWithPopup(auth, provider);
      const token = await userCredential.user.getIdToken();

      await syncUserSession(token);
      navigate("/home");
    } catch (error: any) {
      setError("Prišlo je do napake pri Google prijavi.");
    }
  };

  return (
    <div className='relative flex min-h-screen w-full items-center justify-end bg-neutral-800'>
      <img
        src='logo.svg'
        className='absolute left-9 top-6 h-15 w-auto z-2 cursor-pointer'
        alt='Logo'
        onClick={() => navigate("/")}
      />
      <img
        className='absolute left-0 top-0 h-full w-[80%] max-w-[80%] object-cover'
        src='/LandingPage/background.jpeg'
        alt='Background'
      />
      <ThemeToggle className='absolute right-7 top-6 z-20' />
      <div className='z-10 flex min-h-screen w-lg max-w-137.5 py-5 pr-5 lg:w-full lg:max-w-[50%]'>
        <div className='relative flex w-full flex-col items-center gap-8 rounded-lg bg-card pt-20 text-card-foreground shadow-xl dark:bg-neutral-700'>
          <button
            type='button'
            onClick={() => navigate("/")}
            className='absolute left-6 top-6 flex cursor-pointer items-center gap-2 text-sm font-medium text-foreground transition-colors hover:text-muted-foreground dark:text-white dark:hover:text-neutral-300'>
            <ArrowLeft size={18} />
            Nazaj domov
          </button>
          <h1 className='text-4xl font-semibold'>Prijava</h1>
          <form className='flex flex-col items-center gap-5 w-full max-w-75'>
            <Input
              type='email'
              placeholder='Email'
              className='w-full'
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <div className='relative w-full'>
              <Input
                type={showPassword ? "text" : "password"}
                placeholder='Geslo'
                className='w-full pr-10'
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <button
                type='button'
                onClick={() => setShowPassword((current) => !current)}
                className='absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground dark:hover:text-white'
                aria-label={showPassword ? "Skrij geslo" : "Prikaži geslo"}>
                {showPassword ? <Eye size={18} /> : <EyeOff size={18} />}
              </button>
            </div>
          </form>
          {error && (
            <p className='text-red-400 text-sm text-center w-full max-w-75'>
              {error}
            </p>
          )}
          <Button
            onClick={handleLogin}
            className='bg-red-700 hover:bg-red-800 text-white font-semibold py-2 px-4 rounded-md transition-colors'>
            Prijavi se
          </Button>

          <div className='text-medium flex w-[95%] items-center gap-3 px-10 text-muted-foreground dark:text-white'>
            <Separator className='flex-1' />
            <span className='shrink-0 font-normal text-sm'>
              ali se prijavi z
            </span>
            <Separator className='flex-1' />
          </div>
          <Button
            onClick={handleGoogleLogin}
            className='bg-gray-500 hover:bg-gray-400 text-white font-semibold py-2 px-4 rounded-md transition-colors'>
            <FaGoogle /> Google
          </Button>
          <p className='w-full max-w-75 text-center text-sm'>
            Še nimaš profila?{" "}
            <button
              type='button'
              onClick={() => navigate("/register")}
              className='font-medium transition-colors hover:underline cursor-pointer'>
              Registriraj se
            </button>
          </p>
        </div>
      </div>
    </div>
  );
};
