package org.local.karbalascreen;

import java.time.LocalDate;

/** Scheduling and freshness rules shared by rendering and data loading. */
public final class ScreenPolicy {
    public static final long WEATHER_INTERVAL_MS = 30 * 60 * 1000L;
    public static final long PRAYER_RETRY_MS = 15 * 60 * 1000L;
    public static final long PHOTO_INTERVAL_MS = 10 * 60 * 1000L;
    private ScreenPolicy() {}
    public static int intervalSeconds(int value) {
        return value == 30 || value == 45 || value == 60 || value == 90 ? value : 45;
    }
    public static int dateOffset(int value) { return Math.max(-2, Math.min(2, value)); }
    public static int rotate(int current, int delta, int size) {
        return size <= 0 ? 0 : (int) Math.floorMod((long) current + delta, (long) size);
    }
    public static boolean isPrayerDay(String date, LocalDate today) {
        return today.toString().equals(date);
    }
    public static boolean weatherFresh(long fetched, long now) {
        return fetched > 0 && fetched <= now && now - fetched < WEATHER_INTERVAL_MS;
    }
    public static boolean mayRetry(long attempt, long now, long interval) {
        return attempt == 0 || now < attempt || now - attempt >= interval;
    }
    public static boolean contentEnabled(String kind, boolean devotionals) {
        return "quran".equals(kind) || devotionals && ("dua".equals(kind) || "hadith".equals(kind));
    }
    /** Preserve the approved shrine's combined-prayer times under the requested display labels. */
    public static java.util.LinkedHashMap<String, String> prayerDisplay(java.util.Map<String, String> source, boolean ramadan) {
        java.util.LinkedHashMap<String, String> display = new java.util.LinkedHashMap<>();
        display.put("الفجر", source.getOrDefault("الفجر", ""));
        display.put("الظهر", source.getOrDefault("الظهرين", ""));
        display.put("العشاء", source.getOrDefault("المغرب والعشاء", ""));
        if (ramadan) display.put("الإمساك", source.getOrDefault("الإمساك", ""));
        return display;
    }
    /** Queueing a request costs nothing; only a worker that begins consumes an attempt. */
    public static final class AttemptGate {
        private final long interval;
        private long attemptedAt;
        public AttemptGate(long attemptedAt, long interval) { this.attemptedAt = attemptedAt; this.interval = interval; }
        public synchronized boolean ready(long now) { return mayRetry(attemptedAt, now, interval); }
        public synchronized boolean begin(long now, boolean cancelled) {
            if (cancelled || !ready(now)) return false;
            attemptedAt = now;
            return true;
        }
        public synchronized long lastAttempt() { return attemptedAt; }
    }
}
