package com.sunnykwong.aurorabulb;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.text.format.Time;

/**
 * @author skwong01
 * Thanks to ralfoide's 24clock code; taught me quite a bit about broadcastreceivers
 * Thanks to the Open Font Library for a great resource.
 * 
 */
public class AB extends Application {
	
	static MediaPlayer BEEPER;
	static String THISVERSION;
	static final String PREFNAME = "com.sunnykwong.aurorabulb_preferences";
	static final boolean DEBUG = false;
	static final int BUFFERWIDTH = 240;
	static final int BUFFERHEIGHT = 320;
	static String PREFSCREENTOSHOW="";

	// BEGIN Request Codes for the activity.	
	static final int RENDERAURORA = 0;
	static final int SELECTIMAGE = 1;
	// END Request Codes for the activity.	
	
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

	static Bitmap BMPTODRAW;
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
			
		AB.BEEPER = MediaPlayer.create(this, R.raw.beep7);
		
		AB.PT1 = new Paint();
		AB.PT1.setTextSize(AB.BUFFERHEIGHT);
		AB.PT1.setColor(Color.GREEN);
		AB.PT1.setTextAlign(Paint.Align.CENTER);
		AB.PT1.setAntiAlias(true);

		AB.PT2 = new Paint(AB.PT1);
		AB.PT2.setTextSize(AB.BUFFERHEIGHT*2/3);
		AB.PT2.setColor(Color.WHITE);

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
		if (PREFS.getString("whatToShow", "EMPTY").equals("EMPTY")) PREFS.edit().putString("whatToShow", "text").commit();
		if (PREFS.getString("pickFont", "EMPTY").equals("EMPTY")) PREFS.edit().putString("pickFont", "Unibody 8-SmallCaps.otf").commit();
		if (PREFS.getInt("textColor", 0)==0) PREFS.edit().putInt("textColor", Color.GREEN).commit();
		if (PREFS.getString("pickText", "EMPTY").equals("EMPTY")) PREFS.edit().putString("pickText", "Aurora").commit();
		if (PREFS.getString("timeShutterDuration", "EMPTY").equals("EMPTY")) PREFS.edit().putString("timeShutterDuration", "10").commit();
		if (PREFS.getString("timePhotoTimer", "EMPTY").equals("EMPTY")) PREFS.edit().putString("timePhotoTimer", "10").commit();
	}

	static public void updatePreviewBuffer() {
		if (AB.PREFS.getString("whatToShow", "text").equals("bitmap")) {
			// Bitmap stuff
			// Here we update the preview buffer.
			if (AB.PREVIEWBUFFER!=null)AB.PREVIEWBUFFER.recycle();
			if (AB.BMPTODRAW==null) {
				AB.PREVIEWBUFFER = Bitmap.createBitmap(320, 170, Bitmap.Config.ARGB_4444); 
			} else {
				AB.PREVIEWBUFFER = AB.BMPTODRAW.copy(Bitmap.Config.ARGB_4444, false); 
			}
		} else {
			// Text stuff
	    	//Calibrate text size.
	    	AB.PT1.setColor(AB.PREFS.getInt("textColor", Color.GREEN));
	    	AB.PT1.setTextSize(50f);

	    	AB.PT1.setTextScaleX(1f);
			AB.PT1.setTextAlign(Paint.Align.LEFT);
			int textwidth = (int)AB.PT1.measureText(AB.PREFS.getString("pickText", "Aurora"));

			// Here we update the preview buffer.
			if (AB.PREVIEWBUFFER!=null)AB.PREVIEWBUFFER.recycle();
			AB.PREVIEWBUFFER = Bitmap.createBitmap(textwidth+20, Math.max(150, (int)(textwidth*150f/320f) + 20), Bitmap.Config.ARGB_4444); 
			AB.PREVIEWCANVAS = new Canvas(AB.PREVIEWBUFFER);
			AB.PREVIEWCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora"), 10, AB.PREVIEWBUFFER.getHeight()/2, AB.PT1);
		}
	}
	
    static public void updateSrcBuffer(){

		//ok, set global vars before we pass control to anim.
    	AB.COUNTDOWNSECONDS = Integer.parseInt(AB.PREFS.getString("timePhotoTimer", "10"));

		if (AB.PREFS.getString("whatToShow", "text").equals("bitmap")) {
			//bitmap stuff.
			if (AB.SRCBUFFER!=null) AB.SRCBUFFER.recycle();
			float fYScale = ((float)AB.BUFFERHEIGHT)/AB.BMPTODRAW.getHeight();

			//Since we are aiming at 30fps, we will have to squeeze/stretch the image.
			//How many lines can we manage over the shutter duration?
			int shutterDuration = Integer.parseInt(AB.PREFS.getString("timeShutterDuration", "10"));
			float fXScale = (float)(shutterDuration * AB.TARGETFPS)/AB.BMPTODRAW.getWidth();

			AB.SRCBUFFER = Bitmap.createScaledBitmap(AB.BMPTODRAW, (int)(AB.BMPTODRAW.getWidth()*fXScale), (int)(AB.BMPTODRAW.getHeight()*fYScale), true);
	    	AB.SRCBUFFER2 = AB.SRCBUFFER;

	    	AB.PT2.setColor(Color.WHITE);
		} else {
			//text stuff.
	    	//Calibrate text size.
	    	AB.PT1.setColor(AB.PREFS.getInt("textColor", Color.GREEN));
	    	AB.PT1.setTextSize(320f);
	    	System.out.println("textsz " + (int)AB.PT1.getTextSize() + " top " + AB.PT1.getFontMetricsInt().top + " bottom " + AB.PT1.getFontMetricsInt().bottom);
	    	System.out.println("textsz " + (int)AB.PT1.getTextSize() + " ascent " + AB.PT1.getFontMetricsInt().ascent + " descent " + AB.PT1.getFontMetricsInt().descent);
	    	int fontascent, fontdescent;
	    	String sTemp = AB.PREFS.getString("pickFont", "Unibody 8-SmallCaps.otf");
	    	if (sTemp.equals("Unibody 8-SmallCaps.otf")) {
	    		fontascent = -280;
	    		fontdescent = 80;
	    	} else if (sTemp.equals("Forelle.ttf")) {
	    		fontascent = -250;
	    		fontdescent = 50;
	       	} else if (sTemp.equals("YESTERDAYSMEAL.ttf")) {
	    		fontascent = -260;
	    		fontdescent = 70;
	       	} else if (sTemp.equals("EFON.ttf")) {
	    		fontascent = -260;
	    		fontdescent = 3;
	       	} else if (sTemp.equals("Clockopia.ttf")) {
	    		fontascent = -230;
	    		fontdescent = 80;
	    	} else {
	    		
	    		fontascent = AB.PT1.getFontMetricsInt().ascent; 
	    		fontdescent = AB.PT1.getFontMetricsInt().descent;
	    	}
	    	float textScale = AB.BUFFERHEIGHT * 1f / (fontdescent-fontascent);
	    	AB.PT1.setTextSize(320f * textScale); 
	    	int drawLocn = (int)((0-fontascent) * textScale);

	    	
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
	    	AB.SRCCANVAS2.drawText(AB.PREFS.getString("pickText", "Aurora"), 0, drawLocn, AB.PT1);

	    	AB.PT1.setColor(Color.BLACK);
	    	AB.SRCCANVAS.drawText(AB.PREFS.getString("pickText", "Aurora"), 0, drawLocn, AB.PT1);
		}
    }
    
    @Override
    public void onTerminate() {
        PREFS.edit().commit();
        super.onTerminate();
    }

}
