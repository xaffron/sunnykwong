package com.sunnykwong.omc;

import java.net.URI;
import java.util.StringTokenizer;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class OMCFixedLocationActivity extends Activity {

	Button btnSearch;
	EditText etSearchBox;
	LinearLayout llResults;
	Handler mHandler;
	JSONObject jsonLocations;
	final Runnable mGOODRESULT = new Runnable() {
		
		@Override
		public void run() {
			btnSearch.setEnabled(true);
			populateResults();
		}
	}; 
	final Runnable mBADRESULT = new Runnable() {
		
		@Override
		public void run() {
			Toast.makeText(OMCFixedLocationActivity.this,  "No Results Found.\nPlease Try Again.", Toast.LENGTH_LONG).show();
			btnSearch.setEnabled(true);
		}
	}; 
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mHandler= new Handler();
		//Hide the title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(getResources().getIdentifier("fixedlocation", "layout", OMC.PKGNAME));
		btnSearch = (Button)findViewById(getResources().getIdentifier("SearchButton", "id", OMC.PKGNAME));
		etSearchBox = (EditText)findViewById(getResources().getIdentifier("SearchBox", "id", OMC.PKGNAME));
		llResults = (LinearLayout)findViewById(getResources().getIdentifier("SearchResults", "id", OMC.PKGNAME));
		
		btnSearch.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!OMC.isConnected()) {
					Toast.makeText(OMCFixedLocationActivity.this,  "No Network Connection.\nPlease Try Again.", Toast.LENGTH_LONG).show();
					mHandler.post(mBADRESULT);
					return;
				}
				final String sSearchText = etSearchBox.getText().toString()
						.trim()
						.replace(".", "")
						.replace(",", "+")
						.replace(" ", "+");
				if (sSearchText.equals("") || sSearchText == null) {
					mHandler.post(mBADRESULT);
					return;
				}

				Thread t = new Thread() {
					public void run() {
						try {
							HttpClient client = new DefaultHttpClient();
							HttpGet request = new HttpGet();
							request.setURI(new URI("http://maps.googleapis.com/maps/api/geocode/json?sensor=false&address="+sSearchText));
							HttpResponse response = client.execute(request);
							jsonLocations = OMC.streamToJSONObject(response.getEntity().getContent());
							Log.i(OMC.OMCSHORT + "FixedLocn", jsonLocations.toString(5));
							if (jsonLocations.optString("status").equals("ZERO_RESULTS")) {
								// Not ok response - do nothing
								mHandler.post(mBADRESULT);
							} else {
								mHandler.post(mGOODRESULT);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					};
				};
				t.start();
			}
		});
	}

	public void populateResults() {
		
		llResults.removeAllViews();
		View topseparator = new View(this);
		topseparator.setBackgroundColor(Color.LTGRAY);
		topseparator.setMinimumHeight(1);
		llResults.addView(topseparator);
		// Find locality
		JSONArray jary = jsonLocations.optJSONArray("results");
		for (int counter = 0; counter < jary.length(); counter++){
			final JSONObject jobj = jary.optJSONObject(counter);
			final TextView tv = new TextView(this);
			tv.setPadding(10, 10, 10, 10);
			tv.setMinimumHeight(50);
			tv.setTextColor(Color.WHITE);
			tv.setShadowLayer(3f, 1, 1, Color.BLACK);
			tv.setBackgroundColor(Color.DKGRAY);
			
			tv.setText(jobj.optString("formatted_address","error"));
			tv.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					tv.setBackgroundColor(Color.LTGRAY);
					
					OMC.jsonFIXEDLOCN = new JSONObject();
					StringTokenizer st = new StringTokenizer(jobj.optString("formatted_address","error"),",");
					String city="";
					try {
						if (st.hasMoreElements()) {
							city = st.nextToken().trim();
							OMC.jsonFIXEDLOCN.putOpt("city",city);
						} else {
							OMC.jsonFIXEDLOCN.putOpt("city","Unknown");
						}
						if (st.hasMoreElements()) {
							OMC.jsonFIXEDLOCN.putOpt("country",st.nextToken().trim());
						} else {
							OMC.jsonFIXEDLOCN.putOpt("country",city);
						}
						OMC.jsonFIXEDLOCN.putOpt("latitude",jobj.getJSONObject("geometry").getJSONObject("location").getDouble("lat"));
						OMC.jsonFIXEDLOCN.putOpt("longitude",jobj.getJSONObject("geometry").getJSONObject("location").getDouble("lng"));
	
						OMC.PREFS.edit().putString("weathersetting", "specific").commit();
						
						OMC.PREFS.edit().putString("weather_fixedlocation", OMC.jsonFIXEDLOCN.toString()).commit();
						GoogleWeatherXMLHandler.updateWeather();
						finish();
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(OMCFixedLocationActivity.this, "Unknown Error Occurred.\nPlease Try Again.", Toast.LENGTH_LONG).show();
						tv.setBackgroundColor(Color.DKGRAY);
					}
				}
			});
			llResults.addView(tv);
			View separator = new View(this);
			separator.setBackgroundColor(Color.LTGRAY);
			separator.setMinimumHeight(1);
			llResults.addView(separator);
		}
		llResults.requestLayout();
	}
}