package com.sunnykwong.omc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;
import com.sunnykwong.omcfree.R;

public class OMCPrefActivity extends PreferenceActivity { 
    /** Called when the activity is first created. */
    static int appWidgetID;
    static AlertDialog mAD;

    @Override
    public void onCreate(Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);
        
		if (getIntent().getData() == null)
			appWidgetID=-999;
		else
			appWidgetID = Integer.parseInt(getIntent().getData().getSchemeSpecificPart());

		if (appWidgetID >= 0) {
			if (OMC.DEBUG) Log.i("OMCPref","Called by Widget " + appWidgetID);
        	OMC.getPrefs(appWidgetID);
        	
		} else {
            // If they gave us an intent without the widget id, just bail.
        	if (OMC.DEBUG) Log.i("OMCPref","Called by Launcher - do nothing");
        	OMCPrefActivity.mAD = new AlertDialog.Builder(this)
        		.setTitle("Just One More Clock!")
        		.setMessage("To begin, Add the widget 'One More Clock' to your homescreen!\nLike what you see?  Check out our full version with an awesome selection of dynamic clocks... click on the clock itself for more!")
        	    .setCancelable(true)
        	    .setIcon(R.drawable.fredicon_mdpi)
        	    .setOnKeyListener(new OnKeyListener() {
        	    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
        	    		dialogCancelled();
        	    		return true;
        	    	};
        	    }).create();
        	OMCPrefActivity.mAD.show();

    		try {
    			this.getPackageManager().getPackageInfo("com.sunnykwong.ompc", 0);
    			if (OMC.DEBUG)Log.i("OMCPref","OMPC installed, let OMPC handle onclick");
    			try {
    				getApplicationContext().unregisterReceiver(OMC.cRC);
    			} catch (java.lang.IllegalArgumentException e) {
        			if (OMC.DEBUG)Log.i("OMCPref","OMC's receiver already unregistered - doing nothing");
    				//no need to do anything if receiver not registered
    			}
    		} catch (Exception e) {

    			if (OMC.DEBUG)Log.i("OMCPref","OMPC not installed, register self to handle widget clicks");
    			//e.printStackTrace();
				try {
					getApplicationContext().registerReceiver(OMC.cRC,OMC.PREFSINTENTFILT);
				} catch (Exception ee) {
	    			if (OMC.DEBUG)Log.i("OMCPref","Failed to register self");
					ee.printStackTrace();
				}
    		}
        }

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
    	if (preference == getPreferenceScreen().findPreference("widgetCredits")) {
    		startActivity(OMC.CREDITSINTENT);
    	}
    	if (preference == getPreferenceScreen().findPreference("clearFontCache")) {
    		OMC.TYPEFACEMAP.clear();
    		Toast.makeText(this, "Font Cache Cleared", Toast.LENGTH_SHORT).show();
    	}
    	return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    
    public void dialogCancelled() {
   	if (OMCPrefActivity.mAD!=null) { // && mAD.isShowing()
   		OMCPrefActivity.mAD.dismiss();
   		OMCPrefActivity.mAD = null;
   	}
    	finish();
    }

    @Override
    public void onPause() {
    	if (OMCPrefActivity.mAD != null) finish();
        super.onPause();
    }

    @Override
    public void onDestroy() {
		if (appWidgetID >= 0) {
	    	if (OMC.DEBUG) Log.i("OMCPref","Saving Prefs for Widget " + OMCPrefActivity.appWidgetID);
			OMC.FG = OMC.PREFS.getBoolean("widgetPersistence", true)? true : false;
	    	OMC.setPrefs(OMCPrefActivity.appWidgetID);

			sendBroadcast(OMC.WIDGETREFRESHINTENT);
		}
        super.onDestroy();
    }
} 