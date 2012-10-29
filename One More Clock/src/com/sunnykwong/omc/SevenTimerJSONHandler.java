package com.sunnykwong.omc;

import java.net.HttpURLConnection;
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

public class SevenTimerJSONHandler {

	public static final long MINTIMEBETWEENREQUESTS = 46800000l;
	public static final String URL_V4CIVIL = "http://www.7timer.com/v4/bin/civil.php?app=omc&output=json&tzshift=0&unit=metric&lang=en&ac=0";
	public static boolean LOCATIONCHANGED;
	public static String LASTUSEDCITY, LASTUSEDCOUNTRY;
	public static JSONObject tempJson, jsonWeather, jsonOneDayForecast;
	public static HashMap<String, Double> LOWTEMPS;
	public static HashMap<String, Double> HIGHTEMPS;
	public static HashMap<String, Integer> CONDITIONS;
	public static HashMap<String, Integer> CONDITIONTRANSLATIONS;
	public static String CACHEDFORECAST;
	public static long CACHEDFORECASTMILLIS=0l;
 
	public SevenTimerJSONHandler() {
	}

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
		
		if (LOCATIONCHANGED) {
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
	
					HttpURLConnection huc = null; 
					try {
						// Start building URL string.
						String sURL = URL_V4CIVIL;
						
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
							Log.e(OMC.OMCSHORT + "7TWeather", "OpenWeatherMap plugin does not support weather by location name!");
							return;
						} else {
							if (OMC.DEBUG)
								Log.i(OMC.OMCSHORT + "7TWeather", "Start Building JSON.");
							
							// Get forecast JSON.
							url = new URL(sURL + "&lat="+latitude+"&lon="+longitude);
							huc = (HttpURLConnection) url.openConnection();
							huc.setConnectTimeout(600000);
							huc.setReadTimeout(600000);
	
							tempJson = OMC.streamToJSONObject(huc.getInputStream());

							OMC.WEATHERREFRESHSTATUS = OMC.WRS_PROVIDER;
							
							// Cache the forecast for future use.
							CACHEDFORECAST = tempJson.toString();
							
							if (OMC.DEBUG)
								Log.i(OMC.OMCSHORT + "7TWeather", tempJson.toString());
	
							// First, establish the baseline time in phone time.
							
							String sInitTime = tempJson.optString("init",tNow.format("%Y%m%d%H"));
							int year = Integer.parseInt(sInitTime.substring(0,4));
							int month = Integer.parseInt(sInitTime.substring(4,6))-1;
							int monthDay = Integer.parseInt(sInitTime.substring(6,8));
							int hour = Integer.parseInt(sInitTime.substring(8));
	
							Time tInit = new Time(Time.TIMEZONE_UTC);
							tInit.set(0, 0, hour, monthDay, month, year);
							tInit.normalize(false);
							tInit.switchTimezone(Time.getCurrentTimezone());
	
							// Next, loop over the time points and start building conditions.
							int iSeriesLength = tempJson.getJSONArray("dataseries").length();
							
							long lBestTimeDiff=Long.MAX_VALUE;
							double[] x = new double[iSeriesLength];
							double[] y = new double[iSeriesLength];
							// We're looping from the furthest future back to the past.
							for (int i = iSeriesLength-1; i >=0 ; i--) {
								Time tCurrent = new Time(tInit);
								JSONObject jsDataPoint = tempJson.getJSONArray("dataseries").getJSONObject(i);
								tCurrent.hour=tInit.hour+jsDataPoint.optInt("timepoint", 0);
								tCurrent.normalize(true);
								x[i] = tCurrent.toMillis(false);
								Time tNight = new Time(tCurrent);
								tNight.hour-=7;
								tNight.normalize(true);
								String sHTDay = tCurrent.format("%Y%m%d");
								String sLDDay = tNight.format("%Y%m%d");
								// Overwrite the previous conditions.  We'll end up with the conditions around daybreak for each day.
								// v138: Write only if it's daytime (raw condition contains "day").
								if (jsDataPoint.optString("weather","unknown").contains("day")){
//									if (OMC.DEBUG)
//										Log.i(OMC.OMCSHORT + "7TWeather",
//												"Conditions @ phone time" + tCurrent.format("%Y%m%d%H") + "(timepoint " + jsDataPoint.optInt("timepoint", 0) + "): " + OMC.VERBOSEWEATHER[CONDITIONTRANSLATIONS.get(jsDataPoint.optString("weather","unknown")
//														.replace("day", "")
//														.replace("night", ""))]);
									CONDITIONS.put(sHTDay, 
											CONDITIONTRANSLATIONS.get(jsDataPoint.optString("weather","unknown")
												.replace("day", "")
												.replace("night", "")));
								}	
								// Compare/write the current conditions.  The conditions with the nearest timestamp will be used as current time.
								double tempc = jsDataPoint.optDouble("temp2m");
								y[i] = tempc;

								if (Math.abs(tCurrent.toMillis(false)-tNow.toMillis(false))<lBestTimeDiff) {
									lBestTimeDiff = Math.abs(tCurrent.toMillis(false)-tNow.toMillis(false));
									jsonWeather.putOpt("humidity_raw", jsDataPoint.optString("rh2m").replace("%", ""));
									jsonWeather.putOpt("wind_direction",jsDataPoint.getJSONObject("wind10m").getString("direction"));
									double windmps = jsDataPoint.getJSONObject("wind10m").getDouble("speed");
									jsonWeather.putOpt("wind_direction_mps",windmps);
									jsonWeather.putOpt("wind_speed_mph", (int)(windmps*2.2369362920544+0.5));
									int iConditionCode = CONDITIONTRANSLATIONS.get(jsDataPoint.optString("weather","unknown")
										.replace("day", "")
										.replace("night", ""));
									jsonWeather.putOpt("condition_code", iConditionCode);
								}
								
								// Compare/write the high and low temps.  Low temps are counted from 7pm to 7am next day.
								if (OMC.DEBUG)
									Log.i(OMC.OMCSHORT + "7TWeather",
											"Day for High: " + sHTDay + "; Day for Low: " + sLDDay);
								if (!HIGHTEMPS.containsKey(sHTDay)) {
									HIGHTEMPS.put(sHTDay, tempc);
								} else {
									if (tempc>HIGHTEMPS.get(sHTDay)) HIGHTEMPS.put(sHTDay, tempc);
								}
								if (!LOWTEMPS.containsKey(sLDDay)) {
									LOWTEMPS.put(sLDDay, tempc);
								} else {
									if (tempc<LOWTEMPS.get(sLDDay)) LOWTEMPS.put(sLDDay, tempc);
								}
							}
							
							// Build out wind/humidity conditions.
							String humidityString = OMC.RString("humiditycondition") +
									jsonWeather.optString("humidity_raw") + "%";
							String windString = OMC.RString("windcondition") +
									jsonWeather.optString("wind_direction") + " @ " +
									jsonWeather.optString("wind_speed_mph") + " mph";
							
							jsonWeather.putOpt("humidity", humidityString);
							jsonWeather.putOpt("wind_condition", windString);
	
							// 
							// Build out the forecast array.
							Time day = new Time();
							day.setToNow();
							
							// Make sure today's data contains both high and low, too.
							if (!LOWTEMPS.containsKey(day.format("%Y%m%d"))) {
								Time nextday = new Time(day);
								nextday.hour+=24;
								nextday.normalize(false);
								LOWTEMPS.put(day.format("%Y%m%d"), LOWTEMPS.get(nextday.format("%Y%m%d")));
							}
							if (!CONDITIONS.containsKey(day.format("%Y%m%d"))) {
								Time nextday = new Time(day);
								nextday.hour+=24;
								nextday.normalize(false);
								CONDITIONS.put(day.format("%Y%m%d"), CONDITIONS.get(nextday.format("%Y%m%d")));
							}
							if (!HIGHTEMPS.containsKey(day.format("%Y%m%d"))) {
								HIGHTEMPS.put(day.format("%Y%m%d"), LOWTEMPS.get(day.format("%Y%m%d")));
							}
													
							while (HIGHTEMPS.containsKey(day.format("%Y%m%d")) && LOWTEMPS.containsKey(day.format("%Y%m%d"))) {
								try {
									JSONObject jsonOneDayForecast = new JSONObject();
									jsonOneDayForecast.put("day_of_week", day.format("%a"));
									jsonOneDayForecast.put("condition_code", CONDITIONS.get(day.format("%Y%m%d")));
									double lowc = OMC.roundToSignificantFigures(LOWTEMPS.get(day.format("%Y%m%d")),3);
									double highc = OMC.roundToSignificantFigures(HIGHTEMPS.get(day.format("%Y%m%d")),3);
									double lowf = (int)(lowc/5f*9f+32.7f);
									double highf = (int)(highc/5f*9f+32.7f);
									jsonOneDayForecast.put("low_c", lowc);
									jsonOneDayForecast.put("high_c", highc);
									jsonOneDayForecast.put("low", lowf);
									jsonOneDayForecast.put("high", highf);
									jsonWeather.getJSONArray("zzforecast_conditions").put(
											jsonOneDayForecast);
									day.hour+=24;
									day.normalize(false);
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
							

							UnivariateRealFunction fn = new SplineInterpolator().interpolate(x,y);
							double tempc = fn.value(System.currentTimeMillis());
							jsonWeather.putOpt("temp_c",(int)(tempc+0.5d));
							jsonWeather.putOpt("temp_f",(int)(tempc*9f/5f+32.7f));
							if (OMC.DEBUG)
								Log.i(OMC.OMCSHORT + "7TWeather", jsonWeather.toString());
	
							try {
								if (jsonWeather.optString("city")==null || jsonWeather.optString("city").equals("")) {
									if (!jsonWeather.optString("city2").equals(""))
										jsonWeather.putOpt("city", jsonWeather.optString("city2"));
									else if (!jsonWeather.optString("country2").equals(""))
										jsonWeather.putOpt("city", jsonWeather.optString("country2"));
								}
								
							} catch (JSONException e) {
								e.printStackTrace();
								return;
							}
							OMC.PREFS.edit().putString("weather", jsonWeather.toString()).commit();
							OMC.LASTWEATHERREFRESH = System.currentTimeMillis();
							CACHEDFORECASTMILLIS = System.currentTimeMillis();

							if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Update Succeeded.  Phone Time:" + new java.sql.Time(OMC.LASTWEATHERREFRESH).toLocaleString());
		
							Time t = new Time();
							// If the weather station information (international, mostly) doesn't have a timestamp, set the timestamp to be jan 1st, 1970
							t.parse(jsonWeather.optString("current_local_time","19700101T000000"));
							long lNextWeatherRetryIfFailed = OMC.LASTWEATHERTRY+(1l + Math.max(3660000l, Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))/4l*60000l));
							// If the weather station info looks too stale (more than 2 hours old), it's because the phone's date/time is wrong.  
							// Force the update to the default update period
							if (System.currentTimeMillis()-t.toMillis(false)>7200000l) {
								OMC.NEXTWEATHERREFRESH = Math.max(OMC.LASTWEATHERREFRESH + (1l + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))) * 60000l, 
										lNextWeatherRetryIfFailed);
								if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
								if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Weather Station Time Missing or Stale.  Using default interval.");
							} else if (t.toMillis(false)>System.currentTimeMillis()) {
							// If the weather station time is in the future, something is definitely wrong! 
							// Force the update to the default update period
								if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
								if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Weather Station Time in the future -> phone time is wrong.  Using default interval.");
								OMC.NEXTWEATHERREFRESH = Math.max(OMC.LASTWEATHERREFRESH + (1l + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))) * 60000l, 
										lNextWeatherRetryIfFailed);
							} else {
							// If we get a recent weather station timestamp, we try to "catch" the update by setting next update to 
							// 29 minutes + default update period
							// after the last station refresh.
		
								if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
								OMC.NEXTWEATHERREFRESH = Math.max(t.toMillis(false) + (29l + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))) * 60000l, 
										lNextWeatherRetryIfFailed);
							}
							if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Next Refresh Time:" + new java.sql.Time(OMC.NEXTWEATHERREFRESH).toLocaleString());
							OMC.PREFS.edit().putLong("weather_nextweatherrefresh", OMC.NEXTWEATHERREFRESH).commit();

							//Set next online request to be 7 hours in the future.
							OMC.NEXTWEATHERREQUEST = OMC.NEXTWEATHERREFRESH + MINTIMEBETWEENREQUESTS;
							OMC.WEATHERREFRESHSTATUS = OMC.WRS_SUCCESS;
							
						}
	
					} catch (Exception e) { 
						e.printStackTrace();
						if (huc!=null) huc.disconnect();
						OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
					}
				};
			};
			t.start();
		} else {
			// Update weather from cached forecast.
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
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
		
						Time tNow = new Time();
						tNow.setToNow();
						jsonWeather.putOpt("country2", country);
						jsonWeather.putOpt("city2", city);
						jsonWeather.putOpt("bylatlong", bylatlong);
						jsonWeather.putOpt("longitude_e6",longitude*1000000d);
						jsonWeather.putOpt("latitude_e6",latitude*1000000d);
	
						if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Interpolating weather from previously-cached forecast.");
						tempJson = new JSONObject(CACHEDFORECAST);

						OMC.WEATHERREFRESHSTATUS = OMC.WRS_PROVIDER;
						
						if (OMC.DEBUG)
							Log.i(OMC.OMCSHORT + "7TWeather", tempJson.toString());
	
						// First, establish the baseline time in phone time.
						
						String sInitTime = tempJson.optString("init",tNow.format("%Y%m%d%H"));
						int year = Integer.parseInt(sInitTime.substring(0,4));
						int month = Integer.parseInt(sInitTime.substring(4,6))-1;
						int monthDay = Integer.parseInt(sInitTime.substring(6,8));
						int hour = Integer.parseInt(sInitTime.substring(8));
	
						Time tInit = new Time(Time.TIMEZONE_UTC);
						tInit.set(0, 0, hour, monthDay, month, year);
						tInit.normalize(false);
						tInit.switchTimezone(Time.getCurrentTimezone());
	
						// Next, loop over the time points and start building conditions.
						int iSeriesLength = tempJson.getJSONArray("dataseries").length();
						
						long lBestTimeDiff=Long.MAX_VALUE;
						double[] x = new double[iSeriesLength];
						double[] y = new double[iSeriesLength];
						// We're looping from the furthest future back to the past.
						for (int i = iSeriesLength-1; i >=0 ; i--) {
							Time tCurrent = new Time(tInit);
							JSONObject jsDataPoint = tempJson.getJSONArray("dataseries").getJSONObject(i);
							tCurrent.hour=tInit.hour+jsDataPoint.optInt("timepoint", 0);
							tCurrent.normalize(true);
							x[i] = tCurrent.toMillis(false);
							Time tNight = new Time(tCurrent);
							tNight.hour-=7;
							tNight.normalize(true);
							String sHTDay = tCurrent.format("%Y%m%d");
							String sLDDay = tNight.format("%Y%m%d");
							// Overwrite the previous conditions.  We'll end up with the conditions around daybreak for each day.
							// v138: Write only if it's daytime (raw condition contains "day").
							if (jsDataPoint.optString("weather","unknown").contains("day")){
//								if (OMC.DEBUG)
//									Log.i(OMC.OMCSHORT + "7TWeather",
//											"Conditions @ phone time" + tCurrent.format("%Y%m%d%H") + "(timepoint " + jsDataPoint.optInt("timepoint", 0) + "): " + OMC.VERBOSEWEATHER[CONDITIONTRANSLATIONS.get(jsDataPoint.optString("weather","unknown")
//													.replace("day", "")
//													.replace("night", ""))]);
								CONDITIONS.put(sHTDay, 
										CONDITIONTRANSLATIONS.get(jsDataPoint.optString("weather","unknown")
											.replace("day", "")
											.replace("night", "")));
							}	
							// Compare/write the current conditions.  The conditions with the nearest timestamp will be used as current time.
							double tempc = jsDataPoint.optDouble("temp2m");
							y[i] = tempc;
	
							if (Math.abs(tCurrent.toMillis(false)-tNow.toMillis(false))<lBestTimeDiff) {
								lBestTimeDiff = Math.abs(tCurrent.toMillis(false)-tNow.toMillis(false));
								jsonWeather.putOpt("humidity_raw", jsDataPoint.optString("rh2m").replace("%", ""));
								jsonWeather.putOpt("wind_direction",jsDataPoint.getJSONObject("wind10m").getString("direction"));
								double windmps = jsDataPoint.getJSONObject("wind10m").getDouble("speed");
								jsonWeather.putOpt("wind_direction_mps",windmps);
								jsonWeather.putOpt("wind_speed_mph", (int)(windmps*2.2369362920544+0.5));
								int iConditionCode = CONDITIONTRANSLATIONS.get(jsDataPoint.optString("weather","unknown")
									.replace("day", "")
									.replace("night", ""));
								jsonWeather.putOpt("condition_code", iConditionCode);
							}
							
							// Compare/write the high and low temps.  Low temps are counted from 7pm to 7am next day.
							if (!HIGHTEMPS.containsKey(sHTDay)) {
								HIGHTEMPS.put(sHTDay, tempc);
							} else {
								if (tempc>HIGHTEMPS.get(sHTDay)) HIGHTEMPS.put(sHTDay, tempc);
							}
							if (!LOWTEMPS.containsKey(sLDDay)) {
								LOWTEMPS.put(sLDDay, tempc);
							} else {
								if (tempc<LOWTEMPS.get(sLDDay)) LOWTEMPS.put(sLDDay, tempc);
							}
						}
						
						// Build out wind/humidity conditions.
						String humidityString = OMC.RString("humiditycondition") +
								jsonWeather.optString("humidity_raw") + "%";
						String windString = OMC.RString("windcondition") +
								jsonWeather.optString("wind_direction") + " @ " +
								jsonWeather.optString("wind_speed_mph") + " mph";
						
						jsonWeather.putOpt("humidity", humidityString);
						jsonWeather.putOpt("wind_condition", windString);
	
						// 
						// Build out the forecast array.
						Time day = new Time();
						day.setToNow();
						
						// Make sure today's data contains both high and low, too.
						if (!LOWTEMPS.containsKey(day.format("%Y%m%d"))) {
							Time nextday = new Time(day);
							nextday.hour+=24;
							nextday.normalize(false);
							LOWTEMPS.put(day.format("%Y%m%d"), LOWTEMPS.get(nextday.format("%Y%m%d")));
						}
						if (!CONDITIONS.containsKey(day.format("%Y%m%d"))) {
							Time nextday = new Time(day);
							nextday.hour+=24;
							nextday.normalize(false);
							CONDITIONS.put(day.format("%Y%m%d"), CONDITIONS.get(nextday.format("%Y%m%d")));
						}
						if (!HIGHTEMPS.containsKey(day.format("%Y%m%d"))) {
							HIGHTEMPS.put(day.format("%Y%m%d"), LOWTEMPS.get(day.format("%Y%m%d")));
						}
												
						while (HIGHTEMPS.containsKey(day.format("%Y%m%d")) && LOWTEMPS.containsKey(day.format("%Y%m%d"))) {
							try {
								JSONObject jsonOneDayForecast = new JSONObject();
								jsonOneDayForecast.put("day_of_week", day.format("%a"));
								jsonOneDayForecast.put("condition_code", CONDITIONS.get(day.format("%Y%m%d")));
								double lowc = OMC.roundToSignificantFigures(LOWTEMPS.get(day.format("%Y%m%d")),3);
								double highc = OMC.roundToSignificantFigures(HIGHTEMPS.get(day.format("%Y%m%d")),3);
								double lowf = (int)(lowc/5f*9f+32.7f);
								double highf = (int)(highc/5f*9f+32.7f);
								jsonOneDayForecast.put("low_c", lowc);
								jsonOneDayForecast.put("high_c", highc);
								jsonOneDayForecast.put("low", lowf);
								jsonOneDayForecast.put("high", highf);
								jsonWeather.getJSONArray("zzforecast_conditions").put(
										jsonOneDayForecast);
								day.hour+=24;
								day.normalize(false);
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
						
	
						UnivariateRealFunction fn = new SplineInterpolator().interpolate(x,y);
						double tempc = fn.value(System.currentTimeMillis());
						jsonWeather.putOpt("temp_c",(int)(tempc+0.5d));
						jsonWeather.putOpt("temp_f",(int)(tempc*9f/5f+32.7f));
						if (OMC.DEBUG)
							Log.i(OMC.OMCSHORT + "7TWeather", jsonWeather.toString());
	
						try {
							if (jsonWeather.optString("city")==null || jsonWeather.optString("city").equals("")) {
								if (!jsonWeather.optString("city2").equals(""))
									jsonWeather.putOpt("city", jsonWeather.optString("city2"));
								else if (!jsonWeather.optString("country2").equals(""))
									jsonWeather.putOpt("city", jsonWeather.optString("country2"));
							}
							
						} catch (JSONException e) {
							e.printStackTrace();
							return;
						}
						OMC.PREFS.edit().putString("weather", jsonWeather.toString()).commit();
						OMC.LASTWEATHERREFRESH = System.currentTimeMillis();
						if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Update Succeeded.  Phone Time:" + new java.sql.Time(OMC.LASTWEATHERREFRESH).toLocaleString());
	
						Time t = new Time();
						// If the weather station information (international, mostly) doesn't have a timestamp, set the timestamp to be jan 1st, 1970
						t.parse(jsonWeather.optString("current_local_time","19700101T000000"));
						
						long lNextWeatherRetryIfFailed = OMC.LASTWEATHERTRY+(1l + Math.max(3660000l, Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))/4l*60000l));
						// If the weather station info looks too stale (more than 2 hours old), it's because the phone's date/time is wrong.  
						// Force the update to the default update period
						if (System.currentTimeMillis()-t.toMillis(false)>7200000l) {
							OMC.NEXTWEATHERREFRESH = Math.max(OMC.LASTWEATHERREFRESH + (1l + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))) * 60000l, 
									lNextWeatherRetryIfFailed);
							if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
							if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Weather Station Time Missing or Stale.  Using default interval.");
						} else if (t.toMillis(false)>System.currentTimeMillis()) {
						// If the weather station time is in the future, something is definitely wrong! 
						// Force the update to the default update period
							if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
							if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Weather Station Time in the future -> phone time is wrong.  Using default interval.");
							OMC.NEXTWEATHERREFRESH = Math.max(OMC.LASTWEATHERREFRESH + (1l + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))) * 60000l, 
									lNextWeatherRetryIfFailed);
						} else {
						// If we get a recent weather station timestamp, we try to "catch" the update by setting next update to 
						// 29 minutes + default update period
						// after the last station refresh.
	
							if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
							OMC.NEXTWEATHERREFRESH = Math.max(t.toMillis(false) + (29l + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))) * 60000l, 
									lNextWeatherRetryIfFailed);
						}
						if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "7TWeather", "Next Refresh Time:" + new java.sql.Time(OMC.NEXTWEATHERREFRESH).toLocaleString());
						OMC.PREFS.edit().putLong("weather_nextweatherrefresh", OMC.NEXTWEATHERREFRESH).commit();

						OMC.WEATHERREFRESHSTATUS = OMC.WRS_SUCCESS;
						
						
					} catch (Exception e) {
						e.printStackTrace();
						OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
					}
					
				}

			};
			t.start();
		};
			
	}

}