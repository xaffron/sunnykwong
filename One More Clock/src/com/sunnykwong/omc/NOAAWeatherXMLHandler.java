package com.sunnykwong.omc;

import java.io.StringReader;
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

	public static final long MINTIMEBETWEENREQUESTS = 3660000l; //One hour + change
	public static final long MINRETRYPERIOD = 3660000l; //One hour + change
	public static String CACHEDFORECAST;
	public static long CACHEDFORECASTMILLIS=0l;
	public static boolean LOCATIONCHANGED;
	public static String LASTUSEDCITY, LASTUSEDCOUNTRY;

	public static final String URL_NOAAFORECAST = "http://forecast.weather.gov/MapClick.php?unit=0&lg=english&FcstType=dwml&lat=";
	public static HashMap<String, Integer> CONDITIONTRANSLATIONS;

	public DeeperStack<String[]> tree;
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
	public static double dCurrentTemp;
	public static int iCurrentCondition;
	public static ArrayList<Integer> CONDITIONS;
	public static ArrayList<Double> TEMPTOUPDATE;
	public static String VALUETYPE;
	public static boolean bCurrentObservations;

	
	public NOAAWeatherXMLHandler() {
		super();
		if (tree == null)
			tree = new DeeperStack<String[]>();
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

	static public void updateWeather(final double latitude,
			final double longitude, final String country, final String city,
			final boolean bylatlong) {
		// If the city or country is empty, it's the first time this is run -
		// location has changed.
		if (LASTUSEDCITY == null || LASTUSEDCOUNTRY == null) {
			LASTUSEDCITY = city;
			LASTUSEDCOUNTRY = country;
			LOCATIONCHANGED = true;
			// If either city and country have changed,
			// set the location change flag to true.
		} else if (!LASTUSEDCITY.equals(city)
				|| !LASTUSEDCOUNTRY.equals(country)) {
			LASTUSEDCITY = city;
			LASTUSEDCOUNTRY = country;
			LOCATIONCHANGED = true;
			// If city and country have not changed,
			// but we've lost the cached forecast or the
			// cached weather is old
			// set the location change flag to true.
		} else if (CACHEDFORECAST == null
				|| System.currentTimeMillis() - CACHEDFORECASTMILLIS > MINTIMEBETWEENREQUESTS) {
			LASTUSEDCITY = city;
			LASTUSEDCOUNTRY = country;
			LOCATIONCHANGED = true;
			// otherwise, the location didn't change - let's use the cached
			// forecast.
		} else {
			LOCATIONCHANGED = false;
		}

		if (LOCATIONCHANGED) {
			// Update weather from provider.

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
						} else {
							url = new URL(NOAAWeatherXMLHandler.URL_NOAAFORECAST+latitude+"&lon="+longitude);
						}
						huc = (HttpURLConnection) url.openConnection();
						huc.setConnectTimeout(30000);
						huc.setReadTimeout(30000);

						CACHEDFORECAST = OMC.streamToString(huc
								.getInputStream());
						OMC.WEATHERREFRESHSTATUS = OMC.WRS_PROVIDER;
						xr.parse(new InputSource(new StringReader(
								CACHEDFORECAST)));
						UPDATEDTIME.set(huc.getDate());
						huc.disconnect();

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
						XMLReader xr = XMLReaderFactory.createXMLReader();
						NOAAWeatherXMLHandler GXhandler = new NOAAWeatherXMLHandler();
						GXhandler.jsonWeather.putOpt("country2", country);
						GXhandler.jsonWeather.putOpt("city2", city);
						GXhandler.jsonWeather.putOpt("bylatlong", bylatlong);
						GXhandler.jsonWeather.putOpt("longitude_e6",
								longitude * 1000000d);
						GXhandler.jsonWeather.putOpt("latitude_e6",
								latitude * 1000000d);
						xr.setContentHandler(GXhandler);
						xr.setErrorHandler(GXhandler);

						if (OMC.DEBUG)
							Log.i(OMC.OMCSHORT + "NOAAWeather",
									"Reusing previously-cached forecast.");
						OMC.WEATHERREFRESHSTATUS = OMC.WRS_PROVIDER;
						xr.parse(new InputSource(new StringReader(
								CACHEDFORECAST)));
					} catch (Exception e) {
						e.printStackTrace();
					}
				};
			};
			t.start();
		}
	}

	@Override
	public void startDocument() {
		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "NOAAWeather", "Start Building JSON.");
		// We haven't accumulated current conditions yet
		LOWTEMPS=new ArrayList<Double>();
		HIGHTEMPS=new ArrayList<Double>();
		CONDITIONS = new ArrayList<Integer>();
		VALUETYPE = new String();
		
		// Default timestamp to 1/1/1970
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
			OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		// Discard the tabs between tags.
		String value = new String(ch,start,length).replace("\t", "");
		if (tree.isEmpty()) return;
		if (tree.size()<2) return;
		if (tree.peek()[0].equals("creation-date")) {
			Time creationTime = new Time();
			creationTime.parse3339(value);
			try {
				jsonWeather.putOpt("current_time",
						creationTime.format2445());
				jsonWeather.putOpt("current_millis",
						creationTime.toMillis(false));
				jsonWeather.putOpt("current_local_time",
						creationTime.format2445());
				} catch (Exception e ) {
					e.printStackTrace();
					OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
				}
			return;
		}
		if (!tree.peek(0)[0].equals("value")) return;

		if (tree.peek(1)[0].equals("temperature")) {
			if (bCurrentObservations) {
				try {
					if (VALUETYPE.equals("temp_f")) {
						try {
							dCurrentTemp=Double.parseDouble(value);
							jsonWeather.put(VALUETYPE, value);
						}
						catch (NumberFormatException e) { dCurrentTemp=-999; }
					}
					VALUETYPE="";
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				NOAAWeatherXMLHandler.TEMPTOUPDATE.add(Double.parseDouble(value));
			}
			return;
		}
		// Discard the "false positive" generated by tabs within temperature tags.
		if (VALUETYPE.equals("")) return;
		try {
			jsonWeather.put(VALUETYPE, value);
			VALUETYPE="";
		} catch (JSONException e) {
			e.printStackTrace();
			OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
		}
		
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) {
		try {
			// If we see a tag, first see if it's a top level data tag.
			if (localName.equals("data") && atts.getValue("type").equals("forecast"))
				bCurrentObservations=false;
			if (localName.equals("data") && atts.getValue("type").equals("current observations"))
				bCurrentObservations=true;
			// Depending on the value we want to capture, we set the value type.
			if (!tree.isEmpty()) {
				if (localName.equals("temperature")) {
					if (atts.getValue("type").equals("maximum"))
						NOAAWeatherXMLHandler.TEMPTOUPDATE = HIGHTEMPS;
					else if (atts.getValue("type").equals("minimum"))
						NOAAWeatherXMLHandler.TEMPTOUPDATE = LOWTEMPS;
					else if (atts.getValue("type").equals("apparent") || atts.getValue("type").equals("air"))
						VALUETYPE="temp_f";
					else if (atts.getValue("type").equals("dew point"))
						VALUETYPE="dewpoint_f";
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
					String sCondition = atts.getValue("weather-summary").trim().toLowerCase();
					if (bCurrentObservations) {
						iCurrentCondition = NOAAWeatherXMLHandler.getTranslation(sCondition);
						jsonWeather.put("condition_raw", sCondition);
						jsonWeather.put("condition_code",iCurrentCondition);
					}
					else {
						NOAAWeatherXMLHandler.CONDITIONS.add(NOAAWeatherXMLHandler.getTranslation(sCondition));
					}
				}
			}
			tree.push(new String[] { localName });

		} catch (Exception e) {
		e.printStackTrace();
		try {
			jsonWeather.putOpt("problem_cause", "error");
		} catch (Exception ee) {}
		OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
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
		// Translate wind direction.
		double winddeg = jsonWeather.optDouble("wind_direction");
		try {
			if (winddeg==999)
				jsonWeather.put("wind_direction","Vrbl");
			else if (winddeg>337.5)
				jsonWeather.put("wind_direction","N");
			else if (winddeg>292.5)
				jsonWeather.put("wind_direction","NW");
			else if (winddeg>247.5)
				jsonWeather.put("wind_direction","W");
			else if (winddeg>202.5)
				jsonWeather.put("wind_direction","SW");
			else if (winddeg>157.5)
				jsonWeather.put("wind_direction","S");
			else if (winddeg>112.5)
				jsonWeather.put("wind_direction","SE");
			else if (winddeg>67.5)
				jsonWeather.put("wind_direction","E");
			else if (winddeg>22.5)
				jsonWeather.put("wind_direction","NE");
			else 
				jsonWeather.put("wind_direction","N");
		} catch (JSONException e) {
			e.printStackTrace();
			OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
		}

		
		
		String windString = OMC.RString("windcondition") +
				jsonWeather.optString("wind_direction") + " @ " +
				iWindSpeedMph + " mph";
		try {
			jsonWeather.putOpt("humidity", humidityString);
			jsonWeather.putOpt("wind_condition", windString);
			jsonWeather.putOpt("wind_speed_mph", iWindSpeedMph);
			jsonWeather.putOpt("wind_speed_mps", iWindSpeedMps);

			// Convert current temp and dewpoint from f to c.
			jsonWeather.putOpt("temp_c",(int)((jsonWeather.optDouble("temp_f")-32)/9d*5d+0.5d));
			jsonWeather.putOpt("dewpoint_c",(int)((jsonWeather.optDouble("dewpoint_f")-32)/9d*5d+0.5d));

		} catch (JSONException e) {
			e.printStackTrace();
			OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
		}

		// 
		// Build out the forecast array.
		Time day = new Time();
		day.setToNow();

		// If we have the same # of lowtemps as hightemps, we're near nighttime - 
		// use current temp as today's hightemp
		int iConditionsCount=0;
		if (LOWTEMPS.size()==HIGHTEMPS.size()) {
			HIGHTEMPS.add(0,dCurrentTemp);
			CONDITIONS.add(0,iCurrentCondition);
		}
		//  If current condition code is missing or NA, use nearest forecast condition instead
		String sCurrentRaw = jsonWeather.optString("condition_raw", "na");
		if (sCurrentRaw.equals("na") || sCurrentRaw.equals("")) {
			try {
				jsonWeather.put("condition_code",CONDITIONS.get(0));
			} catch (JSONException e) {
				e.printStackTrace();
				OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
			}
		}
		for (int i = 0; i< 4; i++) {
			try {
				jsonOneDayForecast = new JSONObject();
				jsonOneDayForecast.put("day_of_week", day.format("%a"));
				final int iConditionCode = CONDITIONS.get(iConditionsCount);
				jsonOneDayForecast.put("condition", OMC.VERBOSEWEATHER[iConditionCode]);
				jsonOneDayForecast.put("condition_code", iConditionCode);
				jsonOneDayForecast.put("condition_lcase", OMC.VERBOSEWEATHER[iConditionCode].toLowerCase());

				iConditionsCount+=2;

				double lowf = OMC.roundToSignificantFigures(
						LOWTEMPS.get(i), 3);
				double highf = OMC.roundToSignificantFigures(
						HIGHTEMPS.get(i), 3);
				double lowc = (int) (((lowf-32f) / 9f * 5f)+0.5f);
				double highc = (int) (((highf-32f) / 9f * 5f)+0.5f);
				jsonOneDayForecast.put("low_c", lowc);
				jsonOneDayForecast.put("high_c", highc);
				jsonOneDayForecast.put("low", lowf);
				jsonOneDayForecast.put("high", highf);
				jsonWeather.getJSONArray("zzforecast_conditions").put(
						jsonOneDayForecast);
				day.hour += 24;
				day.normalize(false);
			} catch (JSONException e) {
				e.printStackTrace();
				OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
			}
		}

		try {
			if (jsonWeather.optString("city")==null || jsonWeather.optString("city").equals("")) {
				if (!jsonWeather.optString("city2").equals(""))
					jsonWeather.putOpt("city", jsonWeather.optString("city2"));
				else if (!jsonWeather.optString("country2").equals(""))
					jsonWeather.putOpt("city", jsonWeather.optString("country2"));
			}
			
			//Mark weather forecast source.
			jsonWeather.put("source", "noaa");
			
			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "NOAAWeather", jsonWeather.toString());

		} catch (JSONException e) {
			e.printStackTrace();
			OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
			return;
		}
		OMC.PREFS.edit().putString("weather", jsonWeather.toString()).commit();
		OMC.LASTWEATHERREFRESH = System.currentTimeMillis();
		OMC.WEATHERREFRESHSTATUS = OMC.WRS_SUCCESS;

		if (LOCATIONCHANGED)
			CACHEDFORECASTMILLIS = OMC.LASTWEATHERREFRESH;

//	 	v1.4.1:  Auto weather provider.
//			If NOAA returns NA for current conditions but valid forecast, request METAR.
//			If NOAA returns no forecast, switch to 7Timer + no METAR.

		if (jsonWeather.optInt("condition_code")==0 || !jsonWeather.has("temp_f")) {
			if (CONDITIONS==null || CONDITIONS.size()==0 || CONDITIONS.get(0)==-999) {
        		if (OMC.PREFS.getString("weatherProvider", "auto").equals("auto")) {
    				OMC.PREFS.edit().putBoolean("weatherMETAR", true).commit();
    			}
				SevenTimerJSONHandler.updateWeather(jsonWeather.optDouble("latitude_e6")/1000000d, jsonWeather.optDouble("longitude_e6")/1000000d, jsonWeather.optString("country2"), jsonWeather.optString("city"), true);
			} else {
				METARHandler.updateCurrentConditions(jsonWeather.optDouble("latitude_e6")/1000000d, jsonWeather.optDouble("longitude_e6")/1000000d);
			}
			return;
		} else if (OMC.PREFS.getBoolean("weatherMETAR", true)) {
			METARHandler.updateCurrentConditions(jsonWeather.optDouble("latitude_e6")/1000000d, jsonWeather.optDouble("longitude_e6")/1000000d);
		}
		
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

		OMC.NEXTWEATHERREQUEST = OMC.NEXTWEATHERREFRESH;
		
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "NOAAWeather", "Next Refresh Time:" + new java.sql.Time(OMC.NEXTWEATHERREFRESH).toLocaleString());
		OMC.PREFS.edit().putLong("weather_nextweatherrefresh", OMC.NEXTWEATHERREFRESH).commit();

		CONDITIONTRANSLATIONS=null;
		
	}
	
	static void putCombos(final String condition, final int code) {
		CONDITIONTRANSLATIONS.put(condition,code);
		CONDITIONTRANSLATIONS.put("chance "+condition,code);
		CONDITIONTRANSLATIONS.put("slight chc "+condition,code);
		CONDITIONTRANSLATIONS.put("isolated "+condition,code);
		CONDITIONTRANSLATIONS.put("scattered "+condition,code);
		CONDITIONTRANSLATIONS.put(condition + " in vicinity",code);
		CONDITIONTRANSLATIONS.put(condition + " likely",code);
		CONDITIONTRANSLATIONS.put("areas "+condition,code);
		CONDITIONTRANSLATIONS.put("light "+condition,code);
		CONDITIONTRANSLATIONS.put("heavy "+condition,code);
	}
	
	static int getTranslation(final String input) {
		if (CONDITIONTRANSLATIONS==null) {
			// Populating the condition translations
			CONDITIONTRANSLATIONS = new HashMap<String,Integer>();
			CONDITIONTRANSLATIONS.put("fair",1);
			CONDITIONTRANSLATIONS.put("becoming sunny",1);
			CONDITIONTRANSLATIONS.put("sunny",1);
			CONDITIONTRANSLATIONS.put("clear",1);
			CONDITIONTRANSLATIONS.put("fair with haze",1);
			CONDITIONTRANSLATIONS.put("clear with haze",1);
			CONDITIONTRANSLATIONS.put("fair and breezy",1);
			CONDITIONTRANSLATIONS.put("clear and breezy",1);
			CONDITIONTRANSLATIONS.put("partly sunny",4);
			putCombos("drizzle",16);
			putCombos("showers",15);
			putCombos("rain",15);
			putCombos("rain showers",15);
			putCombos("showers rain",15);
			putCombos("thunderstorms",23);
			putCombos("snow",33);
			putCombos("rain/snow",38);
			putCombos("snow/rain",38);
			CONDITIONTRANSLATIONS.put("thunderstorm in vicinity fog",23);
			CONDITIONTRANSLATIONS.put("thunderstorm in vicinity haze",23);
			CONDITIONTRANSLATIONS.put("showers in vicinity fog/mist",17);
			CONDITIONTRANSLATIONS.put("showers in vicinity fog",17);
			CONDITIONTRANSLATIONS.put("showers in vicinity haze",17);
			CONDITIONTRANSLATIONS.put("rain showers fog/mist",15);
			CONDITIONTRANSLATIONS.put("drizzle fog/mist",16);
			CONDITIONTRANSLATIONS.put("light drizzle fog/mist",16);
			CONDITIONTRANSLATIONS.put("heavy drizzle fog/mist",16);
			CONDITIONTRANSLATIONS.put("drizzle fog",16);
			CONDITIONTRANSLATIONS.put("light drizzle fog",16);
			CONDITIONTRANSLATIONS.put("heavy drizzle fog",16);
			CONDITIONTRANSLATIONS.put("dust",6);
			CONDITIONTRANSLATIONS.put("low drifting dust",6);
			CONDITIONTRANSLATIONS.put("blowing dust",6);
			CONDITIONTRANSLATIONS.put("sand",6);
			CONDITIONTRANSLATIONS.put("blowing sand",6);
			CONDITIONTRANSLATIONS.put("low drifting sand",6);
			CONDITIONTRANSLATIONS.put("dust/sand whirls",6);
			CONDITIONTRANSLATIONS.put("dust/sand whirls in vicinity",6);
			CONDITIONTRANSLATIONS.put("dust storm",6);
			CONDITIONTRANSLATIONS.put("heavy dust storm",6);
			CONDITIONTRANSLATIONS.put("dust storm in vicinity",6);
			CONDITIONTRANSLATIONS.put("sand storm",6);
			CONDITIONTRANSLATIONS.put("heavy sand storm",6);
			CONDITIONTRANSLATIONS.put("sand storm in vicinity",6);
			CONDITIONTRANSLATIONS.put("low drifting snow",28);
			CONDITIONTRANSLATIONS.put("areas frost",28);
			CONDITIONTRANSLATIONS.put("morning frost",28);
			CONDITIONTRANSLATIONS.put("fog\\/mist",13);
			CONDITIONTRANSLATIONS.put("fog/mist",13);
			CONDITIONTRANSLATIONS.put("fog",13);
			CONDITIONTRANSLATIONS.put("freezing fog",13);
			CONDITIONTRANSLATIONS.put("shallow fog",13);
			CONDITIONTRANSLATIONS.put("partial fog",13);
			CONDITIONTRANSLATIONS.put("areas fog",13);
			CONDITIONTRANSLATIONS.put("patchy fog",13);
			CONDITIONTRANSLATIONS.put("patches of fog",13);
			CONDITIONTRANSLATIONS.put("fog in vicinity",13);
			CONDITIONTRANSLATIONS.put("freezing fog in vicinity",13);
			CONDITIONTRANSLATIONS.put("shallow fog in vicinity",13);
			CONDITIONTRANSLATIONS.put("partial fog in vicinity",13);
			CONDITIONTRANSLATIONS.put("patches of fog in vicinity",13);
			CONDITIONTRANSLATIONS.put("showers in vicinity fog",13);
			CONDITIONTRANSLATIONS.put("light freezing fog",13);
			CONDITIONTRANSLATIONS.put("heavy freezing fog",13);
			CONDITIONTRANSLATIONS.put("freezing rain",37);
			CONDITIONTRANSLATIONS.put("freezing drizzle",37);
			CONDITIONTRANSLATIONS.put("light freezing rain",37);
			CONDITIONTRANSLATIONS.put("light freezing drizzle",37);
			CONDITIONTRANSLATIONS.put("heavy freezing rain",37);
			CONDITIONTRANSLATIONS.put("heavy freezing drizzle",37);
			CONDITIONTRANSLATIONS.put("freezing rain in vicinity",37);
			CONDITIONTRANSLATIONS.put("freezing drizzle in vicinity",37);
			CONDITIONTRANSLATIONS.put("freezing rain rain",37);
			CONDITIONTRANSLATIONS.put("light freezing rain rain",37);
			CONDITIONTRANSLATIONS.put("heavy freezing rain rain",37);
			CONDITIONTRANSLATIONS.put("rain freezing rain",37);
			CONDITIONTRANSLATIONS.put("light rain freezing rain",37);
			CONDITIONTRANSLATIONS.put("heavy rain freezing rain",37);
			CONDITIONTRANSLATIONS.put("freezing drizzle rain",37);
			CONDITIONTRANSLATIONS.put("light freezing drizzle rain",37);
			CONDITIONTRANSLATIONS.put("heavy freezing drizzle rain",37);
			CONDITIONTRANSLATIONS.put("rain freezing drizzle",37);
			CONDITIONTRANSLATIONS.put("light rain freezing drizzle",37);
			CONDITIONTRANSLATIONS.put("heavy rain freezing drizzle",37);
			CONDITIONTRANSLATIONS.put("haze",7);
			CONDITIONTRANSLATIONS.put("heavy rain showers",26);
			CONDITIONTRANSLATIONS.put("heavy showers rain",26);
			CONDITIONTRANSLATIONS.put("heavy rain showers fog/mist",26);
			CONDITIONTRANSLATIONS.put("heavy showers rain fog/mist",26);
			CONDITIONTRANSLATIONS.put("heavy rain",26);
			CONDITIONTRANSLATIONS.put("heavy rain fog/mist",26);
			CONDITIONTRANSLATIONS.put("heavy rain fog",26);
			CONDITIONTRANSLATIONS.put("freezing rain snow",35);
			CONDITIONTRANSLATIONS.put("light freezing rain snow",35);
			CONDITIONTRANSLATIONS.put("heavy freezing rain snow",35);
			CONDITIONTRANSLATIONS.put("freezing drizzle snow",35);
			CONDITIONTRANSLATIONS.put("light freezing drizzle snow",35);
			CONDITIONTRANSLATIONS.put("heavy freezing drizzle snow",35);
			CONDITIONTRANSLATIONS.put("snow freezing rain",35);
			CONDITIONTRANSLATIONS.put("light snow freezing rain",35);
			CONDITIONTRANSLATIONS.put("heavy snow freezing rain",35);
			CONDITIONTRANSLATIONS.put("snow freezing drizzle",35);
			CONDITIONTRANSLATIONS.put("light snow freezing drizzle",35);
			CONDITIONTRANSLATIONS.put("heavy snow freezing drizzle",35);
			CONDITIONTRANSLATIONS.put("light rain showers",14);
			CONDITIONTRANSLATIONS.put("light rain and breezy",14);
			CONDITIONTRANSLATIONS.put("light showers rain",14);
			CONDITIONTRANSLATIONS.put("light rain showers fog/mist",14);
			CONDITIONTRANSLATIONS.put("light showers rain fog/mist",14);
			CONDITIONTRANSLATIONS.put("light rain",14);
			CONDITIONTRANSLATIONS.put("light rain fog/mist",14);
			CONDITIONTRANSLATIONS.put("light rain fog",14);
			CONDITIONTRANSLATIONS.put("light snow",29);
			CONDITIONTRANSLATIONS.put("light snow fog/mist",29);
			CONDITIONTRANSLATIONS.put("light snow fog",29);
			CONDITIONTRANSLATIONS.put("blowing snow",29);
			CONDITIONTRANSLATIONS.put("snow low drifting snow",29);
			CONDITIONTRANSLATIONS.put("light snow low drifting snow",29);
			CONDITIONTRANSLATIONS.put("light snow blowing snow",29);
			CONDITIONTRANSLATIONS.put("light snow blowing snow fog/mist",29);
			CONDITIONTRANSLATIONS.put("mostly cloudy",10);
			CONDITIONTRANSLATIONS.put("mostly cloudy with haze",10);
			CONDITIONTRANSLATIONS.put("mostly cloudy and breezy",10);
			CONDITIONTRANSLATIONS.put("overcast",11);
			CONDITIONTRANSLATIONS.put("cloudy",11);
			CONDITIONTRANSLATIONS.put("overcast with haze",11);
			CONDITIONTRANSLATIONS.put("overcast and breezy",11);
			CONDITIONTRANSLATIONS.put("mostly sunny",3);
			CONDITIONTRANSLATIONS.put("mostly clear",3);
			CONDITIONTRANSLATIONS.put("a few clouds",5);
			CONDITIONTRANSLATIONS.put("a few clouds with haze",5);
			CONDITIONTRANSLATIONS.put("a few clouds and breezy",5);
			CONDITIONTRANSLATIONS.put("increasing clouds",5);
			CONDITIONTRANSLATIONS.put("partly cloudy",5);
			CONDITIONTRANSLATIONS.put("partly cloudy with haze",5);
			CONDITIONTRANSLATIONS.put("partly cloudy and breezy",5);
			CONDITIONTRANSLATIONS.put("rain showers",27);
			CONDITIONTRANSLATIONS.put("showers rain",27);
			CONDITIONTRANSLATIONS.put("rain showers in vicinity fog/mist",27);
			CONDITIONTRANSLATIONS.put("showers rain fog/mist",27);
			CONDITIONTRANSLATIONS.put("showers rain in vicinity fog/mist",27);
			CONDITIONTRANSLATIONS.put("rain",27);
			CONDITIONTRANSLATIONS.put("showers",27);
			CONDITIONTRANSLATIONS.put("rain fog/mist",27);
			CONDITIONTRANSLATIONS.put("rain fog",27);
			CONDITIONTRANSLATIONS.put("rain ice pellets",38);
			CONDITIONTRANSLATIONS.put("light rain ice pellets",38);
			CONDITIONTRANSLATIONS.put("heavy rain ice pellets",38);
			CONDITIONTRANSLATIONS.put("drizzle ice pellets",38);
			CONDITIONTRANSLATIONS.put("light drizzle ice pellets",38);
			CONDITIONTRANSLATIONS.put("heavy drizzle ice pellets",38);
			CONDITIONTRANSLATIONS.put("ice pellets rain",38);
			CONDITIONTRANSLATIONS.put("light ice pellets rain",38);
			CONDITIONTRANSLATIONS.put("heavy ice pellets rain",38);
			CONDITIONTRANSLATIONS.put("ice pellets drizzle",38);
			CONDITIONTRANSLATIONS.put("light ice pellets drizzle",38);
			CONDITIONTRANSLATIONS.put("heavy ice pellets drizzle",38);
			CONDITIONTRANSLATIONS.put("rain snow",38);
			CONDITIONTRANSLATIONS.put("light rain snow",38);
			CONDITIONTRANSLATIONS.put("heavy rain snow",38);
			CONDITIONTRANSLATIONS.put("snow rain",38);
			CONDITIONTRANSLATIONS.put("light snow rain",38);
			CONDITIONTRANSLATIONS.put("heavy snow rain",38);
			CONDITIONTRANSLATIONS.put("drizzle snow",38);
			CONDITIONTRANSLATIONS.put("light drizzle snow",38);
			CONDITIONTRANSLATIONS.put("heavy drizzle snow",38);
			CONDITIONTRANSLATIONS.put("snow drizzle",38);
			CONDITIONTRANSLATIONS.put("light snow drizzle",38);
			CONDITIONTRANSLATIONS.put("heavy drizzle snow",38);
			CONDITIONTRANSLATIONS.put("ice pellets",36);
			CONDITIONTRANSLATIONS.put("light ice pellets",36);
			CONDITIONTRANSLATIONS.put("heavy ice pellets",36);
			CONDITIONTRANSLATIONS.put("ice pellets in vicinity",36);
			CONDITIONTRANSLATIONS.put("showers ice pellets",36);
			CONDITIONTRANSLATIONS.put("thunderstorm ice pellets",36);
			CONDITIONTRANSLATIONS.put("ice crystals",36);
			CONDITIONTRANSLATIONS.put("hail",36);
			CONDITIONTRANSLATIONS.put("small hail/snow pellets",36);
			CONDITIONTRANSLATIONS.put("light small hail/snow pellets",36);
			CONDITIONTRANSLATIONS.put("heavy small hail/snow pellets",36);
			CONDITIONTRANSLATIONS.put("showers hail",36);
			CONDITIONTRANSLATIONS.put("hail showers",36);
			CONDITIONTRANSLATIONS.put("smoke",9);
			CONDITIONTRANSLATIONS.put("areas smoke",9);
			CONDITIONTRANSLATIONS.put("heavy snow",33);
			CONDITIONTRANSLATIONS.put("snow fog/mist",33);
			CONDITIONTRANSLATIONS.put("heavy snow fog/mist",33);
			CONDITIONTRANSLATIONS.put("snow fog",33);
			CONDITIONTRANSLATIONS.put("heavy snow fog",33);
			CONDITIONTRANSLATIONS.put("snow blowing snow",33);
			CONDITIONTRANSLATIONS.put("heavy snow low drifting snow",33);
			CONDITIONTRANSLATIONS.put("heavy snow blowing snow",33);
			CONDITIONTRANSLATIONS.put("thunderstorm snow",33);
			CONDITIONTRANSLATIONS.put("light thunderstorm snow",33);
			CONDITIONTRANSLATIONS.put("heavy thunderstorm snow",33);
			CONDITIONTRANSLATIONS.put("snow grains",33);
			CONDITIONTRANSLATIONS.put("light snow grains",33);
			CONDITIONTRANSLATIONS.put("heavy snow grains",33);
			CONDITIONTRANSLATIONS.put("heavy blowing snow",33);
			CONDITIONTRANSLATIONS.put("blowing snow in vicinity",33);
			CONDITIONTRANSLATIONS.put("snow showers",31);
			CONDITIONTRANSLATIONS.put("light snow showers",31);
			CONDITIONTRANSLATIONS.put("heavy snow showers",31);
			CONDITIONTRANSLATIONS.put("showers snow",31);
			CONDITIONTRANSLATIONS.put("light showers snow",31);
			CONDITIONTRANSLATIONS.put("heavy showers snow",31);
			CONDITIONTRANSLATIONS.put("snow showers fog/mist",31);
			CONDITIONTRANSLATIONS.put("light snow showers fog/mist",31);
			CONDITIONTRANSLATIONS.put("heavy snow showers fog/mist",31);
			CONDITIONTRANSLATIONS.put("showers snow fog/mist",31);
			CONDITIONTRANSLATIONS.put("light showers snow fog/mist",31);
			CONDITIONTRANSLATIONS.put("heavy showers snow fog/mist",31);
			CONDITIONTRANSLATIONS.put("snow showers fog",31);
			CONDITIONTRANSLATIONS.put("light snow showers fog",31);
			CONDITIONTRANSLATIONS.put("heavy snow showers fog",31);
			CONDITIONTRANSLATIONS.put("showers snow fog",31);
			CONDITIONTRANSLATIONS.put("light showers snow fog",31);
			CONDITIONTRANSLATIONS.put("heavy showers snow fog",31);
			CONDITIONTRANSLATIONS.put("showers in vicinity snow",31);
			CONDITIONTRANSLATIONS.put("snow showers in vicinity",31);
			CONDITIONTRANSLATIONS.put("snow showers in vicinity fog/mist",31);
			CONDITIONTRANSLATIONS.put("snow showers in vicinity fog",31);
			CONDITIONTRANSLATIONS.put("thunderstorm",21);
			CONDITIONTRANSLATIONS.put("thunderstorm rain",21);
			CONDITIONTRANSLATIONS.put("light thunderstorm rain",21);
			CONDITIONTRANSLATIONS.put("heavy thunderstorm rain",21);
			CONDITIONTRANSLATIONS.put("thunderstorm rain fog/mist",21);
			CONDITIONTRANSLATIONS.put("light thunderstorm rain fog/mist",21);
			CONDITIONTRANSLATIONS.put("heavy thunderstorm rain fog and windy",21);
			CONDITIONTRANSLATIONS.put("heavy thunderstorm rain fog/mist",21);
			CONDITIONTRANSLATIONS.put("thunderstorm showers in vicinity",21);
			CONDITIONTRANSLATIONS.put("light thunderstorm rain haze",21);
			CONDITIONTRANSLATIONS.put("heavy thunderstorm rain haze",21);
			CONDITIONTRANSLATIONS.put("thunderstorm fog",21);
			CONDITIONTRANSLATIONS.put("light thunderstorm rain fog",21);
			CONDITIONTRANSLATIONS.put("heavy thunderstorm rain fog",21);
			CONDITIONTRANSLATIONS.put("thunderstorm light rain",21);
			CONDITIONTRANSLATIONS.put("thunderstorm heavy rain",21);
			CONDITIONTRANSLATIONS.put("thunderstorm rain fog/mist",21);
			CONDITIONTRANSLATIONS.put("thunderstorm light rain fog/mist",21);
			CONDITIONTRANSLATIONS.put("thunderstorm heavy rain fog/mist",21);
			CONDITIONTRANSLATIONS.put("thunderstorm in vicinity fog/mist",21);
			CONDITIONTRANSLATIONS.put("thunderstorm showers in vicinity",21);
			CONDITIONTRANSLATIONS.put("thunderstorm in vicinity haze",21);
			CONDITIONTRANSLATIONS.put("thunderstorm haze in vicinity",21);
			CONDITIONTRANSLATIONS.put("thunderstorm light rain haze",21);
			CONDITIONTRANSLATIONS.put("thunderstorm heavy rain haze",21);
			CONDITIONTRANSLATIONS.put("thunderstorm fog",21);
			CONDITIONTRANSLATIONS.put("thunderstorm light rain fog",21);
			CONDITIONTRANSLATIONS.put("thunderstorm heavy rain fog",21);
			CONDITIONTRANSLATIONS.put("thunderstorm hail",21);
			CONDITIONTRANSLATIONS.put("light thunderstorm rain hail",21);
			CONDITIONTRANSLATIONS.put("heavy thunderstorm rain hail",21);
			CONDITIONTRANSLATIONS.put("thunderstorm rain hail fog/mist",21);
			CONDITIONTRANSLATIONS.put("light thunderstorm rain hail fog/mist",21);
			CONDITIONTRANSLATIONS.put("heavy thunderstorm rain hail fog/hail",21);
			CONDITIONTRANSLATIONS.put("thunderstorm showers in vicinity hail",21);
			CONDITIONTRANSLATIONS.put("light thunderstorm rain hail haze",21);
			CONDITIONTRANSLATIONS.put("heavy thunderstorm rain hail haze",21);
			CONDITIONTRANSLATIONS.put("thunderstorm hail fog",21);
			CONDITIONTRANSLATIONS.put("light thunderstorm rain hail fog",21);
			CONDITIONTRANSLATIONS.put("heavy thunderstorm rain hail fog",21);
			CONDITIONTRANSLATIONS.put("thunderstorm light rain hail",21);
			CONDITIONTRANSLATIONS.put("thunderstorm heavy rain hail",21);
			CONDITIONTRANSLATIONS.put("thunderstorm rain hail fog/mist",21);
			CONDITIONTRANSLATIONS.put("thunderstorm light rain hail fog/mist",21);
			CONDITIONTRANSLATIONS.put("thunderstorm heavy rain hail fog/mist",21);
			CONDITIONTRANSLATIONS.put("thunderstorm in vicinity hail",21);
			CONDITIONTRANSLATIONS.put("thunderstorm in vicinity hail haze",21);
			CONDITIONTRANSLATIONS.put("thunderstorm haze in vicinity hail",21);
			CONDITIONTRANSLATIONS.put("thunderstorm light rain hail haze",21);
			CONDITIONTRANSLATIONS.put("thunderstorm heavy rain hail haze",21);
			CONDITIONTRANSLATIONS.put("thunderstorm hail fog",21);
			CONDITIONTRANSLATIONS.put("thunderstorm light rain hail fog",21);
			CONDITIONTRANSLATIONS.put("thunderstorm heavy rain hail fog",21);
			CONDITIONTRANSLATIONS.put("thunderstorm small hail/snow pellets",21);
			CONDITIONTRANSLATIONS.put("thunderstorm rain small hail/snow pellets",21);
			CONDITIONTRANSLATIONS.put("light thunderstorm rain small hail/snow pellets",21);
			CONDITIONTRANSLATIONS.put("heavy thunderstorm rain small hail/snow pellets",21);
			CONDITIONTRANSLATIONS.put("windy",39);
			CONDITIONTRANSLATIONS.put("breezy",39);
			CONDITIONTRANSLATIONS.put("fair and windy",39);
			CONDITIONTRANSLATIONS.put("a few clouds and windy",39);
			CONDITIONTRANSLATIONS.put("partly cloudy and windy",39);
			CONDITIONTRANSLATIONS.put("mostly cloudy and windy",39);
			CONDITIONTRANSLATIONS.put("mostly cloudy and breezy",39);
			CONDITIONTRANSLATIONS.put("overcast and windy",39);
			CONDITIONTRANSLATIONS.put("funnel cloud",39);
			CONDITIONTRANSLATIONS.put("funnel cloud in vicinity",39);
			CONDITIONTRANSLATIONS.put("tornado/water spout",39);
			CONDITIONTRANSLATIONS.put("NA",0);
			CONDITIONTRANSLATIONS.put("",0);
		}
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "NOAAWeather", "Raw Condition:" + input);
		
		final Object result = CONDITIONTRANSLATIONS.get(input);

		if (result==null) {
			if (input.contains("thunderstorm"))
				return 21;
			if (input.contains("wind"))
				return 39;
			if (input.contains("breezy"))
				return 39;
			if (input.contains("freezing"))
				return 35;
			if (input.contains("drizzle"))
				return 16;
			if (input.contains("showers"))
				return 15;
			if (input.contains("rain"))
				return 15;
			if (input.contains("fog"))
				return 13;
			return 0;
		}
		else return (Integer)result;
	}
	
	class DeeperStack<E> extends Stack<E> {
		static final long serialVersionUID=0; 
		public E peek(int depth) {
			final int iTotalSize = size();
			if (depth>=iTotalSize) return null;
			return get(iTotalSize-1-depth);
		}

	}

}