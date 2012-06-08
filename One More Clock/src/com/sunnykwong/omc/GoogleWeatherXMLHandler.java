package com.sunnykwong.omc;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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

	static public void updateWeather() {
		OMC.LASTWEATHERTRY=System.currentTimeMillis();
		OMC.NEXTWEATHERREFRESH=OMC.LASTWEATHERTRY+15l*60000l;
		OMC.PREFS.edit().putLong("weather_lastweathertry", OMC.LASTWEATHERTRY)
		.putLong("weather_nextweatherrefresh", OMC.NEXTWEATHERREFRESH)
		.commit();
		String sWeatherSetting = OMC.PREFS.getString("weathersetting", "bylatlong");
		if (sWeatherSetting.equals("disabled")) {
        	Log.i(OMC.OMCSHORT + "Weather", "Weather Disabled, no weather update");
			// If weather is disabled (default), do nothing
			return;
		} else if (!OMC.isConnected()) {
			// If phone has no connectivity, do nothing
        	Log.i(OMC.OMCSHORT + "Weather", "No connectivity - no weather update");
			return;
		} else if (sWeatherSetting.equals("bylatlong")) {
			// If weather is disabled (default), do nothing
			GoogleWeatherXMLHandler.updateLocationThenWeather();
			return;
		} else if (sWeatherSetting.equals("specific")) {
			// If weather is disabled (default), do nothing
			GoogleWeatherXMLHandler.updateWeather(OMC.jsonFIXEDLOCN.optDouble("latitude",0d), 
					OMC.jsonFIXEDLOCN.optDouble("longitude",0d), 
					OMC.jsonFIXEDLOCN.optString("country","Unknown"), 
					OMC.jsonFIXEDLOCN.optString("city","Unknown"), true);
			return;
		}
		
	}
	
	  /**
	   * Returns the most accurate and timely previously detected location.
	   * Where the last result is beyond the specified maximum distance or 
	   * latency a one-off location update is returned via the {@link LocationListener}
	   * specified in {@link setChangedLocationListener}.
	   * @param minDistance Minimum distance before we require a location update.
	   * @param minTime Oldest time Acceptable.
	   * @return The most accurate and / or timely previously detected location.
	   * 
	   * CODE BASED ON "Android Protips: A Deep Dive Into Location"... THANKS MR. MEIER!
	   * 
	   */
	  static public void getLastBestLocation(long minTime) {
	    Location bestResult = null;
	    float bestAccuracy = Float.MAX_VALUE;
	    long bestTime = Long.MIN_VALUE;
	    String bestProvider = "";
	    
	    // Iterate through all the providers on the system, keeping
	    // note of the most accurate result within the acceptable time limit.
	    // If no result is found within maxTime, return the newest Location.
	    List<String> matchingProviders = OMC.LM.getAllProviders();
	    for (String provider: matchingProviders) {
	      Location location = OMC.LM.getLastKnownLocation(provider);
	      if (location != null) {
	        float accuracy = location.getAccuracy();
	        long time = location.getTime();
	        
	        if ((time > minTime && accuracy < bestAccuracy)) {
	          bestResult = location;
	          bestAccuracy = accuracy;
	          bestTime = time;
	          bestProvider = provider;
	        }
	        else if (time < minTime && bestAccuracy == Float.MAX_VALUE && time > bestTime) {
	          bestResult = location;
	          bestTime = time;
	          bestProvider = provider;
	        }
	      }
	    }

	    
    	OMC.LL = new LocationListener() {
            public void onLocationChanged(Location location) {
            	if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Using Locn: " + location.getLongitude() + " + " + location.getLatitude());
            	OMC.LM.removeUpdates(OMC.LL);
            	GoogleWeatherXMLHandler.calculateSunriseSunset(location);
            	//GoogleWeatherXMLHandler.updateLocation(location);
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
		    public void onProviderEnabled(String provider) {}
		    public void onProviderDisabled(String provider) {}
		};

		// If the best result is beyond the allowed time limit, or the accuracy of the
	    // best result is wider than the acceptable maximum distance, request a single update.
	    // This check simply implements the same conditions we set when requesting regular
	    // location updates every [minTime] and [minDistance]. 
	    if (bestTime < minTime) { 
			// v134:  Start with network; if it fails, go GPS.
			try {
            	if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Passive failed; Requesting Network Locn.");
				OMC.LM.requestLocationUpdates("network", 0, 0, OMC.LL);
			} catch (IllegalArgumentException e) {
				try {
	            	if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Network failed; Requesting GPS Locn.");
					OMC.LM.requestLocationUpdates("gps", 0, 0, OMC.LL);
				} catch (IllegalArgumentException ee) {
	            	Log.w(OMC.OMCSHORT + "Weather", "Cannot fix location.");
					ee.printStackTrace();
				}
			}
	    } else {
        	if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Using cached location from " + bestProvider + " as of " + new java.sql.Time(bestTime).toLocaleString());
	    	OMC.LL.onLocationChanged(bestResult);
	    }
	  }

	static public void updateLocationThenWeather() {
		//v134:  Get the last best location within the last 90 minutes.
		getLastBestLocation(System.currentTimeMillis()-5400000l);
	}
	
	static public void calculateSunriseSunset(final Location location) {
		//Sunrise Hour Angle in radians.
		Time t = new Time(Time.TIMEZONE_UTC);
		Time t2 = new Time(Time.TIMEZONE_UTC);
		t.setToNow();
		double y = (2d*Math.PI/365d)*(t.yearDay + (t.hour-12d)/24d);
		double eqtime = 229.18*(0.000075+0.001868*Math.cos(y)-0.032077*Math.sin(y)-0.014615*Math.cos(2*y)-0.040849*Math.sin(2*y));
		double declin = 0.006918-0.399912*Math.cos(y)+0.070257*Math.sin(y)-0.006758*Math.cos(2*y)+0.000907*Math.sin(2*y)-0.002697*Math.cos(3*y)+0.00148*Math.sin(3*y);
		double dSunriseHourAngle = Math.acos((Math.cos(1.58533492d) - (Math.sin(location.getLatitude()/ 180d*Math.PI)*Math.sin(declin)))/(Math.cos(location.getLatitude()/ 180d*Math.PI)*Math.cos(declin)))/Math.PI*180d;
		double sunriseminutes = 720 + 4*(location.getLongitude()-dSunriseHourAngle) - eqtime;
		double sunsetminutes = 720 + 4*(location.getLongitude()+dSunriseHourAngle) - eqtime;
		System.out.println("Y: "+y); 
		System.out.println("EQTIME: "+eqtime);
		System.out.println("declin: "+declin* 180d/Math.PI);
		System.out.println("sunrisehrangle: "+dSunriseHourAngle);
		System.out.println("sunriseminutes: "+sunriseminutes);
		System.out.println("sunsetminutes: "+sunsetminutes);
		System.out.println("sunriseminutes: "+(int)(sunriseminutes/60));
		System.out.println("sunriseminutes: "+(int)(sunriseminutes%60));

		t.minute=(int)(sunriseminutes%60);
		t.hour= (int)(sunriseminutes/60);
		t.second = 0;
		t.switchTimezone(Time.getCurrentTimezone());
		System.out.println("Sunrise:" + t.format3339(false)); 
		t2.switchTimezone(Time.getCurrentTimezone());
		t2.minute=(int)(sunsetminutes%60);
		t2.hour= (int)(sunsetminutes/60);
		t2.second = 0;
		System.out.println("Sunset:" + t2.format3339(false));
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
					result = OMC.streamToJSONObject(response.getEntity().getContent());

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
					if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Reverse Geocode: " + city + ", " + country);
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
	
	static public void updateWeather(final double latitude, final double longitude, final String country, final String city, final boolean bylatlong) {
		ELEMENTS = new ArrayList<HashMap<String, String>>();
		Thread t = new Thread() {
			public void run() {
				try {
					XMLReader xr = XMLReaderFactory.createXMLReader();
					GoogleWeatherXMLHandler GXhandler = new GoogleWeatherXMLHandler();
					GXhandler.jsonWeather.putOpt("country2", country);
					GXhandler.jsonWeather.putOpt("city2", city);
					GXhandler.jsonWeather.putOpt("bylatlong", bylatlong);
					xr.setContentHandler(GXhandler);
					xr.setErrorHandler(GXhandler);

					HttpClient client = new DefaultHttpClient();
					HttpGet request = new HttpGet();
					if (!bylatlong) {
						request.setURI(new URI(
								"http://www.google.com/ig/api?oe=utf-8&weather="+city.replace(' ', '+') + "+" + country.replace(' ', '+')));
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
		Time t = new Time();
		t.parse(jsonWeather.optString("current_local_time","19700101T000000"));
		// If the weather information (international, mostly) doesn't have a timestamp, set next update to be
		// 59 minutes from now
		if (t.year<1980) {
			OMC.NEXTWEATHERREFRESH = Math.max(OMC.LASTWEATHERREFRESH + 59l * 60000l, OMC.LASTWEATHERTRY+15l*60000l);
		} else {
		// If we get a weather station timestamp, we try to "catch" the update by setting next update to 89 minutes
		// after the last station refresh.
			OMC.NEXTWEATHERREFRESH = Math.max(t.toMillis(false) + 89l * 60000l, OMC.LASTWEATHERTRY+15l*60000l);
		}
		OMC.PREFS.edit().putLong("weather_nextweatherrefresh", OMC.NEXTWEATHERREFRESH).commit();

		
	}

}