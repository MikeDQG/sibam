import { afterEach, describe, expect, it, vi } from "vitest";

import { getInstructionText, normalizeWhitespace } from "./text";

describe("text helpers", () => {
  const originalDOMParser = globalThis.DOMParser;

  afterEach(() => {
    vi.unstubAllGlobals();
    Object.defineProperty(globalThis, "DOMParser", {
      configurable: true,
      writable: true,
      value: originalDOMParser,
    });
  });

  it("normalizira zaporedne whitespace znake brez zacetnega ali koncnega presledka", () => {
    expect(normalizeWhitespace("  Pojdi\n\t proti   postaji  ")).toBe(
      "Pojdi proti postaji",
    );
  });

  it("odstrani HTML oznake z DOMParserjem", () => {
    expect(getInstructionText("<b>Pojdi</b> proti <span>postaji</span>")).toBe(
      "Pojdi proti postaji",
    );
  });

  it("vrne prazen tekst za manjkajoce navodilo", () => {
    expect(getInstructionText()).toBe("");
    expect(getInstructionText(null)).toBe("");
  });

  it("uporabi fallback odstranjevanje HTML oznak, ko DOMParser ni na voljo", () => {
    Object.defineProperty(globalThis, "DOMParser", {
      configurable: true,
      writable: true,
      value: undefined,
    });

    expect(getInstructionText("<b>Pojdi</b> proti <span>postaji</span>")).toBe(
      "Pojdi proti postaji",
    );
    expect(getInstructionText("Pazi < na znak brez konca")).toBe(
      "Pazi < na znak brez konca",
    );
  });
});
