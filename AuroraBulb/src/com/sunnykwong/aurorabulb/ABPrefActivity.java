package com.sunnykwong.aurorabulb;

import android.app.Activity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.content.pm.PackageManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.widget.Toast;


public class ABPrefActivity extends PreferenceActivity implements OnPreferenceChangeListener {

	AlertDialog mAD;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);
        
    	mAD = new AlertDialog.Builder(this)
    		.setTitle("DroidExpose!")
    		.setMessage("Thanks for downloading!\nTo begin, hit the back button to go back to the home screen, then push the menu button, select 'Add', then 'Widgets' to see 'One More Clock' listed.  Have fun!")
    	    .setCancelable(true)
    	    .setOnKeyListener(new OnKeyListener() {
    	    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
    	    		dialogCancelled();
    	    		return true;
    	    	};
    	    }).create();
    	mAD.show();

    }

    // If user sets a seeded theme, set external to false
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

//        	if (preference == findPreference("widgetTheme")) {
//    	    	if (DE.DEBUG) Log.i("OMCPref","Setting External to false");
//    			DE.PREFS.edit().putBoolean("external", false).commit();
//    	    	return true;
//        	}
    	
    	return false;
    }
    
    // If user clicks on a preference...
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
//        	if (preference == getPreferenceScreen().findPreference("widgetPrefs") && DE.FREEEDITION) {
//            	OMCPrefActivity.mAD = new AlertDialog.Builder(this)
//    			.setCancelable(true)
//    			.setTitle("Why are the widgets so big?")
//    			.setMessage("Actually, OMC offers widget sizes of 4x2, 4x1, 3x1 and 2x1.\nPlease consider upgrading to the full edition of OMC to get these sizes!\nAlternatively, if you use an alternative launcher such as ADW or Launcher Pro, you should be able to approximate this ability by dynamically resizing the 4x2 widget.")
//    			.setPositiveButton("Take me to the paid version!", new DialogInterface.OnClickListener() {
//    					
//    					@Override
//    					public void onClick(DialogInterface dialog, int which) {
//    						// TODO Auto-generated method stub
//    						OMCPrefActivity.mAD.dismiss();
//    						OMCPrefActivity.this.startActivity(DE.OMCMARKETINTENT);
//    						OMCPrefActivity.this.finish();
//    						
//    					}
//    				}).create();
//            	OMCPrefActivity.mAD.show();
//        	}
    	return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    // The result is obtained in onActivityResult:
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		getPreferenceScreen().setEnabled(true);
		// If it's an independent child activity, do nothing
		if (requestCode == 0) return;
		if (data != null) {
			String s = data.toUri(MODE_PRIVATE).toString();
			AB.PREFS.edit().putString("URI", s).commit();
		}
	}

    
    public void dialogCancelled() {
//       	if (OMCPrefActivity.mAD!=null) { // && mAD.isShowing()
//       		OMCPrefActivity.mAD.dismiss();
//       		OMCPrefActivity.mAD = null;
//       	}
    	this.startActivity(new Intent(this,ABAnimActivity.class));
    	finish();
    }

    @Override
    public void onPause() {
    	super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
} 
