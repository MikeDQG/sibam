import { describe, expect, it, vi } from "vitest";

import { Toaster } from "../../components/ui/sonner";
import { renderWithTheme, screen } from "../frontendPlanFixtures";

const sonnerProps = vi.hoisted(() => ({
  latest: null as Record<string, unknown> | null,
}));

vi.mock("sonner", () => ({
  Toaster: (props: Record<string, unknown>) => {
    sonnerProps.latest = props;
    return <div data-testid='sonner-toaster' />;
  },
}));

describe("sonner toaster", () => {
  it("poda temo, ikone in toast nastavitve knjižnici sonner", () => {
    renderWithTheme(<Toaster richColors position='top-right' />);

    expect(screen.getByTestId("sonner-toaster")).toBeInTheDocument();
    expect(sonnerProps.latest).toMatchObject({
      theme: expect.any(String),
      className: "toaster group",
      richColors: true,
      position: "top-right",
      toastOptions: {
        classNames: {
          toast: "cn-toast",
        },
      },
    });
    expect(sonnerProps.latest?.icons).toEqual({
      success: expect.any(Object),
      info: expect.any(Object),
      warning: expect.any(Object),
      error: expect.any(Object),
      loading: expect.any(Object),
    });
    expect(sonnerProps.latest?.style).toMatchObject({
      "--normal-bg": "var(--popover)",
      "--normal-text": "var(--popover-foreground)",
      "--normal-border": "var(--border)",
      "--border-radius": "var(--radius)",
    });
  });
});
