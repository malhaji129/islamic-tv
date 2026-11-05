package org.local.karbalascreen;

import android.content.Context;
import android.content.SharedPreferences;

final class Options {
    final SharedPreferences prefs;
    Options(Context context) { prefs = context.getSharedPreferences("screen", Context.MODE_PRIVATE); }
    int interval() { return ScreenPolicy.intervalSeconds(prefs.getInt("interval", 45)); }
    int offset() { return ScreenPolicy.dateOffset(prefs.getInt("offset", 0)); }
    boolean enabled(String feature) { return prefs.getBoolean(feature, true); }
}
