package org.local.karbalascreen;
import android.content.SharedPreferences;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.time.LocalDate;
public class LocationPersistenceTest {
 /** In-memory implementation exercises real LocationStore/PrayerCache without a device. */
 static final class Prefs implements SharedPreferences {
  final Map<String,Object> data=new HashMap<>();
  public Map<String,?> getAll(){return new HashMap<>(data);} public String getString(String k,String d){return data.containsKey(k)?(String)data.get(k):d;}
  public Set<String> getStringSet(String k,Set<String>d){return d;} public int getInt(String k,int d){return data.containsKey(k)?(Integer)data.get(k):d;}
  public long getLong(String k,long d){return data.containsKey(k)?(Long)data.get(k):d;} public float getFloat(String k,float d){return d;}
  public boolean getBoolean(String k,boolean d){return data.containsKey(k)?(Boolean)data.get(k):d;} public boolean contains(String k){return data.containsKey(k);}
  public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
  public Editor edit(){return new Editor(){final Map<String,Object> pending=new HashMap<>();boolean clear;
   public Editor putString(String k,String v){pending.put(k,v);return this;}public Editor putStringSet(String k,Set<String>v){pending.put(k,v);return this;}
   public Editor putInt(String k,int v){pending.put(k,v);return this;}public Editor putLong(String k,long v){pending.put(k,v);return this;}public Editor putFloat(String k,float v){pending.put(k,v);return this;}public Editor putBoolean(String k,boolean v){pending.put(k,v);return this;}public Editor remove(String k){pending.put(k,null);return this;}public Editor clear(){clear=true;return this;}
   public boolean commit(){if(clear)data.clear();for(Map.Entry<String,Object>e:pending.entrySet())if(e.getValue()==null)data.remove(e.getKey());else data.put(e.getKey(),e.getValue());return true;}public void apply(){commit();}
  };}
 }
 private void set(Prefs prefs,LocationProfile p)throws Exception{prefs.edit().putString("location_profile",new JSONObject().put("city",p.city).put("country",p.country).put("lat",p.latitude).put("lon",p.longitude).put("zone",p.timezone).put("official",p.official).put("method",p.method).put("fajr",p.offsets[0]).put("dhuhr",p.offsets[1]).put("isha",p.offsets[2]).toString()).commit();}
 @Test public void freshInstallStaysUnconfiguredAndLegacyKeepsKarbala(){Prefs fresh=new Prefs();assertNull(LocationStore.read(fresh));fresh.edit().putInt("interval",60).commit();assertNull(LocationStore.read(fresh));Prefs legacy=new Prefs();legacy.edit().putInt("interval",45).commit();assertEquals(LocationProfile.karbala().identity(),LocationStore.read(legacy).identity());}
 @Test public void lateProfileCannotWriteCacheOrThrottleNewLocation()throws Exception{
  Prefs prefs=new Prefs();LocationProfile old=LocationProfile.karbala();set(prefs,old);
  LinkedHashMap<String,String> times=new LinkedHashMap<>();for(String key:AdhanPolicy.KEYS)times.put(key,"05:00");
  PrayerSource.PrayerDay day=new PrayerSource.PrayerDay("2026-11-05","","official",times,old.identity());PrayerCache.write(prefs,day);assertNotNull(PrayerCache.read(prefs,LocalDate.of(2026,11,5)));
  LocationProfile selected=new LocationProfile("London","UK",51.5,-.1,"Europe/London",false,0,0,0,0);set(prefs,selected);
  assertNull(PrayerCache.read(prefs,LocalDate.of(2026,11,5)));assertFalse(LocationStore.attempt(prefs,old,"prayer_attempt",123));assertFalse(prefs.contains("prayer_attempt"));
  String original=prefs.getString("prayer_cache","");PrayerCache.write(prefs,day);assertEquals(original,prefs.getString("prayer_cache",""));
  assertTrue(LocationStore.attempt(prefs,selected,"prayer_attempt",456));assertEquals(456,prefs.getLong("prayer_attempt",0));
 }
}
