import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "../../components/ThemeProvider";

const authFunctions = vi.hoisted(() => ({
  signInWithEmailAndPassword: vi.fn(),
  signInWithPopup: vi.fn(),
  createUserWithEmailAndPassword: vi.fn(),
}));

const sessionMock = vi.hoisted(() => ({
  syncUserSession: vi.fn(),
}));

vi.mock("firebase/app", () => {
  class FirebaseError extends Error {
    code: string;

    constructor(code: string, message: string) {
      super(message);
      this.code = code;
    }
  }

  return {
    initializeApp: vi.fn(() => ({})),
    FirebaseError,
  };
});

vi.mock("firebase/auth", () => ({
  getAuth: vi.fn(() => ({})),
  GoogleAuthProvider: vi.fn(function GoogleAuthProvider() {
    return {};
  }),
  signInWithEmailAndPassword: authFunctions.signInWithEmailAndPassword,
  signInWithPopup: authFunctions.signInWithPopup,
  createUserWithEmailAndPassword: authFunctions.createUserWithEmailAndPassword,
}));

vi.mock("../../components/Authorization/UserSessionProvider", () => ({
  useUserSession: () => sessionMock,
}));

import { FirebaseError } from "firebase/app";
import { Login } from "../../components/Authorization/Login";
import { Register } from "../../components/Authorization/Register";

function renderAuthPage(page: "login" | "register") {
  return render(
    <MemoryRouter initialEntries={[`/${page}`]}>
      <ThemeProvider>
        <Routes>
          <Route path='/login' element={<Login />} />
          <Route path='/register' element={<Register />} />
          <Route path='/home' element={<p>Home page</p>} />
          <Route path='/account' element={<p>Account page</p>} />
        </Routes>
      </ThemeProvider>
    </MemoryRouter>,
  );
}

function mockCredential(token = "id-token") {
  return {
    user: {
      getIdToken: vi.fn().mockResolvedValue(token),
    },
  };
}

describe("integracijski Login in Register", () => {
  it("Login z emailom in geslom pokliče Firebase, sync seje in navigira na /home", async () => {
    authFunctions.signInWithEmailAndPassword.mockResolvedValue(mockCredential());
    sessionMock.syncUserSession.mockResolvedValue({});
    renderAuthPage("login");

    fireEvent.change(screen.getByPlaceholderText("Email"), {
      target: { value: "test@example.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("Geslo"), {
      target: { value: "Password!234" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Prijavi se" }));

    await screen.findByText("Home page");
    expect(authFunctions.signInWithEmailAndPassword).toHaveBeenCalledWith(
      expect.anything(),
      "test@example.com",
      "Password!234",
    );
    expect(sessionMock.syncUserSession).toHaveBeenCalledWith("id-token");
  });

  it("Login prikaže slovensko Firebase napako", async () => {
    authFunctions.signInWithEmailAndPassword.mockRejectedValue(
      new FirebaseError("auth/invalid-credential", "Invalid credential"),
    );
    renderAuthPage("login");

    fireEvent.change(screen.getByPlaceholderText("Email"), {
      target: { value: "test@example.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("Geslo"), {
      target: { value: "wrong" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Prijavi se" }));

    expect(await screen.findByText("Napačen email ali geslo.")).toBeInTheDocument();
  });

  it.each([
    ["auth/invalid-email", "Vnesite veljaven email naslov."],
    ["auth/too-many-requests", "Preveč neuspešnih poskusov. Poskusite kasneje."],
    ["auth/unknown", "Prišlo je do napake. Poskusite znova."],
  ])("Login prikaže napako za %s", async (code, message) => {
    authFunctions.signInWithEmailAndPassword.mockRejectedValue(
      new FirebaseError(code, "Firebase error"),
    );
    renderAuthPage("login");

    fireEvent.click(screen.getByRole("button", { name: "Prijavi se" }));

    expect(await screen.findByText(message)).toBeInTheDocument();
  });

  it("Login omogoča prikaz gesla in navigacijo na register ter domov", () => {
    renderAuthPage("login");

    expect(screen.getByPlaceholderText("Geslo")).toHaveAttribute("type", "password");
    fireEvent.click(screen.getByRole("button", { name: "Prikaži geslo" }));
    expect(screen.getByPlaceholderText("Geslo")).toHaveAttribute("type", "text");
    fireEvent.click(screen.getByRole("button", { name: "Skrij geslo" }));
    expect(screen.getByPlaceholderText("Geslo")).toHaveAttribute("type", "password");

    fireEvent.click(screen.getByRole("button", { name: "Registriraj se" }));
    expect(screen.getByRole("heading", { name: "Registracija" })).toBeInTheDocument();
  });

  it("Login z Google računom pokliče signInWithPopup in sync seje", async () => {
    authFunctions.signInWithPopup.mockResolvedValue(mockCredential("google-token"));
    sessionMock.syncUserSession.mockResolvedValue({});
    renderAuthPage("login");

    fireEvent.click(screen.getByRole("button", { name: /Google/ }));

    await screen.findByText("Home page");
    expect(authFunctions.signInWithPopup).toHaveBeenCalledTimes(1);
    expect(sessionMock.syncUserSession).toHaveBeenCalledWith("google-token");
  });

  it("Login prikaže napako pri neuspešni Google prijavi", async () => {
    authFunctions.signInWithPopup.mockRejectedValue(new Error("google"));
    renderAuthPage("login");

    fireEvent.click(screen.getByRole("button", { name: /Google/ }));

    expect(await screen.findByText("Prišlo je do napake pri Google prijavi.")).toBeInTheDocument();
  });

  it("Register validira zahteve gesla in blokira prekratko geslo", async () => {
    renderAuthPage("register");

    fireEvent.change(screen.getByPlaceholderText("Geslo"), {
      target: { value: "short" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Registriraj se" }));

    expect(await screen.findByText("Geslo ne ustreza zahtevam!")).toBeInTheDocument();
    expect(authFunctions.createUserWithEmailAndPassword).not.toHaveBeenCalled();
  });

  it("Register ob uspehu pokliče Firebase, sync seje z imenom in navigira na /account", async () => {
    authFunctions.createUserWithEmailAndPassword.mockResolvedValue(mockCredential());
    sessionMock.syncUserSession.mockResolvedValue({});
    renderAuthPage("register");

    fireEvent.change(screen.getByPlaceholderText("Ime in priimek"), {
      target: { value: "Test User" },
    });
    fireEvent.change(screen.getByPlaceholderText("Email"), {
      target: { value: "test@example.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("Geslo"), {
      target: { value: "Password!234" },
    });
    fireEvent.change(screen.getByPlaceholderText("Ponovi geslo"), {
      target: { value: "Password!234" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Registriraj se" }));

    await screen.findByText("Account page");
    expect(authFunctions.createUserWithEmailAndPassword).toHaveBeenCalledWith(
      expect.anything(),
      "test@example.com",
      "Password!234",
    );
    expect(sessionMock.syncUserSession).toHaveBeenCalledWith("id-token", "Test User");
  });

  it("Register prikaže napako ob neujemajočih geslih", async () => {
    renderAuthPage("register");

    fireEvent.change(screen.getByPlaceholderText("Ime in priimek"), {
      target: { value: "Test User" },
    });
    fireEvent.change(screen.getByPlaceholderText("Email"), {
      target: { value: "test@example.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("Geslo"), {
      target: { value: "Password!234" },
    });
    fireEvent.change(screen.getByPlaceholderText("Ponovi geslo"), {
      target: { value: "Password!999" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Registriraj se" }));

    await waitFor(() => expect(screen.getByText("Gesli se ne ujemata!")).toBeInTheDocument());
  });

  it("Register zahteva ime in veljaven email", async () => {
    renderAuthPage("register");

    fireEvent.change(screen.getByPlaceholderText("Email"), {
      target: { value: "test@example.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("Geslo"), {
      target: { value: "Password!234" },
    });
    fireEvent.change(screen.getByPlaceholderText("Ponovi geslo"), {
      target: { value: "Password!234" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Registriraj se" }));
    expect(await screen.findByText("Vnesite ime in priimek.")).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText("Ime in priimek"), {
      target: { value: "Test User" },
    });
    fireEvent.change(screen.getByPlaceholderText("Email"), {
      target: { value: "test example.com" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Registriraj se" }));
    expect(await screen.findByText("Vnesite veljaven email naslov.")).toBeInTheDocument();
  });

  it.each([
    ["auth/email-already-in-use", "Ta email je že registriran."],
    ["auth/invalid-email", "Vnesite veljaven email naslov."],
    ["auth/weak-password", "Geslo je prešibko."],
    ["auth/unknown", "Prišlo je do napake. Poskusite znova."],
  ])("Register prikaže Firebase napako za %s", async (code, message) => {
    authFunctions.createUserWithEmailAndPassword.mockRejectedValue(
      new FirebaseError(code, "Firebase error"),
    );
    renderAuthPage("register");

    fireEvent.change(screen.getByPlaceholderText("Ime in priimek"), {
      target: { value: "Test User" },
    });
    fireEvent.change(screen.getByPlaceholderText("Email"), {
      target: { value: "test@example.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("Geslo"), {
      target: { value: "Password!234" },
    });
    fireEvent.change(screen.getByPlaceholderText("Ponovi geslo"), {
      target: { value: "Password!234" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Registriraj se" }));

    expect(await screen.findByText(message)).toBeInTheDocument();
  });

  it("Register omogoča prikaz obeh gesel in navigacijo na login", () => {
    renderAuthPage("register");

    const passwordInputs = [
      screen.getByPlaceholderText("Geslo"),
      screen.getByPlaceholderText("Ponovi geslo"),
    ];
    expect(passwordInputs[0]).toHaveAttribute("type", "password");
    expect(passwordInputs[1]).toHaveAttribute("type", "password");

    fireEvent.click(screen.getAllByRole("button", { name: "Prikaži geslo" })[0]);
    fireEvent.click(screen.getAllByRole("button", { name: "Prikaži geslo" })[0]);
    expect(passwordInputs[0]).toHaveAttribute("type", "text");
    expect(passwordInputs[1]).toHaveAttribute("type", "text");

    fireEvent.click(screen.getByRole("button", { name: "Prijavi se" }));
    expect(screen.getByRole("heading", { name: "Prijava" })).toBeInTheDocument();
  });

  it("Register z Google računom sinhronizira sejo in prikaže napako ob neuspehu", async () => {
    authFunctions.signInWithPopup.mockResolvedValueOnce(mockCredential("google-register-token"));
    sessionMock.syncUserSession.mockResolvedValue({});
    renderAuthPage("register");

    fireEvent.click(screen.getByRole("button", { name: /Google/ }));
    await screen.findByText("Account page");
    expect(sessionMock.syncUserSession).toHaveBeenCalledWith("google-register-token");

    authFunctions.signInWithPopup.mockRejectedValueOnce(new Error("google"));
    renderAuthPage("register");
    fireEvent.click(screen.getByRole("button", { name: /Google/ }));
    expect(await screen.findByText("Prišlo je do napake pri Google registraciji.")).toBeInTheDocument();
  });
});
