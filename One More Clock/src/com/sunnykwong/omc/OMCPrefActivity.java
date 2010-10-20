package com.sunnykwong.omc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

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
        	addPreferencesFromResource(R.xml.omcprefs);

    		findPreference("bFourByTwo").setEnabled(false);
        } else {
            // If they gave us an intent without the widget id, just bail.
        	if (OMC.DEBUG) Log.i("OMCPref","Called by Launcher - do nothing");
        	OMCPrefActivity.mAD = new AlertDialog.Builder(this)
        		.setTitle("Just One More Clock!")
        		.setMessage("Thanks for downloading!\nTo begin, hit the back button to go back to the home screen, then push the menu button, select 'Add', then 'Widgets' to see 'One More Clock' listed.  Have fun!")
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

	    	// Enable/Disable the various size widgets
	    	getApplicationContext().getPackageManager()
					.setComponentEnabledSetting(
							OMC.WIDGET4x2CNAME,
							OMC.PREFS.getBoolean("bFourByTwo", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
									: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
							PackageManager.DONT_KILL_APP);
	    	getApplicationContext().getPackageManager()
					.setComponentEnabledSetting(
							OMC.WIDGET3x1CNAME,
							OMC.PREFS.getBoolean("bThreeByOne", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
									: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
							PackageManager.DONT_KILL_APP);
	    	getApplicationContext().getPackageManager()
					.setComponentEnabledSetting(
							OMC.WIDGET2x1CNAME,
							OMC.PREFS.getBoolean("bTwoByOne", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
									: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
							PackageManager.DONT_KILL_APP);

			OMC.setServiceAlarm(System.currentTimeMillis() + 3000);
			sendBroadcast(OMC.WIDGETREFRESHINTENT);
		}
        super.onDestroy();
    }
} 