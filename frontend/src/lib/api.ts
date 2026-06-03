const apiBaseUrl = import.meta.env.VITE_API_URL;

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export type UuidString = string & { readonly __uuidBrand: unique symbol };

export function parseUuid(value: unknown, fieldName = "id"): UuidString {
  if (typeof value === "string" && uuidPattern.test(value)) {
    return value as UuidString;
  }

  throw new Error(`Invalid ${fieldName}`);
}

export function buildApiUrl(...segments: string[]) {
  const url = new URL(apiBaseUrl);
  const basePath = url.pathname.replace(/\/+$/, "");
  const encodedSegments = segments.map((segment) =>
    encodeURIComponent(segment),
  );

  url.pathname = [basePath, ...encodedSegments].filter(Boolean).join("/");
  return url.toString();
}
