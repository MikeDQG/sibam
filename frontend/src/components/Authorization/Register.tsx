import { useState } from "react";
import { useNavigate } from "react-router";
import { ArrowLeft, Check, Eye, EyeOff, X } from "lucide-react";
import { FaGoogle } from "react-icons/fa";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Separator } from "../ui/separator";
import { auth } from "../../firebase";
import { ThemeToggle } from "../ThemeToggle";
import {
  createUserWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
} from "firebase/auth";

export const Register = () => {
  const [showPassword, setShowPassword] = useState(false);
  const [showRepeatedPassword, setShowRepeatedPassword] = useState(false);
  const [password, setPassword] = useState("");

  const [email, setEmail] = useState("");
  const [fullName, setFullName] = useState("");
  const [repeatedPassword, setRepeatedPassword] = useState("");

  const [error, setError] = useState("");

  const navigate = useNavigate();

  const handleRegister = async () => {
    const allRequirementsMet = passwordRequirements.every((r) => r.isValid);
    if (!allRequirementsMet) {
      setError("Geslo ne ustreza zahtevam!");
      return;
    }

    if (fullName.trim() === "") {
      setError("Vnesite ime in priimek.");
      return;
    }

    if (password !== repeatedPassword) {
      setError("Gesli se ne ujemata!");
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setError("Vnesite veljaven email naslov.");
      return;
    }

    try {
      const userCredential = await createUserWithEmailAndPassword(
        auth,
        email,
        password,
      );
      const token = await userCredential.user.getIdToken();

      await fetch("http://localhost:8080/api/users/me", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "X-Full-Name": fullName,
        },
      });

      navigate("/account");
    } catch (error: any) {
      switch (error.code) {
        case "auth/email-already-in-use":
          setError("Ta email je že registriran.");
          break;
        case "auth/invalid-email":
          setError("Vnesite veljaven email naslov.");
          break;
        case "auth/weak-password":
          setError("Geslo je prešibko.");
          break;
        default:
          setError("Prišlo je do napake. Poskusite znova.");
      }
    }
  };

  const handleGoogleRegister = async () => {
    try {
      const provider = new GoogleAuthProvider();
      const userCredential = await signInWithPopup(auth, provider);
      const token = await userCredential.user.getIdToken();

      await fetch("http://localhost:8080/api/users/me", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      navigate("/account");
    } catch (error: any) {
      setError("Prišlo je do napake pri Google registraciji.");
    }
  };

  const passwordRequirements = [
    {
      label: "Vsaj 12 znakov",
      isValid: password.length >= 12,
    },
    {
      label: "Velike črke",
      isValid: /[A-ZČŠŽ]/.test(password),
    },
    {
      label: "Male črke",
      isValid: /[a-zčšž]/.test(password),
    },
    {
      label: "Posebni znaki",
      isValid: /[^A-Za-z0-9ČŠŽčšž]/.test(password),
    },
  ];

  return (
    <div className='relative flex min-h-screen w-full items-center justify-end bg-neutral-800'>
      <img
        src='logo.svg'
        className='absolute left-9 top-6 z-2 h-15 w-auto'
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
            className='absolute left-6 top-6 flex cursor-pointer items-center gap-2 text-sm font-medium text-foreground transition-colors hover:text-muted-foreground dark:text-white dark:hover:text-neutral-400'>
            <ArrowLeft size={18} />
            Nazaj domov
          </button>
          <h1 className='text-4xl font-semibold'>Registracija</h1>
          <form className='flex w-full max-w-75 flex-col items-center gap-5'>
            <Input
              type='text'
              placeholder='Ime in priimek'
              className='w-full'
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
            />
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
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                className='w-full pr-10'
              />
              <button
                type='button'
                onClick={() => setShowPassword((current) => !current)}
                className='absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground dark:hover:text-white'
                aria-label={showPassword ? "Skrij geslo" : "Prikaži geslo"}>
                {showPassword ? <Eye size={18} /> : <EyeOff size={18} />}
              </button>
            </div>

            <div className='relative w-full'>
              <Input
                type={showRepeatedPassword ? "text" : "password"}
                placeholder='Ponovi geslo'
                className='w-full pr-10'
                value={repeatedPassword}
                onChange={(e) => setRepeatedPassword(e.target.value)}
              />
              <button
                type='button'
                onClick={() => setShowRepeatedPassword((current) => !current)}
                className='absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground dark:hover:text-white'
                aria-label={
                  showRepeatedPassword ? "Skrij geslo" : "Prikaži geslo"
                }>
                {showRepeatedPassword ? (
                  <Eye size={18} />
                ) : (
                  <EyeOff size={18} />
                )}
              </button>
            </div>
            <div className='w-full space-y-2 text-left text-sm'>
              <p>Geslo mora vsebovati:</p>
              <ul className='space-y-1'>
                {passwordRequirements.map((requirement) => (
                  <li
                    key={requirement.label}
                    className='flex items-center gap-2 text-muted-foreground dark:text-white/70'>
                    {requirement.isValid ? (
                      <Check size={16} className='shrink-0 text-green-400' />
                    ) : (
                      <X size={16} className='shrink-0 text-red-400' />
                    )}
                    {requirement.label}
                  </li>
                ))}
              </ul>
            </div>
          </form>
          {error && (
            <p className='text-red-400 text-sm text-center w-full max-w-75'>
              {error}
            </p>
          )}

          <Button
            onClick={handleRegister}
            type='submit'
            className='rounded-md bg-red-700 px-4 py-2 font-semibold text-white transition-colors hover:bg-red-800'>
            Registriraj se
          </Button>

          <div className='text-medium flex w-[95%] items-center gap-3 px-10 text-muted-foreground dark:text-white'>
            <Separator className='flex-1' />
            <span className='shrink-0 text-sm font-normal'>
              ali se registriraj z
            </span>
            <Separator className='flex-1' />
          </div>

          <Button
            onClick={handleGoogleRegister}
            type='submit'
            className='rounded-md bg-gray-500 px-4 py-2 font-semibold text-white transition-colors hover:bg-gray-400'>
            <FaGoogle /> Google
          </Button>
          <p className='w-full max-w-75 text-center text-sm'>
            Že imaš račun?{" "}
            <button
              type='button'
              onClick={() => navigate("/login")}
              className='cursor-pointer font-medium transition-colors hover:underline'>
              Prijavi se
            </button>
          </p>
        </div>
      </div>
    </div>
  );
};
