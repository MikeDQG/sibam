import { LogOut } from "lucide-react";
import { ThemeToggle } from "../../ThemeToggle";
import { Button } from "../../ui/button";
import { MapZoomLocateButtons } from "./MapZoomLocateButtons";
import { ProfileButton } from "./ProfileButton";

type MapControlsProps = {
  isLoggedIn: boolean;
  showDirections: boolean;
  isRouteActive: boolean;
  onProfileClick: () => void;
  onLogoutClick: () => void;
  onZoomIn?: () => void;
  onZoomOut?: () => void;
  onLocate?: () => void;
};

export function MapControls({
  isLoggedIn,
  showDirections,
  isRouteActive,
  onProfileClick,
  onLogoutClick,
  onZoomIn,
  onZoomOut,
  onLocate,
}: MapControlsProps) {
  const loggedInClassName = `pointer-events-auto absolute right-0 flex shrink-0 flex-row gap-2 min-[700px]:top-3 max-[450px]:!fixed max-[450px]:!bottom-12 max-[450px]:!left-4 max-[450px]:!right-auto max-[450px]:!top-auto max-[450px]:flex-col ${showDirections ? "max-[700px]:top-24" : "max-[700px]:top-14"}`;
  const loggedOutClassName = `pointer-events-auto absolute right-0 flex shrink-0 flex-col gap-2 min-[700px]:top-2 max-[450px]:!fixed max-[450px]:!bottom-12 max-[450px]:!left-4 max-[450px]:!right-auto max-[450px]:!top-auto ${showDirections ? "max-[699px]:top-24" : "max-[699px]:top-14"}`;

  if (!isLoggedIn) {
    return (
      <div className={loggedOutClassName}>
        <ProfileButton onClick={onProfileClick} />
        <ThemeToggle />
        <MapZoomLocateButtons
          onZoomIn={onZoomIn}
          onZoomOut={onZoomOut}
          onLocate={onLocate}
          isRouteActive={isRouteActive}
        />
      </div>
    );
  }

  return (
    <div className={loggedInClassName}>
      <ProfileButton onClick={onProfileClick} />
      <div className='flex flex-col gap-2'>
        <Button
          type='button'
          onClick={onLogoutClick}
          aria-label='Odjava'
          className='flex h-10 w-10 items-center justify-center rounded-md bg-red-700 text-white shadow-lg hover:text-red-200'>
          <LogOut />
        </Button>
        <ThemeToggle />
        <MapZoomLocateButtons
          onZoomIn={onZoomIn}
          onZoomOut={onZoomOut}
          onLocate={onLocate}
          isRouteActive={isRouteActive}
        />
      </div>
    </div>
  );
}
