package com.sunnykwong.omc;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
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

	static public String updateLocation(final Location location) throws Exception {
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

//			result = OMC.streamToJSONObject(huc.getInputStream());
			huc.disconnect();
			
			result = new JSONObject("{\"results\":[{\"types\":[\"route\"],\"formatted_address\":\"Nikesite Walkway, Chalfont, PA 18914, USA\",\"address_components\":[{\"types\":[\"route\"],\"short_name\":\"Nikesite Walkway\",\"long_name\":\"Nikesite Walkway\"},{\"types\":[\"locality\",\"political\"],\"short_name\":\"Chalfont\",\"long_name\":\"Chalfont\"},{\"types\":[\"administrative_area_level_3\",\"political\"],\"short_name\":\"Warrington\",\"long_name\":\"Warrington\"},{\"types\":[\"administrative_area_level_2\",\"political\"],\"short_name\":\"Bucks\",\"long_name\":\"Bucks\"},{\"types\":[\"administrative_area_level_1\",\"political\"],\"short_name\":\"PA\",\"long_name\":\"Pennsylvania\"},{\"types\":[\"country\",\"political\"],\"short_name\":\"US\",\"long_name\":\"United States\"},{\"types\":[\"postal_code\"],\"short_name\":\"18914\",\"long_name\":\"18914\"}],\"geometry\":{\"bounds\":{\"southwest\":{\"lng\":-75.1771528,\"lat\":40.2429553},\"northeast\":{\"lng\":-75.1732984,\"lat\":40.246704}},\"viewport\":{\"southwest\":{\"lng\":-75.1771528,\"lat\":40.2429553},\"northeast\":{\"lng\":-75.1732984,\"lat\":40.246704}},\"location\":{\"lng\":-75.1748858,\"lat\":40.2433947},\"location_type\":\"APPROXIMATE\"}},{\"types\":[\"administrative_area_level_3\",\"political\"],\"formatted_address\":\"Warrington, PA, USA\",\"address_components\":[{\"types\":[\"administrative_area_level_3\",\"political\"],\"short_name\":\"Warrington\",\"long_name\":\"Warrington\"},{\"types\":[\"administrative_area_level_2\",\"political\"],\"short_name\":\"Bucks\",\"long_name\":\"Bucks\"},{\"types\":[\"administrative_area_level_1\",\"political\"],\"short_name\":\"PA\",\"long_name\":\"Pennsylvania\"},{\"types\":[\"country\",\"political\"],\"short_name\":\"US\",\"long_name\":\"United States\"}],\"geometry\":{\"bounds\":{\"southwest\":{\"lng\":-75.2141328,\"lat\":40.210886},\"northeast\":{\"lng\":-75.11099279999999,\"lat\":40.2831289}},\"viewport\":{\"southwest\":{\"lng\":-75.2141328,\"lat\":40.210886},\"northeast\":{\"lng\":-75.11099279999999,\"lat\":40.2831289}},\"location\":{\"lng\":-75.1662121,\"lat\":40.250319},\"location_type\":\"APPROXIMATE\"}},{\"types\":[\"postal_code\"],\"formatted_address\":\"Chalfont, PA 18914, USA\",\"address_components\":[{\"types\":[\"postal_code\"],\"short_name\":\"18914\",\"long_name\":\"18914\"},{\"types\":[\"locality\",\"political\"],\"short_name\":\"Chalfont\",\"long_name\":\"Chalfont\"},{\"types\":[\"administrative_area_level_1\",\"political\"],\"short_name\":\"PA\",\"long_name\":\"Pennsylvania\"},{\"types\":[\"country\",\"political\"],\"short_name\":\"US\",\"long_name\":\"United States\"}],\"geometry\":{\"bounds\":{\"southwest\":{\"lng\":-75.26018599999999,\"lat\":40.2286608},\"northeast\":{\"lng\":-75.1513329,\"lat\":40.348218}},\"viewport\":{\"southwest\":{\"lng\":-75.26018599999999,\"lat\":40.2286608},\"northeast\":{\"lng\":-75.1513329,\"lat\":40.348218}},\"location\":{\"lng\":-75.2128996,\"lat\":40.2764064},\"location_type\":\"APPROXIMATE\"}},{\"types\":[\"administrative_area_level_2\",\"political\"],\"formatted_address\":\"Bucks, PA, USA\",\"address_components\":[{\"types\":[\"administrative_area_level_2\",\"political\"],\"short_name\":\"Bucks\",\"long_name\":\"Bucks\"},{\"types\":[\"administrative_area_level_1\",\"political\"],\"short_name\":\"PA\",\"long_name\":\"Pennsylvania\"},{\"types\":[\"country\",\"political\"],\"short_name\":\"US\",\"long_name\":\"United States\"}],\"geometry\":{\"bounds\":{\"southwest\":{\"lng\":-75.48405679999999,\"lat\":40.0526851},\"northeast\":{\"lng\":-74.7236799,\"lat\":40.6085799}},\"viewport\":{\"southwest\":{\"lng\":-75.48405679999999,\"lat\":40.0526851},\"northeast\":{\"lng\":-74.7236799,\"lat\":40.6085799}},\"location\":{\"lng\":-75.2479061,\"lat\":40.4107964},\"location_type\":\"APPROXIMATE\"}},{\"types\":[\"administrative_area_level_1\",\"political\"],\"formatted_address\":\"Pennsylvania, USA\",\"address_components\":[{\"types\":[\"administrative_area_level_1\",\"political\"],\"short_name\":\"PA\",\"long_name\":\"Pennsylvania\"},{\"types\":[\"country\",\"political\"],\"short_name\":\"US\",\"long_name\":\"United States\"}],\"geometry\":{\"bounds\":{\"southwest\":{\"lng\":-80.51989499999999,\"lat\":39.7197989},\"northeast\":{\"lng\":-74.6895018,\"lat\":42.26936509999999}},\"viewport\":{\"southwest\":{\"lng\":-80.51989499999999,\"lat\":39.7197989},\"northeast\":{\"lng\":-74.6895018,\"lat\":42.26936509999999}},\"location\":{\"lng\":-77.1945247,\"lat\":41.2033216},\"location_type\":\"APPROXIMATE\"}},{\"types\":[\"country\",\"political\"],\"formatted_address\":\"United States\",\"address_components\":[{\"types\":[\"country\",\"political\"],\"short_name\":\"US\",\"long_name\":\"United States\"}],\"geometry\":{\"bounds\":{\"southwest\":{\"lng\":172.4546966,\"lat\":18.9110642},\"northeast\":{\"lng\":-66.94976079999999,\"lat\":71.389888}},\"viewport\":{\"southwest\":{\"lng\":-124.39,\"lat\":25.82},\"northeast\":{\"lng\":-66.94,\"lat\":49.38}},\"location\":{\"lng\":-95.712891,\"lat\":37.09024},\"location_type\":\"APPROXIMATE\"}}],\"status\":\"OK\"}");

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
						for (int iType=0;iType<jary2.optJSONObject(counterj).optJSONArray("types").length();iType++) {
							if (jary2.optJSONObject(counterj).optJSONArray("types").optString(iType).equals("sublocality")) {
								city = jary2.optJSONObject(counterj).optString("long_name","Unknown");
							}
							if (jary2.optJSONObject(counterj).optJSONArray("types").optString(iType).equals("locality")) {
								city = jary2.optJSONObject(counterj).optString("long_name","Unknown");
							}
							if (jary2.optJSONObject(counterj).optJSONArray("types").optString(iType).equals("country")) {
								country = jary2.optJSONObject(counterj).optString("long_name","Unknown");
							}
						}
					}
				}
			}
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Reverse Geocode: " + city + ", " + country);
			OMC.LASTKNOWNCITY=city;
			OMC.LASTKNOWNCOUNTRY=country;
			return result.toString();
		} catch (Exception e) {
			if (huc!=null) huc.disconnect();
			throw e;
		}
			
	}
	
}