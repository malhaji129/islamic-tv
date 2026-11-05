package org.local.karbalascreen;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;
/** Public London fixture fetched from the requested method-0 endpoint, 2026-11-05. */
public class CalculatedPrayerParserTest {
 private final LocationProfile london=new LocationProfile("London","UK",51.5,-.1,"Europe/London",false,0,10,0,-5);
 private JSONObject fixture()throws Exception{try(java.io.InputStream in=getClass().getResourceAsStream("/calculated-london.json")){return new JSONObject(StreamText.read(in));}}
 @Test public void preservesRawTimingsAndExplicitCalculationSource()throws Exception{
  PrayerSource.PrayerDay result=CalculatedPrayerParser.parse(fixture(),LocalDate.of(2026,11,5),london);
  assertEquals("05:19",result.times.get(AdhanPolicy.KEYS[0])); // correction is applied exactly once at rendering/alarm time
  assertEquals("11:44",result.times.get(AdhanPolicy.KEYS[1]));assertEquals("17:55",result.times.get(AdhanPolicy.KEYS[2]));
  assertEquals("05:09",result.times.get("الإمساك"));assertTrue(result.source.contains("حسابي"));assertEquals("",result.hijri);assertEquals(london.identity(),result.profile);
 }
 private void rejected(JSONObject fixture)throws Exception{try{CalculatedPrayerParser.parse(fixture,LocalDate.of(2026,11,5),london);fail("Invalid provider data accepted");}catch(IllegalArgumentException expected){}}
 @Test public void rejectsWrongDateZoneCoordinatesAndMethod()throws Exception{
  JSONObject j=fixture();j.getJSONObject("data").getJSONObject("date").getJSONObject("gregorian").put("date","04-11-2026");rejected(j);
  j=fixture();j.getJSONObject("data").getJSONObject("meta").put("timezone","Asia/Baghdad");rejected(j);
  j=fixture();j.getJSONObject("data").getJSONObject("meta").put("latitude",32.616);rejected(j);
  j=fixture();j.getJSONObject("data").getJSONObject("meta").getJSONObject("method").put("id",7);rejected(j);
 }
 @Test public void rejectsMissingAndMalformedTimings()throws Exception{
  JSONObject j=fixture();j.getJSONObject("data").getJSONObject("timings").put("Fajr","25:90");rejected(j);
  j=fixture();j.put("code",500);rejected(j);
  j=fixture();j.getJSONObject("data").getJSONObject("timings").remove("Dhuhr");try{CalculatedPrayerParser.parse(j,LocalDate.of(2026,11,5),london);fail();}catch(org.json.JSONException expected){}
 }
}
