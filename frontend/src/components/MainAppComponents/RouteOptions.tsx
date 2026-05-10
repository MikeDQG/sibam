import { useEffect, useRef, useState } from "react";

type route = {
  title: string;
  time: string;
  className: string;
  icons: React.ComponentType[];
};

type RouteOptionProps = {
  routes: route[];
};

export const RouteOptions = ({ routes }: RouteOptionProps) => {
  const sectionRef = useRef<HTMLElement>(null);
  const dragStartY = useRef(0);
  const dragStartOffset = useRef(0);
  const didDrag = useRef(false);
  const [offsetY, setOffsetY] = useState(0);
  const [maxOffsetY, setMaxOffsetY] = useState(0);

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
      setOffsetY((currentOffset) => Math.min(currentOffset, nextMaxOffset));
    };

    updateSheetHeight();
    window.addEventListener("resize", updateSheetHeight);

    return () => window.removeEventListener("resize", updateSheetHeight);
  }, []);

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

  return (
    <section
      ref={sectionRef}
      className='fixed bottom-0 left-0 right-0 h-[80vh] z-30 rounded-t-[18px] bg-[#292927] px-[6.3%] pb-13 pt-13 shadow-2xl transition-transform duration-200 ease-out'
      style={{ transform: `translateY(${offsetY}px)` }}>
      <button
        type='button'
        className='absolute left-0 right-0 top-0 flex h-11 cursor-grab touch-none items-start justify-center pt-4 active:cursor-grabbing'
        aria-label={offsetY > maxOffsetY / 2 ? "Razširi poti" : "Skrij poti"}
        onClick={toggleSheet}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}>
        <span className='h-1.5 w-17 rounded-full bg-[#aaa69d]' />
      </button>

      <div className='grid grid-cols-3 gap-9'>
        {routes.map((option) => (
          <button
            key={option.title}
            type='button'
            className={`h-40 rounded-[18px] border px-12 py-7 text-left shadow-lg ${option.className}`}>
            <span className='text-[25px] font-normal text-[#b2ada4]'>
              {option.title}
            </span>
            <strong className='mt-2 block text-[32px] font-bold leading-none text-white'>
              {option.time}
            </strong>
            <div className='mt-4 flex items-center gap-3 text-[#aaa69d]'>
              {option.icons.map((Icon, iconIndex) => (
                <Icon key={iconIndex} />
              ))}
            </div>
          </button>
        ))}
      </div>
    </section>
  );
};
