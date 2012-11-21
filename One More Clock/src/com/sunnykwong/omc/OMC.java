package com.sunnykwong.omc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
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
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

/**
 * @author skwong01
 * Thanks to ralfoide's 24clock code; taught me quite a bit about broadcastreceivers
 * Thanks to the Open Font Library for a great resource.
 * 
 */ 
public class OMC extends Application { 

	static final String TESTVER = "preAlpha 5";
	static final boolean FREEEDITION = false;
	static final boolean HDRENDERING = true;
	static final ArrayList<ICAOLatLon> ICAOLIST = new ArrayList<ICAOLatLon>();
	static final int[] BATTVOLTAGESCALE = new int[101];
	
	static final boolean DEBUG = TESTVER.equals("")?false:true; 
	
	static final boolean THEMESFROMCACHE = true;
	static final String FALLBACKTHEME = "{ \"id\": \"Fallback\", \"name\": \"FB\", \"author\": \"\", \"date\": \"\", \"credits\": \"\", \"layers_bottomtotop\": [ { \"name\": \"T\", \"type\": \"text\", \"enabled\": true, \"text\": \"%H:%M\", \"filename\": \"fallback.ttf\", \"x\": 240, \"y\": 100, \"fgcolor\": \"#ffffffff\", \"bgcolor\": \"#ff000000\", \"text_size\": 120, \"text_skew\": 0, \"text_stretch\": 1, \"text_align\": \"center\", \"render_style\": \"glow_5\", \"cw_rotate\": 0 }, { \"name\": \"E\", \"type\": \"text\", \"enabled\": true, \"text\": \"! Theme Loading / No SD Card !\", \"filename\": \"fallback.ttf\", \"x\": 240, \"y\": 118, \"fgcolor\": \"#ffffcccc\", \"bgcolor\": \"#ff000000\", \"text_size\": 28, \"text_skew\": 0, \"text_stretch\": 0.9, \"text_align\": \"center\", \"render_style\": \"glow_3\", \"cw_rotate\": 0 }, { \"name\": \"S\", \"type\": \"text\", \"enabled\": true, \"text\": \"[%ompc_battlevel%]%% - [%weather_city%] - [%weather_temp%] - [%weather_condition%]\", \"filename\": \"fallback.ttf\", \"x\": 240, \"y\": 142, \"fgcolor\": \"#ffffffff\", \"bgcolor\": \"#ff000000\", \"text_size\": 20, \"text_skew\": 0, \"text_stretch\": \"[%maxfit_1_300%]\", \"text_align\": \"center\", \"render_style\": \"glow_5\", \"cw_rotate\": 0 } ] }";
	static String THISVERSION; 
	static final boolean SINGLETON = false;
	static Bitmap PLACEHOLDERBMP;
	
	static final String SINGLETONNAME = "One More Clock";
	static final String STARTERPACKURL = "asset:pk141.omc";
	static final String EXTENDEDPACK = "https://sites.google.com/a/xaffron.com/xaffron-software/OMCThemes_v136.omc";
	static final String EXTENDEDPACKBACKUP = "https://s3.amazonaws.com/Xaffron/OMCThemes_v136.omc";
	static final String DEFAULTTHEME = "IceLock";
	static final String DEFAULTTHEMELONG = "Ice Lock";
	static final String APPICON = "clockicon";
	
	static final String[] APM = {"Ante Meridiem","Post Meridiem"};	
	static final Locale[] LOCALES = 
		{new Locale("zh","TW",""),new Locale("zh","CN",""),new Locale("es","ES",""),
		new Locale("de","",""),new Locale("en","US",""),new Locale("fr","",""),
		new Locale("iw","",""),new Locale("it","",""),new Locale("pl","",""),
		new Locale("pt","",""),new Locale("sv","","")};
	static final String[] LOCALENAMES = 
		{"繁體中文","简体中文","Castellano",
		"Deutsch","English (US)","Le Français",
		"עברית","Italiano","Język Polski",
		"Português","Svenska"};
	static SimpleDateFormat LOCALESDF;
	static Locale CURRENTLOCALE;

	//  NO NEED TO CHANGE BELOW THIS LINE FOR VERSIONING
	
	static final String FGSTRING = FREEEDITION?"com.sunnykwong.omc.FGSERVICEFREE":"com.sunnykwong.omc.FGSERVICEPAID";
	static final String BGSTRING = FREEEDITION?"com.sunnykwong.omc.BGSERVICEFREE":"com.sunnykwong.omc.BGSERVICEPAID";
	static final String CANCELFGSTRING = FREEEDITION?"com.sunnykwong.omc.CANCEL_FGFREE":"com.sunnykwong.omc.CANCEL_FGPAID";
	static final String WEATHERREFRESHSTRING = FREEEDITION?"com.sunnykwong.omc.WEATHERREFRESHFREE":"com.sunnykwong.omc.WEATHERREFRESHPAID";
	static final Intent WRINTENT = new Intent(WEATHERREFRESHSTRING);
	static final Intent FGINTENT = new Intent(FGSTRING);
	static final Intent BGINTENT = new Intent(BGSTRING);
	static final Intent WIDGETREFRESHINTENT = new Intent(FREEEDITION?"com.sunnykwong.freeomc.WIDGET_REFRESH":"com.sunnykwong.omc.WIDGET_REFRESH");
	static final IntentFilter PREFSINTENTFILT = new IntentFilter(FREEEDITION?"com.sunnykwong.freeomc.WIDGET_CONFIG":"com.sunnykwong.omc.WIDGET_CONFIG");
	static long lSUNRISEMILLIS, LSOLARNOONMILLIS, lSUNSETMILLIS;

	static final Intent FINDLAUNCHERINTENT = new Intent("android.intent.action.MAIN");
	static ArrayList<String> INSTALLEDLAUNCHERAPPS;
	
	static int faqtoshow = 0;
	static String[] FAQS;
	
	static final String APPNAME = OMC.SINGLETON? OMC.SINGLETONNAME:"One More Clock";
	static final String OMCSHORT = (OMC.SINGLETON? OMC.SINGLETONNAME.substring(0,4):"OMC") + (OMC.FREEEDITION? "free":"");

	static final String OMCNAME = "com.sunnykwong.omc";
	static String SHAREDPREFNAME;
	static String PKGNAME;
	static String[] VERBOSETIME1, VERBOSETIME2, VERBOSETIME3, VERBOSETIME4;
	static String[] VERBOSEDOW, SHORTDOW, VERBOSEMONTH, SHORTMONTH;
	static String[] VERBOSEWEATHER;
	static String[] VERBOSEWEATHERENG;
	static String[] VERBOSENUMBERS;
	static String DAYSUFFIX;
	static Context CONTEXT;
	static boolean SHOWHELP = true;
	static Uri PAIDURI;
	
	static int CURRENTCLOCKPRIORITY, CURRENTLOCATIONPRIORITY;
	static long LASTUPDATEMILLIS, LEASTLAGMILLIS=200;
	static long LASTWEATHERTRY=0l,LASTWEATHERREFRESH=0l,NEXTWEATHERREFRESH=0l,NEXTWEATHERREQUEST=0l;
	static int WEATHERREFRESHSTATUS;
	static long WEATHERREFRESHTIMESTAMP;
	static final int WRS_IDLE=0, WRS_LOCATION=1, WRS_GPS=2, WRS_NETWORK=3, WRS_DISABLED=4;
	static final int WRS_GEOCODE=5, WRS_PROVIDER=6, WRS_SUCCESS=7, WRS_FAILURE=8, WRS_CACHED=9, WRS_FIXED=10;
	
	static int LASTBATTERYPLUGGEDSTATUS=0;
	static long NEXTBATTSAVEMILLIS=0l;
	static int BATTLEVEL=0, BATTSCALE=100, BATTPERCENT=0;
	static String CHARGESTATUS = "Discharging";
	static String WEATHERTRANSLATETYPE = "AccuWeather";
	static String LASTKNOWNCITY, LASTKNOWNCOUNTRY;
	static JSONObject jsonFIXEDLOCN;
	static JSONObject GEOLOCNCACHE;
	
	static int UPDATEFREQ = 30000;
	static final Random RND = new Random();
	static SharedPreferences PREFS;
	static AlarmManager ALARMS;	// I only need one alarmmanager.
	static AssetManager AM;
	static ActivityManager ACTM;
    static NotificationManager NM;
	static PackageManager PKM;
    static Resources RES;
    static LocationManager LM;
    static LocationListener LL;

    static Typeface GEOFONT,WEATHERFONT;
    
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

	static Location LASTKNOWNLOCN = null;
	
	static int WIDGETWIDTH, WIDGETHEIGHT;
	static final String[] COMPASSPOINTS = {"NW","N","NE","W","C","E","SW","S","SE"};
	static final Time TIME = new Time();
	static Date DATE = new Date(); 
	static final Time LASTRENDEREDTIME = new Time();
	static String CACHEPATH;
	static String[] WORDNUMBERS;
	static JSONObject STRETCHINFO;
	static JSONObject WEATHERCONVERSIONS;
	static String[] OVERLAYURIS;
	static int[] OVERLAYRESOURCES;

	static final PorterDuffXfermode PORTERDUFF_XOR = new PorterDuffXfermode(Mode.XOR);
	static final PorterDuffXfermode PORTERDUFF_SRC_ATOP = new PorterDuffXfermode(Mode.SRC_ATOP);
	static final PorterDuffXfermode PORTERDUFF_DST_ATOP = new PorterDuffXfermode(Mode.DST_ATOP);
	static final PorterDuffXfermode PORTERDUFF_SRC_IN = new PorterDuffXfermode(Mode.SRC_IN);
	static final PorterDuffXfermode PORTERDUFF_DST_IN = new PorterDuffXfermode(Mode.DST_IN);
	static final PorterDuffXfermode PORTERDUFF_SRC_OUT = new PorterDuffXfermode(Mode.SRC_OUT);
	static final PorterDuffXfermode PORTERDUFF_DST_OUT = new PorterDuffXfermode(Mode.DST_OUT);
	static final PorterDuffXfermode PORTERDUFF_SRC_OVER = new PorterDuffXfermode(Mode.SRC_OVER);
	static final PorterDuffXfermode PORTERDUFF_DST_OVER = new PorterDuffXfermode(Mode.DST_OVER);
	static ComponentName WIDGET5x4CNAME;
	static ComponentName WIDGET5x2CNAME;
	static ComponentName WIDGET5x1CNAME;
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
	static final String clockImpls[][] = {
        {"HTC Alarm Clock", "com.htc.android.worldclock", "com.htc.android.worldclock.WorldClockTabControl" },
        {"Standard Alarm Clock", "com.android.deskclock", "com.android.deskclock.AlarmClock"},
        {"Froyo Alarm Clock", "com.android.alarmclock", "com.android.alarmclock.AlarmClock"},
        {"Froyo Nexus Alarm Clock", "com.google.android.deskclock", "com.android.deskclock.DeskClock"},
        {"Moto Blur Alarm Clock", "com.motorola.blur.alarmclock",  "com.motorola.blur.alarmclock.AlarmClock"},
        {"Samsung Galaxy S", "com.sec.android.app.clockpackage","com.sec.android.app.clockpackage.ClockPackage"},
        {"Sony Ericsson XPERIA X10 Mini Pro", "com.sec.android.app.clockpackage","com.sec.android.app.clockpackage.ClockPackage"} 
	};

	
	static final int SVCNOTIFICATIONID = 1; // Notification ID for the one and only message window we'll show
    static final Class<?>[] mStartForegroundSignature = new Class[] {int.class, Notification.class};
    static final Class<?>[] mStopForegroundSignature = new Class[] {boolean.class};
    static final Class<?>[] mSetForegroundSignature = new Class[] {boolean.class};
    static Intent SVCSTARTINTENT, CREDITSINTENT, PREFSINTENT, ALARMCLOCKINTENT, BATTUSAGEINTENT;
    static Intent GETSTARTERPACKINTENT, GETBACKUPPACKINTENT, GETEXTENDEDPACKINTENT, PICKTHEMEINTENT, DUMMYINTENT, OMCMARKETINTENT, OMCWEATHERFORECASTINTENT;
    static PendingIntent FGPENDING, BGPENDING, PREFSPENDING, ALARMCLOCKPENDING, WEATHERFORECASTPENDING, BATTUSAGEPENDING, WEATHERREFRESHPENDING;
    static Notification FGNOTIFICIATION;
    
    static final ArrayBlockingQueue<Matrix> MATRIXPOOL = new ArrayBlockingQueue<Matrix>(2);
    static final ArrayBlockingQueue<Paint> PAINTPOOL = new ArrayBlockingQueue<Paint>(2);
    static final ArrayBlockingQueue<Bitmap> WIDGETPOOL = new ArrayBlockingQueue<Bitmap>(2);
    
    static Bitmap ROTBUFFER;
    
    final Handler mHandler = new Handler();
    static String mMessage;
	final Runnable mPopToast = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(OMC.this, mMessage, Toast.LENGTH_LONG).show();
		}
	};
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		for (int i =0 ; i < OMC.LOCALES.length; i++) {
			if (OMC.PREFS.getString("appLocaleName", "English (US)").equals(OMC.LOCALENAMES[i])) {
				Log.i(OMC.OMCSHORT + "App","Using app locale: " + OMC.LOCALENAMES[i]);
				final Configuration config = new Configuration();
				OMC.CURRENTLOCALE = OMC.LOCALES[i];
				config.locale=OMC.CURRENTLOCALE;
				
				OMC.RES.updateConfiguration(config, 
						OMC.RES.getDisplayMetrics());
				break;
			}
		}
		OMC.CURRENTLOCALE = Locale.getDefault();

	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		OMC.CONTEXT = getApplicationContext();
		OMC.PKGNAME = getPackageName();
		OMC.SHAREDPREFNAME = OMC.PKGNAME + "_preferences";
    	OMC.PREFS = getSharedPreferences(SHAREDPREFNAME, Context.MODE_WORLD_READABLE);
    	OMC.CURRENTCLOCKPRIORITY = Integer.parseInt(OMC.PREFS.getString("clockPriority", "3"));
    	OMC.CURRENTLOCATIONPRIORITY = Integer.parseInt(OMC.PREFS.getString("locationPriority", "4"));
    	
		// Work around pre-Froyo bugs in HTTP connection reuse.
		if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
		    System.setProperty("http.keepAlive", "false");
		}
		// Define XML Parser.
		System.setProperty ("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver");

		try {
			OMC.THISVERSION = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName + " " + OMC.TESTVER;
		} catch (final NameNotFoundException e) {
			OMC.THISVERSION = "1.0.0";
		}

		OMC.PAIDURI = (OMC.SINGLETON? Uri.parse("market://details?id=" + OMC.PKGNAME +"donate"):Uri.parse("market://details?id=com.sunnykwong.omc"));

		OMC.WIDGET5x2CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget5x2");
		OMC.WIDGET5x4CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget5x4");
		OMC.WIDGET5x1CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget5x1");
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
		OMC.PICKTHEMEINTENT = new Intent(OMC.CONTEXT, OMCThemePickerActivity.class);
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
		OMC.OMCWEATHERFORECASTINTENT = new Intent(OMC.CONTEXT, OMCWeatherForecastActivity.class);
        OMC.WEATHERFORECASTPENDING = PendingIntent.getActivity(this, 0, OMC.OMCWEATHERFORECASTINTENT, 0);
        OMC.BATTUSAGEINTENT = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
        OMC.BATTUSAGEPENDING = PendingIntent.getActivity(OMC.CONTEXT, 0, OMC.BATTUSAGEINTENT, 0);
		OMC.ALARMCLOCKINTENT = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
		OMC.WEATHERREFRESHPENDING = PendingIntent.getBroadcast(OMC.CONTEXT, 0, OMC.WRINTENT, 0);
				
		OMC.CACHEPATH = this.getCacheDir().getAbsolutePath() + "/"; 
		
    	OMC.ALARMS = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
    	OMC.PKM = getPackageManager();
    	OMC.AM = getAssets();
    	OMC.RES = getResources();
//SUNNY
    	//  Insert educated guesses for battery voltage, if not already specified.
    	OMC.BATTVOLTAGESCALE[0]=3000;
    	OMC.BATTVOLTAGESCALE[10]=3600;
    	OMC.BATTVOLTAGESCALE[20]=3680;
    	OMC.BATTVOLTAGESCALE[30]=3712;
    	OMC.BATTVOLTAGESCALE[40]=3746;
    	OMC.BATTVOLTAGESCALE[50]=3773;
    	OMC.BATTVOLTAGESCALE[60]=3817;
    	OMC.BATTVOLTAGESCALE[70]=3870;
    	OMC.BATTVOLTAGESCALE[80]=3927;
    	OMC.BATTVOLTAGESCALE[90]=4004;
    	OMC.BATTVOLTAGESCALE[100]=4200;
//SUNNY    	
    	OMC.WIDGETWIDTH=Math.min(getResources().getDisplayMetrics().widthPixels,getResources().getDisplayMetrics().heightPixels);
    	OMC.WIDGETHEIGHT=OMC.WIDGETWIDTH;
        OMC.ROTBUFFER = Bitmap.createBitmap(OMC.WIDGETWIDTH, OMC.WIDGETHEIGHT, Bitmap.Config.ARGB_8888);


    	OMC.LM = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    	
    	OMC.LL = new LocationListener() {
            @Override
			public void onLocationChanged(final Location location) {
            	if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Weather", "Using Locn: " + location.getLongitude() + " + " + location.getLatitude());
            	if (location.getProvider().equals("gps")) {
            		OMC.WEATHERREFRESHSTATUS=OMC.WRS_GPS;
            	} else if (location.getProvider().equals("network")) {
            		OMC.WEATHERREFRESHSTATUS=OMC.WRS_NETWORK;
            	} else if (location.getProvider().equals("passive")) {
            		OMC.WEATHERREFRESHSTATUS=OMC.WRS_CACHED;
            	}
            	OMC.LM.removeUpdates(OMC.LL); 
            	OMC.LASTKNOWNLOCN=new Location(location);
        		final Thread t = new Thread() {
        			@Override
					public void run() {
        				OMC.calculateSunriseSunset(location.getLatitude(), location.getLongitude());
        				try {
        					GoogleReverseGeocodeService.updateLocation(location);
        	        		OMC.WEATHERREFRESHSTATUS=OMC.WRS_GEOCODE;

//       				 	v1.4.1:  Auto weather provider.
//    							if location is in US, switch to NOAA + METAR.
        	        		if (OMC.LASTKNOWNCOUNTRY.equals("United States") && OMC.PREFS.getString("weatherProvider", "auto").equals("auto")) {
        	    				OMC.PREFS.edit().putString("activeWeatherProvider", "noaa")
        	    				.putBoolean("weatherMETAR", true)
        	    				.commit();
        	    			}
        	    			
        					final String sWProvider = OMC.PREFS.getString("activeWeatherProvider", "seventimer");
        					if (sWProvider.equals("ig")) {
            					GoogleWeatherXMLHandler.updateWeather(location.getLatitude(), location.getLongitude(), OMC.LASTKNOWNCOUNTRY, OMC.LASTKNOWNCITY, true);
        					} else if (sWProvider.equals("yr")) {
        						YrNoWeatherXMLHandler.updateWeather(location.getLatitude(), location.getLongitude(), OMC.LASTKNOWNCOUNTRY, OMC.LASTKNOWNCITY, true);
        					} else if (sWProvider.equals("owm")) {
        						OpenWeatherMapJSONHandler.updateWeather(location.getLatitude(), location.getLongitude(), OMC.LASTKNOWNCOUNTRY, OMC.LASTKNOWNCITY, true);
        					} else if (sWProvider.equals("noaa")) {
        						NOAAWeatherXMLHandler.updateWeather(location.getLatitude(), location.getLongitude(), OMC.LASTKNOWNCOUNTRY, OMC.LASTKNOWNCITY, true);
        					} else {
        						SevenTimerJSONHandler.updateWeather(location.getLatitude(), location.getLongitude(), OMC.LASTKNOWNCOUNTRY, OMC.LASTKNOWNCITY, true);
        					}  
        					
        				} catch (final Exception e) {
        					e.printStackTrace();
        				}
        			}
        		};
        		t.start();
            }
            @Override
			public void onStatusChanged(final String provider, final int status, final Bundle extras) {}
		    @Override
			public void onProviderEnabled(final String provider) {}
		    @Override
			public void onProviderDisabled(final String provider) {}
		};

    	OMC.NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    	OMC.ACTM = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

		// Find the currently-installed launchers
		OMC.FINDLAUNCHERINTENT.addCategory("android.intent.category.HOME");
		
		final List<ResolveInfo> launcherlist = OMC.PKM.queryIntentActivities(OMC.FINDLAUNCHERINTENT, 0);
		OMC.INSTALLEDLAUNCHERAPPS = new ArrayList<String>();
		OMC.INSTALLEDLAUNCHERAPPS.add("com.teslacoilsw.widgetlocker");
		OMC.INSTALLEDLAUNCHERAPPS.add("com.jiubang.goscreenlock");
		
		for (final ResolveInfo info : launcherlist) {
			OMC.INSTALLEDLAUNCHERAPPS.add(info.activityInfo.packageName);
		}
    	
    	OMC.GEOFONT = Typeface.createFromAsset(OMC.AM, "GeosansLight.ttf");
    	OMC.WEATHERFONT = Typeface.createFromAsset(OMC.AM, "wef.ttf");
    	OMC.PLACEHOLDERBMP = BitmapFactory.decodeResource(OMC.RES, OMC.RDrawableId("transparent"));
    	
    	
		OMC.FG = OMC.PREFS.getBoolean("widgetPersistence", false)? true : false;
		if (!OMC.PREFS.contains("weathersetting")){
			OMC.PREFS.edit().putString("weathersetting", "bylatlong").commit();
		}
		
		OMC.FGNOTIFICIATION = new Notification(OMC.RDrawableId(OMC.APPICON), 
				"", 
        		System.currentTimeMillis());
        OMC.FGNOTIFICIATION.flags = OMC.FGNOTIFICIATION.flags|Notification.FLAG_ONGOING_EVENT|Notification.FLAG_NO_CLEAR;
		
		OMC.LASTWEATHERTRY = OMC.PREFS.getLong("weather_lastweathertry", 0l);
		OMC.LASTWEATHERREFRESH = OMC.PREFS.getLong("weather_lastweatherrefresh", 0l);
		OMC.NEXTWEATHERREFRESH = OMC.PREFS.getLong("weather_nextweatherrefresh", 0l);
		OMC.NEXTWEATHERREQUEST = OMC.PREFS.getLong("weather_nextweatherrequest", 0l);
		OMC.BATTLEVEL = OMC.PREFS.getInt("ompc_battlevel", 0);
		OMC.BATTSCALE = OMC.PREFS.getInt("ompc_battscale", 100);
		OMC.BATTPERCENT = OMC.PREFS.getInt("ompc_battpercent", 0);
		OMC.CHARGESTATUS = OMC.PREFS.getString("ompc_chargestatus", "Discharging");
		
		final Time t = new Time();
		t.setToNow();
		t.hour=6;
		t.minute=0;
		t.second=0;
		OMC.lSUNRISEMILLIS = t.toMillis(false);
		OMC.LSOLARNOONMILLIS = OMC.lSUNRISEMILLIS+360*60000l;
		OMC.lSUNSETMILLIS = OMC.LSOLARNOONMILLIS+360*60000l;
		
		
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
			OMC.STARTERPACKDLED = false;
			OMC.SHOWHELP=true;
			final Editor ed = OMC.PREFS.edit();
			ed.putBoolean("showhelp", true)
				.putBoolean("starterpack", false);
			ed.putString("weatherProvider", "auto");
			ed.putBoolean("weatherMETAR", true);
			ed.commit();
		}

		// If paid and OMWPP installed, disable ads; otherwise, enable them
		final File noads = new File("/sdcard/Android/data/com.sunnykwong.omwpp/files/.noads");
		if (!OMC.FREEEDITION) {
	        try {
	        	if (!noads.exists()) noads.createNewFile();
	        } catch (final Exception e) {
	        	e.printStackTrace();
	        }
		} else {
	        try {
	        	if (noads.exists()) noads.delete();
	        } catch (final Exception e) {
	        	e.printStackTrace();
	        }
		}
		//Alarm Clock Intent - Thanks frusso for the shared code!
		// http://stackoverflow.com/questions/3590955/intent-to-launch-the-clock-application-on-android/4281243#4281243
	    boolean foundClockImpl = false;

	    for(int i=0; i<clockImpls.length; i++) {
	        final String vendor = clockImpls[i][0];
	        final String packageName = clockImpls[i][1];
	        final String className = clockImpls[i][2];
	        try {
	            final ComponentName cn = new ComponentName(packageName, className);
	            PKM.getActivityInfo(cn, PackageManager.GET_META_DATA);
	            OMC.ALARMCLOCKINTENT.setComponent(cn);
	            OMC.ALARMCLOCKINTENT.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","Found " + vendor + " --> " + packageName + "/" + className);
	            foundClockImpl = true; 
	            break;
	        } catch (final NameNotFoundException e) {
	        	Log.w(OMC.OMCSHORT + "App",vendor + " does not exist");
	        }
	    }

	    if (!foundClockImpl) {
	    	OMC.ALARMCLOCKINTENT = OMC.DUMMYINTENT;
	    }
        OMC.ALARMCLOCKPENDING = PendingIntent.getActivity(this, 0, OMC.ALARMCLOCKINTENT, 0);
		
		OMC.PREFS.edit().putString("version", OMC.THISVERSION).commit();
		OMC.UPDATEFREQ = OMC.PREFS.getInt("iUpdateFreq", 30) * 1000;

		// These are two system intents that Android forces us to register programmatically
		// We register everything we can through manifest because ICS's ActivityManager will
		// randomly wipe out OMC's registered receivers when it kills OMC on low memory.
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_SCREEN_ON));
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		
		OMC.TYPEFACEMAP = new HashMap<String, Typeface>(3);
		OMC.BMPMAP = new HashMap<String, Bitmap>(5);
		OMC.THEMEMAP=Collections.synchronizedMap(new HashMap<String, JSONObject>(2));
		OMC.BMPTOCVAS = new HashMap<Bitmap, Canvas>(3);
		OMC.WIDGETBMPMAP = new HashMap<Integer, Bitmap>(3);
		
		OMC.STRETCHINFO = null;
		try {
			OMC.jsonFIXEDLOCN = new JSONObject(OMC.PREFS.getString("weather_fixedlocation", "{}"));
			OMC.WEATHERCONVERSIONS = streamToJSONObject(OMC.AM.open("weathericons/weathertranslation.json"));
	    	OMC.GEOLOCNCACHE = new JSONObject(OMC.PREFS.getString("geoLocnCache", "{}"));
	    	parseICAOMap();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		OMC.OVERLAYURIS = null;
		OMC.OVERLAYRESOURCES = new int[] {
				OMC.RId("NW"),
				OMC.RId("N"),
				OMC.RId("NE"),
				OMC.RId("W"),
				OMC.RId("C"),
				OMC.RId("E"),
				OMC.RId("SW"),
				OMC.RId("S"),
				OMC.RId("SE")
		};


		// Load locale-specific resources.
		
		for (int i =0 ; i < OMC.LOCALES.length; i++) {
			if (OMC.PREFS.getString("clockLocaleName", "English (US)").equals(OMC.LOCALENAMES[i])) {
				Log.i(OMC.OMCSHORT + "App","Using clock locale: " + OMC.LOCALENAMES[i]);

				OMC.WORDNUMBERS = OMC.RStringArray("WordNumbers", OMC.LOCALES[i]);
				OMC.VERBOSETIME1 = OMC.RStringArray("verbosetime1", OMC.LOCALES[i]);
				OMC.VERBOSETIME2 = OMC.RStringArray("verbosetime2", OMC.LOCALES[i]);
				OMC.VERBOSETIME3 = OMC.RStringArray("verbosetime3", OMC.LOCALES[i]);
				OMC.VERBOSETIME4 = OMC.RStringArray("verbosetime4", OMC.LOCALES[i]);
				OMC.VERBOSEWEATHER = OMC.RStringArray("VerboseWeather", OMC.LOCALES[i]);
				OMC.VERBOSENUMBERS = OMC.RStringArray("WordNumbers", OMC.LOCALES[i]);
				OMC.VERBOSEDOW = OMC.RStringArray("verbosedow", OMC.LOCALES[i]);
				OMC.SHORTDOW = OMC.RStringArray("shortdow", OMC.LOCALES[i]);
				OMC.VERBOSEMONTH = OMC.RStringArray("verbosemonth", OMC.LOCALES[i]);
				OMC.SHORTMONTH = OMC.RStringArray("shortmonth", OMC.LOCALES[i]);
				OMC.DAYSUFFIX = OMC.RString("daysuffix", OMC.LOCALES[i]);
				
			}  
			if (OMC.LOCALENAMES[i].equals("English (US)")){
				OMC.VERBOSEWEATHERENG = OMC.RStringArray("VerboseWeather", OMC.LOCALES[i]);
			}
		}

		OMC.TIME.setToNow();
		OMC.TIME.hour=0;
		OMC.TIME.minute=0;
		
		while (OMC.MATRIXPOOL.remainingCapacity() > 0 ) OMC.MATRIXPOOL.add(new Matrix());
		while (OMC.PAINTPOOL.remainingCapacity() > 0 ) OMC.PAINTPOOL.add(new Paint());
		while (OMC.WIDGETPOOL.remainingCapacity() > 0 ) {
			final Bitmap bmp = Bitmap.createBitmap(OMC.WIDGETWIDTH, OMC.WIDGETHEIGHT, Bitmap.Config.ARGB_8888);
			final Canvas cvas = new Canvas(bmp);
			OMC.WIDGETPOOL.add(bmp);
			OMC.BMPTOCVAS.put(bmp, cvas);
		}
		OMC.BMPTOCVAS.put(OMC.ROTBUFFER, new Canvas(OMC.ROTBUFFER));
		
		Log.i(OMC.OMCSHORT + "App","Starting up... default theme is " + OMC.PREFS.getString("widgetTheme", "MISSING"));
		setupDefaultTheme();
		this.widgetClicks();
		OMC.toggleWidgets(this);

		//v1.3.0 kickstart the widget!
		OMC.setServiceAlarm(System.currentTimeMillis()+500l, (System.currentTimeMillis()+500l)/1000l*1000l);
	}
	
	static public JSONObject streamToJSONObject(final InputStream is) throws IOException {
		final InputStreamReader isr = new InputStreamReader(is);
		final BufferedReader br = new BufferedReader(isr,8192);
		final StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null){
			sb.append(line+"\n");
		}
		isr.close();
		br.close();
		try {
			return new JSONObject(sb.toString());
		} catch (final JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	static public String streamToString(final InputStream is) throws IOException {
		final InputStreamReader isr = new InputStreamReader(is);
		final BufferedReader br = new BufferedReader(isr,8192);
		final StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null){
			sb.append(line+"\n");
		}
		isr.close();
		br.close();
		return sb.toString();
	}

	static public void toggleWidgets(final Context context) {
			
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
					OMC.WIDGET5x4CNAME,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET5x2CNAME,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET5x1CNAME,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
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
					OMC.WIDGET5x4CNAME,
					OMC.PREFS.getBoolean("bFiveByFour", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET5x2CNAME,
					OMC.PREFS.getBoolean("bFiveByTwo", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET5x1CNAME,
					OMC.PREFS.getBoolean("bFiveByOne", true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
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

	static void setServiceAlarm (final long lTimeToRefresh, final long lTargetTime) {
		//We want the pending intent to be for this service, and 
		// at the same FG/BG preference as the intent that woke us up
		final SimpleDateFormat mill = new SimpleDateFormat("HH:mm:ss.SSS");
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","ALARM SET: @"+ mill.format(new Date(lTimeToRefresh))+ " for " + mill.format(new Date(lTargetTime)));
//		int counter=0;
//		for (StackTraceElement e: Thread.currentThread().getStackTrace()) {
//			if (counter++<2) continue;
//			if (OMC.DEBUG) Log.d(OMC.OMCSHORT + "App","   " + e);
//			if (counter>5) break;
//		}

		final int iAlarmSetting = OMC.CURRENTCLOCKPRIORITY>1?AlarmManager.RTC:AlarmManager.RTC_WAKEUP;
		if (OMC.FG) {
			OMC.FGINTENT.putExtra("target", lTargetTime);
			OMC.ALARMS.set(iAlarmSetting, lTimeToRefresh, OMC.FGPENDING);
		} else {
			OMC.BGINTENT.putExtra("target", lTargetTime);
			OMC.ALARMS.set(iAlarmSetting, lTimeToRefresh, OMC.BGPENDING);
		}
    }

	public static void setPrefs(final int aWI) {
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","Committing prefs for widget " + aWI);
		final Editor e = OMC.PREFS.edit();
		e.putString("widgetTheme"+aWI, OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME))
		.putString("widgetThemeLong"+aWI, OMC.PREFS.getString("widgetThemeLong", OMC.DEFAULTTHEMELONG))
		.putBoolean("widget24HrClock"+aWI, OMC.PREFS.getBoolean("widget24HrClock", true))
		.putBoolean("widgetLeadingZero"+aWI, OMC.PREFS.getBoolean("widgetLeadingZero", true))
		.putBoolean("mmddDateFormat"+aWI, OMC.PREFS.getBoolean("mmddDateFormat", true))
		.putString("sTimeZone"+aWI, OMC.PREFS.getString("sTimeZone", "default"));
		for (int i=0;i<9;i++) {
			e.putString("URI"+OMC.COMPASSPOINTS[i]+aWI, OMC.PREFS.getString("URI"+OMC.COMPASSPOINTS[i], ""));
		}
		e.commit();
	}
	 
	public static void initPrefs(final int aWI) {
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","Wiping out/Resetting prefs for widget " + aWI);
		// For new clocks... just like setPrefs but leaves the URI empty.
		final Editor e = OMC.PREFS.edit();
		e.putString("widgetTheme"+aWI, OMC.PREFS.getString("widgetTheme"+aWI, OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME)))
		.putString("widgetThemeLong"+aWI, OMC.PREFS.getString("widgetThemeLong"+aWI, OMC.PREFS.getString("widgetThemeLong", OMC.DEFAULTTHEMELONG)))
		.putBoolean("widget24HrClock"+aWI, OMC.PREFS.getBoolean("widget24HrClock"+aWI, OMC.PREFS.getBoolean("widget24HrClock", true)))
		.putBoolean("widgetLeadingZero"+aWI, OMC.PREFS.getBoolean("widgetLeadingZero"+aWI, OMC.PREFS.getBoolean("widgetLeadingZero", true)))
		.putBoolean("mmddDateFormat"+aWI, OMC.PREFS.getBoolean("mmddDateFormat"+aWI, OMC.PREFS.getBoolean("mmddDateFormat", true)))
		.putString("sTimeZone"+aWI, OMC.PREFS.getString("sTimeZone"+aWI, "default"));
		for (int i=0;i<9;i++) {
			e.putString("URI"+OMC.COMPASSPOINTS[i]+aWI, OMC.PREFS.getString("URI"+OMC.COMPASSPOINTS[i], ""));
		}
		e.commit();
	}
 
	
	public static void getPrefs(final int aWI) {
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","Retrieving prefs for widget " + aWI);
		final Editor e = OMC.PREFS.edit();
		e.putString("widgetTheme", OMC.PREFS.getString("widgetTheme"+aWI, OMC.DEFAULTTHEME))
		.putString("widgetThemeLong", OMC.PREFS.getString("widgetThemeLong"+aWI, OMC.DEFAULTTHEMELONG))
		.putBoolean("widget24HrClock", OMC.PREFS.getBoolean("widget24HrClock"+aWI, true))
		.putBoolean("widgetLeadingZero", OMC.PREFS.getBoolean("widgetLeadingZero"+aWI, true))
		.putBoolean("mmddDateFormat", OMC.PREFS.getBoolean("mmddDateFormat"+aWI, true))
		.putString("URI", OMC.PREFS.getString("URI"+aWI, ""))
		.putString("sTimeZone", OMC.PREFS.getString("sTimeZone"+aWI, "default"));
		for (int i=0;i<9;i++) {
			e.putString("URI"+OMC.COMPASSPOINTS[i], OMC.PREFS.getString("URI"+OMC.COMPASSPOINTS[i]+aWI, ""));
		}
		e.commit();
	}
	
	public static void removePrefs(final int aWI) {
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","Deleting prefs for widget " + aWI);
		final Editor e = OMC.PREFS.edit();
		e.remove("widgetTheme"+aWI)
			.remove("widgetThemeLong"+aWI)
			.remove("widget24HrClock"+aWI)
			.remove("widgetLeadingZero"+aWI)
			.remove("mmddDateFormat"+aWI)
			.remove("URI"+aWI)
			.remove("sTimeZone"+aWI);
		for (int i=0;i<9;i++) {
			e.remove("URI"+OMC.COMPASSPOINTS[i]+aWI);
		}
		e.commit();
		final File f = new File(OMC.CACHEPATH + aWI +"cache.png");
		if (f.exists())f.delete();
	}
	
	public static Typeface getTypeface(final String sTheme, final String src) {
		if (src.equals("wef.ttf")) return OMC.WEATHERFONT;
		if (OMC.THEMESFROMCACHE) {
			//Look in memory cache;
			if (OMC.TYPEFACEMAP.get(src)!=null) {
				return OMC.TYPEFACEMAP.get(src);
			}
			//Look in app cache;
			if (new File(OMC.CACHEPATH + sTheme + src).exists()) {
					try {
						OMC.TYPEFACEMAP.put(src, Typeface.createFromFile(OMC.CACHEPATH + sTheme + src));
						return OMC.TYPEFACEMAP.get(src);
					} catch (final RuntimeException e) {
						// if Cache is invalid, do nothing; we'll let this flow through to the full FS case.
					}
			}
		}
		//Look in full file system;
		if (new File(src).exists()) {
				try {
					OMC.TYPEFACEMAP.put(src, Typeface.createFromFile(src));
					return OMC.TYPEFACEMAP.get(src);
				} catch (final RuntimeException e) {
					// if Cache is invalid, do nothing; we'll let this flow through to the full SD case.
				}
		}
		//Look in sd card;
		if (OMC.checkSDPresent()) {
			final File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/"+sTheme+"/"+src);
			try {
				if (f.exists()) {
					copyFile(f.getAbsolutePath(),OMC.CACHEPATH +"/"+sTheme+f.getName());
					OMC.TYPEFACEMAP.put(src, Typeface.createFromFile(f));
					return OMC.TYPEFACEMAP.get(src);
				}
			} catch (final RuntimeException e) {
				// if Cache is invalid, do nothing; we'll let this flow through to the fallback case.
			}
		}
		//Look in assets.
		Typeface tf = null;
		// New fix 1.2.8:  For phones without the DroidSans.ttf in /system/fonts, we return the system fallback font.
		try {
			tf = Typeface.createFromAsset(OMC.AM, "defaulttheme/"+src);
		} catch (final Exception e) {
			tf = Typeface.DEFAULT;
		}
		return tf;
	}

	public static Bitmap getBitmap(final String sTheme, final String src) {
		if (src.startsWith("ww-")) {
			if (checkSDPresent()) {
				OMC.WEATHERTRANSLATETYPE="AccuWeather";
				File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/zz_WeatherSkin/accuweather.type");
				if (f.exists()) {
					OMC.WEATHERTRANSLATETYPE = "AccuWeather";
				}
				f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/zz_WeatherSkin/weatherdotcom.type");
				if (f.exists()) {
					OMC.WEATHERTRANSLATETYPE = "WeatherDotCom";
				}
			}
			// Translate from condition to filename
			final String[] sTokens = src.split("[-.]");
			int iConditionCode = 0;
			if (sTokens[1]==null) iConditionCode = 0;
			else if (sTokens[1].equals("")) iConditionCode = 0;
			else {
				iConditionCode = Integer.parseInt(sTokens[1].trim()); 
			}
			final String daynight=sTokens[2];;
			String src2;
			try {
				src2 = OMC.WEATHERCONVERSIONS.getJSONObject(OMC.WEATHERTRANSLATETYPE)
							.getJSONArray(daynight).optString(iConditionCode,"Unk").toLowerCase();
			} catch (final JSONException e) {
				e.printStackTrace();
				src2 = "Unk";
			}
			src2+=".png";
			return getBitmap(sTheme, src2);
		}

		if (src.startsWith("w-")) {
			if (checkSDPresent()) {
				OMC.WEATHERTRANSLATETYPE="AccuWeather";
				File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/zz_WeatherSkin/accuweather.type");
				if (f.exists()) {
					OMC.WEATHERTRANSLATETYPE = "AccuWeather";
				}
				f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/zz_WeatherSkin/weatherdotcom.type");
				if (f.exists()) {
					OMC.WEATHERTRANSLATETYPE = "WeatherDotCom";
				}
			}
			// Translate from condition to filename
			final String[] sTokens = src.split("[-.]");

			final String condition = sTokens[1].toLowerCase(), daynight=sTokens[2];;
			int iTarget = 0;
			for (int idx=0;idx<OMC.VERBOSEWEATHERENG.length;idx++) {

				final String tempTarget = OMC.VERBOSEWEATHERENG[idx].toLowerCase();
				if (tempTarget.equals(condition)) {
					iTarget=idx;
					break;
				}
			}
			String src2;
			try {
				src2 = OMC.WEATHERCONVERSIONS.getJSONObject(OMC.WEATHERTRANSLATETYPE)
							.getJSONArray(daynight).optString(iTarget,"Unk").toLowerCase();
			} catch (final JSONException e) {
				e.printStackTrace();
				src2 = "Unk";
			}
			src2+=".png";
			return getBitmap(sTheme, src2);
		}

		if (OMC.THEMESFROMCACHE) {
			//Look in memory cache;
			if (OMC.BMPMAP.get(src)!=null) {
				return OMC.BMPMAP.get(src);
			}
	
			//Look in app cache;
			if (new File(OMC.CACHEPATH + sTheme + src).exists()) {
				OMC.BMPMAP.put(src, BitmapFactory.decodeFile(OMC.CACHEPATH + sTheme + src));
				return OMC.BMPMAP.get(src);
			}
		}
		// Look in SD path
		if (OMC.checkSDPresent()) {
			File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/"+sTheme+"/"+src);
			if (f.exists()) {
				copyFile(f.getAbsolutePath(),OMC.CACHEPATH +"/"+sTheme+f.getName());
				OMC.BMPMAP.put(src, BitmapFactory.decodeFile(f.getAbsolutePath()));
				return OMC.BMPMAP.get(src);
			}
			f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/zz_WeatherSkin/"+src);
			if (f.exists()) {
				copyFile(f.getAbsolutePath(),OMC.CACHEPATH +"/"+sTheme+f.getName());
				OMC.BMPMAP.put(src, BitmapFactory.decodeFile(f.getAbsolutePath()));
				return OMC.BMPMAP.get(src);
			}
		} 
		// Look in assets
		try {
			return BitmapFactory.decodeStream(OMC.AM.open("defaulttheme/"+src));
		} catch (final Exception e) {
			// Asset not found - do nothing
			try {
				return BitmapFactory.decodeStream(OMC.AM.open("weathericons/"+src));
			} catch (final Exception ee) {
				
			}
		}
		// Bitmap can't be found anywhere; return a transparent png
		return BitmapFactory.decodeResource(OMC.RES, OMC.RDrawableId("transparent"));
	}

	public static void purgeTypefaceCache(){
		final Iterator<Entry<String,Typeface>> i = OMC.TYPEFACEMAP.entrySet().iterator();
		while (i.hasNext()) {
			final Entry<String,Typeface> entry = i.next();
			entry.setValue(null);
		}
		OMC.TYPEFACEMAP.clear();
	}
	
	public static void purgeBitmapCache(){
		final Iterator<Entry<String,Bitmap>> i = OMC.BMPMAP.entrySet().iterator();
		while (i.hasNext()) {
			final Entry<String,Bitmap> entry = i.next();
			if (entry.getValue()!=null)	entry.getValue().recycle();
			entry.setValue(null);
		}
		OMC.BMPMAP.clear();
	}
	
	public synchronized static JSONObject getTheme(final Context context, final String nm, final boolean bFromCache){
		// Look in memory cache
		if (OMC.THEMEMAP.containsKey(nm) && OMC.THEMEMAP.get(nm)!=null && bFromCache){ 
//			if (OMC.DEBUG) Log.d(OMC.OMCSHORT + "App",nm + " loaded from mem.");
			return OMC.THEMEMAP.get(nm);
		}
		// Look in cache dir
		if (new File(OMC.CACHEPATH + nm + "00control.json").exists() && bFromCache) {
			try {
				final BufferedReader in = new BufferedReader(new FileReader(OMC.CACHEPATH + nm + "00control.json"),8192);
				final StringBuilder sb = new StringBuilder();
			    final char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    
				final JSONObject oResult = new JSONObject(sb.toString());
				sb.setLength(0);
				OMC.THEMEMAP.put(nm, oResult);
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App",nm + " loaded from cachedir.");
				return oResult;
			} catch (final Exception e) {
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","error reloading " + nm + " from cachedir.");
				e.printStackTrace();
			} finally {
				//System.gc();
			}
		}
		// Look in SD path
		if (OMC.checkSDPresent()) {
			final File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/"+nm);
			if (f.exists() && f.isDirectory()) {
				for (final File ff:f.listFiles()) {
					copyFile(ff.getAbsolutePath(),OMC.CACHEPATH + nm + ff.getName());
				}
			}
			try {
				final BufferedReader in = new BufferedReader(new FileReader(OMC.CACHEPATH + nm + "00control.json"),8192);
				final StringBuilder sb = new StringBuilder();
			    final char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
				final JSONObject oResult = new JSONObject(sb.toString());
				sb.setLength(0);
				if (!OMC.validateTheme(oResult)) throw new Exception();
				OMC.THEMEMAP.put(nm, oResult);
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App",nm + " loaded from SD.");
				return oResult;
			} catch (final Exception e) {
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","error loading " + nm + " from SD.");
			} finally {
				//System.gc();
			}

		} 
		// If default theme, look in assets
		if (nm.equals(OMC.DEFAULTTHEME)) {
			try {
				final InputStreamReader in = new InputStreamReader(OMC.AM.open("defaulttheme/00control.json"));
				final StringBuilder sb = new StringBuilder();
			    final char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    
				final JSONObject oResult = new JSONObject(sb.toString());
				sb.setLength(0);
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App",nm + " loaded from assets.");
				return oResult;
			} catch (final Exception e) {
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","error reloading " + nm + " from assets.");
				e.printStackTrace();
			} finally {
				//System.gc();
			}
			
		}
		JSONObject fb=null;
		try {
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App",nm + " not available; drop to fallback.");
			fb = new JSONObject(FALLBACKTHEME);
		} catch (final JSONException e) {
			e.printStackTrace();
		}
		return fb;
	}

	public static void themeToFile(final JSONObject obj, final File tgt) {
		try {
			final BufferedWriter out = new BufferedWriter(new FileWriter(tgt),8192);
			out.write(obj.toString(5));
			out.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public static void bmpToJPEG(final Bitmap bmp, final File tgt) {
		try {
		       final FileOutputStream out = new FileOutputStream(tgt);
		       Bitmap.createScaledBitmap(Bitmap.createBitmap(bmp,0,0,480,300),320,200,true).compress(Bitmap.CompressFormat.JPEG, 85, out);
		       out.close();
		} catch (final Exception e) {
		       e.printStackTrace();
		}
	}	
	public static void purgeImportCache() {
		for (final File f:(new File(OMC.CACHEPATH).listFiles())) {
			f.delete();
		}
		//System.gc();
	}
	public static void purgeEmailCache() {
		final File tempDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/tmp/");
		if (!tempDir.exists()) return;
		else {
			for (final File f:(tempDir.listFiles())) {
				f.delete();
			}
		}
		//System.gc();
	}
			
	public static void removeDirectory(final File f) {
		for (final File ff:f.listFiles()) {
			if (ff.equals(f)) continue;
	    	if (!ff.isDirectory()) ff.delete();
	    	else removeDirectory(ff);
		}
		f.delete();
	}

	public static void removeFile(final File f) {
		if (f.exists()) {
			if (!f.isDirectory()) f.delete();
		}
	}
	
	public boolean fixKnownBadThemes() {
		final Thread t = new Thread() {
			@Override
			public void run() {
				final String[] badThemes = new String[]{"iPhone"};
				for (final String theme:badThemes) {
					final File badThemeFile = new File( Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + theme + "/00control.json");
					if (!badThemeFile.exists()) continue;
					final File fixFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + theme + "/fixed.txt");
					if (fixFile.exists()) continue;
					try {
						FileWriter fw = new FileWriter(badThemeFile);
						fw.write(OMC.FALLBACKTHEME);
						fw.close();
						fw = new FileWriter(fixFile);
						fw.write("FIXED");
						fw.close();
						mMessage = "A corrupt theme (iPhone) was found and replaced.";
						mHandler.post(mPopToast);
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		t.start();
		return true;
	}
	
	public static void copyDirectory(final File src, final File tgt) {
		if (!tgt.exists()) tgt.mkdirs();
		if (!tgt.isDirectory()) return;

		for (final File ff:src.listFiles()) {
			if (ff.isDirectory()) {
				copyDirectory(ff,new File(tgt.getAbsolutePath()+"/"+ff.getName()));
			} else {
				copyFile(ff.getAbsolutePath(), tgt.getAbsolutePath()+"/"+ff.getName());
			}
		}
	}
	
	public static boolean copyFile(final String src, final String tgt) {
		try {
			final FileOutputStream oTGT = new FileOutputStream(tgt);
			final FileInputStream oSRC = new FileInputStream(src);
		    final byte[] buffer = new byte[8192];
		    int iBytesRead = 0;
		    while ((iBytesRead = oSRC.read(buffer))!= -1){
		    	oTGT.write(buffer,0,iBytesRead);
		    }
		    oTGT.close();
		    oSRC.close();
			return true;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean copyFile(final InputStream oSRC, final OutputStream oTGT) {
		try {
		    final byte[] buffer = new byte[8192];
		    int iBytesRead = 0, iByteCount=0;
		    while ((iBytesRead = oSRC.read(buffer))!= -1){
		    	iByteCount+=iBytesRead;
		    	oTGT.write(buffer,0,iBytesRead);
			    System.out.println(buffer.toString() + " copied to memfile.");
		    }
		    oTGT.close();
		    oSRC.close();
			return true;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean copyAssetToCache(final String src, final String filename, final String sTheme) {
		try {
			final FileOutputStream oTGT = new FileOutputStream(OMC.CACHEPATH + sTheme + filename);
			final InputStream oSRC = OMC.AM.open(src);
			
		    final byte[] buffer = new byte[8192];
		    int iBytesRead = 0;
		    while ((iBytesRead = oSRC.read(buffer))!= -1){
		    	oTGT.write(buffer,0,iBytesRead);
		    }
		    oTGT.close();
		    oSRC.close();
			return true;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static JSONArray loadStringArray(final String sTheme, final int aWI, final String sKey) {	
		try {
			final JSONObject oTheme = OMC.THEMEMAP.get(sTheme);
			return oTheme.getJSONObject("arrays").getJSONArray(sKey);

		} catch (final Exception e) {
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
			} catch (final java.lang.IllegalArgumentException e) {
    			if (OMC.DEBUG)Log.i(OMC.OMCSHORT + "App","OMC's receiver already unregistered - doing nothing");
				//no need to do anything if receiver not registered
			}
		} catch (final Exception e) {

			if (OMC.DEBUG)Log.i(OMC.OMCSHORT + "App","OMPC not installed, register self to handle widget clicks");
			//e.printStackTrace();
			try {
				this.registerReceiver(OMC.cRC,OMC.PREFSINTENTFILT);
			} catch (final Exception ee) {
    			if (OMC.DEBUG)Log.i(OMC.OMCSHORT + "App","Failed to register self");
				ee.printStackTrace();
			}
		}
		

	}
	
	public static void setupDefaultTheme() {
		final String sDefaultThemeAssetDir = "defaulttheme/";
		try {
			if (new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/"+ OMC.DEFAULTTHEME + "/00control.json").exists()) return;
			(new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/"+ OMC.DEFAULTTHEME)).mkdirs();
			for (final String sFile : OMC.AM.list("defaulttheme")) {
				copyAssetToCache(sDefaultThemeAssetDir+sFile,sFile, OMC.DEFAULTTHEME);
				copyFile(OMC.CACHEPATH + OMC.DEFAULTTHEME + sFile, Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes/" + OMC.DEFAULTTHEME + "/" + sFile);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
	public static boolean validateTheme(final JSONObject theme) {	
		
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
		} catch (final JSONException e) {
			valid = false;
		}
		
		return valid;
		
    }
	
	public static boolean checkSDPresent() {
    	
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			return false;
        }

        final File sdRoot = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.OMCThemes");
        if (!sdRoot.exists()) {
        	sdRoot.mkdir();
        }
		if (!sdRoot.canRead()) {
        	return false;
        }
		return true;
    }

	static public JSONObject renderThemeObject(final JSONObject theme, final int aWI) throws JSONException {
		JSONObject result;
		
		result = new JSONObject();

		// Since the rendered object is only used for drawing, we'll skip
		// ID, Name, Author and Credits
		
		// First, render the dynamic elements in arrays
		
		if (theme.has("arrays")) {
			final JSONObject resultArrays = new JSONObject();
			result.put("arrays", resultArrays);
			if (theme.has("translations")) {
				result.put("translations", theme.optJSONObject("translations"));
			}
			
			@SuppressWarnings("unchecked")
			final
			Iterator<String> i = theme.optJSONObject("arrays").keys();
			while (i.hasNext()) {
				final String sKey = i.next();
				final JSONArray tempResultArray = new JSONArray();
				resultArrays.put(sKey, tempResultArray);

				final JSONArray tempArray = theme.optJSONObject("arrays").optJSONArray(sKey);
				for (int j=0;j<tempArray.length();j++) {
					tempResultArray.put(OMC.resolveTokens(tempArray.getString(j), aWI, result));
				}
			}
		}

		// Then, render all the dynamic elements in each layer...
		
		final JSONArray layerJSONArray = theme.optJSONArray("layers_bottomtotop");
		if (layerJSONArray==null) return null; //ERR: A theme cannot have no layers
		final JSONArray tempLayerArray = new JSONArray();
		result.put("layers_bottomtotop", tempLayerArray);
		
		for (int j = 0 ; j < layerJSONArray.length(); j++) {
			final JSONObject layer = layerJSONArray.optJSONObject(j);
			final JSONObject renderedLayer = new JSONObject();
			tempLayerArray.put(renderedLayer);
			
			//v1.3.1: If Layer is null, it's a corrupt layer in a corrupt theme.  
			// Try to make the best of it and move to the next layer.
			if (layer==null) continue;
			@SuppressWarnings("unchecked")
			final
			Iterator<String> i = layer.keys();
			while (i.hasNext()) {
				final String sKey = i.next();
				if (sKey.equals("text_stretch")) continue;

				renderedLayer.put(sKey, OMC.resolveTokens((layer.optString(sKey)), aWI, result));

			}
			
			// before tweaking the max/maxfit stretch factors.
			final String sStretch = layer.optString("text_stretch");
			if (sStretch==null) {
				renderedLayer.put("text_stretch", "1");
			} else {
				renderedLayer.put("text_stretch", OMC.resolveTokens((layer.optString("text_stretch")), aWI, result));
			}
		}

		

		
		return result;
	}
	
	static public String resolveOneToken(final String sRawString, final int aWI, final JSONObject tempResult) {
		boolean isDynamic = false;
		if (sRawString.contains("[%")) isDynamic = true;
		// Strip brackets.
		String sBuffer = sRawString.replace("[%", "").replace("%]", "");
		//by default, return strftime'd string.
		if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
			sBuffer = (OMC.OMCstrf(
				OMC.PREFS.getBoolean("widgetLeadingZero"+aWI, true)? 
						sBuffer : sBuffer.replaceAll("%H", "%k"),aWI
				)
			);
		} else {
			sBuffer = (OMC.OMCstrf(
				OMC.PREFS.getBoolean("widgetLeadingZero"+aWI, true)? 
						sBuffer.replaceAll("%H", "%I") : sBuffer.replaceAll("%H", "%l"),aWI
				)
			);
		}

		// If it's unbracketed text, we're done
		if (!isDynamic) return sBuffer;
		
		// If it is actually a dynamic element, resolve it
		// By default, 
		String result = "";

		final String[] st = sBuffer.split("_");
		int iTokenNum=0;

		//Get the first element (command).
		final String sToken = st[iTokenNum++];
		if (sToken.startsWith("daylight")) {

			// value that changes linearly from sunrise to sundown, then from sundown to sunup.
			final String sType = st[iTokenNum++];

			// Where are we in time?  intGradientSeconds starts the count from either sunrise or sunset
			final long OMCTIMEMillis = OMC.TIME.toMillis(false);

			int iIntervalSeconds;
			int iGradientSeconds;

			if (OMCTIMEMillis < OMC.lSUNRISEMILLIS) {
				//pre-dawn
				iIntervalSeconds = 86400-(int)((OMC.lSUNSETMILLIS-OMC.lSUNRISEMILLIS)/1000l);
				iGradientSeconds = (int)((OMCTIMEMillis - (OMC.lSUNSETMILLIS-86400000l))/1000l);
			} else if (OMCTIMEMillis < OMC.lSUNSETMILLIS) {
				//day
				iIntervalSeconds = (int)((OMC.lSUNSETMILLIS-OMC.lSUNRISEMILLIS)/1000l);
				iGradientSeconds = (int)((OMCTIMEMillis - OMC.lSUNRISEMILLIS)/1000l);
			} else {
				//post-dusk
				iIntervalSeconds = 86400-(int)((OMC.lSUNSETMILLIS-OMC.lSUNRISEMILLIS)/1000l);
				iGradientSeconds = (int)((OMCTIMEMillis - OMC.lSUNSETMILLIS)/1000l);
			}			
			final float gradient = (iGradientSeconds % iIntervalSeconds)/(float)iIntervalSeconds;
			final String sLowVal = st[iTokenNum++];
			final String sMidVal = st[iTokenNum++];
			final String sHighVal = st[iTokenNum++];
			if (sType.equals("number")) {
				result = String.valueOf(OMC.numberInterpolate(
						Integer.parseInt(sLowVal), 
						Integer.parseInt(sMidVal), 
						Integer.parseInt(sHighVal), 
						gradient));
			} else if (sType.equals("color")) {
				final int color = OMC.colorInterpolate(
						sLowVal, 
						sMidVal, 
						sHighVal, 
						gradient);
				result = String.valueOf("#" + Integer.toHexString(color));
			} else if (sType.equals("float")) {
				result = String.valueOf(OMC.floatInterpolate(
						Float.parseFloat(sLowVal), 
						Float.parseFloat(sMidVal), 
						Float.parseFloat(sHighVal), 
						gradient));
			} else {
				//Unknown - do nothing
			}
		} else if (sToken.startsWith("shift")) {

			// value that changes linearly and repeats every X seconds from 6am.
			final String sType = st[iTokenNum++];
			final int iIntervalSeconds =  Integer.parseInt(st[iTokenNum++]);
			int iGradientSeconds = 0;

			// Where are we in time?  intGradientSeconds starts the count from either 12am or 6am
			if (sToken.equals("shift12")) {
				iGradientSeconds = OMC.TIME.second + OMC.TIME.minute*60 + OMC.TIME.hour*3600;
			} else { 
				// (sToken.equals("shift6"))
				iGradientSeconds = OMC.TIME.second + OMC.TIME.minute*60 + ((OMC.TIME.hour+18)%24)*3600;
			} 

			final float gradient = (iGradientSeconds % iIntervalSeconds)/(float)iIntervalSeconds;
			final String sLowVal = st[iTokenNum++];
			final String sMidVal = st[iTokenNum++];
			final String sHighVal = st[iTokenNum++];
			if (sType.equals("number")) {
				result = String.valueOf(OMC.numberInterpolate(
						Integer.parseInt(sLowVal), 
						Integer.parseInt(sMidVal), 
						Integer.parseInt(sHighVal), 
						gradient));
			} else if (sType.equals("color")) {
				final int color = OMC.colorInterpolate(
						sLowVal, 
						sMidVal, 
						sHighVal, 
						gradient);
				result = String.valueOf("#" + Integer.toHexString(color));
			} else if (sType.equals("float")) {
				result = String.valueOf(OMC.floatInterpolate(
						Float.parseFloat(sLowVal), 
						Float.parseFloat(sMidVal), 
						Float.parseFloat(sHighVal), 
						gradient));
			} else {
				//Unknown - do nothing
			}
		} else if (sToken.equals("ifequal")) {
			final String sSubj = st[iTokenNum++];
			final String sTest = st[iTokenNum++];
			final String sPos = st[iTokenNum++];
			final String sNeg = st[iTokenNum++];
			if (sSubj.equals(sTest)) {
				//go to the positive result and skip the negative
				result = sPos;
			} else {
				//Skip the positive result and go to the negative result
				result = sNeg;
			}
		} else if (sToken.equals("ompc")) {
			final String sType = st[iTokenNum++];
			if (sType.equals("battpercent")) {
				result = String.valueOf(1000+OMC.BATTPERCENT).substring(1);
			} else if (sType.equals("battscale")) {
				result = String.valueOf(OMC.BATTSCALE);
			} else if (sType.equals("battdecimal")) {
				result = String.valueOf(OMC.BATTPERCENT/100f);
			} else if (sType.equals("battlevel")) {
				result = String.valueOf(OMC.BATTLEVEL);
			} else if (sType.equals("chargestatus")) {
				result = OMC.CHARGESTATUS;
			} else {
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","fallback");
				result = OMC.PREFS.getString("ompc_"+sType, "99");
			}
		} else if (sToken.equals("weather")) {
			if (OMC.PREFS.getString("weathersetting", "bylatlong").equals("disabled")) {
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "App","Weather Disabled - weather tags ignored");
				result = "--";
			} else {
				JSONObject jsonWeather = new JSONObject();
				try {
					jsonWeather = new JSONObject(OMC.PREFS.getString("weather", "{}"));
					final String sType = st[iTokenNum++];
					if (sType.equals("debug")) {
						final Time t = new Time();
						t.parse(jsonWeather.optString("current_local_time"));
						final Time t2 = new Time();
						t2.set(OMC.NEXTWEATHERREFRESH);
						final Time t3 = new Time();
						t3.set(OMC.LASTWEATHERTRY);
						result = "Weather as of " + t.format("%R") + "; lastry " + t3.format("%R")
								+ "; nextupd " + t2.format("%R");
					} else if (sType.equals("icontext")) {
						String sDay;
						if (OMC.TIME.toMillis(true) >= OMC.lSUNRISEMILLIS && OMC.TIME.toMillis(true) < OMC.lSUNSETMILLIS) {
							//Day
							sDay="day";
						} else {
							//Night - throw away the day token + the night indicator
							sDay="night";
						}
						result = OMC.WEATHERCONVERSIONS.getJSONObject("WeatherFont")
								.getJSONArray(sDay).optString(jsonWeather.optInt("condition_code",0),"E");
					} else if (sType.equals("index")) {
						final String sTranslateType = st[iTokenNum++];
						String sDay;
						if (OMC.TIME.toMillis(true) >= OMC.lSUNRISEMILLIS && OMC.TIME.toMillis(true) < OMC.lSUNSETMILLIS) {
							//Day
							sDay="day";
						} else {
							//Night - throw away the day token + the night indicator
							sDay="night";
						}
						result = OMC.WEATHERCONVERSIONS.getJSONObject(sTranslateType)
								.getJSONArray(sDay).optString(jsonWeather.optInt("condition_code",0),"00");
					} else if (sType.equals("engcondition")) {
						result = OMC.VERBOSEWEATHERENG[jsonWeather.optInt("condition_code",0)];
					} else if (sType.equals("condition")) {
						result = OMC.VERBOSEWEATHER[jsonWeather.optInt("condition_code",0)];
					} else if (sType.equals("conditioncode")) {
						result = jsonWeather.optInt("condition_code",0)+"";
					} else if (sType.equals("condition_lcase")) {
						result = OMC.VERBOSEWEATHER[jsonWeather.optInt("condition_code",0)].toLowerCase();
					} else if (sType.equals("temp")) {
						result = jsonWeather.optString("temp_"+OMC.PREFS.getString("weatherDisplay", "f"),"--")+OMC.PREFS.getString("weatherDisplay", "f").toUpperCase();
					} else if (sType.equals("tempc")) {
						result = jsonWeather.optString("temp_c","--");
					} else if (sType.equals("tempf")) {
						result = jsonWeather.optString("temp_f","--");
					} else if (sType.equals("city")) {
						result = jsonWeather.optString("city","Unknown");
					} else if (sType.equals("high")) {
						final int iDay = Integer.parseInt(st[iTokenNum++]);
						final String sFahrenheit = jsonWeather.getJSONArray("zzforecast_conditions").getJSONObject(iDay).optString("high","--");
						if (OMC.PREFS.getString("weatherDisplay", "f").equals("c")) {
							result = String.valueOf((int)((Float.parseFloat(sFahrenheit)-32f)*5f/9f+0.5f));
						} else {
							result = sFahrenheit;
						}
					} else if (sType.equals("low")) {
						final int iDay = Integer.parseInt(st[iTokenNum++]);
						final String sFahrenheit = jsonWeather.getJSONArray("zzforecast_conditions").getJSONObject(iDay).optString("low","--");
						if (OMC.PREFS.getString("weatherDisplay", "f").equals("c")) {
							result = String.valueOf((int)((Float.parseFloat(sFahrenheit)-32f)*5f/9f+0.5f));
						} else {
							result = sFahrenheit;
						}
					} else {
						// JSON parse error - probably uknown weather. Do nothing
						result = "--";
					}
				} catch (final JSONException e) {
					e.printStackTrace();
					// JSON parse error - probably uknown weather. Do nothing
					result = "--";
				}
			}
		} else if (sToken.equals("circle")) {
			// Specifies a point at angle/radius from point.
			final double dOriginVal = Double.parseDouble(st[iTokenNum++]);
			final String sType = st[iTokenNum++];
			final double dAngle = Double.parseDouble(st[iTokenNum++]);
			final double dRadius =  Double.parseDouble(st[iTokenNum++]);

			if (sType.equals("cos")) {
				result = String.valueOf(dOriginVal + dRadius * Math.cos(dAngle*Math.PI/180d));
			} else if (sType.equals("sin")) {
				result = String.valueOf(dOriginVal + dRadius * Math.sin(dAngle*Math.PI/180d));
			} else {
				//Unknown - do nothing
			}
			
		} else if (sToken.equals("ap24")) {
			if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
				result = (st[iTokenNum+2]);
			} else if (OMC.TIME.hour < 12) {
				result = (st[iTokenNum]);
			} else {
				result = (st[iTokenNum+1]);
			}
		} else if (sToken.equals("apm")) {
			if (OMC.TIME.hour < 12) {
				result = (st[iTokenNum]);
			} else {
				result = (st[iTokenNum+1]);
			}
		} else if (sToken.equals("array")) {
			final String sArrayName = st[iTokenNum++];
			final String sIndex = st[iTokenNum++];
			if (sIndex.equals("--") || sIndex.equals("Unk")) {
				result = "";
			} else {
				result = tempResult.optJSONObject("arrays").optJSONArray(sArrayName).optString(Integer.parseInt(sIndex.replace(" ","")));
			}
			final String sCase = st[iTokenNum++];
			if (result == null) result = "ERROR";
			if (sCase.equals("lower")) result = result.toLowerCase();
			else if (sCase.equals("upper")) result = result.toUpperCase();

		} else if (sToken.equals("flipformat")) {
			int iApply = Integer.parseInt(st[iTokenNum++]);
			final String sType = st[iTokenNum++];
			final StringTokenizer stt = new StringTokenizer(st[iTokenNum++]," ");
			if (sType.equals("bold")) {
				final StringBuilder sb = new StringBuilder();
				while (stt.hasMoreElements()){
					if (iApply==1) sb.append("<B>"+stt.nextToken()+"</B> ");
					else sb.append(stt.nextToken()+" ");
					iApply*=-1;
				}
				result = sb.toString();
			} else if (sType.equals("case")) {
				final StringBuilder sb = new StringBuilder();
				while (stt.hasMoreElements()){
					if (iApply==1) sb.append(stt.nextToken().toUpperCase()+" ");
					else sb.append(stt.nextToken().toLowerCase()+" ");
					iApply*=-1;
				}
				result = sb.toString();
			} else if (sType.equals("italics")) {
				final StringBuilder sb = new StringBuilder();
				while (stt.hasMoreElements()){
					if (iApply==1) sb.append("<I>"+stt.nextToken()+"</I> ");
					else sb.append(stt.nextToken()+" ");
					iApply*=-1;
				}
				result = sb.toString();
			}
		} else if (sToken.equals("stripspaces")){
			result = st[iTokenNum++].replace(" ", "");
		} else if (sToken.equals("fit")){
			result = "f"+st[iTokenNum++];
		} else if (sToken.equals("maxfit")){
			result = st[iTokenNum++]+"m"+st[iTokenNum++];
		} else if (sToken.equals("fullenglishtime")){
			// full english time
			final String sType = st[iTokenNum++];
			final int minuteindex = OMC.TIME.hour*60+OMC.TIME.minute;
			final String sTemp = OMC.getVerboseTime(minuteindex);
			if (sType.equals("diary")) result = (sTemp);
			else if (sType.equals("upper")) result = (sTemp.toUpperCase()); 
			else if (sType.equals("lower")) result = (sTemp.toLowerCase());
			else result = (sTemp);

		} else if (sToken.equals("digit")) {
			final String sTemp = st[iTokenNum++];

    		final int iOffset = Integer.parseInt(st[iTokenNum++]);
    		try {
    			result = (sTemp.substring(iOffset-1,iOffset));
    		} catch (final StringIndexOutOfBoundsException e) {
    			e.printStackTrace();
    			result="";
    		}
			
			
		} else if (sToken.equals("day")){
			// value that switches between two fixed symbols - day (betw sunrise/sunset) and night.
			if (OMC.TIME.toMillis(true) >= OMC.lSUNRISEMILLIS && OMC.TIME.toMillis(true) < OMC.lSUNSETMILLIS) {
				result = st[iTokenNum];
				//Day
			} else {
				//Night - throw away the day token + the night indicator
				result = st[iTokenNum+2];
			}
		} else if (sToken.equals("random")){
			// value that randomly jumps between two values.
			final String sType = st[iTokenNum++];
			final String sLow = st[iTokenNum++];
			final String sHigh = st[iTokenNum++];
			final float gradient = OMC.RND.nextFloat();
			if (sType.equals("number")) {
				result = String.valueOf(OMC.numberInterpolate(
						Integer.parseInt(sLow), 
						Integer.parseInt(sHigh), 
						gradient));
			} else if (sType.equals("color")) { // must be color
				final int color = OMC.colorInterpolate(
						sLow, 
						sHigh, 
						gradient);
				result = String.valueOf("#" + Integer.toHexString(color));
			} else if (sType.equals("float")) {
				result = String.valueOf(OMC.floatInterpolate(
						Float.parseFloat(sLow), 
						Float.parseFloat(sHigh), 
						gradient));
			} else {
				//Unknown - do nothing
			}
			

		} else if (sToken.equals("gradient")){
			// value that randomly jumps between two values.
			final String sType = st[iTokenNum++];
			final String sMin = st[iTokenNum++];
			final String sVal = st[iTokenNum++];
			final String sMax = st[iTokenNum++];
			final float gradient = Float.parseFloat(sVal);
			if (sType.equals("number")) {
				result = String.valueOf(OMC.numberInterpolate(
						Integer.parseInt(sMin), 
						Integer.parseInt(sMax), 
						gradient));
			} else if (sType.equals("color")) { // must be color
				final int color = OMC.colorInterpolate(
						sMin, 
						sMax, 
						gradient);
				
				result = "#"+Long.toHexString(0x300000000l + color).substring(1).toUpperCase();
						
			} else if (sType.equals("float")) {
				result = String.valueOf(OMC.floatInterpolate(
						Float.parseFloat(sMin), 
						Float.parseFloat(sMax), 
						gradient));
			} else {
				//Unknown - do nothing
			}
			
		} else if (sToken.equals("translate")) {
			final String sWord = st[iTokenNum++];
			final JSONObject entry = tempResult.optJSONObject("translations");
			if (entry==null) {
				result=sWord;
			} else {
				final JSONObject subentry = entry.optJSONObject(sWord);
				if (subentry==null) {
					result = sWord;
				} else {
					result = subentry.optString(OMC.PREFS.getString("clockLocaleName", "English (US)"), sWord);
				}
			}
		} else if (sToken.equals("uppercase")){
			try {
				result = st[iTokenNum++].toUpperCase();
			} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
				result = "";
			}
		} else if (sToken.equals("alarm")){
			try {
				result = android.provider.Settings.System.getString(OMC.CONTEXT.getContentResolver(), android.provider.Settings.System.NEXT_ALARM_FORMATTED);
				System.out.println("ALARM RESULT = " + result);
			} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
				result = "";
			}
		} else if (sToken.equals("lowercase")){
			try {
				result = st[iTokenNum++].toLowerCase();
			} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
				result = "";
			}
		} else if (sToken.equals("verbosenumber")){
			String sRawValue = st[iTokenNum++].trim();
			if (OMC.DAYSUFFIX.length()!=0) {
				sRawValue = sRawValue.replace(OMC.DAYSUFFIX,"");
			}
			if (sRawValue.equals("")) {
				result="";
			} else {
				try {
					if (OMC.DAYSUFFIX.length()!=0) {
						result = OMC.VERBOSENUMBERS[Integer.parseInt(sRawValue)]+OMC.DAYSUFFIX;
					} else {
						result = OMC.VERBOSENUMBERS[Integer.parseInt(sRawValue)];
					}
				} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
					result = "";
				}
			}
		} else {
			//unrecognized macro - ignore
		}
		
		return result;
		
	}

	static public String resolveTokens(final String sRawString, final int aWI, final JSONObject tempResult) {
//		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Array","Parsing "+sRawString);
		final StringBuilder result = new StringBuilder();

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
	
	static public String OMCstrf (final String input,final int aWI) {
		String convertedInput="";
		if (OMC.PREFS.getBoolean("mmddDateFormat"+aWI, true)) {
			convertedInput = input
					.replace("%d %m", "%m %d")
					.replace("%e %m", "%m %e")
					.replace("%e %b", "%b %e")
					.replace("%d %b", "%b %d")
					.replace("%d/%m", "%m/%d")
					.replace("%e/%m", "%m/%e")
					.replace("%e/%b", "%b/%e")
					.replace("%d/%b", "%b/%d")
					.replace("%d-%m", "%m-%d")
					.replace("%e-%m", "%m-%e")
					.replace("%e-%b", "%b-%e")
					.replace("%d-%b", "%b-%d")
					;
		} else {
			convertedInput = input
					.replace("%m %d", "%d %m")
					.replace("%m %e", "%e %m")
					.replace("%b %d", "%d %b")
					.replace("%b %e", "%e %b")
					.replace("%m/%d", "%d/%m")
					.replace("%m/%e", "%e/%m")
					.replace("%b/%d", "%d/%b")
					.replace("%b/%e", "%e/%b")
					.replace("%m-%d", "%d-%m")
					.replace("%m-%e", "%e-%m")
					.replace("%b-%d", "%d-%b")
					.replace("%b-%e", "%e-%b")
					;
		}

		final char[] inputChars = convertedInput.toCharArray();
		final StringBuilder sb = new StringBuilder(inputChars.length);
		boolean bIsTag = false;
		for (int i = 0; i < inputChars.length; i++) {
			if (bIsTag) {
				if (inputChars[i]=='A') {
					sb.append(OMC.VERBOSEDOW[OMC.TIME.weekDay]);
				} else if (inputChars[i]=='a') {
					sb.append(OMC.SHORTDOW[OMC.TIME.weekDay]);
				} else if (inputChars[i]=='B') {
					sb.append(OMC.VERBOSEMONTH[OMC.TIME.month]);
				} else if (inputChars[i]=='b') {
					sb.append(OMC.SHORTMONTH[OMC.TIME.month]);
				} else if (inputChars[i]=='d') {
					sb.append(OMC.TIME.format("%d")+OMC.DAYSUFFIX);
				} else if (inputChars[i]=='e') {
					sb.append(OMC.TIME.format("%e")+OMC.DAYSUFFIX);
				} else if (inputChars[i]=='P') {
					sb.append(OMC.TIME.hour<12?"AM":"PM");
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
	
	static public int numberInterpolate (final int n1, final int n2, final int n3, final float gradient) {
		if (gradient > 0.5f) return (int)(n2+ (n3-n2)*(gradient-0.5f) * 2);
		else return (int)(n1 + (n2-n1) * gradient * 2);
	}
	
	static public float floatInterpolate (final float n1, final float n2, final float n3, final float gradient) {
		if (gradient > 0.5f) return (n2+ (n3-n2)*(gradient-0.5f) * 2);
		else return (n1 + (n2-n1) * gradient * 2);
	}
	
	static public int colorInterpolate (final String c1, final String c2, final String c3, float gradient) {
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
	
	static public int numberInterpolate (final int n1, final int n2, final float gradient) {
		return (int)(n1 + (n2-n1) * gradient);
	}

	static public int numberInterpolate (final int x1, final int y1, final int x2, final int y2, final int x) {
		return (int)(((double)x-(double)x1) * ((double)y2-(double)y1)/((double)x2-(double)x1) + (double)y1 + 0.5);
		
	}
	
	static public int floatInterpolate (final float n1, final float n2, final float gradient) {
		return (int)(n1 + (n2-n1) * gradient);
	}
	
	static public int colorInterpolate (final String c1, final String c2, final float gradient) {
		final int minval = Color.parseColor(c1);
		final int maxval = Color.parseColor(c2);
		return Color.argb(numberInterpolate(minval >>> 24, maxval >>> 24, gradient),
				numberInterpolate((minval >> 16) & 0xFF, (maxval >> 16) & 0xFF, gradient),
				numberInterpolate((minval >> 8) & 0xFF, (maxval >> 8) & 0xFF, gradient),
				numberInterpolate(minval & 0xFF , maxval & 0xFF , gradient)
				);
	}
	

    @Override
    public void onTerminate() {
//    	if (!OMCService.STOPNOW4x4 || !OMCService.STOPNOW4x2 || !OMCService.STOPNOW4x1 
//    			|| !OMCService.STOPNOW3x3 || !OMCService.STOPNOW3x1 
//    			|| !OMCService.STOPNOW2x2 || !OMCService.STOPNOW2x1
//    			|| !OMCService.STOPNOW1x3) {
//    		Log.i(OMC.OMCSHORT + "App","APP TERMINATED - NOT UNREGISTERING RECEIVERS - OMC WILL RESTART");
//    		// do nothing
//    	} else {
//    		Log.i(OMC.OMCSHORT + "App","APP TERMINATED - UNREGISTERING RECEIVERS - OMC WILL NOT RESTART");
//    		unregisterReceiver(aRC);
//    	}
        OMC.PREFS.edit().commit();
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
    	purgeBitmapCache();
    	purgeImportCache();
    	purgeEmailCache();
    	purgeTypefaceCache();
    	OMC.THEMEMAP.clear();
    	OMC.WIDGETBMPMAP.clear();
    	OMC.PREFS.edit()
    		.putInt("ompc_battlevel", OMC.BATTLEVEL)
			.putInt("ompc_battscale", OMC.BATTSCALE)
			.putInt("ompc_battpercent", OMC.BATTPERCENT)
			.putString("ompc_chargestatus", OMC.CHARGESTATUS)
			.commit();
    	
    	super.onLowMemory();
    }

    static public Matrix getMatrix() {
    	try {
    		return OMC.MATRIXPOOL.take();
    	} catch (final InterruptedException e) {
    		e.printStackTrace();
    		return null;
    	}
    }
    
    static public void returnMatrix(final Matrix m) {
    	try {
    		m.reset();
        	OMC.MATRIXPOOL.put(m);
    	} catch (final InterruptedException e) {
    		e.printStackTrace();
    	}
    }
    
    static public Paint getPaint() {
    	try {
    		return OMC.PAINTPOOL.take();
    	} catch (final InterruptedException e) {
    		e.printStackTrace();
    		return null;
    	}
    }
    
    static public void returnPaint(final Paint pt) {
    	try {
    		pt.reset();
        	OMC.PAINTPOOL.put(pt);
    	} catch (final InterruptedException e) {
    		e.printStackTrace();
    	}
    }
    
    static public Bitmap getWidgetBMP() {
    	try {
    		return OMC.WIDGETPOOL.take();
    	} catch (final InterruptedException e) {
    		e.printStackTrace();
    		return null;
    	}
    }

    static public boolean isConnected() {
    	final ConnectivityManager conMgr =  (ConnectivityManager)OMC.CONTEXT.getSystemService(Context.CONNECTIVITY_SERVICE);
    	final NetworkInfo ni= conMgr.getActiveNetworkInfo();
    	if (ni==null) return false;
		if (ni.isConnectedOrConnecting()) {
			return true;
		}
		return false;
    }
    
    static public void returnWidgetBMP(final Bitmap bmp) {
    	try {
    		bmp.eraseColor(Color.TRANSPARENT);
        	OMC.WIDGETPOOL.put(bmp);
    	} catch (final InterruptedException e) {
    		e.printStackTrace();
    	}
    }

    static public void abortWeatherUpdate() {
 	    OMC.LM.removeUpdates(OMC.LL);
    }
    
	static public void updateWeather() {
		// When we want a weather update, first we set the "try" timestamp.
		OMC.LASTWEATHERTRY=System.currentTimeMillis();
		// Temporarily set the "next refresh" timestamp to the default retry period
		// (the "next refresh" timestamp will change to default refresh period if weather update succeeds)
		OMC.NEXTWEATHERREFRESH=OMC.LASTWEATHERTRY+Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))/4l*60000l;
		// If the "next refresh" timestamp is later than the "next online request" timestamp,
		// push the "next online request" timestamp out to match.
		if (OMC.NEXTWEATHERREFRESH>OMC.NEXTWEATHERREQUEST) OMC.NEXTWEATHERREQUEST= OMC.NEXTWEATHERREFRESH;
		
		OMC.PREFS.edit().putLong("weather_lastweathertry", OMC.LASTWEATHERTRY)
		.putLong("weather_nextweatherrefresh", OMC.NEXTWEATHERREFRESH)
		.putLong("weather_nextweatherrequest", OMC.NEXTWEATHERREQUEST)
		.commit();
		
		// Find out what the user preference is.
		final String sWeatherSetting = OMC.PREFS.getString("weathersetting", "bylatlong");

		// If weather is disabled (default), do nothing
		if (sWeatherSetting.equals("disabled")) {
        	Log.i(OMC.OMCSHORT + "Weather", "Weather Disabled, no weather update");
			return;
		// If phone has no connectivity, do nothing
		} else if (!OMC.isConnected()) {
        	Log.i(OMC.OMCSHORT + "Weather", "No connectivity - no weather update");
			return;

		// If weather is by latitude/longitude, request lazy location (unless forced).
		// The location listener directs control to the updateweather function upon callback.
		} else if (sWeatherSetting.equals("bylatlong")) {
			GoogleReverseGeocodeService.getLastBestLocation(Integer.parseInt(OMC.PREFS.getString("locationPriority", "4")));
			return;
		} else if (sWeatherSetting.equals("specific")) {
			// If weather is for fixed location, calculate sunrise/sunset for the location, then
			// update weather manually
			OMC.LASTKNOWNCITY=OMC.jsonFIXEDLOCN.optString("city","Unknown");
			OMC.LASTKNOWNCOUNTRY=OMC.jsonFIXEDLOCN.optString("country","Unknown");
			OMC.WEATHERREFRESHSTATUS=OMC.WRS_FIXED;	
			
			if (OMC.LASTKNOWNCOUNTRY.equals("United States") && OMC.PREFS.getString("weatherProvider", "auto").equals("auto")) {
				OMC.PREFS.edit().putString("activeWeatherProvider", "noaa").commit();
				OMC.PREFS.edit().putBoolean("weatherMETAR", true).commit();
			}
			
			OMC.calculateSunriseSunset(OMC.jsonFIXEDLOCN.optDouble("latitude",0d), OMC.jsonFIXEDLOCN.optDouble("longitude",0d));
			final String sWProvider = OMC.PREFS.getString("activeWeatherProvider", "seventimer");
			if (sWProvider.equals("ig")) {
				GoogleWeatherXMLHandler.updateWeather(OMC.jsonFIXEDLOCN.optDouble("latitude",0d), 
				OMC.jsonFIXEDLOCN.optDouble("longitude",0d), 
				OMC.jsonFIXEDLOCN.optString("country","Unknown"), 
				OMC.jsonFIXEDLOCN.optString("city","Unknown"), true);
			} else if (sWProvider.equals("yr")) {
				YrNoWeatherXMLHandler.updateWeather(OMC.jsonFIXEDLOCN.optDouble("latitude",0d), 
				OMC.jsonFIXEDLOCN.optDouble("longitude",0d), 
				OMC.jsonFIXEDLOCN.optString("country","Unknown"), 
				OMC.jsonFIXEDLOCN.optString("city","Unknown"), true);
			} else if (sWProvider.equals("owm")) {
				OpenWeatherMapJSONHandler.updateWeather(OMC.jsonFIXEDLOCN.optDouble("latitude",0d), 
				OMC.jsonFIXEDLOCN.optDouble("longitude",0d), 
				OMC.jsonFIXEDLOCN.optString("country","Unknown"), 
				OMC.jsonFIXEDLOCN.optString("city","Unknown"), true);
			} else if (sWProvider.equals("noaa")) {
				NOAAWeatherXMLHandler.updateWeather(OMC.jsonFIXEDLOCN.optDouble("latitude",0d), 
				OMC.jsonFIXEDLOCN.optDouble("longitude",0d), 
				OMC.jsonFIXEDLOCN.optString("country","Unknown"), 
				OMC.jsonFIXEDLOCN.optString("city","Unknown"), true);
			} else {
				SevenTimerJSONHandler.updateWeather(OMC.jsonFIXEDLOCN.optDouble("latitude",0d), 
				OMC.jsonFIXEDLOCN.optDouble("longitude",0d), 
				OMC.jsonFIXEDLOCN.optString("country","Unknown"), 
				OMC.jsonFIXEDLOCN.optString("city","Unknown"), true);
			} 
			
			return;
		}
		
	}
	
	
	static public void calculateSunriseSunset(final double latitude, final double longitude) {

		final Time t = new Time(Time.TIMEZONE_UTC);
		t.setToNow();
		t.hour=0;
		t.minute=0;
		t.second=0;
		t.switchTimezone(Time.getCurrentTimezone());
		final long lMidnight = t.toMillis(false);
		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "App",("Midnight UTC for this timezone:" + new java.sql.Time(lMidnight).toLocaleString()));

		final double radLatitude = latitude/180d*Math.PI;
		final double y = (2d*Math.PI/365d)*(t.yearDay + (t.hour)/24d);
		final double eqtime = 229.18*(0.000075+0.001868*Math.cos(y)-0.032077*Math.sin(y)-0.014615*Math.cos(2*y)-0.040849*Math.sin(2*y));
		final double declin = 0.006918-0.399912*Math.cos(y)+0.070257*Math.sin(y)-0.006758*Math.cos(2*y)+0.000907*Math.sin(2*y)-0.002697*Math.cos(3*y)+0.00148*Math.sin(3*y);
		final double dSunriseHourAngle = (float)(Math.acos(
				(Math.cos(1.58533492d) - (Math.sin(radLatitude)*Math.sin(declin))) /
				(Math.cos(radLatitude)*Math.cos(declin)))
				/Math.PI*180d);
		
		// This workaround is required to ensure that the noon, sunset and sunrise times are 
		// for this calendar date (today, not yesterday or tomorrow!)
		final Time today = new Time();
		today.setToNow();
		final Time solarNoonTime = new Time();
		solarNoonTime.set(lMidnight + (long)((720d - 4* longitude - eqtime)*60000d));
		solarNoonTime.year = today.year;
		solarNoonTime.month = today.month;
		solarNoonTime.monthDay = today.monthDay;
		solarNoonTime.normalize(false);
		OMC.LSOLARNOONMILLIS = solarNoonTime.toMillis(false);
		
		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "App",("Local Solar Noon today:" + new java.sql.Time(OMC.LSOLARNOONMILLIS).toLocaleString())); 

		OMC.lSUNRISEMILLIS = OMC.LSOLARNOONMILLIS - (long)(dSunriseHourAngle*4d*60000d);
		
		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "App",("Local Sunrise today:" + new java.sql.Time(OMC.lSUNRISEMILLIS).toLocaleString())); 

		OMC.lSUNSETMILLIS = OMC.LSOLARNOONMILLIS + (long)(dSunriseHourAngle*4d*60000d);
		
		if (OMC.DEBUG)
			Log.i(OMC.OMCSHORT + "App",("Local Sunset today:" + new java.sql.Time(OMC.lSUNSETMILLIS).toLocaleString())); 

	}
	
	public static double roundToSignificantFigures(final double num, final int n) {
	    if(num == 0) {
	        return 0;
	    }

	    final double d = Math.ceil(Math.log10(num < 0 ? -num: num));
	    final int power = n - (int) d;

	    final double magnitude = Math.pow(10, power);
	    final long shifted = Math.round(num*magnitude);
	    return shifted/magnitude;
	}
	
	public static float roundToSignificantFigures(final float num, final int n) {
	    if(num == 0) {
	        return 0;
	    }

	    final double d = Math.ceil(Math.log10(num < 0 ? -num: num));
	    final int power = n - (int) d;

	    final double magnitude = Math.pow(10, power);
	    final long shifted = Math.round(num*magnitude);
	    return (float)(shifted/magnitude);
	}
	
	public static String RString(final String key) {
		return OMC.RES.getString(OMC.RES.getIdentifier(key, "string", OMC.PKGNAME));
	}
	
	public static String RString(final String key, final Locale foreignLocale) {
		final Configuration configTemp = new Configuration(OMC.RES.getConfiguration());
		configTemp.locale=foreignLocale;
		final Resources res = new Resources(OMC.AM,OMC.RES.getDisplayMetrics(),configTemp);
		return res.getString(OMC.RES.getIdentifier(key, "string", OMC.PKGNAME));
	}
	
	public static String[] RStringArray(final String key) {
		return OMC.RES.getStringArray(OMC.RES.getIdentifier(key, "array", OMC.PKGNAME));
	}
	
	public static String[] RStringArray(final String key, final Locale foreignLocale) {
		final Configuration configTemp = new Configuration(OMC.RES.getConfiguration());
		configTemp.locale=foreignLocale;
		final Resources res = new Resources(OMC.AM,OMC.RES.getDisplayMetrics(),configTemp);
		return res.getStringArray(res.getIdentifier(key, "array", OMC.PKGNAME));
	}
	
	public static int RId(final String key) {
		return OMC.RES.getIdentifier(key, "id", OMC.PKGNAME);
	}

	public static int RDrawableId(final String key) {
		return OMC.RES.getIdentifier(key, "drawable", OMC.PKGNAME);
	}

	public static int RXmlId(final String key) {
		return OMC.RES.getIdentifier(key, "xml", OMC.PKGNAME);
	}

	public static int RLayoutId(final String key) {
		return OMC.RES.getIdentifier(key, "layout", OMC.PKGNAME);
	}

	public static int RMenuId(final String key) {
		return OMC.RES.getIdentifier(key, "menu", OMC.PKGNAME);
	}

	public static String getVerboseTime(final int minuteindex){
		final int index = minuteindex/360;
		final int subindex = minuteindex%360;
		switch (index) {
			case 0:
				return OMC.VERBOSETIME1[subindex];
			case 1:
				return OMC.VERBOSETIME2[subindex];
			case 2:
				return OMC.VERBOSETIME3[subindex];
			case 3:
			default:
				return OMC.VERBOSETIME4[subindex];
			
		}
	}
	
	public static class ICAOLatLon {
		public String icao;
		public double lat;
		public double lon;
		public ICAOLatLon(String ic, double lt, double ln){
			this.icao= ic;
			this.lat = lt;
			this.lon = ln;
		}
	}
	
	public static class ICAODistPair {
		public String icao;
		public double dist;
		public ICAODistPair(String ic, double dst){
			this.icao= ic;
			this.dist = dst;
		}
	}
	
	public static void parseICAOMap() throws IOException {
		final int ICAOCOLUMN = 20;
		final int LATCOLUMN = 39;
		final int LONCOLUMN = 47;
		final int NSCOLUMN = 44;
		final int EWCOLUMN = 53;
		final int METARCOLUMN = 62;
		
		BufferedReader r = new BufferedReader(new InputStreamReader(OMC.AM.open("stations.txt")));
		String sLine=null;
		while ((sLine = r.readLine())!=null) {
			if (sLine.length()<EWCOLUMN+4) {
				continue;
			}
			try {
				final String sICAO = sLine.substring(ICAOCOLUMN, ICAOCOLUMN+4);
				final double lat = Double.parseDouble(sLine.substring(LATCOLUMN, LATCOLUMN+5).replace(" ", ".")) * (sLine.charAt(NSCOLUMN)=='N'?1d:-1d);
				final double lon = Double.parseDouble(sLine.substring(LONCOLUMN, LONCOLUMN+6).replace(" ", ".")) * (sLine.charAt(EWCOLUMN)=='E'?1d:-1d);
//				If it's not an airport line, just skip
				if (sICAO.equals("    ") || sICAO.equals("ICAO")) continue;
//				If it's not a METAR-reporting airport, just skip
				if (sLine.charAt(METARCOLUMN)!='X') continue;
				OMC.ICAOLIST.add(new ICAOLatLon(sICAO, lat, lon));
			} catch (NumberFormatException e) {
				
			}
		}
		r.close();
	}
	
	public static String flattenString(String[] array) {
		StringBuilder result = new StringBuilder();
		for (String s:array) {
			result.append(s).append(" ");
		}
		return result.toString();
	}
	
	public static String[] findClosestICAOs(final double lat1, final double lon1, final int radiusKM) {
		final double R = 6371; //km
		double bestDistance = Double.MAX_VALUE;
		ArrayList<ICAODistPair> bestICAOs = new ArrayList<ICAODistPair>();
		
		Iterator<ICAOLatLon> i = OMC.ICAOLIST.iterator();
		while (i.hasNext()) {
			ICAOLatLon station = i.next();
			double dLat = Math.toRadians(station.lat-lat1);
			double dLon = Math.toRadians(station.lon-lon1);
			double latR1 = Math.toRadians(lat1);
			double latR2 = Math.toRadians(station.lat);
			
			double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
			        Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(latR1) * Math.cos(latR2); 
			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
			double dist = R * c;
			
			if (dist>radiusKM) continue;
			
			bestICAOs.add(new ICAODistPair(station.icao, dist));
			
		}
		if (bestICAOs.isEmpty()) return null;
		Collections.sort(bestICAOs,new Comparator<ICAODistPair>() {
			@Override
			public int compare(ICAODistPair lhs, ICAODistPair rhs) {
				if (lhs.dist<rhs.dist) return -1;
				else if (lhs.dist>rhs.dist) return 1;
				else return 0;
			}
		});
		String[] result = new String[bestICAOs.size()];
		for (int j=0;j<bestICAOs.size();j++) {
			result[j]=bestICAOs.get(j).icao;
		}
		return result;
	}
}
