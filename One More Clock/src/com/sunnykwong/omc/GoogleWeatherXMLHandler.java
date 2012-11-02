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

public class GoogleWeatherXMLHandler extends DefaultHandler {

	public Stack<String[]> tree;
	public HashMap<String, String> element;
	public JSONObject jsonWeather, jsonOneDayForecast;
	public static final String DELIMITERS = " .,;-";
	public static ArrayList<HashMap<String, String>> ELEMENTS;
	public static HashMap<String,Integer> CONDITIONTRANSLATIONS;

	public GoogleWeatherXMLHandler() {
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

		// Populating the condition translations
		CONDITIONTRANSLATIONS = new HashMap<String,Integer>();
		CONDITIONTRANSLATIONS.put("clear",1);
		CONDITIONTRANSLATIONS.put("sunny",2);
		CONDITIONTRANSLATIONS.put("mostly sunny",3);
		CONDITIONTRANSLATIONS.put("partly sunny",4);
		CONDITIONTRANSLATIONS.put("partly cloudy",5);
		CONDITIONTRANSLATIONS.put("dust",6);
		CONDITIONTRANSLATIONS.put("haze",7);
		CONDITIONTRANSLATIONS.put("mist",8);
		CONDITIONTRANSLATIONS.put("smoke",9);
		CONDITIONTRANSLATIONS.put("mostly cloudy",10);
		CONDITIONTRANSLATIONS.put("overcast",11);
		CONDITIONTRANSLATIONS.put("cloudy",12);
		CONDITIONTRANSLATIONS.put("fog",13);
		CONDITIONTRANSLATIONS.put("light rain",14);
		CONDITIONTRANSLATIONS.put("rain showers",15);
		CONDITIONTRANSLATIONS.put("drizzle",16);
		CONDITIONTRANSLATIONS.put("showers",17);
		CONDITIONTRANSLATIONS.put("chance of rain",18);
		CONDITIONTRANSLATIONS.put("chance of showers",19);
		CONDITIONTRANSLATIONS.put("scattered showers",20);
		CONDITIONTRANSLATIONS.put("thunderstorm",21);
		CONDITIONTRANSLATIONS.put("chance of tstorm",22);
		CONDITIONTRANSLATIONS.put("scattered thunderstorms",23);
		CONDITIONTRANSLATIONS.put("chance of storm",24);
		CONDITIONTRANSLATIONS.put("storm",25);
		CONDITIONTRANSLATIONS.put("heavy rain",26);
		CONDITIONTRANSLATIONS.put("rain",27);
		CONDITIONTRANSLATIONS.put("flurries",28);
		CONDITIONTRANSLATIONS.put("light snow",29);
		CONDITIONTRANSLATIONS.put("chance of snow",30);
		CONDITIONTRANSLATIONS.put("snow showers",31);
		CONDITIONTRANSLATIONS.put("scattered snow showers",32);
		CONDITIONTRANSLATIONS.put("snow",33);
		CONDITIONTRANSLATIONS.put("icy",34);
		CONDITIONTRANSLATIONS.put("ice/snow",35);
		CONDITIONTRANSLATIONS.put("sleet",36);
		CONDITIONTRANSLATIONS.put("freezing drizzle",37);
		CONDITIONTRANSLATIONS.put("rain and snow",38);
		CONDITIONTRANSLATIONS.put("windy",39);
		
		ELEMENTS = new ArrayList<HashMap<String, String>>();
		Thread t = new Thread() {
			@Override
			public void run() {
				HttpURLConnection huc = null; 
				try {
					if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
					    System.setProperty("http.keepAlive", "false");
					}
					XMLReader xr = XMLReaderFactory.createXMLReader();
					GoogleWeatherXMLHandler GXhandler = new GoogleWeatherXMLHandler();
					GXhandler.jsonWeather.putOpt("country2", country);
					GXhandler.jsonWeather.putOpt("city2", city);
					GXhandler.jsonWeather.putOpt("bylatlong", bylatlong);
					xr.setContentHandler(GXhandler);
					xr.setErrorHandler(GXhandler);

					URL url=null;
					if (!bylatlong) {
						url = new URL("http://www.google.com/ig/api?oe=utf-8&weather="+city.replace(' ', '+') + "+" + country.replace(' ', '+'));
					} else {
						url = new URL("http://www.google.com/ig/api?oe=utf-8&weather=,,,"+(long)(latitude*1000000)+","+(long)(longitude*1000000));
					}
					huc = (HttpURLConnection) url.openConnection();
					huc.setConnectTimeout(30000);
					huc.setReadTimeout(30000);

					xr.parse(new InputSource(huc.getInputStream())); 
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
			Log.i(OMC.OMCSHORT + "Weather", "Start Building JSON.");
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		// Google Weather doesn't return data between tags, so nothing here
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) {
		try {
			if (!tree.isEmpty()) {

				if (tree.peek()[0].equals("forecast_information")
						|| tree.peek()[0].equals("current_conditions")) {
					String sData = atts.getValue("data");
					if (OMC.DEBUG)
						Log.i(OMC.OMCSHORT + "Weather",
								"Reading " + tree.peek()[0] + " - " + localName + ": " + sData);
					jsonWeather.putOpt(localName, sData);
					if (localName.equals("current_date_time")) {
						String timeString = sData.substring(0, 19)
								.replace(":", "").replace("-", "")
								.replace(' ', 'T')
								+ "Z";
						Time tCurrentTime = new Time(Time.TIMEZONE_UTC);
						tCurrentTime.parse(timeString);
						jsonWeather.putOpt("current_time",
								tCurrentTime.format2445());
						jsonWeather.putOpt("current_millis",
								tCurrentTime.toMillis(false));
						tCurrentTime.switchTimezone(Time.getCurrentTimezone());
						jsonWeather.putOpt("current_local_time",
								tCurrentTime.format2445());
					} else if (localName.equals("condition")) {
						jsonWeather.putOpt("condition_lcase",
								sData.toLowerCase());
						int iConditionCode=0;
						if (CONDITIONTRANSLATIONS.containsKey(sData.toLowerCase()))
								iConditionCode = CONDITIONTRANSLATIONS.get(sData.toLowerCase());
						jsonWeather.putOpt("condition_code", iConditionCode);
					} 

				} else if (tree.peek()[0].equals("forecast_conditions")) {
					String sData = atts.getValue("data");
					if (OMC.DEBUG)
						Log.i(OMC.OMCSHORT + "Weather",
								"Reading " + tree.peek()[0] + " - " + localName + ": " + sData);
					if (jsonOneDayForecast == null)
						jsonOneDayForecast = new JSONObject();
					jsonOneDayForecast.putOpt(localName, sData);
					if (localName.equals("low") || localName.equals("high")) {
						int tempC = (int) ((Float.parseFloat(sData) - 32f) * 5f / 9f);
						jsonOneDayForecast.putOpt(localName + "_c", tempC);
					} else if (localName.equals("condition")) {
						jsonOneDayForecast.putOpt("condition_lcase", sData.toLowerCase());
						int iConditionCode=0;
						if (CONDITIONTRANSLATIONS.containsKey(sData.toLowerCase()))
								iConditionCode = CONDITIONTRANSLATIONS.get(sData.toLowerCase());
						jsonOneDayForecast.putOpt("condition_code", iConditionCode);
					}
				} else if (localName.equals("problem_cause")) {
					if (OMC.DEBUG)
						Log.i(OMC.OMCSHORT + "Weather",
								"Google Weather returned error.");
					jsonWeather.putOpt(localName, "error");
				}
			} else {
				jsonWeather.putOpt(localName, atts.getValue("data"));
			}
			tree.push(new String[] { localName });
		} catch (JSONException e) {
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
		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "Weather", "EndElement." + name);
		if (name.equals("forecast_conditions")) {
			try {
				jsonWeather.getJSONArray("zzforecast_conditions").put(
						jsonOneDayForecast);
				jsonOneDayForecast = null;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
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
			Log.i(OMC.OMCSHORT + "Weather", jsonWeather.toString());
		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "Weather", "End Document.");
		// OK we're done parsing the whole document.
		// Since the parse() method is synchronous, we don't need to do anything
		// - just basic cleanup.
		tree.clear();
		tree = null;
		
		// Check if the reply was valid.
		if (jsonWeather.optString("condition",null)==null || jsonWeather.optString("problem_cause",null)!=null) {
			//Google returned error - retry by city name, then abandon refresh
			if (jsonWeather.optBoolean("bylatlong")) {
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Error using Lat/Long, retrying using city name.");
				GoogleWeatherXMLHandler.updateWeather(0d, 0d, jsonWeather.optString("country2"), jsonWeather.optString("city2"), false);
				return;
			} else {
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Error using city name. No refresh.");
				return;
			}
		}
			
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
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Update Succeeded.  Phone Time:" + new java.sql.Time(OMC.LASTWEATHERREFRESH).toLocaleString());

		Time t = new Time();
		// If the weather station information (international, mostly) doesn't have a timestamp, set the timestamp to be jan 1st, 1970
		t.parse(jsonWeather.optString("current_local_time","19700101T000000"));
		
		// If the weather station info looks too stale (more than 2 hours old), it's because the phone's date/time is wrong.  
		// Force the update to the default update period
		if (System.currentTimeMillis()-t.toMillis(false)>7200000l) {
			OMC.NEXTWEATHERREFRESH = Math.max(OMC.LASTWEATHERREFRESH + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60")) * 60000l, OMC.LASTWEATHERTRY+Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))/4l*60000l);
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Weather Station Time Missing or Stale.  Using default interval.");
		} else if (t.toMillis(false)>System.currentTimeMillis()) {
		// If the weather station time is in the future, something is definitely wrong! 
		// Force the update to the default update period
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Weather Station Time in the future -> phone time is wrong.  Using default interval.");
			OMC.NEXTWEATHERREFRESH = Math.max(OMC.LASTWEATHERREFRESH + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60")) * 60000l, OMC.LASTWEATHERTRY+Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))/4l*60000l);
		} else {
		// If we get a recent weather station timestamp, we try to "catch" the update by setting next update to 
		// 29 minutes + default update period
		// after the last station refresh.

			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Weather Station Time:" + new java.sql.Time(t.toMillis(false)).toLocaleString());
			OMC.NEXTWEATHERREFRESH = Math.max(t.toMillis(false) + (29l + Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))) * 60000l, OMC.LASTWEATHERTRY+Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))/4l*60000l);
		}
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Next Refresh Time:" + new java.sql.Time(OMC.NEXTWEATHERREFRESH).toLocaleString());
		OMC.PREFS.edit().putLong("weather_nextweatherrefresh", OMC.NEXTWEATHERREFRESH).commit();

		
	}

}