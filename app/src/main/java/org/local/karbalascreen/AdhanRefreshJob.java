package org.local.karbalascreen;

import android.app.job.*;
import android.os.CancellationSignal;
import java.time.LocalDate;

/** System-managed network retry, cancelled as soon as the system stops this job. */
public final class AdhanRefreshJob extends JobService {
    private CancellationSignal cancellation;
    private Thread worker;
    @Override public boolean onStartJob(JobParameters parameters) {
        final LocationProfile profile=LocationStore.read(new Options(this).prefs);
        if (profile==null || !new Options(this).enabled("adhan") || !AdhanScheduler.available(this)) return false;
        CancellationSignal signal = new CancellationSignal(); cancellation = signal;
        worker = new Thread(() -> {
            boolean retry = true;
            try {
                LocalDate today = LocalDate.now(profile.zone());
                for(int delta=0;delta<=(profile.official?0:1);delta++){
                    LocalDate requested=today.plusDays(delta);
                    PrayerSource.PrayerDay day=PrayerCache.read(new Options(this).prefs,requested);
                    if(day==null)day=PrayerCache.valid(PrayerSource.load(this,requested,signal),requested);
                    if(day==null||signal.isCanceled()||!LocationStore.same(new Options(this).prefs,profile)||!new Options(this).enabled("adhan"))throw new IllegalStateException("Refresh no longer applicable");
                    PrayerCache.write(new Options(this).prefs,day);
                    AdhanScheduler.schedule(this,PrayerCache.read(new Options(this).prefs,LocalDate.now(profile.zone())));
                }
                if(LocationStore.same(new Options(this).prefs,profile)&&!signal.isCanceled()){
                    AdhanScheduler.schedule(this,PrayerCache.read(new Options(this).prefs,LocalDate.now(profile.zone())));retry=false;
                }

            } catch (Exception unavailable) { /* JobScheduler retries with at least 15 minute backoff. */ }
            if (!signal.isCanceled()) jobFinished(parameters, retry);
        }, "adhan-day-refresh");
        worker.start(); return true;
    }
    @Override public boolean onStopJob(JobParameters parameters) { stopWorker(); return new Options(this).enabled("adhan"); }
    @Override public void onDestroy() { stopWorker(); super.onDestroy(); }
    private void stopWorker() { if (cancellation != null) cancellation.cancel(); if (worker != null) worker.interrupt(); }
}
