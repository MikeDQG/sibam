import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { auth } from "../../firebase";
import { onAuthStateChanged } from "firebase/auth";

export const AccountPage = () => {
    const [email, setEmail] = useState<string | null>(null);
    const navigate = useNavigate();

    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, (user) => {
            if (user) {
                setEmail(user.email);
            } else {
                navigate("/login");
            }
        });
        return () => unsubscribe();
    }, []);

    return (
        <div className="min-h-screen bg-[#212121] flex flex-col items-center pt-20 px-6">
            <img
                src="/logo.svg"
                className="absolute left-9 top-6 h-15 w-auto cursor-pointer"
                alt="Logo"
                onClick={() => navigate("/")}
            />
            <div className="w-full max-w-xl bg-neutral-700 rounded-lg p-8 flex flex-col gap-6">
                <h1 className="text-3xl font-semibold text-white">
                    Pozdravljeni!
                </h1>
                <p className="text-neutral-300 text-sm">
                    Prijavljeni ste kot:{" "}
                    <span className="text-white font-medium">{email}</span>
                </p>

                <div className="border-t border-neutral-600 pt-4">
                    <h2 className="text-xl font-semibold text-white mb-4">
                        Shranjene lokacije
                    </h2>
                    <div className="flex flex-col gap-3">
                        <div className="bg-neutral-600 rounded-md p-4 text-neutral-400 text-sm text-center">
                            Še nimate shranjenih lokacij.
                        </div>
                    </div>
                </div>

                <button
                    onClick={() => {
                        auth.signOut();
                        navigate("/login");
                    }}
                    className="mt-4 text-sm text-red-400 hover:text-red-300 transition-colors cursor-pointer text-left">
                    Odjava
                </button>
            </div>
        </div>
    );
};
