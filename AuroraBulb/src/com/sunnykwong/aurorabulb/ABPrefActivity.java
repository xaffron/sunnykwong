package com.sunnykwong.aurorabulb;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.DisplayMetrics;
import android.widget.Toast;

public class ABPrefActivity extends PreferenceActivity {
	AlertDialog mAD;
	PreferenceCategory bitmapstuff, textstuff;
	PreferenceScreen toplevel;
	StringWriter sw = new StringWriter(10);;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);

		this.getPreferenceManager().setSharedPreferencesName(AB.PREFNAME);
    	this.addPreferencesFromResource(R.xml.abprefs);
    	
    	findPreference("SectionCredits").setTitle("Aurora Bulb v." + AB.THISVERSION);
    	
    	
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
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
    	if (preference == findPreference("sFlickr")) {
    		Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.flickr.com/groups/aurorabulb"));
    		startActivity(it);
    		finish();
    	}
    	if (preference == findPreference("contactProg")) {
			final CharSequence[] items = {"Email", "Donate"};
			new AlertDialog.Builder(this)
				.setTitle("Email or Donate to Xaffron")
				.setItems(items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							switch (item) {
								case 0: //Email
									Intent it = new Intent(android.content.Intent.ACTION_SEND)
		    		   					.setType("plain/text")
		    		   					.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"skwong@consultant.com"})
		    		   					.putExtra(android.content.Intent.EXTRA_SUBJECT, "Aurora Bulb Feedback v" + AB.THISVERSION);
					    		   	startActivity(Intent.createChooser(it, "Contact Xaffron for issues, help & support."));  
					    		   	finish();
					    		   	break;
								case 1: //Donate
						    		it = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=S9VEL3WFGXK48"));
						    		startActivity(it);
						    		finish();
									break;
								default:
									//do nothing
							}
						}
				})
				.show();
    	}
    	if (preference == findPreference("contactArt")) {
    		//Nothing yet
    	}
    	return super.onPreferenceTreeClick(preferenceScreen, preference);
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
