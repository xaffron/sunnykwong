package com.sunnykwong.omc;

import java.io.File;
import java.util.HashMap;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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
	public boolean bCustomStretch;
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

        setContentView(OMC.RLayoutId("skinnertool"));

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
    	bCustomStretch = true;
    	
    	//Toast.makeText(this, "Refreshing from SD card every " + OMCSkinnerActivity.REFRESHINTERVAL/1000 + " seconds.", Toast.LENGTH_SHORT).show();
    	
    	FourByTwo = (ImageView)this.findViewById(OMC.RId("FourByTwo"));
    	FourByOne = (ImageView)this.findViewById(OMC.RId("FourByOne"));
    	ThreeByOne = (ImageView)this.findViewById(OMC.RId("ThreeByOne"));


    	OMC.PREFS.edit().putString("widgetTheme-1", (String)(data.getExtras().get("theme")))
		.putBoolean("widget24HrClock-1", OMC.PREFS.getBoolean("widget24HrClock", true))
		.putString("URI-1", OMC.PREFS.getString("URI", ""))
		.commit();
    	
    	refreshThread = new Thread() {

    		public void run() {
    			while (true) {
//	    	    	OMCJSONThemeParser parser = new OMCJSONThemeParser(OMC.WORKDIR
//	    					+"/" + sTheme);
//
//	    	    	Log.i(OMC.OMCSHORT + "Skinner","about to parse " + OMC.WORKDIR
//	    					+"/" + sTheme);
//	    			parser.importTheme();
//	
//	    			while (!parser.doneParsing){
//	    				try {
//	    					Thread.sleep(500);
//	    				} catch (InterruptedException e) {
//	    					break;
//	    				}
//	    			}
//	    			
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

    	JSONObject FourByOneStretch, ThreeByOneStretch;
    	bmpRender = OMCWidgetDrawEngine.drawBitmapForWidget(this, -1, true);
    	FourByTwo.setImageBitmap(bmpRender);
		
		try {
			FourByOneStretch = OMC.THEMEMAP.get(sTheme).getJSONObject("customscaling").getJSONObject("4x1");
			ThreeByOneStretch = OMC.THEMEMAP.get(sTheme).getJSONObject("customscaling").getJSONObject("3x1");

			final Matrix tempMatrix = new Matrix();
			tempMatrix.preScale(Float.parseFloat(FourByOneStretch.getString("horizontal_stretch")), 
					Float.parseFloat(FourByOneStretch.getString("vertical_stretch")));
			FourByOne.setImageBitmap(Bitmap.createBitmap(bmpRender, 0, Integer.parseInt(FourByOneStretch.getString("top_crop")), OMC.WIDGETWIDTH, OMC.WIDGETHEIGHT - Integer.parseInt(FourByOneStretch.getString("top_crop")) - Integer.parseInt(FourByOneStretch.getString("bottom_crop")), tempMatrix, true));
			tempMatrix.reset();
			tempMatrix.preScale(Float.parseFloat(ThreeByOneStretch.getString("horizontal_stretch")), 
					Float.parseFloat(ThreeByOneStretch.getString("vertical_stretch")));
			ThreeByOne.setImageBitmap(Bitmap.createBitmap(bmpRender, 0, Integer.parseInt(ThreeByOneStretch.getString("top_crop")), OMC.WIDGETWIDTH, OMC.WIDGETHEIGHT - Integer.parseInt(ThreeByOneStretch.getString("top_crop")) - Integer.parseInt(ThreeByOneStretch.getString("bottom_crop")), tempMatrix, true));

		} catch (org.json.JSONException e) {
			// OMC.STRETCHINFO stays null; do nothing
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Engine","No stretch info found for seeded clock.");

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