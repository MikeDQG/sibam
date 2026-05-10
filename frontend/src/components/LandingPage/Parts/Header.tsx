import { Button } from "@/components/ui/button";
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

export const Header = () => {
  const navigate = useNavigate();

  const [isScrolled, setIsScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 0);
    };

    handleScroll();
    window.addEventListener("scroll", handleScroll);

    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <header
      className={`fixed left-3 right-3 top-3 z-50 flex items-center justify-between rounded-xl px-5 py-3 transition-colors duration-300 ${
        isScrolled ? "bg-neutral-600 shadow-lg" : "bg-neutral-600/00"
      }`}>
      <Link to='/' aria-label='Šibam home'>
        <img src='/logo.svg' alt='Šibam' className='h-10 w-auto' />
      </Link>

      <Button
        onClick={() => navigate("/login")}
        className='rounded-lg bg-white px-5 py-2 font-medium text-neutral-800 transition-colors hover:bg-neutral-100'>
        Prijava
      </Button>
    </header>
  );
};
