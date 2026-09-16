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
