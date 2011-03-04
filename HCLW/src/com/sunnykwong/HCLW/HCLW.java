package com.sunnykwong.HCLW;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;

/**
 * @author skwong01 Thanks to Cosmin Bizon for the idea, graphical assets,
 * constant feedback and reliable testing.
 * 
 */
public class HCLW extends Application {

	static int FPS=25;
	static final boolean JSON = true;
	static String THISVERSION;
	static String PKGNAME;
	static final boolean DEBUG = false;
	static final boolean FREEEDITION = false;
	static boolean SHOWHELP=true;

	static int NUMBEROFFLARECOLORS=0;
	static int OFFSETTHISFRAME=0;
	static int faqtoshow = 0;
	static final String[] FAQS = {
		"Nemuro and Xaffron present their impression of the Honeycomb Live Wallpaper!  This Live Wallpaper is light on CPU usage and has been tested to perform on phones from the G1 to the HD2.  Email either one of us for feedback and issues, and we will resolve them ASAP!",
		"Version 1.0.4 of this wallpaper brings tablet support, at a smaller package size.  If the wallpaper still does not look right on your screen, just let us know!",
//		"Starting in v1.0.5, you can change the color of the slopes as well.  However, the app needs to download extra resources from the web, so please be patient.",
		"v1.0.5 adds a new premium look, 'Searchlight', to the one basic ('Racing Flares') and two premium ('Lightning Strikes' and 'Electric Sparks') looks.",
		"Try changing the 'Target Framerate' setting if your homescreen is choppy.  Remember, higher is not always better, even for fast devices! Your homescreen app also has a lot to do with it.",
		"The free version features the basic 'Racing Flares' look.  The premium looks are also available as one-minute trials.",
		"Like this app?  Check out Xaffron's One More Clock Collection for clock widgets that go well with your new live wallpaper!",
		"Can't get this wallpaper to work?  See a bug?  Feature request?  Send Nemuro or Xaffron a message by scrolling down and tapping on 'Contact Artist' or 'Contact Dev'.",
		"You can enable and disable different colored flares for dramatic effect.",
		"The Frantic! flare frequency is not for everyone, but makes for great fireworks... try it out!",
		"Did you know that there is a support thread on XDA-Developers?  I monitor questions on that board on a daily basis.  http://forum.xda-developers.com/showthread.php?t=975413"
	};

	// gold: http://farm6.static.flickr.com/5254/5496531931_92cb027088_b.jpg
	// red: http://farm6.static.flickr.com/5217/5497124656_bb551e9cb1_b.jpg
	
	static public final Handler HANDLER = new Handler();
	
	static int TARGETFPS;

	static long LASTUPDATEMILLIS;
	static long TRIALOVERTIME = 0l;
	static int UPDATEFREQ = 100;

	static SharedPreferences PREFS;

	static int SCRNWIDTH;
	static int SCRNHEIGHT;
	static int SCRNLONGEREDGELENGTH, SCRNSHORTEREDGELENGTH;
	static int SCRNDPI;
	static int YOFFSET;

	static public final float LDPISCALEX=0.2500f, LDPISCALEY=0.2222f;
	static public final float MDPISCALEX=.3333f, MDPISCALEY=.3333f;
	static public final float HDPISCALEX=.5000f, HDPISCALEY=0.5930f;
	static public float SCALEX, SCALEY;
	
	static public final float[] FLAREPATHINITX
		= {264f,277f,288f,404f,
		418f,432f,440f,454f,
		466f,474.5f,487f,501f,0f};
	static public final float[] FLAREPATHINITY
		= {322f,322f,322f,336f,
			336f,336f,336f,336f,
			336f,336f,336f,336f,0f};
	static public final float[] FLAREPATHINITZ
    	= {0.1f,0.1f,0.1f,0.1f,
		0.07f,0.1f,0.1f,0.1f,
		0.1f,0.1f,0.1f,0.1f,12f};
	
	static public final float[] FLAREPATHMIDX
		= {162,170,180,257,
		313,355,403,449,
		492,536,556,566,640f};
	static public final float[] FLAREPATHMIDY
		= {376,384,394,385,
		382,384,384,380,
		380,380,369,357,120f};
	static public final float[] FLAREPATHMIDZ
    	= {0.15f,0.15f,0.15f,0.2f,
		0.3f,0.3f,0.35f,0.4f,
		0.35f,0.3f,0.2f,0.2f,12f};

	static public final float[] FLAREPATHFINALX
		= {0,-1,-1,-3,
		73,189,312,435,
		558,645,646,646,0f};
	static public final float[] FLAREPATHFINALY
		= {407,423,445,468,
		480,484,492,492,
		480,452,408,385,0f};
	static public final float[] FLAREPATHFINALZ
    	= {.2f,.2f,.2f,.4f,
		.6f,.7f,.7f,.7f,
		.7f,.5f,.3f,.3f,12f};

	static public final float[] MINFLARESPEEDS
	= {0.003f,0.003f,0.003f,0.0025f,
	0.006f,0.01f,0.015f,0.015f,
	0.010f,0.008f,0.008f,0.008f,0.03f};

	static public final float[] FLAREACCEL
	= {0.013f,0.013f,0.013f,0.013f,
	0.015f,0.015f,0.02f,0.02f,
	0.02f,0.02f,0.02f,0.02f,0.02f};

	static public float[] FLARESPEEDS
		= {0.01f,0.01f,0.01f,0.01f,
		0.01f,0.01f,0.01f,0.01f,
		0.01f,0.01f,0.01f,0.01f,0.02f};

	static public float[] DISPLACEMENTS
	= {0f,0f,0f,0f,
		0f,0f,0f,0f,
		0f,0f,0f,0f,0f};

	static public int[] COLORS
	= {-1,-1,-1,-1,
		-1,-1,-1,-1,
		-1,-1,-1,-1,-1};

	static final Paint PaintFlare = new Paint(), PaintBg = new Paint(), PaintMid = new Paint(), PaintFg =  new Paint();
    static Rect srcFullRect, tgtFullRect, srcFlareRect, tgtFlareRect;
	static final Matrix TEMPMATRIX = new Matrix(), TEMPMATRIX2 = new Matrix();
    static public int xPixels;
    static public float TouchX = -1;
    static public float TouchY = -1;
    static public long StartTime;
    static public float CenterX;
    static public float CenterY;
    static public float LightningFactor = 1f;
    static public boolean Sparks;
    
    static public long IGNORETOUCHUNTIL;
    
    static public int CURRENTORIENTATION;
    
    static public Bitmap MIDDLE, FG;
    static public Bitmap[] FLARE;
    
    static public Bitmap BUFFER;
    static public Canvas BUFFERCANVAS;
    
    static public boolean Visible;

    static public Intent HCLWMARKETINTENT;
	static public Uri PAIDURI;
    
	@Override
	public void onCreate() {
		super.onCreate();
  
		PKGNAME = getPackageName();
		PREFS = PreferenceManager.getDefaultSharedPreferences(this);
		HCLW.SHOWHELP = PREFS.getBoolean("showhelp", true);

		if (!PREFS.contains("FrameRates")) {
			PREFS.edit().putString("FrameRates", "25").commit();
		}
		HCLW.FPS = Integer.parseInt(PREFS.getString("FrameRates", "25"));
		
		if (JSON) loadFlaresFromJSON();
		
		for (int i=0;i<5;i++) {
			if (!PREFS.contains("showcolor"+i)) PREFS.edit().putBoolean("showcolor"+i, true).commit();
		}
		
		HCLW.PAIDURI = (Uri.parse("market://details?id=com.sunnykwong.HCLW"));
		HCLW.HCLWMARKETINTENT = new Intent(Intent.ACTION_VIEW,HCLW.PAIDURI);
		
		String sLAF = HCLW.PREFS.getString("HCLWLAF", "Racing Flares");
    	if (sLAF.equals("Racing Flares")) {
    		HCLW.PREFS.edit().putBoolean("FlaresAboveSurface", false)
    		.putBoolean("LightningEffect", false)
    		.putBoolean("SparkEffect", false)
    		.putString("FlareFreqValues", "1")
    		.putString("TrailLength", "#051b1939")
    		.putBoolean("Searchlight", false)
    		.commit();
    	} else if (sLAF.equals("Lightning Strikes")) {
    		// Lightning Strikes
    		HCLW.PREFS.edit().putBoolean("FlaresAboveSurface", false)
    		.putBoolean("LightningEffect", true)
    		.putBoolean("SparkEffect", false)
    		.putBoolean("Searchlight", false)
    		.commit();
    		
       	} else if (sLAF.equals("Searchlight")) {
    		// Searchlight
    		HCLW.PREFS.edit().putBoolean("FlaresAboveSurface", false)
    		.putBoolean("LightningEffect", false)
    		.putBoolean("Searchlight", true)
    		.commit();
    	} else {
    		// Electric Sparks
    		HCLW.PREFS.edit().putBoolean("FlaresAboveSurface", true)
    		.putBoolean("LightningEffect", false)
    		.putBoolean("SparkEffect", true)
    		.putBoolean("Searchlight", false)
    		.commit();
    	}
    	countFlareColors();
    	HCLW.SCRNDPI = getResources().getDisplayMetrics().densityDpi;
    	HCLW.SCRNHEIGHT = getResources().getDisplayMetrics().heightPixels;
    	HCLW.SCRNWIDTH = getResources().getDisplayMetrics().widthPixels;
    	HCLW.SCRNLONGEREDGELENGTH = Math.max(SCRNHEIGHT, SCRNWIDTH);
    	HCLW.SCRNSHORTEREDGELENGTH = Math.min(SCRNHEIGHT, SCRNWIDTH);
    	HCLW.CURRENTORIENTATION = getResources().getConfiguration().orientation;
    	HCLW.BUFFER = Bitmap.createBitmap(SCRNSHORTEREDGELENGTH*2/3, SCRNLONGEREDGELENGTH/6, Bitmap.Config.ARGB_8888);
        HCLW.BUFFERCANVAS = new Canvas(HCLW.BUFFER);

        adjustOrientationOffsets();
        
        SCALEX = HCLW.SCRNSHORTEREDGELENGTH/3f/320f;
        SCALEY = HCLW.SCRNLONGEREDGELENGTH/3f/480f;
        
		HCLW.MIDDLE = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("middle", "drawable", HCLW.PKGNAME)),HCLW.SCRNSHORTEREDGELENGTH*2,HCLW.SCRNLONGEREDGELENGTH,true);
		HCLW.FG = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("top", "drawable", HCLW.PKGNAME)),HCLW.SCRNSHORTEREDGELENGTH*2,HCLW.SCRNLONGEREDGELENGTH,true);
//		Canvas c = new Canvas(HCLW.FG);
//		c.drawColor(Color.parseColor("#FFFF0000"), Mode.MULTIPLY);
		HCLW.FLARE = new Bitmap[] {
			BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("flare_white", "drawable", HCLW.PKGNAME)),
			BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("flare_red", "drawable", HCLW.PKGNAME)),
			BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("flare_green", "drawable", HCLW.PKGNAME)),
			BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("flare_blue", "drawable", HCLW.PKGNAME)),
			BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("flare_yellow", "drawable", HCLW.PKGNAME))
		};
		try {
			THISVERSION = getPackageManager().getPackageInfo(getPackageName(),
					PackageManager.GET_META_DATA).versionName;
		} catch (NameNotFoundException e) {
			THISVERSION = "1.0.0";
		}

		LASTUPDATEMILLIS = 0l;

		TARGETFPS = 30;

		HCLW.PaintBg.setColor(Color.WHITE);
		HCLW.PaintMid.setColor(Color.BLACK);
		HCLW.PaintMid.setAlpha(255);
		HCLW.PaintFlare.setColor(Color.WHITE);
		HCLW.PaintFg.setColor(Color.BLUE);
		
	}

	public void countFlareColors() {
		NUMBEROFFLARECOLORS=0;
		for (int i=0; i<5; i++) {
			if (HCLW.PREFS.getBoolean("showcolor"+i, true)) NUMBEROFFLARECOLORS++;
		}
	}

	public void adjustOrientationOffsets(){
		HCLW.SCRNWIDTH=getResources().getDisplayMetrics().widthPixels;
		HCLW.SCRNHEIGHT=getResources().getDisplayMetrics().heightPixels;
		if (HCLW.SCRNWIDTH < HCLW.SCRNHEIGHT){
			// Portrait
	        HCLW.YOFFSET=0;
	        HCLW.srcFullRect = new Rect(0,0,HCLW.SCRNWIDTH, HCLW.SCRNHEIGHT);
	        HCLW.tgtFullRect = new Rect(0,0,HCLW.SCRNWIDTH,HCLW.SCRNHEIGHT);
	        HCLW.srcFlareRect = new Rect(0,0,HCLW.SCRNWIDTH/3, HCLW.SCRNHEIGHT/6);
	        HCLW.tgtFlareRect = new Rect(0,HCLW.SCRNHEIGHT/2,HCLW.SCRNWIDTH,HCLW.SCRNHEIGHT);
		} else {
			// Landscape
	        HCLW.YOFFSET = HCLW.SCRNHEIGHT-HCLW.SCRNWIDTH;
	        HCLW.srcFullRect = new Rect(0,-HCLW.YOFFSET,HCLW.SCRNWIDTH, HCLW.SCRNWIDTH);
	        HCLW.tgtFullRect = new Rect(0,0,HCLW.SCRNWIDTH,HCLW.SCRNHEIGHT);
	        
	        HCLW.srcFlareRect = new Rect(0,0,HCLW.SCRNWIDTH/3, HCLW.SCRNWIDTH/6);
	        HCLW.tgtFlareRect = new Rect(0,HCLW.SCRNHEIGHT-HCLW.SCRNWIDTH/2,HCLW.SCRNWIDTH,HCLW.SCRNHEIGHT);
		}
	}
	
	static public void resetTheme() {
    		HCLW.PREFS.edit()
    		.putString("HCLWLAF", "Racing Flares")
    		.putBoolean("FlaresAboveSurface", false)
    		.putBoolean("LightningEffect", false)
    		.putBoolean("SparkEffect", false)
    		.putBoolean("Searchlight", false)
    		.commit();
    		HCLW.LightningFactor=1f;
	}
	
	public void loadFlaresFromJSON() {
		// Look in SD path
//			File f = new File("/mnt/sdcard/hclw_flares.json");
			try {
//				BufferedReader in = new BufferedReader(new FileReader("/mnt/sdcard/hclw_flares.json"),8192);
				InputStreamReader in = new InputStreamReader(this.getAssets().open("hclw_flares.json"));
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
				JSONArray oResult = new JSONArray(sb.toString());
				sb.setLength(0);
				
				// now read flare data to our format
				for (int i=0;i<12;i++) {
					JSONObject flare = oResult.getJSONObject(i);
					HCLW.FLAREPATHINITX[i] = (float)(flare.optJSONArray("initial").getDouble(0));
					HCLW.FLAREPATHINITY[i] = (float)(flare.optJSONArray("initial").getDouble(1));
					HCLW.FLAREPATHINITZ[i] = (float)(flare.optJSONArray("initial").getDouble(2));

					HCLW.FLAREPATHMIDX[i] = (float)(flare.optJSONArray("middle").getDouble(0));
					HCLW.FLAREPATHMIDY[i] = (float)(flare.optJSONArray("middle").getDouble(1));
					HCLW.FLAREPATHMIDZ[i] = (float)(flare.optJSONArray("middle").getDouble(2));

					HCLW.FLAREPATHFINALX[i] = (float)(flare.optJSONArray("final").getDouble(0));
					HCLW.FLAREPATHFINALY[i] = (float)(flare.optJSONArray("final").getDouble(1));
					HCLW.FLAREPATHFINALZ[i] = (float)(flare.optJSONArray("final").getDouble(2));
					
					HCLW.MINFLARESPEEDS[i] = (float)(flare.optDouble("minimumspeed"));
					HCLW.FLAREACCEL[i] = (float)(flare.optDouble("accel"));
					
				}
				oResult = null;
				
			} catch (Exception e) {
				
			} finally {
				//System.gc();
			}
	}

	@Override
	public void onTerminate() {
		PREFS.edit().commit();
		super.onTerminate();
	}

}
