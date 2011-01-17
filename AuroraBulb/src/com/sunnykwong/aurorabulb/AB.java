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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.graphics.BitmapFactory;
import android.content.res.Resources;
import android.graphics.Matrix;
import android.view.Display;

/**
 * @author skwong01
 * Thanks to ralfoide's 24clock code; taught me quite a bit about broadcastreceivers
 * Thanks to the Open Font Library for a great resource.
 * 
 */
public class AB extends Application {
	
	static String THISVERSION;
	static final String PREFNAME = "com.sunnykwong.aurorabulb_preferences";
	static final boolean DEBUG = true;
	static final int BUFFERWIDTH = 240;
	static final int BUFFERHEIGHT = 320;
	static String PREFSCREENTOSHOW="";
	
	static int TARGETFPS;
	static int COUNTDOWNSECONDS;
	
	static long LASTUPDATEMILLIS;
	static int UPDATEFREQ = 100;

	static SharedPreferences PREFS;
    
	static Matrix TEMPMATRIX, TEMPMATRIX2;
	static int SCRNWIDTH;
	static int SCRNHEIGHT;
	static int SCRNDPI;
	
	static final Time TIME = new Time();

	static Bitmap SRCBUFFER;
	static Canvas SRCCANVAS;
	static Bitmap SRCBUFFER2;
	static Canvas SRCCANVAS2;
	static Bitmap ROLLBUFFER;
	static Canvas ROLLCANVAS;
	static Bitmap PREVIEWBUFFER;
	static Canvas PREVIEWCANVAS;
	static Bitmap bmpTemp,bmpTemp2;

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

		TARGETFPS = 30;
	
		COUNTDOWNSECONDS = 10;
		
		AB.PT1 = new Paint();
		AB.PT1.setTextSize(AB.BUFFERHEIGHT);
		AB.PT1.setColor(Color.GREEN);
		AB.PT1.setTextAlign(Paint.Align.CENTER);
		AB.PT1.setAntiAlias(true);

		AB.PT2 = new Paint(AB.PT1);
		AB.PT2.setTextSize(AB.BUFFERHEIGHT*2/3);

		TEMPMATRIX = new Matrix();
		TEMPMATRIX2 = new Matrix();
		
		PREFS = getSharedPreferences(AB.PREFNAME, Context.MODE_PRIVATE);

		AB.initSharedPrefs(PREFS);
		
	}

	static public void initSharedPrefs(SharedPreferences sp){

		// If we're coming from a different version, wipe everything clean.
		if (!THISVERSION.equals(PREFS.getString("version", "1.0.0"))) {
			PREFS.edit().clear().commit();
		}
		
		// Initialize the prefs.
		PREFS.edit().putString("version", THISVERSION).commit();
		if (PREFS.getString("pickFont", "EMPTY").equals("EMPTY")) PREFS.edit().putString("pickFont", "Unibody 8-SmallCaps.otf").commit();
		if (PREFS.getInt("textColor", 0)==0) PREFS.edit().putInt("textColor", Color.GREEN).commit();
		if (PREFS.getString("pickText", "EMPTY").equals("EMPTY")) PREFS.edit().putString("pickText", "Aurora").commit();
		if (PREFS.getString("timeShutterDuration", "EMPTY").equals("EMPTY")) PREFS.edit().putString("timeShutterDuration", "10").commit();
		if (PREFS.getString("timePhotoTimer", "EMPTY").equals("EMPTY")) PREFS.edit().putString("timePhotoTimer", "10").commit();
	}

	static public void updatePreviewBuffer() {
    	//Calibrate text size.
    	AB.PT1.setColor(AB.PREFS.getInt("textColor", Color.GREEN));
    	AB.PT1.setTextSize(50f);

    	AB.PT1.setTextScaleX(1f);
		AB.PT1.setTextAlign(Paint.Align.LEFT);
		int textwidth = (int)AB.PT1.measureText(AB.PREFS.getString("pickText", "Aurora"));

		// Here we update the preview buffer as well.
		if (AB.PREVIEWBUFFER!=null)AB.PREVIEWBUFFER.recycle();
		AB.PREVIEWBUFFER = Bitmap.createBitmap(textwidth+20, Math.max(150, (int)(textwidth*150f/320f) + 20), Bitmap.Config.ARGB_4444); 
		AB.PREVIEWCANVAS = new Canvas(AB.PREVIEWBUFFER);
		AB.PREVIEWCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora"), 10, AB.PREVIEWBUFFER.getHeight()/2, AB.PT1);
	}
	
    static public void updateSrcBuffer(){

    	//ok, set global vars before we pass control to anim.
    	AB.COUNTDOWNSECONDS = Integer.parseInt(AB.PREFS.getString("timePhotoTimer", "10"));

    	//Calibrate text size.
    	AB.PT1.setColor(AB.PREFS.getInt("textColor", Color.GREEN));
    	AB.PT1.setTextSize(300f);
    	int fontascent = AB.PT1.getFontMetricsInt().ascent; 
    	int fontdescent = AB.PT1.getFontMetricsInt().descent;
    	
    	float textScale = AB.BUFFERHEIGHT * 1f / (fontdescent-fontascent);
    	AB.PT1.setTextSize(300f * textScale); 
    	int drawLocn = (int)((0-AB.PT1.getFontMetricsInt().ascent) * textScale);
    	
    	
    	AB.PT1.setColor(AB.PREFS.getInt("textColor", 0));
    	AB.PT2.setColor(Color.WHITE);
    	AB.PT1.setTextScaleX(1f);
		AB.PT1.setTextAlign(Paint.Align.LEFT);
		int textwidth = (int)AB.PT1.measureText(AB.PREFS.getString("pickText", "Aurora"));
		
		int shutterDuration = Integer.parseInt(AB.PREFS.getString("timeShutterDuration", "10"));

		float fPassDist = (float)textwidth/AB.SCRNDPI;
    	AB.PREFS.edit().putString("idealPassDist",String.valueOf(Math.round(fPassDist))).commit();
		
		//Since we are aiming at 30fps, we will have to squeeze/stretch the text.
		//How many lines can we manage over the shutter duration?
		int bufferwidth = shutterDuration * AB.TARGETFPS;
		AB.PT1.setTextScaleX((float)bufferwidth/textwidth);

		if (AB.SRCBUFFER!=null) AB.SRCBUFFER.recycle();
    	AB.SRCBUFFER = Bitmap.createBitmap((int)AB.PT1.measureText(AB.PREFS.getString("pickText", "Aurora")), AB.BUFFERHEIGHT, Bitmap.Config.RGB_565);
    	AB.SRCCANVAS = new Canvas(AB.SRCBUFFER);
    	AB.SRCBUFFER2 = Bitmap.createBitmap((int)AB.PT1.measureText(AB.PREFS.getString("pickText", "Aurora")), AB.BUFFERHEIGHT, Bitmap.Config.RGB_565);
    	AB.SRCCANVAS2 = new Canvas(AB.SRCBUFFER2);
    	AB.PT1.setAlpha(100);
    	AB.SRCCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora"), 0-1, drawLocn-1, AB.PT1);
    	AB.SRCCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora"), 0+1, drawLocn-1, AB.PT1);
    	AB.SRCCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora"), 0-1, drawLocn+1, AB.PT1);
    	AB.SRCCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora"), 0+1, drawLocn+1, AB.PT1);
    	AB.PT1.setAlpha(255);
//    	AB.SRCCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora"), 0, drawLocn-1, AB.PT1);
//    	AB.SRCCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora"), 0, drawLocn+1, AB.PT1);
    	AB.SRCCANVAS2.drawText(AB.PREFS.getString("pickText", "Aurora"), 0, drawLocn, AB.PT1);

    	AB.PT1.setColor(Color.BLACK);
    	AB.SRCCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora"), 0, drawLocn, AB.PT1);
    }
    
    @Override
    public void onTerminate() {
        PREFS.edit().commit();
        super.onTerminate();
    }

}
