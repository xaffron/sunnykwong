package com.sunnykwong.omc;

import java.io.BufferedInputStream;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
				final String sSearchText = etSearchBox.getText().toString().replace(".", "").replace(",", "+").replace(" ", "+").trim();
				if (sSearchText.equals("") || sSearchText == null) {
					mHandler.post(mBADRESULT);
					return;
				}
				Toast.makeText(OMCFixedLocationActivity.this,  sSearchText, Toast.LENGTH_LONG).show();
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
		// Find locality
		JSONArray jary = jsonLocations.optJSONArray("results");
		for (int counter = 0; counter < jary.length(); counter++){
			final JSONObject jobj = jary.optJSONObject(counter);
			TextView tv = new TextView(this);
			tv.setPadding(10, 10, 10, 10);
			tv.setTextColor(Color.WHITE);
			tv.setShadowLayer(3f, 1, 1, Color.BLACK);
			tv.setBackgroundColor(Color.DKGRAY);
			
			tv.setText(jobj.optString("formatted_address","error"));
			tv.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					OMC.jsonFIXEDLOCN = jobj;
				}
			});
			llResults.addView(tv);
		}
		llResults.requestLayout();
	}
}