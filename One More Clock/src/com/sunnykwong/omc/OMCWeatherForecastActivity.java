package com.sunnykwong.omc;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class OMCWeatherForecastActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Hide the title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(OMC.RLayoutId("weatherforecast"));
		if (OMC.PREFS.getString("weathersetting", "bylatlong").equals("disabled")) {
			Toast.makeText(this, "Weather Disabled, or No Weather Loaded!", Toast.LENGTH_LONG).show();
			finish();
		} else {
			try {
				final JSONObject weather = new JSONObject(OMC.PREFS.getString("weather", ""));
				String sWeatherDigits = "--";
				if (OMC.PREFS.getString("weatherDisplay", "f").equals("f")) {
					sWeatherDigits = weather.optString("temp_f", "--")+"°F";
				} else {
					sWeatherDigits = weather.optString("temp_c", "--")+"°C";
				}
				float fStretch = Math.min(1f,120f/(sWeatherDigits.length()*40f));
				setText(findViewById(OMC.RId("CurrTemp")),sWeatherDigits,fStretch);
	
				String sCity = weather.optString("city") + ",\n" + weather.optString("country2")+" ";
				fStretch = Math.min(1f,800f/(sCity.length()*40f));
				setText(findViewById(OMC.RId("City")),sCity,fStretch);
				setText(findViewById(OMC.RId("Conditions")),weather.optString("condition") + " | " + weather.optString("wind_condition") + " " + weather.optString("humidity"));
				Time t = new Time();
				t.set(System.currentTimeMillis());
				String sTimeOfDay="day";
				if (t.hour>=6 && t.hour<=18) {
					//Daytime
				} else {
					//Nighttime
					sTimeOfDay="night";
				}
				((ImageView)findViewById(OMC.RId("ConditionImage"))).setImageBitmap(OMC.getBitmap(OMC.DEFAULTTHEME, "w-"+weather.optString("condition_lcase","--")+"-"+sTimeOfDay+".png"));
				JSONArray wary = weather.optJSONArray("zzforecast_conditions");
				for (int i=0; i<Math.min(wary.length(),4); i++) {
					JSONObject day = wary.optJSONObject(i);
					setText(findViewById(OMC.RId("dayofweek"+i)),day.optString("day_of_week"));
					((ImageView)findViewById(OMC.RId("ConditionImage"+i))).setImageBitmap(OMC.getBitmap(OMC.DEFAULTTHEME, "w-"+day.optString("condition_lcase","--")+"-day.png"));
					setText(findViewById(OMC.RId("ForecastCond"+i)),day.optString("condition"));
					if (OMC.PREFS.getString("weatherDisplay", "f").equals("f")) {
						setText(findViewById(OMC.RId("HighTemp"+i)),day.optString("high")+"°F");
						setText(findViewById(OMC.RId("LowTemp"+i)),day.optString("low")+"°F");
					} else {
						setText(findViewById(OMC.RId("HighTemp"+i)),day.optString("high_c")+"°C");
						setText(findViewById(OMC.RId("LowTemp"+i)),day.optString("low_c")+"°C");
					}
//				    TextView gradient = (TextView)findViewById(getResources().OMC.RId("divider"+i));
//				    final int iDay = i;
//				    gradient.setOnClickListener(new View.OnClickListener() {
//						
//						@Override
//						public void onClick(View v) {
//							Intent accu = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.accuweather.com/m/Details"+iDay+".aspx?lat="+weather.optLong("latitude_e6",0l)/1000000d
//									+ "&lon=" + weather.optLong("longitude_e6",0l)/1000000d));
//							startActivity(accu);
//							finish();
//						}
//					});
				}
				Time tStation = new Time();
				tStation.parse(weather.optString("current_local_time","19700101T000000"));
				if (tStation.year < 1980) tStation.set(OMC.LASTWEATHERREFRESH);
					
				String sWProvider = OMC.PREFS.getString("weatherProvider", "7timer");
				if (sWProvider.equals("ig")) {
					setText(findViewById(OMC.RId("LastUpdate")),"Weather updated from Google API at " + tStation.format("%R")  );
				} else if (sWProvider.equals("yr")) {
					setText(findViewById(OMC.RId("LastUpdate")),"Weather forecast from yr.no, delivered by the Norwegian Meteorological Institute and the NRK."  );
				} else if (sWProvider.equals("7timer")) {
					setText(findViewById(OMC.RId("LastUpdate")),"7Timer! forecasts by Ye Quanzhi, supported by the Shanghai Astronomical Observatory, Chinese Academy of Sciences."  );
				} else {
					setText(findViewById(OMC.RId("LastUpdate")),"Weather forecast from yr.no, delivered by the Norwegian Meteorological Institute and the NRK."  );
				}  

				TextView acculink = ((TextView)findViewById(OMC.RId("AccuLink")));
				acculink.setText("Tap for Alternate Forecasts from AccuWeather®");
				acculink.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent accu = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.accuweather.com/m/Forecast.aspx?lat="+weather.optLong("latitude_e6",0l)/1000000d
								+ "&lon=" + weather.optLong("longitude_e6",0l)/1000000d));
						startActivity(accu);
						finish();
					}
				});
			} catch (Exception e) {
				Toast.makeText(this, "Weather Disabled, or No Weather Loaded!", Toast.LENGTH_LONG).show();
				e.printStackTrace();
				finish();
			}
		}
	}			

	public void setText(View vw, String sText) {
		setText(vw,sText,1f);
	}	
	
	public void setText(View vw, String sText, float stretchX) {
		TextView tv = (TextView)vw;
		tv.setTextScaleX(stretchX);
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