package org.local.karbalascreen;
import java.time.*;
public final class AtmospherePolicyTest {
 static int checks;
 static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 static long time(int h,int m){return LocalDate.of(2026,11,2).atTime(h,m).atZone(ZoneId.of("Asia/Baghdad")).toInstant().toEpochMilli();}
 public static void main(String[]args){
  int[][] groups={{0},{1,2},{3},{45,48},{51,53,55},{56,57},{61,63,65},{66,67},{71,73,75},{77},{80,81,82},{85,86},{95},{96,99}};
  AtmospherePolicy.Kind[] kinds={AtmospherePolicy.Kind.CLEAR,AtmospherePolicy.Kind.PARTLY_CLOUDY,AtmospherePolicy.Kind.OVERCAST,AtmospherePolicy.Kind.FOG,AtmospherePolicy.Kind.DRIZZLE,AtmospherePolicy.Kind.FREEZING_DRIZZLE,AtmospherePolicy.Kind.RAIN,AtmospherePolicy.Kind.FREEZING_RAIN,AtmospherePolicy.Kind.SNOW,AtmospherePolicy.Kind.SNOW_GRAINS,AtmospherePolicy.Kind.RAIN_SHOWERS,AtmospherePolicy.Kind.SNOW_SHOWERS,AtmospherePolicy.Kind.THUNDER,AtmospherePolicy.Kind.HAIL_THUNDER};
  for(int i=0;i<groups.length;i++)for(int code:groups[i])check(AtmospherePolicy.kind(code)==kinds[i],"WMO code "+code);
  for(int code:new int[]{-1,4,49,58,68,78,83,94,97,100})check(AtmospherePolicy.kind(code)==AtmospherePolicy.Kind.UNKNOWN,"unknown must have no inferred storm "+code);
  long now=time(12,0);
  check(AtmospherePolicy.wind(24.9,now,now)==0,"calm below threshold");
  check(AtmospherePolicy.wind(25,now,now)==1,"breeze threshold");
  check(AtmospherePolicy.wind(45,now,now)==2,"strong wind threshold");
  check(AtmospherePolicy.wind(Double.NaN,now,now)==0,"unknown wind no streaks");
  check(AtmospherePolicy.wind(90,now-1800000,now)==0,"stale wind off");
  check(!AtmospherePolicy.dust(Double.NaN,now,now),"unknown dust never inferred from wind");
  check(!AtmospherePolicy.dust(49.9,now,now),"dust aesthetic threshold");
  check(AtmospherePolicy.dust(50,now,now),"fresh modelled dust at threshold");
  check(!AtmospherePolicy.dust(90,now-1800000,now),"dust freshness independent");
  check(AtmospherePolicy.liveKind(95,now-1799999,now,true)==AtmospherePolicy.Kind.THUNDER,"fresh storm");
  check(AtmospherePolicy.liveKind(95,now-1800000,now,true)==AtmospherePolicy.Kind.UNKNOWN,"stale weather disables effects");
  check(AtmospherePolicy.liveKind(95,now,now,false)==AtmospherePolicy.Kind.UNKNOWN,"weather disabled means no weather effects");
  long rise=time(6,30),set=time(17,30);
  check(AtmospherePolicy.phase(time(0,0),rise,set)==AtmospherePolicy.Phase.NIGHT,"midnight night");
  check(AtmospherePolicy.phase(time(4,0),rise,set)==AtmospherePolicy.Phase.SAHAR,"sahar starts 150min before sunrise");
  check(AtmospherePolicy.phase(time(5,30),rise,set)==AtmospherePolicy.Phase.DAWN,"dawn boundary");
  check(AtmospherePolicy.phase(rise,rise,set)==AtmospherePolicy.Phase.SUNRISE,"sunrise boundary");
  check(AtmospherePolicy.phase(time(7,15),rise,set)==AtmospherePolicy.Phase.DAY,"day after sunrise band");
  check(AtmospherePolicy.phase(time(16,45),rise,set)==AtmospherePolicy.Phase.SUNSET,"sunset band begins");
  check(AtmospherePolicy.phase(time(18,15),rise,set)==AtmospherePolicy.Phase.NIGHT,"night boundary");
  check(AtmospherePolicy.phase(time(6,10),rise-86400000,set-86400000)==AtmospherePolicy.Phase.SUNRISE,"yesterday solar data falls back to local6am");
  check(AtmospherePolicy.phase(time(6,10),set,rise)==AtmospherePolicy.Phase.SUNRISE,"reversed solar data falls back");
  check(AtmospherePolicy.period(AtmospherePolicy.Phase.SAHAR).equals("night"),"sahar uses real night photo");
  check(AtmospherePolicy.period(AtmospherePolicy.Phase.DAWN).equals("twilight"),"dawn twilight photo");
  check(AtmospherePolicy.period(AtmospherePolicy.Phase.DAY).equals("day"),"day photo");
  check(!AtmospherePolicy.Mode.PREVIEW.allowsLiveData(),"preview cannot fetch cache or schedule live data");
  check(AtmospherePolicy.Mode.LIVE.allowsLiveData(),"live screen may load real data");
  check(!AtmospherePolicy.animate(false,true,AtmospherePolicy.Kind.RAIN,1),"hidden timer off");
  check(!AtmospherePolicy.animate(true,false,AtmospherePolicy.Kind.RAIN,1),"effects off timer off");
  check(!AtmospherePolicy.animate(true,true,AtmospherePolicy.Kind.CLEAR,0),"static sun has no timer");
  check(AtmospherePolicy.animate(true,true,AtmospherePolicy.Kind.SNOW,0),"visible snow timer");
  check(AtmospherePolicy.FRAME_MS>=125,"animation at most8fps");
  check(AtmospherePolicy.THUNDER_PERIOD_MS>=15000 && AtmospherePolicy.THUNDER_MAX_ALPHA<=20,"no strobe thunder bounds");
  check(AtmospherePolicy.expiryDelay(now,now-1799999,0,true)==1,"effects expire at observation boundary, without waiting minute tick");
  check(AtmospherePolicy.expiryDelay(now,now-1800000,now-1000,true)==1799000,"dust expires independently after weather becomes stale");
  check(AtmospherePolicy.expiryDelay(now,now,now,false)==0,"hidden/disabled screen has no freshness timer");
  check(AtmospherePolicy.expiryDelay(now,0,0,true)==0,"unknown observations have no expiry timer");
  check(AtmospherePolicy.windSpeed(12,48)==48,"fresh gust can drive stronger wind without storm inference");
  check(AtmospherePolicy.windSpeed(Double.NaN,28)==28,"gust available without sustained speed");
  check(Double.isNaN(AtmospherePolicy.windSpeed(Double.NaN,Double.NaN)),"both unknown remain unknown");
  System.out.println("PASS "+checks+" atmosphere checks");
 }
}
