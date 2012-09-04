package com.sunnykwong.omc;

import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.Build;
import android.text.format.Time;
import android.util.Log;

public class OpenWeatherMapJSONHandler {

	public static final String URL_STATIONSEARCH = "http://openweathermap.org/data/2.0/find/station?cnt=1&lat=";
	public static final String URL_CITYSEARCH = "http://openweathermap.org/data/2.0/find/city?cnt=1&lat=";
	public static final String URL_CITYFORECAST = "http://openweathermap.org/data/2.0/forecast/city/";

	public static final String[] CONDITIONTRANSLATIONS = new String[] {
		"Unknown", 									//0
		"Clear",									//1 SUN
 		"Mostly Sunny",								//2 LIGHTCLOUD
 		"Partly Cloudy",							//3 PARTLYCLOUD
 		"Cloudy",									//4 CLOUD
 		"Scattered Showers",						//5 LIGHTRAINSUN
		"Chance of Storm",							//6 LIGHTRAINTHUNDERSUN
		"Sleet",									//7 SLEETSUN
		"Snow",										//8 SNOWSUN
		"Light Rain",								//9 LIGHTRAIN
		"Rain",										//10 RAIN
		"Scattered Thunderstorms",					//11 RAINTHUNDER
		"Sleet",									//12 SLEET
		"Snow",										//13 SNOW
		"Scattered Thunderstorms",					//14 SNOWTHUNDER
		"Fog",										//15 FOG
		"Clear",									//16 SUN ( used for winter darkness )
		"Cloudy",									//17 LIGHTCLOUD ( winter darkness )
		"Light Rain",								//18 LIGHTRAINSUN ( used for winter darkness )
		"Scattered Snow Showers",					//19 SNOWSUN ( used for winter darkness )
		"Scattered Thunderstorms",					//20 SLEETSUNTHUNDER
		"Scattered Thunderstorms",					//21 SNOWSUNTHUNDER
		"Thunderstorm",								//22 LIGHTRAINTHUNDER
		"Thunderstorm"								//23 SLEETTHUNDER2
	};
	
	public JSONObject tempJson, jsonWeather, jsonOneDayForecast;
	public String sNearestCityID;
	public static final String DELIMITERS = " .,;-";

	public OpenWeatherMapJSONHandler() {
		sNearestCityID="";
		jsonWeather = new JSONObject();
		try {
			jsonWeather.put("zzforecast_conditions", new JSONArray());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		jsonOneDayForecast = null;
	}

	static public void updateWeather(final double latitude, final double longitude, final String country, final String city, final boolean bylatlong) {
		
		Thread t = new Thread() {
			@Override
			public void run() {
				HttpURLConnection huc = null; 
				try {
					if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
					    System.setProperty("http.keepAlive", "false");
					}
					OpenWeatherMapJSONHandler OWMhandler = new OpenWeatherMapJSONHandler();
					OWMhandler.jsonWeather.putOpt("country2", country);
					OWMhandler.jsonWeather.putOpt("city2", city);
					OWMhandler.jsonWeather.putOpt("bylatlong", bylatlong);

					URL url=null;
					if (!bylatlong) {
						Log.e(OMC.OMCSHORT + "OWMWeather", "OpenWeatherMap plugin does not support weather by location name!");
						return;
//						url = new URL("http://www.google.com/ig/api?oe=utf-8&weather="+city.replace(' ', '+') + "+" + country.replace(' ', '+'));
					} else {
						if (OMC.DEBUG)
							Log.i(OMC.OMCSHORT + "OWMWeather", "Start Building JSON.");
						
						// First, Find current conditions from the closest Weather Station.
						url = new URL(OpenWeatherMapJSONHandler.URL_STATIONSEARCH+latitude+";lon="+longitude);
						huc = (HttpURLConnection) url.openConnection();
						huc.setConnectTimeout(10000);
						huc.setReadTimeout(10000);

						OWMhandler.tempJson = OMC.streamToJSONObject(huc.getInputStream());
						JSONObject stationinfo = OWMhandler.tempJson.getJSONArray("list").getJSONObject(0);
						JSONObject maininfo = stationinfo.getJSONObject("main");
						// Translate temperature.
						double tempc = maininfo.getDouble("temp")-273.15;
						OWMhandler.jsonWeather.put("temp_c", tempc);
						OWMhandler.jsonWeather.put("temp_f",(int)(tempc*9f/5f+32.7f));
						// Translate humidity.
						OWMhandler.jsonWeather.put("humidity_raw",maininfo.getDouble("humidity"));
						OWMhandler.jsonWeather.put("humidity", OMC.RString("humiditycondition") +
								maininfo.getDouble("humidity") + "%");
						
						maininfo=null;

						// Translate windspeed.
						JSONObject windinfo = stationinfo.getJSONObject("wind");
						double windmps = windinfo.getDouble("speed");
						OWMhandler.jsonWeather.put("wind_speed_mps", windmps);
						OWMhandler.jsonWeather.put("wind_speed_mph", (int)(windmps*2.2369362920544+0.5));
						// Translate wind direction.
						double winddeg = windinfo.getDouble("deg");
						if (winddeg>337.5)
							OWMhandler.jsonWeather.put("wind_direction","N");
						else if (winddeg>292.5)
							OWMhandler.jsonWeather.put("wind_direction","NW");
						else if (winddeg>247.5)
							OWMhandler.jsonWeather.put("wind_direction","W");
						else if (winddeg>202.5)
							OWMhandler.jsonWeather.put("wind_direction","SW");
						else if (winddeg>157.5)
							OWMhandler.jsonWeather.put("wind_direction","S");
						else if (winddeg>112.5)
							OWMhandler.jsonWeather.put("wind_direction","SE");
						else if (winddeg>67.5)
							OWMhandler.jsonWeather.put("wind_direction","E");
						else if (winddeg>22.5)
							OWMhandler.jsonWeather.put("wind_direction","NE");
						else 
							OWMhandler.jsonWeather.put("wind_direction","N");

						OWMhandler.jsonWeather.put("wind_condition", OMC.RString("windcondition") +
								OWMhandler.jsonWeather.optString("wind_direction") + " @ " +
								OWMhandler.jsonWeather.optString("wind_speed_mph") + " mph");

						windinfo=null;

						// Done with station info.
						huc.disconnect();
						
						// Second, Find closest City.
						url = new URL(OpenWeatherMapJSONHandler.URL_CITYSEARCH+latitude+";lon="+longitude);
						huc = (HttpURLConnection) url.openConnection();
						huc.setConnectTimeout(10000);
						huc.setReadTimeout(10000);

						OWMhandler.tempJson = OMC.streamToJSONObject(huc.getInputStream());
						huc.disconnect();

						// Build weather condition.

						//	Parse Cloud Cover:
						//	Cloudy: 90-100%
						//	Mostly cloudy: 70-80%
						//	Partly cloudy or Partly sunny: 30-60%
						//	Mostly clear or Mostly sunny:10-30%
						//	Clear or sunny: 0-10%

						//  Parse Storm/Rain:
						
						//  Parse Snow/Sleet:
						
						// Finally, Find forecast at closest City.
						url = new URL(OpenWeatherMapJSONHandler.URL_CITYFORECAST+latitude+";lon="+longitude);
						huc = (HttpURLConnection) url.openConnection();
						huc.setConnectTimeout(10000);
						huc.setReadTimeout(10000);

						OWMhandler.tempJson = OMC.streamToJSONObject(huc.getInputStream());
						huc.disconnect();
					}

				} catch (Exception e) { 
					e.printStackTrace();
					if (huc!=null) huc.disconnect();
				}
			};
		};
		t.start();

	}

//	public void startDocument() {
//		// yr.no does not return a timestamp in the XML, so default it to 1/1/1970
//		Time t = new Time();
//		// If the weather station information (international, mostly) doesn't have a timestamp, set the timestamp to be jan 1st, 1970
//		t.parse("19700101T000000");
//		try {
//		jsonWeather.putOpt("current_time",
//				t.format2445());
//		jsonWeather.putOpt("current_millis",
//				t.toMillis(false));
//		jsonWeather.putOpt("current_local_time",
//				t.format2445());
//		} catch (Exception e ) {
//			e.printStackTrace();
//		}
//	}

//	public void startElement(String namespaceURI, String localName,
//			String qName, Attributes atts) {
//		try {
//			if (!tree.isEmpty()) {
//
//				// First, parse the forecast time.
//				if (localName.equals("weatherdata")) {
//					UPDATEDTIME.parse(atts.getValue("created"));
//				}
//				
//				if (localName.equals("time")) {
//					if (atts.getValue("datatype").equals("forecast")) {
//						FROMTIME.parse(atts.getValue("from").replace("-","").replace(":",""));
//						FROMTIME.switchTimezone(Time.getCurrentTimezone());
//						TOTIME.parse(atts.getValue("to").replace("-","").replace(":",""));
//						TOTIME.switchTimezone(Time.getCurrentTimezone());
//						LOWDATE.parse(atts.getValue("to").replace("-","").replace(":",""));
//						LOWDATE.switchTimezone(Time.getCurrentTimezone());
//						LOWDATE.hour-=7;
//						LOWDATE.normalize(false);
//					}
//				}
//				
//				if (localName.equals("temperature")) {
//					double tempc = Double.parseDouble(atts.getValue("value"));
//					if (OMC.DEBUG)
//						Log.i(OMC.OMCSHORT + "YrNoWeather",
//								"Temp from " + FROMTIME.format2445() + " to " + TOTIME.format2445() + ":" + tempc);
//					if (jsonWeather.optString("temp_c","missing").equals("missing")) {
//						jsonWeather.putOpt("temp_c",tempc);
//						jsonWeather.putOpt("temp_f",(int)(tempc*9f/5f+32.7f));
//					}
//					String sHTDay =TOTIME.format("%Y%m%d");
//					String sLDDay =LOWDATE.format("%Y%m%d");
//					if (OMC.DEBUG)
//						Log.i(OMC.OMCSHORT + "YrNoWeather",
//								"Day for High: " + sHTDay + "; Day for Low: " + sLDDay);
//					if (!HIGHTEMPS.containsKey(sHTDay)) {
//						HIGHTEMPS.put(sHTDay, tempc);
//					} else {
//						if (tempc>HIGHTEMPS.get(sHTDay)) HIGHTEMPS.put(sHTDay, tempc);
//					}
//					if (!LOWTEMPS.containsKey(sLDDay)) {
//						LOWTEMPS.put(sLDDay, tempc);
//					} else {
//						if (tempc<LOWTEMPS.get(sLDDay)) LOWTEMPS.put(sLDDay, tempc);
//					}
//				}
//
//				if (localName.equals("symbol")) {
//					String cond = CONDITIONTRANSLATIONS[Integer.parseInt(atts.getValue("number"))];
//					if (jsonWeather.optString("condition","missing").equals("missing")) {
//						jsonWeather.putOpt("condition",cond);
//						jsonWeather.putOpt("condition_lcase",cond.toLowerCase());
//					}
//					String sCondDay =TOTIME.format("%Y%m%d");
//					CONDITIONS.put(sCondDay, cond);
//				}
//
//				if (localName.equals("windDirection")) {
//					if (jsonWeather.optString("wind_direction","missing").equals("missing")) {
//						String cond = atts.getValue("name");
//						jsonWeather.putOpt("wind_direction",cond);
//					}
//				}
//				if (localName.equals("windSpeed")) {
//					if (jsonWeather.optString("wind_speed","missing").equals("missing")) {
//						int cond = (int)(Double.parseDouble(atts.getValue("mps"))*2.2369362920544f+0.5f);
//						jsonWeather.putOpt("wind_speed_mps",atts.getValue("mps"));
//						jsonWeather.putOpt("wind_speed_mph",cond);
//					}
//				}
//				if (localName.equals("humidity")) {
//					if (jsonWeather.optString("humidity_raw","missing").equals("missing")) {
//						String cond = atts.getValue("value");
//						jsonWeather.putOpt("humidity_raw",cond);
//					}
//				}
//
//			}
//			tree.push(new String[] { localName });
//
//		} catch (JSONException e) {
//		e.printStackTrace();
//		try {
//			jsonWeather.putOpt("problem_cause", "error");
//		} catch (Exception ee) {}
//	}
//	}

	public void endDocument() {
		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "YrNoWeather", "End Document.");

		

		
		// 
		// Build out the forecast array.
		Time day = new Time();
		day.setToNow();
//		while (HIGHTEMPS.containsKey(day.format("%Y%m%d"))) {
//			try {
//				JSONObject jsonOneDayForecast = new JSONObject();
//				jsonOneDayForecast.put("day_of_week", day.format("%a"));
//				jsonOneDayForecast.put("condition", CONDITIONS.get(day.format("%Y%m%d")));
//				jsonOneDayForecast.put("condition_lcase", CONDITIONS.get(day.format("%Y%m%d")).toLowerCase());
//				double lowc = OMC.roundToSignificantFigures(LOWTEMPS.get(day.format("%Y%m%d")),3);
//				double highc = OMC.roundToSignificantFigures(HIGHTEMPS.get(day.format("%Y%m%d")),3);
//				double lowf = (int)(lowc/5f*9f+32.7f);
//				double highf = (int)(highc/5f*9f+32.7f);
//				jsonOneDayForecast.put("low_c", lowc);
//				jsonOneDayForecast.put("high_c", highc);
//				jsonOneDayForecast.put("low", lowf);
//				jsonOneDayForecast.put("high", highf);
//				jsonWeather.getJSONArray("zzforecast_conditions").put(
//						jsonOneDayForecast);
//				day.hour+=24;
//				day.normalize(false);
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "YrNoWeather", jsonWeather.toString());

		// Check if the reply was valid.
//		if (jsonWeather.optString("condition",null)==null || jsonWeather.optString("problem_cause",null)!=null) {
//			//Google returned error - retry by city name, then abandon refresh
//			if (jsonWeather.optBoolean("bylatlong")) {
//				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "YrNoWeather", "Error using Lat/Long, retrying using city name.");
//				YrNoWeatherXMLHandler.updateWeather(0d, 0d, jsonWeather.optString("country2"), jsonWeather.optString("city2"), false);
//				return;
//			} else {
//				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "YrNoWeather", "Error using city name. No refresh.");
//				return;
//			}
//		}
			
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
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "YrNoWeather", "Update Succeeded.  Phone Time:" + new java.sql.Time(OMC.LASTWEATHERREFRESH).toLocaleString());

		Time t = new Time();
		// If the weather station information (international, mostly) doesn't have a timestamp, set the timestamp to be jan 1st, 1970
		t.parse(jsonWeather.optString("current_local_time","19700101T000000"));
		
		// If the weather station info looks too stale (more than 2 hours old), it's because the phone's date/time is wrong.  
		// Force the update to the default update period
		if (System.currentTimeMillis()-t.toMillis(false)>7200000l) {
			OMC.NEXTWEATHERREFRESH = Math.max(OMC.LASTWEATHERREFRESH + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60")) * 60000l, OMC.LASTWEATHERTRY+Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))/4l*60000l);
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "YrNoWeather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "YrNoWeather", "Weather Station Time Missing or Stale.  Using default interval.");
		} else if (t.toMillis(false)>System.currentTimeMillis()) {
		// If the weather station time is in the future, something is definitely wrong! 
		// Force the update to the default update period
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "YrNoWeather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "YrNoWeather", "Weather Station Time in the future -> phone time is wrong.  Using default interval.");
			OMC.NEXTWEATHERREFRESH = Math.max(OMC.LASTWEATHERREFRESH + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60")) * 60000l, OMC.LASTWEATHERTRY+Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))/4l*60000l);
		} else {
		// If we get a recent weather station timestamp, we try to "catch" the update by setting next update to 
		// 29 minutes + default update period
		// after the last station refresh.

			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "YrNoWeather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
			OMC.NEXTWEATHERREFRESH = Math.max(t.toMillis(false) + (29l + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))) * 60000l, OMC.LASTWEATHERTRY+Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))/4l*60000l);
		}
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "YrNoWeather", "Next Refresh Time:" + new java.sql.Time(OMC.NEXTWEATHERREFRESH).toLocaleString());
		OMC.PREFS.edit().putLong("weather_nextweatherrefresh", OMC.NEXTWEATHERREFRESH).commit();

		
	}

}