package com.sunnykwong.omc;

import android.util.Log;
import android.text.format.Time;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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

	static public void updateWeather() {
		ELEMENTS = new ArrayList<HashMap<String, String>>();
		Thread t = new Thread() {
			public void run() {
				try {
					XMLReader xr = XMLReaderFactory.createXMLReader();
					GoogleWeatherXMLHandler GXhandler = new GoogleWeatherXMLHandler();
					xr.setContentHandler(GXhandler);
					xr.setErrorHandler(GXhandler);

					HttpClient client = new DefaultHttpClient();
					HttpGet request = new HttpGet();

					request.setURI(new URI(
							"http://www.google.com/ig/api?weather=77584"));
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
				if (OMC.DEBUG)
					Log.i(OMC.OMCSHORT + "Weather", "Reading " + tree.peek()[0]
							+ " - " + localName);
				try {
					jsonWeather.putOpt(localName, atts.getValue("data"));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else if (tree.peek()[0].equals("forecast_conditions")) {
				if (OMC.DEBUG)
					Log.i(OMC.OMCSHORT + "Weather", "Reading " + tree.peek()[0]
							+ " - " + localName);
				if (jsonOneDayForecast == null)
					jsonOneDayForecast = new JSONObject();
				try {
					jsonOneDayForecast.putOpt(localName, atts.getValue("data"));
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
		OMC.PREFS.edit().putString("weather", jsonWeather.toString()).commit();
	}

}