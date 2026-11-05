package org.local.karbalascreen;

import android.app.*;
import android.content.*;
import android.os.Build;
import java.time.*;

/** Three one-shot source alarms and one daily refresh; never a resident timer. */
final class AdhanScheduler {
    static final String FIRE = "org.local.karbalascreen.ADHAN", REFRESH = "org.local.karbalascreen.REFRESH";
    static final int JOB = 4104, DAILY = 4103;
    static boolean available(Context context) { return context.getResources().getIdentifier("adhan_karbala", "raw", context.getPackageName()) != 0; }
    static void refresh(Context context) {
        Options options = new Options(context);
        LocationProfile profile=LocationStore.read(options.prefs);
        if (profile==null || !options.enabled("adhan") || !available(context)) { cancel(context); return; }
        PrayerSource.PrayerDay day = PrayerCache.read(options.prefs, LocalDate.now(profile.zone()));
        schedule(context, day);
        android.app.job.JobScheduler jobs = context.getSystemService(android.app.job.JobScheduler.class);
        if (day == null || !profile.official && PrayerCache.read(options.prefs,LocalDate.now(profile.zone()).plusDays(1))==null) {
            if (jobs.getPendingJob(JOB) == null) jobs.schedule(new android.app.job.JobInfo.Builder(JOB, new ComponentName(context, AdhanRefreshJob.class))
                    .setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(15 * 60 * 1000L, android.app.job.JobInfo.BACKOFF_POLICY_EXPONENTIAL).build());
        } else jobs.cancel(JOB);
    }
    static void schedule(Context context, PrayerSource.PrayerDay published) {
      synchronized(LocationStore.class){
        Options options = new Options(context);
        LocationProfile profile=LocationStore.read(options.prefs);
        if (profile==null || !options.enabled("adhan") || !available(context)) { cancel(context); return; }
        if(published!=null&&!profile.identity().equals(published.profile))return;
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        cancelPrayerAlarms(context);
        long now = System.currentTimeMillis();
        PrayerSource.PrayerDay day = PrayerCache.valid(published, Instant.ofEpochMilli(now).atZone(profile.zone()).toLocalDate());
        if(day!=null&&!profile.identity().equals(day.profile))return;
        LocalDate today=Instant.ofEpochMilli(now).atZone(profile.zone()).toLocalDate();
        StringBuilder dates=new StringBuilder();
        for(int delta=-1;delta<=1;delta++){
          PrayerSource.PrayerDay candidate=delta==0?day:PrayerCache.read(options.prefs,today.plusDays(delta));
          if(candidate==null)continue;
          dates.append(candidate.date).append(",");
          for(AdhanPolicy.Event event:AdhanPolicy.future(candidate.date,candidate.times,now,profile))for(int i=0;i<3;i++)if(AdhanPolicy.KEYS[i].equals(event.key)){
            PendingIntent pending=prayerIntent(context,i,event,event.date);
            if(Build.VERSION.SDK_INT<31||alarms.canScheduleExactAlarms())alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,event.at,pending);
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,event.at,pending);
          }
        }
        options.prefs.edit().putString("adhan_alarm_dates",dates.toString()).apply();
        ZonedDateTime next = Instant.ofEpochMilli(now).atZone(profile.zone()).withHour(profile.official?0:22).withMinute(profile.official?10:0).withSecond(0).withNano(0);
        if (next.toInstant().toEpochMilli() <= now) next = next.plusDays(1);
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), dailyIntent(context));
    }
      }
    static void cancel(Context context) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        cancelPrayerAlarms(context);
        alarms.cancel(dailyIntent(context));
        context.getSystemService(android.app.job.JobScheduler.class).cancel(JOB);
        context.stopService(new Intent(context, AdhanPlaybackService.class));
    }
    private static void cancelPrayerAlarms(Context c){
      AlarmManager alarms=c.getSystemService(AlarmManager.class);Options options=new Options(c);
      for(int i=0;i<3;i++){
        alarms.cancel(prayerIntent(c,i,null,null));
        for(String date:options.prefs.getString("adhan_alarm_dates","").split(","))if(!date.isEmpty())alarms.cancel(prayerIntent(c,i,null,date));
      }
      options.prefs.edit().remove("adhan_alarm_dates").apply();
    }
    private static PendingIntent dailyIntent(Context c) {
        return PendingIntent.getBroadcast(c, DAILY, new Intent(c, AdhanReceiver.class).setAction(REFRESH), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private static PendingIntent prayerIntent(Context c, int index, AdhanPolicy.Event event,String date) {
        Intent intent = new Intent(c, AdhanReceiver.class).setAction(FIRE).setData(android.net.Uri.parse("karbala:adhan/" + index+(date==null?"":"/"+date)));
        if (event != null) intent.putExtra("profile",LocationStore.read(new Options(c).prefs).identity()).putExtra("date", event.date).putExtra("key", event.key).putExtra("at", event.at);
        return PendingIntent.getBroadcast(c, 4100 + index, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private AdhanScheduler() { }
}
