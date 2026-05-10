import { APIProvider, Map } from "@vis.gl/react-google-maps";

const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;

type MapCenter = {
  lat: number;
  lng: number;
};

type MainMapProps = {
  center: MapCenter;
  zoom: number;
  onCameraChanged?: (center: MapCenter, zoom: number) => void;
};

export const MainMap = ({ center, zoom, onCameraChanged }: MainMapProps) => {
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
          center={center}
          zoom={zoom}
          onCameraChanged={(event) => {
            onCameraChanged?.(event.detail.center, event.detail.zoom);
          }}
          colorScheme='DARK'
          gestureHandling='greedy'
          disableDefaultUI
          reuseMaps
        />
      </APIProvider>
    </div>
  );
};
