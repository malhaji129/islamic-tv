package org.local.karbalascreen;

import android.content.SharedPreferences;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.LinkedHashMap;

final class PrayerCache {
    static PrayerSource.PrayerDay read(SharedPreferences prefs, LocalDate date) {
        try {
            JSONObject json = new JSONObject(prefs.getString("prayer_cache_"+date, prefs.getString("prayer_cache", "{}")));
            LinkedHashMap<String, String> times = new LinkedHashMap<>();
            JSONObject stored = json.getJSONObject("times");
            Iterator<String> names = stored.keys();
            while (names.hasNext()) { String name = names.next(); times.put(name, stored.getString(name)); }
            LocationProfile p=LocationStore.read(prefs);
            if(p==null||!p.identity().equals(json.optString("profile")))return null;
            return valid(new PrayerSource.PrayerDay(json.getString("date"), json.optString("hijri"), json.getString("source"), times,p.identity()), date);
        } catch (Exception ignored) { return null; }
    }
    static PrayerSource.PrayerDay valid(PrayerSource.PrayerDay day, LocalDate date) {
        if (day == null || !ScreenPolicy.isPrayerDay(day.date, date) || day.source == null || day.source.trim().isEmpty() || day.times == null || day.times.isEmpty()) return null;
        for (java.util.Map.Entry<String, String> time : day.times.entrySet()) {
            if (time.getKey() == null || time.getKey().trim().isEmpty() || time.getValue() == null || !time.getValue().matches("(?:[01]\\d|2[0-3]):[0-5]\\d")) return null;
        }
        return day;
    }
    static synchronized void write(SharedPreferences prefs, PrayerSource.PrayerDay day) throws Exception {
        synchronized(LocationStore.class){
        LocationProfile p=LocationStore.read(prefs);if(p==null||!p.identity().equals(day.profile))return;
        JSONObject data = new JSONObject().put("profile",day.profile).put("date", day.date).put("hijri", day.hijri == null ? "" : day.hijri).put("source", day.source).put("times", new JSONObject(day.times));
        SharedPreferences.Editor edit=prefs.edit();for(String key:prefs.getAll().keySet())if(key.startsWith("prayer_cache_")){try{if(LocalDate.parse(key.substring(13)).isBefore(LocalDate.parse(day.date).minusDays(2)))edit.remove(key);}catch(Exception ignored){edit.remove(key);}}
        edit.putString("prayer_cache",data.toString()).putString("prayer_cache_"+day.date,data.toString()).apply();
        }
    }
}
