package com.sunnykwong.omc;

import java.io.BufferedInputStream;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class OMCWeatherForecastActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Hide the title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(getResources().getIdentifier("weatherforecast", "layout", OMC.PKGNAME));
		try {
			JSONObject weather = new JSONObject(OMC.PREFS.getString("weather", ""));
			
			setText(findViewById(getResources().getIdentifier("CurrTemp", "id", OMC.PKGNAME)),weather.optString("tempf"));
			setText(findViewById(getResources().getIdentifier("City", "id", OMC.PKGNAME)),weather.optString("city"));
			setText(findViewById(getResources().getIdentifier("Conditions", "id", OMC.PKGNAME)),weather.optString("condition"));

			JSONArray wary = weather.optJSONArray("forecast_conditions");
			for (int i=0; i<wary.length(); i++) {
				JSONObject day = wary.optJSONObject(i);
				setText(findViewById(getResources().getIdentifier("dayofweek"+i, "id", OMC.PKGNAME)),day.optString("day_of_week"));
				setText(findViewById(getResources().getIdentifier("ForecastCond"+i, "id", OMC.PKGNAME)),day.optString("condition"));
				setText(findViewById(getResources().getIdentifier("HighTemp"+i, "id", OMC.PKGNAME)),day.optString("high"));
				setText(findViewById(getResources().getIdentifier("LowTemp"+i, "id", OMC.PKGNAME)),day.optString("low"));
			}
		} catch (JSONException e) {
			Toast.makeText(this, "No Weather Loaded!", Toast.LENGTH_LONG);
			e.printStackTrace();
		}
	}			

	public void setText(View vw, String sText) {
		TextView tv = (TextView)vw;
		tv.setText(sText);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		finish();
		return super.onKeyUp(keyCode, event);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		finish();
		return super.onTouchEvent(event);
	}
}