package com.sunnykwong.omc;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import com.android.settings.activities.ColorPickerDialog;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

	public boolean bApply;
	public int aWI;
	public int iXDown, iYDown;
	public int iRectWidth, iRectHeight;
	public float fXDown, fYDown, fXMove, fYMove;

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

        bApply = false;
        
        mHandler = new Handler();
        
        aWI = getIntent().getIntExtra("aWI", -1);
        sTheme = getIntent().getStringExtra("theme");
        
     
        try {
        	
        	baseTheme = OMC.getTheme(this, sTheme, true);
        	oTheme = new JSONObject(baseTheme.toString());
        	// If this is already a tweak, just edit the current theme
        	if (sTheme.endsWith("Tweak")) {
        		
        	} else {
	        	// Otherwise, edit a tweaked theme to preserve "stock"
	        	oTheme.put("id", baseTheme.getString("id") + "Tweak");
	        	oTheme.put("name", baseTheme.getString("name") + "(Tweaked)");
        	}
        	OMC.THEMEMAP.put(sTheme,oTheme);
        	
        } catch (JSONException e) {
        	e.printStackTrace();
        }
        
    	OMC.PREFS.edit().putString("widgetTheme-1", sTheme)
		.putBoolean("widget24HrClock-1", true)
		.putString("URI-1", "")
		.commit();

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
//		dragThread.start();
		
		
		
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(getResources().getIdentifier("tweakermenu", "menu", OMC.PKGNAME), menu);
		return true;
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
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
    	if (item.getItemId()==getResources().getIdentifier("tweakmenulayerenable", "id", OMC.PKGNAME)) {
			try {
				if (oActiveLayer.optBoolean("enabled")) oActiveLayer.put("enabled",false);
				else oActiveLayer.put("enabled",true);
				refreshViews();
			} catch (JSONException e) {
				e.printStackTrace();
			}
    	}
    	if (item.getItemId()==getResources().getIdentifier("tweakmenupickColor1", "id", OMC.PKGNAME)) {
    		System.out.println(oActiveLayer.optString("fgcolor"));
    		int initialColor;
    		try {
    			initialColor = Color.parseColor(oActiveLayer.optString("fgcolor"));
    		} catch (IllegalArgumentException e) {
    			initialColor = Color.BLACK;
    		}
    		ColorPickerDialog cpd = new ColorPickerDialog(this, new ColorPickerDialog.OnColorChangedListener() {
				
				@Override
				public void colorUpdate(int color) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void colorChanged(int color) {
					// TODO Auto-generated method stub
					try {
						oActiveLayer.put("fgcolor",String.format("#%X", color));
						refreshViews();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}, initialColor);
    		cpd.show();
    	}
    	if (item.getItemId()==getResources().getIdentifier("tweakmenupickColor2", "id", OMC.PKGNAME)) {
    		System.out.println(oActiveLayer.optString("bgcolor"));
    		int initialColor;
    		try {
    			initialColor = Color.parseColor(oActiveLayer.optString("bgcolor"));
    		} catch (IllegalArgumentException e) {
    			initialColor = Color.BLACK;
    		}
    		ColorPickerDialog cpd = new ColorPickerDialog(this, new ColorPickerDialog.OnColorChangedListener() {
				
				@Override
				public void colorUpdate(int color) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void colorChanged(int color) {
					// TODO Auto-generated method stub
					try {
						oActiveLayer.put("bgcolor",String.format("#%X", color));
						refreshViews();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}, initialColor);
    		cpd.show();
    	}
    	if (item.getItemId()==getResources().getIdentifier("tweakmenurevert", "id", OMC.PKGNAME)) {
    		oTheme = null;
    		finish();
    	}    	
    	if (item.getItemId()==getResources().getIdentifier("tweakmenuapply", "id", OMC.PKGNAME)) {
    		bApply=true;
    		// If it's already a tweak, just overwrite the control file
        	if (sTheme.endsWith("Tweak")) {
        		OMC.themeToFile(oTheme, new File(Environment.getExternalStorageDirectory()+"/OMC/"+sTheme+"/00control.json"));
        		OMC.bmpToJPEG(OMCWidgetDrawEngine.drawBitmapForWidget(this, -1), new File(Environment.getExternalStorageDirectory()+"/OMC/"+sTheme+"/000preview.jpg"));
        	} else {
        		// Otherwise, create a copy of the theme	
        		OMC.copyDirectory(new File(Environment.getExternalStorageDirectory()+"/OMC/"+sTheme), new File(Environment.getExternalStorageDirectory()+"/OMC/"+sTheme+"Tweak"));
        		OMC.PREFS.edit().putString("widgetTheme"+aWI, sTheme+"Tweak")
        				.putString("widgetTheme", sTheme+"Tweak")
        				.commit();
        		OMC.themeToFile(oTheme, new File(Environment.getExternalStorageDirectory()+"/OMC/"+sTheme+"Tweak/00control.json"));
        		OMC.bmpToJPEG(OMCWidgetDrawEngine.drawBitmapForWidget(this, -1), new File(Environment.getExternalStorageDirectory()+"/OMC/"+sTheme+"Tweak/000preview.jpg"));
        	}
    		OMC.purgeBitmapCache();
    		OMC.purgeTypefaceCache();
    		OMC.purgeImportCache();
    		OMC.THEMEMAP.clear();
    		finish();
    		
    	}    	
		return true;
	}
    
    @Override
	@SuppressWarnings("deprecation")
    public boolean onTouch(View v, MotionEvent event) {
    	System.out.println("ONTOUCH");
    	if (v != vPreview) return false;
    	if (event.getAction()==MotionEvent.ACTION_DOWN) {
    		fXDown = event.getX();
    		fYDown = event.getY();
    		if (oActiveLayer.optString("type").equals("panel")) {
        		iXDown = oActiveLayer.optInt("left");
        		iYDown = oActiveLayer.optInt("top");
			} else {
	    		iXDown = oActiveLayer.optInt("x");
	    		iYDown = oActiveLayer.optInt("y");
			}
    		iRectWidth = oActiveLayer.optInt("right")-oActiveLayer.optInt("left");
    		iRectHeight = oActiveLayer.optInt("bottom")-oActiveLayer.optInt("top");
    		
    		vDrag.setImageBitmap(OMCWidgetDrawEngine.drawLayerForWidget(this, aWI, oActiveLayer.optString("name")));
    		bRefresh = true;
    	}
    	if (event.getAction()==MotionEvent.ACTION_MOVE) {
    		fXMove = event.getX();
    		fYMove = event.getY();
    		float fScaleFactor = (float)vPreview.getMeasuredWidth()/OMC.WIDGETWIDTH;
    		try {
    			if (oActiveLayer.getString("type").equals("panel")) {
    				oActiveLayer.put("left", iXDown+(fXMove-fXDown)/fScaleFactor);
    				oActiveLayer.put("top", iYDown+(fYMove-fYDown)/fScaleFactor);
    				oActiveLayer.put("right", iXDown+(fXMove-fXDown)/fScaleFactor + iRectWidth);
    				oActiveLayer.put("bottom", iYDown+(fYMove-fYDown)/fScaleFactor + iRectHeight);
    			} else {
    				oActiveLayer.put("x", iXDown+(fXMove-fXDown)/fScaleFactor);
    				oActiveLayer.put("y", iYDown+(fYMove-fYDown)/fScaleFactor);
    			}
    		} catch (JSONException e) {
    			e.printStackTrace();
    		}
    		vDrag.setLayoutParams(new AbsoluteLayout.LayoutParams(AbsoluteLayout.LayoutParams.WRAP_CONTENT,AbsoluteLayout.LayoutParams.WRAP_CONTENT,
    				(int)event.getX()-(int)fXDown, (int)event.getY()-(int)fYDown ));
			mHandler.post(mDrag);

    		
    	}
    	if (event.getAction()==MotionEvent.ACTION_UP) {
    		bRefresh = false;
    		vDrag.setImageResource(R.drawable.transparent);
    		refreshDrag();
    		refreshViews();
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
    	if (!bApply) {
    		OMC.THEMEMAP.put(sTheme, baseTheme);
    	}
    }
    
}