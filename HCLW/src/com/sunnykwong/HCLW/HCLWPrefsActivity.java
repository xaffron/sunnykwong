package com.sunnykwong.HCLW;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;

import android.preference.Preference.OnPreferenceChangeListener;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HCLWPrefsActivity extends PreferenceActivity { 
    /** Called when the activity is first created. */
    static AlertDialog mAD;
    CheckBox mCheckBox;
    TextView mTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

    	getPreferenceManager().getDefaultSharedPreferences(this);
    	addPreferencesFromResource(R.xml.hclwprefs);

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
    	if (preference == this.findPreference("flarecolors")) {
			LayoutInflater li = LayoutInflater.from(this);
			LinearLayout ll = (LinearLayout)(li.inflate(R.layout.flares, null));
			CheckBox cb = (CheckBox)ll.findViewById(this.getResources().getIdentifier("showcolor0", "id", HCLW.PKGNAME));
			cb.setChecked(HCLW.PREFS.getBoolean("showcolor0", true));
			cb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// TODO Auto-generated method stub
					HCLW.PREFS.edit().putBoolean("showcolor0", isChecked).commit();
				}
			});
			cb = (CheckBox)ll.findViewById(this.getResources().getIdentifier("showcolor1", "id", HCLW.PKGNAME));
			cb.setChecked(HCLW.PREFS.getBoolean("showcolor1", true));
			cb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// TODO Auto-generated method stub
					HCLW.PREFS.edit().putBoolean("showcolor1", isChecked).commit();
				}
			});
			cb = (CheckBox)ll.findViewById(this.getResources().getIdentifier("showcolor2", "id", HCLW.PKGNAME));
			cb.setChecked(HCLW.PREFS.getBoolean("showcolor2", true));
			cb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// TODO Auto-generated method stub
					HCLW.PREFS.edit().putBoolean("showcolor2", isChecked).commit();
				}
			});
			cb = (CheckBox)ll.findViewById(this.getResources().getIdentifier("showcolor3", "id", HCLW.PKGNAME));
			cb.setChecked(HCLW.PREFS.getBoolean("showcolor3", true));
			cb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// TODO Auto-generated method stub
					HCLW.PREFS.edit().putBoolean("showcolor3", isChecked).commit();
				}
			});
			cb = (CheckBox)ll.findViewById(this.getResources().getIdentifier("showcolor4", "id", HCLW.PKGNAME));
			cb.setChecked(HCLW.PREFS.getBoolean("showcolor4", true));
			cb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// TODO Auto-generated method stub
					HCLW.PREFS.edit().putBoolean("showcolor4", isChecked).commit();
				}
			});

			mAD = new AlertDialog.Builder(this)
			.setTitle("Toggle Colors")
		    .setCancelable(true)
		    .setView(ll)
		    .setOnKeyListener(new OnKeyListener() {
		    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
		    		if (arg2.getKeyCode()==android.view.KeyEvent.KEYCODE_BACK) mAD.cancel();
		    		return true;
		    	};
		    })
		    .show();

    	}
    	return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    @Override
    protected void onPause() {
    	//Translate Changes
    	String sLAF = HCLW.PREFS.getString("HCLWLAF", "Racing Flares");
    	if (sLAF.equals("Racing Flares")) {
    		HCLW.PREFS.edit().putBoolean("FlaresAboveSurface", false)
    		.putBoolean("LightningEffect", false)
    		.putBoolean("SparkEffect", false)
    		.commit();
    	} else if (sLAF.equals("Lightning Strikes")) {
    		// Lightning Strikes
    		HCLW.PREFS.edit().putBoolean("FlaresAboveSurface", false)
    		.putBoolean("LightningEffect", true)
    		.putBoolean("SparkEffect", false)
    		.commit();
    		
    	} else {
    		// Electric Sparks
    		HCLW.PREFS.edit().putBoolean("FlaresAboveSurface", true)
    		.putBoolean("LightningEffect", false)
    		.putBoolean("SparkEffect", true)
    		.commit();
    	}
    	super.onPause();
    }
    
    @Override
    public void onDestroy() {
      super.onDestroy();
    }
} 