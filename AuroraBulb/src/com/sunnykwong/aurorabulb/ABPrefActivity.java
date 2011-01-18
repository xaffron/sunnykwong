package com.sunnykwong.aurorabulb;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.DisplayMetrics;
import android.widget.Toast;

public class ABPrefActivity extends PreferenceActivity implements OnPreferenceChangeListener {
	AlertDialog mAD;
	PreferenceCategory bitmapstuff, textstuff;
	PreferenceScreen toplevel;
	StringWriter sw = new StringWriter(10);;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);

		this.getPreferenceManager().setSharedPreferencesName(AB.PREFNAME);
    	this.addPreferencesFromResource(R.xml.abprefs);
    	
    	try {
    		findPreference("sVersion").setTitle(getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName);
    	} catch (NameNotFoundException e) {
    		// package name not found - not a showstopper, so carry on
    	}
    	toplevel = (PreferenceScreen)findPreference("rootPrefs");
    	bitmapstuff = (PreferenceCategory)findPreference("bitmapstuff");
    	textstuff = (PreferenceCategory)findPreference("textstuff");
    	
    	new PrintWriter(sw).format("#%x", AB.PREFS.getInt("textColor", 0));
    	sw.getBuffer().setLength(0);
 
    	DisplayMetrics metrics = new DisplayMetrics();
    	getWindowManager().getDefaultDisplay().getMetrics(metrics);
    	AB.SCRNDPI = metrics.densityDpi;
    	
    	this.setPreferenceScreen((PreferenceScreen)findPreference(AB.PREFSCREENTOSHOW));

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
    	if (preference == findPreference("pickFont")) {
    		AB.PT1.setTypeface(Typeface.createFromAsset(getApplicationContext().getAssets(), (String)newValue));
			AB.PREFS.edit().putString("pickFont", (String)newValue).commit();
			findPreference("pickFont").setSummary("Picked: " + (String)newValue);
			return true;
    	} else if (preference == findPreference("timeShutterDuration")) {
    		int temp;
    		try {
    			temp = Integer.parseInt((String)newValue);
    		} catch (IllegalArgumentException e) {
    			Toast.makeText(this, "Invalid Shutter Duration!", Toast.LENGTH_SHORT).show();
    			return false;
    		}
    		if (temp < 1) {
    			Toast.makeText(this, "Invalid Shutter Duration!", Toast.LENGTH_SHORT).show();
    			return false;
    		}
    		findPreference("timeShutterDuration").setSummary("Shutter at (seconds): " + (String)newValue);
			return true;
    	} else if (preference == findPreference("timePhotoTimer")) {
    		int temp;
    		try {
    			temp = Integer.parseInt((String)newValue);
    		} catch (IllegalArgumentException e) {
    			Toast.makeText(this, "Invalid Photo Timer Duration!", Toast.LENGTH_SHORT).show();
    			return false;
    		}
    		if (temp < 0) {
    			Toast.makeText(this, "Invalid Photo Timer Duration!", Toast.LENGTH_SHORT).show();
    			return false;
    		}
    		findPreference("timePhotoTimer").setSummary("Assuming Cam timer of (seconds): " + (String)newValue);
			return true;
    	} else if (preference == findPreference("pickText")) {
    		if (((String)newValue).equals("")) {
    			Toast.makeText(this, "Zero-length String not allowed!", Toast.LENGTH_SHORT).show();
    			return false;
    		} else {
    			findPreference("pickText").setSummary((String)newValue);
    			AB.PREFS.edit().putString("pickText", (String)newValue).commit();
            	AB.PT1.setTextScaleX(1f);
        		AB.PT1.setTextAlign(Paint.Align.LEFT);
        		int textwidth = (int)AB.PT1.measureText((String)newValue);
        		float fPassDist = (float)textwidth/AB.SCRNDPI;
        		findPreference("pickText").setSummary((String)newValue);
        		findPreference("idealPassDist").setSummary("Ideally: ~" + String.valueOf(Math.round(fPassDist)) + "in./" +String.valueOf(Math.round(fPassDist * 2.54))+ "cm");
    			return true;
    		}
    	}
    	return true;
    }

    public void dialogCancelled() {
    	mAD.cancel();
    	return;
    }

   @Override
    protected void onResume() {
        super.onResume();
   }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
} 
