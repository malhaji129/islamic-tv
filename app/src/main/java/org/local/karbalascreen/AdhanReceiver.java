package org.local.karbalascreen;

import android.content.*;

public final class AdhanReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (AdhanScheduler.FIRE.equals(intent.getAction())) {
            if (!new Options(context).enabled("adhan") || !AdhanScheduler.available(context)) return;
            try { context.startForegroundService(new Intent(context, AdhanPlaybackService.class).setAction(AdhanScheduler.FIRE).putExtras(intent)); }
            catch (RuntimeException restricted) { /* A missed event must never be replayed. */ }
        } else AdhanScheduler.refresh(context);
    }
}
