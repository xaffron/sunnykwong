package com.sunnykwong.omc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Map.Entry;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
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
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.format.Time;
import android.util.Log;
import android.graphics.BitmapFactory;
import android.content.res.Resources;

/**
 * @author skwong01
 * Thanks to ralfoide's 24clock code; taught me quite a bit about broadcastreceivers
 * Thanks to the Open Font Library for a great resource.
 * 
 */
public class OMC extends Application {
	static int UPDATEFREQ = 15000;
	static final String DEFAULTTHEME = "NixieNotions";
	static final boolean DEBUG = true;
	static final Random RND = new Random();
	static SharedPreferences PREFS;
	static AlarmManager ALARMS;	// I only need one alarmmanager.
	static AssetManager AM;
    static NotificationManager NM;
    static Resources RES;
    
    static HashMap<String, Typeface> TYPEFACEMAP;
    static HashMap<String, Bitmap> BMPMAP;
    static HashMap<String, OMCImportedTheme> IMPORTEDTHEMEMAP;
	
    static OMCConfigReceiver cRC;
	static OMCAlarmReceiver aRC;
    static boolean SCREENON = true; 	// Is the screen on?
    static boolean FG = false;
    static OMCTypedArray LAYERATTRIBS;
	static String[] LAYERLIST, TALKBACKS; 

	static final ComponentName WIDGET4x2CNAME = new ComponentName("com.sunnykwong.omc","com.sunnykwong.omc.ClockWidget4x2");
	static final ComponentName WIDGET3x1CNAME = new ComponentName("com.sunnykwong.omc","com.sunnykwong.omc.ClockWidget3x1");
	static final ComponentName WIDGET2x1CNAME = new ComponentName("com.sunnykwong.omc","com.sunnykwong.omc.ClockWidget2x1");
//	static final int WIDGETWIDTH=640;
//	static final int WIDGETHEIGHT=320;
	static final int WIDGETWIDTH=320;
	static final int WIDGETHEIGHT=200;
	static final String CHINESETIME = "子丑寅卯辰巳午未申酉戌亥子";
	static final Time TIME = new Time();

	static final float[] FLARERADII = new float[] {32.f,20.f,21.6f,40.2f,18.4f,19.1f,10.8f,25.f,28.f};
	static final int[] FLARECOLORS = new int[] {855046894,1140258554,938340342,1005583601,855439588,
		669384692,905573859,1105458423,921566437};
	static String TXTBUF;
	
	static final int SVCNOTIFICATIONID = 1; // Notification ID for the one and only message window we'll show
    static final Class<?>[] mStartForegroundSignature = new Class[] {int.class, Notification.class};
    static final Class<?>[] mStopForegroundSignature = new Class[] {boolean.class};
    static Intent FGINTENT, BGINTENT, SVCSTARTINTENT, WIDGETREFRESHINTENT, CREDITSINTENT, PREFSINTENT, IMPORTTHEMEINTENT;
    static PendingIntent FGPENDING, BGPENDING, PREFSPENDING;
    static IntentFilter PREFSINTENTFILT;
    static Notification FGNOTIFICIATION;
    
	static RectF BGRECT, FGRECT;

	static Bitmap BUFFER;
	static Canvas CANVAS;
	static Paint PT1;
	static Paint PT2;
	
	static float fSCALEX, fSCALEY;

	@Override
	public void onCreate() {
		super.onCreate();

		OMC.BUFFER= Bitmap.createBitmap(OMC.WIDGETWIDTH,OMC.WIDGETHEIGHT,Bitmap.Config.ARGB_4444);
		OMC.CANVAS = new Canvas(OMC.BUFFER);
		OMC.PT1 = new Paint();
		OMC.PT2 = new Paint();
		
		OMC.aRC = new OMCAlarmReceiver();
		OMC.cRC = new OMCConfigReceiver();

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

		OMC.BGRECT = new RectF(30,10,295,150);
		OMC.FGRECT = new RectF(25,5,290,145);
		
		OMC.FGNOTIFICIATION = new Notification(R.drawable.fredicon_mdpi, 
				"Keeping the clock running when memory low.",
        		System.currentTimeMillis());
        OMC.FGNOTIFICIATION.flags = OMC.FGNOTIFICIATION.flags|Notification.FLAG_ONGOING_EVENT|Notification.FLAG_NO_CLEAR;
		
    	OMC.ALARMS = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
    	OMC.AM = getAssets();
    	OMC.RES = getResources();
    	OMC.NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		OMC.PREFS = getSharedPreferences("com.sunnykwong.omc_preferences", Context.MODE_PRIVATE);
		OMC.FG = OMC.PREFS.getBoolean("widgetPersistence", false)? true : false;
		OMC.UPDATEFREQ = OMC.PREFS.getInt("iUpdateFreq", 30) * 1000;
		
		registerReceiver(aRC, new IntentFilter(Intent.ACTION_SCREEN_ON));
		registerReceiver(aRC, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		
		OMC.TYPEFACEMAP = new HashMap<String, Typeface>(6);
		OMC.BMPMAP = new HashMap<String, Bitmap>(3);
		OMC.IMPORTEDTHEMEMAP=new HashMap<String, OMCImportedTheme>(3);
		
		OMC.LAYERLIST = null;
		OMC.LAYERATTRIBS = null;

		OMC.TALKBACKS = null;

		try {
			this.getPackageManager().getPackageInfo("com.sunnykwong.ompc", 0);
			if (OMC.DEBUG)Log.i("OMCPref","OMPC installed, let OMPC handle onclick");
			try {
				unregisterReceiver(OMC.cRC);
			} catch (java.lang.IllegalArgumentException e) {
    			if (OMC.DEBUG)Log.i("OMCPref","OMC's receiver already unregistered - doing nothing");
				//no need to do anything if receiver not registered
			}
		} catch (Exception e) {

			if (OMC.DEBUG)Log.i("OMCPref","OMPC not installed, register self to handle widget clicks");
			//e.printStackTrace();
			try {
				this.registerReceiver(OMC.cRC,OMC.PREFSINTENTFILT);
			} catch (Exception ee) {
    			if (OMC.DEBUG)Log.i("OMCPref","Failed to register self");
				ee.printStackTrace();
			}
		}
		
		// Enable/Disable the various size widgets
		if (!OMC.PREFS.getBoolean("bFourByTwo", true)) {
			getPackageManager().setComponentEnabledSetting(	
					new ComponentName("com.sunnykwong.omc",".ClockWidget4x2"),
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
		}
		if (!OMC.PREFS.getBoolean("bThreeByOne", true)) {
			getPackageManager().setComponentEnabledSetting(	
					new ComponentName("com.sunnykwong.omc",".ClockWidget3x1"),
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
		}
		if (!OMC.PREFS.getBoolean("bTwoByOne", true)) {
			getPackageManager().setComponentEnabledSetting(	
					new ComponentName("com.sunnykwong.omc",".ClockWidget2x1"),
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
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
		if (OMC.TYPEFACEMAP.get(src)==null) {
			if (type.equals("fs")) {
				if (!new File(src).exists()) return null;
				OMC.TYPEFACEMAP.put(src, Typeface.createFromFile(src));
			} else
				OMC.TYPEFACEMAP.put(src, Typeface.createFromAsset(OMC.AM, src));
		}
		return OMC.TYPEFACEMAP.get(src);
	}

	public static Bitmap getBitmap(String type, String src) {
		if (OMC.BMPMAP.get(src)==null) {
			if (type.equals("fs")) {
				if (!new File(src).exists()) return null;
				OMC.BMPMAP.put(src, BitmapFactory.decodeFile(src));
			} else
				OMC.BMPMAP.put(src, BitmapFactory.decodeResource(OMC.RES, OMC.RES.getIdentifier(src, "drawable", "com.sunnykwong.omc")));
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
	
	public static OMCImportedTheme getImportedTheme(Context context, String nm){
		System.out.println("list of private files");
		for (String s:context.fileList()) {
			System.out.println(s);
		}
		if (OMC.IMPORTEDTHEMEMAP.containsKey(nm)){ 
			System.out.println(nm + " retrieved from memory.");
			return OMC.IMPORTEDTHEMEMAP.get(nm);
		}
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(context.getCacheDir().getAbsolutePath() + nm + ".omc"));
			OMCImportedTheme oResult = (OMCImportedTheme)in.readObject();
			OMC.IMPORTEDTHEMEMAP.put(nm, oResult);
			System.out.println(nm + " reloaded from cache.");
			return oResult;
		} catch (Exception e) {
			System.out.println("error reloading from cache.");
		
			e.printStackTrace();
		}
		return null;
	}
	
	public static void saveImportedThemeToCache(Context context, String nm) {
		try {
			System.out.println(nm + " saving to cache.");
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(context.getCacheDir().getAbsolutePath() + "/" + nm + ".omc"));
			out.writeObject(OMC.IMPORTEDTHEMEMAP.get(nm));
		} catch (Exception e) {
			System.out.println("error saving to cache.");
			e.printStackTrace();
		}
	}
	
    @Override
    public void onTerminate() {
        unregisterReceiver(aRC);
        OMC.PREFS.edit().commit();
        super.onTerminate();
    }

}
