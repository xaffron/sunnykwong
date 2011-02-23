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

	static String THISVERSION;
	static String PKGNAME;
	static final boolean DEBUG = false;
	static final boolean FREEEDITION = true;
	static boolean SHOWHELP=true;
	
	static int faqtoshow = 0;
	static final String[] FAQS = {
		"Nemuro and Xaffron present their impression of the Honeycomb Live Wallpaper!  This Live Wallpaper is light on CPU usage and has been tested to perform on phones from the G1 to the HD2.  Email either one of us for feedback and issues, and we will resolve them ASAP!",
		"Do the flares not seem to move along the channels?  This is almost certainly because of custom DPI settings.  If you reset your custom DPI settings to what they should be, the wallpaper should work normally.",
		"This free version features a fully-functional 'Racing Flares' look.  The other two looks, 'Lightning Strikes' and 'Electric Sparks', are also available as 5 minute trials.",
		"Like this app?  Check out Xaffron's One More Clock Collection for clock widgets that go well with your new live wallpaper!",
		"Can't get this wallpaper to work?  See a bug?  Feature request?  Send Nemuro or Xaffron a message by scrolling down and tapping on 'Contact Artist' or 'Contact Dev'.",
		"You can enable and disable different colored flares for dramatic effect.",
		"The Frantic! flare frequency is not for everyone, but makes for great fireworks... try it out!",
		"Did you know that there is a support thread on XDA-Developers?  I monitor questions on that board on a daily basis.  http://forum.xda-developers.com/showthread.php?t=807929"
	};

	static public final Handler HANDLER = new Handler();
    static public final Runnable rTRIALOVER = new Runnable() {
        public void run() {
            HCLW.resetTheme();
        }
    };

	
	static int TARGETFPS;

	static long LASTUPDATEMILLIS;
	static int UPDATEFREQ = 100;

	static SharedPreferences PREFS;

	static int SCRNWIDTH;
	static int SCRNHEIGHT;
	static int SCRNDPI;

	static public final float LDPISCALEX=0.25f, LDPISCALEY=0.25f;
	static public final float MDPISCALEX=.33f, MDPISCALEY=.33f;
	static public final float HDPISCALEX=.5f, HDPISCALEY=0.59f;
	static public float SCALEX, SCALEY;
	
	static public final float[] FLAREPATHINITX
		= {269f,280f,297f,422f,
		432f,442f,448f,458f,
		468f,480f,486f,502f};
	static public final float[] FLAREPATHINITY
		= {322f,322f,322f,334f,
			336f,338f,338f,338f,
			338f,338f,338f,338f};
	static public final float[] FLAREPATHINITZ
    	= {0.1f,0.1f,0.1f,0.1f,
		0.1f,0.1f,0.15f,0.15f,
		0.1f,0.1f,0.1f,0.1f};
	
	static public final float[] FLAREPATHMIDX
		= {163,171,181,273,
		322,360,412,452,
		492,534,577,572};
	static public final float[] FLAREPATHMIDY
		= {380,386,395,385,
		381,384,384,380,
		380,380,380,361};
	static public final float[] FLAREPATHMIDZ
    	= {0.2f,0.2f,0.2f,0.3f,
		0.3f,0.3f,0.4f,0.4f,
		0.4f,0.3f,0.2f,0.2f};

	static public final float[] FLAREPATHFINALX
		= {0,0,0,0,
		76,192,318,437,
		558,645,645,645};
	static public final float[] FLAREPATHFINALY
		= {410,424,446,472,
		484,484,492,492,
		480,452,408,387};
	static public final float[] FLAREPATHFINALZ
    	= {.25f,.3f,.3f,.5f,
		.5f,.5f,.7f,.7f,
		.7f,.5f,.3f,.3f};

	static public final float[] MINFLARESPEEDS
	= {0.01f,0.01f,0.01f,0.012f,
	0.012f,0.02f,0.03f,0.03f,
	0.02f,0.015f,0.015f,0.015f};

	static public float[] FLARESPEEDS
		= {0.01f,0.01f,0.01f,0.01f,
		0.01f,0.01f,0.01f,0.01f,
		0.01f,0.01f,0.01f,0.01f};

	static public float[] DISPLACEMENTS
	= {0f,0f,0f,0f,
		0f,0f,0f,0f,
		0f,0f,0f,0f};

	static public int[] COLORS
	= {0,0,0,0,
		0,0,0,0,
		0,0,0,0};

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

		HCLW.PAIDURI = (Uri.parse("market://details?id=com.sunnykwong.HCLW"));
		HCLW.HCLWMARKETINTENT = new Intent(Intent.ACTION_VIEW,HCLW.PAIDURI);
		
		String sLAF = HCLW.PREFS.getString("HCLWLAF", "Racing Flares");
    	if (sLAF.equals("Racing Flares")) {
    		HCLW.PREFS.edit().putBoolean("FlaresAboveSurface", false)
    		.putBoolean("LightningEffect", false)
    		.putBoolean("SparkEffect", false)
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
  
		SCRNDPI = getResources().getDisplayMetrics().densityDpi;
		
        switch (SCRNDPI) {
	    	case (DisplayMetrics.DENSITY_HIGH):
	    		SCALEX = HDPISCALEX;
	    		SCALEY = HDPISCALEY;
	    		BUFFER = Bitmap.createBitmap(960/3, 427/3, Bitmap.Config.ARGB_8888);
	    		SCRNHEIGHT = 854;
	    		SCRNWIDTH = 480;
	    		break;
	    	case (DisplayMetrics.DENSITY_MEDIUM):
	    		SCALEX = MDPISCALEX;
	    		SCALEY = MDPISCALEY;
	    		BUFFER = Bitmap.createBitmap(640/3, 240/3, Bitmap.Config.ARGB_8888);
	    		SCRNHEIGHT = 480;
	    		SCRNWIDTH = 640;
	    		break;
	    	case (DisplayMetrics.DENSITY_LOW):
	    		SCALEX = LDPISCALEX;
	    		SCALEY = LDPISCALEY;
	    		BUFFER = Bitmap.createBitmap(480/3, 160/3, Bitmap.Config.ARGB_8888);
	    		SCRNHEIGHT = 320;
	    		SCRNWIDTH = 480;
	    		break;
	    	default:
	    		break;
        }

        HCLW.BUFFERCANVAS = new Canvas(HCLW.BUFFER);
        HCLW.srcFullRect = new Rect(0,0,SCRNWIDTH, SCRNHEIGHT);
        HCLW.tgtFullRect = new Rect(0,0,SCRNWIDTH,SCRNHEIGHT);
        HCLW.srcFlareRect = new Rect(0,0,SCRNWIDTH/3, SCRNHEIGHT/6);
        HCLW.tgtFlareRect = new Rect(0,SCRNHEIGHT/2,SCRNWIDTH,SCRNHEIGHT);
        
		HCLW.MIDDLE = BitmapFactory.decodeResource(this.getResources(), R.drawable.middle);
		HCLW.FG = BitmapFactory.decodeResource(this.getResources(), R.drawable.top);
		HCLW.FLARE = new Bitmap[] {
			BitmapFactory.decodeResource(this.getResources(), R.drawable.flare_white),
			BitmapFactory.decodeResource(this.getResources(), R.drawable.flare_red),
			BitmapFactory.decodeResource(this.getResources(), R.drawable.flare_green),
			BitmapFactory.decodeResource(this.getResources(), R.drawable.flare_blue),
			BitmapFactory.decodeResource(this.getResources(), R.drawable.flare_yellow)
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

	static public void resetTheme() {
			HCLW.HANDLER.removeCallbacks(HCLW.rTRIALOVER);
    		HCLW.PREFS.edit()
    		.putString("HCLWLAF", "Racing Flares")
    		.putBoolean("FlaresAboveSurface", false)
    		.putBoolean("LightningEffect", false)
    		.putBoolean("SparkEffect", false)
    		.commit();
	}
	
	@Override
	public void onTerminate() {
		PREFS.edit().commit();
		super.onTerminate();
	}

}
