import { Moon, Sun } from "lucide-react";
import { Button } from "./ui/button";
import { useTheme } from "./ThemeProvider";
import { cn } from "@/lib/utils";

type ThemeToggleProps = {
  className?: string;
};

export function ThemeToggle({ className }: ThemeToggleProps) {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === "dark";

  return (
    <Button
      type='button'
      variant='outline'
      size='icon-lg'
      onClick={toggleTheme}
      className={cn(
        "h-10 w-10 rounded-md border-white/15 bg-white/85 text-neutral-900 shadow-lg backdrop-blur-md hover:bg-white dark:bg-neutral-700 dark:text-white dark:hover:bg-neutral-600",
        className,
      )}
      aria-label={isDark ? "Preklopi na svetlo temo" : "Preklopi na temno temo"}
      title={isDark ? "Svetla tema" : "Temna tema"}>
      {isDark ? <Sun size={20} /> : <Moon size={20} />}
    </Button>
  );
}
