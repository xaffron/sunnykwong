package com.sunnykwong.omc;

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


public class OMCPrefActivity extends PreferenceActivity implements OnPreferenceChangeListener{ 
    /** Called when the activity is first created. */
    static int appWidgetID;
    static AlertDialog mAD;
    CheckBox mCheckBox;
    TextView mTextView;
    boolean isInitialConfig=false, mTempFlag=false;
    Preference prefloadThemeFile, prefclearCache, prefdownloadStarterPack, prefbSkinner;
    Preference prefsUpdateFreq, prefwidgetPersistence, prefemailMe, preftweakTheme ;

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
        	prefdownloadStarterPack = findPreference("downloadStarterPack");
        	prefbSkinner = findPreference("bSkinner");
        	prefwidgetPersistence = findPreference("widgetPersistence");
        	preftweakTheme = findPreference("tweakTheme");
        	
        	prefsUpdateFreq = findPreference("sUpdateFreq");
        	prefsUpdateFreq.setOnPreferenceChangeListener(this);
        	prefsUpdateFreq.setSummary("Redraw every " + OMC.PREFS.getString("sUpdateFreq", "30") + " seconds.");
        	
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
        		((PreferenceCategory)findPreference("allClocks")).removePreference(prefdownloadStarterPack);
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
    		final CharSequence MessageCS = "Actually, the donate version offers widget sizes of 4x2, 4x1, 3x1 and 2x1.\nPlease consider upgrading to get these sizes!\nAlternatively, if you use an alternative launcher such as ADW or Launcher Pro, you should be able to approximate this ability by dynamically resizing the 4x2 widget.";
    		final CharSequence PosButtonCS = "Take me to the donate version!";
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
    	if (preference == getPreferenceScreen().findPreference("downloadStarterPack")) {
        	OMCPrefActivity.mAD = new AlertDialog.Builder(this)
			.setCancelable(true)
			.setTitle("Starter Clock Pack")
			.setMessage("Any theme customizations you have made in your sdcard's .OMCThemes folder will be overwritten.  Are you sure?\n(If not sure, tap Yes)")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
		        	Toast.makeText(OMCPrefActivity.this, "Extracting starter clock pack...", Toast.LENGTH_LONG).show();
//		        	OMCThemePickerActivity.THEMEROOT.mkdir();
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
    		getPreferenceScreen().setEnabled(false);
    		startActivityForResult(OMC.IMPORTTHEMEINTENT,0);
    	}
    	if (preference == getPreferenceScreen().findPreference("oTTL")) {
    		if (OMC.SINGLETON && OMC.FREEEDITION) {
            	OMCPrefActivity.mAD = new AlertDialog.Builder(this)
    			.setCancelable(true)
    			.setTitle("Think of the possibilities!")
    			.setMessage("The donate version lets you set " + OMC.APPNAME + " to launch any activity you want, plus it offers widget sizes of 4x2, 4x1, 3x1 and 2x1.\nPlease consider upgrading to get these sizes!")
    			.setPositiveButton("Take me to the donate version!", new DialogInterface.OnClickListener() {
    					
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
    			final CharSequence[] items = {"Open options (default)", "Do nothing", "Launch activity..."};
    			final String[] values = {"default", "noop", "activity"};
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
    		OMC.THEMEMAP.clear();
    		Toast.makeText(this, "Caches Cleared", Toast.LENGTH_SHORT).show();
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
		if (appWidgetID >= 0) {

		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Pref","Saving Prefs for Widget " + OMCPrefActivity.appWidgetID);
		OMC.FG = OMC.PREFS.getBoolean("widgetPersistence", true)? true : false;
		OMC.UPDATEFREQ = Integer.parseInt(OMC.PREFS.getString("sUpdateFreq", "30")) * 1000;
    	OMC.setPrefs(OMCPrefActivity.appWidgetID);

    	OMC.toggleWidgets(getApplicationContext());

		sendBroadcast(OMC.WIDGETREFRESHINTENT); 
		}
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
    }
} 