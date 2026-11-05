package org.local.karbalascreen;

import java.time.*;
import java.util.*;

public final class AdhanPolicyTest {
    private static int checks;
    private static void check(boolean value, String name) { checks++; if (!value) throw new AssertionError(name); }
    private static long at(String time) { return ZonedDateTime.parse("2026-11-02T" + time + ":00+03:00[Asia/Baghdad]").toInstant().toEpochMilli(); }
    public static void main(String[] args) {
        Map<String,String> times = new LinkedHashMap<>();
        times.put("الفجر", "04:59"); times.put("الظهرين", "11:48"); times.put("المغرب والعشاء", "17:29");
        times.put("الشروق", "06:22"); times.put("منتصف الليل", "23:05");
        java.util.List<AdhanPolicy.Event> all = AdhanPolicy.future("2026-11-02", times, at("00:01"));
        check(all.size() == 3, "Only three canonical prayers scheduled");
        check(all.get(0).at == 1793584740000L, "Fajr uses Baghdad instant");
        check(all.get(2).at == at("17:29"), "Isha preserves combined source time");
        check(AdhanPolicy.future("2026-11-02", times, at("11:49")).size() == 1, "Past prayers never replay during scheduling");
        check(AdhanPolicy.future("2026-11-01", times, at("00:01")).isEmpty(), "Stale source day refused");
        check(AdhanPolicy.future("2026-11-03", times, at("00:01")).isEmpty(), "Tomorrow is not today's source");
        check(AdhanPolicy.future("nonsense", times, at("00:01")).isEmpty(), "Malformed source day refused");
        check(AdhanPolicy.future("2026-11-02", times, at("23:59")).isEmpty(), "No alarm beyond published day");
        AdhanPolicy.Event fajr = all.get(0);
        check(AdhanPolicy.shouldPlay(fajr, "2026-11-02", times, fajr.at, false), "On-time event accepted");
        check(AdhanPolicy.shouldPlay(fajr, "2026-11-02", times, fajr.at + 60000, false), "60-second boundary accepted");
        check(!AdhanPolicy.shouldPlay(fajr, "2026-11-02", times, fajr.at + 60001, false), "Older audible event suppressed");
        check(!AdhanPolicy.shouldPlay(fajr, "2026-11-02", times, fajr.at - 1, false), "Early event refused");
        check(!AdhanPolicy.shouldPlay(fajr, "2026-11-02", times, fajr.at, true), "Persisted duplicate refused");
        check(!AdhanPolicy.shouldPlay(fajr, "2026-11-02", times, fajr.at + 86400000L, false), "Expired day never audible");
        check(!AdhanPolicy.shouldPlay(new AdhanPolicy.Event("2026-11-02", "الشروق", at("06:22")), "2026-11-02", times, at("06:22"), false), "Non-prayer event refused");
        check(!AdhanPolicy.shouldPlay(new AdhanPolicy.Event("2026-11-02", "الفجر", at("05:00")), "2026-11-02", times, at("05:00"), false), "Alarm cannot override source time");
        times.put("الظهرين", "25:99");
        check(AdhanPolicy.future("2026-11-02", times, at("00:01")).isEmpty(), "Malformed required prayer invalidates day");
        times.remove("الظهرين");
        check(AdhanPolicy.future("2026-11-02", times, at("00:01")).isEmpty(), "Incomplete source never fabricates prayer");
        System.out.println("Adhan policy checks passed: " + checks);
    }
}
