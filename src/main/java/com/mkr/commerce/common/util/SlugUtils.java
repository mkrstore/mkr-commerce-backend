package com.mkr.commerce.common.util;

import java.text.Normalizer;

public final class SlugUtils {

    private SlugUtils() {}

    public static String toSlug(String input) {
        if (input == null) return null;
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        return normalized
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }
}
