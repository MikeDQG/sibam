import { useEffect, useRef, useState } from "react";
import { RouteErrorBox } from "./RouteErrorBox";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import type { RouteLeg } from "./RoutePolyline";

type route = {
  title: string;
  time: string;
  className: string;
  icons: React.ComponentType[];
};

type RouteOptionProps = {
  routes: route[];
  legs?: RouteLeg[];
  computeError?: {
    code: string;
    message?: string;
  } | null;
  canSaveRoute?: boolean;
  hasFetchedRoute?: boolean;
  onSaveRoute?: (name: string) => Promise<void>;
};

export const RouteOptions = ({
  routes,
  legs,
  computeError,
  canSaveRoute = true,
  hasFetchedRoute = true,
  onSaveRoute,
}: RouteOptionProps) => {
  const sectionRef = useRef<HTMLElement>(null);
  const dragStartY = useRef(0);
  const dragStartOffset = useRef(0);
  const didDrag = useRef(false);
  const [offsetY, setOffsetY] = useState(44);
  const [maxOffsetY, setMaxOffsetY] = useState(0);
  const [editingRouteTitle, setEditingRouteTitle] = useState<string | null>(
    null,
  );
  const [routeName, setRouteName] = useState("");
  const [savingRouteTitle, setSavingRouteTitle] = useState<string | null>(null);

  // initial nastavitev maxOffseta
  useEffect(() => {
    const updateSheetHeight = () => {
      if (!sectionRef.current) return;

      const handleHeight = 40; // toliko je visok handle za section
      const nextMaxOffset = Math.max(
        window.innerHeight * 0.8 - handleHeight, // 0.8 ker je section visok 80vh
        0,
      );

      setMaxOffsetY(nextMaxOffset);
      setOffsetY(computeError ? 0 : nextMaxOffset); // ob prvi renderju naj bo sheet zaprt (offset enak maxOffsetu)
    };

    updateSheetHeight();
    window.addEventListener("resize", updateSheetHeight);

    return () => window.removeEventListener("resize", updateSheetHeight);
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

  const startRouteSave = (routeTitle: string) => {
    setEditingRouteTitle(routeTitle);
    setRouteName("");
  };

  const submitRouteSave = async (routeTitle: string) => {
    const trimmedName = routeName.trim();

    if (!trimmedName || savingRouteTitle) return;

    setSavingRouteTitle(routeTitle);

    try {
      await onSaveRoute?.(trimmedName);
      setEditingRouteTitle(null);
      setRouteName("");
    } finally {
      setSavingRouteTitle(null);
    }
  };

  const getButtonClassName = (className: string) =>
    className.replace(/\bring-\S+/g, "").trim();

  const routeSteps =
    legs?.flatMap((leg, legIndex) =>
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
      style={{ transform: `translateY(${offsetY}px)` }}>
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
              {routes.map((option) => {
                const isSelected = option.className.includes("ring-4");
                const buttonClassName = getButtonClassName(option.className);

                return (
                  <div
                    key={option.title}
                    className='flex w-full flex-col gap-3 lg:min-w-0 lg:flex-1'>
                    <article
                      className={`min-h-40 rounded-[18px] border px-8 py-7 text-left shadow-lg lg:px-12 ${option.className} ${
                        isSelected
                          ? "border-white/80 shadow-white/20 dark:border-white/70 dark:shadow-white/10"
                          : ""
                      }`}>
                      <span className='block wrap-break-words text-[25px] font-normal leading-tight text-current/70 dark:text-[#b2ada4]'>
                        {option.title}
                      </span>
                      <strong className='mt-2 block text-[32px] font-bold leading-none'>
                        {option.time}
                      </strong>
                      <div className='mt-4 flex items-center gap-3 text-current/60 dark:text-[#aaa69d]'>
                        {option.icons.map((Icon, iconIndex) => (
                          <Icon key={iconIndex} />
                        ))}
                      </div>
                    </article>

                    <div className='w-full'>
                      {editingRouteTitle === option.title ? (
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
                            onClick={() => void submitRouteSave(option.title)}
                            disabled={
                              !routeName.trim() || savingRouteTitle !== null
                            }
                            className={`h-11 w-full rounded-[18px] border shadow-md hover:brightness-95 ${buttonClassName}`}>
                            {savingRouteTitle === option.title
                              ? "Shranjevanje ..."
                              : "Shrani"}
                          </Button>
                        </div>
                      ) : (
                        <Button
                          type='button'
                          variant='secondary'
                          onClick={() => startRouteSave(option.title)}
                          disabled={!canSaveRoute}
                          className={`h-11 w-full rounded-[18px] border shadow-md hover:brightness-95 ${buttonClassName}`}>
                          Shrani pot
                        </Button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            {routeSteps.length > 0 && (
              <section className='rounded-[18px] border border-border bg-background/70 px-6 py-5 shadow-md dark:border-neutral-600 dark:bg-neutral-800/70'>
                <h2 className='text-xl font-semibold'>Navodila za pot</h2>
                <ol className='mt-4 space-y-3'>
                  {routeSteps.map((step, index) => (
                    <li
                      key={`${step.legIndex}-${index}-${step.instruction}`}
                      className='grid grid-cols-[2rem_1fr] gap-3 text-sm leading-snug'>
                      <span className='flex h-8 w-8 items-center justify-center rounded-full bg-red-700 text-xs font-semibold text-white'>
                        {index + 1}
                      </span>
                      <div className='min-w-0 rounded-lg bg-card px-3 py-2 shadow-sm dark:bg-[#292927]'>
                        <span className='mb-1 inline-block rounded-full bg-muted px-2 py-0.5 text-[11px] font-medium uppercase tracking-wide text-muted-foreground dark:bg-neutral-700 dark:text-neutral-300'>
                          {getModeLabel(step.mode)}
                        </span>
                        <p className='wrap-break-words text-card-foreground'>
                          {step.instruction}
                        </p>
                      </div>
                    </li>
                  ))}
                </ol>
              </section>
            )}
          </div>
        )}
      </div>
    </section>
  );
};

function getInstructionText(instruction?: string | null) {
  if (!instruction) return "";

  if (typeof DOMParser === "undefined") {
    return instruction
      .replace(/<[^>]*>/g, " ")
      .replace(/\s+/g, " ")
      .trim();
  }

  const document = new DOMParser().parseFromString(instruction, "text/html");
  return (document.body.textContent ?? "").replace(/\s+/g, " ").trim();
}

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
