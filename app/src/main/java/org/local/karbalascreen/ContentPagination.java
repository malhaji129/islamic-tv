package org.local.karbalascreen;

import java.util.ArrayList;
import java.util.List;

/** Word-boundary display slices; concatenating them reproduces the source exactly. */
final class ContentPagination {
    static final int TV_CHARACTERS = 180;
    private ContentPagination() {}
    static List<String> split(String text, int limit) {
        if (limit < 1) throw new IllegalArgumentException("Positive page limit required");
        List<String> pages = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + limit);
            if (end < text.length()) {
                int boundary = end;
                while (boundary > start && !Character.isWhitespace(text.charAt(boundary - 1))) boundary--;
                if (boundary == start) {
                    // A source word is indivisible, even if it exceeds the budget.
                    while (end < text.length() && !Character.isWhitespace(text.charAt(end))) end++;
                    if (end < text.length()) end++;
                } else end = boundary;
            }
            pages.add(text.substring(start, end));
            start = end;
        }
        return pages;
    }
}
