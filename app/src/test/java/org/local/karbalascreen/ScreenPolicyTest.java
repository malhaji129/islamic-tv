package org.local.karbalascreen;

import java.time.LocalDate;

/** Runs without Android or third party dependencies: scripts/test-policy.sh. */
public final class ScreenPolicyTest {
    private static int checks;
    private static void check(boolean result, String reason) {
        checks++;
        if (!result) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        check(ScreenPolicy.intervalSeconds(30) == 30, "minimum supported interval");
        check(ScreenPolicy.intervalSeconds(90) == 90, "maximum supported interval");
        check(ScreenPolicy.intervalSeconds(0) == 45, "corrupt interval cannot spin");
        check(ScreenPolicy.intervalSeconds(1) == 45, "one-second rotation rejected");
        check(ScreenPolicy.intervalSeconds(Integer.MAX_VALUE) == 45, "overflow rejected");
        check(ScreenPolicy.dateOffset(-9) == -2, "negative Hijri correction bounded");
        check(ScreenPolicy.dateOffset(8) == 2, "positive Hijri correction bounded");
        check(ScreenPolicy.dateOffset(1) == 1, "valid correction retained");
        check(ScreenPolicy.rotate(0, -1, 3) == 2, "left rotation wraps");
        check(ScreenPolicy.rotate(2, 1, 3) == 0, "right rotation wraps");
        check(ScreenPolicy.rotate(5, 1, 0) == 0, "empty content safe");
        LocalDate today = LocalDate.of(2026, 11, 2);
        check(ScreenPolicy.isPrayerDay("2026-11-02", today), "today valid");
        check(!ScreenPolicy.isPrayerDay("2026-11-01", today), "yesterday never shown today");
        check(!ScreenPolicy.isPrayerDay("2026-11-03", today), "future date rejected");
        check(!ScreenPolicy.isPrayerDay("", today), "missing date rejected");
        long now = 9_000_000L;
        check(ScreenPolicy.weatherFresh(now - 1_799_999L, now), "weather under 30 minutes fresh");
        check(!ScreenPolicy.weatherFresh(now - 1_800_000L, now), "30-minute weather stale");
        check(!ScreenPolicy.weatherFresh(now + 1, now), "clock rollback not fresh");
        check(!ScreenPolicy.weatherFresh(0, now), "missing timestamp not fresh");
        check(ScreenPolicy.mayRetry(0, now, 900_000), "initial load allowed");
        check(!ScreenPolicy.mayRetry(now - 899_999, now, 900_000), "failed prayer requests throttled");
        check(ScreenPolicy.mayRetry(now - 900_000, now, 900_000), "prayer retry after 15min");
        ScreenPolicy.AttemptGate weatherQueue = new ScreenPolicy.AttemptGate(0, ScreenPolicy.WEATHER_INTERVAL_MS);
        check(weatherQueue.ready(now), "weather may be queued initially");
        check(!weatherQueue.begin(now + 5000, true), "cancelled queued task does not start");
        check(weatherQueue.lastAttempt() == 0, "cancel before worker start consumes no request timestamp");
        check(weatherQueue.begin(now + 10000, false), "next preview can start weather immediately");
        check(weatherQueue.lastAttempt() == now + 10000, "timestamp records worker start, not original queue time");
        check(!weatherQueue.begin(now + 10001, false), "actual weather request enforces retry interval");
        check(weatherQueue.begin(now + 10000 + ScreenPolicy.WEATHER_INTERVAL_MS, false), "weather retry available after actual start interval");
        ScreenPolicy.AttemptGate prayerQueue = new ScreenPolicy.AttemptGate(0, ScreenPolicy.PRAYER_RETRY_MS);
        check(!prayerQueue.begin(now, true) && prayerQueue.ready(now + 1), "cancelled prayer task leaves next preview available");
        check(prayerQueue.begin(now + 1, false), "prayer request begins in next preview");
        check(!prayerQueue.ready(now + ScreenPolicy.PRAYER_RETRY_MS), "prayer retry measured from actual worker start");
        check(ScreenPolicy.contentEnabled("hadith", true), "verified sayings enter devotional rotation");
        check(!ScreenPolicy.contentEnabled("hadith", false), "devotional toggle hides sayings");
        check(!ScreenPolicy.contentEnabled("unknown", true), "unknown content kinds rejected");
        check(ScreenPolicy.contentEnabled("quran", false), "Quran remains when devotional toggle disabled");
        java.util.LinkedHashMap<String, String> fullTimes = new java.util.LinkedHashMap<>();
        fullTimes.put("الفجر", "04:59"); fullTimes.put("الشروق", "06:22"); fullTimes.put("الظهرين", "11:47");
        fullTimes.put("المغرب والعشاء", "17:29"); fullTimes.put("منتصف الليل", "23:05"); fullTimes.put("العشاء", "19:00");
        java.util.LinkedHashMap<String, String> shown = ScreenPolicy.prayerDisplay(fullTimes, false);
        check(shown.size() == 3 && !shown.containsKey("الشروق") && !shown.containsKey("منتصف الليل"), "ordinary prayer row has exactly requested three labels");
        check("17:29".equals(shown.get("العشاء")), "Isha label uses verified combined shrine time, never unrelated Isha value");
        check("11:47".equals(shown.get("الظهر")), "Dhuhr label uses verified combined shrine time");
        check("".equals(ScreenPolicy.prayerDisplay(fullTimes, true).get("الإمساك")), "Ramadan missing imsak remains unavailable");
        fullTimes.put("الإمساك", "04:49");
        check("04:49".equals(ScreenPolicy.prayerDisplay(fullTimes, true).get("الإمساك")), "Ramadan imsak uses only supplied verified value");
        check(!ScreenPolicy.prayerDisplay(fullTimes, false).containsKey("الإمساك"), "imsak not added outside Ramadan");
        ScreenPolicy.AttemptGate photoQueue = new ScreenPolicy.AttemptGate(now, ScreenPolicy.PHOTO_INTERVAL_MS);
        check(!photoQueue.ready(now + 599_999), "photo cannot change before ten minutes");
        check(photoQueue.begin(now + 600_000, false), "photo may change at ten minutes");
        check(!photoQueue.begin(now + 600_001, false), "minute ticks cannot repeatedly decode photos");
        check(!ScreenPolicy.mayRetry(now + 1000, now + 600_999, ScreenPolicy.PHOTO_INTERVAL_MS), "photo display interval measured from actual bitmap application");
        check(ScreenPolicy.mayRetry(now + 1000, now + 601_000, ScreenPolicy.PHOTO_INTERVAL_MS), "next displayed photo waits at least ten full minutes");
        System.out.println("PASS " + checks + " policy checks");
    }
}
