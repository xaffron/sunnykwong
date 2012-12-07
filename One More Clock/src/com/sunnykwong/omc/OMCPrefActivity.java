package com.sunnykwong.omc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ActionBar.LayoutParams;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.format.Time;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class OMCPrefActivity extends PreferenceActivity { 
    /** Called when the activity is first created. */
    static int appWidgetID;
    static AlertDialog mAD;
    AlertDialog mTTL;
	Button[] btnCompass = new Button[9];
    final Time tLastTry = new Time();
    final Time tLastRefresh = new Time();
    final Time tNextRefresh = new Time();
    final Time tNextRequest = new Time();

    CheckBox mCheckBox;
    TextView mTextView;
    Thread mRefresh;
    boolean isInitialConfig=false, mTempFlag=false;
    Preference prefUpdWeatherNow, prefWeather, prefWeatherDisplay, prefWeatherProvider;
    Preference prefloadThemeFile, prefclearCache, prefbSkinner, prefTimeZone;
    ListPreference prefsUpdateFreq; 
    Preference prefwidgetPersistence, prefemailMe, preftweakTheme;
    int iTTLArea=0;

    AsyncTask<String,String,String> mWeatherRefresh;
	
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
    	

    	// Refresh list of installed Launcher Apps.
		List<ResolveInfo> launcherlist = OMC.PKM.queryIntentActivities(OMC.FINDLAUNCHERINTENT, 0);
		OMC.INSTALLEDLAUNCHERAPPS = new ArrayList<String>();
		OMC.INSTALLEDLAUNCHERAPPS.add("com.teslacoilsw.widgetlocker");
		OMC.INSTALLEDLAUNCHERAPPS.add("com.jiubang.goscreenlock");
		
		for (ResolveInfo info : launcherlist) {
			OMC.INSTALLEDLAUNCHERAPPS.add(info.activityInfo.packageName);
		}

    	
    	// FIX FOR BADTHEME
		if (OMC.checkSDPresent()) {
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
			if (OMC.SINGLETON) setTitle(OMC.SINGLETONNAME + OMC.RString("dashPreferences"));

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
        	addPreferencesFromResource(OMC.RXmlId("omcprefs"));

        	// ID specific preferences.
        	// "Set Widget Theme".
        	prefloadThemeFile = findPreference("loadThemeFile");
        	prefloadThemeFile.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
		        	prefloadThemeFile.setSummary(OMC.RString("preselected") + newValue + OMC.RString("postselected"));
					return true;
				}
			});
        	prefloadThemeFile.setSummary(OMC.RString("preselected") + OMC.PREFS.getString("widgetThemeLong", OMC.DEFAULTTHEMELONG) + OMC.RString("postselected"));
        	
    		// "Personalize Clock".
        	preftweakTheme = findPreference("tweakTheme");
        	
        	// "Use 24 Hour Clock".
        	findPreference("widget24HrClock").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if ((Boolean)newValue==true) {
						preference.setSummary(OMC.RString("use24HourTrue"));
					} else {
						preference.setSummary(OMC.RString("use24HourFalse"));
					}
					return true;
				}
			});
			if (OMC.PREFS.getBoolean("widget24HrClock", true)) {
	        	findPreference("widget24HrClock").setSummary(OMC.RString("use24HourTrue"));
			} else {
	        	findPreference("widget24HrClock").setSummary(OMC.RString("use24HourFalse"));
			}
        	
        	// "Show Leading Zero".
        	findPreference("widgetLeadingZero").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if ((Boolean)newValue==true) {
						preference.setSummary(OMC.RString("showLeadingZTrue"));
					} else {
						preference.setSummary(OMC.RString("showLeadingZFalse"));
					}
					return true;
				}
			});
			if (OMC.PREFS.getBoolean("widgetLeadingZero", true)) {
	        	findPreference("widgetLeadingZero").setSummary(OMC.RString("showLeadingZTrue"));
			} else {
	        	findPreference("widgetLeadingZero").setSummary(OMC.RString("showLeadingZFalse"));
			}
        	
        	// "M/D/Y Date Format".
        	findPreference("mmddDateFormat").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if ((Boolean)newValue==true) {
						preference.setSummary(OMC.RString("mmddDateFormatTrue"));
					} else {
						preference.setSummary(OMC.RString("mmddDateFormatFalse"));
					}
					return true;
				}
			});
			if (OMC.PREFS.getBoolean("mmddDateFormat", true)) {
	        	findPreference("mmddDateFormat").setSummary(OMC.RString("mmddDateFormatTrue"));
			} else {
	        	findPreference("mmddDateFormat").setSummary(OMC.RString("mmddDateFormatFalse"));
			}
        	
        	// "Change Time Zone".
        	prefTimeZone = findPreference("timeZone");
        	if (OMC.PREFS.getString("sTimeZone", "default").equals("default")) {
        		findPreference("timeZone").setSummary(OMC.RString("followingDeviceTimeZone"));
    		} else {
    			findPreference("timeZone").setSummary(OMC.PREFS.getString("sTimeZone", "default"));
    		}
        	
        	// "Set Clock Ahead/Behind"
        	Preference prefClockAdjustment = findPreference("clockAdjustment"); 
        	prefClockAdjustment.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					int offset = Integer.parseInt((String)newValue);
					if (offset<0) {
						preference.setSummary(-offset + OMC.RString("minutesBehind"));
					} else {
						preference.setSummary(offset + OMC.RString("minutesAhead"));
					}
					return true;
				}
			});
        	int offset = Integer.parseInt(OMC.PREFS.getString("clockAdjustment", "0"));
        	if (offset < 0 ) {
        		prefClockAdjustment.setSummary(-offset + OMC.RString("minutesBehind"));
        	} else {
        		prefClockAdjustment.setSummary(offset + OMC.RString("minutesAhead"));
        	}
        	
        	// "Backup this Widget"
        	Preference prefBackupWidget = findPreference("backupwidget");
        	prefBackupWidget.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					
					final Object[] backupOptions = new Object[2];
					backupOptions[0] = OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME)+OMC.TIME.format("%Y%m%d");
					backupOptions[1] = Boolean.FALSE;
					
					LinearLayout ll = new LinearLayout(OMCPrefActivity.this);
					ll.setOrientation(LinearLayout.VERTICAL);
					EditText et = new EditText(OMCPrefActivity.this);
					et.setText((String)backupOptions[0]);
					CheckBox cb = new CheckBox(OMCPrefActivity.this);
					cb.setText(OMC.RString("backupThemePersonalizations"));
					cb.setChecked(false);
					cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							if (isChecked) backupOptions[1]=Boolean.TRUE;
							else backupOptions[1]=Boolean.FALSE;
						}
					});
					
					ll.addView(et);
					ll.addView(cb);
					
					new AlertDialog.Builder(OMCPrefActivity.this)
						.setTitle(OMC.RString("nameOfBackup"))
						.setView(ll)
						.setPositiveButton(OMC.RString("_ok"), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								//	v141 backup work here														
								new AsyncTask<Object, String, String>() {
									@Override
									protected String doInBackground(
											Object... params) {
										String backupName = (String)params[0];
										boolean backupTheme = (Boolean)params[1];
										
										if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
											try {
												Toast.makeText(OMCPrefActivity.this, OMC.RString("sdcardNotDetected"), Toast.LENGTH_LONG).show();
											} catch (Exception e) {
												e.printStackTrace();
								        	}
										}
										File OMCRoot = Environment.getExternalStorageDirectory();
										
										JSONArray TTL= new JSONArray();
										for (int i=0; i<9; i++)
											TTL.put(OMC.PREFS.getString("URI"+OMC.COMPASSPOINTS[i], ""));
										JSONObject result = new JSONObject();
										try {
											result.put("TTL",TTL);
											result.put("clockAdjustment", OMC.PREFS.getString("clockAdjustment", "0"));
											result.put("widget24HrClock", OMC.PREFS.getBoolean("widget24HrClock", true));
											result.put("widgetLeadingZero",OMC.PREFS.getBoolean("widgetLeadingZero", true));
											result.put("mmddDateFormat",OMC.PREFS.getBoolean("mmddDateFormat", true));
											result.put("sTimeZone",OMC.PREFS.getString("sTimeZone", "default"));
											if (backupTheme) {

												result.put("widgetTheme", OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME));
												result.put("widgetThemeLong", OMC.PREFS.getString("widgetThemeLong", OMC.DEFAULTTHEMELONG));

											    //convert from paths to Android friendly Parcelable Uri's
											    System.out.println("dir: " + OMCRoot.getAbsolutePath());
											    System.out.println("name: " + backupName+".omc");
											    
											    File outzip = new File(OMCRoot.getAbsolutePath(),backupName+".omc");
											    if (outzip.exists()) outzip.delete();
											    else outzip.mkdirs();

											    
											    File f = new File(OMCRoot.getAbsolutePath() + "/" 
									        			+ OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME));
									        	
									        	try {
										        	ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outzip),8192));
										        	for (File file : f.listFiles())
												    {
										        		ZipEntry ze = new ZipEntry(new ZipEntry(OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME) + "/" + file.getName()));
											        	zos.putNextEntry(ze);
													    FileInputStream ffis = new FileInputStream(file);
														try {
															//Absolute luxury 1980 style!  Using an 8k buffer.
															byte[] buffer = new byte[8192];
															int iBytesRead=0;
															while ((iBytesRead=ffis.read(buffer))!= -1){
																zos.write(buffer, 0, iBytesRead);
															}
															zos.flush();
															zos.closeEntry();
														} catch (Exception e) {
											        		Log.w(OMC.OMCSHORT + "Prefs","cannot zip, zip error below");
															e.printStackTrace();
														}
														ffis.close();
											        	
												    }
								
										        	zos.finish();
										        	zos.close();

										        	OMC.JSONToFile(result,new File(OMCRoot.getAbsolutePath(),backupName+".omcbackup"));
										        	
												} catch (Exception e) {
													// File exists and read-only?  Shouldn't happen
									        		Log.w(OMC.OMCSHORT + "Picker","cannot zip, file already open or RO");
													e.printStackTrace();
												}
											}
											System.out.println(result.toString(3));
										} catch (JSONException e) {
											e.printStackTrace();
										}
										return "";
									}
									@Override
									protected void onPostExecute(String result) {
										Toast.makeText(OMCPrefActivity.this, OMC.RString("taskComplete"), Toast.LENGTH_SHORT).show();
									}
								}.execute(backupOptions[0],backupOptions[1]);
							}
						})
						.setNegativeButton(OMC.RString("_abandon"), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						})
						.create().show();
					
					return false;
				}
			});

        	// "Restore Widget Settings"
        	Preference prefRestoreWidget = findPreference("restorewidget");
        	prefRestoreWidget.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					
					final Object[] restoreOptions = new Object[2];
					restoreOptions[0] = "";
					restoreOptions[1] = Boolean.FALSE;
					
					LinearLayout ll = new LinearLayout(OMCPrefActivity.this);
					ll.setOrientation(LinearLayout.VERTICAL);
					RadioGroup rg = new RadioGroup(OMCPrefActivity.this);
//					ListView lv = new ListView(OMCPrefActivity.this);
					
					File OMCRoot = Environment.getExternalStorageDirectory();
					
					for (String filename: OMCRoot.list()) {
						final String fname = new String(filename);
						if (filename.endsWith(".backup")) {
							RadioButton tv = new RadioButton(OMCPrefActivity.this);
							int scale = (int)(OMC.RES.getDisplayMetrics().density);
							tv.setPadding(60*scale, 20*scale, 20*scale, 20*scale);
							tv.setText(filename);
							tv.setClickable(true);
							tv.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									restoreOptions[0]=fname;
									System.out.println("We're restoring " + restoreOptions[0]);
									System.out.println("restore theme? " + restoreOptions[1]);
								}
							});

							rg.addView(tv);
						}
					}
					
					CheckBox cb = new CheckBox(OMCPrefActivity.this);
					cb.setText(OMC.RString("overwriteThemePersonalizations"));
					cb.setChecked(false);
					cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							if (isChecked) {
								new AlertDialog.Builder(OMCPrefActivity.this)
								.setTitle(OMC.RString("overwriteThemePersonalizations"))
								.setMessage(OMC.RString("overwriteThemeWarning"))
								.setPositiveButton(OMC.RString("_ok"), new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										restoreOptions[1]=Boolean.TRUE;
									}	
								}).create().show();
							}
							else restoreOptions[1]=Boolean.FALSE;
						}
					});
					ll.addView(rg);
					ll.addView(cb);
					
					new AlertDialog.Builder(OMCPrefActivity.this)
						.setTitle(OMC.RString("nameOfBackup"))
						.setView(ll)
						.setNegativeButton(OMC.RString("_abandon"), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						})
						.setPositiveButton(OMC.RString("_ok"), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								//	v141 backup work here														
								new AsyncTask<Object, String, String>() {
									@Override
									protected String doInBackground(
											Object... params) {
										String restoreName = (String)params[0];
										boolean restoreTheme = (Boolean)params[1];
										
										if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
											try {
												Toast.makeText(OMCPrefActivity.this, OMC.RString("sdcardNotDetected"), Toast.LENGTH_LONG).show();
											} catch (Exception e) {
												e.printStackTrace();
								        	}
										}
										File OMCRoot = new File(Environment.getExternalStorageDirectory(),restoreName);
										
										try {
											JSONObject backup = OMC.streamToJSONObject(new FileInputStream(OMCRoot));
											OMC.PREFS.edit()
														.putString("clockAdjustment", backup.optString("clockAdjustment","0"))
														.putString("sTimeZone", backup.optString("sTimeZone","default"))
														.putBoolean("widget24HrClock", backup.optBoolean("widget24HrClock"))
														.putBoolean("widgetLeadingZero", backup.optBoolean("widgetLeadingZero"))
														.putBoolean("mmddDateFormat", backup.optBoolean("mmddDateFormat"))
														.commit();
											JSONArray TTL = backup.optJSONArray("TTL");
											for (int i=0; i<9; i++) {
												OMC.PREFS.edit().putString("URI"+OMC.COMPASSPOINTS[i], TTL.optString(i)).commit();
											}
											OMC.setPrefs(appWidgetID);
											
												if (restoreTheme) {
													OMC.PREFS.edit()
													.putString("widgetTheme", backup.optString("widgetTheme",OMC.DEFAULTTHEME))
													.putString("widgetThemeLong", backup.optString("widgetThemeLong",OMC.DEFAULTTHEMELONG))
													.commit();
													OMC.setPrefs(appWidgetID);

												    //convert from paths to Android friendly Parcelable Uri's
												    System.out.println("dir: " + OMCRoot.getAbsolutePath());
												    System.out.println("name: " + restoreName+".omc");
												    
												    File inzip = new File(OMCRoot.getAbsolutePath(),restoreName+".omc");
												    if (!inzip.exists()) return "";

												    Intent it = new Intent(OMC.CONTEXT, OMCThemeUnzipActivity.class);
												    it.setData(Uri.fromFile(inzip));

												    startActivity(OMC.GETSTARTERPACKINTENT);
											        	
												}
											return "";

										} catch (Exception e) {
											e.printStackTrace();
										}
										return "";
									}
									@Override
									protected void onPostExecute(String result) {
										Toast.makeText(OMCPrefActivity.this, OMC.RString("taskComplete"), Toast.LENGTH_SHORT).show();
									}
								}.execute(restoreOptions[0],restoreOptions[1]);
								
							}
						})
						.create().show();
					
					return false;
				}
			});

        	// "App UI Language".
        	Preference prefLocale = findPreference("appLocale"); 
        	prefLocale.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(final Preference preference) {
					final CharSequence[] items = OMC.LOCALENAMES;
					new AlertDialog.Builder(OMCPrefActivity.this)
						.setTitle(OMC.RString("changeAppLocale"))
						.setItems(items, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int item) {
									Locale selectedLocale = OMC.LOCALES[item];
									OMC.PREFS.edit()
											.putString("appLocaleName", OMC.LOCALENAMES[item])
											.commit();

									// Determine locale.
									Configuration config = new Configuration();
									config.locale=selectedLocale;
									OMC.RES.updateConfiguration(config, 
											OMC.RES.getDisplayMetrics());

									preference.setSummary(items[item]);
									OMCPrefActivity.this.finish();
								}
						})
						.show();					
					return true;
				}
			});
        	prefLocale.setSummary(OMC.PREFS.getString("appLocaleName", "English (US)"));
        	
        	// "Clock Language".
        	Preference prefClockLocale = findPreference("clockLocale"); 
        	prefClockLocale.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(final Preference preference) {
					final CharSequence[] items = OMC.LOCALENAMES;
					new AlertDialog.Builder(OMCPrefActivity.this)
						.setTitle(OMC.RString("changeClockLocale"))
						.setItems(items, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int item) {
									Locale selectedLocale = OMC.LOCALES[item];
									OMC.PREFS.edit()
											.putString("clockLocaleName", OMC.LOCALENAMES[item])
											.commit();

									OMC.WORDNUMBERS = OMC.RStringArray("WordNumbers", selectedLocale);
									OMC.VERBOSETIME1 = OMC.RStringArray("verbosetime1", selectedLocale);
									OMC.VERBOSETIME2 = OMC.RStringArray("verbosetime2", selectedLocale);
									OMC.VERBOSETIME3 = OMC.RStringArray("verbosetime3", selectedLocale);
									OMC.VERBOSETIME4 = OMC.RStringArray("verbosetime4", selectedLocale);
									OMC.VERBOSEWEATHER = OMC.RStringArray("VerboseWeather", selectedLocale);
									OMC.VERBOSENUMBERS = OMC.RStringArray("WordNumbers", selectedLocale);
									OMC.VERBOSEDOW = OMC.RStringArray("verbosedow", selectedLocale);
									OMC.SHORTDOW = OMC.RStringArray("shortdow", selectedLocale);
									OMC.VERBOSEMONTH = OMC.RStringArray("verbosemonth", selectedLocale);
									OMC.SHORTMONTH = OMC.RStringArray("shortmonth", selectedLocale);
									OMC.DAYSUFFIX = OMC.RString("daysuffix", selectedLocale);

									preference.setSummary(items[item]);

									OMCPrefActivity.this.finish();
								}
						})
						.show();					
					return true;
				}
			});
        	prefClockLocale.setSummary(OMC.PREFS.getString("clockLocaleName", "English (US)"));

        	// "Update Weather Now".
        	prefUpdWeatherNow = findPreference("updweathernow");
        	
        	// "Location: [Location] (status)".
        	prefWeather = findPreference("weather");
    		try {
        		String sWSetting = OMC.PREFS.getString("weathersetting", "bylatlong");
    			JSONObject jsonWeather = new JSONObject(OMC.PREFS.getString("weather", "{}"));
    			String sCity = jsonWeather.optString("city","Unknown");
        		if (sWSetting.equals("bylatlong")) {
        			prefWeather.setTitle(OMC.RString("location") + sCity + OMC.RString("detected"));
        		} else if (sWSetting.equals("specific")) {
        			prefWeather.setTitle("Location: "+OMC.jsonFIXEDLOCN.optString("city","Unknown")+OMC.RString("fixed"));
        		} else {
        			prefWeather.setTitle(OMC.RString("weatherFunctionalityDisabled"));
        		} 
    		} catch (JSONException e) {
    			e.printStackTrace();
    			prefWeather.setTitle(OMC.RString("weatherFunctionalityDisabled"));
    		}
        	prefWeather.setSummary(formatWeatherSummary());

        	prefUpdWeatherNow.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					prefUpdWeatherNow.setEnabled(false);
			    	getNewWeatherRefreshTask();

		    		mWeatherRefresh.execute("");
					return true;
				}
			});
        	

        	// "Weather Provider".
        	prefWeatherProvider = findPreference("weatherProvider");
        	prefWeatherProvider.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if (newValue.equals("auto")) {
//   				 	v1.4.1:  Auto weather provider.
//						Rules:
//							Default to 7Timer + METAR.
//							if location is in US, switch to NOAA + METAR.
//								If no nearby airport, use 7Timer + no METAR.
//							If 7Timer times out, switch to yr.no + METAR.
//								If no nearby airport, use yr.no + no METAR.
//						    If no nearby airport, use 7Timer + no METAR.
							
						OMC.PREFS.edit().putString("activeWeatherProvider", "seventimer")
								.putBoolean("weatherMETAR", true)
								.commit();
						findPreference("weatherMETAR").setEnabled(false);
						preference.setSummary(OMC.RString("wp"+newValue));
						return true;
					} else {
						OMC.PREFS.edit().putString("activeWeatherProvider", (String)newValue).commit();
						findPreference("weatherMETAR").setEnabled(true);
						preference.setSummary(OMC.RString("wp"+newValue));
						return true;
					}
				}
			});
        	String sWProvider = OMC.PREFS.getString("weatherProvider", "auto");
			prefWeatherProvider.setSummary(OMC.RString("wp"+sWProvider));
			if (OMC.PREFS.getString("weatherProvider", "auto").equals("auto")) {
				findPreference("weatherMETAR").setEnabled(false);
			}
        	
        	// "Weather via METAR".
        	findPreference("weatherMETAR").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
		    		if (newValue.equals(true)) {
		        		preference.setSummary(OMC.RString("usingMETAR"));
		    		} else {
		        		preference.setSummary(OMC.RString("usingInterpolation"));
		    		}
			    	return true;
				}
			});
        	if (OMC.PREFS.getBoolean("weatherMETAR",true)==true)
        		findPreference("weatherMETAR").setSummary(OMC.RString("usingMETAR"));
        	else findPreference("weatherMETAR").setSummary(OMC.RString("usingInterpolation"));

        	// "Weather Display Units".
        	prefWeatherDisplay = findPreference("weatherDisplay");
        	prefWeatherDisplay.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
		    		if (newValue.equals("c")) {
		        		preference.setSummary(OMC.RString("usingCelsius"));
		    		} else {
		        		preference.setSummary(OMC.RString("usingFahrenheit"));
		    		}
			    	return true;
				}
			});
        	if (OMC.PREFS.getString("weatherDisplay", "f").equals("c"))
        		prefWeatherDisplay.setSummary(OMC.RString("usingCelsius"));
        	else prefWeatherDisplay.setSummary(OMC.RString("usingFahrenheit"));

        	prefsUpdateFreq = (ListPreference)findPreference("sUpdateFreq");

        	// "Clock Update Interval"
        	// Allow one-sec updates only on paid edition
        	int size = OMC.FREEEDITION? OMC.RStringArray("interval_values").length-1: OMC.RStringArray("interval_values").length;
        	String[] options= new String[size], values= new String[size];
           	for (int i = 1; i<= size; i++) {
        		options[size-i] = OMC.RStringArray("interval_options")[OMC.RStringArray("interval_options").length-i];
        		values[size-i] = OMC.RStringArray("interval_values")[OMC.RStringArray("interval_values").length-i];
        	}
        	prefsUpdateFreq.setEntries(options);
        	prefsUpdateFreq.setEntryValues(values);
        	prefsUpdateFreq.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
		    		preference.setSummary(OMC.RString("redrawEvery") + (String)newValue + OMC.RString("seconds"));
		    		getApplicationContext().sendBroadcast(OMC.WIDGETREFRESHINTENT);
					return true;
				}
			});
        	prefsUpdateFreq.setSummary(OMC.RString("redrawEvery") + OMC.PREFS.getString("sUpdateFreq", "30") + OMC.RString("seconds"));

        	// "Weather Update Interval"
        	findPreference("sWeatherFreq").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
		    		int newHrs = Integer.parseInt((String)newValue)/60;
		    		long newMillis = newHrs * 3600000l; 
		        	switch (newHrs) {
		    		case 0:
		    			findPreference("sWeatherFreq").setSummary(OMC.RString("manualWeatherUpdatesOnly"));
		    			OMC.NEXTWEATHERREFRESH = Long.MAX_VALUE;
		    			break;
		    		case 1:
		    			findPreference("sWeatherFreq").setSummary(OMC.RString("refreshWeatherEveryHour"));
		            	// if the last try was unsuccessful, reset the next weather
		            	if (OMC.LASTWEATHERTRY > OMC.LASTWEATHERREFRESH) 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERTRY + newMillis/4l;
		            	else 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERREFRESH + newMillis;
		    			break;
		    		case 4:
		    			findPreference("sWeatherFreq").setSummary(OMC.RString("refreshWeatherEvery4Hours"));
		            	// if the last try was unsuccessful, reset the next weather
		            	if (OMC.LASTWEATHERTRY > OMC.LASTWEATHERREFRESH) 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERTRY + newMillis/4l;
		            	else 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERREFRESH + newMillis;
		    			break;
		    		case 8:
		    			findPreference("sWeatherFreq").setSummary(OMC.RString("refreshWeatherEvery8Hours"));
		            	// if the last try was unsuccessful, reset the next weather
		            	if (OMC.LASTWEATHERTRY > OMC.LASTWEATHERREFRESH) 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERTRY + newMillis/4l;
		            	else 
		            		OMC.NEXTWEATHERREFRESH = OMC.LASTWEATHERREFRESH + newMillis;
		    			break;
		    		default:
		    			findPreference("sWeatherFreq").setSummary(OMC.RString("refreshWeatherEveryHour"));
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
        			findPreference("sWeatherFreq").setSummary(OMC.RString("manualWeatherUpdatesOnly"));
        			break;
        		case 1:
        			findPreference("sWeatherFreq").setSummary(OMC.RString("refreshWeatherEveryHour"));
        			break;
        		case 4:
        			findPreference("sWeatherFreq").setSummary(OMC.RString("refreshWeatherEvery4Hours"));
        			break;
        		case 8:
        			findPreference("sWeatherFreq").setSummary(OMC.RString("refreshWeatherEvery8Hours"));
        			break;
        		default:
        			findPreference("sWeatherFreq").setSummary(OMC.RString("refreshWeatherEveryHour"));
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

        	// "Translator".
        	findPreference("sTranslator").setTitle(OMC.RString("translator"));
        	findPreference("sTranslator").setSummary(OMC.RString("languageWord") + ": " + OMC.RString("languageName"));
        	
        	// "Clock Priority".
        	Preference prefClockPriority = findPreference("clockPriority");
        	prefClockPriority.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(final Preference preference, final Object newValue) {
					final int iPriority = Integer.parseInt((String)newValue);
					String sExplanation = OMC.RStringArray("clockPriority_legend")[iPriority];
					new AlertDialog.Builder(OMCPrefActivity.this)
						.setTitle(OMC.RString("clockPriority"))
						.setMessage(sExplanation)
						.setPositiveButton(OMC.RString("_yes"), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int item) {
								OMC.PREFS.edit().putString("clockPriority", (String)newValue).commit();
					        	preference.setSummary(OMC.RStringArray("clockPriority_options")[Integer.parseInt(OMC.PREFS.getString("clockPriority", "3"))]);
					        	OMC.CURRENTCLOCKPRIORITY = Integer.parseInt(OMC.PREFS.getString("clockPriority", "3"));
							}
						})
						.setNegativeButton(OMC.RString("_no"), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int item) {
							}
						})
						.show();					
					return false;
				}
			});
        	prefClockPriority.setSummary(OMC.RStringArray("clockPriority_options")
        			[Integer.parseInt(OMC.PREFS.getString("clockPriority", "3"))]);
        	
        	// "Location Priority".
        	Preference prefLocnPriority = findPreference("locationPriority");
        	prefLocnPriority.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(final Preference preference, final Object newValue) {
					final int iPriority = Integer.parseInt((String)newValue);
					String sExplanation = OMC.RStringArray("locationPriority_legend")[iPriority];
					new AlertDialog.Builder(OMCPrefActivity.this)
						.setTitle(OMC.RString("locationPriority"))
						.setMessage(sExplanation)
						.setPositiveButton(OMC.RString("_yes"), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int item) {
								OMC.PREFS.edit().putString("locationPriority", (String)newValue).commit();
					        	preference.setSummary(OMC.RStringArray("locationPriority_options")[Integer.parseInt(OMC.PREFS.getString("locationPriority", "4"))]);
							}
						})
						.setNegativeButton(OMC.RString("_no"), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int item) {
							}
						})
						.show();					
					return false;
				}
			});
        	prefLocnPriority.setSummary(OMC.RStringArray("locationPriority_options")
        			[Integer.parseInt(OMC.PREFS.getString("locationPriority", "4"))]);
        	
        	// "Battery Reporting".
        	Preference prefBattReporting = findPreference("battReporting");
        	prefBattReporting.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
		    		if (newValue.equals(true)) {
		        		preference.setSummary(OMC.RString("battReportingNormal"));
		    		} else {
		        		preference.setSummary(OMC.RString("battReportingAlternate"));
		    		}
			    	return true;
				}
			});
        	if (OMC.PREFS.getBoolean("battReporting",true)==true)
        		findPreference("battReporting").setSummary(OMC.RString("battReportingNormal"));
        	else findPreference("battReporting").setSummary(OMC.RString("battReportingAlternate"));

        	// "Weather Diagnostics".
        	Preference prefWeatherDiag = findPreference("weatherDebug");
        	prefWeatherDiag.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					(new Thread() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							try {
								// Build weather debug data.
								String sBody = "";
								Time tNow = new Time();
								tNow.setToNow();
								Time tLock = new Time();
								tLock.set(OMC.LASTKNOWNLOCN.getTime());
								sBody+="Debug Report Timestamp: " + tNow.format3339(false) + "\n\n";
								sBody+="Location:\n";
								sBody+="Provider: " + OMC.LASTKNOWNLOCN.getProvider()+ "\n";
								sBody+="Age: " + tLock.format3339(false) + "\n";
								sBody+="Lat: " + OMC.LASTKNOWNLOCN.getLatitude()+ "\n";
								sBody+="Lon: " + OMC.LASTKNOWNLOCN.getLongitude()+ "\n";
								sBody+="Reverse Geocode:\n";
								sBody+=GoogleReverseGeocodeService.updateLocation(OMC.LASTKNOWNLOCN)+"\n";
								sBody+="WeatherProvider: " + OMC.PREFS.getString("weatherProvider", "NONE")+ "\n";
								sBody+="activeWeatherProvider: " + OMC.PREFS.getString("activeWeatherProvider", "NONE")+ "\n";
								sBody+="Weather:\n";
								sBody+=OMC.PREFS.getString("weather", "Weather JSON Missing!")+"\n";
								Intent it = new Intent(android.content.Intent.ACTION_SEND)
					   					.setType("plain/text")
					   					.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"skwong@consultant.com"})
					   					.putExtra(android.content.Intent.EXTRA_SUBJECT, OMC.APPNAME + " WeatherDebug v" + OMC.THISVERSION)
										.putExtra(android.content.Intent.EXTRA_TEXT, sBody);
								startActivity(Intent.createChooser(it, OMC.RString("contactXaffronForIssues")));  
								finish();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}).start();
					return true;
				}
			});
        	
        	// "Contact Xaffron".
        	prefemailMe = findPreference("emailMe");

        	// Version text.
        	if (OMC.FREEEDITION) {
        		findPreference("sVersion").setTitle(OMC.RString("version")+ " " + OMC.THISVERSION + " Free");
        		findPreference("sVersion").setSummary(OMC.RString("tapToGetFull"));
        		findPreference("sVersion").setSelectable(true);
        	} else {
        		findPreference("sVersion").setTitle(OMC.RString("version")+ " " + OMC.THISVERSION);
        		findPreference("sVersion").setSummary(OMC.RString("thanksForYourSupport"));
        		findPreference("sVersion").setSelectable(false);
        	}
        	
        	findPreference("releaseNotes").setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					// TODO Auto-generated method stub
	    			WebView wv = new WebView(OMCPrefActivity.this);
	    			wv.loadUrl(OMC.OMCCHANGESURL);
	    			AlertDialog d = new AlertDialog.Builder(OMCPrefActivity.this)
	    								.setView(wv)
	    								.setPositiveButton(OMC.RString("_ok"), new DialogInterface.OnClickListener() {
	    									@Override
	    									public void onClick(
	    											DialogInterface dialog,
	    											int which) {
		    										// TODO Auto-generated method stub
	    										OMC.PREFS.edit().putBoolean("showhelp", false).commit(); 
	    										OMC.SHOWHELP=false;
	    									}
										}).create();
	    			d.show();
					return false;
				}
			});

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
    		try {
    			OMC.PKM.getApplicationInfo(sOtherEd, PackageManager.GET_META_DATA);
    			if (!OMC.FREEEDITION) {
	            	mAD = new AlertDialog.Builder(this)
	        		.setTitle(OMC.RString("warningConflictFree"))
	        		.setMessage(OMC.RString("warningConflictText"))
	        	    .setCancelable(true)
	        	    .setIcon(OMC.RDrawableId(OMC.APPICON))
	        	    .setOnKeyListener(new OnKeyListener() {
	        	    	@Override
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
    			} else {
	            	mAD = new AlertDialog.Builder(this)
	        		.setTitle(OMC.RString("warningConflictPaid"))
	        		.setMessage(OMC.RString("warningConflictText"))
	        	    .setCancelable(true)
	        	    .setIcon(OMC.RDrawableId(OMC.APPICON))
	        	    .setOnKeyListener(new OnKeyListener() {
	        	    	@Override
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
    			}
    		} catch (NameNotFoundException e) {
    			// If we can't find the conflicting package, we're all good - no need to show warning
    		}
    		
    		// This is the help/FAQ dialog.
    		
    		if (OMC.SHOWHELP) {
    			WebView wv = new WebView(this);
    			wv.loadUrl(OMC.OMCCHANGESURL);
    			AlertDialog d = new AlertDialog.Builder(this)
    								.setView(wv)
    								.setPositiveButton(OMC.RString("_ok"), new DialogInterface.OnClickListener() {
    									@Override
    									public void onClick(
    											DialogInterface dialog,
    											int which) {
	    										// TODO Auto-generated method stub
    										OMC.PREFS.edit().putBoolean("showhelp", false).commit(); 
    										OMC.SHOWHELP=false;
    									}
									}).create();
    			d.show();
//    			OMC.FAQS = OMC.RStringArray("faqs");
//				LayoutInflater li = LayoutInflater.from(this);
//				LinearLayout ll = (LinearLayout)(li.inflate(OMC.RLayoutId("faqdialog"), null));
//				mTextView = (TextView)ll.findViewById(OMC.RId("splashtext"));
//				mTextView.setAutoLinkMask(Linkify.ALL);
//				mTextView.setMinLines(8);
//				mTextView.setText(OMC.FAQS[OMC.faqtoshow++]);
//				OMC.faqtoshow = OMC.faqtoshow==OMC.FAQS.length?0:OMC.faqtoshow;
//				
//				mCheckBox = (CheckBox)ll.findViewById(OMC.RId("splashcheck"));
//				mCheckBox.setChecked(!OMC.SHOWHELP);
//				mCheckBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
//					
//					@Override
//					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//						// TODO Auto-generated method stub
//						OMC.SHOWHELP = !isChecked;
//					}
//				});
//	
//				((Button)ll.findViewById(OMC.RId("faqOK"))).setOnClickListener(new Button.OnClickListener() {
//					
//					@Override
//					public void onClick(android.view.View v) {
//						OMC.PREFS.edit().putBoolean("showhelp", OMC.SHOWHELP).commit();
//						mAD.dismiss();
//					}
//				});
//				((Button)ll.findViewById(OMC.RId("faqNeutral"))).setOnClickListener(new Button.OnClickListener() {
//					
//					@Override
//					public void onClick(android.view.View v) {
//						mTextView.setText(OMC.FAQS[OMC.faqtoshow++]);
//						mTextView.invalidate();
//						OMC.faqtoshow = OMC.faqtoshow==OMC.FAQS.length?0:OMC.faqtoshow;
//					}
//				});;
//				
//				mAD = new AlertDialog.Builder(this)
//				.setTitle(OMC.RString("usefulTip"))
//			    .setCancelable(true)
//			    .setView(ll)
//			    .setOnKeyListener(new OnKeyListener() {
//			    	@Override
//					public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
//			    		if (arg2.getKeyCode()==android.view.KeyEvent.KEYCODE_BACK) mAD.cancel();
//			    		return true;
//			    	};
//			    })
//			    .show();
    		}

		} else {
            // If they gave us an intent without the widget id, just bail.
        	if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Pref","Called by Launcher - do nothing");
        	OMCPrefActivity.mAD = new AlertDialog.Builder(this)
        		.setTitle(OMC.RString("thanksForDownloading"))
        		.setMessage(OMC.RString("widgetDir1") + OMC.APPNAME + OMC.RString("widgetDir2"))
        	    .setCancelable(true)
        	    .setIcon(OMC.RDrawableId(OMC.APPICON))
        	    .setOnKeyListener(new OnKeyListener() {
        	    	@Override
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
			final CharSequence[] items = {OMC.RString("_yesDelete"), OMC.RString("_yesRestore"), OMC.RString("_no")};
			new AlertDialog.Builder(this)
				.setTitle(OMC.RString("deleteAllThemesFromSD"))
				.setItems(items, new DialogInterface.OnClickListener() {
						@Override
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
						    		Toast.makeText(OMCPrefActivity.this, OMC.RString("omcThemesFolderDeleted"), Toast.LENGTH_SHORT).show();
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
						    		Toast.makeText(OMCPrefActivity.this, OMC.RString("defaultClockPackRestored"), Toast.LENGTH_SHORT).show();
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
			final CharSequence[] items = {OMC.RString("disableLocationAndWeather"), OMC.RString("followDevice"), OMC.RString("setFixedLocation")};
			new AlertDialog.Builder(this)
				.setTitle(OMC.RString("weatherLocation"))
				.setItems(items, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int item) {

							switch (item) {
								case 0: //Disabled (default)
									OMC.PREFS.edit().putString("weathersetting", "disabled").commit();
				        			prefWeather.setTitle(OMC.RString("weatherFunctionalityDisabled"));
				        			prefWeather.setSummary(formatWeatherSummary());
									break;
								case 1: //Follow Device
									OMC.PREFS.edit().putString("weathersetting", "bylatlong").commit();
									getNewWeatherRefreshTask();
									mWeatherRefresh.execute("");
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
			final CharSequence[] items = {OMC.RString("email"), OMC.RString("donate"), OMC.RString("facebook")};
			new AlertDialog.Builder(this)
				.setTitle(OMC.RString("contactXaffron"))
				.setItems(items, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int item) {
							switch (item) {
								case 0: //Email
									Intent it = new Intent(android.content.Intent.ACTION_SEND)
		    		   					.setType("plain/text")
		    		   					.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"skwong@consultant.com"})
		    		   					.putExtra(android.content.Intent.EXTRA_SUBJECT, OMC.APPNAME + " Feedback v" + OMC.THISVERSION);
					    		   	startActivity(Intent.createChooser(it, OMC.RString("contactXaffronForIssues"))); 
					    		   	
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
    	//SUNNY SENDBATTDEBUG
//    	if (preference == findPreference("sendBatteryDebug")) {
//    		JSONArray ja = new JSONArray();
//    		for (int i:OMC.BATTVOLTAGESCALE) {
//    			ja.put(i);
//    		}
//    		try {
//				// Build weather debug data.
//				String sBody = "";
//				sBody+="battery driver info:\n";
//				File dir = new File("/sys/class/power_supply/battery/");
//				for (File f: dir.listFiles()) {
//					sBody+="File:" + f.getName() + "\n";
//					if (f.getName().equals("capacity")||f.getName().equals("charge_counter")||f.getName().equals("uevent")) {
//						try {
//							FileInputStream fis = new FileInputStream(f);
//							sBody+="Content:" + OMC.streamToString(fis);
//							fis.close();
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//					}
//				}
//				sBody+="\nbattery calibration data:\n" + ja.toString(3);
//				Intent it = new Intent(android.content.Intent.ACTION_SEND)
//	   					.setType("plain/text")
//	   					.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"skwong@consultant.com"})
//	   					.putExtra(android.content.Intent.EXTRA_SUBJECT, OMC.APPNAME + " BattDebug v" + OMC.THISVERSION)
//						.putExtra(android.content.Intent.EXTRA_TEXT, sBody);
//				startActivity(Intent.createChooser(it, OMC.RString("contactXaffronForIssues")));  
//				finish();
//    		} catch (JSONException e) {
//    			e.printStackTrace();
//    		}
//    	}
//    	//SUNNY SENDBATTDEBUG
    	if (preference == getPreferenceScreen().findPreference("widgetPrefs") && OMC.FREEEDITION) {
    		final CharSequence TitleCS = OMC.RString("areThereOtherWidgetSizes");
    		final CharSequence MessageCS = OMC.RString("actuallyThePaidVersion");
    		final CharSequence PosButtonCS = OMC.RString("takeMeToPaid");
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
    	if (preference == getPreferenceScreen().findPreference("testerCredits")) {
    		Intent it = new Intent(this, CreditRollActivity.class);
    		it.putExtra("type", 1);
    		startActivityForResult(it, 0);
    	}
    	if (preference == getPreferenceScreen().findPreference("translatorCredits")) {
    		Intent it = new Intent(this, CreditRollActivity.class);
    		it.putExtra("type", 2);
    		startActivityForResult(it, 0);
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
    			.setTitle(OMC.RString("thinkOfThePossibilities"))
    			.setMessage(OMC.RString("prePaidTTL") + OMC.APPNAME + OMC.RString("postPaidTTL"))
    			.setPositiveButton(OMC.RString("takeMeToPaid"), new DialogInterface.OnClickListener() {
    					
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
    			final CharSequence[] items = {
    					OMC.RString("openOptionsDefault"), 
    					OMC.RString("doNothing"), 
    					OMC.RString("refreshWeather"), 
    					OMC.RString("weatherForecast"), 
    					OMC.RString("viewAlarms"),
    					OMC.RString("battUsage"),
    					OMC.RString("otherActivity")};
    			final String[] values = {
    					"default", 
    					"noop", 
    					"wrefresh",
    					"weather", 
    					"alarms", 
    					"batt",
    					"activity"};
				
				final AlertDialog dlgTTL  =  new AlertDialog.Builder(this)
				.setTitle(OMC.RString("chooseAction"))
				.setItems(items, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int item) {
							if (values[item].equals("default")) {
								OMC.PREFS.edit().putString("URI"+OMC.COMPASSPOINTS[iTTLArea], "")
									.putString("URIDesc"+OMC.COMPASSPOINTS[iTTLArea], OMC.RString("widgetPrefsTTL")).commit();
							}
							if (values[item].equals("noop")) {
								OMC.PREFS.edit().putString("URI"+OMC.COMPASSPOINTS[iTTLArea], "noop")
									.putString("URIDesc"+OMC.COMPASSPOINTS[iTTLArea], OMC.RString("doNothingTTL")).commit();
							}
							if (values[item].equals("wrefresh")) {
								OMC.PREFS.edit().putString("URI"+OMC.COMPASSPOINTS[iTTLArea], "wrefresh")
									.putString("URIDesc"+OMC.COMPASSPOINTS[iTTLArea], OMC.RString("refreshWeatherTTL")).commit();
							}
							if (values[item].equals("weather")) {
								OMC.PREFS.edit().putString("URI"+OMC.COMPASSPOINTS[iTTLArea], "weather")
									.putString("URIDesc"+OMC.COMPASSPOINTS[iTTLArea], OMC.RString("weatherForecastTTL")).commit();
							}
							if (values[item].equals("alarms")) {
								OMC.PREFS.edit().putString("URI"+OMC.COMPASSPOINTS[iTTLArea], "alarms")
									.putString("URIDesc"+OMC.COMPASSPOINTS[iTTLArea], OMC.RString("viewAlarmsTTL")).commit();
							}
							if (values[item].equals("batt")) {
								OMC.PREFS.edit().putString("URI"+OMC.COMPASSPOINTS[iTTLArea], "batt")
									.putString("URIDesc"+OMC.COMPASSPOINTS[iTTLArea], OMC.RString("battUsageTTL")).commit();
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
								btnCompass[iCompass].setText(OMC.PREFS.getString("URIDesc"+OMC.COMPASSPOINTS[iCompass],OMC.RString("widgetPrefsTTL")));
							}
						}
				}).create();

				LayoutInflater li = LayoutInflater.from(this);
				LinearLayout ll = (LinearLayout)(li.inflate(OMC.RLayoutId("ttlpreview"), null));

				for (int iCompass = 0; iCompass < 9; iCompass++) {
					btnCompass[iCompass] = (Button)ll.findViewById(OMC.RId("button" + OMC.COMPASSPOINTS[iCompass] + "Prv"));
					btnCompass[iCompass].setText(OMC.PREFS.getString("URIDesc"+OMC.COMPASSPOINTS[iCompass],OMC.RString("widgetPrefsTTL")));
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
    					.setTitle(OMC.RString("areaToCustomize"))
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
    		Toast.makeText(this, OMC.RString("cachesCleared"), Toast.LENGTH_SHORT).show();
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
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (OMC.WEATHERREFRESHSTATUS!=OMC.WRS_IDLE) {
			getNewWeatherRefreshTask();
			mWeatherRefresh.execute("");
		}
		
    	prefloadThemeFile.setSummary(OMC.RString("preselected")  +OMC.PREFS.getString("widgetThemeLong", OMC.DEFAULTTHEMELONG)+ OMC.RString("postselected"));
		if (OMC.PREFS.getString("sTimeZone", "default").equals("default")) {
			getPreferenceScreen().findPreference("timeZone").setSummary(OMC.RString("followingDeviceTimeZone"));
		} else {
			getPreferenceScreen().findPreference("timeZone").setSummary(OMC.PREFS.getString("sTimeZone", "default"));
		}
		getPreferenceScreen().setEnabled(true);

		// If it's an independent child activity, do nothing
		if (requestCode == 0) return;
		if (data != null) {
			String s = data.toUri(MODE_PRIVATE).toString();
			
			OMC.PREFS.edit().putString("URI"+OMC.COMPASSPOINTS[requestCode], s)
				.putString("URIDesc"+OMC.COMPASSPOINTS[requestCode], OMC.RString("otherActivityTTL")).commit();
		}
	}
    
	public void cancelTTL() {
       	if (mTTL!=null) { // && mAD.isShowing()
       		mTTL.dismiss();
       		mTTL = null;
       	}
	}
	
	public void getNewWeatherRefreshTask() {
		boolean bNeedNewTask=false;
		// If we've never requested a task before, we need a new one
		if (mWeatherRefresh==null) {
			bNeedNewTask=true;
		// If there's one that is running, abort it and create a new one
		} else if (mWeatherRefresh.getStatus()!=AsyncTask.Status.PENDING && !mWeatherRefresh.isCancelled()) {
			mWeatherRefresh.cancel(true);
			bNeedNewTask=true;
		}
		
    	if (bNeedNewTask) {
    		mWeatherRefresh = new AsyncTask<String, String, String>() {
    		int currentStatus = OMC.WRS_IDLE;
    		boolean bAutoLocation = true;

    		@Override
    		protected void onPreExecute() {
    			if (OMC.PREFS.getString("weathersetting", "bylatlong").equals("disabled")){
    				OMC.WEATHERREFRESHSTATUS=OMC.WRS_DISABLED;
    				return;
    			}
    			bAutoLocation=OMC.PREFS.getString("weathersetting", "bylatlong").equals("bylatlong")?true:false;
    			if (bAutoLocation)
    				OMC.WEATHERREFRESHSTATUS = OMC.WRS_LOCATION;
    			else
    				OMC.WEATHERREFRESHSTATUS = OMC.WRS_FIXED;
    			OMC.updateWeather();
    		}
    		@Override
    		protected String doInBackground(String... params) {
    			String sAutoLocation = bAutoLocation?OMC.RString("detected"):OMC.RString("fixed");
    			try {
    				while (!isCancelled()) {
    					Thread.sleep(200l);

    					if (currentStatus==OMC.WEATHERREFRESHSTATUS) {
    						if (System.currentTimeMillis()-OMC.WEATHERREFRESHTIMESTAMP>30000l) {
    							publishProgress(OMC.RString("refreshWeatherNow"),OMC.RString("operationTimedOut"),
        								OMC.RString("location") + OMC.RString("unknown"));
    							break;
    						} else {
    							continue;
    						}
    					}
    					currentStatus = OMC.WEATHERREFRESHSTATUS;
    					OMC.WEATHERREFRESHTIMESTAMP=System.currentTimeMillis();

    					switch (OMC.WEATHERREFRESHSTATUS) {
    					case OMC.WRS_LOCATION:
    						publishProgress(OMC.RString("requestingLocation"),OMC.RString("pleaseWait"),
    								OMC.RString("location") + OMC.RString("unknown"));
    						break;
    					case OMC.WRS_FIXED:
    						publishProgress(OMC.RString("inProgress"),
    								OMC.RString("usingFixedCoordinates"),
    								OMC.RString("location") + OMC.jsonFIXEDLOCN.optString("city","Unknown") + sAutoLocation
    								);
    						break;
    					case OMC.WRS_GPS:
    						publishProgress(OMC.RString("inProgress"),OMC.RString("gotGPSLock"),
    								OMC.RString("location") + OMC.LASTKNOWNCITY+ sAutoLocation);
    						break;
    					case OMC.WRS_NETWORK:
    						publishProgress(OMC.RString("inProgress"),OMC.RString("gotNetworkLock"),
    								OMC.RString("location") +OMC.LASTKNOWNCITY+ sAutoLocation);
    						break;
    					case OMC.WRS_CACHED:
    						publishProgress(OMC.RString("inProgress"),OMC.RString("reusingCachedLocation"),
    								OMC.RString("location") +OMC.LASTKNOWNCITY+ sAutoLocation);
    						break;
    					case OMC.WRS_GEOCODE:
    						publishProgress(OMC.RString("inProgress"),OMC.RString("requestingWeather"),
    								OMC.RString("location") + OMC.LASTKNOWNCITY + sAutoLocation,
    								OMC.RString("location") +OMC.LASTKNOWNCITY+ sAutoLocation);
    						break;
    					case OMC.WRS_PROVIDER:
    						publishProgress(OMC.RString("inProgress"),OMC.RString("weatherReceived"),
    								OMC.RString("location") +OMC.LASTKNOWNCITY+ sAutoLocation);
    						break;
    					case OMC.WRS_SUCCESS:
    						publishProgress(OMC.RString("refreshWeatherNow"),OMC.RString("weatherSuccess"),
    								OMC.RString("location") +OMC.LASTKNOWNCITY+ sAutoLocation);
    						return("SUCCESS");
    					case OMC.WRS_FAILURE:
    						publishProgress(OMC.RString("refreshWeatherNow"),OMC.RString("weatherFailure"),
    								OMC.RString("location") +OMC.LASTKNOWNCITY+ sAutoLocation);
    						return("ERROR");
    					case OMC.WRS_DISABLED:
    						publishProgress(OMC.RString("refreshWeatherNow"),OMC.RString("weatherFunctionalityDisabled"),
    								OMC.RString("weatherFunctionalityDisabled"));
    						return("ERROR");
    					case OMC.WRS_IDLE:
    					default:
    						return("ERROR");
    					}
    				}
    				OMC.abortWeatherUpdate();
					prefUpdWeatherNow.setEnabled(true);

    			} catch (InterruptedException e) {
    				e.printStackTrace();
    			}
    			return null;
    			
    		}
    		@Override
    		protected void onProgressUpdate(String... values) {
    			prefUpdWeatherNow.setTitle(values[0]);
    			prefUpdWeatherNow.setSummary(values[1]);
    			prefWeather.setTitle(values[2]);
    			prefWeather.setSummary(formatWeatherSummary());
    		}
    		@Override
    		protected void onPostExecute(String result) {
    			OMC.WEATHERREFRESHSTATUS=OMC.WRS_IDLE;
				prefUpdWeatherNow.setEnabled(true);
    		};
    	};
    	}
	
	}

	
    public String formatWeatherSummary() {
		final String sWSetting = OMC.PREFS.getString("weathersetting", "bylatlong");
		tLastTry.set(OMC.LASTWEATHERTRY);
		tLastRefresh.set(OMC.LASTWEATHERREFRESH);
		tNextRefresh.set(OMC.NEXTWEATHERREFRESH);
		tNextRequest.set(OMC.NEXTWEATHERREQUEST);
		if (sWSetting.equals("disabled")) {
			return (OMC.RString("tapToEnable"));
		}
		if (OMC.PREFS.getString("sWeatherFreq", "60").equals("0")) {
			return (OMC.RString("lastTry")+tLastTry.format("%R") + OMC.RString("manualRefreshOnly"));
		} else {
			return (OMC.RString("lastTry")+tLastTry.format("%R") + OMC.RString("nextRefresh")+tNextRefresh.format("%R") 
						+ "\n" + OMC.RString("lastRefresh")+ tLastRefresh.format("%R") + OMC.RString("nextRequest") + tNextRequest.format("%R"));
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
    	
    	// Shut down any in-progress weather updates
    	if (mWeatherRefresh!=null) {
    		if (!mWeatherRefresh.isCancelled()) mWeatherRefresh.cancel(true);
    	}
    	OMC.abortWeatherUpdate();
    	OMC.WEATHERREFRESHSTATUS=OMC.WRS_IDLE;
    	
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