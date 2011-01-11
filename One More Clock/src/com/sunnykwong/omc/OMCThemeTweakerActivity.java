package com.sunnykwong.omc;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.view.View.OnTouchListener;
import android.widget.AbsoluteLayout;


public class OMCThemeTweakerActivity extends Activity implements OnItemSelectedListener, OnTouchListener {

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

	public ImageView FourByTwo, FourByOne, ThreeByOne, vPreview, vDrag;
	public Bitmap bmpRender,bmpDrag;
	
	public Thread dragThread;

	public static int REFRESHINTERVAL;
	public boolean bCustomStretch;

	public int aWI;
	public int iXDown, iYDown;
	public float fXDown, fYDown, fXCurr, fYCurr;

	public JSONObject oTheme, baseTheme, oActiveLayer;
	public String sTheme;
	public String sActiveLayer;
	public boolean bRefresh;
	public AbsoluteLayout toplevel;
	public Spinner spinnerLayers;
	
	static AlertDialog mAD;	

	final Runnable mResult = new Runnable() {
		public void run() {
			refreshViews();
		}
	};

	final Runnable mDrag = new Runnable() {
		public void run() {
			refreshDrag();
		}
	};

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        //Hide the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(getResources().getIdentifier("tweakertool", "layout", OMC.PKGNAME));
        toplevel = (AbsoluteLayout)findViewById(getResources().getIdentifier("toplevel", "id", OMC.PKGNAME));

        OMCThemeTweakerActivity.REFRESHINTERVAL = 1000;
        
        mHandler = new Handler();
        
        aWI = getIntent().getIntExtra("aWI", -1);
        sTheme = getIntent().getStringExtra("theme");
    	OMC.PREFS.edit().putString("widgetTheme-1", sTheme)
		.putBoolean("widget24HrClock-1", true)
		.putString("URI-1", "")
		.commit();
     
        try {
        	
        	oTheme = OMC.getTheme(this, sTheme, true);
        	oTheme.put("name", oTheme.getString("name") + "(Tweaked)");
        	
        } catch (JSONException e) {
        	e.printStackTrace();
        }
        
        String[] layers = new String[oTheme.optJSONArray("layers_bottomtotop").length()];
        for (int i = 0; i < layers.length; i++) {
        	layers[i]=oTheme.optJSONArray("layers_bottomtotop").optJSONObject(i).optString("name");
        }

        ArrayAdapter<String> aa = new ArrayAdapter<String>(this, getResources().getIdentifier("tweakerlayer", "layout", OMC.PKGNAME),layers);
        aa.setDropDownViewResource(getResources().getIdentifier("tweakerlayerdropdown", "layout", OMC.PKGNAME));
        spinnerLayers = (Spinner)findViewById(getResources().getIdentifier("tweakerlayerspinner", "id", OMC.PKGNAME));
        spinnerLayers.setAdapter(aa);
        spinnerLayers.setOnItemSelectedListener(this);
        vPreview = (ImageView)findViewById(getResources().getIdentifier("tweakerpreview", "id", OMC.PKGNAME));
        vPreview.setImageBitmap(OMCWidgetDrawEngine.drawBitmapForWidget(this, -1));
        vDrag = (ImageView)findViewById(getResources().getIdentifier("tweakerdragpreview", "id", OMC.PKGNAME));
        vPreview.setOnTouchListener(this);
        

    	dragThread = new Thread() {
    		public void run() {
    			while (true) {
    				if (OMCThemeTweakerActivity.this.bRefresh) {
	    				mHandler.post(mDrag);
	    				try {
		    				Thread.sleep(50);
						} catch (InterruptedException e) {
							break;
						}
    				}
    			}
    		};
		};
		bRefresh=false;
		dragThread.start();
    }
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    	//	No layer selected, do nothing    	
    }
    
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos,
    		long id) {
    	sActiveLayer = parent.getItemAtPosition(pos).toString();
    	oActiveLayer = oTheme.optJSONArray("layers_bottomtotop").optJSONObject(pos);
    	System.out.println(oActiveLayer.optString("name"));
    	refreshViews();
    }

    @Override
	@SuppressWarnings("deprecation")
    public boolean onTouch(View v, MotionEvent event) {
    	System.out.println("ONTOUCH");
    	if (v != vPreview) return false;
    	if (event.getAction()==MotionEvent.ACTION_DOWN) {
    		fXDown = event.getX();
    		fYDown = event.getY();
    		iXDown = oActiveLayer.optInt("x");
    		iYDown = oActiveLayer.optInt("y");
    		vDrag.setImageBitmap(OMCWidgetDrawEngine.drawLayerForWidget(this, aWI, oActiveLayer.optString("name")));
    		bRefresh = true;
    		System.out.println("DN x "+ event.getX() + " y " + event.getY());
    		
    	}
    	if (event.getAction()==MotionEvent.ACTION_MOVE) {
    		System.out.println(fYDown);
    		fXCurr = oActiveLayer.optInt("x");
    		fYCurr = oActiveLayer.optInt("y");
    		float fScaleFactor = (float)vPreview.getMeasuredWidth()/OMC.WIDGETWIDTH;
    		try {
	    		oActiveLayer.put("x", iXDown+(event.getX()-fXDown)/fScaleFactor);
	    		oActiveLayer.put("y", iYDown+(event.getY()-fYDown)/fScaleFactor);
    		} catch (JSONException e) {
    			e.printStackTrace();
    		}
    		vDrag.setLayoutParams(new AbsoluteLayout.LayoutParams(AbsoluteLayout.LayoutParams.WRAP_CONTENT,AbsoluteLayout.LayoutParams.WRAP_CONTENT,(int)event.getX(), (int)event.getY() ));

    		
    	}
    	if (event.getAction()==MotionEvent.ACTION_UP) {
    		bRefresh = false;
    		vDrag.setImageResource(R.drawable.transparent);
    		refreshDrag();
    		refreshViews();
    		System.out.println("UP x "+ event.getX() + " y " + event.getY());
    		
    	}
    	return true;
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
    	
    	Toast.makeText(this, "Refreshing from SD card every " + OMCThemeTweakerActivity.REFRESHINTERVAL/1000 + " seconds.", Toast.LENGTH_SHORT).show();
    	
    	FourByTwo = (ImageView)this.findViewById(getResources().getIdentifier("FourByTwo", "id", OMC.PKGNAME));
    	FourByOne = (ImageView)this.findViewById(getResources().getIdentifier("FourByOne", "id", OMC.PKGNAME));
    	ThreeByOne = (ImageView)this.findViewById(getResources().getIdentifier("ThreeByOne", "id", OMC.PKGNAME));


    }

    public void refreshDrag() {
        // redraw the canvas
    	vDrag.invalidate();
        toplevel.requestLayout();        
    }
    
    public void refreshViews() {
		vPreview.setImageBitmap(OMCWidgetDrawEngine.drawBitmapForWidget(this, -1));
		
		System.out.println("redraw");
		vPreview.invalidate();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	// TODO Auto-generated method stub
    	if (dragThread !=null && dragThread.isAlive())dragThread.interrupt();
    }
    
}