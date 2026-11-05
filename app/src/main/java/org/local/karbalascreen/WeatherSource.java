package org.local.karbalascreen;

import android.content.SharedPreferences;
import android.os.CancellationSignal;
import org.json.JSONObject;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;

final class WeatherSource {
    private volatile HttpsURLConnection connection;
    static final class Weather {
        final double temperature, wind, gust;
        final int code;
        final boolean day;
        final long observed, sunrise, sunset;
        Weather(double temperature,int code,boolean day,long observed,double wind,double gust,long sunrise,long sunset){
            this.temperature=temperature;this.code=code;this.day=day;this.observed=observed;this.wind=wind;this.gust=gust;this.sunrise=sunrise;this.sunset=sunset;
        }
    }
    static final class Dust {
        final double value; final long observed;
        Dust(double value,long observed){this.value=value;this.observed=observed;}
    }
    Weather cached(SharedPreferences prefs){
        try {
            LocationProfile p=LocationStore.read(prefs);if(p==null||!p.identity().equals(prefs.getString("weather_profile","")))return null;
            JSONObject json=new JSONObject(prefs.getString("weather_v2","{}"));
            return parseWeather(json,p.zone());
        } catch(Exception ignored){
            return null;
        }
    }
    Dust cachedDust(SharedPreferences prefs){
        try{LocationProfile p=LocationStore.read(prefs);if(p==null||!p.identity().equals(prefs.getString("dust_profile","")))return null;return parseDust(new JSONObject(prefs.getString("dust_v1","{}")),p.zone());}catch(Exception ignored){return null;}
    }
    Weather fetch(SharedPreferences prefs,CancellationSignal signal)throws Exception{
        LocationProfile p=LocationStore.read(prefs);if(p==null)throw new IllegalStateException("Choose city first");
        JSONObject response=request("https://api.open-meteo.com/v1/forecast?latitude="+p.latitude+"&longitude="+p.longitude+"&current=temperature_2m,weather_code,is_day,wind_speed_10m,wind_gusts_10m&wind_speed_unit=kmh&daily=sunrise,sunset&forecast_days=1&timeformat=unixtime&timezone="+JsonHttp.encode(p.timezone)+"",signal);
        Weather result=parseWeather(response,p.zone());
        signal.throwIfCanceled();if(Thread.currentThread().isInterrupted())throw new InterruptedException();
        synchronized(LocationStore.class){if(!LocationStore.same(prefs,p))throw new java.io.IOException("Location changed");prefs.edit().putString("weather_profile",p.identity()).putString("weather_v2",response.toString()).apply();}return result;
    }
    Dust fetchDust(SharedPreferences prefs,CancellationSignal signal)throws Exception{
        LocationProfile p=LocationStore.read(prefs);if(p==null)throw new IllegalStateException("Choose city first");
        JSONObject response=request("https://air-quality-api.open-meteo.com/v1/air-quality?latitude="+p.latitude+"&longitude="+p.longitude+"&current=dust&forecast_days=1&timeformat=unixtime&timezone="+JsonHttp.encode(p.timezone)+"",signal);
        Dust result=parseDust(response,p.zone());
        signal.throwIfCanceled();if(Thread.currentThread().isInterrupted())throw new InterruptedException();
        synchronized(LocationStore.class){if(!LocationStore.same(prefs,p))throw new java.io.IOException("Location changed");prefs.edit().putString("dust_profile",p.identity()).putString("dust_v1",response.toString()).apply();}return result;
    }
    private static long time(String text,ZoneId zone){try{if(text.matches("[0-9]{9,12}"))return Long.parseLong(text)*1000L;return LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli();}catch(Exception ignored){return 0;}}
    private static double optional(JSONObject json,String key,double max){double value=json.optDouble(key,Double.NaN);return Double.isFinite(value)&&value>=0&&value<=max?value:Double.NaN;}
    private static Weather parseWeather(JSONObject response,ZoneId zone)throws Exception{
        JSONObject current=response.getJSONObject("current");
        double temperature=current.getDouble("temperature_2m");int code=current.getInt("weather_code");long observed=time(current.getString("time"),zone);
        if(!Double.isFinite(temperature)||temperature< -50||temperature>65||observed<=0||observed>System.currentTimeMillis()+60000)throw new java.io.IOException("Invalid weather");
        long sunrise=0,sunset=0;
        try{JSONObject daily=response.getJSONObject("daily");sunrise=time(daily.getJSONArray("sunrise").getString(0),zone);sunset=time(daily.getJSONArray("sunset").getString(0),zone);}catch(Exception ignored){}
        return new Weather(temperature,code,current.optInt("is_day",-1)==1,observed,optional(current,"wind_speed_10m",400),optional(current,"wind_gusts_10m",400),sunrise,sunset);
    }
    private static Dust parseDust(JSONObject response,ZoneId zone)throws Exception{
        JSONObject current=response.getJSONObject("current");double value=optional(current,"dust",100000);long observed=time(current.getString("time"),zone);
        if(!Double.isFinite(value)||observed<=0||observed>System.currentTimeMillis()+60000)throw new java.io.IOException("Dust unavailable");return new Dust(value,observed);
    }
    private JSONObject request(String url,CancellationSignal signal)throws Exception{
        signal.throwIfCanceled();
        HttpsURLConnection active=(HttpsURLConnection)new URL(url).openConnection();connection=active;
        ScheduledExecutorService deadline=Executors.newSingleThreadScheduledExecutor();
        active.setConnectTimeout(4500);active.setReadTimeout(4500);active.setRequestProperty("Accept","application/json");
        signal.setOnCancelListener(active::disconnect);deadline.schedule(active::disconnect,8,TimeUnit.SECONDS);
        try{
            signal.throwIfCanceled();if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            if(active.getResponseCode()!=200)throw new java.io.IOException("Weather service unavailable");
            try(java.io.InputStream input=active.getInputStream()){return new JSONObject(StreamText.read(input));}
        }finally{signal.setOnCancelListener(null);active.disconnect();if(connection==active)connection=null;deadline.shutdownNow();}
    }
    void cancel(){HttpsURLConnection active=connection;if(active!=null)active.disconnect();}
    static String description(int code,boolean day){return AtmospherePolicy.description(AtmospherePolicy.kind(code),day);}
}
