import { Bike, Bus } from "lucide-react";

type Props = {
    onDismiss: () => void;
};

export const RouteLoadingOverlay = ({ onDismiss }: Props) => {
    return (
        <button
            type="button"
            onClick={onDismiss}
            aria-label="Prekliči iskanje poti"
            className="fixed inset-0 z-50 flex cursor-pointer flex-col items-center justify-center overflow-hidden border-0 bg-black/60 p-0 backdrop-blur-sm">
            <span className="relative mb-8 block h-20 w-full">
                <span className="absolute left-0 top-0 animate-ride-bike">
                    <Bike size={56} className="text-white drop-shadow-lg" />
                </span>
                <span className="absolute left-0 top-0 animate-ride-bus">
                    <Bus size={48} className="text-white/80 drop-shadow-lg" />
                </span>
            </span>
            <span className="text-sm font-medium uppercase tracking-widest text-white/80">
                Iščem pot...
            </span>
        </button>
    );
};
