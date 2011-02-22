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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.content.SharedPreferences;

/**
 * @author skwong01 Thanks to Cosmin Bizon for the idea, graphical assets,
 * constant feedback and reliable testing.
 * 
 */
public class HCLW extends Application {

	static String THISVERSION;
	static String PKGNAME;
	static final boolean DEBUG = false;

	static int TARGETFPS;

	static long LASTUPDATEMILLIS;
	static int UPDATEFREQ = 100;

	static SharedPreferences PREFS;

	static int SCRNWIDTH;
	static int SCRNHEIGHT;
	static int SCRNDPI;

	static public final float LDPISCALEX=0.75f, LDPISCALEY=0.75f;
	static public final float MDPISCALEX=1f, MDPISCALEY=1f;
	static public final float HDPISCALEX=1.5f, HDPISCALEY=1.78f;
	static public float SCALEX, SCALEY;
	
	static public final float[] FLAREPATHINITX
		= {263f,275f,291f,412f,
		423f,438f,445f,453f,
		463f,477f,489f,507f};
	static public final float[] FLAREPATHINITY
		= {322f,322f,322f,334f,
			336f,334f,334f,334f,
			334f,336f,338f,338f};
	static public final float[] FLAREPATHINITZ
    	= {0.15f,0.15f,0.15f,0.15f,
		0.1f,0.1f,0.1f,0.2f,
		0.1f,0.1f,0.1f,0.1f};
	
	static public final float[] FLAREPATHMIDX
		= {163,171,181,273,
		315,360,404,448,
		492,534,577,572};
	static public final float[] FLAREPATHMIDY
		= {376,383,393,378,
		378,376,376,376,
		376,376,376,358};
	static public final float[] FLAREPATHMIDZ
    	= {0.2f,0.2f,0.2f,0.3f,
		0.3f,0.4f,0.4f,0.4f,
		0.3f,0.4f,0.2f,0.2f};

	static public final float[] FLAREPATHFINALX
		= {0,0,0,0,
		76,192,315,437,
		558,640,640,640};
	static public final float[] FLAREPATHFINALY
		= {407,421,443,469,
		481,480,480,480,
		480,447,403,379};
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
	static final Matrix TEMPMATRIX = new Matrix(), TEMPMATRIX2 = new Matrix();
    static public float Offset;
    static public float TouchX = -1;
    static public float TouchY = -1;
    static public long StartTime;
    static public float CenterX;
    static public float CenterY;
    static public float LightningFactor;
    static public boolean Sparks;
    
    static public long IGNORETOUCHUNTIL;
    
    static public Bitmap MIDDLE, FG;
    static public Bitmap[] FLARE;
    
    static public Bitmap BUFFER;
    static public Canvas BUFFERCANVAS;
    
    static public boolean Visible;

	
	@Override
	public void onCreate() {
		super.onCreate();

		PKGNAME = getPackageName();
		PREFS = PreferenceManager.getDefaultSharedPreferences(this);

        switch (getResources().getDisplayMetrics().densityDpi) {
	    	case (DisplayMetrics.DENSITY_HIGH):
	    		SCALEX = HDPISCALEX;
	    		SCALEY = HDPISCALEY;
	    		BUFFER = Bitmap.createBitmap(960, 854, Bitmap.Config.RGB_565);
	    		break;
	    	case (DisplayMetrics.DENSITY_MEDIUM):
	    		SCALEX = MDPISCALEX;
	    		SCALEY = MDPISCALEY;
	    		BUFFER = Bitmap.createBitmap(640, 480, Bitmap.Config.RGB_565);
	    		break;
	    	case (DisplayMetrics.DENSITY_LOW):
	    		SCALEX = LDPISCALEX;
	    		SCALEY = LDPISCALEY;
	    		BUFFER = Bitmap.createBitmap(480, 320, Bitmap.Config.RGB_565);
	    		break;
	    	default:
	    		break;
        }

        HCLW.BUFFERCANVAS = new Canvas(HCLW.BUFFER);
        
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

	@Override
	public void onTerminate() {
		PREFS.edit().commit();
		super.onTerminate();
	}

}
