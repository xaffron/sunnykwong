package com.sunnykwong.omc;

import java.io.File;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.activities.ColorPickerDialog;


public class OMCThemeTweakerActivity extends Activity implements OnItemSelectedListener, OnTouchListener, OnLongClickListener {

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

	public ImageView FourByTwo, FourByOne, ThreeByOne, vPreview, vDrag, vBounds;
	public Bitmap bmpRender,bmpDrag;
	
	public static int REFRESHINTERVAL;
	public boolean bCustomStretch;

	public boolean bApply;
	public int aWI;
	public int iXDown, iYDown;
	public int iRectWidth, iRectHeight;
	public float fXDown=-1f, fYDown, fXMove, fYMove;

	public JSONObject oTheme, baseTheme, oActiveLayer;
	public int iActivepos;
	public String sTheme;
	public String sActiveLayer;
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
//      requestWindowFeature(Window.FEATURE_NO_TITLE);

		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	Toast.makeText(this, OMC.RString("sdcardNotDetected"), Toast.LENGTH_LONG).show();
			finish();
        	return;
        }
        OMCThemePickerActivity.SDROOT = Environment.getExternalStorageDirectory();
		if (!OMCThemePickerActivity.SDROOT.canRead()) {
        	Toast.makeText(this, OMC.RString("sdcardMissingOrCorrupt"), Toast.LENGTH_LONG).show();
			finish();
        	return;
        }

		setContentView(OMC.RLayoutId("tweakertool"));
        toplevel = (AbsoluteLayout)findViewById(OMC.RId("toplevel"));

        OMCThemeTweakerActivity.REFRESHINTERVAL = 1000;

        bApply = false;
        
        mHandler = new Handler();
        
        aWI = getIntent().getIntExtra("aWI", -1);
        sTheme = getIntent().getStringExtra("theme");
        
     
        try {
        	
        	baseTheme = OMC.getTheme(this, sTheme, true);
        	if (baseTheme==null) {
        		Toast.makeText(this, OMC.RString("themeNotFound"), Toast.LENGTH_LONG).show();
        		OMC.PREFS.edit().putString("widgettheme"+aWI, OMC.DEFAULTTHEME).commit();
        		finish();
        	}
        	oTheme = new JSONObject(baseTheme.toString());
        	// If this is already a tweak, just edit the current theme
        	if (sTheme.endsWith("Tweak")) {
        		
        	} else {
	        	// Otherwise, edit a tweaked theme to preserve "stock"
	        	oTheme.put("id", baseTheme.getString("id") + "Tweak");
	        	oTheme.put("tweaked", true);
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

        ArrayAdapter<String> aa = new ArrayAdapter<String>(this, OMC.RLayoutId("tweakerlayer"),layers);
        aa.setDropDownViewResource(OMC.RLayoutId("tweakerlayerdropdown"));
        spinnerLayers = (Spinner)findViewById(OMC.RId("tweakerlayerspinner"));
        spinnerLayers.setAdapter(aa);
        spinnerLayers.setOnItemSelectedListener(this);

        vPreview = (ImageView)findViewById(OMC.RId("tweakerpreview"));
        Bitmap bmp = OMCWidgetDrawEngine.drawBitmapForWidget(this, -1);
        bmp.setDensity(Bitmap.DENSITY_NONE);
        vPreview.setImageBitmap(bmp);
        vPreview.invalidate();
//        System.out.println(vPreview.getWidth());
//        System.out.println(vPreview.getHeight());
        vPreview.setOnLongClickListener(this);
        vPreview.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				OMCThemeTweakerActivity.this.openOptionsMenu();
			}
		});
        vPreview.invalidate();

        vBounds = (ImageView)findViewById(OMC.RId("tweakerbounds"));
        
        Rect BoundingBox ;
        try {
        	JSONObject box = oTheme.getJSONObject("customscaling").getJSONObject("4x2");
        	BoundingBox = new Rect(box.getInt("left_crop"),box.getInt("top_crop"),
        			OMC.WIDGETWIDTH-box.getInt("right_crop")-1,
        			OMC.WIDGETHEIGHT-box.getInt("bottom_crop")-1);
        } catch (JSONException e) {
        	e.printStackTrace();
        	BoundingBox = new Rect(0,0,480,320);
        }
        Bitmap tempBmp = Bitmap.createBitmap(OMC.WIDGETWIDTH,OMC.WIDGETHEIGHT,OMC.HDRENDERING?Bitmap.Config.ARGB_8888:Bitmap.Config.ARGB_4444);
        Canvas tempCvas = new Canvas(tempBmp);
        Paint tempPaint = new Paint();
        tempPaint.setStyle(Style.STROKE);
        tempPaint.setColor(Color.GREEN);
        
        tempCvas.drawRect(BoundingBox, tempPaint);
        vBounds.setImageBitmap(tempBmp);
        vBounds.invalidate();
        
        vDrag = (ImageView)findViewById(OMC.RId("tweakerdragpreview"));
        
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == android.view.KeyEvent.KEYCODE_BACK || keyCode == android.view.KeyEvent.KEYCODE_HOME) {
    		mAD = new AlertDialog.Builder(this)
    				.setTitle(OMC.RString("keepChanges"))
    				.setPositiveButton(OMC.RString("apply"), new DialogInterface.OnClickListener() {	
						@Override
						public void onClick(DialogInterface dialog, int which) {
				    		bApply=true;
				    		// If it's already a tweak, just overwrite the control file
				        	if (sTheme.endsWith("Tweak")) {
				        		OMC.themeToFile(oTheme, new File(Environment.getExternalStorageDirectory()+"/.OMCThemes/"+sTheme+"/00control.json"));
				        		OMC.bmpToJPEG(OMCWidgetDrawEngine.drawBitmapForWidget(OMCThemeTweakerActivity.this, -1), new File(Environment.getExternalStorageDirectory()+"/.OMCThemes/"+sTheme+"/000preview.jpg"));
				        		// Purge all caches to eliminate "non-sticky" settings bug
				            	OMC.purgeBitmapCache();
				            	OMC.purgeImportCache();
				            	OMC.purgeEmailCache();
				        		OMC.purgeTypefaceCache();
				        		OMC.THEMEMAP.clear();
				            	OMC.WIDGETBMPMAP.clear();
				        	} else {
				        		// Otherwise, create a copy of the theme	
				        		OMC.copyDirectory(new File(Environment.getExternalStorageDirectory()+"/.OMCThemes/"+sTheme), new File(Environment.getExternalStorageDirectory()+"/.OMCThemes/"+sTheme+"Tweak"));
				        		OMC.PREFS.edit().putString("widgetTheme"+aWI, sTheme+"Tweak")
				        				.putString("widgetTheme", sTheme+"Tweak")
				        				.commit();
				        		OMC.themeToFile(oTheme, new File(Environment.getExternalStorageDirectory()+"/.OMCThemes/"+sTheme+"Tweak/00control.json"));
				        		OMC.bmpToJPEG(OMCWidgetDrawEngine.drawBitmapForWidget(OMCThemeTweakerActivity.this, -1), new File(Environment.getExternalStorageDirectory()+"/.OMCThemes/"+sTheme+"Tweak/000preview.jpg"));
				        		// Purge all caches to eliminate "non-sticky" settings bug
				            	OMC.purgeBitmapCache();
				            	OMC.purgeImportCache();
				            	OMC.purgeEmailCache();
				        		OMC.purgeTypefaceCache();
				        		OMC.THEMEMAP.clear();
				            	OMC.WIDGETBMPMAP.clear();
				        	}
				        	finish();
						}
    				})
    				.setNegativeButton(OMC.RString("abandon"), new DialogInterface.OnClickListener() {	
						@Override
						public void onClick(DialogInterface dialog, int which) {
				    		oTheme = null;
				    		finish();
						}
    				})
    				.show();
    		return true;
    	}

    	return super.onKeyDown(keyCode, event);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(OMC.RMenuId("tweakermenu"), menu);
		return true;
	}

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    	//	No layer selected, do nothing    	
    }
    
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos,
    		long id) {
    	iActivepos = pos;
    	sActiveLayer = parent.getItemAtPosition(pos).toString();
    	oActiveLayer = oTheme.optJSONArray("layers_bottomtotop").optJSONObject(iActivepos);
    	refreshViews();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// For Honeycomb and above, sometimes this gets called before active layer is initialized
    	if (oActiveLayer==null) {
    		menu.findItem(OMC.RId("tweakmenulayerenable")).setEnabled(false);
    		menu.findItem(OMC.RId("tweakmenupickColor1")).setEnabled(false);
   			menu.findItem(OMC.RId("tweakmenupickColor2")).setEnabled(false);
   			return super.onPrepareOptionsMenu(menu);
    	}

    	String sLayerType = oActiveLayer.optString("type");
    	if (sLayerType==null) {
    		menu.findItem(OMC.RId("tweakmenulayerenable")).setEnabled(false);
    		menu.findItem(OMC.RId("tweakmenupickColor1")).setEnabled(false);
   			menu.findItem(OMC.RId("tweakmenupickColor2")).setEnabled(false);
    	} else if (sLayerType.equals("image")) {
    		menu.findItem(OMC.RId("tweakmenulayerenable")).setEnabled(true);
    		menu.findItem(OMC.RId("tweakmenupickColor1")).setEnabled(false);
    		menu.findItem(OMC.RId("tweakmenupickColor2")).setEnabled(false);
    	} else if (sLayerType.equals("flare")) {
    		menu.findItem(OMC.RId("tweakmenulayerenable")).setEnabled(true);
    		menu.findItem(OMC.RId("tweakmenupickColor1")).setEnabled(false);
    		menu.findItem(OMC.RId("tweakmenupickColor2")).setEnabled(false);
    	} else {
    		menu.findItem(OMC.RId("tweakmenulayerenable")).setEnabled(true);
    		menu.findItem(OMC.RId("tweakmenupickColor1")).setEnabled(true);
    		menu.findItem(OMC.RId("tweakmenupickColor2")).setEnabled(true);
    	}
    	
    	return super.onPrepareOptionsMenu(menu);
    }
    
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	if (item.getItemId()==OMC.RId("tweakmenulayerenable")) {
			try {
				if (oActiveLayer.optBoolean("enabled")) oActiveLayer.put("enabled",false);
				else oActiveLayer.put("enabled",true);
				refreshViews();
			} catch (JSONException e) {
				e.printStackTrace();
			}
    	}
    	if (item.getItemId()==OMC.RId("tweakmenupickColor1")) {
    		int initialColor;
    		if (oActiveLayer.optString("fgcolor")==null) {
    			initialColor = Color.BLACK;
    		} else {
	    		try {
	    			initialColor = Color.parseColor(oActiveLayer.optString("fgcolor"));
	    		} catch (IllegalArgumentException e) {
	    			initialColor = Color.BLACK;
	    		}
    		}
    		ColorPickerDialog cpd = new ColorPickerDialog(this, new ColorPickerDialog.OnColorChangedListener() {
				
				@Override
				public void colorUpdate(int color) {
				}
				
				@Override
				public void colorChanged(int color) {
					try {
						String sColor = "#"+Long.toHexString(0x300000000l + color).substring(1).toUpperCase();
						oActiveLayer.put("fgcolor",sColor);
						refreshViews();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}, initialColor);
    		cpd.show();
    	}
    	if (item.getItemId()==OMC.RId("tweakmenupickColor2")) {
    		int initialColor;
    		if (oActiveLayer.optString("bgcolor")==null) {
    			initialColor = Color.BLACK;
    		} else {
	    		try {
	    			initialColor = Color.parseColor(oActiveLayer.optString("bgcolor"));
	    		} catch (IllegalArgumentException e) {
	    			initialColor = Color.BLACK;
	    		}
    		}
    		ColorPickerDialog cpd = new ColorPickerDialog(this, new ColorPickerDialog.OnColorChangedListener() {
				
				@Override
				public void colorUpdate(int color) {
				}
				
				@Override
				public void colorChanged(int color) {
					try {
						String sColor = "#"+Long.toHexString(0x300000000l + color).substring(1).toUpperCase();
						oActiveLayer.put("bgcolor",sColor);
						refreshViews();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}, initialColor);
    		cpd.show();
    	}
		return true;
	}

    @Override
	public boolean onLongClick(View v){
    	if (v != vPreview) return false;
    	vPreview.setOnTouchListener(this);
		return true;
	}
	
    @Override
	@SuppressWarnings("deprecation")
    public boolean onTouch(View v, MotionEvent event) {
    	if (v != vPreview) return false;
    	if (event.getAction()==MotionEvent.ACTION_DOWN) {
    		// Not longer setting initial drag vals here because we want longclick.
    	}
    	if (event.getAction()==MotionEvent.ACTION_MOVE) {
    		if (fXDown==-1f) {
        		fXDown = event.getX();
        		fYDown = event.getY();
        		try {
        			final JSONObject tempTheme = OMC.renderThemeObject(oTheme, -1);
            		final JSONObject tempActiveLayer = tempTheme.optJSONArray("layers_bottomtotop").optJSONObject(iActivepos);

            		if (tempActiveLayer.optString("type").equals("panel")) {
                		iXDown = tempActiveLayer.optInt("left");
                		iYDown = tempActiveLayer.optInt("top");
        			} else {
        	    		iXDown = tempActiveLayer.optInt("x");
        	    		iYDown = tempActiveLayer.optInt("y");
        			}
            		iRectWidth = tempActiveLayer.optInt("right")-tempActiveLayer.optInt("left");
            		iRectHeight = tempActiveLayer.optInt("bottom")-tempActiveLayer.optInt("top");
            		
            		vDrag.setImageBitmap(OMCWidgetDrawEngine.drawLayerForWidget(this, aWI, tempTheme, tempActiveLayer.optString("name")));
        			
        		} catch (JSONException e ) {
        			e.printStackTrace();
        		}
        		
    		}
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
    		vDrag.setImageResource(OMC.RDrawableId("transparent"));
    		refreshDrag();
    		refreshViews();
    		vPreview.setOnTouchListener(null);
    		fXDown=-1f;
    	}
    	return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);

    	if (resultCode == Activity.RESULT_CANCELED) {
    		finish();
    		return;
    	}
    	
    	sTheme = (String)(data.getExtras().get("theme"));
    	bCustomStretch = true;
    	
//    	Toast.makeText(this, "Refreshing from SD card every " + OMCThemeTweakerActivity.REFRESHINTERVAL/1000 + " seconds.", Toast.LENGTH_SHORT).show();
    	
    	FourByTwo = (ImageView)this.findViewById(OMC.RId("FourByTwo"));
    	FourByOne = (ImageView)this.findViewById(OMC.RId("FourByOne"));
    	ThreeByOne = (ImageView)this.findViewById(OMC.RId("ThreeByOne"));


    }

    public void refreshDrag() {
        // redraw the canvas
    	vDrag.invalidate();
        toplevel.requestLayout();        
    }
    
    public void refreshViews() {
		vPreview.setImageBitmap(OMCWidgetDrawEngine.drawBitmapForWidget(this, -1));
		vPreview.invalidate();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	if (!bApply) {
    		OMC.THEMEMAP.put(sTheme, baseTheme);
    	}
    }
    
}