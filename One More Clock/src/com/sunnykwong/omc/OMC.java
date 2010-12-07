package com.sunnykwong.omc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Collections;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.graphics.BitmapFactory;
import android.content.res.Resources;
import android.graphics.Matrix;

//import com.sunnykwong.omc.whambamwidget.R;

/**
 * @author skwong01
 * Thanks to ralfoide's 24clock code; taught me quite a bit about broadcastreceivers
 * Thanks to the Open Font Library for a great resource.
 * 
 */
public class OMC extends Application {
	
	
	static String THISVERSION;
	static final boolean SINGLETON = false;
	static final boolean FREEEDITION = false;
	static final String OMCSHORT = "";

	
	static final String OMCNAME = "com.sunnykwong.omc";
	static String SHAREDPREFNAME;
	static String PKGNAME;
	static final String STARTERPACKURL = "omc://omc.colormeandroid.com/pk1204.omc";
//	static final String STARTERPACKURL = "omcs://docs.google.com/uc?id=0B6S4jLNkP1XFYWVjNGQ5Y2QtZmE4Yy00OWM5LWJhNGYtZmQ4NjFjMmM5Yzc1&export=download&authkey=CO66i_8O&hl=en";
	static boolean SHOWHELP = false;
	static final boolean DEBUG = true;

	
	static long LASTUPDATEMILLIS;
	static int UPDATEFREQ = 20000;
	static final String DEFAULTTHEME = "LockscreenLook";
	static final Random RND = new Random();
	static SharedPreferences PREFS;
	static AlarmManager ALARMS;	// I only need one alarmmanager.
	static AssetManager AM;
    static NotificationManager NM;
    static Resources RES;
    
    static HashMap<String, Typeface> TYPEFACEMAP;
    static HashMap<String, Bitmap> BMPMAP;
    static Map<String, OMCImportedTheme> IMPORTEDTHEMEMAP;
	
    static OMCConfigReceiver cRC;
	static OMCAlarmReceiver aRC;
    static boolean SCREENON = true; 	// Is the screen on?
    static boolean FG = false;
    static OMCTypedArray LAYERATTRIBS;
	static String[] LAYERLIST, TALKBACKS; 
	static Matrix TEMPMATRIX;
	static boolean STARTERPACKDLED = false;

	static final int WIDGETWIDTH=480;
	static final int WIDGETHEIGHT=300;
	static final Time TIME = new Time();
	static String CACHEPATH;
	static String[] WORDNUMBERS;
	static String[] STRETCHINFO;
	static String[] OVERLAYURL;
	static int[] OVERLAYRESOURCES;

	static ComponentName WIDGET4x2CNAME;
	static ComponentName WIDGET4x1CNAME;
	static ComponentName WIDGET3x1CNAME;
	static ComponentName WIDGET2x1CNAME;
	static ComponentName SKINNERCNAME;

	static final float[] FLARERADII = new float[] {32.f,20.f,21.6f,40.2f,18.4f,19.1f,10.8f,25.f,28.f};
	static final int[] FLARECOLORS = new int[] {855046894,1140258554,938340342,1005583601,855439588,
		669384692,905573859,1105458423,921566437};
	static String TXTBUF;
	
	static final int SVCNOTIFICATIONID = 1; // Notification ID for the one and only message window we'll show
    static final Class<?>[] mStartForegroundSignature = new Class[] {int.class, Notification.class};
    static final Class<?>[] mStopForegroundSignature = new Class[] {boolean.class};
    static Intent FGINTENT, BGINTENT, SVCSTARTINTENT, WIDGETREFRESHINTENT, CREDITSINTENT, PREFSINTENT;
    static Intent GETSTARTERPACKINTENT, IMPORTTHEMEINTENT, DUMMYINTENT, OMCMARKETINTENT;
    static PendingIntent FGPENDING, BGPENDING, PREFSPENDING;
    static IntentFilter PREFSINTENTFILT;
    static Notification FGNOTIFICIATION;
    
	static RectF BGRECT, FGRECT;

	static Bitmap BUFFER;
	static Canvas CANVAS;
	static Bitmap rotBUFFER;
	static Canvas rotCANVAS;
	static Paint PT1;
	static Paint PT2;
	
	static float fSCALEX, fSCALEY;

	@Override
	public void onCreate() {
		super.onCreate();

		try {
			OMC.THISVERSION = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName;
		} catch (NameNotFoundException e) {
			OMC.THISVERSION = "1.0.0";
		}
		
		OMC.PKGNAME = getPackageName();
		OMC.SHAREDPREFNAME = OMC.PKGNAME + "_preferences";

		OMC.WIDGET4x2CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget4x2");
		OMC.WIDGET4x1CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget4x1");
		OMC.WIDGET3x1CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget3x1");
		OMC.WIDGET2x1CNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".ClockWidget2x1");
		OMC.SKINNERCNAME = new ComponentName(OMC.PKGNAME,OMC.OMCNAME+".OMCSkinnerActivity");

		
		OMC.LASTUPDATEMILLIS = 0l;
		
		OMC.BUFFER= Bitmap.createBitmap(OMC.WIDGETWIDTH,OMC.WIDGETHEIGHT,Bitmap.Config.ARGB_4444);
		OMC.CANVAS = new Canvas(OMC.BUFFER);
		OMC.CANVAS.setDensity(DisplayMetrics.DENSITY_HIGH);
		OMC.PT1 = new Paint();
		OMC.PT2 = new Paint();
		
		OMC.aRC = new OMCAlarmReceiver();
		OMC.cRC = new OMCConfigReceiver();
		
		
		OMC.TEMPMATRIX = new Matrix();
		
		OMC.FGINTENT = new Intent("com.sunnykwong.omc.FGSERVICE");
		OMC.FGPENDING = PendingIntent.getBroadcast(this, 0, OMC.FGINTENT, 0);
		OMC.BGINTENT = new Intent("com.sunnykwong.omc.BGSERVICE");
		OMC.BGPENDING = PendingIntent.getBroadcast(this, 0, OMC.BGINTENT, 0);
		OMC.SVCSTARTINTENT = new Intent(this, OMCService.class);
		OMC.WIDGETREFRESHINTENT = new Intent("com.sunnykwong.omc.WIDGET_REFRESH");
		OMC.CREDITSINTENT = new Intent(this, OMCCreditsActivity.class);
		OMC.PREFSINTENT = new Intent(this, OMCPrefActivity.class);
		OMC.IMPORTTHEMEINTENT = new Intent(this, OMCThemeImportActivity.class);
		OMC.PREFSPENDING = PendingIntent.getActivity(this, 0, new Intent(this, OMCPrefActivity.class), 0);
		OMC.PREFSINTENTFILT = new IntentFilter("com.sunnykwong.omc.WIDGET_CONFIG");
		OMC.PREFSINTENTFILT.addDataScheme("omc");
		OMC.DUMMYINTENT = new Intent(this, DUMMY.class);
		OMC.GETSTARTERPACKINTENT = new Intent(this, OMCThemeUnzipActivity.class);
		OMC.GETSTARTERPACKINTENT.setData(Uri.parse(OMC.STARTERPACKURL));
		OMC.OMCMARKETINTENT = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.sunnykwong.omc"));

		
		OMC.CACHEPATH = this.getCacheDir().getAbsolutePath() + "/";
		
		OMC.BGRECT = new RectF(30,10,295,150);
		OMC.FGRECT = new RectF(25,5,290,145);
		
		OMC.FGNOTIFICIATION = new Notification(this.getResources().getIdentifier("fredicon_mdpi", "drawable", OMC.PKGNAME), 
				"",
        		System.currentTimeMillis());
        OMC.FGNOTIFICIATION.flags = OMC.FGNOTIFICIATION.flags|Notification.FLAG_ONGOING_EVENT|Notification.FLAG_NO_CLEAR;
		
    	OMC.ALARMS = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
    	OMC.AM = getAssets();
    	OMC.RES = getResources();
    	OMC.NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		OMC.PREFS = getSharedPreferences(SHAREDPREFNAME, Context.MODE_PRIVATE);
		// We are using Zehro's solution (listening for TIME_TICK instead of using AlarmManager + FG Notification) which
		// should be quite a bit more graceful.
		OMC.FG = OMC.PREFS.getBoolean("widgetPersistence", false)? true : false;
		
		// If we're from a legacy version, then we need to wipe all settings clean to avoid issues.
		if (OMC.PREFS.getString("version", "1.0.x").startsWith("1.0")) {
			Log.i("OMCApp","Upgrade from legacy version, wiping all settings.");
			OMC.PREFS.edit().clear().commit();
		}
		if (OMC.PREFS.getString("version", "1.0.x").equals(OMC.THISVERSION)) {
			OMC.STARTERPACKDLED = OMC.PREFS.getBoolean("starterpack", false);
		} else {
			OMC.PREFS.edit().putBoolean("starterpack", false).commit();
			OMC.STARTERPACKDLED = false;
		}
		OMC.PREFS.edit().putString("version", OMC.THISVERSION).commit();
		OMC.UPDATEFREQ = OMC.PREFS.getInt("iUpdateFreq", 30) * 1000;
		
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_SCREEN_ON));
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_TIME_TICK));
		
		OMC.TYPEFACEMAP = new HashMap<String, Typeface>(6);
		OMC.BMPMAP = new HashMap<String, Bitmap>(3);
		OMC.IMPORTEDTHEMEMAP=Collections.synchronizedMap(new HashMap<String, OMCImportedTheme>(3));
		
		OMC.LAYERLIST = null;
		OMC.LAYERATTRIBS = null;

		OMC.TALKBACKS = null;
		OMC.STRETCHINFO = null;
		
		OMC.OVERLAYURL = null;
		OMC.OVERLAYRESOURCES = new int[] {
				this.getResources().getIdentifier("N", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("NE", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("E", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("SE", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("S", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("SW", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("W", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("NW", "id", OMC.PKGNAME),
				this.getResources().getIdentifier("C", "id", OMC.PKGNAME)
				};

		OMC.WORDNUMBERS = this.getResources().getStringArray(this.getResources().getIdentifier("WordNumbers", "array", OMC.PKGNAME));
		
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
					OMC.WIDGET4x1CNAME,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET3x1CNAME,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET2x1CNAME,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
    	} else {
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET4x1CNAME,
					OMC.PREFS.getBoolean("bFourByOne", false) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
					.setComponentEnabledSetting(
							OMC.WIDGET3x1CNAME,
							OMC.PREFS.getBoolean("bThreeByOne", false) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
									: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
							PackageManager.DONT_KILL_APP);
	    	context.getPackageManager()
			.setComponentEnabledSetting(
					OMC.WIDGET2x1CNAME,
					OMC.PREFS.getBoolean("bTwoByOne", false) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
							: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
    	}
	}

	static void setServiceAlarm (long lTimeToRefresh) {
		//We want the pending intent to be for this service, and 
		// at the same FG/BG preference as the intent that woke us up
		if (OMC.FG) {
			OMC.ALARMS.set(AlarmManager.RTC, lTimeToRefresh, OMC.FGPENDING);
		} else {
			OMC.ALARMS.set(AlarmManager.RTC, lTimeToRefresh, OMC.BGPENDING);
		}
    }

	public static void setPrefs(int aWI) {
		OMC.PREFS.edit().putString("widgetTheme"+aWI, OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME))
		.putBoolean("widget24HrClock"+aWI, OMC.PREFS.getBoolean("widget24HrClock", true))
		.putBoolean("external"+aWI, OMC.PREFS.getBoolean("external", false))
		.putString("URI"+aWI, OMC.PREFS.getString("URI", ""))
		.commit();
	}
 
	public static void initPrefs(int aWI) {
		// For new clocks... just like setPrefs but leaves the URI empty.
		OMC.PREFS.edit().putString("widgetTheme"+aWI, OMC.PREFS.getString("widgetTheme", OMC.DEFAULTTHEME))
		.putBoolean("widget24HrClock"+aWI, OMC.PREFS.getBoolean("widget24HrClock", true))
		.putBoolean("external"+aWI, OMC.PREFS.getBoolean("external", false))
		.commit();
	}
 
	public static void getPrefs(int aWI) {
    	OMC.PREFS.edit().putString("widgetTheme", OMC.PREFS.getString("widgetTheme"+aWI, OMC.DEFAULTTHEME))
		.putBoolean("widget24HrClock", OMC.PREFS.getBoolean("widget24HrClock"+aWI, true))
		.putBoolean("external", OMC.PREFS.getBoolean("external"+aWI, false))
		.putString("URI", OMC.PREFS.getString("URI"+aWI, ""))
		.commit();
	}
	
	public static void removePrefs(int aWI) {
		OMC.PREFS.edit()
			.remove("widgetTheme"+aWI)
			.remove("widget24HrClock"+aWI)
			.remove("URI"+aWI)
			.commit();
	}
	
	public static Typeface getTypeface(String type, String src) {
//		System.out.println("trying to find " + src);
		if (OMC.TYPEFACEMAP.get(src)==null) {
			if (type.equals("fs")) {
//				System.out.println("trying to find file " + src);
				if (!new File(src).exists()) {
//					System.out.println("giving up - cannot find file " + src);
					return null;
				}
//				System.out.println("found file, now caching " + src);
				OMC.TYPEFACEMAP.put(src, Typeface.createFromFile(src));
			} else
				OMC.TYPEFACEMAP.put(src, Typeface.createFromAsset(OMC.AM, src));
		}
		
//		System.out.println("Now retrieving from cache: " + src);
		return OMC.TYPEFACEMAP.get(src);
	}

	public static Bitmap getBitmap(String type, String src) {
		if (OMC.BMPMAP.get(src)==null) {
			if (type.equals("fs")) {
				if (!new File(src).exists()) {
					return null;
				}
				OMC.BMPMAP.put(src, BitmapFactory.decodeFile(src));
			} else
				OMC.BMPMAP.put(src, BitmapFactory.decodeResource(OMC.RES, OMC.RES.getIdentifier(src, "drawable", OMC.PKGNAME)));
		}
		return OMC.BMPMAP.get(src);
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
	
	public synchronized static OMCImportedTheme getImportedTheme(final Context context, final String nm){
		if (OMC.IMPORTEDTHEMEMAP.containsKey(nm)){ 
			if (OMC.DEBUG) Log.i("OMCApp",nm + " retrieved from memory.");
			return OMC.IMPORTEDTHEMEMAP.get(nm);
		}
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(OMC.CACHEPATH + nm + ".omc"));
			OMCImportedTheme oResult = (OMCImportedTheme)in.readObject();
			OMC.IMPORTEDTHEMEMAP.put(nm, oResult);
			if (OMC.DEBUG) Log.i("OMCApp",nm + " reloaded from cache.");
			return oResult;
		} catch (Exception e) {
			if (OMC.DEBUG) Log.i("OMCApp","error reloading " + nm + " from cache.");
		
			//e.printStackTrace();
		}
		return null;
	}

	public static void clearImportCache() {
		for (File f:(new File(OMC.CACHEPATH).listFiles())) {
			f.delete();
		}
		OMC.IMPORTEDTHEMEMAP.clear();
	}
		
	public static void removeDirectory(File f) {
		for (File ff:f.listFiles()) {
	    	if (!ff.isDirectory()) ff.delete();
	    	else removeDirectory(ff);
		}
		f.delete();
	}

	public static boolean copyFile(String src, String tgt) {
		try {
//			System.out.println("Copying " + src + " to " + tgt);
			FileOutputStream oTGT = new FileOutputStream(tgt);
			FileInputStream oSRC = new FileInputStream(src);
		    byte[] buffer = new byte[16384];
		    while (oSRC.read(buffer)!= -1){
		    	oTGT.write(buffer);
		    }
		    oTGT.close();
		    oSRC.close();
			return true;
		} catch (Exception e) {
//			System.out.println("Exception copying " + src + " to " + tgt);
//			e.printStackTrace();
			return false;
		}
	}
	
	public static void saveImportedThemeToCache(Context context, String nm) {
		try {
			if (OMC.DEBUG) Log.i("OMCApp",nm + " saving to cache.");
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(OMC.CACHEPATH + nm + ".omc"));
			out.writeObject(OMC.IMPORTEDTHEMEMAP.get(nm));
		} catch (Exception e) {
			if (OMC.DEBUG) Log.i("OMCApp","error saving " + nm + " to cache.");
			//e.printStackTrace();
		}
	}
	
	public static void deleteOneThemeFromCache(String nm) {
		File f = new File(OMC.CACHEPATH);
		for (File ff:f.listFiles()) {
	    	if (!ff.getName().startsWith(nm)) ff.delete();
		}
	}
	
	public static String[] loadStringArray(String sTheme, int aWI, String sKey) {	
		boolean bExternal = OMC.PREFS.getBoolean("external"+aWI,false);
		if (bExternal) {
			try {
				OMCImportedTheme oTheme = OMC.IMPORTEDTHEMEMAP.get(sTheme);
				ArrayList<String> tempAL = oTheme.arrays.get(sKey);
				return tempAL.toArray(new String[tempAL.size()]);
			} catch (Exception e) {
				// Can't find in external stuff... see if it's a seeded array.
				if (OMC.DEBUG) Log.i ("OMCApp","Can't find array " + sKey + " in " + sTheme);
				try {
					return OMC.RES.getStringArray(OMC.RES.getIdentifier(sKey, "array", OMC.PKGNAME));
				} catch (Exception ee) {
					if (OMC.DEBUG) Log.i ("OMCApp","Can't find array " + sKey + " in resources!");
					ee.printStackTrace();
					return null;
				}
				
			}
		} else {
			try {
				return OMC.RES.getStringArray(OMC.RES.getIdentifier(sKey, "array", OMC.PKGNAME));
			} catch (Exception e) {
				if (OMC.DEBUG) Log.i ("OMCApp","Can't find array " + sKey + " in resources!");
				e.printStackTrace();
				return null;
			}
		}
	
	}

	public void widgetClicks() {
		try {
			this.getPackageManager().getPackageInfo("com.sunnykwong.ompc", 0);
			if (OMC.DEBUG)Log.i("OMCApp","OMPC installed, let OMPC handle onclick");
			try {
				unregisterReceiver(OMC.cRC);
			} catch (java.lang.IllegalArgumentException e) {
    			if (OMC.DEBUG)Log.i("OMCApp","OMC's receiver already unregistered - doing nothing");
				//no need to do anything if receiver not registered
			}
		} catch (Exception e) {

			if (OMC.DEBUG)Log.i("OMCApp","OMPC not installed, register self to handle widget clicks");
			//e.printStackTrace();
			try {
				this.registerReceiver(OMC.cRC,OMC.PREFSINTENTFILT);
			} catch (Exception ee) {
    			if (OMC.DEBUG)Log.i("OMCApp","Failed to register self");
				ee.printStackTrace();
			}
		}
		

	}
	
    @Override
    public void onTerminate() {
    	if (!OMCService.STOPNOW2x1 || !OMCService.STOPNOW2x1 || !OMCService.STOPNOW2x1 || !OMCService.STOPNOW2x1) {
        	Log.i("OMCApp","APP TERMINATED - NOT UNREGISTERING RECEIVERS - OMC WILL RESTART");
    		// do nothing
    	} else {
        	Log.i("OMCApp","APP TERMINATED - UNREGISTERING RECEIVERS - OMC WILL NOT RESTART");
    		unregisterReceiver(aRC);
    	}
        OMC.PREFS.edit().commit();
        super.onTerminate();
    }

}
