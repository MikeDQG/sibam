import { type ComponentType, useEffect, useState } from "react";
import { FaHome, FaLandmark } from "react-icons/fa";
import { IoIosSchool } from "react-icons/io";
import { MdWork } from "react-icons/md";
import { Button } from "../ui/button";
import { Input } from "../ui/input";

type MapCenter = {
  lat: number;
  lng: number;
};

export type LocationIcon = "school" | "home" | "work" | "landmark";

export type MapLocationDraft = {
  position: MapCenter;
  name: string;
  color: string;
  icon: LocationIcon;
};

type MapLocationPopupProps = {
  draft: MapLocationDraft;
  onColorChange?: (color: string) => void;
  onIconChange?: (icon: LocationIcon) => void;
  onSave?: (name: string) => void;
};

const locationColors = [
  { label: "Rdeča", value: "#b91c1c" },
  { label: "Modra", value: "#2563eb" },
  { label: "Zelena", value: "#15803d" },
  { label: "Rumena", value: "#ca8a04" },
  { label: "Vijolična", value: "#7c3aed" },
];

const locationIcons = [
  { label: "Dom", value: "home", Icon: FaHome },
  { label: "Šola", value: "school", Icon: IoIosSchool },
  { label: "Delo", value: "work", Icon: MdWork },
  { label: "Znamenitost", value: "landmark", Icon: FaLandmark },
] satisfies {
  label: string;
  value: LocationIcon;
  Icon: ComponentType<{ size?: number; className?: string }>;
}[];

export function MapLocationPopup({
  draft,
  onColorChange,
  onIconChange,
  onSave,
}: MapLocationPopupProps) {
  const [name, setName] = useState(draft.name);

  useEffect(() => {
    setName(draft.name);
  }, [draft.position.lat, draft.position.lng, draft.name]);

  return (
    <div className='w-64 mx-2 mb-2 bg-card text-sm text-card-foreground dark:bg-neutral-800 dark:text-white'>
      <strong className='block pr-8 text-base font-semibold'>
        Nova lokacija
      </strong>

      <div className='mt-3 space-y-3'>
        <Input
          autoFocus
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder='Ime lokacije'
          className='h-9'
        />

        <div className='flex items-center gap-2'>
          {locationColors.map((color) => (
            <button
              key={color.value}
              type='button'
              onClick={() => onColorChange?.(color.value)}
              className={`h-7 w-7 rounded-full border-2 transition ${
                draft.color === color.value
                  ? "border-foreground ring-2 ring-ring/40"
                  : "border-white/80 hover:scale-105"
              }`}
              style={{ backgroundColor: color.value }}
              aria-label={color.label}
            />
          ))}
        </div>

        <div className='flex items-center gap-2'>
          {locationIcons.map(({ label, value, Icon }) => (
            <button
              key={value}
              type='button'
              onClick={() => onIconChange?.(value)}
              className={`flex h-9 w-9 items-center justify-center rounded-md border transition ${
                draft.icon === value
                  ? "border-foreground bg-muted text-foreground ring-2 ring-ring/40 dark:bg-neutral-700 dark:text-white"
                  : "border-border text-muted-foreground hover:bg-muted hover:text-foreground dark:border-white/10 dark:hover:bg-neutral-700 dark:hover:text-white"
              }`}
              aria-label={label}
              title={label}>
              <Icon size={20} />
            </button>
          ))}
        </div>
      </div>

      <div className='flex justify-end pt-4'>
        <Button type='button' onClick={() => onSave?.(name)}>
          Shrani
        </Button>
      </div>
    </div>
  );
}
