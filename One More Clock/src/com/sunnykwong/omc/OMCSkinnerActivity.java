package com.sunnykwong.omc;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.File;
import java.util.HashMap;
import android.graphics.Paint;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import android.view.View.OnClickListener;

import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.AdapterView.OnItemSelectedListener;
import android.os.Bundle;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Environment;
import android.widget.BaseAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.view.View.OnClickListener;
import android.net.Uri;
import android.widget.Gallery;

public class OMCSkinnerActivity extends Activity implements OnClickListener {

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
	public static ImageAdapter THEMEARRAY;
	public static Spinner THEMESPINNER;
	public static char[] THEMECREDITS;
	public static String CURRSELECTEDTHEME, RAWCONTROLFILE;
	
	public Button btnReload;
	public Gallery gallery;
	public TextView tvCredits;
	
    static AlertDialog mAD;	

	final Runnable mResult = new Runnable() {
		public void run() {
		// Back from XML importing...
			if (OMCXMLThemeParser.valid) {
	        	Toast.makeText(OMCSkinnerActivity.this, OMC.IMPORTEDTHEMEMAP.get(OMCXMLThemeParser.latestThemeName).arrays.get("theme_options").get(0) + " theme imported.", Toast.LENGTH_SHORT).show();
	        	OMC.PREFS.edit()
			        	.putString("widgetTheme", OMCXMLThemeParser.latestThemeName)
			        	.putBoolean("external", true)
			    		.commit();
	        	OMC.saveImportedThemeToCache(OMCSkinnerActivity.this,OMCXMLThemeParser.latestThemeName);
	        	Toast.makeText(OMCSkinnerActivity.this, OMC.IMPORTEDTHEMEMAP.get(OMCXMLThemeParser.latestThemeName).arrays.get("theme_options").get(0) + " theme cached and applied.", Toast.LENGTH_SHORT).show();
			} else {
	        	Toast.makeText(OMCSkinnerActivity.this, OMCSkinnerActivity.CURRSELECTEDTHEME + " theme did not pass validity checks!\nPlease check with the author of your theme.\nImport cancelled.", Toast.LENGTH_SHORT).show();
			}

			setResult(Activity.RESULT_OK);
        	finish();
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.skinnerlayout);

        mHandler = new Handler();
        
        OMCSkinnerActivity.CURRSELECTEDTHEME = null;

        btnReload = (Button)findViewById(R.id.btnReload);
        btnReload.setOnClickListener(this);

        tvCredits = (TextView)findViewById(R.id.SkinnerThemeCredits);
        
        gallery = (Gallery)this.findViewById(R.id.gallery);
        gallery.setSpacing(10);
        refreshThemeList();

        gallery.setAdapter(OMCSkinnerActivity.THEMEARRAY);

        gallery.setOnItemSelectedListener(new OnItemSelectedListener() {
        	@Override
        	public void onItemSelected(AdapterView<?> arg0, View arg1,
        			int position, long id) {
        		// TODO Auto-generated method stub
        		//setThemePreview(OMCSkinnerActivity.THEMEARRAY.getItem(position));
        		System.out.println("Gallry onitemselected");
        		tvCredits.setText(OMCSkinnerActivity.THEMEARRAY.mCreds.get(position));
        		tvCredits.invalidate();
        	}
        	@Override
        	public void onNothingSelected(AdapterView<?> arg0) {
        		// do nothing
        		System.out.println("Gallry nothingselected");
       		
        	}
		});
        
//        ((Button)this.findViewById(R.id.buttonCancel)).setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				// TODO Auto-generated method stub
//				OMCSkinnerActivity.this.setResult(Activity.RESULT_OK);
//				OMCSkinnerActivity.this.finish();
//			}
//		});
//        
//        ((Button)this.findViewById(R.id.buttonOK)).setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				// TODO Auto-generated method stub
//				if (OMCSkinnerActivity.CURRSELECTEDTHEME!=null) {
//					if (!OMC.IMPORTEDTHEMEMAP.containsKey(OMCSkinnerActivity.CURRSELECTEDTHEME)) importTheme();
//					else {
//						
//						Toast.makeText(OMCSkinnerActivity.this, 
//								"Theme already imported and cached.\n" + 
//								OMC.IMPORTEDTHEMEMAP.get(
//										OMCSkinnerActivity.CURRSELECTEDTHEME).arrays
//										.get("theme_options").get(0) + " theme applied."
//										, Toast.LENGTH_SHORT).show();
//						
//			        	OMC.PREFS.edit()
//			        	.putString("widgetTheme", OMCSkinnerActivity.CURRSELECTEDTHEME)
//			        	.putBoolean("external", true)
//			    		.commit();
//
//			        	setResult(Activity.RESULT_OK);
//						finish();
//					}
//
//				}
//			}
//		});

    }
    
    @Override
    public void onClick(View v) {
    	// TODO Auto-generated method stub
    	if (v==btnReload) {
    		if (OMC.DEBUG) Log.i("OMCSkin","Refreshing Themes");
    		refreshThemeList();
    	}
    }
    
	public void setThemePreview(String sThemeName) {
		OMCSkinnerActivity.CURRSELECTEDTHEME = sThemeName;
		if (sThemeName == null || sThemeName.equals("")) return;
		File root = OMCSkinnerActivity.THEMES.get(sThemeName);
		if (OMC.DEBUG) Log.i("OMCSkinner",root.getAbsolutePath() + "/preview.png");
		Bitmap bmpPreview = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(root.getAbsolutePath() + "/preview.jpg"),320,200,false);
		((ImageView)this.findViewById(R.id.ImagePreview)).setImageBitmap(bmpPreview);
		OMCSkinnerActivity.THEMECREDITS = new char[3000];
		try {
			FileReader fr = new FileReader(root.getAbsolutePath() + "/00credits.txt");
			fr.read(OMCSkinnerActivity.THEMECREDITS);
			((TextView)this.findViewById(R.id.TextPreview)).setText(String.valueOf(OMCSkinnerActivity.THEMECREDITS).trim());
			this.findViewById(R.id.toplevel).invalidate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
 
	public void importTheme() {
		if (OMCSkinnerActivity.CURRSELECTEDTHEME == null) {
        	Toast.makeText(this, "Please select a theme first.", Toast.LENGTH_SHORT).show();
			return;
		}
		
        System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver"); 

		
		
    	Thread t = new Thread () {
    		public void run() {
            	try {
            		// Set SD OMC Root
            		File root = OMCSkinnerActivity.THEMES.get(OMCSkinnerActivity.CURRSELECTEDTHEME);
            		// Setup XML Parsing...
            		XMLReader xr = XMLReaderFactory.createXMLReader();
            		OMCXMLThemeParser parser = new OMCXMLThemeParser(root.getAbsolutePath());
            		xr.setContentHandler(parser);
            		// Feed data from control file to XML Parser.
            		// XML Parser will populate OMC.IMPORTEDTHEME.
            		FileReader fr = new FileReader(root.getAbsolutePath() + "/00control.xml");
            		xr.setErrorHandler(parser);
            		xr.parse(new InputSource(fr));
            		// When we're done, remove all references to parser.
                	parser = null;
                	fr.close();

            	} catch (Exception e) {
            		
                	e.printStackTrace();
            	}

            	// This call will end up passing control to processXMLResults
    			mHandler.post(mResult);
    		}
      	   
    	};
		t.start();

    } 

	public void refreshThemeList() {

		gallery.setVisibility(View.INVISIBLE);
		btnReload.setClickable(false);
		
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	Toast.makeText(this, "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
			setResult(Activity.RESULT_OK);
			finish();
        	return;
        }

        OMCSkinnerActivity.SDROOT = Environment.getExternalStorageDirectory();
        OMCSkinnerActivity.THEMEROOT = new File(OMCSkinnerActivity.SDROOT.getAbsolutePath()+"/OMC");
        if (!OMCSkinnerActivity.THEMEROOT.exists()) {
        	Toast.makeText(this, "OMC folder not found in your SD Card.\nCreating folder...", Toast.LENGTH_LONG).show();
        	OMCSkinnerActivity.THEMEROOT.mkdir();
        }
        if (OMCSkinnerActivity.THEMEROOT.listFiles().length == 0) {
        	//No themes downloaded
        	Toast.makeText(this, "No themes downloaded. Exiting!", Toast.LENGTH_LONG).show();
			setResult(Activity.RESULT_OK);
			finish();
        	return;
        }
        
        if (OMCSkinnerActivity.THEMEARRAY == null) {
        	OMCSkinnerActivity.THEMEARRAY = new ImageAdapter();
        } else {
        	OMCSkinnerActivity.THEMEARRAY.dispose();
            gallery.requestLayout();
        }
        OMCSkinnerActivity.THEMES =  new HashMap<String, File>();
        for (File f:OMCSkinnerActivity.THEMEROOT.listFiles()) {
        	if (!f.isDirectory()) continue;
        	File ff = new File(f.getAbsolutePath()+"/00control.xml");
        	if (ff.exists()) {
        		
        		OMCSkinnerActivity.THEMEARRAY.addItem(f.getName());
        		OMCSkinnerActivity.THEMES.put(f.getName(), f);
        	}
        }

		gallery.setVisibility(View.VISIBLE);
		btnReload.setClickable(true);
	}
	
    public class ImageAdapter extends BaseAdapter {

    	public ArrayList<String> mThemes = new ArrayList<String>();
    	public ArrayList<Bitmap> mBitmaps = new ArrayList<Bitmap>();
    	public ArrayList<String> mCreds = new ArrayList<String>();
    	

        public ImageAdapter() {
        }

        public void addItem(String sTheme){
        	System.out.println("adding " + sTheme);
        	mThemes.add(sTheme);
        	
            Bitmap buffer = Bitmap.createBitmap(320,240,Bitmap.Config.RGB_565);
            Canvas cvas = new Canvas(buffer);
            Paint p = new Paint();
            p.setTextSize(30f);
            p.setAntiAlias(true);
            p.setShadowLayer(2f, -1f, -1f, Color.GRAY);
            p.setFakeBoldText(true);
            p.setColor(Color.WHITE);
            cvas.drawText(sTheme, 0, 35, p);
            Bitmap oBmp = BitmapFactory.decodeFile(OMCSkinnerActivity.THEMEROOT.getAbsolutePath() + "/" + sTheme+"/preview.jpg");
            cvas.drawBitmap(oBmp, 0f, 40f,p);
            oBmp.recycle();
            mBitmaps.add(buffer);
 
    		OMCSkinnerActivity.THEMECREDITS = new char[3000];
    		try {
    			FileReader fr = new FileReader(OMCSkinnerActivity.THEMEROOT.getAbsolutePath()+ "/" + sTheme+"/00credits.txt");
    			fr.read(OMCSkinnerActivity.THEMECREDITS);
    			mCreds.add(String.valueOf(OMCSkinnerActivity.THEMECREDITS).trim());
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
}
        
        public int getCount() {
            return mThemes.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView i = new ImageView(OMCSkinnerActivity.this);
            i.setImageBitmap(mBitmaps.get(position));
            i.setLayoutParams(new Gallery.LayoutParams(240, 180));
            i.setScaleType(ImageView.ScaleType.FIT_XY);
            return i;
        }

        public void dispose() {
        	mThemes.clear();
        	mCreds.clear();
        	mBitmaps.clear();
        }
    
    }

}