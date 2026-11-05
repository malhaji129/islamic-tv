package org.local.karbalascreen;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.drawable.LayerDrawable;
import android.icu.util.IslamicCalendar;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Minute-based content, bounded optional 8fps weather, and no resident screen service. */
final class ScreenView extends LinearLayout {
    private LocationProfile profile;
    private ZoneId selectedZone(){return profile==null?ZoneId.systemDefault():profile.zone();}
    private static final int INK = Color.rgb(221, 226, 219), MUTED = Color.rgb(133, 159, 151), GOLD = Color.rgb(184, 167, 126);
    private static Typeface arabicTypeface, quranTypeface;
    private final Options options;
    private final AtmospherePolicy.Mode mode;
    private final AtmosphereDrawable atmosphere = new AtmosphereDrawable();
    private AtmospherePolicy.Phase solarPhase = AtmospherePolicy.Phase.DAY;
    private int previewWeather, previewPhase;
    private static final int[] PREVIEW_CODES = {0,2,3,45,51,56,61,66,71,77,80,85,95,99,0,0};
    private String photoPeriod = "";
    private volatile String photoAttemptPeriod = "";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final WeatherSource weatherSource = new WeatherSource();
    private final PhotoBackground photoBackground = new PhotoBackground();
    private ScreenPolicy.AttemptGate photoAttempts = new ScreenPolicy.AttemptGate(0, ScreenPolicy.PHOTO_INTERVAL_MS);
    private final TextView city, clock, date, hijri, passage, reference, attribution, prayerSource, weather;
    private final LinearLayout prayers, content, body;
    private ExecutorService executor;
    private CancellationSignal cancellation;
    private ScreenPolicy.AttemptGate prayerAttempts, weatherAttempts;
    private List<ContentStore.Item> items;
    private List<PhotoStore.Photo> photos;
    private PrayerSource.PrayerDay prayerDay;
    private WeatherSource.Weather weatherData;
    private WeatherSource.Dust dustData;
    private ScreenPolicy.AttemptGate dustAttempts;
    private boolean visible, prayerBusy, weatherBusy, photoBusy;
    private int index, generation, photoIndex;
    private long photoShownAt;
    private LocalDate day;
    private final Runnable effectsExpiry = () -> { if (visible) { updateAtmosphere(); renderWeather(); } };
    private final Runnable rotation = new Runnable() {
        public void run() { if (!visible) return; navigate(1); handler.postDelayed(this, options.interval() * 1000L); }
    };
    private final Runnable minute = new Runnable() {
        public void run() {
            if (!visible) return;
            updateTime(); updateAtmosphere(); refreshPhoto(); if (mode.allowsLiveData()) refreshData(); renderWeather();
            long phase = System.currentTimeMillis() / 60000;
            body.setTranslationX(dp((int) (phase % 5 - 2) * 3));
            body.setTranslationY(dp((int) ((phase / 5) % 5 - 2) * 2));
            handler.postDelayed(this, 60000 - System.currentTimeMillis() % 60000);
        }
    };
    ScreenView(Context context) { this(context, AtmospherePolicy.Mode.LIVE); }
    ScreenView(Context context, AtmospherePolicy.Mode mode) {
        super(context);
        this.mode = mode;
        options = new Options(context);
        loadTypefaces(context);
        setBackground(new LayerDrawable(new android.graphics.drawable.Drawable[]{photoBackground, atmosphere})); setOrientation(VERTICAL);
        setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        // Keep information inside TV overscan; the photo still fills the entire panel.
        setPadding(dp(56), dp(32), dp(56), dp(40));
        body = new LinearLayout(context); body.setOrientation(VERTICAL);
        addView(body, new LayoutParams(-1, -1));
        LinearLayout header = new LinearLayout(context); header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout dates = new LinearLayout(context); dates.setOrientation(VERTICAL); dates.setGravity(Gravity.START);
        // Display glyphs can extend beyond their advance width; leave room inside each text view.
        profile=LocationStore.read(options.prefs);
        city = label(profile==null?"اختر المدينة من الإعدادات":profile.city+" · "+profile.timezone, 18, GOLD); city.setTypeface(InfoFonts.display(context)); city.setPadding(dp(8), dp(2), dp(8), dp(2)); dates.addView(city);
        hijri = label("", 23, INK); hijri.setTypeface(InfoFonts.display(context)); hijri.setPadding(dp(8), dp(2), dp(8), dp(2)); dates.addView(hijri);
        date = label("", 16, MUTED); date.setTypeface(InfoFonts.display(context)); date.setPadding(dp(8), dp(2), dp(8), dp(2)); dates.addView(date);
        header.addView(dates, new LayoutParams(0, -2, 1));
        clock = label("", 75, INK); clock.setLayoutDirection(View.LAYOUT_DIRECTION_LTR); clock.setGravity(Gravity.CENTER);
        clock.setTypeface(InfoFonts.display(context));
        clock.setFontFeatureSettings("tnum"); header.addView(clock, new LayoutParams(-2, -2));
        body.addView(header, new LayoutParams(-1, -2));
        content = new LinearLayout(context); content.setOrientation(VERTICAL); content.setGravity(Gravity.CENTER);
        content.setPadding(dp(40), dp(14), dp(40), dp(14));
        passage = label("", 34, INK); passage.setGravity(Gravity.CENTER); passage.setLineSpacing(dp(4), 1.1f);
        passage.setIncludeFontPadding(true);
        passage.setAutoSizeTextTypeUniformWithConfiguration(20, 36, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        content.addView(passage, new LayoutParams(-1, 0, 1));
        reference = label("", 17, GOLD); reference.setGravity(Gravity.CENTER); content.addView(reference);
        attribution = label("", 11, MUTED); attribution.setGravity(Gravity.CENTER); attribution.setMaxLines(2); content.addView(attribution);
        body.addView(content, new LayoutParams(-1, 0, 1));
        View rule = new View(context); rule.setBackgroundColor(Color.rgb(35, 53, 54)); body.addView(rule, new LayoutParams(-1, dp(1)));
        prayers = new LinearLayout(context); prayers.setGravity(Gravity.CENTER); prayers.setPadding(0, dp(12), 0, dp(5)); body.addView(prayers, new LayoutParams(-1, -2));
        prayerSource = label("", 11, MUTED); prayerSource.setGravity(Gravity.CENTER); body.addView(prayerSource);
        weather = label("", 14, MUTED); weather.setGravity(Gravity.CENTER); weather.setPadding(dp(4), dp(9), dp(4), dp(4)); body.addView(weather);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private static synchronized void loadTypefaces(Context context) {
        if (arabicTypeface == null) arabicTypeface = context.getResources().getFont(R.font.amiri_regular);
        if (quranTypeface == null) quranTypeface = context.getResources().getFont(R.font.amiri_quran);
    }
    private TextView label(String text, int size, int color) {
        TextView view = new TextView(getContext()); view.setText(text); view.setTextSize(size); view.setTextColor(color); view.setTypeface(InfoFonts.sans(getContext())); view.setGravity(Gravity.START); view.setIncludeFontPadding(true); return view;
    }
    void start() {
        if (visible) return;
        profile=LocationStore.read(options.prefs);
        city.setText(profile==null?"اختر المدينة من الإعدادات":profile.city+" · "+profile.timezone);
        visible = true; generation++; cancellation = new CancellationSignal(); executor = Executors.newSingleThreadExecutor();
        prayerAttempts = new ScreenPolicy.AttemptGate(options.prefs.getLong("prayer_attempt", 0), ScreenPolicy.PRAYER_RETRY_MS);
        weatherAttempts = new ScreenPolicy.AttemptGate(options.prefs.getLong("weather_attempt", 0), ScreenPolicy.WEATHER_INTERVAL_MS);
        dustAttempts = new ScreenPolicy.AttemptGate(options.prefs.getLong("dust_attempt", 0), ScreenPolicy.WEATHER_INTERVAL_MS);
        items = ContentStore.load(getContext(), options.enabled("duas"));
        index = ContentProgress.restore(options.prefs, items, mode.allowsLiveData());
        photos = PhotoStore.load(getContext());
        photoBackground.setEnabled(options.enabled("photos"));
        day = LocalDate.now(selectedZone());
        if (mode.allowsLiveData()) {
            prayerDay = PrayerCache.read(options.prefs, day); weatherData = weatherSource.cached(options.prefs); dustData = weatherSource.cachedDust(options.prefs);
            AdhanScheduler.refresh(getContext().getApplicationContext());
        }
        renderContent(); renderPrayers(); minute.run();
        handler.postDelayed(rotation, options.interval() * 1000L);
    }
    void stop() {
        visible = false; generation++; atmosphere.stop();
        if (cancellation != null) cancellation.cancel();
        handler.removeCallbacksAndMessages(null); weatherSource.cancel();
        if (executor != null) { executor.shutdownNow(); executor = null; }
        prayerBusy = weatherBusy = photoBusy = false;
    }
    void navigate(int delta) {
        if (items == null || items.isEmpty()) return;
        index = ScreenPolicy.rotate(index, delta, items.size()); renderContent();
    }
    private void renderContent() {
        if (items == null || items.isEmpty()) { passage.setText("المحتوى غير متاح حالياً"); reference.setText(""); attribution.setText(""); return; }
        ContentStore.Item item = items.get(index);
        passage.setTypeface(item.kind.equals("quran") ? quranTypeface : arabicTypeface);
        passage.setText(item.text); reference.setText(item.reference);
        ContentProgress.save(options.prefs, items, index, mode.allowsLiveData());
        attribution.setText(item.kind.equals("hadith") ? "من حِكم الإمام علي (عليه السلام)" : item.kind.equals("dua") ? "من الأدعية المنشورة في حقيبة المؤمن" : "القرآن الكريم · tanzil.net / AlQuran.cloud");
    }
    private void updateTime() {
        ZonedDateTime now = ZonedDateTime.now(selectedZone());
        LocalDate today = now.toLocalDate();
        if (!today.equals(day)) { day = today; prayerDay = mode.allowsLiveData() ? PrayerCache.read(options.prefs, today) : null; renderPrayers(); }
        clock.setText(now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US)));
        date.setText(now.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale.forLanguageTag("ar-IQ"))));
        renderHijri();
    }
    private void renderHijri() {
        if (profile!=null && profile.official && prayerDay != null && ScreenPolicy.isPrayerDay(prayerDay.date, day) && prayerDay.hijri != null && !prayerDay.hijri.trim().isEmpty() && options.offset() == 0) {
            hijri.setText(prayerDay.hijri + " · حسب المصدر"); return;
        }
        IslamicCalendar calendar = new IslamicCalendar(android.icu.util.TimeZone.getTimeZone(selectedZone().getId()));
        calendar.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_CIVIL);
        calendar.setTimeInMillis(System.currentTimeMillis()); calendar.add(android.icu.util.Calendar.DATE, options.offset());
        String[] months = {"محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"};
        int offset = options.offset();
        hijri.setText(calendar.get(android.icu.util.Calendar.DATE) + " " + months[calendar.get(android.icu.util.Calendar.MONTH)] + " " + calendar.get(android.icu.util.Calendar.YEAR) + " هـ · حسابي" + (offset == 0 ? "" : " (" + (offset > 0 ? "+" : "") + offset + ")"));
    }
    private void renderPrayers() {
        prayers.removeAllViews();
        boolean enabled = mode.allowsLiveData() && options.enabled("prayers"); prayers.setVisibility(enabled ? VISIBLE : GONE); prayerSource.setVisibility(enabled ? VISIBLE : GONE);
        if (!enabled) return;
        if (prayerDay == null || !ScreenPolicy.isPrayerDay(prayerDay.date, day)) {
            prayers.addView(label("مواقيت اليوم غير متاحة", 20, MUTED));
            prayerSource.setText((profile==null?"اختر المدينة من الإعدادات":profile.source()+" · يلزم تاريخ اليوم من المصدر") + (isRamadan() ? " · الإمساك غير متاح من المصدر" : "")); return;
        }
        java.util.LinkedHashMap<String,String> displayed=new java.util.LinkedHashMap<>(prayerDay.times);
        if(profile!=null)for(int i=0;i<3;i++){String key=AdhanPolicy.KEYS[i],raw=displayed.get(key);if(raw!=null){ZonedDateTime corrected=java.time.Instant.ofEpochMilli(profile.event(day,raw,profile.offsets[i])).atZone(profile.zone());long delta=java.time.temporal.ChronoUnit.DAYS.between(day,corrected.toLocalDate());displayed.put(key,corrected.format(DateTimeFormatter.ofPattern("HH:mm",Locale.US))+(delta==0?"":" ("+(delta>0?"+":"")+delta+" يوم)"));}}
        for (Map.Entry<String, String> time : ScreenPolicy.prayerDisplay(displayed, isRamadan()).entrySet()) {
            LinearLayout cell = new LinearLayout(getContext()); cell.setOrientation(VERTICAL); cell.setGravity(Gravity.CENTER);
            TextView name = label(time.getKey(), 13, MUTED); name.setGravity(Gravity.CENTER); cell.addView(name);
            boolean unavailable = time.getValue() == null || time.getValue().isEmpty();
            TextView value = label(unavailable ? "غير متاح من المصدر" : time.getValue(), unavailable ? 13 : 24, INK); value.setGravity(Gravity.CENTER); value.setLayoutDirection(unavailable ? LAYOUT_DIRECTION_RTL : LAYOUT_DIRECTION_LTR); cell.addView(value);
            prayers.addView(cell, new LayoutParams(0, -2, 1));
        }
        prayerSource.setText(prayerDay.source + " · " + day);
    }
    private boolean isRamadan() {
        if (profile!=null && profile.official && prayerDay != null && ScreenPolicy.isPrayerDay(prayerDay.date, day) && prayerDay.hijri != null && !prayerDay.hijri.trim().isEmpty() && options.offset() == 0) return prayerDay.hijri.contains("رمضان");
        IslamicCalendar calendar = new IslamicCalendar(android.icu.util.TimeZone.getTimeZone(selectedZone().getId()));
        calendar.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_CIVIL);
        calendar.setTimeInMillis(System.currentTimeMillis()); calendar.add(android.icu.util.Calendar.DATE, options.offset());
        return calendar.get(android.icu.util.Calendar.MONTH) == 8;
    }
    private void refreshPhoto() {
        if (!visible || executor == null || photoBusy || !options.enabled("photos") || photos == null || photos.isEmpty()) return;
        long now = SystemClock.elapsedRealtime();
        final String requestedPeriod = AtmospherePolicy.period(solarPhase);
        final boolean phaseChanged = !requestedPeriod.equals(photoPeriod);
        if (!requestedPeriod.equals(photoAttemptPeriod)) photoAttempts = new ScreenPolicy.AttemptGate(0, ScreenPolicy.PHOTO_INTERVAL_MS);
        if (!photoAttempts.ready(now)) return;
        if (!phaseChanged && (!ScreenPolicy.mayRetry(photoShownAt, now, ScreenPolicy.PHOTO_INTERVAL_MS) || !photoAttempts.ready(now))) return;
        final int ticket = generation; final CancellationSignal signal = cancellation;
        int selected = ScreenPolicy.rotate(photoIndex, 0, photos.size());
        for (int n=0; n<photos.size(); n++) { int candidate = ScreenPolicy.rotate(selected,n,photos.size()); if (requestedPeriod.equals(photos.get(candidate).period)) { selected=candidate; break; } }
        final int chosen = selected;
        final PhotoStore.Photo photo = photos.get(selected);
        final ScreenPolicy.AttemptGate photoGate = photoAttempts;
        photoBusy = true;
        executor.execute(() -> {
            if (signal.isCanceled() || Thread.currentThread().isInterrupted()) return;
            if (!photoGate.begin(SystemClock.elapsedRealtime(), false)) return;
            photoAttemptPeriod = requestedPeriod;
            Bitmap bitmap = null;
            try { bitmap = PhotoStore.decode(getContext().getApplicationContext(), photo); } catch (Exception ignored) { }
            final Bitmap loaded = bitmap;
            if (signal.isCanceled() || Thread.currentThread().isInterrupted()) { if (loaded != null) loaded.recycle(); return; }
            handler.post(() -> {
                if (!visible || ticket != generation) { if (loaded != null) loaded.recycle(); return; }
                photoBusy = false;
                if (loaded != null) {
                    photoBackground.setPhoto(loaded); photoShownAt = SystemClock.elapsedRealtime(); photoPeriod = requestedPeriod;
                    photoIndex = ScreenPolicy.rotate(chosen, 1, photos.size());
                }
                if (!requestedPeriod.equals(AtmospherePolicy.period(solarPhase))) refreshPhoto();
            });
        });
    }
    void previewAtmosphere(int weatherDelta, int phaseDelta) {
        if (mode != AtmospherePolicy.Mode.PREVIEW) return;
        previewWeather=ScreenPolicy.rotate(previewWeather,weatherDelta,PREVIEW_CODES.length);
        previewPhase=ScreenPolicy.rotate(previewPhase,phaseDelta,AtmospherePolicy.Phase.values().length);
        updateAtmosphere(); refreshPhoto(); renderWeather();
    }
    private String previewDescription() {
        if(previewWeather==14)return "رياح قوية";
        if(previewWeather==15)return "غبار نمذجي · عرض توضيحي";
        return WeatherSource.description(PREVIEW_CODES[previewWeather],true);
    }
    private void updateAtmosphere() {
        long now=System.currentTimeMillis();
        handler.removeCallbacks(effectsExpiry);
        if(mode==AtmospherePolicy.Mode.PREVIEW) {
            solarPhase=AtmospherePolicy.Phase.values()[previewPhase];
            atmosphere.update(solarPhase,AtmospherePolicy.kind(PREVIEW_CODES[previewWeather]),previewWeather==14?2:0,previewWeather==15,visible,true);return;
        }
        solarPhase=AtmospherePolicy.phase(now,weatherData==null?0:weatherData.sunrise,weatherData==null?0:weatherData.sunset,selectedZone());
        boolean enabled=options.enabled("weather")&&options.enabled("effects");
        AtmospherePolicy.Kind kind=weatherData==null?AtmospherePolicy.Kind.UNKNOWN:AtmospherePolicy.liveKind(weatherData.code,weatherData.observed,now,enabled);
        int wind=weatherData==null||!enabled?0:AtmospherePolicy.wind(AtmospherePolicy.windSpeed(weatherData.wind,weatherData.gust),weatherData.observed,now);
        boolean dust=enabled&&dustData!=null&&AtmospherePolicy.dust(dustData.value,dustData.observed,now);
        atmosphere.update(solarPhase,kind,wind,dust,visible,enabled);
        long expiry=AtmospherePolicy.expiryDelay(now,weatherData==null?0:weatherData.observed,dustData==null?0:dustData.observed,visible&&enabled);
        if(expiry>0)handler.postDelayed(effectsExpiry,expiry);
    }
    private void renderWeather() {
        if (mode == AtmospherePolicy.Mode.PREVIEW) {
            weather.setVisibility(VISIBLE); weather.setText("معاينة فقط · " + previewDescription() + " · " + AtmospherePolicy.phaseName(solarPhase) + "\nيسار/يمين: الطقس · أعلى/أسفل: الوقت · رجوع: خروج"); return;
        }
        weather.setVisibility(options.enabled("weather") ? VISIBLE : GONE);
        if (weatherData == null) { weather.setText((profile==null?"اختر المدينة من الإعدادات":profile.city+" · الطقس غير متاح")+" · Open-Meteo"); return; }
        long now = System.currentTimeMillis(); long age = Math.max(0, (now - weatherData.observed) / 60000);
        boolean fresh = ScreenPolicy.weatherFresh(weatherData.observed, now);
        weather.setText(String.format(Locale.forLanguageTag("ar-IQ"), "%s  ·  %.0f°م  ·  %s  ·  %s  ·  Open-Meteo", profile.city, weatherData.temperature, WeatherSource.description(weatherData.code, weatherData.day) + (Double.isFinite(weatherData.wind) ? String.format(Locale.forLanguageTag("ar-IQ"), " · رياح %.0f كم/س", weatherData.wind) : "") + (Double.isFinite(weatherData.gust) ? String.format(Locale.forLanguageTag("ar-IQ"), " · هبات %.0f كم/س", weatherData.gust) : ""), (fresh ? "منذ " : "بيانات قديمة: منذ ") + age + " دقيقة"));
    }
    private void refreshData() {
        if (!visible || executor == null || profile==null || !LocationStore.same(options.prefs,profile)) return;
        final int ticket = generation; final long now = System.currentTimeMillis(); final LocalDate requested = day;
        final CancellationSignal signal = cancellation;
        final ScreenPolicy.AttemptGate prayerGate = prayerAttempts, weatherGate = weatherAttempts;
        if (options.enabled("prayers") && prayerDay == null && !prayerBusy && prayerGate.ready(now)) {
            prayerBusy = true;
            executor.execute(() -> {
                long started = System.currentTimeMillis();
                if (!prayerGate.begin(started, signal.isCanceled() || Thread.currentThread().isInterrupted())) return;
                if(!LocationStore.attempt(options.prefs,profile,"prayer_attempt",started))return;
                PrayerSource.PrayerDay result = null;
                try {
                    result = PrayerCache.valid(PrayerSource.load(getContext().getApplicationContext(), requested, signal), requested);
                    if (LocationStore.same(options.prefs,profile) && result != null && !signal.isCanceled() && !Thread.currentThread().isInterrupted()) {
                        PrayerCache.write(options.prefs, result);
                        AdhanScheduler.schedule(getContext().getApplicationContext(), result);
                    }
                } catch (Exception ignored) { }
                if (signal.isCanceled() || Thread.currentThread().isInterrupted()) return;
                final PrayerSource.PrayerDay loaded = result;
                handler.post(() -> { if (!visible || ticket != generation || !LocationStore.same(options.prefs,profile)) return; prayerBusy = false; if (requested.equals(day)) prayerDay = loaded; renderPrayers(); renderHijri(); });
            });
        }
        final ScreenPolicy.AttemptGate dustGate = dustAttempts;
        if (options.enabled("weather") && !weatherBusy && (weatherGate.ready(now) || options.enabled("effects") && dustGate.ready(now))) {
            weatherBusy = true;
            executor.execute(() -> {
                long started = System.currentTimeMillis();
                if (signal.isCanceled() || Thread.currentThread().isInterrupted()) return;
                WeatherSource.Weather result = null;
                if (weatherGate.begin(started, false)) {
                    if(!LocationStore.attempt(options.prefs,profile,"weather_attempt",started))return;
                    try { result = weatherSource.fetch(options.prefs, signal); } catch (Exception ignored) { }
                }
                WeatherSource.Dust dust = null;
                if (options.enabled("effects") && dustGate.begin(System.currentTimeMillis(), signal.isCanceled() || Thread.currentThread().isInterrupted())) {
                    if(!LocationStore.attempt(options.prefs,profile,"dust_attempt",dustGate.lastAttempt()))return;
                    try { dust = weatherSource.fetchDust(options.prefs, signal); } catch (Exception ignored) { }
                }
                if (signal.isCanceled() || Thread.currentThread().isInterrupted()) return;
                final WeatherSource.Weather loaded = result;
                final WeatherSource.Dust loadedDust = dust;
                handler.post(() -> { if (!visible || ticket != generation || !LocationStore.same(options.prefs,profile)) return; weatherBusy = false; if (loaded != null) weatherData = loaded; if (loadedDust != null) dustData = loadedDust; updateAtmosphere(); refreshPhoto(); renderWeather(); });
            });
        }
    }
}
