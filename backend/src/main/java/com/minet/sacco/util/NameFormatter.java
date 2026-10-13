package com.minet.sacco.util;

import java.util.Locale;
import java.util.Set;

/**
 * Normalises person names to Proper Case ("WAKUKHA ANTONY" -> "Wakukha Antony").
 * Handles apostrophes (Karung'a), hyphens (Mary-Ann), "Mc"/"Mac" prefixes,
 * and keeps common honorifics and particles in a sensible form.
 */
public final class NameFormatter {

    private static final Set<String> LOWER_PARTICLES = Set.of("van", "von", "de", "der", "da", "del", "bin", "binti", "wa", "ya");
    private static final Set<String> UPPER_TOKENS = Set.of("ii", "iii", "iv");

    private NameFormatter() {}

    public static String toProperCase(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().replaceAll("\s+", " ");
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        String[] words = trimmed.split(" ");
        StringBuilder out = new StringBuilder(trimmed.length());
        for (int i = 0; i < words.length; i++) {
            if (i > 0) out.append(' ');
            out.append(formatWord(words[i], i == 0));
        }
        return out.toString();
    }

    private static String formatWord(String word, boolean first) {
        String lower = word.toLowerCase(Locale.ROOT);
        if (UPPER_TOKENS.contains(lower)) {
            return lower.toUpperCase(Locale.ROOT);
        }
        if (!first && LOWER_PARTICLES.contains(lower)) {
            return lower;
        }
        StringBuilder sb = new StringBuilder(word.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (capitalizeNext && Character.isLetter(c)) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
            if (c == '-' || c == '.') {
                capitalizeNext = true;
            } else if (c == '\'' || c == '’') {
                // O'Neil, D'Souza -> capitalise; Karung'a, Ng'ang'a -> keep lower
                int letters = 0;
                for (int k = i - 1; k >= 0 && Character.isLetter(lower.charAt(k)); k--) letters++;
                capitalizeNext = letters <= 2 && letters == i;
            }
        }
        String result = sb.toString();
        // McDonald, MacArthur style (only when followed by 3+ more letters)
        if (result.length() > 4 && result.startsWith("Mc") && Character.isLetter(result.charAt(2))) {
            result = "Mc" + Character.toUpperCase(result.charAt(2)) + result.substring(3);
        }
        return result;
    }
}
