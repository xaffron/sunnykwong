package com.sunnykwong.omc;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;

public class GoogleWeatherXMLHandler extends DefaultHandler {

	public Stack<String[]> tree;
	public HashMap<String, String> element;
	public JSONObject jsonWeather, jsonOneDayForecast;
	public static final String DELIMITERS = " .,;-";
	public static ArrayList<HashMap<String, String>> ELEMENTS;

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
		ELEMENTS = new ArrayList<HashMap<String, String>>();
		Thread t = new Thread() {
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
					huc.setConnectTimeout(10000);
					huc.setReadTimeout(10000);

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

	public void startDocument() {
		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "Weather", "Start Building JSON.");
	}

	public void characters(char[] ch, int start, int length) {
		// Google Weather doesn't return data between tags, so nothing here
	}

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
						int tempC = (int) ((Float.parseFloat(sData) - 32.2f) * 5f / 9f);
						jsonOneDayForecast.putOpt(localName + "_c", tempC);
					} else if (localName.equals("condition")) {
						jsonOneDayForecast.putOpt("condition_lcase", sData.toLowerCase());
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