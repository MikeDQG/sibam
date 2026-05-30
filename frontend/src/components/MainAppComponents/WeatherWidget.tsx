import { useEffect, useState } from "react";
import {
    Cloud,
    CloudDrizzle,
    CloudLightning,
    CloudRain,
    CloudSnow,
    Sun,
    Wind,
    CloudFog,
} from "lucide-react";

type WeatherData = {
    temp: number;
    condition: string;
};

const apiKey = import.meta.env.VITE_OPENWEATHER_API_KEY as string;

function getWeatherIcon(condition: string) {
    switch (condition.toLowerCase()) {
        case "thunderstorm":
            return <CloudLightning size={16} />;
        case "drizzle":
            return <CloudDrizzle size={16} />;
        case "rain":
            return <CloudRain size={16} />;
        case "snow":
            return <CloudSnow size={16} />;
        case "fog":
        case "mist":
        case "haze":
            return <CloudFog size={16} />;
        case "clear":
            return <Sun size={16} />;
        case "clouds":
            return <Cloud size={16} />;
        default:
            return <Wind size={16} />;
    }
}

export const WeatherWidget = () => {
    const [weather, setWeather] = useState<WeatherData | null>(null);

    useEffect(() => {
        fetch(
            `https://api.openweathermap.org/data/2.5/weather?lat=46.5547&lon=15.6459&units=metric&appid=${apiKey}`,
        )
            .then((r) => r.json())
            .then((data) => {
                setWeather({
                    temp: Math.round(data.main.temp),
                    condition: data.weather[0].main,
                });
            })
            .catch(() => {});
    }, []);

    if (!weather) return null;

    return (
        <div className="pointer-events-auto z-10 flex h-10 shrink-0 items-center gap-2 rounded-lg bg-red-700 px-3 text-white shadow-md">
            {getWeatherIcon(weather.condition)}
            <span className="text-sm">{weather.temp} °C</span>
        </div>
    );
};
