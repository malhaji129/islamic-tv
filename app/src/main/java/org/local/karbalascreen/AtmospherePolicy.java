package org.local.karbalascreen;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Pure decisions; no persistence, network, Android classes or rendering. */
final class AtmospherePolicy {
    static final long FRAME_MS=125, THUNDER_PERIOD_MS=20000;
    static final int THUNDER_MAX_ALPHA=20;
    enum Mode { LIVE, PREVIEW; boolean allowsLiveData(){return this==LIVE;} }
    enum Phase { SAHAR, DAWN, SUNRISE, DAY, SUNSET, NIGHT }
    enum Kind { UNKNOWN,CLEAR,PARTLY_CLOUDY,OVERCAST,FOG,DRIZZLE,FREEZING_DRIZZLE,RAIN,FREEZING_RAIN,SNOW,SNOW_GRAINS,RAIN_SHOWERS,SNOW_SHOWERS,THUNDER,HAIL_THUNDER }
    static Kind kind(int code){
        switch(code){
            case 0:return Kind.CLEAR;
            case 1:case 2:return Kind.PARTLY_CLOUDY;
            case 3:return Kind.OVERCAST;
            case 45:case 48:return Kind.FOG;
            case 51:case 53:case 55:return Kind.DRIZZLE;
            case 56:case 57:return Kind.FREEZING_DRIZZLE;
            case 61:case 63:case 65:return Kind.RAIN;
            case 66:case 67:return Kind.FREEZING_RAIN;
            case 71:case 73:case 75:return Kind.SNOW;
            case 77:return Kind.SNOW_GRAINS;
            case 80:case 81:case 82:return Kind.RAIN_SHOWERS;
            case 85:case 86:return Kind.SNOW_SHOWERS;
            case 95:return Kind.THUNDER;
            case 96:case 99:return Kind.HAIL_THUNDER;
            default:return Kind.UNKNOWN;
        }
    }
    static Kind liveKind(int code,long observed,long now,boolean enabled){return enabled&&ScreenPolicy.weatherFresh(observed,now)?kind(code):Kind.UNKNOWN;}
    static int wind(double kmh,long observed,long now){return Double.isFinite(kmh)&&ScreenPolicy.weatherFresh(observed,now)?kmh>=45?2:kmh>=25?1:0:0;}
    static double windSpeed(double sustained,double gust){return Double.isFinite(sustained)?Double.isFinite(gust)?Math.max(sustained,gust):sustained:gust;}
    static boolean dust(double value,long observed,long now){return Double.isFinite(value)&&value>=50&&ScreenPolicy.weatherFresh(observed,now);}
    static boolean snow(Kind kind){return kind==Kind.SNOW||kind==Kind.SNOW_GRAINS||kind==Kind.SNOW_SHOWERS;}
    static boolean thunder(Kind kind){return kind==Kind.THUNDER||kind==Kind.HAIL_THUNDER;}
    static boolean rain(Kind kind){return kind==Kind.DRIZZLE||kind==Kind.FREEZING_DRIZZLE||kind==Kind.RAIN||kind==Kind.FREEZING_RAIN||kind==Kind.RAIN_SHOWERS||thunder(kind);}
    static boolean animate(boolean visible,boolean enabled,Kind kind,int wind){return visible&&enabled&&(rain(kind)||snow(kind)||wind>0);}
    static long expiryDelay(long now,long weatherObserved,long dustObserved,boolean active){
        if(!active)return 0;
        long delay=Long.MAX_VALUE;
        if(ScreenPolicy.weatherFresh(weatherObserved,now))delay=Math.min(delay,weatherObserved+ScreenPolicy.WEATHER_INTERVAL_MS-now);
        if(ScreenPolicy.weatherFresh(dustObserved,now))delay=Math.min(delay,dustObserved+ScreenPolicy.WEATHER_INTERVAL_MS-now);
        return delay==Long.MAX_VALUE?0:delay;
    }
    static boolean validSolar(long now,long rise,long set){
        return validSolar(now,rise,set,ZoneId.of("Asia/Baghdad"));
    }
    static boolean validSolar(long now,long rise,long set,ZoneId zone){
        LocalDate today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        return rise>0&&set>rise&&Instant.ofEpochMilli(rise).atZone(zone).toLocalDate().equals(today)&&Instant.ofEpochMilli(set).atZone(zone).toLocalDate().equals(today)&&set-rise>0&&set-rise<=24*3600000L;
    }
    static Phase phase(long now,long rise,long set){
        return phase(now,rise,set,ZoneId.of("Asia/Baghdad"));
    }
    static Phase phase(long now,long rise,long set,ZoneId zone){
        if(!validSolar(now,rise,set,zone)){
            LocalDate today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
            rise=today.atTime(6,0).atZone(zone).toInstant().toEpochMilli();set=today.atTime(18,0).atZone(zone).toInstant().toEpochMilli();
        }
        if(now<rise-150*60000L)return Phase.NIGHT;
        if(now<rise-60*60000L)return Phase.SAHAR;
        if(now<rise)return Phase.DAWN;
        if(now<rise+45*60000L)return Phase.SUNRISE;
        if(now<set-45*60000L)return Phase.DAY;
        if(now<set+45*60000L)return Phase.SUNSET;
        return Phase.NIGHT;
    }
    static String period(Phase phase){return phase==Phase.DAY?"day":phase==Phase.SAHAR||phase==Phase.NIGHT?"night":"twilight";}
    static String phaseName(Phase phase){switch(phase){case SAHAR:return "السَحَر";case DAWN:return "الفجر";case SUNRISE:return "الشروق";case DAY:return "النهار";case SUNSET:return "الغروب";default:return "الليل";}}
    static String description(Kind kind,boolean day){switch(kind){case CLEAR:return day?"سماء صافية":"ليلة صافية";case PARTLY_CLOUDY:return "غائم جزئياً";case OVERCAST:return "غائم";case FOG:return "ضباب";case DRIZZLE:return "رذاذ";case FREEZING_DRIZZLE:return "رذاذ متجمد";case RAIN:return "أمطار";case FREEZING_RAIN:return "أمطار متجمدة";case SNOW:return "ثلوج";case SNOW_GRAINS:return "حبيبات ثلجية";case RAIN_SHOWERS:return "زخات مطر";case SNOW_SHOWERS:return "زخات ثلج";case THUNDER:return "عواصف رعدية";case HAIL_THUNDER:return "رعد مع بَرَد";default:return "حالة الطقس غير مصنفة";}}
}
