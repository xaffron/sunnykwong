package com.sunnykwong.aurorabulb;

import android.graphics.Canvas;
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
    	findPreference("pickColor").setOnPreferenceChangeListener(this);
    	findPreference("timeShutterDuration").setOnPreferenceChangeListener(this);
    	findPreference("timePhotoTimer").setOnPreferenceChangeListener(this);
		findPreference("pickText").setOnPreferenceChangeListener(this);

    	switchTextBitmap(); 
    	prepGlobalVars();
    	
    	mAD = new AlertDialog.Builder(this)
    		.setTitle("Aurora Bulb!")
    		.setMessage("This application requires another camera with long-exposure to shoot the screen of this phone after you hit go.  Slide the phone on a straight line while maintaining the screen in the view of the camera's viewfinder, to embed the characters on the picture.")
    	    .setCancelable(true)
    	    .setOnKeyListener(new OnKeyListener() {
    	    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
    	    		dialogCancelled();
    	    		return true;
    	    	};
    	    }).create();
    	mAD.show();

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
    	if (preference == findPreference("pickColor")) {
    		try {
    			int colr = Color.parseColor((String)newValue);
    			AB.PREFS.edit().putInt("textColor", colr).commit();
    			new PrintWriter(sw).format("#%x", colr);
        		findPreference("pickColor").setSummary("Picked: " + sw.toString());
        		sw.getBuffer().setLength(0);
    			return true;
    		} catch (IllegalArgumentException e) {
    			Toast.makeText(this, "Invalid Color!", Toast.LENGTH_SHORT).show();
    			return false;
    		}
    	} else if (preference == findPreference("pickFont")) {
    		AB.PT1.setTypeface(Typeface.createFromAsset(getApplicationContext().getAssets(), (String)newValue));
			AB.PREFS.edit().putString("pickFont", (String)newValue).commit();
			findPreference("pickFont").setSummary("Picked: " + (String)newValue);
			prepGlobalVars();
    	} else if (preference == findPreference("timeShutterDuration")) {
    		int temp;
    		try {
    			temp = Integer.parseInt((String)newValue);
    		} catch (IllegalArgumentException e) {
    			Toast.makeText(this, "Invalid Shutter Duration!", Toast.LENGTH_SHORT).show();
    			return false;
    		}
    		if (temp < 5) {
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
    			prepGlobalVars();
    			return true;
    		}
    	}
    	return true;
    }

    // If user clicks on a preference...
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
    	if (preference == getPreferenceScreen().findPreference("go")) {
        	prepGlobalVars();
    		prepBuffer();
        	this.startActivityForResult(new Intent(this,ABAnimActivity.class),0);
    	}
    	if (preference == findPreference("showWhat")) {
    		if (AB.PREFS.getString("showWhat","bitmap").equals("text")) {
    			AB.PREFS.edit().putString("showWhat", "bitmap").commit();
    		} else {
    			AB.PREFS.edit().putString("showWhat", "text").commit();
    		}
    		switchTextBitmap();
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
    
    public void prepGlobalVars() {
    	//ok, set global vars before we pass control to anim.
    	AB.COUNTDOWNSECONDS = Integer.parseInt(AB.PREFS.getString("timePhotoTimer", "10"));
    	System.out.println(AB.PREFS.getInt("textColor", Color.WHITE));
    	
    	AB.PT1.setTextSize((float)AB.BUFFERHEIGHT);
    	AB.PT1.setTextScaleX(1f);
		AB.PT1.setTextAlign(Paint.Align.LEFT);
		int textwidth = (int)AB.PT1.measureText(AB.PREFS.getString("pickText", "Aurora Bulb"));
		int shutterDuration = Integer.parseInt(AB.PREFS.getString("timeShutterDuration", "10"));

    	System.out.println(AB.PT1.measureText(AB.PREFS.getString("pickText", "Aurora Bulb")));
		float fPassDist = (float)textwidth/AB.SCRNDPI;
    	findPreference("idealPassDist").setSummary("Ideally: ~" + String.valueOf(Math.round(fPassDist)) + "in./" +String.valueOf(Math.round(fPassDist * 2.54))+ "cm");
		
		//Since we are aiming at 30fps, we will have to squeeze/stretch the text.
		//How many lines can we manage over the shutter duration?
		int bufferwidth = shutterDuration * AB.TARGETFPS;
		AB.PT1.setTextScaleX((float)bufferwidth/textwidth);
    }
    public void prepBuffer(){

    	if (AB.SRCBUFFER!=null) AB.SRCBUFFER.recycle();
    	AB.SRCBUFFER = Bitmap.createBitmap((int)AB.PT1.measureText(AB.PREFS.getString("pickText", "Aurora Bulb")), AB.BUFFERHEIGHT, Bitmap.Config.RGB_565);
    	AB.SRCCANVAS = new Canvas(AB.SRCBUFFER);
    	AB.SRCBUFFER2 = Bitmap.createBitmap((int)AB.PT1.measureText(AB.PREFS.getString("pickText", "Aurora Bulb")), AB.BUFFERHEIGHT, Bitmap.Config.RGB_565);
    	AB.SRCCANVAS2 = new Canvas(AB.SRCBUFFER2);
    	AB.PT1.setColor(AB.PREFS.getInt("textColor", Color.WHITE));
    	AB.SRCCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora Bulb"), 0-2, 0-AB.PT1.getFontMetricsInt().ascent-2, AB.PT1);
    	AB.SRCCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora Bulb"), 0+2, 0-AB.PT1.getFontMetricsInt().ascent-2, AB.PT1);
    	AB.SRCCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora Bulb"), 0-2, 0-AB.PT1.getFontMetricsInt().ascent+2, AB.PT1);
    	AB.SRCCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora Bulb"), 0+2, 0-AB.PT1.getFontMetricsInt().ascent+2, AB.PT1);
    	AB.SRCCANVAS2.drawText(AB.PREFS.getString("pickText", "Aurora Bulb"), 0, 0-AB.PT1.getFontMetricsInt().ascent, AB.PT1);
    	AB.PT1.setColor(Color.BLACK);
    	AB.SRCCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora Bulb"), 0, 0-AB.PT1.getFontMetricsInt().ascent, AB.PT1);
    }


   @Override
    protected void onResume() {
        super.onResume();
        switchTextBitmap();
   }
   
   public void switchTextBitmap() {
        // Set up a listener whenever a key changes
   
		if (AB.PREFS.getString("showWhat", "text").equals("text")) {
			findPreference("showWhat").setTitle("Aurora Bulb Renders Text");
	    	toplevel.removePreference(bitmapstuff);
	    	toplevel.addPreference(textstuff);
	    	findPreference("pickFont").setSummary("Picked: Comic Font");
	        findPreference("pickColor").setSummary("Picked: " + sw.toString());
			sw.getBuffer().setLength(0);
			findPreference("pickText").setSummary(AB.PREFS.getString("pickText", "Aurora Bulb"));
			findPreference("pickFont").setSummary("Picked: " + AB.PREFS.getString("pickFont", "Unibody 8-SmallCaps.otf"));
		} else {
			findPreference("showWhat").setTitle("Aurora Bulb Renders Bitmap");
	    	toplevel.removePreference(textstuff);
	    	toplevel.addPreference(bitmapstuff);
	    	findPreference("pickBitmap").setSummary("Found: /sdcard/aurorabulb.jpg");
		}
		findPreference("timeShutterDuration").setSummary("To match your cam's shutter spd (seconds): " + AB.PREFS.getString("timeShutterDuration", "10"));
		findPreference("timePhotoTimer").setSummary("To match your cam's shutter delay (seconds): " + AB.PREFS.getString("timePhotoTimer", "10"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
} 
