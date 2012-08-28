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

public class GoogleReverseGeocodeService {

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

	static public void updateLocation(final Location location) throws Exception {
		JSONObject result;			
		HttpURLConnection huc = null;
		
		try {
			if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
			    System.setProperty("http.keepAlive", "false");
			}
			URL url = new URL("http://maps.googleapis.com/maps/api/geocode/json?latlng="+location.getLatitude()+","+location.getLongitude()+"&sensor=false");
			huc = (HttpURLConnection) url.openConnection();
			huc.setConnectTimeout(10000);
			huc.setReadTimeout(10000);

			result = OMC.streamToJSONObject(huc.getInputStream());
			huc.disconnect();

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
						if (jary2.optJSONObject(counterj).optJSONArray("types").optString(0).equals("sublocality")) {
							city = jary2.optJSONObject(counterj).optString("long_name","Unknown");
						}
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
		} catch (Exception e) {
			if (huc!=null) huc.disconnect();
			throw e;
		}
			
	}
	
}