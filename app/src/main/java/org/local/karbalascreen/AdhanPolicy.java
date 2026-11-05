package org.local.karbalascreen;

import java.time.*;
import java.util.*;

/** Date and audible-delivery rules shared by alarms and playback. No calculated prayer times. */
final class AdhanPolicy {
    static final ZoneId BAGHDAD = ZoneId.of("Asia/Baghdad");
    static final String[] KEYS = {"الفجر", "الظهرين", "المغرب والعشاء"};
    static final class Event {
        final String date, key; final long at;
        Event(String date, String key, long at) { this.date = date; this.key = key; this.at = at; }
    }
    private static List<Event> today(String date, Map<String,String> times, long now) { try{if(!LocalDate.parse(date).equals(Instant.ofEpochMilli(now).atZone(BAGHDAD).toLocalDate()))return Collections.emptyList();}catch(Exception invalid){return Collections.emptyList();}return events(date,times,now,BAGHDAD,new int[]{0,0,0}); }
    private static List<Event> events(String date,Map<String,String> times,long now,ZoneId zone,int[] offsets){
        List<Event> events = new ArrayList<>();
        try {
            LocalDate day = LocalDate.parse(date);
            if (Math.abs(java.time.temporal.ChronoUnit.DAYS.between(day,Instant.ofEpochMilli(now).atZone(zone).toLocalDate()))>1 || times == null) return events;
            for (int index=0;index<KEYS.length;index++) {
                String key=KEYS[index];
                String value = times.get(key);
                if (value == null || !value.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")) return Collections.emptyList();
                events.add(new Event(date, key, day.atTime(LocalTime.parse(value)).atZone(zone).plusMinutes(offsets[index]).toInstant().toEpochMilli()));
            }
        } catch (RuntimeException invalid) { return Collections.emptyList(); }
        return events;
    }
    static List<Event> future(String date, Map<String,String> times, long now) {
        List<Event> result = new ArrayList<>();
        for (Event event : today(date, times, now)) if (event.at > now) result.add(event);
        return result;
    }
    static boolean shouldPlay(Event event, String date, Map<String,String> times, long now, boolean played) {
        if (event == null || played || now < event.at || now - event.at > 60000) return false;
        for (Event valid : today(date, times, now))
            if (valid.date.equals(event.date) && valid.key.equals(event.key) && valid.at == event.at) return true;
        return false;
    }
    static List<Event> future(String date,Map<String,String> times,long now,LocationProfile p){List<Event> out=new ArrayList<>();for(Event e:events(date,times,now,p.zone(),p.offsets))if(e.at>now)out.add(e);return out;}
    static boolean shouldPlay(Event event,String date,Map<String,String> times,long now,boolean played,LocationProfile p){if(event==null||played||now<event.at||now-event.at>60000)return false;for(Event e:events(date,times,now,p.zone(),p.offsets))if(e.date.equals(event.date)&&e.key.equals(event.key)&&e.at==event.at)return true;return false;}
    private AdhanPolicy() { }
}
