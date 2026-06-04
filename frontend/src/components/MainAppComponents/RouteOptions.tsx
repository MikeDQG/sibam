import { useEffect, useRef, useState } from "react";
import { Bike, Bus, Footprints, type LucideIcon } from "lucide-react";
import { RouteErrorBox } from "./RouteErrorBox";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import type { RouteLeg, RoutePath } from "./RoutePolyline";
import { getInstructionText } from "../../lib/text";

type RouteOptionProps = {
  routes: RoutePath[];
  legs?: RouteLeg[];
  computeError?: {
    code: string;
    message?: string;
  } | null;
  canSaveRoute?: boolean;
  hasFetchedRoute?: boolean;
  isSavedRoute?: boolean;
  activeStepIndex?: number | null;
  activeRouteIndex?: number;
  onRouteSelect?: (route: RoutePath, index: number) => void;
  onSaveRoute?: (name: string, route: RoutePath) => Promise<void>;
};

function getViewportBottomInset() {
  const visualViewport = window.visualViewport;

  if (!visualViewport) return 0;

  return Math.max(
    window.innerHeight - visualViewport.height - visualViewport.offsetTop,
    0,
  );
}

export const RouteOptions = ({
  routes,
  legs,
  computeError,
  canSaveRoute = true,
  hasFetchedRoute = true,
  isSavedRoute = false,
  activeStepIndex = null,
  activeRouteIndex = 0,
  onRouteSelect,
  onSaveRoute,
}: RouteOptionProps) => {
  const sectionRef = useRef<HTMLElement>(null);
  const dragStartY = useRef(0);
  const dragStartOffset = useRef(0);
  const didDrag = useRef(false);
  const [offsetY, setOffsetY] = useState(44);
  const [maxOffsetY, setMaxOffsetY] = useState(0);
  const [viewportBottomInset, setViewportBottomInset] = useState(0);
  const [editingRouteIndex, setEditingRouteIndex] = useState<number | null>(
    null,
  );
  const [routeName, setRouteName] = useState("");
  const [savingRouteIndex, setSavingRouteIndex] = useState<number | null>(null);

  // initial nastavitev maxOffseta
  useEffect(() => {
    const updateSheetHeight = () => {
      if (!sectionRef.current) return;

      const handleHeight = 44; // h-11: v zaprtem stanju ostane vidna samo ročica
      const sheetHeight =
        sectionRef.current.offsetHeight || window.innerHeight * 0.8;
      const nextMaxOffset = Math.max(
        sheetHeight - handleHeight,
        0,
      );

      setViewportBottomInset(getViewportBottomInset());
      setMaxOffsetY(nextMaxOffset);
      setOffsetY(computeError ? 0 : nextMaxOffset); // ob prvi renderju naj bo sheet zaprt (offset enak maxOffsetu)
    };

    updateSheetHeight();
    window.addEventListener("resize", updateSheetHeight);
    window.visualViewport?.addEventListener("resize", updateSheetHeight);
    window.visualViewport?.addEventListener("scroll", updateSheetHeight);

    return () => {
      window.removeEventListener("resize", updateSheetHeight);
      window.visualViewport?.removeEventListener("resize", updateSheetHeight);
      window.visualViewport?.removeEventListener("scroll", updateSheetHeight);
    };
  }, [computeError]);

  useEffect(() => {
    if (computeError) {
      setOffsetY(0);
    }
  }, [computeError]);

  const handlePointerDown = (event: React.PointerEvent<HTMLButtonElement>) => {
    dragStartY.current = event.clientY;
    dragStartOffset.current = offsetY;
    didDrag.current = false;
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handlePointerUp = (event: React.PointerEvent<HTMLButtonElement>) => {
    if (!event.currentTarget.hasPointerCapture(event.pointerId)) return;

    event.currentTarget.releasePointerCapture(event.pointerId);
  };
  const handlePointerMove = (event: React.PointerEvent<HTMLButtonElement>) => {
    if (!event.currentTarget.hasPointerCapture(event.pointerId)) return;

    const nextOffset =
      dragStartOffset.current + event.clientY - dragStartY.current;

    if (Math.abs(event.clientY - dragStartY.current) > 4) {
      didDrag.current = true;
    }

    setOffsetY(Math.min(Math.max(nextOffset, 0), maxOffsetY));
  };

  const toggleSheet = () => {
    if (didDrag.current) {
      didDrag.current = false;
      return;
    }

    setOffsetY((currentOffset) =>
      currentOffset > maxOffsetY / 2 ? 0 : maxOffsetY,
    );
  };

  const startRouteSave = (routeIndex: number) => {
    setEditingRouteIndex(routeIndex);
    setRouteName("");
  };

  const submitRouteSave = async (routeIndex: number, route: RoutePath) => {
    const trimmedName = routeName.trim();

    if (!trimmedName || savingRouteIndex !== null) return;

    setSavingRouteIndex(routeIndex);

    try {
      await onSaveRoute?.(trimmedName, route);
      setEditingRouteIndex(null);
      setRouteName("");
    } finally {
      setSavingRouteIndex(null);
    }
  };

  const activeRoute = routes[activeRouteIndex] ?? routes[0];
  const activeRouteLegs = activeRoute?.legs ?? legs;
  const routeSteps =
    activeRouteLegs?.flatMap((leg, legIndex) =>
      (leg.steps ?? [])
        .map((step) => ({
          instruction: getInstructionText(step.instruction),
          legIndex,
          mode: leg.mode,
        }))
        .filter((step) => step.instruction.length > 0),
    ) ?? [];

  return (
    <section
      ref={sectionRef}
      className='fixed bottom-0 left-0 right-0 z-30 h-[80vh] overflow-hidden rounded-t-[18px] bg-card text-card-foreground shadow-2xl transition-transform duration-200 ease-out dark:bg-[#292927]'
      style={{
        bottom: viewportBottomInset,
        transform: `translateY(${offsetY}px)`,
      }}>
      <button
        type='button'
        className='absolute left-0 right-0 top-0 z-20 flex h-11 cursor-grab touch-none items-start justify-center bg-card pt-4 active:cursor-grabbing dark:bg-[#292927]'
        aria-label={offsetY > maxOffsetY / 2 ? "Razširi poti" : "Skrij poti"}
        onClick={toggleSheet}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}>
        <span className='h-1.5 w-17 rounded-full bg-muted-foreground/50 dark:bg-[#aaa69d]' />
      </button>

      <div className='h-full overflow-y-auto px-[6.3%] pb-13 pt-13'>
        {computeError && (
          <RouteErrorBox
            code={computeError.code}
            message={computeError.message}
          />
        )}

        {!computeError && !hasFetchedRoute ? (
          <div className='flex h-40 items-center justify-center rounded-[18px] border border-border bg-muted px-8 text-center text-lg font-medium text-muted-foreground shadow-md dark:border-neutral-600 dark:bg-neutral-800 dark:text-neutral-300'>
            Za izbiranje načina poti najprej vnesite željeno pot.
          </div>
        ) : (
          <div className='flex flex-col gap-8'>
            <div className='flex flex-col gap-7 lg:flex-row lg:gap-9'>
              {routes.map((route, index) => {
                const isSelected = index === activeRouteIndex;
                const routeTitle = getRouteTitle(route, index);
                const routeTime = getRouteTime(route);
                const routeIcons = getRouteIcons(route);

                return (
                  <div
                    key={`${route.rank ?? index}-${routeTitle}`}
                    className='flex w-full flex-col gap-3 lg:min-w-0 lg:flex-1'>
                    <button
                      type='button'
                      onClick={() => onRouteSelect?.(route, index)}
                      className={`min-h-40 rounded-[18px] border px-8 py-7 text-left shadow-lg transition-colors lg:px-12 ${
                        isSelected
                          ? "border-red-800 bg-red-700 text-white shadow-red-900/20 dark:border-red-300 dark:bg-[#941d38]"
                          : "border-neutral-200 bg-neutral-100 text-neutral-950 hover:bg-neutral-200 dark:border-neutral-600 dark:bg-neutral-800 dark:text-white dark:hover:bg-neutral-700"
                      }`}>
                      <span
                        className={`block wrap-break-words text-[25px] font-normal leading-tight ${
                          isSelected
                            ? "text-white/80"
                            : "text-neutral-600 dark:text-neutral-300"
                        }`}>
                        {routeTitle}
                      </span>
                      <strong className='mt-2 block text-[32px] font-bold leading-none'>
                        {routeTime}
                      </strong>
                      <div
                        className={`mt-4 flex items-center gap-3 ${
                          isSelected
                            ? "text-white/80"
                            : "text-neutral-500 dark:text-neutral-300"
                        }`}>
                        {routeIcons.map((Icon, iconIndex) => (
                          <Icon key={iconIndex} />
                        ))}
                      </div>
                    </button>

                    {!isSavedRoute && (
                      <div className='w-full'>
                        {editingRouteIndex === index ? (
                          <div className='flex flex-col gap-3'>
                            <Input
                              value={routeName}
                              onChange={(event) =>
                                setRouteName(event.target.value)
                              }
                              placeholder='Ime poti'
                              aria-label='Ime poti'
                              className='h-11 rounded-[18px] bg-card text-card-foreground shadow-md dark:bg-[#292927]'
                            />
                            <Button
                              type='button'
                              onClick={() => void submitRouteSave(index, route)}
                              disabled={
                                !routeName.trim() || savingRouteIndex !== null
                              }
                              className={`h-11 w-full rounded-[18px] border shadow-md hover:brightness-95 ${
                                isSelected
                                  ? "border-red-800 bg-red-700 text-white hover:bg-red-800"
                                  : "border-neutral-200 bg-neutral-100 text-neutral-950 hover:bg-neutral-200 dark:border-neutral-600 dark:bg-neutral-800 dark:text-white dark:hover:bg-neutral-700"
                              }`}>
                              {savingRouteIndex === index
                                ? "Shranjevanje ..."
                                : "Shrani"}
                            </Button>
                          </div>
                        ) : (
                          <Button
                            type='button'
                            variant='secondary'
                            onClick={() => startRouteSave(index)}
                            disabled={!canSaveRoute}
                            className={`h-11 w-full rounded-[18px] border shadow-md hover:brightness-95 ${
                              isSelected
                                ? "border-red-800 bg-red-700 text-white hover:bg-red-800"
                                : "border-neutral-200 bg-neutral-100 text-neutral-950 hover:bg-neutral-200 dark:border-neutral-600 dark:bg-neutral-800 dark:text-white dark:hover:bg-neutral-700"
                            }`}>
                            Shrani pot
                          </Button>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            {hasFetchedRoute && (
              <section className='rounded-[18px] border border-border bg-background/70 px-6 py-5 shadow-md dark:border-neutral-600 dark:bg-neutral-800/70'>
                <h2 className='text-xl font-semibold'>Navodila za pot</h2>
                {routeSteps.length > 0 ? (
                  <ol className='mt-4 space-y-3'>
                    {routeSteps.map((step, index) => {
                      const hasActiveStep =
                        typeof activeStepIndex === "number" &&
                        activeStepIndex >= 0;
                      const isPastStep =
                        hasActiveStep && index < activeStepIndex;
                      const isCurrentStep =
                        hasActiveStep && index === activeStepIndex;

                      return (
                        <li
                          key={`${step.legIndex}-${index}-${step.instruction}`}
                          className={`grid grid-cols-[2rem_1fr] gap-3 text-sm leading-snug transition-transform duration-200 ${
                            isCurrentStep ? "scale-[1.02]" : ""
                          }`}>
                          <span
                            className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-semibold text-white transition-all duration-200 ${
                              isCurrentStep
                                ? "scale-110 bg-red-700 shadow-lg shadow-red-700/30 ring-2 ring-red-200 dark:ring-red-500/40"
                                : isPastStep
                                  ? "bg-neutral-400 dark:bg-neutral-600"
                                  : "bg-red-700"
                            }`}>
                            {index + 1}
                          </span>
                          <div
                            className={`min-w-0 rounded-lg bg-card px-3 py-2 shadow-sm transition-all duration-200 dark:bg-[#292927] ${
                              isCurrentStep
                                ? "shadow-lg ring-2 ring-red-200 dark:ring-red-500/40"
                                : ""
                            }`}>
                            <span className='mb-1 inline-block rounded-full bg-muted px-2 py-0.5 text-[11px] font-medium uppercase tracking-wide text-muted-foreground dark:bg-neutral-700 dark:text-neutral-300'>
                              {getModeLabel(step.mode)}
                            </span>
                            <p className='wrap-break-words text-card-foreground'>
                              {step.instruction}
                            </p>
                          </div>
                        </li>
                      );
                    })}
                  </ol>
                ) : (
                  <p className='mt-4 rounded-lg bg-card px-3 py-2 text-sm text-muted-foreground shadow-sm dark:bg-[#292927] dark:text-neutral-300'>
                    Navodila za to pot niso na voljo.
                  </p>
                )}
              </section>
            )}
          </div>
        )}
      </div>
    </section>
  );
};

function getModeLabel(mode: string) {
  switch (mode) {
    case "WALK":
      return "Peš";
    case "BIKE":
      return "Kolo";
    case "BUS":
      return "Bus";
    default:
      return mode;
  }
}

function getRouteTitle(route: RoutePath, index: number) {
  if (typeof route.label === "string" && route.label.trim()) {
    return route.label.trim();
  }

  if (typeof route.rank === "number" && Number.isFinite(route.rank)) {
    return `Pot ${route.rank}`;
  }

  return `Pot ${index + 1}`;
}

function getRouteTime(route: RoutePath) {
  if (
    typeof route.totalDurationSeconds === "number" &&
    Number.isFinite(route.totalDurationSeconds)
  ) {
    return `${Math.round(route.totalDurationSeconds / 60)} min`;
  }

  const duration = Number(route.duration);
  if (Number.isFinite(duration)) {
    return `${Math.round(duration / 60000)} min`;
  }

  return "Ni podatka";
}

function getRouteIcons(route: RoutePath) {
  const modeIcons = {
    WALK: Footprints,
    BIKE: Bike,
    BUS: Bus,
  } satisfies Record<string, LucideIcon>;
  const modes =
    route.modes && route.modes.length > 0
      ? route.modes
      : route.legs.map((leg) => leg.mode);
  const uniqueModes = Array.from(new Set(modes));

  return uniqueModes
    .map((mode) => modeIcons[mode as keyof typeof modeIcons])
    .filter((Icon): Icon is LucideIcon => Boolean(Icon));
}
