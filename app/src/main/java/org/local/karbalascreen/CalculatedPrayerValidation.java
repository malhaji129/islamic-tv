package org.local.karbalascreen;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
/** Provider metadata must agree before any timing is used or cached. */
final class CalculatedPrayerValidation {
 static void validate(String date,String zone,double latitude,double longitude,int method,LocalDate requested,LocationProfile p){
  if(!requested.format(DateTimeFormatter.ofPattern("dd-MM-uuuu")).equals(date)||!p.timezone.equals(zone)||!Double.isFinite(latitude)||!Double.isFinite(longitude)||Math.abs(latitude-p.latitude)>.02||Math.abs(longitude-p.longitude)>.02||method!=p.method)throw new IllegalArgumentException("Wrong prayer date/location/method");
 }
}
