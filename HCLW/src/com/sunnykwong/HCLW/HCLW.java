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
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import java.io.File;
import org.json.JSONArray;
import org.json.JSONObject;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import android.app.WallpaperManager;
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
	static boolean RENDERWHILESWIPING=true;
	static boolean SLOWPAN=true;
	static String BONUSPHRASE="";
	static final int SLOWPANSPEED = 3;

	static boolean TOPSURF_DITHER, TOPSURF_32BIT, PAINTFG_DITHER, PAINTFG_AA, PAINTFG_FILTERBMP, LWPSURF_32BIT, FLARE_USEHUES;
	static String TOPSURF_FILE, FLARE_FILE;
	static int FLAREHUES[], TOPSURF_HUE;
	static int FIXEDOFFSET=-1;
	static int DEFAULTBRIGHTNESS=100;
	
	static int LWPWIDTH, LWPHEIGHT;
	static int NUMBEROFFLARECOLORS=0;
	static int OFFSETTHISFRAME=0;
	static int faqtoshow = 0;
	static final String[] FAQS = {
		"Nemuro and Xaffron present their impression of the Honeycomb Live Wallpaper!  This Live Wallpaper is light on CPU usage and has been tested to perform on phones from the G1 to the HD2.  Email either one of us for feedback and issues, and we will resolve them ASAP!",
		"v1.1.0 adds a fun easter egg to the wallpaper.  See if you can figure it out and unlock a fun extra feature!",
		"Lightning frequency is adjustable in the preferences.  If you find the wallpaper too flashy, you can disable it completely.",
		"v1.0.8 of this wallpaper cleans up any remaining pixellation issues.  If the wallpaper still does not look right on your screen, just let us know.",
		"Try changing the 'Target Framerate' setting if your homescreen is choppy.  Remember, higher is not always better, even for fast devices! Your homescreen app also has a lot to do with it.",
		"The free version features the basic 'Racing Flares' look.  The premium looks are also available as one-minute trials.",
		"Like this app?  Check out Xaffron's One More Clock Collection for clock widgets that go well with your new live wallpaper!",
		"Can't get this wallpaper to work?  See a bug?  Feature request?  Send Nemuro or Xaffron a message by scrolling down and tapping on 'Contact Artist' or 'Contact Dev'.",
		"You can enable and disable different colored flares for dramatic effect.",
		"The Frantic! flare frequency is not for everyone, but makes for great fireworks... try it out!",
		"Did you know that there is a support thread on XDA-Developers?  I monitor questions on that board on a daily basis.  http://forum.xda-developers.com/showthread.php?t=975413"
	};
	
	static final String EGG = "Congratulations on unlocking the Easter Egg!  There is now a 'Reverse Flow' option in the preferences screen; all four looks, premium and basic, work with the reverse flow.  Please remember to visit our thread on XDA Developers for more news! http://forum.xda-developers.com/showthread.php?t=975413";
	static boolean REVERSE = false;
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

	static final int SPARKEFFECTCOLOR = Color.parseColor("#FFACACAC");
	static final int SEARCHLIGHTEFFECTCOLOR = Color.parseColor("#441B1939");
	static int DEFAULTEFFECTCOLOR = Color.parseColor("#051b1939");

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

	static final Paint PaintFlare = new Paint(), PaintBg = new Paint(), PaintMid = new Paint(), PaintFg =  new Paint(), PaintBuf = new Paint();
    static Rect srcFullRect, tgtFullRect, srcFlareRect, tgtFlareRect;
	static final Matrix TEMPMATRIX = new Matrix(), TEMPMATRIX2 = new Matrix();
    static public int xPixels;
    static public int targetXPixels;
    static public float TouchX = -1;
    static public float TouchY = -1;
    static public float UpX = -1;
    static public float UpY = -1;
    static public long StartTime;
    static public float CenterX;
    static public float CenterY;
    static public float LightningFactor = 1f;
    static public boolean Sparks;
    
    static public long IGNORETOUCHUNTIL;
    
    static public int CURRENTORIENTATION;
    
    static public Bitmap MIDDLE, FG;
    static public Canvas FGCANVAS;
    static public Bitmap[] FLARE;
    
    static public Bitmap BUFFER;
    static public Canvas BUFFERCANVAS;
    
    static public Bitmap SCRNBUFFER;
    static public Canvas SCRNBUFFERCANVAS;
    
    static public boolean Visible;

    static public Intent HCLWMARKETINTENT;
	static public Uri PAIDURI;
    
	@Override
	public void onCreate() {
		super.onCreate();
  
		PKGNAME = getPackageName();
		PREFS = PreferenceManager.getDefaultSharedPreferences(this);
		HCLW.SHOWHELP = PREFS.getBoolean("showhelp", true);
		if (!PREFS.contains("Egg")){
			PREFS.edit().putBoolean("Egg", false).commit();
		}
		if (!PREFS.contains("FrameRates")) {
			PREFS.edit().putString("FrameRates", "25").commit();
		}
		HCLW.FPS = Integer.parseInt(PREFS.getString("FrameRates", "25"));
		HCLW.RENDERWHILESWIPING = PREFS.getBoolean("RenderWhileSwiping", true);
		HCLW.SLOWPAN = PREFS.getBoolean("SlowPan", true);
		HCLW.PaintBg.setColor(Color.WHITE);
		HCLW.PaintMid.setColor(Color.BLACK);
		HCLW.PaintMid.setAlpha(255);
		HCLW.PaintMid.setFilterBitmap(true);
		HCLW.PaintFlare.setColor(Color.WHITE);
		HCLW.PaintFg.setColor(Color.BLUE);
		HCLW.PaintFg.setDither(true);
		HCLW.PaintFg.setFilterBitmap(true);
		HCLW.PaintBuf.setColor(Color.BLUE);
		HCLW.PaintBuf.setDither(true);
		HCLW.PaintBuf.setFilterBitmap(false);
		HCLW.PaintBuf.setAntiAlias(true);
	
		if (JSON) loadFlaresFromJSON();
		prepareBitmaps();
		
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
		try {
			THISVERSION = getPackageManager().getPackageInfo(getPackageName(),
					PackageManager.GET_META_DATA).versionName;
		} catch (NameNotFoundException e) {
			THISVERSION = "1.0.0";
		}

		LASTUPDATEMILLIS = 0l;

		TARGETFPS = 30;

	}
	
	public void prepareBitmaps() {

    	HCLW.SCRNHEIGHT = getResources().getDisplayMetrics().heightPixels;
    	HCLW.SCRNWIDTH = getResources().getDisplayMetrics().widthPixels;
    	HCLW.SCRNLONGEREDGELENGTH = Math.max(SCRNHEIGHT, SCRNWIDTH);
    	HCLW.SCRNSHORTEREDGELENGTH = Math.min(SCRNHEIGHT, SCRNWIDTH);

    	
    	// Let's see if the phone/homescreen has a desired minimum width for the wallpaper.
    	// If it does, we will honor it
    	HCLW.LWPWIDTH = WallpaperManager.getInstance(this).getDesiredMinimumWidth();
    	// If it doesn't, we'll eyeball it - say twice the shorter edge length.
    	if (HCLW.LWPWIDTH<=0) HCLW.LWPWIDTH=HCLW.SCRNSHORTEREDGELENGTH*2;

    	// Next, let's see if the phone/homescreen has a desired minimum height.
		HCLW.LWPHEIGHT = WallpaperManager.getInstance(this).getDesiredMinimumHeight();
    	// If it doesn't, we'll eyeball it - say exactly the longer edge length.
	    if (HCLW.LWPHEIGHT<=0) {
	    	HCLW.LWPHEIGHT=HCLW.SCRNLONGEREDGELENGTH;
	    } else {
	    	// Honeycomb feature - only top portion of LWP gets shown... the lower portion is reserved for wallpaper pickers.
			// Since HCLW's action occurs in the lower portion, we will leave the lower portion blank and pretend the requested LWP size
			// is smaller than it really is.
	    	HCLW.LWPHEIGHT = Math.min(WallpaperManager.getInstance(this).getDesiredMinimumHeight(),HCLW.SCRNLONGEREDGELENGTH);
	    }

		HCLW.SCRNDPI = getResources().getDisplayMetrics().densityDpi;
    	HCLW.CURRENTORIENTATION = getResources().getConfiguration().orientation;
    	if (HCLW.BUFFER!=null)HCLW.BUFFER.recycle();

    	// The flare buffer is fixed at the lower half of a 640x480 canvas (i.e. 640x240)
    	HCLW.BUFFER = Bitmap.createBitmap(640, 240, Bitmap.Config.ARGB_8888);
        HCLW.BUFFERCANVAS = new Canvas(HCLW.BUFFER);

        adjustOrientationOffsets();

        // The scaling factor for the flares -
        // From 640x480 format to the full lwp size.
        SCALEX = (float)1f;
        SCALEY = (float)1f;

        Log.i("HCLW","Requested LWP Dim: " +HCLW.LWPWIDTH + " x " + HCLW.LWPHEIGHT);
        Log.i("HCLW","Detected Screen Dim: " +HCLW.SCRNWIDTH + " x " + HCLW.SCRNHEIGHT);
        Log.i("HCLW","Detected Screen Edges: " +HCLW.SCRNSHORTEREDGELENGTH + " x " + HCLW.SCRNLONGEREDGELENGTH);
        
		Bitmap tempBmp = null;
        if (HCLW.MIDDLE!=null)HCLW.MIDDLE.recycle();
        HCLW.MIDDLE = Bitmap.createBitmap(HCLW.LWPWIDTH,HCLW.LWPHEIGHT,Config.ARGB_8888);
        Canvas c = new Canvas(HCLW.MIDDLE);
        
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inTempStorage=new byte[16*1024];
		if (HCLW.TOPSURF_DITHER) opts.inDither=true;
		if (HCLW.TOPSURF_32BIT) opts.inPreferredConfig= Bitmap.Config.ARGB_8888;

        tempBmp = BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("middlenw", "drawable", HCLW.PKGNAME),opts);
        c.drawBitmap(tempBmp,null,new Rect(0,0,HCLW.LWPWIDTH/2,HCLW.LWPHEIGHT/2),HCLW.PaintFg);
		tempBmp.recycle();
        tempBmp = BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("middlene", "drawable", HCLW.PKGNAME),opts);
        c.drawBitmap(tempBmp,null,new Rect(HCLW.LWPWIDTH/2,0,HCLW.LWPWIDTH,HCLW.LWPHEIGHT/2),HCLW.PaintFg);
		tempBmp.recycle();
        tempBmp = BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("middlesw", "drawable", HCLW.PKGNAME),opts);
        c.drawBitmap(tempBmp,null,new Rect(0,HCLW.LWPHEIGHT/2,HCLW.LWPWIDTH/2,HCLW.LWPHEIGHT),HCLW.PaintFg);
		tempBmp.recycle();
        tempBmp = BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("middlese", "drawable", HCLW.PKGNAME),opts);
        c.drawBitmap(tempBmp,null,new Rect(HCLW.LWPWIDTH/2,HCLW.LWPHEIGHT/2,HCLW.LWPWIDTH,HCLW.LWPHEIGHT),HCLW.PaintFg);
		tempBmp.recycle();
		c=null;
		
		if (HCLW.FG!=null)HCLW.FG.recycle();
		if (HCLW.TOPSURF_FILE==null || !new File(HCLW.TOPSURF_FILE).exists()) {
			HCLW.FG = Bitmap.createBitmap(HCLW.LWPWIDTH, HCLW.LWPHEIGHT, Config.ARGB_8888);
			c = new Canvas(HCLW.FG);
	        tempBmp = BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("topnw", "drawable", HCLW.PKGNAME),opts);
	        c.drawBitmap(tempBmp,null,new Rect(0,0,HCLW.LWPWIDTH/2,HCLW.LWPHEIGHT/2),HCLW.PaintFg);
			tempBmp.recycle();
	        tempBmp = BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("topne", "drawable", HCLW.PKGNAME),opts);
	        c.drawBitmap(tempBmp,null,new Rect(HCLW.LWPWIDTH/2,0,HCLW.LWPWIDTH,HCLW.LWPHEIGHT/2),HCLW.PaintFg);
			tempBmp.recycle();
	        tempBmp = BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("topsw", "drawable", HCLW.PKGNAME),opts);
	        c.drawBitmap(tempBmp,null,new Rect(0,HCLW.LWPHEIGHT/2,HCLW.LWPWIDTH/2,HCLW.LWPHEIGHT),HCLW.PaintFg);
			tempBmp.recycle();
	        tempBmp = BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("topse", "drawable", HCLW.PKGNAME),opts);
	        c.drawBitmap(tempBmp,null,new Rect(HCLW.LWPWIDTH/2,HCLW.LWPHEIGHT/2,HCLW.LWPWIDTH,HCLW.LWPHEIGHT),HCLW.PaintFg);
			tempBmp.recycle();
			c=null;
		} else {
			tempBmp = BitmapFactory.decodeFile(HCLW.TOPSURF_FILE,opts);
			HCLW.FG = Bitmap.createScaledBitmap(tempBmp,HCLW.LWPWIDTH, HCLW.LWPHEIGHT,true);
			tempBmp.recycle();
		}

		if (FGCANVAS==null) FGCANVAS = new Canvas(HCLW.FG);
		FGCANVAS.drawColor(HCLW.TOPSURF_HUE, Mode.SRC_ATOP);
		FGCANVAS=null;
		if (!HCLW.FLARE_USEHUES){
			HCLW.FLARE = new Bitmap[] {
				BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("flare_white", "drawable", HCLW.PKGNAME)),
				BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("flare_red", "drawable", HCLW.PKGNAME)),
				BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("flare_green", "drawable", HCLW.PKGNAME)),
				BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("flare_blue", "drawable", HCLW.PKGNAME)),
				BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("flare_yellow", "drawable", HCLW.PKGNAME))
			};
		} else if (HCLW.FLARE_FILE==null) { 
			HCLW.FLARE = new Bitmap[5];
			HCLW.FLARE[0] = BitmapFactory.decodeResource(this.getResources(), getResources().getIdentifier("flare_white", "drawable", HCLW.PKGNAME));
			for (int i=1;i<5;i++) {
				HCLW.FLARE[i]=HCLW.FLARE[0].copy(Bitmap.Config.ARGB_8888, true);
				Canvas cc = new Canvas(HCLW.FLARE[i]);
				cc.drawColor(HCLW.FLAREHUES[i], Mode.SRC_ATOP);
				cc=null;
			}
		} else {	
			HCLW.FLARE = new Bitmap[5];
			HCLW.FLARE[0] = BitmapFactory.decodeFile(HCLW.FLARE_FILE);
			for (int i=1;i<5;i++) {
				HCLW.FLARE[i]=HCLW.FLARE[0].copy(Bitmap.Config.ARGB_8888, true);
				Canvas cc = new Canvas(HCLW.FLARE[i]);
				cc.drawColor(HCLW.FLAREHUES[i], Mode.SRC_ATOP);
				cc=null;
			}
		}
		HCLW.PaintFg.setFilterBitmap(false);
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
        HCLW.SCRNBUFFER = Bitmap.createBitmap(HCLW.SCRNWIDTH, HCLW.SCRNHEIGHT, Bitmap.Config.ARGB_8888);
        HCLW.SCRNBUFFERCANVAS = new Canvas(HCLW.SCRNBUFFER);

        int Midpoint;
		if (HCLW.SCRNWIDTH < HCLW.SCRNHEIGHT){
			// Portrait
	        HCLW.YOFFSET=0;
			Midpoint = (HCLW.YOFFSET + HCLW.LWPHEIGHT)/2; 
	        HCLW.srcFullRect = new Rect(0,0,HCLW.SCRNWIDTH, HCLW.LWPHEIGHT);
	        HCLW.tgtFullRect = new Rect(0,0,HCLW.SCRNWIDTH, HCLW.LWPHEIGHT);
	        
	        float WidthRatio = HCLW.SCRNWIDTH/(float)HCLW.LWPWIDTH;

	        HCLW.srcFlareRect = new Rect(0,0,(int)(WidthRatio*640),(int)(480/2));
	        HCLW.tgtFlareRect = new Rect(0,Midpoint,HCLW.SCRNWIDTH,HCLW.LWPHEIGHT);
		} else {
			// Landscape
	        HCLW.YOFFSET = HCLW.SCRNHEIGHT-HCLW.SCRNWIDTH;
			Midpoint = (HCLW.YOFFSET + HCLW.SCRNHEIGHT)/2; 
	        HCLW.srcFullRect = new Rect(0,-HCLW.YOFFSET,HCLW.SCRNWIDTH, HCLW.SCRNWIDTH);
	        HCLW.tgtFullRect = new Rect(0,0,HCLW.SCRNWIDTH,HCLW.SCRNHEIGHT);

	        float WidthRatio = HCLW.SCRNWIDTH/(float)HCLW.LWPWIDTH;

	        HCLW.srcFlareRect = new Rect(0,0,(int)(WidthRatio*640),480/2);
	        HCLW.tgtFlareRect = new Rect(0,Midpoint,HCLW.SCRNWIDTH,HCLW.SCRNHEIGHT);
	        
	        //HCLW.tgtFlareRect = new Rect(0,HCLW.LWPWIDTH/2-HCLW.LWPHEIGHT/2,HCLW.LWPHEIGHT,HCLW.LWPWIDTH/2);

		}
	}
	
	static public void resetTheme() {
    		HCLW.PREFS.edit()
    		.putString("HCLWLAF", "Racing Flares")
    		.putBoolean("FlaresAboveSurface", false)
    		.putBoolean("LightningEffect", false)
    		.putBoolean("SparkEffect", false)
    		.putBoolean("Searchlight", false)
    		.putString("LightnFrequency", "0.05")
    		.commit();
    		HCLW.LightningFactor=1f;
	}
	
	public void loadFlaresFromJSON() {
		try {
			File f = new File("/mnt/sdcard/hclw_settings.json");
			// Look in SD path
			JSONObject oObj;
			JSONArray oResult;
			if (f.exists()) {
				Toast.makeText(this, "hclw_settings.json file found on SD card.  Applying advanced settings...", Toast.LENGTH_LONG).show();
				BufferedReader in = new BufferedReader(new FileReader(f),8192);
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    oObj = new JSONObject(sb.toString());
				oResult = oObj.getJSONArray("flarepositions");
				sb.setLength(0);
				TOPSURF_FILE = oObj.getString("topsurface_file");
				if (!new File(TOPSURF_FILE).exists()) TOPSURF_FILE=null;
				FLARE_FILE = oObj.getString("flare_file");
				if (!new File(FLARE_FILE).exists()) FLARE_FILE=null;
			} else {
				// Look in assets
				InputStreamReader in = new InputStreamReader(this.getAssets().open("hclw_settings.json"));
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    oObj = new JSONObject(sb.toString());
				oResult = oObj.getJSONArray("flarepositions");
				sb.setLength(0);
				TOPSURF_FILE = null;
				FLARE_FILE = null;
			}
			
			LWPSURF_32BIT = oObj.getBoolean("livewallpaper_surface_32bit");
			FIXEDOFFSET = oObj.getInt("lwp_fixed_offset");
			PAINTFG_DITHER = oObj.getBoolean("paint_dither");
			PAINTFG_AA = oObj.getBoolean("paint_antialias");
			PAINTFG_FILTERBMP = oObj.getBoolean("paint_filterbitmap");
			TOPSURF_HUE = Color.parseColor(oObj.getString("topsurface_hue"));
			TOPSURF_DITHER = oObj.getBoolean("topsurface_dither");
			TOPSURF_32BIT = oObj.getBoolean("topsurface_32bit");
			FLARE_USEHUES = oObj.getBoolean("flare_use_hues");
			DEFAULTBRIGHTNESS = oObj.getInt("lwp_baseline_brightness");
			FLAREHUES = new int[5];
			for (int i=0;i<5;i++) {
				FLAREHUES[i] = Color.parseColor(oObj.getJSONArray("flare_hues_WRGBY").getString(i));
			}
			oObj=null;
			
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
//			prepareBitmaps();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			//System.gc();
		}
	}
	public void loadEggFromJSON() {
		try {
			JSONObject oObj;
			JSONArray oResult;
			// Look in assets
			InputStreamReader in = new InputStreamReader(this.getAssets().open("hclw_reverse.json"));
			StringBuilder sb = new StringBuilder();
		    char[] buffer = new char[8192];
		    int iCharsRead = 0;
		    while ((iCharsRead=in.read(buffer))!= -1){
		    	sb.append(buffer, 0, iCharsRead);
		    }
		    in.close();
		    oObj = new JSONObject(sb.toString());
			oResult = oObj.getJSONArray("flarepositions");
			sb.setLength(0);
			TOPSURF_FILE = null;
			FLARE_FILE = null;
			
			LWPSURF_32BIT = oObj.getBoolean("livewallpaper_surface_32bit");
			FIXEDOFFSET = oObj.getInt("lwp_fixed_offset");
			PAINTFG_DITHER = oObj.getBoolean("paint_dither");
			PAINTFG_AA = oObj.getBoolean("paint_antialias");
			PAINTFG_FILTERBMP = oObj.getBoolean("paint_filterbitmap");
			TOPSURF_HUE = Color.parseColor(oObj.getString("topsurface_hue"));
			TOPSURF_DITHER = oObj.getBoolean("topsurface_dither");
			TOPSURF_32BIT = oObj.getBoolean("topsurface_32bit");
			FLARE_USEHUES = oObj.getBoolean("flare_use_hues");
			DEFAULTBRIGHTNESS = oObj.getInt("lwp_baseline_brightness");
			FLAREHUES = new int[5];
			for (int i=0;i<5;i++) {
				FLAREHUES[i] = Color.parseColor(oObj.getJSONArray("flare_hues_WRGBY").getString(i));
			}
			oObj=null;
			
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
//			prepareBitmaps();
		} catch (Exception e) {
			e.printStackTrace();
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
