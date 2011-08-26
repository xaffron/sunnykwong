package com.sunnykwong.omc;

import java.io.File;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
public class OMCThemeImportActivity extends Activity {

	public Handler mHandler;
	public static TextView TEXT;
	public static LocationManager LM;
	public static Location CURRLOCN;
	public static LocationListener LL;
	public static String TRAFFICRESULTS;
	//public static HashMap<String,String[]> ELEMENTS;
	//public static HashMap<String,File> THEMES;
	public static String tempText = "";
	public static File SDROOT, THEMEROOT;
	public static ArrayAdapter<String> THEMEARRAY;
	public static Spinner THEMESPINNER;
	public static char[] THEMECREDITS;
	public static String CURRSELECTEDTHEME, RAWCONTROLFILE;
	
//    static AlertDialog mAD;	

	final Runnable mResult = new Runnable() {
		public void run() {
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        OMCThemeImportActivity.CURRSELECTEDTHEME = null;
        
        chooseTheme();
    }

    public void chooseTheme() {
        Intent itThemePicker = new Intent(this,OMCThemePickerActivity.class);
        itThemePicker.putExtra("default", OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME));
        startActivityForResult(itThemePicker, 0);
    }
    
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode!=0) return; // bogus call
    	if (resultCode == Activity.RESULT_CANCELED) {
        	setResult(Activity.RESULT_CANCELED);
        	finishActivity(0);
    		finish();
    		return;
    	}
    	OMCThemeImportActivity.CURRSELECTEDTHEME = data.getExtras().getString("theme");

    	if (OMCThemeImportActivity.CURRSELECTEDTHEME!=null) {
    		JSONObject newTheme = OMC.getTheme(this, OMCThemeImportActivity.CURRSELECTEDTHEME, false);
        	Toast.makeText(this, newTheme.optString("name") + " selected.", Toast.LENGTH_SHORT).show();
        	OMC.PREFS.edit()
		        	.putString("widgetTheme", OMCThemeImportActivity.CURRSELECTEDTHEME)
		    		.commit();
        	
        	// Clear the cache for a clean slate
        	OMC.purgeBitmapCache();
        	OMC.purgeTypefaceCache();

        	setResult(Activity.RESULT_OK);
        	finish();

		} else {
			//For some reason, no theme selected
        	setResult(Activity.RESULT_CANCELED);
			finish();
			return;
		}
	}
}