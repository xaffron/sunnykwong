package com.sunnykwong.omc;

import java.io.File;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class OMCSkinnerActivity extends Activity {

	public Handler mHandler;
	public static TextView TEXT;
	public static LocationManager LM;
	public static Location CURRLOCN;
	public static LocationListener LL;
	public static String TRAFFICRESULTS;
	public static HashMap<String,String[]> ELEMENTS;
	public static HashMap<String,File> THEMES;
	public static String tempText = "";
	public static File SDROOT, THEMEROOT;
	public static ArrayAdapter<String> THEMEARRAY;
	public static Spinner THEMESPINNER;
	public static char[] THEMECREDITS;
	public static String CURRSELECTEDTHEME, RAWCONTROLFILE;

	public ImageView FourByTwo, FourByOne, ThreeByOne;
	public Bitmap bmpRender;
	public String sTheme;
	
	public Thread refreshThread;

	public static int REFRESHINTERVAL;
	public boolean bCustomStretch, bExternal;
    static AlertDialog mAD;	

	final Runnable mResult = new Runnable() {
		public void run() {
			refreshViews();
		}
	};

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        //Hide the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.skinnertool);

        OMCSkinnerActivity.REFRESHINTERVAL = 3000;
        
        mHandler = new Handler();

        Intent it = new Intent(this,OMCThemePickerActivity.class);
        startActivityForResult(it, 0);

    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// TODO Auto-generated method stub
    	super.onActivityResult(requestCode, resultCode, data);

    	if (resultCode == Activity.RESULT_CANCELED) {
    		finish();
    		return;
    	}
    	
    	sTheme = (String)(data.getExtras().get("theme"));
    	bExternal = (Boolean)(data.getExtras().get("external"));
    	bCustomStretch = true;
    	
    	Toast.makeText(this, "Refreshing from SD card every " + OMCSkinnerActivity.REFRESHINTERVAL/1000 + " seconds.", Toast.LENGTH_SHORT).show();
    	
    	FourByTwo = (ImageView)this.findViewById(R.id.FourByTwo);
    	FourByOne = (ImageView)this.findViewById(R.id.FourByOne);
    	ThreeByOne = (ImageView)this.findViewById(R.id.ThreeByOne);


    	OMC.PREFS.edit().putString("widgetTheme-1", (String)(data.getExtras().get("theme")))
		.putBoolean("widget24HrClock-1", OMC.PREFS.getBoolean("widget24HrClock", true))
		.putBoolean("external-1", (Boolean)(data.getExtras().get("external")))
		.putString("URI-1", OMC.PREFS.getString("URI", ""))
		.commit();
    	
    	refreshThread = new Thread() {

    		public void run() {
    			while (true) {
	    	    	OMCXMLThemeParser parser = new OMCXMLThemeParser(Environment.getExternalStorageDirectory().getAbsolutePath()
	    					+"/OMC/" + sTheme);

	    	    	Log.i("OMCSkinner","about to parse " + Environment.getExternalStorageDirectory().getAbsolutePath()
	    					+"/OMC/" + sTheme);
	    			parser.importTheme();
	
	    			while (!parser.doneParsing){
	    				try {
	    					Thread.sleep(500);
	    				} catch (InterruptedException e) {
	    					break;
	    				}
	    			}
	    			
//	    			if (!parser.valid) {
//	    	        	Toast.makeText(this, sTheme + " is an invalid theme.  See logcat for errors.", Toast.LENGTH_LONG).show();
//	    	        	return;
//	    			}
	    			
	    			mHandler.post(mResult);

	    			try {
	    				Thread.sleep(OMCSkinnerActivity.REFRESHINTERVAL);
					} catch (InterruptedException e) {
						break;
					}
    			}
    		};
		};
		refreshThread.start();
    }
    
    public void refreshViews() {
		String[] FourByOneStretch = new String[4];
		String[] ThreeByOneStretch = new String[4];

    	bmpRender = OMCWidgetDrawEngine.drawBitmapForWidget(this, -1);
    	FourByTwo.setImageBitmap(bmpRender);
		
		try {
			if (bExternal) {
				OMC.IMPORTEDTHEMEMAP.get(sTheme).arrays.get(sTheme + "_4x1SqueezeInfo").toArray(FourByOneStretch);
				OMC.IMPORTEDTHEMEMAP.get(sTheme).arrays.get(sTheme + "_3x1SqueezeInfo").toArray(ThreeByOneStretch);
			} else {
				int iLayerID = OMC.RES.getIdentifier(sTheme + "_4x1SqueezeInfo", "array", "com.sunnykwong.omc");
				FourByOneStretch = OMC.RES.getStringArray(iLayerID);
				iLayerID = OMC.RES.getIdentifier(sTheme + "_3x1SqueezeInfo", "array", "com.sunnykwong.omc");
				ThreeByOneStretch = OMC.RES.getStringArray(iLayerID);
			}
		} catch (android.content.res.Resources.NotFoundException e) {
			// OMC.STRETCHINFO stays null; do nothing
			if (OMC.DEBUG) Log.i("OMCEngine","No stretch info found for seeded clock.");
			bCustomStretch=false;
		} catch (java.lang.NullPointerException e) {
			// OMC.STRETCHINFO stays null; do nothing
			if (OMC.DEBUG) Log.i("OMCEngine","No stretch info found for imported clock.");
			bCustomStretch=false;
		}

		if (bCustomStretch){
//			System.out.println("CustomStretch" + FourByOneStretch[0] + FourByOneStretch[1]);
			//Custom scaling
			OMC.TEMPMATRIX.reset();
			OMC.TEMPMATRIX.preScale(Float.parseFloat(FourByOneStretch[0]), 
					Float.parseFloat(FourByOneStretch[1]));
			FourByOne.setImageBitmap(Bitmap.createBitmap(bmpRender, 0, Integer.parseInt(FourByOneStretch[2]), OMC.BUFFER.getWidth(), OMC.BUFFER.getHeight() - Integer.parseInt(FourByOneStretch[2]) - Integer.parseInt(FourByOneStretch[3]), OMC.TEMPMATRIX, true));
			OMC.TEMPMATRIX.reset();
			OMC.TEMPMATRIX.preScale(Float.parseFloat(ThreeByOneStretch[0]), 
					Float.parseFloat(ThreeByOneStretch[1]));
			ThreeByOne.setImageBitmap(Bitmap.createBitmap(bmpRender, 0, Integer.parseInt(ThreeByOneStretch[2]), OMC.BUFFER.getWidth(), OMC.BUFFER.getHeight() - Integer.parseInt(ThreeByOneStretch[2]) - Integer.parseInt(ThreeByOneStretch[3]), OMC.TEMPMATRIX, true));
		} else {
			//Default scaling
			OMC.TEMPMATRIX.reset();
//			OMC.TEMPMATRIX.preTranslate(0, 0-iCutTop);
//			OMC.TEMPMATRIX.preScale(fScaleX, fScaleY);
			FourByOne.setImageBitmap(Bitmap.createBitmap(bmpRender, 0, Integer.parseInt(FourByOneStretch[2]), OMC.BUFFER.getWidth(), OMC.BUFFER.getHeight() - Integer.parseInt(FourByOneStretch[2]) - Integer.parseInt(FourByOneStretch[3]), OMC.TEMPMATRIX, true));
			ThreeByOne.setImageBitmap(Bitmap.createBitmap(bmpRender, 0, Integer.parseInt(ThreeByOneStretch[2]), OMC.BUFFER.getWidth(), OMC.BUFFER.getHeight() - Integer.parseInt(ThreeByOneStretch[2]) - Integer.parseInt(ThreeByOneStretch[3]), OMC.TEMPMATRIX, true));
		}
		FourByTwo.invalidate();
		FourByOne.invalidate();
		ThreeByOne.invalidate();
   	
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	// TODO Auto-generated method stub
    	if (refreshThread !=null && refreshThread.isAlive())refreshThread.interrupt();
    }
    
}