package org.local.karbalascreen;

import android.content.Context;
import android.os.CancellationSignal;
import android.util.AtomicFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;

/** The user-approved public shrine timetable, with a dated same-day offline cache. */
public final class PrayerSource {
    private static final String ENDPOINT = "https://www.main.home.alkafeel.net/?lang=ar";
    private static final String SOURCE = "العتبة العباسية المقدسة · كربلاء";
    public static final class PrayerDay {
        public final String profile;
        public final String date;
        public final String hijri;
        public final String source;
        public final LinkedHashMap<String, String> times;
        public PrayerDay(String date, String hijri, String source, LinkedHashMap<String, String> times) {
            this(date,hijri,source,times, "");
        }
        public PrayerDay(String date,String hijri,String source,LinkedHashMap<String,String> times,String profile){
            this.profile=profile;this.date = date; this.hijri = hijri; this.source = source;
            this.times = new LinkedHashMap<>(times);
        }
    }
    private PrayerSource() { }

    public static PrayerDay load(Context context, LocalDate date) throws Exception {
        return load(context, date, new CancellationSignal());
    }
    public static PrayerDay load(Context context, LocalDate date, CancellationSignal cancellation) throws Exception {
        LocationProfile profile=LocationStore.read(new Options(context).prefs);
        if(profile==null)throw new IllegalStateException("Choose city first");
        if(!profile.official){
            String endpoint="https://api.aladhan.com/v1/timings/"+date.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-uuuu"))+"?latitude="+profile.latitude+"&longitude="+profile.longitude+"&method="+profile.method+"&timezonestring="+JsonHttp.encode(profile.timezone);
            return CalculatedPrayerParser.parse(JsonHttp.get(endpoint,cancellation),date,profile);
        }
        cancellation.throwIfCanceled();
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), "prayers-current.html"));
        // Valid cached data avoids a network request on every screensaver activation.
        try (FileInputStream input = file.openRead()) {
            PrayerParser.Day day = PrayerParser.parse(read(input, cancellation), date);
            return result(day,profile);
        } catch (Exception unavailable) { /* Read today's published values below. */ }
        cancellation.throwIfCanceled();
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        cancellation.setOnCancelListener(connection::disconnect);
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(6000);
        connection.setReadTimeout(6000);
        connection.setRequestProperty("User-Agent", "SakinaTV/2.0 (Android TV)");
        try {
            cancellation.throwIfCanceled();
            if (connection.getResponseCode() != 200
                    || !"www.main.home.alkafeel.net".equals(connection.getURL().getHost())) {
                throw new IllegalArgumentException("Prayer source unavailable");
            }
            String html;
            try (InputStream input = connection.getInputStream()) { html = read(input, cancellation); }
            PrayerParser.Day parsed = PrayerParser.parse(html, date);
            cancellation.throwIfCanceled();
            FileOutputStream output = null;
            try {
                output = file.startWrite();
                output.write(html.getBytes(StandardCharsets.UTF_8));
                file.finishWrite(output);
            } catch (Exception storageError) {
                if (output != null) file.failWrite(output);
                // Live verified data remains usable even if its offline cache cannot be saved.
            }
            return result(parsed,profile);
        } finally { cancellation.setOnCancelListener(null); connection.disconnect(); }
    }
    private static PrayerDay result(PrayerParser.Day day,LocationProfile p) {
        return new PrayerDay(day.date, day.hijri, SOURCE+" · تصحيح الدقائق "+java.util.Arrays.toString(p.offsets), day.times,p.identity());
    }
    private static String read(InputStream input, CancellationSignal cancellation) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while (true) {
            cancellation.throwIfCanceled();
            count = input.read(buffer);
            if (count == -1) break;
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            if (bytes.size() + count > 2 * 1024 * 1024) throw new IllegalArgumentException("Source too large");
            bytes.write(buffer, 0, count);
        }
        return bytes.toString(StandardCharsets.UTF_8.name());
    }
}
