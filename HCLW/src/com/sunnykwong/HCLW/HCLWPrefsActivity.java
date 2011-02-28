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
import android.preference.PreferenceManager;
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

    	PreferenceManager.getDefaultSharedPreferences(this);
    	addPreferencesFromResource(R.xml.hclwprefs);
    	findPreference("dpi").setTitle("Screen DPI " +HCLW.SCRNDPI);
    	findPreference("dpi").setSummary("Screen dimension " +HCLW.SCRNWIDTH + "x" + HCLW.SCRNHEIGHT);
    	
    	if (HCLW.FREEEDITION) {
    		findPreference("sVersion").setTitle("Version " + HCLW.THISVERSION + " Free");
    		findPreference("sVersion").setSummary("Tap me to get the full version!");
    		findPreference("sVersion").setSelectable(true);
    	} else {
    		findPreference("sVersion").setTitle("Version " + HCLW.THISVERSION);
    		findPreference("sVersion").setSummary("Thanks for your support!");
    		findPreference("sVersion").setSelectable(false);
    	}

		// This is the help/FAQ dialog.
		
		if (HCLW.SHOWHELP) {
			LayoutInflater li = LayoutInflater.from(this);
			LinearLayout ll = (LinearLayout)(li.inflate(getResources().getIdentifier("faqdialog", "layout", HCLW.PKGNAME), null));
			mTextView = (TextView)ll.findViewById(getResources().getIdentifier("splashtext", "id", HCLW.PKGNAME));
			mTextView.setAutoLinkMask(Linkify.ALL);
			mTextView.setMinLines(8);
			mTextView.setText(HCLW.FAQS[HCLW.faqtoshow++]);
			HCLW.faqtoshow = HCLW.faqtoshow==HCLW.FAQS.length?0:HCLW.faqtoshow;
			
			mCheckBox = (CheckBox)ll.findViewById(getResources().getIdentifier("splashcheck", "id", HCLW.PKGNAME));
			mCheckBox.setChecked(!HCLW.SHOWHELP);
			mCheckBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// TODO Auto-generated method stub
					HCLW.SHOWHELP = !isChecked;
				}
			});

			((Button)ll.findViewById(getResources().getIdentifier("faqOK", "id", HCLW.PKGNAME))).setOnClickListener(new Button.OnClickListener() {
				
				@Override
				public void onClick(android.view.View v) {
					HCLW.PREFS.edit().putBoolean("showhelp", HCLW.SHOWHELP).commit();
					mAD.dismiss();
				}
			});
			((Button)ll.findViewById(getResources().getIdentifier("faqNeutral", "id", HCLW.PKGNAME))).setOnClickListener(new Button.OnClickListener() {
				
				@Override
				public void onClick(android.view.View v) {
					mTextView.setText(HCLW.FAQS[HCLW.faqtoshow++]);
					mTextView.invalidate();
					HCLW.faqtoshow = HCLW.faqtoshow==HCLW.FAQS.length?0:HCLW.faqtoshow;
				}
			});;
			
			mAD = new AlertDialog.Builder(this)
			.setTitle("Useful Tip")
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

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
		final CharSequence[] items = {"Email", "Donate"};
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
    	if (preference == findPreference("contactProg")) {
			new AlertDialog.Builder(this)
				.setTitle("Email or Donate to Xaffron")
				.setItems(items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							switch (item) {
								case 0: //Email
									Intent it = new Intent(android.content.Intent.ACTION_SEND)
		    		   					.setType("plain/text")
		    		   					.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"skwong@consultant.com"})
		    		   					.putExtra(android.content.Intent.EXTRA_SUBJECT, "Aurora Bulb Feedback v" + HCLW.THISVERSION);
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
			new AlertDialog.Builder(this)
				.setTitle("Email or Donate to Nemuro")
				.setItems(items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							switch (item) {
								case 0: //Email
									Intent it = new Intent(android.content.Intent.ACTION_SEND)
		    		   					.setType("plain/text")
		    		   					.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"brcosmin@gmail.com"})
		    		   					.putExtra(android.content.Intent.EXTRA_SUBJECT, "Aurora Bulb Feedback v" + HCLW.THISVERSION);
					    		   	startActivity(Intent.createChooser(it, "Contact Xaffron for issues, help & support."));  
					    		   	finish();
					    		   	break;
								case 1: //Donate
						    		it = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=brcosmin%40gmail%2ecom&lc=RO&item_name=Cosmin%20Bizon&item_number=cosminbizon&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted"));
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
    	if (preference == getPreferenceScreen().findPreference("sVersion")) {
			this.startActivity(HCLW.HCLWMARKETINTENT);
        	this.finish();
    	}

    	return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    @Override
    protected void onPause() {
    	((HCLW)getApplication()).countFlareColors();
    	//Translate Changes
    	String sLAF = HCLW.PREFS.getString("HCLWLAF", "Racing Flares");
    	if (sLAF.equals("Racing Flares")) {
    		HCLW.PREFS.edit().putBoolean("FlaresAboveSurface", false)
    		.putBoolean("LightningEffect", false)
    		.putBoolean("SparkEffect", false)
    		.commit();
    		HCLW.TRIALOVERTIME=0l;
    	} else if (sLAF.equals("Lightning Strikes")) {
    		// Lightning Strikes
    		HCLW.PREFS.edit().putBoolean("FlaresAboveSurface", false)
    		.putBoolean("LightningEffect", true)
    		.putBoolean("SparkEffect", false)
    		.commit();
    		if (HCLW.FREEEDITION) {
    			HCLW.TRIALOVERTIME = System.currentTimeMillis()+ 60000l;
    			Toast.makeText(this, "This look is limited to 1 minute in the free version.  Consider donating if you like this wallpaper!", Toast.LENGTH_LONG).show();
    		}
    	} else {
    		// Electric Sparks
    		HCLW.PREFS.edit().putBoolean("FlaresAboveSurface", true)
    		.putBoolean("LightningEffect", false)
    		.putBoolean("SparkEffect", true)
    		.commit();

    		((HCLW)getApplication()).countFlareColors();
    		
    		if (HCLW.FREEEDITION) {
    			HCLW.TRIALOVERTIME = System.currentTimeMillis()+ 60000l;
    			Toast.makeText(this, "This look is limited to 1 minute in the free version.  Consider donating if you like this wallpaper!", Toast.LENGTH_LONG).show();
    		}
    		
    	}
    	super.onPause();
    }
    
    @Override
    public void onDestroy() {
      super.onDestroy();
    }
} 