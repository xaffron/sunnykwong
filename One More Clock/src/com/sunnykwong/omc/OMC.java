package com.sunnykwong.omc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

/**
 * @author skwong01
 * Thanks to ralfoide's 24clock code; taught me quite a bit about broadcastreceivers
 * Thanks to the Open Font Library for a great resource.
 * 
 */ 
public class OMC extends Application {
	
	
	static final boolean DEBUG = false;
	static final boolean THEMESFROMCACHE = true;
	
	static String THISVERSION; 
	static final boolean SINGLETON = false;

	static final boolean FREEEDITION = false;
	static final String SINGLETONNAME = "One More Clock";
	static final String STARTERPACKURL = "asset:pk126.omc";
	static final String EXTENDEDPACK = "https://sites.google.com/a/xaffron.com/xaffron-software/OMCThemes_v125.omc";
	static final String EXTENDEDPACKBACKUP = "https://s3.amazonaws.com/Xaffron/OMCThemes_v125.omc";
	static final String DEFAULTTHEME = "HoneycombLook";
	static final Intent FGINTENT = new Intent("com.sunnykwong.omc.FGSERVICE");
	static final Intent BGINTENT = new Intent("com.sunnykwong.omc.BGSERVICE");
	static final Intent WIDGETREFRESHINTENT = new Intent("com.sunnykwong.omc.WIDGET_REFRESH");
	static final IntentFilter PREFSINTENTFILT = new IntentFilter("com.sunnykwong.omc.WIDGET_CONFIG");
	static final String APPICON = "clockicon";
	
	static final String[] FULLWEEKDAYS = {"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
	static final String[] ABVWEEKDAYS = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
	static final String[] FULLMONTHS = {"January","February","March","April","May","June",
		"July","August","September","October","November","December"};
	static final String[] ABVMONTHS = {"Jan","Feb","Mar","Apr","May","Jun",
		"Jul","Aug","Sep","Oct","Nov","Dec"};
	static final String[] APM = {"Ante Meridiem","Post Meridiem"};	
//  NO NEED TO CHANGE BELOW THIS LINE FOR VERSIONING
	static int faqtoshow = 0;
	static final String[] FAQS = {
		"Happy birthday, OMC!  v1.2.6 adds two Ice Cream Sandwich-inspired clocks, Ice Roboto and Ice Lock.  Enjoy!",
		"After an extended hiatus, Xaffron presents a new version of OMC!  As of v1.2.6, we are up to 62 clocks (from 37 in v1.2.2).",
		"Our newest additions are two beautiful clocks - Bare Basics and Red LED.  Selected clocks were also tweaked for better presentation.",
		"v1.2.5 reverts to the more popular scaling algorithm used in v1.2.2. Keep the feedback coming!",
		"OMC now supports battery levels.  Some of the widgets contain battery indicators in surprising ways - browse around and experiment!",
		"You can now specify a timezone for each clock independently of the device setting.  This allows for nice home/travel clock scenarios.",
		"Version 1.2.3 had to move back to 16-bit color rendering due to performance issues.  The good news, however, is that v1.2.3 is much snappier than previous versions!",
		"OMC stores themes in the .OMCThemes folder (with a dot).  This will keep OMC from cluttering up your Gallery... feel free to delete any old themes in your SD card's OMCThemes directory (without dot).",
		"OMC lets you send your favorite personalizations to Xaffron!  Tap and hold a clock on the theme picker screen, then select 'Email Tweaked Theme' to send to me.",
		"Not finding your favorite clock?  OMC comes with just a few initially.  To get the full clock collection, tap on 'Download Full Online Collection' when you're in the theme picker screen.",
		"OMC allows theme personalization on the phone.  Try moving each layer around, changing colors and toggling visibility!",
		"Theme personalization overwrites dynamic elements (changing colors, positions, text, etc).  To make fine-tuned changes to dynamic elements, fire up a text editor on your phone or computer and look at the contents of the 00control.json file.",
		"The paid version now features 8 widget sizes.  Not all themes work well with all widget sizes, but feel free to experiment! The newer ones look particularly good in the square and vertical widgets.",
		"Too many widget sizes?  Tap on 'Toggle Widget Sizes' and disable the ones you don't want.  Remember to reboot your phone to apply changes!",
		"Comments? Complaints? Donations?  Tap on the 'Contact Xaffron' box to send me a message.  I'll get back to you ASAP!",
		"Did you know that you can delete any clock you don't like by long-pressing on a clock?  If you want it back, simply select 'Re-Get Starter Clock Pack' and you're right back where you started.",
		"Tap-to-Launch now allows you to set 'Do nothing', which is great for avoiding accidental taps.  Tapping on the top left corner of the widget always brings you back to this screen.",
		"Foreground mode (Android 2.0 and above) is useful if your clock widget keeps lagging.  Clock lag should almost never happen as of version 1.2.0, but it's good to know that Foreground Mode is there when you need it.",
		"Want more quotes/different text?  I hear you!  For now, use a text editor on the 00control.json files (it's easy, I promise).  Otherwise, expect enhanced 'Personalization' features in the next couple of versions!",
		"Have an idea for a cool clock widget?  Tap 'Contact Xaffron' and let me know. I don't bite!",
		"Which clock is your favorite?  Let me know via 'Contact Xaffron'!  This will help me archive less popular widgets and keep the application size manageable.",
		"Did you know that there is a support thread on XDA-Developers?  I monitor questions on that board on a daily basis.  http://forum.xda-developers.com/showthread.php?t=807929"
	};
	
	static final String APPNAME = OMC.SINGLETON? OMC.SINGLETONNAME:"One More Clock";
	static final String OMCSHORT = (OMC.SINGLETON? OMC.SINGLETONNAME.substring(0,4):"OMC") + (OMC.FREEEDITION? "free":"");

	static final String OMCNAME = "com.sunnykwong.omc";
	static String SHAREDPREFNAME;
	static String PKGNAME;
	static Context CONTEXT;
	static boolean SHOWHELP = true;
	static Uri PAIDURI;
	
	static long LASTUPDATEMILLIS, LEASTLAGMILLIS=200;
	static int UPDATEFREQ = 20000;
	static final Random RND = new Random();
	static SharedPreferences PREFS;
	static AlarmManager ALARMS;	// I only need one alarmmanager.
	static AssetManager AM;
    static NotificationManager NM;
    static Resources RES;

    static Typeface GEOFONT;
    
    static HashMap<String, Typeface> TYPEFACEMAP;
    static HashMap<String, Bitmap> BMPMAP;
    static Map<String, JSONObject> THEMEMAP;
    static HashMap<Bitmap, Canvas> BMPTOCVAS;
    static HashMap<Integer, Bitmap> WIDGETBMPMAP;
    
    static OMCConfigReceiver cRC;
	static OMCAlarmReceiver aRC;
    static boolean SCREENON = true; 	// Is the screen on?
    static boolean FG = false;

	static boolean STARTERPACKDLED = false;

	static final int WIDGETWIDTH=480;
	static final int WIDGETHEIGHT=480;
	static final String[] COMPASSPOINTS = {"NW","N","NE","W","C","E","SW","S","SE"};
	static final Time TIME = new Time();
	static String CACHEPATH;
	static String[] WORDNUMBERS;
	static JSONObject STRETCHINFO;
	static String[] OVERLAYURIS;
	static int[] OVERLAYRESOURCES;

	static ComponentName WIDGET4x4CNAME;
	static ComponentName WIDGET4x2CNAME;
	static ComponentName WIDGET4x1CNAME;
	static ComponentName WIDGET3x3CNAME;
	static ComponentName WIDGET3x1CNAME;
	static ComponentName WIDGET2x2CNAME;
	static ComponentName WIDGET2x1CNAME;
	static ComponentName WIDGET1x3CNAME;
	static ComponentName SKINNERCNAME;

	static final float[] FLARERADII = new float[] {32.f,20.f,21.6f,40.2f,18.4f,19.1f,10.8f,25.f,28.f};
	static final int[] FLARECOLORS = new int[] {855046894,1140258554,938340342,1005583601,855439588,
		669384692,905573859,1105458423,921566437};
	
	static final int SVCNOTIFICATIONID = 1; // Notification ID for the one and only message window we'll show
    static final Class<?>[] mStartForegroundSignature = new Class[] {int.class, Notification.class};
    static final Class<?>[] mStopForegroundSignature = new Class[] {boolean.class};
    static Intent SVCSTARTINTENT, CREDITSINTENT, PREFSINTENT;
    static Intent GETSTARTERPACKINTENT, GETBACKUPPACKINTENT, GETEXTENDEDPACKINTENT, IMPORTTHEMEINTENT, DUMMYINTENT, OMCMARKETINTENT;
    static PendingIntent FGPENDING, BGPENDING, PREFSPENDING;
    static Notification FGNOTIFICIATION;
    
    static final ArrayBlockingQueue<Matrix> MATRIXPOOL = new ArrayBlockingQueue<Matrix>(2);
    static final ArrayBlockingQueue<Paint> PAINTPOOL = new ArrayBlockingQueue<Paint>(2);
    static final ArrayBlockingQueue<Bitmap> WIDGETPOOL = new ArrayBlockingQueue<Bitmap>(1);
    
    static final Bitmap ROTBUFFER = Bitmap.createBitmap(OMC.WIDGETWIDTH, OMC.WIDGETHEIGHT, Bitmap.Config.ARGB_8888);

    final Handler mHandler = new Handler();
    static String mMessage;
	final Runnable mPopToast = new Runnable() {
		public void run() {
			Toast.makeText(OMC.this, mMessage, Toast.LENGTH_LONG).show();
		}
	};
    
	@Override
	public void onCreate() {
		super.onCreate();

		try {
			OMC.THISVERSION = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName;
		} catch (NameNotFoundException e) {
			OMC.THISVERSION = "1.0.0";
		}
		OMC.CONTEXT = getApplicationContext();
		
		OMC.PKGNAME = getPackageName();
		OMC.SHAREDPREFNAME = OMC.PKGNAME + "_preferences";
		OMC.PAIDURI = (OMC.SINGLETON? Uri.parse("market://details?id=" + OMC.PKGNAME +"donate"):Uri.parse("market://details?id=com.sunnykwong.omc"));

		OMC.WIDGET4x2CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget4x2");
		OMC.WIDGET4x4CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget4x4");
		OMC.WIDGET4x1CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget4x1");
		OMC.WIDGET3x3CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget3x3");
		OMC.WIDGET3x1CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget3x1");
		OMC.WIDGET2x2CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget2x2");
		OMC.WIDGET2x1CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget2x1");
		OMC.WIDGET1x3CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget1x3");
		OMC.SKINNERCNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".OMCSkinnerActivity");

		
		OMC.LASTUPDATEMILLIS = 0l;
		OMC.LEASTLAGMILLIS = 0l;
		
		OMC.aRC = new OMCAlarmReceiver();
		OMC.cRC = new OMCConfigReceiver();
		
		OMC.FGPENDING = PendingIntent.getBroadcast(OMC.CONTEXT, 0, OMC.FGINTENT, 0);
		OMC.BGPENDING = PendingIntent.getBroadcast(OMC.CONTEXT, 0, OMC.BGINTENT, 0);
		OMC.SVCSTARTINTENT = new Intent(OMC.CONTEXT, OMCService.class);
		OMC.CREDITSINTENT = new Intent(OMC.CONTEXT, OMCCreditsActivity.class);
		OMC.PREFSINTENT = new Intent(OMC.CONTEXT, OMCPrefActivity.class);
		OMC.IMPORTTHEMEINTENT = new Intent(OMC.CONTEXT, OMCThemeImportActivity.class);
		OMC.PREFSPENDING = PendingIntent.getActivity(OMC.CONTEXT, 0, new Intent(OMC.CONTEXT, OMCPrefActivity.class), 0);
		OMC.PREFSINTENTFILT.addDataScheme("omc");
		OMC.DUMMYINTENT = new Intent(OMC.CONTEXT, DUMMY.class);
		OMC.GETSTARTERPACKINTENT = new Intent(OMC.CONTEXT, OMCThemeUnzipActivity.class);
		OMC.GETSTARTERPACKINTENT.setData(Uri.parse(OMC.STARTERPACKURL));
		OMC.GETEXTENDEDPACKINTENT = new Intent(OMC.CONTEXT, OMCThemeUnzipActivity.class);
		OMC.GETEXTENDEDPACKINTENT.setData(Uri.parse(OMC.EXTENDEDPACK));
		OMC.GETBACKUPPACKINTENT = new Intent(OMC.CONTEXT, OMCThemeUnzipActivity.class);
		OMC.GETBACKUPPACKINTENT.setData(Uri.parse(OMC.EXTENDEDPACKBACKUP));
		OMC.OMCMARKETINTENT = new Intent(Intent.ACTION_VIEW,OMC.PAIDURI);

		OMC.CACHEPATH = this.getCacheDir().getAbsolutePath() + "/";
		
		OMC.FGNOTIFICIATION = new Notification(this.getResources().getIdentifier(OMC.APPICON, "drawable", OMC.PKGNAME), 
				"",
        		System.currentTimeMillis());
        OMC.FGNOTIFICIATION.flags = OMC.FGNOTIFICIATION.flags|Notification.FLAG_ONGOING_EVENT|Notification.FLAG_NO_CLEAR;
		
    	OMC.ALARMS = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
    	OMC.AM = getAssets();
    	OMC.RES = getResources();
    	OMC.NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    	OMC.GEOFONT = Typeface.createFromAsset(OMC.AM, "GeosansLight.ttf");
    	OMC.PREFS = getSharedPreferences(SHAREDPREFNAME, Context.MODE_PRIVATE);
		// We are using Zehro's solution (listening for TIME_TICK instead of using AlarmManager + FG Notification) which
		// should be quite a bit more graceful.
		OMC.FG = OMC.PREFS.getBoolean("widgetPersistence", false)? true : false;
		
		// If we're from a legacy version, then we need to wipe all settings clean to avoid issues.
		if (OMC.PREFS.getString("version", "1.0.x").startsWith("1.0") || OMC.PREFS.getString("version", "1.0.x").startsWith("1.1")) {
			Log.i(OMC.OMCSHORT + "App","Upgrade from legacy version, wiping all settings.");
			OMC.PREFS.edit().clear().putBoolean("showhelp", true).commit();
			OMC.SHOWHELP=true;
		}
		if (OMC.PREFS.getString("version", "1.0.x").equals(OMC.THISVERSION)) {
			OMC.STARTERPACKDLED = OMC.PREFS.getBoolean("starterpack", false);
			OMC.SHOWHELP=OMC.PREFS.getBoolean("showhelp", true);
		} else {
			OMC.PREFS.edit().putBoolean("starterpack", false).commit();
			OMC.STARTERPACKDLED = false;
			OMC.PREFS.edit().clear().putBoolean("showhelp", true).commit();
			OMC.SHOWHELP=true;
		}

		OMC.PREFS.edit().putString("version", OMC.THISVERSION).commit();
		OMC.UPDATEFREQ = OMC.PREFS.getInt("iUpdateFreq", 30) * 1000;
		
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_SCREEN_ON));
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_TIME_TICK));
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_TIME_CHANGED));
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED));
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
		OMC.TYPEFACEMAP = new HashMap<String, Typeface>(3);
		OMC.BMPMAP = new HashMap<String, Bitmap>(16);
		OMC.THEMEMAP=Collections.synchronizedMap(new HashMap<String, JSONObject>(3));
		OMC.BMPTOCVAS = new HashMap<Bitmap, Canvas>(16);
		OMC.WIDGETBMPMAP = new HashMap<Integer, Bitmap>(3);
		
		OMC.STRETCHINFO = null;
		
		OMC.OVERLAYURIS = null;
		OMC.OVERLAYRESOURCES = new int[] {
				this.getResources().getIdentifier("NW", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("N", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("NE", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("W", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("C", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("E", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("SW", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("S", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("SE", "id", OMC.PKGNAME)
				};

		OMC.WORDNUMBERS = this.getResources().getStringArray(this.getResources().getIdentifier("WordNumbers", "array", OMC.PKGNAME));

		while (OMC.MATRIXPOOL.remainingCapacity() > 0 ) OMC.MATRIXPOOL.add(new Matrix());
		while (OMC.PAINTPOOL.remainingCapacity() > 0 ) OMC.PAINTPOOL.add(new Paint());
		while (OMC.WIDGETPOOL.remainingCapacity() > 0 ) {
			Bitmap bmp = Bitmap.createBitmap(OMC.WIDGETWIDTH, OMC.WIDGETHEIGHT, Bitmap.Config.ARGB_8888);
			Canvas cvas = new Canvas(bmp);
			OMC.WIDGETPOOL.add(bmp);
			OMC.BMPTOCVAS.put(bmp, cvas);
		}
		OMC.BMPTOCVAS.put(OMC.ROTBUFFER, new Canvas(OMC.ROTBUFFER));
		
		setupDefaultTheme();
		this.widgetClicks();
		OMC.toggleWidgets(this);
	}
	
	static public void toggleWidgets(Context context) {
			
    	// Enable/Disable the various size widgets
    	context.getPackageManager()
		.setComponentEnabledSetting(
				OMC.WIDGET4x2CNAME,
				OMC.PREFS.getBoolean("bFourByTwo", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
						: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
    	context.getPackageManager()
		.setComponentEnabledSetting(
				OMC.SKINNERCNAME,
				OMC.PREFS.getBoolean("bSkinner", false) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
						: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
    	
    	if (OMC.FREEEDITION) {
    		OMC.PREFS.edit()
    				.putBoolean("bFourByOne", false)
    				.putBoolean("bThreeByOne", false)
    				.putBoolean("bTwoByOne", false)
    				.commit();
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET4x4CNAME,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET4x1CNAME,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET3x3CNAME,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET3x1CNAME,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET2x2CNAME,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET2x1CNAME,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET1x3CNAME,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
    	} else {
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET4x4CNAME,
					OMC.PREFS.getBoolean("bFourByFour", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET4x1CNAME,
					OMC.PREFS.getBoolean("bFourByOne", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET3x3CNAME,
					OMC.PREFS.getBoolean("bThreeByThree", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET3x1CNAME,
					OMC.PREFS.getBoolean("bThreeByOne", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET2x2CNAME,
					OMC.PREFS.getBoolean("bTwoByTwo", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET2x1CNAME,
					OMC.PREFS.getBoolean("bTwoByOne", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET1x3CNAME,
					OMC.PREFS.getBoolean("bOneByThree", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
    	}
	}

	static void setServiceAlarm (long lTimeToRefresh) {
		//We want the pending intent to be for this service, and 
		// at the same FG/BG preference as the intent that woke us up
		OMC.ALARMS.cancel(OMC.FGPENDING);
		OMC.ALARMS.cancel(OMC.BGPENDING);
		if (OMC.FG) {
			OMC.ALARMS.set(AlarmManager.RTC, lTimeToRefresh, OMC.FGPENDING);
		} else {
			OMC.ALARMS.set(AlarmManager.RTC, lTimeToRefresh, OMC.BGPENDING);
		}
    }

	public static void setPrefs(int aWI) {
		OMC.PREFS.edit().putString("widgetTheme"+aWI, OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME))
		.putBoolean("widget24HrClock"+aWI, OMC.PREFS.getBoolean("widget24HrClock", true))
		.putBoolean("widgetLeadingZero"+aWI, OMC.PREFS.getBoolean("widgetLeadingZero", true))
		.putString("URI"+aWI, OMC.PREFS.getString("URI", ""))
		.putString("sTimeZone"+aWI, OMC.PREFS.getString("sTimeZone", "default"))
		.commit();
	}
 
	public static void initPrefs(int aWI) {
		// For new clocks... just like setPrefs but leaves the URI empty.
		OMC.PREFS.edit().putString("widgetTheme"+aWI, OMC.PREFS.getString("widgetTheme"+aWI, OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME)))
		.putBoolean("widget24HrClock"+aWI, OMC.PREFS.getBoolean("widget24HrClock"+aWI, OMC.PREFS.getBoolean("widget24HrClock", true)))
		.putBoolean("widgetLeadingZero"+aWI, OMC.PREFS.getBoolean("widgetLeadingZero"+aWI, OMC.PREFS.getBoolean("widgetLeadingZero", true)))
		.putString("URI"+aWI, OMC.PREFS.getString("URI"+aWI, ""))
		.putString("sTimeZone"+aWI, OMC.PREFS.getString("sTimeZone"+aWI, "default"))
		.commit();
	}
 
	public static void getPrefs(int aWI) {
    	OMC.PREFS.edit().putString("widgetTheme", OMC.PREFS.getString("widgetTheme"+aWI, OMC.DEFAULTTHEME))
		.putBoolean("widget24HrClock", OMC.PREFS.getBoolean("widget24HrClock"+aWI, true))
		.putBoolean("widgetLeadingZero", OMC.PREFS.getBoolean("widgetLeadingZero"+aWI, true))
		.putString("URI", OMC.PREFS.getString("URI"+aWI, ""))
		.putString("sTimeZone", OMC.PREFS.getString("sTimeZone"+aWI, "default"))
		.commit();
	}
	
	public static void removePrefs(int aWI) {
		OMC.PREFS.edit()
			.remove("widgetTheme"+aWI)
			.remove("widget24HrClock"+aWI)
			.remove("widgetLeadingZero"+aWI)
			.remove("URI"+aWI)
			.remove("sTimeZone"+aWI)
			.commit();
		File f = new File(OMC.CACHEPATH + aWI +"cache.png");
		if (f.exists())f.delete();
	}
	
	public static Typeface getTypeface(String sTheme, String src) {
		//Look in memory cache;
		if (OMC.TYPEFACEMAP.get(src)!=null) {
			return OMC.TYPEFACEMAP.get(src);
		}
		//Look in app cache;
		if (new File(OMC.CACHEPATH + sTheme + src).exists()) {
				try {
					OMC.TYPEFACEMAP.put(src, Typeface.createFromFile(OMC.CACHEPATH + sTheme + src));
					return OMC.TYPEFACEMAP.get(src);
				} catch (RuntimeException e) {
					// if Cache is invalid, do nothing; we'll let this flow through to the full FS case.
				}
		}
		//Look in full file system;
		if (new File(src).exists()) {
				OMC.TYPEFACEMAP.put(src, Typeface.createFromFile(src));
				return OMC.TYPEFACEMAP.get(src);
		}
		//Look in sd card;
		if (OMC.checkSDPresent()) {
			File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/"+sTheme+"/"+src);
			if (f.exists()) {
				copyFile(f.getAbsolutePath(),OMC.CACHEPATH +"/"+sTheme+f.getName());
				OMC.TYPEFACEMAP.put(src, Typeface.createFromFile(f));
				return OMC.TYPEFACEMAP.get(src);
			}
		}
		//Look in assets if default theme.
		if (sTheme.equals(OMC.DEFAULTTHEME)) {
			return Typeface.createFromAsset(OMC.AM, src);
		}
		return null;
	}

	public static Bitmap getBitmap(String sTheme, String src) {
		
		//Look in memory cache;
		if (OMC.BMPMAP.get(src)!=null) {
			return OMC.BMPMAP.get(src);
		}

		//Look in app cache;
		if (new File(OMC.CACHEPATH + sTheme + src).exists()) {
			OMC.BMPMAP.put(src, BitmapFactory.decodeFile(OMC.CACHEPATH + sTheme + src));
			return OMC.BMPMAP.get(src);
		}
		// Look in SD path
		if (OMC.checkSDPresent()) {
			File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/"+sTheme+"/"+src);
			if (f.exists()) {
				copyFile(f.getAbsolutePath(),OMC.CACHEPATH +"/"+sTheme+f.getName());
				OMC.BMPMAP.put(src, BitmapFactory.decodeFile(f.getAbsolutePath()));
				return OMC.BMPMAP.get(src);
			}
		} 
		// Look in assets
		if (sTheme.equals(OMC.DEFAULTTHEME)) {
			try {
				return BitmapFactory.decodeStream(OMC.AM.open(src));
			} catch (Exception e) {
				// Asset not found - do nothing
			}
		}
		// Bitmap can't be found anywhere
		return null;
	}

	public static void purgeTypefaceCache(){
		Iterator<Entry<String,Typeface>> i = OMC.TYPEFACEMAP.entrySet().iterator();
		while (i.hasNext()) {
			Entry<String,Typeface> entry = i.next();
			entry.setValue(null);
		}
		OMC.TYPEFACEMAP.clear();
	}
	
	public static void purgeBitmapCache(){
		Iterator<Entry<String,Bitmap>> i = OMC.BMPMAP.entrySet().iterator();
		while (i.hasNext()) {
			Entry<String,Bitmap> entry = i.next();
			entry.getValue().recycle();
			entry.setValue(null);
		}
		OMC.BMPMAP.clear();
	}
	
	public synchronized static JSONObject getTheme(final Context context, final String nm, final boolean bFromCache){
		// Look in memory cache
		if (OMC.THEMEMAP.containsKey(nm) && OMC.THEMEMAP.get(nm)!=null && bFromCache){ 
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App",nm + " loaded from mem.");
			return OMC.THEMEMAP.get(nm);
		}
		// Look in cache dir
		if (new File(OMC.CACHEPATH + nm + "00control.json").exists() && bFromCache) {
			try {
				BufferedReader in = new BufferedReader(new FileReader(OMC.CACHEPATH + nm + "00control.json"),8192);
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    
				JSONObject oResult = new JSONObject(sb.toString());
				sb.setLength(0);
				OMC.THEMEMAP.put(nm, oResult);
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App",nm + " loaded from cachedir.");
				return oResult;
			} catch (Exception e) {
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","error reloading " + nm + " from cachedir.");
				e.printStackTrace();
			} finally {
				//System.gc();
			}
		}
		// Look in SD path
		if (OMC.checkSDPresent()) {
			File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/"+nm);
			if (f.exists() && f.isDirectory()) {
				for (File ff:f.listFiles()) {
					copyFile(ff.getAbsolutePath(),OMC.CACHEPATH + nm + ff.getName());
				}
			}
			try {
				BufferedReader in = new BufferedReader(new FileReader(OMC.CACHEPATH + nm + "00control.json"),8192);
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
				JSONObject oResult = new JSONObject(sb.toString());
				sb.setLength(0);
				if (!OMC.validateTheme(oResult)) throw new Exception();
				OMC.THEMEMAP.put(nm, oResult);
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App",nm + " loaded from SD.");
				return oResult;
			} catch (Exception e) {
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","error loading " + nm + " from SD.");
			} finally {
				//System.gc();
			}

		} 
		// If default theme, look in assets
		if (nm.equals(OMC.DEFAULTTHEME)) {
			try {
				InputStreamReader in = new InputStreamReader(OMC.AM.open("00control.json"));
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    
				JSONObject oResult = new JSONObject(sb.toString());
				sb.setLength(0);
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App",nm + " loaded from assets.");
				return oResult;
			} catch (Exception e) {
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","error reloading " + nm + " from assets.");
				e.printStackTrace();
			} finally {
				//System.gc();
			}
			
		}
		return null;
	}

	public static void themeToFile(JSONObject obj, File tgt) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(tgt),8192);
			out.write(obj.toString(5));
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void bmpToJPEG(Bitmap bmp, File tgt) {
		try {
		       FileOutputStream out = new FileOutputStream(tgt);
		       Bitmap.createScaledBitmap(Bitmap.createBitmap(bmp,0,0,480,300),320,200,true).compress(Bitmap.CompressFormat.JPEG, 85, out);
		       out.close();
		} catch (Exception e) {
		       e.printStackTrace();
		}
	}	
	public static void purgeImportCache() {
		for (File f:(new File(OMC.CACHEPATH).listFiles())) {
			f.delete();
		}
		//System.gc();
	}
		
	public static void removeDirectory(File f) {
		for (File ff:f.listFiles()) {
			if (ff.equals(f)) continue;
	    	if (!ff.isDirectory()) ff.delete();
	    	else removeDirectory(ff);
		}
		f.delete();
	}

	public boolean fixnomedia() {
		File oldomcdir = new File( Environment.getExternalStorageDirectory().getAbsolutePath()+"/OMCThemes");
		if (!oldomcdir.exists()) return true;
		Thread t = new Thread() {
			public void run() {
				mMessage = "Moving your themes to the new .OMCThemes directory from the Gallery.  Please do not mount or remove your SD card while this fix is in progress...";
				mHandler.post(mPopToast);
				File srcDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OMCThemes");
				File tgtDir=new File( Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes");
				tgtDir.mkdirs();
				OMC.copyDirectory(srcDir, tgtDir);
				mMessage = "Almost There...";
				mHandler.post(mPopToast);
				OMC.removeDirectory(srcDir);
				mMessage = "Your OMC themes are now hidden from media scans.  You can now use your phone normally.";
				mHandler.post(mPopToast);
			}
		};
		t.start();
		return true;
	}
	
	public static void copyDirectory(File src, File tgt) {
		if (!tgt.exists()) tgt.mkdirs();
		if (!tgt.isDirectory()) return;

		for (File ff:src.listFiles()) {
			if (ff.isDirectory()) {
				copyDirectory(ff,new File(tgt.getAbsolutePath()+"/"+ff.getName()));
			} else {
				copyFile(ff.getAbsolutePath(), tgt.getAbsolutePath()+"/"+ff.getName());
			}
		}
	}
	
	public static boolean copyFile(String src, String tgt) {
		try {
			FileOutputStream oTGT = new FileOutputStream(tgt);
			FileInputStream oSRC = new FileInputStream(src);
		    byte[] buffer = new byte[8192];
		    int iBytesRead = 0;
		    while ((iBytesRead = oSRC.read(buffer))!= -1){
		    	oTGT.write(buffer,0,iBytesRead);
		    }
		    oTGT.close();
		    oSRC.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean copyAssetToCache(String src, String sTheme) {
		try {
			FileOutputStream oTGT = new FileOutputStream(OMC.CACHEPATH + sTheme + src);
			InputStream oSRC = OMC.AM.open(src);
			
		    byte[] buffer = new byte[8192];
		    int iBytesRead = 0;
		    while ((iBytesRead = oSRC.read(buffer))!= -1){
		    	oTGT.write(buffer,0,iBytesRead);
		    }
		    oTGT.close();
		    oSRC.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static JSONArray loadStringArray(String sTheme, int aWI, String sKey) {	
		try {
			JSONObject oTheme = OMC.THEMEMAP.get(sTheme);
			return oTheme.getJSONObject("arrays").getJSONArray(sKey);

		} catch (Exception e) {
			if (OMC.DEBUG) Log.i ("OMCApp","Can't find array " + sKey + " in resources!");
			e.printStackTrace();
			return null;
		}
	}

	public void widgetClicks() {
		try {
			this.getPackageManager().getPackageInfo("com.sunnykwong.ompc", 0);
			if (OMC.DEBUG)Log.i(OMC.OMCSHORT + "App","OMPC installed, let OMPC handle onclick");
			try {
				unregisterReceiver(OMC.cRC);
			} catch (java.lang.IllegalArgumentException e) {
    			if (OMC.DEBUG)Log.i(OMC.OMCSHORT + "App","OMC's receiver already unregistered - doing nothing");
				//no need to do anything if receiver not registered
			}
		} catch (Exception e) {

			if (OMC.DEBUG)Log.i(OMC.OMCSHORT + "App","OMPC not installed, register self to handle widget clicks");
			//e.printStackTrace();
			try {
				this.registerReceiver(OMC.cRC,OMC.PREFSINTENTFILT);
			} catch (Exception ee) {
    			if (OMC.DEBUG)Log.i(OMC.OMCSHORT + "App","Failed to register self");
				ee.printStackTrace();
			}
		}
		

	}
	
	public static void setupDefaultTheme() {
		if (new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/"+ OMC.DEFAULTTHEME + "/00control.json").exists()) return;
		copyAssetToCache("000preview.jpg", OMC.DEFAULTTHEME);
		copyAssetToCache("00control.json", OMC.DEFAULTTHEME);
		copyAssetToCache("A.png", OMC.DEFAULTTHEME);
		copyAssetToCache("colon.png", OMC.DEFAULTTHEME);
		copyAssetToCache("digit .png", OMC.DEFAULTTHEME);
		copyAssetToCache("digit0.png", OMC.DEFAULTTHEME);
		copyAssetToCache("digit1.png", OMC.DEFAULTTHEME);
		copyAssetToCache("digit2.png", OMC.DEFAULTTHEME);
		copyAssetToCache("digit3.png", OMC.DEFAULTTHEME);
		copyAssetToCache("digit4.png", OMC.DEFAULTTHEME);
		copyAssetToCache("digit5.png", OMC.DEFAULTTHEME);
		copyAssetToCache("digit6.png", OMC.DEFAULTTHEME);
		copyAssetToCache("digit7.png", OMC.DEFAULTTHEME);
		copyAssetToCache("digit8.png", OMC.DEFAULTTHEME);
		copyAssetToCache("digit9.png", OMC.DEFAULTTHEME);
		copyAssetToCache("P.png", OMC.DEFAULTTHEME);
		copyAssetToCache("small-a.png", OMC.DEFAULTTHEME);
		copyAssetToCache("small-p.png", OMC.DEFAULTTHEME);
		(new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/"+ OMC.DEFAULTTHEME)).mkdirs();
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "00control.json", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/00control.json");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "000preview.jpg", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/000preview.jpg");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "A.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/A.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "colon.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/colon.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "digit .png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/digit .png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "digit0.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/digit0.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "digit1.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/digit1.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "digit2.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/digit2.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "digit3.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/digit3.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "digit4.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/digit4.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "digit5.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/digit5.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "digit6.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/digit6.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "digit7.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/digit7.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "digit8.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/digit8.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "digit9.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/digit9.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "P.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/P.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "small-a.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/small-a.png");
		copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + "small-p.png", Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/small-p.png");
	}
	
	public static boolean validateTheme(JSONObject theme) {	
		
		//Innocent until proven guilty.
		boolean valid = true;

		// Test theme for the required elements:
		// ID
		// Name
		// Layers
		
		try {
			theme.getString("id");
			theme.getString("name");
			theme.getJSONArray("layers_bottomtotop");
		} catch (JSONException e) {
			valid = false;
		}
		
		return valid;
		
    }
	
	public static boolean checkSDPresent() {
    	
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
//        	Toast.makeText(getApplicationContext(), "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
			return false;
        }

        File sdRoot = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes");
        if (!sdRoot.exists()) {
//        	Toast.makeText(this, "OMCThemes folder not found in your SD Card.\nCreating folder...", Toast.LENGTH_LONG).show();
        	sdRoot.mkdir();
        }
		if (!sdRoot.canRead()) {
//        	Toast.makeText(this, "SD Card missing or corrupt.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
        	return false;
        }
		return true;
    }

	static public JSONObject renderThemeObject(JSONObject theme, int aWI) throws JSONException {
		JSONObject result;
		
		result = new JSONObject();

		// Since the rendered object is only used for drawing, we'll skip
		// ID, Name, Author and Credits
		
		// First, render the dynamic elements in arrays
		
		if (theme.has("arrays")) {
			JSONObject resultArrays = new JSONObject();
			result.put("arrays", resultArrays);
			
			@SuppressWarnings("unchecked")
			Iterator<String> i = theme.optJSONObject("arrays").keys();
			while (i.hasNext()) {
				String sKey = i.next();
				JSONArray tempResultArray = new JSONArray();
				resultArrays.put(sKey, tempResultArray);

				JSONArray tempArray = theme.optJSONObject("arrays").optJSONArray(sKey);
				for (int j=0;j<tempArray.length();j++) {
					tempResultArray.put(OMC.resolveTokens(tempArray.getString(j), aWI, result));
				}
			}
		}

		// Then, render all the dynamic elements in each layer...
		
		JSONArray layerJSONArray = theme.optJSONArray("layers_bottomtotop");
		if (layerJSONArray==null) return null; //ERR: A theme cannot have no layers
		JSONArray tempLayerArray = new JSONArray();
		result.put("layers_bottomtotop", tempLayerArray);
		
		for (int j = 0 ; j < layerJSONArray.length(); j++) {
			JSONObject layer = layerJSONArray.optJSONObject(j);
			JSONObject renderedLayer = new JSONObject();
			tempLayerArray.put(renderedLayer);
			
			@SuppressWarnings("unchecked")
			Iterator<String> i = layer.keys();
			while (i.hasNext()) {
				String sKey = i.next();
				if (sKey.equals("text_stretch")) continue;

				renderedLayer.put(sKey, OMC.resolveTokens((String)(layer.optString(sKey)), aWI, result));

			}
			
			// before tweaking the max/maxfit stretch factors.
			String sStretch = layer.optString("text_stretch");
			if (sStretch==null) {
				renderedLayer.put("text_stretch", "1");
			} else {
				renderedLayer.put("text_stretch", OMC.resolveTokens((String)(layer.optString("text_stretch")), aWI, result));
			}
		}

		

		
		return result;
	}
	
	static public String resolveOneToken(String sRawString, int aWI, JSONObject tempResult) {
		boolean isDynamic = false;
		if (sRawString.contains("[%")) isDynamic = true;
		// Strip brackets.
		String sBuffer = sRawString.replace("[%", "").replace("%]", "");
		//by default, return strftime'd string.
		if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
			sBuffer = (OMC.OMCstrf(
				OMC.PREFS.getBoolean("widgetLeadingZero"+aWI, true)? 
						sBuffer : sBuffer.replaceAll("%H", "%k")
				)
			);
		} else {
			sBuffer = (OMC.OMCstrf(
				OMC.PREFS.getBoolean("widgetLeadingZero"+aWI, true)? 
						sBuffer.replaceAll("%H", "%I") : sBuffer.replaceAll("%H", "%l")
				)
			);
		}

		// If it's unbracketed text, we're done
		if (!isDynamic) return sBuffer;
		
		// If it is actually a dynamic element, resolve it
		// By default, 
		String result = "";

		StringTokenizer st = new StringTokenizer(sBuffer, "_");

		//Get the first element (command).
		String sToken = st.nextToken();
		if (sToken.startsWith("shift")) {

			// value that changes linearly and repeats every X seconds from 6am.
			String sType = st.nextToken();
			int iIntervalSeconds =  Integer.parseInt(st.nextToken());
			int iGradientSeconds = 0;

			// Where are we in time?  intGradientSeconds starts the count from either 12am or 6am
			if (sToken.equals("shift12")) {
				iGradientSeconds = OMC.TIME.second + OMC.TIME.minute*60 + OMC.TIME.hour*3600;
			} else { 
				// (sToken.equals("shift6"))
				iGradientSeconds = OMC.TIME.second + OMC.TIME.minute*60 + ((OMC.TIME.hour+18)%24)*3600;
			} 

			float gradient = (iGradientSeconds % iIntervalSeconds)/(float)iIntervalSeconds;
			
			if (sType.equals("number")) {
				result = String.valueOf(OMC.numberInterpolate(
						Integer.parseInt(st.nextToken()), 
						Integer.parseInt(st.nextToken()), 
						Integer.parseInt(st.nextToken()), 
						gradient));
			} else if (sType.equals("color")) {
				int color = OMC.colorInterpolate(
						st.nextToken(), 
						st.nextToken(), 
						st.nextToken(), 
						gradient);
				result = String.valueOf("#" + Integer.toHexString(color));
			} else if (sType.equals("float")) {
				result = String.valueOf(OMC.floatInterpolate(
						Float.parseFloat(st.nextToken()), 
						Float.parseFloat(st.nextToken()), 
						Float.parseFloat(st.nextToken()), 
						gradient));
			} else {
				//Unknown - do nothing
			}
		} else if (sToken.equals("ifequal")) {
			if (st.nextToken().equals(st.nextToken())) {
				//go to the positive result and skip the negative
				result = st.nextToken();
				st.nextToken();
			} else {
				//Skip the positive result and go to the negative result
				st.nextToken();
				result = st.nextToken();
			}
		} else if (sToken.equals("ompc")) {
			String sType = st.nextToken();
			if (sType.equals("battpercent")) {
				result = String.valueOf((int)(1000+OMC.PREFS.getInt("ompc_"+sType,99))).substring(1);
			} else if (sType.equals("battdecimal")) {
					result = String.valueOf(OMC.PREFS.getInt("ompc_battpercent",99)/100f);
			} else {
				result = String.valueOf(OMC.PREFS.getInt("ompc_"+sType, 99));
			}
		} else if (sToken.equals("circle")) {
			// Specifies a point at angle/radius from point.
			int iOriginVal = Integer.parseInt(st.nextToken());
			String sType = st.nextToken();
			int iAngle = Integer.parseInt(st.nextToken());
			int iRadius =  Integer.parseInt(st.nextToken());

			if (sType.equals("cos")) {
				result = String.valueOf(iOriginVal + (int)(iRadius * Math.cos(iAngle*Math.PI/180d)));
			} else if (sType.equals("sin")) {
				result = String.valueOf(iOriginVal + (int)(iRadius * Math.sin(iAngle*Math.PI/180d)));
			} else {
				//Unknown - do nothing
			}
			
		} else if (sToken.equals("ap24")) {
			if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
				st.nextToken();
				st.nextToken();
				result = (st.nextToken());
			} else if (OMC.TIME.hour < 12) {
				result = (st.nextToken());
			} else {
				st.nextToken();
				result = (st.nextToken());
			}
		} else if (sToken.equals("array")) {

			result = tempResult.optJSONObject("arrays").optJSONArray(st.nextToken()).optString(Integer.parseInt(st.nextToken().replace(" ","")));
			String sCase = st.nextToken();
			if (result == null) result = "ERROR";
			if (sCase.equals("lower")) result = result.toLowerCase();
			else if (sCase.equals("upper")) result = result.toUpperCase();

		} else if (sToken.equals("flipformat")) {
			int iApply = Integer.parseInt(st.nextToken());
			String sType = st.nextToken("_ ");
			if (sType.equals("bold")) {
				StringBuilder sb = new StringBuilder();
				while (st.hasMoreTokens()) {
					if (iApply==1) sb.append("<B>"+st.nextToken()+"</B> ");
					else sb.append(st.nextToken()+" ");
					iApply*=-1;
				}
				result = sb.toString();
			} else if (sType.equals("case")) {
				StringBuilder sb = new StringBuilder();
				while (st.hasMoreTokens()) {
					if (iApply==1) sb.append(st.nextToken().toUpperCase()+" ");
					else sb.append(st.nextToken().toLowerCase()+" ");
					iApply*=-1;
				}
				result = sb.toString();
			} else if (sType.equals("italics")) {
				StringBuilder sb = new StringBuilder();
				while (st.hasMoreTokens()) {
					if (iApply==1) sb.append("<I>"+st.nextToken()+"</I> ");
					else sb.append(st.nextToken()+" ");
					iApply*=-1;
				}
				result = sb.toString();
			}
		} else if (sToken.equals("stripspaces")){
			result = st.nextToken().replace(" ", "");
		} else if (sToken.equals("fit")){
			result = "f"+st.nextToken();
		} else if (sToken.equals("maxfit")){
			result = st.nextToken()+"m"+st.nextToken();
		} else if (sToken.equals("fullenglishtime")){
			// full english time
			String sType = st.nextToken();
			String sTemp = "";
			if (OMC.TIME.minute == 0) {
				sTemp = OMC.WORDNUMBERS[OMC.TIME.hour%12] + " o'Clock.";
			} else if (OMC.TIME.minute == 30) {
				sTemp = "half past " + OMC.WORDNUMBERS[OMC.TIME.hour%12] + "."; 
			} else if (OMC.TIME.minute == 15) {
				sTemp = "A Quarter past " + OMC.WORDNUMBERS[OMC.TIME.hour%12] + "."; 
			} else if (OMC.TIME.minute == 45) {
				if (OMC.TIME.hour == 11 || OMC.TIME.hour == 23) {
					sTemp = "A Quarter to Twelve.";
				} else {
					sTemp = "A Quarter to " 
					+ OMC.WORDNUMBERS[OMC.TIME.hour%12+1] + ".";
				}
			} else if (OMC.TIME.minute > 30) {
				if (OMC.TIME.hour == 11 || OMC.TIME.hour == 23) {
					sTemp = OMC.WORDNUMBERS[60-OMC.TIME.minute] + " to Twelve.";
				} else if (OMC.TIME.hour%12 == 0) {
						sTemp = OMC.WORDNUMBERS[60-OMC.TIME.minute] + " to One.";
				} else {
					sTemp = OMC.WORDNUMBERS[60-OMC.TIME.minute] + " to " 
					+ OMC.WORDNUMBERS[OMC.TIME.hour%12+1] + ".";
				}
			} else {
				if (OMC.TIME.hour%12 == 0) {
					sTemp = OMC.WORDNUMBERS[OMC.TIME.minute] + " past Twelve.";
				} else {
					sTemp = OMC.WORDNUMBERS[OMC.TIME.minute] + " past " 
							+ OMC.WORDNUMBERS[OMC.TIME.hour%12] + ".";
				}
			}
			if (sType.equals("diary")) result = (sTemp);
			else if (sType.equals("upper")) result = (sTemp.toUpperCase());
			else if (sType.equals("lower")) result = (sTemp.toLowerCase());
			else result = (sTemp);

		} else if (sToken.equals("digit")) { // must be color
			String sTemp = st.nextToken();

    		int iOffset = Integer.parseInt(st.nextToken());
    		result = (sTemp.substring(iOffset-1,iOffset));
			
			
		} else if (sToken.equals("day")){
			// value that switches between two fixed symbols - day (6a-6p) and night (6p-6a).
			if (OMC.TIME.hour >= 6 && OMC.TIME.hour < 18) {
				//Day
				result = (st.nextToken());
			} else {
				//Night - throw away the day token + the night indicator
				st.nextToken();
				st.nextToken();
				result = (st.nextToken());
			}
		} else if (sToken.equals("random")){
			// value that randomly jumps between two values.
			String sType = st.nextToken();
			float gradient = OMC.RND.nextFloat();
			if (sType.equals("number")) {
				result = String.valueOf(OMC.numberInterpolate(
						Integer.parseInt(st.nextToken()), 
						Integer.parseInt(st.nextToken()), 
						gradient));
			} else if (sType.equals("color")) { // must be color
				int color = OMC.colorInterpolate(
						st.nextToken(), 
						st.nextToken(), 
						gradient);
				result = String.valueOf("#" + Integer.toHexString(color));
			} else {
				//Unknown - do nothing
			}
			

		} else if (sToken.equals("gradient")){
			// value that randomly jumps between two values.
			String sType = st.nextToken();
			String sMin = st.nextToken();
			String sVal = st.nextToken();
			String sMax = st.nextToken();
			float gradient = Float.parseFloat(sVal);
			if (sType.equals("number")) {
				result = String.valueOf(OMC.numberInterpolate(
						Integer.parseInt(sMin), 
						Integer.parseInt(sMax), 
						gradient));
			} else if (sType.equals("color")) { // must be color
				int color = OMC.colorInterpolate(
						sMin, 
						sMax, 
						gradient);
				result = String.format("#%1$08X",color);
			} else {
				//Unknown - do nothing
			}
			

		} else {
			//unrecognized macro - ignore
		}
		
		return result;
		
	}

	static public String resolveTokens(String sRawString, int aWI, JSONObject tempResult) {
//		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Array","Parsing "+sRawString);
		StringBuilder result = new StringBuilder();

		// If token contains dynamic elements, recursively resolve them
		
		if (sRawString.contains("[%")) {
			int iCursor = 0;
			while (iCursor <= sRawString.length()) {
				//Cruise through the text before the first [% marker
				result.append(sRawString.substring(
						iCursor,sRawString.indexOf(
							"[%",iCursor)==-1? 
							sRawString.length() : 
							sRawString.indexOf("[%",iCursor)
					)
				);
				
				iCursor = sRawString.indexOf("[%",iCursor);

				int iMarker1, iMarker2;
				do {
				// Mark where the next [% and %] are.  Which is closer?
				iMarker1 = sRawString.indexOf("[%",iCursor+1);
				iMarker2 = sRawString.indexOf("%]",iCursor);

//				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Array","Markers: " +iCursor+" " + iMarker1+ " " + iMarker2);
				// No markers found? bad parsing occurred.
				// exit gracefully (try to)
				if (iMarker1 == -1 && iMarker2 == -1) {
//					if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Array","Improper markup. Bailing...");
					break;
					
				} else if (iMarker1 == -1) {
					// No more start markers found, but we have an end marker.
					// Dive into this substring.
//					if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Array","Sending down(-1): " + sRawString.substring(iCursor, iMarker2+2));
					result.append(OMC.resolveOneToken(sRawString.substring(iCursor, iMarker2+2), aWI, tempResult));
					result.append(sRawString.substring(iMarker2+2));
//					if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Array","Now at: " + result.toString());
				} else if (iMarker1 < iMarker2) {
					//if [% is closer, keep going until we find the innermost [%
//					if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Array","Found nested. (1<2)");
					result.append(sRawString.substring(iCursor,iMarker1));
//					if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Array","Now at: " + result.toString());
					iCursor = iMarker1;
				} else if (iMarker2 < iMarker1) {
					//if %] is closer, we have an end marker.
					//Dive into this substring.
//					if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Array","Sending down(2<1): " + sRawString.substring(iCursor, iMarker2+2));
					result.append(OMC.resolveOneToken(sRawString.substring(iCursor, iMarker2+2), aWI, tempResult));
//					if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Array","Now at: " + result.toString());
					//Move all markers to the next [% (we only deal with one layer here).
					//Start looking for the next directive.
					result.append(sRawString.substring(iMarker2+2, iMarker1));
					iCursor = iMarker1;
				}
				} while (iMarker1 != -1);
				
				// Pass it up to resolve nested stuff.
				return OMC.resolveTokens(result.toString(), aWI, tempResult);
			}
		}
		// If no tags found, then just run strftime; we're done! 
		return OMC.resolveOneToken(sRawString, aWI, tempResult);

	}
	
	static public String OMCstrf (final String input) {
		final char[] inputChars = input.toCharArray();
		final StringBuilder sb = new StringBuilder(inputChars.length);
		boolean bIsTag = false;
		for (int i = 0; i < inputChars.length; i++) {
			if (bIsTag) {
				if (inputChars[i]=='A') {
					sb.append(OMC.FULLWEEKDAYS[OMC.TIME.weekDay]);
				} else if (inputChars[i]=='a') {
					sb.append(OMC.ABVWEEKDAYS[OMC.TIME.weekDay]);
				} else if (inputChars[i]=='B') {
					sb.append(OMC.FULLMONTHS[OMC.TIME.month]);
				} else if (inputChars[i]=='b') {
					sb.append(OMC.ABVMONTHS[OMC.TIME.month]);
				} else if (inputChars[i]=='p') {
					sb.append(OMC.APM[OMC.TIME.hour<12?0:1]);
				} else {
					sb.append(OMC.TIME.format("%"+inputChars[i]));
				}
				bIsTag=false;
			} else if (inputChars[i]=='%'){
				bIsTag=true;
			} else {
				sb.append(inputChars[i]);
			}
		}
		
		return sb.toString();
	}
	
	static public int numberInterpolate (int n1, int n2, int n3, float gradient) {
		if (gradient > 0.5f) return (int)(n2+ (n3-n2)*(gradient-0.5f) * 2);
		else return (int)(n1 + (n2-n1) * gradient * 2);
	}
	
	static public float floatInterpolate (float n1, float n2, float n3, float gradient) {
		if (gradient > 0.5f) return (n2+ (n3-n2)*(gradient-0.5f) * 2);
		else return (n1 + (n2-n1) * gradient * 2);
	}
	
	static public int colorInterpolate (String c1, String c2, String c3, float gradient) {
		int a, r, g, b;
		int maxval, minval;
		if (gradient > 0.5f) {
			minval = Color.parseColor(c2);
			maxval = Color.parseColor(c3);
			gradient = (gradient - 0.5f) * 2;
		} else {
			minval = Color.parseColor(c1);
			maxval = Color.parseColor(c2);
			gradient = gradient * 2;
		}
		a = numberInterpolate(minval >>> 24, maxval >>> 24, gradient); 
		r = numberInterpolate((minval >> 16) & 0xFF, (maxval >> 16) & 0xFF, gradient);
		g = numberInterpolate((minval >> 8) & 0xFF, (maxval >> 8) & 0xFF, gradient);
		b = numberInterpolate(minval & 0xFF , maxval & 0xFF , gradient);

		return Color.argb(a,r,g,b);
	}
	
	static public int numberInterpolate (int n1, int n2, float gradient) {
		return (int)(n1 + (n2-n1) * gradient);
	}
	
	static public int colorInterpolate (String c1, String c2, float gradient) {
		int minval = Color.parseColor(c1);
		int maxval = Color.parseColor(c2);
		return Color.argb(numberInterpolate(minval >>> 24, maxval >>> 24, gradient),
				numberInterpolate((minval >> 16) & 0xFF, (maxval >> 16) & 0xFF, gradient),
				numberInterpolate((minval >> 8) & 0xFF, (maxval >> 8) & 0xFF, gradient),
				numberInterpolate(minval & 0xFF , maxval & 0xFF , gradient)
				);
	}
	

    @Override
    public void onTerminate() {
    	if (!OMCService.STOPNOW4x4 || !OMCService.STOPNOW4x2 || !OMCService.STOPNOW4x1 
    			|| !OMCService.STOPNOW3x3 || !OMCService.STOPNOW3x1 
    			|| !OMCService.STOPNOW2x2 || !OMCService.STOPNOW2x1
    			|| !OMCService.STOPNOW1x3) {
    		Log.i(OMC.OMCSHORT + "App","APP TERMINATED - NOT UNREGISTERING RECEIVERS - OMC WILL RESTART");
    		// do nothing
    	} else {
    		Log.i(OMC.OMCSHORT + "App","APP TERMINATED - UNREGISTERING RECEIVERS - OMC WILL NOT RESTART");
    		unregisterReceiver(aRC);
    	}
        OMC.PREFS.edit().commit();
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
    	purgeBitmapCache();
    	purgeImportCache();
    	purgeTypefaceCache();
    	OMC.WIDGETBMPMAP.clear();
    	super.onLowMemory();
    }

    static public Matrix getMatrix() {
    	try {
    		return OMC.MATRIXPOOL.take();
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    		return null;
    	}
    }
    
    static public void returnMatrix(Matrix m) {
    	try {
    		m.reset();
        	OMC.MATRIXPOOL.put(m);
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    }
    
    static public Paint getPaint() {
    	try {
    		return OMC.PAINTPOOL.take();
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    		return null;
    	}
    }
    
    static public void returnPaint(Paint pt) {
    	try {
    		pt.reset();
        	OMC.PAINTPOOL.put(pt);
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    }
    
    static public Bitmap getWidgetBMP() {
    	try {
    		return OMC.WIDGETPOOL.take();
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    		return null;
    	}
    }
    
    static public void returnWidgetBMP(Bitmap bmp) {
    	try {
    		bmp.eraseColor(Color.TRANSPARENT);
        	OMC.WIDGETPOOL.put(bmp);
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    }
   
}
