package com.sunnykwong.omc;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.text.format.Time;
import android.util.Log;

public class GoogleReverseGeocodeService {

	/**
	 * Returns the most accurate and timely previously detected location. Where
	 * the last result is beyond the specified maximum distance or latency a
	 * one-off location update is returned via the {@link LocationListener}
	 * specified in {@link setChangedLocationListener}.
	 * 
	 * @param minTime
	 *            Oldest time Acceptable.
	 * @return The most accurate and / or timely previously detected location.
	 * 
	 *         CODE BASED ON "Android Protips: A Deep Dive Into Location"...
	 *         THANKS MR. MEIER!
	 * 
	 * 
	 */
	static public void getLastBestLocation(int iLocationPriority) {
		long minTime;
		List<String> locnProviders = OMC.LM.getAllProviders();
		// Determine minimum acceptable time
		// via location priority.
		switch (iLocationPriority) {
		// Strict (fine) - 2 hours
		case 0:
		case 1:
			minTime = System.currentTimeMillis() - 3600000l * 2;
			break;
		// Medium - 10 hours
		case 2:
		case 3:
			minTime = System.currentTimeMillis() - 3600000l * 10;
			break;
		// Flexible - any
		case 4:
		default:
			minTime = 0l;
		}

		Location bestResult = null;
		float bestAccuracy = Float.MAX_VALUE;
		long bestTime = Long.MIN_VALUE;
		String bestProvider = "";

		// Iterate through all the providers on the system, keeping
		// note of the most accurate result within the acceptable time
		// limit.
		// If no result is found within maxTime, return the newest Location.

		for (String provider : locnProviders) {
			boolean bSkipProvider;

			// Ignore certain providers according to location priority.
			switch (iLocationPriority) {
			// Fine - GPS only.
			case 0:
			case 2:
				if (!provider.equals(LocationManager.GPS_PROVIDER)) bSkipProvider=true;
				else bSkipProvider=false;
				break;
			// Otherwise - GPS/network/passive.
			case 1:
			case 3:
			case 4:
			default:
				bSkipProvider=false;
			}
			if (bSkipProvider) continue;
			
			Location location = OMC.LM.getLastKnownLocation(provider);
			if (location != null) {
				float accuracy = location.getAccuracy();
				long time = location.getTime();
				Time t = new Time();
				t.set(time);
				if (OMC.DEBUG)
					Log.i(OMC.OMCSHORT + "Locn", "Last Known " + provider +" @" + t.format("%R") + " : " + accuracy);
				
				// We want to place much higher emphasis on timeliness rather
				// than accuracy
				
				if ((time > bestTime)) {
					bestResult = location;
					bestAccuracy = accuracy;
					bestTime = time;
					bestProvider = provider;
					// But if the lock is within 5 minutes of the most
					// recent and has more accuracy, we'll take
					// the more-accurate
				
				} else if (Math.abs(time - bestTime) < 30000l
						&& accuracy < bestAccuracy) {
					bestResult = location;
					bestAccuracy = accuracy;
					bestTime = time;
					bestProvider = provider;

				} else if (time < minTime && bestAccuracy == Float.MAX_VALUE
						&& time > bestTime) {
					bestResult = location;
					bestTime = time;
					bestProvider = provider;
				}
			}
		}
		// If the best result is beyond the allowed time limit, or the
		// accuracy of the
		// best result is wider than the acceptable maximum distance,
		// request a single update.
		// This check simply implements the same conditions we set when
		// requesting regular
		// location updates every [minTime] and [minDistance].

		if (bestTime < minTime) {
			// According to location priority...
			switch (iLocationPriority) {

			// Fine - GPS only.
			case 0:
			case 2:
					boolean bSuccess=false;
					int iRetries = 0;
					while (!bSuccess && iRetries<3) {
						try {
							if (OMC.DEBUG)
								Log.i(OMC.OMCSHORT + "Weather", "Requesting GPS Locn.");
			            	OMC.LM.removeUpdates(OMC.LL); 
							OMC.LM.requestLocationUpdates("gps", 3600000, 10000, OMC.LL);
							bSuccess=true;
						} catch (Exception ee) {
							Log.w(OMC.OMCSHORT + "Weather", "Cannot fix location.");
							ee.printStackTrace();
							iRetries++;
							try {
								Thread.sleep(5000);
							} catch (InterruptedException ie) {
								// Do nothing; just a pause that can be omitted
							}
						}
					}
				break;
				
			// Otherwise - Start with network; if it fails, go GPS.
			case 1:
			case 3:
			case 4:
			default:
				bSuccess=false;
				iRetries = 0;
				while (!bSuccess && iRetries<3) {
					try {
						if (OMC.DEBUG)
							Log.i(OMC.OMCSHORT + "Weather", "Requesting Network Locn.");
		            	OMC.LM.removeUpdates(OMC.LL); 
						OMC.LM.requestLocationUpdates("network", 3600000, 10000, OMC.LL);
						bSuccess=true;
					} catch (Exception e) {
						try {
							if (OMC.DEBUG)
								Log.i(OMC.OMCSHORT + "Weather", "Requesting GPS Locn.");
			            	OMC.LM.removeUpdates(OMC.LL); 
							OMC.LM.requestLocationUpdates("gps", 3600000, 10000, OMC.LL);
							bSuccess=true;
						} catch (Exception ee) {
							Log.w(OMC.OMCSHORT + "Weather", "Cannot fix location.");
							ee.printStackTrace();
							iRetries++;
							try {
								Thread.sleep(5000);
							} catch (InterruptedException ie) {
								// Do nothing; just a pause that can be omitted
							}
						}
					}
				}
			}
		} else {
			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "Weather", "Using cached location from "
						+ bestProvider + " as of "
						+ new java.sql.Time(bestTime).toLocaleString());
//			bestResult.setProvider("passive");
			
// 		 	v1.4.1:  If we want to debug, change bestResult here before sending back to main thread.
//			bestResult.setLatitude(LAT);
//			bestResult.setLongitude(LONG);
			OMC.LL.onLocationChanged(bestResult);
		}
	}

	static public String updateLocation(final Location location)
			throws Exception {
		JSONObject result;
		HttpURLConnection huc = null;

		try {
			if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
				System.setProperty("http.keepAlive", "false");
			}
			URL url = new URL(
					"http://maps.googleapis.com/maps/api/geocode/json?latlng="
							+ location.getLatitude() + ","
							+ location.getLongitude() + "&sensor=false");
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
				for (int counter = 0; counter < jary.length(); counter++) {
					JSONObject jobj = jary.optJSONObject(counter);
					JSONArray jary2 = jobj.optJSONArray("address_components");
					for (int counterj = 0; counterj < jary2.length(); counterj++) {
						for (int iType = 0; iType < jary2
								.optJSONObject(counterj).optJSONArray("types")
								.length(); iType++) {
							if (jary2.optJSONObject(counterj)
									.optJSONArray("types").optString(iType)
									.equals("sublocality")) {
								city = jary2.optJSONObject(counterj).optString(
										"long_name", "Unknown");
							}
							if (jary2.optJSONObject(counterj)
									.optJSONArray("types").optString(iType)
									.equals("locality")) {
								city = jary2.optJSONObject(counterj).optString(
										"long_name", "Unknown");
							}
							if (jary2.optJSONObject(counterj)
									.optJSONArray("types").optString(iType)
									.equals("country")) {
								country = jary2.optJSONObject(counterj)
										.optString("long_name", "Unknown");
							}
						}
					}
				}
			}
			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "Weather", "Reverse Geocode: " + city
						+ ", " + country);
			OMC.LASTKNOWNCITY = city;
			OMC.LASTKNOWNCOUNTRY = country;
			return result.toString();
		} catch (Exception e) {
			if (huc != null)
				huc.disconnect();
			throw e;
		}

	}

}