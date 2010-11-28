package com.sunnykwong.omc;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.widget.Toast;

public class OMCPrefActivity extends PreferenceActivity implements OnPreferenceChangeListener{ 
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
        	OMC.PREFS.edit().putBoolean("widgetPersistence", OMC.FG).commit();
    		if (OMC.FREEEDITION) {
    			OMC.PREFS.edit().putBoolean("bFourByOne", false)
    					.putBoolean("bThreeByOne", false)
    					.putBoolean("bTwoByOne", false)
    					.commit();
    		}
        	addPreferencesFromResource(R.xml.omcprefs);
        	if (OMC.FREEEDITION) {
        		findPreference("sVersion").setTitle("OMC Version " + OMC.THISVERSION + " Free");
        		findPreference("sVersion").setSummary("Tap me to get the full version!");
        	} else {
        		findPreference("sVersion").setTitle("OMC Version " + OMC.THISVERSION);
        		findPreference("sVersion").setSummary("Thanks for supporting OMC!");
        		findPreference("sVersion").setSelectable(false);
        	}
        	//findPreference("widgetTheme").setOnPreferenceChangeListener(this);
        	findPreference("clearImports").setOnPreferenceChangeListener(this);
    		findPreference("bFourByTwo").setEnabled(false);
    		if (OMC.FREEEDITION) {
        		findPreference("bFourByOne").setEnabled(false); 
        		findPreference("bThreeByOne").setEnabled(false);
        		findPreference("bTwoByOne").setEnabled(false);
    		}
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

        	((OMC)getApplication()).widgetClicks();
        	
        }

    }

    // If user sets a seeded theme, set external to false
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

//    	if (preference == findPreference("widgetTheme")) {
//	    	if (OMC.DEBUG) Log.i("OMCPref","Setting External to false");
//			OMC.PREFS.edit().putBoolean("external", false).commit();
//	    	return true;
//    	}
    	
    	return false;
    }
    
    // If user clicks on a preference...
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
    	if (preference == getPreferenceScreen().findPreference("widgetCredits")) {
    		startActivityForResult(OMC.CREDITSINTENT,0);
    	}
    	if (preference == getPreferenceScreen().findPreference("sVersion")) {
			this.startActivity(OMC.OMCMARKETINTENT);
        	this.finish();
    	}
    	if (preference == getPreferenceScreen().findPreference("downloadStarterPack")) {
        	mAD = new AlertDialog.Builder(this)
			.setCancelable(true)
			.setTitle("Starter Clock Pack")
			.setMessage("Any theme customizations you have made in your sdcard's OMC folder will be overwriten.  Are you sure?\n(If not sure, tap Yes)")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
		        	Toast.makeText(OMCPrefActivity.this, "Downloading starter clock pack...", Toast.LENGTH_LONG).show();
		        	OMCThemePickerActivity.THEMEROOT.mkdir();
					startActivity(OMC.GETSTARTERPACKINTENT);
					mAD.cancel();
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mAD.cancel();
				}
			})
			.create();
        	mAD.show();
    	}
    	if (preference == getPreferenceScreen().findPreference("loadThemeFile")) {
    		startActivityForResult(OMC.IMPORTTHEMEINTENT,0);
    	}
    	if (preference == getPreferenceScreen().findPreference("oTTL")) {
    		OMCPrefActivity.this.getPreferenceScreen().setEnabled(false);
    		Intent mainIntent = new Intent(Intent.ACTION_MAIN,
        			null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

			Intent pickIntent = new
			Intent(Intent.ACTION_PICK_ACTIVITY);
			pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
			startActivityForResult(pickIntent, OMCPrefActivity.appWidgetID);
			mainIntent=null;
			pickIntent=null;
    	}
    	if (preference == getPreferenceScreen().findPreference("clearCache")) {
    		OMC.purgeTypefaceCache();
    		Toast.makeText(this, "Font Cache Cleared", Toast.LENGTH_SHORT).show();
    		OMC.purgeBitmapCache();
    		Toast.makeText(this, "Bitmap Cache Cleared", Toast.LENGTH_SHORT).show();
    	}
    	if (preference == findPreference("clearImports")) {
        	OMCPrefActivity.mAD = new AlertDialog.Builder(this)
    		.setTitle("Warning!")
    		.setMessage("Clearing the Import cache will revert all your custom clocks to stock look.  Are you sure?")
    	    .setCancelable(true)
    	    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					OMC.clearImportCache();
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					OMCPrefActivity.mAD.dismiss();
				}
			})
    	    .setIcon(R.drawable.fredicon_mdpi)
    	    .setOnKeyListener(new OnKeyListener() {
    	    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
					OMCPrefActivity.mAD.dismiss();
    	    		return true;
    	    	};
    	    }).create();
        	OMCPrefActivity.mAD.show();
    	}
    	return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    // The result is obtained in onActivityResult:
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		getPreferenceScreen().setEnabled(true);
		// If it's an independent child activity, do nothing
		if (requestCode == 0) return;
		if (data != null) {
			String s = data.toUri(MODE_PRIVATE).toString();
			OMC.PREFS.edit().putString("URI", s).commit();
		}
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
    	super.onPause();
    }

    @Override
    public void onDestroy() {
		if (appWidgetID >= 0) {

			if (OMC.DEBUG) Log.i("OMCPref","Saving Prefs for Widget " + OMCPrefActivity.appWidgetID);
			OMC.FG = OMC.PREFS.getBoolean("widgetPersistence", true)? true : false;
			OMC.UPDATEFREQ = OMC.PREFS.getInt("iUpdateFreq", 30) * 1000;
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
					OMC.WIDGET4x1CNAME,
					OMC.PREFS.getBoolean("bFourByOne", false) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	getApplicationContext().getPackageManager()
					.setComponentEnabledSetting(
							OMC.WIDGET3x1CNAME,
							OMC.PREFS.getBoolean("bThreeByOne", false) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
									: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
							PackageManager.DONT_KILL_APP);
	    	getApplicationContext().getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET2x1CNAME,
					OMC.PREFS.getBoolean("bTwoByOne", false) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	getApplicationContext().getPackageManager()
			.setComponentEnabledSetting(
					OMC.SKINNERCNAME,
					OMC.PREFS.getBoolean("bSkinner", false) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);

			OMC.setServiceAlarm(System.currentTimeMillis()+500);
			sendBroadcast(OMC.WIDGETREFRESHINTENT);
		}
        super.onDestroy();
    }
} 