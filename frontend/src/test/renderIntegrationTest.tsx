import { render } from "@testing-library/react";
import type { ReactElement } from "react";
import { MemoryRouter } from "react-router-dom";
import { ThemeProvider } from "../components/ThemeProvider";

type RenderIntegrationOptions = {
  route?: string;
};

export function renderIntegration(
  ui: ReactElement,
  { route = "/" }: RenderIntegrationOptions = {},
) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <ThemeProvider>{ui}</ThemeProvider>
    </MemoryRouter>,
  );
}
