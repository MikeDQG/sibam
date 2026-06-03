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

  for (let index = 0; index < value.length; index += 1) {
    const character = value[index];

    if (character !== "<") {
      text += character;
      continue;
    }

    const tagEndIndex = value.indexOf(">", index + 1);
    if (tagEndIndex === -1) {
      text += character;
      continue;
    }

    text += " ";
    index = tagEndIndex;
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
