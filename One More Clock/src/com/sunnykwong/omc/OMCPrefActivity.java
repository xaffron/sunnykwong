package com.sunnykwong.omc;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.format.Time;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import java.lang.Runnable;

import org.json.JSONException;
import org.json.JSONObject;

public class OMCPrefActivity extends PreferenceActivity implements OnPreferenceChangeListener{ 
    /** Called when the activity is first created. */
    static int appWidgetID;
    static AlertDialog mAD;
    final Time timeTemp = new Time(), timeTemp2 = new Time();
    Handler mHandler;
    CheckBox mCheckBox;
    TextView mTextView;
    Thread mRefresh;
    boolean isInitialConfig=false, mTempFlag=false;
    Preference prefWeather, prefWeatherDisplay;
    Preference prefloadThemeFile, prefclearCache, prefbSkinner, prefTimeZone;
    Preference prefsUpdateFreq, prefwidgetPersistence, prefemailMe, preftweakTheme;

    final Runnable mUpdatePrefs = new Runnable() {
    	@Override
    	public void run() {								
    		try {
        		String sWSetting = OMC.PREFS.getString("weathersetting", "bylatlong");
    			JSONObject jsonWeather = new JSONObject(OMC.PREFS.getString("weather", ""));
    			String sCity = jsonWeather.getString("city");
    			timeTemp.set(OMC.NEXTWEATHERREFRESH);
    			timeTemp2.set(OMC.LASTWEATHERTRY);
        		if (sWSetting.equals("bylatlong")) {
        			prefWeather.setTitle("Weather: " + sCity +" (Detected)");
        			prefWeather.setSummary("Last try: "+timeTemp2.format("%R") + " Next Refresh: "+timeTemp.format("%R"));
        			prefWeatherDisplay.setSummary("Now displaying in "+ OMC.PREFS.getString("weatherdisplay", "f").toUpperCase());
        		} else if (sWSetting.equals("specific")) {
        			prefWeather.setTitle("Weather: Unknown (Fixed)");
        			prefWeather.setSummary("Last try: "+timeTemp2.format("%R") + " Next Refresh: "+timeTemp.format("%R"));
        			prefWeatherDisplay.setSummary("Now displaying in "+ OMC.PREFS.getString("weatherdisplay", "f").toUpperCase());
        		} else {
        			prefWeather.setTitle("Weather updates disabled");
        			prefWeatherDisplay.setSummary("Now displaying in "+ OMC.PREFS.getString("weatherdisplay", "f").toUpperCase());
        			prefWeather.setSummary("Tap to enable");
        		} 
    		} catch (JSONException e) {
    			prefWeather.setTitle("Weather updates disabled");
    			prefWeather.setSummary("Tap to enable");
    			prefWeatherDisplay.setSummary("Now displaying in "+ OMC.PREFS.getString("weatherdisplay", "f").toUpperCase());
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
		// FIX FOR NOMEDIA
		if (OMC.checkSDPresent()) ((OMC)(this.getApplication())).fixnomedia();

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
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Pref"," Called by Widget " + appWidgetID);

			if (OMC.SINGLETON) setTitle(OMC.SINGLETONNAME + " - Preferences");

			OMC.getPrefs(appWidgetID);
        	OMC.PREFS.edit().putBoolean("widgetPersistence", OMC.FG).commit();
        	OMC.PREFS.edit().putBoolean("bFourByTwo", true).commit();
        	
        	if (OMC.FREEEDITION) {
    			OMC.PREFS.edit().putBoolean("bFourByFour", false)
    					.putBoolean("bFourByOne", false)
    					.putBoolean("bThreeByThree", false)
    					.putBoolean("bThreeByOne", false)
    					.putBoolean("bTwoByTwo", false)
    					.putBoolean("bTwoByOne", false)
    					.putBoolean("bOneByThree", false)
    					.commit();
    		}
        	
    		this.getPreferenceManager().setSharedPreferencesName(OMC.SHAREDPREFNAME);
        	addPreferencesFromResource(getResources().getIdentifier("omcprefs", "xml", OMC.PKGNAME));
        	prefemailMe = findPreference("emailMe");
        	prefloadThemeFile = findPreference("loadThemeFile");
        	prefclearCache = findPreference("clearCache");
        	prefbSkinner = findPreference("bSkinner");
        	prefTimeZone = findPreference("timeZone");
    		if (OMC.PREFS.getString("sTimeZone", "default").equals("default")) {
        		findPreference("timeZone").setSummary("(Following Device Time Zone)");
    		} else {
    			findPreference("timeZone").setSummary(OMC.PREFS.getString("sTimeZone", "default"));
    		}

    		prefwidgetPersistence = findPreference("widgetPersistence");
        	preftweakTheme = findPreference("tweakTheme");
        	
        	prefsUpdateFreq = findPreference("sUpdateFreq");
        	prefsUpdateFreq.setOnPreferenceChangeListener(this);
        	prefsUpdateFreq.setSummary("Redraw every " + OMC.PREFS.getString("sUpdateFreq", "30") + " seconds.");

        	prefWeather = findPreference("weather");
        	prefWeatherDisplay = findPreference("weatherDisplay");
			if (Build.VERSION.SDK_INT <  5) {
    			OMC.PREFS.edit().putBoolean("widgetPersistence", false).commit();
				((PreferenceCategory)findPreference("allClocks")).removePreference(prefwidgetPersistence);
			}
				
        	if (OMC.FREEEDITION) {
        		findPreference("sVersion").setTitle("Version " + OMC.THISVERSION + " Free");
        		findPreference("sVersion").setSummary("Tap me to get the full version!");
        		findPreference("sVersion").setSelectable(true);
        	} else {
        		findPreference("sVersion").setTitle("Version " + OMC.THISVERSION);
        		findPreference("sVersion").setSummary("Thanks for your support!");
        		findPreference("sVersion").setSelectable(false);
        	}

    		((PreferenceScreen)findPreference("widgetPrefs")).removePreference(findPreference("bFourByTwo"));
//    		findPreference("bFourByTwo").setEnabled(false);

    		if (OMC.SINGLETON) {
        		((PreferenceCategory)findPreference("thisClock")).removePreference(prefloadThemeFile);
        		((PreferenceCategory)findPreference("allClocks")).removePreference(prefclearCache);
        		((PreferenceScreen)findPreference("widgetPrefs")).removePreference(prefbSkinner);
        	}
    		
    		if (OMC.FREEEDITION) {
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

    		
    		// This is the help/FAQ dialog.
    		
    		if (OMC.SHOWHELP) {
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
    		mRefresh = (new Thread() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						while(true) {
							mHandler.post(mUpdatePrefs);
							Thread.sleep(5000l);
						}
					} catch (InterruptedException e) {
						// interrupted; stop gracefully
					}
				}
			});
    		mRefresh.start();
        	
        } else {
            // If they gave us an intent without the widget id, just bail.
        	if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Pref","Called by Launcher - do nothing");
        	OMCPrefActivity.mAD = new AlertDialog.Builder(this)
        		.setTitle("Thanks for downloading!")
        		.setMessage("To begin, hit the back button to go back to the home screen, then push the menu button, select 'Add', then 'Widgets' to see " + OMC.APPNAME + " listed.  Have fun!")
        	    .setCancelable(true)
        	    .setIcon(getResources().getIdentifier(OMC.APPICON, "drawable", OMC.PKGNAME))
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
    	if (preference==findPreference("sUpdateFreq")) {
    		preference.setSummary("Redraw every " + (String)newValue + " seconds.");
    		return true;
    	}
    	return false;
    }
    
    // If user clicks on a preference...
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
    	if (preference == findPreference("weather")){
			final CharSequence[] items = {"Disabled", "Follow Device (default)", "Set Location", "Update Now"};
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
						    		GoogleWeatherXMLHandler.updateWeather();
									break;
								case 2: //Set Location
									OMC.PREFS.edit().putString("weathersetting", "specific").commit();
									break;
								case 3: //Update Now
						    		GoogleWeatherXMLHandler.updateWeather();
									break;
								default:
									//do nothing
							}
						}
				})
				.show();
    	}
    	if (preference == findPreference("weatherDisplay")){
			final CharSequence[] items = {"Show Celsius", "Show Fahrenheit"};
			new AlertDialog.Builder(this)
				.setTitle("Experimental Feature\nTry at own risk!")
				.setItems(items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							switch (item) {
								case 0: //Celsius (default)
									OMC.PREFS.edit().putString("weatherdisplay", "c").commit();
									break;
								case 1: //Fahrenheit
									OMC.PREFS.edit().putString("weatherdisplay", "f").commit();
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
		    		   					.putExtra(android.content.Intent.EXTRA_SUBJECT, OMC.APPNAME + " Feedback v" + OMC.THISVERSION);
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
    	if (preference == getPreferenceScreen().findPreference("widgetPrefs") && OMC.FREEEDITION) {
    		final CharSequence TitleCS = "Why are the widgets so big?";
    		final CharSequence MessageCS = "Actually, the paid version offers widget sizes of 4x4, 4x2, 4x1, 3x3, 3x1, 2x2, 2x1 and 1x3.\nPlease consider upgrading to get these sizes!";
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
    		startActivityForResult(OMC.IMPORTTHEMEINTENT,0);
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
    			new AlertDialog.Builder(this)
    					.setTitle("Tap on clock to:")
    					.setItems(items, new DialogInterface.OnClickListener() {
    							public void onClick(DialogInterface dialog, int item) {
    								if (values[item].equals("default")) {
    									OMC.PREFS.edit().putString("URI", "").commit();
    								}
    								if (values[item].equals("noop")) {
    									OMC.PREFS.edit().putString("URI", "noop").commit();
    								}
    								if (values[item].equals("weather")) {
    									OMC.PREFS.edit().putString("URI", "weather").commit();
    								}
    								if (values[item].equals("alarms")) {
    									OMC.PREFS.edit().putString("URI", "alarms").commit();
    								}
    								if (values[item].equals("activity")) {
    						    		getPreferenceScreen().setEnabled(false);
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
    							}
    					})
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
		if (OMC.PREFS.getString("sTimeZone", "default").equals("default")) {
			getPreferenceScreen().findPreference("timeZone").setSummary("(Following Device Time Zone)");
		} else {
			getPreferenceScreen().findPreference("timeZone").setSummary(OMC.PREFS.getString("sTimeZone", "default"));
		}
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
	
			sendBroadcast(OMC.WIDGETREFRESHINTENT); 
		}
		if (mRefresh!=null) mRefresh.interrupt();
		
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
    }
} 