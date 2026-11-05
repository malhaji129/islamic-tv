package org.local.karbalascreen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public final class PrayerParserTest {
    private static int count;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        count++;
    }
    private static void refuses(String html, LocalDate day) {
        try {
            PrayerParser.parse(html, day);
            throw new AssertionError("Untrusted or stale timetable accepted");
        } catch (IllegalArgumentException expected) { count++; }
    }
    public static void main(String[] args) throws Exception {
        String html = Files.readString(Path.of(args[0]));
        LocalDate day = LocalDate.of(2026, 11, 2);
        PrayerParser.Day parsed = PrayerParser.parse(html, day);
        check(parsed.date.equals("2026-11-02"), "Gregorian date");
        check(parsed.hijri.contains("جمادى الأولى"), "Observed Hijri");
        check(parsed.times.size() == 5, "All five source entries");
        check(parsed.times.get("الفجر").equals("04:59"), "Fajr");
        check(parsed.times.get("المغرب والعشاء").equals("17:29"), "PM normalization");
        check(parsed.times.get("منتصف الليل").equals("23:05"), "Midnight normalization");
        refuses(html, day.plusDays(1));
        refuses(html.replace("04:59", "29:59"), day);
        refuses(html.replace("صلاة العشاءَيْن", "غير معروف"), day);
        refuses(html.replace("كربلاء المقدسة", "مدينة أخرى"), day);
        refuses(html.replace("02 نوفمبر 2026", "03 نوفمبر 2026"), day);
        String arabicDigits = html.replace("02 نوفمبر 2026", "٠٢ نوفمبر ٢٠٢٦");
        check(PrayerParser.parse(arabicDigits, day).date.equals("2026-11-02"), "Arabic date digits");
        String afterMidnight = html.replace("11:05", "12:05");
        check(PrayerParser.parse(afterMidnight, day).times.get("منتصف الليل").equals("00:05"), "12AM normalization");
        System.out.println("Prayer source tests passed: " + count);
    }
}
