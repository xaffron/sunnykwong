package com.sunnykwong.aurorabulb;

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

/**
 * @author skwong01
 * Thanks to ralfoide's 24clock code; taught me quite a bit about broadcastreceivers
 * Thanks to the Open Font Library for a great resource.
 * 
 */
public class AB extends Application {
	
	static String THISVERSION;
	static final boolean DEBUG = true;

	static long LASTUPDATEMILLIS;
	static int UPDATEFREQ = 100;

	static SharedPreferences PREFS;
    static Resources RES;
    
	static Matrix TEMPMATRIX;

	static final int WIDGETWIDTH=480;
	static final int WIDGETHEIGHT=300;
	static final Time TIME = new Time();

	static String TXTBUF;
	
	static Bitmap SCRNBUFFER;
	static Canvas SCRNCANVAS;
	static Bitmap ROLLBUFFER;
	static Canvas ROLLCANVAS;

	static Paint PT1;
	static Paint PT2;
	
	static float fSCALEX, fSCALEY;

	@Override
	public void onCreate() {
		super.onCreate();

		try {
			THISVERSION = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName;
		} catch (NameNotFoundException e) {
			THISVERSION = "1.0.0";
		}
		
		LASTUPDATEMILLIS = 0l;
		
		SCRNBUFFER= Bitmap.createBitmap(WIDGETWIDTH,WIDGETHEIGHT,Bitmap.Config.ARGB_4444);
		SCRNCANVAS = new Canvas(SCRNBUFFER);
		SCRNCANVAS.setDensity(DisplayMetrics.DENSITY_HIGH);
		PT1 = new Paint();
		PT2 = new Paint();
		TEMPMATRIX = new Matrix();
		
    	RES = getResources();
		PREFS = getSharedPreferences("com.sunnykwong.omc_preferences", Context.MODE_PRIVATE);
		// We are using Zehro's solution (listening for TIME_TICK instead of using AlarmManager + FG Notification) which
		// should be quite a bit more graceful.

		// If we're from a legacy version, then we need to wipe all settings clean to avoid issues.
		if (PREFS.getString("version", "1.0.x").startsWith("1.0")) {
			Log.i("OMCApp","Upgrade from legacy version, wiping all settings.");
			PREFS.edit().clear().commit();
		}

		PREFS.edit().putString("version", THISVERSION).commit();
		UPDATEFREQ = PREFS.getInt("iUpdateFreq", 30) * 1000;
		
	}

    @Override
    public void onTerminate() {
        PREFS.edit().commit();
        super.onTerminate();
    }

}
