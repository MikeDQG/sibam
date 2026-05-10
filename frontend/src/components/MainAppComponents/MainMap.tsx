import { APIProvider, Map } from "@vis.gl/react-google-maps";

const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;

const mariborCenter = {
  lat: 46.5547,
  lng: 15.6459,
};

export const MainMap = () => {
  const hasApiKey = apiKey && apiKey !== "your_google_maps_api_key";

  if (!hasApiKey) {
    return (
      <div className='absolute inset-0 z-0 flex items-center justify-center bg-neutral-800 text-neutral-300'>
        Manjka Google Maps API key
      </div>
    );
  }

  return (
    <div className='absolute inset-0 z-0'>
      <APIProvider apiKey={apiKey} region='SI' language='sl'>
        <Map
          defaultCenter={mariborCenter}
          defaultZoom={14}
          gestureHandling='greedy'
          disableDefaultUI
          reuseMaps
        />
      </APIProvider>
    </div>
  );
};
