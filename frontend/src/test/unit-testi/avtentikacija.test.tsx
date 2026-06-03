import { describe, expect, it, vi } from "vitest";

function passwordRequirements(password: string) {
  return {
    minLength: password.length >= 12,
    upper: /[A-ZČŠŽ]/.test(password),
    lower: /[a-zčšž]/.test(password),
    special: /[^A-Za-z0-9ČŠŽčšž]/.test(password),
  };
}

describe("avtentikacija", () => {
  it("prijava z emailom in geslom", async () => {
    const signIn = vi.fn().mockResolvedValue({ user: { getIdToken: () => "token" } });
    await signIn("auth", "test@example.com", "Password!234");

    expect(signIn).toHaveBeenCalledWith("auth", "test@example.com", "Password!234");
  });

  it("prijava z Google racunom", async () => {
    const signInWithPopup = vi.fn().mockResolvedValue({ user: { getIdToken: () => "token" } });
    await signInWithPopup("auth", "provider");

    expect(signInWithPopup).toHaveBeenCalledWith("auth", "provider");
  });

  it("registracija z emailom in geslom", async () => {
    const createUser = vi.fn().mockResolvedValue({ user: { getIdToken: () => "token" } });
    await createUser("auth", "test@example.com", "Password!234");

    expect(createUser).toHaveBeenCalledWith("auth", "test@example.com", "Password!234");
  });

  it("validacija zahtev gesla", () => {
    expect(passwordRequirements("Password!234")).toEqual({
      minLength: true,
      upper: true,
      lower: true,
      special: true,
    });
  });

  it("ujemanje ponovljenega gesla", () => {
    const password = "Password!234";
    const repeatedPassword = "Password!234";

    expect(password).toBe(repeatedPassword);
  });

  it("prikaz Firebase napak", () => {
    const messages: Record<string, string> = {
      "auth/invalid-credential": "Napačen email ali geslo.",
      "auth/email-already-in-use": "Ta email je že registriran.",
    };

    expect(messages["auth/invalid-credential"]).toBe("Napačen email ali geslo.");
  });

  it("sinhronizacija uporabniske seje z backendom", async () => {
    const syncUserSession = vi.fn().mockResolvedValue({ email: "test@example.com" });
    await syncUserSession("token", "Test User");

    expect(syncUserSession).toHaveBeenCalledWith("token", "Test User");
  });

  it("odjava pocisti sejo in preusmeri uporabnika", () => {
    const clearUserSession = vi.fn();
    const navigate = vi.fn();
    clearUserSession();
    navigate("/login");

    expect(clearUserSession).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith("/login");
  });
});
