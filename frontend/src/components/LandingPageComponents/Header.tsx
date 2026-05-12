import { Button } from "@/components/ui/button";
import { useEffect, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { auth } from "../../firebase";
import { onAuthStateChanged } from "firebase/auth";

export const Header = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const [isScrolled, setIsScrolled] = useState(false);
    const [isLoggedIn, setIsLoggedIn] = useState(false);

    useEffect(() => {
        const handleScroll = () => {
            setIsScrolled(window.scrollY > 0);
        };

        handleScroll();
        window.addEventListener("scroll", handleScroll);

        return () => window.removeEventListener("scroll", handleScroll);
    }, []);

    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, (user) => {
            setIsLoggedIn(!!user);
        });
        return () => unsubscribe();
    }, []);

    const handleAuthButton = () => {
        if (isLoggedIn) {
            auth.signOut();
            navigate("/login");
        } else {
            navigate("/login");
        }
    };

    return (
        <header
            className={`fixed left-3 right-3 top-3 z-50 flex items-center justify-between rounded-xl px-6 py-3 transition-colors duration-300 ${
                isScrolled
                    ? "bg-neutral-600/40 shadow-lg backdrop-blur-md"
                    : "bg-neutral-600/00"
            }`}>
            <img
                src="/logo.svg"
                alt="Šibam"
                className="h-15 w-auto"
                onClick={() => navigate("/")}
            />
            <div className="flex gap-3">
                {location.pathname !== "/" && (
                    <Button
                        onClick={() => navigate("/home")}
                        className="rounded-lg h-10 px-5 py-2 text-lg font-semibold transition-colors bg-neutral-600 hover:bg-neutral-500 text-white">
                        Najdi pot
                    </Button>
                )}
                {isLoggedIn && location.pathname !== "/account" && (
                    <Button
                        onClick={() => navigate("/account")}
                        className="rounded-lg h-10 px-5 py-2 text-lg font-semibold transition-colors bg-neutral-600 hover:bg-neutral-500 text-white">
                        Moj račun
                    </Button>
                )}
                <Button
                    onClick={handleAuthButton}
                    className="rounded-lg h-10 px-5 py-2 text-lg font-semibold transition-colors bg-red-700 hover:bg-red-800 text-white">
                    {isLoggedIn ? "Odjava" : "Prijava"}
                </Button>
            </div>
        </header>
    );
};
