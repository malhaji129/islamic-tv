package org.local.karbalascreen;
import java.time.*;
public final class LocationProfileTest {
 static void check(boolean x){if(!x)throw new AssertionError();}
 static void reject(Runnable r){try{r.run();throw new AssertionError();}catch(IllegalArgumentException expected){}}
 public static void main(String[] args){
  reject(()->new LocationProfile("x","x",91,0,"Europe/London",false,0,0,0,0));
  reject(()->new LocationProfile("x","x",0,0,"garbage",false,0,0,0,0));
  reject(()->new LocationProfile("Baghdad","Iraq",33.3,44.4,"Asia/Baghdad",true,0,0,0,0));
  reject(()->new LocationProfile("x","x",0,0,"Etc/UTC",false,0,61,0,0));
  reject(()->new LocationProfile("x","x",0,0,"Etc/UTC",false,0,Integer.MIN_VALUE,0,0));
  LocationProfile p=new LocationProfile("London","UK",51.5,-.1,"Europe/London",false,0,0,0,0);
  LocationProfile q=new LocationProfile("London","UK",51.5,-.1,"Europe/London",false,7,0,0,0);
  check(!p.identity().equals(q.identity()));
  check(p.event(LocalDate.of(2026,3,29),"03:00",0)==Instant.parse("2026-03-29T02:00:00Z").toEpochMilli());
  check(p.event(LocalDate.of(2026,1,1),"00:05",-10)==Instant.parse("2025-12-31T23:55:00Z").toEpochMilli());
  check(!p.identity().equals(new LocationProfile("London","UK",51.5,-.1,"Europe/London",false,0,1,0,0).identity()));
  reject(()->CalculatedPrayerValidation.validate("02-01-2026","Europe/London",51.5,-.1,0,LocalDate.of(2026,1,1),p));
  reject(()->CalculatedPrayerValidation.validate("01-01-2026","Asia/Baghdad",51.5,-.1,0,LocalDate.of(2026,1,1),p));
  reject(()->CalculatedPrayerValidation.validate("01-01-2026","Europe/London",33,44,0,LocalDate.of(2026,1,1),p));
  reject(()->CalculatedPrayerValidation.validate("01-01-2026","Europe/London",Double.NaN,-.1,0,LocalDate.of(2026,1,1),p));
  reject(()->CalculatedPrayerValidation.validate("01-01-2026","Europe/London",51.5,-.1,7,LocalDate.of(2026,1,1),p));
  CalculatedPrayerValidation.validate("01-01-2026","Europe/London",51.5,-.1,0,LocalDate.of(2026,1,1),p);
  java.util.Map<String,String> times=new java.util.LinkedHashMap<>();times.put(AdhanPolicy.KEYS[0],"05:00");times.put(AdhanPolicy.KEYS[1],"12:00");times.put(AdhanPolicy.KEYS[2],"23:55");
  LocationProfile late=new LocationProfile("London","UK",51.5,-.1,"Europe/London",false,0,0,0,10);
  java.util.List<AdhanPolicy.Event> events=AdhanPolicy.future("2026-01-01",times,Instant.parse("2026-01-01T23:59:00Z").toEpochMilli(),late);
  check(events.size()==1&&events.get(0).at==Instant.parse("2026-01-02T00:05:00Z").toEpochMilli());
  check(AdhanPolicy.shouldPlay(events.get(0),"2026-01-01",times,events.get(0).at,false,late));
  check(!AdhanPolicy.shouldPlay(events.get(0),"2026-01-01",times,events.get(0).at,false,p));
  times.put(AdhanPolicy.KEYS[2],"00:20");
  LocationProfile early=new LocationProfile("London","UK",51.5,-.1,"Europe/London",false,0,0,0,-30);
  events=AdhanPolicy.future("2026-01-02",times,Instant.parse("2026-01-01T23:30:00Z").toEpochMilli(),early);
  check(events.size()==3);
  check(events.get(2).at==Instant.parse("2026-01-01T23:50:00Z").toEpochMilli());
  check(AdhanPolicy.shouldPlay(events.get(2),"2026-01-02",times,events.get(2).at,false,early));
  System.out.println("Location profile tests passed");
 }
}
