package com.sunnykwong.omc;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;

import org.apache.commons.math.analysis.SplineInterpolator;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import java.net.URL;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.Build;
import android.text.format.Time;
import android.util.Log;

public class METARHandler {

	public static final long MINTIMEBETWEENREQUESTS = 82800000l; //23 hours
	public static final long MINRETRYPERIOD = 3660000l; //One hour + change
	public static String CACHEDFORECAST;
	public static long CACHEDFORECASTMILLIS=0l;
	public static boolean LOCATIONCHANGED;
	public static String LASTUSEDCITY, LASTUSEDCOUNTRY;
	
	//public static final String URL_V4CIVIL = "http://www.7timer.com/v4/bin/civil.php?app=omc&output=json&tzshift=0&unit=metric&lang=en&ac=0";
	public static final String URL_NOAAMETAR = "ftp://tgftp.nws.noaa.gov/data/observations/metar/stations/";
	public static JSONObject tempJson, jsonWeather, jsonOneDayForecast;
	public static HashMap<String, Double> LOWTEMPS;
	public static HashMap<String, Double> HIGHTEMPS;
	public static HashMap<String, Integer> CONDITIONS;
	public static HashMap<String, Integer> CONDITIONTRANSLATIONS;
 
	static public void updateWeather(final double latitude, final double longitude, final String country, final String city, final boolean bylatlong) {

		// If the city or country is empty, it's the first time this is run - location has changed.
		if (LASTUSEDCITY==null || LASTUSEDCOUNTRY==null) {
			LASTUSEDCITY = city;
			LASTUSEDCOUNTRY = country;
			LOCATIONCHANGED=true;
		// If either city and country have changed, 
		// set the location change flag to true.
		} else if (!LASTUSEDCITY.equals(city) || !LASTUSEDCOUNTRY.equals(country)) {
			LASTUSEDCITY = city;
			LASTUSEDCOUNTRY = country;
			LOCATIONCHANGED=true;
		// If city and country have not changed, 
		// but we've lost the cached forecast or the 
		// cached weather is older than 7 hours
		// set the location change flag to true.
		} else if (CACHEDFORECAST==null || System.currentTimeMillis()-CACHEDFORECASTMILLIS>MINTIMEBETWEENREQUESTS) {
			LASTUSEDCITY = city;
			LASTUSEDCOUNTRY = country;
			LOCATIONCHANGED=true;
		// otherwise, the location didn't change - let's use the cached forecast.
		} else  {
			LOCATIONCHANGED=false;
		}

		// Populating the condition translations
		CONDITIONTRANSLATIONS = new HashMap<String,Integer>();
		CONDITIONTRANSLATIONS.put("clear", 1);
		CONDITIONTRANSLATIONS.put("pcloudy", 4);
		CONDITIONTRANSLATIONS.put("mcloudy", 10);
		CONDITIONTRANSLATIONS.put("cloudy", 12);
		CONDITIONTRANSLATIONS.put("humid", 13);
		CONDITIONTRANSLATIONS.put("lightrain", 14);
		CONDITIONTRANSLATIONS.put("oshower", 19);
		CONDITIONTRANSLATIONS.put("ishower", 20);
		CONDITIONTRANSLATIONS.put("lightsnow", 29);
		CONDITIONTRANSLATIONS.put("rain", 27);
		CONDITIONTRANSLATIONS.put("snow", 33);
		CONDITIONTRANSLATIONS.put("rainsnow", 38);
		CONDITIONTRANSLATIONS.put("ts", 21);
		CONDITIONTRANSLATIONS.put("tsrain", 22);
		CONDITIONTRANSLATIONS.put("undefined", 0);
		
//		if (LOCATIONCHANGED) {
			// Update weather from provider.
			Thread t = new Thread() {
				@Override
				public void run() {
	
					jsonWeather = new JSONObject();
					try {
						jsonWeather.put("zzforecast_conditions", new JSONArray());
					} catch (JSONException e) {
						e.printStackTrace();
					}
					jsonOneDayForecast = null;
					LOWTEMPS = new HashMap<String, Double>();
					HIGHTEMPS = new HashMap<String, Double>();
					CONDITIONS = new HashMap<String, Integer>();
	
					URLConnection conn = null; 
					try {
						// Start building URL string.
						String sURL = URL_NOAAMETAR;
						
						Time tNow = new Time();
						tNow.setToNow();
						
						if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
						    System.setProperty("http.keepAlive", "false");
						}
	
						jsonWeather.putOpt("country2", country);
						jsonWeather.putOpt("city2", city);
						jsonWeather.putOpt("bylatlong", bylatlong);
						jsonWeather.putOpt("longitude_e6",longitude*1000000d);
						jsonWeather.putOpt("latitude_e6",latitude*1000000d);
	
						URL url=null;
						if (!bylatlong) {
							Log.e(OMC.OMCSHORT + "NOAAMETAR", "NOAA-METAR plugin does not support weather by location name!");
							return;
						} else {
							
							final String[] sICAOs = OMC.findClosestICAOs(latitude, longitude, 100);
							if (OMC.DEBUG)
								Log.i(OMC.OMCSHORT + "NOAAMETAR", "Nearest airports are " + OMC.flattenString(sICAOs));
							
							
							// Get forecast JSON.
							if (OMC.DEBUG)
								Log.i(OMC.OMCSHORT + "NOAAMETAR", "Start Building JSON.");
							String result = null;
							
							for (String s: sICAOs) {
								try {
									url = new URL(sURL+ s +".TXT");
									
									if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "NOAAMETAR", "Trying: " + url.toExternalForm());
									conn = url.openConnection();
									conn.setConnectTimeout(600000);
									conn.setReadTimeout(600000);
									
									result = OMC.streamToString(conn.getInputStream());
									if (OMC.DEBUG) {
										Log.i(OMC.OMCSHORT + "NOAAMETAR", "Airport " + s + " returned good METAR.");
										Log.i(OMC.OMCSHORT + "NOAAMETAR", result);
									}
									Metar metar = Metar.parse(s, result);
									metar.debugPrint();
								} catch (IOException e) {
									if (OMC.DEBUG)
										Log.i(OMC.OMCSHORT + "NOAAMETAR", "Airport " + s + " returned invalid METAR.");
									continue;
								}
								break;
							}
							if (result==null) {
								OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
							}
							// Test cases
//							Metar.parse(null,"METAR EHAM 1050Z 24015KT 9000 RA SCT025 BKN040 10/09 Q1010 NOSIG").debugPrint();
//							Metar.parse(null,"METAR EGLL 0920Z 26005KT CAVOK 15/14 Q1013 NOSIG").debugPrint();
//							Metar.parse(null,"METAR EDDL 1550Z 26005KT 0550 R23L/0450 FZFG OVC002 M02/M02 Q0994 BECMG OVC005").debugPrint();
//							Metar.parse(null,"METAR EIDW 0900Z 24035G55KT 210V270 1700 +SHRA BKN007 OVC015CB 08/07 TEMPO 3500").debugPrint();
//							Metar.parse(null,"METAR LFPG 1250Z 28010KT 8000 HZ SCT070 BKN240 28/22 Q1003 NOSIG").debugPrint();
//							Metar.parse(null,"10Z KLIT 200953Z 00000KT 4SM -RA BR OVC030 07/05 A3013 RMK AO2 SLP205").debugPrint();
//							Metar.parse(null,"KJGG 071555Z AUTO 34011KT 10SM BKN017 OVC049 07/04 A2987 RMK AO1").debugPrint();
//							Metar.parse(null,"KQA7 071555Z 17004KT 9999 CLR 13/M07 A3016 RMK AO2A SLP209 T01331075").debugPrint();
//							Metar.parse(null,"MKJP 282000Z 290388KT 4000 -SHRA VCSH BKN014 FEW018CB BKN030 OVC100 27/22 Q0990").debugPrint();
//							Metar.parse(null,"METAR KJFK 242235Z 28024G36KT 7SM -RA BR BKN009 OVC020CB 26/24 A2998 RMK AO2 SLP993 T02640238 56012").debugPrint();
							

//							System.out.println(metar.getSkyCondition(0).getNaturalLanguageString());
//							System.out.println(metar.toString());
//							System.out.println(metar.getConditions().toString());
//							System.out.println(metar.getTemperature().getTemperature());
							
						}
					} catch (Exception e) {
						OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
						e.printStackTrace();
					}
				}
			};
			t.start();
		}
	}
