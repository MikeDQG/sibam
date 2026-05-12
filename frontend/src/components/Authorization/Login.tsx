import { useNavigate } from "react-router";
import { Separator } from "../ui/separator";
import { Input } from "../ui/input";
import { Button } from "../ui/button";
import { ArrowLeft, Eye, EyeOff } from "lucide-react";
import { useState } from "react";
import { FaGoogle } from "react-icons/fa";
import { auth } from "../../firebase";
import {
    signInWithEmailAndPassword,
    signInWithPopup,
    GoogleAuthProvider,
} from "firebase/auth";

export const Login = () => {
    const [showPassword, setShowPassword] = useState(false);
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const navigate = useNavigate();

    const handleLogin = async () => {
        try {
            const userCredential = await signInWithEmailAndPassword(
                auth,
                email,
                password,
            );
            const token = await userCredential.user.getIdToken();
            navigate("/account");
        } catch (error: any) {
            console.error("Napaka pri prijavi:", error.message);
        }
    };

    const handleGoogleLogin = async () => {
        try {
            const provider = new GoogleAuthProvider();
            const userCredential = await signInWithPopup(auth, provider);
            const token = await userCredential.user.getIdToken();
            navigate("/account");
        } catch (error: any) {
            console.error("Napaka pri Google prijavi:", error.message);
        }
    };

    return (
        <div className="relative flex min-h-screen w-full items-center justify-end bg-[#212121]">
            <img
                src="logo.svg"
                className="absolute left-9 top-6 h-15 w-auto z-2 cursor-pointer"
                alt="Logo"
                onClick={() => navigate("/")}
            />
            <img
                className="absolute left-0 top-0 h-full w-[80%] max-w-[80%] object-cover"
                src="/LandingPage/background.jpeg"
                alt="Background"
            />
            <div className="z-10 flex min-h-screen w-lg max-w-137.5 py-5 pr-5 lg:w-full lg:max-w-[50%]">
                <div className="relative flex w-full flex-col items-center rounded-lg bg-neutral-700 pt-20 gap-8">
                    <button
                        type="button"
                        onClick={() => navigate("/")}
                        className="absolute left-6 top-6 flex items-center gap-2 text-sm font-medium text-white transition-colors hover:text-neutral-300 cursor-pointer">
                        <ArrowLeft size={18} />
                        Nazaj domov
                    </button>
                    <h1 className="text-4xl font-semibold text-white">
                        Prijava
                    </h1>
                    <form className="flex flex-col items-center gap-5 w-full max-w-75">
                        <Input
                            type="email"
                            placeholder="Email"
                            className="w-full"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                        <div className="relative w-full">
                            <Input
                                type={showPassword ? "text" : "password"}
                                placeholder="Geslo"
                                className="w-full pr-10"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                            />
                            <button
                                type="button"
                                onClick={() =>
                                    setShowPassword((current) => !current)
                                }
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 transition-colors hover:text-white"
                                aria-label={
                                    showPassword
                                        ? "Skrij geslo"
                                        : "Prikaži geslo"
                                }>
                                {showPassword ? (
                                    <Eye size={18} />
                                ) : (
                                    <EyeOff size={18} />
                                )}
                            </button>
                        </div>
                    </form>
                    <Button
                        onClick={handleLogin}
                        className="bg-red-700 hover:bg-red-800 text-white font-semibold py-2 px-4 rounded-md transition-colors">
                        Prijavi se
                    </Button>

                    <div className="flex items-center gap-3 text-medium text-white w-[95%] px-10">
                        <Separator className="flex-1 text-white" />
                        <span className="shrink-0 font-normal text-sm">
                            ali se prijavi z
                        </span>
                        <Separator className="flex-1 text-white" />
                    </div>
                    <Button
                        onClick={handleGoogleLogin}
                        className="bg-gray-500 hover:bg-gray-400 text-white font-semibold py-2 px-4 rounded-md transition-colors">
                        <FaGoogle /> Google
                    </Button>
                    <p className="w-full max-w-75 text-center text-sm text-white">
                        Še nimaš profila?{" "}
                        <button
                            type="button"
                            onClick={() => navigate("/register")}
                            className="font-medium transition-colors hover:underline cursor-pointer">
                            Registriraj se
                        </button>
                    </p>
                </div>
            </div>
        </div>
    );
};
