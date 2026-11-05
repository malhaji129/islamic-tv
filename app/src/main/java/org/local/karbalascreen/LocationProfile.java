package org.local.karbalascreen;
import java.time.*;
import java.util.*;
/** Immutable confirmed location. Its identity binds every cache and asynchronous result. */
final class LocationProfile {
 final String city,country,timezone; final double latitude,longitude; final boolean official; final int method; final int[] offsets;
 LocationProfile(String city,String country,double lat,double lon,String zone,boolean official,int method,int fajr,int dhuhr,int isha){
  if(city==null||city.trim().isEmpty()||city.length()>160||country==null||country.length()>160||!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)throw new IllegalArgumentException("Invalid city coordinates");
  if(zone==null||!ZoneId.getAvailableZoneIds().contains(zone))throw new IllegalArgumentException("Invalid IANA timezone");
  if(!Arrays.asList(0,7,3,4,5,2).contains(method))throw new IllegalArgumentException("Invalid method");
  for(int offset:new int[]{fajr,dhuhr,isha})if(offset < -60 || offset > 60)throw new IllegalArgumentException("Correction must be within 60 minutes");
  if(official&&(!zone.equals("Asia/Baghdad")||Math.abs(lat-32.616)>.12||Math.abs(lon-44.025)>.12))throw new IllegalArgumentException("Abbas timetable is only for Karbala");
  this.city=city.trim();this.country=country;latitude=lat;longitude=lon;timezone=zone;this.official=official;this.method=method;offsets=new int[]{fajr,dhuhr,isha};
 }
 static LocationProfile karbala(){return new LocationProfile("كربلاء","العراق",32.616,44.025,"Asia/Baghdad",true,0,0,0,0);}
 ZoneId zone(){return ZoneId.of(timezone);}
 String identity(){return city+"|"+country+"|"+latitude+"|"+longitude+"|"+timezone+"|"+official+"|"+method+"|"+Arrays.toString(offsets);}
 long event(LocalDate date,String time,int correction){return date.atTime(LocalTime.parse(time)).atZone(zone()).plusMinutes(correction).toInstant().toEpochMilli();}
 String methodName(){switch(method){case 0:return "جعفري · Ithna Ashari";case 7:return "طهران";case 3:return "رابطة العالم الإسلامي";case 4:return "أم القرى";case 5:return "الهيئة المصرية";default:return "ISNA";}}
 String source(){return official?"العتبة العباسية المقدسة · كربلاء":"AlAdhan · حسابي · "+methodName();}
}
