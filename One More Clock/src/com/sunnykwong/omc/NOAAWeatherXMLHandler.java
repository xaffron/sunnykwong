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
	public static HashMap<String, Integer> CONDITIONTRANSLATIONS;

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
		
		// Populating the condition translations
		CONDITIONTRANSLATIONS = new HashMap<String,Integer>();
		CONDITIONTRANSLATIONS.put("Fair ",1);
		CONDITIONTRANSLATIONS.put("Clear ",1);
		CONDITIONTRANSLATIONS.put("Fair with Haze ",1);
		CONDITIONTRANSLATIONS.put("Clear with Haze ",1);
		CONDITIONTRANSLATIONS.put("Fair and Breezy ",1);
		CONDITIONTRANSLATIONS.put("Clear and Breezy",1);
		CONDITIONTRANSLATIONS.put("Drizzle ",16);
		CONDITIONTRANSLATIONS.put("Light Drizzle ",16);
		CONDITIONTRANSLATIONS.put("Heavy Drizzle ",16);
		CONDITIONTRANSLATIONS.put("Drizzle Fog/Mist ",16);
		CONDITIONTRANSLATIONS.put("Light Drizzle Fog/Mist ",16);
		CONDITIONTRANSLATIONS.put("Heavy Drizzle Fog/Mist ",16);
		CONDITIONTRANSLATIONS.put("Drizzle Fog ",16);
		CONDITIONTRANSLATIONS.put("Light Drizzle Fog ",16);
		CONDITIONTRANSLATIONS.put("Heavy Drizzle Fog",16);
		CONDITIONTRANSLATIONS.put("Dust ",6);
		CONDITIONTRANSLATIONS.put("Low Drifting Dust ",6);
		CONDITIONTRANSLATIONS.put("Blowing Dust ",6);
		CONDITIONTRANSLATIONS.put("Sand ",6);
		CONDITIONTRANSLATIONS.put("Blowing Sand ",6);
		CONDITIONTRANSLATIONS.put("Low Drifting Sand ",6);
		CONDITIONTRANSLATIONS.put("Dust/Sand Whirls ",6);
		CONDITIONTRANSLATIONS.put("Dust/Sand Whirls in Vicinity ",6);
		CONDITIONTRANSLATIONS.put("Dust Storm ",6);
		CONDITIONTRANSLATIONS.put("Heavy Dust Storm ",6);
		CONDITIONTRANSLATIONS.put("Dust Storm in Vicinity ",6);
		CONDITIONTRANSLATIONS.put("Sand Storm ",6);
		CONDITIONTRANSLATIONS.put("Heavy Sand Storm ",6);
		CONDITIONTRANSLATIONS.put("Sand Storm in Vicinity",6);
		CONDITIONTRANSLATIONS.put("Low Drifting Snow ",28);
		CONDITIONTRANSLATIONS.put("Fog/Mist ",13);
		CONDITIONTRANSLATIONS.put("Fog ",13);
		CONDITIONTRANSLATIONS.put("Freezing Fog ",13);
		CONDITIONTRANSLATIONS.put("Shallow Fog ",13);
		CONDITIONTRANSLATIONS.put("Partial Fog ",13);
		CONDITIONTRANSLATIONS.put("Patches of Fog ",13);
		CONDITIONTRANSLATIONS.put("Fog in Vicinity ",13);
		CONDITIONTRANSLATIONS.put("Freezing Fog in Vicinity ",13);
		CONDITIONTRANSLATIONS.put("Shallow Fog in Vicinity ",13);
		CONDITIONTRANSLATIONS.put("Partial Fog in Vicinity ",13);
		CONDITIONTRANSLATIONS.put("Patches of Fog in Vicinity ",13);
		CONDITIONTRANSLATIONS.put("Showers in Vicinity Fog ",13);
		CONDITIONTRANSLATIONS.put("Light Freezing Fog ",13);
		CONDITIONTRANSLATIONS.put("Heavy Freezing Fog",13);
		CONDITIONTRANSLATIONS.put("Freezing Rain ",37);
		CONDITIONTRANSLATIONS.put("Freezing Drizzle ",37);
		CONDITIONTRANSLATIONS.put("Light Freezing Rain ",37);
		CONDITIONTRANSLATIONS.put("Light Freezing Drizzle ",37);
		CONDITIONTRANSLATIONS.put("Heavy Freezing Rain ",37);
		CONDITIONTRANSLATIONS.put("Heavy Freezing Drizzle ",37);
		CONDITIONTRANSLATIONS.put("Freezing Rain in Vicinity ",37);
		CONDITIONTRANSLATIONS.put("Freezing Drizzle in Vicinity",37);
		CONDITIONTRANSLATIONS.put("Freezing Rain Rain ",37);
		CONDITIONTRANSLATIONS.put("Light Freezing Rain Rain ",37);
		CONDITIONTRANSLATIONS.put("Heavy Freezing Rain Rain ",37);
		CONDITIONTRANSLATIONS.put("Rain Freezing Rain ",37);
		CONDITIONTRANSLATIONS.put("Light Rain Freezing Rain ",37);
		CONDITIONTRANSLATIONS.put("Heavy Rain Freezing Rain ",37);
		CONDITIONTRANSLATIONS.put("Freezing Drizzle Rain ",37);
		CONDITIONTRANSLATIONS.put("Light Freezing Drizzle Rain ",37);
		CONDITIONTRANSLATIONS.put("Heavy Freezing Drizzle Rain ",37);
		CONDITIONTRANSLATIONS.put("Rain Freezing Drizzle ",37);
		CONDITIONTRANSLATIONS.put("Light Rain Freezing Drizzle ",37);
		CONDITIONTRANSLATIONS.put("Heavy Rain Freezing Drizzle",37);
		CONDITIONTRANSLATIONS.put("Haze",7);
		CONDITIONTRANSLATIONS.put("Heavy Rain Showers ",26);
		CONDITIONTRANSLATIONS.put("Heavy Showers Rain ",26);
		CONDITIONTRANSLATIONS.put("Heavy Rain Showers Fog/Mist ",26);
		CONDITIONTRANSLATIONS.put("Heavy Showers Rain Fog/Mist ",26);
		CONDITIONTRANSLATIONS.put("Heavy Rain ",26);
		CONDITIONTRANSLATIONS.put("Heavy Rain Fog/Mist ",26);
		CONDITIONTRANSLATIONS.put("Heavy Rain Fog",26);
		CONDITIONTRANSLATIONS.put("Freezing Rain Snow ",35);
		CONDITIONTRANSLATIONS.put("Light Freezing Rain Snow ",35);
		CONDITIONTRANSLATIONS.put("Heavy Freezing Rain Snow ",35);
		CONDITIONTRANSLATIONS.put("Freezing Drizzle Snow ",35);
		CONDITIONTRANSLATIONS.put("Light Freezing Drizzle Snow ",35);
		CONDITIONTRANSLATIONS.put("Heavy Freezing Drizzle Snow ",35);
		CONDITIONTRANSLATIONS.put("Snow Freezing Rain ",35);
		CONDITIONTRANSLATIONS.put("Light Snow Freezing Rain ",35);
		CONDITIONTRANSLATIONS.put("Heavy Snow Freezing Rain ",35);
		CONDITIONTRANSLATIONS.put("Snow Freezing Drizzle ",35);
		CONDITIONTRANSLATIONS.put("Light Snow Freezing Drizzle ",35);
		CONDITIONTRANSLATIONS.put("Heavy Snow Freezing Drizzle",35);
		CONDITIONTRANSLATIONS.put("Light Rain Showers ",14);
		CONDITIONTRANSLATIONS.put("Light Rain and Breezy ",14);
		CONDITIONTRANSLATIONS.put("Light Showers Rain ",14);
		CONDITIONTRANSLATIONS.put("Light Rain Showers Fog/Mist ",14);
		CONDITIONTRANSLATIONS.put("Light Showers Rain Fog/Mist ",14);
		CONDITIONTRANSLATIONS.put("Light Rain ",14);
		CONDITIONTRANSLATIONS.put("Light Rain Fog/Mist ",14);
		CONDITIONTRANSLATIONS.put("Light Rain Fog ",14);
		CONDITIONTRANSLATIONS.put("Light Snow ",29);
		CONDITIONTRANSLATIONS.put("Light Snow Fog/Mist ",29);
		CONDITIONTRANSLATIONS.put("Light Snow Fog ",29);
		CONDITIONTRANSLATIONS.put("Blowing Snow ",29);
		CONDITIONTRANSLATIONS.put("Snow Low Drifting Snow ",29);
		CONDITIONTRANSLATIONS.put("Light Snow Low Drifting Snow ",29);
		CONDITIONTRANSLATIONS.put("Light Snow Blowing Snow ",29);
		CONDITIONTRANSLATIONS.put("Light Snow Blowing Snow Fog/Mist ",29);
		CONDITIONTRANSLATIONS.put("Mostly Cloudy ",10);
		CONDITIONTRANSLATIONS.put("Mostly Cloudy with Haze ",10);
		CONDITIONTRANSLATIONS.put("Mostly Cloudy and Breezy",10);
		CONDITIONTRANSLATIONS.put("Overcast ",11);
		CONDITIONTRANSLATIONS.put("Overcast with Haze ",11);
		CONDITIONTRANSLATIONS.put("Overcast and Breezy",11);
		CONDITIONTRANSLATIONS.put("A Few Clouds ",5);
		CONDITIONTRANSLATIONS.put("A Few Clouds with Haze ",5);
		CONDITIONTRANSLATIONS.put("A Few Clouds and Breezy",5);
		CONDITIONTRANSLATIONS.put("Partly Cloudy ",5);
		CONDITIONTRANSLATIONS.put("Partly Cloudy with Haze ",5);
		CONDITIONTRANSLATIONS.put("Partly Cloudy and Breezy",5);
		CONDITIONTRANSLATIONS.put("Rain Showers ",27);
		CONDITIONTRANSLATIONS.put("Showers Rain ",27);
		CONDITIONTRANSLATIONS.put("Rain Showers in Vicinity Fog/Mist ",27);
		CONDITIONTRANSLATIONS.put("Showers Rain Fog/Mist ",27);
		CONDITIONTRANSLATIONS.put("Showers Rain in Vicinity Fog/Mist",27);
		CONDITIONTRANSLATIONS.put("Rain ",27);
		CONDITIONTRANSLATIONS.put("Rain Fog/Mist ",27);
		CONDITIONTRANSLATIONS.put("Rain Fog ",27);
		CONDITIONTRANSLATIONS.put("Rain Ice Pellets ",38);
		CONDITIONTRANSLATIONS.put("Light Rain Ice Pellets ",38);
		CONDITIONTRANSLATIONS.put("Heavy Rain Ice Pellets ",38);
		CONDITIONTRANSLATIONS.put("Drizzle Ice Pellets ",38);
		CONDITIONTRANSLATIONS.put("Light Drizzle Ice Pellets ",38);
		CONDITIONTRANSLATIONS.put("Heavy Drizzle Ice Pellets ",38);
		CONDITIONTRANSLATIONS.put("Ice Pellets Rain ",38);
		CONDITIONTRANSLATIONS.put("Light Ice Pellets Rain ",38);
		CONDITIONTRANSLATIONS.put("Heavy Ice Pellets Rain ",38);
		CONDITIONTRANSLATIONS.put("Ice Pellets Drizzle ",38);
		CONDITIONTRANSLATIONS.put("Light Ice Pellets Drizzle ",38);
		CONDITIONTRANSLATIONS.put("Heavy Ice Pellets Drizzle",38);
		CONDITIONTRANSLATIONS.put("Rain Snow ",38);
		CONDITIONTRANSLATIONS.put("Light Rain Snow ",38);
		CONDITIONTRANSLATIONS.put("Heavy Rain Snow ",38);
		CONDITIONTRANSLATIONS.put("Snow Rain ",38);
		CONDITIONTRANSLATIONS.put("Light Snow Rain ",38);
		CONDITIONTRANSLATIONS.put("Heavy Snow Rain ",38);
		CONDITIONTRANSLATIONS.put("Drizzle Snow ",38);
		CONDITIONTRANSLATIONS.put("Light Drizzle Snow ",38);
		CONDITIONTRANSLATIONS.put("Heavy Drizzle Snow ",38);
		CONDITIONTRANSLATIONS.put("Snow Drizzle ",38);
		CONDITIONTRANSLATIONS.put("Light Snow Drizzle ",38);
		CONDITIONTRANSLATIONS.put("Heavy Drizzle Snow",38);
		CONDITIONTRANSLATIONS.put("Rain Showers in Vicinity ",15);
		CONDITIONTRANSLATIONS.put("Showers Rain in Vicinity ",15);
		CONDITIONTRANSLATIONS.put("Rain Showers Fog/Mist ",15);
		CONDITIONTRANSLATIONS.put("Thunderstorm in Vicinity ",23);
		CONDITIONTRANSLATIONS.put("Thunderstorm in Vicinity Fog ",23);
		CONDITIONTRANSLATIONS.put("Thunderstorm in Vicinity Haze",23);
		CONDITIONTRANSLATIONS.put("Showers in Vicinity",17);
		CONDITIONTRANSLATIONS.put("Showers in Vicinity Fog/Mist ",17);
		CONDITIONTRANSLATIONS.put("Showers in Vicinity Fog ",17);
		CONDITIONTRANSLATIONS.put("Showers in Vicinity Haze",17);
		CONDITIONTRANSLATIONS.put("Ice Pellets ",36);
		CONDITIONTRANSLATIONS.put("Light Ice Pellets ",36);
		CONDITIONTRANSLATIONS.put("Heavy Ice Pellets ",36);
		CONDITIONTRANSLATIONS.put("Ice Pellets in Vicinity ",36);
		CONDITIONTRANSLATIONS.put("Showers Ice Pellets ",36);
		CONDITIONTRANSLATIONS.put("Thunderstorm Ice Pellets ",36);
		CONDITIONTRANSLATIONS.put("Ice Crystals ",36);
		CONDITIONTRANSLATIONS.put("Hail ",36);
		CONDITIONTRANSLATIONS.put("Small Hail/Snow Pellets ",36);
		CONDITIONTRANSLATIONS.put("Light Small Hail/Snow Pellets ",36);
		CONDITIONTRANSLATIONS.put("Heavy small Hail/Snow Pellets ",36);
		CONDITIONTRANSLATIONS.put("Showers Hail ",36);
		CONDITIONTRANSLATIONS.put("Hail Showers",36);
		CONDITIONTRANSLATIONS.put("Smoke",9);
		CONDITIONTRANSLATIONS.put("Snow ",33);
		CONDITIONTRANSLATIONS.put("Heavy Snow ",33);
		CONDITIONTRANSLATIONS.put("Snow Fog/Mist ",33);
		CONDITIONTRANSLATIONS.put("Heavy Snow Fog/Mist ",33);
		CONDITIONTRANSLATIONS.put("Snow Fog ",33);
		CONDITIONTRANSLATIONS.put("Heavy Snow Fog ",33);
		CONDITIONTRANSLATIONS.put("Snow Blowing Snow ",33);
		CONDITIONTRANSLATIONS.put("Heavy Snow Low Drifting Snow ",33);
		CONDITIONTRANSLATIONS.put("Heavy Snow Blowing Snow ",33);
		CONDITIONTRANSLATIONS.put("Thunderstorm Snow ",33);
		CONDITIONTRANSLATIONS.put("Light Thunderstorm Snow ",33);
		CONDITIONTRANSLATIONS.put("Heavy Thunderstorm Snow ",33);
		CONDITIONTRANSLATIONS.put("Snow Grains ",33);
		CONDITIONTRANSLATIONS.put("Light Snow Grains ",33);
		CONDITIONTRANSLATIONS.put("Heavy Snow Grains ",33);
		CONDITIONTRANSLATIONS.put("Heavy Blowing Snow ",33);
		CONDITIONTRANSLATIONS.put("Blowing Snow in Vicinity",33);
		CONDITIONTRANSLATIONS.put("Snow Showers ",31);
		CONDITIONTRANSLATIONS.put("Light Snow Showers ",31);
		CONDITIONTRANSLATIONS.put("Heavy Snow Showers ",31);
		CONDITIONTRANSLATIONS.put("Showers Snow ",31);
		CONDITIONTRANSLATIONS.put("Light Showers Snow ",31);
		CONDITIONTRANSLATIONS.put("Heavy Showers Snow ",31);
		CONDITIONTRANSLATIONS.put("Snow Showers Fog/Mist ",31);
		CONDITIONTRANSLATIONS.put("Light Snow Showers Fog/Mist ",31);
		CONDITIONTRANSLATIONS.put("Heavy Snow Showers Fog/Mist ",31);
		CONDITIONTRANSLATIONS.put("Showers Snow Fog/Mist ",31);
		CONDITIONTRANSLATIONS.put("Light Showers Snow Fog/Mist ",31);
		CONDITIONTRANSLATIONS.put("Heavy Showers Snow Fog/Mist ",31);
		CONDITIONTRANSLATIONS.put("Snow Showers Fog ",31);
		CONDITIONTRANSLATIONS.put("Light Snow Showers Fog ",31);
		CONDITIONTRANSLATIONS.put("Heavy Snow Showers Fog ",31);
		CONDITIONTRANSLATIONS.put("Showers Snow Fog ",31);
		CONDITIONTRANSLATIONS.put("Light Showers Snow Fog ",31);
		CONDITIONTRANSLATIONS.put("Heavy Showers Snow Fog ",31);
		CONDITIONTRANSLATIONS.put("Showers in Vicinity Snow ",31);
		CONDITIONTRANSLATIONS.put("Snow Showers in Vicinity ",31);
		CONDITIONTRANSLATIONS.put("Snow Showers in Vicinity Fog/Mist ",31);
		CONDITIONTRANSLATIONS.put("Snow Showers in Vicinity Fog ",31);
		CONDITIONTRANSLATIONS.put("Thunderstorm ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Rain ",21);
		CONDITIONTRANSLATIONS.put("Light Thunderstorm Rain ",21);
		CONDITIONTRANSLATIONS.put("Heavy Thunderstorm Rain ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Rain Fog/Mist ",21);
		CONDITIONTRANSLATIONS.put("Light Thunderstorm Rain Fog/Mist ",21);
		CONDITIONTRANSLATIONS.put("Heavy Thunderstorm Rain Fog and Windy ",21);
		CONDITIONTRANSLATIONS.put("Heavy Thunderstorm Rain Fog/Mist ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Showers in Vicinity ",21);
		CONDITIONTRANSLATIONS.put("Light Thunderstorm Rain Haze ",21);
		CONDITIONTRANSLATIONS.put("Heavy Thunderstorm Rain Haze ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Fog ",21);
		CONDITIONTRANSLATIONS.put("Light Thunderstorm Rain Fog ",21);
		CONDITIONTRANSLATIONS.put("Heavy Thunderstorm Rain Fog ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Light Rain ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Heavy Rain ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Rain Fog/Mist ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Light Rain Fog/Mist ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Heavy Rain Fog/Mist ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm in Vicinity Fog/Mist ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Showers in Vicinity ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm in Vicinity Haze ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Haze in Vicinity ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Light Rain Haze ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Heavy Rain Haze ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Fog ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Light Rain Fog ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Heavy Rain Fog ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Hail ",21);
		CONDITIONTRANSLATIONS.put("Light Thunderstorm Rain Hail ",21);
		CONDITIONTRANSLATIONS.put("Heavy Thunderstorm Rain Hail ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Rain Hail Fog/Mist ",21);
		CONDITIONTRANSLATIONS.put("Light Thunderstorm Rain Hail Fog/Mist ",21);
		CONDITIONTRANSLATIONS.put("Heavy Thunderstorm Rain Hail Fog/Hail ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Showers in Vicinity Hail ",21);
		CONDITIONTRANSLATIONS.put("Light Thunderstorm Rain Hail Haze ",21);
		CONDITIONTRANSLATIONS.put("Heavy Thunderstorm Rain Hail Haze ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Hail Fog ",21);
		CONDITIONTRANSLATIONS.put("Light Thunderstorm Rain Hail Fog ",21);
		CONDITIONTRANSLATIONS.put("Heavy Thunderstorm Rain Hail Fog ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Light Rain Hail ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Heavy Rain Hail ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Rain Hail Fog/Mist ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Light Rain Hail Fog/Mist ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Heavy Rain Hail Fog/Mist ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm in Vicinity Hail ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm in Vicinity Hail Haze ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Haze in Vicinity Hail ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Light Rain Hail Haze ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Heavy Rain Hail Haze ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Hail Fog ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Light Rain Hail Fog ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Heavy Rain Hail Fog ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Small Hail/Snow Pellets ",21);
		CONDITIONTRANSLATIONS.put("Thunderstorm Rain Small Hail/Snow Pellets ",21);
		CONDITIONTRANSLATIONS.put("Light Thunderstorm Rain Small Hail/Snow Pellets ",21);
		CONDITIONTRANSLATIONS.put("Heavy Thunderstorm Rain Small Hail/Snow Pellets",21);
		CONDITIONTRANSLATIONS.put("Windy ",39);
		CONDITIONTRANSLATIONS.put("Breezy ",39);
		CONDITIONTRANSLATIONS.put("Fair and Windy ",39);
		CONDITIONTRANSLATIONS.put("A Few Clouds and Windy ",39);
		CONDITIONTRANSLATIONS.put("Partly Cloudy and Windy ",39);
		CONDITIONTRANSLATIONS.put("Mostly Cloudy and Windy ",39);
		CONDITIONTRANSLATIONS.put("Overcast and Windy",39);
		CONDITIONTRANSLATIONS.put("Funnel Cloud ",39);
		CONDITIONTRANSLATIONS.put("Funnel Cloud in Vicinity ",39);
		CONDITIONTRANSLATIONS.put("Tornado/Water Spout",39);
		
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
					String sCondition = atts.getValue("weather-summary").toLowerCase();
					jsonWeather.put("condition_code",CONDITIONTRANSLATIONS.get(sCondition));
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