package com.sunnykwong.omc;

import android.util.Log;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.format.Time;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONTokener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.*;

import java.util.ArrayList;
import java.util.Stack;
import java.util.HashMap;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;

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

	static public void updateLocationThenWeather() {
    	OMC.LL = new LocationListener() {
            public void onLocationChanged(Location location) {
            	OMC.LM.removeUpdates(OMC.LL);
            	Log.i(OMC.OMCSHORT + "Weather", "Fixed Locn: " + location.getLongitude() + " + " + location.getLatitude());
            	GoogleWeatherXMLHandler.updateLocation(location);
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
		    public void onProviderEnabled(String provider) {}
		    public void onProviderDisabled(String provider) {}
		};
		OMC.LM.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, OMC.LL);
	}
	
	static public void updateLocation(final Location location) {
		Thread t = new Thread() {
			public void run() {
				JSONObject result;			

				try {
					HttpClient client = new DefaultHttpClient();
					HttpGet request = new HttpGet();
					request.setURI(new URI("http://maps.googleapis.com/maps/api/geocode/json?latlng="+location.getLatitude()+","+location.getLongitude()+"&sensor=false"));
					HttpResponse response = client.execute(request);
					InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());
					BufferedReader br = new BufferedReader(isr,8192);
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null){
						sb.append(line+"\n");
					}
					isr.close();
					br.close();
					result = new JSONObject(sb.toString());
					String city = OMC.LASTKNOWNCITY, country = OMC.LASTKNOWNCOUNTRY;
					if (!result.optString("status").equals("OK")) {
						// Not ok response - do nothing
						city = "Unknown";
						country = "Unknown";
					} else {
						// Find locality
						JSONArray jary = result.optJSONArray("results");
						for (int counter = 0; counter < jary.length(); counter++){
							JSONObject jobj = jary.optJSONObject(counter);
							JSONArray jary2 = jobj.optJSONArray("address_components");
							for (int counterj = 0; counterj < jary2.length(); counterj++){
								if (jary2.optJSONObject(counterj).optJSONArray("types").optString(0).equals("locality")) {
									city = jary2.optJSONObject(counterj).optString("long_name","Unknown");
								}
								if (jary2.optJSONObject(counterj).optJSONArray("types").optString(0).equals("country")) {
									country = jary2.optJSONObject(counterj).optString("long_name","Unknown");
								}
								counterj++;
							}
						}
					}
					Log.i(OMC.OMCSHORT + "Weather", "Reverse Geocode: " + city + ", " + country);
					OMC.LASTKNOWNCITY=city;
					OMC.LASTKNOWNCOUNTRY=country;
					GoogleWeatherXMLHandler.updateWeather(location.getLatitude(), location.getLongitude(), country, city, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
		};
		t.start();
	}
	
	static public void updateWeather() {
		String sWeatherSetting = OMC.PREFS.getString("weathersetting", "bylatlong");
		if (sWeatherSetting.equals("disabled")) {
			// If weather is disabled (default), do nothing
			return;
		} else if (sWeatherSetting.equals("bylatlong")) {
			// If weather is disabled (default), do nothing
			GoogleWeatherXMLHandler.updateLocationThenWeather();
			return;
		} else if (sWeatherSetting.equals("specific")) {
			// If weather is disabled (default), do nothing
			GoogleWeatherXMLHandler.updateWeather(0d, 0d, "", OMC.PREFS.getString("weathercity", "Unknown"), false);
			return;
		}
		
	}
	
	static public void updateWeather(final double latitude, final double longitude, final String country, final String city, final boolean bylatlong) {
		ELEMENTS = new ArrayList<HashMap<String, String>>();
		Thread t = new Thread() {
			public void run() {
				OMC.LASTWEATHERTRY=System.currentTimeMillis();
				try {
					XMLReader xr = XMLReaderFactory.createXMLReader();
					GoogleWeatherXMLHandler GXhandler = new GoogleWeatherXMLHandler();
					GXhandler.jsonWeather.putOpt("country2", country);
					GXhandler.jsonWeather.putOpt("city2", city);
					xr.setContentHandler(GXhandler);
					xr.setErrorHandler(GXhandler);

					HttpClient client = new DefaultHttpClient();
					HttpGet request = new HttpGet();
					if (!bylatlong) {
						request.setURI(new URI(
								"http://www.google.com/ig/api?oe=utf-8&weather="+city));
					} else {
						request.setURI(new URI(
								"http://www.google.com/ig/api?oe=utf-8&weather=,,,"+(long)(latitude*1000000)+","+(long)(longitude*1000000)));
					}
					HttpResponse response = client.execute(request);

					xr.parse(new InputSource(response.getEntity().getContent()));
				} catch (Exception e) {
					e.printStackTrace();
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
		if (!tree.isEmpty()) {

			if (tree.peek()[0].equals("forecast_information")
					|| tree.peek()[0].equals("current_conditions")) {
				String sData = atts.getValue("data");
				if (OMC.DEBUG)
					Log.i(OMC.OMCSHORT + "Weather", "Reading " + tree.peek()[0]
							+ " - " + localName);
				try {
					jsonWeather.putOpt(localName, sData);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (localName.equals("current_date_time")){
					String timeString = sData.substring(0, 19).replace(":","").replace("-","").replace(' ','T')+"Z";
					Time tCurrentTime = new Time(Time.TIMEZONE_UTC);
					tCurrentTime.parse(timeString);
					try {
						jsonWeather.putOpt("current_time", tCurrentTime.format2445());
						jsonWeather.putOpt("current_millis", tCurrentTime.toMillis(false));
						tCurrentTime.switchTimezone(Time.getCurrentTimezone());
						jsonWeather.putOpt("current_local_time", tCurrentTime.format2445());
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			} else if (tree.peek()[0].equals("forecast_conditions")) {
				if (OMC.DEBUG)
					Log.i(OMC.OMCSHORT + "Weather", "Reading " + tree.peek()[0]
							+ " - " + localName);
				if (jsonOneDayForecast == null)
					jsonOneDayForecast = new JSONObject();
				try {
					jsonOneDayForecast.putOpt(localName, atts.getValue("data"));
					if (localName.equals("low") || localName.equals("high")) {
						int tempC = (int)((Float.parseFloat(atts.getValue("data"))-32.2f)*5f/9f);
						jsonOneDayForecast.putOpt(localName+"_c", tempC);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

		}
		tree.push(new String[] { localName });
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
		try {
			if (jsonWeather.optString("city")==null || jsonWeather.optString("city").equals("")) {
				if (!jsonWeather.optString("city2").equals(""))
					jsonWeather.putOpt("city", jsonWeather.optString("city2"));
				else if (!jsonWeather.optString("country2").equals(""))
					jsonWeather.putOpt("city", jsonWeather.optString("country2"));
			}
			
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		OMC.PREFS.edit().putString("weather", jsonWeather.toString()).commit();
		OMC.LASTWEATHERREFRESH=System.currentTimeMillis();
	}

}