package org.local.karbalascreen;
import android.content.*;
import org.json.*;
/** All location changes and cache writes share this lock; a late worker cannot overwrite a new profile. */
final class LocationStore {
 static synchronized LocationProfile read(SharedPreferences prefs){
  String raw=prefs.getString("location_profile",null);
  if(raw==null){
   // Only installations containing earlier app preferences retain the former fixed city.
   if(!prefs.getAll().isEmpty()&&!prefs.contains("location_unconfigured")){LocationProfile old=LocationProfile.karbala();prefs.edit().putString("location_profile",json(old).toString()).commit();return old;}
   prefs.edit().putBoolean("location_unconfigured",true).apply();return null;
  }
  try{JSONObject j=new JSONObject(raw);return new LocationProfile(j.getString("city"),j.getString("country"),j.getDouble("lat"),j.getDouble("lon"),j.getString("zone"),j.getBoolean("official"),j.getInt("method"),j.getInt("fajr"),j.getInt("dhuhr"),j.getInt("isha"));}catch(Exception invalid){return null;}
 }
 private static JSONObject json(LocationProfile p){try{return new JSONObject().put("city",p.city).put("country",p.country).put("lat",p.latitude).put("lon",p.longitude).put("zone",p.timezone).put("official",p.official).put("method",p.method).put("fajr",p.offsets[0]).put("dhuhr",p.offsets[1]).put("isha",p.offsets[2]);}catch(Exception impossible){throw new IllegalArgumentException(impossible);}}
 static boolean same(SharedPreferences prefs,LocationProfile p){LocationProfile current=read(prefs);return p!=null&&current!=null&&current.identity().equals(p.identity());}
 static synchronized boolean attempt(SharedPreferences prefs,LocationProfile p,String key,long at){if(!same(prefs,p))return false;prefs.edit().putLong(key,at).apply();return true;}
 static synchronized void apply(Context c,LocationProfile p){
  AdhanScheduler.cancel(c);SharedPreferences prefs=new Options(c).prefs;
  SharedPreferences.Editor e=prefs.edit();for(String key:prefs.getAll().keySet())if(key.startsWith("prayer_")||key.startsWith("weather_")||key.startsWith("dust_")||key.startsWith("adhan_played_"))e.remove(key);
  e.putString("location_profile",json(p).toString()).remove("location_unconfigured").commit();AdhanScheduler.refresh(c);
 }
}
