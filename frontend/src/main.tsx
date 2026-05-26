import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "./index.css";
import App from "./App.tsx";
import { ThemeProvider } from "./components/ThemeProvider";
import { Toaster } from "./components/ui/sonner";
import { UserSessionProvider } from "./components/UserSessionProvider";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ThemeProvider>
      <UserSessionProvider>
        <BrowserRouter>
          <App />
          <Toaster richColors position='top-right' />
        </BrowserRouter>
      </UserSessionProvider>
    </ThemeProvider>
  </StrictMode>,
);
