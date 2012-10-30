package com.sunnykwong.omc;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.apache.commons.math.analysis.SplineInterpolator;
import org.apache.commons.math.analysis.UnivariateRealFunction;
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

public class YrNoWeatherXMLHandler extends DefaultHandler {

	public static final long MINTIMEBETWEENREQUESTS = 82800000l; //23 hours
	public static final long MINRETRYPERIOD = 3660000l; //One hour + change
	
	public static final String URL_LOCATIONFORECASTLTS = "http://api.met.no/weatherapi/locationforecastlts/1.1/?lat=";
	public static final int[] CONDITIONTRANSLATIONS = new int[] { 0, // 0
			1, // 1 SUN
			3, // 2 LIGHTCLOUD
			5, // 3 PARTLYCLOUD
			12, // 4 CLOUD
			20, // 5 LIGHTRAINSUN
			24, // 6 LIGHTRAINTHUNDERSUN
			36, // 7 SLEETSUN
			33, // 8 SNOWSUN
			14, // 9 LIGHTRAIN
			27, // 10 RAIN
			23, // 11 RAINTHUNDER
			36, // 12 SLEET
			33, // 13 SNOW
			23, // 14 SNOWTHUNDER
			13, // 15 FOG
			1, // 16 SUN ( used for winter darkness )
			12, // 17 LIGHTCLOUD ( winter darkness )
			14, // 18 LIGHTRAINSUN ( used for winter darkness )
			32, // 19 SNOWSUN ( used for winter darkness )
			23, // 20 SLEETSUNTHUNDER
			23, // 21 SNOWSUNTHUNDER
			21, // 22 LIGHTRAINTHUNDER
			21 // 23 SLEETTHUNDER2
	};

	public Stack<String[]> tree;
	public HashMap<String, String> element;
	public JSONObject jsonWeather, jsonOneDayForecast;
	public ArrayList<Double> x, y;
	public static final String DELIMITERS = " .,;-";
	public static final Time FROMTIME = new Time(Time.TIMEZONE_UTC);
	public static final Time TOTIME = new Time(Time.TIMEZONE_UTC);
	public static final Time LOWDATE = new Time(Time.TIMEZONE_UTC);
	public static final Time HIGHDATE = new Time(Time.TIMEZONE_UTC);
	public static final Time UPDATEDTIME = new Time(Time.TIMEZONE_UTC);
	public static HashMap<String, Double> LOWTEMPS;
	public static HashMap<String, Double> HIGHTEMPS;
	public static HashMap<String, Integer> CONDITIONS;
	public static String CACHEDFORECAST;
	public static long CACHEDFORECASTMILLIS = 0l;
	public static boolean LOCATIONCHANGED;
	public static String LASTUSEDCITY, LASTUSEDCOUNTRY;
	public static double[] FCSTCURVEX, FCSTCURVEY;

	public YrNoWeatherXMLHandler() {
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
		x = new ArrayList<Double>();
		y = new ArrayList<Double>();
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
			FCSTCURVEX = null;
			FCSTCURVEY = null;
			// If either city and country have changed,
			// set the location change flag to true.
		} else if (!LASTUSEDCITY.equals(city)
				|| !LASTUSEDCOUNTRY.equals(country)) {
			LASTUSEDCITY = city;
			LASTUSEDCOUNTRY = country;
			LOCATIONCHANGED = true;
			FCSTCURVEX = null;
			FCSTCURVEY = null;
			// If city and country have not changed,
			// but we've lost the cached forecast or the
			// cached weather is older than 8 hours
			// set the location change flag to true.
		} else if (CACHEDFORECAST == null
				|| System.currentTimeMillis() - CACHEDFORECASTMILLIS > MINTIMEBETWEENREQUESTS) {
			LASTUSEDCITY = city;
			LASTUSEDCOUNTRY = country;
			LOCATIONCHANGED = true;
			FCSTCURVEX = null;
			FCSTCURVEY = null;
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
						YrNoWeatherXMLHandler GXhandler = new YrNoWeatherXMLHandler();
						GXhandler.jsonWeather.putOpt("country2", country);
						GXhandler.jsonWeather.putOpt("city2", city);
						GXhandler.jsonWeather.putOpt("bylatlong", bylatlong);
						GXhandler.jsonWeather.putOpt("longitude_e6",
								longitude * 1000000d);
						GXhandler.jsonWeather.putOpt("latitude_e6",
								latitude * 1000000d);
						xr.setContentHandler(GXhandler);
						xr.setErrorHandler(GXhandler);

						URL url = null;
						if (!bylatlong) {
							Log.e(OMC.OMCSHORT + "YrNoWeather",
									"yr.no does not support weather by location name!");
							return;
							// url = new
							// URL("http://www.google.com/ig/api?oe=utf-8&weather="+city.replace(' ',
							// '+') + "+" + country.replace(' ', '+'));
						} else {
							url = new URL(
									YrNoWeatherXMLHandler.URL_LOCATIONFORECASTLTS
											+ latitude + ";lon=" + longitude);
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
						if (huc != null)
							huc.disconnect();
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
						YrNoWeatherXMLHandler GXhandler = new YrNoWeatherXMLHandler();
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
							Log.i(OMC.OMCSHORT + "YWeather",
									"Interpolating weather from previously-cached forecast.");
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
			Log.i(OMC.OMCSHORT + "YrNoWeather", "Start Building JSON.");
		// We haven't accumulated current conditions yet
		LOWTEMPS = new HashMap<String, Double>();
		HIGHTEMPS = new HashMap<String, Double>();
		CONDITIONS = new HashMap<String, Integer>();

		// yr.no does not return a timestamp in the XML, so default it to
		// 1/1/1970
		Time t = new Time();
		// If the weather station information (international, mostly) doesn't
		// have a timestamp, set the timestamp to be jan 1st, 1970
		t.parse("19700101T000000");
		try {
			jsonWeather.putOpt("current_time", t.format2445());
			jsonWeather.putOpt("current_millis", t.toMillis(false));
			jsonWeather.putOpt("current_local_time", t.format2445());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		// yr.no Weather doesn't return data between tags, so nothing here
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) {
		try {
			if (!tree.isEmpty()) {

				// First, parse the forecast time.
				if (localName.equals("weatherdata")) {
					UPDATEDTIME.parse(atts.getValue("created"));
				}

				if (localName.equals("time")) {
					if (atts.getValue("datatype").equals("forecast")) {
						FROMTIME.parse(atts.getValue("from").replace("-", "")
								.replace(":", ""));
						FROMTIME.switchTimezone(Time.getCurrentTimezone());
						TOTIME.parse(atts.getValue("to").replace("-", "")
								.replace(":", ""));
						TOTIME.switchTimezone(Time.getCurrentTimezone());
						LOWDATE.parse(atts.getValue("to").replace("-", "")
								.replace(":", ""));
						LOWDATE.switchTimezone(Time.getCurrentTimezone());
						LOWDATE.hour -= 7;
						LOWDATE.normalize(false);
					}
				}

				if (localName.equals("temperature")) {
					double tempc = Double.parseDouble(atts.getValue("value"));
					x.add((double) (TOTIME.toMillis(false)));
					y.add(tempc);
					String sHTDay = TOTIME.format("%Y%m%d");
					String sLDDay = LOWDATE.format("%Y%m%d");

					if (OMC.DEBUG)
						Log.i(OMC.OMCSHORT + "YrNoWeather",
								"Temp from " + FROMTIME.format2445() + " to "
										+ TOTIME.format2445() + ":" + tempc);
					if (jsonWeather.optString("temp_c", "missing").equals(
							"missing")) {
						jsonWeather.putOpt("temp_c", tempc);
						jsonWeather.putOpt("temp_f",
								(int) (tempc * 9f / 5f + 32.7f));
						Time now = new Time();
						now.setToNow();
						HIGHTEMPS.put(now.format("%Y%m%d"), tempc);
						LOWTEMPS.put(now.format("%Y%m%d"), tempc);
					}

					if (OMC.DEBUG)
						Log.i(OMC.OMCSHORT + "YrNoWeather", "Day for High: "
								+ sHTDay + "; Day for Low: " + sLDDay);
					if (!HIGHTEMPS.containsKey(sHTDay)) {
						HIGHTEMPS.put(sHTDay, tempc);
					} else {
						if (tempc > HIGHTEMPS.get(sHTDay))
							HIGHTEMPS.put(sHTDay, tempc);
					}
					if (!LOWTEMPS.containsKey(sLDDay)) {
						LOWTEMPS.put(sLDDay, tempc);
					} else {
						if (tempc < LOWTEMPS.get(sLDDay))
							LOWTEMPS.put(sLDDay, tempc);
					}
				}

				if (localName.equals("symbol")) {
					int iConditionCode = CONDITIONTRANSLATIONS[Integer
							.parseInt(atts.getValue("number"))];
					if (jsonWeather.optString("condition_code", "missing")
							.equals("missing")) {
						jsonWeather.putOpt("condition_code", iConditionCode);
						Time now = new Time();
						now.setToNow();
						CONDITIONS.put(now.format("%Y%m%d"), iConditionCode);
					}
					String sCondDay = TOTIME.format("%Y%m%d");
					CONDITIONS.put(sCondDay, iConditionCode);
				}

				if (localName.equals("windDirection")) {
					if (jsonWeather.optString("wind_direction", "missing")
							.equals("missing")) {
						String cond = atts.getValue("name");
						jsonWeather.putOpt("wind_direction", cond);
					}
				}
				if (localName.equals("windSpeed")) {
					if (jsonWeather.optString("wind_speed", "missing").equals(
							"missing")) {
						int cond = (int) (Double.parseDouble(atts
								.getValue("mps")) * 2.2369362920544f + 0.5f);
						jsonWeather.putOpt("wind_speed_mps",
								atts.getValue("mps"));
						jsonWeather.putOpt("wind_speed_mph", cond);
					}
				}
				if (localName.equals("humidity")) {
					if (jsonWeather.optString("humidity_raw", "missing")
							.equals("missing")) {
						String cond = atts.getValue("value");
						jsonWeather.putOpt("humidity_raw", cond);
					}
				}

			}
			tree.push(new String[] { localName });

		} catch (JSONException e) {
			e.printStackTrace();
			try {
				jsonWeather.putOpt("problem_cause", "error");
			} catch (Exception ee) {
			}
		}
	}

	@Override
	public void endElement(String uri, String name, String qName) {
		if (tree.isEmpty())
			return;
		// if (OMC.DEBUG)
		// Log.i(OMC.OMCSHORT + "YrNoWeather", "EndElement." + name);
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
			Log.i(OMC.OMCSHORT + "YrNoWeather", "End Document.");
		// OK we're done parsing the whole document.
		// Since the parse() method is synchronous, we don't need to do anything
		// - just basic cleanup.
		tree.clear();
		tree = null;

		// Build out wind/humidity conditions.
		String humidityString = OMC.RString("humiditycondition")
				+ jsonWeather.optString("humidity_raw") + "%";
		String windString = OMC.RString("windcondition")
				+ jsonWeather.optString("wind_direction") + " @ "
				+ jsonWeather.optString("wind_speed_mph") + " mph";
		try {
			jsonWeather.putOpt("humidity", humidityString);
			jsonWeather.putOpt("wind_condition", windString);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		//
		// Build out the forecast array.
		Time day = new Time();
		day.setToNow();

		// Make sure today's data contains both high and low, too.
		if (!LOWTEMPS.containsKey(day.format("%Y%m%d"))) {
			Time nextday = new Time(day);
			nextday.hour += 24;
			nextday.normalize(false);
			LOWTEMPS.put(day.format("%Y%m%d"),
					LOWTEMPS.get(nextday.format("%Y%m%d")));
		}
		if (!HIGHTEMPS.containsKey(day.format("%Y%m%d"))) {
			HIGHTEMPS.put(day.format("%Y%m%d"),
					LOWTEMPS.get(day.format("%Y%m%d")));
		}

		while (HIGHTEMPS.containsKey(day.format("%Y%m%d"))
				&& LOWTEMPS.containsKey(day.format("%Y%m%d"))) {
			try {
				JSONObject jsonOneDayForecast = new JSONObject();
				jsonOneDayForecast.put("day_of_week", day.format("%a"));
				final int iConditionCode = CONDITIONS.get(day.format("%Y%m%d"));
				jsonOneDayForecast.put("condition_code", iConditionCode);
				jsonOneDayForecast.put("condition",
						OMC.VERBOSEWEATHER[iConditionCode]);
				jsonOneDayForecast.put("condition_lcase",
						OMC.VERBOSEWEATHER[iConditionCode].toLowerCase());
				double lowc = OMC.roundToSignificantFigures(
						LOWTEMPS.get(day.format("%Y%m%d")), 3);
				double highc = OMC.roundToSignificantFigures(
						HIGHTEMPS.get(day.format("%Y%m%d")), 3);
				double lowf = (int) (lowc / 5f * 9f + 32.7f);
				double highf = (int) (highc / 5f * 9f + 32.7f);
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
			}
		}
		// Check if newest curve covers current timestamp, if so, cache this
		// curve

		if (System.currentTimeMillis() > x.get(0)) {
			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "YrNoWeather", "caching a fresh curve");
			double[] ex = new double[x.size()];
			double[] ey = new double[y.size()];
			for (int i = 0; i < x.size(); i++) {
				ex[i] = x.get(i);
				ey[i] = y.get(i);
			}
			FCSTCURVEX = ex;
			FCSTCURVEY = ey;

			// If not, and there is no previously cached forecast curve, cache
			// the curve anyway
		} else if (FCSTCURVEX == null) {
			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "YrNoWeather", "caching a fresh curve");
			double[] ex = new double[x.size()];
			double[] ey = new double[y.size()];
			for (int i = 0; i < x.size(); i++) {
				ex[i] = x.get(i);
				ey[i] = y.get(i);
			}
			FCSTCURVEX = ex;
			FCSTCURVEY = ey;
		} else if (FCSTCURVEX.length == 0) {
			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "YrNoWeather", "caching a fresh curve");
			double[] ex = new double[x.size()];
			double[] ey = new double[y.size()];
			for (int i = 0; i < x.size(); i++) {
				ex[i] = x.get(i);
				ey[i] = y.get(i);
			}
			FCSTCURVEX = ex;
			FCSTCURVEY = ey;
		} else {
			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "YrNoWeather", "reusing cached curve");
		}
		// Finally, try to use the best curve available, falling back on the
		// first datapoint
		// if no curves can be used
		if (System.currentTimeMillis() > FCSTCURVEX[0]) {
			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "YrNoWeather", "Interpolating from curve");
			UnivariateRealFunction fn = new SplineInterpolator().interpolate(
					FCSTCURVEX, FCSTCURVEY);
			try {
				double tempc = fn.value(System.currentTimeMillis());
				jsonWeather.putOpt("temp_c", (int) (tempc + 0.5d));
				jsonWeather.putOpt("temp_f", (int) (tempc * 9f / 5f + 32.7f));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "YrNoWeather", "no interpolation");
		}

		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "YrNoWeather", jsonWeather.toString());

		try {
			if (jsonWeather.optString("city") == null
					|| jsonWeather.optString("city").equals("")) {
				if (!jsonWeather.optString("city2").equals(""))
					jsonWeather.putOpt("city", jsonWeather.optString("city2"));
				else if (!jsonWeather.optString("country2").equals(""))
					jsonWeather.putOpt("city",
							jsonWeather.optString("country2"));
			}

		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
		OMC.PREFS.edit().putString("weather", jsonWeather.toString()).commit();
		OMC.LASTWEATHERREFRESH = System.currentTimeMillis();
		OMC.WEATHERREFRESHSTATUS = OMC.WRS_SUCCESS;

		if (LOCATIONCHANGED)
			CACHEDFORECASTMILLIS = OMC.LASTWEATHERREFRESH;

		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "YrNoWeather",
					"Update Succeeded.  Phone Time:"
							+ new java.sql.Time(OMC.LASTWEATHERREFRESH)
									.toLocaleString());

		Time t = new Time();
		// If the weather station information (international, mostly) doesn't
		// have a timestamp, set the timestamp to be jan 1st, 1970
		t.parse(jsonWeather.optString("current_local_time", "19700101T000000"));

		long lNextWeatherRetryIfFailed = OMC.LASTWEATHERTRY+MINRETRYPERIOD;
		
		// If the weather station info looks too stale (more than 2 hours old),
		// it's because the phone's date/time is wrong.
		// Force the update to the default update period
		if (System.currentTimeMillis() - t.toMillis(false) > 7200000l) {
			OMC.NEXTWEATHERREFRESH = Math.max(
					OMC.LASTWEATHERREFRESH
							+ (1l + Long.parseLong(OMC.PREFS.getString(
									"sWeatherFreq", "60"))) * 60000l,
									lNextWeatherRetryIfFailed);
			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "YrNoWeather", "Weather Station Time:"
						+ new java.sql.Time(t.toMillis(false)).toLocaleString());
			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "YrNoWeather",
						"Weather Station Time Missing or Stale.  Using default interval.");
		} else if (t.toMillis(false) > System.currentTimeMillis()) {
			// If the weather station time is in the future, something is
			// definitely wrong!
			// Force the update to the default update period
			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "YrNoWeather", "Weather Station Time:"
						+ new java.sql.Time(t.toMillis(false)).toLocaleString());
			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "YrNoWeather",
						"Weather Station Time in the future -> phone time is wrong.  Using default interval.");
			OMC.NEXTWEATHERREFRESH = Math.max(
					OMC.LASTWEATHERREFRESH
							+ (1l + Long.parseLong(OMC.PREFS.getString(
									"sWeatherFreq", "60"))) * 60000l,
									lNextWeatherRetryIfFailed);
		} else {
			// If we get a recent weather station timestamp, we try to "catch"
			// the update by setting next update to
			// 29 minutes + default update period
			// after the last station refresh.

			if (OMC.DEBUG)
				Log.i(OMC.OMCSHORT + "YrNoWeather", "Weather Station Time:"
						+ new java.sql.Time(t.toMillis(false)).toLocaleString());
			OMC.NEXTWEATHERREFRESH = Math.max(
					t.toMillis(false)
							+ (29l + Long.parseLong(OMC.PREFS.getString(
									"sWeatherFreq", "60"))) * 60000l,
									lNextWeatherRetryIfFailed);
		}
		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "YrNoWeather",
					"Next Refresh Time:"
							+ new java.sql.Time(OMC.NEXTWEATHERREFRESH)
									.toLocaleString());
		if (LOCATIONCHANGED) OMC.NEXTWEATHERREQUEST = OMC.NEXTWEATHERREFRESH+MINTIMEBETWEENREQUESTS;
		OMC.PREFS.edit()
				.putLong("weather_nextweatherrefresh", OMC.NEXTWEATHERREFRESH)
				.commit();

	}

}