package com.sunnykwong.omc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.format.Time;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class OMCPrefActivity extends PreferenceActivity { 
    /** Called when the activity is first created. */
    static int appWidgetID;
    static AlertDialog mAD;
    AlertDialog mTTL;
	Button[] btnCompass = new Button[9];

    final Time timeTemp = new Time(), timeTemp2 = new Time();
    Handler mHandler;
    CheckBox mCheckBox;
    TextView mTextView;
    Thread mRefresh;
    boolean isInitialConfig=false, mTempFlag=false;
    Preference prefUpdWeatherNow, prefWeather, prefWeatherDisplay, prefWeatherProvider;
    Preference prefloadThemeFile, prefclearCache, prefbSkinner, prefTimeZone;
    Preference prefsUpdateFreq, prefwidgetPersistence, prefemailMe, preftweakTheme;
    int iTTLArea=0;

    final Runnable mUpdatePrefs = new Runnable() {
    	@Override
    	public void run() {								
    		try {
        		String sWSetting = OMC.PREFS.getString("weathersetting", "bylatlong");
    			JSONObject jsonWeather = new JSONObject(OMC.PREFS.getString("weather", "{}"));
    			String sCity = jsonWeather.optString("city","Unknown");
    			timeTemp.set(OMC.NEXTWEATHERREFRESH);
    			timeTemp2.set(OMC.LASTWEATHERTRY);
        		if (sWSetting.equals("bylatlong")) {
        			prefWeather.setTitle("Location: " + sCity +" (Detected)");
        			if (OMC.PREFS.getString("sWeatherFreq", "60").equals("0")) {
        				prefWeather.setSummary("Last try: "+timeTemp2.format("%R") + " (Manual Refresh Only)");
        			} else {
        				prefWeather.setSummary("Last try: "+timeTemp2.format("%R") + " Next Refresh: "+timeTemp.format("%R"));
        			}
        		} else if (sWSetting.equals("specific")) {
        			prefWeather.setTitle("Location: "+OMC.jsonFIXEDLOCN.optString("city","Unknown")+" (Fixed)");
        			if (OMC.PREFS.getString("sWeatherFreq", "60").equals("0")) {
        				prefWeather.setSummary("Last try: "+timeTemp2.format("%R") + " (Manual Refresh Only)");
        			} else {
        				prefWeather.setSummary("Last try: "+timeTemp2.format("%R") + " Next Refresh: "+timeTemp.format("%R"));
        			}
        		} else {
        			prefWeather.setTitle("Weather functionality disabled");
        			prefWeather.setSummary("Tap to enable");
        		} 
    		} catch (JSONException e) {
    			e.printStackTrace();
    			prefWeather.setTitle("Weather updates disabled");
    			prefWeather.setSummary("Tap to enable");
    		}
			return;
    	}
    };
    
    
    @Override
    protected void onNewIntent(Intent intent) {
    	if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Pref","NewIntent");
    	super.onNewIntent(intent);

    	// If action is null, we are coming from an existing widget - 
    	// we want both the home and back buttons to apply changes,
    	// So we set default result to OK.
    	if (getIntent().getAction()==null) {
        	setResult(Activity.RESULT_OK, intent);
        	isInitialConfig=false;
    	} else if (getIntent().getAction().equals(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)) {
    		// but if we're from a configure action, that means the widget hasn't been added yet -
    		// the home button must not be used or we'll get zombie widgets.
        	setResult(Activity.RESULT_CANCELED, intent);
        	isInitialConfig=true;
    	}
    	getPreferenceScreen().removeAll();
		if (intent.getData() == null) {
			appWidgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -999);
		} else {
			appWidgetID = Integer.parseInt(intent.getData().getSchemeSpecificPart());
			OMC.PREFS.edit().putBoolean("newbie"+appWidgetID, false).commit();
		}
		setupPrefs(appWidgetID);
		
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Pref","OnCreate");

    	super.onCreate(savedInstanceState);
    	mHandler = new Handler();

    	// Refresh list of installed Launcher Apps.
		List<ResolveInfo> launcherlist = OMC.PKM.queryIntentActivities(OMC.FINDLAUNCHERINTENT, 0);
		OMC.INSTALLEDLAUNCHERAPPS = new ArrayList<String>();
		OMC.INSTALLEDLAUNCHERAPPS.add("com.teslacoilsw.widgetlocker");
		OMC.INSTALLEDLAUNCHERAPPS.add("com.jiubang.goscreenlock");
		
		for (ResolveInfo info : launcherlist) {
			OMC.INSTALLEDLAUNCHERAPPS.add(info.activityInfo.packageName);
		}

    	
    	// FIX FOR NOMEDIA and BADTHEME
		if (OMC.checkSDPresent()) {
			((OMC)(this.getApplication())).fixnomedia();
			((OMC)(this.getApplication())).fixKnownBadThemes();
		}

    	// If action is null, we are coming from an existing widget - 
    	// we want both the home and back buttons to apply changes,
    	// So we set default result to OK.
    	if (getIntent().getAction()==null) {
        	setResult(Activity.RESULT_OK, getIntent());
        	isInitialConfig=false;
    	} else if (getIntent().getAction().equals(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)) {
    		// but if we're from a configure action, that means the widget hasn't been added yet -
    		// the home button must not be used or we'll get zombie widgets.
        	setResult(Activity.RESULT_CANCELED, getIntent());
        	isInitialConfig=true;
    	}
		if (getIntent().getData() == null) {
			appWidgetID = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -999);
		} else {
			appWidgetID = Integer.parseInt(getIntent().getData().getSchemeSpecificPart());
			OMC.PREFS.edit().putBoolean("newbie"+appWidgetID, false).commit();
		}
		setupPrefs(appWidgetID);

    }

    public void setupPrefs(final int appWidgetID) {
		if (appWidgetID >= 0) {
			// We are called by the user tapping on a widget - bring up prefs screen
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Pref"," Called by Widget " + appWidgetID);
			if (OMC.SINGLETON) setTitle(OMC.SINGLETONNAME + " - Preferences");

			// Load the proper prefs into the generic prefs set
			OMC.getPrefs(appWidgetID);

			// Setting foreground options, and making sure we have at least one widget (4x2) enabled
			Editor ed = OMC.PREFS.edit();
			ed.putBoolean("widgetPersistence", OMC.FG);
			ed.putBoolean("bFourByTwo", true);
        	
			// Depending on free ed or not, enable/disable the widgets
        	if (OMC.FREEEDITION) {
        		ed
        		.putBoolean("bFiveByFour", false)
        		.putBoolean("bFiveByTwo", false)
        		.putBoolean("bFiveByOne", false)
        		.putBoolean("bFourByFour", false)
    			.putBoolean("bFourByOne", false)
    			.putBoolean("bThreeByThree", false)
    			.putBoolean("bThreeByOne", false)
    			.putBoolean("bTwoByTwo", false)
    			.putBoolean("bTwoByOne", false)
    			.putBoolean("bOneByThree", false);
    		}
        	ed.commit();

        	// Load generic prefs into the prefscreen. 
    		this.getPreferenceManager().setSharedPreferencesName(OMC.SHAREDPREFNAME);
        	addPreferencesFromResource(getResources().getIdentifier("omcprefs", "xml", OMC.PKGNAME));

        	// ID specific preferences.
        	// "Set Widget Theme".
        	prefloadThemeFile = findPreference("loadThemeFile");
        	prefloadThemeFile.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
		        	prefloadThemeFile.setSummary(newValue+ " selected.");
					return true;
				}
			});
        	prefloadThemeFile.setSummary(OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME)+ " selected.");
        	
    		// "Personalize Clock".
        	preftweakTheme = findPreference("tweakTheme");
        	
        	// "Change Time Zone".
        	prefTimeZone = findPreference("timeZone");
        	if (OMC.PREFS.getString("sTimeZone", "default").equals("default")) {
        		findPreference("timeZone").setSummary("(Following Device Time Zone)");
    		} else {
    			findPreference("timeZone").setSummary(OMC.PREFS.getString("sTimeZone", "default"));
    		}

        	// "Update Weather Now".
        	prefUpdWeatherNow = findPreference("updweathernow");
        	prefUpdWeatherNow.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
		    		OMC.updateWeather();
					return true;
				}
			});
        	
        	// "Location: [Location] (status)".
        	prefWeather = findPreference("weather");

        	// "Weather Provider".
        	prefWeatherProvider = findPreference("weatherProvider");
        	prefWeatherProvider.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
		    		if (newValue.equals("yrno")) {
		        		preference.setSummary("Using yr.no");
		    		} else if (newValue.equals("ig")) {
		        		preference.setSummary("Using iGoogle Weather");
		    		} else {
		    			preference.setSummary("Using OpenWeatherMap.org");
		    		}
			    	return true;
				}
			});
        	String sWProvider = OMC.PREFS.getString("weatherProvider", "yrno");
    		if (sWProvider.equals("yrno")) {
    			prefWeatherProvider.setSummary("Using yr.no");
    		} else if (sWProvider.equals("ig")) {
    			prefWeatherProvider.setSummary("Using iGoogle Weather");
    		} else {
    			prefWeatherProvider.setSummary("Using OpenWeatherMap.org");
    		}

        	
        	// "Weather Display Units".
        	prefWeatherDisplay = findPreference("weatherDisplay");
        	prefWeatherDisplay.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
		    		if (newValue.equals("c")) {
		        		preference.setSummary("Using Celsius");
		    		} else {
		        		preference.setSummary("Using Fahrenheit");
		    		}
			    	return true;
				}
			});
        	if (OMC.PREFS.getString("weatherDisplay", "f").equals("c"))
        		prefWeatherDisplay.setSummary("Using Celsius");
        	else prefWeatherDisplay.setSummary("Using Fahrenheit");

    		// "Clock Update Interval"
        	prefsUpdateFreq = findPreference("sUpdateFreq");
        	prefsUpdateFreq.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
		    		preference.setSummary("Redraw every " + (String)newValue + " seconds.");
		    		getApplicationContext().sendBroadcast(OMC.WIDGETREFRESHINTENT);
					return true;
				}
			});
        	prefsUpdateFreq.setSummary("Redraw every " + OMC.PREFS.getString("sUpdateFreq", "30") + " seconds.");

        	// "Weather Update Interval"
        	findPreference("sWeatherFreq").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
		    		int newHrs = Integer.parseInt((String)newValue)/60;
		    		long newMillis = newHrs * 3600000l; 
		        	switch (newHrs) {
		    		case 0:
		    			findPreference("sWeatherFreq").setSummary("Manual weather updates only.");
		    			OMC.NEXTWEATHERREFRESH = Long.MAX_VALUE;
		    			break;
		    		case 1:
		    			findPreference("sWeatherFreq").setSummary("Refresh weather every 1 hour.");
		            	// if the last try was unsuccessful, reset the next weather
		            	if (OMC.LASTWEATHERTRY > OMC.LASTWEATHERREFRESH) 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERTRY + newMillis/4l;
		            	else 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERREFRESH + newMillis;
		    			break;
		    		case 4:
		    			findPreference("sWeatherFreq").setSummary("Refresh weather every 4 hours.");
		            	// if the last try was unsuccessful, reset the next weather
		            	if (OMC.LASTWEATHERTRY > OMC.LASTWEATHERREFRESH) 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERTRY + newMillis/4l;
		            	else 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERREFRESH + newMillis;
		    			break;
		    		case 8:
		    			findPreference("sWeatherFreq").setSummary("Refresh weather every 8 hours.");
		            	// if the last try was unsuccessful, reset the next weather
		            	if (OMC.LASTWEATHERTRY > OMC.LASTWEATHERREFRESH) 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERTRY + newMillis/4l;
		            	else 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERREFRESH + newMillis;
		    			break;
		    		default:
		    			findPreference("sWeatherFreq").setSummary("Refresh weather at default interval.");
		            	// if the last try was unsuccessful, reset the next weather
		            	if (OMC.LASTWEATHERTRY > OMC.LASTWEATHERREFRESH) 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERTRY + newMillis/4l;
		            	else 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERREFRESH + newMillis;
		        	}
		    		return true;
				}
			});
        	switch (Integer.parseInt(OMC.PREFS.getString("sWeatherFreq", "60"))/60) {
        		case 0:
        			findPreference("sWeatherFreq").setSummary("Manual weather updates only.");
        			break;
        		case 1:
        			findPreference("sWeatherFreq").setSummary("Refresh weather every 1 hour.");
        			break;
        		case 4:
        			findPreference("sWeatherFreq").setSummary("Refresh weather every 4 hours.");
        			break;
        		case 8:
        			findPreference("sWeatherFreq").setSummary("Refresh weather every 8 hours.");
        			break;
        		default:
        			findPreference("sWeatherFreq").setSummary("Refresh weather at default interval.");
        	}
        	
        	// "Set Foreground Mode".
    		prefwidgetPersistence = findPreference("widgetPersistence");

        	if (Build.VERSION.SDK_INT <  5) {
    			OMC.PREFS.edit().putBoolean("widgetPersistence", false).commit();
				((PreferenceCategory)findPreference("allClocks")).removePreference(prefwidgetPersistence);
			}
				
        	// "Enable Theme Tester".
        	prefbSkinner = findPreference("bSkinner");

        	// "Clear Render Caches".
        	prefclearCache = findPreference("clearCache");

        	// "Contact Xaffron".
        	prefemailMe = findPreference("emailMe");

        	// Version text.
        	if (OMC.FREEEDITION) {
        		findPreference("sVersion").setTitle("Version " + OMC.THISVERSION + " Free");
        		findPreference("sVersion").setSummary("Tap me to get the full version!");
        		findPreference("sVersion").setSelectable(true);
        	} else {
        		findPreference("sVersion").setTitle("Version " + OMC.THISVERSION);
        		findPreference("sVersion").setSummary("Thanks for your support!");
        		findPreference("sVersion").setSelectable(false);
        	}

        	// We really don't need this in the prefs screen since we don't allow users to disable the freeware widget size
    		((PreferenceScreen)findPreference("widgetPrefs")).removePreference(findPreference("bFourByTwo"));

    		
    		// If the app is in singleton mode, don't allow themes!
    		if (OMC.SINGLETON) {
        		((PreferenceCategory)findPreference("thisClock")).removePreference(prefloadThemeFile);
        		((PreferenceCategory)findPreference("allClocks")).removePreference(prefclearCache);
        		((PreferenceScreen)findPreference("widgetPrefs")).removePreference(prefbSkinner);
        	}
    		
    		// If it's free, 
    		if (OMC.FREEEDITION) {
        		findPreference("bFiveByFour").setEnabled(false);
        		findPreference("bFiveByFour").setSelectable(false);
        		findPreference("bFiveByTwo").setEnabled(false);
        		findPreference("bFiveByTwo").setSelectable(false);
        		findPreference("bFiveByOne").setEnabled(false);
        		findPreference("bFiveByOne").setSelectable(false);
        		findPreference("bFourByFour").setEnabled(false);
        		findPreference("bFourByFour").setSelectable(false);
        		findPreference("bFourByOne").setEnabled(false);
        		findPreference("bFourByOne").setSelectable(false);
        		findPreference("bThreeByThree").setEnabled(false);
        		findPreference("bThreeByThree").setSelectable(false);
        		findPreference("bThreeByOne").setEnabled(false);
        		findPreference("bThreeByOne").setSelectable(false);
        		findPreference("bTwoByTwo").setEnabled(false);
        		findPreference("bTwoByTwo").setSelectable(false);
        		findPreference("bTwoByOne").setEnabled(false);
        		findPreference("bTwoByOne").setSelectable(false);
        		findPreference("bOneByThree").setEnabled(false);
        		findPreference("bOneByThree").setSelectable(false);
    		}

    		// This is the free/paid version conflict dialog.
    		String sOtherEd = OMC.FREEEDITION? "com.sunnykwong.omc":"com.sunnykwong.freeomc";
    		String sConflictEd = OMC.FREEEDITION? "paid":"free";
    		try {
    			OMC.PKM.getApplicationInfo(sOtherEd, PackageManager.GET_META_DATA);
            	mAD = new AlertDialog.Builder(this)
        		.setTitle("WARNING!\nConflict with " + sConflictEd + " edition")
        		.setMessage("Theme customization and clock settings will not work properly.\nPlease uninstall the free edition at your earliest convenience!")
        	    .setCancelable(true)
        	    .setIcon(getResources().getIdentifier(OMC.APPICON, "drawable", OMC.PKGNAME))
        	    .setOnKeyListener(new OnKeyListener() {
        	    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
        	    		dialogCancelled();
        	    		return true;
        	    	};
        	    })
        	    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
        	    		dialogCancelled();
					}
				})
        	    .create();
            	mAD.show();
    		} catch (NameNotFoundException e) {
    			// If we can't find the conflicting package, we're all good - no need to show warning
    		}

    		mRefresh = (new Thread() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						while(true) {
							mHandler.post(mUpdatePrefs);
							Thread.sleep(1000l);
						}
					} catch (InterruptedException e) {
						// interrupted; stop gracefully
					}
				}
			});
    		mRefresh.start();
    		
    		// This is the help/FAQ dialog.
    		
    		if (OMC.SHOWHELP) {
    			OMC.FAQS = OMC.RES.getStringArray(getResources().getIdentifier("faqs", "array", OMC.PKGNAME));
				LayoutInflater li = LayoutInflater.from(this);
				LinearLayout ll = (LinearLayout)(li.inflate(getResources().getIdentifier("faqdialog", "layout", OMC.PKGNAME), null));
				mTextView = (TextView)ll.findViewById(getResources().getIdentifier("splashtext", "id", OMC.PKGNAME));
				mTextView.setAutoLinkMask(Linkify.ALL);
				mTextView.setMinLines(8);
				mTextView.setText(OMC.FAQS[OMC.faqtoshow++]);
				OMC.faqtoshow = OMC.faqtoshow==OMC.FAQS.length?0:OMC.faqtoshow;
				
				mCheckBox = (CheckBox)ll.findViewById(getResources().getIdentifier("splashcheck", "id", OMC.PKGNAME));
				mCheckBox.setChecked(!OMC.SHOWHELP);
				mCheckBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						OMC.SHOWHELP = !isChecked;
					}
				});
	
				((Button)ll.findViewById(getResources().getIdentifier("faqOK", "id", OMC.PKGNAME))).setOnClickListener(new Button.OnClickListener() {
					
					@Override
					public void onClick(android.view.View v) {
						OMC.PREFS.edit().putBoolean("showhelp", OMC.SHOWHELP).commit();
						mAD.dismiss();
					}
				});
				((Button)ll.findViewById(getResources().getIdentifier("faqNeutral", "id", OMC.PKGNAME))).setOnClickListener(new Button.OnClickListener() {
					
					@Override
					public void onClick(android.view.View v) {
						mTextView.setText(OMC.FAQS[OMC.faqtoshow++]);
						mTextView.invalidate();
						OMC.faqtoshow = OMC.faqtoshow==OMC.FAQS.length?0:OMC.faqtoshow;
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
        	
        } else {
            // If they gave us an intent without the widget id, just bail.
        	if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Pref","Called by Launcher - do nothing");
        	OMCPrefActivity.mAD = new AlertDialog.Builder(this)
        		.setTitle("Thanks for downloading!")
        		.setMessage("To begin, hit the back button to go back to the home screen, then access the 'Widgets' list to see " + OMC.APPNAME + " listed.  Have fun!")
        	    .setCancelable(true)
        	    .setIcon(getResources().getIdentifier(OMC.APPICON, "drawable", OMC.PKGNAME))
        	    .setOnKeyListener(new OnKeyListener() {
        	    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
        	    		dialogCancelled();
        	    		return true;
        	    	};
        	    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
					
					@Override
					public void onCancel(DialogInterface dialog) {
						dialogCancelled();
					}
				})
        	    .create();
        	OMCPrefActivity.mAD.setCanceledOnTouchOutside(true);
        	OMCPrefActivity.mAD.show();

        	((OMC)getApplication()).widgetClicks();
        	
        }

    }

    // If user clicks on a preference...
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
    	if (preference == findPreference("deleteOMCThemes")){
			final CharSequence[] items = {"Yes, delete all", "Yes, restore defaults", "No"};
			new AlertDialog.Builder(this)
				.setTitle("Delete all Themes from your SD Card/Memory?")
				.setItems(items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							switch (item) {
								case 0: //Yes
									OMC.removeDirectory(
											new File(
													Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes"));
						    		OMC.purgeTypefaceCache();
						    		OMC.purgeBitmapCache();
						    		OMC.purgeImportCache();
						    		OMC.purgeEmailCache();
						    		OMC.THEMEMAP.clear();
						        	OMC.WIDGETBMPMAP.clear();
						    		Toast.makeText(OMCPrefActivity.this, "OMCThemes folder deleted.", Toast.LENGTH_SHORT).show();
									break;
								case 1: //Yes but restore
									File omcroot = new File(
											Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes");
									OMC.removeDirectory(omcroot);
									omcroot.mkdirs();
						        	OMC.setupDefaultTheme();
									startActivity(OMC.GETSTARTERPACKINTENT);
						    		OMC.purgeTypefaceCache();
						    		OMC.purgeBitmapCache();
						    		OMC.purgeImportCache();
						    		OMC.purgeEmailCache();
						    		OMC.THEMEMAP.clear();
						        	OMC.WIDGETBMPMAP.clear();
						        	OMCThemePickerActivity.THEMEROOT.mkdir();
						    		Toast.makeText(OMCPrefActivity.this, "Default Clock Pack restored.", Toast.LENGTH_SHORT).show();
									break;
								case 2: //No
									//do nothing
									break;
								default:
									//do nothing
							}
						}
				})
				.show();
    	}
    	if (preference == findPreference("weather")){
			final CharSequence[] items = {"Disabled", "Follow Device (default)", "Set Fixed Location"};
			new AlertDialog.Builder(this)
				.setTitle("Experimental Feature\nTry at own risk!")
				.setItems(items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {

							switch (item) {
								case 0: //Disabled (default)
									OMC.PREFS.edit().putString("weathersetting", "disabled").commit();
									break;
								case 1: //Follow Device
									OMC.PREFS.edit().putString("weathersetting", "bylatlong").commit();
						    		OMC.updateWeather();
									break;
								case 2: //Set Location
									startActivityForResult(new Intent(OMCPrefActivity.this, OMCFixedLocationActivity.class), 0);
									break;
								default:
									//do nothing
							}
						}
				})
				.show();
    	}
    	if (preference == findPreference("tweakTheme")){
    		getPreferenceScreen().setEnabled(false);
    		Intent tweakIntent = new Intent(this, OMCThemeTweakerActivity.class);
    		tweakIntent.putExtra("aWI", OMCPrefActivity.appWidgetID);
    		tweakIntent.putExtra("theme", OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME));
    		tweakIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    		startActivityForResult(tweakIntent,0);
    	}
    	if (preference == findPreference("emailMe")) {
			final CharSequence[] items = {"Email", "Donate", "Facebook"};
			new AlertDialog.Builder(this)
				.setTitle("Contact Xaffron")
				.setItems(items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							switch (item) {
								case 0: //Email
									Intent it = new Intent(android.content.Intent.ACTION_SEND)
		    		   					.setType("plain/text")
		    		   					.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"skwong@consultant.com"})
		    		   					.putExtra(android.content.Intent.EXTRA_SUBJECT, OMC.APPNAME + " Feedback v" + OMC.THISVERSION);
					    		   	startActivity(Intent.createChooser(it, "Contact Xaffron for issues, help & support."));  
					    		   	finish();
					    		   	break;
								case 1: //Donate
						    		it = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=S9VEL3WFGXK48"));
						    		startActivity(it);
						    		finish();
									break;
								case 2: //Facebook
						    		it = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/389054721147516"));
						    		
						    		// Add try and catch because the scheme could be changed in an update! 
						    		// Another reason is that the Facebook-App is not installed 
						    		try {
							    		startActivity(it);
						    		} catch (ActivityNotFoundException ex) {      
						    		// start web browser and the facebook mobile page as fallback    
						    			it = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com/XaffronSoftware"));    
						    			startActivity(it); 
						    		}
						    		finish();
						    		break;
								default:
									//do nothing
							}
						}
				})
				.show();
    	}
    	if (preference == getPreferenceScreen().findPreference("widgetPrefs") && OMC.FREEEDITION) {
    		final CharSequence TitleCS = "Are there other widget sizes?";
    		final CharSequence MessageCS = "Actually, the paid version offers widget sizes of 5x4/5x2/5x1, 4x4/4x2/4x1, 3x3/3x1, 2x2/2x1 and 1x3.\nPlease consider upgrading to get these sizes!";
    		final CharSequence PosButtonCS = "Take me to the paid version!";
        	OMCPrefActivity.mAD = new AlertDialog.Builder(this)
			.setCancelable(true)
			.setTitle(TitleCS)
			.setMessage(MessageCS)
			.setPositiveButton(PosButtonCS, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						OMCPrefActivity.mAD.dismiss();
						OMCPrefActivity.this.startActivity(OMC.OMCMARKETINTENT);
						OMCPrefActivity.this.finish();
					}
				}).create();
        	OMCPrefActivity.mAD.show();
    	}
    	if (preference == getPreferenceScreen().findPreference("widgetCredits")) {
    		startActivityForResult(OMC.CREDITSINTENT,0);
    	}
    	if (preference == getPreferenceScreen().findPreference("sVersion")) {
			this.startActivity(OMC.OMCMARKETINTENT);
        	this.finish();
    	}
    	if (preference == getPreferenceScreen().findPreference("loadThemeFile")) {
    		getPreferenceScreen().setEnabled(false);
    		OMC.PICKTHEMEINTENT.putExtra("appWidgetID", appWidgetID);
    		OMC.PICKTHEMEINTENT.putExtra("default", OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME));
    		startActivityForResult(OMC.PICKTHEMEINTENT,0);
    	}
    	if (preference == getPreferenceScreen().findPreference("oTTL")) {
    		if (OMC.SINGLETON && OMC.FREEEDITION) {
            	OMCPrefActivity.mAD = new AlertDialog.Builder(this)
    			.setCancelable(true)
    			.setTitle("Think of the possibilities!")
    			.setMessage("The paid version lets you set " + OMC.APPNAME + " to launch any activity you want, plus it offers widget sizes of 4x2, 4x1, 3x1 and 2x1.\nPlease consider upgrading to get these sizes!")
    			.setPositiveButton("Take me to the paid version!", new DialogInterface.OnClickListener() {
    					
    					@Override
    					public void onClick(DialogInterface dialog, int which) {
    						// TODO Auto-generated method stub
    						OMCPrefActivity.mAD.dismiss();
    						OMCPrefActivity.this.startActivity(OMC.OMCMARKETINTENT);
    						OMCPrefActivity.this.finish();
    						
    					}
    				}).create();
            	OMCPrefActivity.mAD.show();
    		} else {
    			final CharSequence[] items = {"Open options (default)", "Do nothing", "Weather Forecast (Experimental)", "View alarms (Experimental)", "Other activity..."};
    			final String[] values = {"default", "noop", "weather", "alarms", "activity"};
				
				final AlertDialog dlgTTL  =  new AlertDialog.Builder(this)
				.setTitle("Choose action")
				.setItems(items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							if (values[item].equals("default")) {
								OMC.PREFS.edit().putString("URI"+OMC.COMPASSPOINTS[iTTLArea], "")
									.putString("URIDesc"+OMC.COMPASSPOINTS[iTTLArea], "Widget Prefs").commit();
							}
							if (values[item].equals("noop")) {
								OMC.PREFS.edit().putString("URI"+OMC.COMPASSPOINTS[iTTLArea], "noop")
									.putString("URIDesc"+OMC.COMPASSPOINTS[iTTLArea], "Nothing").commit();
							}
							if (values[item].equals("weather")) {
								OMC.PREFS.edit().putString("URI"+OMC.COMPASSPOINTS[iTTLArea], "weather")
									.putString("URIDesc"+OMC.COMPASSPOINTS[iTTLArea], "Forecast").commit();
							}
							if (values[item].equals("alarms")) {
								OMC.PREFS.edit().putString("URI"+OMC.COMPASSPOINTS[iTTLArea], "alarms")
									.putString("URIDesc"+OMC.COMPASSPOINTS[iTTLArea], "View Alarms").commit();
							}
							if (values[item].equals("activity")) {
					    		getPreferenceScreen().setEnabled(false);
					    		cancelTTL();
					    		Intent mainIntent = new Intent(Intent.ACTION_MAIN,
					        			null);
								mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
					
								Intent pickIntent = new	Intent(Intent.ACTION_PICK_ACTIVITY);
								pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
								startActivityForResult(pickIntent, iTTLArea);
								mainIntent=null;
								pickIntent=null;
							}
							for (int iCompass = 0; iCompass < 9; iCompass++) {
								btnCompass[iCompass].setText(OMC.PREFS.getString("URIDesc"+OMC.COMPASSPOINTS[iCompass],"Widget Prefs"));
							}
						}
				}).create();

				LayoutInflater li = LayoutInflater.from(this);
				LinearLayout ll = (LinearLayout)(li.inflate(getResources().getIdentifier("ttlpreview", "layout", OMC.PKGNAME), null));

				for (int iCompass = 0; iCompass < 9; iCompass++) {
					btnCompass[iCompass] = (Button)ll.findViewById(getResources().getIdentifier("button" + OMC.COMPASSPOINTS[iCompass] + "Prv", "id", OMC.PKGNAME));
					btnCompass[iCompass].setText(OMC.PREFS.getString("URIDesc"+OMC.COMPASSPOINTS[iCompass],"Widget Prefs"));
					final int iTTL = iCompass;
					btnCompass[iCompass].setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							iTTLArea=iTTL;
							dlgTTL.show();
						}
					});
				}
    			mTTL = new AlertDialog.Builder(this)
    					.setView(ll)
    					.setTitle("Area to customize:")
    					.show();
    		}
    	}
    	if (preference == getPreferenceScreen().findPreference("clearCache")) {
    		OMC.purgeTypefaceCache();
    		OMC.purgeBitmapCache();
    		OMC.purgeImportCache();
    		OMC.purgeEmailCache();
    		OMC.THEMEMAP.clear();
        	OMC.WIDGETBMPMAP.clear();
    		Toast.makeText(this, "Caches Cleared", Toast.LENGTH_SHORT).show();
    	}
    	if (preference == getPreferenceScreen().findPreference("timeZone")) {
    		getPreferenceScreen().setEnabled(false);
    		cancelTTL();
    		Intent mainIntent = new Intent(Intent.ACTION_MAIN,
        			null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

			Intent pickIntent = new Intent(OMC.CONTEXT, ZoneList.class);
			startActivityForResult(pickIntent, OMCPrefActivity.appWidgetID);
			mainIntent=null;
			pickIntent=null;
    	}
    	return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    // The result is obtained in onActivityResult:
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	prefloadThemeFile.setSummary(OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME)+ " selected.");
		if (OMC.PREFS.getString("sTimeZone", "default").equals("default")) {
			getPreferenceScreen().findPreference("timeZone").setSummary("(Following Device Time Zone)");
		} else {
			getPreferenceScreen().findPreference("timeZone").setSummary(OMC.PREFS.getString("sTimeZone", "default"));
		}
		getPreferenceScreen().setEnabled(true);

		mRefresh = (new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					while(true) {
						mHandler.post(mUpdatePrefs);
						Thread.sleep(1000l);
					}
				} catch (InterruptedException e) {
					// interrupted; stop gracefully
				}
			}
		});
		mRefresh.start();
		// If it's an independent child activity, do nothing
		if (requestCode == 0) return;
		if (data != null) {
			String s = data.toUri(MODE_PRIVATE).toString();
			
			OMC.PREFS.edit().putString("URI"+OMC.COMPASSPOINTS[requestCode], s)
				.putString("URIDesc"+OMC.COMPASSPOINTS[requestCode], "Custom Activity").commit();
		}
	}
    
	public void cancelTTL() {
       	if (mTTL!=null) { // && mAD.isShowing()
       		mTTL.dismiss();
       		mTTL = null;
       	}
	}
	
    public void dialogCancelled() {
       	if (OMCPrefActivity.mAD!=null) { // && mAD.isShowing()
       		OMCPrefActivity.mAD.dismiss();
       		OMCPrefActivity.mAD = null;
       	}
       	if (mTTL!=null) { // && mAD.isShowing()
       		mTTL.dismiss();
       		mTTL = null;
       	}
    	finish();
    }

    @Override
    public void onPause() {
    	super.onPause();
		if (appWidgetID >= 0) {

			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Pref","Saving Prefs for Widget " + OMCPrefActivity.appWidgetID);
			OMC.FG = OMC.PREFS.getBoolean("widgetPersistence", true)? true : false;
			OMC.UPDATEFREQ = Integer.parseInt(OMC.PREFS.getString("sUpdateFreq", "30")) * 1000;
	    	OMC.setPrefs(OMCPrefActivity.appWidgetID);
	    	if (OMC.WIDGETBMPMAP.containsKey(OMCPrefActivity.appWidgetID)) {
	    		if (!OMC.WIDGETBMPMAP.get(OMCPrefActivity.appWidgetID).isRecycled()) OMC.WIDGETBMPMAP.get(OMCPrefActivity.appWidgetID).recycle();
	        	OMC.WIDGETBMPMAP.remove(OMCPrefActivity.appWidgetID);
	    	}
	
	    	OMC.toggleWidgets(getApplicationContext());
	
			// Set the alarm for next tick first, so we don't lose sync
    		getApplicationContext().sendBroadcast(OMC.WIDGETREFRESHINTENT);
			OMC.setServiceAlarm(System.currentTimeMillis()+10500l, (System.currentTimeMillis()+10500l)/1000l*1000l);

		}
		if (mRefresh!=null) mRefresh.interrupt();
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
    }
} 