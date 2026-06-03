function hasOnlyWhitespace(character: string) {
  return character.trim() === "";
}

export function normalizeWhitespace(value: string) {
  let normalized = "";
  let hasPendingSpace = false;

  for (const character of value) {
    if (hasOnlyWhitespace(character)) {
      hasPendingSpace = normalized.length > 0;
      continue;
    }

    if (hasPendingSpace) {
      normalized += " ";
      hasPendingSpace = false;
    }

    normalized += character;
  }

  return normalized;
}

function stripHtmlTags(value: string) {
  let text = "";
  let index = 0;

  while (index < value.length) {
    const character = value[index];

    if (character !== "<") {
      text += character;
      index += 1;
      continue;
    }

    const tagEndIndex = value.indexOf(">", index + 1);
    if (tagEndIndex === -1) {
      text += character;
      index += 1;
      continue;
    }

    text += " ";
    index = tagEndIndex + 1;
  }

  return text;
}

export function getInstructionText(instruction?: string | null) {
  if (!instruction) return "";

  if (typeof DOMParser === "undefined") {
    return normalizeWhitespace(stripHtmlTags(instruction));
  }

  const document = new DOMParser().parseFromString(instruction, "text/html");
  return normalizeWhitespace(document.body.textContent ?? "");
}
