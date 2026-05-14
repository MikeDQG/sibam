import { useEffect } from "react";
import { useMap } from "@vis.gl/react-google-maps";

declare const google: {
  maps: {
    Polyline: new (options: {
      map: unknown;
      path: { lat: number; lng: number }[];
      strokeColor: string;
      strokeOpacity?: number;
      strokeWeight: number;
      geodesic: boolean;
      icons?: {
        icon: {
          path: string;
          fillOpacity?: number;
          fillColor?: string;
          strokeOpacity?: number;
          strokeColor?: string;
          strokeWeight?: number;
          scale?: number;
        };
        offset: string;
        repeat: string;
      }[];
    }) => {
      setMap: (map: unknown | null) => void;
    };
    LatLngBounds: new () => {
      extend: (point: { lat: number; lng: number }) => void;
      isEmpty: () => boolean;
    };
    SymbolPath: {
      CIRCLE: string;
    };
  };
};

export type RouteLeg = {
  tip: string;
  polyline: {
    lat: number;
    lon: number;
  }[];
};

type RoutePolylineProps = {
  legs: RouteLeg[];
};

const legColors: Record<string, string> = {
  WALK: "#FBFFB9",
  BIKE: "#FF9B42",
  BUS: "#721121",
};

export const RoutePolyline = ({ legs }: RoutePolylineProps) => {
  const map = useMap();

  // funkcija da prikazem različne stile linij glede na tip poti
  const getLineOptions = (tip: string, color: string) => {
    if (tip === "WALK") {
      return {
        strokeOpacity: 0,
        icons: [
          {
            icon: {
              path: google.maps.SymbolPath.CIRCLE,
              fillOpacity: 1,
              fillColor: color,
              strokeOpacity: 1,
              strokeColor: color,
              scale: 2,
            },
            offset: "0",
            repeat: "14px",
          },
        ],
      };
    }

    if (tip === "BIKE") {
      return {
        strokeOpacity: 0,
        icons: [
          {
            icon: {
              path: "M 0,-1 0,2",
              strokeOpacity: 1,
              strokeColor: color,
              strokeWeight: 5,
              scale: 3,
            },
            offset: "0",
            repeat: "23px",
          },
        ],
      };
    }

    return {
      strokeOpacity: 0.95,
    };
  };

  useEffect(() => {
    if (!map || legs.length === 0) return;

    const polylines = legs.map((leg) => {
      const color = legColors[leg.tip] ?? "#ffffff";

      return new google.maps.Polyline({
        map,
        path: leg.polyline.map((point) => ({
          lat: point.lat,
          lng: point.lon,
        })),
        strokeColor: color,
        strokeWeight: 5,
        geodesic: true,
        ...getLineOptions(leg.tip, color),
      });
    });

    const bounds = new google.maps.LatLngBounds();

    legs.forEach((leg) => {
      leg.polyline.forEach((point) => {
        bounds.extend({
          lat: point.lat,
          lng: point.lon,
        });
      });
    });

    if (!bounds.isEmpty()) {
      map.fitBounds(bounds, 64);
    }

    return () => {
      polylines.forEach((polyline) => polyline.setMap(null));
    };
  }, [map, legs]);

  return null;
};
