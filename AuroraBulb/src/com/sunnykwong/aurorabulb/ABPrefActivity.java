package com.sunnykwong.aurorabulb;

import android.graphics.Canvas;
import com.android.settings.activities.ColorPickerDialog;
import android.app.Activity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnKeyListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.widget.Toast;
import java.io.StringWriter;
import java.io.PrintWriter;
import android.util.DisplayMetrics;
import android.graphics.Typeface;

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
    	
    	findPreference("showWhat").setOnPreferenceChangeListener(this);
    	findPreference("pickBitmap").setOnPreferenceChangeListener(this);
    	findPreference("pickFont").setOnPreferenceChangeListener(this);
    	findPreference("timeShutterDuration").setOnPreferenceChangeListener(this);
    	findPreference("timePhotoTimer").setOnPreferenceChangeListener(this);
		findPreference("pickText").setOnPreferenceChangeListener(this);

		updateSummaries();
    	
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

    // If user clicks on a preference...
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
    	if (preference == findPreference("showWhat")) {
    		if (AB.PREFS.getString("showWhat","bitmap").equals("text")) {
    			AB.PREFS.edit().putString("showWhat", "bitmap").commit();
    		} else {
    			AB.PREFS.edit().putString("showWhat", "text").commit();
    		}
    		updateSummaries();
    	}
    	if (preference == findPreference("pickBitmap")) {
            Intent intentBrowseFiles = new
            Intent(Intent.ACTION_GET_CONTENT);
                            intentBrowseFiles.setType("image/*");

            intentBrowseFiles.addCategory(Intent.CATEGORY_OPENABLE);     		
            startActivityForResult(intentBrowseFiles, 1);
    	}
    	return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    // The result is obtained in onActivityResult:
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 ) { // From anim
	    	mAD = new AlertDialog.Builder(this)
    		.setTitle("Did you like the photo?")
    		.setMessage("For best results:\n- Lower your camera ISO\n- Lower camera f/number\n- Set your phone's screen brightness to 70-80% of maximum!")
    	    .setCancelable(true)
    	    .setOnKeyListener(new OnKeyListener() {
    	    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
    	    		dialogCancelled();
    	    		return true;
    	    	};
    	    }).create();
	    	mAD.show();
			return;
		} else if (requestCode == 1) {  // From pick Bitmap
			if (data!=null) {
				findPreference("pickBitmap").setSummary("Selected " + data.getDataString());
			}
		}
}

    
    public void dialogCancelled() {
    	mAD.cancel();
    	return;
    }
    


   @Override
    protected void onResume() {
        super.onResume();
   }
   
   public void updateSummaries() {
    	findPreference("pickFont").setSummary(AB.PREFS.getString("pickFont", "Unibody 8-SmallCaps.otf"));
		findPreference("pickText").setSummary(AB.PREFS.getString("pickText", "Aurora"));
		findPreference("timeShutterDuration").setSummary("Shutter at (seconds): " + AB.PREFS.getString("timeShutterDuration", "10"));
		findPreference("timePhotoTimer").setSummary("Assuming Cam timer of (seconds): " + AB.PREFS.getString("timePhotoTimer", "10"));
    	AB.PT1.setTextScaleX(1f);
		AB.PT1.setTextAlign(Paint.Align.LEFT);
		int textwidth = (int)AB.PT1.measureText(AB.PREFS.getString("pickText", "Aurora"));
		float fPassDist = (float)textwidth/AB.SCRNDPI;
		findPreference("idealPassDist").setSummary("Ideally: ~" + String.valueOf(Math.round(fPassDist)) + "in./" +String.valueOf(Math.round(fPassDist * 2.54))+ "cm");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
} 
