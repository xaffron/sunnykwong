package com.sunnykwong.omc;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import android.os.Build;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;

public class NOAAWeatherXMLHandler extends DefaultHandler {

	public static final String URL_NOAAFORECAST = "http://forecast.weather.gov/MapClick.php?unit=0&lg=english&FcstType=dwml&lat=";
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
	
	public Stack<String[]> tree;
	public HashMap<String, String> element;
	public JSONObject jsonWeather, jsonOneDayForecast;
	public static final String DELIMITERS = " .,;-";
	public static final Time FROMTIME=new Time(Time.TIMEZONE_UTC);
	public static final Time TOTIME=new Time(Time.TIMEZONE_UTC);
	public static final Time LOWDATE= new Time(Time.TIMEZONE_UTC);
	public static final Time HIGHDATE= new Time(Time.TIMEZONE_UTC);
	public static final Time UPDATEDTIME= new Time(Time.TIMEZONE_UTC);
	public static ArrayList<Double> HIGHTEMPS;
	public static ArrayList<Double> LOWTEMPS;
	public static ArrayList<String> CONDITIONS;
	public static ArrayList<Double> TEMPTOUPDATE;
	public static String VALUETYPE;

	public NOAAWeatherXMLHandler() {
		super();
		if (tree == null)
			tree = new Stack<String[]>();
		tree.clear();
		if (element == null)
			element = new HashMap<String, String>();
		element.clear();
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
					XMLReader xr = XMLReaderFactory.createXMLReader();
					NOAAWeatherXMLHandler GXhandler = new NOAAWeatherXMLHandler();
					GXhandler.jsonWeather.putOpt("country2", country);
					GXhandler.jsonWeather.putOpt("city2", city);
					GXhandler.jsonWeather.putOpt("bylatlong", bylatlong);
					GXhandler.jsonWeather.putOpt("longitude_e6",longitude*1000000d);
					GXhandler.jsonWeather.putOpt("latitude_e6",latitude*1000000d);
					xr.setContentHandler(GXhandler);
					xr.setErrorHandler(GXhandler);

					URL url=null;
					if (!bylatlong) {
						Log.e(OMC.OMCSHORT + "NOAAWeather", "NOAA handler does not support weather by location name!");
						return;
//						url = new URL("http://www.google.com/ig/api?oe=utf-8&weather="+city.replace(' ', '+') + "+" + country.replace(' ', '+'));
					} else {
						url = new URL(NOAAWeatherXMLHandler.URL_NOAAFORECAST+latitude+"&lon="+longitude);
					}
					huc = (HttpURLConnection) url.openConnection();
					huc.setConnectTimeout(30000);
					huc.setReadTimeout(30000);

					xr.parse(new InputSource(huc.getInputStream()));
					UPDATEDTIME.set(huc.getDate());
					huc.disconnect();

				} catch (Exception e) { 
					e.printStackTrace();
					if (huc!=null) huc.disconnect();
				}
			};
		};
		t.start();

	}

	@Override
	public void startDocument() {
		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "NOAAWeather", "Start Building JSON.");
		// We haven't accumulated current conditions yet
		LOWTEMPS=new ArrayList<Double>();
		HIGHTEMPS=new ArrayList<Double>();
		CONDITIONS = new ArrayList<String>();
		VALUETYPE = new String();
		
		// yr.no does not return a timestamp in the XML, so default it to 1/1/1970
		Time t = new Time();
		// If the weather station information (international, mostly) doesn't have a timestamp, set the timestamp to be jan 1st, 1970
		t.parse("19700101T000000");
		try {
		jsonWeather.putOpt("current_time",
				t.format2445());
		jsonWeather.putOpt("current_millis",
				t.toMillis(false));
		jsonWeather.putOpt("current_local_time",
				t.format2445());
		} catch (Exception e ) {
			e.printStackTrace();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		String value = new String(ch);
		System.out.println (tree.peek()[0] + "-" + value);
		if (tree.peek()[0].equals("temperature")) {
			NOAAWeatherXMLHandler.TEMPTOUPDATE.add(Double.parseDouble(value));
			return;
		}
		if (tree.peek()[0].equals("weather")) {
			NOAAWeatherXMLHandler.CONDITIONS.add(value);
			return;
		}
		if (tree.peek()[0].equals("value")){
			if (VALUETYPE.equals("")) return;
			try {
				jsonWeather.put(VALUETYPE, value);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) {
		try {
			VALUETYPE="";
			if (!tree.isEmpty()) {
				if (localName.equals("temperature")) {
					if (atts.getValue("type").equals("maximum"))
						NOAAWeatherXMLHandler.TEMPTOUPDATE = HIGHTEMPS;
					else if (atts.getValue("type").equals("minimum"))
						NOAAWeatherXMLHandler.TEMPTOUPDATE = LOWTEMPS;
					else if (atts.getValue("type").equals("apparent"))
						VALUETYPE="temp_f";
				}
				if (localName.equals("humidity")) {
					if (atts.getValue("type").equals("relative"))
						VALUETYPE="humidity_raw";
				}
				if (localName.equals("direction") && atts.getValue("type").equals("wind")) {
					VALUETYPE="wind_direction";
				}
				if (localName.equals("wind-speed") && atts.getValue("type").equals("sustained")) {
					VALUETYPE="wind_speed_knots";
				}
				if (localName.equals("weather-conditions") && atts.getValue("weather-summary")!=null) {
					jsonWeather.put("condition", atts.getValue("weather-summary"));
					jsonWeather.put("condition_lcase", atts.getValue("weather-summary").toLowerCase());
				}
			}
			tree.push(new String[] { localName });

		} catch (Exception e) {
		e.printStackTrace();
		try {
			jsonWeather.putOpt("problem_cause", "error");
		} catch (Exception ee) {}
	}
	}

	@Override
	public void endElement(String uri, String name, String qName) {
		if (tree.isEmpty())
			return;
		// Pop the stack.
		tree.pop();

	}

	public boolean isNumber(String s) {
		for (int i = 0; i < s.length(); i++) {

			// If we find a non-digit character we return false.
			if (!Character.isDigit(s.charAt(i)))
				return false;
		}
		return true;
	}

	@Override
	public void endDocument() {
		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "NOAAWeather", "End Document.");
		// OK we're done parsing the whole document.
		// Since the parse() method is synchronous, we don't need to do anything
		// - just basic cleanup.
		tree.clear();
		tree = null;
		
		// Build out wind/humidity conditions.
		String humidityString = OMC.RString("humiditycondition") +
				jsonWeather.optString("humidity_raw") + "%";
		
		double dWindSpeedKnots = jsonWeather.optDouble("wind_speed_knots");
		int iWindSpeedMps = (int)(dWindSpeedKnots*0.514+0.5);
		int iWindSpeedMph = (int)(dWindSpeedKnots*1.151+0.5);
		String windString = OMC.RString("windcondition") +
				jsonWeather.optString("wind_direction") + " @ " +
				iWindSpeedMph + " mph";
		try {
			jsonWeather.putOpt("humidity", humidityString);
			jsonWeather.putOpt("wind_condition", windString);
			jsonWeather.putOpt("wind_speed_mph", iWindSpeedMph);
			jsonWeather.putOpt("wind_speed_mps", iWindSpeedMps);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// 
		// Build out the forecast array.
		Time day = new Time();
		day.setToNow();

		int iLowInit=0, iHighInit=0;
//		if (LOWTEMPS.size()==HIGHTEMPS.size()) {
//			try {
//				JSONObject jsonOneDayForecast = new JSONObject();
//				jsonOneDayForecast.put("day_of_week", day.format("%a"));
//				jsonOneDayForecast.put("condition",
//						CONDITIONS[0];
//				jsonOneDayForecast.put("condition_lcase",
//						CONDITIONS.get(day.format("%Y%m%d")).toLowerCase());
//				double lowc = OMC.roundToSignificantFigures(
//						LOWTEMPS.get(day.format("%Y%m%d")), 3);
//				double highc = OMC.roundToSignificantFigures(
//						HIGHTEMPS.get(day.format("%Y%m%d")), 3);
//				double lowf = (int) (lowc / 5f * 9f + 32.7f);
//				double highf = (int) (highc / 5f * 9f + 32.7f);
//				jsonOneDayForecast.put("low_c", lowc);
//				jsonOneDayForecast.put("high_c", highc);
//				jsonOneDayForecast.put("low", lowf);
//				jsonOneDayForecast.put("high", highf);
//				jsonWeather.getJSONArray("zzforecast_conditions").put(
//						jsonOneDayForecast);
//				day.hour += 24;
//				day.normalize(false);
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
		// Make sure today's data contains both high and low, too.
//		if (!LOWTEMPS.containsKey(day.format("%Y%m%d"))) {
//			Time nextday = new Time(day);
//			nextday.hour+=24;
//			nextday.normalize(false);
//			LOWTEMPS.put(day.format("%Y%m%d"), LOWTEMPS.get(nextday.format("%Y%m%d")));
//		}
//		if (!HIGHTEMPS.containsKey(day.format("%Y%m%d"))) {
//			HIGHTEMPS.put(day.format("%Y%m%d"), LOWTEMPS.get(day.format("%Y%m%d")));
//		}
//
//		while (HIGHTEMPS.containsKey(day.format("%Y%m%d")) && LOWTEMPS.containsKey(day.format("%Y%m%d"))) {
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
			Log.i(OMC.OMCSHORT + "NOAAWeather", jsonWeather.toString());

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
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "NOAAWeather", "Update Succeeded.  Phone Time:" + new java.sql.Time(OMC.LASTWEATHERREFRESH).toLocaleString());

		Time t = new Time();
		// If the weather station information (international, mostly) doesn't have a timestamp, set the timestamp to be jan 1st, 1970
		t.parse(jsonWeather.optString("current_local_time","19700101T000000"));
		
		// If the weather station info looks too stale (more than 2 hours old), it's because the phone's date/time is wrong.  
		// Force the update to the default update period
		if (System.currentTimeMillis()-t.toMillis(false)>7200000l) {
			OMC.NEXTWEATHERREFRESH = Math.max(OMC.LASTWEATHERREFRESH + (1l + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))) * 60000l, 
					OMC.LASTWEATHERTRY+(1l + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60")))/4l*60000l);
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "NOAAWeather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "NOAAWeather", "Weather Station Time Missing or Stale.  Using default interval.");
		} else if (t.toMillis(false)>System.currentTimeMillis()) {
		// If the weather station time is in the future, something is definitely wrong! 
		// Force the update to the default update period
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "NOAAWeather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "NOAAWeather", "Weather Station Time in the future -> phone time is wrong.  Using default interval.");
			OMC.NEXTWEATHERREFRESH = Math.max(OMC.LASTWEATHERREFRESH + (1l + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))) * 60000l, 
					OMC.LASTWEATHERTRY+(1l + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60")))/4l*60000l);
		} else {
		// If we get a recent weather station timestamp, we try to "catch" the update by setting next update to 
		// 29 minutes + default update period
		// after the last station refresh.

			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "NOAAWeather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
			OMC.NEXTWEATHERREFRESH = Math.max(t.toMillis(false) + 
					(29l + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))) * 60000l, 
					OMC.LASTWEATHERTRY+Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))/4l*60000l);
		}
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "NOAAWeather", "Next Refresh Time:" + new java.sql.Time(OMC.NEXTWEATHERREFRESH).toLocaleString());
		OMC.PREFS.edit().putLong("weather_nextweatherrefresh", OMC.NEXTWEATHERREFRESH).commit();

		
	}

}