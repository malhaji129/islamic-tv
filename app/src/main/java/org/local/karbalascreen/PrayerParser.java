package org.local.karbalascreen;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses dated, published shrine values. It deliberately performs no prayer calculation. */
final class PrayerParser {
    static final class Day {
        final String date;
        final String hijri;
        final LinkedHashMap<String, String> times;
        Day(String date, String hijri, LinkedHashMap<String, String> times) {
            this.date = date;
            this.hijri = hijri;
            this.times = times;
        }
    }

    private static final String[] MONTHS = {"يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};
    private static final String[] LABELS = {"الفجر", "الشروق", "الظهرين", "المغرب والعشاء", "منتصف الليل"};
    private PrayerParser() { }

    static Day parse(String html, LocalDate expected) {
        if (html == null || html.length() > 2 * 1024 * 1024
                || !html.contains("الأوقات الشرعية لمدينة كربلاء المقدسة")) {
            throw new IllegalArgumentException("Not the Karbala timetable");
        }
        String dateText = capture(html,
                "<span\\s+class=[\"']date-text\\s+d-none\\s+d-md-inline[\"'][^>]*>(.*?)</span>");
        Matcher date = Pattern.compile("(\\d{1,2})\\s+([^\\d]+?)\\s+(\\d{4})")
                .matcher(digits(clean(dateText)));
        if (!date.find()) throw new IllegalArgumentException("Gregorian publication date missing");
        String monthText = date.group(2).trim();
        int month = 0;
        for (int i = 0; i < MONTHS.length; i++) if (MONTHS[i].equals(monthText)) month = i + 1;
        if (month == 0) throw new IllegalArgumentException("Unrecognized publication month");
        LocalDate actual = LocalDate.of(Integer.parseInt(date.group(3)), month,
                Integer.parseInt(date.group(1)));
        if (!actual.equals(expected)) throw new IllegalArgumentException("Timetable is not for today");
        String hijri = clean(capture(html, "<span\\s+class=[\"']date-text[\"'][^>]*>(.*?)</span>"));
        if (hijri.length() > 100) throw new IllegalArgumentException("Invalid Hijri label");

        Matcher entry = Pattern.compile("<span\\s+class=[\"']prayer-name[\"'][^>]*>(.*?)</span>"
                + "\\s*<span\\s+class=[\"']prayer-value[\"'][^>]*>(.*?)</span>", Pattern.DOTALL).matcher(html);
        String[] source = new String[5];
        while (entry.find()) {
            String label = clean(entry.group(1));
            int index = label.contains("الصبح") ? 0 : label.contains("شروق") ? 1
                    : label.contains("الظهر") ? 2 : label.contains("العشاء") ? 3
                    : label.contains("منتصف") ? 4 : -1;
            if (index >= 0) {
                if (source[index] != null) throw new IllegalArgumentException("Duplicate prayer value");
                source[index] = digits(clean(entry.group(2)));
            }
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        int[] minutes = new int[5];
        for (int i = 0; i < source.length; i++) {
            if (source[i] == null || !source[i].matches("\\d{2}:\\d{2}")) {
                throw new IllegalArgumentException("Missing prayer value");
            }
            int hour = Integer.parseInt(source[i].substring(0, 2));
            int minute = Integer.parseInt(source[i].substring(3, 5));
            if (hour > 23 || minute > 59) throw new IllegalArgumentException("Invalid time");
            if (i == 3 && hour < 12) hour += 12;
            if (i == 4) {
                if (hour == 12) hour = 0;
                else if (hour >= 10 && hour < 12) hour += 12;
            }
            if ((i == 0 && (hour < 2 || hour > 6)) || (i == 1 && (hour < 4 || hour > 8))
                    || (i == 2 && (hour < 10 || hour > 14)) || (i == 3 && (hour < 15 || hour > 22))
                    || (i == 4 && !(hour >= 21 || hour <= 1))) {
                throw new IllegalArgumentException("Time outside Karbala range");
            }
            minutes[i] = hour * 60 + minute;
            values.put(LABELS[i], String.format(Locale.ROOT, "%02d:%02d", hour, minute));
        }
        if (!(minutes[0] < minutes[1] && minutes[1] < minutes[2] && minutes[2] < minutes[3])) {
            throw new IllegalArgumentException("Prayer order invalid");
        }
        return new Day(actual.toString(), hijri, values);
    }

    private static String capture(String text, String expression) {
        Matcher matcher = Pattern.compile(expression, Pattern.DOTALL).matcher(text);
        if (!matcher.find()) throw new IllegalArgumentException("Required source field missing");
        return matcher.group(1);
    }
    private static String clean(String value) {
        return value.replaceAll("<[^>]*>", "").replace("&nbsp;", " ")
                .replace("&#160;", " ").replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }
    private static String digits(String value) {
        StringBuilder output = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c >= '\u0660' && c <= '\u0669') output.append((char) ('0' + c - '\u0660'));
            else if (c >= '\u06f0' && c <= '\u06f9') output.append((char) ('0' + c - '\u06f0'));
            else output.append(c);
        }
        return output.toString();
    }
}
