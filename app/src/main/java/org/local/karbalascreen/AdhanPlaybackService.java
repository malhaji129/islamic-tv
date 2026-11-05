package org.local.karbalascreen;

import android.app.*;
import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.media.session.*;
import android.os.*;
import java.time.LocalDate;

/** Owns audio resources only for the duration of one local adhan or explicit preview. */
public final class AdhanPlaybackService extends Service {
    static final String PREVIEW = "preview", STOP = "stop";
    private static final String CHANNEL = "adhan";
    private MediaPlayer player;
    private MediaSession session;
    private AudioManager audio;
    private AudioFocusRequest focus;
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || STOP.equals(intent.getAction())) { finish(); return START_NOT_STICKY; }
        boolean preview = PREVIEW.equals(intent.getAction());
        if (player != null && !preview) return START_NOT_STICKY;
        Options options = new Options(this);
        if (!AdhanScheduler.available(this) || (!preview && !options.enabled("adhan"))) { finish(); return START_NOT_STICKY; }
        PowerManager power = getSystemService(PowerManager.class);
        if (!power.isInteractive()) { finish(); return START_NOT_STICKY; }
        if (!preview) {
            LocationProfile profile=LocationStore.read(options.prefs);
            if(profile==null||!profile.identity().equals(intent.getStringExtra("profile"))){finish();return START_NOT_STICKY;}
            LocalDate alarmDate;try{alarmDate=LocalDate.parse(intent.getStringExtra("date"));}catch(Exception invalid){finish();return START_NOT_STICKY;}
            PrayerSource.PrayerDay day = PrayerCache.read(options.prefs, alarmDate);
            AdhanPolicy.Event event = new AdhanPolicy.Event(intent.getStringExtra("date"), intent.getStringExtra("key"), intent.getLongExtra("at", 0));
            boolean played = event.date != null && event.date.equals(options.prefs.getString("adhan_played_" + event.key, ""));
            if (day == null || !AdhanPolicy.shouldPlay(event, day.date, day.times, System.currentTimeMillis(), played,profile)) { finish(); return START_NOT_STICKY; }
            // Persist before any audible side effect, including across process death.
            if (!options.prefs.edit().putString("adhan_played_" + event.key, event.date).commit()) { finish(); return START_NOT_STICKY; }
        }
        release();
        try {
            audio = getSystemService(AudioManager.class);
            AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
            focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener(change -> { if (change < 0) finish(); }).build();
            if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { finish(); return START_NOT_STICKY; }
            player = new MediaPlayer(); player.setAudioAttributes(attributes);
            int resource = getResources().getIdentifier("adhan_karbala", "raw", getPackageName());
            try (AssetFileDescriptor file = getResources().openRawResourceFd(resource)) { player.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength()); }
            player.setLooping(false);
            player.setOnCompletionListener(done -> finish());
            player.setOnErrorListener((failed, what, extra) -> { finish(); return true; });
            player.prepare();
            session = new MediaSession(this, "KarbalaAdhan");
            session.setCallback(new MediaSession.Callback() { @Override public void onStop() { finish(); } @Override public void onPause() { finish(); } });
            session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_STOP | PlaybackState.ACTION_PAUSE)
                    .setState(PlaybackState.STATE_PLAYING, 0, 1f).build());
            session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, "أذان كربلاء · مصطفى الصراف").build());
            session.setActive(true);
            NotificationManager notifications = getSystemService(NotificationManager.class);
            notifications.createNotificationChannel(new NotificationChannel(CHANNEL, "أذان كربلاء", NotificationManager.IMPORTANCE_LOW));
            PendingIntent stop = PendingIntent.getService(this, 4200, new Intent(this, AdhanPlaybackService.class).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification notification = new Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(preview ? "معاينة أذان كربلاء" : "أذان كربلاء")
                    .setContentText("مصطفى الصراف · إيقاف الصوت من هنا أو من الإعدادات")
                    .setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_TRANSPORT)
                    .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_pause, "إيقاف", stop).build())
                    .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0)).build();
            startForeground(4201, notification);
            player.start();
        } catch (Exception unavailable) { finish(); }
        return START_NOT_STICKY;
    }
    private void finish() { release(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); }
    private void release() {
        if (player != null) { player.setOnCompletionListener(null); player.setOnErrorListener(null); player.release(); player = null; }
        if (session != null) { session.setActive(false); session.release(); session = null; }
        if (audio != null && focus != null) audio.abandonAudioFocusRequest(focus);
        focus = null; audio = null;
    }
    @Override public void onDestroy() { release(); super.onDestroy(); }
}
