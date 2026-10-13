import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Format a number as KES currency.
 * Always outputs "KES X,XXX.XX" — never a dollar sign.
 */
export function formatCurrency(amount?: number | null): string {
  if (amount === undefined || amount === null) return "-";
  return "KES " + new Intl.NumberFormat("en-KE", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

/**
 * Proper-case a person's name for display: "WAKUKHA ANTONY" -> "Wakukha Antony".
 * Mirrors the backend NameFormatter so legacy/uppercase data always renders consistently.
 */
export function formatName(name?: string | null): string {
  if (!name) return "";
  const particles = new Set(["van", "von", "de", "der", "da", "del", "bin", "binti", "wa", "ya"]);
  return name
    .trim()
    .replace(/\s+/g, " ")
    .split(" ")
    .map((word, idx) => {
      const lower = word.toLowerCase();
      if (["ii", "iii", "iv"].includes(lower)) return lower.toUpperCase();
      if (idx > 0 && particles.has(lower)) return lower;
      let out = "";
      let cap = true;
      for (let i = 0; i < lower.length; i++) {
        const c = lower[i];
        if (cap && /\p{L}/u.test(c)) {
          out += c.toUpperCase();
          cap = false;
        } else {
          out += c;
        }
        if (c === "-" || c === ".") cap = true;
        else if (c === "'" || c === "’") cap = i <= 2 && /^\p{L}+$/u.test(lower.slice(0, i));
      }
      if (out.length > 4 && out.startsWith("Mc") && /\p{L}/u.test(out[2])) {
        out = "Mc" + out[2].toUpperCase() + out.slice(3);
      }
      return out;
    })
    .join(" ");
}
