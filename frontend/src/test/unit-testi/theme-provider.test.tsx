import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ThemeProvider, useTheme } from "../../components/ThemeProvider";

function ThemeProbe() {
  const { theme, setTheme, toggleTheme } = useTheme();

  return (
    <div>
      <span data-testid='theme'>{theme}</span>
      <button type='button' onClick={toggleTheme}>
        Toggle
      </button>
      <button type='button' onClick={() => setTheme("light")}>
        Set light
      </button>
    </div>
  );
}

function BrokenConsumer() {
  useTheme();
  return null;
}

afterEach(() => {
  localStorage.clear();
  vi.clearAllMocks();
});

describe("ThemeProvider", () => {
  it("uporabi shranjeno temo, nastavi razrede in preklaplja temo", () => {
    localStorage.setItem("sibam-theme", "dark");

    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>,
    );

    expect(screen.getByTestId("theme")).toHaveTextContent("dark");
    expect(document.documentElement).toHaveClass("dark");
    expect(document.documentElement.style.colorScheme).toBe("dark");

    fireEvent.click(screen.getByRole("button", { name: "Toggle" }));
    expect(screen.getByTestId("theme")).toHaveTextContent("light");
    expect(document.documentElement).toHaveClass("light");
    expect(localStorage.getItem("sibam-theme")).toBe("light");

    fireEvent.click(screen.getByRole("button", { name: "Toggle" }));
    expect(screen.getByTestId("theme")).toHaveTextContent("dark");

    fireEvent.click(screen.getByRole("button", { name: "Set light" }));
    expect(screen.getByTestId("theme")).toHaveTextContent("light");
  });

  it("uporabi sistemsko svetlo temo, ko shranjena vrednost ni veljavna", () => {
    localStorage.setItem("sibam-theme", "modra");
    vi.mocked(window.matchMedia).mockReturnValue({
      matches: false,
      media: "(prefers-color-scheme: dark)",
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    });

    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>,
    );

    expect(screen.getByTestId("theme")).toHaveTextContent("light");
  });

  it("zahteva ThemeProvider za useTheme", () => {
    expect(() => render(<BrokenConsumer />)).toThrow(
      "useTheme must be used within ThemeProvider",
    );
  });
});
