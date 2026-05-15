import { Bike, Bus } from "lucide-react";

type Props = {
    onDismiss: () => void;
};

export const RouteLoadingOverlay = ({ onDismiss }: Props) => {
    return (
        <div onClick={onDismiss} className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-black/60 backdrop-blur-sm overflow-hidden cursor-pointer">
            <div className="relative w-full h-20 mb-8">
                <div className="absolute top-0 left-0 animate-ride-bike">
                    <Bike size={56} className="text-white drop-shadow-lg" />
                </div>
                <div className="absolute top-0 left-0 animate-ride-bus">
                    <Bus size={48} className="text-white/80 drop-shadow-lg" />
                </div>
            </div>
            <p className="text-sm font-medium tracking-widest text-white/80 uppercase">
                Iščem pot...
            </p>
        </div>
    );
};
