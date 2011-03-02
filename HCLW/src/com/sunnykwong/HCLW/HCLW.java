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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;

/**
 * @author skwong01 Thanks to Cosmin Bizon for the idea, graphical assets,
 * constant feedback and reliable testing.
 * 
 */
public class HCLW extends Application {

	static int FPS=25;
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
		"Do the flares not seem to move along the channels?  This is almost certainly because of custom DPI settings.  If you reset your custom DPI settings to what they should be, the wallpaper should work normally.",
		"This free version features a fully-functional 'Racing Flares' look.  The other two looks, 'Lightning Strikes' and 'Electric Sparks', are also available as one-minute trials.",
		"Like this app?  Check out Xaffron's One More Clock Collection for clock widgets that go well with your new live wallpaper!",
		"Can't get this wallpaper to work?  See a bug?  Feature request?  Send Nemuro or Xaffron a message by scrolling down and tapping on 'Contact Artist' or 'Contact Dev'.",
		"You can enable and disable different colored flares for dramatic effect.",
		"The Frantic! flare frequency is not for everyone, but makes for great fireworks... try it out!",
		"Did you know that there is a support thread on XDA-Developers?  I monitor questions on that board on a daily basis.  http://forum.xda-developers.com/showthread.php?t=975413"
	};

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
		466f,474.5f,487f,501f};
	static public final float[] FLAREPATHINITY
		= {322f,322f,322f,336f,
			336f,336f,336f,336f,
			336f,336f,336f,336f};
	static public final float[] FLAREPATHINITZ
    	= {0.1f,0.1f,0.1f,0.1f,
		0.07f,0.1f,0.1f,0.1f,
		0.1f,0.1f,0.1f,0.1f};
	
	static public final float[] FLAREPATHMIDX
		= {162,170,180,257,
		313,355,403,449,
		492,536,556,566};
	static public final float[] FLAREPATHMIDY
		= {376,384,394,385,
		382,384,384,380,
		380,380,369,357};
	static public final float[] FLAREPATHMIDZ
    	= {0.15f,0.15f,0.15f,0.2f,
		0.3f,0.3f,0.35f,0.4f,
		0.35f,0.3f,0.2f,0.2f};

	static public final float[] FLAREPATHFINALX
		= {0,-1,-1,-3,
		73,189,312,435,
		558,645,646,646};
	static public final float[] FLAREPATHFINALY
		= {407,423,445,468,
		480,484,492,492,
		480,452,408,385};
	static public final float[] FLAREPATHFINALZ
    	= {.2f,.2f,.2f,.4f,
		.6f,.7f,.7f,.7f,
		.7f,.5f,.3f,.3f};

	static public final float[] MINFLARESPEEDS
	= {0.003f,0.003f,0.003f,0.0025f,
	0.006f,0.01f,0.015f,0.015f,
	0.010f,0.008f,0.008f,0.008f};

	static public final float[] FLAREACCEL
	= {0.013f,0.013f,0.013f,0.013f,
	0.015f,0.015f,0.02f,0.02f,
	0.02f,0.02f,0.02f,0.02f};

	static public float[] FLARESPEEDS
		= {0.01f,0.01f,0.01f,0.01f,
		0.01f,0.01f,0.01f,0.01f,
		0.01f,0.01f,0.01f,0.01f};

	static public float[] DISPLACEMENTS
	= {0f,0f,0f,0f,
		0f,0f,0f,0f,
		0f,0f,0f,0f};

	static public int[] COLORS
	= {-1,-1,-1,-1,
		-1,-1,-1,-1,
		-1,-1,-1,-1};

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
    		.commit();
    	} else if (sLAF.equals("Lightning Strikes")) {
    		// Lightning Strikes
    		HCLW.PREFS.edit().putBoolean("FlaresAboveSurface", false)
    		.putBoolean("LightningEffect", true)
    		.putBoolean("SparkEffect", false)
    		.commit();
    		
    	} else {
    		// Electric Sparks
    		HCLW.PREFS.edit().putBoolean("FlaresAboveSurface", true)
    		.putBoolean("LightningEffect", false)
    		.putBoolean("SparkEffect", true)
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
        
        switch (SCRNDPI) {
	    	case (DisplayMetrics.DENSITY_HIGH):
	    		SCALEX = HDPISCALEX;
	    		SCALEY = HDPISCALEY;
	    		break;
	    	case (DisplayMetrics.DENSITY_MEDIUM):
	    		SCALEX = MDPISCALEX;
	    		SCALEY = MDPISCALEY;
	    		break;
	    	case (DisplayMetrics.DENSITY_LOW):
	    		SCALEX = LDPISCALEX;
	    		SCALEY = LDPISCALEY;
	    		break;
	    	default:
	    		break;
        }

		HCLW.MIDDLE = BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("middle", "drawable", HCLW.PKGNAME));
		HCLW.FG = BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("top", "drawable", HCLW.PKGNAME));
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
    		.commit();
    		HCLW.LightningFactor=1f;
	}
	
	@Override
	public void onTerminate() {
		PREFS.edit().commit();
		super.onTerminate();
	}

}
