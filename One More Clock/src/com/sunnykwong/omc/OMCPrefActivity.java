package com.sunnykwong.omc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

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
        } else {
            // If they gave us an intent without the widget id, just bail.
        	if (OMC.DEBUG) Log.i("OMCPref","Called by Launcher - do nothing");
        	OMCPrefActivity.mAD = new AlertDialog.Builder(this)
        		.setTitle("Just One More Clock!")
        		.setMessage("Thanks for downloading!\nTo begin, hit the back button to \ngo back to the home screen, then \npush the menu button, select 'Add', then 'Widgets' to see 'One More Clock' listed.  Have fun!")
        	    .setCancelable(true)
        	    .setIcon(R.drawable.fredicon_mdpi)
        	    .setOnKeyListener(new OnKeyListener() {
        	    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
        	    		dialogCancelled();
        	    		return true;
        	    	};
        	    }).create();
        	OMCPrefActivity.mAD.show();
        }

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
    	if (preference == getPreferenceScreen().findPreference("widgetCredits")) {
    		startActivity(OMC.CREDITSINTENT);
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